package com.solohsu.android.edxp.manager.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.solohsu.android.edxp.manager.fragment.CompileDialogFragment;

import androidx.fragment.app.FragmentManager;
import com.solohsu.android.edxp.manager.R;

public class CompileUtils {

    private static final String COMPILE_COMMAND_PREFIX = "cmd package compile ";
    private static final String COMPILE_RESET_COMMAND = COMPILE_COMMAND_PREFIX + "--reset ";
    private static final String COMPILE_SPEED_COMMAND = COMPILE_COMMAND_PREFIX + "-f -m speed ";
    private static final String TAG_COMPILE_DIALOG = "compile_dialog";

    public static void reset(Context context, FragmentManager fragmentManager,
                             ApplicationInfo info) {
        compilePackageInBg(context, fragmentManager, info,
                context.getString(R.string.compile_reset_msg), COMPILE_RESET_COMMAND);
    }

    public static void compileSpeed(Context context, FragmentManager fragmentManager,
                                    ApplicationInfo info) {
        compilePackageInBg(context, fragmentManager, info,
                context.getString(R.string.compile_speed_msg), COMPILE_SPEED_COMMAND);
    }

    public static void compilePackageInBg(Context context, FragmentManager fragmentManager,
                                          ApplicationInfo info, String msg, String... commands) {
        CompileDialogFragment fragment = CompileDialogFragment.newInstance(info, msg, commands);
        fragment.show(fragmentManager, TAG_COMPILE_DIALOG);
    }

}
