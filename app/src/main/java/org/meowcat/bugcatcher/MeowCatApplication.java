/*
 * Copyright (c) 2013-2018 MeowCat Studio Powered by MlgmXyysd All Rights Reserved.
 */

package org.meowcat.bugcatcher;

import de.robv.android.xposed.installer.XposedApp;

public class MeowCatApplication extends XposedApp {
    private static final String TAG = XposedApp.TAG;
    
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
    }
}