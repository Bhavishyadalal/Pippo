# Pippo fresh-start instructions

- The owner requested restoring the original Mochy source and starting again carefully. Do not rebrand or add features until requested.
- Do not run tests, builds, test/check scripts, GitHub Actions, or other verification workloads unless the owner explicitly asks. Requests to edit or push code do not authorize these runs.
- Keep every GitHub workflow manual-only. Do not add automatic push, pull-request, schedule or other triggers unless explicitly requested.
- Preserve upstream licenses, source copyright notices and history. Make focused source changes and state verification limits honestly.

- The owner removed the four experimental Cassette, Midnight Glass, Pippo Duo and CD Walkman player screens. Keep the existing player screens; do not reintroduce the removed designs unless requested.
- Do not build or publish another APK, or launch GitHub Actions, until the owner explicitly asks for that again.

- Main remains at the tested design-system commit bd58eae8. The owner explicitly requested `stable-0.1.2` for stable-version testing, then requested restoring both beta and stable app code to 1e17d0b8. Stable retains version 0.1.2/code 74 and release optimization settings. Preserve main and the backup branch; keep beta at the restored code unless the owner requests further changes. Do not merge, build an APK or run Actions unless requested.
