package org.meowcat.edxposed.manager;

import static org.meowcat.edxposed.manager.Constants.getActiveXposedVersion;
import static org.meowcat.edxposed.manager.Constants.getInstalledXposedVersion;

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

public class MainActivity extends Activity {

    public static Uri parseURL(String str) {
        if (str == null || str.isEmpty())
            return null;

        Spannable spannable = new SpannableString(str);
        Linkify.addLinks(spannable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        return (spans.length > 0) ? Uri.parse(spans[0].getURL()) : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String installedXposedVersion = getInstalledXposedVersion();
        String xposedStatus;
        if (installedXposedVersion != null) {
            xposedStatus = getString(R.string.status_2) + " (v" + getActiveXposedVersion() + ".0-" + installedXposedVersion + ")";
        } else {
            xposedStatus = getString(R.string.status_0);
        }
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.status_text) + ": " + xposedStatus + "\n\n" + getString(R.string.upgrade_msg))
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, parseURL("https://github.com/ElderDrivers/EdXposedManager/releases/latest"));
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
                .show();
    }
}
