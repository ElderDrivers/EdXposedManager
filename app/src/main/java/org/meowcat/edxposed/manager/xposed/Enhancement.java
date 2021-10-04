package org.meowcat.edxposed.manager.xposed;

import static org.meowcat.edxposed.manager.BuildConfig.APPLICATION_ID;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Build;
import android.os.FileObserver;
import android.os.Process;
import android.os.StrictMode;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.meowcat.annotation.NotProguard;
import org.meowcat.edxposed.manager.MeowCatApplication;
import org.meowcat.edxposed.manager.StatusInstallerFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@Keep
@NotProguard
public class Enhancement implements IXposedHookLoadPackage {

    private static final String mPretendXposedInstallerFlag = "pretend_xposed_installer";
    private static final String mHideEdXposedManagerFlag = "hide_edxposed_manager";
    private static final String mDisableForceClientSafetyNetFlag = "disable_force_client_safetynet";

    private static final String LEGACY_INSTALLER = "de.robv.android.xposed.installer";

    private static final HashSet<String> HIDE_WHITE_LIST = new HashSet<>();

    private static final SparseArray<List<String>> modulesList = new SparseArray<>();
    private static final SparseArray<FileObserver> modulesListObservers = new SparseArray<>();

    static {
        HIDE_WHITE_LIST.addAll(Arrays.asList( // TODO: more whitelist packages
                APPLICATION_ID, // Whitelist or crash
                LEGACY_INSTALLER, // for safety
                "com.android.providers.downloads", // For download modules
                "com.android.providers.downloads.ui",
                "com.android.packageinstaller", // For uninstall EdXposed Manager
                "com.google.android.packageinstaller",
                "com.android.systemui", // For notifications
                "com.android.permissioncontroller", // For permissions grant
                "com.topjohnwu.magisk", // For superuser root grant
                "eu.chainfire.supersu"
        )); // isUidBelongSystemCoreComponent(uid) will auto pass))
    }

