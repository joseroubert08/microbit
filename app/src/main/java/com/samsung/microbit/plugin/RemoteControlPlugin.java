package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.constants.EventSubCodes;

import java.util.Timer;
import java.util.TimerTask;

import static com.samsung.microbit.BuildConfig.DEBUG;

public class RemoteControlPlugin {
	private static final String TAG = RemoteControlPlugin.class.getSimpleName();

	private static void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		switch (cmd.getCMD()) {
			case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_PLAY:
				if (DEBUG) {
                    logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_PLAY");
                }

				play();
				break;

			case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_PAUSE:
				if (DEBUG) {
                    logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_PAUSE");
                }

				pause();
				break;

			case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_STOP:
				if (DEBUG) {
                    logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_STOP");
                }

				stop();
				break;

			case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_NEXTTRACK:
				if (DEBUG) {
                    logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_NEXTTRACK");
                }

				nextTrack();
				break;

			case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_PREVTRACK:
				if (DEBUG) {
                    logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_PREVTRACK");
                }

				previousTrack();
				break;

			case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_FORWARD:
				if (DEBUG) {
                    logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_FORWARD");
                }

				forward();
				break;

			case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_REWIND:
				if (DEBUG) {
                    logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_REWIND");
                }

				rewind();
				break;

			case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEUP:
				if (DEBUG) {
                    logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEUP");
                }

				volumeUp();
				break;

			case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEDOWN:
				if (DEBUG) {
                    logi("pluginEntry() ##  SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEDOWN");
                }

				volumeDown();
				break;

			default:
				break;
		}
	}

	private static void sendMediaKeyEvent(final int action, final int code) {
		Intent mediaEvent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		KeyEvent event = new KeyEvent(action, code);
		mediaEvent.putExtra(Intent.EXTRA_KEY_EVENT, event);
		MBApp.getApp().sendOrderedBroadcast(mediaEvent, null);
	}

	private static void scheduleMediaKeyEvent(final int action, final int code, final int delay) {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				sendMediaKeyEvent(action, code);
			}
		}, delay);
	}

	private static void play() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 100);
	}

	private static void pause() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 100);
	}

	private static void stop() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP, 100);
	}

	private static void nextTrack() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 100);
	}

	private static void previousTrack() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 100);
	}

	private static void forward() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, 100);
	}

	private static void rewind() {
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND, 0);
		scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_REWIND, 100);
	}

	private static void volumeUp() {
		AudioManager audio = (AudioManager) MBApp.getApp().getSystemService(Context.AUDIO_SERVICE);
		audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
			AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
		/*
		scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP, 100);
        */
	}

	private static void volumeDown() {
		AudioManager audio = (AudioManager) MBApp.getApp().getSystemService(Context.AUDIO_SERVICE);
		audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
			AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
		/*
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 100);
        */
	}
}