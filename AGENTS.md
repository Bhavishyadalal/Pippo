# Pippo fresh-start instructions

- The owner requested restoring the original Mochy source and starting again carefully. Do not rebrand or add features until requested.
- Do not run tests, builds, test/check scripts, GitHub Actions, or other verification workloads unless the owner explicitly asks. Requests to edit or push code do not authorize these runs.
- Keep every GitHub workflow manual-only. Do not add automatic push, pull-request, schedule or other triggers unless explicitly requested.
- Preserve upstream licenses, source copyright notices and history. Make focused source changes and state verification limits honestly.

- The owner removed the four experimental Cassette, Midnight Glass, Pippo Duo and CD Walkman player screens. Keep the existing player screens; do not reintroduce the removed designs unless requested.
- Do not build or publish another APK, or launch GitHub Actions, until the owner explicitly asks for that again.

- The tested design-system pass was merged into main at bd58eae8 at the owner's request. Current customization work belongs on `preview-beta-v0.1.2`; preserve main and the backup branch, and merge only when requested.

- Latest authorized work: playback/network/Bluetooth reliability and module URL copying on `preview-beta-v0.1.2`. The owner explicitly authorized logic/function testing for these fixes. Keep main, stable and backup untouched unless requested. Preserve the restored design; do not reintroduce the reverted mini-player or cozy-settings redesigns.
