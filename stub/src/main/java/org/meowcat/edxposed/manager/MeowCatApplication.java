/*
 * Copyright (c) 2013-2020 MeowCat Studio Powered by MlgmXyysd All Rights Reserved.
 */

package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.os.Build;

import java.lang.reflect.Field;

import de.robv.android.xposed.installer.XposedApp;

import static android.app.ActivityThread.currentActivityThread;

@SuppressLint("Registered")
public class MeowCatApplication extends XposedApp {
    public static final String TAG = "EdXposedManager";

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(getApplicationContext());
        disableAPIDialog();
    }

    private void disableAPIDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        try {
            Field mHiddenApiWarningShown = Class.forName("android.app.ActivityThread").getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(currentActivityThread(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}