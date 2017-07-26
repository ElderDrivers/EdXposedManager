package de.robv.android.xposed.installer.installation;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.google.gson.Gson;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.robv.android.xposed.installer.BuildConfig;
import de.robv.android.xposed.installer.R;
import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.AssetUtil;
import de.robv.android.xposed.installer.util.RootUtil;
import de.robv.android.xposed.installer.util.json.JSONUtils;
import de.robv.android.xposed.installer.util.json.XposedTab;

import static android.content.Context.MODE_PRIVATE;

public class AdvancedInstallerFragment extends Fragment {

    private static ViewPager mPager;
    private TabLayout mTabLayout;
    private RootUtil mRootUtil = new RootUtil();
    private TabsAdapter tabsAdapter;

    public static void gotoPage(int page) {mPager.setCurrentItem(page);}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_advanced_installer, container, false);
        mPager = view.findViewById(R.id.pager);
        mTabLayout = view.findViewById(R.id.tab_layout);

        tabsAdapter = new TabsAdapter(getChildFragmentManager());
        tabsAdapter.notifyDataSetChanged();
        mPager.setAdapter(tabsAdapter);
        mTabLayout.setupWithViewPager(mPager);

        setHasOptionsMenu(true);
        new JSONParser().execute();

        if (!XposedApp.getPreferences().getBoolean("hide_install_warning", false)) {
            final View dontShowAgainView = inflater.inflate(R.layout.dialog_install_warning, null);

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.install_warning_title)
                    .customView(dontShowAgainView, false)
                    .positiveText(android.R.string.ok)
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

        mTabLayout.setBackgroundColor(XposedApp.getColor(getContext()));

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_installer, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean startShell() {
        if (mRootUtil.startShell())
            return true;

        showAlert(getString(R.string.root_failed));
        return false;
    }

    private void areYouSure(int contentTextId, MaterialDialog.ButtonCallback yesHandler) {
        new MaterialDialog.Builder(getActivity()).title(R.string.areyousure)
                .content(contentTextId)
                .iconAttr(android.R.attr.alertDialogIcon)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no).callback(yesHandler).show();
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

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity()).content(result).positiveText(android.R.string.ok).build();
        dialog.show();

        TextView txtMessage = (TextView) dialog
                .findViewById(android.R.id.message);
        try {
            txtMessage.setTextSize(14);
        } catch (NullPointerException ignored) {
        }
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

    private class JSONParser extends AsyncTask<Void, Void, Boolean> {

        private String newApkVersion = null;
        private String newApkLink = null;
        private String newApkChangelog = null;
        private boolean noZips = false;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String originalJson = JSONUtils.getFileContent(JSONUtils.JSON_LINK);
                String newJson = JSONUtils.listZip();

                String jsonString = originalJson.replace("%XPOSED_ZIP%", newJson);

                final JSONUtils.XposedJson xposedJson = new Gson().fromJson(jsonString, JSONUtils.XposedJson.class);

                List<XposedTab> tabs = Stream.of(xposedJson.tabs)
                        .filter(new Predicate<XposedTab>() {
                            @Override
                            public boolean test(XposedTab value) {
                                return value.sdks.contains(Build.VERSION.SDK_INT);
                            }
                        }).toList();

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
                    prefs = getContext().getSharedPreferences(getContext().getPackageName() + "_preferences", MODE_PRIVATE);

                    prefs.edit().putString("changelog_" + newApkVersion, newApkChangelog).apply();
                } catch (NullPointerException ignored) {
                }

                BigInteger a = new BigInteger(BuildConfig.APP_VERSION);
                BigInteger b = new BigInteger(newApkVersion);

                if (a.compareTo(b) == -1) {
                    StatusInstallerFragment.setUpdate(newApkLink, newApkChangelog);
                }

            } catch (Exception ignored) {
            }

        }
    }

    private class TabsAdapter extends FragmentPagerAdapter {

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
        public int getCount() { return listFragment.size(); }

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
