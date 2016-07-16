package de.robv.android.xposed.installer.repo;

import android.util.Pair;

import com.afollestad.inquiry.annotations.Column;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Module {
    public final List<Pair<String, String>> moreInfo = new LinkedList<Pair<String, String>>();
    public final List<ModuleVersion> versions = new ArrayList<ModuleVersion>();
    public final List<String> screenshots = new ArrayList<String>();
    public Repository repository;
    @Column public String packageName;
    @Column public String name;
    @Column public String summary;
    public String description;
    public boolean descriptionIsHtml = false;
    public String author;
    public String support;
    public long created = -1;
    public long updated = -1;

    public Module() {}

    /* package */ Module(Repository repository) {
        this.repository = repository;
    }
}
