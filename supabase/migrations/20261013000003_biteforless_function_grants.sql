-- Bite For Less — function least-privilege hardening (Phase 6, APPROVED)
-- Applies the approved preflight plan: strict is_admin(), least-privilege
-- EXECUTE grants, self-verifying checks.
--
-- Why: every public SECURITY DEFINER function is currently EXECUTE-granted to
-- anon (13 advisor warnings) and authenticated (13 warnings), including 7
-- internal trigger helpers that must never be direct RPC targets.
-- Worse, is_admin() returns NULL (not false) when the caller has no profile
-- row, so `IF NOT is_admin()` fails OPEN for anonymous callers: on a seeded
-- database, anon set_vendor_status/moderate_review would succeed. This
-- migration fixes the boolean and applies least-privilege grants.
--
-- Precise rollback (manual, privileged session only). Restores the exact
-- prior per-function grants to anon/authenticated WITHOUT reopening the
-- PUBLIC default (functionally identical to the pre-migration state for all
-- API callers; triggers/RLS untouched either way):
--   GRANT EXECUTE ON FUNCTION public.create_reservation(TEXT, INTEGER) TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.cancel_reservation(TEXT) TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.complete_pickup(TEXT) TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.set_vendor_status(TEXT, TEXT) TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.set_listing_status(TEXT, TEXT) TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.moderate_review(TEXT, TEXT) TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.handle_new_user() TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.prevent_role_escalation() TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.force_pending_vendor() TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.force_review_author() TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.prevent_vendor_status_edit() TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.prevent_review_status_edit() TO anon, authenticated;
--   GRANT EXECUTE ON FUNCTION public.touch_updated_at() TO anon, authenticated;
-- (is_admin() already retains anon/authenticated under this migration, so it
-- needs no rollback line.)

-- 1. Strict-boolean is_admin: NULL (no profile row, e.g. anon) means false.
--    Additive redefinition; the applied migration 02 file is NOT modified.
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
    RETURN COALESCE(r = 'admin', false);
END;
$$;

-- 2. Revoke direct-call rights everywhere, then re-grant narrowly.
--    Trigger firing does NOT require EXECUTE, so all triggers keep working.
REVOKE ALL ON FUNCTION public.create_reservation(TEXT, INTEGER) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.cancel_reservation(TEXT) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.complete_pickup(TEXT) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.set_vendor_status(TEXT, TEXT) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.set_listing_status(TEXT, TEXT) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.moderate_review(TEXT, TEXT) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.is_admin() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.handle_new_user() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.prevent_role_escalation() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.force_pending_vendor() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.force_review_author() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.prevent_vendor_status_edit() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.prevent_review_status_edit() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.touch_updated_at() FROM PUBLIC, anon, authenticated;

-- 3. Client API surface: the 6 RPCs stay callable by signed-in users.
--    Each enforces its own auth inside (auth.uid unknown → raise;
--    admin/owner checks via the strict is_admin()).
GRANT EXECUTE ON FUNCTION public.create_reservation(TEXT, INTEGER) TO authenticated;
GRANT EXECUTE ON FUNCTION public.cancel_reservation(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.complete_pickup(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.set_vendor_status(TEXT, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.set_listing_status(TEXT, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.moderate_review(TEXT, TEXT) TO authenticated;

-- 4. is_admin() must stay callable wherever RLS policies evaluate it:
--    anon storefront/vendor/review policies AND authenticated policies.
GRANT EXECUTE ON FUNCTION public.is_admin() TO anon, authenticated;

-- 5. Self-verification: fail the migration if the grants are wrong.
DO $$
BEGIN
    IF has_function_privilege('anon', 'public.create_reservation(text, integer)', 'EXECUTE') THEN
        RAISE EXCEPTION 'anon can still execute create_reservation';
    END IF;
    IF has_function_privilege('anon', 'public.set_vendor_status(text, text)', 'EXECUTE') THEN
        RAISE EXCEPTION 'anon can still execute set_vendor_status';
    END IF;
    IF has_function_privilege('anon', 'public.handle_new_user()', 'EXECUTE') THEN
        RAISE EXCEPTION 'anon can still execute handle_new_user';
    END IF;
    IF has_function_privilege('authenticated', 'public.force_pending_vendor()', 'EXECUTE') THEN
        RAISE EXCEPTION 'authenticated can still execute a trigger helper';
    END IF;
    IF NOT has_function_privilege('authenticated', 'public.create_reservation(text, integer)', 'EXECUTE') THEN
        RAISE EXCEPTION 'authenticated lost create_reservation';
    END IF;
    IF NOT has_function_privilege('anon', 'public.is_admin()', 'EXECUTE') THEN
        RAISE EXCEPTION 'anon lost is_admin (RLS policies need it)';
    END IF;
END;
$$;
