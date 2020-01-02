package de.robv.android.xposed.installer;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.solohsu.android.edxp.manager.R;

import de.robv.android.xposed.installer.util.ThemeUtil;

public abstract class XposedBaseActivity extends AppCompatActivity {
    public int mTheme = ThemeUtil.DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        ThemeUtil.apply(this);
        AppCompatDelegate.setDefaultNightMode(XposedApp.getNightMode());
    }

    @Override
    protected void onResume() {
        super.onResume();
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
}