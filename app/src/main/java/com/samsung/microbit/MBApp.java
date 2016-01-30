package com.samsung.microbit;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.util.HashMap;

import uk.co.bbc.echo.EchoClient;
import uk.co.bbc.echo.enumerations.ApplicationType;

public class MBApp extends Application {

    private static Context mContext;
    private static boolean mshareStat = false ;
    private EchoClient echo;
    private Typeface typeface;

    private static MBApp app = null;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public Typeface getTypeface(){
        return Typeface.createFromAsset(getAssets(), "fonts/GT-Walsheim.otf");
    }

    public static Context getContext() { return MBApp.mContext; }

    public static void setContext(Context ctx) { MBApp.mContext = ctx; }

    public static MBApp getApp(){ return app;}

    // Using release 9.2.1 from https://github.com/bbc/echo-client-android/releases/tag/9.2.1
    public void initialiseEcho(HashMap config) {
        echo = new EchoClient(
                "microbit", //getString(R.string.app_name),   // App Name
                ApplicationType.MOBILE_APP,    // App Type
                "kl.education.microbit.splashscreen.page",   // App Countername // ECHO: Label had to be cleaned from: com.samsung.microbit to com.samsung.microbit.page error only thrown in debug mode
                getApplicationContext(),       // The Android Context of your Application
                config
        );

        //echo.setPlayerName("micro-bit Android");
        //echo.setPlayerVersion("1.3.4");
        //String androidDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        //Log.d("Device ID", androidDeviceId);
    }

    public static void setSharingStats(boolean shareStat)
    {
        mshareStat = shareStat ;
    }
    public EchoClient getEcho() {
        if (mshareStat) {
            Log.d("MBApp", "Sharing stats is enabled by user");
            return echo;
        }
        else {
            Log.d("MBApp", "Sharing of stats is disabled by user");
            return null;
        }
    }


    public void sendViewEventStats(String viewEventString)
    {
        if (mshareStat && echo != null){
            Log.d("MBApp", "sendViewEventStats " + viewEventString);
            String counterName = getString(R.string.stats_view_name , viewEventString);
            // (String counterName, HashMap<String, String> eventLabels)
            HashMap <String, String> eventLabels = new HashMap<String,String>();
            eventLabels.put("bbc_site", "bitesize");
            echo.viewEvent(counterName, eventLabels);
        }
        else
        {
            Log.d("MBApp", "Sharing of stats is disabled by user or Echo not initialised");
        }
    }

    public void sendFlashStats(boolean success , String fileName, String hexsize, String binsize, String firmware)
    {
        if (mshareStat && echo != null){
            Log.d("MBApp", "sendFlashStats fileName=" + fileName + " hexsize=" + hexsize + "  binsize=" + binsize + " microbit_firmwwareversion= " + firmware);
            HashMap <String, String> eventLabels = new HashMap<String,String>();
            eventLabels.put("action_location" , "app");
            eventLabels.put("bbc_site", "bitesize");
            eventLabels.put("hex_file_size" , hexsize);
            eventLabels.put("binary_size" , binsize);
            eventLabels.put("firmware" , firmware);
            if (success){
                echo.userActionEvent("success", "hex-file-flash", eventLabels);
            } else {
                echo.userActionEvent("fail", "hex-file-flash",eventLabels);
            }
        } else {
            Log.d("MBApp", "Sharing of stats is disabled by user or Echo not initialised");
        }
    }
}