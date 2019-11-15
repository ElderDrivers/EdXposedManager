package de.robv.android.xposed.installer.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.solohsu.android.edxp.manager.R;
import com.github.coxylicacid.mdwidgets.dialog.MD2Dialog;
import com.google.android.material.button.MaterialButton;

import de.robv.android.xposed.installer.XposedBaseActivity;
import de.robv.android.xposed.installer.util.RootUtil;
import de.robv.android.xposed.installer.util.ThemeUtil;

public class SELinuxActivity extends XposedBaseActivity {

    private MaterialButton forever;
    private MaterialButton temporarily;
    private ImageView statusImg;
    private TextView statusText;
    private TextView commandLog;
    private TextView statusInfo;
    private ScrollView scrollView;
    private RootUtil mRootUtil = new RootUtil();

    @Override

    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_selinux);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.selinux_item);
        initViews();
    }

    @SuppressLint("SetTextI18n")
    private void initViews() {
        forever = findViewById(R.id.closed_forever);
        temporarily = findViewById(R.id.closed_temporarily);
        statusImg = findViewById(R.id.selinux_status);
        statusText = findViewById(R.id.selinux_status_info);
        statusInfo = findViewById(R.id.status_info);
        commandLog = findViewById(R.id.command_log);
        scrollView = findViewById(R.id.command_scrollview);
        View shadowAbove = findViewById(R.id.shadow_above);
        View shadowBelow = findViewById(R.id.shadow_below);

        if (ThemeUtil.getSelectTheme().equals("dark")) {
            shadowAbove.setEnabled(false);
            shadowBelow.setEnabled(false);
            statusInfo.setTextColor(0xFFEAEAEA);
            statusImg.setImageResource(R.drawable.selinux_status_dark);
        }

        fetchStatus();

        forever.setOnClickListener(v -> {
            if (forever.getText() == "开启") {
                log("Executing", "Set the selinux enforcing.");
                exec("setenforce 1");
                log("Executing", "Mounting the system into writable mode...", MD2Dialog.COLOR_SUCCESSFUL);
                exec("mount -o remount,rw /system");
                log("Executing", "Removed boot scripts from /system/etc/init.d.");
                exec("rm -rf /system/etc/init.d/00SELinux");
                log("Executing", "Removed selinux config from /system/etc/selinux/.");
                exec("rm -rf /system/etc/selinux/config");
                log("Status", "the selinux has been turned on", MD2Dialog.COLOR_SUCCESSFUL);
                refreshStatus(true);
            } else {
                MD2Dialog.create(this)
                        .title("请选择一种关闭方式")
                        .singleChoiceMode(true)
                        .darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                        .items(new String[]{"将命令写入init.d (开机时启动的服务)", "将命令写入系统配置 (/system/etc/selinux/config)", "将命令写入post-fs-data (面具做法)"}, 0)
                        .removeDivider()
                        .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
                        .simpleCancel(android.R.string.cancel)
                        .onConfirmClick(android.R.string.ok, (view, dialog) -> {
                            switch (dialog.getSelectedItem()) {
                                case 0:
                                    log("Executing", "Mounting the system into writable mode...", MD2Dialog.COLOR_SUCCESSFUL);
                                    exec("mount -o remount,rw /system", new RootUtil.LineCallback() {
                                        @Override
                                        public void onLine(String line) {
                                        }

                                        @Override
                                        public void onErrorLine(String line) {
                                            log("Executing", "Mounting the system into writable mode failed..." + (line.isEmpty() ? "" : ", details: " + line), MD2Dialog.COLOR_ERROR);
                                        }
                                    });
                                    log("Executing", "Writing these content into init.d/00SELinux:\n\n" + "\t#!/system/bin/sh\n\tsetenforce 0\n", MD2Dialog.COLOR_SUCCESSFUL);
                                    exec("setenforce 0 && echo '#!/system/bin/sh\nsetenforce 0' > /system/etc/init.d/00SELinux", new RootUtil.LineCallback() {
                                        @Override
                                        public void onLine(String line) {
                                        }

                                        @Override
                                        public void onErrorLine(String line) {
                                            log("Executing", "Writing content into init.d/00SELinux failed" + (line.isEmpty() ? "" : ", details: " + line), MD2Dialog.COLOR_ERROR);
                                            log("Status", "Failed to turn down the selinux", MD2Dialog.COLOR_ERROR);
                                        }
                                    });
                                    log("Executing", "Giving the executable permission to '00SELinux' file", MD2Dialog.COLOR_SUCCESSFUL);
                                    exec("chmod 777 /system/etc/init.d/00SELinux");
                                    log("Status", "Congratulations! you have turned down the selinux", MD2Dialog.COLOR_SUCCESSFUL);
                                    dialog.dismiss();
                                    refreshStatus(false);
                                    break;
                                case 1:
                                    log("Executing", "Mounting the system into writable mode...", MD2Dialog.COLOR_SUCCESSFUL);
                                    exec("mount -o remount,rw /system", new RootUtil.LineCallback() {
                                        @Override
                                        public void onLine(String line) {
                                        }

                                        @Override
                                        public void onErrorLine(String line) {
                                            log("Executing", "Mounting the system into writable mode failed..." + (line.isEmpty() ? "" : ", details: " + line), MD2Dialog.COLOR_ERROR);
                                        }
                                    });
                                    log("Executing", "Writing content: \n\tSELINUX=disabled\n\tSELINUXTYPE=targeted\ninto /system/etc/selinux/config successfully.", MD2Dialog.COLOR_SUCCESSFUL);
                                    exec("setenforce 0 && echo 'SELINUX=disabled\nSELINUXTYPE=targeted' >> /system/etc/selinux/config", new RootUtil.LineCallback() {
                                        @Override
                                        public void onLine(String line) {
                                        }

                                        @Override
                                        public void onErrorLine(String line) {
                                            log("Executing", "Writing content into /system/etc/selinux/config failed" + (line.isEmpty() ? "" : ", details: " + line), MD2Dialog.COLOR_ERROR);
                                            log("Status", "Failed to turn down the selinux", MD2Dialog.COLOR_ERROR);
                                        }
                                    });
                                    log("Status", "Congratulations! you have turned down the selinux", MD2Dialog.COLOR_SUCCESSFUL);
                                    dialog.dismiss();
                                    refreshStatus(false);
                                    break;
                                case 2:
                                    Toast.makeText(SELinuxActivity.this, "作者正在努力完善", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }).show();
            }
        });

        temporarily.setOnClickListener(v -> {
            log("Status", "You have turned down the selinux temporarily.", MD2Dialog.COLOR_WARNING);
            exec("setenforce 0", new RootUtil.LineCallback() {
                @Override
                public void onLine(String line) {
                }

                @Override
                public void onErrorLine(String line) {
                    log("Status", "Failed to turn down the selinux temporarily." + (line.isEmpty() ? "" : ", details: " + line), MD2Dialog.COLOR_ERROR);
                }
            });
            refreshStatus(false);
        });
    }

    @SuppressLint("SetTextI18n")
    private void fetchStatus() {
        statusText.setText("状态:  " + "Enforcing");
        exec("getenforce", new RootUtil.LineCallback() {
            @Override
            public void onLine(String line) {
                if (line.contains("Permissive") || line.contains("permissive")) {
                    statusImg.setEnabled(false);
                    forever.setText(R.string.turn_on);
                    statusInfo.setText(R.string.already_turn_off);
                    temporarily.setVisibility(View.GONE);
                } else {
                    statusImg.setEnabled(true);
                    statusInfo.setText(R.string.already_turn_on);
                    forever.setText(R.string.closed_forever);
                    temporarily.setVisibility(View.VISIBLE);
                }
                statusText.setText("状态:  " + line);
                log("Status", "Status has been changed to " + line + ".");
            }

            @Override
            public void onErrorLine(String line) {
                Toast.makeText(SELinuxActivity.this, R.string.selinux_fetch_failed, Toast.LENGTH_LONG).show();
                statusText.setTextColor(MD2Dialog.COLOR_ERROR);
                statusText.setText("SELinux状态获取错误");
                log("Status", "Fetch selinux status error, details: " + (TextUtils.isEmpty(line) ? "none." : line), MD2Dialog.COLOR_ERROR);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void refreshStatus(boolean selinuxon) {
        if (selinuxon) {
            statusImg.setEnabled(true);
            statusInfo.setText("已开启");
            forever.setText(R.string.closed_forever);
            temporarily.setVisibility(View.VISIBLE);
            statusText.setText("状态:  " + "Enforcing");
            log("Status", "Status has been changed to " + "Enforcing" + ".", getResources().getColor(R.color.colorAccent));
        } else {
            statusImg.setEnabled(false);
            forever.setText("开启");
            statusInfo.setText("已关闭");
            temporarily.setVisibility(View.GONE);
            statusText.setText("状态:  " + "Permissive");
            log("Status", "Status has been changed to " + "Permissive" + ".", getResources().getColor(R.color.colorAccent));
        }
    }

    private void exec(String command) {
        try {
            mRootUtil.startShell();
            mRootUtil.execute(command);
        } catch (IllegalStateException e) {
            runOnUiThread(() -> MD2Dialog.create(SELinuxActivity.this).darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                    .title(R.string.warning).msg(R.string.not_su_permission_selinux).simpleConfirm("OK").show());
            log("ROOT", "You don't have the root permission", MD2Dialog.COLOR_WARNING);
        }
    }

    private void exec(String command, RootUtil.LineCallback callback) {
        try {
            mRootUtil.startShell();
            mRootUtil.execute(command, callback);
        } catch (IllegalStateException e) {
            runOnUiThread(() -> MD2Dialog.create(SELinuxActivity.this).darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                    .title(R.string.warning).msg(R.string.not_su_permission_selinux).simpleConfirm("OK").show());
            log("ROOT", "You don't have the root permission", MD2Dialog.COLOR_WARNING);
        }
    }

    @SuppressLint("SetTextI18n")
    private void log(String tag, String logs) {
        Log.v("Edxp/SELinux", logs);
        commandLog.append("\nEdxp/SELinux · " + tag + ": " + logs);
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    @SuppressLint("SetTextI18n")
    private void log(String tag, String logs, int color) {
        Log.v("Edxp/SELinux", logs);
        String log_out = "\nEdxp/SELinux · " + tag + ": " + logs;
        SpannableString spanString = new SpannableString(log_out);
        ForegroundColorSpan span = new ForegroundColorSpan(color);
        spanString.setSpan(span, 0, log_out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        commandLog.append(spanString);
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

}
