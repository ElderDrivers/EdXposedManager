package org.meowcat.edxposed.manager.repo;

import org.meowcat.edxposed.manager.R;

public enum ReleaseType {
    STABLE(R.string.reltype_stable), BETA(R.string.reltype_beta), EXPERIMENTAL(R.string.reltype_experimental);

    private static final ReleaseType[] sValuesCache = values();
    private final int mTitleId;

    ReleaseType(int titleId) {
        mTitleId = titleId;
    }

    public static ReleaseType fromString(String value) {
        if (value == null || value.equals("stable"))
            return STABLE;
        else if (value.equals("beta"))
            return BETA;
        else if (value.equals("experimental"))
            return EXPERIMENTAL;
        else
            return STABLE;
    }

    public static ReleaseType fromOrdinal(int ordinal) {
        return sValuesCache[ordinal];
    }

    public int getTitleId() {
        return mTitleId;
    }

}
