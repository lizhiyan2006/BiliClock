package com.biliclock.deskclock;

/** Immutable snapshot of the public Bilibili profile shown by the clock. */
public final class BiliData {
    public final String uid;
    public final String name;
    public final String avatarUrl;
    public final long fans;
    public final long likes;
    public final long fetchedAt;

    public BiliData(String uid, String name, String avatarUrl, long fans, long likes, long fetchedAt) {
        this.uid = uid;
        this.name = name == null ? "" : name;
        this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
        this.fans = fans;
        this.likes = likes;
        this.fetchedAt = fetchedAt;
    }

    public BiliData withFetchedAt(long time) {
        return new BiliData(uid, name, avatarUrl, fans, likes, time);
    }
}
