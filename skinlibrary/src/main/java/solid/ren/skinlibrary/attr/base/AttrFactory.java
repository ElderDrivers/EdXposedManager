package solid.ren.skinlibrary.attr.base;

import java.util.HashMap;
import java.util.Objects;

import solid.ren.skinlibrary.attr.tablayout.TabIndicatorAttr;
import solid.ren.skinlibrary.attr.tablayout.TabRippleColorAttr;
import solid.ren.skinlibrary.attr.tablayout.TabSelectedTextColorAttr;
import solid.ren.skinlibrary.attr.tablayout.TabTextColorAttr;
import solid.ren.skinlibrary.attr.view.BackgroundAttr;
import solid.ren.skinlibrary.attr.view.BackgroundTintAttr;
import solid.ren.skinlibrary.attr.view.ImageViewSrcAttr;
import solid.ren.skinlibrary.attr.view.TextColorAttr;

/**
 * Created by _SOLID
 * Date:2016/4/14
 * Time:9:47
 */
public class AttrFactory {

    private static HashMap<String, SkinAttr> sSupportAttr = new HashMap<>();

    // TabLayout
    static {
        sSupportAttr.put("tabIndicator", new TabIndicatorAttr());
        sSupportAttr.put("tabTextColor", new TabTextColorAttr());
        sSupportAttr.put("tabRippleColor", new TabRippleColorAttr());
        sSupportAttr.put("tabSelectedTextColor", new TabSelectedTextColorAttr());

        sSupportAttr.put("backgroundTint", new BackgroundTintAttr());
        sSupportAttr.put("background", new BackgroundAttr());
        sSupportAttr.put("textColor", new TextColorAttr());
        sSupportAttr.put("src", new ImageViewSrcAttr());
    }


    public static SkinAttr get(String attrName, int attrValueRefId, String attrValueRefName, String typeName) {
        SkinAttr mSkinAttr = Objects.requireNonNull(sSupportAttr.get(attrName)).clone();
        mSkinAttr.attrName = attrName;
        mSkinAttr.attrValueRefId = attrValueRefId;
        mSkinAttr.attrValueRefName = attrValueRefName;
        mSkinAttr.attrValueTypeName = typeName;
        return mSkinAttr;
    }

    /**
     * check current attribute if can be support
     *
     * @param attrName attribute name
     * @return true : supported <br>
     * false: not supported
     */
    public static boolean isSupportedAttr(String attrName) {
        return sSupportAttr.containsKey(attrName);
    }

    /**
     * add support's attribute
     *
     * @param attrName attribute name
     * @param skinAttr skin attribute
     */
    public static void addSupportAttr(String attrName, SkinAttr skinAttr) {
        sSupportAttr.put(attrName, skinAttr);
    }
}
