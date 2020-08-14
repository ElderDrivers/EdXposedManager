package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import org.meowcat.edxposed.manager.repo.Module;
import org.meowcat.edxposed.manager.repo.ModuleVersion;
import org.meowcat.edxposed.manager.repo.ReleaseType;
import org.meowcat.edxposed.manager.repo.RepoParser;
import org.meowcat.edxposed.manager.util.DownloadsUtil;
import org.meowcat.edxposed.manager.util.HashUtil;
import org.meowcat.edxposed.manager.util.InstallApkUtil;
import org.meowcat.edxposed.manager.util.ModuleUtil.InstalledModule;
import org.meowcat.edxposed.manager.util.RepoLoader;
import org.meowcat.edxposed.manager.util.ThemeUtil;
import org.meowcat.edxposed.manager.util.chrome.LinkTransformationMethod;
import org.meowcat.edxposed.manager.widget.DownloadView;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

import static org.meowcat.edxposed.manager.XposedApp.WRITE_EXTERNAL_PERMISSION;

public class DownloadDetailsVersionsFragment extends ListFragment {
    private DownloadDetailsActivity mActivity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (DownloadDetailsActivity) context;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Module module = mActivity.getModule();
        if (module == null)
            return;

        if (module.versions.isEmpty()) {
            setEmptyText(getString(R.string.download_no_versions));
            setListShown(true);
        } else {
            RepoLoader repoLoader = RepoLoader.getInstance();
            if (!repoLoader.isVersionShown(module.versions.get(0))) {
                TextView txtHeader = new TextView(getActivity());
                txtHeader.setText(R.string.download_test_version_not_shown);
                txtHeader.setTextColor(getResources().getColor(R.color.warning, null));
                txtHeader.setOnClickListener(v -> mActivity.gotoPage(DownloadDetailsActivity.DOWNLOAD_SETTINGS));
                getListView().addHeaderView(txtHeader);
            }

            VersionsAdapter sAdapter = new VersionsAdapter(mActivity, mActivity.getInstalledModule());
            for (ModuleVersion version : module.versions) {
                if (repoLoader.isVersionShown(version))
                    sAdapter.add(version);
            }
            setListAdapter(sAdapter);
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int sixDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, metrics);
        int eightDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
        getListView().setDivider(null);
        getListView().setDividerHeight(sixDp);
        getListView().setPadding(eightDp, eightDp, eightDp, eightDp);
        getListView().setClipToPadding(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setListAdapter(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                DownloadView.mClickedButton.performClick();
            } else {
                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }
    }

    static class ViewHolder {
        TextView txtStatus;
        TextView txtVersion;
        TextView txtRelType;
        TextView txtUploadDate;
        DownloadView downloadView;
        TextView txtChangesTitle;
        TextView txtChanges;
    }

    public static class DownloadModuleCallback implements DownloadsUtil.DownloadFinishedCallback {
        private final ModuleVersion moduleVersion;

        DownloadModuleCallback(ModuleVersion moduleVersion) {
            this.moduleVersion = moduleVersion;
        }

        @Override
        public void onDownloadFinished(Context context, DownloadsUtil.DownloadInfo info) {
            File localFile = new File(info.localFilename);

            if (!localFile.isFile())
                return;

            if (moduleVersion.md5sum != null && !moduleVersion.md5sum.isEmpty()) {
                try {
                    String actualMd5Sum = HashUtil.md5(localFile);
                    if (!moduleVersion.md5sum.equals(actualMd5Sum)) {
                        Toast.makeText(context, context.getString(R.string.download_md5sum_incorrect, actualMd5Sum, moduleVersion.md5sum), Toast.LENGTH_LONG).show();
                        DownloadsUtil.removeById(context, info.id);
                        return;
                    }
                } catch (Exception e) {
                    Toast.makeText(context, context.getString(R.string.download_could_not_read_file, e.getMessage()), Toast.LENGTH_LONG).show();
                    DownloadsUtil.removeById(context, info.id);
                    return;
                }
            }

            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(info.localFilename, 0);

            if (packageInfo == null) {
                Toast.makeText(context, R.string.download_no_valid_apk, Toast.LENGTH_LONG).show();
                DownloadsUtil.removeById(context, info.id);
                return;
            }

            if (!packageInfo.packageName.equals(moduleVersion.module.packageName)) {
                Toast.makeText(context, context.getString(R.string.download_incorrect_package_name, packageInfo.packageName, moduleVersion.module.packageName), Toast.LENGTH_LONG).show();
                DownloadsUtil.removeById(context, info.id);
                return;
            }

            new InstallApkUtil(context, info).execute();
        }
    }

