package org.meowcat.edxposed.manager;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import org.meowcat.edxposed.manager.util.ThemeUtil;

import java.io.File;

import static org.meowcat.edxposed.manager.SettingsFragment.navBar;

public class SettingsActivity extends BaseActivity implements ColorChooserDialog.ColorCallback, FolderChooserDialog.FolderCallback {

    private Toolbar toolbar;

    public static float getDarkenFactor() {
        return XposedApp.getPreferences().getBoolean("dark_status_bar", true) ? 0.85f : 1f;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_container);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(view -> finish());

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.nav_item_settings);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setFloating(toolbar, 0);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment()).commit();
        }

    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        if (folder.canWrite()) {
            XposedApp.getPreferences().edit().putString("download_location", folder.getPath()).apply();
        } else {
            Toast.makeText(this, R.string.sdcard_not_writable, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int color) {
        int colorFrom = XposedApp.getColor(this);

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, color);
        colorAnimation.addUpdateListener(animator -> {
            int color1 = (int) animator.getAnimatedValue();

            toolbar.setBackgroundColor(color1);

            int darkenColor = XposedApp.darkenColor(color1, getDarkenFactor());

            getWindow().setStatusBarColor(darkenColor);

            if (navBar != null && navBar.isChecked()) {
                getWindow().setNavigationBarColor(darkenColor);
            }
        });
        colorAnimation.setDuration(750);
        colorAnimation.start();

        if (!dialog.isAccentMode()) {
            XposedApp.getPreferences().edit().putInt("colors", color).apply();
        }
    }
}
