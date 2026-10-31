# Configuration

All settings are stored locally in `SharedPreferences`. API keys and picked
background images never leave the device.

## Bilibili profile

`UID` is the numeric Bilibili user id (for example `2`). The app calls:

```text
https://api.bilibili.com/x/web-interface/card?mid={uid}
```

The main number is `data.follower`; the caption is `data.like_num` (or the
follower count, switchable in settings). The avatar is `data.card.face`.
Responses are cached in `SharedPreferences` and the avatar is cached as a PNG
under the app's private files directory.

If the request fails, the last successful cache is shown. The request is
refreshed after the configured interval (5–120 minutes), on app resume when
the settings revision changes, and when “保存并应用” is pressed.

## Weather

### City and coordinates

Use “搜索城市” to search Open-Meteo geocoding:

```text
https://geocoding-api.open-meteo.com/v1/search?name={city}&count=8&language=zh&format=json
```

Selecting a result fills the city name, latitude and longitude. Coordinates
can also be edited manually. The default is Zhongshan, Guangdong
(`22.517, 113.392`).

### Open-Meteo (default, no key)

Base URL:

```text
https://api.open-meteo.com
```

Request:

```text
/v1/forecast?latitude={lat}&longitude={lon}
  &current=temperature_2m,relative_humidity_2m,weather_code,is_day
  &timezone=auto
```

Fields read: `current.temperature_2m`,
`current.relative_humidity_2m`, `current.weather_code`, `current.is_day`.
Older `current_weather` responses are also accepted.

### QWeather / compatible API

Host and key are configured in settings. Request:

```text
/v7/weather/now?location={lon},{lat}&key={key}&lang=zh&unit=m
```

Fields read: `now.temp`, `now.text`, `now.humidity`, `now.icon`. A non-`200`
`code` is treated as an error.

### Custom JSON endpoint

URL template placeholders:

| Placeholder | Value |
| --- | --- |
| `{lat}` | latitude |
| `{lon}` | longitude |
| `{city}` | city name |
| `{key}` | custom API key |

Example:

```text
https://example.com/weather?lat={lat}&lon={lon}&city={city}&key={key}
```

The custom key is also sent as the `X-API-Key` header. Field paths use dots,
for example `data.temp`. If a path is blank, the app recursively looks for
common keys:

- temperature: `temp`, `temperature`, `temperature_2m`, `temp_c`, `tempC`
- condition: `text`, `condition`, `weather`, `desc`, `description`
- humidity: `humidity`, `rh`, `relative_humidity`, `relative_humidity_2m`
- icon: `icon`, `iconType`, `weather_icon`, `code`

Icon values:

```text
sunny / partly / cloudy / drizzle / rain / thunder / snow / fog / night
```

## Background

- **纯黑** (default): no image is drawn; suitable for OLED.
- **自定义图片**: “选择图片” opens the system document picker. The chosen image
  is copied into app-private storage as a JPEG (max edge 2560 px), then drawn
  center-cropped with a dark overlay for text contrast. No storage permission
  is required.

The mode is saved with “保存并应用”. Selecting pure black keeps the stored
image so it can be re-enabled later. “恢复默认” deletes both the mode and image.

## Display and refresh

- `常亮显示`: keep the screen awake.
- `显示秒针`: show/hide the analog second hand. The clock itself continues to
  update every second.
- `刷新间隔`: 5/10/15/30/60 minutes. Weather animation has been removed; the
  Meteocons weather glyph is static.

## Android 6 HTTPS

Android 6's system trust store may not contain the roots used by the Bilibili
and weather endpoints. The app bundles:

- ISRG Root X1
- ISRG Root X2
- GlobalSign Root R46

and uses a composite trust manager for `HttpsURLConnection`. Normal system
validation remains enabled for other hosts.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Profile does not load | UID is numeric, network is available, API not rate-limited; cached data may be shown |
| Weather does not load | Coordinates, provider host/key, and endpoint response; cached data may be shown |
| Custom endpoint fails | URL requires `http://`/`https://`; field paths and JSON types |
| Background image does not appear | Pick a JPG/PNG; very large images are downscaled; selecting pure black disables the image |
| Chinese shows boxes | The device lacks a CJK fallback font; install/enable a system CJK font |
