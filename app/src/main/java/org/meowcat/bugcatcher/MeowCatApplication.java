/*
 * Copyright (c) 2013-2020 MeowCat Studio Powered by MlgmXyysd All Rights Reserved.
 */

package org.meowcat.bugcatcher;

import android.annotation.SuppressLint;
import android.os.Build;

import org.meowcat.edxposed.manager.XposedApp;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MeowCatApplication extends XposedApp {
    public static final String TAG = "EdXposedManager";

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(getApplicationContext());
        disableAPIDialog();
    }

    private void disableAPIDialog() {
        if (Build.VERSION.SDK_INT < 28) {
            return;
        }
        try {
            @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"}) Method currentActivityThread = Class.forName("android.app.ActivityThread").getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            @SuppressLint("PrivateApi") Field mHiddenApiWarningShown = Class.forName("android.app.ActivityThread").getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(currentActivityThread.invoke(null), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}