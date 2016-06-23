package com.samsung.microbit.model;

public class ConnectedDevice {

    public String mName;
    public String mPattern;
    public boolean mStatus;
    public String mAddress;
    public int mPairingCode;
    public String mfirmware_version;
    public long mlast_connection_time;

    public ConnectedDevice() {
    }

    public ConnectedDevice(String name, String pattern, boolean status, String address, int pairingCode, String firmware, long connectionTime) {
        this.mName = name;
        this.mPattern = pattern;
        this.mStatus = status;
        this.mAddress = address;
        this.mPairingCode = pairingCode;
        this.mfirmware_version = firmware;
        this.mlast_connection_time = connectionTime;
    }
}