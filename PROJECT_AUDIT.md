# PROJECT_AUDIT.md — Bite For Less Android (Phase 1)

Date: 2026-10-13
Repo: https://github.com/ckmupazviriwo5-hue/BiteForLess-
Local: `C:\Users\Rog Strix\Desktop\BITEFORLESS`
Branch: `main` @ `96edcd9 initial commit`
Tree status at audit: clean, no uncommitted changes.

## 1. Verified repository state (confirmed)

- Single-commit repo, one branch (`main`). Remote `origin` matches the master-prompt URL.
- No `AGENTS.md`, `PROJECT_CONTEXT.md`, or `README.md` in repo. Recorded as missing, not assumed.
- `.artifacts/…` (task / implementation_plan / walkthrough) is **tracked in git** and describes the prior Splash/Login/Signup UI pass. It is history, not a spec for marketplace work.
- `.gitignore` + `app/.gitignore` + `.idea/.gitignore` cover standard Android ignores (`/build`, `.gradle`, `local.properties`, etc.). No secrets found in tree.
- No `local.properties` present (expected — gitignored; SDK auto-resolves from `%LOCALAPPDATA%\Android\Sdk`).

## 2. Verified toolchain (confirmed)

| Item | Finding |
|---|---|
| OS / shell | Windows 11, PowerShell |
| Git | 2.45.1.windows.1, `ls-remote` OK |
| Android Studio | AI-261.25134.95.2612.15914620 present under `C:\Program Files\Android\Android Studio` |
| JDK | No system `java` on PATH. Studio JBR `jbr\bin\java.exe` = OpenJDK 21.0.10. Gradle wrapper runs with `JAVA_HOME=<studio>\jbr`. `gradle-daemon-jvm.properties` requests toolchain 21 (Foojay resolver). |
| Gradle | Wrapper 9.5.0 (`gradle-wrapper.jar` + `properties` present, sha256 pinned). No system `gradle` on PATH — wrapper is the correct build entry. |
| AGP / SDK | `libs.versions.toml`: AGP 9.3.1, appcompat 1.7.1, material 1.14.0, activity-ktx 1.13.0, constraintlayout 2.2.2, junit 4.13.2, ext-junit 1.3.0, espresso-core 3.7.0. `app/build.gradle.kts`: `compileSdk release(36){minorApiLevel=1}` (= 36.1), `targetSdk 36`, `minSdk 24`, Java 11, release optimization disabled. |
| SDK installed | platforms `android-33 … android-37.0` (+ext), build-tools `34.0.0`, `36.0.0`, `platform-tools` (adb 1.0.41 / 37.0.0), `emulator`, system-images for `android-36` (aosp_atd arm64/x86_64, default x86_64). No AVDs under `~\.android\avd` at audit time. `cmdline-tools/latest/bin/sdkmanager.bat` **not found** — CLI SDK management path missing (Studio SDK Manager still available). |
| Build verification | `:app:assembleDebug` launched via wrapper with JBR Java 21 during this audit. Result to be recorded in §7 once finished — **no success claimed yet**. |

## 3. Verified app structure (confirmed — all files read)

- Package: `com.example.biteforlessfoodbridge` (namespace = applicationId). Flat, no subpackages.
- Manifest: `SplashActivity` (LAUNCHER) → `LoginActivity`, `SignupActivity`, `MainActivity`. No permissions, providers, deep links, or `MainActivity` intent filters. `windowSoftInputMode=adjustResize` on Main only.
- `SplashActivity.java:16-34` — sets `activity_splash`, scale anim (0.5→1.0, 1000 ms), `new Handler().postDelayed(..., 2000)` → Login → finish. Issues: `Handler()` no-arg constructor deprecated; fixed 2 s delay; no session/role routing.
- `LoginActivity.java:37-41` — **fake auth**: any tap on Login → `MainActivity` + finish. `et_email`/`et_password` exist in layout but are never read or validated. Touch-scale animation only.
- `SignupActivity.java:43-64` — reads 4 fields; validates non-empty + password==confirm via `Toast`; on success → Login. No email-format check, no length/strength rules, no persistence, no duplicate-account handling (no accounts exist).
- `MainActivity.java:11-24` — placeholder: `EdgeToEdge` + insets padding + single `TextView "Dashboard - Bite for Less"`. No navigation, no list, no actions.
- Layouts: `activity_splash` (logo + name + tagline), `activity_login` (logo, 2 outlined inputs, login button, signup link), `activity_signup` (4 outlined inputs, signup button, login link), `activity_main` (centered TextView). All `ConstraintLayout`, black background.
- Resources: `colors.xml` (food_orange/red/green/yellow/brown + primary/secondary/accent), `strings.xml` (14 strings, auth + tagline + dashboard), `themes.xml` = `Theme.Material3.DayNight.NoActionBar` with black window/status/nav. `values-night/themes.xml` is effectively identical (no real night differentiation). `ic_logo_bite.xml` vector (burger + profile). No `dimens.xml`, no reusable styles, no menus, no nav graph, no item layouts, no dialogs.
- Tests: only Studio defaults — `ExampleUnitTest.addition_isCorrect` and `ExampleInstrumentedTest.useAppContext`. Zero Bite For Less coverage.

