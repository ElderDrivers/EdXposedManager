package org.meowcat.edxposed.manager.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;
import org.meowcat.edxposed.manager.util.InstallApkUtil;
import org.meowcat.edxposed.manager.util.NavUtil;
import org.meowcat.edxposed.manager.util.ThemeUtil;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.meowcat.edxposed.manager.BaseFragment.areYouSure;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> implements Filterable {

    protected final Context context;
    private final ApplicationInfo.DisplayNameComparator displayNameComparator;
    private Callback callback;
    protected List<ApplicationInfo> fullList, showList;
    private final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private List<String> checkedList;
    private final PackageManager pm;
    private final ApplicationFilter filter;
    private Comparator<ApplicationInfo> cmp;
    private static AppAdapter app;
    Intent intent = new Intent();

    AppAdapter(Context context) {
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setAction(Intent.ACTION_MAIN);

        app = this;
        this.context = context;
        fullList = showList = Collections.emptyList();
        checkedList = Collections.emptyList();
        filter = new ApplicationFilter();
        pm = context.getPackageManager();
        displayNameComparator = new ApplicationInfo.DisplayNameComparator(pm);
        cmp = displayNameComparator;
        refresh();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_app, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("NonConstantResourceId")
    public static boolean onOptionsItemSelected(MenuItem item) {
        boolean refresh = false;
        switch (item.getItemId()) {
            case R.id.item_filter_by_name:
                item.setChecked(!item.isChecked());
                XposedApp.getPreferences().edit().putBoolean("item_filter_by_name", item.isChecked()).apply();
                refresh = true;
                break;
            case R.id.item_filter_by_icon:
                item.setChecked(!item.isChecked());
                XposedApp.getPreferences().edit().putBoolean("item_filter_by_icon", item.isChecked()).apply();
                refresh = true;
                break;
            case R.id.item_filter_by_is_overlay:
                item.setChecked(!item.isChecked());
                XposedApp.getPreferences().edit().putBoolean("item_filter_by_is_overlay", item.isChecked()).apply();
                refresh = true;
                break;
            case R.id.item_filter_by_is_system:
                item.setChecked(!item.isChecked());
                XposedApp.getPreferences().edit().putBoolean("item_filter_by_is_system", item.isChecked()).apply();
                refresh = true;
                break;
            case R.id.item_filter_by_is_user:
                item.setChecked(!item.isChecked());
                XposedApp.getPreferences().edit().putBoolean("item_filter_by_is_user", item.isChecked()).apply();
                refresh = true;
                break;
            case R.id.item_filter_by_launcher:
                item.setChecked(!item.isChecked());
                XposedApp.getPreferences().edit().putBoolean("item_filter_by_launcher", item.isChecked()).apply();
                refresh = true;
                break;
            case R.id.item_enabled_top:
                item.setChecked(!item.isChecked());
                XposedApp.getPreferences().edit().putBoolean("enabled_top", item.isChecked()).apply();
                refresh = true;
                break;
            case R.id.item_show_system:
                item.setChecked(!item.isChecked());
                XposedApp.getPreferences().edit().putBoolean("show_system_apps", item.isChecked()).apply();
                refresh = true;
                break;
            case R.id.item_show_modules:
                item.setChecked(!item.isChecked());
                XposedApp.getPreferences().edit().putBoolean("show_modules", item.isChecked()).apply();
                refresh = true;
                break;
            case R.id.item_sort_by_name:
                item.setChecked(true);
                XposedApp.getPreferences().edit().putInt("list_sort", 0).apply();
                refresh = true;
                break;
            case R.id.item_sort_by_name_reverse:
                item.setChecked(true);
                XposedApp.getPreferences().edit().putInt("list_sort", 1).apply();
                refresh = true;
                break;
            case R.id.item_sort_by_package_name:
                item.setChecked(true);
                XposedApp.getPreferences().edit().putInt("list_sort", 2).apply();
                refresh = true;
                break;
            case R.id.item_sort_by_package_name_reverse:
                item.setChecked(true);
                XposedApp.getPreferences().edit().putInt("list_sort", 3).apply();
                refresh = true;
                break;
            case R.id.item_sort_by_install_time:
                item.setChecked(true);
                XposedApp.getPreferences().edit().putInt("list_sort", 4).apply();
                refresh = true;
                break;
            case R.id.item_sort_by_install_time_reverse:
                item.setChecked(true);
                XposedApp.getPreferences().edit().putInt("list_sort", 5).apply();
                refresh = true;
                break;
            case R.id.item_sort_by_update_time:
                item.setChecked(true);
                XposedApp.getPreferences().edit().putInt("list_sort", 6).apply();
                refresh = true;
                break;
            case R.id.item_sort_by_update_time_reverse:
                item.setChecked(true);
                XposedApp.getPreferences().edit().putInt("list_sort", 7).apply();
                refresh = true;
                break;
            case R.id.dexopt_all:
                areYouSure((Activity) app.context, app.context.getString(R.string.dexopt_now) + "\n" + app.context.getString(R.string.take_while_cannot_resore), (d, w) -> new MaterialDialog.Builder(app.context)
                        .title(R.string.dexopt_now)
                        .content(R.string.this_may_take_a_while)
                        .progress(true, 0)
                        .cancelable(false)
                        .showListener(dialog -> new Thread("dexopt") {
                            @Override
                            public void run() {
                                if (!Shell.rootAccess()) {
                                    dialog.dismiss();
                                    NavUtil.showMessage(app.context, app.context.getString(R.string.root_failed));
                                    return;
                                }

                                Shell.su("cmd package bg-dexopt-job").exec();

                                dialog.dismiss();
                                XposedApp.runOnUiThread(() -> Toast.makeText(app.context, R.string.done, Toast.LENGTH_LONG).show());
                            }
                        }.start()).show(), (d, w) -> {
                });
                break;
            case R.id.speed_all:
                areYouSure((Activity) app.context, app.context.getString(R.string.speed_now) + "\n" + app.context.getString(R.string.take_while_cannot_resore), (d, w) ->
                        new MaterialDialog.Builder(app.context)
                                .title(R.string.speed_now)
                                .content(R.string.this_may_take_a_while)
                                .progress(true, 0)
                                .cancelable(false)
                                .showListener(dialog -> new Thread("dex2oat") {
                                    @Override
                                    public void run() {
                                        if (!Shell.rootAccess()) {
                                            dialog.dismiss();
                                            NavUtil.showMessage(app.context, app.context.getString(R.string.root_failed));
                                            return;
                                        }

                                        Shell.su("cmd package compile -m speed -a").exec();

                                        dialog.dismiss();
                                        XposedApp.runOnUiThread(() -> Toast.makeText(app.context, R.string.done, Toast.LENGTH_LONG).show());
                                    }
                                }.start()).show(), (d, w) -> {
                });
                break;
        }
        if (refresh) {
            app.refresh();
        }
        return true;
    }

    /**
     * Called during {@link #loadApps(List<String>)} in non-UI thread.
     *
     * @return list of package names which should be checked when shown
     */
    protected List<String> generateCheckedList() {
        return Collections.emptyList();
    }

    private void sortApps() {
        switch (XposedApp.getPreferences().getInt("list_sort", 0)) {
            case 7:
                cmp = Collections.reverseOrder((ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).lastUpdateTime, pm.getPackageInfo(b.packageName, 0).lastUpdateTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                });
                break;
            case 6:
                cmp = (ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).lastUpdateTime, pm.getPackageInfo(b.packageName, 0).lastUpdateTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                };
                break;
            case 5:
                cmp = Collections.reverseOrder((ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).firstInstallTime, pm.getPackageInfo(b.packageName, 0).firstInstallTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                });
                break;
            case 4:
                cmp = (ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).firstInstallTime, pm.getPackageInfo(b.packageName, 0).firstInstallTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                };
                break;
            case 3:
                cmp = Collections.reverseOrder((a, b) -> a.packageName.compareTo(b.packageName));
                break;
            case 2:
                cmp = (a, b) -> a.packageName.compareTo(b.packageName);
                break;
            case 1:
                cmp = Collections.reverseOrder(displayNameComparator);
                break;
            case 0:
            default:
                cmp = displayNameComparator;
                break;
        }
        fullList.sort((a, b) -> {
            if (XposedApp.getPreferences().getBoolean("enabled_top", true)) {
                boolean aChecked = checkedList.contains(a.packageName);
                boolean bChecked = checkedList.contains(b.packageName);
                if (aChecked == bChecked) {
                    return cmp.compare(a, b);
                } else if (aChecked) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return cmp.compare(a, b);
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationInfo info = showList.get(position);
        holder.appIcon.setImageDrawable(info.loadIcon(pm));
        holder.appName.setText(InstallApkUtil.getAppLabel(info, pm));
        try {
            holder.appVersion.setText(pm.getPackageInfo(info.packageName, 0).versionName);
            holder.appInstallTime.setText(dateformat.format(new Date(pm.getPackageInfo(info.packageName, 0).firstInstallTime)));
            holder.appUpdateTime.setText(dateformat.format(new Date(pm.getPackageInfo(info.packageName, 0).lastUpdateTime)));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        holder.appPackage.setText(info.packageName);
        holder.appPackage.setTextColor(ThemeUtil.getThemeColor(context, android.R.attr.textColorSecondary));

        holder.mSwitch.setOnCheckedChangeListener(null);
        holder.mSwitch.setChecked(checkedList.contains(info.packageName));
        holder.mSwitch.setOnCheckedChangeListener((v, isChecked) ->
                onCheckedChange(v, isChecked, info));
        holder.infoLayout.setOnClickListener(v -> {
            if (callback != null) {
                callback.onItemClick(v, info);
            }
        });
    }

    public static void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_app_list, menu);
        menu.findItem(R.id.item_enabled_top).setChecked(XposedApp.getPreferences().getBoolean("enabled_top", true));
        menu.findItem(R.id.item_show_modules).setChecked(XposedApp.getPreferences().getBoolean("show_modules", true));
        menu.findItem(R.id.item_show_system).setChecked(XposedApp.getPreferences().getBoolean("show_system_apps", true));
        menu.findItem(R.id.item_filter_by_name).setChecked(XposedApp.getPreferences().getBoolean("item_filter_by_name", false));
        menu.findItem(R.id.item_filter_by_icon).setChecked(XposedApp.getPreferences().getBoolean("item_filter_by_icon", false));
        menu.findItem(R.id.item_filter_by_is_overlay).setChecked(XposedApp.getPreferences().getBoolean("item_filter_by_is_overlay", true));
        menu.findItem(R.id.item_filter_by_is_system).setChecked(XposedApp.getPreferences().getBoolean("item_filter_by_is_system", false));
        menu.findItem(R.id.item_filter_by_is_user).setChecked(XposedApp.getPreferences().getBoolean("item_filter_by_is_user", false));
        menu.findItem(R.id.item_filter_by_launcher).setChecked(XposedApp.getPreferences().getBoolean("item_filter_by_launcher", true));
        switch (XposedApp.getPreferences().getInt("list_sort", 0)) {
            case 7:
                menu.findItem(R.id.item_sort_by_update_time_reverse).setChecked(true);
                break;
            case 6:
                menu.findItem(R.id.item_sort_by_update_time).setChecked(true);
                break;
            case 5:
                menu.findItem(R.id.item_sort_by_install_time_reverse).setChecked(true);
                break;
            case 4:
                menu.findItem(R.id.item_sort_by_install_time).setChecked(true);
                break;
            case 3:
                menu.findItem(R.id.item_sort_by_package_name_reverse).setChecked(true);
                break;
            case 2:
                menu.findItem(R.id.item_sort_by_package_name).setChecked(true);
                break;
            case 1:
                menu.findItem(R.id.item_sort_by_name_reverse).setChecked(true);
                break;
            case 0:
                menu.findItem(R.id.item_sort_by_name).setChecked(true);
                break;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            menu.findItem(R.id.menu_optimize).setVisible(false);
        }
    }

    private void loadApps(List<String> removeList) {
        PackageManager manager = context.getPackageManager();
        fullList = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        boolean hide_system = !XposedApp.getPreferences().getBoolean("show_system_apps", true);
        List<ApplicationInfo> rmList = new ArrayList<>();
        for (ApplicationInfo info : fullList) {
            if (removeList.contains(info.packageName)) {
                rmList.add(info);
                continue;
            }
            if (!XposedApp.getPreferences().getBoolean("show_modules", true) || hide_system) {
                if (info.metaData != null && info.metaData.containsKey("xposedmodule") || AppHelper.FORCE_WHITE_LIST_MODULE.contains(info.packageName) || hide_system && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    rmList.add(info);
                    continue;
                }
            }

            if (XposedApp.getPreferences().getBoolean("item_filter_by_name", false)) {
                if (manager.getApplicationLabel(info).toString().equals(info.packageName)) {
                    rmList.add(info);
                    continue;
                }
            }

            if (XposedApp.getPreferences().getBoolean("item_filter_by_icon", false)) {
                if (info.icon == 0) {
                    rmList.add(info);
                    continue;
                }
            }
            if (XposedApp.getPreferences().getBoolean("item_filter_by_is_overlay", true)) {
                try {
                    PackageInfo packageInfo = pm.getPackageInfo(info.packageName, PackageManager.GET_META_DATA);
                    Class<?> clazz = Class.forName(PackageInfo.class.getName());
                    Field field = clazz.getDeclaredField("overlayCategory");
                    Object overlayCategory = field.get(packageInfo);
                    if (overlayCategory != null) {
                        rmList.add(info);
                        continue;
                    }
                } catch (PackageManager.NameNotFoundException | NoSuchFieldException | ClassNotFoundException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            if (XposedApp.getPreferences().getBoolean("item_filter_by_is_system", false)) {
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    rmList.add(info);
                    continue;
                }
            }

            if (XposedApp.getPreferences().getBoolean("item_filter_by_is_user", false)) {
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    rmList.add(info);
                    continue;
                }
            }

            if (XposedApp.getPreferences().getBoolean("item_filter_by_launcher", true)) {
                if (pm.getLaunchIntentForPackage(info.packageName) == null) {
                    rmList.add(info);
                    continue;
                }
            }

            if (this instanceof ActivationScopeAdapter) {
                if (AppHelper.isWhiteListMode()) {
                    List<String> whiteList = AppHelper.getWhiteList();
                    if (!whiteList.contains(info.packageName)) {
                        rmList.add(info);
                        continue;
                    }
                } else {
                    List<String> blackList = AppHelper.getBlackList();
                    if (blackList.contains(info.packageName)) {
                        rmList.add(info);
                        continue;
                    }
                }
                if (info.packageName.equals(((ActivationScopeAdapter) this).modulePackageName)) {
                    rmList.add(info);
                }
            }
        }
        if (rmList.size() > 0) {
            fullList.removeAll(rmList);
        }
        AppHelper.makeSurePath();
        checkedList = generateCheckedList();
        sortApps();
        if (callback != null) {
            callback.onDataReady();
        }
    }

    @Override
    public int getItemCount() {
        return showList.size();
    }

    public void filter(String constraint) {
        filter.filter(constraint);
    }

    public void refresh() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(this::loadApps);
    }

    public void refresh(List<String> removeList) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> loadApps(removeList));
    }

    private void loadApps() {
        loadApps(Collections.emptyList());
    }

    protected void onCheckedChange(CompoundButton buttonView, boolean isChecked, ApplicationInfo info) {
        // override this to implements your functions
    }

    public interface Callback {
        void onDataReady();

        void onItemClick(View v, ApplicationInfo info);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View infoLayout;
        ImageView appIcon;
        TextView appName;
        TextView appPackage;
        TextView appVersion;
        TextView appInstallTime;
        TextView appUpdateTime;
        Switch mSwitch;

        ViewHolder(View itemView) {
            super(itemView);
            infoLayout = itemView.findViewById(R.id.info_layout);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appPackage = itemView.findViewById(R.id.package_name);
            appVersion = itemView.findViewById(R.id.version_name);
            appInstallTime = itemView.findViewById(R.id.tvInstallTime);
            appUpdateTime = itemView.findViewById(R.id.tvUpdateTime);
            mSwitch = itemView.findViewById(R.id.checkbox);
        }
    }

    class ApplicationFilter extends Filter {

        private boolean lowercaseContains(String s, CharSequence filter) {
            return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint == null || constraint.length() == 0) {
                showList = fullList;
            } else {
                showList = new ArrayList<>();
                String filter = constraint.toString().toLowerCase();
                for (ApplicationInfo info : fullList) {
                    if (lowercaseContains(InstallApkUtil.getAppLabel(info, pm), filter)
                            || lowercaseContains(info.packageName, filter)) {
                        showList.add(info);
                    }
                }
            }
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }

    @Override
    public Filter getFilter() {
        return new ApplicationFilter();
    }

}
