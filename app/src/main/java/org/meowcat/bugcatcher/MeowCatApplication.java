/*
 * Copyright (c) 2013-2018 MeowCat Studio Powered by MlgmXyysd All Rights Reserved.
 */

package org.meowcat.bugcatcher;

import android.annotation.SuppressLint;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.installer.XposedApp;

public class MeowCatApplication extends XposedApp {
    public static final String TAG = XposedApp.TAG;

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
        disableAPIDialog();
    }
    private void disableAPIDialog(){
        if (Build.VERSION.SDK_INT < 28) {
            return;
        }
        try {
            @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"}) Method currentActivityThread = Class.forName("android.app.ActivityThread").getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object activityThread = currentActivityThread.invoke(null);
            @SuppressLint("PrivateApi") Field mHiddenApiWarningShown = Class.forName("android.app.ActivityThread").getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(activityThread, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}