package com.samsung.microbit.data.constants;

/**
 * Contains registration id constants.
 * Basically, they use in BLEService and some plugins to identify
 * which registration should be used.
 */
public class RegistrationIds {
    private RegistrationIds() {
    }

    // Registration ID's
    public static final int REG_TELEPHONY = 0x01;    // 0x00000001;
    public static final int REG_MESSAGING = 0x02;    // 0x00000002;
    public static final int REG_DEVICEORIENTATION = 0x04;    // 0x00000004;
    public static final int REG_DEVICEGESTURE = 0x08;    // 0x00000008;
    public static final int REG_DISPLAY = 0x010;    // 0x00000010;
    public static final int REG_SIGNALSTRENGTH = 0x020;    // 0x00000020;
    public static final int REG_BATTERYSTRENGTH = 0x040;    // 0x00000040;
    public static final int REG_TEMPERATURE = 0x080;    // 0x00000080;

    public static final byte[] REGISTRATION_ON = {REG_TELEPHONY, REG_MESSAGING, REG_DEVICEORIENTATION, REG_DEVICEGESTURE, REG_DISPLAY, REG_SIGNALSTRENGTH, REG_BATTERYSTRENGTH};
    public static final byte[] REGISTRATION_OFF = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
}
