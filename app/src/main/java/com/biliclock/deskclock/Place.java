package com.biliclock.deskclock;

/** A geocoding search result. */
public final class Place {
    public final String name;
    public final String region;
    public final double latitude;
    public final double longitude;

    public Place(String name, String region, double latitude, double longitude) {
        this.name = name == null ? "" : name;
        this.region = region == null ? "" : region;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String displayName() {
        return region.isEmpty() ? name : name + " · " + region;
    }
}
