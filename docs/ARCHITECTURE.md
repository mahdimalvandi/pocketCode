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
- `assets/app.js`: session persistence, rendering, async bridge promises, model tool loop, file browsers, voice/Access controls, mandatory post-action readback and bounded recovery.
- `assets/index.html`, `styles.css`: dark Persian-first UI and dialogs.

## Tool loop
The UI sends OpenAI-format messages plus function schemas to native action `gemini` (legacy name retained). Native code sends `/chat/completions` using saved router credentials, with streaming disabled. A model response may request tools. Only the first tool in a model turn executes; additional calls get an error. Tool results are appended with `tool_call_id`. Up to 24 model/tool iterations execute per user message. Completion/error restores the composer. Every UI-changing action (`open_app`, `click_element`, `tap_screen`, `type_text`, `scroll_element`, `phone_back`) creates a runtime verification checkpoint. Before another UI action or a final response, the loop accepts only `read_screen` or `capture_screen`; the fresh observation is attached to the tool result with the action ID and expected outcome. The model must compare that observation with the goal. Native `ok=true` means only that Android accepted the request, not that the intended result occurred. If observation is denied or fails twice, the checkpoint is marked unverified. Denied/blocked verification cannot be bypassed with another UI action.

Recovery policy classifies user denial, blocked target, missing permission, stale UI, unavailable element, transient failure and unknown outcome. Only transient failures from idempotent broad-storage reads may be retried once. UI actions and file mutations are never automatically replayed; ambiguous side effects require observation before any retry.

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
