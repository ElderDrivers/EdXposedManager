package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.meowcat.edxposed.manager.adapter.LogsAdapter;
import org.meowcat.edxposed.manager.adapter.LogsHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;

import static android.app.Activity.RESULT_OK;
import static org.meowcat.edxposed.manager.adapter.LogsHelper.isMainUser;

public class LogsFragment extends Fragment {

    private final static int REQUEST_CODE = 233;
    public static HashMap<String, String> activatedConfig;
    private final HashMap<String, String> mVerboseLogConfig = new HashMap<String, String>() {
        {
            put("name", "Verbose");
            put("fileName", "all");
        }
    };
    private final HashMap<String, String> mModulesLogConfig = new HashMap<String, String>() {
        {
            put("name", "Modules");
            put("fileName", "error");
        }
    };
    private final String LOG_SUFFIX = ".log";
    @SuppressWarnings("FieldCanBeLocal")
    private final String LOG_OLD_SUFFIX = ".log.old";
    private final String LOG_PATH = XposedApp.BASE_DIR + "log/";
    private RecyclerView mRecyclerView;
    private TabLayout mTabLayout;
    private LogsAdapter adapter;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_logs, container, false);
        mTabLayout = v.findViewById(R.id.sliding_tabs);
        mRecyclerView = v.findViewById(R.id.recyclerView);
        mTabLayout.setBackgroundColor(XposedApp.getColor(requireContext()));
        activatedConfig = mModulesLogConfig;
        adapter = new LogsAdapter(getContext(), mRecyclerView);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        View scrollTop = v.findViewById(R.id.scroll_top);
        View scrollDown = v.findViewById(R.id.scroll_down);

        scrollTop.setOnClickListener(v1 -> scrollTop(false));
        scrollDown.setOnClickListener(v12 -> scrollDown(false));

        if (XposedApp.getPreferences().getBoolean("disable_verbose_log", false)) {
            mTabLayout.setVisibility(View.GONE);
        } else {
            mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    switch (tab.getPosition()) {
                        case 0:
                            activatedConfig = mModulesLogConfig;
                            break;
                        case 1:
                            activatedConfig = mVerboseLogConfig;
                            break;
                        default:
                            break;
                    }
                    reloadLog();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }

        if (!XposedApp.getPreferences().getBoolean("hide_logcat_warning", false)) {
            @SuppressLint("InflateParams") final View dontShowAgainView = inflater.inflate(R.layout.dialog_install_warning, null);

            TextView message = dontShowAgainView.findViewById(android.R.id.message);
            message.setText(R.string.not_logcat);

            new MaterialDialog.Builder(requireActivity())
                    .title(R.string.install_warning_title)
                    .customView(dontShowAgainView, false)
                    .positiveText(R.string.ok)
                    .onPositive((dialog, which) -> {
                        CheckBox checkBox = dontShowAgainView.findViewById(android.R.id.checkbox);
                        if (checkBox.isChecked())
                            XposedApp.getPreferences().edit().putBoolean("hide_logcat_warning", true).apply();
                    }).cancelable(false).show();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (XposedApp.getPreferences().getBoolean("disable_verbose_log", false)) {
            mTabLayout.setVisibility(View.GONE);
            activatedConfig = mModulesLogConfig;
        } else {
            mTabLayout.setVisibility(View.VISIBLE);
        }
        mTabLayout.setBackgroundColor(XposedApp.getColor(requireContext()));
        reloadLog();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (isMainUser(requireContext())) {
            inflater.inflate(R.menu.menu_logs, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scroll_top:
                scrollTop(true);
                break;
            case R.id.menu_scroll_down:
                scrollDown(true);
                break;
            case R.id.menu_refresh:
                reloadLog();
                return true;
            case R.id.menu_send:
                send();
                return true;
            case R.id.menu_save:
                save();
                return true;
            case R.id.menu_clear:
                clear();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scrollTop(boolean immediately) {
        if (immediately) {
            mRecyclerView.scrollToPosition(0);
        } else {
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

    private void scrollDown(boolean immediately) {
        if (immediately) {
            mRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
        } else {
            mRecyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void reloadLog() {
        new LogsHelper(getContext(), adapter).execute(new File(LOG_PATH + activatedConfig.get("fileName") + LOG_SUFFIX));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void clear() {
        try {
            new FileOutputStream(LOG_PATH + activatedConfig.get("fileName") + LOG_SUFFIX).close();
            new File(LOG_PATH + activatedConfig.get("fileName") + LOG_OLD_SUFFIX).delete();
            adapter.setEmpty();
            Snackbar.make(requireView().findViewById(R.id.container), R.string.logs_cleared, Snackbar.LENGTH_LONG).show();
            reloadLog();
        } catch (IOException e) {
            Snackbar.make(requireView().findViewById(R.id.container), getResources().getString(R.string.logs_clear_failed) + "\n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void send() {
        Uri uri = FileProvider.getUriForFile(requireActivity(), BuildConfig.APPLICATION_ID + ".fileprovider", new File(LOG_PATH + activatedConfig.get("fileName") + LOG_SUFFIX));
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("application/html");
        startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.menuSend)));
    }

    private void save() {
        Calendar now = Calendar.getInstance();
        String filename = String.format(
                "EdXposed_" + activatedConfig.get("name") + "_%04d%02d%02d_%02d%02d%02d.txt",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), now.get(Calendar.SECOND));

        Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        exportIntent.addCategory(Intent.CATEGORY_OPENABLE);
        exportIntent.setType("text/*");
        exportIntent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(exportIntent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
                        if (os != null) {
                            FileInputStream in = new FileInputStream(LOG_PATH + activatedConfig.get("fileName") + LOG_SUFFIX);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = in.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }
                            os.close();
                        }
                    } catch (Exception e) {
                        Snackbar.make(requireView().findViewById(R.id.container), getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        }
    }


}
