package com.vincentwetzel.androidscreensaver.di

import android.content.Context
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.repository.DropboxPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.DropboxRepository
import com.vincentwetzel.androidscreensaver.data.repository.GalleryPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.data.repository.PhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.WeatherRepository
import com.vincentwetzel.androidscreensaver.dream.SlideshowManager
import com.vincentwetzel.androidscreensaver.utils.DropboxAccountManager
import com.vincentwetzel.androidscreensaver.utils.GoogleAccountManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module to provide repository instances
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGoogleAccountManager(
        @ApplicationContext context: Context
    ): GoogleAccountManager {
        return GoogleAccountManager(context)
    }

    @Provides
    @Singleton
    fun provideGoogleDriveRepository(
        @ApplicationContext context: Context,
        accountManager: GoogleAccountManager
    ): GoogleDriveRepository {
        return GoogleDriveRepository(context, accountManager)
    }

    @Provides
    @Singleton
    fun provideDropboxAccountManager(
        @ApplicationContext context: Context
    ): DropboxAccountManager {
        return DropboxAccountManager(context)
    }

    @Provides
    @Singleton
    fun provideDropboxRepository(
        @ApplicationContext context: Context,
        accountManager: DropboxAccountManager
    ): DropboxRepository {
        return DropboxRepository(context, accountManager)
    }

    @Provides
    @Singleton
    fun provideSlideshowManager(
        @ApplicationContext context: Context,
        driveRepository: GoogleDriveRepository,
        googleDrivePhotoRepository: GoogleDrivePhotoRepository,
        dropboxPhotoRepository: DropboxPhotoRepository,
        galleryPhotoRepository: GalleryPhotoRepository
    ): SlideshowManager {
        val photoRepositories: Map<SourceType, PhotoRepository> = mapOf(
            SourceType.GOOGLE_DRIVE to googleDrivePhotoRepository,
            SourceType.DROPBOX to dropboxPhotoRepository,
            SourceType.GALLERY to galleryPhotoRepository
        )

        return SlideshowManager(
            context,
            driveRepository,
            photoRepositories
        )
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(): WeatherRepository {
        return WeatherRepository()
    }
}
