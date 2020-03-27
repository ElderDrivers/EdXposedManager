package org.meowcat.edxposed.manager;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.util.RepoLoader;
import org.meowcat.edxposed.manager.util.ThemeUtil;
import org.meowcat.edxposed.manager.widget.IconListPreference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import static org.meowcat.edxposed.manager.XposedApp.WRITE_EXTERNAL_PERMISSION;
import static org.meowcat.edxposed.manager.XposedApp.darkenColor;

public class SettingsActivity extends XposedBaseActivity implements ColorChooserDialog.ColorCallback, FolderChooserDialog.FolderCallback {

    @SuppressLint("StaticFieldLeak")
    private static SwitchPreference navBar;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_container);

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

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        if (folder.canWrite()) {
            XposedApp.getPreferences().edit().putString("download_location", folder.getPath()).apply();
        } else {
            Toast.makeText(this, R.string.sdcard_not_writable, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int color) {
        int colorFrom = XposedApp.getColor(this);

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, color);
        colorAnimation.addUpdateListener(animator -> {
            int color1 = (int) animator.getAnimatedValue();

            toolbar.setBackgroundColor(color1);

            int darkenColor = XposedApp.darkenColor(color1, 0.85f);

            getWindow().setStatusBarColor(darkenColor);

            if (navBar != null && navBar.isChecked()) {
                getWindow().setNavigationBarColor(darkenColor);
            }
        });
        colorAnimation.setDuration(750);
        colorAnimation.start();

        if (!dialog.isAccentMode()) {
            XposedApp.getPreferences().edit().putInt("colors", color).apply();
        }
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
    public static class SettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

        final static int[] PRIMARY_COLORS = new int[]{
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
                Color.parseColor("#607D8B"),
                Color.parseColor("#FA7298")
        };
        static final File mDisableResourcesFlag = new File(XposedApp.BASE_DIR + "conf/disable_resources");
        static final File mDynamicModulesFlag = new File(XposedApp.BASE_DIR + "conf/dynamicmodules");
        static final File mWhiteListModeFlag = new File(XposedApp.BASE_DIR + "conf/usewhitelist");
        static final File mBlackWhiteListModeFlag = new File(XposedApp.BASE_DIR + "conf/blackwhitelist");
        static final File mDeoptBootFlag = new File(XposedApp.BASE_DIR + "conf/deoptbootimage");
        static final File mDisableVerboseLogsFlag = new File(XposedApp.BASE_DIR + "conf/disable_verbose_log");
        static final File mDisableModulesLogsFlag = new File(XposedApp.BASE_DIR + "conf/disable_modules_log");
        static final File mVerboseLogProcessID = new File(XposedApp.BASE_DIR + "log/all.pid");
        static final File mModulesLogProcessID = new File(XposedApp.BASE_DIR + "log/error.pid");
        private static final String DIALOG_FRAGMENT_TAG = "list_preference_dialog";

        private Preference mClickedPreference;

        private Preference.OnPreferenceChangeListener iconChange = (preference, newValue) -> {
            String act = ".WelcomeActivity";
            String[] iconsValues = new String[]{"MlgmXyysd", "DVDAndroid", "Hjmodi", "Rovo", "Cornie", "RovoOld", "Staol"};

            PackageManager pm = requireActivity().getPackageManager();
            String packName = requireActivity().getPackageName();

            for (String s : iconsValues) {
                pm.setComponentEnabledSetting(new ComponentName(packName, packName + act + s), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }

            act += iconsValues[Integer.parseInt((String) newValue)];

            int drawable = XposedApp.iconsValues[Integer.parseInt((String) newValue)];

            ActivityManager.TaskDescription tDesc = new ActivityManager.TaskDescription(getString(R.string.app_name),
                    XposedApp.drawableToBitmap(requireContext().getDrawable(drawable)),
                    XposedApp.getColor(requireContext()));
            requireActivity().setTaskDescription(tDesc);

            pm.setComponentEnabledSetting(new ComponentName(requireContext(), packName + act), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            return true;
        };

        private Preference downloadLocation;
        private Preference stopVerboseLog;
        private Preference stopLog;

        public SettingsFragment() {
        }

        @SuppressLint({"ObsoleteSdkInt", "WorldReadableFiles"})
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.prefs);
            PreferenceGroup groupFramework = findPreference("group_framework");
            Preference whiteListSwitch = Objects.requireNonNull(groupFramework).findPreference("white_list_switch");
            Preference passSafetynet = Objects.requireNonNull(groupFramework).findPreference("pass_safetynet");
            Preference hookModules = Objects.requireNonNull(groupFramework).findPreference("hook_modules");
            if (!XposedApp.getPreferences().getBoolean("black_white_list_switch", false)) {
                Objects.requireNonNull(whiteListSwitch).setEnabled(false);
                Objects.requireNonNull(passSafetynet).setEnabled(false);
                Objects.requireNonNull(hookModules).setEnabled(false);
            }
//            PreferenceGroup groupApp = findPreference("group_app");
//            PreferenceGroup lookFeel = findPreference("look_and_feel");

            //Preference headsUp = findPreference("heads_up");
            Preference colors = findPreference("colors");
            downloadLocation = findPreference("download_location");
            stopVerboseLog = findPreference("stop_verbose_log");
            stopLog = findPreference("stop_log");

            ListPreference customIcon = findPreference("custom_icon");
            navBar = findPreference("nav_bar");

//            if (Build.VERSION.SDK_INT < 21) {
//                Objects.requireNonNull(groupApp).removePreference(Objects.requireNonNull(headsUp));
//                Objects.requireNonNull(groupApp).removePreference(navBar);
//            }

            //noinspection ConstantConditions
            findPreference("release_type_global").setOnPreferenceChangeListener((preference, newValue) -> {
                RepoLoader.getInstance().setReleaseTypeGlobal((String) newValue);
                return true;
            });

            SwitchPreference prefWhiteListMode = findPreference("white_list_switch");
            Objects.requireNonNull(prefWhiteListMode).setChecked(mWhiteListModeFlag.exists());
            prefWhiteListMode.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mWhiteListModeFlag.getPath());
                        XposedApp.setFilePermissionsFromMode(mWhiteListModeFlag.getPath(), Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mWhiteListModeFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mWhiteListModeFlag.delete();
                }
                return (enabled == mWhiteListModeFlag.exists());
            });

            SwitchPreference prefVerboseLogs = findPreference("disable_verbose_log");
            Objects.requireNonNull(prefVerboseLogs).setChecked(mDisableVerboseLogsFlag.exists());
            prefVerboseLogs.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDisableVerboseLogsFlag.getPath());
                        XposedApp.setFilePermissionsFromMode(mDisableVerboseLogsFlag.getPath(), Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDisableVerboseLogsFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDisableVerboseLogsFlag.delete();
                }
                return (enabled == mDisableVerboseLogsFlag.exists());
            });

            SwitchPreference prefModulesLogs = findPreference("disable_modules_log");
            Objects.requireNonNull(prefModulesLogs).setChecked(mDisableModulesLogsFlag.exists());
            prefModulesLogs.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDisableModulesLogsFlag.getPath());
                        XposedApp.setFilePermissionsFromMode(mDisableModulesLogsFlag.getPath(), Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDisableModulesLogsFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDisableModulesLogsFlag.delete();
                }
                return (enabled == mDisableModulesLogsFlag.exists());
            });

            SwitchPreference prefBlackWhiteListMode = findPreference("black_white_list_switch");
            Objects.requireNonNull(prefBlackWhiteListMode).setChecked(mBlackWhiteListModeFlag.exists());
            prefBlackWhiteListMode.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mBlackWhiteListModeFlag.getPath());
                        XposedApp.setFilePermissionsFromMode(mBlackWhiteListModeFlag.getPath(), Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mBlackWhiteListModeFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                    Objects.requireNonNull(whiteListSwitch).setEnabled(true);
                    Objects.requireNonNull(passSafetynet).setEnabled(true);
                    Objects.requireNonNull(hookModules).setEnabled(true);
                } else {
                    mBlackWhiteListModeFlag.delete();
                    Objects.requireNonNull(whiteListSwitch).setEnabled(false);
                    Objects.requireNonNull(passSafetynet).setEnabled(false);
                    Objects.requireNonNull(hookModules).setEnabled(false);
                }
                return (enabled == mBlackWhiteListModeFlag.exists());
            });

            SwitchPreference prefEnableDeopt = findPreference("enable_boot_image_deopt");
            Objects.requireNonNull(prefEnableDeopt).setChecked(mDeoptBootFlag.exists());
            prefEnableDeopt.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDeoptBootFlag.getPath());
                        XposedApp.setFilePermissionsFromMode(mDeoptBootFlag.getPath(), Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDeoptBootFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDeoptBootFlag.delete();
                }
                return (enabled == mDeoptBootFlag.exists());
            });

            SwitchPreference prefDynamicResources = findPreference("is_dynamic_modules");
            Objects.requireNonNull(prefDynamicResources).setChecked(mDynamicModulesFlag.exists());
            prefDynamicResources.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDynamicModulesFlag.getPath());
                        XposedApp.setFilePermissionsFromMode(mDynamicModulesFlag.getPath(), Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDynamicModulesFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDynamicModulesFlag.delete();
                }
                return (enabled == mDynamicModulesFlag.exists());
            });

            SwitchPreference prefDisableResources = findPreference("disable_resources");
            Objects.requireNonNull(prefDisableResources).setChecked(mDisableResourcesFlag.exists());
            prefDisableResources.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDisableResourcesFlag.getPath());
                        XposedApp.setFilePermissionsFromMode(mDisableResourcesFlag.getPath(), Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDisableResourcesFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDisableResourcesFlag.delete();
                }
                return (enabled == mDisableResourcesFlag.exists());
            });

            Objects.requireNonNull(colors).setOnPreferenceClickListener(this);
            Objects.requireNonNull(customIcon).setOnPreferenceChangeListener(iconChange);
            downloadLocation.setOnPreferenceClickListener(this);

        }

        @Override
        public void onResume() {
            super.onResume();

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            //if (Build.VERSION.SDK_INT >= 21)
            requireActivity().getWindow().setStatusBarColor(darkenColor(XposedApp.getColor(requireActivity()), 0.85f));
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("theme") || key.equals("nav_bar") || key.equals("ignore_chinese"))
                requireActivity().recreate();

            if (key.equals("force_english"))
                Toast.makeText(getActivity(), getString(R.string.warning_language), Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            SettingsActivity act = (SettingsActivity) getActivity();
            if (act == null)
                return false;

            if (preference.getKey().equals("colors")) {
                new ColorChooserDialog.Builder(act, R.string.choose_color)
                        .backButton(R.string.back)
                        .allowUserColorInput(false)
                        .customColors(PRIMARY_COLORS, null)
                        .doneButton(R.string.ok)
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
            } else if (preference.getKey().equals(stopVerboseLog.getKey())) {
                new Runnable() {
                    @Override
                    public void run() {
                        areYouSure(R.string.settings_summary_stop_verbose_log, new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                Shell.su("kill $(cat " + mVerboseLogProcessID.getAbsolutePath() + ")").exec();
                            }
                        });
                    }
                };
            } else if (preference.getKey().equals(stopLog.getKey())) {
                new Runnable() {
                    @Override
                    public void run() {
                        areYouSure(R.string.settings_summary_stop_log, new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                Shell.su("kill $(cat " + mModulesLogProcessID.getAbsolutePath() + ")").exec();
                            }
                        });
                    }
                };
            }
            return true;
        }

        private void areYouSure(int contentTextId, MaterialDialog.ButtonCallback yesHandler) {
            new MaterialDialog.Builder(requireActivity()).title(R.string.areyousure)
                    .content(contentTextId)
                    .iconAttr(android.R.attr.alertDialogIcon)
                    .positiveText(R.string.ok)
                    .negativeText(android.R.string.no).callback(yesHandler).show();
        }

        private boolean checkPermissions() {
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
                return true;
            }
            return false;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mClickedPreference != null) {
                    new android.os.Handler().postDelayed(() -> onPreferenceClick(mClickedPreference), 500);
                }
            } else {
                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (preference instanceof IconListPreference) {
                final IconListPreference.IconListPreferenceDialog f =
                        IconListPreference.IconListPreferenceDialog.newInstance(preference.getKey());
                f.setTargetFragment(this, 0);
                f.show(requireFragmentManager(), DIALOG_FRAGMENT_TAG);
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }
    }
}
