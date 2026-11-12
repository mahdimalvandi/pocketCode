# Agent handoff / next work

## Start here
Current main includes source changes through v0.18.0: persistent stop control, Accessibility/action-safety, voice-wake/transcription hardening, exact storage-root navigation, release provenance and per-app permission policies. The v0.19.0 candidate adds durable native operation receipts and stable IDs for side-effecting tool calls. v0.18.0/v0.19.0 source changes remain unbuilt, untested and device-unverified; the checked-in APK is still v0.5.0.

## Delivered and verified
- APK compiled/signed with the original local personal debug key.
- Chrome mobile UI inspected; mock automatic-mode, audio draft/no-auto-send, cancel and storage flows passed.
- Real 9Router chat/tool calls previously returned 200. A synthetic WAV audio request returned 200 with browser-compatible User-Agent; silence hallucinated text.
- No phone/emulator was available during implementation. User has installed earlier builds and enabled Accessibility, but the v0.5 behavior is not fully device-verified.

## Historical candidate: 0.7.0 visual screen understanding
- Added an explicitly approved `capture_screen` fallback when the Accessibility tree cannot represent the UI. It sends a bounded JPEG as multimodal image input to the configured 9Router model, only for the current Agent run and without local persistence. No coordinate gestures were added.
- Still requires batch build/lint and physical-device tests. Android 14+ active-window capture and Android 11–13 display capture need separate verification; blocked-package heuristics are not exhaustive and FLAG_SECURE behavior is not guaranteed. Confirm that the selected 9Router model supports `image_url` content.

## Historical candidate: 0.8.0 one-shot visual tap
- Added `tap_screen`: exactly one coordinate tap using x/y pixels and `screenshotId` from the latest approved capture. Each tap prompts separately even in automatic mode; token is one-use, 20-second and bound to the same app package/window/bounds. Image-to-screen mapping clips to the active app window. No swipe/drag/long-press exists.
- Unit tests were authored for coordinate mapping and sensitive-label filtering but have not been run. The source is not built/device-tested. Next batch should validate API 24+ dispatch on real OEM devices, coordinate mapping under rotation and safe rejection of app/window changes. The sensitive-action deny-list is heuristic only.

## Historical candidate: 0.9.0 action verification and recovery
- Added a runtime-enforced checkpoint after `open_app`, `click_element`, `tap_screen`, `type_text`, `scroll_element` and `phone_back`. Before the model can call another UI tool or return a final answer, it must obtain a fresh `read_screen` or `capture_screen` observation. The expected postcondition and a per-action ID accompany that observation.
- The model compares the fresh screen with the expected result; native `ok=true` is explicitly not treated as proof of success. Typed field contents are deliberately omitted from verification summaries.
- Added recovery categories for denial, blocked target, missing permission, stale screen/element, unavailable target, transient and unknown failures. A transient failure may be retried once only for idempotent broad-storage reads. UI actions and writes are never automatically replayed.
- A denied or blocked readback prevents other UI actions from bypassing the stop. If observation fails twice, the action remains unverified and cannot be repeated with the same tool.
- Added Node built-in tests for the reliability policy. **Not built or tested in this task; GitHub Actions remains manual-only.**

## Historical candidate: 0.10.0 task planning and memory
- Added local task tools `set_task_plan`, `update_task_step`, `resume_task`, `clear_task_plan` and `finish_task`. Plans contain 2–10 outcome steps and stable IDs, with bounded status/note fields.
- The active plan is saved in the current chat session and shown in a compact progress panel. Switching chats restores the matching plan; the user can clear it per chat.
- A plan left active when a send is interrupted is paused on the next send. Phone/file work is blocked until a fresh screen observation and explicit `resume_task` call. An interrupted running step stays identifiable so it can be inspected before retry.
- Prior-turn tool-call payloads are trimmed from future model requests; the current run keeps its tool-call pairs and receives a compact task snapshot plus recent user/assistant dialogue.
- Added Node tests for bounded plan lifecycle, ordered steps and safe resume. **Not built or tested in this task; no GitHub Actions run.**

## Merged candidate: 0.11.0 tool batching
- Added a conservative scheduler for up to three independent broad shared-storage reads (`phone_storage_roots`, `read_phone_storage`) from one model response. Calls are executed sequentially on the existing single-threaded Android worker; the benefit is fewer model round-trips, not parallel native I/O.
- Mixed or non-batchable tool responses keep first-tool-only behavior. Task plans, pending UI verification, stop state, approval-gated operations and mutations disable batching.
- Batch file text is bounded to 48,000 characters per file and 120,000 characters total. Truncation metadata is returned; single read calls are unchanged.
- Added scheduler tests; not run. Build/CI remains deferred to the user's final batched test cycle.


