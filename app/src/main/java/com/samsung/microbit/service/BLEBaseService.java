package com.samsung.microbit.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.samsung.microbit.plugin.BLEManager;
import com.samsung.microbit.plugin.CharacteristicChangeListener;

import java.util.List;
import java.util.UUID;

public abstract class BLEBaseService extends Service {

	protected BLEManager bleManager;
	BluetoothManager bluetoothManager;
	BluetoothAdapter bluetoothAdapter;
	BluetoothDevice bluetoothDevice;

	protected String deviceAddress;

	protected String TAG = "BLEBaseService";
	protected boolean debug = true;

	protected void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	private boolean initialize() {

		logi("initialize() :: remoteDevice = " + deviceAddress);
		if (bluetoothManager == null) {
			bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (bluetoothManager == null) {
				return false;
			}
		}

		if (bluetoothAdapter == null) {
			bluetoothAdapter = bluetoothManager.getAdapter();
			if (bluetoothAdapter == null) {
				return false;
			}
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

		String deviceAddress;

		SharedPreferences preferences = getApplicationContext().getSharedPreferences("myAppPrefs", Context.MODE_PRIVATE);

		if (intent != null) {
			logi("onStartCommand() :: Received start id " + startId + ": " + intent);
			deviceAddress = intent.getStringExtra("DEVICE_ADDRESS");
			this.deviceAddress = deviceAddress;
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(this.getClass().getName() + ".deviceAddress", deviceAddress);
			editor.commit();

		} else {
			deviceAddress = preferences.getString(this.getClass().getName() + ".deviceAddress", null);
			this.deviceAddress = deviceAddress;
		}

		if (initialize()) {

			logi("onStartCommand() :: initialize(deviceAddress) = OK");
			if (bleManager == null) {
				bleManager = new BLEManager(getApplicationContext(), bluetoothDevice, new CharacteristicChangeListener() {
					@Override
					public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

						handleCharacteristicChanged(gatt, characteristic);
					}
				});
			}
		}

		return START_STICKY;
		//return START_REDELIVER_INTENT;
	}

	protected void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy called");

		//if (bleManager != null) {
		//	bleManager.disconnect();
		//	bleManager = null;
		//}
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