    private class VersionsAdapter extends ArrayAdapter<ModuleVersion> {
        private final DateFormat mDateFormatter = DateFormat
                .getDateInstance(DateFormat.SHORT);
        private final int mColorRelTypeStable;
        private final int mColorRelTypeOthers;
        private final int mColorInstalled;
        private final int mColorUpdateAvailable;
        private final String mTextInstalled;
        private final String mTextUpdateAvailable;
        private final long mInstalledVersionCode;

        VersionsAdapter(Context context, InstalledModule installed) {
            super(context, R.layout.list_item_version);
            mColorRelTypeStable = ThemeUtil.getThemeColor(context, android.R.attr.textColorTertiary);
            mColorRelTypeOthers = getResources().getColor(R.color.warning, null);
            mColorInstalled = ThemeUtil.getThemeColor(context, R.attr.download_status_installed);
            mColorUpdateAvailable = getResources().getColor(R.color.download_status_update_available, null);
            mTextInstalled = getString(R.string.download_section_installed) + ":";
            mTextUpdateAvailable = getString(R.string.download_section_update_available) + ":";
            mInstalledVersionCode = (installed != null) ? installed.versionCode : -1;
        }

        @SuppressLint("InflateParams")
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.list_item_version, null, true);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.txtStatus = view.findViewById(R.id.txtStatus);
                viewHolder.txtVersion = view.findViewById(R.id.txtVersion);
                viewHolder.txtRelType = view.findViewById(R.id.txtRelType);
                viewHolder.txtUploadDate = view.findViewById(R.id.txtUploadDate);
                viewHolder.downloadView = view.findViewById(R.id.downloadView);
                viewHolder.txtChangesTitle = view.findViewById(R.id.txtChangesTitle);
                viewHolder.txtChanges = view.findViewById(R.id.txtChanges);
                viewHolder.downloadView.fragment = DownloadDetailsVersionsFragment.this;
                view.setTag(viewHolder);
            }

            ViewHolder holder = (ViewHolder) view.getTag();
            ModuleVersion item = getItem(position);

            holder.txtVersion.setText(Objects.requireNonNull(item).name);
            holder.txtRelType.setText(item.relType.getTitleId());
            holder.txtRelType.setTextColor(item.relType == ReleaseType.STABLE
                    ? mColorRelTypeStable : mColorRelTypeOthers);

            if (item.uploaded > 0) {
                holder.txtUploadDate.setText(
                        mDateFormatter.format(new Date(item.uploaded)));
                holder.txtUploadDate.setVisibility(View.VISIBLE);
            } else {
                holder.txtUploadDate.setVisibility(View.GONE);
            }

            if (item.code <= 0 || mInstalledVersionCode <= 0
                    || item.code < mInstalledVersionCode) {
                holder.txtStatus.setVisibility(View.GONE);
            } else if (item.code == mInstalledVersionCode) {
                holder.txtStatus.setText(mTextInstalled);
                holder.txtStatus.setTextColor(mColorInstalled);
                holder.txtStatus.setVisibility(View.VISIBLE);
            } else { // item.code > mInstalledVersionCode
                holder.txtStatus.setText(mTextUpdateAvailable);
                holder.txtStatus.setTextColor(mColorUpdateAvailable);
                holder.txtStatus.setVisibility(View.VISIBLE);
            }

            holder.downloadView.setUrl(item.downloadLink);
            holder.downloadView.setTitle(mActivity.getModule().name);
            holder.downloadView.setDownloadFinishedCallback(new DownloadModuleCallback(item));

            if (item.changelog != null && !item.changelog.isEmpty()) {
                holder.txtChangesTitle.setVisibility(View.VISIBLE);
                holder.txtChanges.setVisibility(View.VISIBLE);

                if (item.changelogIsHtml) {
                    holder.txtChanges.setText(RepoParser.parseSimpleHtml(getActivity(), item.changelog, holder.txtChanges));
                    holder.txtChanges.setTransformationMethod(new LinkTransformationMethod(getActivity()));
                    holder.txtChanges.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    holder.txtChanges.setText(item.changelog);
                    holder.txtChanges.setMovementMethod(null);
                }

            } else {
                holder.txtChangesTitle.setVisibility(View.GONE);
                holder.txtChanges.setVisibility(View.GONE);
            }

            return view;
        }
    }
}