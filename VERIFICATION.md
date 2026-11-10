# Verification log

## v0.6 candidate / foreground wake phrase

- Added an opt-in, foreground-only wake phrase using Android 12+ `SpeechRecognizer.createOnDeviceSpeechRecognizer`; no cloud speech-recognition fallback is used for the wake phrase.
- Wake aliases: “Pocket Code”, “پاکت کد” and common Persian spacing/orthographic variants. After a beep, command capture ends after approximately 1.6 seconds of trailing silence or at 30 seconds.
- Post-wake WAV is transcribed through the already-configured Gemini model / 9Router endpoint and its transcript is submitted through the existing agent loop. Existing native file-mutation and sensitive-action approvals are unchanged.
- Manual microphone dictation remains review-before-send.
- Added JVM unit tests for English/Persian wake-phrase normalization, false-positive rejection, signed PCM RMS calculation, the voice-activity threshold, trailing-silence timer boundaries/resets, WAV metadata and sample payload. CI now runs these tests, Android lint, JavaScript syntax validation and the debug APK build.
- Added a generation guard so wake transcription is cancelled when the app is paused/destroyed or wake mode is disabled; a result is not forwarded to the Agent after the foreground session is invalidated.
- **Still requires a physical device:** install/launch, recognizer language availability, real Persian wake phrase precision, mic handoff, actual AudioRecord silence detection, 9Router/Gemini audio transcription and end-to-end Agent submission.
- The checked-in APK remains v0.5.0; do not install it expecting the new wake-word feature until a candidate APK is built and validated.

# Verification log

## v0.5 / navigation and audio

- Added opt-in automatic navigation with stop control; mutation confirmations retained. Native sensitive-click detection is heuristic, supplemented by tool sensitivity metadata, not a guarantee.
- Added labelled clickable-ancestor resolution and fresh target checks; Telegram fix is not device-verified.
- Added bounded in-memory 60-second WAV microphone recorder and Gemini audio request. Live synthetic-silence WAV request via existing 9Router returned HTTP 200 after using a browser-compatible User-Agent. Silence yielded nonempty text: transcription quality is not established, and hallucinations remain possible.
- Chrome 393 × 852 DOM/screenshot inspection passed. Mock bridge checks passed: automatic mode on/off, recording state, transcript insertion, no chat auto-send, cancellation, no horizontal overflow.
- Console empty, local assets HTTP 200/304. Local mobile-emulated LCP 184 ms, CLS 0.00, not real-phone performance.
- Android microphone permission/capture, pause/cancel lifecycle, real speech transcription, actual sensitive-action approvals and Telegram clicking need physical-device testing.

## v0.4 / broad storage read

- Compiled all Java sources against API 35 and packaged version 0.4.0/code 4; APK v2/v3 signature verification passed.
- Broad storage adapter exposes roots/list/read only, validates canonical paths against shared-volume roots, paginates directory results and rejects oversized/non-UTF-8/binary text content.
- Broad native reads check both persisted consent and the current OS grant. Mutation handlers remain separately approval-gated.
- Chrome mobile emulation at 393 × 852: Access dialog DOM and screenshot inspected; no horizontal overflow. Mock bridge navigation from volume to file, text loading and consent revocation passed. These are UI tests, not real Android storage tests.
- Console empty; page, CSS and JavaScript returned HTTP 200/304. Local performance trace: LCP 169 ms, CLS 0.00; these values do not measure actual phone/WebView or router latency.
- Actual Android permission screens, permission persistence/revocation, protected folders, removable cards and native filesystem behavior remain device-unverified. No emulator or phone was available.

## v0.3 / phone capabilities

- Java compilation for selected-folder provider, native approval handlers and AccessibilityService passed against API 35.
- New Access dialog inspected at 393 × 852 with DOM snapshot and screenshot; no horizontal overflow.
- Mock bridge test: denied `openApp` returned to the agent; no second `read_screen` call executed from the same model turn; composer recovered; folder status displayed correctly.
- Chrome console empty; local assets HTTP 200/304; local mobile-emulated LCP 261 ms, CLS 0.00.
- No real device tests: tree permission persistence/revocation, SAF write failures, accessibility overlay approvals, actual node redaction/action execution, foreground/background lifecycle and OEM behavior remain unverified.
- Package-name sensitive-app filtering is heuristic. Password-marked nodes are excluded; complete sensitive-content recognition and FLAG_SECURE detection are not guaranteed.

## v0.2 / 9Router

- Existing local 9Router credentials read without printing secrets; key kept out of source and APK.
- Real `/chat/completions` text request: HTTP 200, valid assistant message.
- Real tool request: HTTP 200, `list_files` function call returned.
- `/models` discovery: HTTP 403; not required by the application.
- Chrome 393 × 852 screenshot/settings inspection; no horizontal overflow.
- Mock OpenAI tool-call round trip: write gated, exactly one write on approval, correct tool_call_id and final assistant response.
- No console errors; assets HTTP 200/304. Local LCP 179 ms, CLS 0.00 (not a phone benchmark).
- Updated Java/API-35 APK build and v2/v3 signature verification passed.
- Physical Android installation, secure connection import and actual on-phone requests remain untested.

## UI verified in Chrome DevTools

- Mobile viewport 393 × 852: DOM and screenshot inspected; dark RTL interface renders correctly.
- Small viewport 360 × 640: no horizontal overflow; composer remains visible, welcome content scrolls.
- Desktop viewport 1280 × 900: app width capped at 900 px; no overflow.
- File creation/edit UI: `test.txt` created with Persian UTF-8 content in browser preview workspace.
- Settings: dialog opens, form validates and closes after save.
- Actual `file://` asset loading checked in Chrome as well as HTTP preview.
- Console: no messages after fixing an HTML `pattern` escape issue.
- Network: document and both local assets loaded successfully (HTTP 200/304); no external asset requests.
- Performance trace on local mobile-emulated preview: LCP 133 ms, CLS 0.00, unthrottled. Not a physical-phone result.

## Agent orchestration contract tested with a mock bridge

These are **simulated contract tests, not real Gemini/device calls**:

- User message triggers Gemini bridge action.
- Mock model requests `write_file`; approval dialog appears.
- No write action occurs before approval.
- Approving writes the requested UTF-8 content, returns function response to model and displays final answer.
- Rejecting causes no write, records denial and restores composer availability.
- Neither test left an uncaught console error.
- Read disclosure approval: mock `read_file` pauses before native read. Denying performs zero reads; approving performs exactly one read and resumes the agent.

## Static validation

- `node --check app/src/main/assets/app.js` passed.
- Android manifest and theme XML parsed successfully.

## Android build verified

- Java compilation against Android API 35 succeeded (deprecation notices only).
- `aapt2` resource compilation and manifest linking succeeded.
- `d8` dex compilation succeeded.
- APK aligned and signed; `apksigner verify --verbose` passed v2 and v3 schemes.
- `aapt2 dump badging` confirmed application ID `dev.pocketcode.app`, launcher activity, min API 26 and target API 35.
- Output: `dist/pocket-code-debug.apk`.
- This is compilation/package verification, **not a device runtime test**.

## Still requires physical Android testing

- Installation, launch, keyboard/insets and lifecycle behavior.
- Android Keystore encryption/decryption.
- Gemini authentication, model compatibility, quota/network error handling and actual function calls.
- Document import/export across device providers.
- Speech recognition availability and Persian transcription.

No API key or emulator/device was available in this environment. Do not infer end-to-end device success from the UI and mock contract checks above.
