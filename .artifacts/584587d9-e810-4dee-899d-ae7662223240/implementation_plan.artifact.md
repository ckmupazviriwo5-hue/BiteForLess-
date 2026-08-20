# Implementation Plan - Bite for Less UI Overhaul

Implement the Splash, Login, and Signup screens for the "Bite for Less" app, featuring a custom logo, black/food-color theme, and smooth animations.

## User Review Required

> [!IMPORTANT]
> I will be using **XML and Java** to maintain consistency with the existing project structure (`MainActivity.java`). If you'd prefer a migration to **Jetpack Compose and Kotlin**, please let me know.

## Proposed Changes

### 1. Resources & Assets

#### [NEW] [ic_logo_bite.xml](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/res/drawable/ic_logo_bite.xml)
A custom vector drawable featuring a human figure biting into a burger.

#### [MODIFY] [colors.xml](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/res/values/colors.xml)
Add food-inspired colors (Orange, Red, Yellow, Green) to complement the black background.

#### [MODIFY] [themes.xml](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/res/values/themes.xml)
Update the application theme to a dark, food-vibrant style using Material 3.

### 2. Splash Screen

#### [NEW] [SplashActivity.java](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/java/com/example/biteforlessfoodbridge/SplashActivity.java)
Handles the initial entry, logo animation, and routing to the Auth screen.

#### [NEW] [activity_splash.xml](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/res/layout/activity_splash.xml)
Layout for the splash screen with the logo centered on a black background.

### 3. Auth (Login & Signup) Screens

#### [NEW] [LoginActivity.java](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/java/com/example/biteforlessfoodbridge/LoginActivity.java)
Activity hosting the Login and Signup flows.

#### [NEW] [activity_login.xml](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/res/layout/activity_login.xml)
Layout for the login screen with inputs for credentials and a link to signup.

#### [NEW] [activity_signup.xml](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/res/layout/activity_signup.xml)
Layout for the signup screen with registration fields and validation logic.

### 4. Navigation & Animations

- Implement Activity transitions (fade/slide).
- Add button scale animations on tap.
- Use `EdgeToEdge` for an immersive experience as seen in the existing `MainActivity`.

## Verification Plan

### Manual Verification
- Deploy to an emulator/device.
- Verify Splash screen sequence.
- Test Login and Signup input validation.
- Check gesture smoothness and theme consistency.
