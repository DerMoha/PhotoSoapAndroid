# PhotoSoap screenshot pack

The repository README displays the clean emulator captures from `source/`,
without promotional captions, backgrounds, or device frames.

Six English promotional images using real Android captures, with individual
feature palettes, a full dark statistics screen, an angled swipe view, and a
two-device light/dark appearance comparison.

| Image | Screen | Message |
| --- | --- | --- |
| 01 | Review | Swipe through your photos. |
| 06 | Swipe in progress | Swipe right to keep. |
| 02 | Delete List | Check what you delete. |
| 03 | Statistics | See how much space you freed. |
| 04 | Achievements | Earn badges as you go. |
| 05 | Settings | Choose your colors and theme. |

## Files

- `google-play/`: 1080 × 1920 RGB PNG promotional artwork, with captions and a generic phone frame.
- `github/`: optional 360 × 640 promotional versions, plus an `overview.png` contact sheet.
- `source/`: 1080 × 2424 light emulator captures and sample flower photography.
- `source/dark/`: matching captures of the app’s actual dark theme.

The promotional artwork uses the 9:16 portrait size described in
[Google Play's preview asset guidance](https://support.google.com/googleplay/android-developer/answer/9866151).
Upload the six individual files in `google-play/` as phone screenshots; the
overview image is an optional promotional contact sheet. Check that the UI still matches the release you
publish. These assets do not include a Play feature graphic or high-resolution icon.

## Capture provenance

Captured on 7 October 2026 from `PhotoSoap_API37_2` (Android 17 / API 37),
running a freshly built `1.0.0-beta.3` debug APK from this workspace
(`com.dermoha.photosoap.debug`). The emulator display was set to Pixel 10 proportions, 1080 × 2424,
normal text size, English, light and dark appearance, and the Teal accent.

The session ran with `-read-only -no-snapshot-save`, so demo media, app data,
and display changes were not persisted to the saved AVD. No application source
changes were needed for the screenshots.

The flower photograph was extracted from the sibling iOS app’s existing
`public/screenshots/review.png` presentation asset; no iOS interface is included.
The frame is an approximate Pixel-style front view with a centered camera
cutout and side buttons, not an official Google hardware render. The camera
cutout is part of the presentation frame. Android uses its hole-cutout emulation
for appropriate status-bar insets. The app screens are scaled proportionally
inside a separate 1080 × 1920 marketing canvas; they are never stretched.
The swipe image (`06-swipe.png`, presented second in the overview) was captured
while holding a real rightward drag in the emulator. The rotation, Keep badge,
and underlying card are rendered by the app; this image uses light mode to keep
the gesture clear.


Progress statistics are deliberately seeded demonstration data: 361
reviewed (including one queued item), 264 kept, 96 deleted, and 1,240,000,000 bytes freed (shown by the app
as 1.15 GB). The Delete List demonstrates queued media without confirming deletion.
They illustrate the UI and are not measured cleanup results or promised savings.
The achievement state reflects those sample values. Screenshot UI text and
numbers were rendered by the application, not painted onto the captures.

## Regenerate the artwork

With Python 3 and Pillow installed:

```sh
python3 scripts/render-screenshots.py
```

Run from the repository root. Edit `SLIDES` in the script to change the captions.
The script defaults to macOS Arial font files. On another operating system, set
`PHOTOSOAP_FONT_REGULAR` and `PHOTOSOAP_FONT_BOLD` to local TTF paths.
Screens retain their aspect ratio. Some device frames extend beyond the canvas
for a closer view; the underlying source captures remain complete.
Replace the source captures when the application's design changes, then rerun.
