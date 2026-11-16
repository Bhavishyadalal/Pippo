# Playback and reliability fixes

This is a source review of playback transitions, seeking, progress rendering and provider switching. These findings have not been reproduced or verified in a running APK. This is not a claim that every app bug has been found.

## Findings and changes

- **Previous song's elapsed time survives a transition.** `PlayerProgressSlider` retained its drag and pending-seek previews across songs, while `PlayerConnection` only published progress on a timer. Progress now publishes from player events immediately. Each playback occurrence has a generation, so transitions, repeats and replacement players recreate the slider's previews and fill animation using the current player's position. A normal next song starts at its reported position near zero; crossmix retains the incoming song's actual position.
- **Cancelled or outdated gestures can retain a preview or seek the wrong song.** The slider clears cancelled drags and invalidates callbacks when their state is disposed. The turntable commits only completed drags belonging to the same player and playback generation, and clears its drag state in `finally`. Unknown-duration and inactive sliders cannot start a seek.
- **Seek controls bypass transport handling.** The main slider and turntable now call `PlayerConnection.seekTo`, which routes cast seeks and cancels local transport transitions before seeking.
- **Smoothed clocks can carry state into another playback.** Connection-backed clocks now use playback generation, player identity and media identity to reset their timing. They also clear a missing player's position and bound displayed song time to a known duration before applying lyric offsets.
- **Bedside progress can retain the previous song until its next one-second tick.** A playback generation change now wakes its progress sampler immediately, and a replacement connection receives fresh local state.
- **Replacement players inherit stale error and skip controls.** Attaching a player now refreshes these fields alongside metadata, queue and transport state.
- **Superseded provider switches can swallow cancellation and lose resume intent.** Provider resolution now propagates cancellation and checks that the request remains active before changing playback. Replacing a working switch for the same song preserves whether playback should resume.

## Follow-up reliability changes

- **Restore recovery:** prepares replacement files before changing live data and retains originals in a rollback journal until commit. An interrupted, uncommitted restore is rolled back before databases open on the next start. Failed imports keep their staging folder and show a failure note; they are not automatically retried. Preference files, database sidecars and storage-location markers participate in recovery. Staged audio uses hard links where supported to avoid copying large downloads at startup, with a copy fallback. If a failure happens after preference APIs have opened, startup stops after rollback so cached imported preferences cannot be used with the previous library; reopening the app continues with the previous data.
- **Export replacement:** writes and closes a complete temporary file before renaming the original to a backup and promoting the replacement. Failure attempts to restore the original name. Providers without rename support refuse replacement while retaining the original; if a provider also refuses rollback, the original remains under an `.export-backup-...` name.
- **Download resume integrity:** stream identity is stored alongside cached bytes rather than only in memory. Changed or unknown identities cannot append to existing bytes, including after an app restart. Legacy partial downloads without an identity require a clean retry. Module streams use provider and URL identity conservatively; a refreshed URL may therefore require restarting a partial download.
- **Offline auto-downloads:** candidate songs are re-evaluated when offline mode changes, so songs added while offline can be queued when it is disabled. Explicitly stopped and completed downloads retain their existing treatment. The initial download-index cursor is also closed.
- **Sleep timer:** deadlines, adjustments, pause/resume calculations and both countdown displays use elapsed realtime. The job rechecks its deadline after waking, and pausing an already expired timer clears it and pauses playback rather than leaving an unusable paused timer.
- **Album export folders:** distinct album identities receive unique, consistently ordered folder names even when their titles match. Songs without album IDs use album title and lead artist as their identity.
- **Search suggestions:** the shared suggestion view model waits 300 ms for ordinary online queries, cancelling superseded work. Links, empty queries and offline library search remain immediate. The older screen's extra delay is removed.
- **Search pagination:** only one continuation request per filter can run at a time. Cancelled or obsolete responses cannot overwrite a replacement page; the guard is released on completion, failure and cancellation.

## Validation limits

Only source and diff inspection was performed. No tests, builds, verification scripts or GitHub Actions were run, as instructed by the repository owner. Workflows remain manual-only. Runtime behavior and APK compilation remain unverified.
