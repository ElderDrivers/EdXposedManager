package org.meowcat.edxposed.manager.xposed.legacy_override;

import androidx.annotation.Keep;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

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
                    if (param.args != null && param.args[0] != null) {
                        if (param.args[0].equals(LEGACY_INSTALLER)) {
                            param.args[0] = CURRENT_INSTALLER;
                        }
                    }
                }
            });
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getPackageInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        if (param.args[0].equals(LEGACY_INSTALLER)) {
                            param.args[0] = CURRENT_INSTALLER;
                        }
                    }
                }
            });
        }
    }
}