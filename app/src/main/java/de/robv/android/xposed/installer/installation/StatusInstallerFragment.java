package de.robv.android.xposed.installer.installation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.InstallZipUtil;
import de.robv.android.xposed.installer.util.NavUtil;

@SuppressWarnings("deprecation")
public class StatusInstallerFragment extends Fragment {

    public static final File DISABLE_FILE = new File(XposedApp.BASE_DIR + "conf/disabled");
    public static String ARCH = getArch();
    private static Activity sActivity;
    private static String mUpdateLink;
    private static ImageView mErrorIcon;
    private static View mUpdateView;
    private static View mUpdateButton;
    private static TextView mErrorTv;
    private static boolean isXposedInstalled = false;
    private TextView txtKnownIssue;

    public static void setError(boolean connectionFailed, boolean noSdks) {
        if (!connectionFailed && !noSdks) {
            if (isXposedInstalled) return;
            return;
        }

        mErrorTv.setVisibility(View.VISIBLE);
        mErrorIcon.setVisibility(View.VISIBLE);
        if (noSdks) {
            mErrorIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_warning_grey));
            mErrorTv.setText(String.format(sActivity.getString(R.string.phone_not_compatible), Build.VERSION.SDK_INT, Build.CPU_ABI));
        }
        if (connectionFailed) {
            mErrorIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_no_connection));
            mErrorTv.setText(sActivity.getString(R.string.loadingError));
        }
    }

    static void setUpdate(final String link, final String changelog, Context mContext) {
        mUpdateLink = link;

        mUpdateView.setVisibility(View.VISIBLE);
        mUpdateButton.setVisibility(View.VISIBLE);
        mUpdateButton.setOnClickListener(v -> new MaterialDialog.Builder(sActivity)
                .title(R.string.changes)
                .content(Html.fromHtml(changelog))
                .onPositive((dialog, which) -> update(mContext))
                .positiveText(R.string.update)
                .negativeText(R.string.later).show());
    }

    private static void update(Context mContext) {
        Uri uri = Uri.parse(mUpdateLink);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        mContext.startActivity(intent);
    }

