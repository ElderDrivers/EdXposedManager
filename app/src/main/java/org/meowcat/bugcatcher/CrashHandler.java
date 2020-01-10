/*
 * Copyright (c) 2013-2020 MeowCat Studio Powered by MlgmXyysd All Rights Reserved.
 */

package org.meowcat.bugcatcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.meowcat.edxposed.manager.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CrashHandler implements UncaughtExceptionHandler {
    private static final String TAG = MeowCatApplication.TAG;
    @SuppressLint("StaticFieldLeak")
    private static CrashHandler INSTANCE = new CrashHandler();
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private Context Context;
    private Map<String, String> infos = new HashMap<>();
    @SuppressLint("SimpleDateFormat")
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    void init(Context context) {
        Context = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(Context, "Sorry, an error occurred, crash stack info now saved.", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();
        collectDeviceInfo();
        saveCrashInfo2File(ex);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
        return true;
    }

    private void collectDeviceInfo() {
        infos.put("versionName", BuildConfig.VERSION_NAME);
        infos.put("versionCode", BuildConfig.VERSION_CODE + "");
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null) + "");
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "An error occurred when collect crash info", e);
            }
        }
    }

    private boolean isFolderExists(String strFolder) {
        File file = new File(strFolder);
        return file.exists() || file.mkdir();
    }

    private void saveCrashInfo2File(Throwable ex) {

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append("=").append(value).append("\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "BugCatcher-" + time + "-" + timestamp + ".log";
            String crashPath = Objects.requireNonNull(Context.getExternalFilesDir("crash")).getPath() + "/";
            if (isFolderExists(crashPath)) {
                FileOutputStream fos = new FileOutputStream(crashPath + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "An error occurred while writing crash file...", e);
        }
    }
}
