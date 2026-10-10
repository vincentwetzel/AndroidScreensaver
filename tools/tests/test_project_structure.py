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

    def test_onedrive_repository_uses_nonblocking_account_snapshot(self):
        repository = (ROOT / "app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/OneDrivePhotoRepository.kt").read_text(encoding="utf-8")
        self.assertIn("authManager.getCachedAccountIds()", repository)
        self.assertNotIn("authManager.getAllAccountIds()", repository)


if __name__ == "__main__":
    unittest.main()
