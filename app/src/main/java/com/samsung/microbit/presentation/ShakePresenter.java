package com.samsung.microbit.presentation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.InformationPluginNew;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

public class ShakePresenter implements Presenter{
    /*
     * ShakeEventListener
     */
    private SensorEventListener shakeEventListener = new SensorEventListener() {
        static final int THRESHOLD_SWING_COUNT = 3;//nb of times swing must be detected before we call it a shake event
        static final int SWING_EVENT_INTERVAL = 100;
        static final int SPEED_THRESHOLD = 500;
        int mSwingCount;
        long lastTime;
        float speed;
        float x, y, z;
        float lastX;
        float lastY;
        float lastZ;

        @Override
        public void onSensorChanged(SensorEvent event) {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastTime;

            if (deltaTime > SWING_EVENT_INTERVAL) {
                lastTime = currentTime;

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / deltaTime * 10000;

                if (speed > SPEED_THRESHOLD) {
                    mSwingCount++;
                    if (mSwingCount >= THRESHOLD_SWING_COUNT) {
                        if(informationPluginNew != null) {
                            //notify BLE client
                            CmdArg cmd = new CmdArg(InformationPluginNew.AlertType.TYPE_SHAKE, "Device Shaked");
                            informationPluginNew.sendReplyCommand(PluginService.INFORMATION, cmd);
                        }

                        PluginService.sendMessageToBle(Utils.makeMicroBitValue(EventCategories.SAMSUNG_DEVICE_INFO_ID,
                                EventSubCodes.SAMSUNG_DEVICE_GESTURE_DEVICE_SHAKEN));
                        mSwingCount = 0;
                    }
                } else {
                    mSwingCount = 0;
                    //PluginService.sendMessageToBle(Constants.makeMicroBitValue(Constants.SAMSUNG_DEVICE_INFO_ID, Constants.SAMSUNG_DEVICE_GESTURE_NONE));
                }

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private InformationPluginNew informationPluginNew;
    private SensorManager sensorManager;
    private boolean isRegistered;

    public ShakePresenter() {
        sensorManager = (SensorManager) MBApp.getApp().getSystemService(Context.SENSOR_SERVICE);
    }

    public void setInformationPluginNew(InformationPluginNew informationPluginNew) {
        this.informationPluginNew = informationPluginNew;
    }

    @Override
    public void start() {
        if (!isRegistered) {
            isRegistered = true;
            sensorManager.registerListener(shakeEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);

            if(informationPluginNew != null) {
                CmdArg cmd = new CmdArg(0, "Registered Shake.");
                informationPluginNew.sendReplyCommand(PluginService.INFORMATION, cmd);
            }
        }
    }

    @Override
    public void stop() {
        if (isRegistered) {
            sensorManager.unregisterListener(shakeEventListener);

            if(informationPluginNew != null) {
                CmdArg cmd = new CmdArg(0, "Unregistered Shake.");
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
