# TECH_STACK.md — Bite For Less Android (confirmed + proposed)

## 1. Confirmed (from repo + toolchain audit, 2026-10-13)

- Language: Java 11 (`source/targetCompatibility = VERSION_11`), layouts in XML.
- Build: Gradle Wrapper 9.5.0, AGP 9.3.1, Foojay toolchain resolver (JVM 21 via Studio JBR 21.0.10).
- SDK: `compileSdk 36 minor 1`, `targetSdk 36`, `minSdk 24`. Installed platforms 33–37.0, build-tools 34.0.0/36.0.0.
- UI: AndroidX AppCompat 1.7.1, Material Components 1.14.0, ConstraintLayout 2.2.2, `Theme.Material3.DayNight.NoActionBar` dark theme, `EdgeToEdge`.
- carrying: `androidx.activity:activity-ktx 1.13.0` present but unused by Java code (harmless; keep or drop in Phase 2 cleanup).
- Tests: JUnit 4.13.2 (unit), ext-junit 1.3.0 + espresso-core 3.7.0 (instrumented).
- IDE: Android Studio AI-261.25134.95 (run/open project here); SDK at `%LOCALAPPDATA%\Android\Sdk`; builds via `.\gradlew.bat`.

## 2. Proposed additions (Phase 2+, only with approval — local-only, maintained, licensed)

| Need | Choice | Why |
|---|---|---|
| Local relational store | Room (`androidx.room:room-runtime` + `room-compiler` annotationProcessor + `room-testing`) | Single source of truth, `@Transaction` for reserve/cancel, survives restart, fits minSdk 24. **Recommended.** Alternative (JSON/file store) only if you reject annotation processing. |
| Lifecycle-aware UI state | `lifecycle-viewmodel` + `livedata` (or `lifecycle-runtime`) | Rotation-safe lists/forms without a backend; matches “use ViewModel if justified”. |
| Lists | `recyclerview` (+ `cardview` via Material) | Deals/reservations/vendor/admin lists with adapters + reusable item layouts. |
| Splash | `core-splashscreen` | Replaces fixed 2 s `Handler` delay with current recommendation. |
| Session/demo flags | `SharedPreferences` (framework, no dep) | Role selection, demo-seed flag only — never as the marketplace DB. |

No Kotlin, Compose, Flutter/RN, Firebase, Supabase, Retrofit/OkHttp, payments, or analytics SDKs in these phases.

## 3. Compatibility & license notes

- All proposed libs are AndroidX/Google, Apache-2.0, actively maintained, and compatible with AGP 9.3.1 + `minSdk 24` + Java 11. Verify exact versions at Phase-2 implementation time against `google()/mavenCentral()` (offline build not assumed).
- Prefer version-catalog (`libs.versions.toml`) entries for anything added; keep Java 11 unless AGP/Room require a bump — do not upgrade SDK/AGP gratuitously.

## 4. Build / test commands (verified shape; results per audit §7)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
# instrumented (needs emulator/device + AVD):
.\gradlew.bat :app:connectedDebugAndroidTest
```

Debug APK (to verify, not assume): `app\build\outputs\apk\debug\app-debug.apk`.
