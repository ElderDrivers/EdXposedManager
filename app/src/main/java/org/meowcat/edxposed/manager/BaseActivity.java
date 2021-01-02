package org.meowcat.edxposed.manager;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import org.meowcat.edxposed.manager.util.LocaleUtil;
import org.meowcat.edxposed.manager.util.ThemeUtil;

import java.util.Locale;

import static org.meowcat.edxposed.manager.SettingsActivity.getDarkenFactor;
import static org.meowcat.edxposed.manager.XposedApp.darkenColor;

public abstract class BaseActivity extends AppCompatActivity {
    public int mTheme = -1;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        ThemeUtil.setTheme(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        checkForceEnglish(prefs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        XposedApp.setColors(getSupportActionBar(), XposedApp.getColor(this), this);
        getWindow().setStatusBarColor(darkenColor(XposedApp.getColor(this), getDarkenFactor()));
        ThemeUtil.reloadTheme(this);
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
}
