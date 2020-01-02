package de.robv.android.xposed.installer.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import com.solohsu.android.edxp.manager.R;

import de.robv.android.xposed.installer.XposedBaseActivity;
import de.robv.android.xposed.installer.fragment.InstalledDetailsGuide;
import de.robv.android.xposed.installer.fragment.NotActiveGuide;
import de.robv.android.xposed.installer.fragment.NotInstalledGuide;

import static de.robv.android.xposed.installer.XposedApp.Constant.INSTALLED_DETAILS_GUIDE;
import static de.robv.android.xposed.installer.XposedApp.Constant.NOT_ACTIVE_HELP_GUIDE;
import static de.robv.android.xposed.installer.XposedApp.Constant.NOT_INSTALLED_HELP_GUIDE;

public class XposedGuideActivity extends XposedBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_container_no_toolbar);

        Intent intent = getIntent();
        String guide = intent.getStringExtra("guide_fragment");

        switch (guide) {
            case NOT_INSTALLED_HELP_GUIDE:
                getSupportFragmentManager() //
                        .beginTransaction()
                        .add(R.id.container, new NotInstalledGuide())
                        .commit();
//                setTitle(getString(R.string.installation_guide));
                break;
            case NOT_ACTIVE_HELP_GUIDE:
                getSupportFragmentManager() //
                        .beginTransaction()
                        .add(R.id.container, new NotActiveGuide())
                        .commit();
//                setTitle(getString(R.string.active_guide));
                break;
            case INSTALLED_DETAILS_GUIDE:
                getSupportFragmentManager() //
                        .beginTransaction()
                        .add(R.id.container, new InstalledDetailsGuide())
                        .commit();
//                setTitle(getString(R.string.installed_details));
                break;
        }

    }
}
