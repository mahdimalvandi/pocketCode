# Pocket Code · Product and engineering roadmap

This roadmap separates implemented source work from capabilities that still need implementation and device validation. A source change or a unit-test file is not evidence that a feature works on a real phone.

## Current work

- [x] **Per-app permission policy (v0.18.0 source candidate):** native package-scoped enforcement with read-only, navigation, confirm-actions and blocked modes. New apps default to confirm-actions. Real-device behavior is still unverified; the v0.18.0 source has not been built or tested.
- [x] **Idempotent action execution (v0.19.0 source candidate):** stable chat/run/tool-call IDs, durable Native receipts, duplicate suppression, interruption recovery and observation-based reconciliation. Source and test cases are written but remain unbuilt, untested and unverified on a phone.

## Next capabilities

### 1. Persistent personal memory

**Goal:** Pocket Code should build a useful, editable understanding of the user across separate conversations, rather than treating each new chat as a blank slate.

**Memory categories**
- User profile: skills, interests, goals, constraints, communication preferences and stable context the user shares.
- People and entities: a person, company, project, product or concept that has already been researched; keep a short identity-resolving summary and source references so the Agent does not repeat the same discovery work.
- Projects and assets: what the user owns, is building, has completed, current state, important files/links and unresolved decisions.
- Events and decisions: meaningful actions, outcomes, preferences and decisions, with dates where useful.

**Required behavior**
- Persist across new chats and app restarts; this is distinct from the existing per-chat task-plan memory.
- Retrieve only relevant memory before answering or planning. Do not attach the full profile to every request.
- Support explicit commands such as “remember this”, “correct that” and “forget this”, plus a UI to inspect, edit, pin and delete stored memories.
- Offer an optional automatic durable-fact capture mode. When enabled, extract stable user context, project changes and researched-entity notes from conversations, but ignore ephemeral chatter, raw screen/file dumps and secrets. Show what was saved and provide immediate undo; keep this mode off until the user explicitly enables it.
- Store provenance (user-stated, externally researched or inferred), confidence, created/updated/last-verified timestamps and optional expiry. Never present an inference as a user-confirmed fact.
- For researched people/entities, keep identity disambiguation, concise notes, source URLs and lookup date. When the user asks about a person or entity, the Agent should reuse the saved identity/summary instead of repeating discovery; refresh sources when freshness materially matters. A cached note must not be treated as current news.
- Avoid saving secrets or sensitive personal data by default. Make storage transparent and user-controllable; a memory write should not silently turn an uncertain model guess into a permanent fact.
- Encrypt persistent records with Android Keystore-backed encryption. Keep them out of WebView localStorage and ordinary unencrypted chat history. Provide per-item deletion and a complete memory wipe.

**Suggested implementation**
1. Define a versioned memory schema and consent/retention rules.
2. Add a native encrypted SQLite repository and deterministic CRUD/search APIs.
3. Add explicit save/correct/forget tools and a user-facing memory manager.
4. Start with bounded keyword matching over a small set of records decrypted transiently in memory. Do not persist a plaintext FTS index beside encrypted memory. Consider a separately protected search index or embeddings only if real usage justifies the added complexity.
5. Add model instructions for provenance, relevance, conflict resolution and freshness checks.

**Acceptance criteria**
- A fact explicitly saved in one chat is retrieved in a fresh chat after app restart.
- A cached person/entity can be recognized without repeating the initial lookup, and the Agent can cite stored source links or refresh when needed.
- User corrections replace stale values without creating contradictory duplicates.
- A deleted item is not retrieved again; complete wipe removes all personal-memory records and search indexes.
- Sensitive/secret values are not persisted by default; user can inspect what has been saved.
- Automatic capture is disabled by default, can be enabled/disabled at any time, and saved items can be undone/deleted from the memory manager.

### 2. Secure chat history and retention

Move chat persistence out of plain WebView localStorage into a native encrypted store. Add per-chat deletion, clear-all, retention limits and an explicit distinction between chat history, execution timeline, task plans and long-term personal memory. Avoid retaining private screenshots or raw phone content in diagnostics.

### 3. Structured UI postcondition verification

Supplement model judgment with deterministic checks for common outcomes (correct app/package, recipient/header, composer content, dialog state and visible success/error indicators). Do not claim full semantic verification; expose verified, failed or unknown with the evidence used.

### 4. Privacy-safe diagnostics

Add an explicit user-approved diagnostic export containing app version, Android version, target package, action name, failure category, control class/ancestor depth and operation IDs. Exclude message text, screen content, filenames, credentials and screenshots by default; preview the bundle before export.

### 5. Documentation reconciliation

Update stale handoff, security and onboarding statements so documented version, actual source behavior, permission matrix and unverified device limitations remain aligned.
