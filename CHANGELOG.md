## 0.12.0 (candidate)
- Added a collapsible execution timeline for each chat, capped at 100 events and restored independently with that session.
- Records static tool labels, local start time, elapsed duration and bounded states for running, approval, UI verification, observed, failed, denied, unverified and not-executed operations. Tool calls from a permitted batch receive individual entries.
- In-app workspace-file approvals are shown as awaiting approval. UI-changing actions remain awaiting verification until a fresh screen read/capture is obtained; the resulting state is labelled observed, not successful.
- Timeline records exclude tool arguments, file paths, file contents, typed text, screenshots, chat messages, model reasoning and raw error messages. Persisted event objects are allowlist-restored and use text-only DOM rendering.
- Added a pure timeline module and Node tests. Not run, not built and not device-tested in this task; no GitHub Actions workflow was dispatched.

