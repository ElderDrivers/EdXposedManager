package de.robv.android.xposed.installer.util.json;


import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class XposedTab implements Parcelable {

    public static final Creator<XposedTab> CREATOR = new Creator<XposedTab>() {
        @Override
        public XposedTab createFromParcel(Parcel in) {
            return new XposedTab(in);
        }

        @Override
        public XposedTab[] newArray(int size) {
            return new XposedTab[size];
        }
    };

    public List<Integer> sdks = new ArrayList<>();
    public String name = "None";
    public String author = "None";
    public String description = "v0<br />None";
    public boolean stable = true;
    public boolean official = true;

    private HashMap<String, String> compatibility = new HashMap<>();
    private HashMap<String, String> incompatibility = new HashMap<>();
    public HashMap<String, String> support = new HashMap<>();
    private HashMap<String, List<XposedZip>> installers = new HashMap<>();
    public List<XposedZip> uninstallers = new ArrayList<>();

    public XposedTab() { }

    private XposedTab(Parcel in) {
        name = in.readString();
        author = in.readString();
        description = in.readString();
        stable = in.readByte() != 0;
        official = in.readByte() != 0;
    }

    public String getCompatibility() {
        if (compatibility == null) return "";
        return compatibility.get(Integer.toString(Build.VERSION.SDK_INT));
    }

    public String getIncompatibility() {
        if (incompatibility == null) return "";
        return incompatibility.get(Integer.toString(Build.VERSION.SDK_INT));
    }

    public String getSupport() {
        if (support == null) return "";
        return support.get(Integer.toString(Build.VERSION.SDK_INT));
    }

    public List<XposedZip> getInstallers() {
        if (support == null) return new ArrayList<>();
        return installers.get(Integer.toString(Build.VERSION.SDK_INT));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(author);
        dest.writeString(description);
        dest.writeByte((byte) (stable ? 1 : 0));
        dest.writeByte((byte) (official ? 1 : 0));
    }
}