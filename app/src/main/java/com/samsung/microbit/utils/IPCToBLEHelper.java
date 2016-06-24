package com.samsung.microbit.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.Constants;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.model.ConnectedDevice;
import com.samsung.microbit.data.model.ui.BaseActivityState;
import com.samsung.microbit.ui.PopUp;

public class IPCToBLEHelper {
    private IPCToBLEHelper() {
    }

    public static BroadcastReceiver bleHandlerReceiver(final BLEBroadcastHandlable bleBroadcastHandlable) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int error = intent.getIntExtra(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
                String firmware = intent.getStringExtra(IPCMessageManager.BUNDLE_MICROBIT_FIRMWARE);
                int getNotification = intent.getIntExtra(IPCMessageManager.BUNDLE_MICROBIT_REQUESTS, -1);

                bleBroadcastHandlable.preUpdateUi();
                //setConnectedDeviceText();
                if (firmware != null && !firmware.isEmpty()) {
                    BluetoothUtils.updateFirmwareMicrobit(context, firmware);
                    return;
                }

                int mActivityState = bleBroadcastHandlable.getActivityState();

                if (mActivityState == BaseActivityState.STATE_CONNECTING || mActivityState == BaseActivityState
                        .STATE_DISCONNECTING) {

                    if (getNotification == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_CALL ||
                            getNotification == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_SMS) {
                        bleBroadcastHandlable.logi("micro:bit application needs more permissions");
                        bleBroadcastHandlable.addPermissionRequest(getNotification);
                        return;
                    }
                    ConnectedDevice device = BluetoothUtils.getPairedMicrobit(context);
                    if (mActivityState == BaseActivityState.STATE_CONNECTING) {
                        if (error == 0) {
                            MBApp.getApp().getEchoClientManager().sendConnectStats(Constants.ConnectionState.SUCCESS, device.mfirmware_version, null);
                            BluetoothUtils.updateConnectionStartTime(context, System.currentTimeMillis());
                            //Check if more permissions were needed and request in the Application
                            if (!bleBroadcastHandlable.arePermissionsGranted()) {
                                bleBroadcastHandlable.setActivityState(BaseActivityState.STATE_IDLE);
                                PopUp.hide();
                                bleBroadcastHandlable.checkTelephonyPermissions();
                                return;
                            }
                        } else {
                            MBApp.getApp().getEchoClientManager().sendConnectStats(Constants.ConnectionState.FAIL, null, null);
                        }
                    }
                    if (error == 0 && mActivityState == BaseActivityState.STATE_DISCONNECTING) {
                        long now = System.currentTimeMillis();
                        long connectionTime = (now - device.mlast_connection_time) / 1000; //Time in seconds
                        MBApp.getApp().getEchoClientManager().sendConnectStats(Constants.ConnectionState.DISCONNECT, device.mfirmware_version, Long.toString(connectionTime));
                    }

                    bleBroadcastHandlable.setActivityState(BaseActivityState.STATE_IDLE);
                    PopUp.hide();

                    if (error != 0) {
                        String message = intent.getStringExtra(IPCMessageManager.BUNDLE_ERROR_MESSAGE);
                        bleBroadcastHandlable.logi("localBroadcastReceiver Error message = " + message);
                        /*runOnUiThread(new Runnable() {
                            @Override
                            public void run() {*/
                                MBApp application = MBApp.getApp();

                                PopUp.show(application,
                                        application.getString(R.string.micro_bit_reset_msg),
                                        application.getString(R.string.general_error_title),
                                        R.drawable.error_face, R.drawable.red_btn,
                                        PopUp.GIFF_ANIMATION_ERROR,
                                        PopUp.TYPE_ALERT, null, null);
/*                            }
                        });*/
                    }
                }
            }
        };
    }

    public interface BLEBroadcastHandlable {
        void setActivityState(int baseActivityState);
        void preUpdateUi();
        int getActivityState();
        void logi(String message);
        void checkTelephonyPermissions();
        void addPermissionRequest(int permission);
        boolean arePermissionsGranted();
    }
}
