package com.samsung.microbit.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.URL;

public class RemoteConfig
{
    private URL mConfigLocation;
    private long mUpdateTime;
    private SharedPreferences mPreferences;
    private Context mContext;
    private static volatile RemoteConfig instance;

    public static RemoteConfig getInstance()
    {
        if (instance == null) {
            synchronized (RemoteConfig.class) {
                if (instance == null) {
                    instance = new RemoteConfig();
                }
            }
        }
        return instance;
    }

}