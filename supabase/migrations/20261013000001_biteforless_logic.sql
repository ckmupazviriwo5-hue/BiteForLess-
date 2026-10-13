-- Bite For Less — server-side auth + business logic (Phase 6)
-- All RPC functions are SECURITY DEFINER with a fixed search_path and
-- validate the caller inside the function body.

-- 1. Profile auto-creation: every new auth user gets a customer profile.
--    The role NEVER comes from client-supplied metadata.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.profiles (id, email, name, role)
    VALUES (
        NEW.id,
        COALESCE(NEW.email, ''),
        COALESCE(NEW.raw_user_meta_data ->> 'name', ''),
        'customer'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 2. Role-escalation guard: only admins may change profiles.role.
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    r TEXT;
BEGIN
    SELECT role INTO r FROM public.profiles WHERE id = auth.uid();
    RETURN r = 'admin';
END;
$$;

CREATE OR REPLACE FUNCTION public.prevent_role_escalation()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF NEW.role IS DISTINCT FROM OLD.role AND NOT public.is_admin() THEN
        RAISE EXCEPTION 'role changes require an administrator';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS profiles_role_guard ON public.profiles;
CREATE TRIGGER profiles_role_guard
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.prevent_role_escalation();

-- 3. Vendor onboarding guard: client inserts always land as pending,
--    owned by the caller. Approval happens only via set_vendor_status.
CREATE OR REPLACE FUNCTION public.force_pending_vendor()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    NEW.status := 'pending';
    IF NEW.owner_id IS NULL THEN
        NEW.owner_id := auth.uid();
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS vendors_onboarding_guard ON public.vendors;
CREATE TRIGGER vendors_onboarding_guard
    BEFORE INSERT ON public.vendors
    FOR EACH ROW EXECUTE FUNCTION public.force_pending_vendor();

-- 4. Secure reservation creation (atomic, oversell-safe).
--    Locks the listing row; price/total/customer come from the database.
CREATE OR REPLACE FUNCTION public.create_reservation(p_listing_id TEXT, p_quantity INTEGER)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_customer UUID := auth.uid();
    v_listing public.listings%ROWTYPE;
    v_vendor_status TEXT;
    v_res_id TEXT;
    v_pickup TEXT;
    v_total NUMERIC(10, 2);
BEGIN
    IF v_customer IS NULL THEN
        RAISE EXCEPTION 'authentication required';
    END IF;
    IF p_quantity IS NULL OR p_quantity < 1 THEN
        RAISE EXCEPTION 'quantity must be at least 1';
    END IF;

    SELECT * INTO v_listing FROM public.listings WHERE id = p_listing_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'listing not found';
    END IF;
    SELECT status INTO v_vendor_status FROM public.vendors WHERE id = v_listing.vendor_id;
    IF v_vendor_status IS DISTINCT FROM 'approved' THEN
        RAISE EXCEPTION 'vendor is not taking orders right now';
    END IF;
    IF v_listing.status IS DISTINCT FROM 'active' THEN
        RAISE EXCEPTION 'deal is no longer available';
    END IF;
    IF v_listing.deadline IS NOT NULL AND v_listing.deadline <= now() THEN
        RAISE EXCEPTION 'deal is no longer available';
    END IF;
    IF v_listing.stock < p_quantity THEN
        RAISE EXCEPTION 'only % left in stock', v_listing.stock;
    END IF;

    v_total := v_listing.discounted_price * p_quantity;
    v_res_id := 'RES-' || to_char(now(), 'YYYYMMDDHH24MISS') || '-'
        || lpad(floor(random() * 10000)::TEXT, 4, '0');
    v_pickup := 'DEMO-' || lpad(floor(random() * 10000)::TEXT, 4, '0');

    UPDATE public.listings SET stock = stock - p_quantity WHERE id = p_listing_id;

    INSERT INTO public.reservations
        (id, customer_id, listing_id, vendor_id, quantity, unit_price, total,
         status, pickup_code)
    VALUES (v_res_id, v_customer, p_listing_id, v_listing.vendor_id, p_quantity,
        v_listing.discounted_price, v_total, 'reserved', v_pickup);

    INSERT INTO public.activity_events (id, type, text, actor_id, actor_label)
    VALUES ('EVT-' || v_res_id, 'reservation_created',
        'Reservation ' || v_res_id || ' × ' || p_quantity || ' on ' || v_listing.name,
        v_customer, (SELECT email FROM public.profiles WHERE id = v_customer));

    RETURN jsonb_build_object('id', v_res_id, 'pickup_code', v_pickup,
        'quantity', p_quantity, 'total', v_total, 'status', 'reserved');
END;
$$;

-- 5. Cancellation: owner or admin; restores stock exactly once.
CREATE OR REPLACE FUNCTION public.cancel_reservation(p_reservation_id TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_caller UUID := auth.uid();
    v_res public.reservations%ROWTYPE;
BEGIN
    IF v_caller IS NULL THEN
        RAISE EXCEPTION 'authentication required';
    END IF;
    SELECT * INTO v_res FROM public.reservations WHERE id = p_reservation_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'reservation not found';
    END IF;
    IF v_res.customer_id IS DISTINCT FROM v_caller AND NOT public.is_admin() THEN
        RAISE EXCEPTION 'not your reservation';
    END IF;
    IF v_res.status IS DISTINCT FROM 'reserved' THEN
        RAISE EXCEPTION 'only active reservations can be cancelled';
    END IF;

    UPDATE public.reservations SET status = 'cancelled' WHERE id = p_reservation_id;
    UPDATE public.listings SET stock = stock + v_res.quantity
        WHERE id = v_res.listing_id;

    INSERT INTO public.activity_events (id, type, text, actor_id, actor_label)
    VALUES ('EVT-' || p_reservation_id || '-cx', 'reservation_cancelled',
        'Reservation ' || p_reservation_id || ' cancelled', v_caller,
        (SELECT email FROM public.profiles WHERE id = v_caller));

    RETURN jsonb_build_object('id', p_reservation_id, 'status', 'cancelled');
END;
$$;

-- 6. Pickup completion: listing's vendor owner (or admin); never restocks.
CREATE OR REPLACE FUNCTION public.complete_pickup(p_reservation_id TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_caller UUID := auth.uid();
    v_res public.reservations%ROWTYPE;
    v_owner UUID;
BEGIN
    IF v_caller IS NULL THEN
        RAISE EXCEPTION 'authentication required';
    END IF;
    SELECT * INTO v_res FROM public.reservations WHERE id = p_reservation_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'reservation not found';
    END IF;
    SELECT owner_id INTO v_owner FROM public.vendors WHERE id = v_res.vendor_id;
    IF v_owner IS DISTINCT FROM v_caller AND NOT public.is_admin() THEN
        RAISE EXCEPTION 'belongs to another vendor';
    END IF;
    IF v_res.status IS DISTINCT FROM 'reserved' THEN
        RAISE EXCEPTION 'only active reservations can be completed';
    END IF;

    UPDATE public.reservations SET status = 'completed' WHERE id = p_reservation_id;

    INSERT INTO public.activity_events (id, type, text, actor_id, actor_label)
    VALUES ('EVT-' || p_reservation_id || '-ok', 'pickup_completed',
        'Pickup ' || p_reservation_id || ' completed', v_caller,
        (SELECT email FROM public.profiles WHERE id = v_caller));

    RETURN jsonb_build_object('id', p_reservation_id, 'status', 'completed');
END;
$$;

-- 7. Vendor approval / suspension (admin only; history + stock untouched).
CREATE OR REPLACE FUNCTION public.set_vendor_status(p_vendor_id TEXT, p_status TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_name TEXT;
BEGIN
    IF NOT public.is_admin() THEN
        RAISE EXCEPTION 'admin required';
    END IF;
    IF p_status NOT IN ('pending', 'approved', 'suspended') THEN
        RAISE EXCEPTION 'invalid vendor status';
    END IF;
    UPDATE public.vendors SET status = p_status WHERE id = p_vendor_id
        RETURNING name INTO v_name;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'vendor not found';
    END IF;

    INSERT INTO public.activity_events (id, type, text, actor_id, actor_label)
    VALUES ('EVT-' || p_vendor_id || '-' || extract(epoch FROM now())::BIGINT,
        'vendor_status', v_name || ' → ' || p_status, auth.uid(),
        (SELECT email FROM public.profiles WHERE id = auth.uid()));

    RETURN jsonb_build_object('id', p_vendor_id, 'status', p_status);
END;
$$;

-- 8. Listing status (vendor owner or admin; activation needs approved vendor).
CREATE OR REPLACE FUNCTION public.set_listing_status(p_listing_id TEXT, p_status TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_caller UUID := auth.uid();
    v_listing public.listings%ROWTYPE;
    v_owner UUID;
    v_vendor_status TEXT;
BEGIN
    IF v_caller IS NULL THEN
        RAISE EXCEPTION 'authentication required';
    END IF;
    IF p_status NOT IN ('active', 'inactive') THEN
        RAISE EXCEPTION 'invalid listing status';
    END IF;
    SELECT * INTO v_listing FROM public.listings WHERE id = p_listing_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'listing not found';
    END IF;
    SELECT owner_id, status INTO v_owner, v_vendor_status
        FROM public.vendors WHERE id = v_listing.vendor_id;
    IF v_owner IS DISTINCT FROM v_caller AND NOT public.is_admin() THEN
        RAISE EXCEPTION 'belongs to another vendor';
    END IF;
    IF p_status = 'active' AND v_vendor_status IS DISTINCT FROM 'approved' THEN
        RAISE EXCEPTION 'approve the vendor before reactivating';
    END IF;

    UPDATE public.listings SET status = p_status WHERE id = p_listing_id;

    INSERT INTO public.activity_events (id, type, text, actor_id, actor_label)
    VALUES ('EVT-' || p_listing_id || '-' || extract(epoch FROM now())::BIGINT,
        'listing_status', v_listing.name || ' → ' || p_status, v_caller,
        (SELECT email FROM public.profiles WHERE id = v_caller));

    RETURN jsonb_build_object('id', p_listing_id, 'status', p_status);
END;
$$;

-- 9. Review moderation (admin only; record preserved).
CREATE OR REPLACE FUNCTION public.moderate_review(p_review_id TEXT, p_status TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF NOT public.is_admin() THEN
        RAISE EXCEPTION 'admin required';
    END IF;
    IF p_status NOT IN ('pending', 'visible', 'hidden') THEN
        RAISE EXCEPTION 'invalid review status';
    END IF;
    UPDATE public.reviews SET status = p_status WHERE id = p_review_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'review not found';
    END IF;

    INSERT INTO public.activity_events (id, type, text, actor_id, actor_label)
    VALUES ('EVT-' || p_review_id || '-' || extract(epoch FROM now())::BIGINT,
        'review', 'Review ' || p_review_id || ' → ' || p_status, auth.uid(),
        (SELECT email FROM public.profiles WHERE id = auth.uid()));

    RETURN jsonb_build_object('id', p_review_id, 'status', p_status);
END;
$$;
