package com.samsung.microbit.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.samsung.microbit.MBApp;

public class PreferenceUtils {
    public static final String PREFERENCES = "Preferences";
    public static final String PREFERENCES_LIST_ORDER = "Preferences.listOrder";

    private PreferenceUtils() {
    }

    public static int getListOrderPrefs() {
        SharedPreferences prefs = MBApp.getApp().getSharedPreferences(PREFERENCES, Context.MODE_MULTI_PROCESS);

        int i = 0;
        if (prefs != null) {
            i = prefs.getInt(PREFERENCES_LIST_ORDER, 0);
        }

        return i;
    }

    public static void setListOrderPrefs(int orderPref) {
        SharedPreferences prefs = MBApp.getApp().getSharedPreferences(PREFERENCES, Context
                .MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREFERENCES_LIST_ORDER, orderPref);
        editor.apply();
    }
}
