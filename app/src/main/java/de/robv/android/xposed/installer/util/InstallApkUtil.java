package de.robv.android.xposed.installer.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import androidx.core.content.FileProvider;

import org.meowcat.edxposed.manager.R;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import de.robv.android.xposed.installer.XposedApp;

public class InstallApkUtil extends AsyncTask<Void, Void, Integer> {

    private static final int ERROR_ROOT_NOT_GRANTED = -99;

    private final DownloadsUtil.DownloadInfo info;
    @SuppressLint("StaticFieldLeak")
    private final Context context;
    private RootUtil mRootUtil;
    private boolean isApkRootInstallOn;
    private List<String> output = new LinkedList<>();

    public InstallApkUtil(Context context, DownloadsUtil.DownloadInfo info) {
        this.context = context;
        this.info = info;

        mRootUtil = new RootUtil();
    }

    static void installApkNormally(Context context, String localFilename) {
        Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(context, "org.meowcat.edxposed.manager.fileprovider", new File(localFilename));
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(new File(localFilename));
        }
        installIntent.setDataAndType(uri, DownloadsUtil.MIME_TYPE_APK);
        installIntent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.getApplicationInfo().packageName);
        context.startActivity(installIntent);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        SharedPreferences prefs = XposedApp.getPreferences();
        isApkRootInstallOn = prefs.getBoolean("install_with_su", false);

        if (isApkRootInstallOn) {
            NotificationUtil.showModuleInstallingNotification(info.title);
            mRootUtil.startShell();
        }
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int returnCode = 0;
        if (isApkRootInstallOn) {
            try {
                String path = "/data/local/tmp/";
                String fileName = new File(info.localFilename).getName();
                mRootUtil.execute("cat \"" + info.localFilename + "\">" + path + fileName, output);
                returnCode = mRootUtil.execute("pm install -r -f \"" + path + fileName + "\"", output);
            } catch (IllegalStateException e) {
                returnCode = ERROR_ROOT_NOT_GRANTED;
            }
        }
        return returnCode;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        if (isApkRootInstallOn) {
            NotificationUtil.cancel(NotificationUtil.NOTIFICATION_MODULE_INSTALLING);

            if (result.equals(ERROR_ROOT_NOT_GRANTED)) {
                NotificationUtil.showModuleInstallNotification(R.string.installation_error, R.string.root_failed, info.localFilename);
                return;
            }

            StringBuilder out = new StringBuilder();
            for (String o : output) {
                out.append(o);
                out.append("\n");
            }
//            Pattern failurePattern = Pattern.compile("(?m)^Failure\\s+\\[(.*?)]$");
//            Matcher failureMatcher = failurePattern.matcher(out);

            if (result.equals(0)) {
                NotificationUtil.showModuleInstallNotification(R.string.installation_successful, R.string.installation_successful_message, info.localFilename, info.title);
            } else {
                NotificationUtil.showModuleInstallNotification(R.string.installation_error, R.string.installation_error_message, info.localFilename, info.title, out);
                installApkNormally(context, info.localFilename);
            }
        } else {
            installApkNormally(context, info.localFilename);
        }
    }
}
