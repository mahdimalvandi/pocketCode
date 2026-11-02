# Security and privacy model

## Trust boundaries
The model and content from screens/files/audio are untrusted. Native Java is the authorization boundary; a prompt saying “ask permission” is not enforcement. The WebView only renders bundled UI, not imported/generated HTML as executable content; networking is handled natively. Never add arbitrary navigation or expose the bridge to remote documents without a new threat review.

## Data leaving the device
Chat text, tool-returned filenames/content, approved or automatically read screen text and user-submitted recorded audio go to the configured 9Router and its upstream provider. No claim is made about upstream retention. Chat history is local but unencrypted within the app sandbox. Android speech recognition code exists historically, but the current mic UI uses Gemini audio and has no silent recognizer fallback.

## Authorization matrix
| Operation | Enforcement |
|---|---|
| Broad storage read | Saved consent AND current OS permission checked natively |
| Workspace / SAF read or app listing/open | Native approval unless automatic navigation opted in |
| Ordinary Accessibility actions | Overlay approval unless automatic navigation opted in |
| Sensitive click | Model `sensitive` metadata plus native label/view-ID checks; unknown targets prompt |
| File write/delete | Native per-operation confirmation |
| Enable permissions | User manually operates Android settings; app cannot self-grant |
| Audio | OS microphone permission + user starts recording and submits transcription |

All-files permission itself grants writing; application read-only behavior is not OS-enforced. SAF grants can include write permission but handlers still ask. Automatic classification cannot guarantee prevention of all unintended sends/deletions. Use only harmless test apps/data until physical-device review is complete.

## Known weaknesses
- Sensitive-app package and button-label rules are heuristics; custom controls, translations and misleading labels can evade them or trigger false positives.
- Password-marked nodes are omitted, but not every secret is marked as a password. Complete FLAG_SECURE/sensitive-content detection is not implemented.
- Accessibility targets can change between observations; fingerprints/package checks reduce but do not eliminate UI races.
- Model-provided sensitivity flags are not trustworthy by themselves.
- SAF overwrite is not guaranteed atomic and has no undo. Back up important files.
- Microphone lifecycle is compiled but not phone-tested; cancellation/pause races need instrumentation tests.
- Silent/noisy audio can produce hallucinated transcripts; a synthetic-silence test did so. Always review text.
- Router credentials are encrypted at rest, not a defense against a compromised/rooted device or malicious app process.

## Secret handling / publication
Never commit `.debug-signing.keystore`, `*.jks`, `*.keystore`, `local.properties`, `.env*`, private connection JSON, captured production traffic or user conversations. `dist/private-9router-connection.json` may exist locally; it is not distributable. Source archives exclude the entire `dist` directory and signing material. Scan the staged Git tree and APK contents before pushing. Do not print secret values in tool output.

The APK contains a public default router endpoint/model alias, not an embedded API key. A signing certificate in the APK is public; the private keystore is not. Publishing this debug APK is acceptable only as a personal-testing artifact, not as a promise of production security. No license is inferred; owner must choose one before licensing reuse.
