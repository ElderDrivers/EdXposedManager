package org.meowcat.edxposed.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.repo.Module;
import org.meowcat.edxposed.manager.repo.ModuleVersion;
import org.meowcat.edxposed.manager.util.DownloadsUtil;
import org.meowcat.edxposed.manager.util.InstallApkUtil;
import org.meowcat.edxposed.manager.util.RepoLoader;
import org.meowcat.edxposed.manager.util.ThemeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.meowcat.edxposed.manager.XposedApp.WRITE_EXTERNAL_PERMISSION;
import static org.meowcat.edxposed.manager.XposedApp.darkenColor;

public class ModulesBookmark extends XposedBaseActivity {

    private static RepoLoader mRepoLoader;
    @SuppressLint("StaticFieldLeak")
    private static View container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_container);

        mRepoLoader = RepoLoader.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(view -> finish());

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

    public static class ModulesBookmarkFragment extends Fragment implements AdapterView.OnItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

        private List<Module> mBookmarkedModules = new ArrayList<>();
        private BookmarkModuleAdapter mAdapter;
        private SharedPreferences mBookmarksPref;
        private boolean changed;
        private MenuItem mClickedMenuItem = null;
        private ListView mListView;
        private View mBackgroundList;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mBookmarksPref = requireActivity().getSharedPreferences("bookmarks", MODE_PRIVATE);
            mBookmarksPref.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();

            if (changed)
                getModules();

            requireActivity().getWindow().setStatusBarColor(darkenColor(XposedApp.getColor(requireActivity()), 0.85f));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mBookmarksPref.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            getListView().setDivider(null);
            getListView().setDividerHeight(getDp(6));
            getListView().setPadding(getDp(8), getDp(8), getDp(8), getDp(8));
            getListView().setOnItemClickListener(this);
            getListView().setClipToPadding(false);
            getListView().setEmptyView(mBackgroundList);
            registerForContextMenu(getListView());

            mAdapter = new BookmarkModuleAdapter(getActivity());
            getModules();
            getListView().setAdapter(mAdapter);

            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.list_fragment, container, false);

            mListView = view.findViewById(android.R.id.list);

            mBackgroundList = view.findViewById(R.id.background_list);
            ((ImageView) view.findViewById(R.id.background_list_iv)).setImageResource(R.drawable.ic_bookmark);
            ((TextView) view.findViewById(R.id.list_status)).setText(R.string.no_bookmark_added);

            return view;
        }

        private void getModules() {
            mAdapter.clear();
            mBookmarkedModules.clear();
            for (String s : mBookmarksPref.getAll().keySet()) {
                boolean isBookmarked = mBookmarksPref.getBoolean(s, false);

                if (isBookmarked) {
                    Module m = mRepoLoader.getModule(s);
                    if (m != null) mBookmarkedModules.add(m);
                }
            }
            Collections.sort(mBookmarkedModules, (mod1, mod2) -> mod1.name.compareTo(mod2.name));
            mAdapter.addAll(mBookmarkedModules);
            mAdapter.notifyDataSetChanged();
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
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            changed = true;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            Module module = getItemFromContextMenuInfo(menuInfo);
            if (module == null)
                return;

            menu.setHeaderTitle(module.name);
            requireActivity().getMenuInflater().inflate(R.menu.context_menu_modules_bookmark, menu);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            final Module module = getItemFromContextMenuInfo(
                    item.getMenuInfo());
            if (module == null)
                return false;

            final String pkg = module.packageName;
            ModuleVersion mv = DownloadsUtil.getStableVersion(module);

            if (mv == null)
                return false;

            mClickedMenuItem = item;

            switch (item.getItemId()) {
                case R.id.install_bookmark:
                    DownloadsUtil.addModule(getContext(), module.name, mv.downloadLink, false, (context, info) -> new InstallApkUtil(getContext(), info).execute());
                    break;
                case R.id.install_remove_bookmark:
                    DownloadsUtil.addModule(getContext(), module.name, mv.downloadLink, false, (context, info) -> {
                        new InstallApkUtil(getContext(), info).execute();
                        remove(pkg);
                    });
                    break;
                case R.id.download_bookmark:
                    if (checkPermissions())
                        return false;

                    DownloadsUtil.addModule(getContext(), module.name, mv.downloadLink, true, (context, info) -> Toast.makeText(context, getString(R.string.module_saved, info.localFilename), Toast.LENGTH_SHORT).show());
                    break;
                case R.id.download_remove_bookmark:
                    if (checkPermissions())
                        return false;

                    DownloadsUtil.addModule(getContext(), module.name, mv.downloadLink, true, (context, info) -> {
                        remove(pkg);
                        Toast.makeText(context, getString(R.string.module_saved, info.localFilename), Toast.LENGTH_SHORT).show();
                    });
                    break;
                case R.id.remove:
                    remove(pkg);
                    break;
            }

            return false;
        }

        private boolean checkPermissions() {
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
                        new Handler().postDelayed(() -> onContextItemSelected(mClickedMenuItem), 500);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
                }
            }
        }

        private void remove(final String pkg) {
            mBookmarksPref.edit().putBoolean(pkg, false).apply();

            Snackbar.make(container, R.string.bookmark_removed, Snackbar.LENGTH_SHORT).setAction(R.string.undo, v -> {
                mBookmarksPref.edit().putBoolean(pkg, true).apply();

                getModules();
            }).show();

            getModules();
        }

        private Module getItemFromContextMenuInfo(ContextMenu.ContextMenuInfo menuInfo) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            int position = info.position - getListView().getHeaderViewsCount();
            return (position >= 0) ? (Module) getListView().getAdapter().getItem(position) : null;
        }

        ListView getListView() {
            return mListView;
        }
    }

    private static class BookmarkModuleAdapter extends ArrayAdapter<Module> {
        BookmarkModuleAdapter(Context context) {
            super(context, R.layout.list_item_module, R.id.title);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            view.findViewById(R.id.checkbox).setVisibility(View.GONE);
            view.findViewById(R.id.version_name).setVisibility(View.GONE);
            view.findViewById(R.id.icon).setVisibility(View.GONE);

            Module item = getItem(position);

            ((TextView) view.findViewById(R.id.title)).setText(Objects.requireNonNull(item).name);
            ((TextView) view.findViewById(R.id.description))
                    .setText(item.summary);

            return view;
        }
    }
}