package com.samsung.microbit.plugin;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.NameValuePair;
import com.samsung.microbit.service.PluginService;

/**
 * Created by frederic.ma on 10/05/2015.
 */
public class InformationPlugin {
	private static Context mContext = null;

	//Information plugin action
	public static final int ORIENTATION = 0;
	public static final int SHAKE = 1;
	public static final int BATTERY = 2;
	public static final int TEMPERATURE = 3;


	public static void pluginEntry(Context ctx, CmdArg cmd) {
		mContext = ctx;
		boolean register = false;
		if (cmd.getValue() != null) {
			register = cmd.getValue().toLowerCase().equals("on");
		}

		switch (cmd.getCMD()) {
			case Constants.REG_SIGNALSTRENGTH: {
				if (register)
					registerSignalStrength();
				else
					unregisterSignalStrength();
				break;
			}

			case Constants.REG_DEVICEORIENTATION: {
				if (register)
					registerOrientation();
				else
					unregisterOrientation();
				break;
			}

			case Constants.REG_DEVICEGESTURE: {
				if (register)
					registerShake();
				else
					unregisterShake();
				break;
			}

			case Constants.REG_BATTERYSTRENGTH: {
				if (register)
					registerBattery();
				else
					unregisterBattery();
				break;
			}

			case Constants.REG_TEMPERATURE: {
				if (register)
					registerTemperature();
				else
					unregisterTemperature();
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


	public static void sendToBle(int value) {

		NameValuePair[] args = new NameValuePair[4];
		args[0] = new NameValuePair(IPCMessageManager.BUNDLE_SERVICE_GUID, Constants.EVENT_SERVICE.toString());
		args[1] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_GUID, Constants.ES_MICROBIT_EVENT.toString());
		args[2] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_VALUE, value);
		args[3] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_TYPE, Constants.FORMAT_UINT32);
		PluginService.instance.sendtoBLEService(IPCMessageManager.MICROBIT_MESSAGE,
			IPCMessageManager.IPC_FUNCTION_WRITE_CHARACTERISTIC, null, args);
	}

	static SensorManager mSensorManager;
	static OrientationEventListener mOrientationListener;
	static int mPreviousOrientation;

