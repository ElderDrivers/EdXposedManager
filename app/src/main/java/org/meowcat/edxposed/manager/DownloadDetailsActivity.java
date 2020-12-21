package org.meowcat.edxposed.manager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.meowcat.edxposed.manager.repo.Module;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.ModuleUtil.InstalledModule;
import org.meowcat.edxposed.manager.util.ModuleUtil.ModuleListener;
import org.meowcat.edxposed.manager.util.RepoLoader;
import org.meowcat.edxposed.manager.util.RepoLoader.RepoListener;
import org.meowcat.edxposed.manager.util.ThemeUtil;

import java.util.List;
import java.util.Objects;

import static androidx.fragment.app.FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;
import static org.meowcat.edxposed.manager.MeowCatApplication.TAG;

public class DownloadDetailsActivity extends BaseActivity implements RepoListener, ModuleListener {

    public static final int DOWNLOAD_DESCRIPTION = 0;
    public static final int DOWNLOAD_VERSIONS = 1;
    public static final int DOWNLOAD_SETTINGS = 2;
    private static RepoLoader sRepoLoader = RepoLoader.getInstance();
    private static ModuleUtil sModuleUtil = ModuleUtil.getInstance();
    private ViewPager mPager;
    private String mPackageName;
    private Module mModule;
    private InstalledModule mInstalledModule;
    private MenuItem mItemBookmark;
    private boolean changeIcon = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtil.setTheme(this);

        mPackageName = getModulePackageName();
        try {
            mModule = sRepoLoader.getModule(mPackageName);
        } catch (Exception e) {
            Log.i(TAG, "DownloadDetailsActivity -> " + e.getMessage());

            mModule = null;
        }

        mInstalledModule = ModuleUtil.getInstance().getModule(mPackageName);

        super.onCreate(savedInstanceState);
        sRepoLoader.addListener(this, false);
        sModuleUtil.addListener(this);

