package com.solohsu.android.edxp.manager.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.adapter.AppHelper;
import com.github.coxylicacid.mdwidgets.dialog.MD2Dialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import de.robv.android.xposed.installer.WelcomeActivity;
import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.activity.SELinuxActivity;
import de.robv.android.xposed.installer.util.MyFileUtils;
import de.robv.android.xposed.installer.util.RootUtil;
import de.robv.android.xposed.installer.util.ThemeUtil;

import static com.solohsu.android.edxp.manager.adapter.AppHelper.setBlackWhiteListEnabled;
import static com.solohsu.android.edxp.manager.adapter.AppHelper.setBootImageDeoptEnabled;

public class SettingFragment extends BasePreferenceFragment {

    private static final int MODE_WORLD_READABLE = 0x001;
    private static final int MODE_WORLD_WRITEABLE = 0x002;
    private RootUtil mRootUtil = new RootUtil();

    public SettingFragment() {

    }

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    private static final File mDynamicModulesFlag = new File(XposedApp.BASE_DIR + "conf/dynamicmodules");

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBar actionBar = ((WelcomeActivity) getActivity()).getSupportActionBar();
        int toolBarDp = actionBar.getHeight() == 0 ? 196 : actionBar.getHeight();
        RecyclerView listView = getListView();
        listView.setPadding(listView.getPaddingLeft(), toolBarDp + listView.getPaddingTop(),
                listView.getPaddingRight(), listView.getPaddingBottom());

        try {
            mRootUtil.startShell();
            mRootUtil.execute("getenforce", new RootUtil.LineCallback() {
                @Override
                public void onLine(String line) {
                    if ((line.contains("Permissive") || line.contains("permissive")) && ((SwitchPreference) findPreference("dynamic_modules_enabled")).isChecked()) {
                        Toast.makeText(getContext(), "即时模块运行中", Toast.LENGTH_LONG).show();
                    } else {
                        MD2Dialog.create(getContext()).darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                                .title("SELinux")
                                .msg(R.string.selinux_describe)
                                .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
                                .onConfirmClick("关闭它!", (view, dialog) -> startActivity(new Intent(getContext(), SELinuxActivity.class)))
                                .onCancelClick("我害怕.", ((view, dialog) -> dialog.dismiss())).show();
                    }
                }

                @Override
                public void onErrorLine(String line) {
                    Toast.makeText(getContext(), "SELinux状态获取错误", Toast.LENGTH_LONG).show();
                }
            });
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.nav_title_settings);
        setDividerHeight(2);
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.pref_settings);

        File flagFile = null;

        SwitchPreference prefDynamicResources = findPreference("dynamic_modules_enabled");
        flagFile = mDynamicModulesFlag;
        Objects.requireNonNull(prefDynamicResources).setChecked(flagFile.exists());
        File finalFlagFile1 = flagFile;
        prefDynamicResources.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (Boolean) newValue;
            if (enabled) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(finalFlagFile1.getPath());
                    setFilePermissionsFromMode(finalFlagFile1.getPath(), MODE_WORLD_READABLE);
//                    Log.e("DYNAMIC_MODULES", "File is already existed,Now giving the permission");
                } catch (FileNotFoundException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
//                            Log.e("DYNAMIC_MODULES", "File Output Stream is closed");
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            try {
                                boolean isCreate = finalFlagFile1.createNewFile();
//                                Log.e("DYNAMIC_MODULES", "Create new file: " + isCreate);
                            } catch (IOException e1) {
                                Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }

                try {
                    mRootUtil.startShell();
                    mRootUtil.execute("getenforce", new RootUtil.LineCallback() {
                        @Override
                        public void onLine(String line) {
                            if (line.contains("Permissive") || line.contains("permissive")) {
                                Toast.makeText(getContext(), "已开启即时模块", Toast.LENGTH_LONG).show();
                            } else {
                                MD2Dialog.create(getContext()).darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                                        .title("SELinux")
                                        .msg(R.string.selinux_describe)
                                        .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
                                        .onConfirmClick("关闭它!", (view, dialog) -> startActivity(new Intent(getContext(), SELinuxActivity.class)))
                                        .onCancelClick("我害怕.", ((view, dialog) -> dialog.dismiss())).show();
                            }
                        }

                        @Override
                        public void onErrorLine(String line) {
                            Toast.makeText(getContext(), "SELinux状态获取错误", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (IllegalStateException e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MD2Dialog.create(getActivity()).darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                                    .title("警告").msg("您的手机没有root权限\n即时模块功能开启后将会没有任何效果").simpleConfirm("OK").show();
                        }
                    });
                }
            } else {
                boolean isDeleted = finalFlagFile1.delete();
//                Log.e("DYNAMIC_MODULES", "File Deleted: " + isDeleted);
            }
            return (enabled == finalFlagFile1.exists());
        });

        SwitchPreference blackListPref = findPreference("black_white_list_enabled");
        blackListPref.setChecked(AppHelper.blackWhiteListEnabled());
        blackListPref.setOnPreferenceChangeListener(
                (preference, newValue) -> setBlackWhiteListEnabled((Boolean) newValue));


        SwitchPreference enableDeoptPref = findPreference("enable_boot_image_deopt");
        enableDeoptPref.setChecked(AppHelper.bootImageDeoptEnabled());
        enableDeoptPref.setOnPreferenceChangeListener(
                (preference, newValue) -> setBootImageDeoptEnabled((Boolean) newValue));

        final File disableVerboseLogsFlag = new File(XposedApp.BASE_DIR + "conf/disable_verbose_log");
        SwitchPreference prefDisableResources = findPreference("disable_verbose_log");
        prefDisableResources.setChecked(disableVerboseLogsFlag.exists());
        prefDisableResources.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (Boolean) newValue;
            if (enabled) {
                try {
                    disableVerboseLogsFlag.createNewFile();
                } catch (IOException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                disableVerboseLogsFlag.delete();
            }
            return (enabled == disableVerboseLogsFlag.exists());
        });
    }

    @SuppressLint({"WorldReadableFiles", "WorldWriteableFiles"})
    private static void setFilePermissionsFromMode(String name, int mode) {
        int perms = MyFileUtils.S_IRUSR | MyFileUtils.S_IWUSR
                | MyFileUtils.S_IRGRP | MyFileUtils.S_IWGRP;
        if ((mode & MODE_WORLD_READABLE) != 0) {
            perms |= MyFileUtils.S_IROTH;
        }
        if ((mode & MODE_WORLD_WRITEABLE) != 0) {
            perms |= MyFileUtils.S_IWOTH;
        }
        MyFileUtils.setPermissions(name, perms, -1, -1);
    }


}