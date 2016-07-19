package com.samsung.microbit.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.samsung.microbit.MBApp;

/**
 * Provides functionality to work with preferences.
 */
public class PreferenceUtils {
    public static final String PREFERENCES = "Preferences";
    public static final String PREFERENCES_LIST_ORDER = "Preferences.listOrder";

    private PreferenceUtils() {
    }

    /**
     * Allows to get a list sort order from shared preferences, for example, to sort
     * the list of projects on the Flash screen. If nothing found then
     * it return the default value - 0.
     *
     * @return Sort order.
     */
    public static int getListSortOrder() {
        SharedPreferences prefs = MBApp.getApp().getSharedPreferences(PREFERENCES, Context.MODE_MULTI_PROCESS);

        int i = 0;
        if (prefs != null) {
            i = prefs.getInt(PREFERENCES_LIST_ORDER, 0);
        }

        return i;
    }

    /**
     * Allows to save current sort order value to shared preferences.
     *
     * @param listSortOrder Current sort order value.
     */
    public static void setListSortOrder(int listSortOrder) {
        SharedPreferences prefs = MBApp.getApp().getSharedPreferences(PREFERENCES, Context
                .MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREFERENCES_LIST_ORDER, listSortOrder);
        editor.apply();
    }
}
