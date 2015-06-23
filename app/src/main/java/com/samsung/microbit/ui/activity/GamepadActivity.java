package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.LinearLayout;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

public class GamepadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MBApp.setContext(this);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_gamepad);

        //LinearLayout mainContentView = (LinearLayout) findViewById(R.id.mainContentView);
        //mainContentView.getBackground().setAlpha(128);
    }

}
