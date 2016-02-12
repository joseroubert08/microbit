package com.samsung.microbit;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.Log;

import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.Constants;

import java.util.HashMap;

import uk.co.bbc.echo.EchoClient;
import uk.co.bbc.echo.EchoConfigKeys;
import uk.co.bbc.echo.enumerations.ApplicationType;

public class MBApp extends Application {

    private static Context mContext;

    private static MBApp app = null;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        Log.d("MBApp", "App Created");
    }

    public Typeface getTypeface(){
        return Typeface.createFromAsset(getAssets(), "fonts/GT-Walsheim.otf");
    }

    public static Context getContext() { return MBApp.mContext; }

    public static void setContext(Context ctx) { MBApp.mContext = ctx; }

    public static MBApp getApp(){ return app;}
}