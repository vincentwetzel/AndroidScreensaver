# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to the flags
# loaded from the SDK directory in ${sdk.dir}/tools/proguard/proguard-android.txt

# Keep DreamService
-keep class * extends android.service.dreams.DreamService {
    *;
}

# Dropbox SDK
-keep class com.dropbox.core.** { *; }

# Google APIs
-keep class com.google.api.services.** { *; }

# Microsoft Graph
-keep class com.microsoft.graph.** { *; }

# jcifs-ng (SMB)
-keep class jcifs.** { *; }

# Sardine (WebDAV)
-keep class com.thegrizzlylabs.sardine.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
