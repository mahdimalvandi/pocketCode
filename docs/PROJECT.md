# Project specification

## Purpose
Pocket Code is a personal Android coding/phone assistant inspired by OpenCode's quiet dark interface. Tools run on the phone. AI inference runs remotely through an existing 9Router endpoint. It is not an official OpenCode client or a full port.

## Current release
0.7.0 source candidate, code 7; Android 8+ (minSdk 26), compile/target SDK 35; SDK-only Java native host and bundled Persian RTL web interface. The checked-in `dist/pocket-code-debug.apk` remains the older 0.5.0 build until the next batched build and verification.

## User requirements and implemented behavior
| Area | Current behavior |
|---|---|
| Chat | OpenAI-compatible nonstreaming chat through configured 9Router |
| Default model | `ag/gemini-3.8-flash-high`; configured provider alias, not a claim about official model naming |
| Workspace | App-private UTF-8 text files; list/read/create/overwrite/delete; mutations require native approval |
| Selected phone folder | Android SAF tree chosen by user; read/list/write; overwrite confirmation; no guaranteed atomic writes |
| Broad phone reading | One-time app consent plus OS all-files/legacy permission; read-only adapter; discover roots, list 200 entries/page, read UTF-8 up to 512 KB |
| App control | Accessibility screen text, fresh element IDs, click, text entry, scroll, back; launcher discovery/open. On-demand screenshot fallback (API 30+) and one-shot `tap_screen` from a fresh screenshot, each requiring native approval |
| Automatic navigation | Opt-in; ordinary operations skip repeated confirmation; sensitive/unknown clicks still prompt. Screenshot capture and every visual tap always prompt separately |
| Voice | Manual Gemini dictation remains review-before-send. Optional Pocket Code wake phrase uses Android on-device Persian recognition while the app is foreground; after a beep, VAD-bounded audio is sent to Gemini and the transcript is auto-submitted to the existing agent loop. |
| Persistence | Chat sessions in WebView localStorage; credentials encrypted via Android Keystore |

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
