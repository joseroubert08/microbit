package com.samsung.microbit.model;

import java.util.UUID;

public class Constants {

	public static final int REMOTE_CONTROL_PLAY = 0;
	public static final int REMOTE_CONTROL_PAUSE = 1;
	public static final int REMOTE_CONTROL_STOP = 2;
	public static final int REMOTE_CONTROL_NEXTTRACK = 3;
	public static final int REMOTE_CONTROL_PREVTRACK = 4;
	public static final int REMOTE_CONTROL_FORWARD = 5;
	public static final int REMOTE_CONTROL_REWIND = 6;
	public static final int REMOTE_CONTROL_VOLUMEUP = 7;
	public static final int REMOTE_CONTROL_VOLUMEDOWN = 8;

	public static final int CAMERA_LAUNCH_PHOTO_MODE = 0;
	public static final int CAMERA_LAUNCH_VIDEO_MODE = 1;
	public static final int CAMERA_TAKE_PHOTO = 2;
	public static final int CAMERA_START_VIDEO_CAPTURE = 3;
	public static final int CAMERA_STOP_VIDEO_CAPTURE = 4;

	public static final int AUDIO_RECORDER_LAUNCH = 0;
	public static final int AUDIO_RECORDER_START_CAPTURE = 1;
	public static final int AUDIO_RECORDER_STOP_CAPTURE = 2;

	public static final int ALERT_DISPLAY_TOAST = 0;
	public static final int ALERT_VIBRATE = 1;
	public static final int ALERT_PLAY_SOUND = 2;
	public static final int ALERT_PLAY_RINGTONE = 3;
	public static final int FIND_MY_PHONE = 4;

	public static final int ALARM_SOUND = 0;
	public static final int SOUND_TBD_1 = 1;
	public static final int SOUND_TBD_2 = 2;
	public static final int SOUND_TBD_3 = 3;
	public static final int SOUND_TBD_4 = 4;
	public static final int SOUND_TBD_5 = 5;

	public static final int BUTTON_A_UP = 0;
	public static final int BUTTON_A_DOWN = 1;
	public static final int BUTTON_B_UP = 2;
	public static final int BUTTON_B_DOWN = 3;
	public static final int BUTTON_A_LONG_PRESS = 4;
	public static final int BUTTON_B_LONG_PRESS = 5;
	public static final int BOTH_BUTTONS_PRESSED = 6;

	public static final int SIGNAL_STRENGTH_NO_BAR = 0;
	public static final int SIGNAL_STRENGTH_ONE_BAR = 1;
	public static final int SIGNAL_STRENGTH_TWO_BAR = 2;
	public static final int SIGNAL_STRENGTH_THREE_BAR = 3;
	public static final int SIGNAL_STRENGTH_FOUR_BAR = 4;

	public static final int DEVICE_ORIENTATION_LANDSCAPE = 0;
	public static final int DEVICE_ORIENTATION_PORTRAIT = 1;

	public static final int DEVICE_GESTURE_NONE = 0;
	public static final int DEVICE_GESTURE_DEVICE_SHAKEN = 1;

	public static final int DEVICE_DISPLAY_OFF = 0;
	public static final int DEVICE_DISPLAY_ON = 1;

	public static final int BUTTON_UP = 0;
	public static final int BUTTON_DOWN = 1;
	public static final int BUTTON_RIGHT = 2;
	public static final int BUTTON_LEFT = 3;
	public static final int BUTTON_A = 4;
	public static final int BUTTON_B = 5;

	// Registration ID's
	public static final int REG_TELEPHONY = 0x01;    // 0x00000001;
	public static final int REG_MESSAGING = 0x02;    // 0x00000002;
	public static final int REG_DEVICEORIENTATION = 0x04;    // 0x00000004;
	public static final int REG_DEVICEGESTURE = 0x08;    // 0x00000008;
	public static final int REG_DISPLAY = 0x010;    // 0x00000010;
	public static final int REG_SIGNALSTRENGTH = 0x020;    // 0x00000020;
	public static final int REG_BATTERYSTRENGTH = 0x040;    // 0x00000040;
	public static final int REG_TEMPERATURE = 0x080;    // 0x00000080;

