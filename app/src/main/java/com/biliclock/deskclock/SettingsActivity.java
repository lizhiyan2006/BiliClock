package com.biliclock.deskclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Configuration screen for the Bilibili profile, weather backend and display options. */
public final class SettingsActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private Prefs prefs;
    private EditText etUid;
    private EditText etCity;
    private EditText etLat;
    private EditText etLon;
    private EditText etOpenMeteoBase;
    private EditText etQweatherHost;
    private EditText etQweatherKey;
    private EditText etCustomUrl;
    private EditText etCustomKey;
    private EditText etCustomTemp;
    private EditText etCustomText;
    private EditText etCustomHumidity;
    private EditText etCustomIcon;
    private Spinner spSubtitle;
    private Spinner spProvider;
    private Spinner spRefresh;
    private Spinner spBackground;
    private Switch switchKeepOn;
    private Switch switchSecond;
    private TextView tvSearchStatus;
    private TextView tvBackgroundStatus;
    private Button btnSearchCity;
    private Button btnPickBackground;
    private View groupOpenMeteo;
    private View groupQWeather;
    private View groupCustom;

    private static final int REQUEST_PICK_BACKGROUND = 41;

    private final String[] providerValues = {
            Prefs.PROVIDER_OPEN_METEO, Prefs.PROVIDER_QWEATHER, Prefs.PROVIDER_CUSTOM
    };
    private final String[] subtitleValues = {Prefs.SUBTITLE_LIKES, Prefs.SUBTITLE_FANS};
    private final String[] backgroundValues = {Prefs.BACKGROUND_BLACK, Prefs.BACKGROUND_IMAGE};
    private final int[] refreshValues = {5, 10, 15, 30, 60};
    private boolean searching;
    private boolean importingBackground;
    private boolean pendingBackgroundPicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        Window window = getWindow();
        window.setStatusBarColor(getColorCompat(R.color.bg));
        window.setNavigationBarColor(getColorCompat(R.color.bg));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_settings);
        applyContentWidth();
        bindViews();
        setupSpinners();
        setupClicks();
        populate();
        updateProviderVisibility();
        String version = versionName();
        TextView versionTop = findViewById(R.id.tvVersion);
        TextView versionBottom = findViewById(R.id.tvAboutVersion);
        versionTop.setText(getString(R.string.version_fmt, version));
        versionBottom.setText(getString(R.string.version_fmt, version));
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyContentWidth();
    }

    @Override
    protected void onDestroy() {
        if (pendingBackgroundPicked && prefs != null) {
            //noinspection ResultOfMethodCallIgnored
            prefs.pendingBackgroundFile().delete();
        }
        super.onDestroy();
        io.shutdownNow();
        ui.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_BACKGROUND
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }
        final Uri uri = data.getData();
        importingBackground = true;
        btnPickBackground.setEnabled(false);
        updateBackgroundStatus();
        io.execute(() -> {
            final boolean ok = Images.importBackground(this, uri, prefs.pendingBackgroundFile());
            ui.post(() -> {
                if (isFinishing()) {
                    return;
                }
                importingBackground = false;
                btnPickBackground.setEnabled(true);
                if (ok) {
                    pendingBackgroundPicked = true;
                    spBackground.setSelection(1);
                    Toast.makeText(SettingsActivity.this,
                            R.string.background_imported, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this,
                            R.string.background_failed, Toast.LENGTH_SHORT).show();
                }
                updateBackgroundStatus();
            });
        });
    }

    private void pickBackground() {
        if (importingBackground) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_PICK_BACKGROUND);
        } catch (ActivityNotFoundException ignored) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.addCategory(Intent.CATEGORY_OPENABLE);
            fallback.setType("image/*");
            try {
                startActivityForResult(fallback, REQUEST_PICK_BACKGROUND);
            } catch (ActivityNotFoundException error) {
                Toast.makeText(this, R.string.background_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateBackgroundStatus() {
        if (tvBackgroundStatus == null) {
            return;
        }
        int res;
        if (importingBackground) {
            res = R.string.background_processing;
        } else if (pendingBackgroundPicked) {
            res = R.string.background_pending;
        } else if (prefs.hasBackgroundImage()) {
            boolean imageSelected = spBackground != null
                    && spBackground.getSelectedItemPosition() == 1;
            res = imageSelected ? R.string.background_saved : R.string.background_saved_off;
        } else {
            res = R.string.background_none;
        }
        tvBackgroundStatus.setText(res);
    }

    /** Keep the form readable on tablets instead of stretching it edge to edge. */
    private void applyContentWidth() {
        View content = findViewById(R.id.settingsContent);
        if (content == null) {
            return;
        }
        int screen = getResources().getDisplayMetrics().widthPixels;
        int maxContent = dp(760);
        int extra = Math.max(0, (screen - maxContent) / 2);
        int base = dp(20);
        content.setPadding(base + extra, content.getPaddingTop(),
                base + extra, content.getPaddingBottom());
    }

    private void bindViews() {
        etUid = findViewById(R.id.etUid);
        etCity = findViewById(R.id.etCity);
        etLat = findViewById(R.id.etLat);
        etLon = findViewById(R.id.etLon);
        etOpenMeteoBase = findViewById(R.id.etOpenMeteoBase);
        etQweatherHost = findViewById(R.id.etQweatherHost);
        etQweatherKey = findViewById(R.id.etQweatherKey);
        etCustomUrl = findViewById(R.id.etCustomUrl);
        etCustomKey = findViewById(R.id.etCustomKey);
        etCustomTemp = findViewById(R.id.etCustomTemp);
        etCustomText = findViewById(R.id.etCustomText);
        etCustomHumidity = findViewById(R.id.etCustomHumidity);
        etCustomIcon = findViewById(R.id.etCustomIcon);
        spSubtitle = findViewById(R.id.spSubtitle);
        spProvider = findViewById(R.id.spProvider);
        spRefresh = findViewById(R.id.spRefresh);
        spBackground = findViewById(R.id.spBackground);
        switchKeepOn = findViewById(R.id.switchKeepOn);
        switchSecond = findViewById(R.id.switchSecond);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        tvBackgroundStatus = findViewById(R.id.tvBackgroundStatus);
        btnSearchCity = findViewById(R.id.btnSearchCity);
        btnPickBackground = findViewById(R.id.btnPickBackground);
        groupOpenMeteo = findViewById(R.id.groupOpenMeteo);
        groupQWeather = findViewById(R.id.groupQWeather);
        groupCustom = findViewById(R.id.groupCustom);
    }

    private void setupSpinners() {
        spSubtitle.setAdapter(makeAdapter(new String[]{
                "获赞数 · 参考图样式",
                "粉丝数"
        }));
        spProvider.setAdapter(makeAdapter(new String[]{
                getString(R.string.provider_openmeteo),
                getString(R.string.provider_qweather),
                getString(R.string.provider_custom)
        }));
        String[] refreshLabels = new String[refreshValues.length];
        for (int i = 0; i < refreshValues.length; i++) {
            refreshLabels[i] = Format.refreshLabel(refreshValues[i]);
        }
        spRefresh.setAdapter(makeAdapter(refreshLabels));
        spBackground.setAdapter(makeAdapter(new String[]{
                getString(R.string.background_black),
                getString(R.string.background_image)
        }));

        spProvider.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(int position) {
                updateProviderVisibility();
            }
        });
        spBackground.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(int position) {
                updateBackgroundStatus();
            }
        });
    }

    private ArrayAdapter<String> makeAdapter(final String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(getColorCompat(R.color.text_primary));
                view.setTextSize(14f);
                view.setPadding(2, 0, 2, 0);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(getColorCompat(R.color.text_primary));
                view.setTextSize(14f);
                view.setBackgroundColor(getColorCompat(R.color.bg_elevated));
                view.setPadding(dp(14), dp(14), dp(14), dp(14));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void setupClicks() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finishWithFade());
        btnSearchCity.setOnClickListener(v -> searchCity());
        btnPickBackground.setOnClickListener(v -> pickBackground());
        etCity.setOnEditorActionListener((v, actionId, event) -> {
            searchCity();
            return true;
        });
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> confirmReset());
    }

    private void populate() {
        etUid.setText(prefs.getUid());
        etCity.setText(prefs.getCity());
        etLat.setText(Double.toString(prefs.getLatitude()));
        etLon.setText(Double.toString(prefs.getLongitude()));
        etOpenMeteoBase.setText(prefs.getOpenMeteoBase());
        etQweatherHost.setText(prefs.getQWeatherHost());
        etQweatherKey.setText(prefs.getQWeatherKey());
        etCustomUrl.setText(prefs.getCustomUrl());
        etCustomKey.setText(prefs.getCustomKey());
        etCustomTemp.setText(prefs.getCustomTempPath());
        etCustomText.setText(prefs.getCustomTextPath());
        etCustomHumidity.setText(prefs.getCustomHumidityPath());
        etCustomIcon.setText(prefs.getCustomIconPath());
        spSubtitle.setSelection(Prefs.SUBTITLE_FANS.equals(prefs.getSubtitleMode()) ? 1 : 0);
        int providerIndex = indexOf(providerValues, prefs.getProvider());
        spProvider.setSelection(providerIndex < 0 ? 0 : providerIndex);
        int refreshIndex = 0;
        for (int i = 0; i < refreshValues.length; i++) {
            if (refreshValues[i] == prefs.getRefreshMinutes()) {
                refreshIndex = i;
                break;
            }
        }
        spRefresh.setSelection(refreshIndex);
        spBackground.setSelection(Prefs.BACKGROUND_IMAGE.equals(prefs.getBackgroundMode()) ? 1 : 0);
        pendingBackgroundPicked = false;
        importingBackground = false;
        File pending = prefs.pendingBackgroundFile();
        if (pending.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            pending.delete();
        }
        switchKeepOn.setChecked(prefs.isKeepScreenOn());
        switchSecond.setChecked(prefs.isShowSecondHand());
        updateBackgroundStatus();
    }

    private void updateProviderVisibility() {
        if (groupOpenMeteo == null) {
            return;
        }
        int index = spProvider.getSelectedItemPosition();
        groupOpenMeteo.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        groupQWeather.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        groupCustom.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
    }

    private void searchCity() {
        if (searching) {
            return;
        }
        final String city = etCity.getText().toString().trim();
        if (city.isEmpty()) {
            showError(R.string.toast_city_empty, etCity);
            return;
        }
        hideKeyboard();
        searching = true;
        btnSearchCity.setEnabled(false);
        tvSearchStatus.setVisibility(View.VISIBLE);
        tvSearchStatus.setTextColor(getColorCompat(R.color.accent_dim));
        tvSearchStatus.setText(R.string.toast_searching);
        io.execute(() -> {
            List<Place> places = new ArrayList<>();
            Exception error = null;
            try {
                places = WeatherClient.searchCities(city);
            } catch (Exception e) {
                error = e;
            }
            final List<Place> finalPlaces = places;
            final Exception finalError = error;
            ui.post(() -> {
                if (isFinishing()) {
                    return;
                }
                searching = false;
                btnSearchCity.setEnabled(true);
                if (finalError != null) {
                    tvSearchStatus.setTextColor(getColorCompat(R.color.danger));
                    tvSearchStatus.setText(R.string.toast_search_failed);
                    return;
                }
                if (finalPlaces.isEmpty()) {
                    tvSearchStatus.setTextColor(getColorCompat(R.color.danger));
                    tvSearchStatus.setText(R.string.toast_search_empty);
                    return;
                }
                showPlacePicker(finalPlaces);
            });
        });
    }

    private void showPlacePicker(List<Place> places) {
        CharSequence[] labels = new CharSequence[places.size()];
        for (int i = 0; i < places.size(); i++) {
            Place place = places.get(i);
            labels[i] = place.displayName() + "\n"
                    + String.format(java.util.Locale.US, "%.3f, %.3f",
                    place.latitude, place.longitude);
        }
        new AlertDialog.Builder(this, R.style.Theme_BiliClock_Dialog)
                .setTitle(R.string.dialog_city_title)
                .setItems(labels, (dialog, which) -> {
                    Place place = places.get(which);
                    etCity.setText(place.name);
                    etLat.setText(Double.toString(place.latitude));
                    etLon.setText(Double.toString(place.longitude));
                    tvSearchStatus.setVisibility(View.VISIBLE);
                    tvSearchStatus.setTextColor(getColorCompat(R.color.accent_dim));
                    tvSearchStatus.setText(getString(R.string.search_status_format,
                            place.displayName(), place.latitude, place.longitude));
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void save() {
        String uid = etUid.getText().toString().trim();
        if (uid.isEmpty() || !uid.matches("\\d{1,20}")) {
            showError(R.string.toast_uid_invalid, etUid);
            return;
        }
        String city = etCity.getText().toString().trim();
        if (city.isEmpty()) {
            showError(R.string.toast_city_empty, etCity);
            return;
        }
        double lat;
        double lon;
        try {
            lat = Double.parseDouble(etLat.getText().toString().trim());
            lon = Double.parseDouble(etLon.getText().toString().trim());
        } catch (Exception e) {
            showError(R.string.toast_coords_invalid, etLat);
            return;
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            showError(R.string.toast_coords_invalid, etLat);
            return;
        }
        int providerPosition = Math.max(0, Math.min(2, spProvider.getSelectedItemPosition()));
        String provider = providerValues[providerPosition];
        String openMeteo = normalizeUrl(etOpenMeteoBase.getText().toString().trim());
        String qweatherHost = normalizeUrl(etQweatherHost.getText().toString().trim());
        String qweatherKey = etQweatherKey.getText().toString().trim();
        String customUrl = normalizeUrl(etCustomUrl.getText().toString().trim());
        if (Prefs.PROVIDER_OPEN_METEO.equals(provider) && !isHttpUrl(openMeteo)) {
            showError(R.string.toast_url_invalid, etOpenMeteoBase);
            return;
        }
        if (Prefs.PROVIDER_QWEATHER.equals(provider)) {
            if (!isHttpUrl(qweatherHost)) {
                showError(R.string.toast_url_invalid, etQweatherHost);
                return;
            }
            if (qweatherKey.isEmpty()) {
                showError(R.string.toast_qweather_key, etQweatherKey);
                return;
            }
        }
        if (Prefs.PROVIDER_CUSTOM.equals(provider)) {
            if (customUrl.isEmpty() || !isHttpUrl(customUrl)) {
                showError(R.string.toast_custom_url, etCustomUrl);
                return;
            }
        }

        int backgroundPosition = Math.max(0, Math.min(1, spBackground.getSelectedItemPosition()));
        String backgroundMode = backgroundValues[backgroundPosition];
        if (Prefs.BACKGROUND_IMAGE.equals(backgroundMode)) {
            if (pendingBackgroundPicked) {
                if (!prefs.pendingBackgroundFile().isFile()) {
                    showError(R.string.background_failed, spBackground);
                    return;
                }
            } else if (!prefs.hasBackgroundImage()) {
                showError(R.string.background_required, spBackground);
                return;
            }
        }

        prefs.setUid(uid);
        prefs.setCity(city);
        prefs.setLocation(lat, lon);
        prefs.setProvider(provider);
        prefs.setOpenMeteoBase(openMeteo);
        prefs.setQWeatherHost(qweatherHost);
        prefs.setQWeatherKey(qweatherKey);
        prefs.setCustomUrl(customUrl);
        prefs.setCustomKey(etCustomKey.getText().toString().trim());
        prefs.setCustomPaths(
                etCustomTemp.getText().toString(),
                etCustomText.getText().toString(),
                etCustomHumidity.getText().toString(),
                etCustomIcon.getText().toString());
        prefs.setSubtitleMode(subtitleValues[Math.max(0, Math.min(1, spSubtitle.getSelectedItemPosition()))]);
        prefs.setRefreshMinutes(refreshValues[Math.max(0, Math.min(refreshValues.length - 1,
                spRefresh.getSelectedItemPosition()))]);
        prefs.setKeepScreenOn(switchKeepOn.isChecked());
        prefs.setShowSecondHand(switchSecond.isChecked());

        if (Prefs.BACKGROUND_IMAGE.equals(backgroundMode)) {
            if (pendingBackgroundPicked) {
                File pending = prefs.pendingBackgroundFile();
                File destination = prefs.backgroundFile();
                if (destination.exists() && !destination.delete()) {
                    showError(R.string.background_failed, spBackground);
                    return;
                }
                if (!pending.renameTo(destination)) {
                    if (!Images.copyFile(pending, destination)) {
                        showError(R.string.background_failed, spBackground);
                        return;
                    }
                    //noinspection ResultOfMethodCallIgnored
                    pending.delete();
                }
            }
            prefs.setBackgroundMode(Prefs.BACKGROUND_IMAGE);
        } else {
            prefs.setBackgroundMode(Prefs.BACKGROUND_BLACK);
            if (pendingBackgroundPicked) {
                //noinspection ResultOfMethodCallIgnored
                prefs.pendingBackgroundFile().delete();
            }
        }
        pendingBackgroundPicked = false;

        prefs.bumpRevision();
        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
        finishWithFade();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this, R.style.Theme_BiliClock_Dialog)
                .setTitle(R.string.dialog_reset_title)
                .setMessage(R.string.dialog_reset_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    prefs.resetAll();
                    populate();
                    updateProviderVisibility();
                    Toast.makeText(this, R.string.toast_reset_done, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void finishWithFade() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showError(int message, View focus) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if (focus != null) {
            focus.requestFocus();
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            return;
        }
        InputMethodManager manager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private int getColorCompat(int resource) {
        return getResources().getColor(resource);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private String versionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "1.0.0" : info.versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (TextUtils.equals(values[i], value)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalizeUrl(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim();
        if (result.isEmpty()) {
            return "";
        }
        if (!result.startsWith("http://") && !result.startsWith("https://")) {
            return "https://" + result;
        }
        return result;
    }

    private static boolean isHttpUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"))
                && value.length() > 10;
    }

    private abstract static class SimpleItemSelected
            implements android.widget.AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            onItemSelected(position);
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }

        public abstract void onItemSelected(int position);
    }
}
