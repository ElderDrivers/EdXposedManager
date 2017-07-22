package de.robv.android.xposed.installer.util.json;


import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.List;

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

    public String name;
    public List<Integer> sdks;
    public String author;
    public boolean stable;

    public HashMap<String, String> compatibility;
    public HashMap<String, String> incompatibility;
    public HashMap<String, String> support;
    public HashMap<String, List<XposedZip>> installers;
    public List<XposedZip> uninstallers;

    public XposedTab() { }

    protected XposedTab(Parcel in) {
        name = in.readString();
        author = in.readString();
        stable = in.readByte() != 0;
    }

    public String getCompatibility() {
        return compatibility.get(Integer.toString(Build.VERSION.SDK_INT));
    }

    public String getIncompatibility() {
        return incompatibility.get(Integer.toString(Build.VERSION.SDK_INT));
    }

    public String getSupport() {
        return support.get(Integer.toString(Build.VERSION.SDK_INT));
    }

    public List<XposedZip> getInstallers() {
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
        dest.writeByte((byte) (stable ? 1 : 0));
    }
}