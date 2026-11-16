# Playback fixes

This is a source review of playback transitions, seeking, progress rendering and provider switching. These findings have not been reproduced or verified in a running APK. This is not a claim that every app bug has been found.

## Findings and changes

- **Previous song's elapsed time survives a transition.** `PlayerProgressSlider` retained its drag and pending-seek previews across songs, while `PlayerConnection` only published progress on a timer. Progress now publishes from player events immediately. Each playback occurrence has a generation, so transitions, repeats and replacement players recreate the slider's previews and fill animation using the current player's position. A normal next song starts at its reported position near zero; crossmix retains the incoming song's actual position.
- **Cancelled or outdated gestures can retain a preview or seek the wrong song.** The slider clears cancelled drags and invalidates callbacks when their state is disposed. The turntable commits only completed drags belonging to the same player and playback generation, and clears its drag state in `finally`. Unknown-duration and inactive sliders cannot start a seek.
- **Seek controls bypass transport handling.** The main slider and turntable now call `PlayerConnection.seekTo`, which routes cast seeks and cancels local transport transitions before seeking.
- **Smoothed clocks can carry state into another playback.** Connection-backed clocks now use playback generation, player identity and media identity to reset their timing. They also clear a missing player's position and bound displayed song time to a known duration before applying lyric offsets.
- **Bedside progress can retain the previous song until its next one-second tick.** A playback generation change now wakes its progress sampler immediately, and a replacement connection receives fresh local state.
- **Replacement players inherit stale error and skip controls.** Attaching a player now refreshes these fields alongside metadata, queue and transport state.
- **Superseded provider switches can swallow cancellation and lose resume intent.** Provider resolution now propagates cancellation and checks that the request remains active before changing playback. Replacing a working switch for the same song preserves whether playback should resume.

## Validation limits

Only source and diff inspection was performed. No tests, builds, verification scripts or GitHub Actions were run, as instructed by the repository owner. Workflows remain manual-only. Runtime behavior and APK compilation remain unverified.
