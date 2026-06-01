package com.vincentwetzel.androidscreensaver.di

import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.repository.DropboxPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GalleryPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.PhotoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    fun providePhotoRepositories(
        galleryPhotoRepository: GalleryPhotoRepository,
        googleDrivePhotoRepository: GoogleDrivePhotoRepository,
        dropboxPhotoRepository: DropboxPhotoRepository
    ): Map<SourceType, @JvmSuppressWildcards PhotoRepository> {
        return mapOf(
            SourceType.GALLERY to galleryPhotoRepository,
            SourceType.GOOGLE_DRIVE to googleDrivePhotoRepository,
            SourceType.DROPBOX to dropboxPhotoRepository
        )
    }
}