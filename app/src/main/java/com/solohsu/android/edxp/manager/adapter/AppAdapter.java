package com.solohsu.android.edxp.manager.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.solohsu.android.edxp.manager.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.robv.android.xposed.installer.util.ThemeUtil;

import com.solohsu.android.edxp.manager.R;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    protected final Context context;
    private final ApplicationInfo.DisplayNameComparator displayNameComparator;
    private List<ApplicationInfo> fullList, showList;
    private List<String> checkedList;
    private PackageManager pm;
    private ApplicationFilter filter;
    private Callback callback;

    public AppAdapter(Context context) {
        this.context = context;
        fullList = showList = Collections.emptyList();
        checkedList = Collections.emptyList();
        filter = new ApplicationFilter();
        pm = context.getPackageManager();
        displayNameComparator = new ApplicationInfo.DisplayNameComparator(pm);
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

    private void loadApps() {
        fullList = pm.getInstalledApplications(0);
        AppHelper.makeSurePath();
        checkedList = generateCheckedList();
        sortApps();
        if (callback != null) {
            callback.onDataReady();
        }
    }

    /**
     * Called during {@link #loadApps()} in non-UI thread.
     *
     * @return list of package names which should be checked when shown
     */
    protected List<String> generateCheckedList() {
        return Collections.emptyList();
    }

    private void sortApps() {
        Collections.sort(fullList, (a, b) -> {
            boolean aChecked = checkedList.contains(a.packageName);
            boolean bChecked = checkedList.contains(b.packageName);
            if (aChecked == bChecked) {
                return displayNameComparator.compare(a, b);
            } else if (aChecked) {
                return -1;
            } else {
                return 1;
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationInfo info = showList.get(position);

        holder.appIcon.setImageDrawable(info.loadIcon(pm));
        holder.appName.setText(Utils.getAppLabel(info, pm));
        holder.appPackage.setText(info.packageName);
        holder.appPackage.setTextColor(ThemeUtil.getThemeColor(context, android.R.attr.textColorSecondary));

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(checkedList.contains(info.packageName));
        holder.checkBox.setOnCheckedChangeListener((v, isChecked) ->
                onCheckedChange(v, isChecked, info));
        holder.infoLayout.setOnClickListener(v -> {
            if (callback != null) {
                callback.onItemClick(v, info);
            }
        });
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
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            infoLayout = itemView.findViewById(R.id.info_layout);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appPackage = itemView.findViewById(R.id.package_name);
            checkBox = itemView.findViewById(R.id.checkbox);
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
                    if (lowercaseContains(Utils.getAppLabel(info, pm), filter)
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
}
