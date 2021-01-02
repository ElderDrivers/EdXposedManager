package android.os;

import org.meowcat.annotation.NotProguard;

/*
 * Stub!
 * Decompiled by MlgmXyysd.
 */
@SuppressWarnings({"unused", "RedundantSuppression"})
@NotProguard
public class FileUtils {
    public static final int S_IRUSR = 256;
    public static final int S_IWUSR = 128;
    public static final int S_IRGRP = 32;
    public static final int S_IWGRP = 16;
    public static final int S_IROTH = 4;
    public static final int S_IWOTH = 2;

    public static native int setPermissions(String path, int mode, int uid, int gid);
}
