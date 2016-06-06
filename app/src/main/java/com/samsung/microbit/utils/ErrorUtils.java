package com.samsung.microbit.utils;

import android.bluetooth.BluetoothGatt;

import com.samsung.microbit.service.DfuService;

import no.nordicsemi.android.dfu.DfuBaseService;

public class ErrorUtils {
    private ErrorUtils() {
    }

    public static String broadcastGetErrorMessage(int errorCode) {
        switch (errorCode) {
            case 0x0001:
                return "GATT INVALID HANDLE";

            case 0x0002:
                return "GATT READ NOT PERMIT";

            case 0x0003:
                return "GATT WRITE NOT PERMIT";

            case 0x0004:
                return "GATT INVALID PDU";

            case 0x0005:
                return "GATT INSUF AUTHENTICATION";

            case 0x0006:
                return "GATT REQ NOT SUPPORTED";

            case 0x0007:
                return "GATT INVALID OFFSET";

            case 0x0008:
                return "GATT INSUF AUTHORIZATION";

            case 0x0009:
                return "GATT PREPARE Q FULL";

            case 0x000a:
                return "GATT NOT FOUND";

            case 0x000b:
                return "GATT NOT LONG";

            case 0x000c:
                return "GATT INSUF KEY SIZE";

            case 0x000d:
                return "GATT INVALID ATTR LEN";

            case 0x000e:
                return "GATT ERR UNLIKELY";

            case 0x000f:
                return "GATT INSUF ENCRYPTION";

            case 0x0010:
                return "GATT UNSUPPORT GRP TYPE";

            case 0x0011:
                return "GATT INSUF RESOURCE";

            case 0x0087:
                return "GATT ILLEGAL PARAMETER";

            case 0x0080:
                return "GATT NO RESOURCES";

            case 0x0081:
                return "GATT INTERNAL ERROR";

            case 0x0082:
                return "GATT WRONG STATE";

            case 0x0083:
                return "GATT DB FULL";

            case 0x0084:
                return "GATT BUSY";

            case 0x0085:
                return "Cannot connect to micro:bit (GATT error). Please retry.";

            case 0x0086:
                return "GATT CMD STARTED";

            case 0x0088:
                return "GATT PENDING";

            case 0x0089:
                return "GATT AUTH FAIL";

            case 0x008a:
                return "GATT MORE";

            case 0x008b:
                return "GATT INVALID CFG";

            case 0x008c:
                return "GATT SERVICE STARTED";

            case 0x008d:
                return "GATT ENCRYPTED NO MITM";

            case 0x008e:
                return "GATT NOT ENCRYPTED";

            case 0x01FF:
                return "GATT VALUE OUT OF RANGE";

            case 0x0101:
                return "TOO MANY OPEN CONNECTIONS";

            case 0x00FF:
                return "DFU SERVICE DISCOVERY NOT STARTED";

            case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                return "GATT CONNECTION CONGESTED";

            case DfuService.ERROR_DEVICE_DISCONNECTED:
                return "micro:bit disconnected";

            case DfuService.ERROR_FILE_NOT_FOUND:
                return "File not found. Please retry.";
            case DfuService.ERROR_FILE_ERROR:
                return "Unable to open file. Please retry.";
            case DfuService.ERROR_FILE_INVALID:
                return "File not a valid HEX. Please retry.";
            case DfuService.ERROR_FILE_IO_EXCEPTION:
                return "Unable to read file";
            case DfuService.ERROR_SERVICE_DISCOVERY_NOT_STARTED:
                return "Bluetooth Discovery not started";
            case DfuService.ERROR_SERVICE_NOT_FOUND:
                return "Dfu Service not found. Please retry.";
            case DfuService.ERROR_CHARACTERISTICS_NOT_FOUND:
                return "Dfu Characteristics not found. Please retry.";
            case DfuService.ERROR_INVALID_RESPONSE:
                return "Invalid response from micro:bit. Please retry.";
            case DfuService.ERROR_FILE_TYPE_UNSUPPORTED:
                return "Unsupported file type. Please retry.";
            case DfuService.ERROR_BLUETOOTH_DISABLED:
                return "Bluetooth Disabled";

            case DfuService.ERROR_FILE_SIZE_INVALID:
                return "Invalid filesize. Please retry.";

            default:
                if ((DfuBaseService.ERROR_REMOTE_MASK & errorCode) > 0) {
                    switch (errorCode & (~DfuBaseService.ERROR_REMOTE_MASK)) {
                        case DfuBaseService.DFU_STATUS_INVALID_STATE:
                            return "REMOTE DFU INVALID STATE";
                        case DfuBaseService.DFU_STATUS_NOT_SUPPORTED:
                            return "REMOTE DFU NOT SUPPORTED";
                        case DfuBaseService.DFU_STATUS_DATA_SIZE_EXCEEDS_LIMIT:
                            return "REMOTE DFU DATA SIZE EXCEEDS LIMIT";
                        case DfuBaseService.DFU_STATUS_CRC_ERROR:
                            return "REMOTE DFU INVALID CRC ERROR";
                        case DfuBaseService.DFU_STATUS_OPERATION_FAILED:
                            return "REMOTE DFU OPERATION FAILED";
                    }
                }
                return "UNKNOWN (" + errorCode + ")";
        }
    }
}
