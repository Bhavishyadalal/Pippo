# Pippo design system

This design pass lives on `design/pippo-design-system`. The starting code is preserved on `main` and `backup/pre-design-system-2026-11-17` at `07346be3`.

## Visual direction

Dark neutral surfaces with a subtle plum tint, warm ivory text and restrained coral accents. Sage and lavender are secondary semantic colors. Original artwork, album-derived player colors and the selectable icon collection retain their own colors.

The shared definitions are in `ui/theme/PippoDesign.kt`. New components should use these tokens instead of introducing another near-identical palette.

| Role | Value |
| --- | --- |
| Page background | `#0E0D12` (black when the existing black-background preference is on) |
| Card / field | `#18161D` |
| Raised surface | `#231F2A` |
| Primary text | `#F4EFF5` |
| Supporting text | `#BBB4C3` |
| Muted text | `#9D95A7` |
| Accent | `#F2B5A7`, with dark `#35221F` text |
| Divider | `#332D3A` |

## Typography and layout

Inter remains the default, with the existing look-module font override preserved. Headings use bold or semibold weight and modest negative tracking. Body text uses normal weight and normal tracking. Labels use medium weight. Home no longer subtracts extra letter spacing from every text style.

- Main page headings: 32 sp / 40 sp line height.
- Section headings: 22 sp / 28 sp line height.
- Row titles: 16 sp / 24 sp; compact grouped settings retain 14 sp / 20 sp.
- Body: 16 sp / 24 sp or 14 sp / 20 sp.
- Supporting text: 12 sp / 18 sp.
- Spacing scale: 4, 8, 12, 16, 20, 24 and 32 dp.
- Page gutters: 24 dp. Cards: 20 dp corners. Groups: 24 dp. Sheets: 32 dp. Filters use consistent pill or rounded-card geometry.
- Search fields: 52 dp. The updated filters, overflow actions, settings back button and section play buttons have a minimum 48 dp target.

## Applied components

The Material theme owns the palette, typography and default shapes. Search, Library and Settings share the same surface/text values through the existing SearchColors API, preserving its callers. Search fields show a coral outline/cursor when focused. Home and Search page headings use the shared heading role.

Library and Search filters use the same selection color and short state transition. Their selected state is exposed to accessibility services. Existing wrapping in Library is retained for larger font/display settings. Shared filter chips use a stable corner shape rather than changing shape with selection.

Settings groups use the common corner radii and state-change duration, the primary text color for row titles, and supporting text that can occupy two lines when descriptions are enabled. Existing hide-description and long-press behavior remain available. Sub-settings use shared page margins, field height and a larger back target.

Section navigation titles use the text color, with the accent reserved for actions. The section play button has a larger target. Empty placeholders share an icon badge, centered supporting text and consistent spacing without adding a new action or changing existing empty-state copy.

Search and genre loading dots use one component. It stays still when system/app motion is disabled, the lifecycle is not started, or the player covers the page. Animation reads occur in drawing layers, and accessibility announcements describe loading rather than individual dots.

## Integration boundaries and review

Existing player styles, playback, queue, search queries, library operations, navigation destinations and persisted data use their existing implementations. The four rejected player screens remain removed. Shared theme changes can affect Material-based controls elsewhere in the app; visual review on a device is still needed, especially with larger text, look modules and black backgrounds.

No APK, build, test suite, verification script or GitHub Actions workflow was run for this design pass. Review is limited to source and diff inspection. No merge into `main` is part of this change.
