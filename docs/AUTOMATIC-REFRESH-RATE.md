# Automatic display refresh requests

Implemented on `preview-beta-v0.1.2`. No new controls or user setup are required.

The main app window selects the highest finite, positive refresh rate advertised at the current display resolution. A 60 Hz-only display requests its supported 60 Hz mode; a compatible 90/120/144 Hz mode is selected when available. Equal-rate ties retain the current mode where possible. An unsupported or unavailable mode falls back to Android's default choice rather than inventing a rate or changing resolution.

The window preference is reapplied on resume, view attachment and display changes, and cleared on pause/detachment. Display/lifecycle/attachment listeners are removed when their owner is disposed. Identical window and View preference writes are skipped to avoid feedback from display-mode notifications. The controller is event-driven and adds no polling timer or continuous invalidation loop.

On Android 15+, the root View also supplies a frame-rate request. The existing animation-only high-rate voter uses the same resolution-compatible selection and is refreshed on display changes; its existing visibility/lifecycle gating remains. This voter already redraws a pixel while a supported active animation needs it; this change does not introduce an always-on animation vote for idle screens.

The old `enableHighRefreshRate` storage key is retained as a legacy identifier but no longer caps the app at 60 Hz. It is excluded from active appearance preset capture/apply, so restoring an older appearance preset cannot turn automatic detection off. Other saved preferences are unchanged.

These are requests, not a bypass of Android display policy. Battery saver, thermal limits, manufacturer settings, variable-refresh behavior and competing windows can limit the actual rate. A high display rate does not establish that every app frame meets its rendering deadline.

Source/diff review only for this change. No tests, Gradle compilation, APK builds or GitHub Actions were run. The earlier 197-test playback result does not validate this subsequent display change. Physical display switching, OEM behavior, battery use and perceived smoothness remain to be measured on a device. Main and stable remain unchanged. The new icon set is still pending approval/integration and was not altered by this work.
