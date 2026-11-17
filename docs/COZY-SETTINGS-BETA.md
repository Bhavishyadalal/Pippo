# Cozy settings refinement

Branch: `preview-beta-v0.1.2`.

The settings hub now has a small Pippo character welcome card and three visual groups: Sound and listening, Your little space, and Library and connections. Category order and destinations are retained. Category cards use softly tinted icon tiles, fine outlines, selected-page treatment and navigation arrows. Optional category explanations follow the existing hide-descriptions preference. Module-supplied categories keep the same function interface and can omit a subtitle.

Playback, now-playing, behavior and other pages using the shared settings group inherit tonal card surfaces, gentle outlines, accent icon tiles, honored caller-provided icon shapes and softly colored nested groups. Shared section titles and numerical-value badges improve the hierarchy. The title in a value row now takes the remaining width, avoiding an unweighted long title crowding its value.

Beta customization cards have icon-led accordion headers, chevrons, and current-choice summaries for mini-player style, navigation surface, accent/font, transition and character. Details remain composed only while expanded. The other header captions explain their purpose without implying that a performance preset is currently active. Choice menus visually identify the selected item. Existing apply, import/export, reset and playback behavior is unchanged.

Shared settings search fields have a leading search mark, a fine outline, theme-aware text/cursor colors and a 48dp clear target. Empty results in the hub and customization page use a small help card. Search matching, highlight identifiers, scroll handling, description-on-hold and page transitions are retained.

Existing approved character artwork, theme colors, typography, corners and accent choices are reused. The character illustration yields to text on narrow layouts or large combined system/app text scaling and respects hidden-character selection. Decorative circles and surface gradients are static; no new polling, perpetual animation, shader, network image or image-generation dependency is added. Typography and rows can grow instead of relying on the hub's former fixed 64dp category height.

Review is limited to source and diff inspection. No tests, check scripts, compilation, APK builds or GitHub Actions were run. Device rendering, accessibility, large-font behavior and performance remain unverified. Main and the backup branch are preserved.
