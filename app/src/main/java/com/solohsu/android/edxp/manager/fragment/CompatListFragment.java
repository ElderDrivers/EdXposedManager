package com.solohsu.android.edxp.manager.fragment;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.solohsu.android.edxp.manager.adapter.AppAdapter;
import com.solohsu.android.edxp.manager.adapter.CompatListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.solohsu.android.edxp.manager.R;

public class CompatListFragment extends Fragment implements AppAdapter.Callback {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private SearchView mSearchView;
    private CompatListAdapter mAppAdapter;

    private SearchView.OnQueryTextListener mSearchListener;

    public CompatListFragment() {
        setRetainInstance(true);
    }

    public static CompatListFragment newInstance() {
        return new CompatListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.title_compat_list);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_compat_list, menu);
        mSearchView = (SearchView) menu.findItem(R.id.app_search).getActionView();
        mSearchView.setOnQueryTextListener(mSearchListener);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_list, container, false);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        mRecyclerView = view.findViewById(R.id.recyclerView);

        mAppAdapter = new CompatListAdapter(requireActivity());
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

    @Override
    public void onDataReady() {
        mSwipeRefreshLayout.setRefreshing(false);
        String queryStr = mSearchView != null ? mSearchView.getQuery().toString() : "";
        mAppAdapter.filter(queryStr);
    }

    @Override
    public void onItemClick(View v, ApplicationInfo info) {
    }
}
