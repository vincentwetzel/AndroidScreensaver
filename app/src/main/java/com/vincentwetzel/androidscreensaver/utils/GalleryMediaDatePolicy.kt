package com.vincentwetzel.androidscreensaver.utils

/** Resolves the Gallery capture timestamp, falling back to when MediaStore first indexed the item. */
internal object GalleryMediaDatePolicy {
    fun dateTakenMillis(dateTakenMillis: Long, dateAddedSeconds: Long): Long =
        dateTakenMillis.takeIf { it > 0L } ?: dateAddedSeconds * 1000L
}