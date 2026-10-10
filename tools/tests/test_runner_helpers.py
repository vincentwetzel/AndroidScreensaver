import json
import io
import re
import tempfile
import time
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from types import SimpleNamespace

from tools.run_tests import DEFAULT_SUSPECTS_PATH, SuspectsCache, TestRunner, parse_adb_devices, parse_args, quote_for_display, slugify


class RunnerHelpersTest(unittest.TestCase):
    def test_parse_adb_devices_ignores_header_and_blank_lines(self):
        output = """List of devices attached

emulator-5554\tdevice product:sdk_gphone
usb-device\tunauthorized usb:1-1
offline-device\toffline
"""
        self.assertEqual(
            [
                ("emulator-5554", "device"),
                ("usb-device", "unauthorized"),
                ("offline-device", "offline"),
            ],
            parse_adb_devices(output),
        )

    def test_slugify_creates_safe_log_names(self):
        self.assertEqual("assemble_debug", slugify("Assemble Debug"))
        self.assertEqual("adb_install_debug_apk", slugify("ADB install: debug APK"))

    def test_quote_for_display_only_quotes_whitespace_values(self):
        self.assertEqual("assembleDebug", quote_for_display("assembleDebug"))
        self.assertEqual('"assemble debug"', quote_for_display("assemble debug"))

    def test_parse_args_defaults_to_auto_device_mode(self):
        args = parse_args([])

        self.assertEqual("auto", args.device)
        self.assertFalse(args.skip_build)
        self.assertFalse(args.clean_install)
        self.assertEqual(1200, args.command_timeout_seconds)

    def test_parse_args_supports_default_and_explicit_suspects_cache_paths(self):
        self.assertEqual(DEFAULT_SUSPECTS_PATH, parse_args(["--suspects"]).suspects)
        self.assertEqual(Path("cache.json"), parse_args(["--suspects", "cache.json"]).suspects)

    def test_parse_args_rejects_conflicting_install_options(self):
        with self.assertRaises(SystemExit):
            parse_args(["--skip-install", "--clean-install"])

    def test_write_report_prints_end_of_run_summary(self):
        with tempfile.TemporaryDirectory() as directory:
            runner = TestRunner(SimpleNamespace(report_dir=Path(directory)))
            output = io.StringIO()
            with redirect_stdout(output):
                report_path = runner.write_report()

            console = output.getvalue()
            self.assertIn("[REPORT] End-of-run report", console)
            self.assertIn("Android Screensaver Test Suite Summary", console)
            self.assertIn(str(report_path), console)

            report = Path(report_path).with_name("summary.json")
            report_data = __import__("json").loads(report.read_text(encoding="utf-8"))
            self.assertEqual({"PASS": 0, "FAIL": 0, "SKIP": 0}, report_data["counts"])

    def test_suspects_cache_adds_failures_and_removes_recovered_checks(self):
        with tempfile.TemporaryDirectory() as directory:
            cache_path = Path(directory) / "suspects.json"
            runner = TestRunner(SimpleNamespace(report_dir=Path(directory), suspects=cache_path))

            runner.add_result("broken_check", "FAIL", time.monotonic())
            runner.add_result("broken_check", "SKIP", time.monotonic())
            self.assertEqual(["broken_check"], json.loads(cache_path.read_text(encoding="utf-8")))

            runner.add_result("broken_check", "PASS", time.monotonic())
            self.assertEqual([], json.loads(cache_path.read_text(encoding="utf-8")))

    def test_suspects_cache_preserves_existing_entries_until_they_pass(self):
        with tempfile.TemporaryDirectory() as directory:
            cache_path = Path(directory) / "suspects.json"
            cache_path.write_text(json.dumps(["old_failure", "untouched"]), encoding="utf-8")
            runner = TestRunner(SimpleNamespace(report_dir=Path(directory), suspects=cache_path))

            runner.add_result("old_failure", "PASS", time.monotonic())

            self.assertEqual(["untouched"], json.loads(cache_path.read_text(encoding="utf-8")))

    def test_malformed_suspects_cache_is_reported_and_replaced_on_update(self):
        with tempfile.TemporaryDirectory() as directory:
            cache_path = Path(directory) / "suspects.json"
            cache_path.write_text("not json", encoding="utf-8")
            cache = SuspectsCache(cache_path)

            self.assertTrue(cache.load_warning)
            cache.update("new_failure", "FAIL")
            self.assertEqual(["new_failure"], json.loads(cache_path.read_text(encoding="utf-8")))

    def test_log_uses_simple_clock_and_cumulative_elapsed_time(self):
        with tempfile.TemporaryDirectory() as directory:
            runner = TestRunner(SimpleNamespace(report_dir=Path(directory)))
            output = io.StringIO()
            with redirect_stdout(output):
                runner.log("example message")

        self.assertRegex(
            output.getvalue().strip(),
            re.compile(r"^\[\d{2}:\d{2}:\d{2} \+\s+\d+\.\d+s\] example message$"),
        )


if __name__ == "__main__":
    unittest.main()
