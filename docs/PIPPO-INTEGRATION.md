# Pippo artwork and branding integration

- Icon 01 is the default. Icons 01–10 are portraits; 11–20 include the Pippo name. All twenty are selectable through Customisation → Icons.
- Each icon has legacy and adaptive launcher resources. A neutral white three-bar music mark is used for notifications, themed icons, navigation and the launch animation.
- Character 02 (female) and 06 (male) each have welcome, listening, thanks and additional waving illustrations. Production images are resized WebP resources, loaded through Coil.
- A random character and welcome variation are selected once per process. Rotation and recomposition do not reroll them. Artwork is used in phone onboarding, Android TV onboarding and About.
- Old launcher aliases, original onboarding sprite layers, owner portraits and unused large wrapped illustrations were removed. Icon changes enable the new launcher component before disabling the old ones; selection is saved only after success.
- Pippo uses its own Android application ID and matching shortcut packages for each build variant. Internal Kotlin namespace, database/protocol names and module interfaces stay compatible with source integrations.
- Upstream update fetching and downloads, repository statistics, donation prompts and owner social/profile links are disabled or removed. Legacy update/donation sheet entry points open the offline About sheet. Required GPL license text remains available there; attribution is in the root NOTICE.md.
- No tests, builds, verification scripts or GitHub Actions were run. Review was limited to source, resource and diff inspection. Compilation, launcher switching and device rendering still require a manual APK build and device testing by the owner.
