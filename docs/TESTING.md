# Testing plan and evidence standards

`VERIFICATION.md` records actual results per version. This document defines what to verify; checklist items are not claims that tests passed.

## Automated/local baseline
- `node --check app/src/main/assets/agent-reliability.js`, `node --check app/src/main/assets/agent-task-memory.js`, `node --check app/src/main/assets/agent-tool-batching.js`, `node --check app/src/main/assets/agent-execution-timeline.js`, `node --check app/src/main/assets/agent-run-control.js` and `node --check app/src/main/assets/app.js`.
- `node --test app/src/test/js/agent-reliability.test.js` (verification gating, failure classification and retry safety).
- `node --test app/src/test/js/agent-task-memory.test.js` (plan validation, bounded/sanitized storage, ordered step transitions, completion checks, blocked steps and explicit resume).
- `node --test app/src/test/js/agent-tool-batching.test.js` (batch safety classification, bounds, task/verification gating and mixed-call fallback).
- `node --test app/src/test/js/agent-execution-timeline.test.js` (event bounds, persistence sanitization, duration/status transitions, approval states, UI-verification states and failure classification).
- `node --test app/src/test/js/agent-run-control.test.js` (immediate stop state, idempotent requests, operation-boundary gating and in-flight completion semantics).
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
- After every UI-changing action, reject another UI action or a final response until a fresh `read_screen` / `capture_screen` observation is returned with the same action ID and expected outcome.
- Confirm the Agent compares actual screen evidence against the expected outcome and never treats native `ok=true` as proof of success.
- Test denied and blocked observations, stale element IDs, unavailable controls, permission failures, two failed observation attempts, model refusal to verify, stop during a checkpoint, and the 24-turn limit.
- Verify that transient broad-storage read failures retry at most once; never retry taps, clicks, navigation, writes or other side effects automatically. Denied/blocked verification must prevent UI-action bypass.
- `capture_screen` approval denial must result in zero screenshot upload; approval adds a multimodal data-URL image only to the current `send()` request loop, with no Base64 in session/localStorage/tool-result messages.
- Visual tap: tool schema carries screenshotId and image-space x/y; no action without the exact current token; token is consumed after one attempt; denied/cancelled taps are not retried; tap forces fresh screen verification and clears the old in-memory image.
- Tool batching: only previously-consented broad-storage reads are batched; up to three calls; sequential native execution; stable `tool_call_id` mapping; mixed or approval-gated calls fall back to first-only; task-plan and verification checkpoints disable batches; output text budgets and truncation metadata are honored; stop prevents subsequent calls.
- Task planning/memory: plan only multi-step requests; no plan on simple questions; 2–10 step bounds; generated IDs; pending/running/completed/failed/requires-approval states; progress-panel updates; switching chats restores the matching plan; clear removes only that session's task memory; a paused unrelated plan can be cleared without touching device data.
- Task resume: an interrupted active plan becomes paused; phone/file tools are blocked until a fresh read_screen/capture_screen and explicit resume_task; ensure the previous running step is observed before any retry; new unrelated tasks replace rather than resume the old plan.
- Context compaction: previous-turn tool-call payloads are excluded from new requests while the current run keeps valid tool-call/tool-result pairs and the saved plan snapshot.
- Automatic-navigation on/off status and current-loop stop.
- Storage-root navigation, paginated listing, UTF-8 display and consent revocation.
- Voice start/stop/cancel/error; disabled states; transcript insertion; absolutely no automatic chat submission.
- Empty transcript and reopened recording dialog; permission denied and router failure messages.



Stop-control mock checks:
- Stop button is hidden when idle, visible/enabled while the Agent is busy, and becomes disabled with a stopping label after a stop request.
- A stop request during a 9Router response prevents processing its returned final answer or starting any returned tool calls; a stop request during a tool call allows that call to settle but prevents the next tool, including the next item in a read batch.
- Verify the active task plan is paused, pending UI verification is marked unverified, and no false completion is shown.
- Verify the stop button is keyboard/touch accessible at 393×852 and narrow 360px layouts, and that the normal send/new-chat controls recover after the run exits.
- Confirm the UI notice states that current native work may complete and completed side effects are not rolled back. Re-enabling automatic navigation requires the existing explicit user action.
- Verify the in-app file approval dialog offers `رد و توقف Agent`, and an active native Android confirmation dialog offers its conditional `توقف Agent` action. The native stop event must set the WebView stop flag before the denied-operation response can lead to another tool call.

Execution timeline mock checks:
- Verify one event is created for every executed tool call, including local task tools and each member of a permitted batch; deferred calls are clearly marked not_executed.
- Verify event state changes to awaiting_approval during the in-app file confirmation, returns to running after approval, and ends denied/failed/completed as appropriate.
- UI-changing actions remain awaiting_verification after Android accepts the request; a fresh read_screen/capture_screen changes the matching action event to observed, while denied/missing observations produce unverified. Never label observed as successful completion.
- Switch chats, create a new chat and restore persisted history. Verify a maximum of 100 events per chat, safe handling of malformed localStorage data and no cross-session mixing.
- Inspect localStorage payloads: no tool args, full paths, contents, typed strings, screenshots, chat text, model reasoning or raw error strings may appear in timeline records. Confirm DOM renders all fields via textContent.

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
| Action verification | Checkpoint after open/click/type/scroll/back/tap; runtime gates next UI action/final response; only fresh screen tree/image completes readback; denied/blocked readback freezes UI actions; unverified actions are not reported as successful |
| Recovery engine | User denial is terminal; permission needs user action; blocked target stops; stale IDs require fresh reads; unavailable controls require re-observation; only transient broad-storage reads retry once; no side-effect replay |
| Visual tap | `canPerformGestures` registration; exact x/y mapping on multiple DPIs/rotations; reject status/navigation bars and points outside app bounds; stale/wrong screenshotId, expired token, changed package/window/bounds; sensitive English/Persian label rejection; approval denial; gesture cancelled/refused; verify one tap only and fresh-screen verification |
| Telegram | Open, search contact, click result's ancestor, verify chat header, type draft, cancel send approval, approve disposable send only |
| Sensitive effects | English/Persian send/delete/payment/share/publish labels; resource-ID and hint semantics; parent-container labels; unlabelled control; model false-negative flag; changed target; exact replacement-text preview |
| Stop | Stop during model request, pending overlay and between actions; no next action; no undo claim |
| Audio | Permission denial; 1–60 seconds Persian/English; empty/silent/noisy audio; WAV header validation; malformed/empty/string/text-part router responses; wake false positives and word boundaries; retry backoff/stop; manual-vs-wake microphone ownership; cancel; foreground/background; rotation; timeout; transcript remains review-before-send for manual dictation |
| Lifecycle | Screen lock, app switch, process death; no claim of reliable background automation |

Use disposable files and a harmless chat/account. Do not validate payments with real funds or sensitive accounts. Screenshots/logs can contain private data: redact before committing.

## Reporting a failure
Record app version, Android/OEM, exact task, active package, last tool result/error, whether automatic mode was on, and a redacted screen/tree if useful. Distinguish “model declined to call tool,” “element not exposed,” “native action returned false,” and “action performed but outcome not verified.” A synthetic audio HTTP 200 demonstrates payload acceptance, not faithful transcription.
