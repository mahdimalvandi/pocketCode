# Changelog

## 0.18.0 (candidate)
- Added per-app Accessibility policies: read-only, navigation, confirm-actions and blocked. Native Java enforces the active package policy before and after approvals; unknown/corrupt policy data fails closed, and built-in system/security packages remain denied.
- Added a searchable per-app policy editor in Access settings. New apps default to confirm-actions; clicks, text replacement and visual taps always receive a native approval in that mode, even with global automatic navigation enabled.
- Added JVM unit cases for policy levels, invalid policy values and built-in package restrictions; added device-validation scenarios to docs/TESTING.md.
- Added docs/ROADMAP.md and specified persistent personal memory as the next product capability, including cross-chat recall, researched-entity recognition and user controls.
- **No tests, lint, build or GitHub Actions were run by request.** The checked-in APK remains v0.5.0; no v0.18.0 APK has been generated.

## 0.17.0 (candidate)
- Local SDK builds now restrict the generated debug keystore to the current user and produce a verified SHA-256 checksum plus provenance metadata (package, version, source revision and APK hash).
- The manual-only GitHub Actions workflow now generates and verifies matching checksum/provenance files and uploads them beside the CI APK. The workflow remains `workflow_dispatch` only; no automatic build was triggered.
- Updated release instructions to distinguish personal and CI debug signatures, preserve the private local key, verify artifact hashes and leave the repository license choice to the owner. No `LICENSE` file was added.
- **No build, tests, lint or GitHub Actions were run by request.** The committed APK remains v0.5.0; this candidate does not yet have a generated v0.17.0 APK or checksum.

## 0.16.0 (candidate)
- Broad shared-storage directory listings now return the exact `currentPath`, matched `rootPath`, `isRoot` state and canonical `parentPath`. Parent navigation stops at each volume root, including removable volumes.
- Added a root navigator that chooses the longest valid canonical root match and rejects sibling-prefix and traversal paths outside the selected volume.
- Updated the agent tool description and storage policy to require the returned `parentPath` verbatim rather than deriving parents from path depth or string trimming.
- Added JVM coverage for primary/removable roots, overlapping roots, root boundaries and sibling-prefix escapes. **Not run, not built or device-tested** by request; physical removable-volume permissions and OEM storage behavior remain pending.

## 0.15.0 (candidate)
- Bound on-device wake recognizer recovery: expected no-match/speech-timeout results continue normally, while repeated recognizer/service failures use backoff and stop after eight consecutive failures instead of retrying indefinitely.
- Tightened wake phrase matching to word boundaries, retaining Persian/English spacing variants while rejecting phrases embedded inside longer words.
- Prevented wake detection and manual dictation from competing for the microphone; native manual recording also checks for an in-flight wake transcription.
- Validate the locally recorded PCM16 mono 16 kHz WAV header before sending it to the Gemini-selected 9Router model. Empty/malformed response shapes now produce explicit errors; text string and text-part array responses are handled without a generic ASR fallback.
- Rebuilt the agent prompt/policy literals as safely escaped JavaScript strings after finding a malformed prompt concatenation in v0.14.0.
- Added JVM tests for wake-phrase boundaries and WAV shape. **Not run, not built or device-tested** by request; physical Persian recognition, false positives and real router transcription accuracy remain pending.

## 0.14.0 (candidate)
- Expanded clickable-ancestor resolution from six to sixteen parent levels and returned privacy-safe target class/depth metadata after a click, so deeply nested result rows can be diagnosed without logging message text.
- Approval previews now show the intended click reason, nearby non-password context and the exact replacement text for `type_text`; the preview is narrower and bounded.
- Added native English/Persian sensitive-action classification across labels, resource IDs, hints, classes and up to three ancestors. Model flags can add risk but cannot remove native risk.
- Aligned the model prompt, tool descriptions, Android onboarding and Accessibility disclosure on the automatic-navigation and sensitive-action rules. Detection remains heuristic, not a guarantee.
- Added JVM tests for classifier examples. **Not run or built; physical Telegram reproduction and sensitive-control checks remain pending** for the final device test cycle.

## 0.13.0 (candidate)
- Added a persistent stop button to the chat header while the Agent is running.
- Added a per-run stop state checked at model and tool-operation boundaries; new model/tool operations and later members of a batched read sequence are not started after a stop request.
- Stop also disables the existing automatic-navigation setting. An in-flight 9Router request or Android operation may still finish because native bridge calls are serialized; completed side effects are not rolled back.
- Added explicit stop options to the in-app file approval and active native Android confirmation dialogs so modal prompts do not block access to the stop path.
- Pauses an active task plan and marks any pending UI result unverified. A later user send starts a new run-control state; automatic navigation must be explicitly re-enabled.
- Added a pure run-control module and Node tests. Not run, not built and not device-tested in this task; no GitHub Actions run.

## 0.12.0 (candidate)
- Added a collapsible execution timeline for each chat, capped at 100 events and restored independently with that session.
- Records static tool labels, local start time, elapsed duration and bounded states for running, approval, UI verification, observed, failed, denied, unverified and not-executed operations. Tool calls from a permitted batch receive individual entries.
- In-app workspace-file approvals are shown as awaiting approval. UI-changing actions remain awaiting verification until a fresh screen read/capture is obtained; the resulting state is labelled observed, not successful.
- Timeline records exclude tool arguments, file paths, file contents, typed text, screenshots, chat messages, model reasoning and raw error messages. Persisted event objects are allowlist-restored and use text-only DOM rendering.
- Added a pure timeline module and Node tests. Not run, not built and not device-tested in this task; no GitHub Actions workflow was dispatched.

