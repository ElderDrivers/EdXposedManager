package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.meowcat.edxposed.manager.receiver.PackageChangeReceiver;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.NotificationUtil;
import org.meowcat.edxposed.manager.util.RepoLoader;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.robv.android.xposed.installer.util.InstallZipUtil;

import static org.meowcat.edxposed.manager.MeowCatApplication.TAG;
import static org.meowcat.edxposed.manager.adapter.AppHelper.FORCE_WHITE_LIST_MODULE;

@SuppressLint("Registered")
public class XposedApp extends de.robv.android.xposed.installer.XposedApp implements ActivityLifecycleCallbacks {
    public static final int rwxrwxrwx = 511;
    public static final int rw_rw_r__ = 436;
    public static String BASE_DIR = null;
    public static String ENABLED_MODULES_LIST_FILE = null;
    public static int WRITE_EXTERNAL_PERMISSION = 69;
    public static int[] iconsValues = new int[]{R.mipmap.ic_launcher, R.mipmap.ic_launcher_dvdandroid, R.mipmap.ic_launcher_hjmodi, R.mipmap.ic_launcher_rovo, R.mipmap.ic_launcher_cornie, R.mipmap.ic_launcher_rovo_old, R.mipmap.ic_launcher_staol};
    @SuppressLint("StaticFieldLeak")
    private static XposedApp mInstance = null;
    private static Thread mUiThread;
    private static Handler mMainHandler;
    private boolean mIsUiLoaded = false;
    private SharedPreferences mPref;
    private Activity mCurrentActivity = null;

    @SuppressWarnings("deprecation")
    @SuppressLint({"WorldReadableFiles", "WorldWriteableFiles"})
    public static void setFilePermissionsFromMode(String name, int mode) {
        int perms = FileUtils.S_IRUSR | FileUtils.S_IWUSR
                | FileUtils.S_IRGRP | FileUtils.S_IWGRP;
        if ((mode & Context.MODE_WORLD_READABLE) != 0) {
            perms |= FileUtils.S_IROTH;
        }
        if ((mode & Context.MODE_WORLD_WRITEABLE) != 0) {
            perms |= FileUtils.S_IWOTH;
        }
        FileUtils.setPermissions(name, perms, -1, -1);
    }

    public static XposedApp getInstance() {
        return mInstance;
    }

    public static InstallZipUtil.XposedProp getXposedProp() {
        return de.robv.android.xposed.installer.XposedApp.getInstance().mXposedProp;
    }

