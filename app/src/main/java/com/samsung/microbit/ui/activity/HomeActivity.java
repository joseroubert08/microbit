package com.samsung.microbit.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.RemoteConfig;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;

import java.util.HashMap;
import java.util.List;

import uk.co.bbc.echo.EchoConfigKeys;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    // share stats checkbox
    private CheckBox shareStatsCheckBox;

    SharedPreferences prefs = null;
    StableArrayAdapter adapter = null;
    private AppCompatDelegate delegate;

    boolean connectionInitiated = false;

    private MBApp app = null;
    protected String TAG = "HomeActivity";
    protected boolean debug = true;

    /* Debug code*/
    private String urlToOpen = null;
    /* Debug code ends*/


    private String emailBodyString = null;

    protected void logi(String message) {
        if (debug) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //handle orientation change to prevent re-creation of activity.
        //i.e. while recording we need to preserve state of recorder
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logi("onCreate() :: ");
        MBApp.setContext(this);

        setContentView(R.layout.activity_home);
        setupDrawer();

        //Set up Echo
        setupEcho();

        LinearLayout connectBarView = (LinearLayout) findViewById(R.id.connectBarView);
        connectBarView.getBackground().setAlpha(128);

        // Start the other services - local service to handle IPC in the main process
        Intent ipcIntent = new Intent(this, IPCService.class);
        startService(ipcIntent);

        Intent bleIntent = new Intent(this, BLEService.class);
        startService(bleIntent);

        final Intent intent = new Intent(this, PluginService.class);
        startService(intent);

        prefs = getSharedPreferences("com.samsung.microbit", MODE_PRIVATE);

        if (app.getEcho() != null) {
            logi("Page View test for HomeActivity");
            //Page view test
            app.getEcho().viewEvent("com.samsung.microbit.ui.activity.homeactivity.page", null);
        }

        /* Debug code*/
        MenuItem item = (MenuItem) findViewById(R.id.live);
        if (item != null) {
            item.setChecked(true);
        }
        /* Debug code ends*/
        RemoteConfig.getInstance().init();
    }


    private void setupEcho() {
        // Echo Config
        app = (MBApp) MBApp.getApp().getApplicationContext();

        HashMap<String, String> config = new HashMap<String, String>();

        //Use ECHO_TRACE value for searching in echo chamber
        config.put(EchoConfigKeys.ECHO_TRACE, "microbit_android_app"); //TODO Change later
        //Use CS debug mode
        config.put(EchoConfigKeys.COMSCORE_DEBUG_MODE, "1");
        // Send Comscore events to EchoChamber
        config.put(EchoConfigKeys.COMSCORE_URL, "http://data.bbc.co.uk/v1/analytics-echo-chamber-inbound/comscore");
        //Enable debug mode
        config.put(EchoConfigKeys.ECHO_DEBUG, "1");
        // Send RUM events to EchoChamber
        //config.put(EchoConfigKeys.RUM_ENABLED, "true");
        //config.put(EchoConfigKeys.RUM_URL, "http://data.bbc.co.uk/v1/analytics-echo-chamber-inbound/rum");

        // Send BARB events
        //config.put(EchoConfigKeys.BARB_ENABLED, "true");
        //config.put(EchoConfigKeys.BARB_SITE_CODE, "bbcandroidtest");

        // Instantiate EchoClient
        app.initialiseEcho(config);
    }

    private void setupDrawer() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.bbc_microbit);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
       // toggle.setDrawerIndicatorEnabled(false); // TODO: Change to the word 'Menu'
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /* Todo [Hack]:
        * NavigationView items for selection by user using
        * onClick listener instead of overriding onNavigationItemSelected*/
        findViewById(R.id.btn_nav_menu).setOnClickListener(this);
        findViewById(R.id.btn_explore).setOnClickListener(this);
        findViewById(R.id.btn_about).setOnClickListener(this);
        findViewById(R.id.btn_help).setOnClickListener(this);
        findViewById(R.id.btn_privacy_cookies).setOnClickListener(this);
        findViewById(R.id.btn_terms_conditions).setOnClickListener(this);
        findViewById(R.id.btn_send_feedback).setOnClickListener(this);
        // Share stats checkbox
        shareStatsCheckBox = (CheckBox) findViewById(R.id.share_statistics_status);
        shareStatsCheckBox.setOnClickListener(this);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {

            case R.id.nav_menu:
                Toast.makeText(this, "Menu ", Toast.LENGTH_LONG).show();
                break;
            case R.id.nav_explore:
                Toast.makeText(this, "Explore ", Toast.LENGTH_LONG).show();
                break;
            case R.id.nav_about: {
                String url = RemoteConfig.getInstance().getAboutURL();
                if (url.isEmpty()) {
                    Log.d(TAG, "Failed to get the about url from remoteConfig");
                    url = getString(R.string.terms_of_use_url);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
            break;
            case R.id.nav_help:
                Toast.makeText(this, "help", Toast.LENGTH_LONG).show();
                break;
            case R.id.nav_privacy: {
                String url = RemoteConfig.getInstance().getPrivacyURL();
                if (url.isEmpty()) {
                    Log.d(TAG, "Failed to get the privacy url from remoteConfig");
                    url = getString(R.string.privacy_policy_url);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
            break;
            case R.id.nav_terms_conditions: {
                String url = RemoteConfig.getInstance().getTermsOfUseURL();
                if (url.isEmpty()) {
                    Log.d(TAG, "Failed to get the terms of use url from remoteConfig");
                    url = getString(R.string.terms_of_use_url);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
            break;

            case R.id.nav_feedback: {
                String emailAddress = RemoteConfig.getInstance().getSendEmailAddress();
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
                intent.putExtra(Intent.EXTRA_SUBJECT, "[User feedback] ");
                //Prepare the body of email
                String body = prepareEmailBody();
                intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
                Intent mailer = Intent.createChooser(intent, null);
                startActivity(mailer);
            }
            break;
            case R.id.nav_shareable:
                Toast.makeText(this, "Stats are shareable ", Toast.LENGTH_LONG).show();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private String prepareEmailBody() {
        if (emailBodyString != null) {
            return emailBodyString;
        }
        String emailBody = getString(R.string.email_body);
        String version = "0.1.0";
        PackageManager manager = MBApp.getContext().getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(MBApp.getContext().getPackageName(), 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        emailBodyString = String.format(emailBody,
                version,
                Build.MODEL,
                Build.VERSION.RELEASE,
                RemoteConfig.getInstance().getPrivacyURL());
        return emailBodyString;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.live:
                item.setChecked(true);
                urlToOpen = getString(R.string.touchDevLiveURL);
                break;
            case R.id.stage:
                item.setChecked(true);
                urlToOpen = getString(R.string.touchDevStageURL);
                break;
            case R.id.test:
                item.setChecked(true);
                urlToOpen = getString(R.string.touchDevTestURL);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        /* Debug menu. To be removed later */
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* function may be needed */
    }

    @Override
    public void onClick(final View v) {
        if (debug) logi("onBtnClicked() :: ");

        // Drawer closes only after certain items are selected from the Navigation View
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        switch (v.getId()) {
            case R.id.addDevice:
            case R.id.addDeviceEmpty: {
                Intent intent = new Intent(this, PairingActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.startNewProject: {
                //Update Stats
                if (app != null && app.getEcho() != null) {
                    logi("User action test for delete project");
                    app.getEcho().userActionEvent("click", "CreateCode", null);
                }
                //Debug feature to be added. Start Browser with live, stage or test URL
                if (urlToOpen == null) {
                    urlToOpen = getString(R.string.touchDevLiveURL);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(urlToOpen));
                startActivity(intent);
            }
            break;
            case R.id.flashMicrobit:
                Intent i = new Intent(this, ProjectActivity.class);
                startActivity(i);
                break;
            case R.id.discover:
                //Debug feature to be added. Start Browser with live, stage or test URL
                if (urlToOpen == null) {
                    urlToOpen = getString(R.string.touchDevDiscoverURL);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(urlToOpen));
                startActivity(intent);
                break;

            // TODO: HACK - Navigation View items from drawer here instead of [onNavigationItemSelected]
            // NavigationView items
            case R.id.btn_nav_menu: {
                Toast.makeText(this, "Menu...", Toast.LENGTH_LONG).show();
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_explore: {
                Toast.makeText(this, "Explore ", Toast.LENGTH_LONG).show();
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_about: {
                String url = RemoteConfig.getInstance().getAboutURL();
                if (url.isEmpty()) {
                    Log.d(TAG, "Failed to get the about url from remoteConfig");
                    url = getString(R.string.terms_of_use_url);
                }
                Intent aboutIntent = new Intent(Intent.ACTION_VIEW);
                aboutIntent.setData(Uri.parse(url));
                startActivity(aboutIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_help: {
                Toast.makeText(this, "help", Toast.LENGTH_LONG).show();
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_privacy_cookies: {
                String url = RemoteConfig.getInstance().getPrivacyURL();
                if (url.isEmpty()) {
                    Log.d(TAG, "Failed to get the privacy url from remoteConfig");
                    url = getString(R.string.privacy_policy_url);
                }
                Intent privacyIntent = new Intent(Intent.ACTION_VIEW);
                privacyIntent.setData(Uri.parse(url));
                startActivity(privacyIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_terms_conditions: {
                String url = RemoteConfig.getInstance().getTermsOfUseURL();
                if (url.isEmpty()) {
                    Log.d(TAG, "Failed to get the terms of use url from remoteConfig");
                    url = getString(R.string.terms_of_use_url);
                }
                Intent termsIntent = new Intent(Intent.ACTION_VIEW);
                termsIntent.setData(Uri.parse(url));
                startActivity(termsIntent);

                // Close drawer
                drawer.closeDrawer(GravityCompat.START);

            }
            break;

            case R.id.btn_send_feedback: {
                String emailAddress = RemoteConfig.getInstance().getSendEmailAddress();
                Intent feedbackIntent = new Intent(Intent.ACTION_SEND);
                feedbackIntent.setType("message/rfc822");
                feedbackIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
                feedbackIntent.putExtra(Intent.EXTRA_SUBJECT, "[User feedback] ");
                //Prepare the body of email
                String body = prepareEmailBody();
                feedbackIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
                Intent mailer = Intent.createChooser(feedbackIntent, null);
                startActivity(mailer);
                // Close drawer
                if (drawer != null) {
                    drawer.closeDrawer(GravityCompat.START);
                }
            }
            break;
            case R.id.share_statistics_status: {
                // Store checked value to indicate whether stats should be saved
                prefs.edit().putBoolean(getString(R.string.prefs_share_stats_status), shareStatsCheckBox.isChecked()).apply();
            }
            break;

        }//Switch Ends
    }

    @Override
    public void onResume() {
        if (debug) logi("onResume() :: ");
        super.onResume();

        /* TODO Remove this code in commercial build*/
        if (prefs.getBoolean("firstrun", true)) {
            //First Run. Install the Sample applications
            Toast.makeText(MBApp.getContext(), "Installing Sample HEX files. The projects number will be updated in some time", Toast.LENGTH_LONG).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Utils.installSamples();
                    prefs.edit().putBoolean("firstrun", false).commit();
                }
            }).start();
        } else {
            logi("Not the first run");
        }
        /* Code removal ends */
        MBApp.setContext(this);
    }
}
