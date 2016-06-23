package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.RawConstants;
import com.samsung.microbit.presentation.PlayAudioPresenter;
import com.samsung.microbit.ui.activity.CameraActivityPermissionChecker;

public class CameraPlugin {
    private static final String TAG = CameraPlugin.class.getSimpleName();

    private static int m_NextState = -1;
    private static int m_CurrentState = -1;

    private static PowerManager mPowerManager;
    private static PowerManager.WakeLock mWakeLock;

    private static PlayAudioPresenter playAudioPresenter = new PlayAudioPresenter();

    private static MediaPlayer.OnCompletionListener m_OnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            performOnEnd();
        }
    };

    public static void pluginEntry(Context ctx, CmdArg cmd) {
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) ctx.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
        }
        switch (cmd.getCMD()) {
            case EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE:
                mWakeLock.acquire(5 * 1000);
                m_CurrentState = EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE;
                m_NextState = EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE;

                playAudioPresenter.setNotificationForPlay(RawConstants.LAUNCH_CAMERA_AUDIO_PHOTO);
                playAudioPresenter.setCallBack(m_OnCompletionListener);
                playAudioPresenter.start();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE:
                mWakeLock.acquire(5 * 1000);
                m_CurrentState = EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE;
                m_NextState = EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE;

                playAudioPresenter.setNotificationForPlay(RawConstants.LAUNCH_CAMERA_AUDIO_VIDEO);
                playAudioPresenter.setCallBack(m_OnCompletionListener);
                playAudioPresenter.start();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_TAKE_PHOTO:
                mWakeLock.acquire(5 * 1000);
                m_NextState = EventSubCodes.SAMSUNG_CAMERA_EVT_TAKE_PHOTO;
                performOnEnd();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE:
                mWakeLock.acquire(5 * 1000);
                m_NextState = EventSubCodes.SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE;
                performOnEnd();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_STOP_VIDEO_CAPTURE:
                mWakeLock.acquire(5 * 1000);
                recVideoStop();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_STOP_PHOTO_MODE:
            case EventSubCodes.SAMSUNG_CAMERA_EVT_STOP_VIDEO_MODE:
                closeCamera();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_TOGGLE_FRONT_REAR:
                mWakeLock.acquire(5 * 1000);
                toggleCamera();
                break;
            default:
                break;
        }
    }

    public static void sendReplyCommand(int mbsService, CmdArg cmd) {
        if (IPCMessageManager.getInstance().getClientMessenger() != null) {
            Message msg = Message.obtain(null, mbsService);
            Bundle bundle = new Bundle();
            bundle.putInt("cmd", cmd.getCMD());
            bundle.putString("value", cmd.getValue());
            msg.setData(bundle);

            try {
                IPCMessageManager.getInstance().getClientMessenger().send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private static void performOnEnd() {
        Log.d("CameraPlugin", "Next state - " + m_NextState);
        switch (m_NextState) {
            case EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE:
                launchCameraForPic();
                break;
            case EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE:
                launchCameraForVideo();
                break;
            case EventSubCodes.SAMSUNG_CAMERA_EVT_TAKE_PHOTO:
                takePic();
                break;
            case EventSubCodes.SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE:
                recVideoStart();
                break;

        }
    }

    //This function should trigger an Activity that would be responsible of starting the camera app to take a picture.
    //The same activity should also store the result of the camera app, if valid
    private static void launchCameraForPic() {
        Context context = MBApp.getApp();

        Intent mIntent = new Intent(context, CameraActivityPermissionChecker.class);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mIntent.setAction("com.samsung.microbit.activity.CameraActivity.action.OPEN_FOR_PIC");
        context.startActivity(mIntent);
    }

    private static void takePic() {
        Intent intent = new Intent("TAKE_PIC");
        MBApp.getApp().sendBroadcast(intent);
    }

    private static void toggleCamera() {
        Intent intent = new Intent("TOGGLE_CAMERA");
        MBApp.getApp().sendBroadcast(intent);
    }

    private static void launchCameraForVideo() {
        Context context = MBApp.getApp();

        Intent mIntent = new Intent(context, CameraActivityPermissionChecker.class);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mIntent.setAction("com.samsung.microbit.activity.CameraActivity.action.OPEN_FOR_VIDEO");
        context.startActivity(mIntent);
    }

    private static void recVideoStart() {
        Intent intent = new Intent("START_VIDEO");
        MBApp.getApp().sendBroadcast(intent);
    }

    private static void recVideoStop() {
        Intent intent = new Intent("STOP_VIDEO");
        MBApp.getApp().sendBroadcast(intent);
    }

    private static void closeCamera() {
        Intent intent = new Intent("CLOSE");
        MBApp.getApp().sendBroadcast(intent);
    }
}