        if (mModule != null) {
            setContentView(R.layout.activity_download_details);

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            toolbar.setNavigationOnClickListener(view -> finish());

            ActionBar ab = getSupportActionBar();

            if (ab != null) {
                ab.setTitle(R.string.nav_item_download);
                ab.setDisplayHomeAsUpEnabled(true);
            }

            setFloating(toolbar, 0);

            if (changeIcon) {
                toolbar.setNavigationIcon(R.drawable.ic_close);
            }

            setupTabs();

            boolean directDownload = getIntent().getBooleanExtra("direct_download", false);
            // Updates available => start on the versions page
            if (mInstalledModule != null && mInstalledModule.isUpdate(sRepoLoader.getLatestVersion(mModule)) || directDownload)
                mPager.setCurrentItem(DOWNLOAD_VERSIONS);

            findViewById(R.id.fake_elevation).setVisibility(View.GONE);

        } else {
            setContentView(R.layout.activity_download_details_not_found);

            TextView txtMessage = findViewById(android.R.id.message);
            txtMessage.setText(getResources().getString(R.string.download_details_not_found, mPackageName));

            findViewById(R.id.reload).setOnClickListener(v -> {
                v.setEnabled(false);
                sRepoLoader.triggerReload(true);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setupTabs() {
        mPager = findViewById(R.id.download_pager);
        mPager.setAdapter(new SwipeFragmentPagerAdapter(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT));
        TabLayout mTabLayout = findViewById(R.id.sliding_tabs);
        mTabLayout.setupWithViewPager(mPager);
        mTabLayout.setBackgroundColor(XposedApp.getColor(this));
    }

    private String getModulePackageName() {
        Uri uri = getIntent().getData();
        if (uri == null)
            return null;

        String scheme = uri.getScheme();
        if (TextUtils.isEmpty(scheme)) {
            return null;
        } else switch (Objects.requireNonNull(scheme)) {
            case "xposed":
                changeIcon = true;
            case "package":
                return uri.getSchemeSpecificPart().replace("//", "");
            case "http":
                List<String> segments = uri.getPathSegments();
                if (segments.size() > 1)
                    return segments.get(1);
                break;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sRepoLoader.removeListener(this);
        sModuleUtil.removeListener(this);
    }

    public Module getModule() {
        return mModule;
    }

    public InstalledModule getInstalledModule() {
        return mInstalledModule;
    }

    public void gotoPage(int page) {
        mPager.setCurrentItem(page);
    }

    private void reload() {
        runOnUiThread(this::recreate);
    }

    @Override
    public void onRepoReloaded(RepoLoader loader) {
        reload();
    }

    @Override
    public void onInstalledModulesReloaded(ModuleUtil moduleUtil) {
        reload();
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, InstalledModule module) {
        if (packageName.equals(mPackageName))
            reload();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_download_details, menu);

        boolean updateIgnorePreference = XposedApp.getPreferences().getBoolean("ignore_updates", false);
        if (updateIgnorePreference) {
            SharedPreferences prefs = getSharedPreferences("update_ignored", Context.MODE_PRIVATE);

            boolean ignored = prefs.getBoolean(mModule.packageName, false);
            menu.findItem(R.id.ignoreUpdate).setChecked(ignored);
        } else {
            menu.removeItem(R.id.ignoreUpdate);
        }

        mItemBookmark = menu.findItem(R.id.menu_bookmark);
        setupBookmark(false);
        return true;
    }

    private void setupBookmark(boolean clicked) {
        SharedPreferences myPref = getSharedPreferences("bookmarks", MODE_PRIVATE);

        boolean saved = myPref.getBoolean(mModule.packageName, false);
        boolean newValue;

        if (clicked) {
            newValue = !saved;
            myPref.edit().putBoolean(mModule.packageName, newValue).apply();

            int msg = newValue ? R.string.bookmark_added : R.string.bookmark_removed;

            Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
        }

        saved = myPref.getBoolean(mModule.packageName, false);

        if (saved) {
            mItemBookmark.setTitle(R.string.remove_bookmark);
            mItemBookmark.setIcon(R.drawable.ic_bookmark);
        } else {
            mItemBookmark.setTitle(R.string.add_bookmark);
            mItemBookmark.setIcon(R.drawable.ic_bookmark_outline);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_bookmark:
                setupBookmark(true);
                break;
            case R.id.menu_refresh:
                RepoLoader.getInstance().triggerReload(true);
                return true;
            case R.id.menu_share:
                String text = mModule.name + " - ";

                if (isPackageInstalled(mPackageName, this)) {
                    String s = getPackageManager().getInstallerPackageName(mPackageName);
                    boolean playStore;

                    try {
                        playStore = s.equals(ModulesFragment.PLAY_STORE_PACKAGE);
                    } catch (NullPointerException e) {
                        playStore = false;
                    }

                    if (playStore) {
                        text += String.format(ModulesFragment.PLAY_STORE_LINK, mPackageName);
                    } else {
                        text += String.format(ModulesFragment.XPOSED_REPO_LINK, mPackageName);
                    }
                } else {
                    text += String.format(ModulesFragment.XPOSED_REPO_LINK,
                            mPackageName);
                }

                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share)));
                return true;
            case R.id.ignoreUpdate:
                SharedPreferences prefs = getSharedPreferences("update_ignored", Context.MODE_PRIVATE);

                boolean ignored = prefs.getBoolean(mModule.packageName, false);
                prefs.edit().putBoolean(mModule.packageName, !ignored).apply();
                item.setChecked(!ignored);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    class SwipeFragmentPagerAdapter extends FragmentPagerAdapter {
        final int PAGE_COUNT = 3;
        private String[] tabTitles = new String[]{getString(R.string.download_details_page_description), getString(R.string.download_details_page_versions), getString(R.string.download_details_page_settings),};

        SwipeFragmentPagerAdapter(FragmentManager fm, int behaver) {
            super(fm, behaver);
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                default:
                case DOWNLOAD_DESCRIPTION:
                    return new DownloadDetailsFragment();
                case DOWNLOAD_VERSIONS:
                    return new DownloadDetailsVersionsFragment();
                case DOWNLOAD_SETTINGS:
                    return new DownloadDetailsSettingsFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // Generate title based on item position
            return tabTitles[position];
        }
    }
}