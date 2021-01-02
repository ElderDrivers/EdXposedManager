package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;

import org.meowcat.annotation.NotProguard;
import org.meowcat.edxposed.manager.util.NavUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import dalvik.system.VMRuntime;

import static android.os.Build.SUPPORTED_32_BIT_ABIS;
import static android.os.Build.SUPPORTED_64_BIT_ABIS;
import static android.os.SELinux.isSELinuxEnabled;
import static org.meowcat.edxposed.manager.MeowCatApplication.TAG;

@SuppressLint("StaticFieldLeak")
public class StatusInstallerFragment extends Fragment {

    public static final File DISABLE_FILE = new File(XposedApp.BASE_DIR + "conf/disabled");
    //static String ARCH = getArch();
    private static Activity sActivity;
    private static String mUpdateLink;
    private static ImageView mErrorIcon;
    private static View mUpdateView;
    private static View mUpdateButton;
    private static TextView mErrorTv;
    private static boolean isXposedInstalled = false;
    private static String CPU_ABI;
    private static String CPU_ABI2;
    private TextView txtKnownIssue;
    private Button btnKnownIssue;

    static void setError(boolean connectionFailed, boolean noSdks) {
        if (!connectionFailed && !noSdks) {
            if (isXposedInstalled) return;
            return;
        }

        mErrorTv.setVisibility(View.VISIBLE);
        mErrorIcon.setVisibility(View.VISIBLE);
        if (noSdks) {
            mErrorIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_warning_grey, null));
            mErrorTv.setText(String.format(sActivity.getString(R.string.phone_not_compatible), Build.VERSION.SDK_INT, CPU_ABI));
        }
        if (connectionFailed) {
            mErrorIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_no_connection, null));
            mErrorTv.setText(sActivity.getString(R.string.loadingError));
        }
    }

    static void setUpdate(final String link, final String changelog, Context mContext) {
        mUpdateLink = link;

        mUpdateView.setVisibility(View.VISIBLE);
        mUpdateButton.setVisibility(View.VISIBLE);
        mUpdateButton.setOnClickListener(v -> new MaterialDialog.Builder(sActivity)
                .title(R.string.changes)
                .content(Html.fromHtml(changelog, Html.FROM_HTML_MODE_COMPACT))
                .onPositive((dialog, which) -> update(mContext))
                .positiveText(R.string.update)
                .negativeText(R.string.later).show());
    }

    private static void update(Context mContext) {
        Uri uri = Uri.parse(mUpdateLink);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        mContext.startActivity(intent);
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

        info += Build.SUPPORTED_ABIS[0];
        return info + " (" + getArch() + ")";
    }

    private static String getArch() {
        if (CPU_ABI.equals("arm64-v8a")) {
            return "arm64";
        } else if (CPU_ABI.equals("x86_64")) {
            return "x86_64";
        } else if (CPU_ABI.equals("mips64")) {
            return "mips64";
        } else if (CPU_ABI.startsWith("x86") || CPU_ABI2.startsWith("x86")) {
            return "x86";
        } else if (CPU_ABI.startsWith("mips")) {
            return "mips";
        } else if (CPU_ABI.startsWith("armeabi-v5") || CPU_ABI.startsWith("armeabi-v6")) {
            return "armv5";
        } else {
            return "arm";
        }
    }

    public static boolean isEnhancementEnabled() {
        return false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String[] abiList;
        if (VMRuntime.getRuntime().is64Bit()) {
            abiList = SUPPORTED_64_BIT_ABIS;
        } else {
            abiList = SUPPORTED_32_BIT_ABIS;
        }
        CPU_ABI = abiList[0];
        if (abiList.length > 1) {
            CPU_ABI2 = abiList[1];
        } else {
            CPU_ABI2 = "";
        }

        sActivity = getActivity();
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.status_installer, container, false);

        mErrorIcon = v.findViewById(R.id.errorIcon);
        mErrorTv = v.findViewById(R.id.errorTv);
        mUpdateView = v.findViewById(R.id.updateView);
        mUpdateButton = v.findViewById(R.id.click_to_update);

        txtKnownIssue = v.findViewById(R.id.framework_known_issue);
        btnKnownIssue = v.findViewById(R.id.framework_known_issue_detail);

        TextView txtInstallError = v.findViewById(R.id.framework_install_errors);
        View txtInstallContainer = v.findViewById(R.id.status_container);
        View noticeContainer = v.findViewById(R.id.noticeView);
        ImageView txtInstallIcon = v.findViewById(R.id.status_icon);

        if (XposedApp.getPreferences().getBoolean("dismiss_manager_notice", false)) {
            noticeContainer.setVisibility(View.GONE);
        } else {
            v.findViewById(R.id.btn_notice_dismiss).setOnClickListener(view -> {
                noticeContainer.setVisibility(View.GONE);
                XposedApp.getPreferences().edit().putBoolean("dismiss_manager_notice", true).apply();
            });
        }

        String installedXposedVersion;
        try {
            installedXposedVersion = XposedApp.getXposedProp().getVersion();
        } catch (NullPointerException e) {
            installedXposedVersion = null;
        }

        View disableView = v.findViewById(R.id.disableView);
        final SwitchCompat xposedDisable = v.findViewById(R.id.disableSwitch);

        TextView api = v.findViewById(R.id.api);
        TextView framework = v.findViewById(R.id.framework);
        TextView manager = v.findViewById(R.id.manager);
        TextView androidSdk = v.findViewById(R.id.android_version);
        TextView manufacturer = v.findViewById(R.id.ic_manufacturer);
        TextView cpu = v.findViewById(R.id.cpu);
        TextView selinux = v.findViewById(R.id.selinux);

        String mAppVer;
        if (isEnhancementEnabled()) {
            mAppVer = String.format("v%s (%s) (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, getString(R.string.status_enhancement));
        } else {
            mAppVer = String.format("v%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        }

        manager.setText(mAppVer);
        if (installedXposedVersion != null) {
            int installedXposedVersionInt = extractIntPart(installedXposedVersion);
            String installedXposedVersionStr = installedXposedVersionInt + ".0";
            api.setText(installedXposedVersionStr);
            framework.setText(installedXposedVersion.replace(installedXposedVersionStr + "-", ""));
            if (installedXposedVersionInt == XposedApp.getActiveXposedVersion()) {
                txtInstallError.setText(R.string.installed_lollipop);
                if (XposedApp.getPreferences().getBoolean("old_success_color", false)) {
                    txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.download_status_update_available, null));
                    txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.download_status_update_available, null));
                } else {
                    txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.status_success, null));
                    txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.status_success, null));
                }
                txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_check_circle, null));
                isXposedInstalled = true;
            } else {
                txtInstallError.setText(R.string.installed_lollipop_inactive);
                txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.amber_500, null));
                txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.amber_500, null));
                txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_warning, null));
            }
        } else {
            txtInstallError.setText(R.string.not_installed_no_lollipop);
            txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.warning, null));
            txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.warning, null));
            txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_error, null));
            xposedDisable.setVisibility(View.GONE);
            disableView.setVisibility(View.GONE);
        }

        xposedDisable.setChecked(!DISABLE_FILE.exists());

        xposedDisable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (DISABLE_FILE.exists()) {
                DISABLE_FILE.delete();
                Snackbar.make(xposedDisable, R.string.xposed_on_next_reboot, Snackbar.LENGTH_LONG).show();
            } else {
                if (!DISABLE_FILE.exists()) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(DISABLE_FILE.getPath());
                        XposedApp.setFilePermissionsFromMode(DISABLE_FILE.getPath(), Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                                Snackbar.make(xposedDisable, R.string.xposed_off_next_reboot, Snackbar.LENGTH_LONG).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                                try {
                                    DISABLE_FILE.createNewFile();
                                    Snackbar.make(xposedDisable, R.string.xposed_off_next_reboot, Snackbar.LENGTH_LONG).show();
                                } catch (IOException e1) {
                                    Log.e(TAG, "StatusInstallerFragment -> " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        });

        androidSdk.setText(getString(R.string.android_sdk, getAndroidVersion(), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        manufacturer.setText(getUIFramework());
        cpu.setText(getCompleteArch());
        selinux.setText(String.format(getString(R.string.selinux_status), getSELinuxStatus()));

        determineVerifiedBootState(v);

        refreshKnownIssue();
        return v;
    }

    private void determineVerifiedBootState(View v) {
        try {
            String propSystemVerified = SystemProperties.get("partition.system.verified", "0");
            String propState = SystemProperties.get("ro.boot.verifiedbootstate", "");
            File fileDmVerityModule = new File("/sys/module/dm_verity");

            boolean verified = !propSystemVerified.equals("0");
            boolean detected = !propState.isEmpty() || fileDmVerityModule.exists();

            TextView tv = v.findViewById(R.id.dmverity);
            if (verified) {
                tv.setText(R.string.verified_boot_active);
                tv.setTextColor(getResources().getColor(R.color.warning, null));
            } else if (detected) {
                tv.setText(R.string.verified_boot_deactivated);
                v.findViewById(R.id.dmverity_explanation).setVisibility(View.GONE);
            } else {
                tv.setText(R.string.verified_boot_none);
                tv.setTextColor(getResources().getColor(R.color.warning, null));
                v.findViewById(R.id.dmverity_explanation).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not detect Verified Boot state", e);
        }
    }

    private boolean isSELinuxEnforced() {
        boolean result = false;
        final File SELINUX_STATUS_FILE = new File("/sys/fs/selinux/enforce");
        if (SELINUX_STATUS_FILE.exists()) {
            try {
                FileInputStream fis = new FileInputStream(SELINUX_STATUS_FILE);
                int status = fis.read();
                switch (status) {
                    case 49:
                        result = true;
                        break;
                    case 48:
                        result = false;
                        break;
                    default:
                        Log.e(TAG, "Unexpected byte " + status + " in /sys/fs/selinux/enforce");
                }
                fis.close();
            } catch (IOException e) {
                if (e.getMessage().contains("Permission denied")) {
                    result = true;
                } else {
                    Log.e(TAG, "Failed to read SELinux status: " + e.getMessage());
                    result = false;
                }
            }
        }
        return result;
    }

    private String getSELinuxStatus() {
        String result;
        if (isSELinuxEnabled()) {
            if (isSELinuxEnforced()) {
                return "Enforcing";
            } else {
                return "Permissive";
            }
        } else {
            result = "Disabled";
        }
        return result;
    }

    private void refreshKnownIssue() {
        String issueName = null;
        String issueLink = null;
        final ApplicationInfo appInfo = requireActivity().getApplicationInfo();
        final File baseDir = new File(XposedApp.BASE_DIR);
        final File baseDirCanonical = getCanonicalFile(baseDir);
        final File baseDirActual = new File(appInfo.deviceProtectedDataDir);
        final File baseDirActualCanonical = getCanonicalFile(baseDirActual);

        if (!baseDirCanonical.equals(baseDirActualCanonical)) {
            Log.e(TAG, "Base directory: " + getPathWithCanonicalPath(baseDir, baseDirCanonical));
            Log.e(TAG, "Expected: " + getPathWithCanonicalPath(baseDirActual, baseDirActualCanonical));
            issueName = getString(R.string.known_issue_wrong_base_directory, getPathWithCanonicalPath(baseDirActual, baseDirActualCanonical));
        } else if (!baseDir.exists()) {
            issueName = getString(R.string.known_issue_missing_base_directory);
            issueLink = "https://github.com/rovo89/XposedInstaller/issues/393";
        } else if (XposedApp.checkAppInstalled(getContext(), "com.solohsu.android.edxp.manager")) {
            issueName = getString(R.string.edxp_installer_installed);
            issueLink = getString(R.string.about_support);
        }

        if (issueName != null) {
            final String issueLinkFinal = issueLink;
            txtKnownIssue.setText(getString(R.string.install_known_issue, issueName));
            txtKnownIssue.setVisibility(View.VISIBLE);
            if (issueLinkFinal != null) {
                btnKnownIssue.setOnClickListener(v -> NavUtil.startURL(getActivity(), issueLinkFinal));
                btnKnownIssue.setVisibility(View.VISIBLE);
            } else {
                btnKnownIssue.setVisibility(View.GONE);
            }
        } else {
            txtKnownIssue.setVisibility(View.GONE);
            btnKnownIssue.setVisibility(View.GONE);
        }
    }

    private String getAndroidVersion() {
        switch (Build.VERSION.SDK_INT) {
            case 24:
            case 25:
                return "Nougat";
            case 26:
            case 27:
                return "Oreo";
            case 28:
                return "Pie";
            case 29:
                return "Queen Cake";
            case 30:
                return "Red Velvet Cake";
        }
        return "Unknown";
    }

    private String getUIFramework() {
        String manufacturer = Character.toUpperCase(Build.MANUFACTURER.charAt(0)) + Build.MANUFACTURER.substring(1);
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            manufacturer += " " + Character.toUpperCase(Build.BRAND.charAt(0)) + Build.BRAND.substring(1);
        }
        manufacturer += " " + Build.MODEL + " ";
        if (new File("/system/framework/framework-miui-res.apk").exists() || new File("/system/app/miui/miui.apk").exists() || new File("/system/app/miuisystem/miuisystem.apk").exists()) {
            manufacturer += "(MIUI)";
        } else if (new File("/system/priv-app/oneplus-framework-res/oneplus-framework-res.apk").exists()) {
            manufacturer += "(Hydrogen/Oxygen OS)";
        } else if (new File("/system/framework/oppo-framework.jar").exists() || new File("/system/framework/oppo-framework-res.apk").exists() || new File("/system/framework/coloros-framework.jar").exists() || new File("/system/framework/coloros.services.jar").exists() || new File("/system/framework/oppo-services.jar").exists() || new File("/system/framework/coloros-support-wrapper.jar").exists()) {
            manufacturer += "(Color OS)";
        } else if (new File("/system/framework/hwEmui.jar").exists() || new File("/system/framework/hwcustEmui.jar").exists() || new File("/system/framework/hwframework.jar").exists() || new File("/system/framework/framework-res-hwext.apk").exists() || new File("/system/framework/hwServices.jar").exists() || new File("/system/framework/hwcustframework.jar").exists()) {
            manufacturer += "(EMUI)";
        } else if (new File("/system/framework/com.samsung.device.jar").exists() || new File("/system/framework/sec_platform_library.jar").exists()) {
            manufacturer += "(One UI)";
        } else if (new File("/system/priv-app/CarbonDelta/CarbonDelta.apk").exists()) {
            manufacturer += "(Carbon OS)";
        } else if (new File("/system/framework/flyme-framework.jar").exists() || new File("/system/framework/flyme-res").exists() || new File("/system/framework/flyme-telephony-common.jar").exists()) {
            manufacturer += "(Flyme)";
        } else if (new File("/system/framework/org.lineageos.platform-res.apk").exists() || new File("/system/framework/org.lineageos.platform.jar").exists()) {
            manufacturer += "(Lineage OS Based ROM)";
        } else if (new File("/system/framework/twframework.jar").exists() || new File("/system/framework/samsung-services.jar").exists()) {
            manufacturer += "(TouchWiz)";
        } else if (new File("/system/framework/core.jar.jex").exists()) {
            manufacturer += "(Aliyun OS)";
        }
        return manufacturer;
    }

    @NotProguard
    private File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            Log.e(TAG, "Failed to get canonical file for " + file.getAbsolutePath(), e);
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
}
