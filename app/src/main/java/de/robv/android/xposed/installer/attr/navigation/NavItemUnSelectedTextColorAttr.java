package de.robv.android.xposed.installer.attr.navigation;

import android.view.View;

import de.robv.android.xposed.installer.widget.NavigationViewCompact;
import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class NavItemUnSelectedTextColorAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof NavigationViewCompact) {
            NavigationViewCompact nav = (NavigationViewCompact) view;
            if (isColor()) {
                nav.setItemUnSelectedTextColor(SkinResourcesUtils.getColor(attrValueRefId));
            }
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (view instanceof NavigationViewCompact) {
            NavigationViewCompact nav = (NavigationViewCompact) view;
            if (isColor()) {
                nav.setItemUnSelectedTextColor(SkinResourcesUtils.getNightColor(attrValueRefId));
            }
        }
    }
}
