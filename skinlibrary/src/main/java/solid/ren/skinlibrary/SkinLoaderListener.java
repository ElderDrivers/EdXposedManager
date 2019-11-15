package solid.ren.skinlibrary;

/**
 * Created by _SOLID
 * Date:2016/4/13
 * Time:21:07
 * Desc:
 */
public interface SkinLoaderListener {
    void onStart();

    void onSuccess();

    void onFailed(String errMsg);
}
