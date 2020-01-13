package org.meowcat.edxposed.manager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import org.meowcat.edxposed.manager.repo.Module;
import org.meowcat.edxposed.manager.util.PrefixedSharedPreferences;
import org.meowcat.edxposed.manager.util.RepoLoader;

import java.util.Map;

public class DownloadDetailsSettingsFragment extends BasePreferenceFragment {
    private DownloadDetailsActivity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (DownloadDetailsActivity) activity;
    }

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        final Module module = mActivity.getModule();
        if (module == null)
            return;

        final String packageName = module.packageName;

        PreferenceManager prefManager = getPreferenceManager();
        prefManager.setSharedPreferencesName("module_settings");
        PrefixedSharedPreferences.injectToPreferenceManager(prefManager, module.packageName);
        addPreferencesFromResource(R.xml.module_prefs);

        SharedPreferences prefs = getActivity().getSharedPreferences("module_settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (prefs.getBoolean("no_global", true)) {
            for (Map.Entry<String, ?> k : prefs.getAll().entrySet()) {
                if (prefs.getString(k.getKey(), "").equals("global")) {
                    editor.putString(k.getKey(), "").apply();
                }
            }

            editor.putBoolean("no_global", false).apply();
        }

        findPreference("release_type").setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    RepoLoader.getInstance().setReleaseTypeLocal(packageName, (String) newValue);
                    return true;
                });
    }
}