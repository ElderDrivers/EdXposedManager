package de.robv.android.xposed.installer.util;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;

import com.solohsu.android.edxp.manager.R;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.XposedBaseActivity;

public final class ThemeUtil {

    public static int DEFAULT = 0;
    public static int ACCENT_BLUE = 1;
    public static int ACCENT_NAVY_BLUE = 2;
    public static int ACCENT_DIM = 3;
    public static int ACCENT_TIAN_YI = 4;
    public static int ACCENT_PURPLE = 5;
    public static int ACCENT_LIMB = 6;
    public static int ACCENT_PINK = 7;

    public static List<String> ThemeName = new ArrayList<>();
    public static List<Integer> ThemeValue = new ArrayList<>();
    public static List<Integer> PanelDrawable = new ArrayList<>();

    static {
        ThemeName.add("默认（大红色）");
        ThemeName.add("蓝色");
        ThemeName.add("藏青色");
        ThemeName.add("黯色");
        ThemeName.add("天依色");
        ThemeName.add("基佬紫");
        ThemeName.add("草青色");
        ThemeName.add("活力粉");

        ThemeValue.add(R.style.Theme_XposedInstaller_Light);
        ThemeValue.add(R.style.Theme_XposedInstaller_Light_Blue);
        ThemeValue.add(R.style.Theme_XposedInstaller_Light_NavyBlue);
        ThemeValue.add(R.style.Theme_XposedInstaller_Light_DIM);
        ThemeValue.add(R.style.Theme_XposedInstaller_Light_TianYi);
        ThemeValue.add(R.style.Theme_XposedInstaller_Light_Purple);
        ThemeValue.add(R.style.Theme_XposedInstaller_Light_Limb);
        ThemeValue.add(R.style.Theme_XposedInstaller_Light_Pink);

        PanelDrawable.add(R.drawable.status_bg);
        PanelDrawable.add(R.drawable.status_bg_blue);
        PanelDrawable.add(R.drawable.status_bg_navy_blue);
        PanelDrawable.add(R.drawable.status_bg_dim);
        PanelDrawable.add(R.drawable.status_bg_tian_yi);
        PanelDrawable.add(R.drawable.status_bg_purple);
        PanelDrawable.add(R.drawable.status_bg_limb);
        PanelDrawable.add(R.drawable.status_bg_pink);
    }

    private ThemeUtil() {
    }

    public static void apply(XposedBaseActivity activity) {
        activity.mTheme = getAppAccent();
        activity.setTheme(ThemeValue.get(activity.mTheme));
    }

    public static void setTheme(int theme) {
        XposedApp.getPreferences().edit().putInt("manager_theme", theme).apply();
    }

    public static int getAppAccent() {
        return XposedApp.getPreferences().getInt("manager_theme", 0);
    }

    public static int getThemeColor(Context context, int id) {
        Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(new int[]{id});
        int result = a.getColor(0, 0);
        a.recycle();
        return result;
    }
}
