plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.dagger.hilt.android'
    id 'kotlin-kapt'
}

android {
    namespace 'com.vincentwetzel.androidscreensaver'
    compileSdk 34

    defaultConfig {
        applicationId "com.vincentwetzel.androidscreensaver"
        minSdk 26
        targetSdk 34
        
        // Version Management
        // Update policy:
        // - New feature or feature completed → increment versionName minor (1.0.0 → 1.1.0)
        // - Bug fix or minor change → increment versionName patch (1.0.0 → 1.0.1)
        // - Major release → increment versionName major (1.0.0 → 2.0.0)
        // - Every update → increment versionCode by 1
        versionName "1.8.0"
        versionCode 9

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        
        // For TV devices
        manifestPlaceholders = [leanbackRequired: false]
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            applicationIdSuffix ".debug"
            debuggable true
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = '17'
    }
    
    buildFeatures {
        viewBinding true
    }
}

dependencies {
    // AndroidX Core
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.fragment:fragment-ktx:1.6.2'
    implementation 'androidx.activity:activity-ktx:1.8.2'
    
    // Preferences - DataStore
    implementation 'androidx.datastore:datastore-preferences:1.0.0'
    implementation 'androidx.preference:preference-ktx:1.2.1'
    
    // Navigation
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'
    
    // Lifecycle
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-service:2.7.0'
    
    // DreamService (included in android.service.dreams, no additional dependency needed)
    
    // Dependency Injection - Hilt
    implementation 'com.google.dagger:hilt-android:2.48'
    kapt 'com.google.dagger:hilt-compiler:2.48'
    
    // Image Loading - Coil
    implementation 'io.coil-kt:coil:2.5.0'
    implementation 'io.coil-kt:coil-video:2.5.0'  // Video thumbnail support
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3'
    
    // Video Playback - ExoPlayer (Media3)
    implementation 'androidx.media3:media3-exoplayer:1.2.0'
    implementation 'androidx.media3:media3-ui:1.2.0'
    
    // WorkManager (Background Tasks)
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
    
    // Security (EncryptedSharedPreferences)
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
    
    // Location Services (Weather)
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    
    // Google Drive API (v1.0 primary source)
    implementation 'com.google.android.gms:play-services-auth:20.7.0'
    implementation 'com.google.api-client:google-api-client-android:2.2.0'
    implementation 'com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0'
    implementation 'com.google.http-client:google-http-client-gson:1.43.3'

    // OkHttp (for weather API)
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // Future Cloud Storage SDKs (disabled for v1.0)
    // implementation 'com.dropbox.core:dropbox-core-sdk:7.0.0'
    // implementation 'com.microsoft.graph:microsoft-graph:5.80.0'
    // implementation 'com.jcifs:jcifs-ng:2.1.9'
    // implementation 'com.github.thegrizzlylabs:sardine-android:0.8'
    
    // Room (for caching photos metadata)
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}

// Allow references to generated code
kapt {
    correctErrorTypes true
}
