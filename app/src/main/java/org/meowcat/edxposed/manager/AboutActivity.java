package org.meowcat.edxposed.manager;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import org.meowcat.edxposed.manager.util.NavUtil;
import org.meowcat.edxposed.manager.util.ThemeUtil;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.EXTRA_TEXT;

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
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.tab_about, container, false);

            View changelogView = v.findViewById(R.id.changelogView);
            View licensesView = v.findViewById(R.id.licensesView);
            View sourceCodeView = v.findViewById(R.id.sourceCodeView);
            View tgChannelView = v.findViewById(R.id.tgChannelView);
            View installerSupportView = v.findViewById(R.id.installerSupportView);
            View faqView = v.findViewById(R.id.faqView);
            View donateView = v.findViewById(R.id.donateView);
            TextView txtModuleSupport = v.findViewById(R.id.tab_support_module_description);
            View qqGroupView = v.findViewById(R.id.qqGroupView);
            View tgGroupView = v.findViewById(R.id.tgGroupView);

            SharedPreferences prefs = XposedApp.getPreferences();

            final String changes = prefs.getString("changelog", getString(R.string.default_changes_log));

            changelogView.setOnClickListener(v1 -> new MaterialDialog.Builder(requireContext())
                    .title(R.string.changes)
                    .content(Html.fromHtml(changes, Html.FROM_HTML_MODE_LEGACY))
                    .positiveText(R.string.ok).show());

            ((TextView) v.findViewById(R.id.app_version)).setText(BuildConfig.VERSION_NAME);

            licensesView.setOnClickListener(v12 -> {
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.about_libraries_title));
                startActivity(new Intent(getContext(), OssLicensesMenuActivity.class));
            });

            txtModuleSupport.setText(getString(R.string.support_modules_description,
                    getString(R.string.module_support)));

            setupView(installerSupportView, R.string.support_material_xda);
            setupView(faqView, R.string.support_faq_url);
            setupView(tgGroupView, R.string.group_telegram_link);
            setupView(qqGroupView, R.string.group_qq_link);
            setupView(donateView, R.string.support_donate_url);
            setupView(sourceCodeView, R.string.about_source);
            setupView(tgChannelView, R.string.group_telegram_channel_link);

            return v;
        }

        void setupView(View v, final int url) {
            v.setOnClickListener(v1 -> NavUtil.startURL(getActivity(), getString(url)));
        }
    }
}