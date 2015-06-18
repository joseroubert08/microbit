package com.samsung.microbit.model;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public interface CharacteristicChangeListener {

	void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
}
