package com.samsung.microbit.data.constants;

import com.samsung.microbit.utils.MemoryUnits;

import java.util.concurrent.TimeUnit;

public class Constants {
    public static int MAX_VIDEO_RECORDING_TIME_MILLIS = (int) TimeUnit.SECONDS.toMillis(30);
    public static int VIDEO_FLASH_PICK_INTERVAL = (int) TimeUnit.SECONDS.toMillis(1);

    public static int MAX_VIDEO_FILE_SIZE_BYTES = (int) MemoryUnits.Megabytes.instance().toBytes(100);

	public static int PIC_COUNTER_DURATION_MILLIS = (int) TimeUnit.SECONDS.toMillis(5);
	public static int PIC_COUNTER_INTERVAL_MILLIS = 900;

    //For stats
    public enum ConnectionState {
        SUCCESS,
        FAIL,
        DISCONNECT
    }
}
