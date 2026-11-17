# Additional Pippo player styles

Selected concepts: 02 Cassette, 04 Midnight Glass, 09 Pippo Duo, 06 CD Walkman.

## Integration

The four styles are registered in `PlayerStyle`, appended after the existing built-in styles to preserve their positions, and offered as individual selector columns. The optional module entry remains last. Both the real selector cards and the selected player dispatch through `AlternatePlayerStyle`; Customisation has corresponding small thumbnails.

`PremiumPlayerStyles.kt` uses the existing `StyleFrame`, session connection, song carousel, `PlayerProgressSlider`, menu actions and lyrics/queue overlay pages. It does not create another audio player, download service or network endpoint. Saved style names and the older style migrations remain intact. The full flagship editor is intentionally not offered for these complete layouts.

## Rendering and behavior

- Cassette: cached Compose drawing primitives, cream label and graphite chassis, live tape transfer geometry, rotating hubs. Tape radii conserve tape area and safely handle seeking and unknown durations.
- Midnight Glass: album-colored radial illumination, a low-resolution blurred cover backdrop on Android 12+, static gradient fallback on older Android/when element blur is off, and translucent control surfaces.
- Pippo Duo: original AI-generated artwork derived from Pippo's approved characters. The master is in `docs/player-artwork`; a 212 KB WebP is bundled in the app. Animation is subtle camera drift; the illustration is not a rigged character animation.
- CD Walkman: translucent-style enclosure, metallic disc, rotating album-art label, center hub and LCD-style metadata.

One frame clock per live style advances only decorative layers/canvas. It runs only while playback is effective, the screen is visible, the lifecycle is started, and app/system motion settings allow animation. Pausing or hiding preserves the last angle; returning discards the background time gap. Preview cards do not start decorative clocks. Per-frame angle reads happen in drawing/layer callbacks rather than rebuilding the full screen.

Transport buttons use at least 48 dp touch targets, current session availability, localized descriptions, repeat-one indication, and toggle-state descriptions. Narrow panels move shuffle/repeat to the secondary action row. Large text and short windows can scroll rather than clip the controls. Wide layouts put the artwork beside the controls. Android TV exposes Midnight Glass and Pippo Duo with the existing remote control layer; the detailed mechanical styles retain the existing unsupported-style fallback.

## Build compatibility fix

The preceding rebrand introduced Gradle `resValue` entries for variant-specific launcher shortcuts. AGP 9 disables generated resource values by default. `buildFeatures.resValues = true` is required and is now enabled.

## Verification

The implementation includes focused geometry and style-selection regression tests. Device rendering, touch interactions, actual GPU frame timing and low-memory behavior require Android device/emulator testing; this workspace has no connected Android device or emulator. No GitHub Actions workflow is dispatched by this change.

Local verification completed for this change:

- `assembleArm64GmsDebug` succeeded, producing an ARM64 APK with application ID `com.bhavishyadalal.pippo.debug`, label `Pippo Debug`, and minimum SDK 24.
- Targeted Gradle unit tests passed: 41 tests, zero failures/errors (5 deck geometry, 4 style-selection, 9 existing cover-layout, 23 existing video-letterbox).
- Android `apksigner verify --verbose` passed with APK Signature Scheme v2.
- `git diff --check` passed, and GitHub workflow files were unchanged.

The first compile exposed a responsive-layout receiver error, which was corrected before the successful build. Build-environment setup required the workspace's Java proxy configuration and a standard local debug keystore. No signing keys were added to the repository.
