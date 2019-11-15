package de.robv.android.xposed.installer.installation;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.github.coxylicacid.mdwidgets.dialog.MD2Dialog;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.solohsu.android.edxp.manager.BuildConfig;
import com.solohsu.android.edxp.manager.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.installer.activity.PingActivity;
import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.AssetUtil;
import de.robv.android.xposed.installer.util.NavUtil;
import de.robv.android.xposed.installer.util.RootUtil;
import de.robv.android.xposed.installer.util.ThemeUtil;
import de.robv.android.xposed.installer.util.json.JSONUtils;
import de.robv.android.xposed.installer.util.json.XposedTab;
import solid.ren.skinlibrary.base.SkinBaseFragment;

import static android.content.Context.MODE_PRIVATE;

public class AdvancedInstallerFragment extends SkinBaseFragment {

    private static ViewPager mPager;
    private TabLayout mTabLayout;
    private RootUtil mRootUtil = new RootUtil();
    private TabsAdapter tabsAdapter;
    private String JSON_DATA = "";

    public static void gotoPage(int page) {
        mPager.setCurrentItem(page);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_advanced_installer, container, false);
        mPager = view.findViewById(R.id.pager);
        mTabLayout = view.findViewById(R.id.tab_layout);

        dynamicAddView(mTabLayout, "tabIndicator", R.color.colorAccent);
        dynamicAddView(mTabLayout, "tabTextColor", R.color.tab_unselected_color);
        dynamicAddView(mTabLayout, "tabSelectedTextColor", R.color.colorAccent);
        dynamicAddView(mTabLayout, "tabRippleColor", R.color.colorAccentLight);

        tabsAdapter = new TabsAdapter(getChildFragmentManager());
        tabsAdapter.notifyDataSetChanged();
        mPager.setAdapter(tabsAdapter);
        mTabLayout.setupWithViewPager(mPager);

        setHasOptionsMenu(true);
        new JSONParser().execute();

        boolean autoUpdate = XposedApp.getPreferences().getBoolean("auto_update_checkable", true);
//        Log.e("UpdatePush", "自动更新状态: " + autoUpdate);
        if (autoUpdate) {
            UpdatePush updatePush = new UpdatePush(); //更新推送
            updatePush.execute();
        }

