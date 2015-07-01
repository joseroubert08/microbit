package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Timer;
import java.util.TimerTask;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;

/**
 * Created by a.obzhirov on 14/05/2015.
 */
public class RemoteControlPlugin {

	private static Context context = null;

	static final String TAG = "RemoteControlPlugin";
	private static boolean debug = true;

	static void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		context = ctx;
		switch (cmd.getCMD()) {
			case Constants.SAMSUNG_REMOTE_CONTROL_EVT_PLAY:
				if (debug) logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_PLAY");
				Play();
				break;

			case Constants.SAMSUNG_REMOTE_CONTROL_EVT_PAUSE:
				if (debug) logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_PAUSE");
				Pause();
				break;

			case Constants.SAMSUNG_REMOTE_CONTROL_EVT_STOP:
				if (debug) logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_STOP");
				Stop();
				break;

			case Constants.SAMSUNG_REMOTE_CONTROL_EVT_NEXTTRACK:
				if (debug) logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_NEXTTRACK");
				NextTrack();
				break;

			case Constants.SAMSUNG_REMOTE_CONTROL_EVT_PREVTRACK:
				if (debug) logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_PREVTRACK");
				PrevTrack();
				break;

			case Constants.SAMSUNG_REMOTE_CONTROL_EVT_FORWARD:
				if (debug) logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_FORWARD");
				Forward();
				break;

			case Constants.SAMSUNG_REMOTE_CONTROL_EVT_REWIND:
				if (debug) logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_REWIND");
				Rewind();
				break;

			case Constants.SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEUP:
				if (debug) logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEUP");
				VolumeUp();
				break;

			case Constants.SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEDOWN:
				if (debug) logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEDOWN");
				VolumeDown();
				break;

			default:
				break;
		}
	}

	private static final void sendMediaKeyEvent(final int action, final int code) {
		Intent mediaEvent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		KeyEvent event = new KeyEvent(action, code);
		mediaEvent.putExtra(Intent.EXTRA_KEY_EVENT, event);
		context.sendOrderedBroadcast(mediaEvent, null);
	}

	private static final void scheduleMediaKeyEvent(final int action, final int code, final int duration) {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				sendMediaKeyEvent(action, code);
			}
		}, duration);
	}

	private static void Play() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 100);
	}

	private static void Pause() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 100);
	}

	private static void Stop() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP, 100);
	}

	private static void NextTrack() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 100);
	}

	private static void PrevTrack() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 100);
	}

	private static void Forward() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, 100);
	}

	private static void Rewind() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_REWIND, 100);
	}

	private static void VolumeUp() {
		AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
			AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
		/*
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP, 100);
        */
	}

	private static void VolumeDown() {
		AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
			AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
		/*
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 100);
        */
	}

}
