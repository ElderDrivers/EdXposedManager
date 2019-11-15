package de.robv.android.xposed.installer.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.solohsu.android.edxp.manager.BuildConfig;
import com.solohsu.android.edxp.manager.R;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;
import de.robv.android.xposed.installer.XposedBaseActivity;
import de.robv.android.xposed.installer.util.NavUtil;
import de.robv.android.xposed.installer.util.ThemeUtil;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.EXTRA_TEXT;

public class AboutActivity extends XposedBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.nav_item_about);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setFloating(toolbar, R.string.details);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new AboutFragment()).commit();
        }

        ThemeUtil.setTheme(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent sharingIntent = new Intent(ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(EXTRA_TEXT, getString(R.string.share_app_text, getString(R.string.support_material_xda)));
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share)));

        return super.onOptionsItemSelected(item);
    }

    public void openLink(View view) {
        NavUtil.startURL(this, view.getTag().toString());
    }

    public static class AboutFragment extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.tab_about, container, false);

            View changelogView = v.findViewById(R.id.changelogView);
            View licensesView = v.findViewById(R.id.licensesView);
            View translatorsView = v.findViewById(R.id.translatorsView);
            View sourceCodeView = v.findViewById(R.id.sourceCodeView);

            String packageName = getActivity().getPackageName();
            String translator = getResources().getString(R.string.translator);

            SharedPreferences prefs = getContext().getSharedPreferences(packageName + "_preferences", MODE_PRIVATE);

            if (prefs.getBoolean("nav_bar", false)) {
                getActivity().getWindow().setNavigationBarColor(getResources().getColor(R.color.colorAccent));
            }

            final String changes = prefs.getString("changelog_" + BuildConfig.APP_VERSION, null);

            if (changes == null) {
                changelogView.setVisibility(View.GONE);
            } else {
                changelogView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new MaterialDialog.Builder(getContext())
                                .title(R.string.changes)
                                .content(Html.fromHtml(changes))
                                .positiveText(android.R.string.ok).show();
                    }
                });
            }

            try {
                String version = getActivity().getPackageManager().getPackageInfo(packageName, 0).versionName;
                ((TextView) v.findViewById(R.id.app_version)).setText(version);
            } catch (NameNotFoundException ignored) {
            }

            licensesView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createLicenseDialog();
                }
            });

            sourceCodeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NavUtil.startURL(getActivity(), getString(R.string.about_source));
                }
            });

            if (translator.isEmpty()) {
                translatorsView.setVisibility(View.GONE);
            }

            return v;
        }

        private void createLicenseDialog() {
            Notices notices = new Notices();
            notices.addNotice(new Notice("MD2Widgets", "https://github.com/coxylicacid/MD2Widgets", "Cpoyright (c) 2019-~ Krins (CoxylicAcid)", new MITLicense()));
            notices.addNotice(new Notice("material-dialogs", "https://github.com/afollestad/material-dialogs", "Copyright (c) 2014-2016 Aidan Michael Follestad", new MITLicense()));
            notices.addNotice(new Notice("StickyListHeaders", "https://github.com/emilsjolander/StickyListHeaders", "Emil Sjölander", new ApacheSoftwareLicense20()));
            notices.addNotice(new Notice("PreferenceFragment-Compat", "https://github.com/Machinarius/PreferenceFragment-Compat", "machinarius", new ApacheSoftwareLicense20()));
            notices.addNotice(new Notice("libsuperuser", "https://github.com/Chainfire/libsuperuser", "Copyright (C) 2012-2015 Jorrit \"Chainfire\" Jongma", new ApacheSoftwareLicense20()));
            notices.addNotice(new Notice("picasso", "https://github.com/square/picasso", "Copyright 2013 Square, Inc.", new ApacheSoftwareLicense20()));

            new LicensesDialog.Builder(getActivity())
                    .setNotices(notices)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show();
        }
    }
}