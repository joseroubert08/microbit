package com.samsung.microbit.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.samsung.microbit.R;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.PreferencesInteraction;
import com.samsung.microbit.model.Utils;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.BLEManager;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.plugin.RemoteControlPlugin;
import com.samsung.microbit.ui.LEDGridActivity;
import com.samsung.microbit.ui.MainActivity;

import java.util.UUID;

public class BLEService extends BLEBaseService {

	public static final UUID BUTTON_1_SERVICE = UUID.fromString("0000a000-0000-1000-8000-00805f9b34fb");
	public static final UUID BUTTON_2_SERVICE = UUID.fromString("0000b000-0000-1000-8000-00805f9b34fb");

	public static final UUID BUTTON_1_CHARACTERISTIC = UUID.fromString("0000a001-0000-1000-8000-00805f9b34fb");
	public static final UUID BUTTON_2_CHARACTERISTIC = UUID.fromString("0000b001-0000-1000-8000-00805f9b34fb");

	public static final UUID CALLBACK_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	public static final String MESSAGE_NAME = "uBIT_BUTTON_PRESS";

	protected String TAG = "BLEService";
	protected boolean debug = true;

	NotificationManager notifyMgr;
	int notificationId = 1010;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public BLEService getService() {
			return BLEService.this;
		}
	}

	protected String getDeviceAddress() {

		logi("getDeviceAddress()");
		final String[] pairedDeviceName = new String[1];
		Utils.getInstance().preferencesInteraction(this, new PreferencesInteraction() {
			@Override
			public void interAct(SharedPreferences preferences) {
				String s = preferences.getString(Utils.PREFERENCES_ADDRESS_KEY, null);
				logi("getDeviceAddress() :: s = " + s);
				pairedDeviceName[0] = s;
			}
		});

		if (pairedDeviceName[0] == null) {
			setNotification(true);
		}

		return pairedDeviceName[0];
	}

	protected void startupConnection() {

		logi("startupConnection()");
		boolean success = true;
		if (connect() == 0) {
			logi("startupConnection() :: connect() == 0");
			if (discoverServices() == 0) {

				logi("startupConnection() :: discoverServices() == 0");
				if (registerNotifications(true)) {
					setNotification(false);
				} else {
					success = false;
				}
			} else {
				success = false;
				logi("startupConnection() :: discoverServices() != 0");
			}
		} else {
			success = false;
		}

		if (success) {
			if (serviceConnectionPluginService == null) {
				logi("startupConnection() :: serviceConnectionPluginService == null");
				connectWithServer();
			}
		} else {
			if (bleManager != null) {
				bleManager.reset(true);
			}
		}

		logi("startupConnection() :: end");
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy called");
	}

	public boolean registerNotifications(boolean enable) {

		logi("registerNotifications()");
		BluetoothGattService button1s = getService(BUTTON_1_SERVICE);
		if (button1s == null) {
			return false;
		}

		BluetoothGattCharacteristic button1c = button1s.getCharacteristic(BUTTON_1_CHARACTERISTIC);
		if (button1c == null) {
			return false;
		}

		BluetoothGattDescriptor button1d = button1c.getDescriptor(CALLBACK_DESCRIPTOR);
		if (button1d == null) {
			return false;
		}

		enableCharacteristicNotification(button1c, button1d, enable);


		BluetoothGattService button2s = getService(BUTTON_2_SERVICE);
		if (button2s == null) {
			return false;
		}

		BluetoothGattCharacteristic button2c = button2s.getCharacteristic(BUTTON_2_CHARACTERISTIC);
		if (button2c == null) {
			return false;
		}

		BluetoothGattDescriptor button2d = button2c.getDescriptor(CALLBACK_DESCRIPTOR);
		if (button2d == null) {
			return false;
		}

		enableCharacteristicNotification(button2c, button2d, enable);
		logi("registerNotifications() : done");
		return true;
	}

	@Override
	protected void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

		int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		if (value == 0) {
			return;
		}

		// Bit 5 (1)==button down (0)==button up
		// bit 6 (1)==button 2.
		if (BUTTON_2_CHARACTERISTIC.equals(characteristic.getUuid())) {
			value |= 0x020;
		}

		logi("onCharacteristicChanged value = " + value);
		sendMessage(value);
	}

	@Override
	protected void handleUnexpectedConnectionEvent(int event) {

		logi("handleDisconnection() :: event = " + event);
		if ((event & BLEManager.BLE_CONNECTED) != 0) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					logi("handleDisconnection() :: BLE_CONNECTED");
					discoverServices();
					registerNotifications(true);
					setNotification(false);
				}
			}).start();

		} else if (event == BLEManager.BLE_DISCONNECTED) {
			logi("handleDisconnection() :: BLE_DISCONNECTED");
			setNotification(true);
		}
	}

	private void setNotification(boolean enable) {

		logi("setNotification() :: enable = " + enable);
		NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		int notificationId = 001;
		if (enable) {
			NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.un_connected)
					.setContentTitle("Micro:bit companion")
					.setContentText("No micro:bit connected");

			Intent intent = new Intent(this, LEDGridActivity.class);
			intent.putExtra(LEDGridActivity.INTENT_IS_FOR_FLASHING, false);
			PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(resultPendingIntent);
			notifyMgr.notify(notificationId, mBuilder.build());
		} else {
			notifyMgr.cancel(notificationId);
		}
	}

	// ######################################################################

	private boolean mIsRemoteControlPlay = false;
	private Messenger messengerPluginService = null;
	private Messenger mClientMessenger = null;
	private ServiceConnection serviceConnectionPluginService = null;
	private IncomingHandler messageHandler = null;
	private HandlerThread messageHandlerThread = null;
	private boolean isBoundToPluginService = false;

	void sendMessage(int buttonPressed) {

		if (buttonPressed == 0) {
			return;
		}

		if ((buttonPressed & 0x010) == 0) {
			return;
		}

		Log.i(TAG, "### uBit Button detected for button = " + buttonPressed);
		int msgService = PluginService.ALERT;
		CmdArg cmd = null;
		switch (buttonPressed) {
			case 0x011:
				msgService = PluginService.REMOTE_CONTROL;
				mIsRemoteControlPlay = !mIsRemoteControlPlay;
				cmd = new CmdArg(mIsRemoteControlPlay ? RemoteControlPlugin.PLAY : RemoteControlPlugin.PAUSE, "");
				break;

			case 0x012:
				msgService = PluginService.CAMERA;
				cmd = new CmdArg(CameraPlugin.LAUNCH_CAMERA_FOR_PIC, "");
				break;

			case 0x014:
			case 0x018:
				cmd = new CmdArg(AlertPlugin.FINDPHONE, "");
				break;

			case 0x031:
				msgService = PluginService.REMOTE_CONTROL;
				cmd = new CmdArg(RemoteControlPlugin.NEXT_TRACK, "");
				break;

			case 0x032:
				msgService = PluginService.CAMERA;
				cmd = new CmdArg(CameraPlugin.TAKE_PIC, "");
				break;

			case 0x034:
			case 0x038:
				cmd = new CmdArg(AlertPlugin.VIBRATE, "500");
				break;
		}

		if (cmd != null) {
			sendCommand(msgService, cmd);
		}
	}

	class IncomingHandler extends Handler {
		public IncomingHandler(HandlerThread thr) {
			super(thr.getLooper());
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	}

	public void connectWithServer() {
		messageHandlerThread = new HandlerThread("BLEReceiverThread");
		messageHandlerThread.start();
		messageHandler = new IncomingHandler(messageHandlerThread);
		mClientMessenger = new Messenger(messageHandler);

		serviceConnectionPluginService = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				isBoundToPluginService = true;
				messengerPluginService = new Messenger(service);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				isBoundToPluginService = false;
				serviceConnectionPluginService = null;
			}
		};

		Intent mIntent = new Intent();
		mIntent.setAction(PluginService.class.getName());
		mIntent = MainActivity.createExplicitFromImplicitIntent(getApplicationContext(), mIntent);
		bindService(mIntent, serviceConnectionPluginService, Context.BIND_AUTO_CREATE);
	}

	public void sendCommand(int mbsService, CmdArg cmd) {
		if (messengerPluginService != null) {
			Message msg = Message.obtain(null, mbsService);
			Bundle bundle = new Bundle();
			bundle.putInt(PluginService.BUNDLE_DATA, cmd.getCMD());
			bundle.putString(PluginService.BUNDLE_VALUE, cmd.getValue());
			msg.setData(bundle);
			msg.replyTo = mClientMessenger;
			try {
				messengerPluginService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	// ######################################################################
}
