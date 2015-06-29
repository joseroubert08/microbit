package com.samsung.microbit.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.samsung.microbit.R;
import com.samsung.microbit.core.BLEManager;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.ui.activity.ConnectActivity;

public class BLEService extends BLEBaseService {

	protected String TAG = "BLEService";
	protected boolean debug = true;

	NotificationManager notifyMgr;
	int notificationId = 1010;

	public BLEService() {
		startIPCListener();
	}

	protected String getDeviceAddress() {

		logi("getDeviceAddress()");
		ConnectedDevice currentDevice = Utils.getPairedMicrobit(this);
		String pairedDeviceName = currentDevice.mAddress;
		if (pairedDeviceName == null) {
			setNotification(false);
		}

		return pairedDeviceName;
	}

	protected void startupConnection() {

		logi("startupConnection()");
		boolean success = true;
		if (connect() == 0) {
			logi("startupConnection() :: connect() == 0");
			if (discoverServices() == 0) {

				logi("startupConnection() :: discoverServices() == 0");
				if (registerNotifications(true)) {
					setNotification(true);
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

		if (!success) {
			if (bleManager != null) {
				bleManager.reset(true);
				setNotification(false);
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
		BluetoothGattService button1s = getService(Constants.EVENT_SERVICE);
		if (button1s == null) {
			logi("registerNotifications() :: not found service " + Constants.EVENT_SERVICE.toString());
			return false;
		}

		BluetoothGattCharacteristic button1c = button1s.getCharacteristic(Constants.ES_CLIENT_EVENT);
		if (button1c == null) {
			logi("registerNotifications() :: not found Characteristic " + Constants.ES_CLIENT_EVENT.toString());
			return false;
		}

		BluetoothGattDescriptor button1d = button1c.getDescriptor(Constants.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
		if (button1d == null) {
			logi("registerNotifications() :: not found descriptor " + Constants.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR.toString());
			return false;
		}

		enableCharacteristicNotification(button1c, button1d, enable);
		logi("registerNotifications() : done");
		return true;
	}

	@Override
	protected void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

		int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
		int eventSrc = value & 0x0ffff;
		if (eventSrc < 1001) {
			return;
		}

		int event = (value >> 16) & 0x0ffff;
		sendMessage(eventSrc, event);
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
					setNotification(true);
				}
			}).start();

		} else if (event == BLEManager.BLE_DISCONNECTED) {
			logi("handleDisconnection() :: BLE_DISCONNECTED");
			setNotification(false);
		}
	}

	private void setNotification(boolean isConnected) {

		logi("setNotification() :: isConnected = " + isConnected);
		NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		int notificationId = 1001;
		if (!isConnected) {
			logi("setNotification() :: !isConnected");
			if (bluetoothAdapter != null) {
				if (!bluetoothAdapter.isEnabled()) {

					logi("setNotification() :: !bluetoothAdapter.isEnabled()");
					if (bleManager != null) {
						bleManager.reset(true);
					}

					bleManager = null;
					bluetoothDevice = null;
				}
			}

			NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ble_connection_off)
					.setContentTitle("Micro:bit companion")
					.setOngoing(true)
					.setContentText("micro:bit Disconnected");

			Intent intent = new Intent(this, ConnectActivity.class);
			PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(resultPendingIntent);
			notifyMgr.notify(notificationId, mBuilder.build());
			sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED, null);
			sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED, null);
		} else {
			NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ble_connection_on)
					.setContentTitle("Micro:bit companion")
					.setOngoing(true)
					.setContentText("micro:bit Connected");


			Intent intent = new Intent(this, ConnectActivity.class);
			PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(resultPendingIntent);
			notifyMgr.notify(notificationId, mBuilder.build());
			sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_CONNECTED, null);
			sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_CONNECTED, null);
		}
	}

	// ######################################################################

	void sendMessage(int eventSrc, int event) {

		int msgService = 0;
		CmdArg cmd = null;
		switch (eventSrc) {
			case Constants.SAMSUNG_REMOTE_CONTROL_ID:
			case Constants.SAMSUNG_ALERTS_ID:
			case Constants.SAMSUNG_AUDIO_RECORDER_ID:
			case Constants.SAMSUNG_CAMERA_ID:
				eventSrc = Constants.SAMSUNG_CAMERA_ID;
				event = (event == Constants.SAMSUNG_REMOTE_CONTROL_EVT_FORWARD) ? Constants.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE : Constants.SAMSUNG_CAMERA_EVT_TAKE_PHOTO;

				msgService = eventSrc;
				cmd = new CmdArg(event, "");
				break;

			default:
				return;

		}

		if (cmd != null) {
			sendtoPluginService(IPCMessageManager.MICIROBIT_MESSAGE, msgService, cmd);
		}
	}

	/*
	 * IPC Messenger handling
	 */
	@Override
	public IBinder onBind(Intent intent) {

		return IPCMessageManager.getInstance().getClientMessenger().getBinder();
	}

	public void startIPCListener() {

		logi("startIPCListener()");
		if (IPCMessageManager.getInstance() == null) {


			logi("startIPCListener() :: IPCMessageManager.getInstance() == null");
			IPCMessageManager inst = IPCMessageManager.getInstance("BLEServiceReceiver", new Handler() {

				@Override
				public void handleMessage(Message msg) {
					logi("startIPCListener().handleMessage");
					handleIncomingMessage(msg);
				}

			});

			/*
			 * Make the initial connection to other processes
			 */
			new Thread(new Runnable() {
				@Override
				public void run() {

					try {
						Thread.sleep(3000);
						sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null);
						sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	public void sendtoPluginService(int mbsService, int functionCode, CmdArg cmd) {

		logi("sendtoPluginService()");
		Class destService = PluginService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd);
	}

	public void sendtoIPCService(int mbsService, int functionCode, CmdArg cmd) {

		logi("sendtoIPCService()");
		Class destService = IPCService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd);
	}

	public void sendIPCMessge(Class destService, int mbsService, int functionCode, CmdArg cmd) {

		logi("sendIPCMessge()");
		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		Message msg = Message.obtain(null, mbsService);
		msg.arg1 = functionCode;
		Bundle bundle = new Bundle();
		if (mbsService == IPCMessageManager.ANDROID_MESSAGE) {
			logi("sendIPCMessge() :: IPCMessageManager.ANDROID_MESSAGE functionCode=" + functionCode);
		} else if (mbsService == IPCMessageManager.MICIROBIT_MESSAGE) {
			logi("sendIPCMessge() :: IPCMessageManager.MICIROBIT_MESSAGE functionCode=" + functionCode);
			if (cmd != null) {
				bundle.putInt(IPCMessageManager.BUNDLE_DATA, cmd.getCMD());
				bundle.putString(IPCMessageManager.BUNDLE_VALUE, cmd.getValue());
			}
		} else {
			return;
		}

		msg.setData(bundle);
		try {
			inst.sendMessage(destService, msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void handleIncomingMessage(Message msg) {
		logi("handleIncomingMessage() :: Start BLEService");
		if (msg.what == IPCMessageManager.ANDROID_MESSAGE) {
			logi("handleIncomingMessage() :: IPCMessageManager.ANDROID_MESSAGE msg.arg1 = " + msg.arg1);
			switch (msg.arg1) {
				case IPCMessageManager.IPC_FUNCTION_CONNECT:
					logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_CONNECT");
					break;

				case IPCMessageManager.IPC_FUNCTION_DISCONNECT:
					logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_DISCONNECT");
					break;

				case IPCMessageManager.IPC_FUNCTION_RECONNECT:
					logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_RECONNECT");
					break;

				default:
			}
		} else if (msg.what == IPCMessageManager.MICIROBIT_MESSAGE) {
			logi("handleIncomingMessage() :: IPCMessageManager.MICIROBIT_MESSAGE msg.arg1 = " + msg.arg1);
		}
	}
}
