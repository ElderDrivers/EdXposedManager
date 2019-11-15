package solid.ren.skinlibrary.attr.tablayout;

import android.content.res.ColorStateList;
import android.view.View;

import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class TabTextColorAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof TabLayout) {
            if (isColor()) {
                TabLayout tab = (TabLayout) view;
                tab.setTabTextColors(SkinResourcesUtils.getColor(attrValueRefId), Objects.requireNonNull(tab.getTabTextColors()).getDefaultColor());
            }
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (view instanceof TabLayout) {
            if (isColor()) {
                TabLayout tab = (TabLayout) view;
                tab.setTabTextColors(SkinResourcesUtils.getNightColor(attrValueRefId), Objects.requireNonNull(tab.getTabTextColors()).getDefaultColor());
            }
        }
    }
}
