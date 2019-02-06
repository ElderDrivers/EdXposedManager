package com.solohsu.android.edxp.manager.util;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class Utils {

    public static String getAppLabel(ApplicationInfo info, PackageManager pm) {
        try {
            if (info.labelRes > 0) {
                Resources res = pm.getResourcesForApplication(info);
                Configuration config = new Configuration();
                config.setLocale(Locale.getDefault());
                res.updateConfiguration(config, res.getDisplayMetrics());
                return res.getString(info.labelRes);
            }
        } catch (Exception ignored) {
        }
        return info.loadLabel(pm).toString();
    }

}
