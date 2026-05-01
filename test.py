import os

def check_hilt_setup():
    # Check for the plugin in the app-level file
    app_gradle = os.path.join('app', 'build.gradle.kts')
    if os.path.exists(app_gradle):
        with open(app_gradle, 'r') as f:
            content = f.read()
            if 'id("com.google.dagger.hilt.android")' in content:
                print("[✓] Hilt plugin is applied in app/build.gradle.kts")
            else:
                print("[!] ERROR: Hilt plugin is MISSING from app/build.gradle.kts plugins block!")

    # Check for Jetifier
    if os.path.exists('gradle.properties'):
        with open('gradle.properties', 'r') as f:
            if 'android.enableJetifier=false' in f.read():
                print("[✓] Jetifier is correctly disabled.")
            else:
                print("[!] WARNING: Set android.enableJetifier=false in gradle.properties")

    input("\nPress Enter to exit...")

if __name__ == "__main__":
    check_hilt_setup()