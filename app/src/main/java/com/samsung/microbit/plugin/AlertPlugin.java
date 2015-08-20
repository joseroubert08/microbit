package com.samsung.microbit.plugin;

import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import com.samsung.microbit.R;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.ui.PopUp;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class AlertPlugin {

	private static Context context = null;

	private static AlertDialog customDialog = null;
	private static MediaPlayer mediaPlayer = null;

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		context = ctx;
		switch (cmd.getCMD()) {
			case Constants.SAMSUNG_ALERT_EVT_DISPLAY_TOAST:
				PopUp.showFromService(context,cmd.getValue(),
									"Message from Micro:Bit",
									R.drawable.message_face, R.drawable.blue_btn,
									PopUp.TYPE_ALERT);
				break;

			case Constants.SAMSUNG_ALERT_EVT_VIBRATE:
				vibrate(Integer.parseInt(cmd.getValue()));
				break;

			case Constants.SAMSUNG_ALERT_EVT_PLAY_SOUND:
				playNotification();
				break;

			case Constants.SAMSUNG_ALERT_EVT_PLAY_RINGTONE:
				playRingTone();
				break;

			case Constants.SAMSUNG_ALERT_EVT_FIND_MY_PHONE:
				findPhone();
				break;

			default:
				break;
		}
	}

	//TODO: needed?
	private static void playAlarm() {
		showDialog(context.getString(R.string.sound_via_microbit));
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
		dialogTimer(duration);
	}

	private static void playRingTone() {
		showDialog(context.getString(R.string.ringtone_via_microbit));
		Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		int duration = getDuration(ringtone);
		Ringtone r = RingtoneManager.getRingtone(context, ringtone);
		r.play();
		dialogTimer(duration);
	}

	private static void playNotification() {
		showDialog(context.getString(R.string.sound_via_microbit));
		Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		int duration = getDuration(ringtone);
		Ringtone r = RingtoneManager.getRingtone(context, ringtone);
		r.play();
		dialogTimer(duration);
	}

	private static void findPhone() {
		showDialog(context.getString(R.string.findphone_via_microbit));
		Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(context, ringtone);
		int duration = getDuration(ringtone);
		r.setStreamType(AudioManager.STREAM_ALARM);
		r.play();
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(duration);
	}

	private static void vibrate(int duration) {
		showDialog(context.getString(R.string.vibrating_via_microbit));
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(duration);
		dialogTimer(duration);
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


	private static void showDialog(String textMsg){
		PopUp.showFromService(context,"",
				textMsg,
				R.drawable.message_face, R.drawable.blue_btn,
				PopUp.TYPE_ALERT);
	}

	private static void dialogTimer(int duration) {
		//Keep dialog for longer
		duration = duration * 3;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				resetMediaPlayer();
			}
		};

		Timer timer = new Timer();
		timer.schedule(task, duration);
	}

	private static void resetMediaPlayer() {
		if(mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.reset();
			mediaPlayer = null;
		}
	}
}
