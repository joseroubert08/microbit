package com.samsung.microbit.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.utils.UnpackUtils;

import java.util.HashMap;

import uk.co.bbc.echo.EchoClient;
import uk.co.bbc.echo.EchoConfigKeys;
import uk.co.bbc.echo.enumerations.ApplicationType;

/**
 * Created by kkulendiran on 12/02/16.
 */
public class EchoClientManager {

    private static EchoClientManager instance = null;

    public static synchronized EchoClientManager getInstance(MBApp mbApp) {
        if (instance == null) {
            instance = new EchoClientManager(mbApp);
        }
        return instance;
    }

    private EchoClient echo;
    private Context context;

    private boolean shareStatistic = false;

    private EchoClientManager(MBApp app) {
        this.context = app;

        SharedPreferences sharedPreferences = context.getSharedPreferences("com.samsung.microbit", Context
                 .MODE_PRIVATE);
        if (sharedPreferences != null) {
            shareStatistic = sharedPreferences.getBoolean(context.getString(R.string.prefs_share_stats_status), true);
        }

        initialiseEcho();
    }

    // Using release 9.2.1 from https://github.com/bbc/echo-client-android/releases/tag/9.2.1
    public void initialiseEcho() {
        HashMap<String, String> config = new HashMap<String, String>();
        //Use ECHO_TRACE value for searching in echo chamber
        config.put(EchoConfigKeys.ECHO_TRACE, "microbit_android_app");
        //Use CS debug mode
        config.put(EchoConfigKeys.COMSCORE_DEBUG_MODE, "1");
        // Send Comscore events to EchoChamber
        config.put(EchoConfigKeys.COMSCORE_URL, "https://sb.scorecardresearch.com/p2");
        //Enable debug mode
        config.put(EchoConfigKeys.ECHO_DEBUG, "1");

        echo = new EchoClient(
                "microbit", //getString(R.string.app_name),   // App Name
                ApplicationType.MOBILE_APP,    // App Type
                "kl.education.microbit.android.page",   // App Countername // ECHO: Label had to be cleaned from: com.samsung.microbit to com.samsung.microbit.page error only thrown in debug mode
                context,       // The Android Context of your Application
                config
        );
    }

    public void setShareStatistic(boolean shareStatistic) {
        this.shareStatistic = shareStatistic;
    }

    public EchoClient getEcho() {
        if (shareStatistic) {
            Log.d("EchoClientManager", "Sharing stats is enabled by user");
            return echo;
        } else {
            Log.d("EchoClientManager", "Sharing of stats is disabled by user");
            return null;
        }
    }

    public void sendAppStats() {
        if (shareStatistic && echo != null) {
            Log.d("EchoClientManager", "sendAppStats ");
            HashMap<String, String> eventLabels = new HashMap<String, String>();
            eventLabels.put("name", "kl.education.microbit.appstart.page");
            eventLabels.put("bbc_site", "bitesize");
            eventLabels.put("microbits_paired", Integer.toString(BluetoothUtils.getTotalPairedMicroBitsFromSystem()));
            eventLabels.put("saved_projects", Integer.toString(UnpackUtils.getTotalSavedPrograms()));
            echo.userActionEvent(null, null, eventLabels);
        } else {
            Log.d("EchoClientManager", "Sharing of stats is disabled by user or Echo not initialised");
        }
    }

    public void sendViewEventStats(String viewEventString) {
        if (shareStatistic && echo != null) {
            Log.d("EchoClientManager", "sendViewEventStats " + viewEventString);
            String counterName = MBApp.getContext().getString(R.string.stats_view_name, viewEventString);
            HashMap<String, String> eventLabels = new HashMap<String, String>();
            eventLabels.put("bbc_site", "bitesize");
            echo.viewEvent(counterName, eventLabels);
        } else {
            Log.d("EchoClientManager", "Sharing of stats is disabled by user or Echo not initialised");
        }
    }

