package solid.ren.skinlibrary.loader;

import android.app.Activity;
import android.graphics.Typeface;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import solid.ren.skinlibrary.utils.TypefaceUtils;

/**
 * Created by _SOLID
 * Date:2016/7/12
 * Time:17:58
 * Desc:
 */
class TextViewRepository {
    private static Map<String, List<TextView>> mTextViewMap = new HashMap<>();

    static void add(Activity activity, TextView textView) {

        String className = activity.getLocalClassName();
        if (mTextViewMap.containsKey(className)) {
            mTextViewMap.get(className).add(textView);
        } else {
            List<TextView> textViews = new ArrayList<>();
            textViews.add(textView);
            mTextViewMap.put(className, textViews);
        }
        textView.setTypeface(TypefaceUtils.CURRENT_TYPEFACE);
    }

    static void remove(Activity activity) {
        mTextViewMap.remove(activity.getLocalClassName());
    }

    static void remove(Activity activity, TextView textView) {
        if (mTextViewMap.containsKey(activity.getLocalClassName())) {
            mTextViewMap.get(activity.getLocalClassName()).remove(textView);
        }
    }

    static void applyFont(Typeface tf) {
        for (String className : mTextViewMap.keySet()) {
            for (TextView textView : mTextViewMap.get(className)) {
                textView.setTypeface(tf);
            }
        }
    }
}
