package org.meowcat.edxposed.manager;

import android.util.Log;

import org.meowcat.annotation.NotProguard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.meowcat.edxposed.manager.MeowCatApplication.TAG;

@NotProguard
public class Constants {

    public static int getActiveXposedVersion() {
        return -1;
    }

    public static String getInstalledXposedVersion() {
        return null;
    }

    public static String getBaseDir() {
        return XposedApp.getInstance().getApplicationInfo().deviceProtectedDataDir + "/";
    }

    public static String getEnabledModulesListFile() {
        return getBaseDir() + "conf/enabled_modules.list";
    }

    public static String getModulesListFile() {
        return getBaseDir() + "conf/modules.list";
    }

    public static boolean isSELinuxEnforced() {
        boolean result = false;
        final File SELINUX_STATUS_FILE = new File("/sys/fs/selinux/enforce");
        if (SELINUX_STATUS_FILE.exists()) {
            try {
                FileInputStream fis = new FileInputStream(SELINUX_STATUS_FILE);
                int status = fis.read();
                switch (status) {
                    case 49:
                        result = true;
                        break;
                    case 48:
                        result = false;
                        break;
                    default:
                        Log.e(TAG, "Unexpected byte " + status + " in /sys/fs/selinux/enforce");
                }
                fis.close();
            } catch (IOException e) {
                if (e.getMessage().contains("Permission denied")) {
                    result = true;
                } else {
                    Log.e(TAG, "Failed to read SELinux status: " + e.getMessage());
                    result = false;
                }
            }
        }
        return result;
    }
}
