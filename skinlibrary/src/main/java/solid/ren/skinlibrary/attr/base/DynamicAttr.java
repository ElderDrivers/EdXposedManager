package solid.ren.skinlibrary.attr.base;

/**
 * Created by _SOLID
 * Date:2016/4/14
 * Time:10:21
 */
public class DynamicAttr {
    /**
     * attr name,defined from {@link AttrFactory}
     */
    public String attrName;

    /**
     * resource id from default context,eg: "R.drawable.app_bg"
     */
    public int refResId;

    public DynamicAttr(String attrName, int refResId) {
        this.attrName = attrName;
        this.refResId = refResId;
    }
}