	/*
	 * BLE UUID's for Service and Characteristic
	 */
	public static final String BASE_UUID_STR = "00000000-0000-1000-8000-00805f9b34fb";
	public static final String MICROBIT_BASE_UUID_STR = "e95d5be9-251d-470a-a062-fa1922dfa9a8";

	public static final UUID BASE_UUID = UUID.fromString(BASE_UUID_STR);
	public static final UUID MICROBIT_BASE_UUID = UUID.fromString(MICROBIT_BASE_UUID_STR);

	// This descriptor is used for requesting notification
	public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR = makeUUID(BASE_UUID_STR, 0x02902);

	public static final UUID SERVICE_GENERIC_ACCESS = makeUUID(BASE_UUID_STR, 0x01800);
	public static final UUID SGA_DEVICE_NAME = makeUUID(BASE_UUID_STR, 0x02a00);
	public static final UUID SGA_APPEARANCE = makeUUID(BASE_UUID_STR, 0x02a01);
	public static final UUID SGA_PPRIVACY_FLAG = makeUUID(BASE_UUID_STR, 0x02a02);
	public static final UUID SGA_RECONNECTION_ADDRESS = makeUUID(BASE_UUID_STR, 0x02a03);
	public static final UUID SGA_PPCONNECTION_PARAMETERS = makeUUID(BASE_UUID_STR, 0x02a04);

	public static final UUID SERVICE_DEVICE_INFORMATION = makeUUID(BASE_UUID_STR, 0x0180a);
	public static final UUID SDI_SYSTEM_ID = makeUUID(BASE_UUID_STR, 0x02a23);
	public static final UUID SDI_MODEL_NUMBER_STRING = makeUUID(BASE_UUID_STR, 0x02a24);
	public static final UUID SDI_SERIAL_NUMBER_STRING = makeUUID(BASE_UUID_STR, 0x02a25);
	public static final UUID SDI_FIRMWARE_REVISION_STRING = makeUUID(BASE_UUID_STR, 0x02a26);
	public static final UUID SDI_HARDWARE_REVISION_STRING = makeUUID(BASE_UUID_STR, 0x02a27);
	public static final UUID SDI_SOFTWARE_REVISION_STRING = makeUUID(BASE_UUID_STR, 0x02a28);
	public static final UUID SDI_REGULATORY_CERTIFICATION_DATA_LIST = makeUUID(BASE_UUID_STR, 0x02a2a);
	public static final UUID SDI_PNP_ID = makeUUID(BASE_UUID_STR, 0x02a50);

	public static final UUID SERVICE_BATTERY_SERVICE = makeUUID(BASE_UUID_STR, 0x0180f);
	public static final UUID SBS_BATTERY_LEVEL = makeUUID(BASE_UUID_STR, 0x02a19);

	/*
	 * Microbit specific UUID's
	 */
	public static final UUID ACCELEROMETER_SERVICE = makeUUID(MICROBIT_BASE_UUID_STR, 0x00753);
	public static final UUID AS_ACCELEROMETER_DATA = makeUUID(MICROBIT_BASE_UUID_STR, 0x0ca4b);
	public static final UUID AS_ACCELEROMETER_PERIOD = makeUUID(MICROBIT_BASE_UUID_STR, 0x0fb24);

	public static final UUID MAGNETOMETER_SERVICE = makeUUID(MICROBIT_BASE_UUID_STR, 0x0f2d8);
	public static final UUID MS_MAGNETOMETER_DATA = makeUUID(MICROBIT_BASE_UUID_STR, 0x0fb11);
	public static final UUID MS_MAGNETOMETER_PERIOD = makeUUID(MICROBIT_BASE_UUID_STR, 0x0386c);

	public static final UUID BUTTON_SERVICE = makeUUID(MICROBIT_BASE_UUID_STR, 0x09882);
	public static final UUID BS_BUTTON_STATE = makeUUID(MICROBIT_BASE_UUID_STR, 0x0da90);

