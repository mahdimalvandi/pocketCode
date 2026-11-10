# Security and privacy model

## Trust boundaries
The model and content from screens/files/audio are untrusted. Native Java is the authorization boundary; a prompt saying “ask permission” is not enforcement. The WebView only renders bundled UI, not imported/generated HTML as executable content; networking is handled natively. Never add arbitrary navigation or expose the bridge to remote documents without a new threat review.

## Data leaving the device
Chat text, tool-returned filenames/content, approved or automatically read screen text, approved screen images and submitted command audio go to the configured 9Router and its upstream provider. `capture_screen` is never automatic: a native confirmation appears for every image, including when automatic navigation is enabled. The image is a bounded JPEG held in memory and attached to model requests only during the current Agent run; it is not written to disk or conversation localStorage. On Android 14+ the active window is captured; on Android 11–13 the display is captured and may include other visible UI such as notifications. Known system/security/financial packages are blocked heuristically, but package-name filtering is not exhaustive and FLAG_SECURE detection is not guaranteed. Review the screen before approving. Wake-phrase detection uses Android's on-device SpeechRecognizer (Android 12+); it does not send wake-listening audio to 9Router. Audio recorded after the wake phrase is sent to Gemini for transcription, then its transcript is submitted to the agent loop. No claim is made about upstream retention. Chat text/history is local but unencrypted within the app sandbox; screenshot bytes are excluded from it.

## Authorization matrix
| Operation | Enforcement |
|---|---|
| Broad storage read | Saved consent AND current OS permission checked natively |
| Workspace / SAF read or app listing/open | Native approval unless automatic navigation opted in |
| Ordinary Accessibility actions | Overlay approval unless automatic navigation opted in |
| Sensitive click | Model `sensitive` metadata plus native label/view-ID checks; unknown targets prompt |
| File write/delete | Native per-operation confirmation |
| Enable permissions | User manually operates Android settings; app cannot self-grant |
| Audio | OS microphone permission; user starts dictation or explicitly enables foreground wake mode. Wake phrase is on-device; post-wake command audio is sent to 9Router and auto-submitted to the agent. |
| Screen image | Every `capture_screen` call requires native per-request approval, even in automatic mode; known blocked packages rejected; image attached in memory to the current Agent run only and not saved locally. |

All-files permission itself grants writing; application read-only behavior is not OS-enforced. SAF grants can include write permission but handlers still ask. Automatic classification cannot guarantee prevention of all unintended sends/deletions. Use only harmless test apps/data until physical-device review is complete.

## Known weaknesses
- Sensitive-app package and button-label rules are heuristics; custom controls, translations and misleading labels can evade them or trigger false positives. Screenshot blocking uses the same package-name heuristics and is not a complete sensitive-screen classifier.
- Password-marked nodes are omitted, but not every secret is marked as a password. Complete FLAG_SECURE/sensitive-content detection is not implemented; Android may return a black/rejected screenshot for secure windows, but no guarantee is made.
- Accessibility targets can change between observations; fingerprints/package checks reduce but do not eliminate UI races.
- Model-provided sensitivity flags are not trustworthy by themselves.
- SAF overwrite is not guaranteed atomic and has no undo. Back up important files.
- Microphone lifecycle is compiled but not phone-tested; cancellation/pause races need instrumentation tests.
- Silent/noisy audio can produce hallucinated transcripts; a synthetic-silence test did so. Always review text.
- Router credentials are encrypted at rest, not a defense against a compromised/rooted device or malicious app process.

## Secret handling / publication
Never commit `.debug-signing.keystore`, `*.jks`, `*.keystore`, `local.properties`, `.env*`, private connection JSON, captured production traffic or user conversations. `dist/private-9router-connection.json` may exist locally; it is not distributable. Source archives exclude the entire `dist` directory and signing material. Scan the staged Git tree and APK contents before pushing. Do not print secret values in tool output.

The APK contains a public default router endpoint/model alias, not an embedded API key. A signing certificate in the APK is public; the private keystore is not. Publishing this debug APK is acceptable only as a personal-testing artifact, not as a promise of production security. No license is inferred; owner must choose one before licensing reuse.
