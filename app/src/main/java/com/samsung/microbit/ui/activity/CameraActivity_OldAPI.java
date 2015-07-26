package com.samsung.microbit.ui.activity;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Rect;
import 	android.os.SystemClock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.AutoFocusCallback;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceView;
import android.view.Surface;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import android.drm.DrmManagerClient.OnInfoListener;
import android.media.MediaScannerConnection;

import com.samsung.microbit.R;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.ui.view.CameraPreview;

/**
 * Created by t.maestri on 09/06/2015.
 */
public class CameraActivity_OldAPI extends Activity {

	private static boolean mInstanceActive = false;

	private CameraPreview mPreview;
	private ImageButton mButtonClick;
	private Camera mCamera;
	private int mCameraIdx;
	private BroadcastReceiver mMessageReceiver;
	private boolean mVideo = false;
	private boolean mIsRecording = false;
	private MediaRecorder mMediaRecorder;
	private File mVideoFile = null;

	private static final String TAG = "CameraActivity_OldAPI";
	private boolean debug = false;

	void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	private int getFrontFacingCamera() {
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				return camIdx;
			}
		}
		return -1;
	}

	private void setButtonForPicture() {

		mButtonClick.setBackgroundResource(R.drawable.camera_icon);
		mButtonClick.invalidate();
		mButtonClick.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
			}
		});

		mButtonClick.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				mCamera.autoFocus(new AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean arg0, Camera arg1) {
						//TODO Is there anything we have to do after autofocus?
					}
				});
				return true;
			}
		});

	}

	private void setPreviewForPicture() {
		mPreview.setSoundEffectsEnabled(false);
		mPreview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCamera.autoFocus(new AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean arg0, Camera arg1) {
						//TODO Is there anything we have to do after autofocus?
					}
				});
			}
		});
	}

	private void setButtonForVideo() {

		mButtonClick.setBackgroundResource(R.drawable.start_record_icon);
		mButtonClick.invalidate();
		mButtonClick.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mIsRecording) {
					// stop recording and release mCamera
					mMediaRecorder.stop(); // stop the recording
					refreshGallery(mVideoFile);
					releaseMediaRecorder(); // release the MediaRecorder object
					mIsRecording = false;
					mButtonClick.setBackgroundResource(R.drawable.start_record_icon);
					releaseMediaRecorder();
//					finish();
					resetCam();
				} else {
					if (!prepareMediaRecorder()) {
						sendCameraError();
						Log.e(TAG,"Error preparing mediaRecorder");
						finish();
					}

					mButtonClick.setBackgroundResource(R.drawable.stop_record_icon);
					mButtonClick.invalidate();

					//TODO Check that is true
					// work on UiThread for better performance
					runOnUiThread(new Runnable() {
						public void run() {
							try {
								mMediaRecorder.start();
							} catch (final Exception ex) {
								sendCameraError();
								//TODO Check that can be used
								Log.e(TAG, "Error during video recording",ex);
								finish();
							}
						}
					});

					mIsRecording = true;
				}
			}
		});
	}

	private void setPreviewForVideo() {

	}

	private void sendCameraError() {
		CmdArg cmd = new CmdArg(0,"Camera Error");
		CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		logi("onCreate() :: Start");
		super.onCreate(savedInstanceState);

		mCamera = null;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.camera_old_api);
		Intent intent = getIntent();
		if(intent.getAction().contains("OPEN_FOR_PIC")) {
			mVideo = false;
		}else if(intent.getAction().contains("OPEN_FOR_VIDEO")) {
			mVideo = true;
		}

		SurfaceView mSurfaceView = new SurfaceView(this);
		mPreview = new CameraPreview(this, mSurfaceView);
		mPreview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		((FrameLayout) findViewById(R.id.camera_preview_container)).addView(mPreview);
		mPreview.setKeepScreenOn(true);
		mPreview.setParentActivity(this);

		mButtonClick = (ImageButton) findViewById(R.id.picture);
