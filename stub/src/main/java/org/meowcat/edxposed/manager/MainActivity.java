package org.meowcat.edxposed.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import installer.XposedApp;
import installer.util.InstallZipUtil;

public class MainActivity extends Activity {

    private static int getXposedStatus(String installedXposedVersion) {
        if (installedXposedVersion != null) {
            int installedXposedVersionInt = InstallZipUtil.extractIntPart(installedXposedVersion);
            if (installedXposedVersionInt == XposedApp.getActiveXposedVersion()) {
                return 2;
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String installedXposedVersion;
        try {
            installedXposedVersion = XposedApp.getInstance().mXposedProp.getVersion();
        } catch (NullPointerException e) {
            installedXposedVersion = null;
        }
        String xposedStatus;
        switch (getXposedStatus(installedXposedVersion)) {
            case 2:
                xposedStatus = getString(R.string.status_2) + " (" + installedXposedVersion + ")";
                break;
            case 1:
                xposedStatus = getString(R.string.status_1) + " (" + installedXposedVersion + ")";
                break;
            case 0:
            default:
                xposedStatus = getString(R.string.status_0);
        }
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.status_text) + ": " + xposedStatus + "\n\n" + getString(R.string.upgrade_msg))
                .setPositiveButton(R.string.btn_ok, (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, parseURL("https://github.com/ElderDrivers/EdXposedManager/releases/latest"));
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.btn_no, (d, w) -> finish())
                .show();
    }

    public static Uri parseURL(String str) {
        if (str == null || str.isEmpty())
            return null;

        Spannable spannable = new SpannableString(str);
        Linkify.addLinks(spannable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        return (spans.length > 0) ? Uri.parse(spans[0].getURL()) : null;
    }
}
