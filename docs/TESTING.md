# Testing plan and evidence standards

`VERIFICATION.md` records actual results per version. This document defines what to verify; checklist items are not claims that tests passed.

## Automated/local baseline
- `node --check app/src/main/assets/app.js`.
- `gradle testDebugUnitTest` (wake-phrase normalization, PCM RMS threshold, trailing-silence state transitions, WAV header/encoding, visual-tap coordinate mapping and sensitive-label deny-list).
- `gradle lintDebug` and `gradle assembleDebug`.
- Verify debug APK with `apksigner`, confirm package/version metadata with `aapt dump badging`, and check ZIP integrity with `unzip -t`.
- CI uploads JUnit XML and the Android Lint HTML report for inspection.
- Scan staged text/binaries and decompressed APK entries for configured credential without printing it.
- Verify no keystore/private connection/build directory enters source archive or Git tree.

## Chrome frontend checks (required after frontend edits)
Open local assets server; inspect DOM and screenshot at 393×852 and desktop. Check no horizontal overflow, scrollable dialogs, visible focus, correct RTL labels and usable controls. Inspect console for exceptions/warnings and network for failed assets. Capture performance trace; report local LCP/CLS as local only.

With a mock native bridge test:
- Error/denial recovery; first-tool-only behavior; no false success on tool failure.
- `capture_screen` approval denial must result in zero screenshot upload; approval adds a multimodal data-URL image only to the current `send()` request loop, with no Base64 in session/localStorage/tool-result messages.
- Visual tap: tool schema carries screenshotId and image-space x/y; no action without the exact current token; token is consumed after one attempt; denied/cancelled taps are not retried; tap forces fresh screen verification and clears the old in-memory image.
- Automatic-navigation on/off status and current-loop stop.
- Storage-root navigation, paginated listing, UTF-8 display and consent revocation.
- Voice start/stop/cancel/error; disabled states; transcript insertion; absolutely no automatic chat submission.
- Empty transcript and reopened recording dialog; permission denied and router failure messages.

## Physical-device matrix (still required)
| Area | Cases |
|---|---|
| Install | Fresh install; upgrade using original signer; retain settings/chats; uninstall/export warning |
| Permissions | Deny/regrant all-files, microphone and Accessibility; restricted-settings sideload flow |
| Storage | Internal root, Downloads, removable card, empty/large directories, protected paths, revoked permission |
| Text | Persian UTF-8, malformed UTF-8, binary, 512 KB boundary, symlink/canonical containment |
| SAF mutation | Cancel approval, approve disposable overwrite, missing parent, read-only provider, interrupted write |
| Accessibility | Read non-sensitive notes app; fresh IDs; stale nodes; app change; keyboard/password field; blocked package |
| Screenshot | Android 11/12/13 display capture; Android 14+ active-window capture; explicit approval even in automatic mode; reject blocked packages; capture failure/secure-window behavior; JPEG size cap; verify image not persisted in localStorage; model vision request failure path |
| Visual tap | `canPerformGestures` registration; exact x/y mapping on multiple DPIs/rotations; reject status/navigation bars and points outside app bounds; stale/wrong screenshotId, expired token, changed package/window/bounds; sensitive English/Persian label rejection; approval denial; gesture cancelled/refused; verify one tap only and fresh-screen verification |
| Telegram | Open, search contact, click result's ancestor, verify chat header, type draft, cancel send approval, approve disposable send only |
| Sensitive effects | English/Persian send/delete/payment labels; unlabelled control; model false-negative flag; changed target |
| Stop | Stop during model request, pending overlay and between actions; no next action; no undo claim |
| Audio | Permission denial, 1–60 seconds Persian/English, silence/noise, cancel, pause/background, rotation, router timeout |
| Lifecycle | Screen lock, app switch, process death; no claim of reliable background automation |

Use disposable files and a harmless chat/account. Do not validate payments with real funds or sensitive accounts. Screenshots/logs can contain private data: redact before committing.

## Reporting a failure
Record app version, Android/OEM, exact task, active package, last tool result/error, whether automatic mode was on, and a redacted screen/tree if useful. Distinguish “model declined to call tool,” “element not exposed,” “native action returned false,” and “action performed but outcome not verified.” A synthetic audio HTTP 200 demonstrates payload acceptance, not faithful transcription.
