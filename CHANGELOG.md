# Changelog

## 0.8.0 (candidate)
- Added `tap_screen` for exactly one tap at image-pixel x/y from the latest approved `capture_screen` image.
- Every tap requires a native approval even in automatic-navigation mode. The screenshot ID is one-time, expires after 20 seconds and must match the active package, window ID and bounds.
- Maps image pixels into screen coordinates; rejects coordinates outside the active app window and uses a conservative English/Persian deny-list for obvious sensitive-action labels.
- Requires Accessibility `canPerformGestures`; no swipe, drag, long-press, tap chaining or background gesture execution.
- Added eight JVM unit tests for coordinate mapping and label filtering. Not built or device-tested in this task.

## 0.7.0 (candidate)
- Added `capture_screen` as an on-demand visual fallback when Accessibility text is insufficient. Native approval is required for every capture, even in automatic-navigation mode.
- Captures the active app window on Android 14+ and the display on Android 11–13; known blocked packages are rejected before and after approval.
- Sends a bounded JPEG as OpenAI-compatible `image_url` content through 9Router. Image bytes stay in memory for the current Agent run and are excluded from persisted chat history and disk.
- Does not add coordinate clicking. Requires a vision-capable selected model and Android 11+ Accessibility screenshot support. Physical-device and live-router vision testing remain outstanding.

## 0.6.0 (candidate)
- Added opt-in “Pocket Code” / “پاکت کد” wake phrase using Android 12+ on-device speech recognition while the app is foreground.
- Added silence-ended command capture, Gemini transcription through the existing 9Router connection, and automatic submission to the current agent loop.
- Preserved manual review-before-send dictation and native approvals for sensitive actions; wake listening stops on Activity pause/background.

## 0.5.0
- Opt-in automatic ordinary navigation and stop control; retained native mutation/sensitive-action approvals.
- Clickable-ancestor resolution and effective clickability metadata for text-inside-row patterns.
- Tool-loop budget increased from eight to 24 model turns.
- In-memory 60-second Gemini audio transcription to editable draft; no automatic send.
- Browser-compatible router User-Agent; audio payload compatibility tested, speech accuracy not established.

## 0.4.0
- Broad shared-storage read consent, Android all-files permission, roots discovery and paginated file browser.
- Read-only broad adapter with canonical root containment and strict bounded UTF-8 reading.

## 0.3.0
- SAF selected-folder operations and native mutation confirmations.
- Accessibility screen reading, click/type/scroll/back, app discovery/open and per-step overlays.

## 0.2.0
- Existing 9Router OpenAI-compatible chat/tool integration and encrypted credential import.

## 0.1.0
- Initial standalone Java/WebView Android host, Persian dark UI, workspace, sessions and original direct-Gemini integration.
