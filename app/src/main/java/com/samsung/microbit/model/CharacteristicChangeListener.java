package com.samsung.microbit.plugin;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public interface CharacteristicChangeListener {

	void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
}
