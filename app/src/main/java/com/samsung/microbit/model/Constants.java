package com.samsung.microbit.model;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Environment;

import java.io.File;
import java.util.UUID;

public class Constants {

	/*
	 * Going to and coming from microbit the following rul eapplies:
	 * high 16 bits ==  event_sub_code (eg. SAMSUNG_REMOTE_CONTROL_EVT_PLAY)
	 * low 16 bits == event category    (eg. SAMSUNG_REMOTE_CONTROL_ID)
	 */

	public static final int FORMAT_UINT8 = BluetoothGattCharacteristic.FORMAT_UINT8;
	public static final int FORMAT_UINT16 = BluetoothGattCharacteristic.FORMAT_UINT16;
	public static final int FORMAT_UINT32 = BluetoothGattCharacteristic.FORMAT_UINT32;

	public static final int FORMAT_SINT8 = BluetoothGattCharacteristic.FORMAT_SINT8;
	public static final int FORMAT_SINT16 = BluetoothGattCharacteristic.FORMAT_SINT16;
	public static final int FORMAT_SINT32 = BluetoothGattCharacteristic.FORMAT_SINT32;


	/*
	 * Events that Samsung devices respond to:
	 */
	public static final int SAMSUNG_REMOTE_CONTROL_ID = 1001; //0x03E9
	public static final int SAMSUNG_REMOTE_CONTROL_EVT_PLAY = 0;
	public static final int SAMSUNG_REMOTE_CONTROL_EVT_PAUSE = 1;
	public static final int SAMSUNG_REMOTE_CONTROL_EVT_STOP = 2;
	public static final int SAMSUNG_REMOTE_CONTROL_EVT_NEXTTRACK = 3;
	public static final int SAMSUNG_REMOTE_CONTROL_EVT_PREVTRACK = 4;
	public static final int SAMSUNG_REMOTE_CONTROL_EVT_FORWARD = 5;
	public static final int SAMSUNG_REMOTE_CONTROL_EVT_REWIND = 6;
	public static final int SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEUP = 7;
	public static final int SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEDOWN = 8;

	public static final int SAMSUNG_CAMERA_ID = 1002;//0x03EA
	public static final int SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE = 0;
	public static final int SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE = 1;
	public static final int SAMSUNG_CAMERA_EVT_TAKE_PHOTO = 2;
	public static final int SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE = 3;
	public static final int SAMSUNG_CAMERA_EVT_STOP_VIDEO_CAPTURE = 4;
	public static final int SAMSUNG_CAMERA_EVT_STOP_PHOTO_MODE = 5;
	public static final int SAMSUNG_CAMERA_EVT_STOP_VIDEO_MODE = 6;
	public static final int SAMSUNG_CAMERA_EVT_TOGGLE_FRONT_REAR = 7;

	public static final int SAMSUNG_AUDIO_RECORDER_ID = 1003; //0x03EB
	public static final int SAMSUNG_AUDIO_RECORDER_EVT_LAUNCH = 0;
	public static final int SAMSUNG_AUDIO_RECORDER_EVT_START_CAPTURE = 1;
	public static final int SAMSUNG_AUDIO_RECORDER_EVT_STOP_CAPTURE = 2;
	public static final int SAMSUNG_AUDIO_RECORDER_EVT_STOP = 3;

	public static final int SAMSUNG_ALERTS_ID = 1004; //0x03EC
	public static final int SAMSUNG_ALERT_EVT_DISPLAY_TOAST = 0;
	public static final int SAMSUNG_ALERT_EVT_VIBRATE = 1;
	public static final int SAMSUNG_ALERT_EVT_PLAY_SOUND = 2;
	public static final int SAMSUNG_ALERT_EVT_PLAY_RINGTONE = 3;
	public static final int SAMSUNG_ALERT_EVT_FIND_MY_PHONE = 4;
	public static final int SAMSUNG_ALERT_EVT_ALARM1 = 5;
	public static final int SAMSUNG_ALERT_EVT_ALARM2 = 6;
	public static final int SAMSUNG_ALERT_EVT_ALARM3 = 7;
	public static final int SAMSUNG_ALERT_EVT_ALARM4 = 8;
	public static final int SAMSUNG_ALERT_EVT_ALARM5 = 9;
	public static final int SAMSUNG_ALERT_EVT_ALARM6 = 10;

	/*
	 * Events that Samsung devices generate:
	 */

