package com.samsung.microbit.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BLEManager {

	public static int BLE_DISCONNECTED = 0x0000;
	public static int BLE_CONNECTED = 0x0001;
	public static int BLE_SERVICES_DISCOVERED = 0x0002;

	public static int BLE_ERROR_OK = 0x00000000;
	public static int BLE_ERROR_FAIL = 0x00010000;
	public static int BLE_ERROR_TIMEOUT = 0x00020000;

	public static int BLE_ERROR_NOOP = -1 & 0xFFFF0000;
	public static int BLE_ERROR_NOGATT = -2 & 0xFFFF0000;

	public static long BLE_WAIT_TIMEOUT = 10000;

	public static int OP_NOOP = 0;
	public static int OP_CONNECT = 1;
	public static int OP_DISCOVER_SERVICES = 2;
	public static int OP_READ_CHARACTERISTIC = 3;
	public static int OP_WRITE_CHARACTERISTIC = 4;
	public static int OP_READ_DESCRIPTOR = 5;
	public static int OP_WRITE_DESCRIPTOR = 6;
	public static int OP_CHARACTERISTIC_CHANGED = 7;
	public static int OP_RELIABLE_WRITE_COMPLETED = 8;
	public static int OP_READ_REMOTE_RSSI = 9;
	public static int OP_MTU_CHANGED = 10;

	protected volatile int bleState = 0;
	protected volatile int error = 0;

	protected volatile int inBleOp = 0;
	protected volatile boolean callbackCompleted = false;

	protected volatile int rssi;
	protected volatile BluetoothGattCharacteristic lastCharacteristic;
	protected volatile BluetoothGattDescriptor lastDescriptor;

	protected Context context;
	protected BluetoothGatt gatt;
	protected BluetoothDevice bluetoothDevice;

	protected final Object locker = new Object();
	protected CharacteristicChangeListener characteristicChangeListener;
	protected UnexpectedConnectionEventListener unexpectedDisconnectionListener;

	static final String TAG = "BLEManager";
	private boolean debug = true;

	void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	public BLEManager(Context context, BluetoothDevice bluetoothDevice, UnexpectedConnectionEventListener unexpectedDisconnectionListener) {
		if (debug) logi("BLEManager(,,) :: start");
		this.context = context;
		this.bluetoothDevice = bluetoothDevice;
		this.unexpectedDisconnectionListener = unexpectedDisconnectionListener;
	}

	public BLEManager(Context context, BluetoothDevice bluetoothDevice, CharacteristicChangeListener characteristicChangeListener,
					  UnexpectedConnectionEventListener unexpectedDisconnectionListener) {

		if (debug) logi("BLEManager(,,,) :: start");
		this.context = context;
		this.bluetoothDevice = bluetoothDevice;
		this.characteristicChangeListener = characteristicChangeListener;
		this.unexpectedDisconnectionListener = unexpectedDisconnectionListener;
	}

	public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
	}

	public void setCharacteristicChangeListener(CharacteristicChangeListener characteristicChangeListener) {
		this.characteristicChangeListener = characteristicChangeListener;
	}

	public int getError() {
		return error;
	}

	public int getBleState() {
		return bleState;
	}

	public int getInBleOp() {
		return inBleOp;
	}

	public BluetoothGattService getService(UUID uuid) {
		if ((bleState & BLE_SERVICES_DISCOVERED) != 0) {
			return gatt.getService(uuid);
		}

		return null;
	}

	public List<BluetoothGattService> getServices() {
		if ((bleState & BLE_SERVICES_DISCOVERED) != 0) {
			return gatt.getServices();
		}

		return null;

	}

	public boolean reset() {

		if (debug) logi("reset()");
		synchronized (locker) {
			if (bleState != 0) {
				disconnect();
			}

			if (bleState != 0) {
				return false;
			}

			lastCharacteristic = null;
			lastDescriptor = null;
			rssi = 0;
			error = 0;
			inBleOp = OP_NOOP;
			callbackCompleted = false;
			if (gatt != null) {
				if (debug) logi("reset() :: gatt != null : closing gatt");
				gatt.close();
			}

			gatt = null;
			return true;
		}
	}

	public int connect(boolean autoReconnect) {

		if (debug) logi("connect() :: autoReconnect=" + autoReconnect);
		int rc = BLE_ERROR_NOOP;

		if (gatt == null) {
			if (debug) logi("connect() :: gatt == null");
			synchronized (locker) {
				if (inBleOp == OP_NOOP) {
					inBleOp = OP_CONNECT;
					try {
						if (debug) logi("connect() :: bluetoothDevice.connectGatt(context, autoReconnect, bluetoothGattCallback)");
						gatt = bluetoothDevice.connectGatt(context, false, bluetoothGattCallback);

						if(gatt == null)
							bluetoothDevice.connectGatt(context, autoReconnect, bluetoothGattCallback);

						if (gatt != null) {
							error = 0;
							locker.wait(BLE_WAIT_TIMEOUT);
							if (debug) logi("connect() :: remote device = " + gatt.getDevice().getAddress());
							if (!callbackCompleted) {
								error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
							}

							rc = error | bleState;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					inBleOp = OP_NOOP;
				}
			}
		} else {
			rc = gattConnect();
		}

		if (debug) logi("connect() :: rc = " + rc);
		return rc;
	}

	private int gattConnect() {

		if (debug) logi("gattConnect() :: start");
		int rc = BLE_ERROR_NOOP;
		synchronized (locker) {
			if (gatt != null && inBleOp == OP_NOOP) {

				if (debug) logi("gattConnect() :: gatt != null");
				inBleOp = OP_CONNECT;
				error = 0;
				try {
					if (bleState == 0) {
						callbackCompleted = false;
						if (debug) logi("gattConnect() :: gatt.connect()");
						gatt.connect();
						locker.wait(BLE_WAIT_TIMEOUT);
						if (debug) logi("gattConnect() :: remote device = " + gatt.getDevice().getAddress());
						if (!callbackCompleted) {
							error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
						}

						rc = error | bleState;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				inBleOp = OP_NOOP;
			}
		}

		if (debug) logi("gattConnect() :: rc = " + rc);
		return rc;
	}

	public int disconnect() {

		if (debug) logi("disconnect() :: start");
		int rc = BLE_ERROR_NOOP;
		synchronized (locker) {
			if (gatt != null && inBleOp == OP_NOOP) {

				inBleOp = OP_CONNECT;
				try {
					error = 0;
					if (bleState != 0) {
						callbackCompleted = false;
						gatt.disconnect();
						locker.wait(BLE_WAIT_TIMEOUT);
						if (!callbackCompleted) {
							error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
						}
					}

					rc = error | bleState;
				} catch (InterruptedException e) {
				}

				inBleOp = OP_NOOP;
			}
		}

		if (debug) logi("disconnect() :: rc = " + rc);
		return rc;
	}

	public int waitDisconnect() {

		if (debug) logi("waitDisconnect() :: start");
		int rc = BLE_ERROR_NOOP;
		synchronized (locker) {
			if (gatt != null && inBleOp == OP_NOOP) {

				inBleOp = OP_CONNECT;
				this.error = 0;
				int bleState = this.bleState;
				try {
					if (bleState != 0) {
						callbackCompleted = false;
						locker.wait(BLE_WAIT_TIMEOUT);
						if (!callbackCompleted) {
							error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
						} else {
							error = this.error;
							bleState = this.bleState;
						}
					}

					rc = error | bleState;
				} catch (InterruptedException e) {
				}

				inBleOp = OP_NOOP;
			}
		}

		if (debug) logi("waitDisconnect() :: rc = " + rc);
		return rc;
	}

	public int discoverServices() {

		if (debug) logi("discoverServices() :: start");
		int rc = BLE_ERROR_NOOP;
		synchronized (locker) {
			if (gatt != null && inBleOp == OP_NOOP) {

				inBleOp = OP_DISCOVER_SERVICES;
				error = 0;
				try {
					callbackCompleted = false;
					if (gatt.discoverServices()) {
						locker.wait(BLE_WAIT_TIMEOUT);
						if (!callbackCompleted) {
							error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
						}

						rc = error | bleState;
					}
				} catch (InterruptedException e) {
				}

				inBleOp = OP_NOOP;
			}
		}

		if (debug) logi("discoverServices() :: end : rc = " + rc);
		return rc;
	}

	public boolean isConnected() {
		return (bleState == BLE_CONNECTED || bleState == BLE_SERVICES_DISCOVERED);
	}

	public int writeDescriptor(BluetoothGattDescriptor descriptor) {

		if (debug) logi("writeDescriptor() :: start");
		int rc = BLE_ERROR_NOOP;
		synchronized (locker) {
			if (gatt != null && inBleOp == OP_NOOP) {

				inBleOp = OP_WRITE_DESCRIPTOR;
				lastDescriptor = null;
				error = 0;
				try {
					if (gatt.writeDescriptor(descriptor)) {
						callbackCompleted = false;
						locker.wait(BLE_WAIT_TIMEOUT);
						if (!callbackCompleted) {
							error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
						}

						rc = error | bleState;
					}

				} catch (InterruptedException e) {
				}

				inBleOp = OP_NOOP;
			}
		}

		if (debug) logi("writeDescriptor() :: end : rc = " + rc);
		return rc;
	}

	public int readDescriptor(BluetoothGattDescriptor descriptor) {

		if (debug) logi("readDescriptor() :: start");
		int rc = BLE_ERROR_NOOP;
		synchronized (locker) {
			if (gatt != null && inBleOp > OP_NOOP) {

				inBleOp = OP_READ_DESCRIPTOR;
				lastDescriptor = null;
				error = 0;
				try {
					callbackCompleted = false;
					if (gatt.readDescriptor(descriptor)) {
						locker.wait(BLE_WAIT_TIMEOUT);
						if (!callbackCompleted) {
							error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
						}

						rc = error | bleState;
					}
				} catch (InterruptedException e) {
				}

				inBleOp = OP_NOOP;
			}
		}

		if (debug) logi("readDescriptor() :: end : rc = " + rc);
		return rc;
	}

	public int writeCharacteristic(BluetoothGattCharacteristic characteristic) {

		if (debug) logi("writeCharacteristic() :: start");
		int rc = BLE_ERROR_NOOP;
		synchronized (locker) {
			if (gatt != null && inBleOp > OP_NOOP) {

				inBleOp = OP_WRITE_CHARACTERISTIC;
				lastCharacteristic = null;
				error = 0;
				try {
					callbackCompleted = false;
					if (gatt.writeCharacteristic(characteristic)) {
						locker.wait(BLE_WAIT_TIMEOUT);
						if (!callbackCompleted) {
							error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
						}

						rc = error | bleState;
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				inBleOp = OP_NOOP;
			}

		}

		if (debug) logi("writeCharacteristic() :: end : rc = " + rc);
		return rc;
	}

	public int readCharacteristic(BluetoothGattCharacteristic characteristic) {

		if (debug) logi("readCharacteristic() :: start");
		int rc = BLE_ERROR_NOOP;
		synchronized (locker) {
			if (gatt != null && inBleOp > OP_NOOP) {

				inBleOp = OP_READ_CHARACTERISTIC;
				lastCharacteristic = null;
				this.error = 0;
				int bleState = this.bleState;
				try {
					callbackCompleted = false;
					if (gatt.readCharacteristic(characteristic)) {
						locker.wait(BLE_WAIT_TIMEOUT);
						if (!callbackCompleted) {
							error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
						} else {
							error = this.error;
							bleState = this.bleState;
						}

						rc = error | bleState;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				inBleOp = OP_NOOP;
			}
		}

		if (debug) logi("readCharacteristic() :: end : rc = " + rc);
		return rc;
	}

	public BluetoothGattCharacteristic getLastCharacteristic() {
		return lastCharacteristic;
	}

	public BluetoothGattDescriptor getLastDescriptor() {
		return lastDescriptor;
	}

	public int enableCharacteristicNotification(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, boolean enable) {

		if (enable) {
			gatt.setCharacteristicNotification(characteristic, enable);
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		} else {
			gatt.setCharacteristicNotification(characteristic, false);
			descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
		}

		return writeDescriptor(descriptor);
	}

	final protected BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

			super.onConnectionStateChange(gatt, status, newState);

			int state;
			int error = 0;
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					state = BLE_CONNECTED;
				} else {
					state = BLE_DISCONNECTED;
				}
			} else {
				state = BLE_DISCONNECTED;
				error = BLE_ERROR_FAIL;
			}

			synchronized (locker) {
				if (debug)
					logi("BluetoothGattCallback.onConnectionStateChange() :: start : status = " + status + " newState = " + newState);

				if (inBleOp == OP_CONNECT) {

					if (debug) logi("BluetoothGattCallback.onConnectionStateChange() :: inBleOp == OP_CONNECT");
					if (state != (bleState & BLE_CONNECTED)) {
						bleState = state;
					}

					callbackCompleted = true;
					BLEManager.this.error = error;
					locker.notify();
				} else {
					if (debug) logi("onConnectionStateChange() :: inBleOp != OP_CONNECT");
					bleState = state;
					unexpectedDisconnectionListener.handleConnectionEvent(bleState);
				}

				if (debug) logi("BluetoothGattCallback.onConnectionStateChange() :: end");
			}

		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {

			super.onServicesDiscovered(gatt, status);
			int state = BLE_SERVICES_DISCOVERED;
			synchronized (locker) {
				if (debug) logi("BluetoothGattCallback.onServicesDiscovered() :: start : status = " + status);

				if (inBleOp == OP_DISCOVER_SERVICES) {
					if (debug) logi("BluetoothGattCallback.onServicesDiscovered() :: inBleOp == OP_DISCOVER_SERVICES");
					if (status == BluetoothGatt.GATT_SUCCESS) {
						bleState |= state;

					} else {
						bleState &= (~state);
					}

					callbackCompleted = true;
					locker.notify();
				}

				if (debug) logi("BluetoothGattCallback.onServicesDiscovered() :: end");
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

			super.onCharacteristicRead(gatt, characteristic, status);
			synchronized (locker) {
				if (debug) logi("BluetoothGattCallback.onCharacteristicRead() :: start : status = " + status);

				if (inBleOp == OP_READ_CHARACTERISTIC) {
					if (debug) logi("BluetoothGattCallback.onCharacteristicRead() :: inBleOp == OP_READ_CHARACTERISTIC");
					if (status == BluetoothGatt.GATT_SUCCESS) {
						error = BLE_ERROR_OK;
					} else {
						error = BLE_ERROR_FAIL;
					}

					lastCharacteristic = characteristic;
					callbackCompleted = true;
					locker.notify();
				}

				if (debug) logi("BluetoothGattCallback.onCharacteristicRead() :: end");
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

			super.onCharacteristicWrite(gatt, characteristic, status);
			synchronized (locker) {
				if (debug) logi("BluetoothGattCallback.onCharacteristicWrite() :: start : status = " + status);

				if (inBleOp == OP_WRITE_CHARACTERISTIC) {
					if (debug) logi("BluetoothGattCallback.onCharacteristicWrite() :: inBleOp == OP_WRITE_CHARACTERISTIC");
					if (status == BluetoothGatt.GATT_SUCCESS) {
						error = BLE_ERROR_OK;
					} else {
						error = BLE_ERROR_FAIL;
					}

					lastCharacteristic = characteristic;
					callbackCompleted = true;
					locker.notify();
				}

				if (debug) logi("BluetoothGattCallback.onCharacteristicWrite() :: end");
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

			if (debug) logi("BluetoothGattCallback.onCharacteristicChanged() :: start");
			super.onCharacteristicChanged(gatt, characteristic);
			characteristicChangeListener.onCharacteristicChanged(gatt, characteristic);
			if (debug) logi("BluetoothGattCallback.onCharacteristicChanged() :: end");
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

			super.onDescriptorRead(gatt, descriptor, status);
			synchronized (locker) {
				if (debug) logi("BluetoothGattCallback.onDescriptorRead() :: start : status = " + status);

				if (inBleOp == OP_READ_DESCRIPTOR) {
					if (debug) logi("BluetoothGattCallback.onDescriptorRead() :: inBleOp == OP_READ_DESCRIPTOR");
					if (status == BluetoothGatt.GATT_SUCCESS) {
						error = BLE_ERROR_OK;
					} else {
						error = BLE_ERROR_FAIL;
					}

					lastDescriptor = descriptor;
					callbackCompleted = true;
					locker.notify();
				}

				if (debug) logi("BluetoothGattCallback.onDescriptorRead() :: end");
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

			super.onDescriptorWrite(gatt, descriptor, status);
			synchronized (locker) {
				if (debug) logi("BluetoothGattCallback.onDescriptorWrite() :: start : status = " + status);

				if (inBleOp == OP_WRITE_DESCRIPTOR) {
					if (debug) logi("BluetoothGattCallback.onDescriptorWrite() :: inBleOp == OP_WRITE_DESCRIPTOR");
					if (status == BluetoothGatt.GATT_SUCCESS) {
						error = BLE_ERROR_OK;
					} else {
						error = BLE_ERROR_FAIL;
					}

					lastDescriptor = descriptor;
					callbackCompleted = true;
					locker.notify();
				}

				if (debug) logi("BluetoothGattCallback.onDescriptorWrite() :: end");
			}
		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {

			super.onReliableWriteCompleted(gatt, status);
			synchronized (locker) {
				if (debug) logi("BluetoothGattCallback.onReliableWriteCompleted() :: start");

				if (status == BluetoothGatt.GATT_SUCCESS) {
					error = BLE_ERROR_OK;
				} else {
					error = BLE_ERROR_FAIL;
				}

				callbackCompleted = true;
				locker.notify();

				if (debug) logi("BluetoothGattCallback.onReliableWriteCompleted() :: end");
			}
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

			super.onReadRemoteRssi(gatt, rssi, status);
			synchronized (locker) {
				if (debug) logi("BluetoothGattCallback.onReadRemoteRssi() :: start");

				if (inBleOp == OP_READ_REMOTE_RSSI) {
					if (debug) logi("BluetoothGattCallback.onReadRemoteRssi() :: inBleOp == OP_READ_REMOTE_RSSI");
					if (status == BluetoothGatt.GATT_SUCCESS) {
						error = BLE_ERROR_OK;
					} else {
						error = BLE_ERROR_FAIL;
					}

					BLEManager.this.rssi = rssi;
					callbackCompleted = true;
					locker.notify();
				}

				if (debug) logi("BluetoothGattCallback.onMtuChanged() :: end");
			}
		}

		@Override
		public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {

			super.onMtuChanged(gatt, mtu, status);
			synchronized (locker) {
				if (debug) logi("BluetoothGattCallback.onMtuChanged() :: start");

				if (status == BluetoothGatt.GATT_SUCCESS) {
					error = BLE_ERROR_OK;
				} else {
					error = BLE_ERROR_FAIL;
				}

				if (debug) logi("BluetoothGattCallback.onMtuChanged() :: end");
			}
		}
	};
}