        if (!XposedApp.getPreferences().getBoolean("hide_install_warning", false)) {
            final View dontShowAgainView = inflater.inflate(R.layout.dialog_install_warning, null);

            new MD2Dialog(getActivity())
                    .title(R.string.install_warning_title)
                    .msg(R.string.install_warning)
                    .enableCheckBox(R.string.dont_show_again, true)
                    .onConfirmClick(android.R.string.ok, (view1, dialog) -> {
                        if (dialog.getCheckBoxStatus())
                            XposedApp.getPreferences().edit().putBoolean("hide_install_warning", true).apply();
                        dialog.dismiss();
                    }).show();

//            new MaterialDialog.Builder(getActivity())
//                    .title(R.string.install_warning_title)
//                    .customView(dontShowAgainView, false)
//                    .positiveText(android.R.string.ok)
//                    .callback(new MaterialDialog.ButtonCallback() {
//                        @Override
//                        public void onPositive(MaterialDialog dialog) {
//                            super.onPositive(dialog);
//                            CheckBox checkBox = dontShowAgainView.findViewById(android.R.id.checkbox);
//                            if (checkBox.isChecked())
//                                XposedApp.getPreferences().edit().putBoolean("hide_install_warning", true).apply();
//                        }
//                    }).cancelable(false).show();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        //mTabLayout.setBackgroundColor(XposedApp.getColor(getContext()));

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_installer, menu);
        if (Build.VERSION.SDK_INT < 26) {
            menu.findItem(R.id.dexopt_all).setVisible(false);
            menu.findItem(R.id.speed_all).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dexopt_all:
                MD2Dialog.create(this.getActivity())
                        .title(R.string.areyousure)
                        .msg(R.string.take_while_cannot_resore)
                        .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
                        .simpleCancel()
                        .darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                        .onConfirmClick((view, dg) -> {
                            new MaterialDialog.Builder(Objects.requireNonNull(getActivity()))
                                    .title(R.string.dexopt_now)
                                    .content(R.string.this_may_take_a_while)
                                    .progress(true, 0)
                                    .cancelable(false)
                                    .showListener(dialog -> new Thread("dexopt") {
                                        @Override
                                        public void run() {
                                            RootUtil rootUtil = new RootUtil();
                                            if (!rootUtil.startShell()) {
                                                dialog.dismiss();
                                                NavUtil.showMessage(Objects.requireNonNull(getActivity()), getString(R.string.root_failed));
                                                return;
                                            }

                                            rootUtil.execute("cmd package bg-dexopt-job", new ArrayList<>());

                                            dialog.dismiss();
                                            XposedApp.runOnUiThread(() -> Toast.makeText(getActivity(), R.string.done, Toast.LENGTH_LONG).show());
                                        }
                                    }.start()).show();
                            dg.dismiss();
                        }).show();
                break;
            case R.id.speed_all:
                MD2Dialog.create(this.getActivity())
                        .title(R.string.areyousure)
                        .msg(R.string.take_while_cannot_resore)
                        .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
                        .simpleCancel()
                        .darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                        .onConfirmClick((view, dg) -> {
                            new MaterialDialog.Builder(Objects.requireNonNull(getActivity()))
                                    .title(R.string.speed_now)
                                    .content(R.string.this_may_take_a_while)
                                    .progress(true, 0)
                                    .cancelable(false)
                                    .showListener(dialog -> new Thread("dex2oat") {
                                        @Override
                                        public void run() {
                                            RootUtil rootUtil = new RootUtil();
                                            if (!rootUtil.startShell()) {
                                                dialog.dismiss();
                                                NavUtil.showMessage(Objects.requireNonNull(getActivity()), getString(R.string.root_failed));
                                                return;
                                            }

                                            rootUtil.execute("cmd package compile -m speed -a", new ArrayList<>());

                                            dialog.dismiss();
                                            XposedApp.runOnUiThread(() -> Toast.makeText(getActivity(), R.string.done, Toast.LENGTH_LONG).show());
                                        }
                                    }.start()).show();
                            dg.dismiss();
                        }).show();
                break;
            case R.id.reboot:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot, 0);
                } else {
                    reboot(null);
                }
                break;
            case R.id.soft_reboot:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.soft_reboot, 1);
                } else {
                    softReboot();
                }
                break;
            case R.id.reboot_recovery:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_recovery, 2);
                } else {
                    reboot("recovery");
                }
                break;
            case R.id.reboot_bootloader:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_bootloader, 3);
                } else {
                    reboot("bootloader");
                }
                break;
            case R.id.reboot_download:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_download, 4);
                } else {
                    reboot("download");
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public String getJSON_DATA() {
        return JSON_DATA;
    }

    public void setJSON_DATA(String json) {
        this.JSON_DATA = json;
        tabsAdapter.notifyDataSetChanged();
        mPager.setAdapter(tabsAdapter);
        mTabLayout.setupWithViewPager(mPager);
    }

    private boolean startShell() {
        if (mRootUtil.startShell())
            return true;

        showAlert(getString(R.string.root_failed));
        return false;
    }

    private void areYouSure(int contentTextId, int mode) {
        new MD2Dialog(getActivity())
                .title(R.string.areyousure)
                .msg(contentTextId)
                .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
                .darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                .onConfirmClick(android.R.string.yes, new MD2Dialog.OptionsButtonCallBack() {
                    @Override
                    public void onClick(View view, MD2Dialog dialog) {
                        switch (mode) {
                            case 0:
                                reboot(null);
                                break;
                            case 1:
                                softReboot();
                                break;
                            case 2:
                                reboot("recovery");
                                break;
                            case 3:
                                reboot("bootloader");
                                break;
                            case 4:
                                reboot("download");
                                break;
                        }
                        dialog.dismiss();
                    }
                }).simpleCancel().show();


//        new MaterialDialog.Builder(getActivity()).title(R.string.areyousure)
//                .content(contentTextId)
//                .iconAttr(android.R.attr.alertDialogIcon)
//                .positiveText(android.R.string.yes)
//                .negativeText(android.R.string.no).callback(yesHandler).show();
    }

    private void showAlert(final String result) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showAlert(result);
                }
            });
            return;
        }

        MD2Dialog dialog = new MD2Dialog(getActivity()).msg(result).simpleConfirm(android.R.string.ok);
        dialog.darkMode(ThemeUtil.getSelectTheme().equals("dark"));
        dialog.show();
    }

    private void softReboot() {
        if (!startShell())
            return;

        List<String> messages = new LinkedList<>();
        if (mRootUtil.execute("setprop ctl.restart surfaceflinger; setprop ctl.restart zygote", messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.reboot_failed));
            showAlert(TextUtils.join("\n", messages).trim());
        }
    }

    private void reboot(String mode) {
        if (!startShell())
            return;

        List<String> messages = new LinkedList<>();

        String command = "reboot";
        if (mode != null) {
            command += " " + mode;
            if (mode.equals("recovery"))
                // create a flag used by some kernels to boot into recovery
                mRootUtil.executeWithBusybox("touch /cache/recovery/boot", messages);
        }

        if (mRootUtil.executeWithBusybox(command, messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.reboot_failed));
            showAlert(TextUtils.join("\n", messages).trim());
        }
        AssetUtil.removeBusybox();
    }

    @SuppressLint("StaticFieldLeak")
    public class UpdatePush extends AsyncTask<Void, Void, Boolean> {

        private String newApkVersion = null;
        private String newApkLink = null;
        private String newApkChangelog = null;

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String unOfficialLink = JSONUtils.getFileContent(PingActivity.getMaterialApkLink());
                final JSONUtils.UNOFFICIAL_UPDATE updateJson = new Gson().fromJson(unOfficialLink, JSONUtils.UNOFFICIAL_UPDATE.class);
                newApkVersion = updateJson.version;
                newApkLink = updateJson.link;
                newApkChangelog = updateJson.changes;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            try {
                if (newApkVersion == null) return;
                SharedPreferences prefs;
                try {
                    prefs = getContext().getSharedPreferences(getContext().getPackageName() + "_preferences", MODE_PRIVATE);
                    prefs.edit().putString("changelog_" + newApkVersion, newApkChangelog).apply();
                } catch (NullPointerException ignored) {
                }
                String a = BuildConfig.VERSION_NAME;
                String b = newApkVersion;
                if (!a.equals(b)) {
                    StatusInstallerFragment.setUpdate(newApkLink, newApkChangelog, newApkVersion);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class JSONParser extends AsyncTask<Void, Void, Boolean> {

        private boolean noZips = false;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String originalJson;
                if (!TextUtils.isEmpty(JSON_DATA)) {
                    originalJson = JSON_DATA; //直接加载之前加载过的数据
//                    Log.e("JSONParser", "Through The Previous State");
                } else {
                    originalJson = JSONUtils.getFileContent(PingActivity.getFrameWorkLink()); //通过网络更新数据
                    JSON_DATA = originalJson;
//                    Log.e("JSONParser", originalJson);
                }
                String newJson = JSONUtils.listZip();
                String jsonString = originalJson.replace("%XPOSED_ZIP%", newJson);
                final JSONUtils.XposedJson xposedJson = new Gson().fromJson(jsonString, JSONUtils.XposedJson.class);
                List<XposedTab> tabs = Stream.of(xposedJson.tabs)
                        .filter(value -> value.sdks.contains(Build.VERSION.SDK_INT)).toList();

                noZips = tabs.isEmpty();

                for (XposedTab tab : tabs) {
                    tabsAdapter.addFragment(tab.name, BaseAdvancedInstaller.newInstance(tab));
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(XposedApp.TAG, "AdvancedInstallerFragment -> " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            tabsAdapter.notifyDataSetChanged();
            try {
                if (!result) {
                    StatusInstallerFragment.setError(true/* connection failed */, true /* so no sdks available*/);
                } else {
                    StatusInstallerFragment.setError(false /*connection ok*/, noZips /*if counter is 0 there aren't sdks*/);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public class TabsAdapter extends FragmentPagerAdapter {

        private final ArrayList<String> titles = new ArrayList<>();
        private final ArrayList<Fragment> listFragment = new ArrayList<>();

        TabsAdapter(FragmentManager mgr) {
            super(mgr);
            addFragment(getString(R.string.status), new StatusInstallerFragment());
        }

        void addFragment(String title, Fragment fragment) {
            titles.add(title);
            listFragment.add(fragment);
        }

        @Override
        public int getCount() {
            return listFragment.size();
        }

        @Override
        public Fragment getItem(int position) {
            return listFragment.get(position);
        }

        @Override
        public String getPageTitle(int position) {
            return titles.get(position);
        }
    }

}
