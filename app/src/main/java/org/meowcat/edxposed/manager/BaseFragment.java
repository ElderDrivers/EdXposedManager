package org.meowcat.edxposed.manager;

import android.app.Activity;
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

public class BaseFragment extends Fragment {

    static void areYouSure(@NonNull Activity activity, String contentText, MaterialDialog.SingleButtonCallback positiveSingleButtonCallback, MaterialDialog.SingleButtonCallback negativeSingleButtonCallback) {
        new MaterialDialog.Builder(activity).title(R.string.areyousure)
                .content(contentText)
                .iconAttr(android.R.attr.alertDialogIcon)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive(positiveSingleButtonCallback)
                .onNegative(negativeSingleButtonCallback)
                .show();
    }

    private void showAlert(final String result) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            requireActivity().runOnUiThread(() -> showAlert(result));
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(requireActivity()).content(result).positiveText(R.string.ok).build();
        dialog.show();

        TextView txtMessage = (TextView) dialog
                .findViewById(android.R.id.message);
        try {
            txtMessage.setTextSize(14);
        } catch (NullPointerException ignored) {
        }
    }

    private void softReboot() {
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

    private void reboot(String mode) {
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dexopt_all:
                areYouSure(requireActivity(), getString(R.string.dexopt_now) + "\n" + getString(R.string.take_while_cannot_resore), (d, w) -> new MaterialDialog.Builder(requireActivity())
                        .title(R.string.dexopt_now)
                        .content(R.string.this_may_take_a_while)
                        .progress(true, 0)
                        .cancelable(false)
                        .showListener(dialog -> new Thread("dexopt") {
                            @Override
                            public void run() {
                                if (!Shell.rootAccess()) {
                                    dialog.dismiss();
                                    NavUtil.showMessage(requireActivity(), getString(R.string.root_failed));
                                    return;
                                }

                                Shell.su("cmd package bg-dexopt-job").exec();

                                dialog.dismiss();
                                XposedApp.runOnUiThread(() -> Toast.makeText(getActivity(), R.string.done, Toast.LENGTH_LONG).show());
                            }
                        }.start()).show(), (d, w) -> {
                });
                break;
            case R.id.speed_all:
                areYouSure(requireActivity(), getString(R.string.speed_now) + "\n" + getString(R.string.take_while_cannot_resore), (d, w) ->
                        new MaterialDialog.Builder(requireActivity())
                                .title(R.string.speed_now)
                                .content(R.string.this_may_take_a_while)
                                .progress(true, 0)
                                .cancelable(false)
                                .showListener(dialog -> new Thread("dex2oat") {
                                    @Override
                                    public void run() {
                                        if (!Shell.rootAccess()) {
                                            dialog.dismiss();
                                            NavUtil.showMessage(requireActivity(), getString(R.string.root_failed));
                                            return;
                                        }

                                        Shell.su("cmd package compile -m speed -a").exec();

                                        dialog.dismiss();
                                        XposedApp.runOnUiThread(() -> Toast.makeText(getActivity(), R.string.done, Toast.LENGTH_LONG).show());
                                    }
                                }.start()).show(), (d, w) -> {
                });
                break;
            case R.id.reboot:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(requireActivity(), getString(R.string.reboot_system), (d, w) -> reboot(null), (d, w) -> {
                    });
                } else {
                    reboot(null);
                }
                break;
            case R.id.soft_reboot:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(requireActivity(), getString(R.string.soft_reboot), (d, w) -> softReboot(), (d, w) -> {
                    });
                } else {
                    softReboot();
                }
                break;
            case R.id.reboot_recovery:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(requireActivity(), getString(R.string.reboot_recovery), (d, w) -> reboot("recovery"), (d, w) -> {
                    });
                } else {
                    reboot("recovery");
                }
                break;
            case R.id.reboot_bootloader:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(requireActivity(), getString(R.string.reboot_bootloader), (d, w) -> reboot("bootloader"), (d, w) -> {
                    });
                } else {
                    reboot("bootloader");
                }
                break;
            case R.id.reboot_download:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(requireActivity(), getString(R.string.reboot_download), (d, w) -> reboot("download"), (d, w) -> {
                    });
                } else {
                    reboot("download");
                }
                break;
            case R.id.reboot_edl:
                if (XposedApp.getPreferences().getBoolean("confirm_reboots", true)) {
                    areYouSure(requireActivity(), getString(R.string.reboot_edl), (d, w) -> reboot("edl"), (d, w) -> {
                    });
                } else {
                    reboot("edl");
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