## Current candidate: 0.13.0 persistent stop control
- Added a stop button in the chat header, visible only while an Agent run is active. Stop state is updated immediately in the WebView and checked before later model/tool operations, including subsequent entries in a batch.
- Stop also requests that the existing native automatic-navigation setting be disabled. The native bridge uses a serial worker, so that request may wait behind the in-flight call. An already-started model HTTP request or Android action may finish and is not guaranteed to be interrupted or rolled back.
- Active task plans are paused and unverified UI outcomes remain unverified. A subsequent user turn gets a fresh stop state; automatic navigation remains off until manually re-enabled.
- Added a pure stop-policy module and Node tests. Not run, not built and not device-tested by request; no Actions run.

## Merged candidate: 0.12.0 execution timeline
- Added a per-chat collapsible timeline of tool-call metadata: tool label, start time, elapsed duration, bounded status and static note code. The history is capped at 100 events per chat.
- Logs only static tool identifiers and execution metadata. It deliberately excludes arguments, paths, file contents, typed text, screenshots, chat contents, private model reasoning and raw error strings. Persisted records are restored through an allowlist.
- The in-app workspace-file confirmation is reflected as awaiting approval. Native Android confirmation dialogs still appear as a running bridge operation until the response returns. UI-changing operations remain awaiting verification after the native request returns; a fresh screen observation changes the matching operation to observed, which is explicitly not the same as success. Failed/denied/unverified/deferred states remain distinguishable.
- Added Node tests for bounded history, allowlist restoration, durations and status classification. Not run, not built and not device-tested by request; no GitHub Actions run. The final combined test cycle should include the mock scenarios in docs/TESTING.md.

## Highest-priority follow-up
1. **Actual Telegram reproduction:** the candidate now searches up to sixteen clickable ancestors and returns target class/depth metadata, but this does not prove the original Telegram failure is fixed. Capture a redacted Accessibility tree and exact error on a physical device; do not use blind coordinate taps as a workaround.
2. **Sensitive-action safety:** native classification now covers common English/Persian side-effect semantics across text, descriptions, IDs, hints, classes and nearby ancestors, and approval previews display intended target plus draft/replacement text. It remains heuristic; validate Telegram and other apps, especially custom icon-only controls and false negatives.
3. **Voice wake mode:** code now handles expected no-match/timeouts, bounds recognizer retries and prevents concurrent manual recording/wake recognition. Validate physical Android 12+ devices with offline Persian speech installed: false positives, mic handoff, silence detection, background pause/resume and manual-dictation coexistence. The feature is foreground-only, not an always-on background assistant.
4. **Real Persian speech:** the WAV header is validated, string/text-part response shapes are accepted and empty/malformed transcripts produce explicit errors. Verify real Persian accuracy and 9Router compatibility on-device with clean, noisy and silent recordings. No generic ASR fallback is used.
5. **Prompt/disclosure consistency:** the authoritative rule is now aligned in the model prompt, tool descriptions, native onboarding and Accessibility service text. Verify disclosures match actual native behavior; do not weaken native enforcement.
6. **Stop UX:** v0.13.0 adds a persistent in-run control and operation-boundary gating. Feature implementation is merged; validation only remains. It cannot reliably interrupt an in-flight native operation or roll back side effects.
7. **Storage navigation:** code now returns exact current/root/parent paths and prevents traversal above the matched volume root. Validate internal storage, removable SD/USB volumes, empty directories, pagination, revoked permission, symlinks and boundary/traversal paths on physical devices.
8. **Tests and release engineering:** JVM unit cases now cover the new classifier, wake matcher/WAV validation and storage-root navigator; the workflow remains manual-only and emits verified SHA-256/provenance artifacts. Still run the full JS/JVM/lint/build/device matrix once at the end, compare the personal APK signing certificate before upgrading, and make an owner decision on the repository license. No instrumentation tests or production signing identity have been introduced. CI signer differs from the personal installed signer.

## Known constraints to preserve
No root/private-app filesystem access, direct Telegram API, shell/runtime or robust background service. Broad adapter stays read-only; selected-folder writes keep native approval. File text reader supports strict UTF-8 ≤512 KB; PDF/image parsing is not implemented. Model replies are not streamed. No sensitive data should enter Git history.

## Suggested next-agent brief
> Read AGENTS.md and all docs/ files, then VERIFICATION.md. Inspect the current implementation before edits. Preserve the existing router and authorization boundaries. Reproduce the requested issue with evidence; do not claim real-device success from Chrome mocks. Build the APK, test the UI in Chrome, update verification and explain any remaining physical-device tests.
