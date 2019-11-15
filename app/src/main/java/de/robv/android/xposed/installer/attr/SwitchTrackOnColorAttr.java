package de.robv.android.xposed.installer.attr;

import android.view.View;

import de.robv.android.xposed.installer.widget.CoxylicSwitch;
import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class SwitchTrackOnColorAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof CoxylicSwitch) {
            if (isColor()) {
                CoxylicSwitch sw = ((CoxylicSwitch) view);
                sw.setTrackOnColor(SkinResourcesUtils.getColor(attrValueRefId));
            }
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (view instanceof CoxylicSwitch) {
            if (isColor()) {
                CoxylicSwitch sw = ((CoxylicSwitch) view);
                sw.setTrackOnColor(SkinResourcesUtils.getNightColor(attrValueRefId));
            }
        }
    }
}
