package com.samsung.microbit.data.constants;

public class IPCConstants {
    public static final int MESSAGE_ANDROID = 1;
    public static final int MESSAGE_MICROBIT = 2;

    public static final String BROADCAST_ANDROID_NOTIFICATION = "com.samsung.microbit.service.IPCService" +
            ".INTENT_BLE_NOTIFICATION";
    public static final String BROADCAST_MICROBIT_NOTIFICATION = "com.samsung.microbit.service.IPCService" +
            ".INTENT_MICROBIT_NOTIFICATION";

    public static final String BUNDLE_DATA = "data";
    public static final String BUNDLE_VALUE = "value";
    public static final String BUNDLE_MICROBIT_FIRMWARE = "BUNDLE_MICROBIT_FIRMWARE";
    public static final String BUNDLE_MICROBIT_REQUESTS = "BUNDLE_MICROBIT_REQUESTS";
    public static final String BUNDLE_ERROR_CODE = "BUNDLE_ERROR_CODE";
    public static final String BUNDLE_ERROR_MESSAGE = "BUNDLE_ERROR_MESSAGE";
    public static final String BUNDLE_SERVICE_GUID = "BUNDLE_SERVICE_GUID";
    public static final String BUNDLE_CHARACTERISTIC_GUID = "BUNDLE_CHARACTERISTIC_GUID";
    public static final String BUNDLE_CHARACTERISTIC_TYPE = "BUNDLE_CHARACTERISTIC_TYPE";
    public static final String BUNDLE_CHARACTERISTIC_VALUE = "BUNDLE_CHARACTERISTIC_VALUE";
    public static final String BUNDLE_DEVICE_ADDRESS = "BUNDLE_DEVICE_ADDRESS";

    public static final String INTENT_BLE_NOTIFICATION = "com.samsung.microbit.service.IPCService.INTENT_BLE_NOTIFICATION";
    public static final String INTENT_MICROBIT_NOTIFICATION = "com.samsung.microbit.service.IPCService.INTENT_MICROBIT_NOTIFICATION";

    public static final String NOTIFICATION_CAUSE = "com.samsung.microbit.service.IPCService.CAUSE";

    public static final int JUST_PAIRED = 1;
    public static final int PAIRED_EARLIER = 0;


}
