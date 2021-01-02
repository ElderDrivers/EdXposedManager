package org.meowcat.edxposed.manager.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;
import android.widget.CompoundButton;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.ToastUtil;
import org.meowcat.edxposed.manager.widget.MasterSwitch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.meowcat.edxposed.manager.XposedApp.setFilePermissionsFromMode;
import static org.meowcat.edxposed.manager.adapter.AppHelper.BASE_PATH;

public class ActivationScopeAdapter extends AppAdapter {

    private static final HashMap<String, List<String>> scopeList = new HashMap<>();
    private static final String SCOPE_LIST_PATH = "conf/%s.conf";
    protected static boolean enabled = false;
    private static File scopeFile;
    protected final String modulePackageName;
    private final MasterSwitch masterSwitch;
    private final SwipeRefreshLayout swipeRefreshLayout;
    private List<String> checkedList = new ArrayList<>();

    public ActivationScopeAdapter(Context context, String modulePackageName, MasterSwitch masterSwitch, SwipeRefreshLayout swipeRefreshLayout) {
        super(context);
        this.modulePackageName = modulePackageName;
        this.masterSwitch = masterSwitch;
        this.swipeRefreshLayout = swipeRefreshLayout;
        scopeFile = new File(BASE_PATH + String.format(SCOPE_LIST_PATH, modulePackageName));
        masterSwitch.setTitle(context.getString(R.string.enable_scope));
        enabled = scopeFile.exists();
        masterSwitch.setOnCheckedChangedListener(new MasterSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean checked) {
                ModuleUtil moduleUtil = ModuleUtil.getInstance();
                moduleUtil.updateModulesList(false, null);
                enabled = checked;
                saveScopeList(modulePackageName, enabled, checkedList);
                notifyDataSetChanged();
                if (enabled) {
                    swipeRefreshLayout.setVisibility(View.VISIBLE);
                } else {
                    swipeRefreshLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    static List<String> getScopeList(String modulePackageName) {
        if (scopeList.containsKey(modulePackageName)) {
            return scopeList.get(modulePackageName);
        }
        List<String> s = new ArrayList<>();
        if (scopeFile.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(scopeFile));
                for (String line; (line = bufferedReader.readLine()) != null; ) {
                    s.add(line);
                }
                scopeList.put(modulePackageName, s);
                enabled = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return s;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    static boolean saveScopeList(String modulePackageName, boolean enabled, List<String> list) {
        if (!enabled) {
            scopeList.remove(modulePackageName);
            return scopeFile.delete();
        }
        try {
            PrintWriter pr = new PrintWriter(new FileWriter(scopeFile));
            for (String line : list) {
                pr.println(line);
            }
            pr.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        scopeList.put(modulePackageName, list);
        setFilePermissionsFromMode(scopeFile.getPath(), Context.MODE_WORLD_READABLE);
        return true;
    }

    @Override
    public List<String> generateCheckedList() {
        AppHelper.makeSurePath();
        List<String> scopeList = getScopeList(modulePackageName);
        List<String> list = new ArrayList<>();
        for (ApplicationInfo info : fullList) {
            list.add(info.packageName);
        }
        scopeList.retainAll(list);
        checkedList = scopeList;
        ((Activity) context).runOnUiThread(() -> {
            masterSwitch.setChecked(enabled);
            if (enabled) {
                swipeRefreshLayout.setVisibility(View.VISIBLE);
            } else {
                swipeRefreshLayout.setVisibility(View.GONE);
            }
        });
        return checkedList;
    }

    @Override
    protected void onCheckedChange(CompoundButton view, boolean isChecked, ApplicationInfo info) {
        ModuleUtil moduleUtil = ModuleUtil.getInstance();
        moduleUtil.updateModulesList(false, null);

        if (isChecked) {
            checkedList.add(info.packageName);
        } else {
            checkedList.remove(info.packageName);
        }
        if (!saveScopeList(modulePackageName, enabled, checkedList)) {
            ToastUtil.showShortToast(context, R.string.add_package_failed);
            if (!isChecked) {
                checkedList.add(info.packageName);
            } else {
                checkedList.remove(info.packageName);
            }
            view.setChecked(!isChecked);
        }
    }
}
