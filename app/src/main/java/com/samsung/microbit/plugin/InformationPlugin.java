package com.samsung.microbit.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.RegistrationIds;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

public class InformationPlugin {
    //Information plugin action
    public static final int ORIENTATION = 0;
    public static final int SHAKE = 1;
    public static final int BATTERY = 2;
    public static final int TEMPERATURE = 3;


    public static void pluginEntry(Context ctx, CmdArg cmd) {
        boolean register = false;
        if (cmd.getValue() != null) {
            register = cmd.getValue().toLowerCase().equals("on");
        }

        switch (cmd.getCMD()) {
            case RegistrationIds.REG_SIGNALSTRENGTH: {
                if (register) {
                    registerSignalStrength();
                } else {
                    unregisterSignalStrength();
                }
                break;
            }

            case RegistrationIds.REG_DEVICEORIENTATION: {
                if (register) {
                    registerOrientation();
                } else {
                    unregisterOrientation();
                }
                break;
            }

            case RegistrationIds.REG_DEVICEGESTURE: {
                if (register) {
                    registerShake();
                } else {
                    unregisterShake();
                }
                break;
            }

            case RegistrationIds.REG_BATTERYSTRENGTH: {
                if (register) {
                    registerBattery();
                } else {
                    unregisterBattery();
                }
                break;
            }

            case RegistrationIds.REG_TEMPERATURE: {
                if (register) {
                    registerTemperature();
                } else {
                    unregisterTemperature();
                }
                break;
            }

            case RegistrationIds.REG_DISPLAY: {
                if (register) {
                    registerDisplay();
                } else {
                    unregisterDisplay();
                }
                break;

            }
        }
    }

    public static void sendReplyCommand(int mbsService, CmdArg cmd) {

        // TODO not needed ??? remove
/*		if (PluginService.mClientMessenger != null) {
            Message msg = Message.obtain(null, mbsService);
			Bundle bundle = new Bundle();
			bundle.putInt("cmd", cmd.getCMD());
			bundle.putString("value", cmd.getValue());
			msg.setData(bundle);

			try {
				PluginService.mClientMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
*/
    }


    static SensorManager sSensorManager;
    static SensorEventListener sOrientationListener;
    static int sPreviousOrientation;

    //Signal strength code
    static TelephonyManager sTelephonyManager;
    static PowerManager mPowerManager;

    static int sCurrentSignalStrength = 0;

    static PhoneStateListener sPhoneListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Log.i("InformationPlugin", "onSignalStrengthsChanged: ");

