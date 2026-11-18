# Home icon and scroll fixes

Work is confined to `preview-beta-v0.1.2`; main and stable are not updated.

## Matching identity

The selected Pippo portrait stays visible through the launch intro and lands in a matching circular Home badge. The old equalizer drawing and portrait-to-equalizer morph are removed from this flow. Both the loading shell and loaded Home use the same badge and 34 dp target geometry. A preference listener updates the Home badge after an icon change and unregisters when the composable leaves composition.

Each of the 20 existing icon choices has a corresponding splash theme. The activity selects it before installing the splash and Android 13+ persists it for subsequent system splashes, including immediately after icon selection. Android controls the initial system-created splash: Android 12 and manufacturer launchers may still show the manifest default before the app-owned intro. The app-owned intro and Home always use the saved selection. Launcher masks and this handoff need device review.

## Less work and fewer jumps

- Removed the intro's animated blur and equalizer crossfade. Opaque portrait frames no longer request a separate alpha buffer. The settling phase is shortened from 420 ms to 180 ms; existing readiness gates and launch timeout behavior remain.
- Home's collapse state and its providers are keyed together. Switching Home styles or changing display geometry no longer leaves callbacks reading an obsolete collapse-distance state.
- Removed automatic horizontal-grid resets when recommendation lists change. Existing stable song keys preserve the visible item instead of forcing the user back to the beginning.
- The header's portrait/Home transition uses a 260 ms fade/scale rather than a 600 ms blurred transition. The main Home glass, hero, artwork, section layouts and existing player designs remain.

The earlier playback/Bluetooth, module URL copying and automatic refresh-rate changes remain intact. This change does not reintroduce the withdrawn customization expansion.

## Review limits

Source and Git-diff inspection only. No tests, build/check scripts, Gradle tasks, APK builds or GitHub Actions were run. There is no measured frame-time improvement yet, and this is not a claim that every app bug is fixed. Device review should cover cold launch, icon selection/relaunch, Home style changes, fast scrolling, a recommendation update while a row is scrolled, and reduced-motion settings.
