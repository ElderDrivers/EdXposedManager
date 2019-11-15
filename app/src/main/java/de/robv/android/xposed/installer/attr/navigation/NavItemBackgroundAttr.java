package de.robv.android.xposed.installer.attr.navigation;

import android.view.View;

import com.google.android.material.navigation.NavigationView;

import de.robv.android.xposed.installer.widget.NavigationViewCompact;
import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class NavItemBackgroundAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof NavigationViewCompact) {
            NavigationViewCompact nav = (NavigationViewCompact) view;
            if (isDrawable()) {
                nav.setItemBackground(SkinResourcesUtils.getDrawable(attrValueRefId));
            }
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (view instanceof NavigationViewCompact) {
            NavigationViewCompact nav = (NavigationViewCompact) view;
            if (isDrawable()) {
                nav.setItemBackground(SkinResourcesUtils.getNightDrawable(attrValueRefName));
            }
        }
    }
}
