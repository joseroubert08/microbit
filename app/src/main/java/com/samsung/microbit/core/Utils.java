package com.samsung.microbit.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.Project;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Utils {

	public static final String PREFERENCES_KEY = "Microbit_PairedDevices";
	public static final String PREFERENCES_NAME_KEY = "PairedDeviceName";  // To be removed
	public static final String PREFERENCES_ADDRESS_KEY = "PairedDeviceAddress"; // To be removed
	public static final String PREFERENCES_PAIREDDEV_KEY = "PairedDeviceDevice";

	public static final String PREFERENCES = "Preferences";
	public static final String PREFERENCES_LIST_ORDER = "Preferences.listOrder";

	public final static int SORTBY_PROJECT_NAME = 0;
	public final static int SORTBY_PROJECT_TIMESTAMP = 1;
	public final static int ORDERBY_ASCENDING = 0;
	public final static int ORDERBY_DESCENDING = 1;

	private static ConnectedDevice pairedDevice = new ConnectedDevice();

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
					pairedDevice = new ConnectedDevice();
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

	public static int findProgramsAndPopulate(HashMap<String, String> prettyFileNameMap, List<Project> list) {
		File sdcardDownloads = Constants.HEX_FILE_DIR;
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
						list.add(new Project(parsedFileName, files[i].getAbsolutePath(), files[i].lastModified(), null, false));

					++totalPrograms;
				}
			}
		}

		return totalPrograms;
	}

	public static List<Project> sortProjectList(List<Project> list, final int orderBy, final int sortOrder) {

		Project[] projectArray = list.toArray(new Project[0]);
		Comparator<Project> comparator = new Comparator<Project>() {
			@Override
			public int compare(Project lhs, Project rhs) {
				int rc;
				switch (orderBy) {
					case SORTBY_PROJECT_NAME:
						// byName
						rc = lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
						break;

					default:
						// byTimestamp
						if (lhs.timestamp < rhs.timestamp) {
							rc = 1;
						} else if (lhs.timestamp > rhs.timestamp) {
							rc = -1;
						} else {
							rc = lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
						}

						break;
				}

				if (sortOrder != ORDERBY_ASCENDING) {
					rc = 0 - rc;
				}

				return rc;
			}
		};

		Arrays.sort(projectArray, comparator);
		list.clear();
		list.addAll(Arrays.asList(projectArray));
		return list;
	}

	public static boolean deleteFile(String filePath) {
		File fdelete = new File(filePath);
		if (fdelete.exists()) {
			if (fdelete.delete()) {
				Log.d("MicroBit", "file Deleted :" + filePath);
				return true;
			} else {
				Log.d("MicroBit", "file not Deleted :" + filePath);
			}
		}

		return false;
	}

	public static ConnectedDevice getPairedMicrobit(Context ctx) {
		SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_MULTI_PROCESS);

		if (pairedDevice == null) {
			pairedDevice = new ConnectedDevice();
		}

		if (pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
			String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
			Log.d("MicroBit", "ConnectedDevice - pairedDeviceString - " + pairedDeviceString);

			Gson gson = new Gson();
			pairedDevice = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
		} else {
			pairedDevice.mPattern = null;
			pairedDevice.mName = null;
		}


		Log.d("MicroBit", "ConnectedDevice - pairedDevice.mPattern - " + pairedDevice.mPattern);
		Log.d("MicroBit", "ConnectedDevice - pairedDevice.mName - " + pairedDevice.mName);

		return pairedDevice;
	}

	public static void setPairedMicrobit(Context ctx, ConnectedDevice newDevice) {

		SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = pairedDevicePref.edit();
		if (newDevice == null) {
			editor.clear();
		} else {
			Gson gson = new Gson();
			String jsonActiveDevice = gson.toJson(newDevice);
			editor.putString(PREFERENCES_PAIREDDEV_KEY, jsonActiveDevice);
		}

		editor.commit();
	}

	public static int getListOrderPrefs(Context ctx) {
		SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_MULTI_PROCESS);

		int i = 0;
		if (prefs != null) {
			i = prefs.getInt(PREFERENCES_LIST_ORDER, 0);
		}

		return i;
	}

	public static void setListOrderPrefs(Context ctx, int orderPref) {

		SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(PREFERENCES_LIST_ORDER, orderPref);
		editor.commit();
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
