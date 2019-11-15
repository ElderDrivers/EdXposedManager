package de.robv.android.xposed.installer.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.github.coxylicacid.mdwidgets.dialog.MD2Dialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.solohsu.android.edxp.manager.R;
import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;
import com.stealthcopter.networktools.ping.PingStats;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.XposedBaseActivity;
import de.robv.android.xposed.installer.util.RepoLoader;
import de.robv.android.xposed.installer.util.ThemeUtil;
import de.robv.android.xposed.installer.util.json.JSONUtils;

public class PingActivity extends XposedBaseActivity implements View.OnClickListener {

    private static final int UPDATE_FRAMEWORK_STATUS = -1;
    private static final int UPDATE_MODULES_STATUS = -2;
    private static final int UPDATE_APK_STATUS = -3;

    private ExtendedFloatingActionButton pingGo;

    private LinearLayout frameWorkContainer;
    private LinearLayout modulesContainer;
    private LinearLayout md2Container;

    private ProgressBar frameWorkProgress;
    private ProgressBar modulesProgress;
    private ProgressBar md2Progress;

    private TextView frameWorkSpeed;
    private TextView modulesSpeed;
    private TextView md2Speed;

    private TextView frameWorkLink;
    private TextView modulesLink;
    private TextView md2Link;

    private ImageView frameWorkNav;
    private ImageView modulesNav;
    private ImageView md2Nav;

    private MaterialButton frameWorkCustom;
    private MaterialButton modulesCustom;
    private MaterialButton md2Custom;

    private Toolbar toolbar;

