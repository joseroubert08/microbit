package com.samsung.microbit.model;

/**
 * Created by Kupes on 07/03/2015.
 */
public class ConnectedDevice {

    private String mName;
    private String mPattern;
    private boolean mStatus;

    public ConnectedDevice(){}

    public ConnectedDevice(String name, String pattern, boolean status) {
        this.mName = name;
        this.mPattern = pattern;
        this.mStatus = status;
    }

    //getters & setters

    public void setName(String name) {
        this.mName = name;
    }

    public void setPattern(String pattern) {
        this.mPattern = pattern;
    }

    public void setStatus(boolean status) { this.mStatus = status; }

    public String getName() {
        return this.mName;
    }

    public String getPattern() {
        return this.mPattern;
    }

    public boolean getStatus() {
        return this.mStatus;
    }
}
