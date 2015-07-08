package com.samsung.microbit.model;

public class Project {

	public String name;
	public String filePath;
	public String codeUrl;
	public boolean runStatus;

	public Project(String name, String filePath, String codeUrl, boolean runStatus) {
		this.name = name;
		this.filePath = filePath;
		this.codeUrl = codeUrl;
		this.runStatus = runStatus;
	}
}
