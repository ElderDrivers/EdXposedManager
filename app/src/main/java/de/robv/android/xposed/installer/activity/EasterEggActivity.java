package de.robv.android.xposed.installer.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.solohsu.android.edxp.manager.R;

import java.io.IOException;
import java.util.Objects;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.XposedBaseActivity;
import de.robv.android.xposed.installer.util.NavUtil;
import de.robv.android.xposed.installer.util.ThemeUtil;

public class EasterEggActivity extends XposedBaseActivity implements View.OnTouchListener {

    private TextView eggInfo;
    private RelativeLayout egg;
    private ProgressBar eggProgress;
    private boolean onEgg = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_easter_egg);
        View donateMe = findViewById(R.id.donateMe);
        View iconDesigner = findViewById(R.id.icon_designer);
        View githubLink = findViewById(R.id.github_link);
        View seePlan = findViewById(R.id.update_plan);
        View updateCheck = findViewById(R.id.update_check);
        TextView iconDesignerText = findViewById(R.id.icon_designer_text);
        Switch updateCheckSwitch = findViewById(R.id.update_check_switch);
        ImageView edxpIcon = findViewById(R.id.edxp_icon);
        ImageView back = findViewById(R.id.nav_back);
        View shadow = findViewById(R.id.shadow);
        final ImageView ico = findViewById(R.id.donateIcon);
        final int[] click = {0};

        egg = findViewById(R.id.egg);
        eggInfo = findViewById(R.id.egg_info);
        eggProgress = findViewById(R.id.egg_progress);

        edxpIcon.setOnTouchListener(this);

        donateMe.setOnClickListener(v -> {
            if (click[0] == 0) {
                ico.setVisibility(View.VISIBLE);
                donateMe.setMinimumHeight(dp(58));
                click[0] = 1;
            } else if (click[0] == 1) {
                ico.setVisibility(View.GONE);
                donateMe.setMinimumHeight(dp(58));
                click[0] = 0;
            }
        });

        iconDesigner.setOnClickListener(v -> {
            iconDesignerText.setText("特别鸣谢: 酷安@微软官");
            Toast toast = Toast.makeText(v.getContext(), "图标设计说自己设计得太丑了，不好意思，所以特此改为特别鸣谢！", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        });

        githubLink.setOnClickListener(v -> NavUtil.startURL(this, Uri.parse("https://github.com/coxylicacid/Xposed-Fast-Repo")));

        seePlan.setOnClickListener(v -> startActivity(new Intent(EasterEggActivity.this, PlansActivity.class)));

        updateCheckSwitch.setChecked(XposedApp.getPreferences().getBoolean("auto_update_checkable", true));
        updateCheck.setOnClickListener(v -> {
            updateCheckSwitch.toggle();
            XposedApp.getPreferences().edit().putBoolean("auto_update_checkable", updateCheckSwitch.isChecked()).apply();
        });

        back.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
//        MD2Dialog.create(this)
//                .title("彩蛋！！")
//                .msg("恭喜你打开了新世界的大门！\n好像也并没有啥特殊的嘛！\n其实就是另一个关于界面啦○|￣|_")
//                .buttonStyle(MD2Dialog.ButtonStyle.AGREEMENT)
//                .simpleConfirm("吼！")
//                .darkMode(mTheme.equals("dark"))
//                .show();
    }

    private int dp(float value) {
        float density = getResources().getDisplayMetrics().density;
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    private int sx;
    private int sy;
    private boolean isDraged = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.edxp_icon) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sx = (int) event.getRawX();
                    sy = (int) event.getRawY();
                    v.setElevation(50);
                    break;
                case MotionEvent.ACTION_MOVE:// 手指在屏幕上移动对应的事件
                    int x = (int) event.getRawX();
                    int y = (int) event.getRawY();
                    // 获取手指移动的距离
                    int dx = x - sx;
                    int dy = y - sy;
                    // 得到imageView最开始的各顶点的坐标
                    int l = v.getLeft();
                    int r = v.getRight();
                    int t = v.getTop();
                    int b = v.getBottom();
                    // 更改imageView在窗体的位置
                    v.layout(l + dx, t + dy, r + dx, b + dy);
                    // 获取移动后的位置
                    sx = (int) event.getRawX();
                    sy = (int) event.getRawY();
                    if (!isDraged) {
                        isDraged = true;
                        AnimatorSet set = new AnimatorSet();
                        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1, 1.1f, 1.2f, 1.1f, 1, 0.55f);
                        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1, 1.1f, 1.2f, 1.1f, 1, 0.55f);
                        set.setDuration(250);
                        set.playTogether(scaleX, scaleY);
                        set.start();
                    }
                    break;
                case MotionEvent.ACTION_UP:// 手指离开屏幕对应事件
                    AnimatorSet set = new AnimatorSet();
                    ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 0.55f, 0.35f, 0);
                    ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 0.55f, 0.35f, 0);
                    set.setDuration(100);
                    set.playTogether(scaleX, scaleY);
                    set.start();
                    set.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation, boolean isReverse) {
                            onEgg = true;
//                            if (sy < getWindow().getDecorView().getHeight() / 4) {
//                                NavUtil.startURL(EasterEggActivity.this, Uri.parse("https://nyan.takwolf.com/nyancat.html#http://blog.takwolf.com"));
//                            } else {
//                                egg.setVisibility(View.VISIBLE);
//                                startmusic();
//                            }
                            egg.setVisibility(View.VISIBLE);
                            egg();
                        }
                    });
                    break;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (onEgg) {
            Toast.makeText(this, "车门已经焊死了，我又给你松开了...", Toast.LENGTH_SHORT).show();
            super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    private void egg() {
        eggInfo.setText("作者在筹备新彩蛋...");
        ObjectAnimator animator = ObjectAnimator.ofInt(egg, "backgroundColor", 0xFFFF0000, 0xFF00FF00, 0xFF0000FF).setDuration(2500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
        animator.addUpdateListener(animation -> getWindow().setStatusBarColor((int) animation.getAnimatedValue("backgroundColor")));
        ObjectAnimator animator2 = ObjectAnimator.ofInt(eggInfo, "textColor", 0xFFFFFFFF, 0xFF000000).setDuration(500);
        animator2.setRepeatCount(ValueAnimator.INFINITE);
        animator2.start();
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(eggProgress, "scaleX", 1, 0.35f, 1);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(eggProgress, "scaleY", 1, 0.35f, 1);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        set.setDuration(1500);
        set.playTogether(scaleX, scaleY);
        set.start();
    }

    private void startmusic() {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = Objects.requireNonNull(mAudioManager).getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);

        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = 1;
        window.setAttributes(lp);

        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource("https://raw.githubusercontent.com/coxylicacid/Xposed-Fast-Repo/master/nyancat.mp3");
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                egg();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
