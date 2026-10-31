package com.biliclock.deskclock;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Full-screen landscape clock face. Tap the avatar or long-press anywhere for settings. */
public final class MainActivity extends Activity implements ClockView.OnOpenSettingsListener {
    private static final long TOAST_THROTTLE_MS = 60_000L;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newFixedThreadPool(2);

    private Prefs prefs;
    private ClockView clockView;
    private Bitmap backgroundBitmap;
    private boolean biliInFlight;
    private boolean weatherInFlight;
    private boolean hasBiliCache;
    private boolean hasWeatherCache;
    private int seenRevision = -1;
    private long lastBiliToast;
    private boolean settingsOpen;
    private boolean biliNoticeShown;
    private boolean weatherNoticeShown;
    private boolean networkNoticeShown;
    private boolean resumed;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!resumed) {
                return;
            }
            clockView.setNow(System.currentTimeMillis());
            long delay = 1000L - (System.currentTimeMillis() % 1000L);
            ui.postDelayed(this, Math.max(50L, delay));
        }
    };

    private final Runnable refreshTicker = new Runnable() {
        @Override
        public void run() {
            if (!resumed) {
                return;
            }
            refreshData(false);
            ui.postDelayed(this, TOAST_THROTTLE_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        applyWindowFlags();
        setContentView(R.layout.activity_main);
        clockView = findViewById(R.id.clock);
        clockView.setOnOpenSettingsListener(this);
        clockView.setAlpha(0f);
        clockView.animate().alpha(1f).setDuration(650L).start();

        loadCachedData();
        applyPreferences(false);

        if (!prefs.isHintShown()) {
            prefs.setHintShown(true);
            ui.postDelayed(() -> Toast.makeText(MainActivity.this,
                    R.string.toast_settings_hint, Toast.LENGTH_LONG).show(), 900L);
        }
        enterImmersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        settingsOpen = false;
        applyWindowFlags();
        enterImmersive();
        boolean changed = prefs.getRevision() != seenRevision;
        applyPreferences(changed);
        ui.removeCallbacks(ticker);
        ui.removeCallbacks(refreshTicker);
        ui.post(ticker);
        refreshData(changed);
        ui.postDelayed(refreshTicker, TOAST_THROTTLE_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        ui.removeCallbacks(ticker);
        ui.removeCallbacks(refreshTicker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ui.removeCallbacksAndMessages(null);
        io.shutdownNow();
        if (backgroundBitmap != null) {
            backgroundBitmap.recycle();
            backgroundBitmap = null;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersive();
        }
    }

    @Override
    public void onOpenSettings() {
        if (settingsOpen) {
            return;
        }
        settingsOpen = true;
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void applyPreferences(boolean forceReload) {
        if (prefs == null || clockView == null) {
            return;
        }
        seenRevision = prefs.getRevision();
        clockView.setShowSecondHand(prefs.isShowSecondHand());
        if (forceReload) {
            loadCachedData();
        }
    }

    private void loadCachedData() {
        String uid = prefs.getUid();
        BiliData cached = prefs.loadBili(uid);
        if (cached != null) {
            hasBiliCache = true;
            clockView.setBili(cached.fans, cached.likes, prefs.getSubtitleMode());
            Bitmap avatar = Images.loadCached(prefs, uid);
            clockView.setAvatar(avatar);
        } else {
            hasBiliCache = false;
            clockView.setBili(-1, -1, prefs.getSubtitleMode());
            clockView.setAvatar(null);
        }
        WeatherData weather = prefs.loadWeather(prefs.getCity());
        hasWeatherCache = weather != null;
        clockView.setWeather(weather, prefs.getCity());
        clockView.setShowSecondHand(prefs.isShowSecondHand());
        loadBackground();
    }

    private void loadBackground() {
        Bitmap next = null;
        if (Prefs.BACKGROUND_IMAGE.equals(prefs.getBackgroundMode())) {
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            next = Images.loadBackground(prefs.backgroundFile(), width, height);
        }
        clockView.setBackgroundImage(next);
        if (backgroundBitmap != null && backgroundBitmap != next) {
            backgroundBitmap.recycle();
        }
        backgroundBitmap = next;
    }

    private void refreshData(boolean force) {
        if (!hasNetwork()) {
            if (!networkNoticeShown) {
                networkNoticeShown = true;
                maybeToast(true, false);
            }
            return;
        }
        long now = System.currentTimeMillis();
        long interval = prefs.getRefreshMinutes() * 60_000L;
        if (force || now - prefs.getBiliCacheTime() > interval) {
            fetchBili();
        }
        if (force || now - prefs.getWeatherCacheTime() > interval) {
            fetchWeather();
        }
    }

    private void fetchBili() {
        if (biliInFlight) {
            return;
        }
        final String uid = prefs.getUid();
        biliInFlight = true;
        final boolean hadCache = hasBiliCache;
        io.execute(() -> {
            BiliData result = null;
            Bitmap bitmap = null;
            try {
                result = BiliClient.fetch(uid);
                prefs.saveBili(result);
                if (Images.needsRefresh(prefs, uid, result.avatarUrl)) {
                    try {
                        bitmap = Images.download(prefs, uid, result.avatarUrl);
                    } catch (Exception ignored) {
                        bitmap = Images.loadCached(prefs, uid);
                    }
                } else {
                    bitmap = Images.loadCached(prefs, uid);
                }
            } catch (Exception ignored) {
            }
            final BiliData finalResult = result;
            final Bitmap finalBitmap = bitmap;
            ui.post(() -> {
                biliInFlight = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (!prefs.getUid().equals(uid)) {
                    return;
                }
                if (finalResult != null) {
                    hasBiliCache = true;
                    clockView.setBili(finalResult.fans, finalResult.likes, prefs.getSubtitleMode());
                    if (finalBitmap != null) {
                        clockView.setAvatar(finalBitmap);
                    }
                } else if (!hadCache && !hasBiliCache) {
                    maybeToast(false, true);
                }
            });
        });
    }

    private void fetchWeather() {
        if (weatherInFlight) {
            return;
        }
        weatherInFlight = true;
        final String city = prefs.getCity();
        final boolean hadCache = hasWeatherCache;
        io.execute(() -> {
            WeatherData result = null;
            try {
                result = WeatherClient.fetch(prefs);
                prefs.saveWeather(result);
            } catch (Exception ignored) {
            }
            final WeatherData finalResult = result;
            ui.post(() -> {
                weatherInFlight = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (!prefs.getCity().equals(city)) {
                    return;
                }
                if (finalResult != null) {
                    hasWeatherCache = true;
                    clockView.setWeather(finalResult, city);
                } else if (!hadCache && !hasWeatherCache) {
                    maybeToast(false, false);
                }
            });
        });
    }

    private void maybeToast(boolean offline, boolean bilibili) {
        long now = System.currentTimeMillis();
        if (offline) {
            if (now - lastBiliToast > TOAST_THROTTLE_MS) {
                lastBiliToast = now;
                Toast.makeText(this, R.string.toast_network_offline, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (bilibili) {
            if (!biliNoticeShown) {
                biliNoticeShown = true;
                Toast.makeText(this, R.string.toast_bili_failed, Toast.LENGTH_SHORT).show();
            }
        } else if (!weatherNoticeShown) {
            weatherNoticeShown = true;
            Toast.makeText(this, R.string.toast_weather_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasNetwork() {
        try {
            ConnectivityManager manager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return true;
            }
            NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception ignored) {
            return true;
        }
    }

    private void applyWindowFlags() {
        Window window = getWindow();
        if (prefs == null || prefs.isKeepScreenOn()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void enterImmersive() {
        View decor = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hideModern(decor);
        } else {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private void hideModern(View decor) {
        WindowInsetsController controller = decor.getWindowInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
}
