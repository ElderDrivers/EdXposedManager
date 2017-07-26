package de.robv.android.xposed.installer.util.json;

import android.os.Build;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONUtils {

    public static final String JSON_LINK = "https://raw.githubusercontent.com/DVDAndroid/XposedInstaller/material/app/xposed_list_v2.json";

    public static String getFileContent(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setInstanceFollowRedirects(false);
        c.setDoOutput(false);
        c.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();

        return sb.toString();
    }

    private static String getLatestVersion() throws IOException {
        String site = getFileContent("http://dl-xda.xposed.info/framework/sdk" + Build.VERSION.SDK_INT + "/arm/");

        Pattern pattern = Pattern.compile("(href=\")([^\\?\"]*)\\.zip");
        Matcher matcher = pattern.matcher(site);
        String last = "";
        while (matcher.find()) {
            if (matcher.group().contains("test")) continue;
            last = matcher.group();
        }
        last = last.replace("href=\"", "");
        String[] file = last.split("-");

        return file[1].replace("v", "");
    }

    public static String listZip() {
        String latest;
        try {
            latest = getLatestVersion();
        } catch (IOException e) {
            // Got 404 response; no official Xposed zips available
            return "";
        }

        String newJson = ",\"" + Build.VERSION.SDK_INT + "\": [";
        String[] arch = new String[]{
                "arm",
                "arm64",
                "x86"
        };

        for (String a : arch) {
            newJson += installerToString(latest, a) + ",";
        }

        newJson = newJson.substring(0, newJson.length() - 1);
        newJson += "]";

        return newJson;
    }

    private static String installerToString(String latest, String architecture) {
        String filename = "xposed-v" + latest + "-sdk" + Build.VERSION.SDK_INT + "-" + architecture;

        XposedZip installer = new XposedZip();
        installer.name = filename;
        installer.version = latest;
        installer.architecture = architecture;
        installer.link = "http://dl-xda.xposed.info/framework/sdk" + Build.VERSION.SDK_INT + "/" + architecture + "/" + filename + ".zip";

        return new Gson().toJson(installer);
    }

    public class XposedJson {
        public List<XposedTab> tabs;
        public ApkRelease apk;
    }

    public class ApkRelease {
        public String version;
        public String changelog;
        public String link;
    }

}