//    private static boolean checkPermissions() {
//        if (Build.VERSION.SDK_INT < 23) return false;
//
//        if (ActivityCompat.checkSelfPermission(sActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            sFragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
//            return true;
//        }
//        return false;
//    }

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

    @SuppressLint({"WorldReadableFiles", "WorldWriteableFiles"})
    private static void setFilePermissionsFromMode(String name, int mode) {
        int perms = FileUtils.S_IRUSR | FileUtils.S_IWUSR
                | FileUtils.S_IRGRP | FileUtils.S_IWGRP;
        if ((mode & Context.MODE_WORLD_READABLE) != 0) {
            perms |= FileUtils.S_IROTH;
        }
        if ((mode & Context.MODE_WORLD_WRITEABLE) != 0) {
            perms |= FileUtils.S_IWOTH;
        }
        FileUtils.setPermissions(name, perms, -1, -1);
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == WRITE_EXTERNAL_PERMISSION) {
//            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        update();
//                    }
//                }, 500);
//            } else {
//                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
//            }
//        }
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sActivity = getActivity();
        Fragment sFragment = this;
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.status_installer, container, false);

        mErrorIcon = v.findViewById(R.id.errorIcon);
        mErrorTv = v.findViewById(R.id.errorTv);
        mUpdateView = v.findViewById(R.id.updateView);
        mUpdateButton = v.findViewById(R.id.click_to_update);

        txtKnownIssue = v.findViewById(R.id.framework_known_issue);

        TextView txtInstallError = v.findViewById(R.id.framework_install_errors);
        View txtInstallContainer = v.findViewById(R.id.status_container);
        ImageView txtInstallIcon = v.findViewById(R.id.status_icon);

        String installedXposedVersion;
        try {
            installedXposedVersion = XposedApp.getXposedProp().getVersion();
        } catch (NullPointerException e) {
            installedXposedVersion = null;
        }

        View disableView = v.findViewById(R.id.disableView);
        final SwitchCompat xposedDisable = v.findViewById(R.id.disableSwitch);

        TextView androidSdk = v.findViewById(R.id.android_version);
        TextView manufacturer = v.findViewById(R.id.ic_manufacturer);
        TextView cpu = v.findViewById(R.id.cpu);

        if (Build.VERSION.SDK_INT >= 21) {
            if (installedXposedVersion != null) {
                int installedXposedVersionInt = extractIntPart(installedXposedVersion);
                if (installedXposedVersionInt == XposedApp.getXposedVersion()) {
                    txtInstallError.setText(sActivity.getString(R.string.installed_lollipop, installedXposedVersion));
                    txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.darker_green));
                    txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.darker_green));
                    txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_check_circle));
                    isXposedInstalled = true;
                } else {
                    txtInstallError.setText(sActivity.getString(R.string.installed_lollipop_inactive, installedXposedVersion));
                    txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.amber_500));
                    txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.amber_500));
                    txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_warning));
                }
            } else {
                txtInstallError.setText(R.string.not_installed_no_lollipop);
                txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.warning));
                txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.warning));
                txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_error));
                xposedDisable.setVisibility(View.GONE);
                disableView.setVisibility(View.GONE);
            }
        } else {
            int installedXposedVersionInt = XposedApp.getXposedVersion();
            if (installedXposedVersionInt != 0) {
                txtInstallError.setText(sActivity.getString(R.string.installed_lollipop, "" + installedXposedVersionInt));
                txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.darker_green));
                txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.darker_green));
                txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_check_circle));
                isXposedInstalled = true;
                if (DISABLE_FILE.exists()) {
                    txtInstallError.setText(sActivity.getString(R.string.installed_lollipop_inactive, "" + installedXposedVersionInt));
                    txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.amber_500));
                    txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.amber_500));
                    txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_warning));
                }
            } else {
                txtInstallError.setText(sActivity.getString(R.string.not_installed_no_lollipop));
                txtInstallError.setTextColor(sActivity.getResources().getColor(R.color.warning));
                txtInstallContainer.setBackgroundColor(sActivity.getResources().getColor(R.color.warning));
                txtInstallIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_error));
                xposedDisable.setVisibility(View.GONE);
                disableView.setVisibility(View.GONE);
            }
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
                        setFilePermissionsFromMode(DISABLE_FILE.getPath(), Context.MODE_WORLD_READABLE);
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
                                    Log.e(XposedApp.TAG, "StatusInstallerFragment -> " + e.getMessage());
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

        determineVerifiedBootState(v);

        refreshKnownIssue();
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
                tv.setText(R.string.verified_boot_none);
                tv.setTextColor(getResources().getColor(R.color.warning));
                v.findViewById(R.id.dmverity_explanation).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(XposedApp.TAG, "Could not detect Verified Boot state", e);
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
            issueName = getString(R.string.installer_needs_update, getString(R.string.app_name));
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
        } else if (checkAppInstalled(getContext(), "com.solohsu.android.edxp.manager")) {
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
            case 29:
                return "Q";
            case 30:
                return "R";
        }
        return "Unknown";
    }

    private String getUIFramework() {
        String manufacturer = Character.toUpperCase(Build.MANUFACTURER.charAt(0)) + Build.MANUFACTURER.substring(1);
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            manufacturer += " " + Character.toUpperCase(Build.BRAND.charAt(0)) + Build.BRAND.substring(1);
        }
        manufacturer += " " + Build.MODEL + " ";
        if (new File("/system/framework/samsung-services.jar").exists()) {
            manufacturer += "(TouchWiz)";
        } else if (new File("/system/framework/framework-miui-res.apk").exists() || new File("/system/app/miui/miui.apk").exists() || new File("/system/app/miuisystem/miuisystem.apk").exists()) {
            manufacturer += "(MIUI)";
        } else if (new File("/system/priv-app/oneplus-framework-res/oneplus-framework-res.apk").exists()) {
            manufacturer += "(Oxygen/Hydrogen OS)";
        }
        /*if (manufacturer.contains("Samsung")) {
            manufacturer += new File("/system/framework/twframework.jar").exists() ||
                    new File("/system/framework/samsung-services.jar").exists()
                    ? "(TouchWiz)" : "(AOSP-based ROM)";
        } else if (manufacturer.contains("Xiaomi")) {
            manufacturer += new File("/system/framework/framework-miui-res.apk").exists() ? "(MIUI)" : "(AOSP-based ROM)";
        }*/
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
}