package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;
import android.widget.VideoView;

import com.samsung.microbit.R;

public class SplashScreenActivityVideo extends Activity {

    // Splash screen timer (6 second video) extra second for smooth transition
    private static int SPLASH_TIME_OUT = 7000;

    MediaPlayer mediaPlayer;
    private VideoView videoViewSplashScreen;
    private String splashScreenPath;
    SurfaceHolder surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen splash screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash_screen_video);
        videoViewSplashScreen = (VideoView) findViewById(R.id.video_view_splash_screen);

        mediaPlayer = new MediaPlayer();

        // Find data source - location of splash screen
        splashScreenPath = "android.resource://" + getPackageName() + "/" + R.raw.splash_screen_v1;

        Uri uri = Uri.parse(splashScreenPath);

        mediaPlayer.setDisplay(surfaceHolder);

        // Pass to the VideoView
        videoViewSplashScreen.setVideoURI(uri);

        // Play the splash screen
        videoViewSplashScreen.start();

        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                Intent i = new Intent(SplashScreenActivityVideo.this, HomeActivity.class);
                startActivity(i);

                // close this activity
                finish();
            }
        }, SPLASH_TIME_OUT);
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
    }
}
