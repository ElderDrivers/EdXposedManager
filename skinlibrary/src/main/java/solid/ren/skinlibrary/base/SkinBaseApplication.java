package solid.ren.skinlibrary.base;

import android.app.Application;

import solid.ren.skinlibrary.loader.SkinManager;


/**
 * Created by _SOLID
 * Date:2016/4/14
 * Time:10:54
 * Desc:
 */
public class SkinBaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SkinManager.getInstance().init(this);
    }
}
