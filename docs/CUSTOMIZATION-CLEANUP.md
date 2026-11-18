# Selective customization cleanup

The owner requested removal of the first customization expansion (`bb0a4fef`) while preserving subsequent independent work. This is a new change in history, not a reset to an old snapshot.

Removed: the eight added settings sections, custom navigation/theme/layout controls, appearance profiles, character overrides and their runtime behavior. Original settings and existing player designs remain. On launch, a one-time migration removes only the expansion's `pippo.ui.*` preferences; original preferences, accounts, music and downloads are not cleared.

Beta retains its playback/network/Bluetooth fixes, module URL copying, automatic refresh-rate controller, icon/startup handoff and icon concept files. Concept artwork remains preview-only. Stable receives the same removal while retaining its existing stable version metadata and other branch contents; this cleanup does not merge beta-only features into stable. Main is unchanged.

Recovery references saved before any edits:

- `backup/beta-before-customization-cleanup-2026-11-18` (`118b13d1`)
- `backup/stable-before-customization-cleanup-2026-11-18` (`94d1b699`)

Validation is limited to source and Git comparisons. No tests, APK builds or GitHub Actions were run for this cleanup; runtime behavior has not been verified on a device.
