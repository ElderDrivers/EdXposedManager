package org.meowcat.edxposed.manager.util;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;
import org.meowcat.edxposed.manager.XposedBaseActivity;

public final class ThemeUtil {
    private static int[] THEMES = new int[]{
            R.style.Theme_XposedInstaller_Light,
            R.style.Theme_XposedInstaller_Dark,
            R.style.Theme_XposedInstaller_Dark_Black,};

    private ThemeUtil() {
    }

    private static int getSelectTheme() {
        int theme = XposedApp.getPreferences().getInt("theme", 0);
        return (theme >= 0 && theme < THEMES.length) ? theme : 0;
    }

    public static void setTheme(XposedBaseActivity activity) {
        activity.mTheme = getSelectTheme();
        activity.setTheme(THEMES[activity.mTheme]);
    }

    public static void reloadTheme(XposedBaseActivity activity) {
        int theme = getSelectTheme();
        if (theme != activity.mTheme)
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