    public void sendFlashStats(boolean success, String fileName, String hexsize, String binsize, String firmware) {
        try {
            if (shareStatistic && echo != null) {
                Log.d("EchoClientManager", "sendFlashStats fileName=" + fileName + " hexsize=" + hexsize + "  " +
                         "binsize=" + binsize + " microbit_firmwwareversion= " + firmware);
                HashMap<String, String> eventLabels = new HashMap<String, String>();
                eventLabels.put("action_location", "app");
                eventLabels.put("bbc_site", "bitesize");
                eventLabels.put("hex_file_size", hexsize);
                eventLabels.put("binary_size", binsize);
                eventLabels.put("firmware", firmware);
                if (success) {
                    echo.userActionEvent("success", "hex-file-flash", eventLabels);
                } else {
                    echo.userActionEvent("fail", "hex-file-flash", eventLabels);
                }
            } else {
                Log.d("EchoClientManager", "Sharing of stats is disabled by user or Echo not initialised");
            }
        } catch (RuntimeException e) {
            Log.e("EchoClientManager", "Sending stats exception " + e.getMessage());
        }
    }

    public void sendNavigationStats(String location, String button) {
        try {
            if (shareStatistic && echo != null) {
                HashMap<String, String> eventLabels = new HashMap<String, String>();
                eventLabels.put("action_location", location);
                eventLabels.put("button", button);
                eventLabels.put("bbc_site", "bitesize");
                echo.userActionEvent("click", "navigate", eventLabels);
            } else {
                Log.d("EchoClientManager", "Sharing of stats is disabled by user or Echo not initialised");
            }
        } catch (RuntimeException e) {
            Log.e("EchoClientManager", "Sending stats exception " + e.getMessage());
        }
    }

    public void sendStatSharing(boolean enable) {
        try {
            if (echo != null) {
                HashMap<String, String> eventLabels = new HashMap<String, String>();
                eventLabels.put("bbc_site", "bitesize");
                if (enable) {
                    echo.userActionEvent("opt-in", "stats-tracking", eventLabels);
                } else {
                    echo.userActionEvent("opt-out", "stats-tracking", eventLabels);
                }
            } else {
                Log.d("EchoClientManager", "Sharing of stats is disabled by user or Echo not initialised");
            }
        } catch (RuntimeException e) {
            Log.e("EchoClientManager", "Sending stats exception " + e.getMessage());
        }
    }

    public void sendPairingStats(boolean paired, String firmware) {
        try {
            if (echo != null) {
                HashMap<String, String> eventLabels = new HashMap<String, String>();
                eventLabels.put("bbc_site", "bitesize");
                if (paired) {
                    eventLabels.put("firmware", firmware);
                    echo.userActionEvent("success", "pair", eventLabels);
                } else {
                    echo.userActionEvent("fail", "pair", eventLabels);
                }
            } else {
                Log.d("EchoClientManager", "Sharing of stats is disabled by user or Echo not initialised");
            }
        } catch (RuntimeException e) {
            Log.e("EchoClientManager", "Sending stats exception " + e.getMessage());
        }
    }

    public void sendConnectStats(Constants.CONNECTION_STATE connectionState, String firmware, String duration) {
        try {
            if (echo != null) {
                HashMap<String, String> eventLabels = new HashMap<String, String>();
                eventLabels.put("bbc_site", "bitesize");
                switch (connectionState) {
                    case SUCCESS:
                        Log.d("EchoClientManager", "Sending Connection stats - MSG(SUCCESS) - Firmware = " + firmware);
                        eventLabels.put("firmware", firmware);
                        echo.userActionEvent("success", "connect", eventLabels);
                        break;
                    case FAIL:
                        Log.d("EchoClientManager", "Sending Connection stats - MSG(Failed)");
                        echo.userActionEvent("fail", "connect", null);
                        break;
                    case DISCONNECT:
                        Log.d("EchoClientManager", "Sending Connection stats - MSG(DISCONNECT) - Firmware = " + firmware + " Duration =" + duration);
                        eventLabels.put("firmware", firmware);
                        eventLabels.put("duration", duration);
                        echo.userActionEvent("disconnect", "connectMaybeInit", eventLabels);
                        break;

                }
            } else {
                Log.d("EchoClientManager", "Sharing of stats is disabled by user or Echo not initialised");
            }
        } catch (RuntimeException e) {
            Log.e("EchoClientManager", "Sending stats exception " + e.getMessage());
        }
    }
}
