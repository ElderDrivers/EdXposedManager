package org.meowcat.edxposed.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import org.meowcat.edxposed.manager.util.LocaleUtil;

import java.util.Locale;
import java.util.Objects;

public abstract class XposedBaseActivity extends AppCompatActivity {

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";
    private String mTheme;

    public static boolean isBlackNightTheme() {
        return XposedApp.getPreferences().getBoolean("black_dark_theme", false);
    }

    public static String getTheme(Context context) {
        if (isBlackNightTheme()
                && isNightMode(context.getResources().getConfiguration()))
            return THEME_BLACK;

        return THEME_DEFAULT;
    }

    @StyleRes
    public static int getThemeStyleRes(Context context) {
        switch (getTheme(context)) {
            case THEME_BLACK:
                return R.style.ThemeOverlay_Black;
            case THEME_DEFAULT:
            default:
                return R.style.ThemeOverlay;
        }
    }

    public static boolean isNightMode(Configuration configuration) {
        return (configuration.uiMode & Configuration.UI_MODE_NIGHT_YES) > 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        AppCompatDelegate.setDefaultNightMode(XposedApp.getPreferences().getInt("theme", 0));
        mTheme = getTheme(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        checkForceEnglish(prefs);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // keep force English after orientation changed
        checkForceEnglish(prefs);
    }

    public void setFloating(androidx.appcompat.widget.Toolbar toolbar, @StringRes int details) {
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        if (isTablet) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.height = getResources().getDimensionPixelSize(R.dimen.floating_height);
            params.width = getResources().getDimensionPixelSize(R.dimen.floating_width);
            params.alpha = 1.0f;
            params.dimAmount = 0.6f;
            params.flags |= 2;
            getWindow().setAttributes(params);

            if (details != 0) {
                toolbar.setTitle(details);
            }
            toolbar.setNavigationIcon(R.drawable.ic_close);
            setFinishOnTouchOutside(true);
        }
    }

    private void checkForceEnglish(SharedPreferences prefs) {
        if (prefs.getBoolean("force_english", false)) {
            LocaleUtil.setLocale(this.getBaseContext(), Locale.ENGLISH);
        } else {
            LocaleUtil.setLocale(this.getBaseContext(), Locale.getDefault());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Objects.equals(mTheme, getTheme(this))) {
            recreate();
        }
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        // apply real style and our custom style
        if (getParent() == null) {
            theme.applyStyle(resid, true);
        } else {
            try {
                theme.setTo(getParent().getTheme());
            } catch (Exception e) {
                // Empty
            }
            theme.applyStyle(resid, false);
        }
        theme.applyStyle(getThemeStyleRes(this), true);
        // only pass theme style to super, so styled theme will not be overwritten
        super.onApplyThemeResource(theme, R.style.ThemeOverlay, first);
    }

}
