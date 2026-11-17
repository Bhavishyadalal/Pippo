# Pippo

An Android music player with cozy original character artwork, twenty selectable anime launcher icons, phone/tablet and Android TV interfaces, local music, downloads, lyrics, and optional music integrations.

Pippo's characters are selected randomly once per app process. They stay consistent while navigating and recomposing screens. The default launcher uses icon 01; the other portraits and named variants are available in **Customisation → Icons**.

## APK builds

All GitHub Actions workflows are manual. Open **Actions → CI & Debug Build → Run workflow** for a debug APK without release signing secrets. Download the APK artifact when your run completes.

On `design/pippo-design-system`, **Android Release APK** and the new **Optimized Release APK** use a shared optimized release builder. Select ARM64 for a smaller build or Universal for broader device support. Auto signing needs no secrets for a temporary preview; a complete private signing configuration enables stable release signing. See [optimized APK and signing instructions](docs/OPTIMIZED-APK.md).

The separate **Fast Signed APK (ARM64)** workflow still requires release signing secrets. All workflows remain manual; none was launched while adding these build options.

The release application ID is `com.bhavishyadalal.pippo`, with separate `.debug`, `.fast`, and `.profileable` variants. Pippo installs separately from older Mochy APKs; use the existing export/import features to transfer supported library data. Updates are installed manually. The app does not fetch upstream app updates, developer profiles, donation prompts, or the upstream module catalogue. Modules supplied by the user and configured music integrations remain supported.

## Source and artwork

The Kotlin namespace, library interfaces, and stored compatibility keys retain their existing names where needed. Visible app branding and production artwork use Pippo. Approved character masters are in `docs/mascot-concepts/refined-cozy`, with icon concept sheets in `docs/icon-concepts/anime-20`.

See [NOTICE.md](NOTICE.md) for upstream attribution and [LICENSE](LICENSE) for the retained GNU GPL v3 license. Copyright notices and upstream history are preserved.


## Preview beta v0.1.2

The `preview-beta-v0.1.2` branch expands **Settings → Customisation** with navigation layouts/order/labels, accents and text controls, page motion and feedback, Home shelf visibility/order, Library spacing, mini-player options, character selection and named appearance presets with import/export/reset. Performance presets reuse the existing blur and animation controls. See [beta customization details](docs/PREVIEW-BETA-V0.1.2.md) for behavior and review limits. No APK or workflow was run while preparing this beta.


## Stable 0.1.2 testing branch

`stable-0.1.2` contains the customization and startup changes through commit `1e17d0b8`; later mini-player and cozy-settings redesigns have been reverted, with application version **0.1.2** / code **74**. Main and beta are preserved. Use **Optimized Release APK**, choose `stable-0.1.2` in **Run workflow → Branch**, and select ARM64 or Universal as appropriate. The release configuration enables R8 optimization/resource shrinking, excludes debugging/profiling, and retains the existing baseline profile. This branch was prepared without running builds, tests or Actions; device stability and smoothness remain unverified. See [stable preparation notes](docs/STABLE-0.1.2.md).
