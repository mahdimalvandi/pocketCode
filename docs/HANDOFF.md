# Agent handoff / next work

## Start here
Current main snapshot is v0.10.0 source candidate; the next feature branch develops v0.11.0 tool batching. The 0.6 CI build is historical; the 0.7 screenshot and 0.8 visual-tap additions have not been built or device-tested.

## Delivered and verified
- APK compiled/signed with the original local personal debug key.
- Chrome mobile UI inspected; mock automatic-mode, audio draft/no-auto-send, cancel and storage flows passed.
- Real 9Router chat/tool calls previously returned 200. A synthetic WAV audio request returned 200 with browser-compatible User-Agent; silence hallucinated text.
- No phone/emulator was available during implementation. User has installed earlier builds and enabled Accessibility, but the v0.5 behavior is not fully device-verified.

## Latest candidate: 0.7.0 visual screen understanding
- Added an explicitly approved `capture_screen` fallback when the Accessibility tree cannot represent the UI. It sends a bounded JPEG as multimodal image input to the configured 9Router model, only for the current Agent run and without local persistence. No coordinate gestures were added.
- Still requires batch build/lint and physical-device tests. Android 14+ active-window capture and Android 11–13 display capture need separate verification; blocked-package heuristics are not exhaustive and FLAG_SECURE behavior is not guaranteed. Confirm that the selected 9Router model supports `image_url` content.

## Latest candidate: 0.8.0 one-shot visual tap
- Added `tap_screen`: exactly one coordinate tap using x/y pixels and `screenshotId` from the latest approved capture. Each tap prompts separately even in automatic mode; token is one-use, 20-second and bound to the same app package/window/bounds. Image-to-screen mapping clips to the active app window. No swipe/drag/long-press exists.
- Unit tests were authored for coordinate mapping and sensitive-label filtering but have not been run. The source is not built/device-tested. Next batch should validate API 24+ dispatch on real OEM devices, coordinate mapping under rotation and safe rejection of app/window changes. The sensitive-action deny-list is heuristic only.

## Latest candidate: 0.9.0 action verification and recovery
- Added a runtime-enforced checkpoint after `open_app`, `click_element`, `tap_screen`, `type_text`, `scroll_element` and `phone_back`. Before the model can call another UI tool or return a final answer, it must obtain a fresh `read_screen` or `capture_screen` observation. The expected postcondition and a per-action ID accompany that observation.
- The model compares the fresh screen with the expected result; native `ok=true` is explicitly not treated as proof of success. Typed field contents are deliberately omitted from verification summaries.
- Added recovery categories for denial, blocked target, missing permission, stale screen/element, unavailable target, transient and unknown failures. A transient failure may be retried once only for idempotent broad-storage reads. UI actions and writes are never automatically replayed.
- A denied or blocked readback prevents other UI actions from bypassing the stop. If observation fails twice, the action remains unverified and cannot be repeated with the same tool.
- Added Node built-in tests for the reliability policy. **Not built or tested in this task; GitHub Actions remains manual-only.**

## Latest candidate: 0.10.0 task planning and memory
- Added local task tools `set_task_plan`, `update_task_step`, `resume_task`, `clear_task_plan` and `finish_task`. Plans contain 2–10 outcome steps and stable IDs, with bounded status/note fields.
- The active plan is saved in the current chat session and shown in a compact progress panel. Switching chats restores the matching plan; the user can clear it per chat.
- A plan left active when a send is interrupted is paused on the next send. Phone/file work is blocked until a fresh screen observation and explicit `resume_task` call. An interrupted running step stays identifiable so it can be inspected before retry.
- Prior-turn tool-call payloads are trimmed from future model requests; the current run keeps its tool-call pairs and receives a compact task snapshot plus recent user/assistant dialogue.
- Added Node tests for bounded plan lifecycle, ordered steps and safe resume. **Not built or tested in this task; no GitHub Actions run.**

## Next candidate: 0.11.0 tool batching
- Added a conservative scheduler for up to three independent broad shared-storage reads (`phone_storage_roots`, `read_phone_storage`) from one model response. Calls are executed sequentially on the existing single-threaded Android worker; the benefit is fewer model round-trips, not parallel native I/O.
- Mixed or non-batchable tool responses keep first-tool-only behavior. Task plans, pending UI verification, stop state, approval-gated operations and mutations disable batching.
- Batch file text is bounded to 48,000 characters per file and 120,000 characters total. Truncation metadata is returned; single read calls are unchanged.
- Added scheduler tests; not run. Build/CI remains deferred to the user's final batched test cycle.

## Highest-priority follow-up
1. **Actual Telegram reproduction:** capture redacted Accessibility tree and error; confirm clickable-ancestor fix. A plausible code defect was fixed, but it is not a confirmed diagnosis. Do not add blind coordinate taps as an easy workaround.
2. **Sensitive-action safety:** test send/delete controls in Telegram and other apps. Native classification currently uses substring heuristics and model flags; ambiguous custom controls can evade them. Stronger app-specific semantics and clearer exact-target/draft previews are desirable.
3. **Voice wake mode:** validate physical Android 12+ devices with offline Persian speech installed; test wake phrase false positives, mic handoff, silence detection, background pause/resume, manual-dictation coexistence and Gemini audio transcription. The wake mode is intentionally foreground-only; do not describe it as an always-on background assistant.
4. **Real Persian speech:** verify router/model audio support and accurate transcription; handle empty/noisy recordings. No generic ASR fallback should silently replace Gemini.
5. **Prompt/disclosure consistency:** older system prompt/tool descriptions and Android Accessibility description retain per-step approval wording; appended navigation policy overrides it. Consolidate into one authoritative policy and update strings, without weakening native enforcement.
6. **Stop UX:** stop lives in Access dialog; a persistent, easy-to-reach stop affordance could improve safety. Do not promise cancellation reverses already dispatched effects.
7. **Storage navigation:** parent navigation currently uses a depth heuristic; track exact discovered roots instead, especially for removable volumes.
8. **Tests and release engineering:** add unit/instrumentation coverage, stable signing/release process, license decision and artifact checksums. CI signer differs from personal installed signer.

## Known constraints to preserve
No root/private-app filesystem access, direct Telegram API, shell/runtime or robust background service. Broad adapter stays read-only; selected-folder writes keep native approval. File text reader supports strict UTF-8 ≤512 KB; PDF/image parsing is not implemented. Model replies are not streamed. No sensitive data should enter Git history.

## Suggested next-agent brief
> Read AGENTS.md and all docs/ files, then VERIFICATION.md. Inspect the current implementation before edits. Preserve the existing router and authorization boundaries. Reproduce the requested issue with evidence; do not claim real-device success from Chrome mocks. Build the APK, test the UI in Chrome, update verification and explain any remaining physical-device tests.
