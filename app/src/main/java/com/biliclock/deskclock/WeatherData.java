package com.biliclock.deskclock;

/** Immutable current-weather snapshot. */
public final class WeatherData {
    public final String city;
    public final String condition;
    public final String iconType;
    public final double temperatureC;
    public final int humidity;
    public final long fetchedAt;

    public WeatherData(String city, String condition, String iconType,
                       double temperatureC, int humidity, long fetchedAt) {
        this.city = city == null ? "" : city;
        this.condition = condition == null || condition.trim().isEmpty()
                ? "天气" : condition.trim();
        this.iconType = iconType == null ? WeatherCode.ICON_PARTLY : iconType;
        this.temperatureC = temperatureC;
        this.humidity = humidity;
        this.fetchedAt = fetchedAt;
    }
}
