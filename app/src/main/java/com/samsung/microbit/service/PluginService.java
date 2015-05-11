package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.widget.Toast;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.AlertPlugin;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class PluginService extends Service {

    Messenger mMessenger = new Messenger(new IncomingHandler());

   //MBS Services
    static final int ALERT = 0;
    static final int FEEDBACK = 1;

    /**
     * Handler of incoming messages from BLEListner.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            switch (msg.what) {
                case ALERT:
                    CmdArg cmd = new CmdArg(data.getInt("cmd"), data.getString("value"));
                    AlertPlugin.pluginEntry(PluginService.this,cmd);
                    break;
                case FEEDBACK:
                    //TODO
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Plugin Service Started", Toast.LENGTH_SHORT).show();
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Plugin Service Destroyed", Toast.LENGTH_SHORT).show();
    }
}
