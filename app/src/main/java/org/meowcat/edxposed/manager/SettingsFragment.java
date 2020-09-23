package org.meowcat.edxposed.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.adapter.AppHelper;
import org.meowcat.edxposed.manager.adapter.ApplicationListAdapter;
import org.meowcat.edxposed.manager.util.RepoLoader;
import org.meowcat.edxposed.manager.widget.IconListPreference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import static org.meowcat.edxposed.manager.SettingsActivity.getDarkenFactor;
import static org.meowcat.edxposed.manager.XposedApp.WRITE_EXTERNAL_PERMISSION;
import static org.meowcat.edxposed.manager.XposedApp.darkenColor;
import static org.meowcat.edxposed.manager.XposedApp.getPreferences;

public class SettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    @SuppressLint("StaticFieldLeak")
    static SwitchPreference navBar;

    private final static int[] PRIMARY_COLORS = new int[]{
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
    private static final File mDisableForceClientSafetyNetFlag = new File(XposedApp.BASE_DIR + "conf/disable_force_client_safetynet");
    private static final File mPretendXposedInstallerFlag = new File(XposedApp.BASE_DIR + "conf/pretend_xposed_installer");
    private static final File mHideEdXposedManagerFlag = new File(XposedApp.BASE_DIR + "conf/hide_edxposed_manager");
    private static final File mDisableResourcesFlag = new File(XposedApp.BASE_DIR + "conf/disable_resources");
    private static final File mDisableHiddenAPIBypassFlag = new File(XposedApp.BASE_DIR + "conf/disable_hidden_api_bypass");
    private static final File mDynamicModulesFlag = new File(XposedApp.BASE_DIR + "conf/dynamicmodules");
    private static final File mWhiteListModeFlag = new File(XposedApp.BASE_DIR + "conf/usewhitelist");
    private static final File mBlackWhiteListModeFlag = new File(XposedApp.BASE_DIR + "conf/blackwhitelist");
    private static final File mDeoptBootFlag = new File(XposedApp.BASE_DIR + "conf/deoptbootimage");
    private static final File mDisableVerboseLogsFlag = new File(XposedApp.BASE_DIR + "conf/disable_verbose_log");
    private static final File mDisableModulesLogsFlag = new File(XposedApp.BASE_DIR + "conf/disable_modules_log");
    private static final File mVerboseLogProcessID = new File(XposedApp.BASE_DIR + "log/all.pid");
    private static final File mModulesLogProcessID = new File(XposedApp.BASE_DIR + "log/error.pid");
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

//        ActivityManager.TaskDescription tDesc = new ActivityManager.TaskDescription(getString(R.string.app_name),
//                requireContext().getDrawable(XposedApp.iconsValues[Integer.parseInt(Objects.requireNonNull(getPreferences().getString("custom_icon", "0")))]),
//                XposedApp.getColor(requireContext()));
        @SuppressWarnings("deprecation") ActivityManager.TaskDescription tDesc = new ActivityManager.TaskDescription(getString(R.string.app_name),
                XposedApp.drawableToBitmap(requireContext().getDrawable(XposedApp.iconsValues[Integer.parseInt(Objects.requireNonNull(getPreferences().getString("custom_icon", "0")))])),
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

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
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

        Preference colors = findPreference("colors");
        downloadLocation = findPreference("download_location");
        stopVerboseLog = findPreference("stop_verbose_log");
        stopLog = findPreference("stop_log");

        ListPreference customIcon = findPreference("custom_icon");
        navBar = findPreference("nav_bar");

        Preference release_type_global = findPreference("release_type_global");
        Objects.requireNonNull(release_type_global).setOnPreferenceChangeListener((preference, newValue) -> {
            RepoLoader.getInstance().setReleaseTypeGlobal((String) newValue);
            return true;
        });

        Preference enhancement_status = findPreference("enhancement_status");
        Objects.requireNonNull(enhancement_status).setSummary(StatusInstallerFragment.isEnhancementEnabled() ? R.string.settings_summary_enhancement_enabled : R.string.settings_summary_enhancement);
        
        SwitchPreference darkStatusBarPref = findPreference("dark_status_bar");
        Objects.requireNonNull(darkStatusBarPref).setOnPreferenceChangeListener((preference, newValue) -> {
            requireActivity().getWindow().setStatusBarColor(darkenColor(XposedApp.getColor(requireActivity()), (boolean) newValue ? 0.85f : 1f));
            return true;
        });

        SwitchPreference prefPassClientSafetyNet = findPreference("pass_client_safetynet");
        Objects.requireNonNull(prefPassClientSafetyNet).setChecked(!mDisableForceClientSafetyNetFlag.exists());
        prefPassClientSafetyNet.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean) newValue;
            if (!enabled) {
                new ApplicationListAdapter(getContext(), AppHelper.isWhiteListMode()).generateCheckedList();
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(mDisableForceClientSafetyNetFlag.getPath());
                    XposedApp.setFilePermissionsFromMode(mDisableForceClientSafetyNetFlag.getPath(), Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            try {
                                mDisableForceClientSafetyNetFlag.createNewFile();
                            } catch (IOException e1) {
                                Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            } else {
                mDisableForceClientSafetyNetFlag.delete();
            }
            return (enabled != mDisableForceClientSafetyNetFlag.exists());
        });

        SwitchPreference prefPretendXposedInstaller = findPreference("pretend_xposed_installer");
        Objects.requireNonNull(prefPretendXposedInstaller).setChecked(mPretendXposedInstallerFlag.exists());
        prefPretendXposedInstaller.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean) newValue;
            if (enabled) {
                new ApplicationListAdapter(getContext(), AppHelper.isWhiteListMode()).generateCheckedList();
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(mPretendXposedInstallerFlag.getPath());
                    XposedApp.setFilePermissionsFromMode(mPretendXposedInstallerFlag.getPath(), Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            try {
                                mPretendXposedInstallerFlag.createNewFile();
                            } catch (IOException e1) {
                                Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            } else {
                mPretendXposedInstallerFlag.delete();
            }
            return (enabled == mPretendXposedInstallerFlag.exists());
        });

        SwitchPreference prefHideEdXposedManager = findPreference("hide_edxposed_manager");
        Objects.requireNonNull(prefHideEdXposedManager).setChecked(mHideEdXposedManagerFlag.exists());
        prefHideEdXposedManager.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean) newValue;
            if (enabled) {
                new ApplicationListAdapter(getContext(), AppHelper.isWhiteListMode()).generateCheckedList();
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(mHideEdXposedManagerFlag.getPath());
                    XposedApp.setFilePermissionsFromMode(mHideEdXposedManagerFlag.getPath(), Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            try {
                                mHideEdXposedManagerFlag.createNewFile();
                            } catch (IOException e1) {
                                Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            } else {
                mHideEdXposedManagerFlag.delete();
            }
            return (enabled == mHideEdXposedManagerFlag.exists());
        });

        SwitchPreference prefWhiteListMode = findPreference("white_list_switch");
        Objects.requireNonNull(prefWhiteListMode).setChecked(mWhiteListModeFlag.exists());
        prefWhiteListMode.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean) newValue;
            if (enabled) {
                new ApplicationListAdapter(getContext(), AppHelper.isWhiteListMode()).generateCheckedList();
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
            boolean enabled = (boolean) newValue;
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
            boolean enabled = (boolean) newValue;
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
            boolean enabled = (boolean) newValue;
            if (enabled) {
                new ApplicationListAdapter(getContext(), AppHelper.isWhiteListMode()).generateCheckedList();
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
            boolean enabled = (boolean) newValue;
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
            boolean enabled = (boolean) newValue;
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
            boolean enabled = (boolean) newValue;
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

        SwitchPreference prefDisableHiddenAPIBypass = findPreference("disable_hidden_api_bypass");
        Objects.requireNonNull(prefDisableHiddenAPIBypass).setChecked(mDisableHiddenAPIBypassFlag.exists());
        prefDisableHiddenAPIBypass.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean) newValue;
            if (enabled) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(mDisableHiddenAPIBypassFlag.getPath());
                    XposedApp.setFilePermissionsFromMode(mDisableHiddenAPIBypassFlag.getPath(), Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            try {
                                mDisableHiddenAPIBypassFlag.createNewFile();
                            } catch (IOException e1) {
                                Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            } else {
                mDisableHiddenAPIBypassFlag.delete();
            }
            return (enabled == mDisableHiddenAPIBypassFlag.exists());
        });

        Objects.requireNonNull(colors).setOnPreferenceClickListener(this);
        Objects.requireNonNull(customIcon).setOnPreferenceChangeListener(iconChange);
        downloadLocation.setOnPreferenceClickListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        requireActivity().getWindow().setStatusBarColor(darkenColor(XposedApp.getColor(requireActivity()), getDarkenFactor()));
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
                    BaseFragment.areYouSure(requireActivity(), getString(R.string.settings_summary_stop_log), (d, w) -> Shell.su("pkill -P $(cat " + mVerboseLogProcessID.getAbsolutePath() + ")").exec(), (d, w) -> {
                    });
                }
            };
        } else if (preference.getKey().equals(stopLog.getKey())) {
            new Runnable() {
                @Override
                public void run() {
                    BaseFragment.areYouSure(requireActivity(), getString(R.string.settings_summary_stop_log), (d, w) -> Shell.su("pkill -P $(cat " + mModulesLogProcessID.getAbsolutePath() + ")").exec(), (d, w) -> {
                    });
                }
            };
        }
        return true;
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
            f.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
