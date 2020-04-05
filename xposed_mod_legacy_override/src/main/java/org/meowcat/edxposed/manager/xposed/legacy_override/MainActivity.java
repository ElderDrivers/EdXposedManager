package org.meowcat.edxposed.manager.xposed.legacy_override;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.collection.SparseArrayCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.PREF_KEY_SHOW_NON_NORMAL_FIRST;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.PREF_KEY_SHOW_SYSTEM;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.PREF_KEY_SORT_ORDER;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.SORT_ORDER_INSTALL_TIME;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.SORT_ORDER_LABEL;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.SORT_ORDER_PACKAGE_NAME;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.SORT_ORDER_UPDATE_TIME;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.STATE_HIDDEN;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.STATE_NORMAL;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.STATE_PRETEND;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private View mRootView;
    private InstalledPackageAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        setContentView(R.layout.activity_main);
        mRootView = findViewById(R.id.root);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final RecyclerView packages = findViewById(R.id.packages);
        packages.setAdapter(mAdapter = new InstalledPackageAdapter());

        updatePackageList();
    }

    private void updatePackageList() {
        final PackageManager pm = getPackageManager();
        final List<PackageInfo> packages = pm.getInstalledPackages(0);
        mAdapter.updateInstalledPackages(packages);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        switch (mSharedPreferences.getInt(PREF_KEY_SORT_ORDER, SORT_ORDER_LABEL)) {
            case SORT_ORDER_LABEL:
                menu.findItem(R.id.action_sort_label).setChecked(true);
                break;
            case SORT_ORDER_PACKAGE_NAME:
                menu.findItem(R.id.action_sort_package_name).setChecked(true);
                break;
            case SORT_ORDER_INSTALL_TIME:
                menu.findItem(R.id.action_sort_install_time).setChecked(true);
                break;
            case SORT_ORDER_UPDATE_TIME:
                menu.findItem(R.id.action_sort_update_time).setChecked(true);
                break;
        }
        menu.findItem(R.id.action_show_non_normal_first)
                .setChecked(mSharedPreferences.getBoolean(PREF_KEY_SHOW_NON_NORMAL_FIRST, false));
        menu.findItem(R.id.action_show_system)
                .setChecked(mSharedPreferences.getBoolean(PREF_KEY_SHOW_SYSTEM, false));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                updatePackageList();
                return true;
            case R.id.action_sort_label:
                mSharedPreferences.edit().putInt(PREF_KEY_SORT_ORDER, SORT_ORDER_LABEL).apply();
                invalidateOptionsMenu();
                return true;
            case R.id.action_sort_package_name:
                mSharedPreferences.edit().putInt(PREF_KEY_SORT_ORDER, SORT_ORDER_PACKAGE_NAME).apply();
                invalidateOptionsMenu();
                return true;
            case R.id.action_sort_install_time:
                mSharedPreferences.edit().putInt(PREF_KEY_SORT_ORDER, SORT_ORDER_INSTALL_TIME).apply();
                invalidateOptionsMenu();
                return true;
            case R.id.action_sort_update_time:
                mSharedPreferences.edit().putInt(PREF_KEY_SORT_ORDER, SORT_ORDER_UPDATE_TIME).apply();
                invalidateOptionsMenu();
                return true;
            case R.id.action_show_non_normal_first:
                mSharedPreferences.edit()
                        .putBoolean(PREF_KEY_SHOW_NON_NORMAL_FIRST, !mSharedPreferences.getBoolean(PREF_KEY_SHOW_NON_NORMAL_FIRST, false))
                        .apply();
                invalidateOptionsMenu();
                return true;
            case R.id.action_show_system:
                mSharedPreferences.edit()
                        .putBoolean(PREF_KEY_SHOW_SYSTEM, !mSharedPreferences.getBoolean(PREF_KEY_SHOW_SYSTEM, false))
                        .apply();
                invalidateOptionsMenu();
                return true;
            case R.id.action_help:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.action_help)
                        .setView(R.layout.dialog_help)
                        .create()
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class InstalledPackageAdapter extends RecyclerView.Adapter<InstalledPackageAdapter.ViewHolder>
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case PREF_KEY_SORT_ORDER:
                case PREF_KEY_SHOW_NON_NORMAL_FIRST:
                case PREF_KEY_SHOW_SYSTEM:
                    filterAndSort();
                    notifyDataSetChanged();
                    break;
            }
        }

        private class PackageInfoCache {
            public PackageInfo raw;
            private CharSequence mLabel = null;
            private Drawable mIcon = null;

            public PackageInfoCache(PackageInfo pkg) {
                raw = pkg;
            }

            public Drawable getIcon(final PackageManager pm) {
                if (mIcon == null)
                    mIcon = pm.getApplicationIcon(raw.applicationInfo);
                return mIcon;
            }

            public CharSequence getLabel(final PackageManager pm) {
                if (mLabel == null)
                    mLabel = pm.getApplicationLabel(raw.applicationInfo);
                return mLabel;
            }

            public String getPackageName() {
                return raw.packageName;
            }

            @Constants.AppState
            public int getState() {
                return Configuration.getState(getUid());
            }

            public boolean isSystemApp() {
                return (raw.applicationInfo.flags & FLAG_SYSTEM) == FLAG_SYSTEM;
            }

            public long getInstallTime() {
                return raw.firstInstallTime;
            }

            public long getUpdateTime() {
                return raw.lastUpdateTime;
            }

            public int getUid() {
                return raw.applicationInfo.uid;
            }
        }

        private List<PackageInfoCache> mInstalledPackages = new ArrayList<>();
        private List<PackageInfoCache> mFilteredPackages = new ArrayList<>();

        InstalledPackageAdapter() {
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(mFilteredPackages.get(position));
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            holder.unbind();
        }

        @Override
        public int getItemCount() {
            return mFilteredPackages.size();
        }

        @Override
        public long getItemId(int position) {
            return mFilteredPackages.get(position).getPackageName().hashCode();
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        public void updateInstalledPackages(List<PackageInfo> packages) {
            final SparseArrayCompat<PackageInfoCache> map = new SparseArrayCompat<>(mInstalledPackages.size());
            for (PackageInfoCache pkg : mInstalledPackages)
                map.put(pkg.getPackageName().hashCode(), pkg);

            mInstalledPackages = new ArrayList<>(packages.size());
            for (PackageInfo pkg : packages) {
                final PackageInfoCache pkg2 = map.containsKey(pkg.packageName.hashCode()) ?
                        map.get(pkg.packageName.hashCode()) :
                        new PackageInfoCache(pkg);
                pkg2.raw = pkg;
                mInstalledPackages.add(pkg2);
            }

            filterAndSort();
            notifyDataSetChanged();
        }

        private void filterAndSort() {
            mFilteredPackages.clear();
            if (mSharedPreferences.getBoolean(PREF_KEY_SHOW_SYSTEM, false)) {
                mFilteredPackages.addAll(mInstalledPackages);
            } else {
                for (PackageInfoCache pkg : mInstalledPackages)
                    if (!pkg.isSystemApp())
                        mFilteredPackages.add(pkg);
            }

            final PackageManager pm = getPackageManager();
            Comparator<PackageInfoCache> orderComparator = null;
            switch (mSharedPreferences.getInt(PREF_KEY_SORT_ORDER, SORT_ORDER_LABEL)) {
                case SORT_ORDER_LABEL:
                    orderComparator = (pkg1, pkg2) -> pkg1.getLabel(pm).toString().compareTo(pkg2.getLabel(pm).toString());
                    break;
                case SORT_ORDER_PACKAGE_NAME:
                    orderComparator = (pkg1, pkg2) -> pkg1.getPackageName().compareTo(pkg2.getPackageName());
                    break;
                case SORT_ORDER_INSTALL_TIME:
                    orderComparator = (pkg1, pkg2) -> Long.compare(pkg1.getInstallTime(), pkg2.getInstallTime());
                    orderComparator = orderComparator.reversed();
                    break;
                case SORT_ORDER_UPDATE_TIME:
                    orderComparator = (pkg1, pkg2) -> Long.compare(pkg1.getUpdateTime(), pkg2.getUpdateTime());
                    orderComparator = orderComparator.reversed();
                    break;
            }

            if (mSharedPreferences.getBoolean(PREF_KEY_SHOW_NON_NORMAL_FIRST, false)) {
                final Comparator<PackageInfoCache> stateComparator = (pkg1, pkg2) -> {
                    if (pkg1.getState() == STATE_NORMAL && pkg2.getState() != STATE_NORMAL)
                        return -1;
                    if (pkg1.getState() != STATE_NORMAL && pkg2.getState() == STATE_NORMAL)
                        return 1;
                    return 0;
                };
                mFilteredPackages.sort(stateComparator.reversed().thenComparing(orderComparator));
            } else {
                mFilteredPackages.sort(orderComparator);
            }
        }

        private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final AppCompatImageView icon;
            private final AppCompatTextView applicationLabel;
            private final AppCompatTextView packageName;
            private final AppCompatImageView toggle;
            private final View highlight;
            private PackageInfoCache pkg;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                icon = itemView.findViewById(R.id.icon);
                applicationLabel = itemView.findViewById(R.id.application_label);
                packageName = itemView.findViewById(R.id.package_name);
                toggle = itemView.findViewById(R.id.toggle);
                highlight = itemView.findViewById(R.id.highlight);
            }

            public void bind(final PackageInfoCache pkg) {
                this.pkg = pkg;
                final PackageManager pm = itemView.getContext().getPackageManager();
                icon.setImageDrawable(pkg.getIcon(pm));
                applicationLabel.setText(pkg.getLabel(pm));
                packageName.setText(pkg.getPackageName());
                switch (pkg.getState()) {
                    case STATE_HIDDEN:
                        toggle.setImageResource(R.drawable.ic_toggle_hidden);
                        break;
                    case STATE_NORMAL:
                        toggle.setImageResource(R.drawable.ic_toggle_normal);
                        break;
                    case STATE_PRETEND:
                        toggle.setImageResource(R.drawable.ic_toggle_incognito);
                        break;
                }
                highlight.setVisibility(pkg.isSystemApp() ? View.VISIBLE : View.INVISIBLE);
            }

            public void unbind() {
                pkg = null;
            }

            @Override
            public void onClick(View view) {
                if (pkg == null)
                    return;
                switch (pkg.getState()) {
                    case STATE_HIDDEN:
                        Configuration.setState(pkg.getUid(), STATE_NORMAL);
                        toggle.setImageResource(R.drawable.ic_toggle_normal);
                        break;
                    case STATE_NORMAL:
                        Configuration.setState(pkg.getUid(), STATE_PRETEND);
                        toggle.setImageResource(R.drawable.ic_toggle_incognito);
                        break;
                    case STATE_PRETEND:
                        Configuration.setState(pkg.getUid(), STATE_HIDDEN);
                        toggle.setImageResource(R.drawable.ic_toggle_hidden);
                        break;
                }
                final Snackbar snackbar = Snackbar.make(mRootView, R.string.restart_required, LENGTH_SHORT);
                snackbar.setAction(R.string.dismiss, v -> snackbar.dismiss());
                snackbar.show();
            }
        }
    }
}
