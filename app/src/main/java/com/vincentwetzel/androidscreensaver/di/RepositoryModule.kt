package com.vincentwetzel.androidscreensaver.di

import android.content.Context
import com.vincentwetzel.androidscreensaver.data.repository.GalleryPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.data.repository.WeatherRepository
import com.vincentwetzel.androidscreensaver.dream.SlideshowManager
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
    fun provideGalleryPhotoRepository(
        @ApplicationContext context: Context
    ): GalleryPhotoRepository {
        return GalleryPhotoRepository(context)
    }

    @Provides
    @Singleton
    fun provideGoogleDriveRepository(
        @ApplicationContext context: Context
    ): GoogleDriveRepository {
        return GoogleDriveRepository(context)
    }

    @Provides
    @Singleton
    fun provideGoogleDrivePhotoRepository(
        driveRepository: GoogleDriveRepository
    ): GoogleDrivePhotoRepository {
        return GoogleDrivePhotoRepository(driveRepository)
    }

    @Provides
    @Singleton
    fun provideSlideshowManager(
        @ApplicationContext context: Context,
        driveRepository: GoogleDriveRepository,
        photoRepository: GoogleDrivePhotoRepository,
        galleryPhotoRepository: GalleryPhotoRepository
    ): SlideshowManager {
        return SlideshowManager(context, driveRepository, photoRepository, galleryPhotoRepository)
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(): WeatherRepository {
        return WeatherRepository()
    }
}
