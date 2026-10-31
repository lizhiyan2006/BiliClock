package com.biliclock.deskclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

/**
 * The entire clock face is drawn on one canvas in a 1920x1200 design space.
 * The avatar and clock stay on the left; the follower count, like caption and
 * weather row all share the centre line of the right-hand text area.
 *
 * The follower count is never scaled horizontally. Long counts are reduced in
 * type size so the glyph proportions stay intact.
 */
public final class ClockView extends View {
    private static final float DESIGN_W = 1920f;
    private static final float DESIGN_H = 1200f;

    private static final float CONTENT_LEFT = 76f;
    private static final float CONTENT_RIGHT = DESIGN_W - 76f;
    /** Shared centre line for the fan count, like caption and weather row. */
    private static final float TEXT_CENTER_X = 1115f;

    private static final float R_AVATAR = 131f;
    private static final float R_CLOCK = 155f;
    private static final float CX_AVATAR = CONTENT_LEFT + R_AVATAR;
    private static final float CX_CLOCK = CONTENT_LEFT + R_CLOCK;
    private static final float CY_TOP = 350f;
    private static final float CY_AVATAR = CY_TOP;
    private static final float CY_NUMBER = CY_TOP;
    private static final float CY_SUBTITLE = 658f;
    private static final float CY_BOTTOM = 913f;
    private static final float CY_CLOCK = CY_BOTTOM;
    private static final float CY_ICON = CY_BOTTOM;
    private static final float CY_WEATHER = CY_BOTTOM;

    private static final float NUMBER_SIZE = 445f;
    private static final float NUMBER_MIN_LEFT = CONTENT_LEFT + R_AVATAR * 2f + 52f;
    private static final float NUMBER_MAX_WIDTH = Math.min(
            2f * (TEXT_CENTER_X - NUMBER_MIN_LEFT),
            2f * (CONTENT_RIGHT - TEXT_CENTER_X));

    private static final float SUBTITLE_SIZE = 64f;
    private static final float SUBTITLE_GAP = 28f;

    private static final float WEATHER_SIZE = 88f;
    private static final float ICON_SIZE = 112f;
    private static final float ICON_GAP = 6f;
    /** Keep the icon clear of the clock even when the weather line is long. */
    private static final float WEATHER_CLOCK_GAP = 40f;
    private static final float WEATHER_TEXT_MAX_WIDTH = 2f * Math.min(
            TEXT_CENTER_X - (CONTENT_LEFT + R_CLOCK * 2f + WEATHER_CLOCK_GAP + ICON_SIZE + ICON_GAP),
            CONTENT_RIGHT - TEXT_CENTER_X);

    private static final int COLOR_TEXT = Color.rgb(214, 231, 232);
    private static final int COLOR_SUB_LABEL = Color.rgb(150, 191, 210);
    private static final int COLOR_SUB_VALUE = Color.rgb(207, 220, 221);
    private static final int COLOR_WEATHER_TEMP = Color.rgb(214, 228, 229);
    private static final int COLOR_WEATHER_CN = Color.rgb(150, 191, 210);
    private static final int COLOR_CLOCK_FACE = Color.rgb(223, 233, 232);
    private static final int COLOR_CLOCK_INK = Color.rgb(9, 12, 15);
    private static final int COLOR_SECOND = Color.rgb(244, 58, 61);
    private static final int COLOR_SECOND_LINE = Color.argb(96, 9, 12, 15);
    private static final int COLOR_BACKGROUND_OVERLAY = Color.argb(130, 0, 0, 0);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint clockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final Rect srcRect = new Rect();
    private final Rect textBounds = new Rect();
    private final Rect backgroundSrc = new Rect();
    private final RectF backgroundDest = new RectF();

    private Typeface bodyTypeface;
    private Typeface numberTypeface;
    private MeteoWeatherIcon weatherIcon;

