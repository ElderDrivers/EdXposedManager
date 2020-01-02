package de.robv.android.xposed.installer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.fragment.BlackListFragment;
import com.solohsu.android.edxp.manager.fragment.CompatListFragment;
import com.solohsu.android.edxp.manager.fragment.SettingFragment;

import de.robv.android.xposed.installer.activity.PersonalizeFragment;
import de.robv.android.xposed.installer.activity.SELinuxActivity;
import de.robv.android.xposed.installer.activity.XposedSettingsActivity;

public class MoreOptionsFragment extends Fragment {

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
        LinearLayout personalize = rootView.findViewById(R.id.opt_personalize);
        View donateMe = rootView.findViewById(R.id.donateMe);
        View selinux = rootView.findViewById(R.id.opt_selinux);
        View divider = rootView.findViewById(R.id.black_list_divider);
        final ImageView ico = rootView.findViewById(R.id.donateIcon);
        final int[] click = {0};

        black_lis.setOnClickListener(view -> switchFragment(rootView.getContext(), new BlackListFragment()));

        details.setOnClickListener(view -> switchFragment(rootView.getContext(), LogsFragment.newInstance("all")));

        support_mode.setOnClickListener(view -> switchFragment(rootView.getContext(), new CompatListFragment()));

        edxp.setOnClickListener(view -> switchFragment(rootView.getContext(), new SettingFragment()));

        settings.setOnClickListener(view -> startActivity(new Intent(rootView.getContext(), XposedSettingsActivity.class)));

        personalize.setOnClickListener(v -> switchFragment(rootView.getContext(), new PersonalizeFragment()));

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

        selinux.setOnClickListener(v -> startActivity(new Intent(rootView.getContext(), SELinuxActivity.class)));

        super.onActivityCreated(savedInstanceState);
    }

}
