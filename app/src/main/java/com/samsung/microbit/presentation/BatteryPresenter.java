package com.samsung.microbit.presentation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.InformationPluginNew;
import com.samsung.microbit.service.PluginService;

public class BatteryPresenter implements Presenter{
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) (level / (float) scale * 100);

            if (batteryPct != previousBatteryPct) {
                if(informationPluginNew != null) {
                    CmdArg cmd = new CmdArg(InformationPluginNew.AlertType.TYPE_BATTERY, "Battery level " + batteryPct);
                    informationPluginNew.sendReplyCommand(PluginService.INFORMATION, cmd);
                }

                previousBatteryPct = batteryPct;
            }
        }
    };

    private MBApp application;
    private InformationPluginNew informationPluginNew;
    private int previousBatteryPct;
    private boolean isRegistered;

    public BatteryPresenter() {
        application = MBApp.getApp();
    }

    public void setInformationPluginNew(InformationPluginNew informationPluginNew) {
        this.informationPluginNew = informationPluginNew;
    }

    @Override
    public void start() {
        if(!isRegistered) {
            isRegistered = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            application.registerReceiver(batteryReceiver, filter);

            if(informationPluginNew != null) {
                CmdArg cmd = new CmdArg(0, "Registered Battery.");
                informationPluginNew.sendReplyCommand(PluginService.INFORMATION, cmd);
            }
        }
    }

    @Override
    public void stop() {
        if(isRegistered) {
            application.unregisterReceiver(batteryReceiver);

            if(informationPluginNew != null) {
                CmdArg cmd = new CmdArg(0, "Unregistered Battery.");
                informationPluginNew.sendReplyCommand(PluginService.INFORMATION, cmd);
            }
            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
