# Test Tools

`run_tests.py` is the host-side test entry point for Android Screensaver. It
keeps a timestamped command log for every stage and writes an end-of-run report
to `build/test-reports/<timestamp>/summary.txt` and `summary.json`. The same
summary is printed to the console after the final test stage.

Console lines use a compact local clock and cumulative suite runtime, for
example `[14:27:38 +   1.8s]`. The JSON report retains full ISO timestamps for
machine-readable run metadata.

## Usage

From the repository root:

```text
python tools/run_tests.py
```

The default `auto` device mode runs the ADB smoke checks when an authorized
device is connected and records a skip otherwise. Use these explicit modes for
CI or debugging:

```text
python tools/run_tests.py --device on
python tools/run_tests.py --device off
python tools/run_tests.py --device on --serial emulator-5554
python tools/run_tests.py --device on --clean-install
```

`--device on` fails during preflight when ADB is missing, no device is
connected, or the selected device is unauthorized. A device run builds and
installs the debug APK, then verifies the installed package, launcher activity,
DreamService, and a launcher smoke start.

Use `--clean-install` only when an existing installation blocks replacement,
such as a debug-signing mismatch. It explicitly uninstalls the exact debug
package before installing, which deletes that package's device data; this is
never done automatically.

The runner is fail-fast after preflight: project structure tests run first,
`assembleDebug` is the compile gate, JVM tests run next, and device checks run
last. `--skip-build` is available only for fast host-side tooling iteration;
it intentionally skips the compile and Gradle unit-test gates.
