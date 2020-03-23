package org.meowcat.edxposed.manager;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import org.meowcat.edxposed.manager.util.NavUtil;
import org.meowcat.edxposed.manager.util.ThemeUtil;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.EXTRA_TEXT;
import static org.meowcat.edxposed.manager.XposedApp.darkenColor;

public class AboutActivity extends XposedBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_container);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(view -> finish());

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.nav_item_about);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setFloating(toolbar, R.string.details);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new AboutFragment()).commit();
        }
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
            requireActivity().getWindow().setStatusBarColor(darkenColor(XposedApp.getColor(requireActivity()), 0.85f));
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.tab_about, container, false);

            View changelogView = v.findViewById(R.id.changelogView);
            View licensesView = v.findViewById(R.id.licensesView);
            View translatorsView = v.findViewById(R.id.translatorsView);
            View sourceCodeView = v.findViewById(R.id.sourceCodeView);
            View tgChannelView = v.findViewById(R.id.tgChannelView);
            View installerSupportView = v.findViewById(R.id.installerSupportView);
            View faqView = v.findViewById(R.id.faqView);
            View donateView = v.findViewById(R.id.donateView);
            TextView txtModuleSupport = v.findViewById(R.id.tab_support_module_description);
            View qqGroupView = v.findViewById(R.id.qqGroupView);
            View tgGroupView = v.findViewById(R.id.tgGroupView);

            String packageName = requireActivity().getPackageName();
            String translator = getResources().getString(R.string.translator);

            SharedPreferences prefs = requireContext().getSharedPreferences(packageName + "_preferences", MODE_PRIVATE);

            final String changes = prefs.getString("changelog", null);

            if (changes == null) {
                changelogView.setVisibility(View.GONE);
            } else {
                changelogView.setOnClickListener(v1 -> new MaterialDialog.Builder(requireContext())
                        .title(R.string.changes)
                        .content(Html.fromHtml(changes))
                        .positiveText(R.string.ok).show());
            }

            try {
                String version = requireActivity().getPackageManager().getPackageInfo(packageName, 0).versionName;
                ((TextView) v.findViewById(R.id.app_version)).setText(version);
            } catch (NameNotFoundException ignored) {
            }

            licensesView.setOnClickListener(v12 -> createLicenseDialog());

            txtModuleSupport.setText(getString(R.string.support_modules_description,
                    getString(R.string.module_support)));

            setupView(installerSupportView, R.string.support_material_xda);
            setupView(faqView, R.string.support_faq_url);
            setupView(tgGroupView, R.string.group_telegram_link);
            setupView(qqGroupView, R.string.group_qq_link);
            setupView(donateView, R.string.support_donate_url);
            setupView(sourceCodeView, R.string.about_source);
            setupView(tgChannelView, R.string.group_telegram_channel_link);

            if (translator.isEmpty()) {
                translatorsView.setVisibility(View.GONE);
            }

            return v;
        }

        void setupView(View v, final int url) {
            v.setOnClickListener(v1 -> NavUtil.startURL(getActivity(), getString(url)));
        }

        private void createLicenseDialog() {
            Notices notices = new Notices();
            notices.addNotice(new Notice("material-dialogs", "https://github.com/afollestad/material-dialogs", "Copyright (c) 2014-2016 Aidan Michael Follestad", new MITLicense()));
            notices.addNotice(new Notice("StickyListHeaders", "https://github.com/emilsjolander/StickyListHeaders", "Emil Sjölander", new ApacheSoftwareLicense20()));
            notices.addNotice(new Notice("PreferenceFragment-Compat", "https://github.com/Machinarius/PreferenceFragment-Compat", "machinarius", new ApacheSoftwareLicense20()));
            notices.addNotice(new Notice("libsuperuser", "https://github.com/Chainfire/libsuperuser", "Copyright (C) 2012-2015 Jorrit \"Chainfire\" Jongma", new ApacheSoftwareLicense20()));
            notices.addNotice(new Notice("picasso", "https://github.com/square/picasso", "Copyright 2013 Square, Inc.", new ApacheSoftwareLicense20()));

            new LicensesDialog.Builder(requireActivity())
                    .setNotices(notices)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show();
        }
    }
}