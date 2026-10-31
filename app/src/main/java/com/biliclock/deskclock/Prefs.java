package com.biliclock.deskclock;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/** Single source of truth for user settings and the on-disk display cache. */
public final class Prefs {
    public static final String PROVIDER_OPEN_METEO = "openmeteo";
    public static final String PROVIDER_QWEATHER = "qweather";
    public static final String PROVIDER_CUSTOM = "custom";

    public static final String SUBTITLE_LIKES = "likes";
    public static final String SUBTITLE_FANS = "fans";

    public static final String BACKGROUND_BLACK = "black";
    public static final String BACKGROUND_IMAGE = "image";

    public static final String DEFAULT_UID = "2";
    public static final String DEFAULT_CITY = "中山";
    public static final double DEFAULT_LAT = 22.517;
    public static final double DEFAULT_LON = 113.392;
    public static final String DEFAULT_OPEN_METEO = "https://api.open-meteo.com";
    public static final String DEFAULT_QWEATHER_HOST = "https://devapi.qweather.com";

    private static final String FILE = "biliclock_prefs";

    private static final String K_UID = "uid";
    private static final String K_CITY = "city";
    private static final String K_LAT = "lat";
    private static final String K_LON = "lon";
    private static final String K_PROVIDER = "provider";
    private static final String K_OPEN_METEO = "open_meteo_base";
    private static final String K_QWEATHER_HOST = "qweather_host";
    private static final String K_QWEATHER_KEY = "qweather_key";
    private static final String K_CUSTOM_URL = "custom_url";
    private static final String K_CUSTOM_KEY = "custom_key";
    private static final String K_CUSTOM_TEMP = "custom_temp";
    private static final String K_CUSTOM_TEXT = "custom_text";
    private static final String K_CUSTOM_HUMIDITY = "custom_humidity";
    private static final String K_CUSTOM_ICON = "custom_icon";
    private static final String K_SUBTITLE = "subtitle_mode";
    private static final String K_REFRESH = "refresh_minutes";
    private static final String K_KEEP_ON = "keep_screen_on";
    private static final String K_SECOND = "show_second";
    private static final String K_BACKGROUND_MODE = "background_mode";
    private static final String K_REVISION = "settings_revision";
    private static final String K_HINT_SHOWN = "hint_shown";

    private static final String K_CACHE_BILI_UID = "cache_bili_uid";
    private static final String K_CACHE_FANS = "cache_fans";
    private static final String K_CACHE_LIKES = "cache_likes";
    private static final String K_CACHE_NAME = "cache_name";
    private static final String K_CACHE_AVATAR_URL = "cache_avatar_url";
    private static final String K_CACHE_BILI_TIME = "cache_bili_time";
    private static final String K_CACHE_WEATHER_CITY = "cache_weather_city";
    private static final String K_CACHE_WEATHER_CONDITION = "cache_weather_condition";
    private static final String K_CACHE_WEATHER_ICON = "cache_weather_icon";
    private static final String K_CACHE_WEATHER_TEMP = "cache_weather_temp";
    private static final String K_CACHE_WEATHER_HUMIDITY = "cache_weather_humidity";
    private static final String K_CACHE_WEATHER_TIME = "cache_weather_time";

    private final Context appContext;
    private final SharedPreferences sp;

