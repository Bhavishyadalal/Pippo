# Preview beta v0.1.2

The first customization expansion has been withdrawn at the owner’s request. Its additional settings sections, custom theme/navigation/layout behavior and appearance-profile system are removed. The original customization controls are retained.

See [the selective cleanup record](CUSTOMIZATION-CLEANUP.md) for preserved features and backup references.

## Main icon and startup follow-up

The existing approved portrait #01 remains the default launcher icon; the 20 selectable alternatives are retained. The system splash now explicitly uses that same main adaptive icon instead of the plain equalizer. Home cold starts pass the main icon into the existing Canvas intro, enabling its zoom/ripple handoff to the music mark and Home header. The foreground respects the launcher's 18% inset. The icon prelude is shortened to 420 ms and the mark flight to 480 ms; existing readiness gates, timeouts and global animation-duration scaling are retained. Search-first launches, deep links and restored activities retain their existing startup paths. No new artwork, video or animation dependency is introduced. Device-specific splash masking and the handoff remain unverified without a build and device run.
