package com.samsung.microbit.core;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.AudioPlugin;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.plugin.FeedbackPlugin;
import com.samsung.microbit.plugin.FilePlugin;
import com.samsung.microbit.plugin.InformationPlugin;
import com.samsung.microbit.plugin.RemoteControlPlugin;
import com.samsung.microbit.plugin.TelephonyPlugin;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by frederic.ma on 16/05/2015.
 */
public class PluginInterface  extends CordovaPlugin {

    private static PluginInterface instance = null;

    private static CallbackContext callback = null;//callback used to call into JS from Java side

    IntentFilter broadcastIntentFilter;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleBLENotification(context, intent);
        }
    };

    private void handleBLENotification(Context context, Intent intent) {

        JSONObject parameters = new JSONObject();
        try {
            parameters.put("type", "button_press");
            parameters.put("source", "left");
            PluginInterface.getInstance().dispatchToJs(parameters);

        } catch (JSONException e) {
            Log.e("onMicrobitEvent", e.toString());
        }
    }

    public static synchronized PluginInterface getInstance() {
        if (instance == null) {
            instance = new PluginInterface();
        }

        return instance;
    }

    protected void pluginInitialize() {

        getInstance();

        if (broadcastIntentFilter == null) {
            broadcastIntentFilter = new IntentFilter(IPCService.INTENT_MICROBIT_BUTTON_NOTIFICATION);
            MBApp.getContext().registerReceiver(broadcastReceiver, broadcastIntentFilter);
        }
    }

    //TODO: this code should be removed eventually.
    private void simulateMicrobitEvent()
    {
        //This is just to test Java to JS flow as currently this is
        //the only way to simulate an event from microbit.
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Log.v("Timer", "run");
                //TODO: pass relevant parameters based on microbit event
                //for now it is hardcoded
                JSONObject parameters = new JSONObject();
                try {
                    parameters.put("type", "button_press");
                    parameters.put("source", "left");
                    dispatchToJs(parameters);

                } catch (JSONException e) {
                    Log.e("onMicrobitEvent", e.toString());
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 5000);
    }

    //entry point into native cordova plugin from JS
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        if (action.equals("handleMessage")) {
            handleMessage(args.getInt(0), args.getInt(1), args.getString(2), callbackContext);
            return true;
        }
        else if (action.equals("setCallback")) {
            callback = callbackContext;

            return true;
        }
        return false;
    }

    //handle incoming calls from HTML to trigger plugin calls
    private synchronized void handleMessage(final int service, final int action, final String value, final CallbackContext callbackContext)
    {
        Log.v("PluginInterface", "handleMessage");
        switch(service) {
            case PluginService.ALERT:
                AlertPlugin.pluginEntry(webView.getContext(), new CmdArg(action, value));
                break;
            case PluginService.FEEDBACK:
                FeedbackPlugin.pluginEntry(webView.getContext(), new CmdArg(action, value));
                break;
            case PluginService.INFORMATION:
                InformationPlugin.pluginEntry(webView.getContext(), new CmdArg(action, value));
                break;
            case PluginService.AUDIO:
                AudioPlugin.pluginEntry(webView.getContext(), new CmdArg(action, value));
                break;
            case PluginService.REMOTE_CONTROL:
                RemoteControlPlugin.pluginEntry(webView.getContext(), new CmdArg(action, value));
                break;
            case PluginService.TELEPHONY:
                TelephonyPlugin.pluginEntry(webView.getContext(), new CmdArg(action, value));
                break;
            case PluginService.CAMERA:
                CameraPlugin.pluginEntry(webView.getContext(), new CmdArg(action, value));
                break;
            case PluginService.FILE:
                FilePlugin.pluginEntry(webView.getContext(), new CmdArg(action, value));
                break;
        }
        //execute result callback if specified in JS side
        if (callbackContext != null) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 0));
        }
    }

    //TODO: call this function upon receiving BLE messages from Microbit i.e. button press
    /*
    JSONObject parameters = new JSONObject();
    try {
        parameters.put("type", "button_press");
        parameters.put("source", "left");
        dispatchToJs(parameters); // this calls the JS callback in the html file specified by PluginInterface.init
    } catch (JSONException e) {
        Log.e("onMicrobitEvent", e.toString());
    }
    */
    public void dispatchToJs(JSONObject params) {
        if(callback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, params);
            result.setKeepCallback(true);//allows repeat execution of the same callback
            callback.sendPluginResult(result);
        }
    }
}

