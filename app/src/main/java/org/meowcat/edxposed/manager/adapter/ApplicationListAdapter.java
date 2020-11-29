package org.meowcat.edxposed.manager.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.widget.CompoundButton;

import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.MeowCatApplication;
import org.meowcat.edxposed.manager.R;

import java.util.List;

public class ApplicationListAdapter extends AppAdapter {

    private volatile boolean isWhiteListMode;
    private List<String> checkedList;

    public ApplicationListAdapter(Context context, boolean isWhiteListMode) {
        super(context);
        this.isWhiteListMode = isWhiteListMode;
    }

//    public void setWhiteListMode(boolean isWhiteListMode) {
//        this.isWhiteListMode = isWhiteListMode;
//    }

    @Override
    public List<String> generateCheckedList() {
        AppHelper.makeSurePath();
        if (isWhiteListMode) {
            checkedList = AppHelper.getWhiteList();
        } else {
            checkedList = AppHelper.getBlackList();
        }
        Log.d(MeowCatApplication.TAG, "ApplicationList -> generateCheckedList: generate done");
        return checkedList;
    }

    @Override
    protected void onCheckedChange(CompoundButton view, boolean isChecked, ApplicationInfo info) {
        boolean success = isChecked ?
                AppHelper.addPackageName(isWhiteListMode, info.packageName) :
                AppHelper.removePackageName(isWhiteListMode, info.packageName);
        if (success) {
            if (isChecked) {
                checkedList.add(info.packageName);
            } else {
                checkedList.remove(info.packageName);
            }
        } else {
            Snackbar.make(view, R.string.add_package_failed, Snackbar.LENGTH_SHORT).show();
            view.setChecked(!isChecked);
        }
    }
}