	public static final int SAMSUNG_SIGNAL_STRENGTH_ID = 1101; //0x044D
	//public static final int SAMSUNG_SIGNAL_STRENGTH_IDF = SAMSUNG_SIGNAL_STRENGTH_ID << 16;
	public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_NO_BAR = 0;
	public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_ONE_BAR = 1;
	public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_TWO_BAR = 2;
	public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_THREE_BAR = 3;
	public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_FOUR_BAR = 4;

	public static final int SAMSUNG_PLAY_CONTROLLER_ID = 1102; //0x044E
	//public static final int SAMSUNG_PLAY_CONTROLLER_IDF = SAMSUNG_PLAY_CONTROLLER_ID << 16;
	public static final int SAMSUNG_BUTTON_UP = 0;
	public static final int SAMSUNG_BUTTON_DOWN = 1;
	public static final int SAMSUNG_BUTTON_RIGHT = 2;
	public static final int SAMSUNG_BUTTON_LEFT = 3;
	public static final int SAMSUNG_BUTTON_A = 4;
	public static final int SAMSUNG_BUTTON_B = 5;
	public static final int SAMSUNG_BUTTON_C = 6;
	public static final int SAMSUNG_BUTTON_D = 7;

	public static final int SAMSUNG_DEVICE_INFO_ID = 1103; //0x044F
	//public static final int SAMSUNG_DEVICE_INFO_IDF = SAMSUNG_DEVICE_INFO_ID << 16;
	public static final int SAMSUNG_DEVICE_ORIENTATION_LANDSCAPE = 0;
	public static final int SAMSUNG_DEVICE_ORIENTATION_PORTRAIT = 1;
	public static final int SAMSUNG_DEVICE_GESTURE_NONE = 2;
	public static final int SAMSUNG_DEVICE_GESTURE_DEVICE_SHAKEN = 3;
	public static final int SAMSUNG_DEVICE_DISPLAY_OFF = 4;
	public static final int SAMSUNG_DEVICE_DISPLAY_ON = 5;
	public static final int SAMSUNG_DEVICE_BATTERY_STRENGTH = 6;


	public static final int SAMSUNG_TELEPHONY_ID = 1104; //0X0450
	public static final int SAMSUNG_INCOMING_SMS = 0;
	public static final int SAMSUNG_INCOMING_CALL = 1;


	/*
	 * this is microbit buttons
	 */
	public static final int MICROBIT_BUTTON_A_ID = 1;
	//public static final int MICROBIT_BUTTON_A_IDF = MICROBIT_BUTTON_A_ID << 16;

	public static final int MICROBIT_BUTTON_B_ID = 2;
	//public static final int MICROBIT_BUTTON_B_IDF = MICROBIT_BUTTON_B_ID << 16;

	public static final int MICROBIT_BUTTON_RESET_ID = 3;
	//public static final int MICROBIT_BUTTON_RESET_IDF = MICROBIT_BUTTON_RESET_ID << 16;

	public static final int MICROBIT_BUTTON_EVT_DOWN = 1;
	public static final int MICROBIT_BUTTON_EVT_UP = 2;
	public static final int MICROBIT_BUTTON_EVT_CLICK = 3;
	public static final int MICROBIT_BUTTON_EVT_LONG_CLICK = 4;
	public static final int MICROBIT_BUTTON_EVT_HOLD = 5;

	// Registration ID's
	public static final int REG_TELEPHONY = 0x01;    // 0x00000001;
	public static final int REG_MESSAGING = 0x02;    // 0x00000002;
	public static final int REG_DEVICEORIENTATION = 0x04;    // 0x00000004;
	public static final int REG_DEVICEGESTURE = 0x08;    // 0x00000008;
	public static final int REG_DISPLAY = 0x010;    // 0x00000010;
	public static final int REG_SIGNALSTRENGTH = 0x020;    // 0x00000020;
	public static final int REG_BATTERYSTRENGTH = 0x040;    // 0x00000040;
	public static final int REG_TEMPERATURE = 0x080;    // 0x00000080;


