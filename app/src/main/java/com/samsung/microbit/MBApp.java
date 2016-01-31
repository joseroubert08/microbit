package com.samsung.microbit;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.Log;

import com.samsung.microbit.core.Utils;

import java.util.HashMap;

import uk.co.bbc.echo.EchoClient;
import uk.co.bbc.echo.EchoConfigKeys;
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
        Log.d("MBApp", "App Created");
        SharedPreferences sharedPreferences = getSharedPreferences("com.samsung.microbit", MODE_PRIVATE);
        if (sharedPreferences != null) {
            mshareStat = sharedPreferences.getBoolean(getString(R.string.prefs_share_stats_status), true);
        }
        initialiseEcho();
    }

    public Typeface getTypeface(){
        return Typeface.createFromAsset(getAssets(), "fonts/GT-Walsheim.otf");
    }

    public static Context getContext() { return MBApp.mContext; }

    public static void setContext(Context ctx) { MBApp.mContext = ctx; }

    public static MBApp getApp(){ return app;}

    // Using release 9.2.1 from https://github.com/bbc/echo-client-android/releases/tag/9.2.1
    public void initialiseEcho() {

        HashMap<String, String> config = new HashMap<String, String>();
        //Use ECHO_TRACE value for searching in echo chamber
        config.put(EchoConfigKeys.ECHO_TRACE, "microbit_android_app");
        //Use CS debug mode
        config.put(EchoConfigKeys.COMSCORE_DEBUG_MODE, "1");
        // Send Comscore events to EchoChamber
        //config.put(EchoConfigKeys.COMSCORE_URL, "https://sb.scorecardresearch.com/p2");
        config.put(EchoConfigKeys.COMSCORE_URL, "http://data.bbc.co.uk/v1/analytics-echo-chamber-inbound/comscore");
        config.put(EchoConfigKeys.COMSCORE_SITE, "test");
        //Enable debug mode
        config.put(EchoConfigKeys.ECHO_DEBUG, "1");



        echo = new EchoClient(
                "microbit", //getString(R.string.app_name),   // App Name
                ApplicationType.MOBILE_APP,    // App Type
                "kl.education.microbit.android.page",   // App Countername // ECHO: Label had to be cleaned from: com.samsung.microbit to com.samsung.microbit.page error only thrown in debug mode
                getApplicationContext(),       // The Android Context of your Application
                config
        );
        sendAppStats();
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
    public void sendAppStats()
    {
        if (mshareStat && echo != null){
            Log.d("MBApp", "sendAppStats ");
            HashMap <String, String> eventLabels = new HashMap<String,String>();
            eventLabels.put("name", "kl.education.microbit.appstart.page");
            eventLabels.put("bbc_site", "bitesize");
            eventLabels.put("microbits_paired", Integer.toString(Utils.getTotalPairedMicroBitsFromSystem()));
            eventLabels.put("saved_projects", Integer.toString(Utils.getTotalSavedPrograms()));
            echo.userActionEvent(null, null, eventLabels);
        }
        else
        {
            Log.d("MBApp", "Sharing of stats is disabled by user or Echo not initialised");
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
            eventLabels.put("firmware", firmware);
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