# Build Pippo APKs

## From your phone: GitHub Actions

1. Open **Bhavishyadalal/Pippo → Actions → Android Production APK**.
2. Select **Run workflow**, branch **main**.
3. Choose **production** for the signed Pippo app, **arm64** for most phones (or **universal**) and **foss** (or **gms** for Google Play Services Cast).
4. When the run succeeds, open its summary, scroll to **Artifacts**, download the Pippo ZIP, extract it and install the APK. The ZIP also contains a SHA-256 checksum and public signing-certificate details.

The normal app is **Pippo**, package `app.pippo.music`, version 0.1.0 / code 1. Development builds are **Pippo Dev**, package `app.pippo.music.debug`, and install alongside it. Neither automatically imports another app's private library.

### One-time production signing setup

In **Settings → Secrets and variables → Actions → New repository secret**, configure these four names (the same convention as Veyra's production workflow):

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 contents of your private production `.jks` keystore |
| `KEYSTORE_PASSWORD` | Password protecting the keystore |
| `KEY_ALIAS` | Signing-key alias in that keystore |
| `KEY_PASSWORD` | Password protecting that signing key |

Use the **same signing key for every future Pippo update** and keep a private backup. These values cannot be recovered from a production APK. Never commit the keystore or passwords or place them in an issue. A new app may use a dedicated key, or a key you already own; it must not use the upstream developer's identity.

If you need a new key, run this on your own computer and follow the password prompts:

```sh
keytool -genkeypair -keystore pippo-release.jks -alias pippo -keyalg RSA -keysize 4096 -validity 10000
```

Encode that file to a text file on Linux/macOS with `base64 < pippo-release.jks > pippo-keystore-base64.txt`. Paste its entire contents into `KEYSTORE_BASE64`. Keep these files private.

Production mode deliberately fails with a named missing-secret error instead of silently signing a production app with an ephemeral debug key. Until these secrets are set, choose **development** to test the app. Development uses the runner's debug key, so a later development APK may require uninstalling the previous development copy; production updates keep their stable key.

Optional integrations: `LASTFM_API_KEY`, `LASTFM_SECRET`, `LIBREFM_API_KEY`, `LIBREFM_SECRET`, and Pippo's own `DISCORD_CLIENT_ID` (Actions variable or secret). Without a Discord application, that login remains unavailable. External Discord application names/icons are configured on Discord, not by the APK's display name.

## What Actions verifies

The selected APK variant is assembled, its Android unit tests and lint task are run, then the APK's application ID, label and signature are verified. Source checks also cover branding, launcher aliases, shortcuts and JNI names. Lint currently inherits upstream's non-blocking issue policy; consult the uploaded lint report. Artifacts and reports expire after seven days.

**Android Checks** performs the development arm64 FOSS build on pushes and pull requests. GMS and universal can be selected manually. The workflow does not create a public GitHub Release. The in-app updater reads only this Pippo repository; it will have no update to offer until a compatible signed APK is published as a Release.

## Local Android Studio build

Install JDK 21, Android SDK platform 37, NDK `27.0.12077973`, and CMake `3.22.1`. Open the repository root and let Gradle sync.

```sh
./gradlew :app:assembleArm64FossDebug :app:testArm64FossDebugUnitTest
```

For a locally signed production build, copy `keystore.properties.example` to `keystore.properties`, set the real private values and place the keystore at its configured path, then run:

```sh
./gradlew :app:assembleArm64FossRelease -PpippoRequireReleaseSigning=true
```

Alternatively set `PIPPO_KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` in your local environment. Production CI always enables the signing guard. A local unguarded release without credentials retains upstream's debug-signing fallback for profiling; it is not a production-signed release.

The upstream module API now belongs to `app.pippo.music.module`. Existing executable Mochy modules are not guaranteed compatible: they must target this API and satisfy Pippo's signing checks. Declarative/HTTP modules still follow their supported manifest contracts.
