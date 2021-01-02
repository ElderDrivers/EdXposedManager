package org.meowcat.edxposed.manager;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.meowcat.edxposed.manager.adapter.ActivationScopeAdapter;
import org.meowcat.edxposed.manager.adapter.AppAdapter;
import org.meowcat.edxposed.manager.adapter.AppHelper;
import org.meowcat.edxposed.manager.widget.MasterSwitch;

import java.util.Collections;

public class ActivationScopeActivity extends BaseActivity implements AppAdapter.Callback {
    private SearchView mSearchView;
    private ActivationScopeAdapter mAppAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private SearchView.OnQueryTextListener mSearchListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_module_scope);
        String modulePackageName = getIntent().getStringExtra("modulePackageName");
        String moduleName = getIntent().getStringExtra("moduleName");

        Toolbar toolbar = findViewById(R.id.toolbar);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        MasterSwitch masterSwitch = findViewById(R.id.master_switch);
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(moduleName);
            bar.setSubtitle(modulePackageName);
        }
        mAppAdapter = new ActivationScopeAdapter(this, modulePackageName, masterSwitch, mSwipeRefreshLayout);
        recyclerView.setAdapter(mAppAdapter);
        mAppAdapter.setCallback(this);

        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(() -> mAppAdapter.refresh(Collections.singletonList(modulePackageName)));

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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        AppAdapter.onCreateOptionsMenu(menu, getMenuInflater());
        mSearchView = (SearchView) menu.findItem(R.id.app_search).getActionView();
        mSearchView.setOnQueryTextListener(mSearchListener);
        menu.findItem(R.id.menu_optimize).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return AppAdapter.onOptionsItemSelected(item);
    }

    @Override
    public void onDataReady() {
        mSwipeRefreshLayout.setRefreshing(false);
        String queryStr = mSearchView != null ? mSearchView.getQuery().toString() : "";
        mAppAdapter.filter(queryStr);
    }

    @Override
    public void onItemClick(View v, ApplicationInfo info) {
        AppHelper.showMenu(this, getSupportFragmentManager(), v, info);
    }

    @Override
    public void onBackPressed() {
        if (mSearchView.isIconified()) {
            super.onBackPressed();
        } else {
            mSearchView.setIconified(true);
        }
    }
}
