package com.biliclock.deskclock;

import java.util.Locale;

/** Number and text formatting shared by the clock face and settings screen. */
public final class Format {
    private Format() {
    }

    /**
     * The reference UI shows the raw fan count without separators.
     * Negative values mean "not loaded yet".
     */
    public static String fans(long value) {
        return value < 0 ? "—" : Long.toString(value);
    }

    /** Compact Chinese notation used by Bilibili: 100.5万, 1.2亿. */
    public static String compact(long value) {
        if (value < 0) {
            return "—";
        }
        if (value >= 100_000_000L) {
            return trimZero(value / 100_000_000.0) + "亿";
        }
        if (value >= 10_000L) {
            return trimZero(value / 10_000.0) + "万";
        }
        return Long.toString(value);
    }

    private static String trimZero(double value) {
        String s = String.format(Locale.US, "%.1f", value);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s;
    }

    public static String temperature(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "--";
        }
        // The reference displays whole degrees; round instead of showing 24.9°C.
        return Long.toString(Math.round(value));
    }

    public static String weatherLine(WeatherData data, String fallbackCity) {
        if (data == null) {
            String city = fallbackCity == null || fallbackCity.trim().isEmpty() ? "本地" : fallbackCity.trim();
            return city + " · 天气获取中";
        }
        String city = data.city.trim().isEmpty()
                ? (fallbackCity == null ? "" : fallbackCity.trim())
                : data.city.trim();
        StringBuilder sb = new StringBuilder();
        sb.append(temperature(data.temperatureC)).append("°C");
        if (!city.isEmpty()) {
            sb.append(' ').append(city);
        }
        if (!data.condition.isEmpty()) {
            sb.append(" · ").append(data.condition);
        }
        if (data.humidity >= 0) {
            sb.append(" 湿度 ").append(data.humidity).append('%');
        }
        return sb.toString();
    }

    public static String refreshLabel(int minutes) {
        return minutes + " 分钟";
    }

    public static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
