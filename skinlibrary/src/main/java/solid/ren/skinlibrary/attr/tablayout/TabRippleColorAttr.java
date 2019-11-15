package solid.ren.skinlibrary.attr.tablayout;

import android.content.res.ColorStateList;
import android.view.View;

import com.google.android.material.tabs.TabLayout;

import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class TabRippleColorAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof TabLayout) {
            if (isColor()) {
                TabLayout tab = (TabLayout) view;
                tab.setTabRippleColor(ColorStateList.valueOf(SkinResourcesUtils.getColor(attrValueRefId)));
            }
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (view instanceof TabLayout) {
            if (isColor()) {
                TabLayout tab = (TabLayout) view;
                tab.setTabRippleColor(ColorStateList.valueOf(SkinResourcesUtils.getNightColor(attrValueRefId)));
            }
        }
    }
}
