package com.biliclock.deskclock;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Fetches the public Bilibili profile shown on the clock. No login is required. */
public final class BiliClient {
    private static final String CARD_API = "https://api.bilibili.com/x/web-interface/card?mid=";

    private BiliClient() {
    }

    public static BiliData fetch(String uid) throws IOException {
        if (uid == null || !uid.matches("\\d{1,20}")) {
            throw new IOException("UID 格式不正确");
        }
        String url = CARD_API + URLEncoder.encode(uid, StandardCharsets.UTF_8.name());
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", "https://www.bilibili.com/");
        headers.put("Origin", "https://www.bilibili.com");
        JSONObject root = Http.getJson(url, headers);
        int code = root.optInt("code", -1);
        if (code != 0) {
            String message = root.optString("message", "未知错误");
            if (code == -404) {
                throw new IOException("找不到这个 B 站用户");
            }
            throw new IOException("B 站接口错误 " + code + " · " + message);
        }
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            throw new IOException("B 站接口没有返回用户资料");
        }
        JSONObject card = data.optJSONObject("card");
        if (card == null) {
            throw new IOException("B 站接口没有返回用户资料");
        }
        long fans = data.optLong("follower", card.optLong("fans", -1));
        if (fans < 0) {
            fans = card.optLong("fans", -1);
        }
        long likes = data.has("like_num") ? data.optLong("like_num", -1) : -1;
        String face = normalizeImage(card.optString("face", ""));
        return new BiliData(
                uid,
                card.optString("name", ""),
                face,
                fans,
                likes,
                System.currentTimeMillis());
    }

    public static Map<String, String> imageHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", "https://www.bilibili.com/");
        headers.put("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        return headers;
    }

    private static String normalizeImage(String url) {
        if (url == null) {
            return "";
        }
        String value = url.trim();
        if (value.startsWith("//")) {
            return "https:" + value;
        }
        if (value.startsWith("http://")) {
            return "https://" + value.substring(7);
        }
        return value;
    }
}
