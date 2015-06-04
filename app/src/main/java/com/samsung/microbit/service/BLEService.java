package com.samsung.microbit.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.samsung.microbit.plugin.BLEManager;
import com.samsung.microbit.plugin.CharacteristicChangeListener;
import com.samsung.microbit.ui.fragment.MyBlankFragment;

import java.util.List;
import java.util.UUID;

public class BLEService extends Service {

	BLEManager bleManager;
	BluetoothDevice bluetoothDevice;
	String deviceAddress;

	public static final String messageName = "uBIT_BUTTON_PRESS_";

	static final String TAG = "BLEService";
	private boolean debug = false;

	void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	public class LocalBinder extends Binder {
		public BLEService getService() {
			return BLEService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients.  See RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	@Override
	public void onCreate() {
		super.onCreate();
	}


	private boolean initialize() {

		logi("initialize() :: remoteDevice = " + deviceAddress);
		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		if (bluetoothManager == null) {
			return false;
		}

		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			return false;
		}

		if (bluetoothDevice == null) {
			bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
			if (bluetoothDevice == null) {
				return false;
			}
		}

		logi("initialize() :: complete");
		return true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent != null) {
			logi("onStartCommand() :: Received start id " + startId + ": " + intent);
			String deviceAddress = intent.getStringExtra("DEVICE_ADDRESS");
			ResultReceiver resultReceiver = (ResultReceiver) intent.getParcelableExtra("com.samsung.resultReceiver");
			this.deviceAddress = deviceAddress;

			if (initialize()) {

				logi("onStartCommand() :: initialize(deviceAddress) = OK");
				bleManager = new BLEManager(getApplicationContext(), bluetoothDevice, new CharacteristicChangeListener() {
					@Override
					public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {


						int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
						if (value == 0) {
							return;
						}

						if (MyBlankFragment.button2Characteristic.equals(characteristic.getUuid())) {
							value += 10;
						}

						logi("onCharacteristicChanged value = " + value);
						sendMessage(value);
					}
				});
			}

			// Notify the called we have initialisd.
			resultReceiver.send(1, null);

			// We want this service to continue running until it is explicitly stopped, so return sticky.
		}

        Toast.makeText(this, "BLEService started", Toast.LENGTH_SHORT).show();
		return START_STICKY;
	}

	private void sendMessage(int buttonNumber) {

		Log.i(TAG, "sendMessage");
		final Intent intent = new Intent();
		intent.setAction(messageName + buttonNumber);
		intent.putExtra("buttonPressed", buttonNumber);

		new Thread(new Runnable() {
			@Override
			public void run() {
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
			}
		}).start();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy called");

		if (bleManager != null) {
			bleManager.disconnect();
			bleManager = null;
		}
	}

	public BluetoothGattService getService(UUID uuid) {

		return bleManager.getService(uuid);
	}

	public List<BluetoothGattService> getServices() {

		return bleManager.getServices();
	}

	int interpretCode(int rc, int goodCode) {

		if (rc > 0) {
			if ((rc & BLEManager.BLE_ERROR_FAIL) != 0) {
				if ((rc & BLEManager.BLE_ERROR_TIMEOUT) != 0) {
					rc = 10;
				} else {
					rc = 99;
				}
			} else {
				rc &= 0x0ffff;
				if ((rc & goodCode) != 0) {
					rc = 0;
				} else {
					rc = 1;
				}
			}
		}

		return rc;
	}

	int interpretCode(int rc) {
		if (rc > 0) {
			if ((rc & BLEManager.BLE_ERROR_FAIL) != 0) {
				if ((rc & BLEManager.BLE_ERROR_TIMEOUT) != 0) {
					rc = 10;
				} else {
					rc = 99;
				}
			} else {
				rc &= 0x0000ffff;
				if (rc == BLEManager.BLE_DISCONNECTED) {
					rc = 1;
				} else {
					rc = 0;
				}
			}
		}

		return rc;
	}

	public int getError() {
		return bleManager.getError();
	}

	public int getBleState() {
		return bleManager.getBleState();
	}

	public int connect() {

		int rc = bleManager.connect();
		rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
		return rc;
	}

	public int disconnect() {

		int rc = bleManager.disconnect();
		rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
		return rc;
	}

	public int waitDisconnect() {

		int rc = bleManager.waitDisconnect();
		rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
		return rc;
	}

	public int discoverServices() {
		int rc = bleManager.discoverServices();
		rc = interpretCode(rc, BLEManager.BLE_SERVICES_DISCOVERED);
		return rc;
	}

	public int enableCharacteristicNotification(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, boolean enable) {
		return bleManager.enableCharacteristicNotification(characteristic, descriptor, enable);
	}

	public int writeDescriptor(BluetoothGattDescriptor descriptor) {

		int rc = bleManager.writeDescriptor(descriptor);
		rc = interpretCode(rc);
		return rc;
	}

	public BluetoothGattDescriptor readDescriptor(BluetoothGattDescriptor descriptor) {
		int rc = bleManager.readDescriptor(descriptor);
		rc = interpretCode(rc);
		if (rc == 0) {
			return bleManager.getLastDescriptor();
		}

		return null;
	}

	public int writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		int rc = bleManager.writeCharacteristic(characteristic);
		rc = interpretCode(rc);
		return rc;
	}

	public BluetoothGattCharacteristic readCharacteristic(BluetoothGattCharacteristic characteristic) {
		int rc = bleManager.readCharacteristic(characteristic);
		rc = interpretCode(rc);
		if (rc == 0) {
			return bleManager.getLastCharacteristic();
		}

		return null;
	}
}
