package de.robv.android.xposed.installer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.internal.NavigationMenuPresenter;
import com.google.android.material.internal.NavigationMenuView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.fragment.BlackListFragment;
import com.solohsu.android.edxp.manager.fragment.CompatListFragment;
import com.solohsu.android.edxp.manager.fragment.SettingFragment;

import java.lang.reflect.Field;
import java.util.Objects;

import de.robv.android.xposed.installer.activity.AboutActivity;
import de.robv.android.xposed.installer.activity.PersonalizeFragment;
import de.robv.android.xposed.installer.activity.SELinuxActivity;
import de.robv.android.xposed.installer.activity.SupportActivity;
import de.robv.android.xposed.installer.activity.XposedSettingsActivity;
import de.robv.android.xposed.installer.installation.AdvancedInstallerFragment;
import de.robv.android.xposed.installer.util.ModuleUtil;
import de.robv.android.xposed.installer.util.ModuleUtil.InstalledModule;
import de.robv.android.xposed.installer.util.ModuleUtil.ModuleListener;
import de.robv.android.xposed.installer.util.RepoLoader;
import de.robv.android.xposed.installer.util.RepoLoader.RepoListener;
import de.robv.android.xposed.installer.widget.NavigationViewCompact;

public class WelcomeActivity extends XposedBaseActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        ModuleListener, RepoListener {

    private static final String SELECTED_ITEM_ID = "SELECTED_ITEM_ID";
    private final Handler mSwitchHandler = new Handler();
    private RepoLoader mRepoLoader;
    private int mPrevSelectedId;
    private BottomNavigationView mNavigationView;
    private DrawerLayout drawer;
    private NavigationViewCompact navView;
    private int mSelectedId;
    private Toolbar mToolbar;
    private boolean isFirstTimeLoad = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isDrawerMode = XposedApp.getPreferences().getBoolean("drawer_layout_home", false);
        if (isDrawerMode) {
            setContentView(R.layout.activity_welcome_drawer);
            setUpDrawerMode(savedInstanceState);
        } else {
            setContentView(R.layout.activity_welcome);
            setUpBottomNavigationMode(savedInstanceState);
        }
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        enableMenuIcon();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBlackListEntry();
        updateVerboseLogsEntry();
        XposedApp.getPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        XposedApp.getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    private void setUpBottomNavigationMode(Bundle savedInstanceState) {
        mNavigationView = findViewById(R.id.bottom_navigation);
        assert mNavigationView != null;
        mNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            menuItem.setChecked(true);
            mSelectedId = menuItem.getItemId();
            mSwitchHandler.removeCallbacksAndMessages(null);
            mSwitchHandler.postDelayed(() -> navigate(mSelectedId), 250);
            return true;
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSelectedId = mNavigationView.getMenu().getItem(prefs.getInt("default_view", 0)).getItemId();
        mSelectedId = savedInstanceState == null ? mSelectedId : savedInstanceState.getInt(SELECTED_ITEM_ID);
        mPrevSelectedId = mSelectedId;
        isFirstTimeLoad = true;
        mNavigationView.getMenu().findItem(mSelectedId).setChecked(true);

        if (savedInstanceState == null) {
            mSwitchHandler.removeCallbacksAndMessages(null);
            mSwitchHandler.postDelayed(() -> navigate(mSelectedId), 250);
        }

        int value = XposedApp.getPreferences().getInt("default_view", 0);
        switchFragment(value);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            value = extras.getInt("fragment", XposedApp.getPreferences().getInt("default_view", 0));
            switchFragment(value);
        }

        mRepoLoader = RepoLoader.getInstance();
        ModuleUtil.getInstance().addListener(this);
        mRepoLoader.addListener(this, false);