## 4. Gap analysis vs master-prompt §6 (confirmed gaps)

| Requirement | State |
|---|---|
| Splash/branding/nav | Splash exists but delay-based, no AndroidX SplashScreen API, no role-aware routing; no primary navigation at all |
| Customer marketplace (browse/search/filter/sort/details/reserve/history/cancel/profile) | None — no list, no adapter, no detail, no model |
| Listing details (prices, savings, qty, vendor, windows, expiry) | None |
| Reservation lifecycle (Available→Reserved→Completed/Cancelled/Expired, qty restore-once, restart persistence) | None |
| Vendor dashboard | None |
| Admin dashboard | None |
| Reviews / notifications | None |
| Local data + single source of truth + reset demo data | None (no models, repos, Room/DAO, fixtures, SharedPreferences usage) |
| Validation | Login: none. Signup: empty + match only. No stock/price/deadline rules anywhere |
| Rotation / recreation | No state handling beyond default; signup/login inputs use plain views (recreation OK by default), but nothing lifecycle-aware exists |
| Empty/loading/error/success states | None beyond `Toast` on signup |

## 5. Assumptions (explicitly NOT confirmed)

1. Assignment requires Java + XML (master prompt §4). No assignment PDF/brief was supplied in this session — Java/XML retention is assumed from your instruction, not from a course doc.
2. No design reference/Figma supplied — current black+orange theme treated as starting point, not approved final design.
3. Package `com.example.*` assumed placeholder to keep for now; rename is possible but is an irreversible-touch decision — deferred to your call.
4. Room assumed acceptable for persistence (minSdk 24 + AGP 9.3.1 support it), but not yet adopted — needs your approval before adding dependencies.

## 6. Recommendations (need your review before Phase 2+)

1. Keep Java/XML, AndroidX + Material Components. Do **not** migrate to Kotlin/Compose/Flutter.
2. Prefer compatible-dependency additions only: Room (+ annotationProcessor), Lifecycle ViewModel/LiveData, RecyclerView (via appcompat chain), SplashScreen compat, Espresso/JUnit for new tests. No Firebase/network/payments.
3. Adopt packages `model / repository / data / ui / util` inside existing `com.example.biteforlessfoodbridge`, adapting rather than renaming the applicationId yet.
4. Single local data source per entity + documented `Available → Reserved → Completed/Cancelled/Expired` rules with restore-once guards; `SharedPreferences` only for session/demo flags; visible "Reset demo data" path.
5. Navigation: single-`MainActivity` + Fragments + BottomNavigation (Customer: Home/Deals/Reservations/Profile; role entry points for Vendor/Admin demo) — proposed, **not started** pending your approval.
6. Remove/untrack noise? `.artifacts/` is committed history — leave untouched unless you ask.
7. Create AVD (android-36 x86_64 image available) for install/run verification in Phase 7.

## 7. Build & test commands (verified 2026-10-13)

- `./gradlew.bat --version` — OK (Gradle 9.5.0, JVM 21.0.10).
- `./gradlew.bat :app:assembleDebug` — **BUILD SUCCESSFUL** (2m 30s, 33 tasks). Only note: `SplashActivity.java uses or overrides a deprecated API` (`Handler()` no-arg — slated for Phase 2 SplashScreen migration).
- Verified APK: `app\build\outputs\apk\debug\app-debug.apk` (6,933,759 bytes, built 13/10/2026 16:12). `output-metadata.json` alongside. No guessing — path confirmed on disk.
- `./gradlew.bat :app:testDebugUnitTest` — **BUILD SUCCESSFUL** (1 test, `ExampleUnitTest.addition_isCorrect`, pass). No Bite For Less logic covered yet — real tests land Phase 3+.
- Instrumented (`connectedDebugAndroidTest`) — **not run**: no AVD/device configured at audit time.

## 8. Risks / missing inputs

- No assignment brief, no design reference, no language-requirement doc provided in-session — proceeding on master-prompt authority only.
- No AVD configured; emulator/device run not yet verified.
- `Handler()` deprecation + hardcoded splash delay should be replaced in Phase 2 (AndroidX SplashScreen).
- Login is fake auth with zero validation — must be relabelled/rebuilt as simulated role selection, never “secure”.
