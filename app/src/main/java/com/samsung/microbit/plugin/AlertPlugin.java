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

	//Alert plugin action
	public static final int TOAST = 0;
	public static final int VIBRATE = 1;
	public static final int SOUND = 2;
	public static final int RINGTONE = 3;
	public static final int FINDPHONE = 4;

	//Sound file to play
	public static final int ALARAM = 0;
	public static final int TARDIS = 1;
	public static final int YOUREFIRED = 2;

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		context = ctx;
		switch (cmd.getCMD()) {
			case TOAST:
				PopUp.show(context,
						cmd.getValue(),
						"Message from Micro:Bit",
						0, 0,
						PopUp.TYPE_ALERT, null, null);
				break;

			case VIBRATE:
				vibrate(Integer.parseInt(cmd.getValue()));
				break;

			case SOUND:
				playSound(Integer.parseInt(cmd.getValue()));
				break;

			case RINGTONE:
				playRingTone();
				break;

			case FINDPHONE:
				PopUp.show(context,
						context.getString(R.string.findphone_via_microbit),
						"Message from Micro:Bit",
						0, 0,
						PopUp.TYPE_ALERT, null, null);
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

	private static void playSound(int sound) {
		switch(sound) {
			case TARDIS:
			case YOUREFIRED:
				playSoundFile(sound);
				break;
			default:
				playAlarm();
				break;
		}
	}

	private static void playSoundFile(int sound) {
		int duration = 500;
		resetMediaPlayer();

		switch(sound) {
			case 1:
				mediaPlayer = MediaPlayer.create(context, R.raw.tardis);
				break;
			case 2:
				mediaPlayer = MediaPlayer.create(context, R.raw.yourefired);
				break;
		}

		duration = mediaPlayer.getDuration();

		if(mediaPlayer != null) {
			showDialog(context.getString(R.string.sound_via_microbit));
			mediaPlayer.start();
			dialogTimer(duration);
		}
	}

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
		Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		int duration = getDuration(ringtone);
		Ringtone r = RingtoneManager.getRingtone(context, ringtone);
		r.play();
		dialogTimer(duration);
	}

	private static void findPhone() {
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

		if(customDialog != null) {
			customDialog.dismiss();
		}

		customDialog = new AlertDialog.Builder(context).create();
		customDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		customDialog.setCanceledOnTouchOutside(true);
		customDialog.setTitle(context.getString(R.string.alert));
		customDialog.setMessage(textMsg);
		customDialog.setIcon(R.drawable.ic_launcher);
		customDialog.show();
	}

	private static void dismissDialog(){
		if(customDialog != null) {
			customDialog.dismiss();
		}

		resetMediaPlayer();
		customDialog = null;
	}

	private static void dialogTimer(int duration) {
		//Keep dialog for longer
		duration = duration * 3;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				dismissDialog();
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
