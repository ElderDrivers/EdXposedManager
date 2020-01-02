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
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import org.meowcat.edxposed.manager.BuildConfig;
import org.meowcat.edxposed.manager.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.AssetUtil;
import de.robv.android.xposed.installer.util.NavUtil;
import de.robv.android.xposed.installer.util.RootUtil;
import de.robv.android.xposed.installer.util.json.JSONUtils;
import de.robv.android.xposed.installer.util.json.XposedTab;

import static android.content.Context.MODE_PRIVATE;

public class AdvancedInstallerFragment extends Fragment {

    //private static ViewPager mPager;
    private TabLayout mTabLayout;
    private RootUtil mRootUtil = new RootUtil();
    private TabsAdapter tabsAdapter;

    //public static void gotoPage(int page) {mPager.setCurrentItem(page);}

    @SuppressWarnings("deprecation")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_advanced_installer, container, false);
        ViewPager mPager = view.findViewById(R.id.pager);
        mTabLayout = view.findViewById(R.id.tab_layout);

        tabsAdapter = new TabsAdapter(getChildFragmentManager());
        tabsAdapter.notifyDataSetChanged();
        mPager.setAdapter(tabsAdapter);
        mTabLayout.setupWithViewPager(mPager);

        setHasOptionsMenu(true);
        new JSONParser().execute();

        if (!XposedApp.getPreferences().getBoolean("hide_install_warning", false)) {
            @SuppressLint("InflateParams") final View dontShowAgainView = inflater.inflate(R.layout.dialog_install_warning, null);

            new MaterialDialog.Builder(Objects.requireNonNull(getActivity()))
                    .title(R.string.install_warning_title)
                    .customView(dontShowAgainView, false)
                    .positiveText(R.string.ok)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            CheckBox checkBox = dontShowAgainView.findViewById(android.R.id.checkbox);
                            if (checkBox.isChecked())
                                XposedApp.getPreferences().edit().putBoolean("hide_install_warning", true).apply();
                        }
                    }).cancelable(false).show();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mTabLayout.setBackgroundColor(XposedApp.getColor(Objects.requireNonNull(getContext())));

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_installer, menu);
        if (Build.VERSION.SDK_INT < 26) {
            menu.findItem(R.id.dexopt_all).setVisible(false);
            menu.findItem(R.id.speed_all).setVisible(false);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dexopt_all:
                areYouSure(R.string.take_while_cannot_resore, new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog mDialog) {
                        super.onPositive(mDialog);
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
                    }
                });
                break;
            case R.id.speed_all:
                areYouSure(R.string.take_while_cannot_resore, new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog mDialog) {
                        super.onPositive(mDialog);
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
                    }
                });
                break;
            case R.id.reboot:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot(null);
                        }
                    });
                } else {
                    reboot(null);
                }
                break;
            case R.id.soft_reboot:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.soft_reboot, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            softReboot();
                        }
                    });
                } else {
                    softReboot();
                }
                break;
            case R.id.reboot_recovery:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_recovery, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot("recovery");
                        }
                    });
                } else {
                    reboot("recovery");
                }
                break;
            case R.id.reboot_bootloader:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_bootloader, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot("bootloader");
                        }
                    });
                } else {
                    reboot("bootloader");
                }
                break;
            case R.id.reboot_download:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_download, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot("download");
                        }
                    });
                } else {
                    reboot("download");
                }
                break;
            case R.id.reboot_edl:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_download, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot("edl");
                        }
                    });
                } else {
                    reboot("edl");
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean startShell() {
        if (mRootUtil.startShell())
            return false;

        showAlert(getString(R.string.root_failed));
        return true;
    }

    private void areYouSure(int contentTextId, MaterialDialog.ButtonCallback yesHandler) {
        new MaterialDialog.Builder(Objects.requireNonNull(getActivity())).title(R.string.areyousure)
                .content(contentTextId)
                .iconAttr(android.R.attr.alertDialogIcon)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no).callback(yesHandler).show();
    }

    private void showAlert(final String result) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> showAlert(result));
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(Objects.requireNonNull(getActivity())).content(result).positiveText(R.string.ok).build();
        dialog.show();

        TextView txtMessage = (TextView) dialog
                .findViewById(android.R.id.message);
        try {
            txtMessage.setTextSize(14);
        } catch (NullPointerException ignored) {
        }
    }

    private void softReboot() {
        if (startShell())
            return;

        List<String> messages = new LinkedList<>();
        if (mRootUtil.execute("setprop ctl.restart surfaceflinger; setprop ctl.restart zygote", messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.reboot_failed));
            showAlert(TextUtils.join("\n", messages).trim());
        }
    }

    private void reboot(String mode) {
        if (startShell())
            return;

        List<String> messages = new LinkedList<>();

        String command = "/system/bin/svc power reboot";
        if (mode != null) {
            command += " " + mode;
            if (mode.equals("recovery"))
                // create a flag used by some kernels to boot into recovery
                mRootUtil.executeWithBusybox("touch /cache/recovery/boot", messages);
        }

        if (mRootUtil.execute(command, messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.reboot_failed));
            showAlert(TextUtils.join("\n", messages).trim());
        }
        AssetUtil.removeBusybox();
    }

    @SuppressLint("StaticFieldLeak")
    private class JSONParser extends AsyncTask<Void, Void, Boolean> {

        private String newApkVersion = null;
        private String newApkLink = null;
        private String newApkChangelog = null;
        private boolean noZips = false;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String originalJson = JSONUtils.getFileContent(JSONUtils.JSON_LINK);

                final JSONUtils.XposedJson xposedJson = new Gson().fromJson(originalJson, JSONUtils.XposedJson.class);

                List<XposedTab> tabs = Stream.of(xposedJson.tabs)
                        .filter(value -> value.sdks.contains(Build.VERSION.SDK_INT)).toList();

                noZips = tabs.isEmpty();

                for (XposedTab tab : tabs) {
                    tabsAdapter.addFragment(tab.name, BaseAdvancedInstaller.newInstance(tab));
                }

                newApkVersion = xposedJson.apk.version;
                newApkLink = xposedJson.apk.link;
                newApkChangelog = xposedJson.apk.changelog;

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

            try {
                tabsAdapter.notifyDataSetChanged();

                if (!result) {
                    StatusInstallerFragment.setError(true/* connection failed */, true /* so no sdks available*/);
                } else {
                    StatusInstallerFragment.setError(false /*connection ok*/, noZips /*if counter is 0 there aren't sdks*/);
                }

                if (newApkVersion == null) return;

                SharedPreferences prefs;
                try {
                    prefs = Objects.requireNonNull(getContext()).getSharedPreferences(Objects.requireNonNull(getContext()).getPackageName() + "_preferences", MODE_PRIVATE);

                    prefs.edit().putString("changelog", newApkChangelog).apply();
                } catch (NullPointerException ignored) {
                }

                Integer a = BuildConfig.VERSION_CODE;
                Integer b = Integer.valueOf(newApkVersion);

                if (a.compareTo(b) < 0) {
                    StatusInstallerFragment.setUpdate(newApkLink, newApkChangelog, getContext());
                }

            } catch (Exception ignored) {
            }

        }
    }

    private class TabsAdapter extends FragmentPagerAdapter {

        private final ArrayList<String> titles = new ArrayList<>();
        private final ArrayList<Fragment> listFragment = new ArrayList<>();

        @SuppressWarnings("deprecation")
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

        @NonNull
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
