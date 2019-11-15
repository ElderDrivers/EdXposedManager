package de.robv.android.xposed.installer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.adapter.AppHelper;
import com.solohsu.android.edxp.manager.fragment.BlackListFragment;
import com.solohsu.android.edxp.manager.fragment.CompatListFragment;
import com.solohsu.android.edxp.manager.fragment.SettingFragment;

import java.util.Objects;

import de.robv.android.xposed.installer.activity.AboutActivity;
import de.robv.android.xposed.installer.activity.SELinuxActivity;
import de.robv.android.xposed.installer.activity.SettingsActivity;
import de.robv.android.xposed.installer.activity.SupportActivity;
import de.robv.android.xposed.installer.util.ThemeUtil;
import solid.ren.skinlibrary.base.SkinBaseFragment;

import static android.content.Context.MODE_PRIVATE;

public class MoreOptionsFragment extends SkinBaseFragment {

    private View rootView = null;
    private BottomNavigationView bnv;

    public MoreOptionsFragment(BottomNavigationView view) {
        bnv = view;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.options_more, container, false);
        }
        return rootView;
    }

    private void switchFragment(Context context, Fragment navFragment) {
        bnv.getMenu().findItem(R.id.nav_item_more).setIcon(R.drawable.ic_back).setTitle("返回");
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
        if (navFragment != null) {
            FragmentTransaction transaction = ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
            try {
                transaction.replace(R.id.content_frame, navFragment).commit();
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private int dp(float value) {
        float density = rootView.getContext().getResources().getDisplayMetrics().density;
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LinearLayout black_lis = rootView.findViewById(R.id.opt_black_lists);
        LinearLayout details = rootView.findViewById(R.id.opt_logs_details);
        LinearLayout support_mode = rootView.findViewById(R.id.opt_support_mode);
        LinearLayout edxp = rootView.findViewById(R.id.opt_edxp_settings);
        LinearLayout settings = rootView.findViewById(R.id.opt_settings);
        LinearLayout support = rootView.findViewById(R.id.opt_support_me);
        LinearLayout about = rootView.findViewById(R.id.opt_about);
        View donateMe = rootView.findViewById(R.id.donateMe);
        View changeToDrawer = rootView.findViewById(R.id.changeToDrawer);
        View nightMode = rootView.findViewById(R.id.night_mode_container);
        View selinux = rootView.findViewById(R.id.opt_selinux);
        Switch nightModeSwitch = rootView.findViewById(R.id.night_mode_switch);
        View divider = rootView.findViewById(R.id.black_list_divider);
        final ImageView ico = rootView.findViewById(R.id.donateIcon);
        final int[] click = {0};

        if (AppHelper.blackWhiteListEnabled()) {
            black_lis.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
        }

        black_lis.setOnClickListener(view -> switchFragment(rootView.getContext(), new BlackListFragment()));

        details.setOnClickListener(view -> switchFragment(rootView.getContext(), LogsFragment.newInstance("all")));

        support_mode.setOnClickListener(view -> switchFragment(rootView.getContext(), new CompatListFragment()));

        edxp.setOnClickListener(view -> switchFragment(rootView.getContext(), new SettingFragment()));

        donateMe.setOnClickListener(view -> {
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

        changeToDrawer.setOnClickListener(v -> {
            SharedPreferences.Editor editor = getActivity().getPreferences(MODE_PRIVATE).edit();
            editor.putBoolean("drawer_layout_home", true);
            editor.apply();
            Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(getActivity().getApplication().getPackageName());
            Objects.requireNonNull(intent).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            getActivity().finish();
        });

        if (ThemeUtil.getSelectTheme().equals("light")) {
            nightModeSwitch.setChecked(false);
        } else if (ThemeUtil.getSelectTheme().equals("dark")) {
            nightModeSwitch.setChecked(true);
        }

        nightMode.setOnClickListener(v -> {
            nightModeSwitch.toggle();
            boolean isChecked = nightModeSwitch.isChecked();
            XposedApp.getPreferences().edit().putString("theme", isChecked ? "dark" : "light").apply();
            Intent intent = Objects.requireNonNull(getActivity()).getPackageManager().getLaunchIntentForPackage(getActivity().getApplication().getPackageName());
            Objects.requireNonNull(intent).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            getActivity().finish();
        });

        selinux.setOnClickListener(v -> startActivity(new Intent(rootView.getContext(), SELinuxActivity.class)));

        settings.setOnClickListener(view -> startActivity(new Intent(rootView.getContext(), SettingsActivity.class)));

        support.setOnClickListener(view -> startActivity(new Intent(rootView.getContext(), SupportActivity.class)));

        about.setOnClickListener(view -> startActivity(new Intent(rootView.getContext(), AboutActivity.class)));

        super.onActivityCreated(savedInstanceState);
    }

}
