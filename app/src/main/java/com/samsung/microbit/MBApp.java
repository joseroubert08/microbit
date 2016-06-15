package com.samsung.microbit;

import android.app.Application;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import com.samsung.microbit.common.AppInfo;
import com.samsung.microbit.core.EchoClientManager;

public class MBApp extends Application {

    private static final int MAX_STREAMS_SIMULTANIOUSLY = 10;

    private static MBApp app = null;

    private EchoClientManager echoClientManager;

    private SoundPool appSoundPool;

    private AppInfo appInfo;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        echoClientManager = EchoClientManager.getInstance(this);

        if(Build.VERSION.SDK_INT >= 21) {
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            attrBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            attrBuilder.setUsage(AudioAttributes.USAGE_MEDIA);
            attrBuilder.setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED);

            appSoundPool = new SoundPool.Builder().setMaxStreams(MAX_STREAMS_SIMULTANIOUSLY).setAudioAttributes
                    (attrBuilder.build()).build();
        } else {
            appSoundPool = new SoundPool(MAX_STREAMS_SIMULTANIOUSLY, AudioManager.STREAM_MUSIC, 0);
        }

        appInfo = new AppInfo(this);
        Log.d("MBApp", "App Created");
    }

    public Typeface getTypeface() {
        return Typeface.createFromAsset(getAssets(), "fonts/GT-Walsheim.otf");
    }

    public Typeface getTypefaceBold() {
        return Typeface.createFromAsset(getAssets(), "fonts/GT-Walsheim-Bold.otf");
    }

    public static MBApp getApp() {
        return app;
    }

    public EchoClientManager getEchoClientManager() {
        return echoClientManager;
    }

    public SoundPool getAppSoundPool() {
        return appSoundPool;
    }

    public AppInfo getAppInfo() {
        return appInfo;
    }
}