package org.meowcat.edxposed.manager;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.meowcat.edxposed.manager.adapter.AppAdapter;
import org.meowcat.edxposed.manager.adapter.AppHelper;
import org.meowcat.edxposed.manager.adapter.BlackListAdapter;

import java.util.Objects;

public class BlackListFragment extends Fragment implements AppAdapter.Callback {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    //private RecyclerView mRecyclerView;
    private SearchView mSearchView;
    private BlackListAdapter mAppAdapter;

    private SearchView.OnQueryTextListener mSearchListener;

    public BlackListFragment() {
        setRetainInstance(true);
    }

    //public static BlackListFragment newInstance() {
    //    return new BlackListFragment();
    //}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_app_list, menu);
        mSearchView = (SearchView) menu.findItem(R.id.app_search).getActionView();
        mSearchView.setOnQueryTextListener(mSearchListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        changeTitle(isBlackListMode(), isWhiteListMode());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_list, container, false);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        RecyclerView mRecyclerView = view.findViewById(R.id.recyclerView);

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

    private void changeTitle(boolean isBlackListMode, boolean isWhiteListMode) {
        if (isBlackListMode) {
            requireActivity().setTitle(isWhiteListMode ? R.string.title_white_list : R.string.title_black_list);
        } else {
            requireActivity().setTitle(R.string.nav_title_black_list);
        }
    }

    private boolean isWhiteListMode() {
        return AppHelper.isWhiteListMode();
    }

    private boolean isBlackListMode() {
        return AppHelper.isBlackListMode();
    }

    @Override
    public void onDataReady() {
        mSwipeRefreshLayout.setRefreshing(false);
        String queryStr = mSearchView != null ? mSearchView.getQuery().toString() : "";
        mAppAdapter.filter(queryStr);
    }

    @SuppressWarnings("deprecation")
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
}
