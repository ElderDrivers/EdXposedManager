package de.robv.android.xposed.installer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;
import org.meowcat.edxposed.manager.BuildConfig;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.NotificationUtil;
import de.robv.android.xposed.installer.util.json.JSONUtils;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isOnline(context)) return;

                new CheckUpdates().execute();
            }
        }, 60 * 60 * 1000 /*60 min*/);
    }

    private boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private class CheckUpdates extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String jsonString = JSONUtils.getFileContent(JSONUtils.JSON_LINK).replace("%XPOSED_ZIP%", "");

                String newApkVersion = new JSONObject(jsonString).getJSONObject("apk").getString("version");

                Integer a = BuildConfig.VERSION_CODE;
                Integer b = Integer.valueOf(newApkVersion);

                if (a.compareTo(b) < 0) {
                    NotificationUtil.showInstallerUpdateNotification();
                }
            } catch (Exception e) {
                Log.d(XposedApp.TAG, e.getMessage());
            }
            return null;
        }

    }
}
