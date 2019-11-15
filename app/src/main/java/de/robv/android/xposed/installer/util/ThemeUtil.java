package de.robv.android.xposed.installer.util;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;

import com.solohsu.android.edxp.manager.R;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.XposedBaseActivity;

public final class ThemeUtil {

    private ThemeUtil() {
    }

    public static String getSelectTheme() {
        return XposedApp.getPreferences().getString("theme", "light");
    }

    public static void setTheme(XposedBaseActivity activity) {
        activity.mTheme = getSelectTheme();
        if (activity.mTheme.equals("light")) {
            activity.setTheme(R.style.Theme_XposedInstaller_Light);
        } else if (activity.mTheme.equals("dark")) {
            activity.setTheme(R.style.Theme_XposedInstaller_Dark);
        }
    }

    public static void reloadTheme(XposedBaseActivity activity) {
        String theme = getSelectTheme();
        if (!theme.equals(activity.mTheme))
            activity.recreate();
    }

    public static int getThemeColor(Context context, int id) {
        Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(new int[]{id});
        int result = a.getColor(0, 0);
        a.recycle();
        return result;
    }
}
