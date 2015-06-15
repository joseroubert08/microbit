package com.samsung.microbit.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.BLEManager;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.plugin.RemoteControlPlugin;
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

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		int rc = super.onStartCommand(intent, flags, startId);
		if (connect() == 0) {
			if (discoverServices() == 0) {
				registerNotifications(true);
			}
		}

		connectWithServer();
		return rc;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy called");
	}

	public void registerNotifications(boolean enable) {

		BluetoothGattService button1s = getService(BUTTON_1_SERVICE);
		if (button1s == null) {
			return;
		}

		BluetoothGattCharacteristic button1c = button1s.getCharacteristic(BUTTON_1_CHARACTERISTIC);
		if (button1c == null) {
			return;
		}

		BluetoothGattDescriptor button1d = button1c.getDescriptor(CALLBACK_DESCRIPTOR);
		if (button1d == null) {
			return;
		}

		enableCharacteristicNotification(button1c, button1d, enable);


		BluetoothGattService button2s = getService(BUTTON_2_SERVICE);
		if (button2s == null) {
			return;
		}

		BluetoothGattCharacteristic button2c = button2s.getCharacteristic(BUTTON_2_CHARACTERISTIC);
		if (button2c == null) {
			return;
		}

		BluetoothGattDescriptor button2d = button2c.getDescriptor(CALLBACK_DESCRIPTOR);
		if (button2d == null) {
			return;
		}

		enableCharacteristicNotification(button2c, button2d, enable);
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
			logi("handleDisconnection() :: BLE_CONNECTED");
		} else if (event == BLEManager.BLE_DISCONNECTED) {
			logi("handleDisconnection() :: BLE_DISCONNECTED");
		}
	}

	/*
	private void sendMessage(int buttonNumber) {

		Log.i(TAG, "sendMessage");
		final Intent intent = new Intent();
		intent.setAction(MESSAGE_NAME);
		intent.putExtra("buttonPressed", buttonNumber);
		new Thread(new Runnable() {
			@Override
			public void run() {
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
			}
		}).start();
	}
	*/

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
		mIntent.setAction("com.samsung.microbit.service.PluginService");
		mIntent = MainActivity.createExplicitFromImplicitIntent(getApplicationContext(), mIntent);
		bindService(mIntent, serviceConnectionPluginService, Context.BIND_AUTO_CREATE);
	}

	public void sendCommand(int mbsService, CmdArg cmd) {
		if (messengerPluginService != null) {
			Message msg = Message.obtain(null, mbsService);
			Bundle bundle = new Bundle();
			bundle.putInt("cmd", cmd.getCMD());
			bundle.putString("value", cmd.getValue());
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
