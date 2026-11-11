# Project specification

## Purpose
Pocket Code is a personal Android coding/phone assistant inspired by OpenCode's quiet dark interface. Tools run on the phone. AI inference runs remotely through an existing 9Router endpoint. It is not an official OpenCode client or a full port.

## Current release
0.10.0 source candidate, code 10; Android 8+ (minSdk 26), compile/target SDK 35; SDK-only Java native host and bundled Persian RTL web interface. The checked-in `dist/pocket-code-debug.apk` remains the older 0.5.0 build until the next batched build and verification.

## User requirements and implemented behavior
| Area | Current behavior |
|---|---|
| Chat | OpenAI-compatible nonstreaming chat through configured 9Router |
| Default model | `ag/gemini-3.8-flash-high`; configured provider alias, not a claim about official model naming |
| Workspace | App-private UTF-8 text files; list/read/create/overwrite/delete; mutations require native approval |
| Selected phone folder | Android SAF tree chosen by user; read/list/write; overwrite confirmation; no guaranteed atomic writes |
| Broad phone reading | One-time app consent plus OS all-files/legacy permission; read-only adapter; discover roots, list 200 entries/page, read UTF-8 up to 512 KB |
| App control | Accessibility screen text, fresh element IDs, click, text entry, scroll, back; launcher discovery/open. On-demand screenshot fallback (API 30+) and one-shot `tap_screen` from a fresh screenshot, each requiring native approval. Every UI-changing action is followed by a runtime-enforced fresh screen observation before the Agent may continue or claim success |
| Automatic navigation | Opt-in; ordinary operations skip repeated confirmation; sensitive/unknown clicks still prompt. Screenshot capture and every visual tap always prompt separately |
| Voice | Manual Gemini dictation remains review-before-send. Optional Pocket Code wake phrase uses Android on-device Persian recognition while the app is foreground; after a beep, VAD-bounded audio is sent to Gemini and the transcript is auto-submitted to the existing agent loop. |
| Task planning | Local `set_task_plan` / `update_task_step` / `resume_task` / `finish_task` tools; progress panel; 2–10 bounded steps; task state stored per chat session |\n| Persistence | Chat sessions and compact task memory in WebView localStorage; credentials encrypted via Android Keystore |

## Agent execution reliability
Every UI-changing tool action creates a verification checkpoint. The runtime requires a fresh `read_screen` or `capture_screen` observation before another UI action or final response; the model compares that observation with the expected outcome. Android's `ok=true` is treated only as request acceptance. Failures are classified into denial, blocked target, permission, stale UI, unavailable target, transient and unknown categories. Only transient idempotent broad-storage reads may be retried once. UI actions and file mutations are never replayed automatically; ambiguous outcomes must be inspected first. A denied or blocked verification cannot be bypassed by trying a different UI action.

## Task planning and memory
For multi-step requests, the Agent creates a bounded plan before phone/file operations. Each step has a stable ID and one of pending, running, completed, failed or requires-approval states. Paused unrelated plans can be discarded with `clear_task_plan` without affecting device data. Only short operational notes are retained; screenshots, raw tool payloads and file contents are not copied into task memory. The plan persists with its chat session and is visible in a progress panel. If an active task is interrupted, it becomes paused. Continuing requires a clear user request, a fresh screen observation and an explicit resume tool call. Previous-turn tool-call payloads are omitted from future model requests; compact user/assistant dialogue and the structured task snapshot are retained. Simple questions and one-action tasks do not need a plan.

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