            updateSignalStrength(signalStrength);
        }
    };

    private static void updateSignalStrength(SignalStrength signalStrength) {
        final int level;
        Log.i("InformationPlugin", "updateSignalStrength: ");
        if (!isCdma(signalStrength)) {
            int asu = signalStrength.getGsmSignalStrength();
            // ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
            // asu = 0 (-113dB or less) is very weak
            // signal, its better to show 0 bars to the user in such cases.
            // asu = 99 is a special case, where the signal strength is unknown.
            if (asu <= 2 || asu == 99) level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_NO_BAR;
            else if (asu >= 12) level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_FOUR_BAR;
            else if (asu >= 8) level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_THREE_BAR;
            else if (asu >= 5) level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_TWO_BAR;
            else level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_ONE_BAR;
        } else {
            level = getCdmaLevel(signalStrength);
        }

        if (level != sCurrentSignalStrength) {
            sCurrentSignalStrength = level;
            PluginService.sendMessageToBle(Utils.makeMicroBitValue(EventCategories.SAMSUNG_SIGNAL_STRENGTH_ID, level));
        }
    }

    private static int getCdmaLevel(SignalStrength signalStrength) {
        final int cdmaDbm = signalStrength.getCdmaDbm();
        final int cdmaEcio = signalStrength.getCdmaEcio();

        final int levelDbm;
        if (cdmaDbm >= -75) levelDbm = 4;
        else if (cdmaDbm >= -85) levelDbm = 3;
        else if (cdmaDbm >= -95) levelDbm = 2;
        else if (cdmaDbm >= -100) levelDbm = 1;
        else levelDbm = 0;

        final int levelEcio;
        // Ec/Io are in dB*10
        if (cdmaEcio >= -90) levelEcio = 4;
        else if (cdmaEcio >= -110) levelEcio = 3;
        else if (cdmaEcio >= -130) levelEcio = 2;
        else if (cdmaEcio >= -150) levelEcio = 1;
        else levelEcio = 0;

        return (levelDbm < levelEcio) ? levelDbm : levelEcio;
    }

    private static boolean isCdma(SignalStrength signalStrength) {
        return (signalStrength != null) && !signalStrength.isGsm();
    }

    /*
     *
     */
    static class TemperatureListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float temperature = event.values[0];

            //notify BLE
            CmdArg cmd = new CmdArg(InformationPlugin.TEMPERATURE, "Temperature " + temperature);
            InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    static TemperatureListener sTemperatureListener;

    /*
     * ShakeEventListener
     */
    static class ShakeEventListener implements SensorEventListener {
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

        public ShakeEventListener() {
            mSwingCount = 0;
            lastX = 0;
            lastY = 0;
            lastZ = 0;
        }

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

                        //notify BLE client
                        CmdArg cmd = new CmdArg(InformationPlugin.SHAKE, "Device Shaked");
                        InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
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
    }

    static ShakeEventListener sShakeListener;
    static BroadcastReceiver sBatteryReceiver;
    static BroadcastReceiver sScreenReceiver;
    static int sPreviousBatteryPct;

    static {
        sOrientationListener = null;
        sTemperatureListener = null;
        sShakeListener = null;
        sBatteryReceiver = null;
        sTelephonyManager = null;
        sPreviousBatteryPct = 0;
        sPreviousOrientation = -1;
    }

    /*
     * ORIENTATION
     */
    public static void registerOrientation() {
        if (sOrientationListener != null) {
            return;
        }

        if (sSensorManager == null) {
            sSensorManager = (SensorManager) MBApp.getApp().getSystemService(Context.SENSOR_SERVICE);
        }

        sOrientationListener = new SensorEventListener() {
            int orientation = -1;
            ;

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[1] < 6.5 && event.values[1] > -6.5) {
                    if (orientation != 1) {
                        Log.d("Sensor", "Landscape");
                    }
                    orientation = EventSubCodes.SAMSUNG_DEVICE_ORIENTATION_LANDSCAPE;
                } else {
                    if (orientation != 0) {
                        Log.d("Sensor", "Portrait");
                    }
                    orientation = EventSubCodes.SAMSUNG_DEVICE_ORIENTATION_PORTRAIT;
                }
                if (sPreviousOrientation != orientation) {

                    PluginService.sendMessageToBle(Utils.makeMicroBitValue(EventCategories.SAMSUNG_DEVICE_INFO_ID,
                            orientation));
                    sPreviousOrientation = orientation;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sSensorManager.registerListener(sOrientationListener, sSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    public static void unregisterOrientation() {
        if (sOrientationListener == null) {
            return;
        }

        sSensorManager.unregisterListener(sOrientationListener);
        sOrientationListener = null;
    }

    public static boolean isOrientationRegistered() {
        return sOrientationListener != null;
    }

    /*
     * registerSignalStrength
     */
    public static void registerSignalStrength() {
        Log.i("Information Plugin", "registerSignalStrength 1 ");
        if (sTelephonyManager != null) {
            return;
        }
        Log.i("Information Plugin", "registerSignalStrength 2 ");
        sTelephonyManager = (TelephonyManager) MBApp.getApp().getSystemService(Context.TELEPHONY_SERVICE);
        sTelephonyManager.listen(sPhoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        CmdArg cmd = new CmdArg(0, "Registered Signal Strength.");
        InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
    }

    public static void unregisterSignalStrength() {
        if (sTelephonyManager == null) {
            return;
        }

        sTelephonyManager.listen(sPhoneListener, PhoneStateListener.LISTEN_NONE);
        sTelephonyManager = null;
        CmdArg cmd = new CmdArg(0, "Unregistered Signal Strength.");
        InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
    }

    public static boolean isSignalStrengthRegistered() {
        return sTelephonyManager != null;
    }

    public static void registerTemperature() {
        if (sTemperatureListener != null) {
            return;
        }

        sSensorManager = (SensorManager) MBApp.getApp().getSystemService(Context.SENSOR_SERVICE);

        Sensor temperatureSensor = sSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (temperatureSensor == null)//no temperature sensor
            return;

        sTemperatureListener = new TemperatureListener();
        sSensorManager.registerListener(sTemperatureListener, temperatureSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        CmdArg cmd = new CmdArg(0, "Registered Temperature.");
        InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
    }

    public static void unregisterTemperature() {
        if (sTemperatureListener == null) {
            return;
        }

        sSensorManager.unregisterListener(sTemperatureListener);
        sTemperatureListener = null;

        CmdArg cmd = new CmdArg(0, "Unregistered Temperature.");
        InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
    }

    public static void registerDisplay() {
        if (sScreenReceiver != null) {
            return;
        }

        Log.i("Information Plugin", "registerDisplay() ");
        sScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    PluginService.sendMessageToBle(Utils.makeMicroBitValue(EventCategories.SAMSUNG_DEVICE_INFO_ID,
                            EventSubCodes.SAMSUNG_DEVICE_DISPLAY_OFF));
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    PluginService.sendMessageToBle(Utils.makeMicroBitValue(EventCategories.SAMSUNG_DEVICE_INFO_ID,
                            EventSubCodes.SAMSUNG_DEVICE_DISPLAY_ON));
                }
            }
        };

        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        MBApp.getApp().registerReceiver(sScreenReceiver, screenStateFilter);
    }

    public static void unregisterDisplay() {
        Log.i("Information Plugin", "unregisterDisplay() ");
        if (sScreenReceiver == null) {
            return;
        }

        MBApp.getApp().unregisterReceiver(sScreenReceiver);
        sScreenReceiver = null;
    }

    public static boolean isTemperatureRegistered() {
        return sTemperatureListener != null;
    }

    public static void registerShake() {
        if (sShakeListener != null) {
            return;
        }

        sSensorManager = (SensorManager) MBApp.getApp().getSystemService(Context.SENSOR_SERVICE);

        sShakeListener = new ShakeEventListener();
        sSensorManager.registerListener(sShakeListener, sSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        CmdArg cmd = new CmdArg(0, "Registered Shake.");
        InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
    }

    public static void unregisterShake() {
        if (sShakeListener == null) {
            return;
        }

        sSensorManager.unregisterListener(sShakeListener);
        sShakeListener = null;

        CmdArg cmd = new CmdArg(0, "Unregistered Shake.");
        InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
    }

    public static boolean isShakeRegistered() {
        return sShakeListener != null;
    }

    public static void registerBattery() {
        if (sBatteryReceiver != null) {
            return;
        }

        sBatteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = (int) (level / (float) scale * 100);

                if (batteryPct != sPreviousBatteryPct) {
                    CmdArg cmd = new CmdArg(InformationPlugin.BATTERY, "Battery level " + batteryPct);
                    InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);

                    sPreviousBatteryPct = batteryPct;
                }
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        MBApp.getApp().registerReceiver(sBatteryReceiver, filter);

        CmdArg cmd = new CmdArg(0, "Registered Battery.");
        InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
    }

    public static void unregisterBattery() {
        if (sBatteryReceiver == null) {
            return;
        }

        MBApp.getApp().unregisterReceiver(sBatteryReceiver);
        sBatteryReceiver = null;

        CmdArg cmd = new CmdArg(0, "Unregistered Battery.");
        InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
    }

    public static boolean isBatteryRegistered() {
        return sBatteryReceiver != null;
    }
}
