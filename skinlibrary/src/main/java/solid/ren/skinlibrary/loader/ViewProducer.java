package solid.ren.skinlibrary.loader;

import android.content.Context;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.View;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Created by _SOLID
 * Date:2016/7/11
 * Time:11:35
 */
class ViewProducer {
    private static final Object[] mConstructorArgs = new Object[2];
    private static final Map<String, Constructor<? extends View>> sConstructorMap
            = new ArrayMap<>();
    private static final Class<?>[] sConstructorSignature = new Class[]{
            Context.class, AttributeSet.class};
    private static final String[] sClassPrefixList = {
            "android.widget.",
            "android.view.",
            "android.webkit."
    };

    static View createViewFromTag(Context context, String name, AttributeSet attrs) {
        if (name.equals("view")) {
            name = attrs.getAttributeValue(null, "class");
        }

        try {
            mConstructorArgs[0] = context;
            mConstructorArgs[1] = attrs;

            if (-1 == name.indexOf('.')) {
                for (int i = 0; i < sClassPrefixList.length; i++) {
                    final View view = createView(context, name, sClassPrefixList[i]);
                    if (view != null) {
                        return view;
                    }
                }
                return null;
            } else {
                return createView(context, name, null);
            }
        } catch (Exception e) {
            // We do not want to catch these, lets return null and let the actual LayoutInflater
            // try
            return null;
        } finally {
            // Don't retain references on context.
            mConstructorArgs[0] = null;
            mConstructorArgs[1] = null;
        }
    }

    private static View createView(Context context, String name, String prefix)
            throws ClassNotFoundException, InflateException {
        Constructor<? extends View> constructor = sConstructorMap.get(name);

        try {
            if (constructor == null) {
                // Class not found in the cache, see if it's real, and try to add it
                Class<? extends View> clazz = context.getClassLoader().loadClass(
                        prefix != null ? (prefix + name) : name).asSubclass(View.class);

                constructor = clazz.getConstructor(sConstructorSignature);
                sConstructorMap.put(name, constructor);
            }
            constructor.setAccessible(true);
            return constructor.newInstance(mConstructorArgs);
        } catch (Exception e) {
            // We do not want to catch these, lets return null and let the actual LayoutInflater
            // try
            return null;
        }
    }


    //    private View createView(Context context, String name, AttributeSet attrs) {
//        Log.i(TAG, "createView:" + name);
//        View view = null;
//        try {
//            if (-1 == name.indexOf('.')) {
//                if ("View".equals(name)) {
//                    view = LayoutInflater.from(context).createView(name, "android.view.", attrs);
//                }
//                if (view == null) {
//                    view = LayoutInflater.from(context).createView(name, "android.widget.", attrs);
//                }
//                if (view == null) {
//                    view = LayoutInflater.from(context).createView(name, "android.webkit.", attrs);
//                }
//            } else {
//                view = LayoutInflater.from(context).createView(name, null, attrs);
//            }
//
//        } catch (Exception e) {
//            SkinL.e(TAG, "Error while create 【" + name + "】 : " + e.getMessage());
//            view = null;
//        }
//        return view;
//    }
}
