package com.samsung.microbit.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.UUID;

public class BLEService extends BLEBaseService {

	public static final UUID BUTTON_1_SERVICE = UUID.fromString("0000a000-0000-1000-8000-00805f9b34fb");
	public static final UUID BUTTON_2_SERVICE = UUID.fromString("0000b000-0000-1000-8000-00805f9b34fb");

	public static final UUID BUTTON_1_CHARACTERISTIC = UUID.fromString("0000a001-0000-1000-8000-00805f9b34fb");
	public static final UUID BUTTON_2_CHARACTERISTIC = UUID.fromString("0000b001-0000-1000-8000-00805f9b34fb");

	public static final UUID CALLBACK_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	public static final String MESSAGE_NAME = "uBIT_BUTTON_PRESS";

	protected String TAG = "BLEService";
	protected boolean debug = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		int rc = super.onStartCommand(intent, flags, startId);

		// Notify the called we have initialisd.
		ResultReceiver resultReceiver = (ResultReceiver) intent.getParcelableExtra("com.samsung.resultReceiver");
		resultReceiver.send(1, null);
		return rc;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy called");
	}

	public void registerNotifications (boolean enable) {

		BluetoothGattService button1s = getService(BUTTON_1_SERVICE);
		BluetoothGattCharacteristic button1c = button1s.getCharacteristic(BUTTON_1_CHARACTERISTIC);
		BluetoothGattDescriptor button1d = button1c.getDescriptor(CALLBACK_DESCRIPTOR);
		enableCharacteristicNotification(button1c, button1d, enable);

		BluetoothGattService button2s = getService(BUTTON_2_SERVICE);
		BluetoothGattCharacteristic button2c = button2s.getCharacteristic(BUTTON_2_CHARACTERISTIC);
		BluetoothGattDescriptor button2d = button2c.getDescriptor(CALLBACK_DESCRIPTOR);
		enableCharacteristicNotification(button2c, button2d, enable);
	}

	@Override
	protected void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		if (value == 0) {
			return;
		}

		if (BUTTON_2_CHARACTERISTIC.equals(characteristic.getUuid())) {
			value += 10;
		}

		logi("onCharacteristicChanged value = " + value);
		sendMessage(value);
	}

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
}
