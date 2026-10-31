package com.biliclock.deskclock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Open-Meteo, QWeather-compatible, and fully custom JSON weather backends. */
public final class WeatherClient {
    private static final String GEOCODING_API =
            "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=8&language=zh&format=json";

    private WeatherClient() {
    }

    public static WeatherData fetch(Prefs prefs) throws IOException {
        String provider = prefs.getProvider();
        if (Prefs.PROVIDER_QWEATHER.equals(provider)) {
            return fetchQWeather(prefs);
        }
        if (Prefs.PROVIDER_CUSTOM.equals(provider)) {
            return fetchCustom(prefs);
        }
        return fetchOpenMeteo(prefs);
    }

    private static WeatherData fetchOpenMeteo(Prefs prefs) throws IOException {
        String base = stripTrailingSlash(prefs.getOpenMeteoBase());
        String endpoint = base.endsWith("/v1/forecast") ? base : base + "/v1/forecast";
        String query = endpoint
                + "?latitude=" + encode(Double.toString(prefs.getLatitude()))
                + "&longitude=" + encode(Double.toString(prefs.getLongitude()))
                + "&current=temperature_2m,relative_humidity_2m,weather_code,is_day"
                + "&timezone=auto";
        try {
            JSONObject root = Http.getJson(query, null);
            if (root.optBoolean("error", false)) {
                throw new IOException(root.optString("reason", "天气服务返回错误"));
            }
            JSONObject current = root.optJSONObject("current");
            if (current == null) {
                JSONObject legacy = root.optJSONObject("current_weather");
                if (legacy == null) {
                    throw new IOException("天气服务没有返回当前天气");
                }
                double temp = legacy.optDouble("temperature", Double.NaN);
                int code = legacy.optInt("weathercode", -1);
                return data(prefs, temp, WeatherCode.textForWmo(code),
                        WeatherCode.iconForWmo(code, true), -1);
            }
            double temp = current.optDouble("temperature_2m", Double.NaN);
            int humidity = current.has("relative_humidity_2m")
                    ? current.optInt("relative_humidity_2m", -1) : -1;
            int code = current.optInt("weather_code", -1);
            boolean day = current.optInt("is_day", 1) == 1;
            if (Double.isNaN(temp)) {
                throw new IOException("天气服务没有返回温度");
            }
            return data(prefs, temp, WeatherCode.textForWmo(code),
                    WeatherCode.iconForWmo(code, day), humidity);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("解析 Open-Meteo 天气失败", e);
        }
    }

    private static WeatherData fetchQWeather(Prefs prefs) throws IOException {
        String host = stripTrailingSlash(prefs.getQWeatherHost());
        String endpoint = host.endsWith("/v7/weather/now") ? host : host + "/v7/weather/now";
        String location = prefs.getLongitude() + "," + prefs.getLatitude();
        String url = endpoint
                + "?location=" + encode(location)
                + "&key=" + encode(prefs.getQWeatherKey())
                + "&lang=zh&unit=m";
        try {
            JSONObject root = Http.getJson(url, null);
            String code = root.optString("code", "");
            if (!"200".equals(code)) {
                throw new IOException("和风天气错误 " + code + " · " + root.optString("message", ""));
            }
            JSONObject now = root.optJSONObject("now");
            if (now == null) {
                throw new IOException("和风天气没有返回 now 数据");
            }
            double temp = parseDouble(now.opt("temp"), Double.NaN);
            String text = now.optString("text", "天气");
            String rawIcon = now.optString("icon", "");
            int humidity = (int) parseDouble(now.opt("humidity"), -1);
            String icon = WeatherCode.iconForQWeather(rawIcon, text);
            if (Double.isNaN(temp)) {
                throw new IOException("和风天气没有返回温度");
            }
            return data(prefs, temp, text, icon, humidity);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("解析和风天气失败", e);
        }
    }

