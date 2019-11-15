package de.robv.android.xposed.installer.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import org.meowcat.edxposed.manager.R;

import java.util.LinkedList;
import java.util.List;

import de.robv.android.xposed.installer.WelcomeActivity;
import de.robv.android.xposed.installer.XposedApp;

public final class NotificationUtil {

    public static final int NOTIFICATION_MODULE_NOT_ACTIVATED_YET = 0;
    public static final int NOTIFICATION_MODULE_INSTALLING = 4;
    private static final int NOTIFICATION_MODULES_UPDATED = 1;
    private static final int NOTIFICATION_INSTALLER_UPDATE = 2;
    private static final int NOTIFICATION_MODULE_INSTALLATION = 3;
    private static final int PENDING_INTENT_OPEN_MODULES = 0;
    private static final int PENDING_INTENT_OPEN_INSTALL = 1;
    private static final int PENDING_INTENT_SOFT_REBOOT = 2;
    private static final int PENDING_INTENT_REBOOT = 3;
    private static final int PENDING_INTENT_ACTIVATE_MODULE_AND_REBOOT = 4;
    private static final int PENDING_INTENT_ACTIVATE_MODULE = 5;
    private static final int PENDING_INTENT_INSTALL_APK = 6;

    private static final String COLORED_NOTIFICATION = "colored_notification";
    private static final String HEADS_UP = "heads_up";
    private static final String FRAGMENT_ID = "fragment";

    private static final String NOTIFICATION_UPDATE_CHANNEL = "app_update_channel";
    private static final String NOTIFICATION_MODULES_CHANNEL = "modules_channel";

    private static Context sContext = null;
    private static NotificationManager sNotificationManager;
    private static SharedPreferences prefs;

