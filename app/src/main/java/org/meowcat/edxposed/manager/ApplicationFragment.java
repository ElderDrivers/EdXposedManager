package org.meowcat.edxposed.manager;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.meowcat.edxposed.manager.adapter.AppAdapter;
import org.meowcat.edxposed.manager.adapter.AppHelper;
import org.meowcat.edxposed.manager.adapter.ApplicationListAdapter;

public class ApplicationFragment extends Fragment implements AppAdapter.Callback {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SearchView mSearchView;
    private ApplicationListAdapter mAppAdapter;

    private SearchView.OnQueryTextListener mSearchListener;

    public ApplicationFragment() {
        setRetainInstance(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        AppAdapter.onCreateOptionsMenu(menu, inflater);
        mSearchView = (SearchView) menu.findItem(R.id.app_search).getActionView();
        mSearchView.setOnQueryTextListener(mSearchListener);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return AppAdapter.onOptionsItemSelected(item);
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
        RecyclerView mRecyclerView = view.findViewById(R.id.recyclerView);

        final boolean isWhiteListMode = isWhiteListMode();
        mAppAdapter = new ApplicationListAdapter(requireActivity(), isWhiteListMode);
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
        requireActivity().setTitle(String.format("%s(%s)", getString(R.string.nav_title_black_list), getString(isWhiteListMode ? R.string.title_white_list : R.string.title_black_list)));
    }

    private boolean isWhiteListMode() {
        return AppHelper.isWhiteListMode();
    }

    private boolean isBlackListMode() {
        return true;
    }

    @Override
    public void onDataReady() {
        mSwipeRefreshLayout.setRefreshing(false);
        String queryStr = mSearchView != null ? mSearchView.getQuery().toString() : "";
        mAppAdapter.filter(queryStr);
    }

    @Override
    public void onItemClick(View v, ApplicationInfo info) {
        AppHelper.showMenu(requireActivity(), getParentFragmentManager(), v, info);
    }
}
