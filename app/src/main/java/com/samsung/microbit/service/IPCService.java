package com.samsung.microbit.service;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.constants.ServiceIds;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.model.ConnectedDevice;
import com.samsung.microbit.utils.ServiceUtils;
import com.samsung.microbit.utils.Utils;

import java.lang.ref.WeakReference;

public class IPCService extends Service {

    private static final String TAG = IPCService.class.getSimpleName();

    private ServiceConnector serviceConnector;

    private static final class IPCHandler extends Handler {
        private WeakReference<IPCService> ipcServiceWeakReference;

        private IPCHandler(IPCService bleService) {
            super();
            ipcServiceWeakReference = new WeakReference<>(bleService);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (ipcServiceWeakReference.get() != null) {
                ipcServiceWeakReference.get().handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serviceConnector = new ServiceConnector(this);
        serviceConnector.bindServices();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        IPCHandler ipcHandler = new IPCHandler(this);
        serviceConnector.setClientHandler(ipcHandler);

        return serviceConnector.mClientMessenger.getBinder();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if(serviceConnector != null) {
            Log.i(TAG, "Unbind services");
            serviceConnector.unbindServices();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int category = intent.getIntExtra(IPCConstants.INTENT_TYPE, EventCategories.CATEGORY_UNKNOWN);

        if(category == EventCategories.CATEGORY_UNKNOWN) {
            Log.e(TAG, "Unknown category");
            return START_REDELIVER_INTENT;
        }

        switch (category) {
            case EventCategories.IPC_START_PROCESS: {
                Log.i(TAG, "Process started");
                break;
            }
            case EventCategories.IPC_BLE_CONNECT: {
                Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                        .IPC_BLE_CONNECT, ServiceIds.SERVICE_BLE, null, null);

                if (message != null) {
                    message.arg2 = MBApp.getApp().isJustPaired() ? IPCConstants.JUST_PAIRED : IPCConstants
                            .PAIRED_EARLIER;
                    handleMessage(message);
                }
                break;
            }
            case EventCategories.IPC_BLE_DISCONNECT: {
                Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                        .IPC_BLE_DISCONNECT, ServiceIds.SERVICE_BLE, null, null);

                if (message != null) {
                    handleMessage(message);
                }
                break;
            }
            case EventCategories.CATEGORY_REPLY: {
                sendReplyCommand(intent.getIntExtra(IPCConstants.INTENT_MBS_SERVICE, 0), intent.getIntExtra(IPCConstants.INTENT_REPLY_TO, ServiceIds.SERVICE_NONE), (CmdArg) intent.getParcelableExtra(IPCConstants.INTENT_CMD_ARG));
                break;
            }
            case EventCategories.IPC_PLUGIN_STOP_PLAYING: {
                Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID,
                        EventCategories.IPC_PLUGIN_STOP_PLAYING, ServiceIds.SERVICE_PLUGIN, null, null);
                handleMessage(message);
                break;
            }
            case EventCategories.IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED: {
                Message message = ServiceUtils.composeBLECharacteristicMessage(intent.getIntExtra(IPCConstants.INTENT_CHARACTERISTIC_MESSAGE, 0));
                message.arg2 = ServiceIds.SERVICE_PLUGIN;
                handleMessage(message);
                break;
            }
            case EventCategories.CATEGORY_SIMULATE: {
                Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, BLEService.SIMULATE, ServiceIds.SERVICE_BLE, null, null);
                handleMessage(message);
                break;
            }
            default:
                Log.e(TAG, "Unknown category");
        }

        return START_REDELIVER_INTENT;
    }

    private void handleMessage(Message message) {
        String replyToServiceName = null;
        switch (message.arg2) {
            case ServiceIds.SERVICE_PLUGIN:
                replyToServiceName = PluginService.class.getName();
                break;
            case ServiceIds.SERVICE_BLE:
                replyToServiceName = BLEService.class.getName();
                break;
        }

        if(replyToServiceName != null) {
            ServiceUtils.IMessengerFinder messengerFinder = serviceConnector.getConnection();

            if(messengerFinder != null) {
                Messenger messenger = messengerFinder.getMessengerForService(replyToServiceName);
                if (messenger != null) {
                    Message newMessage = ServiceUtils.copyMessageFromOld(message, ServiceIds.SERVICE_NONE);
                    newMessage.replyTo = serviceConnector.mClientMessenger;
                    try {
                        messenger.send(newMessage);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        } else {
            if (message.arg1 == EventCategories.IPC_BLE_NOTIFICATION_GATT_CONNECTED ||
                    message.arg1 == EventCategories.IPC_BLE_NOTIFICATION_GATT_DISCONNECTED) {
                Context appContext = MBApp.getApp();

                ConnectedDevice cd = BluetoothUtils.getPairedMicrobit(MBApp.getApp());
                cd.mStatus = (message.arg1 == EventCategories.IPC_BLE_NOTIFICATION_GATT_CONNECTED);
                BluetoothUtils.setPairedMicroBit(appContext, cd);

                Bundle messageData = message.getData();

                int errorCode = (int) messageData.getSerializable(IPCConstants.BUNDLE_ERROR_CODE);

                String error_message = (String) messageData.getSerializable(IPCConstants.BUNDLE_ERROR_MESSAGE);

                String firmware = (String) messageData.getSerializable(IPCConstants.BUNDLE_MICROBIT_FIRMWARE);

                int microbitRequest = -1;
                if (messageData.getSerializable(IPCConstants.BUNDLE_MICROBIT_REQUESTS) != null) {
                    microbitRequest = (int) messageData.getSerializable(IPCConstants.BUNDLE_MICROBIT_REQUESTS);
                }

                Intent intent = new Intent(IPCConstants.INTENT_BLE_NOTIFICATION);
                intent.putExtra(IPCConstants.NOTIFICATION_CAUSE, message.arg1);
                intent.putExtra(IPCConstants.BUNDLE_ERROR_CODE, errorCode);
                intent.putExtra(IPCConstants.BUNDLE_ERROR_MESSAGE, error_message);
                intent.putExtra(IPCConstants.BUNDLE_MICROBIT_FIRMWARE, firmware);
                intent.putExtra(IPCConstants.BUNDLE_MICROBIT_REQUESTS, microbitRequest);

                LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
            }
        }
    }

    /**
     * Send some reply message to the ipc.
     *
     * @param mbsService MbsService of reply.
     * @param cmd Command should be sent, as reply.
     */
    private void sendReplyCommand(int mbsService, int replyTo, CmdArg cmd) {
            Message msg = Message.obtain(null, mbsService);
            Bundle bundle = new Bundle();
            bundle.putInt("cmd", cmd.getCMD());
            bundle.putString("value", cmd.getValue());
            msg.setData(bundle);
        msg.arg2 = replyTo;
        handleMessage(msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serviceConnector.unbindServices();
    }
}
