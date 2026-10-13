-- Bite For Less — demo-data cleanup (MANUAL ONLY, Phase 6)
-- Removes ONLY rows explicitly flagged is_demo = true, in FK-safe order.
-- Never deletes real user-generated records. Review before running.
-- Run via: supabase db execute --file supabase/demo_cleanup.sql
--          (or paste into the SQL editor after review).

BEGIN;

-- Reservations referencing demo listings (real reservations belong to auth
-- users; only remove those tied to demo listings).
DELETE FROM public.reservations r
USING public.listings l
WHERE r.listing_id = l.id AND l.is_demo = true;

-- Demo reviews.
DELETE FROM public.reviews WHERE is_demo = true;

-- Demo listings.
DELETE FROM public.listings WHERE is_demo = true;

-- Demo vendors (ownerless demo records only).
DELETE FROM public.vendors WHERE is_demo = true;

-- Demo categories only if no real listings reference them.
DELETE FROM public.categories c
WHERE c.id IN ('bakery', 'produce', 'dairy', 'meals', 'beverages', 'snacks')
  AND NOT EXISTS (SELECT 1 FROM public.listings l WHERE l.category_id = c.id);

COMMIT;
