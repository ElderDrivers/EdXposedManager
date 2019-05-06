package de.robv.android.xposed.installer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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

import com.afollestad.materialdialogs.MaterialDialog;

import org.meowcat.edxposed.manager.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import de.robv.android.xposed.installer.util.RootUtil;

import static de.robv.android.xposed.installer.XposedApp.WRITE_EXTERNAL_PERMISSION;
import static de.robv.android.xposed.installer.XposedApp.createFolder;

@SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
public class ErrorLogsFragment extends Fragment {

    private File mFileErrorLog = new File(XposedApp.BASE_DIR + "log/error.log");
    private File mFileErrorLogOld = new File(
            XposedApp.BASE_DIR + "log/error.log.old");
    private TextView mTxtLog;
    private ScrollView mSVLog;
    private HorizontalScrollView mHSVLog;
    private MenuItem mClickedMenuItem = null;
    private RootUtil mRootUtil = new RootUtil();;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_logs_error, container, false);
        mTxtLog = v.findViewById(R.id.txtLog_err);
        mTxtLog.setTextIsSelectable(true);
        mSVLog = v.findViewById(R.id.svLog_err);
        mHSVLog = v.findViewById(R.id.hsvLog_err);

//        View scrollTop = v.findViewById(R.id.scroll_top_err);
//        View scrollDown = v.findViewById(R.id.scroll_down_err);
//
//        scrollTop.setOnClickListener(v12 -> scrollTop());
//        scrollDown.setOnClickListener(v1 -> scrollDown());

        if (!XposedApp.getPreferences().getBoolean("hide_logcat_warning", false)) {
            @SuppressLint("InflateParams") final View dontShowAgainView = inflater.inflate(R.layout.dialog_install_warning, null);

            TextView message = dontShowAgainView.findViewById(android.R.id.message);
            message.setText(R.string.not_logcat);

            new MaterialDialog.Builder(Objects.requireNonNull(getActivity()))
                    .title(R.string.install_warning_title)
                    .customView(dontShowAgainView, false)
                    .positiveText(android.R.string.ok)
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
        inflater.inflate(R.menu.menu_logs, menu);
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
                    send();
                } catch (NullPointerException ignored) {
                }
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

    private void scrollTop() {
        mSVLog.post(() -> mSVLog.scrollTo(0, 0));
        mHSVLog.post(() -> mHSVLog.scrollTo(0, 0));
    }

    private void scrollDown() {
        mSVLog.post(() -> mSVLog.scrollTo(0, mTxtLog.getHeight()));
        mHSVLog.post(() -> mHSVLog.scrollTo(0, 0));
    }

    private void enableLogAccess() {
        if (mRootUtil.startShell()) {
            mRootUtil.execute("chmod 777 " + mFileErrorLog.getAbsolutePath());
        } else {
            try {
                Runtime.getRuntime().exec("chmod 777 " + mFileErrorLog.getAbsolutePath()).waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void reloadErrorLog() {
        enableLogAccess();
        new LogsReader().execute(mFileErrorLog);
        mSVLog.post(() -> mSVLog.scrollTo(0, mTxtLog.getHeight()));
        mHSVLog.post(() -> mHSVLog.scrollTo(0, 0));
    }

    private void clear() {
        try {
            enableLogAccess();
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

    private void send() {
        enableLogAccess();
        Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(getActivity()), "org.meowcat.edxposed.manager.fileprovider", mFileErrorLog);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("application/html");
        startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.menuSend)));
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

    @SuppressLint("DefaultLocale")
    private void save() {
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
            return;
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getActivity(), R.string.sdcard_not_writable, Toast.LENGTH_LONG).show();
            return;
        }

        Calendar now = Calendar.getInstance();
        String filename = String.format(
                "edxposed_module_%s_%04d%02d%02d_%02d%02d%02d.log", "error",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), now.get(Calendar.SECOND));

        File targetFile = new File(createFolder(), filename);

        try {
            enableLogAccess();
            FileInputStream in = new FileInputStream(mFileErrorLog);
            FileOutputStream out = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();

            Toast.makeText(getActivity(), targetFile.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getActivity(), getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Toast.LENGTH_LONG).show();
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
            mProgressDialog = new MaterialDialog.Builder(Objects.requireNonNull(getContext())).content(R.string.loading).progress(true, 0).show();
        }

        @Override
        protected String doInBackground(File... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

            StringBuilder llog = new StringBuilder(15 * 10 * 1024);
            try {
                File logfile = log[0];
                BufferedReader br;
                br = new BufferedReader(new FileReader(logfile));
                long skipped = skipLargeFile(br, logfile.length());
                if (skipped > 0) {
                    llog.append("-----------------\n");
                    llog.append("Log too long");
                    llog.append("\n-----------------\n\n");
                }

                char[] temp = new char[1024];
                int read;
                while ((read = br.read(temp)) > 0) {
                    llog.append(temp, 0, read);
                }
                br.close();
            } catch (IOException e) {
                llog.append("Cannot read log");
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
