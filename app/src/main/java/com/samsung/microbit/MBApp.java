package com.samsung.microbit;

import android.app.Application;
import android.content.Context;

public class MBApp extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static Context getContext() { return MBApp.mContext; }

    public static void setContext(Context ctx) { MBApp.mContext = ctx; }
}