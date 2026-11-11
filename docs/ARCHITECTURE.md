# Architecture and code map

## Components
```text
Bundled WebView UI (assets/index.html, styles.css, app.js)
    | NativeBridge.request(JSON {id, action, payload})
    v
MainActivity Java dispatcher / native authorization
    |-- private workspace / SharedFolder SAF / PhoneFiles broad reads
    |-- PhoneAccessService Accessibility tree + explicitly approved screenshot capture and one-shot visual tap (API 30+)
    |-- WakeWordListener on-device SpeechRecognizer → wake phrase only while Activity is foreground
    |-- VoiceRecorder AudioRecord → VAD-bounded WAV in memory
    |-- Android pickers and settings
    `-- HTTPS 9Router /chat/completions → upstream model
    |
    `→ window.onNativeResponse({id, result, error})
```

## Source ownership
- `MainActivity.java`: WebView setup, bridge validation/dispatch, worker queue, native confirmation dialogs, private file operations, Keystore credential encryption, settings, network client, pickers and recorder integration.
- `SharedFolder.java`: resolve relative paths under a persisted document tree; list/read/write via document provider. Provider writes may truncate/partially fail.
- `PhoneFiles.java`: discovers shared-volume roots, canonical-path containment, paginated listings and strict UTF-8 file reads. No mutation API.
- `PhoneAccessService.java`: active-window snapshots, node cache, epoch IDs, fingerprints, ancestor clicks, action execution, package/password filtering, per-action approvals, on-demand screenshot capture and one-shot `dispatchGesture` taps. Screenshot tokens are random, one-use and expire after 20 seconds; they are tied to the active package/window and bounds. Image coordinates are mapped into screen space and clipped to the active app window.
- `WakeWordListener.java`: Android 12+ on-device Persian speech recognition for “Pocket Code” / “پاکت کد”; no cloud wake-word fallback; Activity owns its foreground lifecycle.
- `VoiceRecorder.java`: 16 kHz mono PCM AudioRecord, bounded manual recording and silence-ended wake-command recording, WAV serialization, cancellation.
- `AndroidManifest.xml`, `res/xml/phone_access.xml`: permissions, service declaration and capabilities.
- `assets/agent-reliability.js`: pure policy module for UI-action verification requirements, expected outcomes, failure categories and safe retry classification. Shared by WebView runtime and Node unit tests.
- `assets/agent-task-memory.js`: bounded task-plan lifecycle, stable step IDs, step transitions, pause/resume, safe restoration and compact state summaries. Shared by WebView runtime and Node unit tests.
- `assets/agent-tool-batching.js`: conservative classifier for independent shared-storage read batches. It returns stable tool-call indices and prevents batching when a task plan or UI verification checkpoint is active.
- `assets/agent-execution-timeline.js`: pure bounded event-history module. It stores only tool identifiers, timestamps, durations, allowlisted states and static note codes; it restores persisted data through an allowlist and is shared with Node tests.
- `assets/app.js`: session persistence, rendering, async bridge promises, model tool loop, file browsers, voice/Access controls, mandatory post-action readback, bounded recovery, per-session task planning/memory and execution-timeline updates.
- `assets/index.html`, `styles.css`: dark Persian-first UI, dialogs, task-progress panel and collapsible per-chat execution timeline.

## Tool loop
The UI sends OpenAI-format messages plus function schemas to native action `gemini` (legacy name retained). Native code sends `/chat/completions` using saved router credentials, with streaming disabled. A model response may request tools. Up to three independent, consented broad-storage reads may execute sequentially as a batch; mixed or otherwise non-batchable calls retain first-tool-only behavior. Tool results are appended with `tool_call_id`. Up to 24 model/tool iterations execute per user message. Completion/error restores the composer. Every UI-changing action (`open_app`, `click_element`, `tap_screen`, `type_text`, `scroll_element`, `phone_back`) creates a runtime verification checkpoint. Before another UI action or a final response, the loop accepts only `read_screen` or `capture_screen`; the fresh observation is attached to the tool result with the action ID and expected outcome. The model must compare that observation with the goal. Native `ok=true` means only that Android accepted the request, not that the intended result occurred. If observation is denied or fails twice, the checkpoint is marked unverified. Denied/blocked verification cannot be bypassed with another UI action.

