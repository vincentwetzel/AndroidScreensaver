#!/usr/bin/env python3
"""Run the Android Screensaver host and optional device test suite.

The runner intentionally keeps the build gate near the beginning of the run:
static project checks run first, followed by ``assembleDebug`` and JVM tests.
Device checks are opt-in (or automatically used when a ready ADB device is
present) and are never allowed to hide a compile or unit-test failure.
"""

from __future__ import annotations

import argparse
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import time
import unittest
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Iterable, Sequence


ROOT = Path(__file__).resolve().parents[1]
TOOLS_DIR = ROOT / "tools"
APK_PATH = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
PACKAGE_NAME = "com.vincentwetzel.androidscreensaver.debug"


@dataclass
class TestResult:
    name: str
    status: str
    duration_seconds: float
    details: str = ""
    log_file: str = ""


class TestRunner:
    def __init__(self, args: argparse.Namespace) -> None:
        self.args = args
        self.started_at = datetime.now().astimezone()
        self.started_monotonic = time.monotonic()
        self.run_id = self.started_at.strftime("%Y%m%d-%H%M%S")
        default_report_dir = ROOT / "build" / "test-reports" / self.run_id
        self.report_dir = Path(args.report_dir).resolve() if args.report_dir else default_report_dir
        self.report_dir.mkdir(parents=True, exist_ok=True)
        self.results: list[TestResult] = []
        self.aborted = False
        self.abort_reason = ""
        self.adb_serial: str | None = None

    def timestamp(self) -> str:
        return datetime.now().strftime("%H:%M:%S")

    def format_log_line(self, message: str) -> str:
        elapsed = time.monotonic() - self.started_monotonic
        return f"[{self.timestamp()} + {elapsed:6.1f}s] {message}"

    def log(self, message: str) -> None:
        print(self.format_log_line(message), flush=True)

    def add_result(
        self,
        name: str,
        status: str,
        started: float,
        details: str = "",
        log_file: Path | None = None,
    ) -> TestResult:
        result = TestResult(
            name=name,
            status=status,
            duration_seconds=round(time.monotonic() - started, 3),
            details=details.strip(),
            log_file=str(log_file) if log_file else "",
        )
        self.results.append(result)
        detail_suffix = f" - {result.details}" if result.details else ""
        self.log(f"[{status}] {name} ({result.duration_seconds:.3f}s){detail_suffix}")
        return result

    def abort(self, reason: str) -> None:
        self.aborted = True
        self.abort_reason = reason
        self.log(f"[FAIL-FAST] {reason}")

    def run_command(
        self,
        name: str,
        command: Sequence[str],
        *,
        timeout_seconds: int,
        env: dict[str, str] | None = None,
    ) -> tuple[int, Path]:
        log_file = self.report_dir / f"{len(self.results) + 1:02d}-{slugify(name)}.log"
        display_command = " ".join(quote_for_display(part) for part in command)
        self.log(f"[RUN] {name}: {display_command}")
        started = self.timestamp()
        process_env = os.environ.copy()
        if env:
            process_env.update(env)
        try:
            process = subprocess.Popen(
                list(command),
                cwd=ROOT,
                env=process_env,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                encoding="utf-8",
                errors="replace",
                bufsize=1,
            )
        except OSError as error:
            log_file.write_text(
                f"[{started}] command={display_command}\n{self.format_log_line(f'launch error: {error}')}\n",
                encoding="utf-8",
            )
            return 127, log_file

        with log_file.open("w", encoding="utf-8") as output:
            output.write(f"[{started}] command={display_command}\n")
            assert process.stdout is not None
            try:
                for line in process.stdout:
                    line = line.rstrip("\r\n")
                    timestamped_line = self.format_log_line(line)
                    output.write(timestamped_line + "\n")
                    print(timestamped_line, flush=True)
                return_code = process.wait(timeout=timeout_seconds)
            except subprocess.TimeoutExpired:
                process.kill()
                return_code = 124
                timeout_line = self.format_log_line(f"timeout after {timeout_seconds}s")
                output.write(timeout_line + "\n")
                self.log(f"[TIMEOUT] {name} exceeded {timeout_seconds}s")
        return return_code, log_file

    def gradle_command(self, *tasks: str) -> list[str]:
        wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
        if os.name == "nt":
            return ["cmd.exe", "/d", "/c", str(wrapper), *tasks, "--console=plain"]
        return [str(wrapper), *tasks, "--console=plain"]

    def adb_command(self, *args: str) -> list[str]:
        adb = shutil.which("adb")
        if not adb:
            raise RuntimeError("adb was not found on PATH")
        command = [adb]
        if self.adb_serial:
            command.extend(("-s", self.adb_serial))
        command.extend(args)
        return command

    def preflight(self) -> bool:
        started = time.monotonic()
        required_files = (
            ROOT / "gradlew.bat",
            ROOT / "settings.gradle.kts",
            ROOT / "app" / "build.gradle.kts",
            ROOT / "app" / "src" / "main" / "AndroidManifest.xml",
            TOOLS_DIR / "tests",
        )
        missing = [str(path.relative_to(ROOT)) for path in required_files if not path.exists()]
        if sys.version_info < (3, 10):
            missing.append(f"Python >= 3.10 (found {platform.python_version()})")
        if not shutil.which("java"):
            missing.append("java on PATH")
        if missing:
            self.add_result("preflight", "FAIL", started, "Missing: " + ", ".join(missing))
            self.abort("Required test tooling is unavailable")
            return False
        self.add_result(
            "preflight",
            "PASS",
            started,
            f"Python {platform.python_version()}, Java and project inputs are available",
        )
        return True

    def select_device(self) -> bool:
        """Select a ready device and return whether device checks can run."""
        started = time.monotonic()
        if self.args.device == "off":
            self.add_result("adb_preflight", "SKIP", started, "Device checks disabled with --device off")
            return False
        if not shutil.which("adb"):
            message = "adb was not found on PATH"
            status = "FAIL" if self.args.device == "on" else "SKIP"
            self.add_result("adb_preflight", status, started, message)
            if status == "FAIL":
                self.abort(message)
            return False

        command = [shutil.which("adb") or "adb", "devices"]
        completed = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, encoding="utf-8", errors="replace")
        devices = parse_adb_devices(completed.stdout)
        requested_serial = self.args.serial or os.environ.get("ANDROID_SERIAL")
        if requested_serial:
            selected = next((device for device in devices if device[0] == requested_serial), None)
            if selected is None:
                message = f"Requested ADB serial is not ready: {requested_serial}"
                status = "FAIL" if self.args.device == "on" else "SKIP"
                self.add_result("adb_preflight", status, started, message)
                if status == "FAIL":
                    self.abort(message)
                return False
        else:
            selected = next((device for device in devices if device[1] == "device"), None)

        if selected is None:
            states = ", ".join(f"{serial}={state}" for serial, state in devices) or "none"
            message = f"No authorized ADB device is connected (devices: {states})"
            status = "FAIL" if self.args.device == "on" else "SKIP"
            self.add_result("adb_preflight", status, started, message)
            if status == "FAIL":
                self.abort(message)
            return False

        self.adb_serial = selected[0]
        self.add_result("adb_preflight", "PASS", started, f"Using authorized device {self.adb_serial}")
        return True

    def run_python_checks(self) -> bool:
        started = time.monotonic()
        command = [
            sys.executable,
            "-m",
            "unittest",
            "discover",
            "-s",
            str(TOOLS_DIR / "tests"),
            "-p",
            "test_*.py",
            "-v",
            "-f",
        ]
        return_code, log_file = self.run_command(
            "python_structure_tests",
            command,
            timeout_seconds=self.args.command_timeout_seconds,
        )
        if return_code != 0:
            self.add_result("python_structure_tests", "FAIL", started, f"exit code {return_code}", log_file)
            self.abort("Project structure tests failed")
            return False
        self.add_result("python_structure_tests", "PASS", started, log_file=log_file)
        return True

    def run_gradle_checks(self) -> bool:
        if self.args.skip_build:
            self.add_result("assemble_debug", "SKIP", time.monotonic(), "Build checks disabled with --skip-build")
            self.add_result("gradle_unit_tests", "SKIP", time.monotonic(), "Build checks disabled with --skip-build")
            return True

        for name, tasks in (
            ("assemble_debug", ("assembleDebug",)),
            ("gradle_unit_tests", ("test",)),
        ):
            started = time.monotonic()
            return_code, log_file = self.run_command(
                name,
                self.gradle_command(*tasks),
                timeout_seconds=self.args.command_timeout_seconds,
            )
            if return_code != 0:
                self.add_result(name, "FAIL", started, f"exit code {return_code}", log_file)
                self.abort(f"{name} failed; stopping before later tests")
                return False
            self.add_result(name, "PASS", started, log_file=log_file)
        return True

    def run_device_checks(self) -> bool:
        if self.adb_serial is None:
            return True
        if not APK_PATH.exists():
            self.add_result("device_apk_available", "FAIL", time.monotonic(), f"Missing {APK_PATH}")
            self.abort("Device checks require the debug APK")
            return False

        if not self.args.skip_install:
            if self.args.clean_install:
                started = time.monotonic()
                return_code, log_file = self.run_command(
                    "adb_clean_install",
                    self.adb_command("uninstall", PACKAGE_NAME),
                    timeout_seconds=self.args.command_timeout_seconds,
                )
                output = log_file.read_text(encoding="utf-8", errors="replace")
                package_was_absent = "Unknown package" in output or "not installed" in output
                if return_code != 0 and not package_was_absent:
                    self.add_result("adb_clean_install", "FAIL", started, f"exit code {return_code}", log_file)
                    self.abort("ADB clean install package removal failed")
                    return False
                detail = "package removed" if return_code == 0 else "package was not installed"
                self.add_result("adb_clean_install", "PASS", started, detail, log_file)

            started = time.monotonic()
            return_code, log_file = self.run_command(
                "adb_install_debug_apk",
                self.adb_command("install", "-r", str(APK_PATH)),
                timeout_seconds=self.args.command_timeout_seconds,
            )
            if return_code != 0:
                self.add_result("adb_install_debug_apk", "FAIL", started, f"exit code {return_code}", log_file)
                self.abort("Debug APK installation failed")
                return False
            self.add_result("adb_install_debug_apk", "PASS", started, log_file=log_file)

        checks = (
            ("device_package_installed", ("shell", "pm", "path", PACKAGE_NAME), ("package:",)),
            (
                "device_launcher_activity",
                ("shell", "cmd", "package", "resolve-activity", "--brief", PACKAGE_NAME),
                (f"{PACKAGE_NAME}/com.vincentwetzel.androidscreensaver.ui.main.MainActivity",),
            ),
            (
                "device_dream_service",
                ("shell", "dumpsys", "package", PACKAGE_NAME),
                ("PhotoScreensaverService", "android.permission.BIND_DREAM_SERVICE"),
            ),
        )
        for name, args, expected_markers in checks:
            started = time.monotonic()
            return_code, log_file = self.run_command(
                name,
                self.adb_command(*args),
                timeout_seconds=self.args.command_timeout_seconds,
            )
            output = log_file.read_text(encoding="utf-8", errors="replace")
            missing_markers = [marker for marker in expected_markers if marker not in output]
            if return_code != 0 or missing_markers:
                detail = f"exit code {return_code}" if return_code != 0 else "missing output: " + ", ".join(missing_markers)
                self.add_result(name, "FAIL", started, detail, log_file)
                self.abort(f"ADB smoke check {name} failed")
                return False
            self.add_result(name, "PASS", started, log_file=log_file)

        started = time.monotonic()
        return_code, log_file = self.run_command(
            "device_launcher_smoke",
            self.adb_command("shell", "monkey", "-p", PACKAGE_NAME, "1"),
            timeout_seconds=self.args.command_timeout_seconds,
        )
        if return_code != 0:
            self.add_result("device_launcher_smoke", "FAIL", started, f"exit code {return_code}", log_file)
            self.abort("ADB launcher smoke test failed")
            return False
        self.add_result("device_launcher_smoke", "PASS", started, log_file=log_file)
        return True

    def write_report(self) -> Path:
        finished_at = datetime.now().astimezone()
        duration = (finished_at - self.started_at).total_seconds()
        counts = {status: sum(result.status == status for result in self.results) for status in ("PASS", "FAIL", "SKIP")}
        report = {
            "run_id": self.run_id,
            "started_at": self.started_at.isoformat(timespec="seconds"),
            "finished_at": finished_at.isoformat(timespec="seconds"),
            "duration_seconds": round(duration, 3),
            "aborted": self.aborted,
            "abort_reason": self.abort_reason,
            "counts": counts,
            "results": [asdict(result) for result in self.results],
        }
        json_path = self.report_dir / "summary.json"
        json_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        text_path = self.report_dir / "summary.txt"
        failed = [result.name for result in self.results if result.status == "FAIL"]
        lines = [
            "Android Screensaver Test Suite Summary",
            "=======================================",
            f"Started : {report['started_at']}",
            f"Finished: {report['finished_at']}",
            f"Duration: {duration:.3f} seconds",
            f"Passed  : {counts['PASS']}",
            f"Failed  : {counts['FAIL']}",
            f"Skipped : {counts['SKIP']}",
            f"Aborted : {self.aborted}",
        ]
        if failed:
            lines.append("Failed tests: " + ", ".join(failed))
        if self.abort_reason:
            lines.append("Abort reason: " + self.abort_reason)
        lines.extend(("", "Results:"))
        for result in self.results:
            detail = f" - {result.details}" if result.details else ""
            lines.append(f"[{result.status}] {result.name} ({result.duration_seconds:.3f}s){detail}")
        text_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        self.log("[REPORT] End-of-run report")
        for line in lines:
            self.log(f"[REPORT] {line}" if line else "[REPORT]")
        self.log(f"[REPORT] {text_path}")
        self.log(f"[REPORT] {json_path}")
        return text_path

    def run(self) -> int:
        try:
            if not self.preflight():
                return 1
            device_requested = self.select_device()
            if self.aborted:
                return 1
            if not self.run_python_checks():
                return 1
            if not self.run_gradle_checks():
                return 1
            if device_requested and not self.run_device_checks():
                return 1
            self.log("[DONE] Test suite completed")
            return 0
        except KeyboardInterrupt:
            self.abort("Interrupted by user")
            return 130
        except Exception as error:  # Keep the end-of-run report available for unexpected failures.
            self.abort(f"Unexpected runner error: {type(error).__name__}: {error}")
            return 1
        finally:
            self.write_report()


