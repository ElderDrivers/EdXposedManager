package org.meowcat.edxposed.manager;

import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.util.NavUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class BaseFragment extends Fragment {
    void showAlert(final String result) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> showAlert(result));
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(Objects.requireNonNull(getActivity())).content(result).positiveText(R.string.ok).build();
        dialog.show();

        TextView txtMessage = (TextView) dialog
                .findViewById(android.R.id.message);
        try {
            txtMessage.setTextSize(14);
        } catch (NullPointerException ignored) {
        }
    }

    void softReboot() {
        if (startShell())
            return;

        List<String> messages = new LinkedList<>();
        Shell.Result result = Shell.su("setprop ctl.restart surfaceflinger; setprop ctl.restart zygote").exec();
        if (result.getCode() != 0) {
            messages.add(result.getOut().toString());
            messages.add("");
            messages.add(getString(R.string.reboot_failed));
            showAlert(TextUtils.join("\n", messages).trim());
        }
    }

    void reboot(String mode) {
        if (startShell())
            return;

        List<String> messages = new LinkedList<>();

        String command = "/system/bin/svc power reboot";
        if (mode != null) {
            command += " " + mode;
            if (mode.equals("recovery"))
                // create a flag used by some kernels to boot into recovery
                Shell.su("touch /cache/recovery/boot").exec();
        }
        Shell.Result result = Shell.su(command).exec();
        if (result.getCode() != 0) {
            messages.add(result.getOut().toString());
            messages.add("");
            messages.add(getString(R.string.reboot_failed));
            showAlert(TextUtils.join("\n", messages).trim());
        }
    }

    private boolean startShell() {
        if (Shell.rootAccess())
            return false;

        showAlert(getString(R.string.root_failed));
        return true;
    }

    void areYouSure(int contentTextId, MaterialDialog.ButtonCallback yesHandler) {
        new MaterialDialog.Builder(Objects.requireNonNull(getActivity())).title(R.string.areyousure)
                .content(contentTextId)
                .iconAttr(android.R.attr.alertDialogIcon)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no).callback(yesHandler).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dexopt_all:
                areYouSure(R.string.take_while_cannot_resore, new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog mDialog) {
                        super.onPositive(mDialog);
                        new MaterialDialog.Builder(Objects.requireNonNull(getActivity()))
                                .title(R.string.dexopt_now)
                                .content(R.string.this_may_take_a_while)
                                .progress(true, 0)
                                .cancelable(false)
                                .showListener(dialog -> new Thread("dexopt") {
                                    @Override
                                    public void run() {
                                        if (!Shell.rootAccess()) {
                                            dialog.dismiss();
                                            NavUtil.showMessage(Objects.requireNonNull(getActivity()), getString(R.string.root_failed));
                                            return;
                                        }

                                        Shell.su("cmd package bg-dexopt-job").exec();

                                        dialog.dismiss();
                                        XposedApp.runOnUiThread(() -> Toast.makeText(getActivity(), R.string.done, Toast.LENGTH_LONG).show());
                                    }
                                }.start()).show();
                    }
                });
                break;
            case R.id.speed_all:
                areYouSure(R.string.take_while_cannot_resore, new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog mDialog) {
                        super.onPositive(mDialog);
                        new MaterialDialog.Builder(Objects.requireNonNull(getActivity()))
                                .title(R.string.speed_now)
                                .content(R.string.this_may_take_a_while)
                                .progress(true, 0)
                                .cancelable(false)
                                .showListener(dialog -> new Thread("dex2oat") {
                                    @Override
                                    public void run() {
                                        if (!Shell.rootAccess()) {
                                            dialog.dismiss();
                                            NavUtil.showMessage(Objects.requireNonNull(getActivity()), getString(R.string.root_failed));
                                            return;
                                        }

                                        Shell.su("cmd package compile -m speed -a").exec();

                                        dialog.dismiss();
                                        XposedApp.runOnUiThread(() -> Toast.makeText(getActivity(), R.string.done, Toast.LENGTH_LONG).show());
                                    }
                                }.start()).show();
                    }
                });
                break;
            case R.id.reboot:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot(null);
                        }
                    });
                } else {
                    reboot(null);
                }
                break;
            case R.id.soft_reboot:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.soft_reboot, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            softReboot();
                        }
                    });
                } else {
                    softReboot();
                }
                break;
            case R.id.reboot_recovery:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_recovery, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot("recovery");
                        }
                    });
                } else {
                    reboot("recovery");
                }
                break;
            case R.id.reboot_bootloader:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_bootloader, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot("bootloader");
                        }
                    });
                } else {
                    reboot("bootloader");
                }
                break;
            case R.id.reboot_download:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_download, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot("download");
                        }
                    });
                } else {
                    reboot("download");
                }
                break;
            case R.id.reboot_edl:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(R.string.reboot_download, new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            reboot("edl");
                        }
                    });
                } else {
                    reboot("edl");
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
