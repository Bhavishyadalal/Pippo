# Pippo

Your music, your little world.

Pippo is Bhavishya Dalal's independently maintained Android music player, based on Mochy Music. The app now uses its own identity throughout the interface, notifications, widgets, sharing and update configuration.

- **App:** Pippo · Android package `app.pippo.music`
- **Version:** 0.1.0 / code 1
- **Developer build:** Pippo Dev · `app.pippo.music.debug`
- **Owner:** [Bhavishyadalal](https://github.com/Bhavishyadalal)
- **Build an APK:** [GitHub Actions and local build instructions](BUILDING.md)

## Build from GitHub Actions

Open **Actions → Android Production APK → Run workflow**. Choose production/development, arm64/universal, and FOSS/GMS. Download the APK ZIP from the successful run's Artifacts section. Production needs the four signing secrets described in [BUILDING.md](BUILDING.md); development can run without them.

This source push does not itself publish a Pippo APK release. The updater checks this repository only.

## Source and credits

Pippo preserves Mochy's playback, modules, layouts and integrations while establishing its own app identity. All upstream contributors retain credit in the app's credits and [NOTICE](NOTICE). Existing launcher artwork is retained for this naming stage.

- [Mochy Music upstream](https://github.com/realidkroo/Mochy-Music), imported at `eafbbe6d1c2a6590cbdfa045b42b572611bb2133`
- [Original upstream README and acknowledgements](docs/UPSTREAM_MOCHY_README.md)
- [Initial architecture review](docs/PIPPO_SOURCE_REVIEW.md) (historical, before the namespace change)

GNU GPL v3 applies; see [LICENSE](LICENSE). Preserve upstream and third-party notices when redistributing.
