package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.samsung.microbit.R;
import com.samsung.microbit.ui.fragment.CodeSectionFragment;

public class LauncherActivity extends Activity implements  View.OnClickListener {


    final String liveMainPageURL = "https://microbit:bitbug42@live.microbit.co.uk/" ;
    final String stageMainPageURL = "https://microbit:bitbug42@stage.microbit.co.uk/" ;
    final String loginURL = "https://stage.microbit.co.uk/oauth/dialog?response_type=token&client_id=webapp&redirect_uri=https%3A%2F%2Fstage.microbit.co.uk%2Fapp%2F%23list%3Ainstalled-scripts&identity_provider=&state=0.47333144595053633";


    Button connectButton = null ;
    Button projectsButton = null ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_launcher);

        connectButton = (Button) findViewById(R.id.connectButton);
        projectsButton = (Button) findViewById(R.id.projectsButton);
        connectButton.setOnClickListener(this);
        projectsButton.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.connectButton:
                Toast.makeText(this, "Connect button clicked", Toast.LENGTH_LONG).show();
                break;

            case R.id.projectsButton:
                Toast.makeText(this, "Program list clicked", Toast.LENGTH_LONG).show();
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launcher, menu);
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
}
