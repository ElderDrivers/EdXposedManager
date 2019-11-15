package de.robv.android.xposed.installer.attr;

import android.view.View;

import de.robv.android.xposed.installer.widget.XposedStatusPanel;
import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class XposedStateWarnAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof XposedStatusPanel) {
            if (isDrawable()) {
                ((XposedStatusPanel) view).setWarnDrawable(SkinResourcesUtils.getDrawable(attrValueRefId));
            }
        }
    }
}