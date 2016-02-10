package com.samsung.microbit.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.Project;
import com.samsung.microbit.service.DfuService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import no.nordicsemi.android.dfu.DfuBaseService;

public class Utils {

    public static final String PREFERENCES_KEY = "Microbit_PairedDevices";
    public static final String PREFERENCES_PAIREDDEV_KEY = "PairedDeviceDevice";
    public static String PREFERENCES_MICROBIT_FIRMWARE = "PairedDeviceDevice";

    public static final String PREFERENCES = "Preferences";
    public static final String PREFERENCES_LIST_ORDER = "Preferences.listOrder";

    public final static int SORTBY_PROJECT_DATE = 0;
    public final static int SORTBY_PROJECT_NAME = 1;
    public final static int ORDERBY_ASCENDING = 0;
    public final static int ORDERBY_DESCENDING = 1;

    private static ConnectedDevice pairedDevice = new ConnectedDevice();

    private static final Object lock = new Object();

    private static AudioManager mAudioManager = null ;
    private static MediaPlayer mMediaplayer = null ;
    private static int originalRingerMode = -1 ;
    private static int originalRingerVolume = -1 ;

    private static Utils instance;




    //private volatile SharedPreferences preferences;

    protected String TAG = "Utils";
    protected boolean debug = BuildConfig.DEBUG;

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
                    mAudioManager = (AudioManager) MBApp.getContext().getSystemService(MBApp.getContext().AUDIO_SERVICE);
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

