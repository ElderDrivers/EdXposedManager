package de.robv.android.xposed.installer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.getkeepsafe.taptargetview.TapTarget;
import com.solohsu.android.edxp.manager.BuildConfig;
import com.solohsu.android.edxp.manager.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.installer.receivers.PackageChangeReceiver;
import de.robv.android.xposed.installer.util.AssetUtil;
import de.robv.android.xposed.installer.util.InstallZipUtil;
import de.robv.android.xposed.installer.util.ModuleUtil;
import de.robv.android.xposed.installer.util.MyFileUtils;
import de.robv.android.xposed.installer.util.NotificationUtil;
import de.robv.android.xposed.installer.util.RepoLoader;

@SuppressWarnings({"ResultOfMethodCallIgnored", "OctalInteger"})
@SuppressLint("Registered")
public class XposedApp extends Application implements ActivityLifecycleCallbacks {
    public static final String TAG = "EdXposedManager";
    @SuppressLint("SdCardPath")
    private static final String BASE_DIR_LEGACY = "/data/data/" + BuildConfig.APPLICATION_ID + "/";
    public static final String BASE_DIR = "/data/user_de/0/" + BuildConfig.APPLICATION_ID + "/";
    private static final File EDXPOSED_PROP_FILE = new File("/system/framework/edconfig.jar");
    public static final String ENABLED_MODULES_LIST_FILE = BASE_DIR + "conf/enabled_modules.list";
    public static int WRITE_EXTERNAL_PERMISSION = 69;
    public static int[] iconsValues = new int[]{R.mipmap.ic_launcher, R.mipmap.circle_ic_launcher, R.mipmap.ic_launcher_hjmodi, R.mipmap.ic_launcher_staol};
    private static Pattern PATTERN_APP_PROCESS_VERSION = Pattern.compile(".*with Xposed support \\(version (.+)\\).*");
    @SuppressLint("StaticFieldLeak")
    private static XposedApp mInstance = null;
    private static Thread mUiThread;
    private static Handler mMainHandler;
    private boolean mIsUiLoaded = false;
    private SharedPreferences mPref;
    private InstallZipUtil.XposedProp mXposedProp;
    private Activity mCurrentActivity = null;

    public static XposedApp getInstance() {
        return mInstance;
    }

