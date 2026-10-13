-- Bite For Less — Row Level Security (Phase 6)
-- Server-side functions (migration 02) bypass RLS as SECURITY DEFINER;
-- these policies govern direct Data API access.

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vendors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reservations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activity_events ENABLE ROW LEVEL SECURITY;

-- ---------- profiles ----------
-- Read own profile; admins read all.
CREATE POLICY profiles_select_own ON public.profiles
    FOR SELECT TO authenticated
    USING (id = auth.uid() OR public.is_admin());
-- No client INSERT (trigger creates the row).
-- Update own name/email; the trigger blocks non-admin role changes.
CREATE POLICY profiles_update_own ON public.profiles
    FOR UPDATE TO authenticated
    USING (id = auth.uid())
    WITH CHECK (id = auth.uid());

-- ---------- categories ----------
-- Public catalog data.
CREATE POLICY categories_select_all ON public.categories
    FOR SELECT TO anon, authenticated USING (true);

-- ---------- vendors ----------
-- Public sees approved vendors; owners see their own; admins all.
CREATE POLICY vendors_select_public ON public.vendors
    FOR SELECT TO anon, authenticated
    USING (status = 'approved' OR owner_id = auth.uid() OR public.is_admin());
-- Onboarding: authenticated users may insert their own pending vendor
-- (trigger forces pending + owner).
CREATE POLICY vendors_insert_own ON public.vendors
    FOR INSERT TO authenticated
    WITH CHECK (owner_id = auth.uid());
-- Owners may edit descriptive fields of their own vendor (status changes
-- still go through set_vendor_status; the trigger below blocks direct edits).
CREATE POLICY vendors_update_own ON public.vendors
    FOR UPDATE TO authenticated
    USING (owner_id = auth.uid() OR public.is_admin())
    WITH CHECK (owner_id = auth.uid() OR public.is_admin());

CREATE OR REPLACE FUNCTION public.prevent_vendor_status_edit()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM OLD.status AND NOT public.is_admin() THEN
        RAISE EXCEPTION 'vendor status changes require an administrator';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS vendors_status_guard ON public.vendors;
CREATE TRIGGER vendors_status_guard
    BEFORE UPDATE ON public.vendors
    FOR EACH ROW EXECUTE FUNCTION public.prevent_vendor_status_edit();

-- ---------- listings ----------
-- Anonymous storefront: eligible listings from approved vendors only.
CREATE POLICY listings_select_public ON public.listings
    FOR SELECT TO anon
    USING (
        status = 'active' AND stock > 0
        AND (deadline IS NULL OR deadline > now())
        AND EXISTS (SELECT 1 FROM public.vendors v
                    WHERE v.id = listings.vendor_id AND v.status = 'approved')
    );
-- Signed-in users may read the catalog (needed for history/details).
CREATE POLICY listings_select_auth ON public.listings
    FOR SELECT TO authenticated USING (true);
-- Vendor owners manage their own listings; admins manage all.
CREATE POLICY listings_insert_owner ON public.listings
    FOR INSERT TO authenticated
    WITH CHECK (EXISTS (SELECT 1 FROM public.vendors v
        WHERE v.id = listings.vendor_id
        AND (v.owner_id = auth.uid() OR public.is_admin())));
CREATE POLICY listings_update_owner ON public.listings
    FOR UPDATE TO authenticated
    USING (EXISTS (SELECT 1 FROM public.vendors v
        WHERE v.id = listings.vendor_id
        AND (v.owner_id = auth.uid() OR public.is_admin())))
    WITH CHECK (EXISTS (SELECT 1 FROM public.vendors v
        WHERE v.id = listings.vendor_id
        AND (v.owner_id = auth.uid() OR public.is_admin())));
CREATE POLICY listings_delete_admin ON public.listings
    FOR DELETE TO authenticated USING (public.is_admin());

-- ---------- reservations ----------
-- Read: owner, vendor owner, admin.
CREATE POLICY reservations_select_scoped ON public.reservations
    FOR SELECT TO authenticated
    USING (
        customer_id = auth.uid()
        OR public.is_admin()
        OR EXISTS (SELECT 1 FROM public.vendors v
            WHERE v.id = reservations.vendor_id AND v.owner_id = auth.uid())
    );
-- No direct INSERT/UPDATE/DELETE: create_reservation / cancel_reservation /
-- complete_pickup are the only write paths (SECURITY DEFINER functions).

-- ---------- reviews ----------
-- Everyone sees visible reviews; authors see their own; admins all.
CREATE POLICY reviews_select_scoped ON public.reviews
    FOR SELECT TO anon, authenticated
    USING (status = 'visible' OR reviewer_id = auth.uid() OR public.is_admin());
-- Authenticated customers may post as themselves (label enforced by trigger
-- default below is client-supplied demo label; reviewer_id is forced).
CREATE POLICY reviews_insert_own ON public.reviews
    FOR INSERT TO authenticated
    WITH CHECK (reviewer_id = auth.uid());
-- Authors may edit body/rating of their own review, but never the status
-- (trigger below blocks direct status edits).
CREATE POLICY reviews_update_own ON public.reviews
    FOR UPDATE TO authenticated
    USING (reviewer_id = auth.uid() OR public.is_admin())
    WITH CHECK (reviewer_id = auth.uid() OR public.is_admin());

CREATE OR REPLACE FUNCTION public.prevent_review_status_edit()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM OLD.status AND NOT public.is_admin() THEN
        RAISE EXCEPTION 'review moderation requires an administrator';
    END IF;
    IF NEW.reviewer_id IS DISTINCT FROM OLD.reviewer_id AND NOT public.is_admin() THEN
        RAISE EXCEPTION 'reviewer cannot be reassigned';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS reviews_status_guard ON public.reviews;
CREATE TRIGGER reviews_status_guard
    BEFORE UPDATE ON public.reviews
    FOR EACH ROW EXECUTE FUNCTION public.prevent_review_status_edit();

-- Force reviewer_id to the caller on insert (no impersonation).
CREATE OR REPLACE FUNCTION public.force_review_author()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    NEW.reviewer_id := auth.uid();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS reviews_author_guard ON public.reviews;
CREATE TRIGGER reviews_author_guard
    BEFORE INSERT ON public.reviews
    FOR EACH ROW EXECUTE FUNCTION public.force_review_author();

-- ---------- activity_events ----------
-- Admins read everything; others see only events they triggered (their own
-- reservations, moderation-free). No customer/vendor can read unrelated
-- users' activity, and nobody fabricates audit events from the client.
CREATE POLICY activity_select_scoped ON public.activity_events
    FOR SELECT TO authenticated
    USING (actor_id = auth.uid() OR public.is_admin());
-- No INSERT/UPDATE/DELETE policies: only server functions write events.
