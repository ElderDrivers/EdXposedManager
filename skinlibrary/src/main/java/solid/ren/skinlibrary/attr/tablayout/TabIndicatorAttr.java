package solid.ren.skinlibrary.attr.tablayout;

import android.view.View;

import com.google.android.material.tabs.TabLayout;

import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class TabIndicatorAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof TabLayout) {
            if (isColor()) {
                ((TabLayout) view).setSelectedTabIndicatorColor(SkinResourcesUtils.getColor(attrValueRefId));
            } else if (isDrawable()) {
                ((TabLayout) view).setSelectedTabIndicator(SkinResourcesUtils.getDrawable(attrValueRefId));
            }
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (view instanceof TabLayout) {
            if (isColor()) {
                ((TabLayout) view).setSelectedTabIndicatorColor(SkinResourcesUtils.getNightColor(attrValueRefId));
            } else if (isDrawable()) {
                ((TabLayout) view).setSelectedTabIndicator(SkinResourcesUtils.getNightDrawable(attrValueRefName));
            }
        }
    }
}
