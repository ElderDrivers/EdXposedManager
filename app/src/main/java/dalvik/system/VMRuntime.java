package dalvik.system;

import org.meowcat.annotation.NotProguard;

/*
 * Stub!
 * Decompiled by MlgmXyysd.
 */
@NotProguard
public class VMRuntime {

    public static VMRuntime getRuntime() {
        throw new RuntimeException("Stub!");
    }

    public native boolean is64Bit();
}
