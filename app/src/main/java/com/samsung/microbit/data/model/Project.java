package com.samsung.microbit.data.model;

public class Project {

    public final String name;
    public final long timestamp;
    public final String filePath;
    public final String codeUrl;
    public final boolean runStatus;
    public boolean actionBarExpanded;
    public boolean inEditMode;

    public Project(String name, String filePath, long timestamp, String codeUrl, boolean runStatus) {
        this.name = name;
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.codeUrl = codeUrl;
        this.runStatus = runStatus;
        this.actionBarExpanded = false;
    }
}
