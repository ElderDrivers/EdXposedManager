package de.robv.android.xposed.installer.attr.navigation;

import android.content.res.ColorStateList;
import android.view.View;

import de.robv.android.xposed.installer.widget.BottomNavigationViewCompact;
import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class BottomNavItemRippleColorAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof BottomNavigationViewCompact) {
            if (isColor())
                ((BottomNavigationViewCompact) view).setItemRippleColor(ColorStateList.valueOf(SkinResourcesUtils.getColor(attrValueRefId)));
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (view instanceof BottomNavigationViewCompact) {
            if (isColor())
                ((BottomNavigationViewCompact) view).setItemRippleColor(ColorStateList.valueOf(SkinResourcesUtils.getNightColor(attrValueRefId)));
        }
    }
}
