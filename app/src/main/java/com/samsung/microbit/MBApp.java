package com.samsung.microbit;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

public class MBApp extends Application {

    private static Context mContext;

    private static MBApp app = null;

    private Typeface mTypeface;
    private Typeface mBoldTypeface;
    private Typeface mRobotoTypeface;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        initTypefaces();
        Log.d("MBApp", "App Created");
    }

    /**
     * Creates font styles from an asset.
     */
    private void initTypefaces() {
        mTypeface = Typeface.createFromAsset(getAssets(), "fonts/GT-Walsheim.otf");
        mBoldTypeface = Typeface.createFromAsset(getAssets(), "fonts/GT-Walsheim-Bold.otf");
        mRobotoTypeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
    }

    public Typeface getTypeface(){
        return mTypeface;
    }

    public Typeface getTypefaceBold(){
        return mBoldTypeface;
    }

    public Typeface getRobotoTypeface() {
        return mRobotoTypeface;
    }

    public static Context getContext() { return MBApp.mContext; }

    public static void setContext(Context ctx) { MBApp.mContext = ctx; }

    public static MBApp getApp(){ return app;}
}