        notifyDataSetChanged();
    }

    private void enableMenuIcon() {
        if (drawer != null) {
            ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this,
                    drawer, mToolbar, R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close) {
                @Override
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                }

                @Override
                public void onDrawerSlide(View drawerView, float slideOffset) {
                    super.onDrawerSlide(drawerView, slideOffset); // this disables the animation
                }
            };

            mDrawerToggle.setDrawerIndicatorEnabled(true);
            mDrawerToggle.setHomeAsUpIndicator(android.R.drawable.menu_frame);
            mDrawerToggle.syncState();
            drawer.addDrawerListener(mDrawerToggle);
        }
    }

    private void setUpDrawerMode(Bundle savedInstanceState) {
        drawer = findViewById(R.id.drawer);
        navView = findViewById(R.id.navigation_View);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSelectedId = navView.getMenu().getItem(prefs.getInt("default_view", 0)).getItemId();
        mSelectedId = savedInstanceState == null ? mSelectedId : savedInstanceState.getInt(SELECTED_ITEM_ID);
        mPrevSelectedId = mSelectedId;
        isFirstTimeLoad = true;
        navView.getMenu().findItem(mSelectedId).setChecked(true);

        if (savedInstanceState == null) {
            mSwitchHandler.removeCallbacksAndMessages(null);
            mSwitchHandler.postDelayed(() -> navigateDrawer(mSelectedId), 250);

            boolean openDrawer = prefs.getBoolean("open_drawer", false);

            if (openDrawer)
                drawer.openDrawer(GravityCompat.START);
            else
                drawer.closeDrawers();
        }

        navView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_about:
                case R.id.nav_support_me:
                case R.id.nav_settings:
                case R.id.nav_back_to_bottom_mode:
                case R.id.nav_selinux:
                    break;
                default:
                    item.setChecked(true);
                    break;
            }
            mSelectedId = item.getItemId();
            mSwitchHandler.removeCallbacksAndMessages(null);
            mSwitchHandler.postDelayed(() -> navigateDrawer(mSelectedId), 250);
            drawer.closeDrawers();
            return true;
        });

        int value = XposedApp.getPreferences().getInt("default_view", 0);
        switchFragment(value);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            value = extras.getInt("fragment", XposedApp.getPreferences().getInt("default_view", 0));
            switchFragment(value);
        }

        mRepoLoader = RepoLoader.getInstance();
        ModuleUtil.getInstance().addListener(this);
        mRepoLoader.addListener(this, false);

        notifyDataSetChanged();
    }

    public void switchFragment(int itemId) {
        if (drawer != null) {
            mSelectedId = navView.getMenu().getItem(itemId).getItemId();
            navView.getMenu().findItem(mSelectedId).setChecked(true);
            mSwitchHandler.removeCallbacksAndMessages(null);
            mSwitchHandler.postDelayed(() -> navigateDrawer(mSelectedId), 250);
            drawer.closeDrawers();
        } else {
            mSelectedId = mNavigationView.getMenu().getItem(itemId).getItemId();
            mNavigationView.getMenu().findItem(mSelectedId).setChecked(true);
            mSwitchHandler.removeCallbacksAndMessages(null);
            mSwitchHandler.postDelayed(() -> navigate(mSelectedId), 250);
        }
    }

    private String jsonData;
    private AdvancedInstallerFragment advancedInstallerFragment;

    public void navigateDrawer(final int itemId) {
        Fragment navFragment = null;
        switch (itemId) {
            case R.id.nav_item_framework:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.app_name_full);
                    if (XposedApp.getPreferences().getBoolean("save_loaded_state", true) && advancedInstallerFragment != null) {
                        advancedInstallerFragment.setJSON_DATA(jsonData == null ? "" : jsonData);
                        navFragment = advancedInstallerFragment;
                    } else {
                        navFragment = new AdvancedInstallerFragment();
                        advancedInstallerFragment = (AdvancedInstallerFragment) navFragment;
                    }
                    isFirstTimeLoad = false;
                }
                break;
            case R.id.nav_item_modules:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.nav_item_modules);
                    navFragment = new ModulesFragment(this);
                    isFirstTimeLoad = false;
                    if (advancedInstallerFragment != null) {
                        jsonData = advancedInstallerFragment.getJSON_DATA();
                    }
                }
                break;
            case R.id.nav_item_downloads:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.nav_item_download);
                    navFragment = new DownloadFragment();
                    isFirstTimeLoad = false;
                    if (advancedInstallerFragment != null) {
                        jsonData = advancedInstallerFragment.getJSON_DATA();
                    }
                }
                break;
            case R.id.nav_item_logs:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.nav_item_logs);
                    navFragment = LogsFragment.newInstance("error");
                    isFirstTimeLoad = false;
                    if (advancedInstallerFragment != null) {
                        jsonData = advancedInstallerFragment.getJSON_DATA();
                    }
                }
                break;
            case R.id.nav_black_white_list:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.black_and_white_list);
                    navFragment = new BlackListFragment();
                    isFirstTimeLoad = false;
                    if (advancedInstallerFragment != null) {
                        jsonData = advancedInstallerFragment.getJSON_DATA();
                    }
                }
                break;
            case R.id.nav_verbose_logs:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.verbose_logs);
                    navFragment = LogsFragment.newInstance("all");
                    isFirstTimeLoad = false;
                    if (advancedInstallerFragment != null) {
                        jsonData = advancedInstallerFragment.getJSON_DATA();
                    }
                }
                break;
            case R.id.nav_support_mode:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.support_mode);
                    navFragment = new CompatListFragment();
                    isFirstTimeLoad = false;
                    if (advancedInstallerFragment != null) {
                        jsonData = advancedInstallerFragment.getJSON_DATA();
                    }
                }
                break;
            case R.id.nav_edxp_settings:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.edxp_settings);
                    navFragment = new SettingFragment();
                    isFirstTimeLoad = false;
                    if (advancedInstallerFragment != null) {
                        jsonData = advancedInstallerFragment.getJSON_DATA();
                    }
                }
                break;
            case R.id.nav_selinux:
                if (advancedInstallerFragment != null) {
                    jsonData = advancedInstallerFragment.getJSON_DATA();
                }
                startActivity(new Intent(WelcomeActivity.this, SELinuxActivity.class));
                break;
            case R.id.nav_personalize:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.personalize);
                    navFragment = new PersonalizeFragment();
                    isFirstTimeLoad = false;
                    if (advancedInstallerFragment != null) {
                        jsonData = advancedInstallerFragment.getJSON_DATA();
                    }
                }
                break;
            case R.id.nav_about:
                if (advancedInstallerFragment != null) {
                    jsonData = advancedInstallerFragment.getJSON_DATA();
                }
                startActivity(new Intent(WelcomeActivity.this, AboutActivity.class));
                break;
            case R.id.nav_support_me:
                if (advancedInstallerFragment != null) {
                    jsonData = advancedInstallerFragment.getJSON_DATA();
                }
                startActivity(new Intent(WelcomeActivity.this, SupportActivity.class));
                break;
            case R.id.nav_settings:
                if (advancedInstallerFragment != null) {
                    jsonData = advancedInstallerFragment.getJSON_DATA();
                }
                startActivity(new Intent(WelcomeActivity.this, XposedSettingsActivity.class));
                break;
            case R.id.nav_back_to_bottom_mode:
                SharedPreferences.Editor editor = XposedApp.getPreferences().edit();
                editor.putBoolean("drawer_layout_home", false);
                editor.apply();
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                Objects.requireNonNull(intent).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                break;
        }
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
        if (navFragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
            try {
                transaction.replace(R.id.content_frame, navFragment).commit();
            } catch (IllegalStateException ignored) {
            }
        }
        System.gc();
    }

    private void navigate(final int itemId) {
        mNavigationView.getMenu().findItem(R.id.nav_item_more).setIcon(R.drawable.ic_settings_).setTitle("选项");

        Fragment navFragment = null;
        switch (itemId) {
            case R.id.nav_item_framework:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.app_name_full);
                    if (XposedApp.getPreferences().getBoolean("save_loaded_state", true) && advancedInstallerFragment != null) {
                        navFragment = advancedInstallerFragment;
                    } else {
                        navFragment = new AdvancedInstallerFragment();
                        advancedInstallerFragment = (AdvancedInstallerFragment) navFragment;
                    }
                    isFirstTimeLoad = false;
                }
                break;
            case R.id.nav_item_modules:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.nav_item_modules);
                    navFragment = new ModulesFragment(this);
                    isFirstTimeLoad = false;
                }
                break;
            case R.id.nav_item_downloads:
                mPrevSelectedId = itemId;
                setTitle(R.string.nav_item_download);
                navFragment = new DownloadFragment();
                isFirstTimeLoad = false;
                break;
            case R.id.nav_item_logs:
                if (mPrevSelectedId != itemId || isFirstTimeLoad) {
                    mPrevSelectedId = itemId;
                    setTitle(R.string.nav_item_logs);
                    navFragment = LogsFragment.newInstance("error");
                    isFirstTimeLoad = false;
                }
                break;
            case R.id.nav_item_more:
                mPrevSelectedId = itemId;
                setTitle("选项");
                navFragment = new MoreOptionsFragment(mNavigationView);
                isFirstTimeLoad = false;
                break;
        }

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4));

        if (navFragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
            try {
                transaction.replace(R.id.content_frame, navFragment).commit();
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_ITEM_ID, mSelectedId);
    }

    @Override
    public void onBackPressed() {
        if (drawer != null) {
            if (drawer.isDrawerOpen(GravityCompat.START)) { //是否打开侧滑
                drawer.closeDrawer(GravityCompat.START); //关闭侧滑
            } else if (mPrevSelectedId != R.id.nav_item_framework) { //是否选中了框架以外的Fragment
                navigateDrawer(R.id.nav_item_framework); //选回框架Fragment
                navView.getMenu().findItem(R.id.nav_item_framework).setChecked(true);
            } else {
                super.onBackPressed(); //默认返回事件
            }
        } else if (mNavigationView != null) {
            if (mPrevSelectedId != R.id.nav_item_framework) {
                navigate(R.id.nav_item_framework);
                mNavigationView.getMenu().findItem(R.id.nav_item_framework).setChecked(true);
            } else {
                super.onBackPressed();
            }
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
                Snackbar.make(parentLayout, R.string.welcome_framework_update_available + " " + String.valueOf(frameworkUpdateVersion), Snackbar.LENGTH_LONG).show();
            }
        }

        boolean snackBar = getSharedPreferences(
                getPackageName() + "_preferences", MODE_PRIVATE).getBoolean("snack_bar", true);

        if (moduleUpdateAvailable && snackBar) {
            Snackbar.make(parentLayout, R.string.modules_updates_available, Snackbar.LENGTH_LONG).setAction(getString(R.string.view), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switchFragment(2);
                }
            }).show();
        }
    }

    @Override
    public void onInstalledModulesReloaded(ModuleUtil moduleUtil) {
        notifyDataSetChanged();
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String
            packageName, InstalledModule module) {
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
//        if ("black_white_list_enabled".equals(key)) {
//            updateBlackListEntry();
//        }
//        if ("disable_verbose_log".equals(key)) {
//            updateVerboseLogsEntry();
//        }
    }

    private void updateBlackListEntry() {
        if (drawer != null) {
            navView.getMenu().findItem(R.id.nav_black_white_list).setVisible(
                    XposedApp.getPreferences().getBoolean(
                            "black_white_list_enabled", false));
        }
    }

    private void updateVerboseLogsEntry() {
        if (drawer != null) {
            navView.getMenu().findItem(R.id.nav_verbose_logs).setVisible(
                    !XposedApp.getPreferences().getBoolean(
                            "disable_verbose_log", false));
        }
    }

    public static void setNavigationMenuLineStyle(NavigationView navigationView,
                                                  @ColorInt final int color, final int height) {
        try {
            Field fieldByPressenter = navigationView.getClass().getDeclaredField("presenter");
            fieldByPressenter.setAccessible(true);
            NavigationMenuPresenter menuPresenter = (NavigationMenuPresenter) fieldByPressenter.get(navigationView);
            Field fieldByMenuView = menuPresenter.getClass().getDeclaredField("menuView");
            fieldByMenuView.setAccessible(true);
            final NavigationMenuView mMenuView = (NavigationMenuView) fieldByMenuView.get(menuPresenter);
            mMenuView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(View view) {
                    RecyclerView.ViewHolder viewHolder = mMenuView.getChildViewHolder(view);
                    if (viewHolder != null && "SeparatorViewHolder".equals(viewHolder.getClass().getSimpleName()) && viewHolder.itemView != null) {
                        if (viewHolder.itemView instanceof FrameLayout) {
                            FrameLayout frameLayout = (FrameLayout) viewHolder.itemView;
                            View line = frameLayout.getChildAt(0);
                            line.setBackgroundColor(color);
                            line.getLayoutParams().height = height;
                            line.setLayoutParams(line.getLayoutParams());
                        }
                    }
                }

                @Override
                public void onChildViewDetachedFromWindow(View view) {

                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}