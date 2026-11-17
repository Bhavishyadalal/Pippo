# Pippo beta mini-player customization

Branch: `preview-beta-v0.1.2`. Main remains unchanged.

Open Settings → Customisation → Mini-player styles (also searchable).

| Design | Treatment |
| --- | --- |
| Classic | Retains the current morphing mini-player when its detail controls are at their defaults. Changing a detail uses the shared customizable layout. |
| Floating pill | Stadium silhouette, shared glass, compact artwork and existing button press feedback. |
| Compact card | 12dp surface corners, larger artwork and solid background. |
| Two line | Solid surface, stronger title typography, artist on the supporting line. |
| Artwork tint | Dark tonal surface derived from the app's existing artwork color, without another palette request. |
| Minimal | Small artwork, solid page-color surface, title and essential controls; artist is omitted. |
| Connected dock | Shared flat backing behind the phone's mini-player and navigation, aligned margins and corners; preserves the existing integrated dock on wide layouts. |

Optional details: previous/next, favorite and queue controls; top/bottom/off progress line; elapsed time; audio codec; playback status; long-title scrolling; artwork corners; content spacing; outline and black background. The global quality-badge hiding preference overrides the mini-player codec label. Default options preserve Classic. Saved appearance presets, JSON import/export and customization reset include these preferences.

Controls reuse the existing connection for play/pause/replay, previous/next and favorites. Queue opens the existing queue page while expanding the player. The existing sheet owns swipes and expansion; no second player, navigation controller or progress timer is created. Requested controls beyond the available width move into a labeled More menu, retaining 48dp targets in customized layouts. Text stays within two lines; the supporting line combines selected details and truncates when needed. The existing Classic buttons also receive accessible action labels.

Customized layouts crossfade into the full cover over the opening sheet's first 18%. The original full-cover renderer is retained so full-screen player styles and the editor keep their established geometry. Customized mini layouts use still artwork; Classic with default options retains existing miniature video/morphing behavior. Solid styles use a flat mini-player backing rather than mounting its glass renderer; the wide dock retains its shared glass. Connected dock temporarily coordinates the phone navigation surface/margins with the mini-player style; changing away restores the stored navigation style.

Progress is collected only by its enabled display, using the existing shared progress flow. The progress line reads updates during drawing; elapsed text is deduplicated to whole seconds. Codec collection is enabled only when requested. Custom mini-player UI and these subscriptions are dropped when the expanding player takes over. Long-title scrolling requires active playback, foreground lifecycle, a visible app and enabled system/app animations. The Light preset turns scrolling off. Artwork tint reuses the current cached artwork color. Loading, paused, finished, network-wait and error details come from existing playback state; a network-wait label does not declare downloaded audio unavailable.

Review: source/diff inspection only. No tests, check scripts, compilation, APK generation or GitHub Actions were run. Device rendering, extreme font scaling, narrow/rotated layouts, gesture interaction, casting and performance still need a later authorized build and device run. No claim of zero lag is made.
