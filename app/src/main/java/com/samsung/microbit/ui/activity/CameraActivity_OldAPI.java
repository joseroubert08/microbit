package com.samsung.microbit.activity;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.samsung.microbit.R;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.service.PluginService;

/**
 * Created by t.maestri on 09/06/2015.
 */
public class CameraActivity_OldAPI extends Activity {

	private static boolean mInstanceActive = false;

	private CameraPreview mPreview;
	private Button mButtonClick;
	private Camera mCamera;
	private BroadcastReceiver mMessageReceiver;
	private boolean mVideo = false;
	private boolean mIsRecording = false;
	private MediaRecorder mMediaRecorder;
	private File mVideoFile = null;

	private static final String TAG = "CameraActivity_OldAPI";
	private boolean debug = true;

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

		mButtonClick.setText(R.string.picture);
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

		mButtonClick.setText(R.string.record);
		mButtonClick.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mIsRecording) {
					// stop recording and release mCamera
					mMediaRecorder.stop(); // stop the recording
					refreshGallery(mVideoFile);
					releaseMediaRecorder(); // release the MediaRecorder object
					mIsRecording = false;
					finish();
				} else {
					if (!prepareMediaRecorder()) {
						sendCameraError();
						Log.e(TAG,"Error preparing mediaRecorder");
						finish();
					}

					mButtonClick.setText(R.string.stop);

					//TODO Check that is true
					// work on UiThread for better performance
					runOnUiThread(new Runnable() {
						public void run() {
							try {
								mMediaRecorder.start();
							} catch (final Exception ex) {
								sendCameraError();
								//TODO Check that can be used
								Log.e(TAG, "Error during video recording");
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
		mPreview = new CameraPreview(this, mCamera);
		((FrameLayout) findViewById(R.id.surfaceView)).addView(mPreview);

		mPreview.setKeepScreenOn(true);

		mButtonClick = (Button) findViewById(R.id.picture);


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
				Log.i("CameraActivity_OldAPI", "mMessageReceiver.onReceive() :: Start");

				if (!mVideo && intent.getAction().equals("TAKE_PIC")) {
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


//    public int getCameraDisplayOrientation(int cameraId, android.hardware.Camera mCamera)
//    {
//        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
//        android.hardware.Camera.getCameraInfo(cameraId, info);
//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//        int degrees = 0;
//        switch (rotation)
//        {
//            case Surface.ROTATION_0:
//                degrees = 0;
//                break;
//            case Surface.ROTATION_90:
//                degrees = 90;
//                break;
//            case Surface.ROTATION_180:
//                degrees = 180;
//                break;
//            case Surface.ROTATION_270:
//                degrees = 270;
//                break;
//        }
//
//        Log.d(TAG, "degrees = " + degrees);
//
//        int result;
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
//        {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360; // compensate the mirror
//        } else
//        { // back-facing
//            result = (info.orientation - degrees + 360) % 360;
//        }
//
//        return result;
//    }

	@Override
	protected void onResume() {
		logi("onCreate() :: onResume");

		super.onResume();
		int camIdx = getFrontFacingCamera();
		try {
			mCamera = Camera.open(camIdx);
			//int mRotation = getCameraDisplayOrientation(camIdx,mCamera);
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setRotation(270); //set rotation to save the picture
			mCamera.setParameters(parameters);
			mCamera.setDisplayOrientation(90);
			mCamera.startPreview();
			mPreview.setCamera(mCamera);
			//LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("TAKE_PIC"));

			if(mVideo){
				this.registerReceiver(mMessageReceiver, new IntentFilter("START_VIDEO"));
				this.registerReceiver(mMessageReceiver, new IntentFilter("STOP_VIDEO"));
			}
			else {
				this.registerReceiver(mMessageReceiver, new IntentFilter("TAKE_PIC"));
			}

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
			mPreview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
		super.onPause();
	}

	private void resetCam() {
		mCamera.startPreview();
		mPreview.setCamera(mCamera);
	}

	private void refreshGallery(File file) {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		sendBroadcast(mediaScanIntent);
	}

	//TODO Add Sound here
	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			//			 Log.d(TAG, "onShutter'd");
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
			resetCam();
			finish();
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};

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
		if (mMediaRecorder != null) {
			mMediaRecorder.reset(); // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			mVideoFile = null;
			//TODO Check that is not necessary
			//mCamera.lock(); // lock camera for later use
		}
	}

	private boolean prepareMediaRecorder() {

		mMediaRecorder = new MediaRecorder();

		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);

		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		try {
			//TODO Check that the video quality is enough
			mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

			//TODO defining the file name
			File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Microbit/");

			if(!dir.exists())
				dir.mkdirs();

			//TODO defining the file name
			String fileName = String.format("%d.mp4", System.currentTimeMillis());
			mVideoFile = new File(dir, fileName);

			mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());

			//TODO Check File Limits
			mMediaRecorder.setMaxDuration(600000); // Set max duration 60 sec.
			mMediaRecorder.setMaxFileSize(50000000); // Set max file size 50M
			mMediaRecorder.setOrientationHint(270);
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