    private static boolean getFlagState(int user, String flag) {
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            return new File(String.format("/data/user_de/%s/%s/conf/%s", user, APPLICATION_ID, flag)).exists();
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private static List<String> getModulesList(final int user) {
        final int index = modulesList.indexOfKey(user);
        if (index >= 0) {
            return modulesList.valueAt(index);
        }

        final String filename = String.format("/data/user_de/%s/%s/conf/enabled_modules.list", user, APPLICATION_ID);
        final FileObserver observer = new FileObserver(new File(filename)) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                switch (event) {
                    case FileObserver.MODIFY:
                        modulesList.put(user, readModulesList(filename));
                        break;
                    case FileObserver.MOVED_FROM:
                    case FileObserver.MOVED_TO:
                    case FileObserver.MOVE_SELF:
                    case FileObserver.DELETE:
                    case FileObserver.DELETE_SELF:
                        modulesList.remove(user);
                        modulesListObservers.remove(user);
                        break;
                }
            }
        };
        modulesListObservers.put(user, observer);
        final List<String> list = readModulesList(filename);
        modulesList.put(user, list);
        observer.startWatching();
        return list;
    }

    private static List<String> readModulesList(final String filename) {
        Log.d(MeowCatApplication.TAG, "Reading modules list " + filename + "...");
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            final File listFile = new File(filename);
            final List<String> list = new ArrayList<>();
            try {
                final FileReader fileReader = new FileReader(listFile);
                final BufferedReader bufferedReader = new BufferedReader(fileReader);
                String str;
                while ((str = bufferedReader.readLine()) != null) {
                    list.add(str);
                }
                bufferedReader.close();
                fileReader.close();
            } catch (IOException e) {
                Log.e(MeowCatApplication.TAG, "Read modules list error:", e);
            }
            return list;
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private static void hookAllMethods(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) {
        try {
            final Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass == null || XposedBridge.hookAllMethods(hookClass, methodName, callback).size() == 0)
                Log.w(MeowCatApplication.TAG, "Failed to hook " + methodName + " method in " + className);
        } catch (Throwable t) {
            Log.e(MeowCatApplication.TAG, "Failed to hook " + methodName + " method in " + className + ":", t);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam applicationParam) {
                XposedHelpers.findAndHookMethod(JSONObject.class, "getBoolean", String.class, new XC_MethodHook() {
                    public void beforeHookedMethod(MethodHookParam param) {
                        String str = (String) param.args[0];
                        Context context = (Context) applicationParam.args[0];
                        ApplicationInfo applicationInfo = context.getApplicationInfo();
                        if (applicationInfo != null) {
                            int userId = UserHandle.getUserHandleForUid(applicationInfo.uid).hashCode();
                            if (getFlagState(userId, mDisableForceClientSafetyNetFlag)) {
                                return;
                            }
                        }
                        if (("ctsProfileMatch".equals(str) || "basicIntegrity".equals(str) || "isValidSignature".equals(str))) {
                            param.setResult(true);
                        }
                    }
                });
            }
        });

        if (lpparam.packageName.equals("android")) {
            // com.android.server.pm.PackageManagerService.getInstalledApplications(int flag, int userId)
            findAndHookMethod("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getInstalledApplications", int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int packageUid = Binder.getCallingUid();
                        if (isUidBelongSystemCoreComponent(packageUid)) {
                            return;
                        }

                        final int userId = (int) param.args[1];
                        boolean isXposedModule = false;
                        final String[] packages = (String[]) callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (HIDE_WHITE_LIST.contains(packageName)) {
                                return;
                            }
                            if (new HashSet<>(getModulesList(userId)).contains(packageName)) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        @SuppressWarnings("unchecked") final List<ApplicationInfo> applicationInfoList = (List<ApplicationInfo>) callMethod(param.getResult(), "getList");
                        if (isXposedModule) {
                            if (getFlagState(userId, mPretendXposedInstallerFlag)) {
                                ListIterator<ApplicationInfo> iterator = applicationInfoList.listIterator();
                                while (iterator.hasNext()) {
                                    ApplicationInfo applicationInfo = (iterator.next());
                                    if (applicationInfo.packageName.equals(APPLICATION_ID)) {
                                        applicationInfo.packageName = LEGACY_INSTALLER;
                                        iterator.add(applicationInfo);
                                    }
                                }
                            }
                        } else {
                            if (getFlagState(userId, mHideEdXposedManagerFlag)) {
                                ListIterator<ApplicationInfo> iterator = applicationInfoList.listIterator();
                                while (iterator.hasNext()) {
                                    String packageName = (iterator.next()).packageName;
                                    if (packageName.equals(APPLICATION_ID) || packageName.equals(LEGACY_INSTALLER)) {
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                        param.setResult(param.getResult()); // "reset" the result to indicate that we handled it
                    }
                }
            });
            // com.android.server.pm.PackageManagerService.getInstalledPackages(int flag, int userId)
            findAndHookMethod("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getInstalledPackages", int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int packageUid = Binder.getCallingUid();
                        if (isUidBelongSystemCoreComponent(packageUid)) {
                            return;
                        }

                        final int userId = (int) param.args[1];
                        boolean isXposedModule = false;
                        final String[] packages = (String[]) callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (HIDE_WHITE_LIST.contains(packageName)) {
                                return;
                            }
                            if (new HashSet<>(getModulesList(userId)).contains(packageName)) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        @SuppressWarnings("unchecked") final List<PackageInfo> packageInfoList = (List<PackageInfo>) callMethod(param.getResult(), "getList");
                        if (isXposedModule) {
                            if (getFlagState(userId, mPretendXposedInstallerFlag)) {
                                ListIterator<PackageInfo> iterator = packageInfoList.listIterator();
                                while (iterator.hasNext()) {
                                    PackageInfo packageInfo = (iterator.next());
                                    if (packageInfo.packageName.equals(APPLICATION_ID)) {
                                        packageInfo.packageName = LEGACY_INSTALLER;
                                        iterator.add(packageInfo);
                                    }
                                }
                            }
                        } else {
                            if (getFlagState(userId, mHideEdXposedManagerFlag)) {
                                ListIterator<PackageInfo> iterator = packageInfoList.listIterator();
                                while (iterator.hasNext()) {
                                    String packageName = (iterator.next()).packageName;
                                    if (packageName.equals(APPLICATION_ID) || packageName.equals(LEGACY_INSTALLER)) {
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                        param.setResult(param.getResult()); // "reset" the result to indicate that we handled it
                    }
                }
            });
            // com.android.server.pm.PackageManagerService.getApplicationInfo(String packageName, int flag, int userId)
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getApplicationInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int packageUid = Binder.getCallingUid();
                        if (isUidBelongSystemCoreComponent(packageUid)) {
                            return;
                        }

                        final int userId = (int) param.args[2];
                        boolean isXposedModule = false;
                        final String[] packages = (String[]) callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (HIDE_WHITE_LIST.contains(packageName)) {
                                return;
                            }
                            if (new HashSet<>(getModulesList(userId)).contains(packageName)) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        if (isXposedModule) {
                            if (getFlagState(userId, mPretendXposedInstallerFlag)) {
                                if (param.args[0].equals(LEGACY_INSTALLER)) {
                                    param.args[0] = APPLICATION_ID;
                                }
                            }
                        } else {
                            if (getFlagState(userId, mHideEdXposedManagerFlag)) {
                                if (param.args[0].equals(APPLICATION_ID) || param.args[0].equals(LEGACY_INSTALLER)) {
                                    param.setResult(null);
                                }
                            }
                        }
                    }
                }
            });
            // com.android.server.pm.PackageManagerService.getPackageInfo(String packageName, int flag, int userId)
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getPackageInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int packageUid = Binder.getCallingUid();
                        if (isUidBelongSystemCoreComponent(packageUid)) {
                            return;
                        }

                        final int userId = (int) param.args[2];
                        boolean isXposedModule = false;
                        final String[] packages = (String[]) callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (HIDE_WHITE_LIST.contains(packageName)) {
                                return;
                            }
                            if (new HashSet<>(getModulesList(userId)).contains(packageName)) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        if (isXposedModule) {
                            if (getFlagState(userId, mPretendXposedInstallerFlag)) {
                                if (param.args[0].equals(LEGACY_INSTALLER)) {
                                    param.args[0] = APPLICATION_ID;
                                }
                            }
                        } else {
                            if (getFlagState(userId, mHideEdXposedManagerFlag)) {
                                if (param.args[0].equals(APPLICATION_ID) || param.args[0].equals(LEGACY_INSTALLER)) {
                                    param.setResult(null);
                                }
                            }
                        }
                    }
                }
            });
            // Hook AM to remove restrict of EdXposed Manager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final XC_MethodHook hook = new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args[1] != null) {
                            if (param.args[1].equals(APPLICATION_ID)) {
                                param.setResult(0);
                            }
                        }
                    }
                };
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "appRestrictedInBackgroundLocked", hook);
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "appServicesRestrictedInBackgroundLocked", hook);
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "getAppStartModeLocked", hook);
            }
        } else if (lpparam.packageName.equals(APPLICATION_ID)) {
            // Make sure Xposed work
            XposedHelpers.findAndHookMethod(StatusInstallerFragment.class.getName(), lpparam.classLoader, "isEnhancementEnabled", XC_MethodReplacement.returnConstant(true));
            // XposedHelpers.findAndHookMethod(StatusInstallerFragment.class.getName(), lpparam.classLoader, "isSELinuxEnforced", XC_MethodReplacement.returnConstant(SELinuxHelper.isSELinuxEnforced()));
        }
    }

    private boolean isUidBelongSystemCoreComponent(int uid) {
        if (uid >= 0) {
            final int appId = (int) callStaticMethod(UserHandle.class, "getAppId", uid);
            return appId < Process.FIRST_APPLICATION_UID;
        } else {
            return false;
        }
    }
}
