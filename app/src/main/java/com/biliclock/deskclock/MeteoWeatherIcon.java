package com.biliclock.deskclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

/**
 * Static weather icon built from the Meteocons vector set (MIT, Bas Milius).
 * SVG layers are stored as VectorDrawables and composed here. The clock is a
 * desktop clock, so the weather glyph is deliberately static; only the clock
 * hands move.
 */
public final class MeteoWeatherIcon {
    private final Drawable cloud;
    private final Drawable sunSmall;
    private final Drawable sunLarge;
    private final Drawable raysSmall;
    private final Drawable raysLarge;
    private final Drawable[] rainDrops = new Drawable[3];
    private final Drawable bolt;
    private final Drawable snow;
    private final Drawable fogLines;
    private final Drawable moon;

    public MeteoWeatherIcon(Context context) {
        cloud = load(context, R.drawable.weather_cloud);
        sunSmall = load(context, R.drawable.weather_sun_disc_small);
        sunLarge = load(context, R.drawable.weather_sun_disc_large);
        raysSmall = load(context, R.drawable.weather_sun_rays_small);
        raysLarge = load(context, R.drawable.weather_sun_rays_large);
        rainDrops[0] = load(context, R.drawable.weather_rain_drop_1);
        rainDrops[1] = load(context, R.drawable.weather_rain_drop_2);
        rainDrops[2] = load(context, R.drawable.weather_rain_drop_3);
        bolt = load(context, R.drawable.weather_bolt);
        snow = load(context, R.drawable.weather_snow);
        fogLines = load(context, R.drawable.weather_fog_lines);
        moon = load(context, R.drawable.weather_moon);
    }

    private static Drawable load(Context context, int id) {
        try {
            Drawable drawable = context.getDrawable(id);
            if (drawable != null) {
                drawable.setBounds(0, 0, 64, 64);
            }
            return drawable;
        } catch (Exception ignored) {
            return null;
        }
    }

    public void draw(Canvas canvas, String type, float cx, float cy, float size) {
        String active = type == null ? WeatherCode.ICON_PARTLY : type;

        canvas.save();
        canvas.translate(cx - size / 2f, cy - size / 2f);
        canvas.scale(size / 64f, size / 64f);

        if (WeatherCode.ICON_SUNNY.equals(active)) {
            draw(canvas, raysLarge, 255);
            draw(canvas, sunLarge, 255);
        } else if (WeatherCode.ICON_NIGHT.equals(active)) {
            draw(canvas, moon, 255);
        } else if (WeatherCode.ICON_CLOUDY.equals(active)) {
            draw(canvas, cloud, 255);
        } else if (WeatherCode.ICON_PARTLY.equals(active)) {
            draw(canvas, raysSmall, 255);
            draw(canvas, sunSmall, 255);
            draw(canvas, cloud, 255);
        } else if (WeatherCode.ICON_DRIZZLE.equals(active)) {
            draw(canvas, raysSmall, 255);
            draw(canvas, sunSmall, 255);
            draw(canvas, cloud, 255);
            drawRain(canvas);
        } else if (WeatherCode.ICON_RAIN.equals(active)) {
            draw(canvas, cloud, 255);
            drawRain(canvas);
        } else if (WeatherCode.ICON_THUNDER.equals(active)) {
            draw(canvas, cloud, 255);
            drawRain(canvas);
            draw(canvas, bolt, 255);
        } else if (WeatherCode.ICON_SNOW.equals(active)) {
            draw(canvas, cloud, 255);
            draw(canvas, snow, 255);
        } else if (WeatherCode.ICON_FOG.equals(active)) {
            draw(canvas, cloud, 255);
            draw(canvas, fogLines, 255);
        } else {
            draw(canvas, raysSmall, 255);
            draw(canvas, sunSmall, 255);
            draw(canvas, cloud, 255);
        }
        canvas.restore();
    }

    private void drawRain(Canvas canvas) {
        for (Drawable drop : rainDrops) {
            draw(canvas, drop, 255);
        }
    }

    private void draw(Canvas canvas, Drawable drawable, int alpha) {
        if (drawable == null) {
            return;
        }
        drawable.setAlpha(Math.max(0, Math.min(255, alpha)));
        drawable.draw(canvas);
    }
}
