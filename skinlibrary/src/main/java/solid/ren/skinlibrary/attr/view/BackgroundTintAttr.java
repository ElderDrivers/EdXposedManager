package solid.ren.skinlibrary.attr.view;

import android.content.res.ColorStateList;
import android.view.View;

import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

public class BackgroundTintAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (isColor()) {
            int color = SkinResourcesUtils.getColor(attrValueRefId);
            view.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    @Override
    protected void applyNightMode(View view) {
        if (isColor()) {
            int color = SkinResourcesUtils.getColor(attrValueRefId);
            view.setBackgroundTintList(ColorStateList.valueOf(SkinResourcesUtils.getNightColor(attrValueRefId)));
        }
    }
}