Multi-step tasks use local-only tools `set_task_plan`, `update_task_step`, `resume_task`, `clear_task_plan` and `finish_task`. Plans contain 2–10 bounded outcome steps, per-step statuses and short operational notes; the plan is persisted in the active chat session's localStorage record. A paused plan cannot run phone/file tools until the user clearly requests continuation, the current screen is freshly read, and `resume_task` succeeds. A running step is retained across interruption so the Agent inspects current state before retrying. The task panel displays progress and can clear the saved plan; `clear_task_plan` lets the Agent discard an unrelated paused plan without touching device data. The model receives the structured plan snapshot plus only compact prior user/assistant dialogue; full tool-call history is retained for the current run but excluded from later requests.

Tool batching is limited to up to three independent `phone_storage_roots` / `read_phone_storage` calls from one model response, and only when no active/paused task plan or pending UI verification exists. The WebView executes each call sequentially through the existing single-threaded native worker. The combined batch file-text output is capped at 120,000 characters (48,000 per file), with explicit truncation metadata. Mixed/unsafe tool responses execute only the first call; the rest are returned as not executed. No writes, UI actions, screenshots or approval-gated operations are batched.

Execution timeline entries are persisted with the active chat session in WebView localStorage and capped at 100 events per chat. Each event contains only a tool name, start/end timestamps, duration, a bounded state and a static note code. Raw arguments, full or partial paths, file contents, typed text, screenshots, model/user message text and raw error messages are not recorded. An accepted UI action remains awaiting_verification; a fresh observation moves it to observed, which means only that the screen was read again, not that the intended outcome was confirmed. The model's private reasoning is never logged.

Recovery policy classifies user denial, blocked target, missing permission, stale UI, unavailable element, transient failure and unknown outcome. Only transient failures from idempotent broad-storage reads may be retried once. UI actions and file mutations are never automatically replayed; ambiguous side effects require observation before any retry.

Task planning tools are handled inside the WebView and never cross the native bridge. Plan data is bounded and excludes screenshots, raw tool payloads and file contents; notes are limited to short operational summaries.

Tool groups: workspace `list_files/read_file/write_file`; selected folder `list_phone_files/read_phone_file/write_phone_file`; broad storage `phone_storage_roots/list_phone_storage/read_phone_storage`; phone `list_apps/open_app/read_screen/capture_screen/tap_screen/click_element/type_text/scroll_element/phone_back`. `capture_screen` is the approved visual fallback when Accessibility text is insufficient. `tap_screen` allows exactly one coordinate tap from that exact image, requires a separate native approval even in automatic mode, and has no swipe/drag/long-press support.

## Accessibility action identity
Snapshots collect bounded visible, non-password node text and return IDs scoped to an epoch. Actions verify package, refresh node, compare fingerprint and invalidate IDs afterward. `click_element` searches up to six levels for an enabled clickable ancestor; effective clickability is reported separately from direct clickability. This addresses text-in-clickable-row patterns but not inaccessible canvas controls. Known sensitive/system package names are blocked heuristically.

## Storage and state
- Private workspace: `getFilesDir()/workspace`.
- Preferences: `secure_settings`; encrypted API key plus model/base URL, SAF tree, broad-read consent and automatic-navigation flag.
- Credential encryption: Android Keystore AES/GCM; imported plaintext JSON is not automatically removed from Downloads.
- Conversations: WebView localStorage, app sandbox, not separately encrypted.
- Voice: memory only; lifecycle pause discards active capture; no audio history. Wake phrase recognition is on-device and only active while the Activity is foreground. Post-wake audio is sent to the configured Gemini model through 9Router; its transcript enters the same agent send/tool loop.

## Limits and lifecycle
One worker executor handles bridge work. HTTP timeouts are 60 seconds. Request limit is 4 MB; response limit 4 MB; file text limit 512 KB. Wake listening and capture stop when the Activity is paused; no foreground service or background wake guarantee is provided. Accessibility uses a main-thread handler and native overlay prompts. Screenshot capture is API 30+, uses `takeScreenshot` on API 30–33 and `takeScreenshotOfWindow` on API 34+, rejects known blocked packages before and after approval, compresses to bounded JPEG in memory, and never writes the image to disk. Screenshot permission is per request even when automatic navigation is on. `tap_screen` uses `dispatchGesture` only after a per-tap approval; its token is one-use/20-second, must match the same package/window/bounds, and coordinates outside the active app window are rejected. The service declares `canPerformGestures`; no gesture batching or background action queue exists. Stop clears cached nodes and ends the current JS loop but cannot reverse completed actions.
