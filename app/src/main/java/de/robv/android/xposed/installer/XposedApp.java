package de.robv.android.xposed.installer;

import android.util.Log;

import org.meowcat.bugcatcher.MeowCatApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.robv.android.xposed.installer.util.InstallZipUtil;

import static de.robv.android.xposed.installer.util.InstallZipUtil.parseXposedProp;

public class XposedApp {
    public static InstallZipUtil.XposedProp mXposedProp;
    private static final File EDXPOSED_PROP_FILE = new File("/system/framework/edconfig.jar");

    // This method is hooked by XposedBridge to return the current version
    public static Integer getActiveXposedVersion() {
        Log.d(MeowCatApplication.TAG, "EdXposed is not active");
        return -1;
    }

    public static void reloadXposedProp() {
        InstallZipUtil.XposedProp prop = null;
        File file = null;

        if (EDXPOSED_PROP_FILE.canRead()) {
            file = EDXPOSED_PROP_FILE;
        }

        if (file != null) {
            try (FileInputStream is = new FileInputStream(file)) {
                prop = parseXposedProp(is);
            } catch (IOException e) {
                Log.e(org.meowcat.edxposed.manager.XposedApp.TAG, "Could not read " + file.getPath(), e);
            }
        }

        mXposedProp = prop;
    }
}