    public static void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            mMainHandler.post(action);
        } else {
            action.run();
        }
    }

    public static File createFolder() {
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/EdXposedManager/");

        if (!dir.exists()) dir.mkdir();

        return dir;
    }

    public static void postOnUiThread(Runnable action) {
        mMainHandler.post(action);
    }

    public static Integer getXposedVersion() {
        if (Build.VERSION.SDK_INT >= 21) {
            return getActiveXposedVersion();
        } else {
            return getInstalledAppProcessVersion();
        }
    }

    private static int getInstalledAppProcessVersion() {
        try {
            return getAppProcessVersion(new FileInputStream("/system/bin/app_process"));
        } catch (IOException e) {
            return 0;
        }
    }

    private static int getAppProcessVersion(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.contains("Xposed"))
                continue;

            Matcher m = PATTERN_APP_PROCESS_VERSION.matcher(line);
            if (m.find()) {
                is.close();
                return ModuleUtil.extractIntPart(m.group(1));
            }
        }
        is.close();
        return 0;
    }

    // This method is hooked by XposedBridge to return the current version
    @SuppressWarnings({"NumericOverflow", "divzero"})
    public static Integer getActiveXposedVersion() {
        try {
            Log.d("stub", String.valueOf(1 / 0));
        } catch (Exception ex) {
            return -1;
        }
        return -1;
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    public static InstallZipUtil.XposedProp getXposedProp() {
        synchronized (mInstance) {
            return mInstance.mXposedProp;
        }
    }

    public static SharedPreferences getPreferences() {
        return mInstance.mPref;
    }

    public static int getColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_PRIVATE);
        int defaultColor = context.getResources().getColor(R.color.colorPrimary);

        return prefs.getInt("colors", defaultColor);
    }

    public static void setColors(ActionBar actionBar, Integer value, Activity activity) {
        int color = value;
        SharedPreferences prefs = activity.getSharedPreferences(activity.getPackageName() + "_preferences", MODE_PRIVATE);

        int drawable = iconsValues[Integer.parseInt(Objects.requireNonNull(prefs.getString("custom_icon", "0")))];

        if (actionBar != null)
            actionBar.setBackgroundDrawable(new ColorDrawable(color));

        if (Build.VERSION.SDK_INT >= 21) {

            ActivityManager.TaskDescription tDesc = new ActivityManager.TaskDescription(activity.getString(R.string.app_name),
                    drawableToBitmap(activity.getDrawable(drawable)), color);
            activity.setTaskDescription(tDesc);

//            if (getPreferences().getBoolean("nav_bar", false)) {
//                activity.getWindow().setNavigationBarColor(darkenColor(color, 0.85f));
//            } else {
//                int black = activity.getResources().getColor(android.R.color.black);
//                activity.getWindow().setNavigationBarColor(black);
//            }
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
        return getPreferences().getString("download_location", Environment.getExternalStorageDirectory() + "/Download/EdXposedMaterial/");
    }

    public static void mkdirAndChmod(String dir, int permissions) {
        dir = BASE_DIR + dir;
        //noinspection ResultOfMethodCallIgnored
        new File(dir).mkdir();
        MyFileUtils.setPermissions(dir, permissions, -1, -1);
    }

    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mUiThread = Thread.currentThread();
        mMainHandler = new Handler();

        mPref = PreferenceManager.getDefaultSharedPreferences(this);

        reloadXposedProp();
        createDirectories();
        delete(new File(Environment.getExternalStorageDirectory() + "/Download/EdXposedMaterial/.temp"));
        NotificationUtil.init();
        AssetUtil.removeBusybox();
        registerReceivers();

        registerActivityLifecycleCallbacks(this);

        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
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
            conf.locale = Locale.ENGLISH;
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

    private void delete(File file) {
        if (file != null) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) for (File f : file.listFiles()) delete(f);
            }
            file.delete();
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint({"PrivateApi", "NewApi"})
    private void createDirectories() {
        MyFileUtils.setPermissions(BASE_DIR, 00777, -1, -1);
        mkdirAndChmod("conf", 00777);
        mkdirAndChmod("log", 00777);

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method deleteDir = MyFileUtils.class.getDeclaredMethod("deleteContents", File.class);
                deleteDir.invoke(null, new File(BASE_DIR_LEGACY, "bin"));
                deleteDir.invoke(null, new File(BASE_DIR_LEGACY, "conf"));
                deleteDir.invoke(null, new File(BASE_DIR_LEGACY, "log"));
            } catch (ReflectiveOperationException e) {
                Log.w(XposedApp.TAG, "Failed to delete obsolete directories", e);
            }
        }
    }

    private void reloadXposedProp() {
        InstallZipUtil.XposedProp prop = null;
        File file = null;

        if (EDXPOSED_PROP_FILE.canRead()) {
            file = EDXPOSED_PROP_FILE;
        }

        if (file != null) {
            try (FileInputStream is = new FileInputStream(file)) {
                prop = InstallZipUtil.parseXposedProp(is);
            } catch (IOException e) {
                Log.e(XposedApp.TAG, "Could not read " + file.getPath(), e);
            }
        }

        synchronized (this) {
            mXposedProp = prop;
        }
    }

    public void updateProgressIndicator(final SwipeRefreshLayout refreshLayout) {
        final boolean isLoading = RepoLoader.getInstance().isLoading() || ModuleUtil.getInstance().isLoading();
        runOnUiThread(() -> {
            synchronized (XposedApp.this) {
                if (mCurrentActivity != null) {
                    mCurrentActivity.setProgressBarIndeterminateVisibility(isLoading);
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
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        mCurrentActivity = activity;
        updateProgressIndicator(null);
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        activity.setProgressBarIndeterminateVisibility(false);
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

    public static boolean isNightMode() {
        return AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
                || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public static int getNightMode() {
        String nightMode = getPreferences().getString("night_mode", AppCompatDelegate.MODE_NIGHT_NO + "");
        return nightMode == null ? AppCompatDelegate.MODE_NIGHT_NO : Integer.valueOf(nightMode);
    }

    public static TapTarget targetView(Rect bounds, CharSequence title, CharSequence description) {
        return TapTarget.forBounds(bounds, title, description)
                .textColor(R.color.colorAccent).titleTextColor(R.color.colorAccent)
                .descriptionTextColor(R.color.colorAccent)
                .transparentTarget(true)
                .dimColorInt(0x55000000)
                .targetCircleColor(R.color.colorAccent).cancelable(false);
    }

    public class Constant {
        public static final String NOT_INSTALLED_HELP_GUIDE = "Not Installed Help Guide";
        public static final String NOT_ACTIVE_HELP_GUIDE = "Not Active Help Guide";
        public static final String INSTALLED_DETAILS_GUIDE = "Installed Details Guide";
    }
}