    private List<UpdateLink> lstApi = new ArrayList<>();

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_FRAMEWORK_STATUS:
                    frameWorkProgress.setVisibility(View.GONE);
                    if ((float) msg.obj > 0)
                        frameWorkSpeed.setText(msg.obj.toString() + "ms");
                    else
                        frameWorkSpeed.setText("超时");
                    break;
                case UPDATE_MODULES_STATUS:
                    modulesProgress.setVisibility(View.GONE);
                    if ((float) msg.obj > 0)
                        modulesSpeed.setText(msg.obj.toString() + "ms");
                    else
                        modulesSpeed.setText("超时");
                    break;
                case UPDATE_APK_STATUS:
                    md2Progress.setVisibility(View.GONE);
                    if ((float) msg.obj > 0)
                        md2Speed.setText(msg.obj.toString() + "ms");
                    else
                        md2Speed.setText("超时");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_ping);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("获取源设置");

        initData();
        initViews();
        ThemeUtil.setTheme(this);
    }

    private void initData() {
        lstApi.add(new UpdateLink(getAddress(getFrameWorkLink()), 0));
        lstApi.add(new UpdateLink(getAddress(getModulesLink()), 1));
        lstApi.add(new UpdateLink(getAddress(getMaterialApkLink()), 2));
    }

    private void refreshPing() {
        lstApi.clear();
        initData();
    }

    private void initViews() {
        frameWorkContainer = findViewById(R.id.framework_container);
        modulesContainer = findViewById(R.id.modules_container);
        md2Container = findViewById(R.id.md2_container);
        frameWorkProgress = findViewById(R.id.framework_source_progress);
        modulesProgress = findViewById(R.id.modules_source_progress);
        md2Progress = findViewById(R.id.md2_source_progress);
        frameWorkSpeed = findViewById(R.id.framework_source_speed);
        modulesSpeed = findViewById(R.id.modules_source_speed);
        md2Speed = findViewById(R.id.md2_source_speed);
        frameWorkLink = findViewById(R.id.framework_source_text);
        modulesLink = findViewById(R.id.modules_source_text);
        md2Link = findViewById(R.id.md2_source_text);
        frameWorkNav = findViewById(R.id.framework_source_nav);
        modulesNav = findViewById(R.id.modules_source_nav);
        md2Nav = findViewById(R.id.md2_source_nav);
        frameWorkCustom = findViewById(R.id.framework_source_custom);
        modulesCustom = findViewById(R.id.modules_source_custom);
        md2Custom = findViewById(R.id.md2_source_custom);

        MaterialButton frameWorkReset = findViewById(R.id.framework_source_reset);
        MaterialButton modulesReset = findViewById(R.id.modules_source_reset);
        MaterialButton md2Reset = findViewById(R.id.md2_source_reset);

        frameWorkReset.setOnClickListener(this);
        modulesReset.setOnClickListener(this);
        md2Reset.setOnClickListener(this);

        pingGo = findViewById(R.id.ping_go);
        pingGo.setOnClickListener(this);

        frameWorkLink.setText(getFrameWorkLink());
        modulesLink.setText(getModulesLink());
        md2Link.setText(getMaterialApkLink());

        frameWorkNav.setOnClickListener(v -> {
            if (frameWorkContainer.getVisibility() == View.VISIBLE) {
                frameWorkContainer.setVisibility(View.GONE);
                frameWorkNav.setImageResource(R.drawable.ic_expand_more);
            } else {
                frameWorkContainer.setVisibility(View.VISIBLE);
                frameWorkNav.setImageResource(R.drawable.ic_expand_less);
            }
        });

        modulesNav.setOnClickListener(v -> {
            if (modulesContainer.getVisibility() == View.VISIBLE) {
                modulesContainer.setVisibility(View.GONE);
                modulesNav.setImageResource(R.drawable.ic_expand_more);
            } else {
                modulesContainer.setVisibility(View.VISIBLE);
                modulesNav.setImageResource(R.drawable.ic_expand_less);
            }
        });

        md2Nav.setOnClickListener(v -> {
            if (md2Container.getVisibility() == View.VISIBLE) {
                md2Container.setVisibility(View.GONE);
                md2Nav.setImageResource(R.drawable.ic_expand_more);
            } else {
                md2Container.setVisibility(View.VISIBLE);
                md2Nav.setImageResource(R.drawable.ic_expand_less);
            }
        });

        frameWorkCustom.setOnClickListener(v -> {
            MD2Dialog alert = MD2Dialog.create(this)
                    .title("自定义框架获取源")
                    .darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                    .customView(R.layout.md2_custom_source)
                    .simpleCancel(android.R.string.cancel).show();

            TextInputEditText editText = (TextInputEditText) alert.findView(R.id.md2_custom_edit);
            editText.setText(getMaterialApkLink());

            alert.onConfirmClick(android.R.string.ok, (view, dialog) -> {
                if (TextUtils.isEmpty(editText.getText())) {
                    editText.setError("不能为空");
                } else {
                    if (isUrl(editText.getText().toString())) {
                        setFrameWorkLink(editText.getText().toString());
                        Toast.makeText(view.getContext(), "已更新框架源为:" + editText.getText().toString(), Toast.LENGTH_SHORT).show();
                        frameWorkLink.setText(editText.getText().toString());
                        refreshPing();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(view.getContext(), "您输入的不是一个合法地址", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        modulesCustom.setOnClickListener(v -> {
            MD2Dialog alert = MD2Dialog.create(this)
                    .title("自定义模块获取源")
                    .darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                    .customView(R.layout.md2_custom_source)
                    .simpleCancel(android.R.string.cancel).show();

            TextInputEditText editText = (TextInputEditText) alert.findView(R.id.md2_custom_edit);
            editText.setText(getMaterialApkLink());

            alert.onConfirmClick(android.R.string.ok, (view, dialog) -> {
                if (TextUtils.isEmpty(editText.getText())) {
                    editText.setError("不能为空");
                } else {
                    if (isUrl(editText.getText().toString())) {
                        setModulesLink(editText.getText().toString());
                        Toast.makeText(view.getContext(), "已更新模块源为:" + editText.getText().toString(), Toast.LENGTH_SHORT).show();
                        modulesLink.setText(editText.getText().toString());
                        refreshPing();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(view.getContext(), "您输入的不是一个合法地址", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        md2Custom.setOnClickListener(v -> {

            MD2Dialog alert = MD2Dialog.create(this)
                    .title("自定义美化版本获取源")
                    .darkMode(ThemeUtil.getSelectTheme().equals("dark"))
                    .customView(R.layout.md2_custom_source)
                    .simpleCancel(android.R.string.cancel).show();

            TextInputEditText editText = (TextInputEditText) alert.findView(R.id.md2_custom_edit);
            editText.setText(getMaterialApkLink());

            alert.onConfirmClick(android.R.string.ok, (view, dialog) -> {
                if (TextUtils.isEmpty(editText.getText())) {
                    editText.setError("不能为空");
                } else {
                    if (isUrl(editText.getText().toString())) {
                        setMaterialApkLink(editText.getText().toString());
                        Toast.makeText(this, "已更新美化版本源为:" + editText.getText().toString(), Toast.LENGTH_SHORT).show();
                        md2Link.setText(editText.getText().toString());
                        refreshPing();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, "您输入的不是一个合法地址", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private boolean isUrl(String urls) {
        boolean isurl = false;
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))"
                + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";//设置正则表达式
        Pattern pat = Pattern.compile(regex.trim());//对比
        Matcher mat = pat.matcher(urls.trim());
        isurl = mat.matches();//判断是否匹配
        return isurl;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ping_go:
                if (frameWorkProgress.getVisibility() == View.GONE && modulesProgress.getVisibility() == View.GONE && md2Progress.getVisibility() == View.GONE) {
                    frameWorkProgress.setVisibility(View.VISIBLE);
                    modulesProgress.setVisibility(View.VISIBLE);
                    md2Progress.setVisibility(View.VISIBLE);

                    for (final UpdateLink api : lstApi) {
                        new Thread(() -> Ping.onAddress(api.getLink()).setTimeOutMillis(1000).setTimes(5).doPing(new Ping.PingListener() {
                            @Override
                            public void onResult(PingResult pingResult) {
                            }

                            @Override
                            public void onFinished(PingStats pingStats) {
//                                Log.e("PING", pingStats.toString());
                                Message msg = new Message();
                                msg.obj = pingStats.getAverageTimeTaken();
                                switch (api.getType()) {
                                    case 0:
                                        msg.what = UPDATE_FRAMEWORK_STATUS;
                                        break;
                                    case 1:
                                        msg.what = UPDATE_MODULES_STATUS;
                                        break;
                                    case 2:
                                        msg.what = UPDATE_APK_STATUS;
                                        break;
                                }
                                handler.sendMessage(msg);
                            }

                            @Override
                            public void onError(Exception e) {

                            }
                        })).start();
                    }
                } else {
                    Toast.makeText(this, "正在测试连通性，请勿重复操作", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.framework_source_reset:
                MD2Dialog.create(this).title("重置")
                        .msg("您确定重置?")
                        .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
                        .simpleCancel()
                        .darkMode(mTheme.equals("dark"))
                        .onConfirmClick(android.R.string.ok, (view, dialog) -> {
                            frameWorkLink.setText(JSONUtils.JSON_LINK);
                            setFrameWorkLink(JSONUtils.JSON_LINK);
                            Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show();
                            refreshPing();
                            dialog.dismiss();
                        }).show();
                break;
            case R.id.modules_source_reset:
                MD2Dialog.create(this).title("重置")
                        .msg("您确定重置?")
                        .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
                        .simpleCancel()
                        .darkMode(mTheme.equals("dark"))
                        .onConfirmClick(android.R.string.ok, (view, dialog) -> {
                            modulesLink.setText(RepoLoader.DEFAULT_REPOSITORIES);
                            setModulesLink(RepoLoader.DEFAULT_REPOSITORIES);
                            Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show();
                            refreshPing();
                            dialog.dismiss();
                        }).show();
                break;
            case R.id.md2_source_reset:
                MD2Dialog.create(this).title("重置")
                        .msg("您确定重置?")
                        .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
                        .simpleCancel()
                        .darkMode(mTheme.equals("dark"))
                        .onConfirmClick(android.R.string.ok, (view, dialog) -> {
                            md2Link.setText(JSONUtils.UNOFFICIAL_UPDATE_LINK);
                            setMaterialApkLink(JSONUtils.UNOFFICIAL_UPDATE_LINK);
                            Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show();
                            refreshPing();
                            dialog.dismiss();
                        }).show();
                break;
        }
    }

    public static String getFrameWorkLink() {
        return XposedApp.getPreferences().getString("framework_source", JSONUtils.JSON_LINK);
    }

    public static String getModulesLink() {
        return XposedApp.getPreferences().getString("modules_source", RepoLoader.DEFAULT_REPOSITORIES);
    }

    public static String getMaterialApkLink() {
        return XposedApp.getPreferences().getString("apk_md2_source", JSONUtils.UNOFFICIAL_UPDATE_LINK);
    }

    public static void setFrameWorkLink(String link) {
        XposedApp.getPreferences().edit().putString("framework_source", link).apply();
    }

    public static void setModulesLink(String link) {
        XposedApp.getPreferences().edit().putString("modules_source", link).apply();
    }

    public static void setMaterialApkLink(String link) {
        XposedApp.getPreferences().edit().putString("apk_md2_source", link).apply();
    }

    private String getAddress(String urls) {
        URL url = null;
        try {
            url = new URL(urls);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url != null ? url.getHost() : "";
    }

    private class UpdateLink {
        private String link;
        private int type;

        public UpdateLink(String link, int type) {
            this.link = link;
            this.type = type;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public String getLink() {
            return link;
        }
    }
}