    public static void init() {
        if (sContext != null) return;

        sContext = XposedApp.getInstance();
        prefs = XposedApp.getPreferences();
        sNotificationManager = (NotificationManager) sContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_UPDATE_CHANNEL, sContext.getString(R.string.download_section_update_available), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel channel1 = new NotificationChannel(NOTIFICATION_MODULES_CHANNEL, sContext.getString(R.string.nav_item_modules), NotificationManager.IMPORTANCE_DEFAULT);
            sNotificationManager.createNotificationChannel(channel);
            sNotificationManager.createNotificationChannel(channel1);
        }
    }

    public static void cancel(int id) {
        sNotificationManager.cancel(id);
    }

    public static void cancel(String tag, int id) {
        sNotificationManager.cancel(tag, id);
    }

    public static void cancelAll() {
        sNotificationManager.cancelAll();
    }

    public static void showNotActivatedNotification(String packageName, String appName) {
        Intent intent = new Intent(sContext, WelcomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(FRAGMENT_ID, 1);
        PendingIntent pModulesTab = PendingIntent.getActivity(sContext, PENDING_INTENT_OPEN_MODULES, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = sContext.getString(R.string.module_is_not_activated_yet);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(sContext).setContentTitle(title).setContentText(appName)
                .setTicker(title).setContentIntent(pModulesTab)
                .setVibrate(new long[]{0}).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification);

        if (prefs.getBoolean(HEADS_UP, true) && Build.VERSION.SDK_INT >= 21)
            builder.setPriority(2);

        if (prefs.getBoolean(COLORED_NOTIFICATION, false))
            builder.setColor(XposedApp.getColor(sContext));

        Intent iActivateAndReboot = new Intent(sContext, RebootReceiver.class);
        iActivateAndReboot.putExtra(RebootReceiver.EXTRA_ACTIVATE_MODULE, packageName);
        PendingIntent pActivateAndReboot = PendingIntent.getBroadcast(sContext, PENDING_INTENT_ACTIVATE_MODULE_AND_REBOOT,
                iActivateAndReboot, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent iActivate = new Intent(sContext, RebootReceiver.class);
        iActivate.putExtra(RebootReceiver.EXTRA_ACTIVATE_MODULE, packageName);
        iActivate.putExtra(RebootReceiver.EXTRA_ACTIVATE_MODULE_AND_RETURN, true);
        PendingIntent pActivate = PendingIntent.getBroadcast(sContext, PENDING_INTENT_ACTIVATE_MODULE,
                iActivate, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.BigTextStyle notiStyle = new NotificationCompat.BigTextStyle();
        notiStyle.setBigContentTitle(title);
        notiStyle.bigText(sContext.getString(R.string.module_is_not_activated_yet_detailed, appName));
        builder.setStyle(notiStyle).setChannelId(NOTIFICATION_MODULES_CHANNEL);

        // Only show the quick activation button if any module has been
        // enabled before,
        // to ensure that the user know the way to disable the module later.
        if (!ModuleUtil.getInstance().getEnabledModules().isEmpty()) {
            builder.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_menu_refresh, sContext.getString(R.string.activate_and_reboot), pActivateAndReboot).build());
            builder.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_save, sContext.getString(R.string.activate_only), pActivate).build());
        }

        sNotificationManager.notify(packageName, NOTIFICATION_MODULE_NOT_ACTIVATED_YET, builder.build());
    }

    public static void showModulesUpdatedNotification() {
        Intent intent = new Intent(sContext, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(FRAGMENT_ID, 0);

        PendingIntent pInstallTab = PendingIntent.getActivity(sContext, PENDING_INTENT_OPEN_INSTALL,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = sContext
                .getString(R.string.xposed_module_updated_notification_title);
        String message = sContext
                .getString(R.string.xposed_module_updated_notification);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(sContext).setContentTitle(title).setContentText(message)
                .setTicker(title).setContentIntent(pInstallTab)
                .setVibrate(new long[]{0}).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification);

        if (prefs.getBoolean(HEADS_UP, true) && Build.VERSION.SDK_INT >= 21)
            builder.setPriority(2);

        if (prefs.getBoolean(COLORED_NOTIFICATION, false))
            builder.setColor(XposedApp.getColor(sContext));

        Intent iSoftReboot = new Intent(sContext, RebootReceiver.class);
        iSoftReboot.putExtra(RebootReceiver.EXTRA_SOFT_REBOOT, true);
        PendingIntent pSoftReboot = PendingIntent.getBroadcast(sContext, PENDING_INTENT_SOFT_REBOOT,
                iSoftReboot, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent iReboot = new Intent(sContext, RebootReceiver.class);
        PendingIntent pReboot = PendingIntent.getBroadcast(sContext, PENDING_INTENT_REBOOT,
                iReboot, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.addAction(new NotificationCompat.Action.Builder(0, sContext.getString(R.string.reboot), pReboot).build());
        builder.addAction(new NotificationCompat.Action.Builder(0, sContext.getString(R.string.soft_reboot), pSoftReboot).build());
        builder.setChannelId(NOTIFICATION_MODULES_CHANNEL);

        sNotificationManager.notify(null, NOTIFICATION_MODULES_UPDATED, builder.build());
    }

    static void showModuleInstallNotification(@StringRes int title, @StringRes int message, String path, Object... args) {
        showModuleInstallNotification(sContext.getString(title), sContext.getString(message, args), path, title == R.string.installation_error);
    }

    private static void showModuleInstallNotification(String title, String message, String path, boolean error) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                sContext).setContentTitle(title).setContentText(message)
                .setVibrate(new long[]{0}).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification);

        if (error) {
            Intent iInstallApk = new Intent(sContext, ApkReceiver.class);
            iInstallApk.putExtra(ApkReceiver.EXTRA_APK_PATH, path);
            PendingIntent pInstallApk = PendingIntent.getBroadcast(sContext, PENDING_INTENT_INSTALL_APK, iInstallApk, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(new NotificationCompat.Action.Builder(0, sContext.getString(R.string.installation_apk_normal), pInstallApk).build());
        }

        if (prefs.getBoolean(HEADS_UP, true) && Build.VERSION.SDK_INT >= 21)
            builder.setPriority(2);

        if (prefs.getBoolean(COLORED_NOTIFICATION, false))
            builder.setColor(XposedApp.getColor(sContext));

        NotificationCompat.BigTextStyle notiStyle = new NotificationCompat.BigTextStyle();
        notiStyle.setBigContentTitle(title);
        notiStyle.bigText(message);
        builder.setStyle(notiStyle).setChannelId(NOTIFICATION_MODULES_CHANNEL);

        sNotificationManager.notify(null, NOTIFICATION_MODULE_INSTALLATION, builder.build());

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cancel(NOTIFICATION_MODULE_INSTALLATION);
            }
        }, 10 * 1000);
    }

    public static void showModuleInstallingNotification(String appName) {
        String title = sContext.getString(R.string.install_load);
        String message = sContext.getString(R.string.install_load_apk, appName);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(sContext).setContentTitle(title).setContentText(message)
                .setVibrate(new long[]{0}).setProgress(0, 0, true)
                .setSmallIcon(R.drawable.ic_notification).setOngoing(true);

        if (prefs.getBoolean(COLORED_NOTIFICATION, false))
            builder.setColor(XposedApp.getColor(sContext));

        NotificationCompat.BigTextStyle notiStyle = new NotificationCompat.BigTextStyle();
        notiStyle.setBigContentTitle(title);
        notiStyle.bigText(message);
        builder.setStyle(notiStyle).setChannelId(NOTIFICATION_MODULES_CHANNEL);

        sNotificationManager.notify(null, NOTIFICATION_MODULE_INSTALLING, builder.build());
    }

    public static void showInstallerUpdateNotification() {
        Intent intent = new Intent(sContext, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(FRAGMENT_ID, 0);

        PendingIntent pInstallTab = PendingIntent.getActivity(sContext, PENDING_INTENT_OPEN_INSTALL,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = sContext.getString(R.string.app_name);
        String message = sContext.getString(R.string.newVersion);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(sContext).setContentTitle(title).setContentText(message)
                .setTicker(title).setContentIntent(pInstallTab)
                .setVibrate(new long[]{0}).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification);

        if (prefs.getBoolean(HEADS_UP, true) && Build.VERSION.SDK_INT >= 21)
            builder.setPriority(2);

        if (prefs.getBoolean(COLORED_NOTIFICATION, false))
            builder.setColor(XposedApp.getColor(sContext));

        NotificationCompat.BigTextStyle notiStyle = new NotificationCompat.BigTextStyle();
        notiStyle.setBigContentTitle(title);
        notiStyle.bigText(message);
        builder.setStyle(notiStyle).setChannelId(NOTIFICATION_UPDATE_CHANNEL);

        sNotificationManager.notify(null, NOTIFICATION_INSTALLER_UPDATE, builder.build());
    }

    public static class RebootReceiver extends BroadcastReceiver {
        public static String EXTRA_SOFT_REBOOT = "soft";
        public static String EXTRA_ACTIVATE_MODULE = "activate_module";
        public static String EXTRA_ACTIVATE_MODULE_AND_RETURN = "activate_module_and_return";

        @Override
        public void onReceive(Context context, Intent intent) {
            /*
             * Close the notification bar in order to see the toast that module
             * was enabled successfully. Furthermore, if SU permissions haven't
             * been granted yet, the SU dialog will be prompted behind the
             * expanded notification panel and is therefore not visible to the
             * user.
             */
            sContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            cancelAll();

            if (intent.hasExtra(EXTRA_ACTIVATE_MODULE)) {
                String packageName = intent.getStringExtra(EXTRA_ACTIVATE_MODULE);
                ModuleUtil moduleUtil = ModuleUtil.getInstance();
                moduleUtil.setModuleEnabled(packageName, true);
                moduleUtil.updateModulesList(false);
                Toast.makeText(sContext, R.string.module_activated, Toast.LENGTH_SHORT).show();

                if (intent.hasExtra(EXTRA_ACTIVATE_MODULE_AND_RETURN)) return;
            }

            RootUtil rootUtil = new RootUtil();
            if (!rootUtil.startShell()) {
                Log.e(XposedApp.TAG, "NotificationUtil -> Could not start root shell");
                return;
            }

            List<String> messages = new LinkedList<>();
            boolean isSoftReboot = intent.getBooleanExtra(EXTRA_SOFT_REBOOT,
                    false);
            int returnCode = isSoftReboot
                    ? rootUtil.execute("setprop ctl.restart surfaceflinger; setprop ctl.restart zygote", messages)
                    : rootUtil.executeWithBusybox("reboot", messages);

            if (returnCode != 0) {
                Log.e(XposedApp.TAG, "NotificationUtil -> Could not reboot:");
                for (String line : messages) {
                    Log.e(XposedApp.TAG, line);
                }
            }

            rootUtil.dispose();
            AssetUtil.removeBusybox();
        }
    }

    public static class ApkReceiver extends BroadcastReceiver {
        public static final String EXTRA_APK_PATH = "path";

        @Override
        public void onReceive(Context context, Intent intent) {
            /*
             * Close the notification bar in order to see the toast that module
             * was enabled successfully. Furthermore, if SU permissions haven't
             * been granted yet, the SU dialog will be prompted behind the
             * expanded notification panel and is therefore not visible to the
             * user.
             */
            sContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

            if (intent.hasExtra(EXTRA_APK_PATH)) {
                String path = intent.getStringExtra(EXTRA_APK_PATH);
                InstallApkUtil.installApkNormally(context, path);
            }
            NotificationUtil.cancel(NotificationUtil.NOTIFICATION_MODULE_INSTALLATION);
        }
    }
}