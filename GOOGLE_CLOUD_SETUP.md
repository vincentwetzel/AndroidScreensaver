# Google Cloud Setup Guide

This guide will help you verify your Google Cloud Project is configured correctly for the Android Screensaver app.

## ✅ Current Status

**OAuth Client ID**: Already configured in code
- Client ID: `459442467476-r9unkjslnp6giff3v0iv642hiss1ntap.apps.googleusercontent.com`
- Project ID: `androidscreensaver`
- Location: `app/src/main/java/.../utils/GoogleOAuthConfig.kt`

**No additional file downloads needed!** The app is already configured.

---

## What You Need to Verify

### Step 1: Enable Google Drive API

1. Go to [Google Cloud Console - API Library](https://console.cloud.google.com/apis/library)
2. Make sure project `androidscreensaver` is selected (top dropdown)
3. Search for **"Google Drive API"**
4. Click on it
5. If you see **"Enable"** button, click it
6. If you see **"Manage"** or **"API enabled"**, it's already enabled ✅

---

### Step 2: Configure OAuth Consent Screen

1. Go to [OAuth Consent Screen](https://console.cloud.google.com/apis/credentials/consent)
2. Verify **"User Type"** is **"External"** (unless you have Google Workspace)
3. Fill in required fields if not already done:
   - **App name**: `Android Screensaver`
   - **User support email**: Select your email
   - **Developer contact information**: Your email
4. Click **"Save and Continue"** (if you made changes)

### Step 3: Add Google Drive Scope

1. On the OAuth consent screen page, go to **"Scopes"** section
2. Click **"Add or Remove Scopes"**
3. Search for and add these scopes:
   - `https://www.googleapis.com/auth/drive.readonly` (Drive ReadOnly)
   - `https://www.googleapis.com/auth/drive.metadata.readonly` (Metadata ReadOnly)
4. Click **"Update"**
5. Click **"Save and Continue"**

### Step 4: Add Test Users

1. On the OAuth consent screen page, go to **"Test users"** section
2. Click **"+ Add users"**
3. Add your Google account email (the one you'll use to test the app)
4. Click **"Save and Continue"**

**Note**: During development, only added test users can authenticate. When you're ready for production, you'll submit the app for verification.

---

## That's It!

Your app is already configured with the OAuth Client ID. You just need to ensure:
- ✅ Google Drive API is enabled
- ✅ OAuth consent screen is configured
- ✅ Drive scopes are added
- ✅ Your Google account is a test user

---

## Testing OAuth

Once you build and run the app:

1. Open the app
2. Go to Google Drive source settings
3. Click "Authenticate" or "Sign in with Google"
4. Google will show a consent screen
5. Grant permissions
6. You should be able to browse your Google Drive folders

---

## Troubleshooting

### "Google Drive API has not been used" error:
- Go to [API Library](https://console.cloud.google.com/apis/library)
- Enable Google Drive API

### "Unauthorized client" or "Access denied" error:
- Verify your Google account is added as a **test user** in OAuth consent screen
- Verify the scopes are added (`drive.readonly`)

### "Sign-in failed" error:
- Check that OAuth Client ID in `GoogleOAuthConfig.kt` matches your Google Cloud Console
- Verify you have internet connection
- Check logcat for detailed error messages

---

## Next Steps

After verifying the above:
1. ✅ Google Cloud project configured
2. ⏭️ Build and run the app in Android Studio
3. ⏭️ Test Google Drive authentication
