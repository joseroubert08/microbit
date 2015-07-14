package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

public class GamepadActivity extends Activity {

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MBApp.setContext(this);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_gamepad);

        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        //LinearLayout mainContentView = (LinearLayout) findViewById(R.id.mainContentView);
        //mainContentView.getBackground().setAlpha(128);
    }

    public void onBtnClicked(View v){


        vibrator.vibrate(100);

        switch(v.getId())
        {
            case R.id.gamepadHomeBtn:
            {
                finish();
                break;
            }

            case R.id.gamepadLeftButton:
            {
                Toast.makeText(this.getApplicationContext(), "LEFT", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.gamepadRightButton:
            {
                Toast.makeText(this.getApplicationContext(), "RIGHT", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.gamepadUpButton:
            {
                Toast.makeText(this.getApplicationContext(), "UP", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.gamepadDownButton:
            {
                Toast.makeText(this.getApplicationContext(), "DOWN", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.gamepadAButton:
            {
                Toast.makeText(this.getApplicationContext(), "A", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.gamepadBButton:
            {
                Toast.makeText(this.getApplicationContext(), "B", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.gamepadCButton:
            {
                Toast.makeText(this.getApplicationContext(), "C", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.gamepadDButton:
            {
                Toast.makeText(this.getApplicationContext(), "D", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }
}
