package com.samsung.microbit;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.util.HashMap;

import uk.co.bbc.echo.EchoClient;
import uk.co.bbc.echo.enumerations.ApplicationType;

public class MBApp extends Application {

    private static Context mContext;
    private EchoClient echo;

    private static MBApp app = null;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public static Context getContext() { return MBApp.mContext; }

    public static void setContext(Context ctx) { MBApp.mContext = ctx; }

    public static MBApp getApp(){ return app;}

    // Using release 9.2.1 from https://github.com/bbc/echo-client-android/releases/tag/9.2.1
    public void initialiseEcho(HashMap config) {
        echo = new EchoClient(
                "microbit", //getString(R.string.app_name),   // App Name
                ApplicationType.MOBILE_APP,    // App Type
                "com.samsung.microbit.page",   // App Countername // ECHO: Label had to be cleaned from: com.samsung.microbit to com.samsung.microbit.page error only thrown in debug mode
                getApplicationContext(),       // The Android Context of your Application
                config
        );

        echo.setPlayerName("micro-bit Android");
        echo.setPlayerVersion("1.3.4");
        String androidDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("Device ID", androidDeviceId);
    }

    public EchoClient getEcho() {
        return echo;
    }
}