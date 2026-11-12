# Project specification

## Purpose
Pocket Code is a personal Android coding/phone assistant inspired by OpenCode's quiet dark interface. Tools run on the phone. AI inference runs remotely through an existing 9Router endpoint. It is not an official OpenCode client or a full port.

## Current release
0.15.0 source candidate, code 15; Android 8+ (minSdk 26), compile/target SDK 35; SDK-only Java native host and bundled Persian RTL web interface. The checked-in `dist/pocket-code-debug.apk` remains the older 0.5.0 build until the next batched build and verification.

## User requirements and implemented behavior
| Area | Current behavior |
|---|---|
| Chat | OpenAI-compatible nonstreaming chat through configured 9Router |
| Default model | `ag/gemini-3.8-flash-high`; configured provider alias, not a claim about official model naming |
| Workspace | App-private UTF-8 text files; list/read/create/overwrite/delete; mutations require native approval |
| Selected phone folder | Android SAF tree chosen by user; read/list/write; overwrite confirmation; no guaranteed atomic writes |
| Broad phone reading | One-time app consent plus OS all-files/legacy permission; read-only adapter; discover roots, list 200 entries/page, read UTF-8 up to 512 KB |
| App control | Accessibility screen text, fresh element IDs, click with up-to-16-level clickable-ancestor resolution, text entry with an exact replacement-text confirmation preview, scroll, back; launcher discovery/open. On-demand screenshot fallback (API 30+) and one-shot `tap_screen` from a fresh screenshot, each requiring native approval. Every UI-changing action is followed by a runtime-enforced fresh screen observation before the Agent may continue or claim success |
| Automatic navigation | Opt-in; ordinary operations skip repeated confirmation only when enabled. Native English/Persian sensitive-action classification checks visible labels, resource IDs, hints, classes and nearby ancestors; sensitive/ambiguous clicks still prompt. This heuristic is not exhaustive. Screenshot capture and every visual tap always prompt separately |
| Voice | Manual Gemini dictation remains review-before-send. Optional Pocket Code wake phrase uses Android on-device Persian recognition while the app is foreground; after a beep, VAD-bounded audio is sent to the selected Gemini model through 9Router and the transcript is submitted to the existing agent loop. Expected no-match/time-out results restart listening; repeated recognizer failures back off and stop after eight consecutive failures. Wake phrase matching uses word boundaries. Manual recording and wake listening share an explicit native microphone guard. Recorded WAV headers and 9Router transcript response shapes are validated; malformed or empty results are surfaced as errors without switching to a different ASR provider. Physical-device accuracy remains unverified. |
| Task planning | Local `set_task_plan` / `update_task_step` / `resume_task` / `finish_task` tools; progress panel; 2–10 bounded steps; task state stored per chat session |
| Persistence | Chat sessions and compact task memory in WebView localStorage; credentials encrypted via Android Keystore |
| Run control | Persistent stop button appears while the Agent is active; it prevents starting later model/tool operations, disables automatic navigation and warns that in-flight work may still finish |

## Agent execution reliability
Every UI-changing tool action creates a verification checkpoint. The runtime requires a fresh `read_screen` or `capture_screen` observation before another UI action or final response; the model compares that observation with the expected outcome. Android's `ok=true` is treated only as request acceptance. Failures are classified into denial, blocked target, permission, stale UI, unavailable target, transient and unknown categories. Only transient idempotent broad-storage reads may be retried once. UI actions and file mutations are never replayed automatically; ambiguous outcomes must be inspected first. A denied or blocked verification cannot be bypassed by trying a different UI action.

## Tool batching
The Agent can receive up to three independent `phone_storage_roots` / `read_phone_storage` calls in one model response and execute them sequentially through the existing native worker, reducing model round-trips without parallel native actions. Batching is disabled for active/paused task plans, UI verification checkpoints, mixed calls, approval-gated operations and all mutations. Batch file text is capped at 48,000 characters per file and 120,000 characters total; truncation is disclosed. Individual reads retain their existing behavior.


## Execution timeline
Each chat has a collapsible local timeline with at most 100 tool-call events. Entries show a static tool label, local start time, elapsed duration, and a constrained status such as running, awaiting approval, awaiting UI verification, observed, failed, denied or not executed. UI mutations are not marked complete just because Android accepted the API request; the event remains pending until a fresh screen observation is captured. An observation is not a semantic success verdict. The timeline intentionally excludes tool arguments, full paths, file contents, typed strings, screenshots, private model reasoning and raw error text. Data stays in that chat's WebView localStorage session and follows the existing chat-storage limitations.

## Task planning and memory
For multi-step requests, the Agent creates a bounded plan before phone/file operations. Each step has a stable ID and one of pending, running, completed, failed or requires-approval states. Paused unrelated plans can be discarded with `clear_task_plan` without affecting device data. Only short operational notes are retained; screenshots, raw tool payloads and file contents are not copied into task memory. The plan persists with its chat session and is visible in a progress panel. If an active task is interrupted, it becomes paused. Continuing requires a clear user request, a fresh screen observation and an explicit resume tool call. Previous-turn tool-call payloads are omitted from future model requests; compact user/assistant dialogue and the structured task snapshot are retained. Simple questions and one-action tasks do not need a plan.


## Stop behavior
The header stop button marks only the current in-memory run as stopped. It blocks additional model/tool starts at execution boundaries and turns off automatic navigation through the existing native action. A request already sent to 9Router or a phone operation already dispatched may finish; it is not guaranteed to be interrupted or reversed. The in-app file approval prompt and active native confirmation dialog also expose a stop option so modal confirmations do not hide the control. An active task plan is paused, and any UI action lacking a fresh readback is left unverified. Starting a later user request creates a fresh run-control state; automatic navigation remains disabled until re-enabled by the user.

## Permission semantics
The user explicitly asked for reads and ordinary clicks without repeated approval but confirmed that sending messages, deleting, paying and changing files should still ask. Automatic navigation therefore has a one-time disclosure, an on/off control and native sensitive-action checks. Sensitive-button detection is heuristic and cannot guarantee that every real-world effect is recognized.

Broad file reading is separate from Accessibility and selected-folder permission. All-files access does not unlock `/data/data`, other apps' private stores or all protected `Android/data` paths. Its OS grant permits writes, although this adapter exposes only reads. Editing original phone files still uses the separately chosen SAF tree.

## Non-goals / unsupported
No shell, root, arbitrary code runtime, package installation, Git engine, MCP runtime, passive background recording, background wake-word service, direct Telegram account API, binary/PDF/image extraction, swipe/drag/long-press gestures or reliable unattended background engine. Visual tapping is limited to one approved coordinate on a fresh screenshot.

## Typical flows
1. Install over existing signed APK; import private router connection via Settings; never paste keys in chat.
2. Enable broad reading in Access and enable Android all-files permission; return and reopen Access to refresh status.
3. Manually enable Accessibility (Android restricted-settings approval may be needed for sideloaded APKs).
4. Opt into automatic navigation; ask for a concrete task. Review sensitive action confirmations.
5. Tap microphone, grant recording permission, start again, speak, finish/transcribe, review and send manually.

Uninstalling removes private workspace/history and keys. Export important data before uninstalling. Debug builds are for personal testing, not a security-audited production product.