    private Bitmap avatar;
    private Bitmap backgroundImage;
    private long fans = -1;
    private long likes = -1;
    private String subtitleMode = Prefs.SUBTITLE_LIKES;
    private WeatherData weather;
    private String fallbackCity = "中山";
    private boolean showSecondHand = true;

    private final Calendar now = Calendar.getInstance();

    private float scale = 1f;
    private float offsetX;
    private float offsetY;

    private GestureDetector gestureDetector;
    private OnOpenSettingsListener openSettingsListener;

    public interface OnOpenSettingsListener {
        void onOpenSettings();
    }

    public ClockView(Context context) {
        super(context);
        init();
    }

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        try {
            bodyTypeface = Typeface.createFromAsset(getContext().getAssets(),
                    "Helvetica-Regular.ttf");
        } catch (Exception ignored) {
            bodyTypeface = Typeface.create("sans-serif", Typeface.NORMAL);
        }
        if (bodyTypeface == null) {
            bodyTypeface = Typeface.SANS_SERIF;
        }
        // The clock uses one thin Helvetica weight throughout. Higher weights
        // looked too heavy in the large follower count.
        numberTypeface = bodyTypeface;
        weatherIcon = new MeteoWeatherIcon(getContext());
        bodyPaint.setTypeface(bodyTypeface);
        bodyPaint.setTextAlign(Paint.Align.LEFT);
        numberPaint.setTypeface(numberTypeface);
        numberPaint.setTextAlign(Paint.Align.LEFT);

        gestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        if (isAvatarHit(e.getX(), e.getY())) {
                            performClick();
                            notifyOpenSettings();
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        notifyOpenSettings();
                    }
                });
    }

    public void setOnOpenSettingsListener(OnOpenSettingsListener listener) {
        openSettingsListener = listener;
    }

    private void notifyOpenSettings() {
        if (openSettingsListener != null) {
            openSettingsListener.onOpenSettings();
        }
    }

    public void setAvatar(Bitmap bitmap) {
        if (avatar != bitmap) {
            avatar = bitmap;
            invalidate();
        }
    }

    public void setBackgroundImage(Bitmap bitmap) {
        if (backgroundImage != bitmap) {
            backgroundImage = bitmap;
            invalidate();
        }
    }

    public void setBili(long fanCount, long likeCount, String mode) {
        this.fans = fanCount;
        this.likes = likeCount;
        this.subtitleMode = Prefs.SUBTITLE_FANS.equals(mode) ? Prefs.SUBTITLE_FANS : Prefs.SUBTITLE_LIKES;
        invalidate();
    }

    public void setWeather(WeatherData data, String cityFallback) {
        this.weather = data;
        if (cityFallback != null && !cityFallback.trim().isEmpty()) {
            this.fallbackCity = cityFallback.trim();
        }
        invalidate();
    }

    public void setShowSecondHand(boolean show) {
        if (showSecondHand != show) {
            showSecondHand = show;
            invalidate();
        }
    }

    public void setNow(long millis) {
        now.setTimeInMillis(millis);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        scale = Math.min(w / DESIGN_W, h / DESIGN_H);
        if (scale <= 0f) {
            scale = 1f;
        }
        offsetX = (w - DESIGN_W * scale) / 2f;
        offsetY = (h - DESIGN_H * scale) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);

        drawAvatar(canvas);
        drawFanCount(canvas);
        drawSubtitle(canvas);
        drawClock(canvas);
        drawWeather(canvas);

        canvas.restore();
    }

    private void drawBackground(Canvas canvas) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColorFilter(null);
        paint.setAlpha(255);

        if (backgroundImage != null && !backgroundImage.isRecycled()) {
            float bw = backgroundImage.getWidth();
            float bh = backgroundImage.getHeight();
            if (bw <= 0f || bh <= 0f) {
                canvas.drawColor(Color.BLACK);
                return;
            }
            float imageScale = Math.max(getWidth() / bw, getHeight() / bh);
            float drawW = bw * imageScale;
            float drawH = bh * imageScale;
            float left = (getWidth() - drawW) / 2f;
            float top = (getHeight() - drawH) / 2f;
            backgroundSrc.set(0, 0, Math.round(bw), Math.round(bh));
            backgroundDest.set(left, top, left + drawW, top + drawH);
            paint.setFilterBitmap(true);
            canvas.drawBitmap(backgroundImage, backgroundSrc, backgroundDest, paint);
            paint.setColor(COLOR_BACKGROUND_OVERLAY);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        } else {
            canvas.drawColor(Color.BLACK);
        }
    }

    private void drawAvatar(Canvas canvas) {
        float r = R_AVATAR;
        path.reset();
        path.addCircle(CX_AVATAR, CY_AVATAR, r, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(path);

        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColorFilter(null);
        paint.setFilterBitmap(true);
        paint.setAlpha(255);

        if (avatar != null && !avatar.isRecycled()) {
            float bw = avatar.getWidth();
            float bh = avatar.getHeight();
            float side = Math.min(bw, bh);
            float left = (bw - side) / 2f;
            float top = (bh - side) / 2f;
            rect.set(CX_AVATAR - r, CY_AVATAR - r, CX_AVATAR + r, CY_AVATAR + r);
            srcRect.set(Math.round(left), Math.round(top),
                    Math.round(left + side), Math.round(top + side));
            canvas.drawBitmap(avatar, srcRect, rect, paint);
        } else {
            paint.setShader(new RadialGradient(CX_AVATAR - r * 0.32f, CY_AVATAR - r * 0.36f, r * 1.4f,
                    new int[]{Color.rgb(47, 59, 65), Color.rgb(19, 24, 28)},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(CX_AVATAR, CY_AVATAR, r, paint);
            paint.setShader(null);
            paint.setColor(Color.rgb(110, 128, 133));
            canvas.drawCircle(CX_AVATAR, CY_AVATAR - r * 0.24f, r * 0.24f, paint);
            rect.set(CX_AVATAR - r * 0.52f, CY_AVATAR + r * 0.12f,
                    CX_AVATAR + r * 0.52f, CY_AVATAR + r * 0.82f);
            canvas.drawRoundRect(rect, r * 0.40f, r * 0.40f, paint);
        }
        canvas.restore();

        // The ring uses its own paint; drawing it with the bitmap paint would
        // leak its low alpha into the next frame and darken the avatar.
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1.4f);
        ringPaint.setColor(Color.argb(34, 255, 255, 255));
        canvas.drawCircle(CX_AVATAR, CY_AVATAR, r - 0.7f, ringPaint);
    }

    private void drawFanCount(Canvas canvas) {
        String text = Format.fans(fans);
        numberPaint.setTypeface(numberTypeface);
        numberPaint.setLetterSpacing(0f);
        numberPaint.setColor(COLOR_TEXT);
        numberPaint.setTextAlign(Paint.Align.LEFT);
        numberPaint.setStyle(Paint.Style.FILL);

        float size = NUMBER_SIZE;
        numberPaint.setTextSize(size);
        float width = numberPaint.measureText(text);
        if (width > NUMBER_MAX_WIDTH && width > 0f) {
            // Uniform type-size reduction only; never scale horizontally.
            size = size * NUMBER_MAX_WIDTH / width;
            numberPaint.setTextSize(size);
        }

        numberPaint.getTextBounds(text, 0, text.length(), textBounds);
        float drawX = TEXT_CENTER_X - (textBounds.left + textBounds.right) / 2f;
        float baseline = CY_NUMBER - (textBounds.top + textBounds.bottom) / 2f;
        canvas.drawText(text, drawX, baseline, numberPaint);
    }

    private void drawSubtitle(Canvas canvas) {
        boolean useLikes = Prefs.SUBTITLE_LIKES.equals(subtitleMode) && likes >= 0;
        String label = useLikes ? "获赞" : "粉丝";
        String amount = Format.compact(useLikes ? likes : fans);

        bodyPaint.setTextSize(SUBTITLE_SIZE);
        bodyPaint.setTypeface(bodyTypeface);
        bodyPaint.setColor(COLOR_SUB_LABEL);
        bodyPaint.setTextAlign(Paint.Align.LEFT);
        numberPaint.setTextSize(SUBTITLE_SIZE);
        numberPaint.setTypeface(numberTypeface);
        numberPaint.setColor(COLOR_SUB_VALUE);
        numberPaint.setTextAlign(Paint.Align.LEFT);
        numberPaint.setLetterSpacing(0f);

        float labelWidth = bodyPaint.measureText(label);
        float amountWidth = numberPaint.measureText(amount);
        float total = labelWidth + SUBTITLE_GAP + amountWidth;
        float startX = TEXT_CENTER_X - total / 2f;

        Paint.FontMetrics metrics = bodyPaint.getFontMetrics();
        float baseline = CY_SUBTITLE - (metrics.ascent + metrics.descent) / 2f;

        canvas.drawText(label, startX, baseline, bodyPaint);
        canvas.drawText(amount, startX + labelWidth + SUBTITLE_GAP,
                baseline, numberPaint);
    }

    private void drawClock(Canvas canvas) {
        float r = R_CLOCK;

        clockPaint.setStyle(Paint.Style.FILL);
        clockPaint.setShadowLayer(30f, 0f, 12f, Color.argb(80, 0, 0, 0));
        clockPaint.setColor(COLOR_CLOCK_FACE);
        canvas.drawCircle(CX_CLOCK, CY_CLOCK, r, clockPaint);
        clockPaint.clearShadowLayer();

        clockPaint.setColor(COLOR_CLOCK_INK);
        for (int i = 0; i < 12; i++) {
            boolean major = i % 3 == 0;
            float angle = (float) (Math.PI * 2 * i / 12.0);
            float inner = r * (major ? 0.80f : 0.855f);
            float outer = r * 0.925f;
            clockPaint.setStrokeWidth(major ? 3.4f : 1.9f);
            clockPaint.setStrokeCap(Paint.Cap.ROUND);
            clockPaint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(
                    CX_CLOCK + (float) Math.sin(angle) * inner,
                    CY_CLOCK - (float) Math.cos(angle) * inner,
                    CX_CLOCK + (float) Math.sin(angle) * outer,
                    CY_CLOCK - (float) Math.cos(angle) * outer,
                    clockPaint);
        }

        int hour = now.get(Calendar.HOUR);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);
        float hourAngle = (hour + minute / 60f + second / 3600f) * 30f;
        float minuteAngle = (minute + second / 60f) * 6f;
        float secondAngle = second * 6f;

        if (showSecondHand) {
            double angle = Math.toRadians(secondAngle);
            float sx = CX_CLOCK + (float) Math.sin(angle) * r * 0.89f;
            float sy = CY_CLOCK - (float) Math.cos(angle) * r * 0.89f;

            clockPaint.setStyle(Paint.Style.STROKE);
            clockPaint.setStrokeCap(Paint.Cap.ROUND);
            clockPaint.setColor(COLOR_SECOND_LINE);
            clockPaint.setStrokeWidth(1.5f);
            canvas.drawLine(CX_CLOCK, CY_CLOCK, sx, sy, clockPaint);

            clockPaint.setStyle(Paint.Style.FILL);
            clockPaint.setColor(COLOR_SECOND);
            canvas.drawCircle(sx, sy, 8.5f, clockPaint);
        }

        drawHand(canvas, hourAngle, r * 0.52f, 7.2f, COLOR_CLOCK_INK);
        drawHand(canvas, minuteAngle, r * 0.72f, 5.2f, COLOR_CLOCK_INK);

        clockPaint.setColor(COLOR_CLOCK_INK);
        canvas.drawCircle(CX_CLOCK, CY_CLOCK, 7.5f, clockPaint);
    }

    private void drawHand(Canvas canvas, float angleDegrees, float length, float width, int color) {
        double angle = Math.toRadians(angleDegrees);
        float x = CX_CLOCK + (float) Math.sin(angle) * length;
        float y = CY_CLOCK - (float) Math.cos(angle) * length;
        clockPaint.setStyle(Paint.Style.STROKE);
        clockPaint.setStrokeCap(Paint.Cap.ROUND);
        clockPaint.setStrokeWidth(width);
        clockPaint.setColor(color);
        canvas.drawLine(CX_CLOCK, CY_CLOCK, x, y, clockPaint);
        clockPaint.setStyle(Paint.Style.FILL);
    }

    private void drawWeather(Canvas canvas) {
        WeatherData data = weather;
        String icon = data == null ? WeatherCode.ICON_PARTLY : data.iconType;

        String temperature = "";
        String middle;
        String humidity = "";
        if (data == null) {
            String city = fallbackCity == null || fallbackCity.trim().isEmpty()
                    ? "本地" : fallbackCity.trim();
            middle = city + " · 天气获取中";
        } else {
            String city = data.city.trim().isEmpty()
                    ? (fallbackCity == null ? "" : fallbackCity.trim())
                    : data.city.trim();
            temperature = Format.temperature(data.temperatureC) + "°C";
            StringBuilder builder = new StringBuilder();
            if (!city.isEmpty()) {
                builder.append(' ').append(city);
            }
            if (!data.condition.isEmpty()) {
                builder.append(" · ").append(data.condition);
            }
            if (data.humidity >= 0) {
                builder.append(" 湿度 ");
                humidity = data.humidity + "%";
            }
            middle = builder.toString();
        }
        String line = temperature + middle + humidity;

        bodyPaint.setTypeface(bodyTypeface);
        bodyPaint.setTextAlign(Paint.Align.LEFT);
        bodyPaint.setTextSize(WEATHER_SIZE);

        float fullWidth = bodyPaint.measureText(line);
        if (fullWidth > WEATHER_TEXT_MAX_WIDTH && fullWidth > 0f) {
            float size = Math.max(55f, WEATHER_SIZE * WEATHER_TEXT_MAX_WIDTH / fullWidth);
            bodyPaint.setTextSize(size);
            fullWidth = bodyPaint.measureText(line);
        }

        // The text itself is centred on the shared axis; the icon sits just
        // to its left and is not counted in the text's centre.
        float textDrawX = TEXT_CENTER_X - fullWidth / 2f;
        float iconCx = textDrawX - ICON_GAP - ICON_SIZE / 2f;
        weatherIcon.draw(canvas, icon, iconCx, CY_ICON, ICON_SIZE);

        Paint.FontMetrics metrics = bodyPaint.getFontMetrics();
        float baseline = CY_WEATHER - (metrics.ascent + metrics.descent) / 2f;

        float x = textDrawX;
        if (!temperature.isEmpty()) {
            bodyPaint.setColor(COLOR_WEATHER_TEMP);
            canvas.drawText(temperature, x, baseline, bodyPaint);
            x += bodyPaint.measureText(temperature);
        }
        if (!middle.isEmpty()) {
            bodyPaint.setColor(COLOR_WEATHER_CN);
            canvas.drawText(middle, x, baseline, bodyPaint);
            x += bodyPaint.measureText(middle);
        }
        if (!humidity.isEmpty()) {
            bodyPaint.setColor(COLOR_WEATHER_TEMP);
            canvas.drawText(humidity, x, baseline, bodyPaint);
        }
    }

    private float toDesignX(float viewX) {
        return (viewX - offsetX) / scale;
    }

    private float toDesignY(float viewY) {
        return (viewY - offsetY) / scale;
    }

    public boolean isAvatarHit(float viewX, float viewY) {
        float x = toDesignX(viewX);
        float y = toDesignY(viewY);
        float dx = x - CX_AVATAR;
        float dy = y - CY_AVATAR;
        float radius = R_AVATAR * 1.20f;
        return dx * dx + dy * dy <= radius * radius;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
