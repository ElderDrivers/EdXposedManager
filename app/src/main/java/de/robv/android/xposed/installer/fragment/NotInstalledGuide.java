package de.robv.android.xposed.installer.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.annimon.stream.Stream;
import com.github.coxylicacid.takagi.Takagi;
import com.google.gson.Gson;
import com.solohsu.android.edxp.manager.R;

import java.util.List;

import de.robv.android.xposed.installer.activity.PingActivity;
import de.robv.android.xposed.installer.util.json.JSONUtils;
import de.robv.android.xposed.installer.util.json.XposedTab;
import de.robv.android.xposed.installer.util.json.XposedZip;

@SuppressLint("InflateParams")
public class NotInstalledGuide extends Fragment {

    private View rootView;
    private FrameLayout container;
    private RelativeLayout progress;
    private Takagi takagi;
    private List<XposedZip> core;
    private List<XposedZip> alpha;
    private List<XposedZip> canary;
    private int selected = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.activity_guide_container, container, false);
        }

        container = rootView.findViewById(R.id.container);
        progress = rootView.findViewById(R.id.progress);

        takagi = new Takagi(requireActivity());
        takagi.setTitle(getString(R.string.installation_guide));

        takagi.add("Riru-Core", getString(R.string.rirucore_description));
        takagi.add("Riru-EdXposed", getString(R.string.riruedxposed_description));
        takagi.add("Reboot", getString(R.string.reboot_description));
        takagi.select(0);
        takagi.setIndicatorRadius(120);

        takagi.applyForViewGroup(container);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loading(true);
        new LoadFrameWork().execute();

        findViewById(R.id.nextStep).setOnClickListener(v -> {
            takagi.next();
            if (selected < 3) {
                selected++;
                findViewById(R.id.previous).setVisibility(View.VISIBLE);
            }

            if (selected == 3) {
                findViewById(R.id.previous).setVisibility(View.VISIBLE);
                findViewById(R.id.nextStep).setVisibility(View.GONE);
            }
        });

        findViewById(R.id.previous).setOnClickListener(v -> {
            takagi.previous();
            if (selected > 0) {
                selected--;
                findViewById(R.id.nextStep).setVisibility(View.VISIBLE);
            }
            if (selected == 0) {
                findViewById(R.id.previous).setVisibility(View.GONE);
                findViewById(R.id.nextStep).setVisibility(View.VISIBLE);
            }
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

    private View findViewById(int id) {
        return rootView.findViewById(id);
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadFrameWork extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                String s = JSONUtils.getFileContent(PingActivity.getFrameWorkLink());
                String newJson = JSONUtils.listZip();
                String jsonString = s.replace("%XPOSED_ZIP%", newJson);
                final JSONUtils.XposedJson xposedJson = new Gson().fromJson(jsonString, JSONUtils.XposedJson.class);
                List<XposedTab> tabs = Stream.of(xposedJson.tabs).filter(value -> value.sdks.contains(Build.VERSION.SDK_INT)).toList();
                core = tabs.get(0).getInstallers();
                alpha = tabs.get(1).getInstallers();
                canary = tabs.get(2).getInstallers();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            takagi.get(0).setSummary(String.format(
                    getString(R.string.rirucore_description),
                    core.get(0).version
            ));
            takagi.get(1).setSummary(String.format(
                    getString(R.string.riruedxposed_description),
                    alpha.get(0).version,
                    canary.get(canary.size() - 1).version
            ));
            loading(false);
        }
    }

}
