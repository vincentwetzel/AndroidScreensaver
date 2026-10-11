import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


class ProjectStructureTest(unittest.TestCase):
    def test_required_project_inputs_exist(self):
        for relative_path in (
            "README.md",
            "AGENTS.md",
            "build.gradle.kts",
            "settings.gradle.kts",
            "app/build.gradle.kts",
            "app/src/main/AndroidManifest.xml",
            "app/src/test",
            "tools/run_tests.py",
            "tools/README.md",
        ):
            with self.subTest(relative_path=relative_path):
                self.assertTrue((ROOT / relative_path).exists(), relative_path)

    def test_manifest_has_launcher_and_dream_service(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        activities = manifest.findall("application/activity")
        services = manifest.findall("application/service")

        launcher_names = {
            activity.get(ANDROID_NS + "name")
            for activity in activities
            if activity.find("intent-filter/action") is not None
            and activity.find("intent-filter/action").get(ANDROID_NS + "name") == "android.intent.action.MAIN"
        }
        self.assertIn(".ui.main.MainActivity", launcher_names)

        dream_service = next(
            (service for service in services if service.get(ANDROID_NS + "name") == ".dream.PhotoScreensaverService"),
            None,
        )
        self.assertIsNotNone(dream_service)
        self.assertEqual(
            "android.permission.BIND_DREAM_SERVICE",
            dream_service.get(ANDROID_NS + "permission"),
        )

    def test_gradle_declares_jvm_test_dependencies(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('testImplementation("junit:junit:', gradle)
        self.assertIn('testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"', gradle)

    def test_existing_jvm_tests_are_present(self):
        tests = list((ROOT / "app/src/test").rglob("*Test.kt"))
        self.assertGreaterEqual(len(tests), 2)

    def test_active_changelog_entries_are_not_duplicated_in_history(self):
        changelog = (ROOT / "docs/CHANGELOG.md").read_text(encoding="utf-8")
        for phrase in (
            "Slideshow photo aggregation now filters and deduplicates",
            "The preview activity now preserves pause events",
            "Video playback errors now release the slideshow advance guard",
        ):
            with self.subTest(phrase=phrase):
                self.assertEqual(1, changelog.count(phrase))

    def test_documentation_describes_the_test_runner(self):
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        build_doc = (ROOT / "docs/BUILD.md").read_text(encoding="utf-8")
        self.assertIn("tools/run_tests.py", readme)
        self.assertIn("tools/run_tests.py", build_doc)

    def test_folder_browser_back_string_is_defined(self):
        layout = (ROOT / "app/src/main/res/layout/activity_folder_browser.xml").read_text(encoding="utf-8")
        strings = (ROOT / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8")
        self.assertIn('@string/back', layout)
        self.assertIn('<string name="back">', strings)

    def test_tv_launcher_has_banner_and_alarm_broadcast_is_package_scoped(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        receiver = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/receiver/AlarmReceiver.kt").read_text(encoding="utf-8")
        settings_ui = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/ScheduleSettingsActivity.kt").read_text(encoding="utf-8")
        self.assertIn('android:banner="@drawable/tv_banner"', manifest)
        self.assertRegex(manifest, r'<uses-feature\s+android:name="android.hardware.location.gps"\s+android:required="false"')
        main_activity = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/MainActivity.kt").read_text(encoding="utf-8")
        self.assertLess(main_activity.index("setTheme(R.style.Theme_AndroidScreensaver_TV)"), main_activity.index("ActivityMainTvBinding.inflate"))
        tv_layout = (ROOT / "app/src/main/res/layout/activity_main_tv.xml").read_text(encoding="utf-8")
        tv_theme = (ROOT / "app/src/main/res/values/themes_tv.xml").read_text(encoding="utf-8")
        self.assertNotIn("android:background=\"@color/background_tv\"", tv_layout)
        self.assertIn("<item name=\"android:windowBackground\">@color/background_tv</item>", tv_theme)
        self.assertNotIn(".ui.sources.OAuthCallbackActivity", manifest)
        self.assertIn(".setPackage(context.packageName)", receiver)
    def test_slideshow_manager_routes_cloud_lists_to_the_current_account(self):
        manager = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt").read_text(encoding="utf-8")
        self.assertRegex(
            manager,
            r"photoRepository\.listFolders\(\s*parentFolderId\s*=\s*null,\s*forceRefresh\s*=\s*false,\s*accountId\s*=\s*account\.accountId\s*\)",
        )
        self.assertRegex(
            manager,
            r"photoRepository\.listPhotos\(folder\.id,\s*excludedFolderIds,\s*config\.mediaTypeFilter,\s*accountId\s*=\s*account\.accountId\s*\)",
        )

    def test_slideshow_media_is_filtered_and_deduplicated_per_result_batch(self):
        manager = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt").read_text(encoding="utf-8")
        self.assertIn("val seenPhotoKeys = mutableSetOf<Triple<SourceType, String?, String>>()", manager)
        self.assertIn("if (matchesFilter && seenPhotoKeys.add(key)) filteredUniquePhotos.add(photo)", manager)
        self.assertIn("results.forEach(::addPhotos)", manager)
        self.assertIn("PhotoMediaClassifier.mediaKey(photo)", manager)
        self.assertIn("PhotoMediaClassifier.matchesFilter(photo, config.mediaTypeFilter)", manager)
        self.assertNotIn("private fun isImage(photo: Photo)", manager)
        self.assertNotIn("private fun isVideo(photo: Photo)", manager)
        self.assertNotIn("folderPhotosList", manager)
        self.assertNotIn("distinctPhotos(allPhotos)", manager)

    def test_onedrive_repository_uses_nonblocking_account_snapshot(self):
        repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/OneDrivePhotoRepository.kt").read_text(encoding="utf-8")
        self.assertIn("authManager.getCachedAccountIds()", repository)
        self.assertNotIn("authManager.getAllAccountIds()", repository)

    def test_onedrive_auth_persists_provider_account_id_and_email(self):
        auth_activity = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/OneDriveAuthActivity.kt").read_text(encoding="utf-8")
        self.assertIn("accountId = authenticatedAccount.accountId", auth_activity)
        self.assertIn("accountEmail = authenticatedAccount.email", auth_activity)
        self.assertIn("Successfully signed in as ${authenticatedAccount.email}", auth_activity)
        self.assertNotIn('accountEmail = "OneDrive Account"', auth_activity)

    def test_onedrive_graph_requests_are_cancellable(self):
        repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/OneDrivePhotoRepository.kt").read_text(encoding="utf-8")
        self.assertNotIn(".execute()", repository)
        self.assertGreaterEqual(repository.count("newCall(request).await()"), 5)
        self.assertIn("invokeOnCancellation { cancel() }", repository)

    def test_google_drive_api_execution_is_interruptible(self):
        photo_repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDrivePhotoRepository.kt").read_text(encoding="utf-8")
        repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDriveRepository.kt").read_text(encoding="utf-8")
        self.assertGreaterEqual(photo_repository.count("executeDriveRequest {"), 5)
        self.assertIn("runInterruptible(block = request)", photo_repository)
        self.assertIn("private suspend fun collectPhotosFromFolder", photo_repository)
        self.assertIn("private suspend fun collectSubfolderIds", photo_repository)
        self.assertIn("runInterruptible {", repository)

    def test_slideshow_clock_updates_use_a_single_cancellable_job(self):
        view = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt").read_text(encoding="utf-8")
        self.assertIn("if (clockUpdateJob?.isActive == true) return", view)
        self.assertIn("clockUpdateJob = slideshowScope.launch", view)
        self.assertIn("clockUpdateJob?.cancel()", view)
        self.assertNotIn("decorationHandler.postDelayed", view)

    def test_video_errors_release_advance_guard_before_skipping(self):
        view = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt").read_text(encoding="utf-8")
        error_handler = view.split("override fun onPlayerError(", 1)[1].split("\n                }", 1)[0]
        self.assertLess(error_handler.index("isAdvancing = false"), error_handler.index("advanceToNext()"))
        self.assertIn("pendingVideoFailure = true", error_handler)
        resume_handler = view.split("fun resume()", 1)[1].split("fun stop()", 1)[0]
        self.assertLess(resume_handler.index("if (pendingVideoFailure)"), resume_handler.index("advanceToNext()"))
    def test_preview_activity_preserves_pause_before_slideshow_creation(self):
        activity = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/ScreensaverPreviewActivity.kt").read_text(encoding="utf-8")
        self.assertIn("private var isActivityResumed = false", activity)
        self.assertIn("if (!isActivityResumed) slideshowView?.pause()", activity)
        self.assertIn("isActivityResumed = true", activity)
        self.assertIn("isActivityResumed = false", activity)
    def test_dropbox_sdk_calls_are_interruptible_and_search_failures_propagate(self):
        repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxPhotoRepository.kt").read_text(encoding="utf-8")
        self.assertGreaterEqual(repository.count("executeDropboxCall {"), 10)
        self.assertIn("runInterruptible(block = call)", repository)
        search = repository.split("override suspend fun searchFoldersForAccount", 1)[1].split("override suspend fun getFilteredFolderMediaCountForAccount", 1)[0]
        self.assertIn('Log.e(TAG, "Failed to search Dropbox folders")', search)
        self.assertIn("throw e", search)
    def test_dropbox_account_restore_is_not_repeated_on_repository_construction(self):
        repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxRepository.kt").read_text(encoding="utf-8")
        manager = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxAccountManager.kt").read_text(encoding="utf-8")
        self.assertNotIn("checkExistingSignIn()", repository)
        self.assertIn("CoroutineScope(SupervisorJob() + Dispatchers.IO).launch", manager)
        self.assertIn("private val accountRestoreReady = CompletableDeferred<Unit>()", manager)
        self.assertIn("accountRestoreReady.await()", manager)
        photo_repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxPhotoRepository.kt").read_text(encoding="utf-8")
        self.assertGreaterEqual(photo_repository.count("dropboxRepository.awaitAccountRestore()"), 8)
    def test_gallery_and_slideshow_logs_omit_media_identifiers(self):
        manager = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt").read_text(encoding="utf-8")
        gallery = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryPhotoRepository.kt").read_text(encoding="utf-8")
        self.assertNotIn("Sample IDs:", manager)
        self.assertIn("Warning: Had $loadedPhotoCount photos but filtered to 0", manager)
        self.assertIn("Failed to get folder relative path: ${e.javaClass.simpleName}", gallery)
        self.assertNotIn("Failed to get relative path for folder $folderId", gallery)
        self.assertIn("context, SourceType.GALLERY", gallery)
        self.assertNotIn("context, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY", gallery)
    def test_slideshow_config_refresh_is_once_per_displayed_item(self):
        view = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt").read_text(encoding="utf-8")
        self.assertIn("var configuredPhotoIndex = -1", view)
        self.assertIn("if (configuredPhotoIndex != currentIndex)", view)
        self.assertIn("configuredPhotoIndex = currentIndex", view)
        self.assertIn("delay(500) // Poll every 500ms", view)
    def test_remote_source_cards_use_authenticated_account_state(self):
        main = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/MainActivity.kt").read_text(encoding="utf-8")
        self.assertEqual(3, main.count("AccountStatusText.status(account.isAuthenticated, account.accountEmail"))
    def test_slideshow_display_effect_animators_are_canceled(self):
        view = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt").read_text(encoding="utf-8")
        self.assertIn("private val displayEffectAnimators = mutableSetOf<Animator>()", view)
        self.assertIn("displayEffectAnimators.toList().forEach(Animator::cancel)", view)
        self.assertIn("startDisplayEffectAnimator(this)", view)
        self.assertIn("cancelDisplayEffectAnimations()", view)
        self.assertIn("stopDisplayEffects()", view)
    def test_slideshow_transition_animators_are_tracked_and_canceled(self):
        view = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt").read_text(encoding="utf-8")
        transition = view.split("private fun applyTransition(", 1)[1].split("private fun applyDecorationStyle(", 1)[0]
        self.assertIn("private val transitionAnimators = mutableSetOf<Animator>()", view)
        self.assertIn("private fun startTransitionAnimator(animator: Animator)", view)
        self.assertIn("animator.removeAllListeners()", view)
        self.assertIn("animator.cancel()", view)
        self.assertNotRegex(transition, r"\.start\(\)")
        self.assertIn("cancelTransitionAnimations()", view)
    def test_slideshow_duration_waits_for_media_readiness(self):
        view = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt").read_text(encoding="utf-8")
        self.assertIn("private var currentPhotoReady = false", view)
        self.assertIn("currentPhotoReady = true", view)
        self.assertIn("if (!currentPhotoReady)", view)
        self.assertIn("delay(500)", view)
        self.assertIn("slideshowManager.trimCache(photos.getOrNull(currentIndex)?.uri)", view)
        self.assertIn('photos.getOrNull(currentIndex)?.let(::updatePhotoInfoOverlay)', view)
        self.assertIn('updatePhotoInfoOverlay(photo)', view)
    def test_slideshow_pause_suspends_active_display_animations(self):
        view = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt").read_text(encoding="utf-8")
        pause = view.split("fun pause()", 1)[1].split("fun resume()", 1)[0]
        resume = view.split("fun resume()", 1)[1].split("fun stop()", 1)[0]
        self.assertIn("pauseDisplayAnimations()", pause)
        self.assertIn("resumeDisplayAnimations()", resume)
        self.assertIn("displayEffectAnimators.forEach(Animator::pause)", view)
        self.assertIn("transitionAnimators.forEach(Animator::pause)", view)
        self.assertIn("private var isPaused = false", view)
        loading = view.split("private fun loadPhotosAndStart()", 1)[1].split("private fun showPhoto(", 1)[0]
        self.assertIn("isPlaying = !isPaused", loading)
        self.assertIn("isPaused = true", pause)
        self.assertIn("isPaused = false", resume)
    def test_slideshow_image_requests_allow_hardware_bitmaps(self):
        slideshow = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt").read_text(encoding="utf-8")
        self.assertNotIn(".allowHardware(false)", slideshow)
    def test_svg_media_uses_the_explicit_coil_decoder(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        slideshow = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt").read_text(encoding="utf-8")
        self.assertIn('implementation("io.coil-kt:coil-svg:2.7.0")', gradle)
        self.assertIn("requestBuilder.decoderFactory(SvgDecoder.Factory())", slideshow)
        self.assertIn('endsWith(".svg", ignoreCase = true)', slideshow)
    def test_repository_cache_keys_encode_account_and_folder_components(self):
        key_policy = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/RepositoryCacheKey.kt").read_text(encoding="utf-8")
        self.assertIn("fun of(vararg components: String?)", key_policy)
        self.assertIn("fun set(values: Set<String>)", key_policy)
        for name in ("DropboxPhotoRepository.kt", "GoogleDrivePhotoRepository.kt", "OneDrivePhotoRepository.kt", "BaseCloudPhotoRepository.kt"):
            repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository" / name).read_text(encoding="utf-8")
            self.assertIn("RepositoryCacheKey.of(", repository, name)
        self.assertNotIn('"${accountId}_${folderId}', "".join((ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository" / name).read_text(encoding="utf-8") for name in ("DropboxPhotoRepository.kt", "GoogleDrivePhotoRepository.kt", "OneDrivePhotoRepository.kt", "BaseCloudPhotoRepository.kt")))
    def test_cloud_media_cache_paths_use_validated_extensions(self):
        policy = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/MediaCacheFilePolicy.kt").read_text(encoding="utf-8")
        google = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDrivePhotoRepository.kt").read_text(encoding="utf-8")
        dropbox = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxPhotoRepository.kt").read_text(encoding="utf-8")
        self.assertIn("Regex(\"[A-Za-z0-9]{1,10}\")", policy)
        self.assertGreaterEqual(google.count("MediaCacheFilePolicy.extension("), 3)
        self.assertGreaterEqual(dropbox.count("MediaCacheFilePolicy.extension("), 3)
        onedrive = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/OneDrivePhotoRepository.kt").read_text(encoding="utf-8")
        self.assertGreaterEqual(google.count("MediaCacheFilePolicy.cacheKey("), 3)
        self.assertGreaterEqual(dropbox.count("MediaCacheFilePolicy.cacheKey("), 4)
        self.assertIn("MediaCacheFilePolicy.cacheKey(", onedrive)
        self.assertNotIn("safeFileId =", google + dropbox + onedrive)
    def test_onedrive_search_pages_fail_as_a_unit_and_logs_are_sanitized(self):
        repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/OneDrivePhotoRepository.kt").read_text(encoding="utf-8")
        search = repository.split("override suspend fun searchFoldersForAccount", 1)[1].split("override suspend fun getSubfolderIdsForAccount", 1)[0]
        self.assertIn("requireOneDrivePageBody(response.code, response.body?.string())", search)
        self.assertIn('throw IOException("Graph API page did not contain a value array")', search)
        self.assertIn("throw e", search)
        self.assertNotRegex(repository, r"Log\\.e\\([^\\n]*, e\\)")
        self.assertIn("Failed to collect photos: ${e.javaClass.simpleName}", repository)
    def test_onedrive_auth_config_matches_msals_account_mode_and_refreshes_tokens(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        manager = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/auth/OneDriveAuthManager.kt").read_text(encoding="utf-8")
        config = (ROOT / "msal_auth_config.json").read_text(encoding="utf-8")
        self.assertIn("prepareDebugMsalConfig", gradle)
        self.assertIn("createMultipleAccountPublicClientApplication", manager)
        self.assertIn('"account_mode": "MULTIPLE"', config)
        self.assertIn('"grant_type", "refresh_token"', manager)
        self.assertIn("acquireTokenSilent(SCOPES.toTypedArray(), account, authority)", manager)


    def test_schedule_settings_are_read_written_and_applied_by_alarms(self):
        settings = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/utils/SettingsManager.kt").read_text(encoding="utf-8")
        scheduler = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/service/ScheduleService.kt").read_text(encoding="utf-8")
        receiver = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/receiver/AlarmReceiver.kt").read_text(encoding="utf-8")
        settings_ui = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/ScheduleSettingsActivity.kt").read_text(encoding="utf-8")
        self.assertIn("autostartSchedules = ScheduleConfigCodec.decode", settings)
        self.assertIn("autostopSchedules = ScheduleConfigCodec.decode", settings)
        self.assertIn("ScheduleConfigCodec.encode(config.autostartSchedules)", settings)
        self.assertIn("ScheduleConfigCodec.replaceFirst(schedules, schedule)", settings)
        self.assertIn("ScheduleConfigCodec.encode(config.autostopSchedules)", settings)
        self.assertIn("EXTRA_ONLY_WHEN_CHARGING, schedule.onlyWhenCharging", scheduler)
        self.assertIn("ScheduleAlarmPolicy.shouldRunAutostart", receiver)
        self.assertIn("ScheduleAlarmPolicy.shouldDisableAfterTrigger", receiver)
        self.assertIn("binding.switchCharging.visibility = if (isAutostart) View.VISIBLE else View.GONE", settings_ui)
    def test_repository_prefetch_respects_cache_and_deduplicates_work(self):
        cloud = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/BaseCloudPhotoRepository.kt").read_text(encoding="utf-8")
        gallery = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryPhotoRepository.kt").read_text(encoding="utf-8")
        self.assertIn("if (!prefetchingAccountIds.add(accountId)) return", cloud)
        self.assertIn("listFoldersForAccount(null, false, accountId)", cloud)
        self.assertIn("rootPrefetchInProgress.compareAndSet(false, true)", gallery)
        self.assertIn("listFolders(null, forceRefresh = false)", gallery)
        self.assertIn("hasGalleryPermission(mediaFilter", gallery)
    def test_gallery_capture_dates_prefer_media_metadata(self):
        gallery = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryPhotoRepository.kt").read_text(encoding="utf-8")
        self.assertGreaterEqual(gallery.count("MediaStore.MediaColumns.DATE_TAKEN"), 2)
        self.assertEqual(2, gallery.count("GalleryMediaDatePolicy.dateTakenMillis("))
    def test_android14_gallery_selected_access_is_declared_and_refreshed(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        activity = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GalleryFolderBrowserActivity.kt").read_text(encoding="utf-8")
        manager = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt").read_text(encoding="utf-8")
        self.assertIn("android.permission.READ_MEDIA_VISUAL_USER_SELECTED", manifest)
        self.assertIn("MENU_CHANGE_SELECTED_MEDIA", activity)
        self.assertIn("viewModel.invalidateGalleryCaches()", activity)
        self.assertIn("galleryRepository?.hasSelectedMediaAccess()", manager)
        self.assertIn("galleryRepository.syncPhotos()", manager)
if __name__ == "__main__":
    unittest.main()
