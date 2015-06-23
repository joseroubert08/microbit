package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.Utils;
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

        updateConnectBarTitle();

        RelativeLayout projectBarView = (RelativeLayout) findViewById(R.id.projectBarView);
        projectBarView.getBackground().setAlpha(128);

        updateProjectBarProjects();
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

    private final void updateConnectBarTitle()
    {
        TextView connectBarTitle = (TextView) findViewById(R.id.title);
        SharedPreferences p = Utils.getInstance().getPreferences(this);
        connectBarTitle.setText(p.getString(Utils.PREFERENCES_NAME_KEY, "Connect to your Micro:Bit"));
    }

    private final void updateProjectBarProjects()
    {
        TextView projectBarProjects = (TextView) findViewById(R.id.projects);
        projectBarProjects.setText(Integer.toString(Utils.findProgramsAndPopulate(null, null)) + " saved projects");
    }

    public void onResume() {
        super.onResume();

    }

}