## 0.11.0 (candidate)
- Added a conservative tool-call scheduler: up to three independent, previously-consented broad shared-storage reads (`phone_storage_roots`, `read_phone_storage`) from one model response may execute in a single model turn.
- Batch calls are executed sequentially through the existing single-threaded native bridge; no parallel Android operation queue or new native permissions were added.
- Batching is disabled during active/paused task plans, pending UI verification, stop state, mixed/approval-gated calls and all UI/file mutation operations. Unsafe mixed responses execute only the first tool; deferred calls receive explicit tool errors.
- Batch text outputs are capped at 48,000 characters per file and 120,000 characters total; truncation is explicitly marked. Individual read calls remain unchanged.
- Added a pure scheduler module and Node tests. Not built or executed in this task.

## 0.10.0 (candidate)
- Added local `set_task_plan`, `update_task_step`, `resume_task`, `clear_task_plan` and `finish_task` tools for multi-step task planning and progress tracking.
- Added a session-scoped structured task memory (goal, ordered steps, statuses and short operational notes) persisted with the current chat. The model receives a compact snapshot rather than the previous turns' full tool-call history.
- Added a visible progress panel with per-step state, completion count and a control to clear the plan memory.
- Interrupted active plans are paused. Resuming requires an explicit user continuation, a fresh screen observation and a `resume_task` call; an unfinished running step is never blindly replayed.
- Added bounded plan sizes (2–10 steps), sanitized/bounded notes, blocked-step handling and final-response checks for unfinished plans. Added 14 Node tests. Not built or executed in this task.

## 0.9.0 (candidate)
- Added a runtime-enforced verification checkpoint after every UI-changing phone action. The Agent must obtain a fresh `read_screen` or approved `capture_screen` observation before another UI action or final answer.
- Each checkpoint records a per-action ID, expected outcome and whether Android accepted the request. The fresh observation is attached to the matching tool result; typed text is deliberately omitted from verification summaries.
- Added recovery categories for user denial, blocked target, missing permission, stale UI IDs, unavailable controls, transient failures and unknown outcomes.
- Only transient failures from idempotent broad-storage reads may be retried once. UI actions, taps and file mutations are never automatically replayed; ambiguous outcomes require observation first.
- A denied or blocked verification cannot be bypassed through another UI action. Added Node built-in policy tests. Not built or device-tested in this task.

## 0.8.0 (candidate)
- Added `tap_screen` for exactly one tap at image-pixel x/y from the latest approved `capture_screen` image.
- Every tap requires a native approval even in automatic-navigation mode. The screenshot ID is one-time, expires after 20 seconds and must match the active package, window ID and bounds.
- Maps image pixels into screen coordinates; rejects coordinates outside the active app window and uses a conservative English/Persian deny-list for obvious sensitive-action labels.
- Requires Accessibility `canPerformGestures`; no swipe, drag, long-press, tap chaining or background gesture execution.
- Added eight JVM unit tests for coordinate mapping and label filtering. Not built or device-tested in this task.

## 0.7.0 (candidate)
- Added `capture_screen` as an on-demand visual fallback when Accessibility text is insufficient. Native approval is required for every capture, even in automatic-navigation mode.
- Captures the active app window on Android 14+ and the display on Android 11–13; known blocked packages are rejected before and after approval.
- Sends a bounded JPEG as OpenAI-compatible `image_url` content through 9Router. Image bytes stay in memory for the current Agent run and are excluded from persisted chat history and disk.
- Does not add coordinate clicking. Requires a vision-capable selected model and Android 11+ Accessibility screenshot support. Physical-device and live-router vision testing remain outstanding.

## 0.6.0 (candidate)
- Added opt-in “Pocket Code” / “پاکت کد” wake phrase using Android 12+ on-device speech recognition while the app is foreground.
- Added silence-ended command capture, Gemini transcription through the existing 9Router connection, and automatic submission to the current agent loop.
- Preserved manual review-before-send dictation and native approvals for sensitive actions; wake listening stops on Activity pause/background.

## 0.5.0
- Opt-in automatic ordinary navigation and stop control; retained native mutation/sensitive-action approvals.
- Clickable-ancestor resolution and effective clickability metadata for text-inside-row patterns.
- Tool-loop budget increased from eight to 24 model turns.
- In-memory 60-second Gemini audio transcription to editable draft; no automatic send.
- Browser-compatible router User-Agent; audio payload compatibility tested, speech accuracy not established.

## 0.4.0
- Broad shared-storage read consent, Android all-files permission, roots discovery and paginated file browser.
- Read-only broad adapter with canonical root containment and strict bounded UTF-8 reading.

## 0.3.0
- SAF selected-folder operations and native mutation confirmations.
- Accessibility screen reading, click/type/scroll/back, app discovery/open and per-step overlays.

## 0.2.0
- Existing 9Router OpenAI-compatible chat/tool integration and encrypted credential import.

## 0.1.0
- Initial standalone Java/WebView Android host, Persian dark UI, workspace, sessions and original direct-Gemini integration.
