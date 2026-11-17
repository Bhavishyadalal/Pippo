# Build an optimized Pippo APK

## Open and run

The builder is on `design/pippo-design-system`. Main and the backup branch are preserved.

1. Open [Android Release APK](https://github.com/Bhavishyadalal/Pippo/actions/workflows/android.yml).
2. Press **Run workflow** and select **design/pippo-design-system** in the branch selector.
3. Use **arm64** and **auto** for the simplest build. Press **Run workflow** yourself.
4. When it completes, open the run and download the `pippo-arm64-gms-preview-optimized-…` or `pippo-arm64-gms-release-optimized-…` artifact. Extract the ZIP and install its APK. The run summary also links to the artifact.

The new **Optimized Release APK** workflow uses the same shared builder. GitHub normally discovers new workflow entries from the default branch; the existing Android Release APK entry lets you run the design branch now without merging the app changes into main. Use the design branch when running either entry to build the new design system.

The workflow is manual only. Creating or pushing it does not run a build or publish an APK. It uploads build artifacts rather than creating GitHub releases.

## Build choices

- **arm64:** one ABI, smaller download and less native compilation; suitable for modern ARM64 phones.
- **universal:** all ABIs supported by the app; larger and slower to build.
- **auto signing:** all four secrets configured means private release signing; no secrets means temporary preview signing. Partial signing configuration fails early with the missing names.
- **preview signing:** no signing secrets needed. A temporary key is generated for this run.
- **release signing:** requires your private signing secrets and fails before compilation when they are missing or invalid.

Both preview and private release modes use the real **release build type**: non-debuggable, R8 code optimization/minification and resource shrinking enabled. Preview refers to its signing key, not debug build behavior. All use the release application ID `com.bhavishyadalal.pippo`.

A preview key changes on every run. For consistent updates, configure and keep the same private release key. Changing between signing keys can prevent installing over an existing APK. The older `.debug` application is a separate installation.

## Private release signing (recommended for ongoing use)

Add repository secrets at [Pippo → Settings → Secrets and variables → Actions](https://github.com/Bhavishyadalal/Pippo/settings/secrets/actions):

| Secret name | Value |
| --- | --- |
| `KEYSTORE_BASE64` | The entire Base64-encoded contents of your private keystore file. |
| `STORE_PASSWORD` | The password you chose for that keystore. The existing `KEYSTORE_PASSWORD` name is also accepted; `STORE_PASSWORD` takes precedence. |
| `KEY_ALIAS` | The alias inside the keystore. The example below uses `pippo`. |
| `KEY_PASSWORD` | The private-key password you chose; it can be the same as the store password. |

These values are specific to your key; they are not GitHub tokens or a shared password someone can give you. The workflow works without them in preview mode. Existing optional Last.fm, Libre.fm and Discord secrets remain supported but are not required just to build the APK.

### Generate your key once

On a computer with Java installed (or an Android terminal with Java), run:

```bash
keytool -genkeypair -v -keystore pippo-release.jks -storetype JKS \
  -alias pippo -keyalg RSA -keysize 3072 -validity 10000
```

Follow the interactive prompts. Record the store password. When asked for the key password, pressing Enter uses the same password. Keep a secure backup of the keystore and its passwords.

On Linux/macOS/Android terminals, encode the file:

```bash
base64 < pippo-release.jks | tr -d '\r\n' > pippo-keystore-base64.txt
```

On Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("pippo-release.jks")) | Set-Content -NoNewline "pippo-keystore-base64.txt"
```

Create each repository secret separately using **New repository secret**. Paste the contents of `pippo-keystore-base64.txt` into `KEYSTORE_BASE64`, the alias `pippo` into `KEY_ALIAS`, and your chosen passwords into the two password secrets. Keep the keystore, its encoded file and passwords out of commits, issues and chat. GitHub does not let you read a stored secret back later.

After all four are present, **auto** uses private release signing; you can also select **release** to require it explicitly.

## Optimization and diagnosis

The shared action builds a single selected release variant, caches Gradle dependencies/task outputs and native compilation, and avoids the separate Kotlin daemon to limit memory use. It does not disable R8 to make builds look faster. Initial dependency downloads and R8 processing can still take time; caching primarily helps later runs.

The action verifies the APK signature, release application ID and non-debuggable manifest, and requires R8 mapping output. The mapping is uploaded as a separate diagnostic artifact. APKs are already compressed, so artifact upload skips redundant compression. Private keystores are removed after success or failure and are excluded from the explicit native cache.

This workflow was prepared by source inspection only. It has not been dispatched or built as part of this change. Successful compilation and actual device performance still require running the workflow and testing its APK.
