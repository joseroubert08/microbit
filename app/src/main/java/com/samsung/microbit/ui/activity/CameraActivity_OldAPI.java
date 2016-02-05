package com.samsung.microbit.ui.activity;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.ui.view.CameraPreview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity_OldAPI extends Activity {

	private static boolean mInstanceActive = false;

	private CameraPreview mPreview;
	private ImageButton mButtonClick, mButtonBack_portrait, mButtonBack_landscape;
	private Camera mCamera;
	private int mCameraIdx;
    private static boolean mfrontCamera = true ;
	private BroadcastReceiver mMessageReceiver;
	private boolean mVideo = false;
	private boolean mIsRecording = false;
	private MediaRecorder mMediaRecorder;
	private File mVideoFile = null;
    private OrientationEventListener myOrientationEventListener;
    private int mCurrentRotation = -1;
    private int mStoredRotation = -1;
    private int mOrientationOffset = 0;
    private int mCurrentIconIndex = 0;
    private ArrayList<Drawable> mTakePhoto, mStartRecord, mStopRecord, mCurrentIconList;
    private Camera.Parameters mParameters = null;

	private static final String TAG = "CameraActivity_OldAPI";
	private boolean debug = true;


    private MediaRecorder.OnInfoListener m_MediaInfoListner = new MediaRecorder.OnInfoListener(){

        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            switch (what)
            {
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                    Utils.playAudio(Utils.getMaxVideoRecordedAudio(),null);
                    stopRecording();
                    break;
                case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                    logi("Error in media recorder - What = " + what + " extra = " + extra);
                    stopRecording();
                    break;
            }
        }
    };

	void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	private int getCurrentCamera() {
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
            if (mfrontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                    logi("returning front camera");
                    return camIdx;
            } else if (!mfrontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
                    logi("returning back camera");
                    return camIdx;
            }

		}
		return -1;
	}

    private Drawable rotateIcon(Drawable icon, int rotation) {
        Bitmap existingBitmap = ((BitmapDrawable) icon).getBitmap();
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation - mOrientationOffset);
        Bitmap rotated = Bitmap.createBitmap(existingBitmap, 0, 0, existingBitmap.getWidth(), existingBitmap.getHeight(), matrix, true);
        return new BitmapDrawable(rotated);
    }

    private void createRotatedIcons() {
        Drawable icon = getResources().getDrawable(R.drawable.take_photo);
        mTakePhoto = new ArrayList<Drawable>();
        mStartRecord = new ArrayList<Drawable>();
        mStopRecord = new ArrayList<Drawable>();
        mTakePhoto.add(rotateIcon(icon,0));
        mTakePhoto.add(rotateIcon(icon,-90));
        mTakePhoto.add(rotateIcon(icon,180));
        mTakePhoto.add(rotateIcon(icon,-270));
        icon = getResources().getDrawable(R.drawable.start_record_icon);
        mStartRecord.add(rotateIcon(icon,0));
        mStartRecord.add(rotateIcon(icon,-90));
        mStartRecord.add(rotateIcon(icon,180));
        mStartRecord.add(rotateIcon(icon,-270));
        icon = getResources().getDrawable(R.drawable.stop_record_icon);
        mStopRecord.add(rotateIcon(icon,0));
        mStopRecord.add(rotateIcon(icon,-90));
        mStopRecord.add(rotateIcon(icon, 180));
        mStopRecord.add(rotateIcon(icon,-270));
    }

	private void setButtonForBackAction() {
		mButtonBack_portrait.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                goBackAction();
            }
        });

		mButtonBack_landscape.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                goBackAction();
            }
        });
	}

    private void goBackAction() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateButtonClickIcon() {
        mButtonClick.setBackground(mCurrentIconList.get(mCurrentIconIndex));
        mButtonClick.invalidate();
    }

    private void updateCameraRotation() {
        if(mCamera!=null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRotation(getRotationCameraCorrection(mCurrentRotation)); //set rotation to save the picture
            mCamera.setParameters(parameters);
        }
    }

    private void updateButtonOrientation(int rotation) {
        rotation = (rotation + mOrientationOffset)%360;
        int quant_rotation = 0;
        boolean buttonPortraitVisible = true;
        if(rotation == OrientationEventListener.ORIENTATION_UNKNOWN)
            return;
        if(rotation < 45 || rotation >= 315) {
            buttonPortraitVisible = true;
            quant_rotation = 0;
            mCurrentIconIndex = 0;
        }else if((rotation >= 135 && rotation < 225)) {
            buttonPortraitVisible = true;
            quant_rotation = 180;
            //mCurrentIconIndex = 2;
            mCurrentIconIndex = 0; //This way only 2 configurations are allowed
        }else if((rotation >= 45 && rotation < 135)) {
            buttonPortraitVisible = false;
            quant_rotation = 270;
            //mCurrentIconIndex = 1;
            mCurrentIconIndex = 3; //This way only 2 configurations are allowed
        }else if((rotation >= 225 && rotation < 315)) {
            buttonPortraitVisible = false;
            quant_rotation = 90;
            mCurrentIconIndex = 3;
        }
        if(quant_rotation!=mCurrentRotation)
        {
            mCurrentRotation = quant_rotation;

            if(buttonPortraitVisible) {
                mButtonBack_landscape.setVisibility(View.INVISIBLE);
                mButtonBack_portrait.setVisibility(View.VISIBLE);
                mButtonBack_portrait.bringToFront();
            }
            else {
                mButtonBack_landscape.setVisibility(View.VISIBLE);
                mButtonBack_landscape.bringToFront();
                mButtonBack_portrait.setVisibility(View.INVISIBLE);
            }
            mButtonBack_portrait.invalidate();
            mButtonBack_landscape.invalidate();

            updateButtonClickIcon();
            updateCameraRotation();
        }
    }

	private void setButtonForPicture() {

        mCurrentIconList = mTakePhoto;
        updateButtonClickIcon();
		mButtonClick.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
                } catch (final Exception ex) {
                    sendCameraError();
                    Log.e(TAG, "Error during take picture", ex);
                }
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

    private void stopRecording() {
        logi("Stop recording");
        mMediaRecorder.stop(); // stop the recording
        refreshGallery(mVideoFile);
        releaseMediaRecorder(); // release the MediaRecorder object
        mIsRecording = false;
        mCurrentIconList = mStartRecord;
        updateButtonClickIcon();
        releaseMediaRecorder();
        resetCam();
    }

	private void setButtonForVideo() {

        mCurrentIconList = mStartRecord;
        updateButtonClickIcon();
		mButtonClick.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mIsRecording) {
                    // stop recording and release mCamera
                    stopRecording();
                } else {
                    if (!prepareMediaRecorder()) {
                        sendCameraError();
                        Log.e(TAG, "Error preparing mediaRecorder");
                        finish();
                    }

                    mCurrentIconList = mStopRecord;
                    updateButtonClickIcon();
                    //TODO Video recodring crashing. Check #112 for details. Temporary fix for the BETT
                    //indicateVideoRecording();
                    //TODO Check that is true
                    // work on UiThread for better performance
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                mMediaRecorder.start();
                            } catch (final Exception ex) {
                                sendCameraError();
                                Log.e(TAG, "Error during video recording", ex);
                                finish();
                            }
                        }
                    });
                    mIsRecording = true;
                }
            }
        });
	}

    private void indicateVideoRecording() {
        logi("indicateVideoRecording");
        if (mParameters == null && mCamera != null){
            setParameters();

        }
        new CountDownTimer(Constants.MAX_VIDEO_RECORDING_TIME, 1000) {
            boolean flashON = false ;
            public void onTick(long millisUntilFinished) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (mIsRecording && mCamera != null){
                            if (flashON) {
                                logi("turning flash ON");
                                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                            } else
                            {
                                logi("turning flash OFF");
                                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            }
                            mCamera.setParameters(mParameters);
                            flashON = !flashON;
                        }
                    }
                });
            }

            public void onFinish() {
                //Do nothing
            }
        }.start();
    }

    private void setParameters() {
        mParameters = mCamera.getParameters();

        List<String> flashModes = mParameters.getSupportedFlashModes();
        for (String flashmode : flashModes){
            logi("Supported flash mode : " + flashmode);
        }
        mCamera.setParameters(mParameters);
        mCamera.enableShutterSound(true);
    }

    private void setPreviewForVideo() {

	}

    private int getRotationCameraCorrection(int current_rotation) {
        return (current_rotation + 270)%360;
    }

	private void sendCameraError() {
		CmdArg cmd = new CmdArg(0,"Camera Error");
		CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
	}

    private void setOrientationOffset() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        Point screenSize = new Point(0,0);
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        logi("Display size " + screenSize.x + "x" + screenSize.y);

        //Checking if it's a tablet
        if(rotation == 0 || rotation == 2){
            if(screenSize.x > screenSize.y){
                //Tablet
                mOrientationOffset = 270;
                logi("Tablet");
            }else{
                //Phone
                mOrientationOffset = 0;
                logi("Phone");
            }
        }else{
            if(screenSize.x > screenSize.y){
                //Phone
                mOrientationOffset = 0;
                logi("Phone");
            }else{
                //Tablet
                mOrientationOffset = 270;
                logi("Tablet");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

		logi("onCreate() :: Start");
		super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        createRotatedIcons();

        setOrientationOffset();

        myOrientationEventListener
                = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL){

            @Override
            public void onOrientationChanged(int arg0) {
                updateButtonOrientation(arg0);
            }};

		mCamera = null;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_camera_old_api);
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
        mButtonBack_portrait = (ImageButton) findViewById(R.id.back_portrait);
        mButtonBack_landscape = (ImageButton) findViewById(R.id.back_landscape);

        setButtonForBackAction();

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

        updateButtonOrientation(0);

		mMessageReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals("CLOSE")){
					finish();
				}
				else if(!mVideo && intent.getAction().equals("TAKE_PIC")) {
                    mfrontCamera = true;
					mButtonClick.callOnClick();
				}
                else if (intent.getAction().equals("TOGGLE_CAMERA")){
                    toggleCamera();
                }
				else if(mVideo && !mIsRecording && intent.getAction().equals("START_VIDEO")) {
                    mfrontCamera = true;
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

    private void toggleCamera()
    {
        if (mCamera == null) {
            logi("Camera not open yet.");
            return;
        }
        mfrontCamera = !mfrontCamera ;
        recreate();
    }
	@Override
	protected void onResume() {
		logi("onCreate() :: onResume");

		super.onResume();
        //This intent filter has to be set even if no camera is found otherwise the unregisterReceiver()
        //fails during the onPause()
        if (myOrientationEventListener.canDetectOrientation()){
            logi("DetectOrientation Enabled");
            myOrientationEventListener.enable();
        }
        else{
            logi("DetectOrientation Disabled");
        }
		this.registerReceiver(mMessageReceiver, new IntentFilter("CLOSE"));
		mCameraIdx = getCurrentCamera();
        logi("mCameraIdx = " + mCameraIdx);
		try {
			mCamera = Camera.open(mCameraIdx);
            if(mCamera==null) {
                logi("Couldn't open the camera");
            }
            logi("Step 2");
			mPreview.setCamera(mCamera, mCameraIdx);
            logi("Step 3");

			if(mVideo){
				this.registerReceiver(mMessageReceiver, new IntentFilter("START_VIDEO"));
				this.registerReceiver(mMessageReceiver, new IntentFilter("STOP_VIDEO"));
                this.registerReceiver(mMessageReceiver, new IntentFilter("TOGGLE_CAMERA"));
			}
			else {
				this.registerReceiver(mMessageReceiver, new IntentFilter("TAKE_PIC"));
                this.registerReceiver(mMessageReceiver, new IntentFilter("TOGGLE_CAMERA"));
			}

            logi("Step 3");
            mPreview.restartCameraPreview();
            logi("Step 4");
            updateCameraRotation();
			logi("onCreate() :: onResume # ");

		} catch (RuntimeException ex) {
            logi(ex.toString());
			Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
			sendCameraError();
			finish();
		}
	}

	@Override
	protected void onPause() {

		logi("onCreate() :: onPause");

        if(mIsRecording) {
            stopRecording();
        }

        if (myOrientationEventListener.canDetectOrientation()){
            logi("DetectOrientation Disabled");
            myOrientationEventListener.disable();
        }
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
        Camera.Parameters parameters = mCamera.getParameters();
        logi("Set Flash mode ON");
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.autoFocus(new AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
            }
        });
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
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
            ImageView blinkRect = (ImageView) findViewById(R.id.blink_rectangle);
            blinkRect.setVisibility(View.VISIBLE);
            blinkRect.bringToFront();
            blinkRect.invalidate();
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			// Display toast here and play audio
            Toast toast = Toast.makeText(MBApp.getApp().getApplicationContext(),"Photo taken", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            Utils.playAudio(Utils.getPictureTakenAudio(),null);

		}
	};
	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			new SaveImageTask().execute(data);
			DrawBlink();
			resetCam();
		}
	};

	void DrawBlink(){
		SystemClock.sleep(500);
        ImageView blinkRect = (ImageView) findViewById(R.id.blink_rectangle);
		blinkRect.setVisibility(View.INVISIBLE);
        blinkRect.invalidate();
	}

	@Override
	protected void onStart() {
        logi("onCreate() :: onStart");
		//Informing microbit that the mCamera is active now
		CmdArg cmd = new CmdArg(0, "Camera on");
		CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
		super.onStart();
	}

	@Override
	protected void onDestroy() {
        logi("onCreate() :: onDestroy");
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
				File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/" + Constants.MEDIA_OUTPUT_FOLDER);

				if(!dir.exists()) {
                    dir.mkdirs();
                }

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
			mCamera.lock(); // lock camera for later use
		}
	}



	private boolean prepareMediaRecorder() {

		if(mCameraIdx<0 || mCamera==null)
			return false;

		mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setOnInfoListener(m_MediaInfoListner);

		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);

        //Rohit - Removed the audio source as BBC doesn't want sound in the recording
        //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);

		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		//TODO Check because depending on the quality on some devices the MediaRecorder doesn't work
		if(CamcorderProfile.hasProfile(mCameraIdx,CamcorderProfile.QUALITY_TIME_LAPSE_HIGH))
			mMediaRecorder.setProfile(CamcorderProfile.get(mCameraIdx,CamcorderProfile.QUALITY_TIME_LAPSE_HIGH));
		else if(CamcorderProfile.hasProfile(mCameraIdx,CamcorderProfile.QUALITY_TIME_LAPSE_LOW))
			mMediaRecorder.setProfile(CamcorderProfile.get(mCameraIdx,CamcorderProfile.QUALITY_TIME_LAPSE_LOW));
		else {
			releaseMediaRecorder();
			Log.e(TAG, "Error preparing media Recorder: no CamcorderProfile available");
			return false;
		}

		//Setting output file
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/" +Constants.MEDIA_OUTPUT_FOLDER);
		if(!dir.exists()) {
            dir.mkdirs();
        }
		String fileName = String.format("%d.mp4", System.currentTimeMillis());
		mVideoFile = new File(dir, fileName);
		mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());

		//Setting limits
		mMediaRecorder.setMaxDuration(Constants.MAX_VIDEO_RECORDING_TIME);
		mMediaRecorder.setMaxFileSize(Constants.MAX_VIDEO_FILE_SIZE);

        int rotation = getRotationCameraCorrection(mCurrentRotation);
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
