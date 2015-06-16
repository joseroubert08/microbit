package com.samsung.microbit.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.service.BLEService;

public class Utils {

	public static final String PREFERENCES_KEY = "Microbit_PairedDevices";
	public static final String PREFERENCES_NAME_KEY = "PairedDeviceName";
	public static final String PREFERENCES_ADDRESS_KEY = "PairedDeviceAddress";

	private static final Object lock = new Object();
	private static Utils instance;

	//private volatile SharedPreferences preferences;

	protected String TAG = "Utils";
	protected boolean debug = true;

	protected void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	public static Utils getInstance() {

		if (instance == null) {
			synchronized (lock) {
				if (instance == null) {
					Utils u = new Utils();
					instance = u;
				}
			}
		}

		return instance;
	}

	public SharedPreferences getPreferences(Context ctx) {

		logi("getPreferences() :: ctx.getApplicationContext() = " + ctx.getApplicationContext());
		SharedPreferences p = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_MULTI_PROCESS);
		return p;
	}

	public void preferencesInteraction(Context ctx, PreferencesInteraction interAction) {
		synchronized (lock) {

			logi("preferencesInteraction()");
			interAction.interAct(getPreferences(ctx));
		}
	}
}
