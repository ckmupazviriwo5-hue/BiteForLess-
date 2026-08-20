# Walkthrough - Bite for Less UI Implementation

I have implemented the Splash, Login, and Signup screens for the **Bite for Less** app. The UI follows the requested theme: a deep black background accented by vibrant food colors.

## Key Features

### 1. Visual Identity
- **Custom Logo**: Created [ic_logo_bite.xml](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/res/drawable/ic_logo_bite.xml) featuring a human figure profile biting a stylized burger.
- **Color Palette**: Defined in [colors.xml](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/res/values/colors.xml) with `food_orange`, `food_red`, and `food_yellow`.

### 2. User Experience
- **Splash Screen**: [SplashActivity](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/java/com/example/biteforlessfoodbridge/SplashActivity.java) features a smooth scale-in animation for the logo.
- **Login & Signup**: Both screens use Material 3 `TextInputLayout` for modern input fields.
- **Gesture Animations**: Interactive elements like buttons and text links have a scale-down effect on touch to provide tactile feedback.

### 3. Navigation Flow
1. **Splash Screen** (2 seconds) -> **Login Screen**
2. **Login Screen** -> **Signup Screen** (and back)
3. **Login Screen** -> **MainActivity** (Dashboard)

## Verification Results

- **Build Status**: `app:assembleDebug` completed successfully.
- **Theme Consistency**: Applied dark theme across all activities via `themes.xml`.
- **Validation**: [SignupActivity](file:///C:/Users/Rog Strix/AndroidStudioProjects/BiteForLessFoodBridge/app/src/main/java/com/example/biteforlessfoodbridge/SignupActivity.java) includes logic to check for empty fields and matching passwords.

> [!TIP]
> You can now run the app to see the transitions and animations in action. The app starts with the Splash screen as the main entry point.
