# Stream engine beta 0.1.4

Based on Pippo beta 0.1.3 (2a6d3de4939f386ef66923e094be5ba881343495).

Architectural reference: [BitChord](https://github.com/kushagrasinghx/BitChord/tree/21356206de5594ca0bdaf4680ca2db68a2287729), GPL-3.0, particularly SharedCalls, SourceResolver, AudioCache, PlaybackService and AudioOutputPolicy. This implementation adapts the concepts to Pippo's existing resolver and output pipeline; no third-party service credentials or databases are imported.

- Concurrent duplicate anonymous module and guest YouTube requests share one bounded in-flight lookup. No general persistent result cache.
- Force playback can start a guest fallback lookup after 1.5 seconds if permitted; the normal data source commits actual format and cache metadata. Manual choices, offline, signed-in speculative lookup and active Data Saver remain excluded.
- Read-ahead defaults on for unmetered connections only; next-track audio precedes current-file completion. The following two tracks may warm URLs rather than audio bytes. Existing explicit off remains off.
- Route-aware float PCM policy avoids unsupported Bluetooth/phone speaker paths, preserving the USB/wired policy. Existing audio output watchdog retains playback intent after reopening.
- Automatic mid-song source upgrades and wholesale DSP replacement are intentionally deferred: the current handoff and exclusive-output logic needs hardware validation first.

Validation: run the beta workflow (assemble, lint, unit tests). Compare cold/warm time to audible audio, failures, and buffering using identical recordings, provider settings and networks. Test Bluetooth transitions, pause during recovery, USB DAC, offline downloads, manual provider selection, shuffled queue, and Data Saver. No performance claims without device measurements.
