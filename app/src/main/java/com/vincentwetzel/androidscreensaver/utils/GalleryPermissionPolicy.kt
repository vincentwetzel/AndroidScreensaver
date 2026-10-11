package com.vincentwetzel.androidscreensaver.utils

import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter

/** MediaStore access required to show content selected by the current filter. */
internal object GalleryPermissionPolicy {
    const val READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"

    fun hasAccess(
        filter: MediaTypeFilter,
        imagesGranted: Boolean,
        videosGranted: Boolean,
        selectedMediaGranted: Boolean = false
    ): Boolean =
        when (filter) {
            MediaTypeFilter.IMAGES_ONLY -> imagesGranted || selectedMediaGranted
            MediaTypeFilter.VIDEOS_ONLY -> videosGranted || selectedMediaGranted
            MediaTypeFilter.IMAGES_AND_VIDEOS -> imagesGranted || videosGranted || selectedMediaGranted
        }

    fun requiredPermissions(filter: MediaTypeFilter, includeSelectedMedia: Boolean): List<String> = buildList {
        when (filter) {
            MediaTypeFilter.IMAGES_ONLY -> add("android.permission.READ_MEDIA_IMAGES")
            MediaTypeFilter.VIDEOS_ONLY -> add("android.permission.READ_MEDIA_VIDEO")
            MediaTypeFilter.IMAGES_AND_VIDEOS -> {
                add("android.permission.READ_MEDIA_IMAGES")
                add("android.permission.READ_MEDIA_VIDEO")
            }
        }
        if (includeSelectedMedia) add(READ_MEDIA_VISUAL_USER_SELECTED)
    }
}
