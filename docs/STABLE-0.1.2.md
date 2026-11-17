# Pippo stable 0.1.2 preparation

- Branch: `stable-0.1.2`.
- Based on beta commit `068574eeac1c98c55331521da2a5643186c22c92`.
- Version name: `0.1.2`, version code: `74` (previously `1.0.0`, code `73`). Android compares the increasing version code for installation upgrades.
- Package remains `com.bhavishyadalal.pippo`; stored data and the existing preferences/schema are retained.
- Main, `preview-beta-v0.1.2` and the backup branch are preserved. No merge or release tag/publication is performed.

Includes beta customization, seven mini-player choices, the main-icon startup handoff and cozy settings refinements. Internal resource identifiers and helper names containing `beta` are retained for compatibility; they do not add a beta suffix to the installed version.

## Optimized release configuration

Retains R8 minification/optimization with `proguard-android-optimize.txt`, resource shrinking, the existing app baseline profile and release Compose source-information stripping. Release explicitly disables both debugging and profiling. The separate profileable development variant still enables profiling. Compose inspection tooling is debug-only; runtime trace tooling is profileable-only. Release does not plant the debug Timber tree.

These are production build settings, not evidence of measured performance. No existing visual preferences are forcibly reset and no additional effects are enabled by promotion. The Light preset remains available for devices that benefit from fewer effects.

## Manual APK build later

Open Actions → **Optimized Release APK** → **Run workflow**, select branch **stable-0.1.2**, then choose ARM64 for supported modern phones or Universal for broader ABI support. This uses the existing release task rather than the debug/fast variants. No workflow is started as part of this preparation.

Auto signing uses the private key when all signing secrets are supplied. With no secrets, the existing builder can create a temporary signed test APK with the same release optimizations. That temporary key changes between builds, so it cannot update an installation signed with another key. Keep the same private key for installable updates; a higher version code cannot bypass a signing mismatch. No secrets are generated or exposed by this change.

## Review limits

Source/diff review only. No tests, check scripts, Gradle configuration/compilation, APK builds or GitHub Actions were run. The stable branch name/version is for the owner's testing; rendering, device performance, accessibility and stability are not certified. Existing beta feature notes document remaining device-review needs.
