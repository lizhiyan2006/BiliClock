package com.biliclock.deskclock;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/** Tiny HTTP helper so the app needs no third-party networking library. */
public final class Http {
    public static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private static final int CONNECT_TIMEOUT = 15_000;
    private static final int READ_TIMEOUT = 25_000;
    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;

    private Http() {
    }

    public static JSONObject getJson(String address, Map<String, String> headers) throws IOException {
        byte[] bytes = getBytes(address, headers, MAX_JSON_BYTES);
        try {
            return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IOException("服务返回了无法识别的 JSON", e);
        }
    }

    public static byte[] getImage(String address, Map<String, String> headers) throws IOException {
        return getBytes(address, headers, MAX_IMAGE_BYTES);
    }

    public static byte[] getBytes(String address, Map<String, String> headers, int maxBytes)
            throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(address);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            if (connection instanceof HttpsURLConnection && App.context() != null) {
                SSLSocketFactory factory = Tls.socketFactory(App.context());
                if (factory != null) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(factory);
                }
            }
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.7");
            connection.setRequestProperty("User-Agent", BROWSER_UA);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
            int status = connection.getResponseCode();
            InputStream raw = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (raw == null) {
                throw new IOException("HTTP " + status);
            }
            InputStream stream = raw;
            String encoding = connection.getContentEncoding();
            if (encoding != null && encoding.toLowerCase(Locale.ROOT).contains("gzip")) {
                stream = new GZIPInputStream(raw);
            }
            byte[] body = readFully(stream, maxBytes);
            if (status < 200 || status >= 300) {
                String snippet = new String(body, StandardCharsets.UTF_8);
                if (snippet.length() > 240) {
                    snippet = snippet.substring(0, 240);
                }
                throw new IOException("HTTP " + status + (snippet.isEmpty() ? "" : " · " + snippet));
            }
            return body;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] readFully(InputStream stream, int maxBytes) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = stream.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IOException("响应内容过大");
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    static InputStream open(byte[] data) {
        return new ByteArrayInputStream(data);
    }
}
