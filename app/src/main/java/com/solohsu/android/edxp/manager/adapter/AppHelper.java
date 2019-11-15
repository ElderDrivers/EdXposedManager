package com.solohsu.android.edxp.manager.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;

import com.solohsu.android.edxp.manager.BuildConfig;
import com.solohsu.android.edxp.manager.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.MyFileUtils;

import static de.robv.android.xposed.installer.XposedApp.BASE_DIR;

public class AppHelper {

    public static final String TAG = XposedApp.TAG;

    private static final String WHITE_LIST_PATH = BASE_DIR + "conf/whitelist/";
    private static final String BLACK_LIST_PATH = BASE_DIR + "conf/blacklist/";
    private static final String COMPAT_LIST_PATH = BASE_DIR + "conf/compatlist/";
    private static final String WHITE_LIST_MODE = BASE_DIR + "conf/usewhitelist";
    private static final String DYNAMIC_MODULES = BASE_DIR + "conf/dynamicmodules";
    private static final String BLACK_WHITE_LIST = BASE_DIR + "conf/blackwhitelist";
    private static final String DEOPT_BOOT_IMAGE = BASE_DIR + "conf/deoptbootimage";

    private static final List<String> FORCE_WHITE_LIST = Arrays.asList(
            BuildConfig.APPLICATION_ID,
            "org.meowcat.edxposed.manager",
            "de.robv.android.xposed.installer");

    public static void makeSurePath() {
        new File(WHITE_LIST_PATH).mkdirs();
        new File(BLACK_LIST_PATH).mkdirs();
        new File(COMPAT_LIST_PATH).mkdirs();
        MyFileUtils.setPermissions(WHITE_LIST_PATH, 00711, -1, -1);
        MyFileUtils.setPermissions(BLACK_LIST_PATH, 00711, -1, -1);
        MyFileUtils.setPermissions(COMPAT_LIST_PATH, 00711, -1, -1);
    }

    public static boolean isWhiteListMode() {
        return isFileExists(WHITE_LIST_MODE);
    }

    public static boolean setWhiteListMode(boolean isWhiteListMode) {
        return isWhiteListMode ? createFile(WHITE_LIST_MODE) : deleteFile(WHITE_LIST_MODE);
    }

    public static boolean addWhiteList(String packageName) {
        return createFile(WHITE_LIST_PATH + packageName);
    }

    public static boolean addBlackList(String packageName) {
        if (FORCE_WHITE_LIST.contains(packageName)) {
            removeBlackList(packageName);
            return false;
        }
        return createFile(BLACK_LIST_PATH + packageName);
    }

    public static boolean removeWhiteList(String packageName) {
        if (FORCE_WHITE_LIST.contains(packageName)) {
            return false;
        }
        return deleteFile(WHITE_LIST_PATH + packageName);
    }

    public static boolean removeBlackList(String packageName) {
        return deleteFile(BLACK_LIST_PATH + packageName);
    }

    public static List<String> getBlackList() {
        return listFiles(BLACK_LIST_PATH);
    }

    public static List<String> getWhiteList() {
        List<String> result = listFiles(WHITE_LIST_PATH);
        for (String pn : FORCE_WHITE_LIST) {
            if (!result.contains(pn)) {
                result.add(pn);
                addWhiteList(pn);
            }
        }
        return result;
    }

    public static boolean addPackageName(boolean isWhiteListMode, String packageName) {
        return isWhiteListMode ? addWhiteList(packageName) : addBlackList(packageName);
    }

    public static boolean removePackageName(boolean isWhiteListMode, String packageName) {
        return isWhiteListMode ? removeWhiteList(packageName) : removeBlackList(packageName);
    }

    public static boolean setDynamicModulesEnabled(boolean dynamicModulesEnabled) {
        return dynamicModulesEnabled ? createFile(DYNAMIC_MODULES) : deleteFile(DYNAMIC_MODULES);
    }

    public static boolean dynamicModulesEnabled() {
        return isFileExists(DYNAMIC_MODULES);
    }

    public static boolean blackWhiteListEnabled() {
        return isFileExists(BLACK_WHITE_LIST);
    }

    public static boolean setBlackWhiteListEnabled(boolean blackWhiteListEnabled) {
        return blackWhiteListEnabled ? createFile(BLACK_WHITE_LIST) : deleteFile(BLACK_WHITE_LIST);
    }

    public static boolean bootImageDeoptEnabled() {
        return isFileExists(DEOPT_BOOT_IMAGE);
    }

    public static boolean setBootImageDeoptEnabled(boolean bootImageDeoptEnabled) {
        return bootImageDeoptEnabled ? createFile(DEOPT_BOOT_IMAGE) : deleteFile(DEOPT_BOOT_IMAGE);
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

    public static List<String> getCompatList() {
        return listFiles(COMPAT_LIST_PATH);
    }

    public static boolean addCompatList(String packageName) {
        return createFile(COMPAT_LIST_PATH + packageName);
    }

    public static boolean removeCompatList(String packageName) {
        return deleteFile(COMPAT_LIST_PATH + packageName);
    }

    private static List<String> listFiles(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            return new ArrayList<>(Arrays.asList(file.list()));
        } else {
            return new ArrayList<>();
        }
    }

    private static boolean isFileExists(String path) {
        return new File(path).exists();
    }

    private static boolean deleteFile(String path) {
        return new File(path).delete();
    }

    private static boolean createFile(String path) {
        try {
            new File(path).createNewFile();
            MyFileUtils.setPermissions(path, 00664, -1, -1);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
