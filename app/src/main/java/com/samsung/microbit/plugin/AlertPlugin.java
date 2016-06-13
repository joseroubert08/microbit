package com.samsung.microbit.plugin;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.RawConstants;
import com.samsung.microbit.presentation.PlayAudioPresenter;
import com.samsung.microbit.ui.PopUp;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class AlertPlugin {
    private static PlayAudioPresenter playAudioPresenter = new PlayAudioPresenter();

    private static AlertDialog customDialog = null;
    private static Ringtone mRingtone = null;
    private static Vibrator mVibrator = null;
    private static Timer mTimer = null;
    private static TimerTask mStopTask = new TimerTask() {
        @Override
        public void run() {

        }
    };

    private static void stopPlaying() {
        if (mRingtone != null && mRingtone.isPlaying()) {
            mRingtone.stop();
        }
    }

    private static void playSound(Uri alarm, int maxDuration, boolean vibrate, boolean isAlarm) {
        Context context = MBApp.getApp();

        int duration = getDuration(alarm);
        if (maxDuration > 0 && duration > maxDuration)
            duration = maxDuration;

        if (mRingtone != null && mRingtone.isPlaying()) {
            mRingtone.stop();
        }

        if (mTimer != null)
            //After this operation the timer cannot be used anymore
            mTimer.cancel();

        mTimer = new Timer();

        mRingtone = RingtoneManager.getRingtone(context, alarm);

        if (isAlarm)
            mRingtone.setStreamType(AudioManager.STREAM_ALARM);
        mRingtone.play();

        TimerTask stopTask = new TimerTask() {
            @Override
            public void run() {
                stopPlaying();
            }
        };

        mTimer.schedule(stopTask, duration);

        if (vibrate) {
            if (mVibrator == null)
                mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            if (mVibrator != null && mVibrator.hasVibrator()) {
                mVibrator.cancel();
                mVibrator.vibrate(duration);
            }
        }
    }

    public static void pluginEntry(Context ctx, CmdArg cmd) {
        Context context = MBApp.getApp();
        switch (cmd.getCMD()) {
            case Constants.SAMSUNG_ALERT_EVT_DISPLAY_TOAST:
                PopUp.showFromService(context, cmd.getValue(),
                        "Message from Micro:Bit",
                        R.drawable.message_face, R.drawable.blue_btn,
                        0, /* TODO - nothing needs to be done */
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
            case Constants.SAMSUNG_ALERT_EVT_ALARM1:
            case Constants.SAMSUNG_ALERT_EVT_ALARM2:
            case Constants.SAMSUNG_ALERT_EVT_ALARM3:
            case Constants.SAMSUNG_ALERT_EVT_ALARM4:
            case Constants.SAMSUNG_ALERT_EVT_ALARM5:
            case Constants.SAMSUNG_ALERT_EVT_ALARM6:
                playAlarm(cmd.getCMD());
            default:
                break;
        }
    }

    private static void playAlarm(int alarmId) {
        Context context = MBApp.getApp();

        showDialog(context.getString(R.string.sound_via_microbit));
        Uri alarm = null;
        RingtoneManager ringtoneMgr = new RingtoneManager(context);
        ringtoneMgr.setType(RingtoneManager.TYPE_ALARM);
        Cursor alarms = ringtoneMgr.getCursor();
        int alarmsCount = alarms.getCount();
        Log.i("Alerts Plugin", "playAlarm: total alarms = " + alarms.getCount());

        alarms.moveToPosition(alarmId - 4);
        alarm = ringtoneMgr.getRingtoneUri(alarms.getPosition());
        if (alarm == null) {
            Log.i("Alerts Plugin", "Cannot play nth Alarm. Playing default");
            alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        playSound(alarm, 10000, false, false);
    }

    private static void playRingTone() {
        showDialog(MBApp.getApp().getString(R.string.ringtone_via_microbit));
        Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        playSound(ringtone, 10000, false, false);
    }

    private static void playNotification() {
        showDialog(MBApp.getApp().getString(R.string.sound_via_microbit));
        Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        playSound(ringtone, 10000, false, false);
    }

    private static void findPhone() {
        Context context = MBApp.getApp();

        showDialog(context.getString(R.string.findphone_via_microbit));
        if (mVibrator == null)
            mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.cancel();
            mVibrator.vibrate(5 * 1000);
        }

        playAudioPresenter.setNotificationForPlay(RawConstants.FIND_MY_PHONE_AUDIO);
        playAudioPresenter.start();
    }

    private static void vibrate(int duration) {
        Context context = MBApp.getApp();

        showDialog(context.getString(R.string.vibrating_via_microbit));
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(duration);
    }

    private static int getDuration(Uri file) {
        int duration = 500;
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(MBApp.getApp(), file);
            mp.prepare();
            duration = mp.getDuration();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mp.reset();
        mp = null;

        return duration;
    }


    private static void showDialog(String textMsg) {
        PopUp.showFromService(MBApp.getApp(), "",
                textMsg,
                R.drawable.message_face, R.drawable.blue_btn,
                0, /* TODO - nothing needs to be done */
                PopUp.TYPE_ALERT);
    }

}
