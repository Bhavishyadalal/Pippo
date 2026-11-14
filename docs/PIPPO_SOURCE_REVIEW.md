# Pippo: Mochy source review

Reviewed 2026-11-14, before the initial Pippo import. This records that review checkpoint; subsequent import changes are described in the repository README. This is a static architecture review and development handoff, not a completed runtime audit.

## Source baseline

- Upstream: https://github.com/realidkroo/Mochy-Music
- Branch: main
- Reviewed commit: `eafbbe6d1c2a6590cbdfa045b42b572611bb2133`
- Gradle version label: `8.4.1 build#1256`; Android version code: 73.
- Local source: `/workspace/Mochy-Music`, shallow sparse checkout. Source, tests, native audio, selected resources, providers and module code are available; promotional assets and some other resources are omitted.
- Inventory: 2,370 tracked files; 1,214 Kotlin files; 28 JVM unit-test source files across app/modules/providers. Static count of `@Test` occurrences: 201. These are inventory counts, not executed test results.
- Source worktree was unchanged after review. No fork, Pippo repository, commit, push, APK, or deployment was created.

## Stack and build

`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `build.sh`:

- Android/Kotlin/Jetpack Compose app, with Hilt injection, Room, DataStore, coroutines/Flow, Coil, Ktor/OkHttp and Media3.
- JDK 21; Gradle 9.3.1; AGP 9.0.0; Kotlin 2.3.10; Compose 1.10.2; Media3 1.7.1; Room 2.8.4.
- Source build declares minSdk 24, compileSdk/targetSdk 37, NDK 27.0.12077973, CMake 3.22.1. README's Android 8+ headline does not capture the current minimum SDK declaration.
- Flavor dimensions: CPU ABI and service variant. Service variants include FOSS, GMS and F-Droid; GMS enables Google Cast. F-Droid reuses FOSS sources.
- Build types include debug, release, profileable and fast. Profileable derives from release, preserves optimization and permits profiling. Debug performance is not a valid shipped-performance baseline.
- Release signing falls back to the debug keystore when release signing inputs are absent. A successful local release build alone would not prove production signing.
- Native library `mochypass`: direct USB and exclusive AAudio output. JNI entry points encode the Kotlin package name.
- JitPack is among the required dependency repositories. Media3's bundled FFmpeg fallback is version-coupled to Media3.
- `build.sh` can edit build-notice source and publish/upload releases. Use explicit Gradle tasks for initial verification; inspect script flags before invoking automation.

## System map

Paths below are relative to `app/src/main/kotlin/com/mochyy/mochymusic/` unless otherwise noted. Large implementations were traced through key declarations and relevant execution paths, rather than claiming every line was audited.

| Area | Important files | Role and integration constraints |
| --- | --- | --- |
| Process startup | `App.kt`, `ui/launch/LaunchIntro.kt` | Pending transfer restore runs before Hilt opens data. Settings initialize providers/network behavior. LaunchGate delays heavier work; background/service-only startup has a bounded wait. |
| Navigation and service connection | `MainActivity.kt` | Activity binds/unbinds playback, disposes PlayerConnection, owns Compose navigation, search state, glass capture, player sheet and responsive panes. |
| Shared dependencies | `di/AppModule.kt`, `di/NetworkModule.kt` | Creates Room, separate player/download caches, executors and application scope. Database migration registration must remain consistent. |
| Playback owner | `playback/MusicService.kt` | MediaLibraryService, ExoPlayer creation, media session, queues, audio focus, stream routing, source failures/retries, metadata/history, widgets, crossmix and output. About 5,428 lines; high integration impact. |
| UI playback state | `playback/PlayerConnection.kt` | Player listener and Flow bridge. Owns a child scope and exposes readiness/transport state to UI. Preserve disposal and player replacement behavior. |
| Stream selection | `modules/SourceModules.kt`, `utils/YTPlayerUtils.kt` | Modules before YouTube are queried concurrently, but selection preserves source order with a shared 12-second waiting budget. YouTube uses InnerTubeX and client fallback paths. Downloads have their own order. |
| Media preparation | `playback/custom/DynamicResolvingMediaSource.kt` | Resolves media asynchronously, prepares children on the playback thread, uses prepare/release generations, and configures progressive/HLS/DASH/DRM playback. |
| Manual source changes | `playback/ProviderSwitch.kt` | Bounded repeated source lookup, holds/resumes playback, checks current song before switching. Must remain consistent with cache identity and stream headers. |
| Module contract | root `modules/api/.../MochyModule.kt` | API version 3. Supports audio sources, lyrics, looks and metadata; defines account, settings, request, matching and stream contracts. API package compatibility matters. |
| Module lifecycle | `modules/ModuleRegistry.kt`, `modules/web/WebModuleManifest.kt` | Built-ins, installed apps, web/ZIP modules, persistence, settings, account status and entry-point discovery. Installed module API version is checked. |
| Executable modules | `modules/code/CodeModules.kt`, root `modules/bridge/` | APK-bearing ZIP modules require exactly the host's current signing certificates. A new Pippo key changes compatibility. This is distinct from data/HTTP modules. |
| Skins and customization | `modules/skin/SkinParser.kt`, `SkinFiles.kt`, `constants/PreferenceKeys.kt` | Declarative player/pages/settings, fonts, assets and local shaders. Range/path/file limits exist; ZIP skins have bounded extraction. Preserve parser compatibility and saved preference values. |
| Home | `viewmodels/HomeViewModel.kt`, `HomeFeedCache.kt`, `ui/screens/HomeScreen.kt` | Saved shelves, local/history recommendations, online feeds, refresh mutexes and launch staging. Cache and account switching deserve targeted review before changing behavior. |
| Rendering | `ui/component/GlassKit.kt`, `ui/player/BottomSheetPlayer.kt` | Shared backdrop and damage boundaries, guarded newer graphics APIs, multiple player families. Profile blur, artwork, lyrics and transitions on device. |
| Library | `db/MusicDatabase.kt`, `DatabaseDao.kt`, `db/entities/` | Room schema 43, songs/playlists/artists/history/lyrics/formats/import data. Central AllMigrations list; destructive fallback remains configured. |
| Downloads | `playback/DownloadUtil.kt`, `ExoDownloadService.kt` | Media3 downloads, independent network resolution, source fallback, headers, stream expiry and consistent formats on resume. Player cache bytes cannot simply be mixed into another rendition's download. |
| File storage | `playback/StoragePlaces.kt`, `StorageMover.kt`, `DownloadExporter.kt` | Device/download/cache paths and exported music have separate ownership. Storage changes must consider already-saved files and interrupted moves. |
| Backup and transfer | `viewmodels/BackupRestoreViewModel.kt`, `importer/transfer/TransferRestore.kt`, `TransferArchive.kt` | Older DB/preferences ZIP path coexists with staged transfer restore before DB initialization. Formats and secret handling differ; don't promise cross-app migration without conversion tests. |
| Lyrics | `lyrics/LyricsHelper.kt`, `LyricsProviderRegistry.kt`, `LyricsUtils.kt` | Embedded/offline lyrics, concurrent providers, language/script and timing grading, module providers, cache refresh, alignment and translation integrations. Preserve lyric attribution and user edits. |
| Mixing and DSP | `crossmix/CrossmixPlanner.kt`, `playback/CrossmixDirector.kt`, `eq/`, `playback/audio/` | Beat/energy/vocal-aware planning, dual-player transitions, equalization/effects, loudness, silence and haptics. These affect output eligibility and timing. |
| Audio output | `playback/audio/pass/AudioPass.kt`, `PassBlockers.kt`, `PassNative.kt`, root `app/src/main/cpp/` | Mixer/exclusive/direct USB paths, capabilities, output status, DSP blockers and native bindings. Hardware validation required. |
| Accounts and networking | `utils/SecretPreferences.kt`, `SecretVault.kt`, `utils/AppProxy.kt`, `importer/account/`, `discord/` | Selected secrets encrypted using Android Keystore AES-GCM. Vault can fall back to plaintext when encryption is unavailable; account portability needs explicit design. |
| Phone/TV | `link/LinkWire.kt`, `link/phone/`, `link/tv/` | LAN discovery, pairing, signed requests, linked media and remote control; protocol version 2. Not equivalent to Veyra's internet Listen Together rooms. |
| Car, cast, widgets | `car/`, `widget/`, flavor-specific `playback/CastConnectionHandler.kt` | Media session and app identity extend beyond visible phone screens. Preserve flavor-specific behavior. |
| Updates | `core/updater/GithubUpdates.kt`, `UpdateChannel.kt`, `downloadmanager/UpdateDownloadWorker.kt` | Hardcoded upstream GitHub owner/repo, version parsing, release/nightly channels, APK selection/download and installation. Must be independently configured for Pippo. |

## Existing customization to retain

- Home styles: Mochyy, Listen now, Hop in.
- Player styles: Flagship, Classic, Turntable, Full video, iPod, Poster, Timeline, Lyrics stage, two bedside variants, plus active module players.
- Playlist styles: Flow, Classic, Minimal.
- Saved preferences govern blur, layout, transport, lyrics, motion and network behavior. Renaming enum values indiscriminately can break restoration even if the app compiles.
- Custom modules can contribute more than a color palette: layouts, pages, fonts, graphics and source behavior. Veyra's Theme Studio packs are not a drop-in Mochy skin format.

## Findings to carry into Pippo implementation

1. **Identity touches several contracts.** Define Pippo's application ID, namespace policy, signing, update repository and version scheme first. Review manifest providers, widgets, shortcuts, deep links, module metadata keys, LAN service names and native JNI exports. Distinguish compatibility identifiers from visible branding.
2. **Keep playback behavior as a baseline.** Preserve known source selection, matching, decoder fallback, headers, cache keys, offline enforcement and download resume semantics while changing appearance or adding sources.
3. **Avoid parallel playback stacks.** Adapt incoming features to MusicService/PlayerConnection and the module API. Copying another app's service alongside these would create competing queue/session/cache ownership.
4. **Module signing needs a deliberate policy.** Pippo-signed executable modules will be separate from Mochy-signed modules under the existing rule. Do not silently weaken signature checks to make old modules load.
5. **Data migration is its own feature.** Neither Mochy nor Veyra's private storage automatically becomes Pippo's after a package change. Inspect backup schemas, media references and device-bound credentials before offering import.
6. **Optimization must be measured.** Startup staging, image caching, drawing isolation and a profileable variant already exist. Candidate areas include glass redraws, animated covers/lyrics, startup I/O, cache pressure, source latency and crossmix CPU. No measured defect or speed improvement is claimed here.
7. **Existing verification is incomplete for a new fork.** CI build.yml assembles GMS debug; build_pr.yml assembles/lints FOSS debug. Neither explicitly invokes unit-test tasks. Lint is configured with abortOnError=false, so a green build alone is insufficient evidence of clean lint.
8. **Review hardening candidates before release.** The examined GitHub APK download path checks downloaded size but does not itself verify a release checksum; Android still enforces signing for in-place updates. The older restore path writes DB/preferences directly. Database destructive fallback and secret plaintext fallback deserve deliberate policies. These are static observations, not reproduced exploits or failures.
9. **Features from other apps need adapters.** Veyra's remote rooms and Theme Studio, and OpenTune's podcasts/radio/Subsonic and broader platforms, should be individually scoped. A targeted code search did not find equivalent full systems for internet rooms, Radio Browser or Subsonic in this Mochy snapshot; isolated podcast references do not establish a full podcast feature.
10. **Preserve upstream attribution.** GPL-3.0 license and existing copyright notices remain. Credit compatible imported code and retain provider-specific attribution. Pippo branding should not imply the upstream authors maintain this fork.

## Verification status and next development baseline

- Completed: source checkout, pinned commit, architecture/source-path review, dependency/flavor/signing inspection, static test inventory, workflow inspection, native binding and updater review, clean-worktree check.
- Not completed: full compile, unit-test execution, lint execution, APK installation, live provider checks, real-device rendering/audio/USB tests, memory/thermal/frame-time profiling.
- Workspace has JDK 21. `adb` and `sdkmanager` were not found on PATH; no SDK was found in the common locations inspected. Android build readiness is not established.
- Checkout is intentionally sparse. Expand all app resources/assets and other build inputs before compiling; it is not yet a complete offline Android Studio handoff.
- Initial verification should use an explicit FOSS variant, for example `:app:assembleUniversalFossDebug`, `:app:testUniversalFossDebugUnitTest`, and `:app:lintUniversalFossDebug`, plus tests for affected provider/module projects. Establish a release-equivalent profileable device baseline for performance work.
- This review authorizes no upstream edits or publishing. The next development stage can use a separate Pippo repo/branch, retain this source commit as provenance, establish a working baseline, then implement identity and features in focused increments.
