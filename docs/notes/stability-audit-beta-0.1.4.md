# Beta 0.1.4 stability audit follow-up

Based on 2ac054d. No workflow dispatch or automatic build trigger.

- Shared requests release their producer and slot when the final consumer cancels. A full sharing table does not block independent requests. Provider results and prefetches check current configuration.
- Normal YouTube URL/header pairs carry a network, account and quality context and monotonic expiry. New player-cache entries record quality and file identity. Explicit downloads remain separate.
- Module rendition keys now hash the resolved URL and request headers, avoiding reuse across different signed URLs (a deliberate cache-hit-rate tradeoff). Complete, unlicensed progressive module caches can be rediscovered offline and in the Cached tab. Old unmarked cache entries are not automatically trusted.
- Adaptive manifests and same-origin requests receive source-specific headers; cross-origin authorization is not guessed and provider-specific headers are stripped on cross-origin HTTP redirects. Signed CDN redirects and cross-origin manifests still require provider-specific validation.
- Bluetooth watchdog verifies after reopening and pauses/reports if the output remains unhealthy. A heuristic route is not considered proof of a mismatch. Actual acoustic silence with healthy platform counters remains unobservable.
- Foreground buffering takes priority; full-file speculative reads are removed. Network handovers/metering reset plans; completed files survive read-ahead eviction.
- Imports share a staging lock, enforce an extraction budget, reject duplicates, and open the staged database through Room before installation. No destructive migration or invented historical migration is added.
- Unit test sources cover sharing cancellation, cache-key parsing, and import byte budgets.

## Remaining verification
Compile, lint, unit tests, Room migration fixtures for historical versions, real Android Bluetooth/USB and offline playback, authenticated adaptive streams and redirects, rapid skipping, restore interruption, memory and battery profiling. Existing historical schema gaps require authentic old schemas; no claim of universal bug-free operation. This is a static source change, not a tested APK.
