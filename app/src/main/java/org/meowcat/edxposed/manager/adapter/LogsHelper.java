package org.meowcat.edxposed.manager.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.UserHandle;

import com.afollestad.materialdialogs.MaterialDialog;

import org.meowcat.edxposed.manager.LogsFragment;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class LogsHelper extends AsyncTask<File, Integer, ArrayList<String>> {
    private MaterialDialog mProgressDialog;
    @SuppressLint("StaticFieldLeak")
    protected final Context context;
    protected final LogsAdapter adapter;

    public LogsHelper(Context context, LogsAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
    }

    @Override
    protected void onPreExecute() {
        adapter.setEmpty();
        mProgressDialog = new MaterialDialog.Builder(context).content(R.string.loading).progress(true, 0).build();
        if (isMainUser(context) && !(XposedApp.getPreferences().getBoolean("disable_verbose_log", false) && Objects.requireNonNull(LogsFragment.activatedConfig.get("name")).toString().equalsIgnoreCase("Verbose"))) {
            mProgressDialog.show();
        }
    }

    @Override
    protected ArrayList<String> doInBackground(File... log) {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

        ArrayList<String> logs = new ArrayList<>();

        if (!isMainUser(context)) {
            logs.add(context.getResources().getString(R.string.logs_not_primary_user));
            return logs;
        }

        if (XposedApp.getPreferences().getBoolean(
                "disable_verbose_log", false) && Objects.requireNonNull(LogsFragment.activatedConfig.get("name")).toString().equalsIgnoreCase("Verbose")) {
            logs.add(context.getResources().getString(R.string.logs_verbose_disabled));
            return logs;
        }

        try {
            File logfile = log[0];
            try (Scanner scanner = new Scanner(logfile)) {
                while (scanner.hasNextLine()) {
                    logs.add(scanner.nextLine());
                }
            }
        } catch (IOException e) {
            logs.add(context.getResources().getString(R.string.logs_cannot_read));
            logs.addAll(Arrays.asList(e.getMessage().split("\n")));
        }

        return logs;
    }

    @Override
    protected void onPostExecute(ArrayList<String> logs) {
        if (logs.size() == 0) {
            adapter.setEmpty();
        } else {
            adapter.setLogs(logs);
        }
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public static boolean isMainUser(Context context) {
        return UserHandle.getUserHandleForUid(context.getApplicationInfo().uid).hashCode() == 0;
    }
}