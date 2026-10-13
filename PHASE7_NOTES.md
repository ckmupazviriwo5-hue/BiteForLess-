# PHASE7_NOTES.md — Live auth & marketplace data (inspect + prepare, 2026-10-13)

## 1. Live inspection (read-only)

- Project `ktvlytdmjqkcdeahgdqy` (BiteforLess, eu-west-1, ACTIVE_HEALTHY).
- 1 Auth user + 1 `customer` profile (name empty — signed up without metadata).
  Catalog tables (categories/vendors/listings/reservations/reviews/events) all 0.
- Public `/auth/v1/settings` → 401: provider/confirm config is dashboard-only.
- App `local.properties` has NO `supabase.url`/`supabase.anonKey` → the local
  build currently runs in demo mode. No keys in git.

## 2. Auth audit (code-complete, partially unverified live)

- Works locally (unit-tested): PKCE verifier/challenge/URL/callback parsing,
  mappers, error mapping (9/9 SupabaseRemoteTest); email signup/signin,
  refresh/restore-with-fallback, encrypted session, server-role routing,
  dual sign-out, deep link `biteforless://auth-callback` → OAuthCallbackActivity.
- Gaps found: NO password-reset flow (`/auth/v1/recover` + UI missing);
  signup does not handle the email-confirmation-required case (empty session
  would enter Main signed-out — must fix or keep confirm-email OFF in dev).
- Google OAuth: app-side ready (Custom Tabs + PKCE, no secret); provider NOT
  configured (needs §5 below). Claimed NOT verified.
- Leaked-password protection: dashboard-only toggle (Auth → Password
  Security); could not verify via CLI — check plan eligibility in dashboard.
  No app change needed (server rejection surfaces through existing errors).

## 3. Seed plan (NOT applied — awaiting approval)

- `supabase/seed.sql` (local, edited this run): 6 categories; 5 Lusaka demo
  vendors (v1–v3 approved, v4 pending, v5 suspended; owner_id NULL — see §4);
  12 ZMW listings (ids l1–l12, prices 20–120, active/inactive/sold-out/expired
  mix); 4 sample reviews (2 visible, 2 pending, NULL reviewers).
- Strategy: apply ONCE on the empty catalog via `supabase db execute --file
  supabase/seed.sql` (service context, bypasses RLS; triggers fire with
  NULL auth.uid → owners/reviewers stay NULL as designed). Reruns touch only
  `is_demo` rows (new WHERE guard); stock/status/deadlines are first-write —
  refresh via `demo_cleanup.sql` + reseed. Existing Auth user untouched (no
  profile/vendor/reservation writes reference it).
- Why ownerless vendors are valid: `owner_id` is nullable by design; demo rows
  take no owner-gated actions; customer RPC checks status only; admin manages
  via `set_vendor_status`. No invented user IDs, no fake owners, no fake
  reservations/statistics. Real vendor users onboard later and attach owners.

## 4. Onboarding test plan (accounts needed — none created)

1. Customer signup (app or dashboard) → customer profile auto-created.
2. Vendor application: new vendor user signs up → vendor onboarding (owner
   link) → row lands `pending` (trigger-forced).
3. Admin approval: `admin_bootstrap.sql` (explicit approval required, exact
   email, trusted SQL editor only) → admin approves via app/API.
4. Vendor dashboard access + listing CRUD as owner; customer reserve/cancel.
5. Role routing: CUSTOMER/VENDOR/ADMIN from server profile (existing
   customer account NEVER auto-promoted).

## 5. OAuth requirements (human tasks)

- Supabase: enable Google provider, set Client ID/Secret, allowlist
  `biteforless://auth-callback` (+ Site URL).
- Google Cloud: OAuth client (Web type), authorized redirect =
  `https://ktvlytdmjqkcdeahgdqy.supabase.co/auth/v1/callback`.
- Test on device/emulator: full loop + deny path + restart persistence.

## 6. Integration readiness

- All 6 RPCs wired (create/cancel/complete/vendor-status/listing-status/
  moderate) + reads + owner CRUD + onboarding + review submit; dual-path
  screens with loading/error/retry; demo mode preserved and labelled.
- Pending live verification: real sign-in → browse → reserve → cancel →
  vendor approve flow → pickup → moderation, on a configured build.

## 7. Tests (executed vs pending)

- Executed 2026-10-13: `assembleDebug` + `testDebugUnitTest` — BUILD
  SUCCESSFUL, **66/66 pass, 0 failures** (incl. ZMW currency test; one
  vendor-search test updated for the Lusaka rename — see report). APK
  `app-debug.apk` (8,883,359 bytes). No APK released.
- Pending (need accounts/device): authenticated-as-role RPC tests, OAuth
  loop, pgTAP (no Docker), live end-to-end.
