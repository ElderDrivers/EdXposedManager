package de.robv.android.xposed.installer;

import android.annotation.SuppressLint;
import android.app.Application;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.robv.android.xposed.installer.util.InstallZipUtil;

import static de.robv.android.xposed.installer.util.InstallZipUtil.parseXposedProp;
import static org.meowcat.edxposed.manager.MeowCatApplication.TAG;

@SuppressLint("Registered")
public class XposedApp extends Application {
    private static final File EDXPOSED_PROP_FILE = new File("/system/framework/edconfig.jar");
    private static XposedApp mInstance = null;
    public InstallZipUtil.XposedProp mXposedProp;

    public static XposedApp getInstance() {
        return mInstance;
    }

    // This method is hooked by XposedBridge to return the current version
    public static Integer getActiveXposedVersion() {
        Log.d(TAG, "EdXposed is not active");
        return -1;
    }

    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public void reloadXposedProp() {
        InstallZipUtil.XposedProp prop = null;
        File file = null;

        if (EDXPOSED_PROP_FILE.canRead()) {
            file = EDXPOSED_PROP_FILE;
        }

        if (file != null) {
            try (FileInputStream is = new FileInputStream(file)) {
                prop = parseXposedProp(is);
            } catch (IOException e) {
                Log.e(TAG, "Could not read " + file.getPath(), e);
            }
        }
        synchronized (this) {
            mXposedProp = prop;
        }
    }
}
