package com.samsung.microbit.model;

import java.util.Comparator;

public class Project {

	public String name;
	public long timestamp;
	public String filePath;
	public String codeUrl;
	public boolean runStatus;
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
