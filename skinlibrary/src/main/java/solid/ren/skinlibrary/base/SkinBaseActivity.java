package solid.ren.skinlibrary.base;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.LayoutInflaterCompat;

import java.util.List;

import solid.ren.skinlibrary.IDynamicNewView;
import solid.ren.skinlibrary.ISkinUpdate;
import solid.ren.skinlibrary.SkinConfig;
import solid.ren.skinlibrary.attr.base.DynamicAttr;
import solid.ren.skinlibrary.loader.SkinInflaterFactory;
import solid.ren.skinlibrary.loader.SkinManager;
import solid.ren.skinlibrary.utils.SkinL;
import solid.ren.skinlibrary.utils.SkinResourcesUtils;

/**
 * Created by _SOLID
 * Date:2016/4/14
 * Time:10:24
 * Your activity need extend
 */
public class SkinBaseActivity extends AppCompatActivity implements ISkinUpdate, IDynamicNewView {

    private SkinInflaterFactory mSkinInflaterFactory;

    private final static String TAG = "SkinBaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSkinInflaterFactory = new SkinInflaterFactory(this);
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), mSkinInflaterFactory);
        super.onCreate(savedInstanceState);
        changeStatusColor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SkinManager.getInstance().attach(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SkinManager.getInstance().detach(this);
        mSkinInflaterFactory.clean();
    }

    @Override
    public void onThemeUpdate() {
        SkinL.i(TAG, "onThemeUpdate");
        mSkinInflaterFactory.applySkin();
        changeStatusColor();
    }

    public SkinInflaterFactory getInflaterFactory() {
        return mSkinInflaterFactory;
    }

    public void changeStatusColor() {
        if (!SkinConfig.isCanChangeStatusColor()) {
            return;
        }
        int color = SkinResourcesUtils.getColorPrimaryDark();
        if (color != -1) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(SkinResourcesUtils.getColorPrimaryDark());
        }
    }

    @Override
    public void dynamicAddView(View view, List<DynamicAttr> pDAttrs) {
        mSkinInflaterFactory.dynamicAddSkinEnableView(this, view, pDAttrs);
    }

    @Override
    public void dynamicAddView(View view, String attrName, int attrValueResId) {
        mSkinInflaterFactory.dynamicAddSkinEnableView(this, view, attrName, attrValueResId);
    }

    @Override
    public void dynamicAddFontView(TextView textView) {
        mSkinInflaterFactory.dynamicAddFontEnableView(this, textView);
    }

}