    public static void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            mMainHandler.post(action);
        } else {
            action.run();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createFolder() {
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/EdXposedManager/");

        if (!dir.exists()) dir.mkdir();

        return dir;
    }

//    public static void postOnUiThread(Runnable action) {
//        mMainHandler.post(action);
//    }

    public static int getXposedVersion() {
        return getActiveXposedVersion();
    }

    public static SharedPreferences getPreferences() {
        return mInstance.mPref;
    }

    public static int getColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_PRIVATE);
        int defaultColor = context.getResources().getColor(R.color.colorPrimary, null);

        return prefs.getInt("colors", defaultColor);
    }

    @SuppressWarnings("deprecation")
    public static void setColors(ActionBar actionBar, Integer value, Activity activity) {
        int color = value;
        SharedPreferences prefs = activity.getSharedPreferences(activity.getPackageName() + "_preferences", MODE_PRIVATE);

        int drawable = iconsValues[Integer.parseInt(Objects.requireNonNull(prefs.getString("custom_icon", "0")))];

        if (actionBar != null)
            actionBar.setBackgroundDrawable(new ColorDrawable(color));

        ActivityManager.TaskDescription tDesc = new ActivityManager.TaskDescription(activity.getString(R.string.app_name),
                drawableToBitmap(activity.getDrawable(drawable)), color);
//        ActivityManager.TaskDescription tDesc = new ActivityManager.TaskDescription(activity.getString(R.string.app_name),
//                drawable, color);
        activity.setTaskDescription(tDesc);

        if (getPreferences().getBoolean("nav_bar", false)) {
            activity.getWindow().setNavigationBarColor(darkenColor(color, 0.85f));
        } else {
            int black = activity.getResources().getColor(android.R.color.black, null);
            activity.getWindow().setNavigationBarColor(black);
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * @author PeterCxy https://github.com/PeterCxy/Lolistat/blob/aide/app/src/
     * main/java/info/papdt/lolistat/support/Utility.java
     */
    public static int darkenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor;
        return Color.HSVToColor(hsv);
    }

    public static String getDownloadPath() {
        return getPreferences().getString("download_location", Environment.getExternalStorageDirectory() + "/Download/EdXposedManager/");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void mkdirAndChmod(String dir, int permissions) {
        dir = BASE_DIR + dir;
        new File(dir).mkdir();
        FileUtils.setPermissions(dir, permissions, -1, -1);
    }

    public static boolean checkAppInstalled(Context context, String pkgName) {
        if (pkgName == null || pkgName.isEmpty()) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> info = packageManager.getInstalledPackages(0);
        if (info == null || info.isEmpty()) {
            return false;
        }
        for (int i = 0; i < info.size(); i++) {
            if (pkgName.equals(info.get(i).packageName)) {
                return true;
            }
        }
        return false;
    }

    public void onCreate() {
        super.onCreate();

        final ApplicationInfo appInfo = getApplicationInfo();
        BASE_DIR = appInfo.deviceProtectedDataDir + "/";
        ENABLED_MODULES_LIST_FILE = BASE_DIR + "conf/enabled_modules.list";

        mInstance = this;
        mUiThread = Thread.currentThread();
        mMainHandler = new Handler();

        mPref = PreferenceManager.getDefaultSharedPreferences(this);

        de.robv.android.xposed.installer.XposedApp.getInstance().reloadXposedProp();
        createDirectories();
        delete(new File(Environment.getExternalStorageDirectory() + "/Download/EdXposedManager/.temp"));
        NotificationUtil.init();
        registerReceivers();

        registerActivityLifecycleCallbacks(this);

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date date = new Date();

        if (!Objects.requireNonNull(mPref.getString("date", "")).equals(dateFormat.format(date))) {
            mPref.edit().putString("date", dateFormat.format(date)).apply();

            try {
                Log.i(TAG, String.format("EdXposedManager - %s - %s", BuildConfig.VERSION_CODE, getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        if (mPref.getBoolean("force_english", false)) {
            Resources res = getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.setLocale(Locale.ENGLISH);
            res.updateConfiguration(conf, dm);
        }
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(new PackageChangeReceiver(), filter);

        PendingIntent.getBroadcast(this, 0,
                new Intent(this, PackageChangeReceiver.class), 0);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void delete(File file) {
        if (file != null) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) for (File f : file.listFiles()) delete(f);
            }
            file.delete();
        }
    }

    private void createDirectories() {
        FileUtils.setPermissions(BASE_DIR, rwxrwxrwx, -1, -1);
        mkdirAndChmod("conf", rwxrwxrwx);
        mkdirAndChmod("log", rwxrwxrwx);
    }

    public void updateProgressIndicator(final SwipeRefreshLayout refreshLayout) {
        final boolean isLoading = RepoLoader.getInstance().isLoading() || ModuleUtil.getInstance().isLoading();
        runOnUiThread(() -> {
            synchronized (XposedApp.this) {
                if (mCurrentActivity != null) {
                    //mCurrentActivity.setProgressBarIndeterminateVisibility(isLoading);
                    if (refreshLayout != null)
                        refreshLayout.setRefreshing(isLoading);
                }
            }
        });
    }

    @Override
    public synchronized void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (mIsUiLoaded)
            return;

        RepoLoader.getInstance().triggerFirstLoadIfNecessary();
        mIsUiLoaded = true;

        if (mPref.getBoolean("hook_modules", true)) {
            Collection<ModuleUtil.InstalledModule> installedModules = ModuleUtil.getInstance().getModules().values();
            for (ModuleUtil.InstalledModule info : installedModules) {
                if (!FORCE_WHITE_LIST_MODULE.contains(info.packageName)) {
                    FORCE_WHITE_LIST_MODULE.add(info.packageName);
                }
            }
            Log.d(MeowCatApplication.TAG, "ApplicationList: Force add modules to list");
        }
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        mCurrentActivity = activity;
        updateProgressIndicator(null);
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        //activity.setProgressBarIndeterminateVisibility(false);
        mCurrentActivity = null;
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

}