//		mButtonClick.bringToFront();
//		((ImageView)findViewById(R.id.bbcLogo)).bringToFront();

		if(mVideo) {
			//Setup specific to OPEN_FOR_VIDEO
			setButtonForVideo();
			setPreviewForVideo();
		}
		else{
			//Setup specific to OPEN_FOR_PIC
			setButtonForPicture();
			setPreviewForPicture();
		}

		mMessageReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals("CLOSE")){
					finish();
				}
				else if(!mVideo && intent.getAction().equals("TAKE_PIC")) {
					mButtonClick.callOnClick();
				}
				else if(mVideo && !mIsRecording && intent.getAction().equals("START_VIDEO")) {
					mButtonClick.callOnClick();
				}
				else if(mVideo && mIsRecording && intent.getAction().equals("STOP_VIDEO")) {
					mButtonClick.callOnClick();
				}
				else {
					//Wrong sequence of commands
					CmdArg cmd = new CmdArg(0,"Wrong Camera Command Sequence");
					CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
					Log.e(TAG, "Wrong command sequence");
				}
			}
		};

		logi("onCreate() :: Done");
	}


    public int getCameraDisplayOrientation(int cameraId, android.hardware.Camera mCamera)
    {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else
        { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

	@Override
	protected void onResume() {
		logi("onCreate() :: onResume");

		super.onResume();
		mCameraIdx = getFrontFacingCamera();
		try {
			mCamera = Camera.open(mCameraIdx);
			mPreview.setCamera(mCamera, mCameraIdx);

			if(mVideo){
				this.registerReceiver(mMessageReceiver, new IntentFilter("START_VIDEO"));
				this.registerReceiver(mMessageReceiver, new IntentFilter("STOP_VIDEO"));
			}
			else {
				this.registerReceiver(mMessageReceiver, new IntentFilter("TAKE_PIC"));
			}

			this.registerReceiver(mMessageReceiver, new IntentFilter("CLOSE"));

			logi("onCreate() :: onResume # ");
		} catch (RuntimeException ex) {
			Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
			sendCameraError();
			finish();
		}
	}

	@Override
	protected void onPause() {

		logi("onCreate() :: onPause");
		this.unregisterReceiver(
				mMessageReceiver);
		if (mCamera != null) {
			mCamera.stopPreview();
			mPreview.setCamera(null,-1);
			mCamera.release();
			mCamera = null;
		}
		super.onPause();
	}

	private void resetCam() {
		mCamera.startPreview();
		//mPreview.setCamera(mCamera);
	}

	private void refreshGallery(File file) {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		sendBroadcast(mediaScanIntent);
	}

	//TODO Add Sound here
	//Currently if the device is on silent mode no sound is going to be heard
	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			//			 Log.d(TAG, "onShutter'd");
			((ImageView) findViewById(R.id.blink_rectangle)).setVisibility(View.VISIBLE);
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			//			 Log.d(TAG, "onPictureTaken - raw");
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			new SaveImageTask().execute(data);
			DrawBlink();
			resetCam();
//			finish();
		}
	};

	void DrawBlink(){
		SystemClock.sleep(500);
		((ImageView) findViewById(R.id.blink_rectangle)).setVisibility(View.GONE);
	}

	@Override
	protected void onStart() {
		//Informing microbit that the mCamera is active now
		CmdArg cmd = new CmdArg(0, "Camera on");
		CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		//Informing microbit that the mCamera is active now
		CmdArg cmd = new CmdArg(0, "Camera off");
		CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
		super.onDestroy();
	}

	private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

		@Override
		protected Void doInBackground(byte[]... data) {
			FileOutputStream outStream = null;

			// Write to SD Card
			try {
				File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Microbit/");

				if(!dir.exists())
					dir.mkdirs();

				//TODO defining the file name
				String fileName = String.format("%d.jpg", System.currentTimeMillis());
				File outFile = new File(dir, fileName);

				outStream = new FileOutputStream(outFile);
				outStream.write(data[0]);
				outStream.flush();
				outStream.close();

				refreshGallery(outFile);

				CmdArg cmd = new CmdArg(0, "Camera picture saved");
				CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			return null;
		}

	}

	private void releaseMediaRecorder() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
		if (mMediaRecorder != null) {
			mMediaRecorder.reset(); // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			mVideoFile = null;
			//TODO Check that is not necessary
			mCamera.lock(); // lock camera for later use
		}
	}

	private boolean prepareMediaRecorder() {

		if(mCameraIdx<0 || mCamera==null)
			return false;

		int currentOrientation = getResources().getConfiguration().orientation;
		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
		}
		else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
		}

		mMediaRecorder = new MediaRecorder();

		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		//TODO Check because depending on the quality on some devices the MediaRecorder doesn't work
		if(CamcorderProfile.hasProfile(mCameraIdx,CamcorderProfile.QUALITY_HIGH))
			mMediaRecorder.setProfile(CamcorderProfile.get(mCameraIdx,CamcorderProfile.QUALITY_HIGH));
		else if(CamcorderProfile.hasProfile(mCameraIdx,CamcorderProfile.QUALITY_LOW))
			mMediaRecorder.setProfile(CamcorderProfile.get(mCameraIdx,CamcorderProfile.QUALITY_LOW));
		else {
			releaseMediaRecorder();
			Log.e(TAG, "Error preparing media Recorder: no CamcorderProfile available");
			return false;
		}

		//Setting output file
		//TODO defining the file name
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Microbit/");
		if(!dir.exists())
			dir.mkdirs();
		//TODO defining the file name
		String fileName = String.format("%d.mp4", System.currentTimeMillis());
		mVideoFile = new File(dir, fileName);
		mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());

		//Setting fiel limits
		//TODO Check File Limits
		mMediaRecorder.setMaxDuration(600000); // Set max duration 60 sec.
		mMediaRecorder.setMaxFileSize(50000000); // Set max file size 50M

		int rotation = (360-getCameraDisplayOrientation(mCameraIdx,mCamera))%360;
		mMediaRecorder.setOrientationHint(rotation);

		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			Log.e(TAG,"Error preparing media Recorder: " + e.getLocalizedMessage());
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			Log.e(TAG, "Error preparing media Recorder IOException: " + e.getLocalizedMessage());
			return false;
		}
		return true;

	}
}
