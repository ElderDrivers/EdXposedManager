package org.meowcat.edxposed.manager.xposed.legacy_override;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;

import static org.meowcat.edxposed.manager.xposed.legacy_override.BuildConfig.APPLICATION_ID;

public final class Constants {

    public static final String LOG_TAG = "EdXpMgr-FakeLegacy";

    @SuppressLint("SdCardPath")
    public static final String CONFIG_PATH_FORMAT = "/data/user_de/%d/" + APPLICATION_ID + "/config/%s";

    public static final int SORT_ORDER_LABEL = 0;
    public static final int SORT_ORDER_PACKAGE_NAME = 1;
    public static final int SORT_ORDER_INSTALL_TIME = 2;
    public static final int SORT_ORDER_UPDATE_TIME = 3;

    public static final String PREF_KEY_SORT_ORDER = "preference_sort_order";
    public static final String PREF_KEY_SHOW_NON_NORMAL_FIRST = "preference_show_non_normal_first";
    public static final String PREF_KEY_SHOW_SYSTEM = "preference_show_system";

    public static final int STATE_HIDDEN = 0;
    public static final int STATE_NORMAL = 1;
    public static final int STATE_PRETEND = 2;

    @IntDef({STATE_HIDDEN, STATE_NORMAL, STATE_PRETEND})
    public @interface AppState {
    }

    private Constants() {
    }

}
