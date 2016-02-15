package com.samsung.microbit;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

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

    public Typeface getTypefaceBold(){
        return Typeface.createFromAsset(getAssets(), "fonts/GT-Walsheim-Bold.otf");
    }
    public static Context getContext() { return MBApp.mContext; }

    public static void setContext(Context ctx) { MBApp.mContext = ctx; }

    public static MBApp getApp(){ return app;}
}