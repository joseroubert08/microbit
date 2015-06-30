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
import android.util.Log;

import com.samsung.microbit.core.BLEManager;
import com.samsung.microbit.core.CharacteristicChangeListener;
import com.samsung.microbit.core.UnexpectedConnectionEventListener;

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
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (debug) logi("onStartCommand()");
		return START_STICKY;
	}

	protected boolean reset() {
		boolean rc = false;
		if (bleManager != null) {
			rc = bleManager.reset();
			if (rc) {
				bleManager = null;
			}
		}

		return rc;
	}

	protected void setupBLE() {

		if (debug) logi("setupBLE()");
		//if (bleManager != null) {
		//	return;
		//}

		this.deviceAddress = getDeviceAddress();
		if (debug) logi("setupBLE() :: deviceAddress = " + deviceAddress);
		if (this.deviceAddress != null) {
			bluetoothDevice = null;
			if (initialize()) {

				bleManager = new BLEManager(getApplicationContext(), bluetoothDevice,
					new CharacteristicChangeListener() {
						@Override
						public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

							if (debug) logi("setupBLE().CharacteristicChangeListener.onCharacteristicChanged()");
							if (bleManager != null) {
								handleCharacteristicChanged(gatt, characteristic);
							}
						}
					},
					new UnexpectedConnectionEventListener() {
						@Override
						public void handleConnectionEvent(int event) {
							if (debug) logi("setupBLE().CharacteristicChangeListener.onCharacteristicChanged()");
							if (bleManager != null) {
								handleUnexpectedConnectionEvent(event);
							}
						}
					});

				startupConnection();
				return;
			}
		}

		setNotification(false);
	}

	private boolean initialize() {

		boolean rc = true;
		if (debug) logi("initialize() :: remoteDevice = " + deviceAddress);
		if (rc && bluetoothManager == null) {
			bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			rc = (bluetoothManager != null) ? true : false;
		}

		if (rc && (bluetoothAdapter == null)) {
			bluetoothAdapter = bluetoothManager.getAdapter();
			rc = (bluetoothAdapter != null) ? true : false;
		}

		if (rc && (bluetoothDevice == null)) {
			if (deviceAddress != null) {
				bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
				rc = (bluetoothAdapter != null) ? true : false;
			} else {
				rc = false;
			}
		}

		if (debug) logi("initialize() :: complete rc = " + rc);
		return rc;
	}

	protected abstract void startupConnection();

	protected abstract String getDeviceAddress();

	protected abstract void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

	protected abstract void handleUnexpectedConnectionEvent(int event);

	protected abstract void setNotification(boolean isConnected);

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy called");
	}

	public BluetoothGattService getService(UUID uuid) {

		if (bleManager != null) {
			return bleManager.getService(uuid);
		}

		return null;
	}

	public List<BluetoothGattService> getServices() {

		if (bleManager != null) {
			return bleManager.getServices();
		}

		return null;
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

		int rc = 99;
		if (bleManager != null) {
			rc = bleManager.getError();
		}

		return rc;
	}

	public int getBleState() {

		int rc = 99;
		if (bleManager != null) {
			rc = bleManager.getBleState();
		}

		return rc;
	}

	public int connect() {

		int rc = 99;
		if (bleManager != null) {
			rc = bleManager.connect(true);
			rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
		}

		return rc;
	}

	public int disconnect() {

		int rc = 99;
		if (bleManager != null) {
			rc = bleManager.disconnect();
			rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
		}

		return rc;
	}

	public int waitDisconnect() {

		int rc = 99;
		if (bleManager != null) {
			rc = bleManager.waitDisconnect();
			rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
		}

		return rc;
	}

	public int discoverServices() {

		int rc = 99;
		if (bleManager != null) {
			if (debug) logi("discoverServices() :: bleManager != null");
			rc = bleManager.discoverServices();
			rc = interpretCode(rc, BLEManager.BLE_SERVICES_DISCOVERED);
		}

		return rc;
	}

	public int enableCharacteristicNotification(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, boolean enable) {

		int rc = 99;
		if (bleManager != null) {
			rc = bleManager.enableCharacteristicNotification(characteristic, descriptor, enable);
		}

		return rc;

	}

	public int writeDescriptor(BluetoothGattDescriptor descriptor) {

		int rc = 99;
		if (bleManager != null) {
			rc = bleManager.writeDescriptor(descriptor);
			rc = interpretCode(rc);
		}

		return rc;
	}

	public BluetoothGattDescriptor readDescriptor(BluetoothGattDescriptor descriptor) {

		if (bleManager != null) {
			int rc = bleManager.readDescriptor(descriptor);
			rc = interpretCode(rc);
			if (rc == 0) {
				return bleManager.getLastDescriptor();
			}
		}

		return null;
	}

	public int writeCharacteristic(BluetoothGattCharacteristic characteristic) {

		int rc = 99;
		if (bleManager != null) {
			bleManager.writeCharacteristic(characteristic);
			rc = interpretCode(rc);
		}

		return rc;
	}

	public BluetoothGattCharacteristic readCharacteristic(BluetoothGattCharacteristic characteristic) {

		if (bleManager != null) {
			int rc = bleManager.readCharacteristic(characteristic);
			rc = interpretCode(rc);
			if (rc == 0) {
				return bleManager.getLastCharacteristic();
			}
		}

		return null;
	}
}
