/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsung.microbit;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = "MicroBit.BluetoothLeService";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =  "com.samsung.microbit.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.samsung.microbit.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.samsung.microbit.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.samsung.microbit.ACTION_DATA_AVAILABLE";
    public final static String ACTION_ON_CHARACTERISTIC_WRITE = "com.samsung.microbit.ACTION_ON_CHARACTERISTIC_WRITE";
    public final static String ACTION_PROGRESS_UPDATE = "com.samsung.microbit.ACTION_PROGRESS_UPDATE";
    public final static String ACTION_ON_ERROR = "com.samsung.microbit.ACTION_ON_ERROR";
    public final static String EXTRA_DATA = "com.samsung.microbit.EXTRA_DATA";

    public final static UUID UUID_DFU_SERVICE = UUID.fromString(SampleGattAttributes.DEVICE_FIRMWARE_UPDATE);

    public final static UUID DFU_PACKET_UUID = UUID.fromString(SampleGattAttributes.DFU_PACKET);
	private static final int MAX_PACKET_SIZE = 20; // the maximum number of bytes in one packet is 20. May be less.
	private final byte[] mBuffer = new byte[MAX_PACKET_SIZE];

    
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
        @Override
		public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        	if (status == BluetoothGatt.GATT_SUCCESS) {
        		if (DFU_PACKET_UUID.equals(characteristic.getUuid())) {
    				/*
    				 * This method is called when either a CONTROL POINT or PACKET characteristic has been written.
    				 * If it is the CONTROL POINT characteristic, just set the {@link mRequestCompleted} flag to true. The main thread will continue its task when notified.
    				 * If the PACKET characteristic was written we must:
    				 * - if the image size was written in DFU Start procedure, just set flag to true
    				 * otherwise
    				 * - send the next packet, if notification is not required at that moment, or
    				 * - do nothing, because we have to wait for the notification to confirm the data received
    				 */
					if (mBytesSent ==0 ) progressUpdate(ACTION_PROGRESS_UPDATE,"Starting to Send file now. Fingers crossed");
        			sendFile(characteristic);
        		}
        		else {
    				/*
    				 * If a Reset (Op Code = 6) or Activate and Reset (Op Code = 5) commands are sent, the DFU target resets and sometimes does it so quickly that does not manage to send
    				 * any ACK to the controller and error 133 is thrown here. This bug should be fixed in SDK 8.0+ where the target would gracefully disconnect before restarting.
    				 */
        			broadcastUpdate(ACTION_ON_ERROR, characteristic);
    			}
    		}
        }        
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void progressUpdate(final String action, String progress) {
			final Intent intent = new Intent(action);
			intent.putExtra(EXTRA_DATA, progress);
			sendBroadcast(intent);
    }
    
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

	private int m_imageSizeInBytes = 0 ;
	private int mBytesSent = 0 ;

	private FileInputStream initIs;

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic){
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return false;
		}
		return mBluetoothGatt.writeCharacteristic(characteristic);
    }
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

	public void sendFile(BluetoothGattCharacteristic characteristic) {
		if (m_imageSizeInBytes == 0 ){
			try {
				initIs = new FileInputStream(FlashSectionFragment.BINARY_FILE_NAME);
				m_imageSizeInBytes = initIs.available();
	            Log.i(TAG, "sendFile - Imagesize = " + m_imageSizeInBytes);
			} catch (Exception e ) {
				broadcastUpdate(ACTION_ON_ERROR, characteristic);
				return ;
			}
		}
		mBytesSent += characteristic.getValue().length;
        Log.i(TAG, "sendFile - mBytesSent = " + mBytesSent);
		final boolean lastPacketTransferred = mBytesSent == m_imageSizeInBytes;
		if (lastPacketTransferred){
			broadcastUpdate(ACTION_ON_CHARACTERISTIC_WRITE, characteristic);
			return ;
		}
		//Transfer the file
		final byte[] buffer = mBuffer;
		try {
			final int size = initIs.read(buffer);
			byte[] locBuffer = buffer;
			if (buffer.length != size) {
				locBuffer = new byte[size];
				System.arraycopy(buffer, 0, locBuffer, 0, size);
				characteristic.setValue(locBuffer);
				if (mBluetoothGatt.writeCharacteristic(characteristic)){
					progressUpdate(ACTION_PROGRESS_UPDATE,String.valueOf(mBytesSent));
				} else {
					broadcastUpdate(ACTION_ON_ERROR, characteristic);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
