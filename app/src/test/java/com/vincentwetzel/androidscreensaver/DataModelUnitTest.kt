package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for data models and configuration
 */
class DataModelUnitTest {

    @Test
    fun `Photo model creates correctly`() {
        val photo = Photo(
            id = "123",
            sourceType = SourceType.GOOGLE_DRIVE,
            uri = "https://example.com/photo.jpg",
            title = "Test Photo",
            dateTaken = 1234567890L,
            width = 1920,
            height = 1080,
            fileSize = 2048000
        )

        assertEquals("123", photo.id)
        assertEquals(SourceType.GOOGLE_DRIVE, photo.sourceType)
        assertEquals("Test Photo", photo.title)
        assertEquals(1920, photo.width)
        assertEquals(1080, photo.height)
    }

    @Test
    fun `PhotoFolder model creates correctly`() {
        val folder = PhotoFolder(
            id = "folder1",
            sourceType = SourceType.GOOGLE_DRIVE,
            name = "Vacation Photos",
            parentFolderId = null,
            photoCount = 150
        )

        assertEquals("folder1", folder.id)
        assertEquals("Vacation Photos", folder.name)
        assertEquals(150, folder.photoCount)
    }

    @Test
    fun `SlideshowConfig has correct defaults`() {
        val config = SlideshowConfig()

        assertEquals(5, config.slideDurationSeconds)
        assertTrue(config.shuffle)
        assertEquals(DisplayEffect.CROP_TO_FIT, config.displayEffect)
        assertEquals(TransitionEffect.FADE, config.transitionEffect)
        assertEquals(1000, config.transitionDurationMs)
        assertFalse(config.keepScreenOn)
        assertEquals(ScreenOrientation.SYSTEM_DEFAULT, config.screenOrientation)
    }

    @Test
    fun `ScheduleConfig creates correctly`() {
        val schedule = ScheduleConfig(
            enabled = true,
            timeHour = 20,
            timeMinute = 30,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            schedulePreset = SchedulePreset.CUSTOM,
            repeat = true,
            onlyWhenCharging = false
        )

        assertTrue(schedule.enabled)
        assertEquals(20, schedule.timeHour)
        assertEquals(30, schedule.timeMinute)
        assertTrue(schedule.repeat)
        assertFalse(schedule.onlyWhenCharging)
    }

    @Test
    fun `WeatherDecorationConfig defaults to Open-Meteo`() {
        val config = WeatherDecorationConfig()

        assertEquals(WeatherProvider.OPEN_METEO, config.weatherProvider)
        assertEquals(TemperatureUnit.FAHRENHEIT, config.temperatureUnit)
        assertNull(config.apiKey)
    }

    @Test
    fun `CacheConfig has correct defaults`() {
        val config = CacheConfig()

        assertTrue(config.enabled)
        assertEquals(500, config.cacheSizeLimitMB)
        assertEquals(3, config.preloadCount)
    }

    @Test
    fun `PhotoInfoConfig has correct defaults`() {
        val config = PhotoInfoConfig()

        assertFalse(config.enabled)
        assertTrue(config.showFileName)
        assertTrue(config.showDateTaken)
        assertEquals(5, config.fadeOutAfterSeconds)
        assertEquals(PhotoInfoBackground.SEMI_TRANSPARENT, config.background)
        assertTrue(config.textShadow)
    }

    @Test
    fun `VideoPlayback config defaults are correct`() {
        val config = SlideshowConfig()

        assertEquals(VideoAudioMode.SYSTEM_VOLUME, config.videoAudioMode)
        assertEquals(75, config.videoCustomVolume)
        assertEquals(120, config.videoMaxDurationSeconds)
        assertTrue(config.videoAutoPlay)
        assertEquals(VideoDisplayMode.PLAY_FULL, config.videoDisplayMode)
    }

    @Test
    fun `MediaTypeFilter enum has all values`() {
        val values = MediaTypeFilter.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(MediaTypeFilter.IMAGES_AND_VIDEOS))
        assertTrue(values.contains(MediaTypeFilter.IMAGES_ONLY))
        assertTrue(values.contains(MediaTypeFilter.VIDEOS_ONLY))
    }

    @Test
    fun `TransitionEffect enum has all values`() {
        val values = TransitionEffect.values()
        assertEquals(15, values.size)
        assertTrue(values.contains(TransitionEffect.FADE))
        assertTrue(values.contains(TransitionEffect.CROSS_FADE))
        assertTrue(values.contains(TransitionEffect.SLIDE))
    }

    @Test
    fun `DayOfWeek enum has all 7 days`() {
        val values = DayOfWeek.values()
        assertEquals(7, values.size)
        assertTrue(values.contains(DayOfWeek.MONDAY))
        assertTrue(values.contains(DayOfWeek.SUNDAY))
    }
}
