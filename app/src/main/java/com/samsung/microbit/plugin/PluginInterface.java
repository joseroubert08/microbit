package com.samsung.microbit.plugin;

import android.content.res.Configuration;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.samsung.microbit.model.CmdArg;
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

    static private CallbackContext callback = null;//callback used to call into JS from Java side

    protected void pluginInitialize() {
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

                    //simulateMicrobitEvent(); this will trigger repeat button press event
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
            handleMessage(args.getString(0), args.getString(1), callbackContext);
            return true;
        }
        else if (action.equals("setCallback")) {
            callback = callbackContext;

            //TODO: this call should be removed eventually.
            //just there to simulate sending microbit event to JS
            simulateMicrobitEvent();

            return true;
        }
        return false;
    }

    //handle incoming calls from HTML to trigger plugin calls
    private synchronized void handleMessage(final String command, final String value, final CallbackContext callbackContext)
    {
        Log.v("PluginInterface", "handleMessage");
        if (command.equals("ringtone")) {
            AlertPlugin.pluginEntry(webView.getContext(), new CmdArg(AlertPlugin.RINGTONE, value));
        }
        else if (command.equals("sound")) {
            AlertPlugin.pluginEntry(webView.getContext(), new CmdArg(AlertPlugin.SOUND, value));
        }
        else if (command.equals("toast")) {
            AlertPlugin.pluginEntry(webView.getContext(), new CmdArg(AlertPlugin.TOAST, value));
        }
        else if (command.equals("vibrate")) {
            AlertPlugin.pluginEntry(webView.getContext(), new CmdArg(AlertPlugin.VIBRATE, value));
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
    static public void dispatchToJs(JSONObject params) {
        if(callback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, params);
            result.setKeepCallback(true);//allows repeat execution of the same callback
            callback.sendPluginResult(result);
        }
    }
}

