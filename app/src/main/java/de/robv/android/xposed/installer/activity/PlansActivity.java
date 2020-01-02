package de.robv.android.xposed.installer.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.coxylicacid.mdwidgets.dialog.MD2Dialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.rxjava.rxlife.RxLife;
import com.solohsu.android.edxp.manager.BuildConfig;
import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.adapter.PlansAdapter;

import java.io.File;
import java.io.IOException;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.XposedBaseActivity;
import de.robv.android.xposed.installer.installation.StatusInstallerFragment;
import de.robv.android.xposed.installer.util.InstallApkUtil;
import de.robv.android.xposed.installer.util.NavUtil;
import de.robv.android.xposed.installer.util.ThemeUtil;
import de.robv.android.xposed.installer.util.json.JSONUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import rxhttp.wrapper.param.RxHttp;

public class PlansActivity extends XposedBaseActivity {

    private Menu menu;
    private PlansAdapter adapter;
    private String plansJson;
    private RecyclerView recyclerView;
    private RelativeLayout progress;

    private String newApkVersion = null;
    private String newApkLink = null;
    private String newApkChangelog = null;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_plans);

        progress = findViewById(R.id.progress);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.updtae_plans);

        recyclerView = findViewById(R.id.recyclerView);

        loading(true);
        load(false);
        new UpdatePush().execute();
    }

    private void load(boolean update) {
        RxHttp.get(JSONUtils.PLANS_LINK + "?token=" + Math.floor((Math.random() * 10000000) + 1))
                .asString()
                .as(RxLife.asOnMain(this))
                .subscribe(s -> {
                    plansJson = s;
//                    Log.e("PLANS", s);
                    final PlansAdapter.Plans plansClass = new Gson().fromJson(s, PlansAdapter.Plans.class);
                    plansClass.plans.remove(plansClass.plans.size() - 1);
                    adapter = new PlansAdapter(PlansActivity.this, plansClass.plans);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(adapter);
                    if (update) Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show();
                    loading(false);
                }, throwable -> {
                });
    }

    private void loading(boolean onload) {
        if (onload) {
            progress.setVisibility(View.VISIBLE);
            ObjectAnimator.ofFloat(progress, "alpha", 0, 1).setDuration(150).start();
        } else {
            ObjectAnimator animator = ObjectAnimator.ofFloat(progress, "alpha", 1, 0).setDuration(150);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation, boolean isReverse) {
                    progress.setVisibility(View.GONE);
                }
            });
            animator.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_plans, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_update_apk:
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.changes)
                        .setMessage(BuildConfig.VERSION_NAME + " -> " + newApkChangelog)
                        .setPositiveButton("更新", (dialog, which) -> {
                            update();
                            dialog.dismiss();
                        })
                        .setNegativeButton("稍后", (dialog, which) -> dialog.dismiss())
                        .setNeutralButton("Github", ((dialog, which) -> NavUtil.startURL(this, Uri.parse("https://github.com/coxylicacid/Xposed-Fast-Repo")))).show();
                break;
            case R.id.nav_update_list:
                loading(true);
                load(true);
                break;
            case R.id.nav_github:
                NavUtil.startURL(this, Uri.parse("https://github.com/coxylicacid/Xposed-Fast-Repo"));
                break;
        }
        return true;
    }

    @SuppressLint("CheckResult")
    private void update() {
        if (StatusInstallerFragment.checkPermissions()) return;

        final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/EdXpMaterial_" + newApkVersion + ".apk";

        //noinspection ResultOfMethodCallIgnored
        new File(path).delete();

        MD2Dialog alert = MD2Dialog.create(this)
                .title(newApkVersion)
                .msg(R.string.download_view_waiting)
                .progress(false, 100)
                .kbs("")
                .canceledOnTouchOutside(false)
                .darkMode(XposedApp.isNightMode())
                .onCancelClick(R.string.download_view_cancel, (view, dialog) -> dialog.dismiss()).show();

        long length = new File(path).length();
        //noinspection ResultOfMethodCallIgnored
        RxHttp.get(newApkLink)
                .setRangeHeader(length)
                .asDownload(path, length, progress -> {
                    alert.updateProgress(progress.getProgress());
                    alert.kbs(StatusInstallerFragment.fileSize(progress.getCurrentSize()) + "/" + StatusInstallerFragment.fileSize(progress.getTotalSize()));
                }, AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    InstallApkUtil.installApkNormally(this, s);
                    Toast.makeText(this, "文件已保存至: " + s, Toast.LENGTH_LONG).show();
                    alert.dismiss();
                }, throwable -> {
                    throwable.printStackTrace();
                    Toast.makeText(this, "下载失败", Toast.LENGTH_LONG).show();
                });
    }

    @SuppressLint("StaticFieldLeak")
    public class UpdatePush extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String unOfficialLink = JSONUtils.getFileContent(PingActivity.getMaterialApkLink());
                final JSONUtils.UNOFFICIAL_UPDATE updateJson = new Gson().fromJson(unOfficialLink, JSONUtils.UNOFFICIAL_UPDATE.class);
                newApkVersion = updateJson.version;
                newApkLink = updateJson.link;
                newApkChangelog = updateJson.changes;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            try {
                if (newApkVersion == null) return;
                SharedPreferences prefs;
                try {
                    prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
                    prefs.edit().putString("changelog_" + newApkVersion, newApkChangelog).apply();
                } catch (NullPointerException ignored) {
                }
                String a = BuildConfig.VERSION_NAME;
                String b = newApkVersion;
                if (!a.equals(b)) {
                    if (menu.findItem(R.id.nav_update_apk) != null)
                        menu.findItem(R.id.nav_update_apk).setVisible(true);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
