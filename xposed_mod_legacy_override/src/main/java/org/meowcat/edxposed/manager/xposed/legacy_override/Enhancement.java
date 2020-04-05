package org.meowcat.edxposed.manager.xposed.legacy_override;

import android.annotation.SuppressLint;
import android.os.Binder;

import java.io.File;
import java.util.Locale;

import androidx.annotation.Keep;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static org.meowcat.edxposed.manager.xposed.legacy_override.BuildConfig.APPLICATION_ID;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.CONFIG_PATH_FORMAT;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.STATE_HIDDEN;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.STATE_NORMAL;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.STATE_PRETEND;

@Keep
public class Enhancement implements IXposedHookLoadPackage {

    private static final String LEGACY_INSTALLER = "de.robv.android.xposed.installer";
    private static final String CURRENT_INSTALLER = "org.meowcat.edxposed.manager";

    private static void hookAllMethods(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass == null || XposedBridge.hookAllMethods(hookClass, methodName, callback).size() == 0)
                XposedBridge.log("Failed to hook " + methodName + " method in " + className);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("android")) {
            // Hook PM to pretend to have legacy Xposed Installer installed
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getApplicationInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null)
                        return;

                    final String packageName = (String) param.args[0];
                    if (packageName == null)
                        return;

                    final int userId = (int) param.args[2];
                    int caller = Binder.getCallingUid();

                    switch (getState(caller, userId)) {
                        case STATE_HIDDEN:
                            if (LEGACY_INSTALLER.equals(packageName) || CURRENT_INSTALLER.equals(packageName) || APPLICATION_ID.equals(packageName)) {
                                param.setResult(null);
                            }
                            break;
                        case STATE_PRETEND:
                            if (LEGACY_INSTALLER.equals(packageName)) {
                                param.args[0] = CURRENT_INSTALLER;
                            }
                            break;
                        // case STATE_NORMAL: break; // does nothing
                    }
                }
            });
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getPackageInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null)
                        return;

                    final String packageName = (String) param.args[0];
                    if (packageName == null)
                        return;

                    final int userId = (int) param.args[2];
                    int caller = Binder.getCallingUid();

                    switch (getState(caller, userId)) {
                        case STATE_HIDDEN:
                            if (LEGACY_INSTALLER.equals(packageName) || CURRENT_INSTALLER.equals(packageName) || APPLICATION_ID.equals(packageName)) {
                                param.setResult(null);
                            }
                            break;
                        case STATE_PRETEND:
                            if (LEGACY_INSTALLER.equals(packageName)) {
                                param.args[0] = CURRENT_INSTALLER;
                            }
                            break;
                        // case STATE_NORMAL: break; // does nothing
                    }
                }
            });
        }
    }

    @SuppressLint("SdCardPath")
    private static int getState(final int caller, final int userId) {
        final String path = String.format(Locale.getDefault(), CONFIG_PATH_FORMAT, userId, caller);
        if (new File(path, "hidden").exists())
            return STATE_HIDDEN;
        if (new File(path, "pretend").exists())
            return STATE_PRETEND;
        return STATE_NORMAL;
    }
}