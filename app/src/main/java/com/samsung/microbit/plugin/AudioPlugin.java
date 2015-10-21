package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.ui.activity.AudioRecorderActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AudioPlugin {
	private static Context context = null;

	public static final String INTENT_ACTION_LAUNCH = "com.samsung.microbit.ui.activity.AudioRecorderActivity.action.LAUNCH";
	public static final String INTENT_ACTION_START_RECORD = "com.samsung.microbit.ui.activity.AudioRecorderActivity.action.START_RECORD";
	public static final String INTENT_ACTION_STOP_RECORD = "com.samsung.microbit.ui.activity.AudioRecorderActivity.action.STOP_RECORD";
	public static final String INTENT_ACTION_STOP = "com.samsung.microbit.ui.activity.AudioRecorderActivity.action.STOP";//close

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		context = ctx;
		switch (cmd.getCMD()) {
			case Constants.SAMSUNG_AUDIO_RECORDER_EVT_START_CAPTURE: {
				launchActivity(INTENT_ACTION_START_RECORD);
				break;
			}
			case Constants.SAMSUNG_AUDIO_RECORDER_EVT_STOP_CAPTURE: {
				launchActivity(INTENT_ACTION_STOP_RECORD);
				break;
			}
			case Constants.SAMSUNG_AUDIO_RECORDER_EVT_LAUNCH: {
				launchActivity(INTENT_ACTION_LAUNCH);
				break;
			}
			case Constants.SAMSUNG_AUDIO_RECORDER_EVT_STOP: {
				launchActivity(INTENT_ACTION_STOP);
				break;
			}
		}
	}

	static private void launchActivity(String action) {
		Intent mIntent = new Intent(context, AudioRecorderActivity.class);
		mIntent.setAction(action);

		// keep same instance of activity
		mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(mIntent);
	}
}
