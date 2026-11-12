
# Verification log

## v0.19.0 candidate / idempotent action execution

- Side-effecting Agent tool calls receive an operation ID scoped to chat, current send/run and provider tool-call ID. Native Android commits a receipt before dispatch and suppresses duplicate IDs even after completion; reusing an ID for a different native action is rejected.
- Started operations become unknown after process interruption. Completed/unknown receipts block later side effects until the Agent inspects the matching screen or file surface and reconciles the outcome as applied, not_applied or uncertain.
- The bounded app-private journal stores operation IDs, native action names, states, timestamps and optional resolution only. It does not store tool arguments, paths, typed text, messages or screen content. Terminal resolved/failed records are pruned before unresolved records; a journal full of unresolved receipts fails closed.
- Added JavaScript tests for required receipts, matching observation surfaces and duplicate classification, plus JVM tests for covered action types, ID validation, action conflicts, known preflight failures and process-interruption recovery.
- The exact semantic match between the intended target and the observation is partly enforced by the JS tool gate and Agent interpretation; Native does not independently compare a stored path, recipient or typed value. This remains a limitation for final device validation.
- **No tests, lint, build, GitHub Actions or phone validation were run before the requested validation cycle.** The previous checked-in APK remains v0.5.0 until a new artifact is successfully produced.

