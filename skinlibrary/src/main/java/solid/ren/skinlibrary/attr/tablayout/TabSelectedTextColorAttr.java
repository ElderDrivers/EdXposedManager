package solid.ren.skinlibrary.attr.tablayout;

import android.view.View;

import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class TabSelectedTextColorAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof TabLayout) {
            if (isColor()) {
                TabLayout tab = (TabLayout) view;
                tab.setTabTextColors(Objects.requireNonNull(tab.getTabTextColors()).getDefaultColor(), SkinResourcesUtils.getColor(attrValueRefId));
            }
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (view instanceof TabLayout) {
            if (isColor()) {
                TabLayout tab = (TabLayout) view;
                tab.setTabTextColors(Objects.requireNonNull(tab.getTabTextColors()).getDefaultColor(), SkinResourcesUtils.getNightColor(attrValueRefId));
            }
        }
    }
}