    public static int getTotalSavedPrograms()
    {
        File sdcardDownloads = Constants.HEX_FILE_DIR;
        int totalPrograms = 0;
        if (sdcardDownloads.exists()) {
            File files[] = sdcardDownloads.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    String fileName = files[i].getName();
                    if (fileName.endsWith(".hex")) {
                        ++totalPrograms;
                    }
                }
            }
        }
        return totalPrograms;
    }

    public static int getTotalPairedMicroBitsFromSystem()
    {
        int totalPairedMicrobits = 0;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled())
        {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice bt : pairedDevices) {
                if (bt.getName().contains("micro:bit")) {
                    ++totalPairedMicrobits;
                }
            }
        }
        return totalPairedMicrobits;
    }
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

    public static String parse(final BluetoothGattCharacteristic characteristic) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        final byte[] data = characteristic.getValue();
        if (data == null)
            return "";
        final int length = data.length;
        if (length == 0)
            return "";

        final char[] out = new char[length * 3 - 1];
        for (int j = 0; j < length; j++) {
            int v = data[j] & 0xFF;
            out[j * 3] = HEX_ARRAY[v >>> 4];
            out[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            if (j != length - 1)
                out[j * 3 + 2] = '-';
        }
        return new String(out);
    }
    private static void dirChecker(String dir) {
        File f = new File(android.os.Environment.DIRECTORY_DOWNLOADS + dir);

        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }

    public static String getLaunchCameraPhotoMode()
    {
        return Constants.LAUNCH_CAMERA_AUDIO_PHOTO;
    }

    public static String getLaunchCameraVideoMode()
    {
        return Constants.LAUNCH_CAMERA_AUDIO_VIDEO;
    }
    public static String geTakingPhotoAudio()
    {
        return Constants.TAKING_PHOTO_AUDIO;
    }
    public static String getRecordingVideoAudio()
    {
        return Constants.RECORDING_VIDEO_AUDIO;
    }

    public static String getPictureTakenAudio()
    {
        return Constants.PICTURE_TAKEN_AUDIO;
    }

    public static String getMaxVideoRecordedAudio()
    {
        return Constants.MAX_VIDEO_RECORDED;
    }

    public static String getFindMyPhoneAudio()
    {
        return Constants.FIND_MY_PHONE_AUDIO;
    }
    public static void playAudio(String filename, final MediaPlayer.OnCompletionListener callBack )
    {
        Resources resources = MBApp.getApp().getApplicationContext().getResources();
        int resID = resources.getIdentifier(filename, "raw", MBApp.getApp().getApplicationContext().getPackageName());
        AssetFileDescriptor afd = resources.openRawResourceFd(resID);
        Utils.preparePhoneToPlayAudio();

        if (mMediaplayer != null)
        {
            mMediaplayer.release();
        }
        mMediaplayer = new MediaPlayer();

        mMediaplayer.reset(); //MBApp.getApp().getApplicationContext(), resID);
        mMediaplayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
        try {
            mMediaplayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mMediaplayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Utils", "playAudio: exception");
            mMediaplayer.release();
            return;
        }
        //Set a callback for completion
        mMediaplayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                restoreAudioMode();
                if (callBack != null)
                    callBack.onCompletion(mp);
            }
        });
        mMediaplayer.start();
    }

    public static boolean inZenMode(Context paramContext)
    {
        /*
         /**
         * Defines global zen mode.  ZEN_MODE_OFF, ZEN_MODE_IMPORTANT_INTERRUPTIONS,

         public static final String ZEN_MODE = "zen_mode";
         public static final int ZEN_MODE_OFF = 0;
         public static final int ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1;
         public static final int ZEN_MODE_NO_INTERRUPTIONS = 2;
         public static final int ZEN_MODE_ALARMS = 3;
        */
        int zenMode = Settings.Global.getInt(paramContext.getContentResolver(), "zen_mode", 0);
        Log.i("MicroBit", "zen_mode : " + zenMode);
        return (zenMode != 0);
    }

    private static void preparePhoneToPlayAudio()
    {
        if (mAudioManager == null)
        {
            mAudioManager = (AudioManager) MBApp.getApp().getApplicationContext().getSystemService(MBApp.getApp().getApplicationContext().AUDIO_SERVICE);
        }
        originalRingerMode = mAudioManager.getRingerMode();
        originalRingerVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);


        if (originalRingerMode != AudioManager.RINGER_MODE_NORMAL) {
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0);
    }

    private static void restoreAudioMode()
    {
        if (mAudioManager == null)
        {
            mAudioManager = (AudioManager) MBApp.getApp().getApplicationContext().getSystemService(MBApp.getApp().getApplicationContext().AUDIO_SERVICE);
        }
        mAudioManager.setRingerMode(originalRingerMode);
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalRingerVolume, 0);
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

    public static String getFileSize(String filePath)
    {
        String size = "0";
        File file = new File(filePath);
        if (file.exists()){
            size = Long.toString(file.length());
        }
        return  size;
    }

    public static void updateFirmwareMicrobit(Context ctx, String firmware)
    {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_MULTI_PROCESS);
        if (pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Log.v("Utils","Updating the microbit firmware");
            ConnectedDevice deviceInSharedPref = new ConnectedDevice();
            Gson gson = new Gson();
            deviceInSharedPref = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
            deviceInSharedPref.mfirmware_version = firmware;
            setPairedMicrobit(ctx,deviceInSharedPref);
        }
    }

    public static void updateConnectionStartTime(Context ctx, long time)
    {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_MULTI_PROCESS);
        if (pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Log.e("Utils","Updating the microbit firmware");
            ConnectedDevice deviceInSharedPref = new ConnectedDevice();
            Gson gson = new Gson();
            deviceInSharedPref = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
            deviceInSharedPref.mlast_connection_time = time;
            setPairedMicrobit(ctx,deviceInSharedPref);
        }
    }

    public static ConnectedDevice getPairedMicrobit(Context ctx) {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_MULTI_PROCESS);

        if (pairedDevice == null) {
            pairedDevice = new ConnectedDevice();
        }

        if (pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            boolean pairedMicrobitInSystemList = false;
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Gson gson = new Gson();
            pairedDevice = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
            //Check if the microbit is still paired with our mobile
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled())
            {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                for (BluetoothDevice bt : pairedDevices) {
                    if (bt.getAddress().equals(pairedDevice.mAddress)) {
                        pairedMicrobitInSystemList = true;
                        break;
                    }
                }
            } else {
                //Do not change the list until the Bluetooth is back ON again
                pairedMicrobitInSystemList = true;
            }

            if (!pairedMicrobitInSystemList){
                Log.e("Utils","The last paired microbit is no longer in the system list. Hence removing it");
                //Return a NULL device & update preferences
                pairedDevice.mPattern = null;
                pairedDevice.mName = null;
                pairedDevice.mStatus = false;
                pairedDevice.mAddress = null;
                pairedDevice.mPairingCode = 0;
                pairedDevice.mfirmware_version = null;
                pairedDevice.mlast_connection_time = 0;

                setPairedMicrobit(ctx,null);
            }

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

        switch (errorCode) {
            case 0x0001:
                return "GATT INVALID HANDLE";

            case 0x0002:
                return "GATT READ NOT PERMIT";

            case 0x0003:
                return "GATT WRITE NOT PERMIT";

            case 0x0004:
                return "GATT INVALID PDU";

            case 0x0005:
                return "GATT INSUF AUTHENTICATION";

            case 0x0006:
                return "GATT REQ NOT SUPPORTED";

            case 0x0007:
                return "GATT INVALID OFFSET";

            case 0x0008:
                return "GATT INSUF AUTHORIZATION";

            case 0x0009:
                return "GATT PREPARE Q FULL";

            case 0x000a:
                return "GATT NOT FOUND";

            case 0x000b:
                return "GATT NOT LONG";

            case 0x000c:
                return "GATT INSUF KEY SIZE";

            case 0x000d:
                return "GATT INVALID ATTR LEN";

            case 0x000e:
                return "GATT ERR UNLIKELY";

            case 0x000f:
                return "GATT INSUF ENCRYPTION";

            case 0x0010:
                return "GATT UNSUPPORT GRP TYPE";

            case 0x0011:
                return "GATT INSUF RESOURCE";

            case 0x0087:
                return "GATT ILLEGAL PARAMETER";

            case 0x0080:
                return "GATT NO RESOURCES";

            case 0x0081:
                return "GATT INTERNAL ERROR";

            case 0x0082:
                return "GATT WRONG STATE";

            case 0x0083:
                return "GATT DB FULL";

            case 0x0084:
                return "GATT BUSY";

            case 0x0085:
                return "Cannot connect to micro:bit (GATT error). Please retry.";

            case 0x0086:
                return "GATT CMD STARTED";

            case 0x0088:
                return "GATT PENDING";

            case 0x0089:
                return "GATT AUTH FAIL";

            case 0x008a:
                return "GATT MORE";

            case 0x008b:
                return "GATT INVALID CFG";

            case 0x008c:
                return "GATT SERVICE STARTED";

            case 0x008d:
                return "GATT ENCRYPTED NO MITM";

            case 0x008e:
                return "GATT NOT ENCRYPTED";

            case 0x01FF:
                return "GATT VALUE OUT OF RANGE";

            case 0x0101:
                return "TOO MANY OPEN CONNECTIONS";

            case 0x00FF:
                return "DFU SERVICE DISCOVERY NOT STARTED";

            case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                return  "GATT CONNECTION CONGESTED";

            case DfuService.ERROR_DEVICE_DISCONNECTED:
                return "micro:bit disconnected";

            case DfuService.ERROR_FILE_NOT_FOUND:
                return "File not found. Please retry.";
            case DfuService.ERROR_FILE_ERROR:
                return "Unable to open file. Please retry.";
            case DfuService.ERROR_FILE_INVALID:
                return "File not a valid HEX. Please retry.";
            case DfuService.ERROR_FILE_IO_EXCEPTION:
                return "Unable to read file";
            case DfuService.ERROR_SERVICE_DISCOVERY_NOT_STARTED:
                return "Bluetooth Discovery not started";
            case DfuService.ERROR_SERVICE_NOT_FOUND:
                return  "Dfu Service not found. Please retry.";
            case DfuService.ERROR_CHARACTERISTICS_NOT_FOUND:
                return "Dfu Characteristics not found. Please retry.";
            case DfuService.ERROR_INVALID_RESPONSE:
                return "Invalid response from micro:bit. Please retry.";
            case DfuService.ERROR_FILE_TYPE_UNSUPPORTED:
                return "Unsupported file type. Please retry.";
            case DfuService.ERROR_BLUETOOTH_DISABLED:
                return "Bluetooth Disabled";

            case DfuService.ERROR_FILE_SIZE_INVALID:
                return "Invalid filesize. Please retry.";

            default:
                if ((DfuBaseService.ERROR_REMOTE_MASK & errorCode) > 0) {
                    switch (errorCode & (~DfuBaseService.ERROR_REMOTE_MASK)) {
                        case DfuBaseService.DFU_STATUS_INVALID_STATE:
                            return "REMOTE DFU INVALID STATE";
                        case DfuBaseService.DFU_STATUS_NOT_SUPPORTED:
                            return "REMOTE DFU NOT SUPPORTED";
                        case DfuBaseService.DFU_STATUS_DATA_SIZE_EXCEEDS_LIMIT:
                            return "REMOTE DFU DATA SIZE EXCEEDS LIMIT";
                        case DfuBaseService.DFU_STATUS_CRC_ERROR:
                            return "REMOTE DFU INVALID CRC ERROR";
                        case DfuBaseService.DFU_STATUS_OPERATION_FAILED:
                            return "REMOTE DFU OPERATION FAILED";
                    }
                }
                return "UNKNOWN (" + errorCode + ")";
        }

    }
}
