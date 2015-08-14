package com.samsung.microbit.model;

public class ConnectedDevice {

    public String mName;
    public String mPattern;
    public boolean mStatus;
    public String mAddress;
    public int mPairingCode;

    public ConnectedDevice(){}

    public ConnectedDevice(String name, String pattern, boolean status, String address , int pairingCode) {
        this.mName = name;
        this.mPattern = pattern;
        this.mStatus = status;
        this.mAddress = address;
        this.mPairingCode = pairingCode;
    }
}

