package solid.ren.skinlibrary.utils;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * Created by _SOLID
 * Date:2016/12/27
 * Time:17:44
 * Desc:兼容处理类
 */

public class ResourcesCompat {

    public static Resources getResources(AssetManager assetManager,
                                         DisplayMetrics displayMetrics,
                                         Configuration configuration) {
        Resources resources= null;
        try {
            resources = new Resources(assetManager, displayMetrics, configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resources;
    }
}
