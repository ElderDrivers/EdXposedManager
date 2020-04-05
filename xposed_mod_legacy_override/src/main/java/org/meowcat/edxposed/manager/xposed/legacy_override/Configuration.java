package org.meowcat.edxposed.manager.xposed.legacy_override;

import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.util.Locale;

import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.CONFIG_PATH_FORMAT;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.LOG_TAG;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.STATE_HIDDEN;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.STATE_NORMAL;
import static org.meowcat.edxposed.manager.xposed.legacy_override.Constants.STATE_PRETEND;

public final class Configuration {

    public static void setState(int uid, @Constants.AppState int state) {
        final String path = String.format(
                Locale.getDefault(),
                CONFIG_PATH_FORMAT,
                Process.myUserHandle().hashCode(), String.valueOf(uid)
        );

        try {
            switch (state) {
                case STATE_HIDDEN:
                    mkdirs(path);
                    new File(path, "pretend").delete();
                    new File(path, "hidden").createNewFile();
                    break;
                case STATE_NORMAL:
                    new File(path, "hidden").delete();
                    new File(path, "pretend").delete();
                    new File(path).delete();
                    break;
                case STATE_PRETEND:
                    mkdirs(path);
                    new File(path, "hidden").delete();
                    new File(path, "pretend").createNewFile();
                    break;
            }
        } catch (Throwable ex) {
            Log.e(LOG_TAG, "Cannot set state to " + state + " for uid = " + uid + ", reason = " + Log.getStackTraceString(ex));
        }
    }

    private static void mkdirs(final String path) throws ErrnoException {
        final File file = new File(path);
        final File parent1 = new File(file.getParent());
        final File parent2 = new File(parent1.getParent());
        file.mkdirs();
        Os.chmod(parent2.getPath(), 00755);
        Os.chmod(parent1.getPath(), 00755);
        Os.chmod(file.getPath(), 00755);
    }

    @Constants.AppState
    public static int getState(int uid) {
        final String path = String.format(Locale.getDefault(), CONFIG_PATH_FORMAT, Process.myUserHandle().hashCode(), String.valueOf(uid));
        if (new File(path, "hidden").exists())
            return STATE_HIDDEN;
        if (new File(path, "pretend").exists())
            return STATE_PRETEND;
        return STATE_NORMAL;
    }

    private Configuration() {
    }

}