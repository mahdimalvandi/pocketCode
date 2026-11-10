# Agent handoff / next work

## Start here
Current feature branch snapshot is v0.6.0 candidate; its CI build and host-side tests pass, but the foreground wake flow still needs physical-device verification. Read root `AGENTS.md` and the other docs before implementing. The owner prefers Persian conversation, an actual standalone Android app, existing 9Router credentials and low-friction ordinary navigation. They explicitly retained approval for sensitive operations, including sending messages and changing files.

## Delivered and verified
- APK compiled/signed with the original local personal debug key.
- Chrome mobile UI inspected; mock automatic-mode, audio draft/no-auto-send, cancel and storage flows passed.
- Real 9Router chat/tool calls previously returned 200. A synthetic WAV audio request returned 200 with browser-compatible User-Agent; silence hallucinated text.
- No phone/emulator was available during implementation. User has installed earlier builds and enabled Accessibility, but the v0.5 behavior is not fully device-verified.

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
