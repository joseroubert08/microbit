package com.samsung.microbit.model;

public class Project {

	private String name;
	private String codeUrl;
	private boolean runStatus;

	public Project(String name, String codeUrl, boolean runStatus) {
		this.name = name;
		this.codeUrl = codeUrl;
		this.runStatus = runStatus;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCodeUrl() {
		return codeUrl;
	}

	public void setCodeUrl(String codeUrl) {
		this.codeUrl = codeUrl;
	}

	public boolean isRunStatus() {
		return runStatus;
	}

	public void setRunStatus(boolean runStatus) {
		this.runStatus = runStatus;
	}
}
