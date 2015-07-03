package com.samsung.microbit.model;

/**
 * Created by Kupes on 07/03/2015.
 */
public class ConnectedDevice {

    public String mName;
    public String mPattern;
    public boolean mStatus;
    public String mAddress;

    public ConnectedDevice(){}

    public ConnectedDevice(String name, String pattern, boolean status, String address) {
        this.mName = name;
        this.mPattern = pattern;
        this.mStatus = status;
        this.mAddress = address;
    }

}
    /*
    //getters & setters

    public void setName(String name) {
        this.mName = name;
    }

    public void setPattern(String pattern) {
        this.mPattern = pattern;
    }

    public void setStatus(boolean status) { this.mStatus = status; }

    public void setAddress(String address) { this.mAddress = address; }

    public String getName() {
        return this.mName;
    }

    public String getPattern() {
        return this.mPattern;
    }

    public boolean getStatus() {
        return this.mStatus;
    }
    public String getAddress() {
        return this.mAddress;
    }
} */
