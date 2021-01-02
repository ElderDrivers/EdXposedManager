package org.meowcat.edxposed.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.repo.Module;
import org.meowcat.edxposed.manager.repo.ModuleVersion;
import org.meowcat.edxposed.manager.repo.ReleaseType;
import org.meowcat.edxposed.manager.repo.RepoDb;
import org.meowcat.edxposed.manager.repo.RepoDb.RowNotFoundException;
import org.meowcat.edxposed.manager.util.DownloadsUtil;
import org.meowcat.edxposed.manager.util.InstallApkUtil;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.ModuleUtil.InstalledModule;
import org.meowcat.edxposed.manager.util.ModuleUtil.ModuleListener;
import org.meowcat.edxposed.manager.util.NavUtil;
import org.meowcat.edxposed.manager.util.RepoLoader;
import org.meowcat.edxposed.manager.util.ThemeUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import static androidx.constraintlayout.widget.Constraints.TAG;
import static de.robv.android.xposed.installer.XposedApp.getActiveXposedVersion;
import static org.meowcat.edxposed.manager.XposedApp.WRITE_EXTERNAL_PERMISSION;
import static org.meowcat.edxposed.manager.XposedApp.createFolder;

public class ModulesFragment extends BaseFragment implements ModuleListener, AdapterView.OnItemClickListener {
    public static final String SETTINGS_CATEGORY = "de.robv.android.xposed.category.MODULE_SETTINGS";
    static final String XPOSED_REPO_LINK = "http://repo.xposed.info/module/%s";
    static final String PLAY_STORE_PACKAGE = "com.android.vending";
    static final String PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=%s";
    private static final String NOT_ACTIVE_NOTE_TAG = "NOT_ACTIVE_NOTE";
    private int installedXposedVersion;
    private ApplicationFilter filter;
    private SearchView mSearchView;
    private SearchView.OnQueryTextListener mSearchListener;
    private PackageManager mPm;
    private Comparator<ApplicationInfo> cmp;
    private ApplicationInfo.DisplayNameComparator displayNameComparator;
    private DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private ModuleUtil mModuleUtil;
    private ModuleAdapter mAdapter = null;
    private Runnable reloadModules = new Runnable() {
        public void run() {
            mAdapter.setNotifyOnChange(false);
            mAdapter.clear();
            String queryStr = mSearchView != null ? mSearchView.getQuery().toString() : "";
            Collection<InstalledModule> showList;
            Collection<InstalledModule> fullList = mModuleUtil.getModules().values();
            if (queryStr.length() == 0) {
                showList = fullList;
            } else {
                showList = new ArrayList<>();
                String filter = queryStr.toLowerCase();
                for (InstalledModule info : fullList) {
                    if (lowercaseContains(InstallApkUtil.getAppLabel(info.app, mPm), filter)
                            || lowercaseContains(info.packageName, filter)) {
                        showList.add(info);
                    }
                }
            }
            mAdapter.addAll(showList);
            switch (XposedApp.getPreferences().getInt("list_sort", 0)) {
                case 7:
                    cmp = Collections.reverseOrder((ApplicationInfo a, ApplicationInfo b) -> {
                        try {
                            return Long.compare(mPm.getPackageInfo(a.packageName, 0).lastUpdateTime, mPm.getPackageInfo(b.packageName, 0).lastUpdateTime);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                            return displayNameComparator.compare(a, b);
                        }
                    });
                    break;
                case 6:
                    cmp = (ApplicationInfo a, ApplicationInfo b) -> {
                        try {
                            return Long.compare(mPm.getPackageInfo(a.packageName, 0).lastUpdateTime, mPm.getPackageInfo(b.packageName, 0).lastUpdateTime);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                            return displayNameComparator.compare(a, b);
                        }
                    };
                    break;
                case 5:
                    cmp = Collections.reverseOrder((ApplicationInfo a, ApplicationInfo b) -> {
                        try {
                            return Long.compare(mPm.getPackageInfo(a.packageName, 0).firstInstallTime, mPm.getPackageInfo(b.packageName, 0).firstInstallTime);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                            return displayNameComparator.compare(a, b);
                        }
                    });
                    break;
                case 4:
                    cmp = (ApplicationInfo a, ApplicationInfo b) -> {
                        try {
                            return Long.compare(mPm.getPackageInfo(a.packageName, 0).firstInstallTime, mPm.getPackageInfo(b.packageName, 0).firstInstallTime);
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
            mAdapter.sort((lhs, rhs) -> {
                if (XposedApp.getPreferences().getBoolean("enabled_top", true)) {
                    boolean aChecked = mModuleUtil.isModuleEnabled(lhs.packageName);
                    boolean bChecked = mModuleUtil.isModuleEnabled(rhs.packageName);
                    if (aChecked == bChecked) {
                        return cmp.compare(lhs.app, rhs.app);
                    } else if (aChecked) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    return cmp.compare(lhs.app, rhs.app);
                }
            });
            mAdapter.notifyDataSetChanged();
            mModuleUtil.updateModulesList(false, null);
        }
    };
    private MenuItem mClickedMenuItem = null;
    private ListView mListView;
    private View mBackgroundList;

    public ModulesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filter = new ApplicationFilter();
        mModuleUtil = ModuleUtil.getInstance();
        mPm = requireActivity().getPackageManager();
        displayNameComparator = new ApplicationInfo.DisplayNameComparator(mPm);
        cmp = displayNameComparator;
    }

    private void filter(String constraint) {
        filter.filter(constraint);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        installedXposedVersion = XposedApp.getActiveXposedVersion();
        if (installedXposedVersion <= 0) {
            addHeader();
        }
        mAdapter = new ModuleAdapter(getActivity());
        reloadModules.run();
        getListView().setAdapter(mAdapter);
        mModuleUtil.addListener(this);
        ActionBar actionBar = ((WelcomeActivity) requireActivity()).getSupportActionBar();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int sixDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, metrics);
        int eightDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
        assert actionBar != null;
        int toolBarDp = actionBar.getHeight() == 0 ? 196 : actionBar.getHeight();

        getListView().setDivider(null);
        getListView().setDividerHeight(sixDp);
        getListView().setPadding(eightDp, toolBarDp + eightDp, eightDp, eightDp);
        getListView().setClipToPadding(false);
        getListView().setOnItemClickListener(this);
        getListView().setEmptyView(mBackgroundList);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_fragment, container, false);

        mListView = view.findViewById(android.R.id.list);

        mBackgroundList = view.findViewById(R.id.background_list);
        ((ImageView) view.findViewById(R.id.background_list_iv)).setImageResource(R.drawable.ic_nav_modules);
        ((TextView) view.findViewById(R.id.list_status)).setText(R.string.no_xposed_modules_found);

        mSearchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return false;
            }
        };
        return view;
    }

    private void addHeader() {
        View notActiveNote = requireActivity().getLayoutInflater().inflate(R.layout.xposed_not_active_note, getListView(), false);
        Button mSettingsButton = notActiveNote.findViewById(R.id.btnSettings);
        mSettingsButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));
        notActiveNote.setTag(NOT_ACTIVE_NOTE_TAG);
        getListView().addHeaderView(notActiveNote);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_modules, menu);
        mSearchView = (SearchView) menu.findItem(R.id.app_search).getActionView();
        mSearchView.setOnQueryTextListener(mSearchListener);

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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
        if (requestCode == WRITE_EXTERNAL_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mClickedMenuItem != null) {
                    new Handler().postDelayed(() -> onOptionsItemSelected(mClickedMenuItem), 500);
                }
            } else {
                Snackbar.make(requireView(), R.string.permissionNotGranted, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.bookmarks) {
            startActivity(new Intent(getActivity(), ModulesBookmark.class));
            return true;
        }

        boolean refresh = false;
        File enabledModulesPath = new File(createFolder(), "enabled_modules.list");
        File installedModulesPath = new File(createFolder(), "installed_modules.list");
        File listModules = new File(XposedApp.ENABLED_MODULES_LIST_FILE);

        mClickedMenuItem = item;

        switch (item.getItemId()) {
            case R.id.refresh:
                refresh = true;
                break;
            case R.id.export_enabled_modules:
                if (checkPermissions())
                    return false;

                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    return false;
                }

                if (ModuleUtil.getInstance().getEnabledModules().isEmpty()) {
                    Snackbar.make(requireView(), R.string.no_enabled_modules, Snackbar.LENGTH_SHORT).show();
                    return false;
                }

                try {
                    createFolder();

                    FileInputStream in = new FileInputStream(listModules);
                    FileOutputStream out = new FileOutputStream(enabledModulesPath);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    Snackbar.make(requireView(), getResources().getString(R.string.logs_save_failed) + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    Log.e(TAG, "ModulesFragment: ", e);
                    return false;
                }

                Snackbar.make(requireView(), enabledModulesPath.toString(), Snackbar.LENGTH_LONG).show();
                return true;
            case R.id.export_installed_modules:
                if (checkPermissions())
                    return false;

                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Snackbar.make(requireView(), R.string.sdcard_not_writable, Snackbar.LENGTH_LONG).show();
                    return false;
                }
                Map<String, InstalledModule> installedModules = ModuleUtil.getInstance().getModules();

                if (installedModules.isEmpty()) {
                    Snackbar.make(requireView(), R.string.sdcard_not_writable, Snackbar.LENGTH_SHORT).show();
                    return false;
                }

                try {
                    createFolder();

                    FileWriter fw = new FileWriter(installedModulesPath);
                    BufferedWriter bw = new BufferedWriter(fw);
                    PrintWriter fileOut = new PrintWriter(bw);

                    Set<String> keys = installedModules.keySet();
                    for (String key1 : keys) {
                        fileOut.println(key1);
                    }

                    fileOut.close();
                } catch (IOException e) {
                    Snackbar.make(requireView(), getResources().getString(R.string.logs_save_failed) + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    Log.e(TAG, "ModulesFragment: ", e);
                    return false;
                }

                Snackbar.make(requireView(), installedModulesPath.toString(), Snackbar.LENGTH_LONG).show();
                return true;
            case R.id.import_installed_modules:
                if (checkPermissions())
                    return false;
                return importModules(installedModulesPath);
            case R.id.import_enabled_modules:
                if (checkPermissions())
                    return false;
                return importModules(enabledModulesPath);
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
        }
        if (refresh) {
            mModuleUtil.updateModulesList(false, null);
            requireActivity().runOnUiThread(reloadModules);
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
            return true;
        }
        return false;
    }

    private boolean importModules(File path) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Snackbar.make(requireView(), R.string.sdcard_not_writable, Snackbar.LENGTH_LONG).show();
            return false;
        }
        InputStream ips = null;
        RepoLoader repoLoader = RepoLoader.getInstance();
        List<Module> list = new ArrayList<>();
        if (!path.exists()) {
            Snackbar.make(requireView(), R.string.no_backup_found, Snackbar.LENGTH_LONG).show();
            return false;
        }
        try {
            ips = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "ModulesFragment -> " + e.getMessage());
        }

        if (path.length() == 0) {
            Snackbar.make(requireView(), R.string.file_is_empty, Snackbar.LENGTH_LONG).show();
            return false;
        }

        try {
            assert ips != null;
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr);
            String line;
            while ((line = br.readLine()) != null) {
                Module m = repoLoader.getModule(line);

                if (m == null) {
                    Snackbar.make(requireView(), R.string.download_details_not_found, Snackbar.LENGTH_LONG).show();
                } else {
                    list.add(m);
                }
            }
            br.close();
        } catch (ActivityNotFoundException | IOException e) {
            Snackbar.make(requireView(), e.toString(), Snackbar.LENGTH_LONG).show();
        }

        for (final Module m : list) {
            ModuleVersion mv = null;
            for (int i = 0; i < m.versions.size(); i++) {
                ModuleVersion mvTemp = m.versions.get(i);

                if (mvTemp.relType == ReleaseType.STABLE) {
                    mv = mvTemp;
                    break;
                }
            }

            if (mv != null) {
                DownloadsUtil.addModule(getContext(), m.name, mv.downloadLink, false, (context, info) -> new InstallApkUtil(getContext(), info).execute());
            }
        }

        ModuleUtil.getInstance().reloadInstalledModules();

        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mModuleUtil.removeListener(this);
        getListView().setAdapter(null);
        mAdapter = null;
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, InstalledModule module) {
        mModuleUtil.updateModulesList(false, null);
        requireActivity().runOnUiThread(reloadModules);
    }

    @Override
    public void onInstalledModulesReloaded(ModuleUtil moduleUtil) {
        mModuleUtil.updateModulesList(false, null);
        requireActivity().runOnUiThread(reloadModules);
    }

    @SuppressLint("RestrictedApi")
    private void showMenu(@NonNull Context context,
                          @NonNull View anchor,
                          @NonNull ApplicationInfo info) {
        PopupMenu appMenu = new PopupMenu(context, anchor);
        appMenu.inflate(R.menu.context_menu_modules);
        InstalledModule installedModule = ModuleUtil.getInstance().getModule(info.packageName);
        if (installedModule == null) {
            return;
        }
        try {
            String support = RepoDb
                    .getModuleSupport(installedModule.packageName);
            if (NavUtil.parseURL(support) == null)
                appMenu.getMenu().removeItem(R.id.menu_support);
        } catch (RowNotFoundException e) {
            appMenu.getMenu().removeItem(R.id.menu_download_updates);
            appMenu.getMenu().removeItem(R.id.menu_support);
        }
        if (getActiveXposedVersion() < 92 || installedModule.packageName.equals(BuildConfig.APPLICATION_ID)) {
//            appMenu.getMenu().removeItem(R.id.menu_activation_scope);
        }
        if (getSettingsIntent(installedModule.packageName) == null) {
            appMenu.getMenu().removeItem(R.id.menu_launch);
        }
        appMenu.setOnMenuItemClickListener(menuItem -> {
            InstalledModule module = ModuleUtil.getInstance().getModule(info.packageName);
            if (module == null) {
                return false;
            }
            switch (menuItem.getItemId()) {
                case R.id.menu_launch:
                    String packageName = module.packageName;
                    if (packageName == null) {
                        return false;
                    }
                    Intent launchIntent = getSettingsIntent(packageName);
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    } else {
                        Snackbar.make(requireView(), R.string.module_no_ui, Snackbar.LENGTH_LONG).show();
                    }
                    break;

                case R.id.menu_download_updates:
                    Intent detailsIntent = new Intent(getActivity(), DownloadDetailsActivity.class);
                    detailsIntent.setData(Uri.fromParts("package", module.packageName, null));
                    startActivity(detailsIntent);
                    break;

                case R.id.menu_support:
                    NavUtil.startURL(getActivity(), Uri.parse(RepoDb.getModuleSupport(module.packageName)));
                    break;

                case R.id.menu_app_store:
                    Uri uri = Uri.parse("market://details?id=" + module.packageName);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(intent);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;

                case R.id.menu_app_info:
                    startActivity(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", module.packageName, null)));
                    break;

                case R.id.menu_uninstall:
                    startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", module.packageName, null)));
                    break;

                case R.id.menu_activation_scope:
                    Intent scopeIntent = new Intent(context, ActivationScopeActivity.class);
                    scopeIntent.putExtra("modulePackageName", module.packageName);
                    scopeIntent.putExtra("moduleName", module.getAppName());
                    startActivity(scopeIntent);
                    break;
            }
            return true;
        });
        MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) appMenu.getMenu(), anchor);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    private Intent getSettingsIntent(String packageName) {
        // taken from
        // ApplicationPackageManager.getLaunchIntentForPackage(String)
        // first looks for an Xposed-specific category, falls back to
        // getLaunchIntentForPackage
        PackageManager pm = requireActivity().getPackageManager();

        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(SETTINGS_CATEGORY);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, 0);

        if (ris == null || ris.size() <= 0) {
            return pm.getLaunchIntentForPackage(packageName);
        }

        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
        return intent;
    }

    private ListView getListView() {
        return mListView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            showMenu(requireActivity(), view, requireContext().getPackageManager().getApplicationInfo((String) view.getTag(), 0));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            String packageName = (String) view.getTag();
            if (packageName == null)
                return;

            Intent launchIntent = getSettingsIntent(packageName);
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Snackbar.make(view, R.string.module_no_ui, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private boolean lowercaseContains(String s, CharSequence filter) {
        return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
    }

    private class ModuleAdapter extends ArrayAdapter<InstalledModule> {
        ModuleAdapter(Context context) {
            super(context, R.layout.list_item_module, R.id.title);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            if (convertView == null) {
                // The reusable view was created for the first time, set up the
                // listener on the checkbox
                ((Switch) view.findViewById(R.id.checkbox)).setOnCheckedChangeListener((buttonView, isChecked) -> {
                    String packageName = (String) buttonView.getTag();
                    boolean changed = mModuleUtil.isModuleEnabled(packageName) ^ isChecked;
                    if (changed) {
                        mModuleUtil.setModuleEnabled(packageName, isChecked);
                        mModuleUtil.updateModulesList(true, view);
                    }
                });
            }

            InstalledModule item = getItem(position);

            TextView version = view.findViewById(R.id.version_name);
            version.setText(Objects.requireNonNull(item).versionName);
            version.setSelected(true);
            version.setTextColor(Color.parseColor("#808080"));

            TextView packageTv = view.findViewById(R.id.package_name);
            packageTv.setText(item.packageName);
            packageTv.setSelected(true);

            TextView installTimeTv = view.findViewById(R.id.tvInstallTime);
            installTimeTv.setText(dateformat.format(new Date(item.installTime)));
            installTimeTv.setSelected(true);

            TextView updateTv = view.findViewById(R.id.tvUpdateTime);
            updateTv.setText(dateformat.format(new Date(item.updateTime)));
            updateTv.setSelected(true);

            // Store the package name in some views' tag for later access
            view.findViewById(R.id.checkbox).setTag(item.packageName);
            view.setTag(item.packageName);

            ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(item.getIcon());

            TextView descriptionText = view.findViewById(R.id.description);
            if (!item.getDescription().isEmpty()) {
                descriptionText.setText(item.getDescription());
                descriptionText.setTextColor(ThemeUtil.getThemeColor(getContext(), android.R.attr.textColorSecondary));
            } else {
                descriptionText.setText(getString(R.string.module_empty_description));
                descriptionText.setTextColor(getResources().getColor(R.color.warning, null));
            }

            Switch mSwitch = view.findViewById(R.id.checkbox);
            mSwitch.setChecked(mModuleUtil.isModuleEnabled(item.packageName));
            TextView warningText = view.findViewById(R.id.warning);

            if (item.minVersion == 0) {
                if (!XposedApp.getPreferences().getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(getString(R.string.no_min_version_specified));
                warningText.setVisibility(View.VISIBLE);
            } else if (installedXposedVersion > 0 && item.minVersion > installedXposedVersion) {
                if (!XposedApp.getPreferences().getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(String.format(getString(R.string.warning_xposed_min_version), item.minVersion));
                warningText.setVisibility(View.VISIBLE);
            } else if (item.minVersion < ModuleUtil.MIN_MODULE_VERSION) {
                if (!XposedApp.getPreferences().getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(String.format(getString(R.string.warning_min_version_too_low), item.minVersion, ModuleUtil.MIN_MODULE_VERSION));
                warningText.setVisibility(View.VISIBLE);
            } else if (item.isInstalledOnExternalStorage()) {
                if (!XposedApp.getPreferences().getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(getString(R.string.warning_installed_on_external_storage));
                warningText.setVisibility(View.VISIBLE);
            } else if (installedXposedVersion == 0 || (installedXposedVersion == -1 && !StatusInstallerFragment.DISABLE_FILE.exists())) {
                if (!XposedApp.getPreferences().getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(getString(R.string.not_installed_no_lollipop));
                warningText.setVisibility(View.VISIBLE);
            } else {
                mSwitch.setEnabled(true);
                warningText.setVisibility(View.GONE);
            }
            return view;
        }
    }

    class ApplicationFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            requireActivity().runOnUiThread(reloadModules);
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            requireActivity().runOnUiThread(reloadModules);
        }
    }
}
