package de.robv.android.xposed.installer.attr;

import android.view.View;

import com.google.android.material.tabs.TabLayout;

import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class TabLayoutIndicatorAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof TabLayout) {
            if (isColor()) {
                int color = SkinResourcesUtils.getColor(attrValueRefId);
                ((TabLayout) view).setSelectedTabIndicatorColor(color);
            }
        }
    }
}