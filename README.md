# Pocket Code · Android alpha

**Current source candidate: 0.6.0 (personal-testing alpha).** CI validation has passed, but the checked-in APK remains the previous 0.5.0 build until physical-device validation. Standalone Android assistant; not an official OpenCode client. AI runs through 9Router, tools run on the phone.

## Documentation / agent onboarding

Start with [`AGENTS.md`](AGENTS.md), then:
- [Project specification](docs/PROJECT.md)
- [Architecture and source map](docs/ARCHITECTURE.md)
- [Security and privacy model](docs/SECURITY.md)
- [Development, build and release](docs/DEVELOPMENT.md)
- [Testing plan](docs/TESTING.md) and [actual verification log](VERIFICATION.md)
- [Next-agent handoff and open issues](docs/HANDOFF.md)
- [Changelog](CHANGELOG.md)

Checked-in personal-testing APK: [`dist/pocket-code-debug.apk`](dist/pocket-code-debug.apk) (v0.5.0). The CI-built v0.6.0 candidate is available as the `pocket-code-debug-apk` artifact in [workflow run 42788994845](https://github.com/mahdimalvandi/pocketCode/actions/runs/42788994845). A CI debug build may not upgrade a locally signed APK because its signer differs; back up app data and credentials before uninstalling/reinstalling. Source does not include credentials or the original private signing key.

The sections below are chronological release notes. **Newer sections supersede older feature/permission descriptions.** Native microphone, Telegram and other cross-app behavior still need physical-device testing. No license has been selected by the owner yet.

## v0.6 candidate: Pocket Code wake phrase → Gemini → Agent

Enable **بیدارباش صوتی Pocket Code** in **دسترسی‌ها** and approve the native disclosure. Grant microphone permission if Android asks. On Android 12+ with an installed offline Persian speech model, the app listens for **“Pocket Code”** or **“پاکت کد”** using Android's on-device speech recognizer. A beep means it heard the phrase; speak the command after the beep. The recorder ends after about 1.6 seconds of silence (maximum 30 seconds), sends the WAV to the configured Gemini model through 9Router, and automatically submits the resulting transcript to the existing agent loop. Native confirmations for file changes and sensitive phone actions remain in force.

This is deliberately **foreground-only**: listening stops when Pocket Code is paused, sent to the background or closed, and resumes when the app returns. It is not an always-on hotword service. Wake detection stays on-device; only the command recorded after the beep goes to 9Router/upstream provider. The device must support Android's on-device recognizer and have an offline Persian model installed; there is no online wake-word fallback. The old manual microphone flow remains available and still requires the user to review and send its transcript.

The v0.6.0 candidate has passed CI compilation, 17 JVM tests, Android Lint, APK signature/metadata checks and archive integrity validation. It is not yet verified on a physical Android device; microphone behavior, offline Persian recognition, Gemini transcription and end-to-end Agent submission remain untested.

## v0.5: automatic navigation, ancestor clicks and Gemini dictation

Install over the previous version. In **دسترسی‌ها**, enable **فعال‌کردن کنترل خودکار** once after reviewing the disclosure. Ordinary file/screen reads, app listing/opening, text entry, scrolling, back and labelled navigation clicks then skip per-step prompts. File writes/deletes retain native confirmation. Message sending, deletion, payment and other sensitive clicks are flagged by the model and native label/view-ID heuristics; unknown/unlabelled click targets also prompt. **This is not a reliable semantic safety boundary:** custom controls, languages and app changes can evade classification. Avoid sensitive apps/screens. The off control stops the current UI tool loop, clears cached nodes and cancels a pending accessibility approval; already completed effects cannot be undone. Accessibility can also be disabled in Android settings.

Clicking now resolves a fresh text element to a clickable ancestor (up to six levels) and reports effective versus direct clickability. This addresses a plausible cause of Telegram search-result failures, not a device-verified diagnosis. No coordinate fallback was added. App/element identity checks remain, failed actions must be verified by a fresh screen read, and the model tool budget is now 24 turns rather than eight.

The composer microphone opens **صدا به متن · Gemini**. Grant microphone permission and tap start again if Android first requests it. Record up to 60 seconds, then tap **پایان و تبدیل به متن**. Audio is 16 kHz mono PCM WAV held in memory, sent via the configured 9Router chat endpoint using `input_audio`, and transcribed by the selected Gemini model. The transcript is appended to the composer and is **never auto-sent**. Cancel discards the recording; leaving the activity stops/discards microphone capture. No silent fallback to Android speech recognition is used. The configured router/model must support audio. Audio may contain private speech and is processed by the router/upstream provider.

A live synthetic-silence WAV request returned HTTP 200, establishing request compatibility only; the model produced text for silence, so empty/noisy recordings can hallucinate. Always review the transcript. Real Persian speech accuracy, phone microphone lifecycle and Telegram actions still require device testing.

## v0.4: broad shared-storage reading

This section supersedes earlier per-read approval requirements **for the new broad-storage tools only**. Install over the existing APK without uninstalling to retain data.

1. Open **دسترسی‌ها → فعال‌کردن خواندن گسترده** and review the native consent dialog.
2. On Android 11+, enable **Allow access to manage all files** for Pocket Code. On Android 8–10, accept the legacy storage read permission instead; older scoped-storage behavior may further limit access.
3. Return to Pocket Code and reopen Access if the status has not refreshed. Use **مرور حافظه گوشی**, or ask the assistant to discover storage roots and read a relevant text file.

After consent and the OS grant, `phone_storage_roots`, `list_phone_storage` and `read_phone_storage` do not prompt per directory/file. No automatic background scan is performed. Directory results are paginated (200 entries); content reading currently supports strict UTF-8 text up to 512 KB, not binary/PDF/image extraction. Internal shared storage and discoverable external volumes are included, subject to device restrictions. Private app storage, system files and Android-protected directories are not unlocked; this is not root access.

**Android's all-files grant is not read-only:** it permits writing as well. The app's broad-storage adapter exposes only list/read operations; read-only behavior is an application-level restriction, not an OS security guarantee. Existing file mutations still require native confirmation and use the private workspace or a separately selected SAF folder; broad access does not add an arbitrary-path write/delete/rename tool. Back up before approving overwrites.

Model-requested names and file contents are sent through 9Router to its upstream model. Grant only if you accept this exposure, including the possibility of sensitive files being read. **قطع خواندن در اپ** removes the app's consent immediately for subsequent broad reads but leaves the OS grant intact. Use **مدیریت مجوز در اندروید** to revoke the OS permission too. Existing selected-folder grants and Accessibility are separate permissions.

The APK compiles and its signatures verify; UI navigation/read/revoke behavior was tested with a mock bridge in Chrome. The actual Android all-files settings flow, storage/card access and device-specific behavior still require physical-device testing.

## v0.3: selected phone folder and approval-gated app actions

Install the new APK over the old version to preserve settings. Open **دسترسی‌ها**:

1. **انتخاب پوشه گوشی** opens Android's folder picker. Only the chosen tree is granted, not the entire filesystem. Android may prohibit some roots and app-private folders. **قطع دسترسی پوشه** releases the grant.
2. Ask the assistant to list/read UTF-8 files in the connected phone folder. Every list/read/write requires native confirmation. Maximum text file size is 512 KB; PDFs, images and binary parsing are not implemented.
3. For app interaction, review the disclosure and manually enable **Pocket Code** in Android Accessibility settings. Accessibility is powerful; only enable if you accept the risks. Disable it after use.
4. Ask to open a non-sensitive app, read visible text, click a named control, type into a non-password field or scroll. Every screen read/action has a native accessibility-overlay approval. The assistant cannot authorize itself. Keep Pocket Code running; it is not a background/foreground-service automation engine and Android can terminate it.

Only the first tool call of a model turn executes; additional calls are rejected so the model must request a fresh step. Element IDs expire after actions; app identity and element fingerprint are rechecked. No coordinate gestures, shell, root, package installation, automatic system permission grants or passive event logging. On-demand screenshot capture is a separate, per-request approved visual fallback; it does not add coordinate clicks. Known system/security/financial package names and password nodes are blocked. **This is a heuristic, not a guarantee of detecting every sensitive screen or every FLAG_SECURE window.** Approved visible text may contain personal information and goes through 9Router to the model provider. Never use on banking, authentication or private screens.

Native confirmations also protect app-private writes/deletes. Existing UI confirmations may appear in addition to native prompts. File lists/reads also ask separately, following the request for confirmation on every operation. Opening an imported file from the shared browser currently loads a copy into the private-workspace editor; editing the original is performed through the explicit `write_phone_file` tool and native approval.

Shared document providers may truncate or partially write on error; writes are **not guaranteed atomic and have no undo**. Back up before approving overwrites. App clicks can have irreversible consequences (sending, deleting, submitting): inspect the package, target and text shown in the approval. Denying a request prevents that operation; closing/disabling the service is the stop mechanism.

**Device testing is still required.** Native classes compile and APK signatures pass, but real Accessibility overlays, provider grants, cross-app execution and lifecycle reliability have not been verified on a phone. Start with a disposable test folder and a harmless notes app, never production data.

## v0.2: 9Router connection

The current build uses OpenAI-compatible **`/chat/completions`**, not Google's direct Gemini API. Default base URL: `https://malvandi.founderos.ir/v1`; model: `ag/gemini-3.8-flash-high`. Authorization uses a Bearer key encrypted in Android Keystore.

Install the updated `dist/pocket-code-debug.apk`. In Settings choose **وارد کردن فایل اتصال همین سیستم**, then select your private connection JSON. This imports the existing router credentials without embedding them in the APK or creating a workspace file. Delete the transferred connection JSON from Downloads after import; it contains a plaintext credential. Do not share it or commit it. Source archives exclude `dist` and signing keys.

The router chat endpoint and model tool calling were tested live successfully (HTTP 200). `/models` returned HTTP 403 in this environment; the app does not depend on model discovery. Actual Android network/Keystore/picker behavior still needs phone testing. The UI bridge action remains named `gemini` for internal compatibility but sends OpenAI chat format.

Old direct-Gemini conversations are retained for viewing. Sending from an old-protocol session starts a fresh compatible conversation.

The original v0.1 description below documents the initial architecture. References to direct Google requests/default `gemini-2.5-flash` are superseded by this section: cloud disclosure is now through **9Router and its upstream provider**.

An installable Android app inspired by the quiet dark interface of OpenCode. **Not an official OpenCode client. No desktop or remote agent required.** Android hosts a packaged HTML/CSS/JS interface in a locked-down WebView; a native Java bridge handles local files, secure credentials, Gemini HTTPS calls and system pickers.

## What works

- Persian-first dark interface, conversation history and model settings.
- Direct Gemini `generateContent` requests from the phone, with function calling.
- Local workspace: list/read/create/edit files. Model-requested writes and cloud disclosure of file contents require explicit approval.
- Import UTF-8 source/text files with Android's document picker; export saved files.
- API key encrypted at rest using Android Keystore AES/GCM. The key is never saved in browser storage.
- Speech-to-text via the installed Android speech recognizer, if available. This is dictation, **not Gemini Live or continuous voice chat**.

## Limits you should know

- Alpha prototype, not a full OpenCode port. **No shell, code execution, package installation, Git, MCP, app control or generated-page preview.**
- Files are in the app-private workspace; other files must be imported. Maximum UTF-8 file size: 512 KB. Uninstalling deletes private files and conversation history. Export important files first.
- Gemini runs in Google's cloud, not offline on your phone. Messages and tool-read file contents are transmitted to Google using your API key. Charges, quota and regional restrictions depend on your Google account.
- Conversations are stored locally in WebView localStorage, unencrypted inside the app sandbox. Device/system speech recognition may use its own online service.
- Requests are not streamed. The app should stay in the foreground during generation. Android terminating the app during a request interrupts it; saved conversation state is restored, but an interrupted tool sequence may need a fresh conversation.
- Default model ID: `gemini-2.5-flash`. Change it to an exact supported model ID in Settings. No invented Gemini version aliases are baked in.
- Debug APK is for personal testing, not a production/security-audited release.

## Install and use

Built personal-testing APK: **`dist/pocket-code-debug.apk`** (Android 8.0+). Compiled using Android SDK 35; APK v2/v3 signatures verified. No physical-device launch has been performed yet.

1. Install the debug APK on Android 8.0+ (API 26+), allowing installation from that source if Android asks.
2. Open **تنظیمات**, enter a Gemini API key and the exact model ID available to your account, then save.
3. Send a message. Try asking it to create `hello.html`. Review and approve the proposed file write.
4. Open **فایل‌ها** to inspect, edit or export the result.

Never paste API keys into chat. Use Settings. No key is included in the source or APK.

## Build

Requirements: JDK 17 or 21, Gradle 8.9, Android SDK platform 35 and build-tools 35.0.0. Android Gradle Plugin 8.7.3.

Create an untracked `local.properties` with your SDK path:

```properties
sdk.dir=/absolute/path/to/android-sdk
```

```sh
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. On a machine with Gradle installed, `gradle wrapper --gradle-version 8.9` can create a wrapper. A GitHub Actions workflow is included for an alternative build environment.

Alternatively, without Gradle or Maven downloads:

```sh
JAVA_HOME=/path/to/jdk ANDROID_HOME=/path/to/sdk bash build-apk.sh
```

This uses SDK build-tools 35.0.0 directly (`aapt2`, `d8`, `zipalign`, `apksigner`) and outputs `dist/pocket-code-debug.apk`. The local debug signing key is generated on first run and must be kept private and retained for installing compatible updates.

## Browser-only UI preview

```sh
cd app/src/main/assets
python3 -m http.server 8765 --bind 127.0.0.1
```

Open `http://127.0.0.1:8765`. This preview uses in-memory files. It does **not** call Gemini, encrypt credentials, invoke Android dialogs or persist preview API keys. It explicitly labels itself as a browser preview.

## Verification

Chrome mobile emulation: visual inspection, accessible DOM snapshot, file creation/edit flow, settings modal, no horizontal overflow, no console errors, all local assets HTTP 200/304. Initial local measurement: LCP 133 ms, CLS 0.00 (development machine, not a phone benchmark).

Actual Gemini, Android Keystore, speech recognizer and document-provider behavior require device testing with a user's own API key. Do not treat browser verification as Android device verification.
