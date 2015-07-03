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

/**
 * Created by frederic.ma on 13/05/2015.
 */
public class AudioPlugin {
	private static Context context = null;

	public static final String INTENT_ACTION_LAUNCH = "com.samsung.microbit.ui.activity.AudioRecorderActivity.action.LAUNCH";
	public static final String INTENT_ACTION_START_RECORD = "com.samsung.microbit.ui.activity.AudioRecorderActivity.action.START_RECORD";
	public static final String INTENT_ACTION_STOP_RECORD = "com.samsung.microbit.ui.activity.AudioRecorderActivity.action.STOP_RECORD";

	private static MediaRecorder mRecorder = null;
	private static File mFile = null;
	private static boolean mIsRecording = false;

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		context = ctx;
		switch (cmd.getCMD()) {
			case Constants.SAMSUNG_AUDIO_RECORDER_EVT_START_CAPTURE: {
				startRecording();
				break;
			}
			case Constants.SAMSUNG_AUDIO_RECORDER_EVT_STOP_CAPTURE: {
				stopRecording();
				break;
			}
			case Constants.SAMSUNG_AUDIO_RECORDER_EVT_LAUNCH: {
				launchActivity(INTENT_ACTION_LAUNCH, null);
				break;
			}
			case Constants.SAMSUNG_AUDIO_RECORDER_EVT_STOP: {
				if (mIsRecording) {
					stopRecording();
				} else {
					startRecording();
				}
				break;
			}
		}
	}

	static public File getAudioFilename() {
		// Get the directory for the user's public pictures directory.
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e("AudioPlugin", "Failed to create directory");
			}
		}

		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy_HHmmss");
		String filename = "voice_" + sdf.format(c.getTime());

		File file = new File(dir, filename + ".3gp");
		return file;
	}

	static private void refreshAudio(File file) {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		context.sendBroadcast(mediaScanIntent);
	}

	static private void launchActivity(String action, String filename) {
		Intent mIntent = new Intent(context, AudioRecorderActivity.class);
		mIntent.setAction(action);
		if (filename != null)
			mIntent.putExtra("filename", filename);
		// keep same instance of activity
		// FLAG_ACTIVITY_SINGLE_TOP is needed for some reasons.
		// if not used, it will create new instance
		mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(mIntent);
	}

	public static boolean isRecording() {
		return mIsRecording;
	}

	private static void startRecording() {
		if (mRecorder != null)
			return;

		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

		mFile = getAudioFilename();

		mRecorder.setOutputFile(mFile.getPath());
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e("AudioPlugin", "prepare() failed" + e.toString());
		}

		launchActivity(INTENT_ACTION_START_RECORD, mFile.getName());

		mRecorder.start();
		mIsRecording = true;
	}

	private static void stopRecording() {
		if (mRecorder == null)
			return;

		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;

		if (mFile != null) {

			launchActivity(INTENT_ACTION_STOP_RECORD, null);

			refreshAudio(mFile);
		}

		mIsRecording = false;
	}
}