def parse_adb_devices(output: str) -> list[tuple[str, str]]:
    devices: list[tuple[str, str]] = []
    for line in output.splitlines():
        line = line.strip()
        if not line or line.startswith("List of devices"):
            continue
        match = re.match(r"^(\S+)\s+(\S+)", line)
        if match:
            devices.append((match.group(1), match.group(2)))
    return devices


def slugify(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")


def quote_for_display(value: str) -> str:
    return f'"{value}"' if any(char.isspace() for char in value) else value


def parse_args(argv: Iterable[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--device",
        choices=("auto", "on", "off"),
        default="auto",
        help="device checks use a ready ADB device when present; on requires one; off disables them",
    )
    parser.add_argument("--serial", help="ADB serial to use when more than one device is connected")
    parser.add_argument("--skip-install", action="store_true", help="do not install the debug APK during device checks")
    parser.add_argument(
        "--clean-install",
        action="store_true",
        help="uninstall the exact debug package before installing; deletes that package's device data",
    )
    parser.add_argument("--skip-build", action="store_true", help="run Python checks only; disables Gradle compile and unit-test gates")
    parser.add_argument(
        "--command-timeout-seconds",
        type=int,
        default=1200,
        help="timeout for each Gradle, Python, or ADB command (default: 1200)",
    )
    parser.add_argument("--report-dir", type=Path, help="write reports to this directory instead of build/test-reports/<timestamp>")
    args = parser.parse_args(argv)
    if args.skip_install and args.clean_install:
        parser.error("--skip-install and --clean-install cannot be used together")
    return args


if __name__ == "__main__":
    sys.exit(TestRunner(parse_args()).run())
