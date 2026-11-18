# Pippo

An Android music player with cozy original character artwork, 53 launcher icons, phone/tablet and Android TV interfaces, local music, downloads, lyrics, and optional music integrations.

Pippo's characters are selected randomly once per app process. They stay consistent while navigating and recomposing screens. The black Ribbon P is the default launcher icon. Pick lettering, illustrated, glass, or earlier portrait designs in **Customisation → Icons**.

## APK builds

All GitHub Actions workflows are manual. Open **Actions → CI & Debug Build → Run workflow** for a debug APK without release signing secrets. Download the APK artifact when your run completes.

On `design/pippo-design-system`, **Android Release APK** and the new **Optimized Release APK** use a shared optimized release builder. Select ARM64 for a smaller build or Universal for broader device support. Auto signing needs no secrets for a temporary preview; a complete private signing configuration enables stable release signing. See [optimized APK and signing instructions](docs/OPTIMIZED-APK.md).

The separate **Fast Signed APK (ARM64)** workflow still requires release signing secrets. All workflows remain manual; none was launched while adding these build options.

The release application ID is `com.bhavishyadalal.pippo`, with separate `.debug`, `.fast`, and `.profileable` variants. Pippo installs separately from older Mochy APKs; use the existing export/import features to transfer supported library data. Updates are installed manually. The app does not fetch upstream app updates, developer profiles, donation prompts, or the upstream module catalogue. Modules supplied by the user and configured music integrations remain supported.

## Source and artwork

The Kotlin namespace, library interfaces, and stored compatibility keys retain their existing names where needed. Visible app branding and production artwork use Pippo. Approved character masters are in `docs/mascot-concepts/refined-cozy`, with icon concept sheets in `docs/icon-concepts/anime-20`.

See [NOTICE.md](NOTICE.md) for upstream attribution and [LICENSE](LICENSE) for the retained GNU GPL v3 license. Copyright notices and upstream history are preserved.

## Stable 0.1.2 testing branch

`stable-0.1.2` includes the latest beta changes for testing with version **0.1.2** and version code **74**. This update brings in playback and Bluetooth fixes, automatic refresh rate, module URL copying, and the selected icon and scroll fixes for Home. See the [stable testing notes](docs/STABLE-0.1.2.md).
