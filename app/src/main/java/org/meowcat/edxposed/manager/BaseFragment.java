package org.meowcat.edxposed.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.topjohnwu.superuser.Shell;

import java.util.LinkedList;
import java.util.List;

public class BaseFragment extends Fragment {

    public static void areYouSure(@NonNull Activity activity, String contentText, MaterialDialog.SingleButtonCallback positiveSingleButtonCallback, MaterialDialog.SingleButtonCallback negativeSingleButtonCallback) {
        new MaterialDialog.Builder(activity).title(R.string.areyousure)
                .content(contentText)
                .iconAttr(android.R.attr.alertDialogIcon)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive(positiveSingleButtonCallback)
                .onNegative(negativeSingleButtonCallback)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
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
        String command = "setprop ctl.restart surfaceflinger; setprop ctl.restart zygote";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                ((PowerManager) requireContext().getSystemService(Context.POWER_SERVICE)).isRebootingUserspaceSupported()) {
            command = "/system/bin/svc power reboot userspace";
        }

        if (startShell())
            return;

        List<String> messages = new LinkedList<>();
        Shell.Result result = Shell.su(command).exec();
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
