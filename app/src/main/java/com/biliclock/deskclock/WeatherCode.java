package com.biliclock.deskclock;

import java.util.Locale;

/** Maps WMO / QWeather weather codes to Chinese labels and drawable icon types. */
public final class WeatherCode {
    public static final String ICON_SUNNY = "sunny";
    public static final String ICON_PARTLY = "partly";
    public static final String ICON_CLOUDY = "cloudy";
    public static final String ICON_DRIZZLE = "drizzle";
    public static final String ICON_RAIN = "rain";
    public static final String ICON_THUNDER = "thunder";
    public static final String ICON_SNOW = "snow";
    public static final String ICON_FOG = "fog";
    public static final String ICON_NIGHT = "night";

    private WeatherCode() {
    }

    public static String textForWmo(int code) {
        switch (code) {
            case 0:
                return "晴";
            case 1:
                return "晴间多云";
            case 2:
                return "多云";
            case 3:
                return "阴";
            case 45:
            case 48:
                return "雾";
            case 51:
                return "小毛毛雨";
            case 53:
                return "毛毛雨";
            case 55:
                return "浓毛毛雨";
            case 56:
            case 57:
                return "冻毛毛雨";
            case 61:
                return "小雨";
            case 63:
                return "中雨";
            case 65:
                return "大雨";
            case 66:
            case 67:
                return "冻雨";
            case 71:
                return "小雪";
            case 73:
                return "中雪";
            case 75:
                return "大雪";
            case 77:
                return "雪粒";
            case 80:
                return "小阵雨";
            case 81:
                return "阵雨";
            case 82:
                return "强阵雨";
            case 85:
            case 86:
                return "阵雪";
            case 95:
                return "雷阵雨";
            case 96:
            case 99:
                return "雷阵雨伴冰雹";
            default:
                return "天气";
        }
    }

    public static String iconForWmo(int code, boolean day) {
        if (code == 0) {
            return day ? ICON_SUNNY : ICON_NIGHT;
        }
        if (code == 1 || code == 2) {
            return ICON_PARTLY;
        }
        if (code == 3) {
            return ICON_CLOUDY;
        }
        if (code == 45 || code == 48) {
            return ICON_FOG;
        }
        if (code >= 51 && code <= 57) {
            return ICON_DRIZZLE;
        }
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) {
            return ICON_RAIN;
        }
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            return ICON_SNOW;
        }
        if (code >= 95) {
            return ICON_THUNDER;
        }
        return ICON_PARTLY;
    }

    public static String iconForQWeather(String rawCode, String text) {
        if (text != null && !text.trim().isEmpty() && !text.trim().equals("天气")) {
            return iconForText(text);
        }
        int code;
        try {
            code = Integer.parseInt(rawCode);
        } catch (Exception e) {
            return ICON_PARTLY;
        }
        if (code == 100 || code == 150) {
            return code == 150 ? ICON_NIGHT : ICON_SUNNY;
        }
        if (code == 104) {
            return ICON_CLOUDY;
        }
        if (code == 302 || code == 303 || code == 304) {
            return ICON_THUNDER;
        }
        if (code >= 300 && code < 400) {
            return ICON_RAIN;
        }
        if (code >= 400 && code < 500) {
            return ICON_SNOW;
        }
        if (code >= 500 && code < 600) {
            return ICON_FOG;
        }
        if (code >= 101 && code < 104 || code >= 151 && code < 154) {
            return ICON_PARTLY;
        }
        return ICON_PARTLY;
    }

    public static String iconForText(String text) {
        if (text == null) {
            return ICON_PARTLY;
        }
        String value = text.toLowerCase(Locale.ROOT);
        if (value.contains("雷") || value.contains("thunder") || value.contains("storm")) {
            return ICON_THUNDER;
        }
        if (value.contains("冰雹") || value.contains("雪") || value.contains("snow")
                || value.contains("sleet") || value.contains("blizzard")) {
            return ICON_SNOW;
        }
        if (value.contains("毛毛") || value.contains("drizzle")) {
            return ICON_DRIZZLE;
        }
        if (value.contains("雨") || value.contains("rain") || value.contains("shower")) {
            return ICON_RAIN;
        }
        if (value.contains("雾") || value.contains("霾") || value.contains("沙")
                || value.contains("尘") || value.contains("fog") || value.contains("mist")
                || value.contains("haze")) {
            return ICON_FOG;
        }
        if (value.contains("阴") || value.contains("overcast") || value.contains("cloudy")) {
            return ICON_CLOUDY;
        }
        if (value.contains("多云") || value.contains("少云") || value.contains("晴间")
                || value.contains("partly") || value.contains("cloud")) {
            return ICON_PARTLY;
        }
        if (value.contains("晴") || value.contains("clear") || value.contains("sun")) {
            return ICON_SUNNY;
        }
        if (value.contains("夜") || value.contains("月") || value.contains("night")) {
            return ICON_NIGHT;
        }
        return ICON_PARTLY;
    }

    public static String normalizeIcon(String raw, String text) {
        if (raw != null) {
            String value = raw.trim().toLowerCase(Locale.ROOT);
            if (value.equals(ICON_SUNNY) || value.equals(ICON_PARTLY) || value.equals(ICON_CLOUDY)
                    || value.equals(ICON_DRIZZLE) || value.equals(ICON_RAIN)
                    || value.equals(ICON_THUNDER) || value.equals(ICON_SNOW)
                    || value.equals(ICON_FOG) || value.equals(ICON_NIGHT)) {
                return value;
            }
            if (value.matches("\\d+")) {
                return iconForQWeather(value, text);
            }
            if (!value.isEmpty()) {
                return iconForText(raw + " " + (text == null ? "" : text));
            }
        }
        return iconForText(text);
    }
}
