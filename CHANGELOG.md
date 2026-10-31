# Changelog

All notable changes to BiliClock. Versions follow semantic versioning; Android
`versionCode` is shown in parentheses.

## 1.3.1 (5) — 2026-11-01

- Bundled the supplied `Helvetica-Regular.ttf` (Helvetica Neue LT 55 Roman).
- All clock Latin text and digits now use one thin Helvetica weight.
- Weather temperature, humidity and percentage values are white; only
  non-numeric text keeps the light-blue accent.
- Subtitle amounts and the main follower count stay white/mint, not blue.
- Chinese text continues to use the system Noto Sans CJK fallback.

## 1.3.0 (4) — 2026-11-01

- Centered the follower count, like caption and weather text on the right-hand
  text axis; avatar and clock remain left-aligned.
- Removed weather animation and the “weather animation” switch. Weather icons
  are now static Meteocons vectors.
- Added a background setting: pure black (default) or a locally picked image.
  Images are center-cropped with a dark overlay.
- Fixed the night moon icon by rewriting its packed arc path.
- Replaced Noto Sans SC assets with a Latin-first font strategy.

## 1.2.0 (3) — 2026-10-31

- Reworked the composition to match the supplied reference: avatar and clock
  share a left edge, count and weather share a right edge, caption centered.
- Removed horizontal compression of long follower counts; shrink type size
  uniformly instead.
- Fixed the dark avatar caused by Paint alpha leakage.
- Fixed blank cloud icons by rewriting the cloud VectorDrawable with plain
  cubic Béziers.
- Rounded weather temperatures to whole degrees.

## 1.1.0 (2) — 2026-10-31

- Enlarged the avatar and aligned it with the clock.
- Added a SemiBold numeric font, refined subtitle weight/spacing.
- Moved the weather icons to Meteocons vector layers with animation.
- Added the weather animation switch.

## 1.0.0 (1) — 2026-10-31

- Initial native Android release: Bilibili profile, like count, analog clock,
  weather and settings.
- Android 6 TLS compatibility via bundled ISRG/GlobalSign roots.
