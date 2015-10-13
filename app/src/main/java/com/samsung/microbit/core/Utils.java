package com.samsung.microbit.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.Project;
import com.samsung.microbit.service.DfuService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {

	public static final String PREFERENCES_KEY = "Microbit_PairedDevices";
	public static final String PREFERENCES_NAME_KEY = "PairedDeviceName";  // To be removed
	public static final String PREFERENCES_ADDRESS_KEY = "PairedDeviceAddress"; // To be removed
	public static final String PREFERENCES_PAIREDDEV_KEY = "PairedDeviceDevice";

	public static final String PREFERENCES = "Preferences";
	public static final String PREFERENCES_LIST_ORDER = "Preferences.listOrder";

	public final static int SORTBY_PROJECT_DATE = 0;
	public final static int SORTBY_PROJECT_NAME = 1;
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

	private static void dirChecker(String dir) {
		File f = new File(android.os.Environment.DIRECTORY_DOWNLOADS + dir);

		if (!f.isDirectory()) {
			f.mkdirs();
		}
	}

	public static boolean installSamples() {
		try {
			Resources resources = MBApp.getContext().getResources();
			final int ZIP_INTERNAL = resources.getIdentifier(Constants.ZIP_INTERNAL_NAME, "raw", MBApp.getContext().getPackageName());
			final String OUTPUT_DIR = Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
			Log.d("MicroBit", "Resource id: " + ZIP_INTERNAL);
			AssetFileDescriptor afd = resources.openRawResourceFd(ZIP_INTERNAL);
			//Unzip the file now
			ZipInputStream zin = new ZipInputStream(resources.openRawResource(ZIP_INTERNAL));
			ZipEntry ze = null;
			while ((ze = zin.getNextEntry()) != null) {
				Log.v("MicroBit", "Unzipping " + ze.getName());

				if (ze.isDirectory()) {
					dirChecker(ze.getName());
				} else {
					FileOutputStream fout = new FileOutputStream(OUTPUT_DIR + File.separator + ze.getName());
					for (int c = zin.read(); c != -1; c = zin.read()) {
						fout.write(c);
					}
					zin.closeEntry();
					fout.close();
				}
			}
			zin.close();
		} catch (Resources.NotFoundException e) {
			Log.e("MicroBit", "No internal zipfile present", e);
			return false;
		} catch (Exception e) {
			Log.e("MicroBit", "unzip", e);
			return false;
		}
		return true;
	}

	public static int renameFile(String filePath, String newName) {

		File oldPathname = new File(filePath);
		newName = newName.replace(' ', '_');
		if (!newName.toLowerCase().endsWith(".hex")) {
			newName = newName + ".hex";
		}

		File newPathname = new File(oldPathname.getParentFile().getAbsolutePath(), newName);
		if (newPathname.exists()) {
			return 1;
		}

		if (!oldPathname.exists() || !oldPathname.isFile()) {
			return 2;
		}

		int rc = 3;
		if (oldPathname.renameTo(newPathname)) {
			rc = 0;
		}

		return rc;
	}

	public static List<Project> sortProjectList(List<Project> list, final int orderBy, final int sortOrder) {

		Project[] projectArray = list.toArray(new Project[0]);
		Comparator<Project> comparator = new Comparator<Project>() {
			@Override
			public int compare(Project lhs, Project rhs) {
				int rc;
				switch (orderBy) {

					case SORTBY_PROJECT_DATE:
						// byTimestamp
						if (lhs.timestamp < rhs.timestamp) {
							rc = 1;
						} else if (lhs.timestamp > rhs.timestamp) {
							rc = -1;
						} else {
							rc = lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
						}

						break;

					default:
						// byName
						rc = lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
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
			Gson gson = new Gson();
			pairedDevice = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
		} else {
			pairedDevice.mPattern = null;
			pairedDevice.mName = null;
		}
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

	public static String broadcastGetErrorMessage(int errorCode) {
		String errorMessage;

		switch (errorCode) {
			case DfuService.ERROR_DEVICE_DISCONNECTED:
				errorMessage = "micro:bit disconnected";
				break;
			case DfuService.ERROR_FILE_NOT_FOUND:
				errorMessage = "File not found";
				break;
			/**
			 * Thrown if service was unable to open the file ({@link java.io.IOException} has been thrown).
			 */
			case DfuService.ERROR_FILE_ERROR:
				errorMessage = "Unable to open file";
				break;
			/**
			 * Thrown then input file is not a valid HEX or ZIP file.
			 */
			case DfuService.ERROR_FILE_INVALID:
				errorMessage = "File not a valid HEX";
				break;
			/**
			 * Thrown when {@link java.io.IOException} occurred when reading from file.
			 */
			case DfuService.ERROR_FILE_IO_EXCEPTION:
				errorMessage = "Unable to read file";
				break;
			/**
			 * Error thrown then {@code gatt.discoverServices();} returns false.
			 */
			case DfuService.ERROR_SERVICE_DISCOVERY_NOT_STARTED:
				errorMessage = "Bluetooth Discovery not started";
				break;
			/**
			 * Thrown when the service discovery has finished but the DFU service has not been found. The device does not support DFU of is not in DFU mode.
			 */
			case DfuService.ERROR_SERVICE_NOT_FOUND:
				errorMessage = "Dfu Service not found";
				break;
			/**
			 * Thrown when the required DFU service has been found but at least one of the DFU characteristics is absent.
			 */
			case DfuService.ERROR_CHARACTERISTICS_NOT_FOUND:
				errorMessage = "Dfu Characteristics not found";
				break;
			/**
			 * Thrown when unknown response has been obtained from the target. The DFU target must follow specification.
			 */
			case DfuService.ERROR_INVALID_RESPONSE:
				errorMessage = "Invalid response from micro:bit";
				break;

			/**
			 * Thrown when the the service does not support given type or mime-type.
			 */
			case DfuService.ERROR_FILE_TYPE_UNSUPPORTED:
				errorMessage = "Unsupported file type";
				break;

			/**
			 * Thrown when the the Bluetooth adapter is disabled.
			 */
			case DfuService.ERROR_BLUETOOTH_DISABLED:
				errorMessage = "Bluetooth Disabled";
				break;

			case DfuService.ERROR_FILE_SIZE_INVALID:
				errorMessage = "Invalid filesize";
				break;

			default:
				errorMessage = "Unknown Error";
				break;
		}

		return errorMessage;
	}
}
