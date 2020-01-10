package org.meowcat.edxposed.manager.util;

import android.content.res.AssetManager;
import android.os.Build;
import android.os.FileUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.meowcat.edxposed.manager.XposedApp;

public class AssetUtil {
    static final File BUSYBOX_FILE = new File(XposedApp.getInstance().getCacheDir(), "busybox-xposed");

    private static String getBinariesFolder() {
        if (Build.CPU_ABI.startsWith("arm")) {
            return "arm/";
        } else if (Build.CPU_ABI.startsWith("x86")) {
            return "x86/";
        } else {
            return null;
        }
    }

//    public static File writeAssetToCacheFile(String name, int mode) {
//        return writeAssetToCacheFile(name, name, mode);
//    }
//
//    private static File writeAssetToCacheFile(String assetName, String fileName, int mode) {
//        return writeAssetToFile(assetName, new File(XposedApp.getInstance().getCacheDir(), fileName), mode);
//    }
//
//    public static File writeAssetToSdcardFile(String name, int mode) {
//        return writeAssetToSdcardFile(name, name, mode);
//    }
//
//    private static File writeAssetToSdcardFile(String assetName, String fileName, int mode) {
//        File dir = XposedApp.getInstance().getExternalFilesDir(null);
//        return writeAssetToFile(assetName, new File(dir, fileName), mode);
//    }
//
//    private static File writeAssetToFile(String assetName, File targetFile, int mode) {
//        return writeAssetToFile(null, assetName, targetFile, mode);
//    }

    @SuppressWarnings("UnusedReturnValue")
    private static File writeAssetToFile(@SuppressWarnings("SameParameterValue") AssetManager assets, String assetName, @SuppressWarnings("SameParameterValue") File targetFile, @SuppressWarnings("SameParameterValue") int mode) {
        try {
            if (assets == null)
                assets = XposedApp.getInstance().getAssets();
            InputStream in = assets.open(assetName);
            writeStreamToFile(in, targetFile, mode);
            return targetFile;
        } catch (IOException e) {
            Log.e(XposedApp.TAG, "AssetUtil -> could not extract asset", e);
            if (targetFile != null)
                //noinspection ResultOfMethodCallIgnored
                targetFile.delete();

            return null;
        }
    }

    private static void writeStreamToFile(InputStream in, File targetFile, int mode) throws IOException {
        FileOutputStream out = new FileOutputStream(targetFile);

        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();

        FileUtils.setPermissions(targetFile.getAbsolutePath(), mode, -1, -1);
    }

    synchronized static void extractBusybox() {
        if (BUSYBOX_FILE.exists())
            return;

        //noinspection OctalInteger
        writeAssetToFile(null, getBinariesFolder() + "busybox-xposed", BUSYBOX_FILE, 00700);
    }

    public synchronized static void removeBusybox() {
        //noinspection ResultOfMethodCallIgnored
        BUSYBOX_FILE.delete();
    }
}
