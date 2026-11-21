plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing is supplied out-of-band, never committed. Provide it either as
// environment variables or as Gradle properties (e.g. in ~/.gradle/gradle.properties):
//   EDUFOSS_STORE_FILE / edufoss.storeFile        path to the keystore
//   EDUFOSS_STORE_PASSWORD / edufoss.storePassword
//   EDUFOSS_KEY_ALIAS / edufoss.keyAlias
//   EDUFOSS_KEY_PASSWORD / edufoss.keyPassword
// With no keystore configured, minified ("release"/"wearRelease") builds still
// assemble and are simply left unsigned, which is what reproducible builds want.
val ksFile     = System.getenv("EDUFOSS_STORE_FILE")     ?: (findProperty("edufoss.storeFile")     as String?)
val ksPassword = System.getenv("EDUFOSS_STORE_PASSWORD") ?: (findProperty("edufoss.storePassword") as String?)
val ksAlias    = System.getenv("EDUFOSS_KEY_ALIAS")      ?: (findProperty("edufoss.keyAlias")      as String?)
val ksKeyPass  = System.getenv("EDUFOSS_KEY_PASSWORD")   ?: (findProperty("edufoss.keyPassword")   as String?)

android {
    namespace   = "org.edufosspage"
    compileSdk  = 36

    defaultConfig {
        applicationId = "org.edufosspage"
        minSdk        = 23
        targetSdk     = 36
        versionCode   = 10
        versionName   = "1.0"
    }

    signingConfigs {
        if (ksFile != null && ksPassword != null && ksAlias != null && ksKeyPass != null) {
            create("edufoss") {
                storeFile     = file(ksFile)
                storePassword = ksPassword
                keyAlias      = ksAlias
                keyPassword   = ksKeyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                          "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("edufoss")
        }

        // Watch build. Same module and the same data/UI code as the phone app;
        // only the launcher activity, the Wear Compose dependency set and the
        // app identity differ. It installs alongside the phone app as ".wear".
        create("wear") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".wear"
            versionNameSuffix   = "-wear"
            resValue("string", "app_name", "EduFOSSpage Wear")

            // The target watch is 32-bit (armeabi-v7a); ship only that ABI.
            ndk { abiFilters += "armeabi-v7a" }
        }

        // Publishable, minified watch build. Unsigned unless a keystore is
        // configured above.
        create("wearRelease") {
            initWith(getByName("wear"))
            isDebuggable      = false
            isMinifyEnabled   = true
            isShrinkResources = true
            ndk { abiFilters += "armeabi-v7a" }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                          "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("edufoss")
        }
    }

    // The watch UI lives in src/wear and must be compiled into every watch
    // variant. AGP gives each build type its own source set, so wearRelease
    // has to be pointed at the same directory as wear.
    sourceSets {
        getByName("wearRelease") {
            java.srcDir("src/wear/java")
            res.srcDirs("src/wear/res")
            manifest.srcFile("src/wear/AndroidManifest.xml")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// Wear Compose needs API 26+, and the target watch runs API 34. Raise minSdk on
// the watch variants only, leaving the phone app at its own minSdk 23.
androidComponents {
    beforeVariants(selector().withBuildType("wear"))        { it.minSdk = 30 }
    beforeVariants(selector().withBuildType("wearRelease")) { it.minSdk = 30 }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    val lifecycle = "2.9.0"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycle")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.woheller69:FreeDroidWarn:V1.3")

    // Wear-only runtime, scoped to the watch build types so the phone APK is
    // byte-for-byte unaffected. 1.6.x targets compileSdk 35 / AGP 8.6+, which
    // keeps us on the toolchain the phone app already uses.
    val wearCompose = "1.6.2"
    listOf("wearImplementation", "wearReleaseImplementation").forEach { configuration ->
        add(configuration, "androidx.wear.compose:compose-material:$wearCompose")
        add(configuration, "androidx.wear.compose:compose-foundation:$wearCompose")
    }
}
