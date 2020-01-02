package de.robv.android.xposed.installer.activity;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.fragment.BasePreferenceFragment;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.XposedBaseActivity;
import de.robv.android.xposed.installer.util.RepoLoader;

import static de.robv.android.xposed.installer.XposedApp.WRITE_EXTERNAL_PERMISSION;

public class XposedSettingsActivity extends XposedBaseActivity implements ColorChooserDialog.ColorCallback, FolderChooserDialog.FolderCallback {

    @SuppressLint("StaticFieldLeak")
    private static SwitchPreference navBar;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        FrameLayout container = findViewById(R.id.container);
        container.setPadding(dp(8), 0, dp(8), 0);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(view -> finish());

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.nav_item_settings);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setFloating(toolbar, 0);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment()).commit();
        }
    }

    private int dp(float value) {
        float density = getResources().getDisplayMetrics().density;
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        if (folder.canWrite()) {
            XposedApp.getPreferences().edit().putString("download_location", folder.getPath()).apply();
        } else {
            Toast.makeText(this, R.string.sdcard_not_writable, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onColorSelection(ColorChooserDialog dialog, @ColorInt int color) {
        int colorFrom = XposedApp.getColor(this);

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, color);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (int) animator.getAnimatedValue();

                toolbar.setBackgroundColor(color);

                int darkenColor = XposedApp.darkenColor(color, 0.85f);
                if (navBar != null && navBar.isChecked()) {
                    getWindow().setNavigationBarColor(darkenColor);
                }
            }
        });
        colorAnimation.setDuration(750);
        colorAnimation.start();

        if (!dialog.isAccentMode()) {
            XposedApp.getPreferences().edit().putInt("colors", color).apply();
        }
    }

    public static class SettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

        public final static int[] PRIMARY_COLORS = new int[]{
                Color.parseColor("#F44336"),
                Color.parseColor("#E91E63"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#673AB7"),
                Color.parseColor("#3F51B5"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#03A9F4"),
                Color.parseColor("#00BCD4"),
                Color.parseColor("#009688"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#8BC34A"),
                Color.parseColor("#CDDC39"),
                Color.parseColor("#FFEB3B"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#FF5722"),
                Color.parseColor("#795548"),
                Color.parseColor("#9E9E9E"),
                Color.parseColor("#607D8B")
        };

        private static final File mDisableResourcesFlag = new File(XposedApp.BASE_DIR + "conf/disable_resources");
        private static final String DIALOG_FRAGMENT_TAG = "list_preference_dialog";

        private Preference mClickedPreference;

        private Preference downloadLocation;

        public SettingsFragment() {
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.prefs);

            PreferenceGroup groupApp = (PreferenceGroup) findPreference("group_app");
            PreferenceGroup lookFeel = (PreferenceGroup) findPreference("look_and_feel");

            Preference headsUp = findPreference("heads_up");
            Preference colors = findPreference("colors");
            Preference forceEnglish = findPreference("force_english");
            downloadLocation = findPreference("download_location");

            ListPreference customIcon = (ListPreference) findPreference("custom_icon");
            navBar = (SwitchPreference) findPreference("nav_bar");

            if (Build.VERSION.SDK_INT < 21) {
                groupApp.removePreference(headsUp);
                lookFeel.removePreference(navBar);
            }

            findPreference("release_type_global").setOnPreferenceChangeListener((preference, newValue) -> {
                RepoLoader.getInstance().setReleaseTypeGlobal((String) newValue);
                return true;
            });

            SwitchPreference prefDisableResources = (SwitchPreference) findPreference("disable_resources");
            prefDisableResources.setChecked(mDisableResourcesFlag.exists());
            prefDisableResources.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        try {
                            mDisableResourcesFlag.createNewFile();
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        mDisableResourcesFlag.delete();
                    }
                    return (enabled == mDisableResourcesFlag.exists());
                }
            });

            downloadLocation.setOnPreferenceClickListener(this);

            if (Locale.getDefault().getLanguage().contains("en")
                    && !XposedApp.getPreferences().getBoolean("force_english", false)) {
                groupApp.removePreference(forceEnglish);
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            getListView().setVerticalScrollBarEnabled(false);
            getListView().setOverScrollMode(View.OVER_SCROLL_NEVER);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

//            if (Build.VERSION.SDK_INT >= 21) {
//                if (ThemeUtil.getSelectTheme().equals("light"))
//                    getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.app_light));
//                else if (ThemeUtil.getSelectTheme().equals("dark"))
//                    getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.app_dark));
//            }
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("theme") || key.equals("nav_bar") || key.equals("ignore_chinese"))
                getActivity().recreate();

            if (key.equals("force_english"))
                Toast.makeText(getActivity(), getString(R.string.warning_language), Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            XposedSettingsActivity act = (XposedSettingsActivity) getActivity();
            if (act == null)
                return false;

            if (preference.getKey().equals("colors")) {
//                new ColorChooserDialog.Builder(act, R.string.choose_color)
//                        .backButton(R.string.back)
//                        .allowUserColorInput(false)
//                        .customColors(PRIMARY_COLORS, null)
//                        .doneButton(android.R.string.ok)
//                        .preselect(XposedApp.getColor(act)).show();
            } else if (preference.getKey().equals(downloadLocation.getKey())) {
                if (checkPermissions()) {
                    mClickedPreference = downloadLocation;
                    return false;
                }

                new FolderChooserDialog.Builder(act)
                        .cancelButton(android.R.string.cancel)
                        .initialPath(XposedApp.getDownloadPath())
                        .show();
            }

            return true;
        }

        private boolean checkPermissions() {
            if (Build.VERSION.SDK_INT < 23) return false;

            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
                return true;
            }
            return false;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mClickedPreference != null) {
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onPreferenceClick(mClickedPreference);
                        }
                    }, 500);
                }
            } else {
                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }
    }
}
