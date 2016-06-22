package com.samsung.microbit.presentation;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.samsung.microbit.MBApp;

import java.io.IOException;

public class PlayAudioPresenter implements Presenter {

    private AudioManager audioManager;

    private int originalRingerMode;

    private String notificationForPlay;
    private MediaPlayer mediaplayer;
    private MediaPlayer.OnCompletionListener callBack;

    public PlayAudioPresenter() {
    }

    public void setNotificationForPlay(String notificationForPlay) {
        this.notificationForPlay = notificationForPlay;
    }

    public void setCallBack(MediaPlayer.OnCompletionListener callBack) {
        this.callBack = callBack;
    }

    @Override
    public void start() {
        MBApp app = MBApp.getApp();

        Resources resources = app.getResources();
        int resID = resources.getIdentifier(notificationForPlay, "raw", app.getPackageName());
        AssetFileDescriptor afd = resources.openRawResourceFd(resID);

        preparePhoneToPlayAudio(app);

        mediaplayer = new MediaPlayer();

        mediaplayer.reset();
        mediaplayer.setVolume(1.0f, 1.0f);
        mediaplayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
        try {
            mediaplayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaplayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Utils", "playAudio: exception");
            mediaplayer.release();
            return;
        }
        //Set a callback for completion
        mediaplayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                restoreAudioMode();
                if (callBack != null) {
                    callBack.onCompletion(mp);
                }
                mediaplayer.release();
            }
        });
        mediaplayer.start();
    }

    private void preparePhoneToPlayAudio(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        originalRingerMode = audioManager.getRingerMode();

        //if (originalRingerMode != AudioManager.RINGER_MODE_NORMAL) {
        //    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        //}
    }

    private void restoreAudioMode() {
        audioManager.setRingerMode(originalRingerMode);
    }

    @Override
    public void stop() {
        if(mediaplayer != null && mediaplayer.isPlaying()) {
            mediaplayer.stop();
        }
    }

    @Override
    public void destroy() {
        mediaplayer.release();
        mediaplayer = null;
    }
}