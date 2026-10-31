# Third-party notices

BiliClock contains original code plus third-party fonts, icons and certificates.
The MIT license in `LICENSE` covers the original code only.

## Fonts

### Helvetica Neue LT 55 Roman

- File: `app/src/main/assets/Helvetica-Regular.ttf`
- Supplied by the project owner.
- Helvetica is a trademark of Monotype. This file is **not** covered by the
  project's MIT license. Confirm that you have redistribution rights before
  publishing the repository or an APK containing it.
- If you do not have those rights, replace it with a font you can redistribute
  (for example Arimo, Liberation Sans or the platform sans-serif fallback) and
  rebuild.

### Noto Sans CJK

Chinese characters are not present in Helvetica. Android falls back to the
system CJK font (Noto Sans CJK on Android 6+); that font is not distributed
with this project.

## Weather icons

**Meteocons** by Bas Milius — MIT License.

- Source: <https://github.com/basmilius/weather-icons>
- License text: `app/src/main/assets/METEOCONS-LICENSE.txt`

The vector layers were adapted into Android VectorDrawables and a few paths
were redrawn with cubic Béziers so they parse on older Android versions.

## Certificates

- ISRG Root X1
- ISRG Root X2
- GlobalSign Root R46

These public root certificates are bundled to keep HTTPS working on Android 6
devices whose system trust store predates the current certificate chains.

## Data sources and trademarks

- Bilibili profile data comes from the public endpoint
  `https://api.bilibili.com/x/web-interface/card`.
- Weather data can come from Open-Meteo, QWeather-compatible services, or a
  user-provided JSON endpoint.
- Bilibili, Open-Meteo and QWeather are trademarks of their respective owners.
  This is an unofficial project and is not affiliated with any of them.
