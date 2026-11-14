# Pippo maintenance

- The owner explicitly said not to run GitHub Actions. Do not dispatch, rerun or otherwise start a GitHub workflow unless the owner explicitly asks for that run. A request to edit or push code does not override this preference.
- Keep every workflow manual-only (`workflow_dispatch`, with `workflow_call` where needed). Do not add push, pull-request, schedule or other automatic triggers without the owner's explicit request. Prefer local verification; state blockers honestly.
- Production identity: **Pippo**, Android application ID/namespace `app.pippo.music`. Development is **Pippo Dev**, `app.pippo.music.debug`. Current initial version is 0.1.0 / code 1; increase version code for future published updates.
- `BUILDING.md` documents the manual APK workflow and production signing. Production CI must enable `pippoRequireReleaseSigning`; never silently ship a temporary debug key as production. Preserve the owner's signing key across updates, and keep all keystores/passwords out of Git.
- Preserve `LICENSE`, `NOTICE`, upstream authorship/history and third-party credits. Pippo product branding belongs in ordinary app UI; upstream references belong in provenance and credits.
- Namespace changes include the module API, native JNI symbols, ProGuard rules, manifests, widgets, launcher aliases, shortcuts and update configuration. Keep these consistent and run `scripts/check_pippo_branding.py` when changing identity/resources.
- The app updater must target `Bhavishyadalal/Pippo`. An artifact is not a published GitHub Release; never claim an APK release exists based on a source push.
- Keep existing playback, source routing, download/cache identities, DSP and module signature checks intact when adding features. Existing executable upstream modules are not automatically compatible with Pippo's API/signing identity.
- Do not claim real-device behavior, performance, a signed production build or passing tests without corresponding evidence. The initial source review under `docs/` is historical and predates the rebrand.
