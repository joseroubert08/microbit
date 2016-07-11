package com.samsung.microbit;

import android.app.Application;
import android.graphics.Typeface;
import android.util.Log;

import com.samsung.microbit.common.ConfigInfo;
import com.samsung.microbit.core.EchoClientManager;

public class MBApp extends Application {

    private static MBApp app = null;

    private Typeface mTypeface;
    private Typeface mBoldTypeface;
    private Typeface mRobotoTypeface;

    private EchoClientManager echoClientManager;

    private ConfigInfo configInfo;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        initTypefaces();
        echoClientManager = EchoClientManager.getInstance(this);

        configInfo = new ConfigInfo(this);
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

    public static MBApp getApp() {
        return app;
    }

    public EchoClientManager getEchoClientManager() {
        return echoClientManager;
    }
    public ConfigInfo getConfigInfo() {
        return configInfo;
    }
}