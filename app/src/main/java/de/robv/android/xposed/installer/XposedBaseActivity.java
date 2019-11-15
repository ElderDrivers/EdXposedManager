package de.robv.android.xposed.installer;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.StringRes;

import com.solohsu.android.edxp.manager.R;

import de.robv.android.xposed.installer.util.ThemeUtil;
import solid.ren.skinlibrary.base.SkinBaseActivity;

public abstract class XposedBaseActivity extends SkinBaseActivity {
    public String mTheme = "light";

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        ThemeUtil.setTheme(this);
    }

//    private boolean mDelegateCreated;
//
//    @NonNull
//    @Override
//    public AppCompatDelegate getDelegate() {
//        AppCompatDelegate delegate = super.getDelegate();
//        if (!mDelegateCreated) {
//            mDelegateCreated = true;
////            NightModeHelper.apply(this);
//        }
//        return delegate;
//    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTheme.equals("light")) {
//            XposedApp.setColors(getSupportActionBar(), getResources().getColor(R.color.app_light), this);
//            getWindow().setStatusBarColor(getResources().getColor(R.color.app_light));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.app_light));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().setNavigationBarDividerColor(0xFFF5F5F5);
            }
        } else if (mTheme.equals("dark")) {
//            XposedApp.setColors(getSupportActionBar(), getResources().getColor(R.color.app_dark), this);
//            getWindow().setStatusBarColor(getResources().getColor(R.color.app_dark));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.app_dark));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().setNavigationBarDividerColor(0xFF151515);
            }
        }
        ThemeUtil.reloadTheme(this);
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