package org.meowcat.edxposed.manager.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;

import org.meowcat.edxposed.manager.BaseActivity;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;

public final class ThemeUtil {
    private static final int[] THEMES = new int[]{
            R.style.Theme_XposedInstaller_Light,
            R.style.Theme_XposedInstaller_Dark,
            R.style.Theme_XposedInstaller_Dark_Black,};

    private ThemeUtil() {
    }

    private static boolean isNightTheme(Context context) {
        int theme = XposedApp.getPreferences().getInt("theme", 0);
        return (theme == 2 && (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) > 0) || theme == 1;
    }

    private static int getSelectTheme(Context context) {
        int theme = 0;
        if (isNightTheme(context)) {
            if (XposedApp.getPreferences().getBoolean("pure_black", false)) {
                theme = 2;
            } else {
                theme = 1;
            }
        }
        return theme;
    }

    public static void setTheme(BaseActivity activity) {
        activity.mTheme = getSelectTheme(activity);
        activity.setTheme(THEMES[activity.mTheme]);
    }

    public static void reloadTheme(BaseActivity activity) {
        int theme = getSelectTheme(activity);
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
