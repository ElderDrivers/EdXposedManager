package de.robv.android.xposed.installer.installation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.coxylicacid.mdwidgets.dialog.MD2Dialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.solohsu.android.edxp.manager.BuildConfig;
import com.solohsu.android.edxp.manager.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.activity.EasterEggActivity;
import de.robv.android.xposed.installer.activity.PingActivity;
import de.robv.android.xposed.installer.anim.ViewSizeChangeAnimation;
import de.robv.android.xposed.installer.util.InstallApkUtil;
import de.robv.android.xposed.installer.util.InstallZipUtil;
import de.robv.android.xposed.installer.util.ModuleUtil;
import de.robv.android.xposed.installer.util.NavUtil;
import de.robv.android.xposed.installer.util.RootUtil;
import de.robv.android.xposed.installer.util.ThemeUtil;
import de.robv.android.xposed.installer.widget.XposedStatusPanel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.supercharge.shimmerlayout.ShimmerLayout;
import rxhttp.wrapper.param.RxHttp;
import solid.ren.skinlibrary.SkinLoaderListener;
import solid.ren.skinlibrary.base.SkinBaseFragment;
import solid.ren.skinlibrary.loader.SkinManager;

import static de.robv.android.xposed.installer.XposedApp.WRITE_EXTERNAL_PERMISSION;

@SuppressLint("StaticFieldLeak")
public class StatusInstallerFragment extends SkinBaseFragment {

    public static final File DISABLE_FILE = new File(XposedApp.BASE_DIR + "conf/disabled");
    public static String ARCH = getArch();
    private static Activity sActivity;
    private static Fragment sFragment;
    private static String mUpdateLink;
    private static ImageView mErrorIcon;
    private static View mUpdateButton;
    private static TextView mErrorTv;
    private static boolean isXposedInstalled = false;
    private TextView txtKnownIssue;
    private long lastClickTime = 0;
    private int clickCount = 0;
    private static String mVersion;
    private boolean isHelperShow = true;
    private boolean isPanelAnimeStarted = false;
    private int statusPanelHeight;
    private int defaultPanelHeight;