	public static final byte[] REGISTRATION_ON = {REG_TELEPHONY, REG_MESSAGING, REG_DEVICEORIENTATION,REG_DEVICEGESTURE,REG_DISPLAY,REG_SIGNALSTRENGTH,REG_BATTERYSTRENGTH  };
	public static final byte[] REGISTRATION_OFF = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00 ,0x00};
	/*
	 * BLE UUID's for Service and Characteristic
	 */
	public static final String BASE_UUID_STR = "00000000-0000-1000-8000-00805f9b34fb";
	public static final UUID BASE_UUID = UUID.fromString(BASE_UUID_STR);

	public static final String MICROBIT_BASE_UUID_STR = "e95d5be9-251d-470a-a062-fa1922dfa9a8";
	public static final UUID MICROBIT_BASE_UUID = UUID.fromString(MICROBIT_BASE_UUID_STR);

	// This descriptor is used for requesting notification
	public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR = makeUUID(BASE_UUID_STR, 0x02902);

	public static final UUID SERVICE_GENERIC_ACCESS = makeUUID(MICROBIT_BASE_UUID_STR, 0x01800);
	public static final UUID SGA_DEVICE_NAME = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a00);
	public static final UUID SGA_APPEARANCE = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a01);
	public static final UUID SGA_PPRIVACY_FLAG = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a02);
	public static final UUID SGA_RECONNECTION_ADDRESS = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a03);
	public static final UUID SGA_PPCONNECTION_PARAMETERS = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a04);

	public static final UUID SERVICE_DEVICE_INFORMATION = makeUUID(MICROBIT_BASE_UUID_STR, 0x0180a);
	public static final UUID SDI_SYSTEM_ID = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a23);
	public static final UUID SDI_MODEL_NUMBER_STRING = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a24);
	public static final UUID SDI_SERIAL_NUMBER_STRING = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a25);
	public static final UUID SDI_FIRMWARE_REVISION_STRING = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a26);
	public static final UUID SDI_HARDWARE_REVISION_STRING = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a27);
	public static final UUID SDI_SOFTWARE_REVISION_STRING = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a28);
	public static final UUID SDI_REGULATORY_CERTIFICATION_DATA_LIST = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a2a);
	public static final UUID SDI_PNP_ID = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a50);

	public static final UUID SERVICE_BATTERY_SERVICE = makeUUID(MICROBIT_BASE_UUID_STR, 0x0180f);
	public static final UUID SBS_BATTERY_LEVEL = makeUUID(MICROBIT_BASE_UUID_STR, 0x02a19);

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
	public static final UUID ES_MICROBIT_REQUIREMENTS = makeUUID(MICROBIT_BASE_UUID_STR, 0x0B84C);

    public static final UUID ES_MICROBIT_EVENT = makeUUID(MICROBIT_BASE_UUID_STR, 0x09775); //Events going to the micro:bit. For this  ES_MICROBIT_REQUIREMENTS needs to be set properly.
	public static final UUID ES_CLIENT_EVENT = makeUUID(MICROBIT_BASE_UUID_STR, 0x05404); //Events Coming from micro:bit. For this  ES_CLIENT_REQUIREMENTS needs to be set properly.

    public static final UUID ES_CLIENT_REQUIREMENTS = makeUUID(MICROBIT_BASE_UUID_STR, 0x023C4);

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

	public static int makeMicroBitValue(int category, int value) {
		return ((value << 16) | category);
	}

	public static String URL = "URL";

	//TODO: Change to data/data/appName/files MBApp.getContext().getFilesDir();
	public static File HEX_FILE_DIR = Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
	//public static File HEX_FILE_DIR = MBApp.getContext().getFilesDir();

    public static String ZIP_INTERNAL_NAME = "raw/samples";

	public static int NOTIFICATION_ID = 1001 ;
    public static int BLE_DISCONNECTED_FOR_FLASH = 501 ;
    public static int PAIRING_CONTROL_CODE_REQUESTED = 0x01 ;
    public static int FLASHING_PHASE_2_COMPLETE = 0x02 ;
    public static int FLASHING_PAIRING_CODE_CHARACTERISTIC_RECIEVED = 0X33 ;

    public static int REQUEST_ENABLE_BT = 12345 ; //Magic number

	public static String LAUNCH_CAMERA_AUDIO = "raw/en_gb_emma_launching_camera";

    public static String TAKING_PHOTO_AUDIO = "raw/en_gb_emma_taking_photo";
    public static String RECORDING_VIDEO_AUDIO = "raw/en_gb_emma_recording_video_30sec";
    public static String PICTURE_TAKEN_AUDIO = "raw/en_gb_emma_photo_taken";
    public static String MAX_VIDEO_RECORDED = "raw/en_gb_emma_max_video";


    public static int MAX_VIDEO_RECORDING_TIME = 30 * 1000 ; //Duration in ms
    public static int MAX_VIDEO_FILE_SIZE = 100 * 1024 * 1024 ; //Size in bytes

    public static String MEDIA_OUTPUT_FOLDER = "bbc-microbit";
}
