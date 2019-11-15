package de.robv.android.xposed.installer.attr.navigation;

import android.view.View;

import de.robv.android.xposed.installer.widget.BottomNavigationViewCompact;
import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class BottomNavItemUnSelectedIconTintAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof BottomNavigationViewCompact) {
            BottomNavigationViewCompact nav = (BottomNavigationViewCompact) view;
            if (isColor()) {
                nav.setItemUnSelectedIconColor(SkinResourcesUtils.getColor(attrValueRefId));
            }
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (view instanceof BottomNavigationViewCompact) {
            BottomNavigationViewCompact nav = (BottomNavigationViewCompact) view;
            if (isColor()) {
                nav.setItemUnSelectedIconColor(SkinResourcesUtils.getNightColor(attrValueRefId));
            }
        }
    }
}
