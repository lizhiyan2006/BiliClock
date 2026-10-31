package com.biliclock.deskclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Decodes and caches Bilibili avatars for fast, offline-friendly startup. */
public final class Images {
    private static final int TARGET_AVATAR = 512;
    private static final int BACKGROUND_MAX = 2560;

    private Images() {
    }

    public static Bitmap loadCached(Prefs prefs, String uid) {
        File file = prefs.avatarFile(uid);
        if (!file.isFile()) {
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            return BitmapFactory.decodeStream(in);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean needsRefresh(Prefs prefs, String uid, String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        if (!prefs.avatarFile(uid).isFile()) {
            return true;
        }
        String saved = readUrl(prefs.avatarFile(uid));
        return !url.equals(saved);
    }

    /** Downloads and persists a square-cropped-ready avatar. Call off the UI thread. */
    public static Bitmap download(Prefs prefs, String uid, String url) throws IOException {
        if (url == null || url.isEmpty()) {
            return null;
        }
        byte[] raw = Http.getImage(url, BiliClient.imageHeaders());
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(raw, 0, raw.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("头像图片无法解析");
        }
        int sample = 1;
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        while (longest / (sample * 2) >= TARGET_AVATAR) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length, options);
        if (bitmap == null) {
            throw new IOException("头像图片无法解析");
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w > TARGET_AVATAR || h > TARGET_AVATAR) {
            float factor = Math.min((float) TARGET_AVATAR / w, (float) TARGET_AVATAR / h);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                    Math.max(1, Math.round(w * factor)),
                    Math.max(1, Math.round(h * factor)), true);
            if (scaled != bitmap) {
                bitmap.recycle();
            }
            bitmap = scaled;
        }
        File file = prefs.avatarFile(uid);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        writeUrl(file, url);
        return bitmap;
    }

    /**
     * Copies the picked image into app-private storage as a reasonably sized JPEG.
     * Runs off the UI thread; returns false for unreadable images.
     */
    public static boolean importBackground(Context context, Uri uri, File dest) {
        if (context == null || uri == null || dest == null) {
            return false;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return false;
            }
            BitmapFactory.decodeStream(in, null, bounds);
        } catch (Exception ignored) {
            return false;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return false;
        }

        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= BACKGROUND_MAX
                || bounds.outHeight / (sample * 2) >= BACKGROUND_MAX) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return false;
            }
            bitmap = BitmapFactory.decodeStream(in, null, options);
        } catch (Exception ignored) {
            return false;
        }
        if (bitmap == null) {
            return false;
        }

        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int longest = Math.max(width, height);
            if (longest > BACKGROUND_MAX) {
                float factor = (float) BACKGROUND_MAX / longest;
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                        Math.max(1, Math.round(width * factor)),
                        Math.max(1, Math.round(height * factor)), true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                }
                bitmap = scaled;
            }

            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            if (dest.exists() && !dest.delete()) {
                return false;
            }
            try (FileOutputStream out = new FileOutputStream(dest)) {
                return bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
        } catch (Exception ignored) {
            return false;
        } finally {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /** Decodes the saved background at a size suitable for the current screen. */
    public static Bitmap loadBackground(File file, int maxWidth, int maxHeight) {
        if (file == null || !file.isFile()) {
            return null;
        }
        int maxW = maxWidth > 0 ? maxWidth : BACKGROUND_MAX;
        int maxH = maxHeight > 0 ? maxHeight : BACKGROUND_MAX;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (FileInputStream in = new FileInputStream(file)) {
            BitmapFactory.decodeStream(in, null, bounds);
        } catch (Exception ignored) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= maxW
                || bounds.outHeight / (sample * 2) >= maxH) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (FileInputStream in = new FileInputStream(file)) {
            return BitmapFactory.decodeStream(in, null, options);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean copyFile(File source, File dest) {
        if (source == null || dest == null || !source.isFile()) {
            return false;
        }
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String readUrl(File avatarFile) {
        File sidecar = new File(avatarFile.getAbsolutePath() + ".url");
        if (!sidecar.isFile()) {
            return "";
        }
        try (FileInputStream in = new FileInputStream(sidecar)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void writeUrl(File avatarFile, String url) {
        File sidecar = new File(avatarFile.getAbsolutePath() + ".url");
        try (FileOutputStream out = new FileOutputStream(sidecar)) {
            out.write(url.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
}
