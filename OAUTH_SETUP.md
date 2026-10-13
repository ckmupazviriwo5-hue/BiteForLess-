# OAUTH_SETUP.md — Google sign-in for Bite For Less (Phase 6)

Status: application-side flow IMPLEMENTED, provider setup PENDING (needs
human actions below). OAuth is NOT claimed verified until a real device or
emulator completes a full sign-in + return-to-app loop.

## 1. How the app flow works

1. Login → “Continue with Google” builds a PKCE authorize URL
   (`SupabaseAuth.providerUrl("google", verifier)`, S256, random state via
   verifier) and opens it in a Chrome Custom Tab.
2. Supabase redirects to Google, then back to the allowlisted redirect below.
3. `OAuthCallbackActivity` (`biteforless://auth-callback`) extracts `?code=`
   (or surfaces `?error=`), exchanges it via
   `/auth/v1/token?grant_type=pkce` with the stored verifier — no client
   secret in the app.
4. Session saved (EncryptedSharedPreferences), server role fetched from
   `profiles`, app enters with CUSTOMER/VENDOR/ADMIN navigation.
5. More providers later: call `providerUrl("<name>", verifier)` — no redesign.

## 2. Supabase Auth provider settings

In the dashboard project (`ktvlytdmjqkcdeahgdqy`) → Authentication →
Providers → Google → enable, then set:

- Client ID + Client Secret (from §3).
- Redirect (callback) URL registered in Supabase (default hosted):
  `https://ktvlytdmjqkcdeahgdqy.supabase.co/auth/v1/callback`
  (verify under Authentication → URL Configuration; keep Site URL +
  Additional Redirect URLs including `biteforless://auth-callback`).

## 3. Google Cloud OAuth application (human task)

1. https://console.cloud.google.com → new/existing project → enable the
   “Google+ API”/“Google Identity” (People API as prompted).
2. OAuth consent screen → External → app name “Bite For Less”, support email.
3. Credentials → Create → OAuth client ID → **Web application**.
4. Authorized redirect URI: add exactly
   `https://ktvlytdmjqkcdeahgdqy.supabase.co/auth/v1/callback`
5. Copy Client ID + Client Secret into Supabase (§2). No IDs/secrets go into
   the repo, chat, or the APK — Supabase holds them server-side.

## 4. Android package / signing config

- applicationId: `com.example.biteforlessfoodbridge`
- Callback: `biteforless://auth-callback` (`OAuthCallbackActivity`,
  `singleTask`, BROWSABLE). No SHA-1 needed for the custom-scheme flow;
  if Google ever requires asset links, add Digital Asset Links for the
  package + release keystore fingerprint then.
- Debug builds use the debug keystore; release needs its own fingerprint
  recorded wherever the OAuth client is managed.

## 5. Testing OAuth (real device or emulator)

1. Configure `local.properties` (`SUPABASE_SETUP.md`), build debug APK.
2. Complete Google provider setup (§2–§3).
3. Tap “Continue with Google” → browser → choose account → return to app.
4. Expected: lands in MainActivity with CUSTOMER role; session survives
   restart (kill + relaunch → straight to Main).
5. Deny/cancel path: error toast, stays on Login.
6. Record result in `PHASE6_NOTES.md` before claiming verification.
