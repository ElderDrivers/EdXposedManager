package com.solohsu.android.edxp.manager.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;

import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.BuildConfig;
import org.meowcat.edxposed.manager.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import de.robv.android.xposed.installer.XposedApp;

public class AppHelper {

    public static final String TAG = XposedApp.TAG;

    private static final String BASE_PATH = XposedApp.BASE_DIR;
    private static final String WHITE_LIST_PATH = "conf/whitelist/";
    private static final String BLACK_LIST_PATH = "conf/blacklist/";
    private static final String COMPAT_LIST_PATH = "conf/compatlist/";
    private static final String WHITE_LIST_MODE = "conf/usewhitelist";

    private static final List<String> FORCE_WHITE_LIST = Arrays.asList(
            BuildConfig.APPLICATION_ID,
            "com.solohsu.android.edxp.manager",
            "de.robv.android.xposed.installer");

    static void makeSurePath() {
        XposedApp.mkdirAndChmod(WHITE_LIST_PATH,00771);
        XposedApp.mkdirAndChmod(BLACK_LIST_PATH,00771);
        XposedApp.mkdirAndChmod(COMPAT_LIST_PATH,00771);
    }

    public static boolean isWhiteListMode() {
        return new File(BASE_PATH + WHITE_LIST_MODE).exists();
    }

    private static boolean addWhiteList(String packageName) {
        return whiteListFileName(packageName, true);
    }

    private static boolean addBlackList(String packageName) {
        if (FORCE_WHITE_LIST.contains(packageName)) {
            removeBlackList(packageName);
            return false;
        }
        return blackListFileName(packageName, true);
    }

    private static boolean removeWhiteList(String packageName) {
        if (FORCE_WHITE_LIST.contains(packageName)) {
            return false;
        }
        return whiteListFileName(packageName, false);
    }

    private static boolean removeBlackList(String packageName) {
        return blackListFileName(packageName, false);
    }

    static List<String> getBlackList() {
        File file=new File(BASE_PATH + BLACK_LIST_PATH);
        File[] files=file.listFiles();
        if (files == null){
            return new ArrayList<>();
        }
        List<String> s = new ArrayList<>();
        for (File file1 : files) {
            if (!file1.isDirectory()) {
                System.out.println(file1.getName());
                s.add(file1.getName());
            }
        }
        return s;
    }

    static List<String> getWhiteList() {
        File file=new File(BASE_PATH + WHITE_LIST_PATH);
        File[] files=file.listFiles();
        if (files == null){
            return FORCE_WHITE_LIST;
        }
        List<String> result = new ArrayList<>();
        for (File file1 : files) {
            result.add(file1.getName());
        }
        for (String pn : FORCE_WHITE_LIST) {
            if (!result.contains(pn)) {
                result.add(pn);
                addWhiteList(pn);
            }
        }
        return new ArrayList<>(result);
    }

    private static Boolean whiteListFileName(String packageName, boolean isAdd) {
        boolean returns=true;
        File file=new File(BASE_PATH + WHITE_LIST_PATH + packageName);
        if (isAdd) {
            if (!file.exists()) {
                try {
                    returns = file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (file.exists()) {
                returns = file.delete();
            }
        }
        return returns;
    }

    private static Boolean blackListFileName(String packageName, boolean isAdd) {
        boolean returns=true;
        File file=new File(BASE_PATH + BLACK_LIST_PATH + packageName);
        if (isAdd) {
            if (!file.exists()) {
                try {
                    returns = file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (file.exists()) {
                returns = file.delete();
            }
        }
        return returns;
    }

    private static Boolean compatListFileName(String packageName, boolean isAdd) {
        boolean returns=true;
        File file=new File(BASE_PATH + COMPAT_LIST_PATH + packageName);
        if (isAdd) {
            if (!file.exists()) {
                try {
                    returns = file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (file.exists()) {
                returns = file.delete();
            }
        }
        return returns;
    }

    static boolean addPackageName(boolean isWhiteListMode, String packageName) {
        return isWhiteListMode ? addWhiteList(packageName) : addBlackList(packageName);
    }

    static boolean removePackageName(boolean isWhiteListMode, String packageName) {
        return isWhiteListMode ? removeWhiteList(packageName) : removeBlackList(packageName);
    }

    @SuppressLint("RestrictedApi")
    public static void showMenu(@NonNull Context context,
                                @NonNull FragmentManager fragmentManager,
                                @NonNull View anchor,
                                @NonNull ApplicationInfo info) {
        PopupMenu appMenu = new PopupMenu(context, anchor);
        appMenu.inflate(R.menu.menu_app_item);
        appMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.app_menu_compile_reset:
                    CompileUtils.reset(context, fragmentManager, info);
                    break;
                case R.id.app_menu_compile_speed:
                    CompileUtils.compileSpeed(context, fragmentManager, info);
                    break;
            }
            return true;
        });
        MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) appMenu.getMenu(), anchor);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    static List<String> getCompatList() {
        File file=new File(BASE_PATH + COMPAT_LIST_PATH);
        File[] files=file.listFiles();
        if (files == null){
            return new ArrayList<>();
        }
        List<String> s = new ArrayList<>();
        for (File file1 : files) {
            s.add(file1.getName());
        }
        return s;
    }

    public static boolean addCompatList(String packageName) {
        return compatListFileName(packageName, true);
    }

    public static boolean removeCompatList(String packageName) {
        return compatListFileName(packageName, false);
    }
}
