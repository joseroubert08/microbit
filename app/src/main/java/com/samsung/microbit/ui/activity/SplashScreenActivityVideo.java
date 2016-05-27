package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.EchoClientManager;

import java.io.IOException;

public class SplashScreenActivityVideo extends Activity implements SurfaceHolder.Callback, MediaPlayer.OnCompletionListener {

    // Splash screen timer (6 second video) extra second for smooth transition
    private static int SPLASH_TIME_OUT = 7000;

    MediaPlayer mediaPlayer;
    private SurfaceView mSurfaceView;
    private String splashScreenPath;
    SurfaceHolder surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MBApp.setContext(this);

        //Track fresh app launch
        EchoClientManager.getInstance().sendAppStats();

        // Full screen splash screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash_screen_video);
        // Get the surface
        mSurfaceView = (SurfaceView) findViewById(R.id.video_view_splash_screen);

        // Setup surface holder
        surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
        if (mediaPlayer != null){
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        unbindDrawables(mSurfaceView);
        System.gc();
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
            view.setBackgroundResource(0);
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        // Mediaplayer
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        // Find data source - location of splash screen
        splashScreenPath = "android.resource://" + getPackageName() + "/" + R.raw.splash_screen_v1;

        // Parse the location of the video
        Uri uri = Uri.parse(splashScreenPath);
        try {
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepare();
            mediaPlayer.setDisplay(surfaceHolder);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //Get the dimensions of the video
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();

        //Get the width of the screen
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();

        // Get the SurfaceView layout parameters
        android.view.ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();

        // Set the width of the SurfaceView to the width of the screen
        lp.width = screenWidth;

        // Set the height of the SurfaceView to match the aspect ratio of the video
        //be sure to cast these as floats otherwise the calculation will likely be 0
        lp.height = (int) (((float) videoHeight / (float) videoWidth) * (float) screenWidth);

        //Commit the layout parameters
        mSurfaceView.setLayoutParams(lp);

        //Start video
        mediaPlayer.start();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onBackPressed() {
        //Do nothing here. We need it to be non-cancellable
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // only need to see splash screen on cold start
        if (mediaPlayer!= null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Intent i = new Intent(SplashScreenActivityVideo.this, HomeActivity.class);
        startActivity(i);
        // close this activity
        finish();
    }
}
