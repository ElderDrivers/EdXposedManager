package de.robv.android.xposed.installer.installation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.robv.android.xposed.installer.R;
import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.AssetUtil;
import de.robv.android.xposed.installer.util.DownloadsUtil;
import de.robv.android.xposed.installer.util.InstallZipUtil;
import de.robv.android.xposed.installer.util.NavUtil;
import de.robv.android.xposed.installer.util.RootUtil;
import de.robv.android.xposed.installer.util.json.XposedTab;
import de.robv.android.xposed.installer.util.json.XposedZip;

import static de.robv.android.xposed.installer.XposedApp.WRITE_EXTERNAL_PERMISSION;
import static de.robv.android.xposed.installer.XposedApp.runOnUiThread;

public class BaseAdvancedInstaller extends Fragment implements DownloadsUtil.DownloadFinishedCallback {

    public static final String JAR_PATH = XposedApp.BASE_DIR + "bin/XposedBridge.jar";
    private static final int INSTALL_MODE_NORMAL = 0;
    private static final int INSTALL_MODE_RECOVERY_AUTO = 1;
    private static final int INSTALL_MODE_RECOVERY_MANUAL = 2;
    private static final String BINARIES_FOLDER = AssetUtil.getBinariesFolder();
    public static String APP_PROCESS_NAME = null;
    private static RootUtil mRootUtil = new RootUtil();
    private List<String> messages = new ArrayList<>();
    private View mClickedButton;

    public static BaseAdvancedInstaller newInstance(XposedTab tab) {
        BaseAdvancedInstaller myFragment = new BaseAdvancedInstaller();

        Bundle args = new Bundle();
        args.putParcelable("tab", tab);
        myFragment.setArguments(args);

        return myFragment;
    }

    protected List<XposedZip> installers() {
        XposedTab tab = getArguments().getParcelable("tab");

        assert tab != null;
        return tab.getInstallers();
    }

    protected List<XposedZip> uninstallers() {
        XposedTab tab = getArguments().getParcelable("tab");

        assert tab != null;
        return tab.uninstallers;
    }

    protected String compatibility() {
        XposedTab tab = getArguments().getParcelable("tab");

        assert tab != null;
        return tab.getCompatibility();
    }

    protected String incompatibility() {
        XposedTab tab = getArguments().getParcelable("tab");

        assert tab != null;
        return tab.getIncompatibility();
    }

    protected String author() {
        XposedTab tab = getArguments().getParcelable("tab");

        assert tab != null;
        return tab.author;
    }

    protected String xdaUrl() {
        XposedTab tab = getArguments().getParcelable("tab");

        assert tab != null;
        return tab.getSupport();
    }

