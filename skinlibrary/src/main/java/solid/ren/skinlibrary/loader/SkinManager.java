package solid.ren.skinlibrary.loader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import solid.ren.skinlibrary.ISkinUpdate;
import solid.ren.skinlibrary.SkinConfig;
import solid.ren.skinlibrary.SkinLoaderListener;
import solid.ren.skinlibrary.utils.ResourcesCompat;
import solid.ren.skinlibrary.utils.SkinFileUtils;
import solid.ren.skinlibrary.utils.SkinL;
import solid.ren.skinlibrary.utils.TypefaceUtils;


/**
 * Created by _SOLID
 * Date:2016/4/13
 * Time:21:07
 */
public class SkinManager implements ISkinLoader {
    private static final String TAG = "SkinManager";
    private List<ISkinUpdate> mSkinObservers;
    @SuppressLint("StaticFieldLeak")
    private static volatile SkinManager mInstance;
    private Context context;
    private Resources mResources;
    private boolean isDefaultSkin = false;
    /**
     * skin package name
     */
    private String skinPackageName;

    private SkinManager() {

    }

    public static SkinManager getInstance() {
        if (mInstance == null) {
            synchronized (SkinManager.class) {
                if (mInstance == null) {
                    mInstance = new SkinManager();
                }
            }
        }
        return mInstance;
    }

    public void init(Context ctx) {
        context = ctx.getApplicationContext();
        TypefaceUtils.CURRENT_TYPEFACE = TypefaceUtils.getTypeface(context);
        setUpSkinFile(context);
        if (SkinConfig.isInNightMode(ctx)) {
            SkinManager.getInstance().nightMode();
        } else {
            String skin = SkinConfig.getCustomSkinPath(context);
            if (SkinConfig.isDefaultSkin(context)) {
                return;
            }
            loadSkin(skin, null);
        }
    }