    public static void setError(boolean connectionFailed, boolean noSdks) {
        if (!connectionFailed && !noSdks) {
            if (isXposedInstalled) return;
            return;
        }

        mErrorTv.setVisibility(View.GONE);
        mErrorIcon.setVisibility(View.GONE);
        if (noSdks) {
            mErrorIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_warning_grey));
            mErrorTv.setText(String.format(sActivity.getString(R.string.phone_not_compatible), Build.VERSION.SDK_INT, Build.CPU_ABI));
        }
        if (connectionFailed) {
            mErrorIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_no_connection));
            mErrorTv.setText(sActivity.getString(R.string.loadingError));
        }
    }

    public static void setUpdate(final String link, final String changelog, String version) {
        mUpdateLink = link;
        mVersion = version;
        mUpdateButton.setVisibility(View.VISIBLE);
        mUpdateButton.setOnClickListener(v -> new MD2Dialog(sActivity)
                .title(R.string.changes)
                .msg(BuildConfig.VERSION_NAME + " -> " + changelog)
                .buttonStyle(MD2Dialog.ButtonStyle.FLAT)
                .darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                .onConfirmClick("更新", (view, dialog) -> {
                    update();
                    dialog.dismiss();
                })
                .onCancelClick("稍后", (view, dialog) -> dialog.dismiss())
                .onNegativeClick("Github", ((view, dialog) -> NavUtil.startURL(sActivity, Uri.parse("https://github.com/coxylicacid/Xposed-Fast-Repo")))).show());
    }

    @SuppressLint("CheckResult")
    private static void update() {
        if (checkPermissions()) return;

        final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/EdXpMaterial_" + mVersion + ".apk";

        //noinspection ResultOfMethodCallIgnored
        new File(path).delete();

        MD2Dialog alert = MD2Dialog.create(sActivity)
                .title(mVersion)
                .msg(R.string.download_view_waiting)
                .progress(false, 100)
                .kbs("")
                .canceledOnTouchOutside(false)
                .darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                .onCancelClick(R.string.download_view_cancel, (view, dialog) -> dialog.dismiss()).show();

        long length = new File(path).length();
        //noinspection ResultOfMethodCallIgnored
        RxHttp.get(mUpdateLink)
                .setRangeHeader(length)
                .asDownload(path, length, progress -> {
                    alert.updateProgress(progress.getProgress());
                    alert.kbs(fileSize(progress.getCurrentSize()) + "/" + fileSize(progress.getTotalSize()));
                }, AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    InstallApkUtil.installApkNormally(sActivity, s);
                    Toast.makeText(sActivity, "文件已保存至: " + s, Toast.LENGTH_LONG).show();
                    alert.dismiss();
                }, throwable -> {
                    throwable.printStackTrace();
                    Toast.makeText(sActivity, "下载失败", Toast.LENGTH_LONG).show();
                });
    }

    public static String fileSize(long size) {
        return Formatter.formatFileSize(sActivity, size);
    }

    public static boolean checkPermissions() {
        if (Build.VERSION.SDK_INT < 23) return false;

        if (ActivityCompat.checkSelfPermission(sActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            sFragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
            return true;
        }
        return false;
    }

    private static boolean checkClassExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static String getCompleteArch() {
        String info = "";

        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text;
            while ((text = br.readLine()) != null) {
                if (!text.startsWith("processor")) break;
            }
            br.close();
            String[] array = text != null ? text.split(":\\s+", 2) : new String[0];
            if (array.length >= 2) {
                info += array[1] + " ";
            }
        } catch (IOException ignored) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            info += Build.SUPPORTED_ABIS[0];
        } else {
            String arch = System.getenv("os.arch");
            if (arch != null) info += arch;
        }
        return info + " (" + getArch() + ")";
    }

    @SuppressWarnings("deprecation")
    private static String getArch() {
        if (Build.CPU_ABI.equals("arm64-v8a")) {
            return "arm64";
        } else if (Build.CPU_ABI.equals("x86_64")) {
            return "x86_64";
        } else if (Build.CPU_ABI.equals("mips64")) {
            return "mips64";
        } else if (Build.CPU_ABI.startsWith("x86") || Build.CPU_ABI2.startsWith("x86")) {
            return "x86";
        } else if (Build.CPU_ABI.startsWith("mips")) {
            return "mips";
        } else if (Build.CPU_ABI.startsWith("armeabi-v5") || Build.CPU_ABI.startsWith("armeabi-v6")) {
            return "armv5";
        } else {
            return "arm";
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sActivity = getActivity();
        sFragment = this;
        defaultPanelHeight = dp(100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                }, 500);
            } else {
                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.status_installer, container, false);

        View nightMask = v.findViewById(R.id.night_mask);
        nightMask.setVisibility(ThemeUtil.getSelectTheme().equals("dark") ? View.VISIBLE : View.GONE);
        TextView app_ver = v.findViewById(R.id.app_ver_code);
        String packageName = getActivity().getPackageName();
        String version = null;
        try {
            version = getActivity().getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        app_ver.setText(version);

        mUpdateButton = v.findViewById(R.id.click_to_update);

        txtKnownIssue = v.findViewById(R.id.framework_known_issue_);

        TextView txtInstallError = v.findViewById(R.id.xp_ver);
        XposedStatusPanel panel = v.findViewById(R.id.status_container_bg);
        TextView helpertext = v.findViewById(R.id.helper_text);
        MaterialButton helperButton = v.findViewById(R.id.helper_button);
        ImageView txtInstallIcon = v.findViewById(R.id.status_icon_);
        ImageView helperExpander = v.findViewById(R.id.helperExpander);
        TextView installStatus = v.findViewById(R.id.install_status);
        View defaultSkin = v.findViewById(R.id.loadDefaultSkin);
        View loadSkin = v.findViewById(R.id.loadSkin);

        helperExpander.setRotation(-180f);

        dynamicAddView(panel, "errorBackground", R.drawable.status_bg_error);
        dynamicAddView(panel, "warnBackground", R.drawable.status_bg_warn);
        dynamicAddView(panel, "successBackground", R.drawable.status_bg);

        String installedXposedVersion;
        try {
            installedXposedVersion = XposedApp.getXposedProp().getVersion();
        } catch (NullPointerException e) {
            installedXposedVersion = null;
        }

        View disableView = v.findViewById(R.id.disableView_);
        final Switch xposedDisable = v.findViewById(R.id.disableSwitch_);

        TextView androidSdk = v.findViewById(R.id.android_version);
        TextView manufacturer = v.findViewById(R.id.ic_manufacturer);
        TextView cpu = v.findViewById(R.id.cpu);

        if (installedXposedVersion != null) {
            int installedXposedVersionInt = extractIntPart(installedXposedVersion);
            if (installedXposedVersionInt == XposedApp.getXposedVersion()) {
                txtInstallError.setText(sActivity.getString(R.string.xp_ver, installedXposedVersion));
                installStatus.setText("已安装");
                panel.setXposedState(XposedStatusPanel.XPOSED_STATE_SUCCESS);
                txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_check_circle));
                panel.setOnClickListener(null);
                isXposedInstalled = true;

                RootUtil rootUtil = new RootUtil();
                helperButton.setText("详情");
                //, SeLinux正处于 %s 模式, 模块处于%s激活状态
                String installedInfo = "您已安装EdXposed，目前的版本为: %s, 管理器版本为: %s, 您的手机中一共安装了%s个模块";
                helpertext.setText(String.format(installedInfo,
                        installedXposedVersion, getString(R.string.app_ver_code), ModuleUtil.getInstance().getModules().size(), "宽容", "可"));
            } else {
                txtInstallError.setText(sActivity.getString(R.string.xp_ver, installedXposedVersion));
                installStatus.setText("未启用");
                panel.setXposedState(XposedStatusPanel.XPOSED_STATE_WARN);
                txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_warning));
                helpertext.setText(getString(R.string.xp_not_active_info));
                helperButton.setText(R.string.reboot);
                helperButton.setOnClickListener(v1 -> {
                    if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                        MD2Dialog.create(requireActivity())
                                .title(R.string.reboot).msg(R.string.areyousure).simpleCancel(android.R.string.cancel)
                                .onConfirmClick(android.R.string.yes, (view, dialog) -> RootUtil.reboot(RootUtil.RebootMode.NORMAL, requireContext())).show();
                    } else {
                        RootUtil.reboot(RootUtil.RebootMode.NORMAL, requireContext());
                    }
                });
            }
        } else {
            txtInstallError.setText("未安装");
            installStatus.setText("未安装");
            panel.setXposedState(XposedStatusPanel.XPOSED_STATE_ERROR);
            txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_error));
            xposedDisable.setVisibility(View.GONE);
            disableView.setVisibility(View.GONE);
            helpertext.setText(getString(R.string.xposed_install_helper_info));
            helperButton.setOnClickListener(v1 -> {
            });
        }

        panel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // TODO Auto-generated method stub
                panel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                statusPanelHeight = panel.getMeasuredHeight();
            }
        });

        xposedDisable.setChecked(!DISABLE_FILE.exists());

        xposedDisable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (DISABLE_FILE.exists()) {
                DISABLE_FILE.delete();
                Snackbar.make(xposedDisable, R.string.xposed_on_next_reboot, Snackbar.LENGTH_LONG).show();
            } else {
                try {
                    DISABLE_FILE.createNewFile();
                    Snackbar.make(xposedDisable, R.string.xposed_off_next_reboot, Snackbar.LENGTH_LONG).show();
                } catch (IOException e) {
                    Log.e(XposedApp.TAG, "StatusInstallerFragment -> " + e.getMessage());
                }
            }
        });

        panel.setOnClickListener(v1 -> {
            if (!isPanelAnimeStarted) {
                panel.clearAnimation();
//                Log.e("STATUS", "isHelperShow: " + isHelperShow + ", panelExpandedHeight: " + statusPanelHeight + ", panelUnExpandedHeight: " + defaultPanelHeight);
//                Log.e("STATUS", "currentHeight: " + (isHelperShow ? defaultPanelHeight : statusPanelHeight));
                ViewSizeChangeAnimation animation = new ViewSizeChangeAnimation(panel,
                        (isHelperShow ? defaultPanelHeight : statusPanelHeight),
                        (isHelperShow ? panel.getWidth() : panel.getWidth()));
                animation.setDuration(150);
                animation.setFillAfter(true);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        isPanelAnimeStarted = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        isPanelAnimeStarted = false;
                        isHelperShow = !isHelperShow;
                        panel.clearAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        panel.clearAnimation();
                    }
                });

                Animation rotation = new RotateAnimation(isHelperShow ? -180f : 0, isHelperShow ? 0 : -180f, helperExpander.getPivotX(), helperExpander.getPivotY());
                rotation.setDuration(150);
                rotation.setFillAfter(true);

                helperExpander.startAnimation(rotation);
                panel.setAnimation(animation);
                panel.startLayoutAnimation();
            }
        });

        defaultSkin.setOnClickListener(v1 -> SkinManager.getInstance().restoreDefaultTheme());

        loadSkin.setOnClickListener(v1 -> {
            if (checkPermissions()) return;
            SkinManager.getInstance().loadSkin("theme-20191103.skin", new SkinLoaderListener() {
                @Override
                public void onStart() {
                    Toast.makeText(getContext(), "正在切换中", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "切换成功", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailed(String errMsg) {
                    Toast.makeText(getContext(), "切换失败", Toast.LENGTH_SHORT).show();
                }
            });
        });

        androidSdk.setText(

                getString(R.string.android_sdk, getAndroidVersion(), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        manufacturer.setText(

                getUIFramework());
        cpu.setText(

                getCompleteArch());

        determineVerifiedBootState(v);

        refreshKnownIssue();

        RelativeLayout egg = v.findViewById(R.id.easter_egg);
//        Toast toast = Toast.makeText(getContext(), "彩蛋", Toast.LENGTH_SHORT);
        egg.setOnClickListener(view ->

        {
//            if (System.currentTimeMillis() - lastClickTime < 300) {
//                if (clickCount >= 5 && clickCount < 10) {
//                    toast.show();
//                    toast.setText("再点击" + (10 - clickCount) + "次就能开启彩蛋");
//                } else if (clickCount >= 10) {
//
//                }
//                clickCount++;
//            } else {
//                clickCount = 0;
//                toast.cancel();
//            }
            startActivity(new Intent(getActivity(), EasterEggActivity.class));
//            lastClickTime = System.currentTimeMillis();
        });

        ShimmerLayout shimmer = v.findViewById(R.id.shimmer);
        if (XposedApp.getPreferences().

                getBoolean("mask_animate", true)) {
            shimmer.startShimmerAnimation();
        } else {
            shimmer.stopShimmerAnimation();
        }


        LinearLayout checkConnection = v.findViewById(R.id.checkConnection);
        checkConnection.setOnClickListener(v1 -> startActivity(new Intent(getActivity(), PingActivity.class)));
        return v;
    }

    private void determineVerifiedBootState(View v) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("get", String.class, String.class);
            m.setAccessible(true);

            String propSystemVerified = (String) m.invoke(null, "partition.system.verified", "0");
            String propState = (String) m.invoke(null, "ro.boot.verifiedbootstate", "");
            File fileDmVerityModule = new File("/sys/module/dm_verity");

            boolean verified = !propSystemVerified.equals("0");
            boolean detected = !propState.isEmpty() || fileDmVerityModule.exists();

            TextView tv = v.findViewById(R.id.dmverity);
            if (verified) {
                tv.setText(R.string.verified_boot_active);
                tv.setTextColor(getResources().getColor(R.color.warning));
            } else if (detected) {
                tv.setText(R.string.verified_boot_deactivated);
                v.findViewById(R.id.dmverity_explanation).setVisibility(View.GONE);
            } else {
                v.findViewById(R.id.dmverity_row).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(XposedApp.TAG, "Could not detect Verified Boot state", e);
        }
    }

    @SuppressLint("StringFormatInvalid")
    private void refreshKnownIssue() {
        String issueName = null;
        String issueLink = null;
        final ApplicationInfo appInfo = Objects.requireNonNull(getActivity()).getApplicationInfo();
        final Set<String> missingFeatures = XposedApp.getXposedProp() == null ? new HashSet<>() : XposedApp.getXposedProp().getMissingInstallerFeatures();
        final File baseDir = new File(XposedApp.BASE_DIR);
        final File baseDirCanonical = getCanonicalFile(baseDir);
        final File baseDirActual = new File(Build.VERSION.SDK_INT >= 24 ? appInfo.deviceProtectedDataDir : appInfo.dataDir);
        final File baseDirActualCanonical = getCanonicalFile(baseDirActual);

        if (!missingFeatures.isEmpty()) {
            InstallZipUtil.reportMissingFeatures(missingFeatures);
            issueName = getString(R.string.installer_needs_update, getString(R.string.app_name_full));
            issueLink = getString(R.string.about_support);
        } else if (new File("/system/framework/core.jar.jex").exists()) {
            issueName = "Aliyun OS";
            issueLink = "https://forum.xda-developers.com/showpost.php?p=52289793&postcount=5";
        } else if (Build.VERSION.SDK_INT < 24 && (new File("/data/miui/DexspyInstaller.jar").exists() || checkClassExists("miui.dexspy.DexspyInstaller"))) {
            issueName = "MIUI/Dexspy";
            issueLink = "https://forum.xda-developers.com/showpost.php?p=52291098&postcount=6";
        } else if (Build.VERSION.SDK_INT < 24 && new File("/system/framework/twframework.jar").exists()) {
            issueName = "Samsung TouchWiz ROM";
            issueLink = "https://forum.xda-developers.com/showthread.php?t=3034811";
        } else if (!baseDirCanonical.equals(baseDirActualCanonical)) {
            Log.e(XposedApp.TAG, "Base directory: " + getPathWithCanonicalPath(baseDir, baseDirCanonical));
            Log.e(XposedApp.TAG, "Expected: " + getPathWithCanonicalPath(baseDirActual, baseDirActualCanonical));
            issueName = getString(R.string.known_issue_wrong_base_directory, getPathWithCanonicalPath(baseDirActual, baseDirActualCanonical));
        } else if (!baseDir.exists()) {
            issueName = getString(R.string.known_issue_missing_base_directory);
            issueLink = "https://github.com/rovo89/XposedInstaller/issues/393";
        } else if (!checkAppInstalled(getContext(), "com.solohsu.android.edxp.manager")) {
            issueName = getString(R.string.edxp_installer_installed);
            issueLink = getString(R.string.about_support);
        }

        if (issueName != null) {
            final String issueLinkFinal = issueLink;
            txtKnownIssue.setText(getString(R.string.install_known_issue, issueName));
            txtKnownIssue.setVisibility(View.VISIBLE);
            txtKnownIssue.setOnClickListener(v -> NavUtil.startURL(getActivity(), issueLinkFinal));
        } else {
            txtKnownIssue.setVisibility(View.GONE);
        }
    }

    private boolean checkAppInstalled(Context context, String pkgName) {
        if (pkgName == null || pkgName.isEmpty()) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> info = packageManager.getInstalledPackages(0);
        if (info == null || info.isEmpty()) {
            return false;
        }
        for (int i = 0; i < info.size(); i++) {
            if (pkgName.equals(info.get(i).packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_installer, menu);
        if (Build.VERSION.SDK_INT < 26) {
            menu.findItem(R.id.dexopt_all).setVisible(false);
            menu.findItem(R.id.speed_all).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dexopt_all:
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.dexopt_now)
                        .content(R.string.this_may_take_a_while)
                        .progress(true, 0)
                        .cancelable(false)
                        .showListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(final DialogInterface dialog) {
                                new Thread("dexopt") {
                                    @Override
                                    public void run() {
                                        RootUtil rootUtil = new RootUtil();
                                        if (!rootUtil.startShell()) {
                                            dialog.dismiss();
                                            NavUtil.showMessage(getActivity(), getString(R.string.root_failed));
                                            return;
                                        }

                                        rootUtil.execute("cmd package bg-dexopt-job", new ArrayList<String>());

                                        dialog.dismiss();
                                        XposedApp.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(getActivity(), R.string.done, Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }.start();
                            }
                        }).show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private String getAndroidVersion() {
        switch (Build.VERSION.SDK_INT) {
            case 16:
            case 17:
            case 18:
                return "Jelly Bean";
            case 19:
                return "KitKat";
            case 21:
            case 22:
                return "Lollipop";
            case 23:
                return "Marshmallow";
            case 24:
            case 25:
                return "Nougat";
            case 26:
            case 27:
                return "Oreo";
            case 28:
                return "Pie";
        }
        return "";
    }

    private String getUIFramework() {
        String manufacturer = Character.toUpperCase(Build.MANUFACTURER.charAt(0)) + Build.MANUFACTURER.substring(1);
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            manufacturer += " " + Character.toUpperCase(Build.BRAND.charAt(0)) + Build.BRAND.substring(1);
        }
        manufacturer += " " + Build.MODEL + " ";
        if (manufacturer.contains("Samsung")) {
            manufacturer += new File("/system/framework/twframework.jar").exists() ||
                    new File("/system/framework/samsung-services.jar").exists()
                    ? "(TouchWiz)" : "(AOSP-based ROM)";
        } else if (manufacturer.contains("Xiaomi")) {
            manufacturer += new File("/system/framework/framework-miui-res.apk").exists() ? "(MIUI)" : "(AOSP-based ROM)";
        }
        return manufacturer;
    }

    private File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            Log.e(XposedApp.TAG, "Failed to get canonical file for " + file.getAbsolutePath(), e);
            return file;
        }
    }

    private String getPathWithCanonicalPath(File file, File canonical) {
        if (file.equals(canonical)) {
            return file.getAbsolutePath();
        } else {
            return file.getAbsolutePath() + " \u2192 " + canonical.getAbsolutePath();
        }
    }

    private int extractIntPart(String str) {
        int result = 0, length = str.length();
        for (int offset = 0; offset < length; offset++) {
            char c = str.charAt(offset);
            if ('0' <= c && c <= '9')
                result = result * 10 + (c - '0');
            else
                break;
        }
        return result;
    }

    private int dp(float value) {
        float density = getResources().getDisplayMetrics().density;
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }
}