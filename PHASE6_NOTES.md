# PHASE6_NOTES.md — Supabase backend + app integration (Phase 6)

## 1. Architecture

- Hosted Postgres (project `ktvlytdmjqkcdeahgdqy`) is the source of truth when
  the app is configured AND signed in. Otherwise the app runs the existing
  local demo repository (all Phases 1–5 behavior preserved, labelled “demo”).
- Android talks to Supabase with OkHttp (`data/remote/`): PostgREST reads,
  GoTrue auth, secure RPC writes. No Kotlin SDK, no service-role key, no
  secrets in git (`local.properties` → BuildConfig; empty = demo mode).
- UI screens are dual-path: live fetch with loading overlay + Snackbar retry,
  demo path untouched. Live errors surface — never silent demo fallback.
- Currency is ZMW (Zambian kwacha, ISO 4217) for the initial Zambia market:
  DB CHECK enforces it, seed uses it, `PriceFormat` formats `en_ZM`.

## 2. Schema / migrations (`supabase/migrations/`, ordered)

- `..._schema.sql`: profiles (→ auth.users), categories, vendors
  (owner link, pending/approved/suspended, is_demo), listings (vendor/category
  FKs, ZMW CHECK, discounted < original, stock ≥ 0, active/inactive, deadline),
  reservations (customer/listing/vendor FKs, price snapshot, 4 statuses),
  reviews (nullable reviewer for samples, 1–5, 3 states), activity_events.
- `..._logic.sql`: profile auto-create trigger (customer-only, ignores client
  metadata), role-escalation guard, vendor onboarding guard (pending + owner),
  RPC `create_reservation` (row lock, eligibility, server price, atomic +
  event), `cancel_reservation` (owner/admin, restore-once), `complete_pickup`
  (vendor-owner/admin, no restock), `set_vendor_status` / `set_listing_status`
  / `moderate_review` (role-checked, logged). All SECURITY DEFINER, fixed
  search_path.
- `..._rls.sql`: RLS on all tables; anon storefront = eligible listings only;
  no direct reservation writes; review/vendor status edits trigger-guarded;
  activity log append-only from clients and readable only by admins or the
  row's own actor (no cross-user audit reads).
- `supabase/seed.sql` (idempotent upserts: 6 categories, 5 vendors, 12 ZMW
  listings, 4 sample reviews — no fake users/reservations).
- `supabase/demo_cleanup.sql` (manual, is_demo-only, FK order),
  `supabase/admin_bootstrap.sql` (manual, exact-email, fail-safe),
  `supabase/tests/biteforless.test.sql` (pgTAP: 24 assertions).

## 3. App integration (`data/remote/`, Java/XML preserved)

- `SupabaseConfig` (BuildConfig, `isConfigured`, callback URI),
  `SupabaseHttp` (timeouts, main-thread callbacks, friendly errors),
  `SupabaseAuth` (signup/signin/refresh/restore, encrypted session, Google
  PKCE: `providerUrl` + code exchange, multi-provider ready),
  `RemoteRepository` (reads, RPC writes, owner CRUD, tested mappers),
  `RepositoryProvider.isLive`, `LoadingOverlay`, `OAuthCallbackActivity`
  (`biteforless://auth-callback`, manifest-registered).
- Auth screens do real auth when configured (role from server profile);
  Splash restores sessions; sign-out clears both sessions.
- Customer/vendor/admin screens load live data with loading/error/retry and
  perform real operations (reserve, cancel, listing CRUD, pickup, approve/
  suspend, listing status, review moderation). Admin reservation screen stays
  view-only (no lifecycle bypass — documented).

## 4. Verification status (honest)

- `assembleDebug`: SUCCESS. APK verified:
  `app/build/outputs/apk/debug/app-debug.apk` (8,883,355 bytes).
- Unit tests: **66/66 pass, 0 failures** — SupabaseRemoteTest 9/9
  (PKCE/URL/mappers/errors) + new `currency_isZMW` (ISO code, formatting,
  discount consistency) + all prior tests green. pgTAP suite extended to 27
  assertions (ZMW CHECK, activity-log append-only).
- DB tests / dry-run / push / seed / live-API / OAuth: PENDING — need
  `supabase login` (interactive) + Google provider setup. Not claimed.
- No Docker here → `supabase start` / `test db` unavailable; noted.

## 5. Remaining manual steps
1. `supabase login`, `link --project-ref ktvlytdmjqkcdeahgdqy`, `db pull`
   (reconcile), `db push --dry-run`, `db push`, run `seed.sql`.
2. `admin_bootstrap.sql` after first signup. 3. Google provider (§OAUTH).
3. Fill `local.properties`, build, test live flows on device/emulator.

## 6. Security hardening (pending approval, NOT applied)

- Finding: all 14 public functions EXECUTE-granted to anon (+13 advisor
  WARNs); trigger helpers also callable by authenticated (+13 WARNs).
- Critical: `is_admin()` returned NULL (not false) with no profile row, so
  `IF NOT is_admin()` failed OPEN for anon — live-proven: anon
  `set_vendor_status`/`moderate_review` passed the admin check and failed
  only on missing rows (would succeed on seeded data).
- Fix prepared in `supabase/migrations/20261013000003_biteforless_function_grants.sql`
  (additive; applied files untouched): strict-boolean `is_admin()`, revoke
  anon/authenticated/PUBLIC everywhere, re-grant 6 API RPCs to authenticated
  + `is_admin()` to anon/authenticated (RLS needs it), self-verifying DO
  block, rollback = re-grant all to anon/authenticated.
- Accepted residuals: authenticated-definer WARNs on the 6 intentional RPCs;
  12 initplan PERF WARNs (demo scale); trigger-firing post-apply check needs
  a write-capable test user later.

## 7. Migration 04 applied (security hardening)

- `20261013000003_biteforless_function_grants.sql` pushed after dry-run
  showed it as the only pending migration. History is now 4/4.
- `is_admin()` is strict (`COALESCE(r = 'admin', false)` — verified live).
- Grants matrix verified live: 6 API RPCs = authenticated-only; `is_admin()`
  = anon + authenticated (RLS requirement); 7 trigger helpers = no anon or
  authenticated EXECUTE (triggers unaffected — firing needs no grant).
- Anon RPC retest: `create_reservation`/`set_vendor_status` → 401,
  `handle_new_user` → 404. Privilege path closed.
- Advisors after: single anon-definer WARN left (`is_admin`, intentional);
  authenticated WARNs only on the 6 intentional RPCs + `is_admin`;
  12 initplan PERF WARNs accepted at demo scale.
- Precise per-function rollback documented in the migration header (no
  blanket re-grant, PUBLIC default stays closed).
- Tables still empty, no users created — nothing to lose; RLS/policies/
  triggers untouched by this migration.
