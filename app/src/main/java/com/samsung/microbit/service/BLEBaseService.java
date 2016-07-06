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

import com.samsung.microbit.core.bluetooth.BLEManager;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.core.bluetooth.CharacteristicChangeListener;
import com.samsung.microbit.core.bluetooth.UnexpectedConnectionEventListener;

import java.util.List;
import java.util.UUID;

import static com.samsung.microbit.BuildConfig.DEBUG;

public abstract class BLEBaseService extends Service {
    private static final String TAG = BLEBaseService.class.getSimpleName();

    public static final boolean AUTO_CONNECT = true;

    private BLEManager bleManager;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private String deviceAddress;

    private int actualError = 0;

    private void logi(String message) {
        if (DEBUG) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    protected BLEManager getBleManager() {
        return bleManager;
    }

    protected BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    protected void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) {
            logi("onStartCommand()");
        }

        return START_STICKY;
    }

    protected boolean reset() {
        boolean rc = false;
        if (bleManager != null) {
            disconnectAll();
            rc = bleManager.reset();
            if (rc) {
                bleManager = null;
            }
        }

        return rc;
    }

    protected void setupBLE() {
        if (DEBUG) {
            logi("setupBLE()");
        }

        this.deviceAddress = getDeviceAddress();

        if (DEBUG) {
            logi("setupBLE() :: deviceAddress = " + deviceAddress);
        }

        if (this.deviceAddress != null) {
            bluetoothDevice = null;
            if (initialize()) {
                bleManager = new BLEManager(getApplicationContext(), bluetoothDevice,
                        new CharacteristicChangeListener() {
                            @Override
                            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

                                if (DEBUG) {
                                    logi("setupBLE().CharacteristicChangeListener.onCharacteristicChanged()");
                                }

                                if (bleManager != null) {
                                    handleCharacteristicChanged(gatt, characteristic);
                                }
                            }
                        },
                        new UnexpectedConnectionEventListener() {
                            @Override
                            public void handleConnectionEvent(int event, boolean gattForceClosed) {
                                if (DEBUG) {
                                    logi("setupBLE().CharacteristicChangeListener.handleUnexpectedConnectionEvent()"
                                            + event);
                                }

                                if (bleManager != null) {
                                    handleUnexpectedConnectionEvent(event, gattForceClosed);
                                }
                            }
                        });
                startupConnection();
                return;
            }
        }

        setNotification(false, 1);
    }

    private boolean initialize() {
        if (DEBUG) {
            logi("initialize() :: remoteDevice = " + deviceAddress);
        }

        boolean rc = true;

        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            rc = bluetoothManager != null;
        }

        if (rc && (bluetoothAdapter == null)) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            rc = bluetoothAdapter != null;
        }

        if (rc && (bluetoothDevice == null)) {
            if (deviceAddress != null) {
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
                rc = bluetoothAdapter != null;
            } else {
                rc = false;
            }
        }

        if (DEBUG) {
            logi("initialize() :: complete rc = " + rc);
        }

        return rc;
    }

    protected abstract void startupConnection();

    protected abstract void disconnectAll();

    protected abstract String getDeviceAddress();

    protected abstract void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    protected abstract void handleUnexpectedConnectionEvent(int event, boolean gattForceClosed);

    protected abstract void setNotification(boolean isConnected, int errorCode);

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy called");
    }

    protected BluetoothGattService getService(UUID uuid) {
        if (bleManager != null) {
            return bleManager.getService(uuid);
        }

        return null;
    }

    protected List<BluetoothGattService> getServices() {
        if (bleManager != null) {
            return bleManager.getServices();
        }

        return null;
    }

    private int interpretCode(int rc, int goodCode) {
        if (rc > 0) {
            if ((rc & BLEManager.BLE_ERROR_FAIL) != 0) {
                actualError = bleManager.getExtendedError();
                if ((rc & BLEManager.BLE_ERROR_TIMEOUT) != 0) {
                    rc = 10;
                } else {
                    rc = 99;
                }
            } else {
                actualError = 0;
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

    private int interpretCode(int rc) {
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

    protected int getError() {
        int rc = 99;

        if (bleManager != null) {
            rc = bleManager.getError();
        }

        return rc;
    }

    protected boolean isConnected() {
        return bleManager != null && bleManager.isConnected();
    }

    protected int getBleState() {
        int rc = 99;

        if (bleManager != null) {
            rc = bleManager.getBleState();
        }

        return rc;
    }

    protected int connect() {
        int rc = 99;

        if (bleManager != null) {
            rc = bleManager.connect(AUTO_CONNECT);
            rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
        }

        return rc;
    }

    protected int disconnect() {
        int rc = 99;

        if (bleManager != null) {
            rc = bleManager.disconnect();
            rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
        }

        return rc;
    }

    protected int waitDisconnect() {
        int rc = 99;

        if (bleManager != null) {
            rc = bleManager.waitDisconnect();
            rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
        }

        return rc;
    }

    protected int discoverServices() {
        int rc = 99;

        if (bleManager != null) {
            if (DEBUG) {
                logi("discoverServices() :: bleManager != null");
            }

            rc = bleManager.discoverServices();
            rc = interpretCode(rc, BLEManager.BLE_SERVICES_DISCOVERED);
        }

        return rc;
    }

    protected int enableCharacteristicNotification(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor
            descriptor, boolean enable) {
        int rc = 99;

        if (bleManager != null) {
            rc = bleManager.enableCharacteristicNotification(characteristic, descriptor, enable);
        }

        return rc;
    }

    protected int writeDescriptor(BluetoothGattDescriptor descriptor) {
        int rc = 99;

        if (bleManager != null) {
            rc = bleManager.writeDescriptor(descriptor);
            rc = interpretCode(rc);
        }

        return rc;
    }

    protected BluetoothGattDescriptor readDescriptor(BluetoothGattDescriptor descriptor) {
        if (bleManager != null) {
            int rc = bleManager.readDescriptor(descriptor);
            rc = interpretCode(rc);
            if (rc == 0) {
                return bleManager.getLastDescriptor();
            }
        }

        return null;
    }

    protected int writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        int rc = 99;

        if (bleManager != null) {
            rc = bleManager.writeCharacteristic(characteristic);
            rc = interpretCode(rc);

            logi("Data written to " + characteristic.getUuid() + " value : (0x)" + BluetoothUtils.parse
                    (characteristic) + " Return Value = 0x" + Integer.toHexString(rc));
        }
        return rc;
    }

    protected BluetoothGattCharacteristic readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bleManager != null) {
            int rc = bleManager.readCharacteristic(characteristic);
            rc = interpretCode(rc);
            if (rc == 0) {
                return bleManager.getLastCharacteristic();
            }
        }

        return null;
    }

    protected int getActualError() {
        return actualError;
    }

    protected String getInitializedDeviceAddress() {
        return deviceAddress;
    }
}