	public static final UUID LED_SERVICE = makeUUID(MICROBIT_BASE_UUID_STR, 0x0d91d);
	public static final UUID LS_LED_MATRIX_STATE = makeUUID(MICROBIT_BASE_UUID_STR, 0x07b77);
	public static final UUID LS_SYSTEM_LED_STATE = makeUUID(MICROBIT_BASE_UUID_STR, 0x0b744);
	public static final UUID LS_LED_TEXT = makeUUID(MICROBIT_BASE_UUID_STR, 0x093ee);
	public static final UUID LS_SCROLLING_STATE = makeUUID(MICROBIT_BASE_UUID_STR, 0X08136);
	public static final UUID LS_SCROLLING_SPEED = makeUUID(MICROBIT_BASE_UUID_STR, 0x00d2d);

	public static final UUID IO_PIN_SERVICE = makeUUID(MICROBIT_BASE_UUID_STR, 0x0127b);
	public static final UUID IOPS_PIN_0 = makeUUID(MICROBIT_BASE_UUID_STR, 0x08d00);
	public static final UUID IOPS_PIN_1 = makeUUID(MICROBIT_BASE_UUID_STR, 0x0c58c);
	public static final UUID IOPS_PIN_2 = makeUUID(MICROBIT_BASE_UUID_STR, 0x004f4);
	public static final UUID IOPS_PIN_3 = makeUUID(MICROBIT_BASE_UUID_STR, 0x0bf30);
	public static final UUID IOPS_PIN_4 = makeUUID(MICROBIT_BASE_UUID_STR, 0x0e5c1);
	public static final UUID IOPS_PIN_5 = makeUUID(MICROBIT_BASE_UUID_STR, 0X05281);
	public static final UUID IOPS_PIN_6 = makeUUID(MICROBIT_BASE_UUID_STR, 0x02c44);
	public static final UUID IOPS_PIN_7 = makeUUID(MICROBIT_BASE_UUID_STR, 0x0d205);
	public static final UUID IOPS_PIN_8 = makeUUID(MICROBIT_BASE_UUID_STR, 0x055ff);
	public static final UUID IOPS_PIN_9 = makeUUID(MICROBIT_BASE_UUID_STR, 0X00906);
	public static final UUID IOPS_PIN_10 = makeUUID(MICROBIT_BASE_UUID_STR, 0x020be);
	public static final UUID IOPS_PIN_11 = makeUUID(MICROBIT_BASE_UUID_STR, 0x0e36e);
	public static final UUID IOPS_PIN_12 = makeUUID(MICROBIT_BASE_UUID_STR, 0x02c29);
	public static final UUID IOPS_PIN_13 = makeUUID(MICROBIT_BASE_UUID_STR, 0x0b67a);
	public static final UUID IOPS_PIN_14 = makeUUID(MICROBIT_BASE_UUID_STR, 0x0c2fe);
	public static final UUID IOPS_PIN_15 = makeUUID(MICROBIT_BASE_UUID_STR, 0x074b4);
	public static final UUID IOPS_PIN_16 = makeUUID(MICROBIT_BASE_UUID_STR, 0x0ab2c);
	public static final UUID IOPS_PIN_17 = makeUUID(MICROBIT_BASE_UUID_STR, 0x0100a);
	public static final UUID IOPS_PIN_CONFIGURATION = makeUUID(MICROBIT_BASE_UUID_STR, 0x05899);
	public static final UUID IOPS_PARALLEL_PORT = makeUUID(MICROBIT_BASE_UUID_STR, 0x060cf);

	public static final UUID EVENT_SERVICE = makeUUID(MICROBIT_BASE_UUID_STR, 0x093af);
	public static final UUID ES_MICROBIT_REQUIREMENTS = makeUUID(MICROBIT_BASE_UUID_STR, 0x03ad2);
	public static final UUID ES_CLIENT_EVENT = makeUUID(MICROBIT_BASE_UUID_STR, 0x09775);
	public static final UUID ES_MICROBIT_EVENT = makeUUID(MICROBIT_BASE_UUID_STR, 0x05404);
	public static final UUID ES_CLIENT_REQUIREMENTS = makeUUID(MICROBIT_BASE_UUID_STR, 0x023c4);

	public static UUID makeUUID(String baseUUID, long shortUUID) {

		UUID u = UUID.fromString(baseUUID);
		long msb = u.getMostSignificantBits();
		long mask = 0x0ffffL;
		shortUUID &= mask;
		msb &= ~(mask << 32);
		msb |= (shortUUID << 32);
		u = new UUID(msb, u.getLeastSignificantBits());
		return u;
	}

	public static String URL = "URL";
}
