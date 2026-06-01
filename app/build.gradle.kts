import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    namespace = "com.vincentwetzel.androidscreensaver"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vincentwetzel.androidscreensaver"
        minSdk = 26
        targetSdk = 34

        // Version Management
        // Update policy:
        // - New feature or feature completed → increment versionName minor (1.0.0 → 1.1.0)
        // - Bug fix or minor change → increment versionName patch (1.0.0 → 1.0.1)
        // - Major release → increment versionName major (1.0.0 → 2.0.0)
        // - Every update → increment versionCode by 1
        versionName = "1.11.0"
        versionCode = 10

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // For TV devices
        manifestPlaceholders["leanbackRequired"] = false
        
        // Read and aggressively sanitize the app key to prevent quote/whitespace errors
        val dropboxAppKey = localProperties.getProperty("DROPBOX_APP_KEY", "default_key").replace("\"", "").replace("'", "").trim()
        manifestPlaceholders["DROPBOX_APP_KEY"] = dropboxAppKey
        buildConfigField("String", "DROPBOX_APP_KEY", "\"$dropboxAppKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("Boolean", "DEBUG_LOGCAT_MIRROR", "true")
        }
        release {
            buildConfigField("Boolean", "DEBUG_LOGCAT_MIRROR", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.activity:activity-ktx:1.13.0")
    
    // Preferences - DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.7")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")
    
    // DreamService (included in android.service.dreams, no additional dependency needed)
    
    // Dependency Injection - Hilt
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-android-compiler:2.59.2")
    
    // Image Loading - Coil
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")  // Video thumbnail support
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    
    // Video Playback - ExoPlayer (Media3)
    implementation("androidx.media3:media3-exoplayer:1.10.0")
    implementation("androidx.media3:media3-ui:1.10.0")
    
    // WorkManager (Background Tasks)
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    
    // Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0")
    
    // Location Services (Weather)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    // Google Drive API (v1.0 primary source)
    implementation("com.google.android.gms:play-services-auth:21.5.1")
    implementation("com.google.api-client:google-api-client-android:2.9.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:2.1.0")

    // OkHttp (for weather API)
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    
    // Future Cloud Storage SDKs (disabled for v1.0)
    implementation("com.dropbox.core:dropbox-core-sdk:5.4.6")
    // implementation("com.microsoft.graph:microsoft-graph:5.80.0")
    // implementation("com.jcifs:jcifs-ng:2.1.9")
    // implementation("com.github.thegrizzlylabs:sardine-android:0.8")
    
    // Room (for caching photos metadata)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

hilt {
    enableAggregatingTask = true
}
