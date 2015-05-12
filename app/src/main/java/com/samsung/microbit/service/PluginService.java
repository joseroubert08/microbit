package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.FeedbackPlugin;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class PluginService extends Service {

    Messenger mMessenger = new Messenger(new IncomingHandler());
    public static Messenger mClientMessenger = null;

    //MBS Services
    public static final int ALERT = 0;
    public static final int FEEDBACK = 1;

    /**
     * Handler of incoming messages from BLEListner.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            mClientMessenger = msg.replyTo;
            CmdArg cmd = new CmdArg(data.getInt("cmd"), data.getString("value"));;
            switch (msg.what) {
                case ALERT:
                    AlertPlugin.pluginEntry(PluginService.this,cmd);
                    break;
                case FEEDBACK:
                    FeedbackPlugin.pluginEntry(PluginService.this, cmd);
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
