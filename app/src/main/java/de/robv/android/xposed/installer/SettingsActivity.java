package de.robv.android.xposed.installer;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import de.robv.android.xposed.installer.util.RepoLoader;
import de.robv.android.xposed.installer.util.ThemeUtil;

import static de.robv.android.xposed.installer.XposedApp.WRITE_EXTERNAL_PERMISSION;
import static de.robv.android.xposed.installer.XposedApp.darkenColor;

public class SettingsActivity extends XposedBaseActivity implements ColorChooserDialog.ColorCallback, FolderChooserDialog.FolderCallback {

    private static SwitchPreference navBar;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_container);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.nav_item_settings);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setFloating(toolbar, 0);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment()).commit();
        }

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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int darkenColor = XposedApp.darkenColor(color, 0.85f);

                    getWindow().setStatusBarColor(darkenColor);

                    if (navBar != null && navBar.isChecked()) {
                        getWindow().setNavigationBarColor(darkenColor);
                    }
                }
            }
        });
        colorAnimation.setDuration(750);
        colorAnimation.start();

        if (!dialog.isAccentMode()) {
            XposedApp.getPreferences().edit().putInt("colors", color).apply();
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

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

        private Preference mClickedPreference;

        private Preference.OnPreferenceChangeListener iconChange = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String act = ".WelcomeActivity";
                String[] iconsValues = new String[]{"dvdandroid", "hjmodi", "rovo", "rovoold", "staol"};

                Context context = getActivity();
                PackageManager pm = getActivity().getPackageManager();
                String packName = getActivity().getPackageName();

                for (String s : iconsValues) {
                    pm.setComponentEnabledSetting(new ComponentName(packName, packName + act + s), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }

                act += iconsValues[Integer.parseInt((String) newValue)];

                int drawable = XposedApp.iconsValues[Integer.parseInt((String) newValue)];

                if (Build.VERSION.SDK_INT >= 21) {

                    ActivityManager.TaskDescription tDesc = new ActivityManager.TaskDescription(getString(R.string.app_name),
                            XposedApp.drawableToBitmap(context.getDrawable(drawable)),
                            XposedApp.getColor(context));
                    getActivity().setTaskDescription(tDesc);
                }

                pm.setComponentEnabledSetting(new ComponentName(context, packName + act), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                return true;
            }
        };

        private Preference downloadLocation;

        public SettingsFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
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

            findPreference("release_type_global").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    RepoLoader.getInstance().setReleaseTypeGlobal((String) newValue);
                    return true;
                }
            });

            CheckBoxPreference prefDisableResources = (CheckBoxPreference) findPreference("disable_resources");
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

            colors.setOnPreferenceClickListener(this);
            customIcon.setOnPreferenceChangeListener(iconChange);
            downloadLocation.setOnPreferenceClickListener(this);

            if (Locale.getDefault().getLanguage().contains("en")
                    && !XposedApp.getPreferences().getBoolean("force_english", false)) {
                groupApp.removePreference(forceEnglish);
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            if (Build.VERSION.SDK_INT >= 21)
                getActivity().getWindow().setStatusBarColor(darkenColor(XposedApp.getColor(getActivity()), 0.85f));
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
            SettingsActivity act = (SettingsActivity) getActivity();
            if (act == null)
                return false;

            if (preference.getKey().equals("colors")) {
                new ColorChooserDialog.Builder(act, preference.getTitleRes())
                        .backButton(R.string.back)
                        .allowUserColorInput(false)
                        .customColors(PRIMARY_COLORS, null)
                        .doneButton(android.R.string.ok)
                        .preselect(XposedApp.getColor(act)).show();
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
