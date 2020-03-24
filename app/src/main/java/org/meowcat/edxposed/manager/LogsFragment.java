package org.meowcat.edxposed.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

import static org.meowcat.edxposed.manager.XposedApp.WRITE_EXTERNAL_PERMISSION;
import static org.meowcat.edxposed.manager.XposedApp.createFolder;

@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class LogsFragment extends Fragment {

    private File mFileErrorLog = new File(XposedApp.BASE_DIR + "log/all.log");
    private File mFileErrorLogOld = new File(
            XposedApp.BASE_DIR + "log/all.log.old");
    private TextView mTxtLog;
    private ScrollView mSVLog;
    private HorizontalScrollView mHSVLog;
    private MenuItem mClickedMenuItem = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_logs_modules, container, false);
        mTxtLog = v.findViewById(R.id.txtLog);
        mTxtLog.setTextIsSelectable(true);
        mSVLog = v.findViewById(R.id.svLog);
        mHSVLog = v.findViewById(R.id.hsvLog);

//        View scrollTop = v.findViewById(R.id.scroll_top);
//        View scrollDown = v.findViewById(R.id.scroll_down);
//
//        scrollTop.setOnClickListener(v1 -> scrollTop());
//        scrollDown.setOnClickListener(v12 -> scrollDown());

        if (!XposedApp.getPreferences().getBoolean("hide_logcat_warning", false)) {
            @SuppressLint("InflateParams") final View dontShowAgainView = inflater.inflate(R.layout.dialog_install_warning, null);

            TextView message = dontShowAgainView.findViewById(android.R.id.message);
            message.setText(R.string.not_logcat);

            new MaterialDialog.Builder(requireActivity())
                    .title(R.string.install_warning_title)
                    .customView(dontShowAgainView, false)
                    .positiveText(R.string.ok)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            CheckBox checkBox = dontShowAgainView.findViewById(android.R.id.checkbox);
                            if (checkBox.isChecked())
                                XposedApp.getPreferences().edit().putBoolean("hide_logcat_warning", true).apply();
                        }
                    }).cancelable(false).show();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadErrorLog();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (isMainUser(requireContext())) {
            inflater.inflate(R.menu.menu_logs, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        mClickedMenuItem = item;
        switch (item.getItemId()) {
            case R.id.menu_scroll_top:
                scrollTop();
                break;
            case R.id.menu_scroll_down:
                scrollDown();
                break;
            case R.id.menu_refresh:
                reloadErrorLog();
                return true;
            case R.id.menu_send:
                try {
                    send(requireActivity(), mFileErrorLog);
                } catch (NullPointerException ignored) {
                }
                return true;
            case R.id.menu_save:
                save(requireActivity(), "Verbose", mFileErrorLog);
                return true;
            case R.id.menu_clear:
                clear();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scrollTop() {
        mSVLog.post(() -> mSVLog.scrollTo(0, 0));
        mHSVLog.post(() -> mHSVLog.scrollTo(0, 0));
    }

    private void scrollDown() {
        mSVLog.post(() -> mSVLog.scrollTo(0, mTxtLog.getHeight()));
        mHSVLog.post(() -> mHSVLog.scrollTo(0, 0));
    }

    private void reloadErrorLog() {
        new LogsReader().execute(mFileErrorLog);
        mSVLog.post(() -> mSVLog.scrollTo(0, mTxtLog.getHeight()));
        mHSVLog.post(() -> mHSVLog.scrollTo(0, 0));
    }

    private void clear() {
        try {
            new FileOutputStream(mFileErrorLog).close();
            mFileErrorLogOld.delete();
            mTxtLog.setText(R.string.log_is_empty);
            Toast.makeText(getActivity(), R.string.logs_cleared,
                    Toast.LENGTH_SHORT).show();
            reloadErrorLog();
        } catch (IOException e) {
            Toast.makeText(getActivity(), getResources().getString(R.string.logs_clear_failed) + "n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    static void send(Activity activity, File target) {
        Uri uri = FileProvider.getUriForFile(activity, "org.meowcat.edxposed.manager.fileprovider", target);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("application/html");
        activity.startActivity(Intent.createChooser(sendIntent, activity.getResources().getString(R.string.menuSend)));
    }

    static boolean isMainUser(Context context) {
        return UserHandle.getUserHandleForUid(context.getApplicationInfo().uid).hashCode() == 0;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
        if (requestCode == WRITE_EXTERNAL_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mClickedMenuItem != null) {
                    new Handler().postDelayed(() -> onOptionsItemSelected(mClickedMenuItem), 500);
                }
            } else {
                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }
    }

    static void save(Activity activity, String type, File target) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
            return;
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(activity, R.string.sdcard_not_writable, Toast.LENGTH_LONG).show();
            return;
        }

        Calendar now = Calendar.getInstance();
        String filename = String.format(
                "EdXposed_" + type + "_%04d%02d%02d_%02d%02d%02d.txt",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), now.get(Calendar.SECOND));

        File targetFile = new File(createFolder(), filename);

        try {
            FileInputStream in = new FileInputStream(target);
            FileOutputStream out = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();

            Toast.makeText(activity, targetFile.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(activity, activity.getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LogsReader extends AsyncTask<File, Integer, String> {

        private static final int MAX_LOG_SIZE = 1000 * 1024; // 1000 KB
        private MaterialDialog mProgressDialog;

        private long skipLargeFile(BufferedReader is, long length) throws IOException {
            if (length < MAX_LOG_SIZE)
                return 0;

            long skipped = length - MAX_LOG_SIZE;
            long yetToSkip = skipped;
            do {
                yetToSkip -= is.skip(yetToSkip);
            } while (yetToSkip > 0);

            int c;
            do {
                c = is.read();
                if (c == -1)
                    break;
                skipped++;
            } while (c != '\n');

            return skipped;

        }

        @Override
        protected void onPreExecute() {
            mTxtLog.setText("");
            mProgressDialog = new MaterialDialog.Builder(requireContext()).content(R.string.loading).progress(true, 0).show();
        }

        @Override
        protected String doInBackground(File... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

            StringBuilder llog = new StringBuilder(15 * 10 * 1024);

            if (!isMainUser(requireContext())) {
                llog.append(requireContext().getResources().getString(R.string.logs_not_primary_user));
                return llog.toString();
            }

            if (XposedApp.getPreferences().getBoolean(
                    "disable_verbose_log", false)) {
                llog.append(requireContext().getResources().getString(R.string.logs_verbose_disabled));
                return llog.toString();
            }

            try {
                File logfile = log[0];
                BufferedReader br;
                br = new BufferedReader(new FileReader(logfile));
                long skipped = skipLargeFile(br, logfile.length());
                if (skipped > 0) {
                    llog.append(requireContext().getResources().getString(R.string.logs_too_long));
                    llog.append("\n-----------------\n");
                }

                char[] temp = new char[1024];
                int read;
                while ((read = br.read(temp)) > 0) {
                    llog.append(temp, 0, read);
                }
                br.close();
            } catch (IOException e) {
                llog.append(requireContext().getResources().getString(R.string.logs_cannot_read));
                llog.append(e.getMessage());
            }

            return llog.toString();
        }

        @Override
        protected void onPostExecute(String llog) {
            mProgressDialog.dismiss();
            mTxtLog.setText(llog);

            if (llog.length() == 0)
                mTxtLog.setText(R.string.log_is_empty);
        }

    }
}
