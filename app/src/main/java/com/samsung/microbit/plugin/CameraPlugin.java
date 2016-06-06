package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.RawConstants;
import com.samsung.microbit.presentation.PlayAudioPresenter;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.ui.activity.CameraActivityPermissionChecker;

public class CameraPlugin {

	private static Context context = null;

	private static final String TAG = "CameraPlugin";

    private static int m_NextState = -1 ;
	private static int m_CurrentState = -1;

    private static PowerManager mPowerManager;
    private static PowerManager.WakeLock mWakeLock;

    private static PlayAudioPresenter playAudioPresenter = new PlayAudioPresenter();

    private static MediaPlayer.OnCompletionListener m_OnCompletionListener = new MediaPlayer.OnCompletionListener()
    {
        @Override
        public void onCompletion(MediaPlayer mp) {
            performOnEnd();
        }
    };

	public static void pluginEntry(Context ctx, CmdArg cmd) {

		context = ctx;

        if (mPowerManager == null)
        {
            mPowerManager = (PowerManager) ctx.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
        }
		switch (cmd.getCMD()) {
			case Constants.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE:
                mWakeLock.acquire(5*1000);
                m_CurrentState = Constants.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE ;
                m_NextState = Constants.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE ;

                playAudioPresenter.setNotificationForPlay(RawConstants.LAUNCH_CAMERA_AUDIO_PHOTO);
                playAudioPresenter.setCallBack(m_OnCompletionListener);
                playAudioPresenter.start();
				break;

			case Constants.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE:
                mWakeLock.acquire(5*1000);
                m_CurrentState = Constants.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE ;
                m_NextState = Constants.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE ;

                playAudioPresenter.setNotificationForPlay(RawConstants.LAUNCH_CAMERA_AUDIO_VIDEO);
                playAudioPresenter.setCallBack(m_OnCompletionListener);
                playAudioPresenter.start();
				break;

			case Constants.SAMSUNG_CAMERA_EVT_TAKE_PHOTO:
                mWakeLock.acquire(5*1000);
                m_NextState = Constants.SAMSUNG_CAMERA_EVT_TAKE_PHOTO ;
                performOnEnd();
				break;

			case Constants.SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE:
                mWakeLock.acquire(5*1000);
                m_NextState = Constants.SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE ;
                performOnEnd();
				break;

			case Constants.SAMSUNG_CAMERA_EVT_STOP_VIDEO_CAPTURE:
                mWakeLock.acquire(5*1000);
                recVideoStop();
				break;

			case Constants.SAMSUNG_CAMERA_EVT_STOP_PHOTO_MODE:
			case Constants.SAMSUNG_CAMERA_EVT_STOP_VIDEO_MODE:
                closeCamera();
				break;

			case Constants.SAMSUNG_CAMERA_EVT_TOGGLE_FRONT_REAR:
                mWakeLock.acquire(5*1000);
                toggleCamera();
				break;
			default:
				break;
		}
	}

	public static void sendReplyCommand(int mbsService, CmdArg cmd) {
		if (PluginService.mClientMessenger != null) {
			Message msg = Message.obtain(null, mbsService);
			Bundle bundle = new Bundle();
			bundle.putInt("cmd", cmd.getCMD());
			bundle.putString("value", cmd.getValue());
			msg.setData(bundle);

			try {
				PluginService.mClientMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

    private static void performOnEnd()
    {
        Log.d("CameraPlugin" , "Next state - " + m_NextState);
        switch (m_NextState)
        {
            case Constants.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE:
                launchCameraForPic();
                break;
            case Constants.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE:
                launchCameraForVideo();
                break;
            case Constants.SAMSUNG_CAMERA_EVT_TAKE_PHOTO:
                takePic();
                break;
            case Constants.SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE:
                recVideoStart();
                break;

        }
    }
	//This function should trigger an Activity that would be responsible of starting the camera app to take a picture.
	//The same activity should also store the result of the camera app, if valid
	private static void launchCameraForPic() {
        Intent mIntent = new Intent(context, CameraActivityPermissionChecker.class);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mIntent.setAction("com.samsung.microbit.activity.CameraActivity.action.OPEN_FOR_PIC");
		context.startActivity(mIntent);
	}

	private static void takePic() {
		Intent intent = new Intent("TAKE_PIC");
		context.sendBroadcast(intent);
	}

    private static void toggleCamera(){
        Intent intent = new Intent("TOGGLE_CAMERA");
        context.sendBroadcast(intent);
    }

	private static void launchCameraForVideo() {
        Intent mIntent = new Intent(context, CameraActivityPermissionChecker.class);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mIntent.setAction("com.samsung.microbit.activity.CameraActivity.action.OPEN_FOR_VIDEO");
		context.startActivity(mIntent);
	}

	private static void recVideoStart() {
		Intent intent = new Intent("START_VIDEO");
		context.sendBroadcast(intent);
	}

	private static void recVideoStop() {
		Intent intent = new Intent("STOP_VIDEO");
		context.sendBroadcast(intent);
	}

	private static void closeCamera() {
		Intent intent = new Intent("CLOSE");
		context.sendBroadcast(intent);
	}
}
