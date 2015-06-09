package com.samsung.microbit.plugin;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import android.os.Vibrator;
import android.view.Gravity;
import android.widget.Toast;

import com.samsung.microbit.model.CmdArg;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class AlertPlugin {

	private static Context context = null;

	//Alert plugin action
	public static final int TOAST = 0;
	public static final int VIBRATE = 1;
	public static final int SOUND = 2;
	public static final int RINGTONE = 3;
	public static final int FINDPHONE = 4;

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		context = ctx;
		switch (cmd.getCMD()) {
			case TOAST:
				showToast(cmd.getValue());
				break;

			case VIBRATE:
				vibrate(Integer.parseInt(cmd.getValue()));
				break;

			case SOUND:
				playSound(cmd.getValue());
				break;

			case RINGTONE:
				playRingTone();
				break;

			case FINDPHONE:
				findPhone();
				break;

			default:
				break;
		}
	}

	private static void showToast(final String msg) {
		Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	private static void playSound(String sound) {
		Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		int duration = getDuration(alarm);
		final Ringtone r = RingtoneManager.getRingtone(context, alarm);
		r.play();

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				r.stop();
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, duration);
	}

	private static void playRingTone() {
		Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(context, ringtone);
		r.play();
	}

	private static void findPhone() {
		Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(context, ringtone);
		r.setStreamType(AudioManager.STREAM_ALARM);
		r.play();
		vibrate(1000);
	}

	private static void vibrate(int duration) {
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(duration);
	}

	private static int getDuration(Uri file) {
		int duration = 500;
		MediaPlayer mp = new MediaPlayer();
		try {
			mp.setDataSource(context, file);
			mp.prepare();
			duration = mp.getDuration();
		} catch (IOException e) {
			e.printStackTrace();
		}

		mp.reset();
		mp = null;

		return duration;
	}
}
