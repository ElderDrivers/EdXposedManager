package solid.ren.skinlibrary.attr.view;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;

import solid.ren.skinlibrary.attr.base.SkinAttr;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

/**
 * Created by _SOLID
 * Date:2017/2/15
 * Time:17:39
 * Desc:
 */

public class ImageViewSrcAttr extends SkinAttr {
    @Override
    protected void applySkin(View view) {
        if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            if (isDrawable()) {
                iv.setImageDrawable(SkinResourcesUtils.getDrawable(attrValueRefId));
            } else if (isColor()) {
                iv.setImageDrawable(new ColorDrawable(SkinResourcesUtils.getColor(attrValueRefId)));
            }
        }
    }
}
