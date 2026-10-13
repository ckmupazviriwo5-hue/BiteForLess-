plugins {
    alias(libs.plugins.android.application)
}

import java.util.Properties

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}
fun supaProp(name: String): String =
    (localProps.getProperty(name) ?: System.getenv(name.uppercase().replace('.', '_')) ?: "")

android {
    namespace = "com.example.biteforlessfoodbridge"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.biteforlessfoodbridge"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Hosted Supabase backend (public URL + publishable key only).
        // Set in local.properties (gitignored): supabase.url, supabase.anonKey.
        // Empty values = local demo mode (existing in-memory repository).
        buildConfigField("String", "SUPABASE_URL", "\"${supaProp("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supaProp("supabase.anonKey")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.fragment)
    implementation(libs.splashscreen)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.okhttp)
    implementation(libs.security.crypto)
    implementation(libs.browser)
    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}