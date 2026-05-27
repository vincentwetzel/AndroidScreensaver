// Root build.gradle.kts
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.google.devtools.ksp") version "2.3.2" apply false
    id("com.google.dagger.hilt.android") version "2.59" apply false // MUST be 2.59+
}