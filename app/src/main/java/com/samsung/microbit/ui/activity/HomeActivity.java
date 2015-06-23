package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.Constants;

public class HomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        MBApp.setContext(this);

        setContentView(R.layout.activity_home);

        RelativeLayout connectBarView = (RelativeLayout) findViewById(R.id.connectBarView);
        connectBarView.getBackground().setAlpha(128);

        RelativeLayout projectBarView = (RelativeLayout) findViewById(R.id.projectBarView);
        projectBarView.getBackground().setAlpha(128);
    }

    public void onBtnClicked(View v){
        if(v.getId() == R.id.addDevice){
            Intent intent = new Intent(this, ConnectActivity.class);
            startActivity(intent);
        } else if(v.getId() == R.id.startNewProject){
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra(Constants.URL, getString(R.string.touchDevURLNew));
            startActivity(intent);
        }
    }
}