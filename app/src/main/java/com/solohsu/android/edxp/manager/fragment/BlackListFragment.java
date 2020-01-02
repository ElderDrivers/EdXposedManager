package com.solohsu.android.edxp.manager.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.adapter.AppAdapter;
import com.solohsu.android.edxp.manager.adapter.AppHelper;
import com.solohsu.android.edxp.manager.adapter.BlackListAdapter;

import java.lang.reflect.Field;
import java.util.Objects;

import de.robv.android.xposed.installer.WelcomeActivity;
import de.robv.android.xposed.installer.XposedApp;

public class BlackListFragment extends Fragment implements AppAdapter.Callback {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private androidx.appcompat.widget.SearchView mSearchView;
    private BlackListAdapter mAppAdapter;
    private MenuItem whiteList;
    private boolean white_liste_mode = false;

    private androidx.appcompat.widget.SearchView.OnQueryTextListener mSearchListener;

    private static final int COMPLETED = 0;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == COMPLETED) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    };

    public BlackListFragment() {
        setRetainInstance(true);
    }

    public static BlackListFragment newInstance() {
        return new BlackListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_app_list, menu);
        whiteList = menu.add(R.string.white_list_mode_title).setTitle(R.string.white_list_mode_title);
        mSearchView = (SearchView) menu.findItem(R.id.app_search).getActionView();
        mSearchView.setOnQueryTextListener(mSearchListener);
        DisplayMetrics outMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchView.setMaxWidth(outMetrics.widthPixels);
        try {        //--拿到字节码
            Class<?> argClass = mSearchView.getClass();
            //--指定某个私有属性,mSearchPlate是搜索框父布局的名字
            Field ownField = argClass.getDeclaredField("mSearchPlate");
            //--暴力反射,只有暴力反射才能拿到私有属性
            ownField.setAccessible(true);
            View mView = (View) ownField.get(mSearchView);
            //--设置背景
            Objects.requireNonNull(mView).setBackgroundColor(Color.TRANSPARENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        changeTitle(isWhiteListMode());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == whiteList.getItemId()) {
            if (white_liste_mode) {
                updateUi(false);
                changeTitle(false);
            } else {
                updateUi(true);
                changeTitle(true);
            }
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        changeTitle(isWhiteListMode());

        if (!XposedApp.getPreferences().getBoolean("black_white_list_enabled", false)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("注意")
                    .setMessage("您目前尚未开启黑白名单功能，请打开EdXp设置打开\"黑/白名单\"选项")
                    .setNegativeButton("稍后", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("去打开", (dialog, which) -> {
                        if (requireActivity() instanceof WelcomeActivity) {
                            ((WelcomeActivity) requireActivity()).navigateDrawer(R.id.nav_edxp_settings);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(requireContext(), "无法跳转,请自行打开", Toast.LENGTH_LONG).show();
                        }
                    }).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_list, container, false);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        mRecyclerView = view.findViewById(R.id.recyclerView);

        final boolean isWhiteListMode = isWhiteListMode();
        mAppAdapter = new BlackListAdapter(requireActivity(), isWhiteListMode);
        mRecyclerView.setAdapter(mAppAdapter);
        mAppAdapter.setCallback(this);

        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(mAppAdapter::refresh);

        mSearchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAppAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAppAdapter.filter(newText);
                return false;
            }
        };

        int left = requireActivity().getWindow().getDecorView().getWidth() - dp(45);
        int top = dp(35);
        int right = requireActivity().getWindow().getDecorView().getWidth();
        int bottom = top + dp(35);
        if (!XposedApp.getPreferences().getBoolean("first_show_guide", false)) {
            new TapTargetSequence(requireActivity())
                    .targets(
                            XposedApp.targetView(new Rect(left, top, right, bottom), "白名单入口", "切换EdXposed白名单/黑名单")
                    ).listener(new TapTargetSequence.Listener() {
                // This listener will tell us when interesting(tm) events happen in regards
                // to the sequence
                @Override
                public void onSequenceFinish() {
                    XposedApp.getPreferences().edit().putBoolean("first_show_guide", true).apply();
                }

                @Override
                public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                }

                @Override
                public void onSequenceCanceled(TapTarget lastTarget) {
                    // Boo
                }
            }).start();
        }
        return view;
    }

    private void changeTitle(boolean isWhiteListMode) {
        requireActivity().setTitle(isWhiteListMode ? R.string.title_white_list : R.string.title_black_list);
        if (whiteList != null) {
            whiteList.setTitle(isWhiteListMode ? R.string.title_black_list : R.string.title_white_list);
            whiteList.setTooltipText(isWhiteListMode ? getString(R.string.title_black_list) : getString(R.string.title_white_list));
        }
    }

    private void updateUi(boolean isWhiteListMode) {
        changeTitle(isWhiteListMode());
        mAppAdapter.setWhiteListMode(isWhiteListMode);
        mSwipeRefreshLayout.setRefreshing(true);
        mAppAdapter.refresh();
        white_liste_mode = !white_liste_mode;
    }

    private boolean isWhiteListMode() {
        return AppHelper.isWhiteListMode();
    }

    @Override
    public void onDataReady() {
        Message message = new Message();
        message.what = COMPLETED;
        handler.sendMessage(message);
        String queryStr = mSearchView != null ? mSearchView.getQuery().toString() : "";
        mAppAdapter.filter(queryStr);
    }

    @Override
    public void onItemClick(View v, ApplicationInfo info) {
        if (getFragmentManager() != null) {
            AppHelper.showMenu(requireActivity(), getFragmentManager(), v, info);
        } else {
            String packageName = (String) v.getTag();
            if (packageName == null)
                return;

            Intent launchIntent = Objects.requireNonNull(getContext()).getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(getActivity(), Objects.requireNonNull(getActivity()).getString(R.string.app_no_ui), Toast.LENGTH_LONG).show();
            }
        }
    }

    private int dp(float value) {
        float density = requireActivity().getResources().getDisplayMetrics().density;
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }
}
