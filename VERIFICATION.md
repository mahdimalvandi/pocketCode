# Verification log

## v0.15.0 candidate / wake reliability and Persian transcription validation

- Wake-listener retry behavior now distinguishes normal `ERROR_NO_MATCH`/`ERROR_SPEECH_TIMEOUT` from recognizer/service failures. Repeated failures use bounded backoff and stop after eight consecutive failures; unsupported on-device Persian language remains an explicit stop condition.
- Wake phrase matching uses Unicode-aware token boundaries and retains recognized Persian/English spacing variants while rejecting partial matches embedded inside longer words.
- Native code prevents wake listening from starting while any manual recorder is active, stops the wake listener before manual recording, and rejects a manual recording request while a wake transcription is still in flight.
- Transcription validates the generated PCM16 mono 16 kHz WAV header and supports OpenAI-compatible string or text-part-array response content. Empty/malformed transcript responses are explicit errors; no alternate ASR provider is silently substituted. Manual dictation still populates the draft for review rather than sending automatically.
- Replaced the malformed v0.14.0 prompt literal concatenation with JSON-escaped JavaScript string literals for the system and authoritative navigation policy.
- Added JVM tests for wake phrase boundaries and WAV validation. **Not run, not built or device-tested by request.** Real Persian recognition accuracy, microphone ownership on OEM devices, silence/noise thresholds and 9Router audio fidelity still require the final physical-device test cycle.


## v0.14.0 candidate / Accessibility target diagnostics and action safety

- Clickable-ancestor lookup now traverses up to sixteen parent levels. A successful click returns only the resolved view class and ancestor depth as diagnostic metadata; no message text or raw path is added to persistent logs.
- Approval text now includes the bounded intended click effect, nearby non-password UI context, and exact text proposed for a field replacement. Context preview is bounded to eight levels and 1,200 characters.
- Added native English/Persian sensitive-action classification across labels, descriptions, resource IDs, view classes, hints and up to three parent nodes. The heuristic is not exhaustive; a model flag cannot downgrade a native match.
- Updated model tool descriptions, navigation policy, app onboarding and Android Accessibility disclosure to match the same consent rule.
- Added JVM test cases for sensitive-action labels. **Not run, not built and not device-tested by request.** No Actions run. Actual Telegram reproduction, custom icon-only controls, confirmation preview behavior, Java tests and all UI/device validation remain pending.


## v0.13.0 candidate / persistent stop control

- Added a chat-header stop button that appears during an active Agent run. Stop requests immediately update WebView run state and block subsequent model/tool operation starts at the loop boundaries, including later members of a read batch.
- The stop request also disables automatic navigation through the existing native bridge action. Because bridge work is serialized, this request may wait for the current native call. In-flight model HTTP and phone operations may finish; there is no rollback or guaranteed cancellation.
- The active-run native confirmation dialog and the in-app file approval dialog expose dedicated stop options. The native stop event is delivered to WebView before the denied-operation response to avoid a race that could start another tool.
- Active task plans are paused after stop. Pending UI actions without a fresh observation remain unverified.
- Added a pure run-control policy and Node tests for idempotence, stop-state gating and the semantics of already-started work.
- **Not run, not built and not device-tested by request.** No GitHub Actions workflow was dispatched. JS syntax/tests, Chrome responsive interaction checks, Android build/lint and physical-device stop tests remain for the combined final validation.
- **Requires later checks:** tap stop during a long 9Router request, during a shared-storage read, during a UI action, between calls in a batch, and while a task plan or UI-verification checkpoint is active; verify that new operations never begin after the boundary and that completed native side effects are not described as cancelled.

## v0.12.0 candidate / execution timeline

- Added a per-chat collapsible execution history capped at 100 events. It tracks tool labels, local timestamps, elapsed duration and constrained operation states. Tool calls inside permitted read batches are recorded individually, as are deferred calls.
- Added explicit in-app file approval states. UI-changing actions remain awaiting verification after the native request returns; only a fresh read_screen/capture_screen changes the matching action to observed. This is not a semantic success assertion.
- Timeline data intentionally excludes arguments, paths, file content, typed text, screenshots, chat messages, model reasoning and raw error payloads. Persisted event structures are restored through an allowlist and are displayed through textContent.
- Added the pure module and tests for 100-event bounds, metadata sanitization, state transitions, durations, approval, verification and errors.
- **Not run, not built and not device-tested by request.** No GitHub Actions workflow was dispatched. JavaScript syntax, Node tests, Chrome visual/interaction checks, Android lint/build and physical-device validation remain pending the combined final test cycle.
- **Requires later checks:** mock a native approval while a tool is pending; verify event mapping for batched and deferred calls; stop during a UI verification checkpoint; simulate denied readback and app interruption; switch/create/restore chats; confirm old sessions restore safely; inspect localStorage for sensitive-data absence; verify that observed never implies semantic success.

