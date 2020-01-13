package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

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

import org.meowcat.edxposed.manager.util.json.JSONUtils;
import org.meowcat.edxposed.manager.util.json.XposedTab;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class AdvancedInstallerFragment extends BaseFragment {

    //private static ViewPager mPager;
    private TabLayout mTabLayout;
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
