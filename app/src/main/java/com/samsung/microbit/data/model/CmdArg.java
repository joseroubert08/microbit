package com.samsung.microbit.data.model;

/**
 * Represents class, that consists of int command code
 * and a String value.
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
