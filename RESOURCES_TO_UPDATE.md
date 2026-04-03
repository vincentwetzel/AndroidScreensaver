# Resources That Need User Updates

This file tracks all placeholder resources that need to be replaced with actual assets before release.

## 🎨 App Icons (PLACEHOLDER - NEEDS UPDATE)

### Launcher Icons
These icons are currently placeholders and MUST be replaced before release:

| Resource | Location | Size | Description |
|----------|----------|------|-------------|
| `ic_launcher.png` | `res/mipmap-*/` | Various | Main app icon (adaptive) |
| `ic_launcher_round.png` | `res/mipmap-*/` | Various | Round app icon |
| `ic_launcher_foreground.png` | `res/mipmap-*/` | Various | Adaptive icon foreground |
| `ic_launcher_background.png` | `res/mipmap-*/` | Various | Adaptive icon background |

**Required Densities:**
- `mipmap-hdpi/` (72x72)
- `mipmap-mdpi/` (48x48)
- `mipmap-xhdpi/` (96x96)
- `mipmap-xxhdpi/` (144x144)
- `mipmap-xxxhdpi/` (192x192)

**Design Guidelines:**
- Use adaptive icon format (Android 8.0+)
- Safe zone: 108x108dp within 144x144dp canvas
- Background should be solid color or gradient
- Foreground should be simple, recognizable shape

---

## 📁 Source Icons (PLACEHOLDER - NEEDS UPDATE)

These icons represent photo sources in the main menu:

| Resource | Description | Current State |
|----------|-------------|---------------|
| `ic_source_gallery.png` | Local gallery icon | ⚠️ Placeholder |
| `ic_source_dropbox.png` | Dropbox icon | ⚠️ Placeholder |
| `ic_source_google_drive.png` | Google Drive icon | ⚠️ Placeholder |
| `ic_source_google_photos.png` | Google Photos icon | ⚠️ Placeholder |
| `ic_source_onedrive.png` | OneDrive icon | ⚠️ Placeholder |
| `ic_source_network.png` | Local network icon | ⚠️ Placeholder |

**Size:** 24dp (48x48 mdpi, 72x72 hdpi, 96x96 xhdpi, 144x144 xxhdpi, 192x192 xxxhdpi)

**Recommendation:** Use Material Design icons or brand-specific logos

---

## 🎬 Transition Effect Icons (PLACEHOLDER - NEEDS UPDATE)

| Resource | Description | Current State |
|----------|-------------|---------------|
| `ic_transition_fade.png` | Fade transition | ⚠️ Placeholder |
| `ic_transition_crossfade.png` | Cross-fade transition | ⚠️ Placeholder |
| `ic_transition_slide.png` | Slide transition | ⚠️ Placeholder |
| `ic_transition_wipe.png` | Wipe transition | ⚠️ Placeholder |
| `ic_transition_swap.png` | Swap transition | ⚠️ Placeholder |
| `ic_transition_cube.png` | Cube transition | ⚠️ Placeholder |
| `ic_transition_doorway.png` | Doorway transition | ⚠️ Placeholder |
| `ic_transition_radial.png` | Radial transition | ⚠️ Placeholder |
| `ic_transition_ripple.png` | Ripple transition | ⚠️ Placeholder |
| `ic_transition_flash.png` | Flash transition | ⚠️ Placeholder |
| `ic_transition_star.png` | Star transition | ⚠️ Placeholder |
| `ic_transition_wind.png` | Wind transition | ⚠️ Placeholder |
| `ic_transition_circle.png` | Circle transition | ⚠️ Placeholder |

**Size:** 24dp standard

---

## 🖼️ Display Effect Icons (PLACEHOLDER - NEEDS UPDATE)

| Resource | Description | Current State |
|----------|-------------|---------------|
| `ic_effect_pan.png` | Pan effect | ⚠️ Placeholder |
| `ic_effect_scale.png` | Scale effect | ⚠️ Placeholder |
| `ic_effect_crop.png` | Crop effect | ⚠️ Placeholder |
| `ic_effect_zoom.png` | Zoom effect | ⚠️ Placeholder |
| `ic_effect_focus.png` | Focus effect | ⚠️ Placeholder |

**Size:** 24dp standard

---

## 🌟 Miscellaneous Icons (PLACEHOLDER - NEEDS UPDATE)

| Resource | Description | Current State |
|----------|-------------|---------------|
| `ic_no_photos.png` | No photos found placeholder | ⚠️ Placeholder |
| `ic_preview.png` | Preview screensaver button | ⚠️ Placeholder |
| `ic_sync.png` | Sync action icon | ⚠️ Placeholder |
| `ic_folder.png` | Folder icon | ⚠️ Placeholder |
| `ic_folder_open.png` | Open folder icon | ⚠️ Placeholder |
| `ic_clock.png` | Clock icon for decorations | ⚠️ Placeholder |
| `ic_calendar.png` | Calendar icon for date | ⚠️ Placeholder |
| `ic_weather.png` | Weather icon | ⚠️ Placeholder |
| `ic_settings.png` | Settings icon | ⚠️ Placeholder |

---

## 🎯 Priority Order for Icon Updates

1. **HIGH PRIORITY** (Before first demo):
   - `ic_launcher.png` and `ic_launcher_round.png`
   - `ic_no_photos.png`
   - `ic_source_google_drive.png` (primary source for v1.0)

2. **MEDIUM PRIORITY** (Before release):
   - All source icons
   - `ic_preview.png`
   - `ic_settings.png`

3. **LOW PRIORITY** (Can be added later):
   - Transition effect icons
   - Display effect icons
   - Miscellaneous icons

---

## 🛠️ How to Update Icons

1. Create icons in vector format (SVG) or PNG at various densities
2. Place files in appropriate `res/drawable/` or `res/mipmap-*/` directories
3. Replace placeholder files with actual icons
4. Update references in XML layouts if needed
5. Test on multiple screen densities

### Icon Generation Tools
- **Android Studio**: Right-click `res/` → New → Image Asset
- **Material Icons**: https://fonts.google.com/icons
- **Icon generators**: https://romannurik.github.io/AndroidAssetStudio/

---

## ✅ Checklist: Icon Updates

- [ ] Replace launcher icons (all densities)
- [ ] Create source icons (all 6 sources)
- [ ] Create transition effect icons (13 icons)
- [ ] Create display effect icons (5 icons)
- [ ] Create miscellaneous icons (9 icons)
- [ ] Test on NVidia Shield TV
- [ ] Test on phone/tablet
- [ ] Verify all densities render correctly