    private static WeatherData fetchCustom(Prefs prefs) throws IOException {
        String template = prefs.getCustomUrl().trim();
        if (template.isEmpty()) {
            throw new IOException("尚未填写自定义天气地址");
        }
        String key = prefs.getCustomKey();
        String url = template
                .replace("{lat}", encode(Double.toString(prefs.getLatitude())))
                .replace("{lon}", encode(Double.toString(prefs.getLongitude())))
                .replace("{city}", encode(prefs.getCity()))
                .replace("{key}", encode(key));
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        if (!key.isEmpty()) {
            headers.put("X-API-Key", key);
        }
        try {
            JSONObject root = Http.getJson(url, headers);
            Object tempNode = getPathOrFind(root, prefs.getCustomTempPath(),
                    "temp", "temperature", "temperature_2m", "temp_c", "tempC");
            double temp = parseDouble(tempNode, Double.NaN);
            Object textNode = getPathOrFind(root, prefs.getCustomTextPath(),
                    "text", "condition", "weather", "desc", "description");
            String text = textNode == null ? "天气" : String.valueOf(textNode);
            Object humidityNode = getPathOrFind(root, prefs.getCustomHumidityPath(),
                    "humidity", "rh", "relative_humidity", "relative_humidity_2m");
            int humidity = (int) parseDouble(humidityNode, -1);
            Object iconNode = getPathOrFind(root, prefs.getCustomIconPath(),
                    "icon", "iconType", "weather_icon", "code");
            String icon = WeatherCode.normalizeIcon(
                    iconNode == null ? "" : String.valueOf(iconNode), text);
            if (Double.isNaN(temp)) {
                throw new IOException("自定义接口没有找到温度字段");
            }
            return data(prefs, temp, text, icon, humidity);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("解析自定义天气失败", e);
        }
    }

    public static List<Place> searchCities(String query) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String url = String.format(Locale.ROOT, GEOCODING_API,
                URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name()));
        try {
            JSONObject root = Http.getJson(url, null);
            JSONArray results = root.optJSONArray("results");
            List<Place> places = new ArrayList<>();
            if (results == null) {
                return places;
            }
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                StringBuilder region = new StringBuilder();
                appendRegion(region, item.optString("admin1", ""));
                appendRegion(region, item.optString("admin2", ""));
                appendRegion(region, item.optString("country", ""));
                places.add(new Place(
                        item.optString("name", ""),
                        region.toString(),
                        item.optDouble("latitude", 0),
                        item.optDouble("longitude", 0)));
            }
            return places;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("解析城市搜索结果失败", e);
        }
    }

    private static WeatherData data(Prefs prefs, double temp, String text, String icon, int humidity) {
        return new WeatherData(prefs.getCity(), text, icon, temp, humidity, System.currentTimeMillis());
    }

    private static void appendRegion(StringBuilder sb, String part) {
        if (part == null || part.trim().isEmpty()) {
            return;
        }
        String value = part.trim();
        if (sb.indexOf(value) >= 0) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" · ");
        }
        sb.append(value);
    }

    private static Object getPathOrFind(JSONObject root, String path, String... fallbackKeys) {
        if (path != null && !path.trim().isEmpty()) {
            Object value = getPath(root, path.trim());
            if (value != null) {
                return value;
            }
        }
        return findRecursive(root, fallbackKeys, 0);
    }

    private static Object getPath(JSONObject root, String path) {
        String normalized = path.replace("[", ".").replace("]", "");
        String[] parts = normalized.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).opt(part);
            } else if (current instanceof JSONArray) {
                int index;
                try {
                    index = Integer.parseInt(part);
                } catch (Exception e) {
                    return null;
                }
                current = ((JSONArray) current).opt(index);
            } else {
                return null;
            }
            if (current == null || current == JSONObject.NULL) {
                return null;
            }
        }
        return current;
    }

    private static Object findRecursive(JSONObject object, String[] keys, int depth) {
        if (object == null || depth > 5) {
            return null;
        }
        for (String key : keys) {
            if (object.has(key) && !object.isNull(key)) {
                return object.opt(key);
            }
        }
        JSONArray names = object.names();
        if (names == null) {
            return null;
        }
        for (int i = 0; i < names.length(); i++) {
            String name = names.optString(i, "");
            Object child = object.opt(name);
            if (child instanceof JSONObject) {
                Object found = findRecursive((JSONObject) child, keys, depth + 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static double parseDouble(Object value, double fallback) {
        if (value == null || value == JSONObject.NULL) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = String.valueOf(value).trim()
                .replace("°C", "")
                .replace("℃", "")
                .replace("%", "")
                .replace(",", ".")
                .trim();
        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (Exception e) {
            return "";
        }
    }

    private static String stripTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