## v0.11.0 candidate / conservative tool batching

- Added a pure tool-batch scheduler allowing up to three independent, previously-consented broad-storage reads (`phone_storage_roots`, `read_phone_storage`) from one model tool-call response.
- Tool calls execute sequentially through the existing single-threaded native bridge; no native threading or bridge queue changes were made. The expected gain is fewer model turns, not parallel Android I/O.
- Batching is disabled when a UI verification checkpoint is pending, a task plan is active/paused, the user has stopped automation, or the response contains any mixed/unsafe/approval-gated/mutating call. In those cases only the first model tool call executes and the others receive explicit not-executed tool results.
- Batch read text is limited to 48,000 characters per file and 120,000 characters across the batch; truncated results carry original/returned character counts. A normal single `read_phone_storage` call is unchanged.
- Added Node scheduler tests for safe classification, mixed-call fallback, bounds, stop state and verification/task-plan gating.
- **Not built or executed in this task, by request.** No GitHub Actions workflow was dispatched. JavaScript syntax, scheduler tests, Android build and physical-device testing remain pending the next combined test cycle.
- **Requires later checks:** test multiple read calls and stable `tool_call_id` mapping against a mocked 9Router response; confirm exactly one bridge read per call and sequential ordering; test two/three/four calls, mixed safe+unsafe calls, malformed args, denial/permission errors, task/verification gates, stop mid-batch, 48k/120k truncation, 4 MB HTTP request bound and context-token cost. Confirm the model retries truncated/overflow reads as individual calls only when needed.

## v0.10.0 candidate / task planning and memory

- Added a WebView-local task-plan module with bounded 2–10 step plans, stable step IDs, sanitized goal/title/note lengths, ordered step progression and terminal completion checks.
- Added `set_task_plan`, `update_task_step`, `resume_task`, `clear_task_plan` and `finish_task` local tools. The model is instructed to plan only genuinely multi-step requests and update progress after verifying outcomes.
- The plan is stored in the active chat session's localStorage record and rendered in a progress panel. The user can clear the plan for that session. No screenshot, raw tool response or file content is stored in task memory.
- An unfinished active plan is paused when the prior run was interrupted. Resuming requires an explicit continuation intent, a fresh `read_screen` or approved `capture_screen`, and `resume_task`; phone/file operations are rejected before resume. Failed/approval-blocked earlier steps cannot be skipped automatically.
- Future model requests omit prior-turn tool-call payloads and retain only the latest eight plain user/assistant text messages, the current run's full tool history, and a compact task-memory snapshot. `clear_task_plan` discards unrelated paused metadata without touching phone data. Session display/history remains unchanged.
- Added Node tests for plan bounds, status transitions, ordered steps, safe pause/resume, blocking of approval-required steps, restoration and compact summaries.
- **Not built or executed in this task, by request.** No GitHub Actions workflow was dispatched. JS syntax, tests, APK and real-device execution remain pending the next batched validation.
- **Requires later checks:** run Node syntax/tests; verify plan tool compatibility with the selected 9Router model; render progress on mobile/desktop and session switching; test pause/resume after app process death, denied/stale readback, new task replacing an old plan, plan clear and the 24-turn budget. Confirm old tool payloads do not leak into a resumed request and current-run tool-call/result pairs stay valid.

## v0.9.0 candidate / action verification and recovery

- Added `agent-reliability.js` as a pure policy module shared by the WebView runtime and Node's built-in test runner.
- Every UI-changing action now creates an action-ID checkpoint with its expected outcome and native API acceptance state. The Agent cannot take another UI action or return a final answer until it obtains a fresh Accessibility screen read or approved screenshot. The observation is attached to the matching tool result for model comparison.
- A native `ok=true` is treated only as request acceptance, not proof that the intended UI state occurred. Typed text is omitted from the verification summary.
- Added conservative recovery categories for user denial, blocked target, missing permission, stale UI, unavailable target, transient failure and unknown outcome. Only transient failures from idempotent broad-storage reads may be retried once. UI actions and file mutations are never automatically replayed.
- Denied/blocked verification cannot be bypassed with another UI action. If an observation is denied or fails twice, the action is marked unverified and the same action is not automatically repeated.
- Added ten Node tests covering action classification, observation gating, sensitive-text omission, denials, stale IDs, ambiguous side effects, safe-read retry eligibility and blocked-action bypass.
- **Not built and not executed in this task, by request.** No GitHub Actions workflow was dispatched. JS syntax and Node tests remain pending the next batched validation.
- **Requires later checks:** node syntax and tests; mock a multi-step action→readback flow; test premature final response prevention, denial, blocked target, stale IDs, unavailable screen, model ignoring the checkpoint, stop behavior and router/native errors. Confirm that fresh evidence contradicting the expected outcome is not described as success.

