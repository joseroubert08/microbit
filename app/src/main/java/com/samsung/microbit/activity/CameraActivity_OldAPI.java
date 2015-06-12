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
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.AutoFocusCallback;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.service.PluginService;

/**
 * Created by t.maestri on 09/06/2015.
 */
public class CameraActivity_OldAPI extends Activity {

	CameraPreview preview;
	Button buttonClick;
	Camera camera;
	Activity act;
	Context ctx;
	private BroadcastReceiver mMessageReceiver;

	private static final String TAG = "CameraActivity_OldAPI";
	private boolean debug = true;

	void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
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

	@Override
	public void onCreate(Bundle savedInstanceState) {

		logi("onCreate() :: Start");
		super.onCreate(savedInstanceState);

		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.camera_old_api);
		preview = new CameraPreview(this, (SurfaceView) findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		((FrameLayout) findViewById(R.id.camera_preview_layout)).addView(preview);
		preview.setKeepScreenOn(true);
		preview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				camera.takePicture(shutterCallback, rawCallback, jpegCallback);
			}
		});

		buttonClick = (Button) findViewById(R.id.picture);
		buttonClick.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				camera.takePicture(shutterCallback, rawCallback, jpegCallback);
			}
		});

		buttonClick.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				camera.autoFocus(new AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean arg0, Camera arg1) {
						//TODO Is there anything we have to do after autofocus?
					}
				});
				return true;
			}
		});

		mMessageReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i("CameraActivity_OldAPI", "mMessageReceiver.onReceive() :: Start");

				if (intent.getAction().equals("TAKE_PIC")) {
					//TODO Complete with the correct code
					buttonClick.callOnClick();
				}
			}
		};

		logi("onCreate() :: Done");
	}


//    public int getCameraDisplayOrientation(int cameraId, android.hardware.Camera camera)
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
			camera = Camera.open(camIdx);
			//int mRotation = getCameraDisplayOrientation(camIdx,camera);
			Camera.Parameters parameters = camera.getParameters();
			parameters.setRotation(270); //set rotation to save the picture
			camera.setParameters(parameters);
			camera.setDisplayOrientation(90);
			camera.startPreview();
			preview.setCamera(camera);
			//LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("TAKE_PIC"));

			this.registerReceiver(mMessageReceiver, new IntentFilter("TAKE_PIC"));

			logi("onCreate() :: onResume # ");
		} catch (RuntimeException ex) {
			Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onPause() {

		logi("onCreate() :: onPause");
		this.unregisterReceiver(
			mMessageReceiver);
		if (camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();
	}

	private void resetCam() {
		camera.startPreview();
		preview.setCamera(camera);
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
		//Informing microbit that the camera is active now
		CmdArg cmd = new CmdArg(0, "Camera on");
		CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
		Log.d(TAG, "Sent message CameraOn to microbit");
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		//Informing microbit that the camera is active now
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
				File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

				//TODO defining the file name
				String fileName = String.format("microbit_%d.jpg", System.currentTimeMillis());
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
}
