package com.solohsu.android.edxp.manager.fragment;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.solohsu.android.edxp.manager.R;
import com.solohsu.android.edxp.manager.adapter.AppAdapter;
import com.solohsu.android.edxp.manager.adapter.AppHelper;
import com.solohsu.android.edxp.manager.adapter.BlackListAdapter;
import com.solohsu.android.edxp.manager.util.ToastUtils;

import java.lang.reflect.Field;

public class BlackListFragment extends Fragment implements AppAdapter.Callback {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private androidx.appcompat.widget.SearchView mSearchView;
    private BlackListAdapter mAppAdapter;

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_black_list, menu);
        mSearchView = (androidx.appcompat.widget.SearchView) menu.findItem(R.id.app_search).getActionView();
        mSearchView.setOnQueryTextListener(mSearchListener);
        DisplayMetrics outMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
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
            mView.setBackgroundColor(Color.TRANSPARENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        MenuItem whiteListMenuItem = menu.findItem(R.id.white_list_switch);
        whiteListMenuItem.setChecked(isWhiteListMode());
        whiteListMenuItem.setOnMenuItemClickListener(item -> {
            item.setChecked(!item.isChecked());
            if (AppHelper.setWhiteListMode(item.isChecked())) {
                updateUi(item.isChecked());
            } else {
                ToastUtils.showShortToast(requireContext(), R.string.mode_change_failed);
            }
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        changeTitle(isWhiteListMode());
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
        return view;
    }

    private void changeTitle(boolean isWhiteListMode) {
        requireActivity().setTitle(isWhiteListMode ? R.string.title_white_list : R.string.title_black_list);
    }

    private void updateUi(boolean isWhiteListMode) {
        changeTitle(isWhiteListMode());
        mAppAdapter.setWhiteListMode(isWhiteListMode);
        mSwipeRefreshLayout.setRefreshing(true);
        mAppAdapter.refresh();
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
        }
    }
}
