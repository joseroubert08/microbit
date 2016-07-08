package com.samsung.microbit.data.constants;

import com.samsung.microbit.utils.MemoryUnits;

import java.util.concurrent.TimeUnit;

/**
 * Contains common constants that use different part of the app.
 */
public class Constants {
    public static final int MAX_VIDEO_RECORDING_TIME_MILLIS = (int) TimeUnit.SECONDS.toMillis(30);
    public static final int VIDEO_FLASH_PICK_INTERVAL = (int) TimeUnit.SECONDS.toMillis(1);

    public static final int MAX_VIDEO_FILE_SIZE_BYTES = (int) MemoryUnits.Megabytes.instance().toBytes(100);

    public static final int PIC_COUNTER_DURATION_MILLIS = (int) TimeUnit.SECONDS.toMillis(5);
    public static final int PIC_COUNTER_INTERVAL_MILLIS = 900;

    public static final int MAX_COUNT_OF_RE_CONNECTIONS_FOR_DFU = 3;
    public static final long TIME_FOR_RECONNECTION = TimeUnit.SECONDS.toMillis(15);
    public static final long DELAY_BETWEEN_PAUSE_AND_RESUME = TimeUnit.SECONDS.toMillis(1);

    /**
     * Represents common states of connection operations.
     * It uses to identify connection state and send appropriate statistics.
     */
    public enum ConnectionState {
        SUCCESS,
        FAIL,
        DISCONNECT
    }
}