## v0.8.0 candidate / one-shot visual tap

- Added `tap_screen` through Android `AccessibilityService.dispatchGesture`, with `canPerformGestures=true` in the service metadata.
- Every tap requires fresh native approval, even in automatic-navigation mode. It requires the exact `screenshotId` from the current `capture_screen`, expires after 20 seconds, is consumed before dispatch, and is bound to the same active package/window/bounds. Image-pixel coordinates are converted to display/window coordinates and rejected outside the app window.
- Obvious English/Persian labels for send, submit, delete, payment, transfer, authentication, permissions and similar effects are blocked heuristically. This list is incomplete and does not replace user review.
- Added eight JVM unit tests for coordinate mapping, display-to-window clipping, invalid dimensions and sensitive label handling.
- **Not built or tested in this task, by request.** GitHub Actions remains manual-only; no workflow was dispatched.
- **Requires later batch verification:** JS syntax, Android compile/lint/package, test suite, real-device gesture support and coordinate accuracy at different DPIs/rotations, app-switch/layout-change/expiry handling, denial/cancellation, custom controls and actual post-tap verification. Verify that gesture dispatch does not interrupt other active gestures unexpectedly.

## v0.7.0 candidate / visual screen understanding

- Implemented `capture_screen` through Android AccessibilityService screenshot APIs (Android 11+); Android 14+ uses active-window capture, Android 11–13 use display capture.
- The tool is always protected by a fresh native approval, even when automatic navigation is enabled. It checks the active package before approval and again before capture, rejects known blocked packages, limits image dimensions and JPEG size, and returns Base64 only in memory.
- JavaScript attaches the image as OpenAI-compatible `image_url` content during the current Agent run. Base64 is stripped from the tool result and never added to persisted `session.contents` or localStorage. No coordinate actions were added.
- **Not built or tested in this task, by request.** GitHub Actions remains manual-only; no workflow was dispatched.
- **Requires later batch verification:** JS syntax, Android compile/lint/package, screenshot API capability on Android 11–15, capture denial and app-switch race, FLAG_SECURE/blocked-package handling, no local persistence, 9Router model image compatibility and actual visual reasoning on a physical device.

## v0.6 candidate / foreground wake phrase

- Added an opt-in, foreground-only wake phrase using Android 12+ `SpeechRecognizer.createOnDeviceSpeechRecognizer`; no cloud speech-recognition fallback is used for the wake phrase.
- Wake aliases: “Pocket Code”, “پاکت کد” and common Persian spacing/orthographic variants. After a beep, command capture ends after approximately 1.6 seconds of trailing silence or at 30 seconds.
- Post-wake WAV is transcribed through the already-configured Gemini model / 9Router endpoint and its transcript is submitted through the existing agent loop. Existing native file-mutation and sensitive-action approvals are unchanged.
- Manual microphone dictation remains review-before-send.
- Added JVM unit tests for English/Persian wake-phrase normalization, false-positive rejection, signed PCM RMS calculation, the voice-activity threshold, trailing-silence timer boundaries/resets, WAV metadata and sample payload. CI now runs these tests, Android lint, JavaScript syntax validation and the debug APK build.
- Added a generation guard so wake transcription is cancelled when the app is paused/destroyed or wake mode is disabled; a result is not forwarded to the Agent after the foreground session is invalidated.
- **Still requires a physical device:** install/launch, recognizer language availability, real Persian wake phrase precision, mic handoff, actual AudioRecord silence detection, 9Router/Gemini audio transcription and end-to-end Agent submission.
- Final GitHub Actions PR run 51 on commit `8741d86345a5a57d2dd006616336655494b9f4ca` passed: `node --check`, all 17 JVM tests (0 failures/errors/skips), `lintDebug`, `assembleDebug`, APK signature verification, package/version metadata check and ZIP integrity check.
- Android Lint reports 13 non-blocking warnings: broad shared-storage permission policy, eight synchronous `SharedPreferences.commit()` calls, target-SDK freshness, WebView JavaScript enablement, missing data-extraction rules and missing application icon. No Lint errors remain.
- Candidate debug APK was uploaded as the `pocket-code-debug-apk` workflow artifact for run [42788994845](https://github.com/mahdimalvandi/pocketCode/actions/runs/42788994845). The checked-in `dist/pocket-code-debug.apk` is still v0.5.0; use the CI artifact for the v0.6.0 wake-word candidate.
- **Still requires a physical device:** install/launch, recognizer language availability, real Persian wake phrase precision, mic handoff, actual AudioRecord silence detection, 9Router/Gemini audio transcription and end-to-end Agent submission.

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
