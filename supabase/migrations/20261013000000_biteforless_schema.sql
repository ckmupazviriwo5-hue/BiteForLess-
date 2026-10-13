-- Bite For Less — initial schema (Phase 6)
-- Currency: ZMW (Zambian kwacha, ISO 4217) for the initial Zambia market.
-- All app tables live in public; RLS is enabled in migration 03.

-- Profiles (1:1 with auth.users; role defaults to customer via trigger).
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    name TEXT NOT NULL DEFAULT '',
    role TEXT NOT NULL DEFAULT 'customer'
        CHECK (role IN ('customer', 'vendor', 'admin')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.vendors (
    id TEXT PRIMARY KEY,
    owner_id UUID REFERENCES public.profiles (id) ON DELETE SET NULL,
    name TEXT NOT NULL CHECK (char_length(name) > 0),
    description TEXT NOT NULL DEFAULT '',
    location TEXT NOT NULL DEFAULT '',
    contact TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'approved', 'suspended')),
    is_demo BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX vendors_owner_idx ON public.vendors (owner_id);
CREATE INDEX vendors_status_idx ON public.vendors (status);

CREATE TABLE public.listings (
    id TEXT PRIMARY KEY,
    vendor_id TEXT NOT NULL REFERENCES public.vendors (id) ON DELETE RESTRICT,
    category_id TEXT NOT NULL REFERENCES public.categories (id) ON DELETE RESTRICT,
    name TEXT NOT NULL CHECK (char_length(name) > 0),
    description TEXT NOT NULL DEFAULT '',
    image_url TEXT,
    currency CHAR(3) NOT NULL DEFAULT 'ZMW' CHECK (currency = 'ZMW'),
    original_price NUMERIC(10, 2) NOT NULL CHECK (original_price > 0),
    discounted_price NUMERIC(10, 2) NOT NULL CHECK (discounted_price >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    status TEXT NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'inactive')),
    deadline TIMESTAMPTZ,
    collection_window TEXT NOT NULL DEFAULT '',
    location TEXT NOT NULL DEFAULT '',
    is_demo BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT listings_discount_below_original
        CHECK (discounted_price < original_price)
);
CREATE INDEX listings_vendor_idx ON public.listings (vendor_id);
CREATE INDEX listings_category_idx ON public.listings (category_id);
CREATE INDEX listings_status_idx ON public.listings (status);
CREATE INDEX listings_deadline_idx ON public.listings (deadline);

CREATE TABLE public.reservations (
    id TEXT PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES public.profiles (id) ON DELETE RESTRICT,
    listing_id TEXT NOT NULL REFERENCES public.listings (id) ON DELETE RESTRICT,
    vendor_id TEXT NOT NULL REFERENCES public.vendors (id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    unit_price NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    total NUMERIC(10, 2) NOT NULL CHECK (total >= 0),
    status TEXT NOT NULL DEFAULT 'reserved'
        CHECK (status IN ('reserved', 'completed', 'cancelled', 'expired')),
    pickup_code TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX reservations_customer_idx ON public.reservations (customer_id);
CREATE INDEX reservations_listing_idx ON public.reservations (listing_id);
CREATE INDEX reservations_vendor_idx ON public.reservations (vendor_id);
CREATE INDEX reservations_status_idx ON public.reservations (status);

CREATE TABLE public.reviews (
    id TEXT PRIMARY KEY,
    listing_id TEXT NOT NULL REFERENCES public.listings (id) ON DELETE CASCADE,
    reviewer_id UUID REFERENCES public.profiles (id) ON DELETE SET NULL,
    reviewer_label TEXT NOT NULL DEFAULT '',
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    body TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'visible', 'hidden')),
    is_demo BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX reviews_listing_idx ON public.reviews (listing_id);
CREATE INDEX reviews_status_idx ON public.reviews (status);

CREATE TABLE public.activity_events (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL DEFAULT 'admin',
    text TEXT NOT NULL DEFAULT '',
    actor_id UUID REFERENCES public.profiles (id) ON DELETE SET NULL,
    actor_label TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX activity_events_created_idx ON public.activity_events (created_at DESC);

-- updated_at maintenance.
CREATE OR REPLACE FUNCTION public.touch_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER profiles_touch BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER vendors_touch BEFORE UPDATE ON public.vendors
    FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER listings_touch BEFORE UPDATE ON public.listings
    FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER reservations_touch BEFORE UPDATE ON public.reservations
    FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
