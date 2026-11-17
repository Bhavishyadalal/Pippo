# Preview beta v0.1.2 · customization

Branch: `preview-beta-v0.1.2`, starting from the APK-tested `bd58eae8` on main. Main and the existing backup branch are preserved. The branch label does not change the release application ID, signing configuration or existing app version number.

## Available controls

Open Settings → Customisation. New areas use the existing LabCard/toggle styling and searchable section index. Cards start collapsed so their detailed controls are only composed when opened.

| Area | Controls |
| --- | --- |
| Performance | Apply Light, Balanced or Expressive; further adjustments remain available afterward. |
| App navigation | Current glass, flat, outlined or full-width phone bar; labels off/selected/all; pill/dot/no indicator; small/normal/large icons; bar-swipe toggle; reorder all four tabs. |
| Colors and text | Coral/sage/lavender/ice/gold/monochrome/custom accents, high-contrast supporting text, black backgrounds, Inter/system/serif/monospace shared fonts, text scaling, shared corner and spacing presets. |
| Page motion | Existing directional slide, fade, gentle scale or instant page switching; fade/scale duration; press effects; Compose interface haptics; loading motion; record rotation; animated cover backgrounds. |
| Home | Show/hide and reorder 11 known shelves. Restore original mixed ordering. Opening tab can be set for the next launch. |
| Library and browsing | Compact/comfortable/roomy Library row spacing and existing browse-grid tile size. |
| Mini-player | Outline and black-background options; hide shared stream-format badges. |
| Characters | Existing random-per-session behavior, female, male or hidden illustrations. Launcher icon selection stays independent. |
| Saved UI presets | Up to five named presets, apply/delete, export/import an appearance JSON document, and a customization-only reset. |
| Existing customization | The original launcher icons, player styles/editor, cover rounding, playlist/Home styles, blur quality/strength, global animation speed, lyrics, system bars and swipe-action controls remain available. Now-playing/playback pages are linked for additional existing options. |

## Behavior and compatibility

Defaults preserve the tested app's current bar, tab order, effects, palette and character selection. No rejected player screen is reintroduced. All navigation tabs remain reachable; duplicate/unknown stored order entries cannot remove a screen. Ordering is applied to the navigation model before both the bar and tab-transition ownership, so swipe direction follows the chosen order without replacing the controller or discarding tab stacks.

The phone surface styles preserve the 64 dp navigation height and existing mini-player geometry. Wide layouts retain their integrated surface while honoring tab order, labels, icon size, indicators and bar swipe. A merged wide mini-player continues to share its containing surface. Look modules can override fonts, colors and parts of glass rendering.

Shared Material typography and Inter-aware text read the selected font; explicitly styled components may retain their own styling. App text scaling is bounded and combines with Android font scaling. Shared spacing does not shrink touch-target minimums. Very dark custom accents are lifted for visibility; an appropriate light or dark foreground is selected for custom accents.

Manual Home order places known shelves before the remaining online recommendations. With no manual order, the original interleaved feed is preserved. Hidden shelves are filtered at the rendering list, not deleted from music storage; existing recommendation fetching is unchanged. The underlying row renderer and click/long-press actions are retained in Library.

Page transition choices affect navigation-owned transitions. Interactive player expansion, sheet dragging, back stacks and audio playback continue to use their existing systems. Existing global/system animation scaling still applies. Interface haptics controls Compose UI feedback, not music-synchronized vibration, keyboard feedback or system feedback.

## Performance approach

Customization is mirrored into an immutable snapshot from the existing settings observer and deduplicated before updating UI state. Rendering never writes preferences. There are no new polling loops, perpetual background timers, video decoders or shader dependencies.

Light disables glass, progressive/element blur, loading decoration, press deformation, cover rotation and animated cover backgrounds, and selects a flat navigation surface with a short fade. Balanced uses lower-quality glass and restrained effects. Expressive enables richer blur, motion and cover backgrounds; it can consume more GPU, battery and data. Presets apply once in a single preference transaction and do not keep enforcing their settings over manual choices.

Disabling press effects avoids constructing the associated animation/gesture machinery. Flat navigation styles skip frosted-glass rendering. Existing hidden-page/reduced-motion handling is retained for loading dots and the app animation clock. Palette, corner and typography changes are preference-driven rather than frame-driven.

## Preset format and reset

Only an explicit appearance-setting allowlist is exported or applied. Accounts, tokens, music, playlists, downloads, filesystem paths and provider credentials are excluded. Files have schema version 1, a 64 Ki-character limit, bounded nesting, recognized enum validation, bounded numeric settings and a normalized navigation order. Validation completes before the apply transaction. Unknown keys are ignored; unsupported values and documents without recognized settings are rejected. Empty settings represent default customization.

Named presets are kept separately from active settings and survive customization reset. Saving an existing name replaces that preset; reset asks for confirmation and removes only allowlisted active appearance settings. Profiles include the shared controls and selected existing appearance settings; player-editor-specific menu mappings and individual per-screen sort positions remain configured separately.

## Review status

Source and diff inspection only. No tests, compilation, APK build/publication or GitHub Actions were run for this beta. Compilation, device rendering, accessibility at large font sizes, document import/export, and frame-rate/battery measurements remain unverified. No claim of zero lag is made. Any later manual workflow run should select `preview-beta-v0.1.2` to build this source.

## Main icon and startup follow-up

The existing approved portrait #01 remains the default launcher icon; the 20 selectable alternatives are retained. The system splash now explicitly uses that same main adaptive icon instead of the plain equalizer. Home cold starts pass the main icon into the existing Canvas intro, enabling its zoom/ripple handoff to the music mark and Home header. The foreground respects the launcher's 18% inset. The icon prelude is shortened to 420 ms and the mark flight to 480 ms; existing readiness gates, timeouts and global animation-duration scaling are retained. Search-first launches, deep links and restored activities retain their existing startup paths. No new artwork, video or animation dependency is introduced. Device-specific splash masking and the handoff remain unverified without a build and device run.
