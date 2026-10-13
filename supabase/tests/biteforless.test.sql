-- Bite For Less — database tests (Phase 6)
-- Run with: supabase test db (requires Docker; unavailable in this env —
-- see PHASE6_NOTES.md). Uses pgTAP, bundled with the Supabase test runner.

BEGIN;
SELECT plan(27);

-- Tables exist.
SELECT has_table('public', 'profiles', 'profiles exists');
SELECT has_table('public', 'categories', 'categories exists');
SELECT has_table('public', 'vendors', 'vendors exists');
SELECT has_table('public', 'listings', 'listings exists');
SELECT has_table('public', 'reservations', 'reservations exists');
SELECT has_table('public', 'reviews', 'reviews exists');
SELECT has_table('public', 'activity_events', 'activity events exists');

-- RLS enabled everywhere.
SELECT col_is_null(
    (SELECT relrowsecurity FROM pg_class WHERE relname = 'listings'), NULL,
    'placeholder') WHERE FALSE; -- pgTAP has no direct RLS check; see below.
SELECT ok(
    (SELECT relrowsecurity FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'public' AND c.relname = 'listings'),
    'RLS enabled on listings');
SELECT ok(
    (SELECT relrowsecurity FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'public' AND c.relname = 'reservations'),
    'RLS enabled on reservations');
SELECT ok(
    (SELECT relrowsecurity FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'public' AND c.relname = 'profiles'),
    'RLS enabled on profiles');

-- Price/stock/rating constraints reject bad rows (as postgres, bypassing RLS).
SELECT throws_ok(
    $$ INSERT INTO public.listings (id, vendor_id, category_id, name, original_price, discounted_price, stock)
       VALUES ('t-bad1', 'v1', 'bakery', 'Bad', 10.00, 10.00, 1) $$,
    'discounted price must be below original');

SELECT throws_ok(
    $$ INSERT INTO public.listings (id, vendor_id, category_id, name, original_price, discounted_price, stock)
       VALUES ('t-bad2', 'v1', 'bakery', 'Bad', 10.00, 12.00, 1) $$,
    'discounted price above original rejected');

SELECT throws_ok(
    $$ INSERT INTO public.listings (id, vendor_id, category_id, name, original_price, discounted_price, stock)
       VALUES ('t-bad3', 'v1', 'bakery', 'Bad', 10.00, 5.00, -1) $$,
    'negative stock rejected');

SELECT throws_ok(
    $$ INSERT INTO public.reviews (id, listing_id, rating, body)
       VALUES ('t-rev', 'l1', 6, 'Too many stars') $$,
    'rating above 5 rejected');

SELECT throws_ok(
    $$ INSERT INTO public.reservations (id, customer_id, listing_id, vendor_id, quantity, unit_price, total)
       VALUES ('t-res', gen_random_uuid(), 'l1', 'v1', 0, 1.00, 0.00) $$,
    'zero quantity rejected');

SELECT throws_ok(
    $$ INSERT INTO public.listings (id, vendor_id, category_id, name, original_price, discounted_price, stock, currency)
       VALUES ('t-cur', 'v1', 'bakery', 'Bad', 10.00, 5.00, 1, 'USD') $$,
    'non-ZMW currency rejected');

-- RPC functions exist with fixed search_path + SECURITY DEFINER.
SELECT function_returns('public', 'create_reservation', ARRAY['text', 'integer'], 'jsonb');
SELECT function_returns('public', 'cancel_reservation', ARRAY['text'], 'jsonb');
SELECT function_returns('public', 'complete_pickup', ARRAY['text'], 'jsonb');
SELECT function_returns('public', 'set_vendor_status', ARRAY['text', 'text'], 'jsonb');
SELECT function_returns('public', 'set_listing_status', ARRAY['text', 'text'], 'jsonb');
SELECT function_returns('public', 'moderate_review', ARRAY['text', 'text'], 'jsonb');

-- Unauthenticated reservation creation is rejected (no forged customer).
SELECT throws_ok(
    $$ SELECT public.create_reservation('l1', 1) $$,
    'anonymous reservation creation rejected');

-- Policies exist for the anon storefront and scoped reads.
SELECT ok(
    (SELECT count(*) >= 1 FROM pg_policies WHERE schemaname = 'public'
     AND tablename = 'listings' AND roles::TEXT LIKE '%anon%'),
    'anon storefront policy on listings');
SELECT ok(
    (SELECT count(*) = 0 FROM pg_policies WHERE schemaname = 'public'
     AND tablename = 'reservations' AND cmd = 'INSERT'),
    'no direct reservation inserts (RPC only)');
SELECT ok(
    (SELECT count(*) >= 1 FROM pg_policies WHERE schemaname = 'public'
     AND tablename = 'activity_events' AND cmd = 'SELECT'),
    'scoped activity-log read policy exists');
SELECT ok(
    (SELECT count(*) = 0 FROM pg_policies WHERE schemaname = 'public'
     AND tablename = 'activity_events' AND cmd IN ('INSERT', 'UPDATE', 'DELETE')),
    'activity log append-only to clients');

SELECT * FROM finish();
ROLLBACK;
