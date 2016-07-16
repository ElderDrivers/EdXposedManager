package de.robv.android.xposed.installer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.inquiry.Inquiry;
import com.afollestad.inquiry.callbacks.GetCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.robv.android.xposed.installer.repo.Module;
import de.robv.android.xposed.installer.repo.ModuleVersion;
import de.robv.android.xposed.installer.util.DownloadsUtil;
import de.robv.android.xposed.installer.util.InstallApkUtil;
import de.robv.android.xposed.installer.util.RepoLoader;
import de.robv.android.xposed.installer.util.ThemeUtil;

import static de.robv.android.xposed.installer.XposedApp.WRITE_EXTERNAL_PERMISSION;
import static de.robv.android.xposed.installer.XposedApp.darkenColor;

public class ModulesBookmark extends XposedBaseActivity {

    private static RepoLoader mRepoLoader;
    private static View container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_container);

        mRepoLoader = RepoLoader.getInstance();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.bookmarks);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setFloating(toolbar, 0);

        container = findViewById(R.id.container);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new ModulesBookmarkFragment()).commit();
        }
    }

    public static class ModulesBookmarkFragment extends ListFragment implements AdapterView.OnItemClickListener {

        private List<Module> mBookmarkedModules = new ArrayList<>();
        private BookmarkModuleAdapter mAdapter;
        private MenuItem mClickedMenuItem = null;

        @Override
        public void onResume() {
            super.onResume();

            if (Build.VERSION.SDK_INT >= 21) {
                getActivity().getWindow().setStatusBarColor(darkenColor(XposedApp.getColor(getActivity()), 0.85f));
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            getListView().setDivider(null);
            getListView().setDividerHeight(getDp(6));
            getListView().setPadding(getDp(8), getDp(8), getDp(8), getDp(8));
            getListView().setOnItemClickListener(this);
            getListView().setClipToPadding(false);
            registerForContextMenu(getListView());
            setEmptyText(getString(R.string.no_bookmark_added));

            mAdapter = new BookmarkModuleAdapter(getContext());
            getModules();

            setHasOptionsMenu(true);
        }

        private void getModules() {
            mAdapter.clear();
            mBookmarkedModules.clear();

            Inquiry.get().selectFrom("bookmarks", Module.class).all(new GetCallback<Module>() {
                @Override
                public void result(Module[] result) {
                    if (result == null || result.length == 0) return;

                    mBookmarkedModules.addAll(Arrays.asList(result));

                    Collections.sort(mBookmarkedModules, new Comparator<Module>() {
                        @Override
                        public int compare(Module mod1, Module mod2) {
                            return mod1.name.compareTo(mod2.name);
                        }
                    });
                    mAdapter.addAll(mBookmarkedModules);
                    setListAdapter(mAdapter);
                }
            });
        }

        private int getDp(float value) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();

            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent detailsIntent = new Intent(getActivity(), DownloadDetailsActivity.class);
            detailsIntent.setData(Uri.fromParts("package", mBookmarkedModules.get(position).packageName, null));
            startActivity(detailsIntent);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            Module module = getItemFromContextMenuInfo(menuInfo);
            if (module == null)
                return;

            menu.setHeaderTitle(module.name);
            getActivity().getMenuInflater().inflate(R.menu.context_menu_modules_bookmark, menu);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            Module module = getItemFromContextMenuInfo(item.getMenuInfo());
            if (module == null) return false;
            module = mRepoLoader.getModule(module.packageName);
            if (module == null) return false;

            final String pkg = module.packageName;
            ModuleVersion mv = DownloadsUtil.getStableVersion(module);

            if (mv == null)
                return false;

            mClickedMenuItem = item;

            switch (item.getItemId()) {
                case R.id.install_bookmark:
                    DownloadsUtil.add(getContext(), module.name, mv.downloadLink, new DownloadsUtil.DownloadFinishedCallback() {
                        @Override
                        public void onDownloadFinished(Context context, DownloadsUtil.DownloadInfo info) {
                            new InstallApkUtil(getContext(), info).execute();
                        }
                    }, DownloadsUtil.MIME_TYPES.APK);
                    break;
                case R.id.install_remove_bookmark:
                    DownloadsUtil.add(getContext(), module.name, mv.downloadLink, new DownloadsUtil.DownloadFinishedCallback() {
                        @Override
                        public void onDownloadFinished(Context context, DownloadsUtil.DownloadInfo info) {
                            new InstallApkUtil(getContext(), info).execute();
                            remove(pkg);
                        }
                    }, DownloadsUtil.MIME_TYPES.APK);
                    break;
                case R.id.download_bookmark:
                    if (checkPermissions())
                        return false;

                    DownloadsUtil.add(getContext(), module.name, mv.downloadLink, new DownloadsUtil.DownloadFinishedCallback() {
                        @Override
                        public void onDownloadFinished(Context context, DownloadsUtil.DownloadInfo info) {
                            Toast.makeText(context, getString(R.string.module_saved,
                                    info.localFilename), Toast.LENGTH_SHORT).show();
                        }
                    }, DownloadsUtil.MIME_TYPES.APK, true, true);
                    break;
                case R.id.download_remove_bookmark:
                    if (checkPermissions())
                        return false;

                    DownloadsUtil.add(getContext(), module.name, mv.downloadLink, new DownloadsUtil.DownloadFinishedCallback() {
                        @Override
                        public void onDownloadFinished(Context context, DownloadsUtil.DownloadInfo info) {
                            remove(pkg);
                            Toast.makeText(context, getString(R.string.module_saved, info.localFilename), Toast.LENGTH_SHORT).show();
                        }
                    }, DownloadsUtil.MIME_TYPES.APK, true, true);
                    break;
                case R.id.remove:
                    remove(pkg);
                    break;
            }

            return false;
        }

        private boolean checkPermissions() {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
                return true;
            }
            return false;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == WRITE_EXTERNAL_PERMISSION) {
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mClickedMenuItem != null) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                onContextItemSelected(mClickedMenuItem);
                            }
                        }, 500);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
                }
            }
        }

        private void remove(final String pkg) {
            Inquiry.get().deleteFrom("bookmarks", Module.class).where("packageName = ?", pkg).run();
            getModules();
        }

        private Module getItemFromContextMenuInfo(ContextMenu.ContextMenuInfo menuInfo) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            int position = info.position - getListView().getHeaderViewsCount();
            return (position >= 0) ? (Module) getListAdapter().getItem(position) : null;
        }
    }

    private static class BookmarkModuleAdapter extends ArrayAdapter<Module> {
        public BookmarkModuleAdapter(Context context) {
            super(context, R.layout.list_item_module, R.id.title);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            view.findViewById(R.id.checkbox).setVisibility(View.GONE);
            view.findViewById(R.id.version_name).setVisibility(View.GONE);
            view.findViewById(R.id.icon).setVisibility(View.GONE);

            Module item = getItem(position);

            ((TextView) view.findViewById(R.id.title)).setText(item.name);
            ((TextView) view.findViewById(R.id.description))
                    .setText(item.summary);

            return view;
        }
    }
}