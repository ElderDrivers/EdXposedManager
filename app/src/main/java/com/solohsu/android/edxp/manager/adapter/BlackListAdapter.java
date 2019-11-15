package com.solohsu.android.edxp.manager.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.widget.CompoundButton;

import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.util.ToastUtils;

import java.util.List;

public class BlackListAdapter extends AppAdapter {

    private volatile boolean isWhiteListMode;
    private List<String> checkedList;

    public BlackListAdapter(Context context, boolean isWhiteListMode) {
        super(context);
        this.isWhiteListMode = isWhiteListMode;
    }

    public void setWhiteListMode(boolean isWhiteListMode) {
        this.isWhiteListMode = isWhiteListMode;
    }

    @Override
    protected List<String> generateCheckedList() {
        AppHelper.makeSurePath();
        if (isWhiteListMode) {
            checkedList = AppHelper.getWhiteList();
        } else {
            checkedList = AppHelper.getBlackList();
        }
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
            ToastUtils.showShortToast(context, R.string.add_package_failed);
            view.setChecked(!isChecked);
        }
    }
}
