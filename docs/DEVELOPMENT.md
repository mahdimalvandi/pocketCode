# Development, build and release

## Requirements
- JDK 17-compatible compiler/runtime.
- Android SDK platform 35 and build-tools 35.0.0.
- Python 3, Bash; Node.js for JS syntax checks.
- Optional Gradle 8.9 for the configured Gradle project; no wrapper is committed.

## SDK-only build (used for delivered APK)
```bash
export JAVA_HOME=/path/to/jdk
export ANDROID_HOME=/path/to/android-sdk
export PATH="$JAVA_HOME/bin:$PATH"
node --check app/src/main/assets/app.js
bash build-apk.sh
```
The script compiles resources with aapt2, compiles all Java sources, dexes, bundles assets, zipaligns, signs and verifies `dist/pocket-code-debug.apk`. It creates `.debug-signing.keystore` if absent, using standard debug passwords. **Keep the original key privately** to build upgrades for this installed APK. A new clone without that key generates a different signer.

Environment used for initial builds (machine-specific, not required paths): JDK `/opt/goland/jbr`, SDK `/tmp/opencode/android-build/sdk`.

## Gradle / CI
```bash
gradle assembleDebug --no-daemon
```
GitHub workflow `.github/workflows/android.yml` installs Java, SDK and Gradle and uploads `app/build/outputs/apk/debug/app-debug.apk`. CI has its own debug signer and is **not guaranteed to upgrade the manually signed APK**. No production signing credentials are in CI. Do not replace the personal release artifact with a CI build without comparing certificates.

## Artifact integrity and signing provenance

Every local SDK build writes three files under `dist/`:
- `pocket-code-debug.apk`
- `pocket-code-debug.apk.sha256`
- `pocket-code-debug.apk.provenance.txt`

Verify the checksum from that directory with:
```bash
cd dist
sha256sum --check pocket-code-debug.apk.sha256
cat pocket-code-debug.apk.provenance.txt
```
The provenance file records package ID, version code/name, Git revision (or `unknown` for a source archive without Git metadata) and the APK hash. A checksum proves that the artifact bytes match the supplied digest; it does not prove the APK is safe, trustworthy or production-signed.

The local debug keystore is ignored by Git and restricted to owner-only permissions by the SDK build script. Back it up securely outside the repository. Never upload it to GitHub or include it in source archives. CI intentionally uses a separate debug signer; CI artifacts may not upgrade the locally installed APK. The Actions workflow is manual-only.

**License status:** no project license has been selected. Do not add or imply an open-source license until the repository owner chooses one.

## UI preview
```bash
python3 -m http.server 8765 --bind 127.0.0.1 --directory app/src/main/assets
```
Browser preview is not the Android runtime and cannot read phone storage. Inject a mock `NativeBridge.request` before page scripts if testing native response flows; respond asynchronously with the same request ID through `onNativeResponse`. Never put a real API key in preview JS or browser storage.

## Release checklist
1. Update `app/build.gradle` versionCode/name, matching `build-apk.sh` flags and UI build label.
2. Update feature docs and verification evidence; do not rewrite historical results as new tests.
3. Run JS syntax, Android build, signature verification and browser checks; run physical-device matrix if a device is available.
4. Confirm application ID `dev.pocketcode.app` and signing certificate remain compatible with installed build.
5. Scan staged files and APK for credentials; ensure local signing keys and private JSON are ignored.
6. Commit source, docs and the current personal APK; tag the version only after checks. For public distribution prefer a GitHub Release asset rather than many APK revisions in Git.
7. Archive source excluding build, dist, local.properties and signing files; do not include a recursive archive inside itself.

## Publication
Repository: `https://github.com/mahdimalvandi/pocketCode`.
SSH remote: `git@github.com:mahdimalvandi/pocketCode.git`.
Do not force-push, reset or rewrite owner history. GitHub authentication and Android signing are independent. Repository visibility/licensing changes require an owner decision.
