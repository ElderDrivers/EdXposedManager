package de.robv.android.xposed.installer.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.fragment.BasePreferenceFragment;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import de.robv.android.xposed.installer.WelcomeActivity;
import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.ThemeUtil;

public class PersonalizeFragment extends BasePreferenceFragment {

    private static final int REQUEST_CODE = 777;
    private static final int GALLERY_CODE = 2;
    private static final int CROP_CODE = 3;
    private AlertDialog panelChooseDialog;
    private File img = null;
    private Bitmap panel_bg = null;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_personalize);

        img = new File(requireActivity().getExternalFilesDir("img"), "panel_background.png");
        panel_bg = img.exists() ? BitmapFactory.decodeFile(img.getAbsolutePath()) : null;

        Preference theme = findPreference("manager_theme");
        Preference panelBackground = findPreference("panel_background");
        Preference nightModePref = findPreference("night_mode");

        Preference drawer = findPreference("drawer_layout_home");

        Objects.requireNonNull(drawer).setOnPreferenceChangeListener((preference, newValue) -> {
            recreateActivity();
            return true;
        });

        if (theme != null) {
            theme.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setSingleChoiceItems(new CharSequence[]{
                                ThemeUtil.ThemeName.get(0), ThemeUtil.ThemeName.get(1), ThemeUtil.ThemeName.get(2),
                                ThemeUtil.ThemeName.get(3), ThemeUtil.ThemeName.get(4), ThemeUtil.ThemeName.get(5),
                                ThemeUtil.ThemeName.get(6), ThemeUtil.ThemeName.get(7)
                        }, ThemeUtil.getAppAccent(), (dialog, which) -> {
                            ThemeUtil.setTheme(which);
                            dialog.dismiss();
                            recreateActivity();
                        }).show();
                return true;
            });
        }

        if (panelBackground != null) {
            panelBackground.setOnPreferenceClickListener(preference -> {
                panelChooseDialog = new MaterialAlertDialogBuilder(requireActivity())
                        .setView(R.layout.status_panel_dialog)
                        .setTitle("面板背景图")
                        .setPositiveButton(android.R.string.ok, (dialog1, which) -> dialog1.dismiss()).show();
                Switch enableSwitch = panelChooseDialog.findViewById(R.id.enableSwitch);
                Switch isDarkBackground = panelChooseDialog.findViewById(R.id.isDarkBackground);
                ImageView panelView = panelChooseDialog.findViewById(R.id.panel_view);
                ImageView chooseBackground = panelChooseDialog.findViewById(R.id.chooseBackground);
                ImageView isDarckIcon = panelChooseDialog.findViewById(R.id.dark_bg);
                if (img != null && img.exists() && panel_bg != null) {
                    Objects.requireNonNull(panelView).setImageBitmap(panel_bg);
                }
                Objects.requireNonNull(enableSwitch).setChecked(XposedApp.getPreferences().getBoolean("enabled_panel_background", false));
                Objects.requireNonNull(isDarkBackground).setChecked(XposedApp.getPreferences().getBoolean("is_dark_panel_background", true));

                isDarkBackground.setEnabled(enableSwitch.isChecked());

                Objects.requireNonNull(enableSwitch).setOnClickListener(v -> {
                    XposedApp.getPreferences().edit().putBoolean("enabled_panel_background", ((Switch) v).isChecked()).apply();
                    isDarkBackground.setEnabled(enableSwitch.isChecked());
                });
                Objects.requireNonNull(isDarkBackground).setOnClickListener(v -> XposedApp.getPreferences().edit().putBoolean("is_dark_panel_background", ((Switch) v).isChecked()).apply());
                Objects.requireNonNull(chooseBackground).setOnClickListener(v -> chooseFromGallery());
                return true;
            });
        }

        if (nightModePref != null) {
            nightModePref.setOnPreferenceChangeListener((preference, o) -> {
                if (o instanceof String) {
                    int mode = Integer.valueOf((String) o);
                    if (XposedApp.getNightMode() != mode) {
                        AppCompatDelegate.setDefaultNightMode(mode);
                        recreateActivity();
                    }
                }
                return true;
            });
        }
    }

    private void chooseFromGallery() {
        // 构建一个内容选择的Intent
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // 设置选择类型为图片类型
        intent.setType("image/*");
        // 打开图片选择
        startActivityForResult(intent, GALLERY_CODE);
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        return recyclerView;
    }

    private void recreateActivity() {
        if (getActivity() != null) {
            getActivity().finish();
            startActivity(new Intent(getActivity(), WelcomeActivity.class));
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GALLERY_CODE:
                if (data == null) {
                    return;
                } else {
                    int width = XposedApp.getPreferences().getInt("suggested_panel_width", 1080);
                    int height = XposedApp.getPreferences().getInt("suggested_panel_height", 540);
                    // 用户从图库选择图片后会返回所选图片的Uri
                    String path = convertUri(data.getData());
                    Uri uri = Uri.fromFile(new File(Objects.requireNonNull(path)));
                    Uri destinationUri = Uri.fromFile(new File(requireActivity().getExternalFilesDir("img"), "panel_background.png"));
                    UCrop.Options options = new UCrop.Options();
                    options.setToolbarColor(requireActivity().getColor(R.color.colorAccent));
                    options.setStatusBarColor(requireActivity().getColor(R.color.colorAccent));
                    options.setCropGridColor(requireActivity().getColor(R.color.colorAccent));
                    options.setShowCropGrid(true);
                    UCrop.of(uri, destinationUri)
                            .withAspectRatio(16, 9)
                            .withOptions(options)
                            .withMaxResultSize(width, height)
                            .start(requireContext(), this);
                }
                break;
            case UCrop.REQUEST_CROP:
                if (data == null) {
                    return;
                } else {
                    Toast.makeText(requireActivity(), "以设置您的面板图片，快去看看吧，伙计！", Toast.LENGTH_LONG).show();
                    panel_bg = BitmapFactory.decodeFile(img.getAbsolutePath());
                    if (panelChooseDialog != null && panelChooseDialog.isShowing()) {
                        ImageView panelView = panelChooseDialog.findViewById(R.id.panel_view);
                        Objects.requireNonNull(panelView).setImageBitmap(panel_bg);
                    }
                }
                break;
            default:
                break;
        }
    }

    public static Uri getUriForFile(Context context, File file) {
        Uri fileUri = null;
        //24 android7
        fileUri = FileProvider.getUriForFile(context, "com.solohsu.android.edxp.manager.fileprovider", file);
        return fileUri;
    }

    private String convertUri(Uri uri) {
        InputStream is;
        try {
            is = requireActivity().getContentResolver().openInputStream(uri);
            Bitmap bm = BitmapFactory.decodeStream(is);
            // 关闭流
            Objects.requireNonNull(is).close();
            return saveBitmap(bm, "temp.png");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String saveBitmap(Bitmap bm, String dirPath) {
        // 新建文件存储裁剪后的图片
        File img = new File(requireActivity().getExternalFilesDir("img"), dirPath);
        try {
            // 打开文件输出流
            FileOutputStream fos = new FileOutputStream(img);
            // 将bitmap压缩后写入输出流(参数依次为图片格式、图片质量和输出流)
            bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
            // 刷新输出流
            fos.flush();
            // 关闭输出流
            fos.close();
            // 返回File类型的Uri
            return img.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private int dp(float value) {
        float density = getResources().getDisplayMetrics().density;
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

}
