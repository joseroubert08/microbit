package com.samsung.microbit.data.model.ui;

public class BaseActivityState {
    protected BaseActivityState() {
    }

    public static final int STATE_IDLE = 1;
    public static final int STATE_ENABLE_BT_FOR_CONNECT = 2;
    public static final int STATE_CONNECTING = 3;
    public static final int STATE_DISCONNECTING = 4;
}
