package com.solohsu.android.edxp.manager.fragment;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.solohsu.android.edxp.manager.adapter.AppAdapter;
import com.solohsu.android.edxp.manager.adapter.AppHelper;
import com.solohsu.android.edxp.manager.adapter.BlackListAdapter;
import com.solohsu.android.edxp.manager.util.ToastUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.meowcat.edxposed.manager.R;

public class BlackListFragment extends Fragment implements AppAdapter.Callback {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private SearchView mSearchView;
    private BlackListAdapter mAppAdapter;

    private SearchView.OnQueryTextListener mSearchListener;

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
        mSearchView = (SearchView) menu.findItem(R.id.app_search).getActionView();
        mSearchView.setOnQueryTextListener(mSearchListener);
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
        mSwipeRefreshLayout.setRefreshing(false);
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
