package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Remove extra reserved space when preference icon is not set, this workaround is from
 * https://stackoverflow.com/a/51568782
 */
public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {

    private void setAllPreferencesToAvoidHavingExtraSpace(Preference preference) {
        preference.setIconSpaceReserved(false);
        if (preference instanceof PreferenceGroup) {
            PreferenceGroup preferenceGroup = ((PreferenceGroup) preference);
            for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
                setAllPreferencesToAvoidHavingExtraSpace(preferenceGroup.getPreference(i));
            }
        }
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != null)
            setAllPreferencesToAvoidHavingExtraSpace(preferenceScreen);
        super.setPreferenceScreen(preferenceScreen);
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        return new PreferenceGroupAdapter(preferenceScreen) {
            @SuppressLint("RestrictedApi")
            @Override
            public void onPreferenceHierarchyChange(Preference preference) {
                if (preference != null)
                    setAllPreferencesToAvoidHavingExtraSpace(preference);
                super.onPreferenceHierarchyChange(preference);
            }
        };
    }
}