    private void setUpSkinFile(Context context) {
        try {
            String[] skinFiles = context.getAssets().list(SkinConfig.SKIN_DIR_NAME);
            for (String fileName : skinFiles) {
                File file = new File(SkinFileUtils.getSkinDir(context), fileName);
                if (!file.exists()) {
                    SkinFileUtils.copySkinAssetsToDir(context, fileName, SkinFileUtils.getSkinDir(context));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getColorPrimaryDark() {
        if (mResources != null) {
            int identify = mResources.getIdentifier("colorPrimaryDark", "color", skinPackageName);
            if (identify > 0) {
                return mResources.getColor(identify);
            }
        }
        return -1;
    }

    boolean isExternalSkin() {
        return !isDefaultSkin && mResources != null;
    }

    public String getCurSkinPackageName() {
        return skinPackageName;
    }

    public Resources getResources() {
        return mResources;
    }

    public void restoreDefaultTheme() {
        SkinConfig.saveSkinPath(context, SkinConfig.DEFAULT_SKIN);
        isDefaultSkin = true;
        SkinConfig.setNightMode(context, false);
        mResources = context.getResources();
        skinPackageName = context.getPackageName();
        notifySkinUpdate();
    }

    @Override
    public void attach(ISkinUpdate observer) {
        if (mSkinObservers == null) {
            mSkinObservers = new ArrayList<>();
        }
        if (!mSkinObservers.contains(observer)) {
            mSkinObservers.add(observer);
        }
    }

    @Override
    public void detach(ISkinUpdate observer) {
        if (mSkinObservers != null && mSkinObservers.contains(observer)) {
            mSkinObservers.remove(observer);
        }
    }

    @Override
    public void notifySkinUpdate() {
        if (mSkinObservers != null) {
            for (ISkinUpdate observer : mSkinObservers) {
                observer.onThemeUpdate();
            }
        }
    }

    public boolean isNightMode() {
        return SkinConfig.isInNightMode(context);
    }

    //region Load skin or font


    /**
     * load skin form local
     * <p>
     * eg:theme.skin
     * </p>
     *
     * @param skinName the name of skin(in assets/skin)
     * @param callback load Callback
     */
    @SuppressLint("StaticFieldLeak")
    public void loadSkin(String skinName, final SkinLoaderListener callback) {
        new AsyncTask<String, Void, Resources>() {
            @Override
            protected void onPreExecute() {
                if (callback != null) {
                    callback.onStart();
                }
            }

            @Override
            protected Resources doInBackground(String... params) {
                try {
                    String skinPkgPath = SkinFileUtils.getSkinDir(context) + File.separator + params[0];
                    SkinL.i(TAG, "skinPackagePath:" + skinPkgPath);
                    File file = new File(skinPkgPath);
                    if (!file.exists()) {
                        return null;
                    }
                    PackageManager mPm = context.getPackageManager();
                    PackageInfo mInfo = mPm.getPackageArchiveInfo(skinPkgPath, PackageManager.GET_ACTIVITIES);
                    skinPackageName = Objects.requireNonNull(mInfo).packageName;
                    AssetManager assetManager = AssetManager.class.newInstance();
                    Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
                    addAssetPath.invoke(assetManager, skinPkgPath);

                    Resources superRes = context.getResources();
                    Resources skinResource = ResourcesCompat.getResources(assetManager, superRes.getDisplayMetrics(), superRes.getConfiguration());
                    SkinConfig.saveSkinPath(context, params[0]);
                    isDefaultSkin = false;
                    return skinResource;
                } catch (Exception e) {
                    Log.e("Skin", e.getMessage() + "");
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Resources result) {
                mResources = result;
                if (mResources != null) {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    SkinConfig.setNightMode(context, false);
                    notifySkinUpdate();
                } else {
                    isDefaultSkin = true;
                    if (callback != null) {
                        callback.onFailed("没有获取到资源");
                    }
                }
            }

        }.execute(skinName);
    }


    /**
     * load font
     *
     * @param fontName font name in assets/fonts
     */
    public void loadFont(String fontName) {
        Typeface tf = TypefaceUtils.createTypeface(context, fontName);
        TextViewRepository.applyFont(tf);
    }

    public void nightMode() {
        if (!isDefaultSkin) {
            restoreDefaultTheme();
        }
        SkinConfig.setNightMode(context, true);
        notifySkinUpdate();
    }
    //endregion

    //region Resource obtain
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public int getColor(int resId) {
        int originColor = ContextCompat.getColor(context, resId);
        if (mResources == null || isDefaultSkin) {
            return originColor;
        }

        String resName = context.getResources().getResourceEntryName(resId);

        int trueResId = mResources.getIdentifier(resName, "color", skinPackageName);
        int trueColor;
        if (trueResId == 0) {
            trueColor = originColor;
        } else {
            trueColor = mResources.getColor(trueResId);
        }
        return trueColor;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ColorStateList getNightColorStateList(int resId) {

        String resName = mResources.getResourceEntryName(resId);
        String resNameNight = resName + "_night";
        int nightResId = mResources.getIdentifier(resNameNight, "color", skinPackageName);
        if (nightResId == 0) {
            return ContextCompat.getColorStateList(context, resId);
        } else {
            return ContextCompat.getColorStateList(context, nightResId);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public int getNightColor(int resId) {

        String resName = mResources.getResourceEntryName(resId);
        String resNameNight = resName + "_night";
        int nightResId = mResources.getIdentifier(resNameNight, "color", skinPackageName);
        if (nightResId == 0) {
            return ContextCompat.getColor(context, resId);
        } else {
            return ContextCompat.getColor(context, nightResId);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Drawable getNightDrawable(String resName) {

        String resNameNight = resName + "_night";

        int nightResId = mResources.getIdentifier(resNameNight, "drawable", skinPackageName);
        if (nightResId == 0) {
            nightResId = mResources.getIdentifier(resNameNight, "mipmap", skinPackageName);
        }
        Drawable color;
        if (nightResId == 0) {
            int resId = mResources.getIdentifier(resName, "drawable", skinPackageName);
            if (resId == 0) {
                resId = mResources.getIdentifier(resName, "mipmap", skinPackageName);
            }
            color = mResources.getDrawable(resId);
        } else {
            color = mResources.getDrawable(nightResId);
        }
        return color;
    }

    /**
     * get drawable from specific directory
     *
     * @param resId res id
     * @param dir   res directory
     * @return drawable
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Drawable getDrawable(int resId, String dir) {
        Drawable originDrawable = ContextCompat.getDrawable(context, resId);
        if (mResources == null || isDefaultSkin) {
            return originDrawable;
        }
        String resName = context.getResources().getResourceEntryName(resId);
        int trueResId = mResources.getIdentifier(resName, dir, skinPackageName);
        Drawable trueDrawable;
        if (trueResId == 0) {
            trueDrawable = originDrawable;
        } else {
            if (android.os.Build.VERSION.SDK_INT < 22) {
                trueDrawable = mResources.getDrawable(trueResId);
            } else {
                trueDrawable = mResources.getDrawable(trueResId, null);
            }
        }
        return trueDrawable;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Drawable getDrawable(int resId) {
        Drawable originDrawable = ContextCompat.getDrawable(context, resId);
        if (mResources == null || isDefaultSkin) {
            return originDrawable;
        }
        String resName = context.getResources().getResourceEntryName(resId);
        int trueResId = mResources.getIdentifier(resName, "drawable", skinPackageName);
        Drawable trueDrawable;
        if (trueResId == 0) {
            trueResId = mResources.getIdentifier(resName, "mipmap", skinPackageName);
        }
        if (trueResId == 0) {
            trueDrawable = originDrawable;
        } else {
            if (android.os.Build.VERSION.SDK_INT < 22) {
                trueDrawable = mResources.getDrawable(trueResId);
            } else {
                trueDrawable = mResources.getDrawable(trueResId, null);
            }
        }
        return trueDrawable;
    }

    /**
     * 加载指定资源颜色drawable,转化为ColorStateList，保证selector类型的Color也能被转换。
     * 无皮肤包资源返回默认主题颜色
     * author:pinotao
     *
     * @param resId resources id
     * @return ColorStateList
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ColorStateList getColorStateList(int resId) {
        boolean isExternalSkin = true;
        if (mResources == null || isDefaultSkin) {
            isExternalSkin = false;
        }

        String resName = context.getResources().getResourceEntryName(resId);
        if (isExternalSkin) {
            int trueResId = mResources.getIdentifier(resName, "color", skinPackageName);
            ColorStateList trueColorList;
            if (trueResId == 0) { // 如果皮肤包没有复写该资源，但是需要判断是否是ColorStateList

                return ContextCompat.getColorStateList(context, resId);
            } else {
                trueColorList = mResources.getColorStateList(trueResId);
                return trueColorList;
            }
        } else {
            return ContextCompat.getColorStateList(context, resId);
        }
    }
    //endregion
}