    public Prefs(Context context) {
        appContext = context.getApplicationContext();
        sp = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public String getUid() {
        String value = sp.getString(K_UID, DEFAULT_UID);
        return value == null || value.trim().isEmpty() ? DEFAULT_UID : value.trim();
    }

    public void setUid(String value) {
        sp.edit().putString(K_UID, value == null ? DEFAULT_UID : value.trim()).apply();
    }

    public String getCity() {
        String value = sp.getString(K_CITY, DEFAULT_CITY);
        return value == null || value.trim().isEmpty() ? DEFAULT_CITY : value.trim();
    }

    public void setCity(String value) {
        sp.edit().putString(K_CITY, value == null ? DEFAULT_CITY : value.trim()).apply();
    }

    public double getLatitude() {
        return parseDouble(sp.getString(K_LAT, Double.toString(DEFAULT_LAT)), DEFAULT_LAT);
    }

    public double getLongitude() {
        return parseDouble(sp.getString(K_LON, Double.toString(DEFAULT_LON)), DEFAULT_LON);
    }

    public void setLocation(double latitude, double longitude) {
        sp.edit()
                .putString(K_LAT, Double.toString(latitude))
                .putString(K_LON, Double.toString(longitude))
                .apply();
    }

    public String getProvider() {
        String value = sp.getString(K_PROVIDER, PROVIDER_OPEN_METEO);
        if (PROVIDER_QWEATHER.equals(value) || PROVIDER_CUSTOM.equals(value)) {
            return value;
        }
        return PROVIDER_OPEN_METEO;
    }

    public void setProvider(String provider) {
        sp.edit().putString(K_PROVIDER, provider).apply();
    }

    public String getOpenMeteoBase() {
        return Format.nonEmpty(sp.getString(K_OPEN_METEO, DEFAULT_OPEN_METEO), DEFAULT_OPEN_METEO);
    }

    public void setOpenMeteoBase(String value) {
        sp.edit().putString(K_OPEN_METEO, Format.nonEmpty(value, DEFAULT_OPEN_METEO)).apply();
    }

    public String getQWeatherHost() {
        return Format.nonEmpty(sp.getString(K_QWEATHER_HOST, DEFAULT_QWEATHER_HOST), DEFAULT_QWEATHER_HOST);
    }

    public void setQWeatherHost(String value) {
        sp.edit().putString(K_QWEATHER_HOST, Format.nonEmpty(value, DEFAULT_QWEATHER_HOST)).apply();
    }

    public String getQWeatherKey() {
        return sp.getString(K_QWEATHER_KEY, "");
    }

    public void setQWeatherKey(String value) {
        sp.edit().putString(K_QWEATHER_KEY, value == null ? "" : value.trim()).apply();
    }

    public String getCustomUrl() {
        return sp.getString(K_CUSTOM_URL, "");
    }

    public void setCustomUrl(String value) {
        sp.edit().putString(K_CUSTOM_URL, value == null ? "" : value.trim()).apply();
    }

    public String getCustomKey() {
        return sp.getString(K_CUSTOM_KEY, "");
    }

    public void setCustomKey(String value) {
        sp.edit().putString(K_CUSTOM_KEY, value == null ? "" : value.trim()).apply();
    }

    public String getCustomTempPath() {
        return sp.getString(K_CUSTOM_TEMP, "");
    }

    public String getCustomTextPath() {
        return sp.getString(K_CUSTOM_TEXT, "");
    }

    public String getCustomHumidityPath() {
        return sp.getString(K_CUSTOM_HUMIDITY, "");
    }

    public String getCustomIconPath() {
        return sp.getString(K_CUSTOM_ICON, "");
    }

    public void setCustomPaths(String temp, String text, String humidity, String icon) {
        sp.edit()
                .putString(K_CUSTOM_TEMP, clean(temp))
                .putString(K_CUSTOM_TEXT, clean(text))
                .putString(K_CUSTOM_HUMIDITY, clean(humidity))
                .putString(K_CUSTOM_ICON, clean(icon))
                .apply();
    }

    public String getSubtitleMode() {
        return SUBTITLE_FANS.equals(sp.getString(K_SUBTITLE, SUBTITLE_LIKES))
                ? SUBTITLE_FANS : SUBTITLE_LIKES;
    }

    public void setSubtitleMode(String mode) {
        sp.edit().putString(K_SUBTITLE, SUBTITLE_FANS.equals(mode) ? SUBTITLE_FANS : SUBTITLE_LIKES).apply();
    }

    public int getRefreshMinutes() {
        int value = sp.getInt(K_REFRESH, 15);
        if (value < 5) {
            return 5;
        }
        if (value > 120) {
            return 120;
        }
        return value;
    }

    public void setRefreshMinutes(int minutes) {
        sp.edit().putInt(K_REFRESH, Math.max(5, Math.min(120, minutes))).apply();
    }

    public boolean isKeepScreenOn() {
        return sp.getBoolean(K_KEEP_ON, true);
    }

    public void setKeepScreenOn(boolean value) {
        sp.edit().putBoolean(K_KEEP_ON, value).apply();
    }

    public boolean isShowSecondHand() {
        return sp.getBoolean(K_SECOND, true);
    }

    public void setShowSecondHand(boolean value) {
        sp.edit().putBoolean(K_SECOND, value).apply();
    }

    public String getBackgroundMode() {
        String value = sp.getString(K_BACKGROUND_MODE, BACKGROUND_BLACK);
        return BACKGROUND_IMAGE.equals(value) ? BACKGROUND_IMAGE : BACKGROUND_BLACK;
    }

    public void setBackgroundMode(String mode) {
        sp.edit().putString(K_BACKGROUND_MODE,
                BACKGROUND_IMAGE.equals(mode) ? BACKGROUND_IMAGE : BACKGROUND_BLACK).apply();
    }

    public File backgroundFile() {
        return new File(appContext.getFilesDir(), "background.img");
    }

    public File pendingBackgroundFile() {
        return new File(appContext.getFilesDir(), "background_pending.img");
    }

    public boolean hasBackgroundImage() {
        return backgroundFile().isFile();
    }

    public void clearBackground() {
        sp.edit().remove(K_BACKGROUND_MODE).apply();
        //noinspection ResultOfMethodCallIgnored
        backgroundFile().delete();
        //noinspection ResultOfMethodCallIgnored
        pendingBackgroundFile().delete();
    }

    public int getRevision() {
        return sp.getInt(K_REVISION, 0);
    }

    public void bumpRevision() {
        sp.edit().putInt(K_REVISION, getRevision() + 1).apply();
    }

    public boolean isHintShown() {
        return sp.getBoolean(K_HINT_SHOWN, false);
    }

    public void setHintShown(boolean shown) {
        sp.edit().putBoolean(K_HINT_SHOWN, shown).apply();
    }

    // ---------------------------------------------------------------- cache

    public void saveBili(BiliData data) {
        sp.edit()
                .putString(K_CACHE_BILI_UID, data.uid)
                .putLong(K_CACHE_FANS, data.fans)
                .putLong(K_CACHE_LIKES, data.likes)
                .putString(K_CACHE_NAME, data.name)
                .putString(K_CACHE_AVATAR_URL, data.avatarUrl)
                .putLong(K_CACHE_BILI_TIME, data.fetchedAt)
                .apply();
    }

    public BiliData loadBili(String uid) {
        String cachedUid = sp.getString(K_CACHE_BILI_UID, "");
        if (cachedUid.isEmpty() || !cachedUid.equals(uid)) {
            return null;
        }
        long time = sp.getLong(K_CACHE_BILI_TIME, 0);
        if (time == 0) {
            return null;
        }
        return new BiliData(
                cachedUid,
                sp.getString(K_CACHE_NAME, ""),
                sp.getString(K_CACHE_AVATAR_URL, ""),
                sp.getLong(K_CACHE_FANS, -1),
                sp.getLong(K_CACHE_LIKES, -1),
                time);
    }

    public void saveWeather(WeatherData data) {
        sp.edit()
                .putString(K_CACHE_WEATHER_CITY, data.city)
                .putString(K_CACHE_WEATHER_CONDITION, data.condition)
                .putString(K_CACHE_WEATHER_ICON, data.iconType)
                .putString(K_CACHE_WEATHER_TEMP, Double.toString(data.temperatureC))
                .putInt(K_CACHE_WEATHER_HUMIDITY, data.humidity)
                .putLong(K_CACHE_WEATHER_TIME, data.fetchedAt)
                .apply();
    }

    public WeatherData loadWeather(String expectedCity) {
        long time = sp.getLong(K_CACHE_WEATHER_TIME, 0);
        if (time == 0) {
            return null;
        }
        String cachedCity = sp.getString(K_CACHE_WEATHER_CITY, "");
        if (expectedCity != null && !expectedCity.isEmpty() && !expectedCity.equals(cachedCity)) {
            return null;
        }
        return new WeatherData(
                cachedCity,
                sp.getString(K_CACHE_WEATHER_CONDITION, ""),
                sp.getString(K_CACHE_WEATHER_ICON, WeatherCode.ICON_PARTLY),
                parseDouble(sp.getString(K_CACHE_WEATHER_TEMP, "NaN"), Double.NaN),
                sp.getInt(K_CACHE_WEATHER_HUMIDITY, -1),
                time);
    }

    public long getBiliCacheTime() {
        return sp.getLong(K_CACHE_BILI_TIME, 0);
    }

    public long getWeatherCacheTime() {
        return sp.getLong(K_CACHE_WEATHER_TIME, 0);
    }

    public String getCachedAvatarUrl() {
        return sp.getString(K_CACHE_AVATAR_URL, "");
    }

    public File avatarFile(String uid) {
        String safe = uid == null ? "default" : uid.replaceAll("[^0-9A-Za-z_-]", "_");
        return new File(new File(appContext.getFilesDir(), "avatars"), safe + ".img");
    }

    public void clearCache() {
        sp.edit()
                .remove(K_CACHE_BILI_UID)
                .remove(K_CACHE_FANS)
                .remove(K_CACHE_LIKES)
                .remove(K_CACHE_NAME)
                .remove(K_CACHE_AVATAR_URL)
                .remove(K_CACHE_BILI_TIME)
                .remove(K_CACHE_WEATHER_CITY)
                .remove(K_CACHE_WEATHER_CONDITION)
                .remove(K_CACHE_WEATHER_ICON)
                .remove(K_CACHE_WEATHER_TEMP)
                .remove(K_CACHE_WEATHER_HUMIDITY)
                .remove(K_CACHE_WEATHER_TIME)
                .apply();
        File dir = new File(appContext.getFilesDir(), "avatars");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    public void resetAll() {
        boolean hintShown = isHintShown();
        sp.edit().clear().apply();
        if (hintShown) {
            sp.edit().putBoolean(K_HINT_SHOWN, true).apply();
        }
        clearCache();
        clearBackground();
        bumpRevision();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
