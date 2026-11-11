# Changelog

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