	//Signal strength code
	static TelephonyManager mTelephonyManager;
	static SignalStrength mSignalStrength;
	static PhoneStateListener mPhoneListener = new PhoneStateListener() {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			mSignalStrength = signalStrength;
			updateSignalStrength();
		}
	};

	static private final void updateSignalStrength() {
		int level = 0;
		if (!isCdma()) {
			int asu = mSignalStrength.getGsmSignalStrength();
			// ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
			// asu = 0 (-113dB or less) is very weak
			// signal, its better to show 0 bars to the user in such cases.
			// asu = 99 is a special case, where the signal strength is unknown.
			if (asu <= 2 || asu == 99) level = Constants.SAMSUNG_SIGNAL_STRENGTH_EVT_NO_BAR;
			else if (asu >= 12) level = Constants.SAMSUNG_SIGNAL_STRENGTH_EVT_FOUR_BAR;
			else if (asu >= 8) level = Constants.SAMSUNG_SIGNAL_STRENGTH_EVT_THREE_BAR;
			else if (asu >= 5) level = Constants.SAMSUNG_SIGNAL_STRENGTH_EVT_TWO_BAR;
			else level = Constants.SAMSUNG_SIGNAL_STRENGTH_EVT_ONE_BAR;
		} else {
			level = getCdmaLevel();
		}

		sendToBle(Constants.makeMicroBitValue(Constants.SAMSUNG_SIGNAL_STRENGTH_ID, level));

		//CmdArg cmd = new CmdArg(InformationPlugin.SIGNAL, "SignalStrength " + level);
		//InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	static private int getCdmaLevel() {
		final int cdmaDbm = mSignalStrength.getCdmaDbm();
		final int cdmaEcio = mSignalStrength.getCdmaEcio();
		int levelDbm = 0;
		int levelEcio = 0;
		if (cdmaDbm >= -75) levelDbm = 4;
		else if (cdmaDbm >= -85) levelDbm = 3;
		else if (cdmaDbm >= -95) levelDbm = 2;
		else if (cdmaDbm >= -100) levelDbm = 1;
		else levelDbm = 0;
		// Ec/Io are in dB*10
		if (cdmaEcio >= -90) levelEcio = 4;
		else if (cdmaEcio >= -110) levelEcio = 3;
		else if (cdmaEcio >= -130) levelEcio = 2;
		else if (cdmaEcio >= -150) levelEcio = 1;
		else levelEcio = 0;
		return (levelDbm < levelEcio) ? levelDbm : levelEcio;
	}

	static private boolean isCdma() {
		return (mSignalStrength != null) && !mSignalStrength.isGsm();
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

	static TemperatureListener mTemperatureListener;

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
						mSwingCount = 0;
					}
				} else {
					mSwingCount = 0;
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

	static ShakeEventListener mShakeListener;
	static BroadcastReceiver mBatteryReceiver;
	static int mPreviousBatteryPct;

	static {
		mOrientationListener = null;
		mTemperatureListener = null;
		mShakeListener = null;
		mBatteryReceiver = null;
		mTelephonyManager = null;
		mPreviousBatteryPct = 0;
		mPreviousOrientation = -1;
	}

	/*
	 * ORIENTATION
	 */
	public static void registerOrientation() {
		if (mOrientationListener != null)
			return;

		mOrientationListener = new OrientationEventListener(mContext,
			SensorManager.SENSOR_DELAY_NORMAL) {
			static private final int INTERVAL = 2000;//TODO adjust
			private long previousTime = 0;

			@Override
			public void onOrientationChanged(int orientation) {   //we use orientation listener as a callback mechanism
				//but in fact we do not use the given orientation value (angle)
				long currentTime = System.currentTimeMillis();
				long deltaTime = currentTime - previousTime;

				if (deltaTime > INTERVAL)//uses interval to avoid spamming client
				{
					previousTime = currentTime;
					Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
					Point size = new Point();
					display.getRealSize(size);

					orientation = Constants.SAMSUNG_DEVICE_ORIENTATION_PORTRAIT;
					if (size.y < size.x) {
						orientation = Constants.SAMSUNG_DEVICE_ORIENTATION_LANDSCAPE;
					}

					if (mPreviousOrientation != orientation) {

						//notify BLE client
						//CmdArg cmd = new CmdArg(InformationPlugin.ORIENTATION, "Device orientation " + orientation);
						//InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);

						sendToBle(Constants.makeMicroBitValue(Constants.SAMSUNG_DEVICE_INFO_ID, (orientation & 0x01)));

						mPreviousOrientation = orientation;
					}
				}
			}
		};

		if (mOrientationListener.canDetectOrientation() == true) {
			mOrientationListener.enable();

			//CmdArg cmd = new CmdArg(0, "Registered Orientation.");
			//InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
		} else {
			mOrientationListener.disable();
		}
	}

	public static void unregisterOrientation() {
		if (mOrientationListener == null)
			return;

		mOrientationListener.disable();
		mOrientationListener = null;

		CmdArg cmd = new CmdArg(0, "Unregistered Orientation.");
		InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	public static boolean isOrientationRegistered() {
		return mOrientationListener != null;
	}

	/*
	 * registerSignalStrength
	 */
	public static void registerSignalStrength() {
		if (mTelephonyManager != null) {
			return;
		}

		mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		CmdArg cmd = new CmdArg(0, "Registered Signal Strength.");
		InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	public static void unregisterSignalStrength() {
		if (mTelephonyManager == null) {
			return;
		}

		mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
		mTelephonyManager = null;
		CmdArg cmd = new CmdArg(0, "Unregistered Signal Strength.");
		InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	public static boolean isSignalStrengthRegistered() {
		return mTelephonyManager != null;
	}

	public static void registerTemperature() {
		if (mTemperatureListener != null)
			return;

		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

		Sensor temperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		if (temperatureSensor == null)//no temperature sensor
			return;

		mTemperatureListener = new TemperatureListener();
		mSensorManager.registerListener(mTemperatureListener, temperatureSensor,
			SensorManager.SENSOR_DELAY_NORMAL);

		CmdArg cmd = new CmdArg(0, "Registered Temperature.");
		InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	public static void unregisterTemperature() {
		if (mTemperatureListener == null)
			return;

		mSensorManager.unregisterListener(mTemperatureListener);
		mTemperatureListener = null;

		CmdArg cmd = new CmdArg(0, "Unregistered Temperature.");
		InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	public static boolean isTemperatureRegistered() {
		return mTemperatureListener != null;
	}

	public static void registerShake() {
		if (mShakeListener != null)
			return;

		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

		mShakeListener = new ShakeEventListener();
		mSensorManager.registerListener(mShakeListener, mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
			SensorManager.SENSOR_DELAY_NORMAL);

		CmdArg cmd = new CmdArg(0, "Registered Shake.");
		InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	public static void unregisterShake() {
		if (mShakeListener == null)
			return;

		mSensorManager.unregisterListener(mShakeListener);
		mShakeListener = null;

		CmdArg cmd = new CmdArg(0, "Unregistered Shake.");
		InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	public static boolean isShakeRegistered() {
		return mShakeListener != null;
	}

	public static void registerBattery() {
		if (mBatteryReceiver != null)
			return;

		mBatteryReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				int batteryPct = (int) (level / (float) scale * 100);

				if (batteryPct != mPreviousBatteryPct) {
					CmdArg cmd = new CmdArg(InformationPlugin.BATTERY, "Battery level " + batteryPct);
					InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);

					mPreviousBatteryPct = batteryPct;
				}
			}
		};

		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		mContext.registerReceiver(mBatteryReceiver, filter);

		CmdArg cmd = new CmdArg(0, "Registered Battery.");
		InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	public static void unregisterBattery() {
		if (mBatteryReceiver == null)
			return;

		mContext.unregisterReceiver(mBatteryReceiver);
		mBatteryReceiver = null;

		CmdArg cmd = new CmdArg(0, "Unregistered Battery.");
		InformationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
	}

	public static boolean isBatteryRegistered() {
		return mBatteryReceiver != null;
	}
}
