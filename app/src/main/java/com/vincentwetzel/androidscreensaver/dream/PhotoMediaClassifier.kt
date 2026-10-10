package com.vincentwetzel.androidscreensaver.dream

import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoOrder

/** Pure media classification and ordering rules used by the slideshow. */
internal object PhotoMediaClassifier {

    private val imageExtensions = listOf(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif", ".svg", ".tiff", ".tif"
    )
    private val videoExtensions = listOf(
        ".mp4", ".avi", ".mov", ".mkv", ".webm", ".wmv", ".flv", ".m4v"
    )

    fun isImage(photo: Photo): Boolean {
        val nameToCheck = photo.title ?: photo.uri
        return imageExtensions.any { nameToCheck.endsWith(it, ignoreCase = true) } ||
            photo.uri.contains("/images/media/", ignoreCase = true) ||
            photo.uri.contains("media_type_image", ignoreCase = true)
    }

    fun isVideo(photo: Photo): Boolean {
        val nameToCheck = photo.title ?: photo.uri
        return videoExtensions.any { nameToCheck.endsWith(it, ignoreCase = true) } ||
            photo.uri.contains("/video/media/", ignoreCase = true) ||
            photo.uri.contains("media_type_video", ignoreCase = true)
    }

    fun sortKey(photo: Photo, order: PhotoOrder): Comparable<*> = when (order) {
        PhotoOrder.DATE_NEWEST_FIRST -> -(photo.dateTaken ?: 0L)
        PhotoOrder.DATE_OLDEST_FIRST -> photo.dateTaken ?: 0L
        PhotoOrder.NAME_A_Z -> photo.title ?: ""
        PhotoOrder.NAME_Z_A -> ReverseString(photo.title ?: "")
        PhotoOrder.SIZE_LARGEST_FIRST -> -(photo.fileSize ?: 0L)
        PhotoOrder.SIZE_SMALLEST_FIRST -> photo.fileSize ?: 0L
        else -> 0
    }

    private class ReverseString(private val value: String) : Comparable<ReverseString> {
        override fun compareTo(other: ReverseString): Int = other.value.compareTo(value)
    }
}