    protected boolean isStable() {
        XposedTab tab = getArguments().getParcelable("tab");

        assert tab != null;
        return tab.stable;
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT < 23) return false;

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRootUtil.dispose();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.single_installer_view, container, false);

        final Spinner chooserInstallers = view.findViewById(R.id.chooserInstallers);
        final Spinner chooserUninstallers = view.findViewById(R.id.chooserUninstallers);
        final Button btnInstall = view.findViewById(R.id.btnInstall);
        final Button btnUninstall = view.findViewById(R.id.btnUninstall);
        ImageView infoInstaller = view.findViewById(R.id.infoInstaller);
        ImageView infoUninstaller = view.findViewById(R.id.infoUninstaller);
        TextView compatibleTv = view.findViewById(R.id.compatibilityTv);
        TextView incompatibleTv = view.findViewById(R.id.incompatibilityTv);
        TextView author = view.findViewById(R.id.author);
        View showOnXda = view.findViewById(R.id.show_on_xda);

        try {
            chooserInstallers.setAdapter(new XposedZip.MyAdapter(getContext(), installers()));
            chooserUninstallers.setAdapter(new XposedZip.MyAdapter(getContext(), uninstallers()));
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT >= 21 && installers().size() >= 3 && uninstallers().size() >= 4) {
            if (StatusInstallerFragment.ARCH.contains("86")) {
                chooserInstallers.setSelection(2);
                chooserUninstallers.setSelection(3);
            } else if (StatusInstallerFragment.ARCH.contains("64")) {
                chooserInstallers.setSelection(1);
                chooserUninstallers.setSelection(1);
            }
        }

        infoInstaller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XposedZip selectedInstaller = (XposedZip) chooserInstallers.getSelectedItem();
                String s = getString(R.string.infoInstaller,
                        selectedInstaller.name, Build.VERSION.SDK_INT,
                        selectedInstaller.architecture,
                        selectedInstaller.version);

                new MaterialDialog.Builder(getContext()).title(R.string.info)
                        .content(s).positiveText(android.R.string.ok).show();
            }
        });
        infoUninstaller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XposedZip selectedUninstaller = (XposedZip) chooserUninstallers.getSelectedItem();
                String s = getString(R.string.infoUninstaller,
                        selectedUninstaller.name,
                        selectedUninstaller.architecture,
                        selectedUninstaller.version);

                new MaterialDialog.Builder(getContext()).title(R.string.info)
                        .content(s).positiveText(android.R.string.ok).show();
            }
        });

        btnInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickedButton = v;
                if (checkPermissions()) return;

                areYouSure(R.string.warningArchitecture,
                        new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);

                                XposedZip selectedInstaller = (XposedZip) chooserInstallers.getSelectedItem();

                                checkAndDelete(selectedInstaller.name);

                                new DownloadsUtil.Builder(getContext())
                                        .setTitle(selectedInstaller.name)
                                        .setUrl(selectedInstaller.link)
                                        .setSave(true)
                                        .setCallback(BaseAdvancedInstaller.this)
                                        .setMimeType(DownloadsUtil.MIME_TYPES.ZIP)
                                        .setDialog(true)
                                        .download();
                            }
                        });
            }
        });

        btnUninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickedButton = v;
                if (checkPermissions()) return;

                areYouSure(R.string.warningArchitecture,
                        new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);

                                XposedZip selectedUninstaller = (XposedZip) chooserUninstallers.getSelectedItem();

                                checkAndDelete(selectedUninstaller.name);

                                new DownloadsUtil.Builder(getContext())
                                        .setTitle(selectedUninstaller.name)
                                        .setUrl(selectedUninstaller.link)
                                        .setSave(true)
                                        .setCallback(BaseAdvancedInstaller.this)
                                        .setMimeType(DownloadsUtil.MIME_TYPES.ZIP)
                                        .setDialog(true)
                                        .download();
                            }
                        });
            }
        });

        compatibleTv.setText(compatibility());
        incompatibleTv.setText(incompatibility());
        author.setText(getString(R.string.download_author, author()));

        try {
            if (uninstallers().size() == 0) {
                infoUninstaller.setVisibility(View.GONE);
                chooserUninstallers.setVisibility(View.GONE);
                btnUninstall.setVisibility(View.GONE);
            }
        } catch (Exception ignored) {}

        if (!isStable()) {
            view.findViewById(R.id.warning_unstable).setVisibility(View.VISIBLE);
        }

        showOnXda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtil.startURL(getActivity(), xdaUrl());
            }
        });

        if (Build.VERSION.SDK_INT == 15) {
            APP_PROCESS_NAME = BINARIES_FOLDER + "app_process_xposed_sdk15";
        } else if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT <= 18) {
            APP_PROCESS_NAME = BINARIES_FOLDER + "app_process_xposed_sdk16";
        } else if (Build.VERSION.SDK_INT == 19) {
            APP_PROCESS_NAME = BINARIES_FOLDER + "app_process_xposed_sdk19";
        }

        return view;
    }

    private void checkAndDelete(String name) {
        new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/XposedInstaller/" + name + ".zip").delete();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mClickedButton != null) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mClickedButton.performClick();
                        }
                    }, 500);
                }
            } else {
                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDownloadFinished(final Context context, final DownloadsUtil.DownloadInfo info) {
        messages.clear();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, getString(R.string.downloadZipOk, info.localFilename), Toast.LENGTH_LONG).show();
            }
        });

        if (getInstallMode() == INSTALL_MODE_RECOVERY_MANUAL)
            return;

        if (getInstallMode() == INSTALL_MODE_NORMAL) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        areYouSure(R.string.install_warning, new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);

                                if (!startShell()) return;

                                if (info.localFilename.contains("Disabler")) {
                                    prepareUninstall(messages);
                                } else {
                                    prepareInstall(messages);
                                }
                                offerReboot(messages);
                            }
                        });
                    }
                });
                return;
            } else if (InstallZipUtil.checkZip(InstallZipUtil.getZip(info.localFilename)).isFlashableInApp()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        areYouSure(R.string.install_warning, new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);

                                if (!startShell()) return;

                                Intent install = new Intent(getContext(), InstallationActivity.class);
                                install.putExtra(Flashable.KEY, new FlashDirectly(info.localFilename, false));
                                startActivity(install);
                            }
                        });
                    }
                });
                return;
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.not_flashable_inapp, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                areYouSure(R.string.install_warning, new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);

                        if (!startShell()) return;

                        prepareAutoFlash(messages, new File(info.localFilename));
                        offerRebootToRecovery(messages, info.title, INSTALL_MODE_RECOVERY_AUTO);
                    }
                });
            }
        });
    }

    private void areYouSure(int contentTextId, MaterialDialog.ButtonCallback yesHandler) {
        new MaterialDialog.Builder(getActivity()).title(R.string.areyousure)
                .content(contentTextId)
                .iconAttr(android.R.attr.alertDialogIcon)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no).callback(yesHandler).show();
    }

    private boolean startShell() {
        if (mRootUtil.startShell())
            return true;

        showAlert(getString(R.string.root_failed));
        return false;
    }

    private void showAlert(final String result) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showAlert(result);
                }
            });
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity()).content(result).positiveText(android.R.string.ok).build();
        dialog.show();

        TextView txtMessage = (TextView) dialog
                .findViewById(android.R.id.message);
        try {
            txtMessage.setTextSize(14);
        } catch (NullPointerException ignored) {
        }
    }

    private int getInstallMode() {
        return XposedApp.getPreferences().getInt("install_mode", INSTALL_MODE_NORMAL);
    }

    private void showConfirmDialog(final String message, final MaterialDialog.ButtonCallback callback) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showConfirmDialog(message, callback);
                }
            });
            return;
        }

        new MaterialDialog.Builder(getActivity())
                .content(message).positiveText(android.R.string.yes)
                .negativeText(android.R.string.no).callback(callback).show();
    }

    private boolean prepareInstall(List<String> messages) {
        File appProcessFile = AssetUtil.writeAssetToFile(APP_PROCESS_NAME, new File(XposedApp.BASE_DIR + "bin/app_process"), 00700);
        if (appProcessFile == null) {
            showAlert(getString(R.string.file_extract_failed, "app_process"));
            return false;
        }

        messages.add(getString(R.string.file_copying, "XposedBridge.jar"));
        File jarFile = AssetUtil.writeAssetToFile("XposedBridge.jar", new File(JAR_PATH), 00644);
        if (jarFile == null) {
            messages.add("");
            messages.add(getString(R.string.file_extract_failed, "XposedBridge.jar"));
            return false;
        }

        mRootUtil.executeWithBusybox("sync", messages);

        messages.add(getString(R.string.file_mounting_writable, "/system"));
        if (mRootUtil.executeWithBusybox("mount -o remount,rw /system", messages) != 0) {
            messages.add(getString(R.string.file_mount_writable_failed, "/system"));
            messages.add(getString(R.string.file_trying_to_continue));
        }

        if (new File("/system/bin/app_process.orig").exists()) {
            messages.add(getString(R.string.file_backup_already_exists, "/system/bin/app_process.orig"));
        } else {
            if (mRootUtil.executeWithBusybox("cp -a /system/bin/app_process /system/bin/app_process.orig", messages) != 0) {
                messages.add("");
                messages.add(getString(R.string.file_backup_failed, "/system/bin/app_process"));
                return false;
            } else {
                messages.add(getString(R.string.file_backup_successful, "/system/bin/app_process.orig"));
            }

            mRootUtil.executeWithBusybox("sync", messages);
        }

        messages.add(getString(R.string.file_copying, "app_process"));
        if (mRootUtil.executeWithBusybox("cp -a " + appProcessFile.getAbsolutePath() + " /system/bin/app_process", messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.file_copy_failed, "app_process", "/system/bin"));
            return false;
        }
        if (mRootUtil.executeWithBusybox("chmod 755 /system/bin/app_process", messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.file_set_perms_failed, "/system/bin/app_process"));
            return false;
        }
        if (mRootUtil.executeWithBusybox("chown root:shell /system/bin/app_process", messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.file_set_owner_failed, "/system/bin/app_process"));
            return false;
        }

        return true;
    }

    private boolean prepareUninstall(List<String> messages) {
        new File(JAR_PATH).delete();
        new File(XposedApp.BASE_DIR + "bin/app_process").delete();

        if (!startShell())
            return false;


        messages.add(getString(R.string.file_mounting_writable, "/system"));
        if (mRootUtil.executeWithBusybox("mount -o remount,rw /system", messages) != 0) {
            messages.add(getString(R.string.file_mount_writable_failed, "/system"));
            messages.add(getString(R.string.file_trying_to_continue));
        }

        messages.add(getString(R.string.file_backup_restoring, "/system/bin/app_process.orig"));
        if (!new File("/system/bin/app_process.orig").exists()) {
            messages.add("");
            messages.add(getString(R.string.file_backup_not_found, "/system/bin/app_process.orig"));
            return false;
        }

        if (mRootUtil.executeWithBusybox("mv /system/bin/app_process.orig /system/bin/app_process", messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.file_move_failed, "/system/bin/app_process.orig", "/system/bin/app_process"));
            return false;
        }
        if (mRootUtil.executeWithBusybox("chmod 755 /system/bin/app_process", messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.file_set_perms_failed, "/system/bin/app_process"));
            return false;
        }
        if (mRootUtil.executeWithBusybox("chown root:shell /system/bin/app_process", messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.file_set_owner_failed, "/system/bin/app_process"));
            return false;
        }
        // Might help on some SELinux-enforced ROMs, shouldn't hurt on others
        mRootUtil.execute("/system/bin/restorecon /system/bin/app_process", (RootUtil.LineCallback) null);

        return true;
    }

    private boolean prepareAutoFlash(List<String> messages, File file) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            File appProcessFile = AssetUtil.writeAssetToFile(APP_PROCESS_NAME, new File(XposedApp.BASE_DIR + "bin/app_process"), 00700);
            if (appProcessFile == null) {
                showAlert(getString(R.string.file_extract_failed, "app_process"));
                return false;
            }

            messages.add(getString(R.string.file_copying, "XposedBridge.jar"));
            File jarFile = AssetUtil.writeAssetToFile("XposedBridge.jar", new File(JAR_PATH), 00644);
            if (jarFile == null) {
                messages.add("");
                messages.add(getString(R.string.file_extract_failed, "XposedBridge.jar"));
                return false;
            }

            mRootUtil.executeWithBusybox("sync", messages);
        }

        Intent install = new Intent(getContext(), InstallationActivity.class);
        install.putExtra(Flashable.KEY, new FlashRecoveryAuto(file.getAbsoluteFile()));
        startActivity(install);

        return true;
    }

    private void offerReboot(List<String> messages) {
        messages.add(getString(R.string.file_done));
        messages.add("");
        messages.add(getString(R.string.reboot_confirmation));
        showConfirmDialog(TextUtils.join("\n", messages).trim(),
                new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        reboot(null);
                    }
                });
    }

    private void offerRebootToRecovery(List<String> messages, final String file, final int installMode) {
        if (installMode == INSTALL_MODE_RECOVERY_AUTO)
            messages.add(getString(R.string.auto_flash_note, file));
        else
            messages.add(getString(R.string.manual_flash_note, file));

        messages.add("");
        messages.add(getString(R.string.reboot_recovery_confirmation));
        showConfirmDialog(TextUtils.join("\n", messages).trim(),
                new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        reboot("recovery");
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        if (installMode == INSTALL_MODE_RECOVERY_AUTO) {
                            // clean up to avoid unwanted flashing
                            mRootUtil.executeWithBusybox("rm /cache/recovery/command");
                            mRootUtil.executeWithBusybox("rm /cache/recovery/" + file);
                            AssetUtil.removeBusybox();
                        }
                    }
                }

        );
    }

    private void reboot(String mode) {
        if (!startShell())
            return;

        List<String> messages = new LinkedList<>();

        String command = "reboot";
        if (mode != null) {
            command += " " + mode;
            if (mode.equals("recovery"))
                // create a flag used by some kernels to boot into recovery
                mRootUtil.executeWithBusybox("touch /cache/recovery/boot", messages);
        }

        if (mRootUtil.executeWithBusybox(command, messages) != 0) {
            messages.add("");
            messages.add(getString(R.string.reboot_failed));
            showAlert(TextUtils.join("\n", messages).trim());
        }
        AssetUtil.removeBusybox();
    }

}