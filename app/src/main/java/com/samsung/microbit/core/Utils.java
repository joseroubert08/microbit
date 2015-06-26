package com.samsung.microbit.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	final public static String BINARY_FILE_NAME = "/sdcard/output.bin";

	public static int findProgramsAndPopulate(HashMap<String, String> prettyFileNameMap, ArrayList<String> list) {
		File sdcardDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		Log.d("MicroBit", "Searching files in " + sdcardDownloads.getAbsolutePath());

		int totalPrograms = 0;
		if (sdcardDownloads.exists()) {
			File files[] = sdcardDownloads.listFiles();
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i].getName();
				if (fileName.endsWith(".hex")) {

					//Beautify the filename
					String parsedFileName;

					int dot = fileName.lastIndexOf(".");
					parsedFileName = fileName.substring(0, dot);
					parsedFileName = parsedFileName.replace('_', ' ');

					if (prettyFileNameMap != null)
						prettyFileNameMap.put(parsedFileName, fileName);

					if (list != null)
						list.add(parsedFileName);
					++totalPrograms;
				}
			}
		}

		if (totalPrograms == 0) {
			if (list != null)
				list.add("No programs found !");
		}

		return totalPrograms;
	}


	// bit position to value mask
	public static int getBitMask(int x) {
		return (0x01 << x);
	}

	// multiple bit positions to value mask
	public static int getBitMask(int[] x) {
		int rc = 0;
		for (int i = 0; i < x.length; i++) {
			rc |= getBitMask(x[i]);
		}

		return rc;
	}

	public int setBit(int v, int x) {
		v |= getBitMask(x);
		return v;
	}

	public int setBits(int v, int[] x) {
		v |= getBitMask(x);
		return v;
	}

	public static int clearBit(int v, int x) {
		v &= ~getBitMask(x);
		return v;
	}

	public static int clearBits(int v, int[] x) {
		v &= ~getBitMask(x);
		return v;
	}

	public static int maskBit(int v, int x) {
		v &= getBitMask(x);
		return v;
	}

	public static int maskBits(int v, int[] x) {
		v &= getBitMask(x);
		return v;
	}
}
