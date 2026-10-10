import io
import re
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from types import SimpleNamespace

from tools.run_tests import TestRunner, parse_adb_devices, slugify


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
