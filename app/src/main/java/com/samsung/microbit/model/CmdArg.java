package com.samsung.microbit.model;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class CmdArg {

	private int cmd;
	private String value;

	public CmdArg(int cmd, String val) {
		this.cmd = cmd;
		this.value = val;
	}

	public int getCMD() {
		return this.cmd;
	}

	public String getValue() {
		return this.value;
	}
}

