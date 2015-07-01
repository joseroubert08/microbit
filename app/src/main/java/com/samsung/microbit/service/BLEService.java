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
import com.samsung.microbit.model.NameValuePair;
import com.samsung.microbit.ui.activity.ConnectActivity;

import java.util.UUID;

public class BLEService extends BLEBaseService {

	public static final int FORMAT_UINT8 = BluetoothGattCharacteristic.FORMAT_UINT8;
	public static final int FORMAT_UINT16 = BluetoothGattCharacteristic.FORMAT_UINT16;
	public static final int FORMAT_UINT32 = BluetoothGattCharacteristic.FORMAT_UINT32;

	public static final int FORMAT_SINT8 = BluetoothGattCharacteristic.FORMAT_SINT8;
	public static final int FORMAT_SINT16 = BluetoothGattCharacteristic.FORMAT_SINT16;
	public static final int FORMAT_SINT32 = BluetoothGattCharacteristic.FORMAT_SINT32;


	protected String TAG = "BLEService";
	protected boolean debug = true;

	protected void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	NotificationManager notifyMgr = null;
	int notificationId = 1010;

	public BLEService() {
		startIPCListener();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		int rc = 0;
		if (debug) logi("onStartCommand()");
		rc = super.onStartCommand(intent, flags, startId);
		/*
		 * Make the initial connection to other processes
		 */
		new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					Thread.sleep(IPCMessageManager.STARTUP_DELAY + 500L);
					sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null, null);
					sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null, null);
					setNotification(false, 0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		return rc;
	}

	protected String getDeviceAddress() {

		if (debug) logi("getDeviceAddress()");
		ConnectedDevice currentDevice = Utils.getPairedMicrobit(this);
		String pairedDeviceName = currentDevice.mAddress;
		if (pairedDeviceName == null) {
			setNotification(false, 2);
		}

		return pairedDeviceName;
	}

	protected void startupConnection() {

		if (debug) logi("startupConnection() bleManager=" + bleManager);
		boolean success = true;
		int rc = connect();
		if (rc == 0) {
			if (debug) logi("startupConnection() :: connect() == 0");
			rc = discoverServices();
			if (rc == 0) {

				if (debug) logi("startupConnection() :: discoverServices() == 0");
				if (registerNotifications(true)) {
					setNotification(true, 0);
				} else {
					rc = 1;
					success = false;
				}
			} else {
				success = false;
				if (debug) logi("startupConnection() :: discoverServices() != 0");
			}
		} else {
			success = false;
		}

		if (!success) {
			if (bleManager != null) {
				reset();
				setNotification(false, 1);
			}
		}

		if (debug) logi("startupConnection() :: end");
	}

	@Override
	public void onDestroy() {
		logi("onDestroy()");
	}

	public boolean registerNotifications(boolean enable) {

		if (debug) logi("registerNotifications()");
		BluetoothGattService button1s = getService(Constants.EVENT_SERVICE);
		if (button1s == null) {
			if (debug) logi("registerNotifications() :: not found service " + Constants.EVENT_SERVICE.toString());
			return false;
		}

		BluetoothGattCharacteristic button1c = button1s.getCharacteristic(Constants.ES_CLIENT_EVENT);
		if (button1c == null) {
			if (debug) logi("registerNotifications() :: not found Characteristic " + Constants.ES_CLIENT_EVENT.toString());
			return false;
		}

		BluetoothGattDescriptor button1d = button1c.getDescriptor(Constants.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
		if (button1d == null) {
			if (debug)
				logi("registerNotifications() :: not found descriptor " + Constants.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR.toString());
			return false;
		}

		enableCharacteristicNotification(button1c, button1d, enable);
		if (debug) logi("registerNotifications() : done");
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

		if (debug) logi("handleDisconnection() :: event = " + event);
		if ((event & BLEManager.BLE_CONNECTED) != 0) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (debug) logi("handleDisconnection() :: BLE_CONNECTED");
					discoverServices();
					registerNotifications(true);
					setNotification(true, 0);
				}
			}).start();

		} else if (event == BLEManager.BLE_DISCONNECTED) {
			if (debug) logi("handleDisconnection() :: BLE_DISCONNECTED");
			setNotification(false, 0);
		}
	}

	@Override
	protected void setNotification(boolean isConnected, int errorCode) {

		if (debug) logi("setNotification() :: isConnected = " + isConnected);
		NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		int notificationId = 1001;

		NameValuePair[] args = new NameValuePair[2];
		args[0] = new NameValuePair(IPCMessageManager.BUNDLE_ERROR_CODE, errorCode);
		args[1] = new NameValuePair(IPCMessageManager.BUNDLE_DEVICE_ADDRESS, deviceAddress);

		if (!isConnected) {
			if (debug) logi("setNotification() :: !isConnected");
			if (bluetoothAdapter != null) {
				if (!bluetoothAdapter.isEnabled()) {

					if (debug) logi("setNotification() :: !bluetoothAdapter.isEnabled()");
					reset();

					//bleManager = null;
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

			sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED, null, args);
			sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED, null, args);
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

			sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_CONNECTED, null, args);
			sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_CONNECTED, null, args);
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
			sendtoPluginService(IPCMessageManager.MICIROBIT_MESSAGE, msgService, cmd, null);
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

		if (debug) logi("startIPCListener()");
		if (IPCMessageManager.getInstance() == null) {


			if (debug) logi("startIPCListener() :: IPCMessageManager.getInstance() == null");
			IPCMessageManager inst = IPCMessageManager.getInstance("BLEServiceReceiver", new Handler() {

				@Override
				public void handleMessage(Message msg) {
					if (debug) logi("startIPCListener().handleMessage");
					handleIncomingMessage(msg);
				}

			});
		}
	}

	public void sendtoPluginService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		if (debug) logi("sendtoPluginService()");
		Class destService = PluginService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd, args);
	}

	public void sendtoIPCService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		if (debug) logi("sendtoIPCService()");
		Class destService = IPCService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd, args);
	}

	public void sendIPCMessge(Class destService, int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		if (debug) logi("sendIPCMessge()");
		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		Message msg = Message.obtain(null, mbsService);
		msg.arg1 = functionCode;
		Bundle bundle = new Bundle();
		if (mbsService == IPCMessageManager.ANDROID_MESSAGE) {
			if (debug) logi("sendIPCMessge() :: IPCMessageManager.ANDROID_MESSAGE functionCode=" + functionCode);
			if (cmd != null) {
				bundle.putInt(IPCMessageManager.BUNDLE_DATA, cmd.getCMD());
				bundle.putString(IPCMessageManager.BUNDLE_VALUE, cmd.getValue());
			}

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					bundle.putSerializable(args[i].getName(), args[i].getValue());
				}
			}

		} else if (mbsService == IPCMessageManager.MICIROBIT_MESSAGE) {
			if (debug) logi("sendIPCMessge() :: IPCMessageManager.MICIROBIT_MESSAGE functionCode=" + functionCode);
			if (cmd != null) {
				bundle.putInt(IPCMessageManager.BUNDLE_DATA, cmd.getCMD());
				bundle.putString(IPCMessageManager.BUNDLE_VALUE, cmd.getValue());
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						bundle.putSerializable(args[i].getName(), args[i].getValue());
					}
				}
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

	public void writeCharacteristic(String serviceGuid, String characteristic, int value, int type) {

		BluetoothGattService s = getService(UUID.fromString(serviceGuid));
		if (s != null) {
			BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(characteristic));
			if (c != null) {
				/*
			 	 * TODO Need to try and write, to see if we have an endian issue
		 		*/
				c.setValue(value, type, 0);
				writeCharacteristic(c);
			}
		}
	}

	private void handleIncomingMessage(Message msg) {
		if (debug) logi("handleIncomingMessage() :: Start BLEService");
		Bundle bundle = msg.getData();
		if (msg.what == IPCMessageManager.ANDROID_MESSAGE) {
			if (debug) logi("handleIncomingMessage() :: IPCMessageManager.ANDROID_MESSAGE msg.arg1 = " + msg.arg1);

			switch (msg.arg1) {
				case IPCMessageManager.IPC_FUNCTION_CONNECT:
					if (debug) logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_CONNECT bleManager = " + bleManager);
					setupBLE();
					break;

				case IPCMessageManager.IPC_FUNCTION_DISCONNECT:
					if (debug) logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_DISCONNECT = " + bleManager);
					if (reset()) {
						setNotification(false, 0);
					}

					break;

				case IPCMessageManager.IPC_FUNCTION_RECONNECT:
					if (debug) logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_RECONNECT = " + bleManager);
					if (reset()) {
						setupBLE();
					}

					break;

				case IPCMessageManager.IPC_FUNCTION_WRITE_CHARACTERISTIC:
					if (debug) logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_WRITE_CHARACTERISTIC = " + bleManager);
					String service = (String) bundle.getSerializable(IPCMessageManager.BUNDLE_SERVICE_GUID);
					String characteristic = (String) bundle.getSerializable(IPCMessageManager.BUNDLE_CHARACTERISTIC_GUID);
					int value = (int) bundle.getSerializable(IPCMessageManager.BUNDLE_CHARACTERISTIC_VALUE);
					int type = (int) bundle.getSerializable(IPCMessageManager.BUNDLE_CHARACTERISTIC_TYPE);
					writeCharacteristic(service, characteristic, value, type);
					break;
				default:
			}
		} else if (msg.what == IPCMessageManager.MICIROBIT_MESSAGE) {
			if (debug) logi("handleIncomingMessage() :: IPCMessageManager.MICIROBIT_MESSAGE msg.arg1 = " + msg.arg1);
		}
	}
}
