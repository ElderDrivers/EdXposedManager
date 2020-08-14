package org.meowcat.edxposed.manager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.adapter.AppHelper;
import org.meowcat.edxposed.manager.adapter.ApplicationListAdapter;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.ModuleUtil.InstalledModule;
import org.meowcat.edxposed.manager.util.ModuleUtil.ModuleListener;
import org.meowcat.edxposed.manager.util.RepoLoader;
import org.meowcat.edxposed.manager.util.RepoLoader.RepoListener;
import org.meowcat.edxposed.manager.util.ThemeUtil;

import static org.meowcat.edxposed.manager.SettingsActivity.getDarkenFactor;
import static org.meowcat.edxposed.manager.XposedApp.darkenColor;

public class WelcomeActivity extends XposedBaseActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        ModuleListener, RepoListener {

    private static final String SELECTED_ITEM_ID = "SELECTED_ITEM_ID";
    private final Handler mDrawerHandler = new Handler();
    private RepoLoader mRepoLoader;
    private DrawerLayout mDrawerLayout;
    private int mPrevSelectedId;
    private NavigationView mNavigationView;
    private int mSelectedId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_welcome);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mNavigationView = findViewById(R.id.navigation_view);
        assert mNavigationView != null;
        mNavigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, mToolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                super.onDrawerSlide(drawerView, 0); // this disables the arrow @ completed state
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0); // this disables the animation
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSelectedId = mNavigationView.getMenu().getItem(prefs.getInt("default_view", 0)).getItemId();
        mSelectedId = savedInstanceState == null ? mSelectedId : savedInstanceState.getInt(SELECTED_ITEM_ID);
        mPrevSelectedId = mSelectedId;
        mNavigationView.getMenu().findItem(mSelectedId).setChecked(true);

        if (savedInstanceState == null) {
            mDrawerHandler.removeCallbacksAndMessages(null);
            mDrawerHandler.postDelayed(() -> navigate(mSelectedId), 250);

            boolean openDrawer = prefs.getBoolean("open_drawer", false);

            if (openDrawer) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            } else {
                mDrawerLayout.closeDrawers();
            }
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int value = extras.getInt("fragment", prefs.getInt("default_view", 0));
            switchFragment(value);
        }

        mRepoLoader = RepoLoader.getInstance();
        ModuleUtil.getInstance().addListener(this);
        mRepoLoader.addListener(this, false);

        notifyDataSetChanged();

        new Thread(() -> new ApplicationListAdapter(getApplicationContext(), AppHelper.isWhiteListMode()).generateCheckedList());

    }

    @Override
    protected void onResume() {
        super.onResume();
        mDrawerLayout.setStatusBarBackgroundColor(darkenColor(XposedApp.getColor(this), getDarkenFactor()));
        XposedApp.getPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        XposedApp.getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void switchFragment(int itemId) {
        mSelectedId = mNavigationView.getMenu().getItem(itemId).getItemId();
        mNavigationView.getMenu().findItem(mSelectedId).setChecked(true);
        mDrawerHandler.removeCallbacksAndMessages(null);
        mDrawerHandler.postDelayed(() -> navigate(mSelectedId), 250);
        mDrawerLayout.closeDrawers();
    }

    private void navigate(final int itemId) {
        final View elevation = findViewById(R.id.elevation);
        Fragment navFragment = null;
        switch (itemId) {
            case R.id.drawer_item_1:
                mPrevSelectedId = itemId;
                setTitle(R.string.app_name);
                navFragment = new AdvancedInstallerFragment();
                break;
            case R.id.drawer_item_2:
                mPrevSelectedId = itemId;
                setTitle(R.string.nav_item_modules);
                navFragment = new ModulesFragment();
                break;
            case R.id.drawer_item_3:
                mPrevSelectedId = itemId;
                setTitle(R.string.nav_item_download);
                navFragment = new DownloadFragment();
                break;
            case R.id.drawer_item_4:
                mPrevSelectedId = itemId;
                setTitle(R.string.nav_item_logs);
                navFragment = new LogsFragment();
                break;
            case R.id.nav_black_list:
                mPrevSelectedId = itemId;
                setTitle(R.string.nav_title_black_list);
                navFragment = new ApplicationFragment();
                break;
            case R.id.nav_compat_list:
                mPrevSelectedId = itemId;
                setTitle(R.string.title_compat_list);
                navFragment = new CompatListFragment();
                break;
            case R.id.drawer_item_5:
                startActivity(new Intent(this, SettingsActivity.class));
                mNavigationView.getMenu().findItem(mPrevSelectedId).setChecked(true);
                return;
            case R.id.drawer_item_6:
                startActivity(new Intent(this, AboutActivity.class));
                mNavigationView.getMenu().findItem(mPrevSelectedId).setChecked(true);
                return;
        }

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4));

        if (navFragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
            try {
                transaction.replace(R.id.content_frame, navFragment).commit();

                if (elevation != null) {
                    params.topMargin = navFragment instanceof AdvancedInstallerFragment ? dp(48) : 0;

                    Animation a = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            elevation.setLayoutParams(params);
                        }
                    };
                    a.setDuration(150);
                    elevation.startAnimation(a);
                }
            } catch (IllegalStateException ignored) {
            }
        }
    }

    public int dp(float value) {
        float density = getApplicationContext().getResources().getDisplayMetrics().density;

        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        menuItem.setChecked(true);
        mSelectedId = menuItem.getItemId();
        mDrawerHandler.removeCallbacksAndMessages(null);
        mDrawerHandler.postDelayed(() -> navigate(mSelectedId), 250);
        mDrawerLayout.closeDrawers();
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_ITEM_ID, mSelectedId);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void notifyDataSetChanged() {
        View parentLayout = findViewById(R.id.content_frame);
        String frameworkUpdateVersion = mRepoLoader.getFrameworkUpdateVersion();
        boolean moduleUpdateAvailable = mRepoLoader.hasModuleUpdates();

        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (currentFragment instanceof DownloadDetailsFragment) {
            if (frameworkUpdateVersion != null) {
                Snackbar.make(parentLayout, R.string.welcome_framework_update_available + " " + frameworkUpdateVersion, Snackbar.LENGTH_LONG).show();
            }
        }

        boolean snackBar = getSharedPreferences(
                getPackageName() + "_preferences", MODE_PRIVATE).getBoolean("snack_bar", true);

        if (moduleUpdateAvailable && snackBar) {
            Snackbar.make(parentLayout, R.string.modules_updates_available, Snackbar.LENGTH_LONG).setAction(getString(R.string.view), view -> switchFragment(4)).show();
        }
    }

    @Override
    public void onInstalledModulesReloaded(ModuleUtil moduleUtil) {
        notifyDataSetChanged();
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, InstalledModule module) {
        notifyDataSetChanged();
    }

    @Override
    public void onRepoReloaded(RepoLoader loader) {
        notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ModuleUtil.getInstance().removeListener(this);
        mRepoLoader.removeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    }
}