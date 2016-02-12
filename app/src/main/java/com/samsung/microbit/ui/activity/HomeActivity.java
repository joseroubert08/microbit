package com.samsung.microbit.ui.activity;

import android.Manifest;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.EchoClientManager;
import com.samsung.microbit.core.RemoteConfig;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.ui.PopUp;

import java.util.HashMap;
import java.util.List;

import pl.droidsonroids.gif.GifImageView;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    // share stats checkbox
    private CheckBox mShareStatsCheckBox;

    SharedPreferences mPrefs = null;
    StableArrayAdapter adapter = null;
    private AppCompatDelegate delegate;
    // Hello animation
    private GifImageView gifAnimationHelloEmoji;
    boolean connectionInitiated = false;

    private MBApp app = null;
    protected String TAG = "HomeActivity";
    protected boolean debug = BuildConfig.DEBUG;

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

        RemoteConfig.getInstance().init();

        setContentView(R.layout.activity_home);
        setupDrawer();

        if (app == null)
            app = (MBApp) MBApp.getApp().getApplicationContext();

        LinearLayout connectBarView = (LinearLayout) findViewById(R.id.connectBarView);
        connectBarView.getBackground().setAlpha(128);

        // Font Style for buttons
        Button connectButton = (Button) findViewById(R.id.connect_device_btn);
        connectButton.setTypeface(MBApp.getApp().getTypeface());
        Button flashButton = (Button) findViewById(R.id.flash_microbit_btn);
        flashButton.setTypeface(MBApp.getApp().getTypeface());
        Button createCodeButton = (Button) findViewById(R.id.create_code_btn);
        createCodeButton.setTypeface(MBApp.getApp().getTypeface());
        Button discoverButton = (Button) findViewById(R.id.discover_btn);
        discoverButton.setTypeface(MBApp.getApp().getTypeface());

        checkMinimumPermissionsForThisScreen();
        startOtherServices();

        EchoClientManager.getInstance().sendViewEventStats("homeactivity");

        /* Debug code*/
        MenuItem item = (MenuItem) findViewById(R.id.live);
        if (item != null) {
            item.setChecked(true);
        }

        if (!RemoteConfig.getInstance().isAppStatusOn()) {
            finish();
            //Cannot proceed with the application. Shutdown NOW
            PopUp.show(MBApp.getContext(),
                    RemoteConfig.getInstance().getExceptionMsg(),
                    RemoteConfig.getInstance().getExceptionTitle(),
                    R.drawable.error_face,//image icon res id
                    R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
                    PopUp.TYPE_ALERT, //type of popup.
                    null,
                    null);

        }
        // animation for loading hello .giff
        gifAnimationHelloEmoji = (GifImageView) findViewById(R.id.homeHelloAnimationGifView);
    }

    private void startOtherServices() {
        // IPC service to communicate between the services
        Intent ipcIntent = new Intent(this, IPCService.class);
        startService(ipcIntent);

        // BLE service to Handle all BLE communications
        Intent bleIntent = new Intent(this, BLEService.class);
        startService(bleIntent);

        // Plugin service to handle incoming requests
        final Intent intent = new Intent(this, PluginService.class);
        startService(intent);
    }

    private void setupDrawer() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationContentDescription(R.string.content_description_toolbar_home);
        ImageView imgToolbarLogo = (ImageView) findViewById(R.id.img_toolbar_bbc_logo);
        imgToolbarLogo.setContentDescription("BBC Micro:bit");
        //toolbar.setLogo(R.drawable.bbc_microbit_app_bar_logo);
        // toolbar.setNavigationIcon(R.drawable.white_red_led_btn);
        //  toolbar.setLogoDescription(R.string.content_description_toolbar_logo);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerTitle(GravityCompat.START, "Menu"); // TODO - Accessibility for touching the drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);


        boolean shareStats = false;
        mPrefs = getSharedPreferences("com.samsung.microbit", MODE_PRIVATE);
        if (mPrefs != null) {
            shareStats = mPrefs.getBoolean(getString(R.string.prefs_share_stats_status), true);
            EchoClientManager.getInstance().setSharingStats(shareStats);
        }
        //TODO focusable view
        drawer.setDrawerListener(toggle);

        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        /* Todo [Hack]:
        * NavigationView items for selection by user using
        * onClick listener instead of overriding onNavigationItemSelected*/
        Button menuNavBtn = (Button) findViewById(R.id.btn_nav_menu);
        menuNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_nav_menu).setOnClickListener(this);

        Button aboutNavBtn = (Button) findViewById(R.id.btn_about);
        aboutNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_about).setOnClickListener(this);

        Button helpNavBtn = (Button) findViewById(R.id.btn_help);
        helpNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_help).setOnClickListener(this);

        Button privacyNavBtn = (Button) findViewById(R.id.btn_privacy_cookies);
        privacyNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_privacy_cookies).setOnClickListener(this);

        Button termsNavBtn = (Button) findViewById(R.id.btn_terms_conditions);
        termsNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_terms_conditions).setOnClickListener(this);

        Button sendFeedbackNavbtn = (Button) findViewById(R.id.btn_send_feedback);
        sendFeedbackNavbtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_send_feedback).setOnClickListener(this);

        // Share stats checkbox
        TextView shareStatsCheckTitle = (TextView) findViewById(R.id.share_statistics_title);
        shareStatsCheckTitle.setTypeface(MBApp.getApp().getTypeface());
        TextView shareStatsDescription = (TextView) findViewById(R.id.share_statistics_description);
        shareStatsDescription.setTypeface(MBApp.getApp().getTypeface());
        mShareStatsCheckBox = (CheckBox) findViewById(R.id.share_statistics_status);
        mShareStatsCheckBox.setOnClickListener(this);
        mShareStatsCheckBox.setChecked(shareStats);
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
        urlToOpen = RemoteConfig.getInstance().getCreateCodeURL();
        switch (id) {
            case R.id.live:
                item.setChecked(true);
                break;
            case R.id.stage:
                item.setChecked(true);
                urlToOpen = urlToOpen.replace("www", "stage");
                break;
            case R.id.test:
                item.setChecked(true);
                urlToOpen = urlToOpen.replace("www", "test");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
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
    protected void onPause() {
        super.onPause();
        // Pause animation
        gifAnimationHelloEmoji.setFreezesAnimation(true);
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
//            case R.id.addDevice:
            case R.id.connect_device_btn: {
                Intent intent = new Intent(this, PairingActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.create_code_btn: {
                //Update Stats
                EchoClientManager.getInstance().sendNavigationStats("home", "create-code");
                if (urlToOpen == null) {
                    urlToOpen = RemoteConfig.getInstance().getCreateCodeURL();
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(urlToOpen));

                startActivity(intent);
            }
            break;
            case R.id.flash_microbit_btn:
                EchoClientManager.getInstance().sendNavigationStats("home", "flash");
                Intent i = new Intent(this, ProjectActivity.class);
                startActivity(i);
                break;
            case R.id.discover_btn:
                EchoClientManager.getInstance().sendNavigationStats("home", "discover");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(RemoteConfig.getInstance().getDiscoverURL()));
                startActivity(intent);
                break;

            // TODO: HACK - Navigation View items from drawer here instead of [onNavigationItemSelected]
            // NavigationView items
            case R.id.btn_nav_menu: {
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_about: {
                String url = RemoteConfig.getInstance().getAboutURL();
                Intent aboutIntent = new Intent(Intent.ACTION_VIEW);
                aboutIntent.setData(Uri.parse(url));
                startActivity(aboutIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_help: {
                Intent launchHelpIntent = new Intent(this, HelpWebView.class);
                launchHelpIntent.putExtra("url", "file:///android_asset/htmls/help/index.html");
                startActivity(launchHelpIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
                EchoClientManager.getInstance().sendNavigationStats("overflow-menu", "help");
            }
            break;
            case R.id.btn_privacy_cookies: {
                String url = RemoteConfig.getInstance().getPrivacyURL();
                Intent privacyIntent = new Intent(Intent.ACTION_VIEW);
                privacyIntent.setData(Uri.parse(url));
                startActivity(privacyIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
                EchoClientManager.getInstance().sendNavigationStats("overflow-menu", "privacy-policy");
            }
            break;
            case R.id.btn_terms_conditions: {
                String url = RemoteConfig.getInstance().getTermsOfUseURL();
                Intent termsIntent = new Intent(Intent.ACTION_VIEW);
                termsIntent.setData(Uri.parse(url));
                startActivity(termsIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
                EchoClientManager.getInstance().sendNavigationStats("overflow-menu", "ts-and-cs");

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
                toggleShareStatistics();
            }
            break;

        }//Switch Ends
    }


    private void toggleShareStatistics() {
        if (mShareStatsCheckBox == null) {
            return;
        }
        boolean shareStatistics = false;
        shareStatistics = mShareStatsCheckBox.isChecked();
        mPrefs.edit().putBoolean(getString(R.string.prefs_share_stats_status), shareStatistics).apply();
        logi("shareStatistics = " + shareStatistics);
        EchoClientManager.getInstance().setSharingStats(shareStatistics);
        EchoClientManager.getInstance().sendStatSharing(shareStatistics);
    }


    private void installSamples() {
        if (mPrefs.getBoolean("firstrun", true)) {
            mPrefs.edit().putBoolean("firstrun", false).commit();
            //First Run. Install the Sample applications
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PopUp.show(MBApp.getContext(),
                            "Samples will now be copied to your device. You can check them out in the Flash section.",
                            "Thank you",
                            R.drawable.message_face, R.drawable.blue_btn,
                            PopUp.GIFF_ANIMATION_NONE,
                            PopUp.TYPE_ALERT,
                            null, null);
                    Utils.installSamples();

                }
            }).start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode)
        {
            case Constants.APP_STORAGE_PERMISSIONS_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    installSamples();
                } else {
                    if (mPrefs!= null) mPrefs.edit().putBoolean("firstrun", false).commit();
                    PopUp.show(MBApp.getContext(),
                            getString(R.string.storage_permission_for_samples_error),
                            "",
                            R.drawable.error_face, R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_ALERT,
                            null, null);
                }
            }
            break;

        }
    }

    private void requetPermission(String[] permissions, final int requestCode) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    View.OnClickListener diskStoragePermissionOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("diskStoragePermissionOKHandler");
            PopUp.hide();
            String[] permissionsNeeded = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            requetPermission(permissionsNeeded, Constants.APP_STORAGE_PERMISSIONS_REQUESTED);
        }
    };

    View.OnClickListener diskStoragePermissionCancelHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("diskStoragePermissionCancelHandler");
            PopUp.hide();
            PopUp.show(MBApp.getContext(),
                    getString(R.string.storage_permission_for_samples_error),
                    "",
                    R.drawable.error_face, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
                    PopUp.TYPE_ALERT,
                    null, null);
            if (mPrefs!= null) mPrefs.edit().putBoolean("firstrun", false).commit();
        }
    };


    private void checkMinimumPermissionsForThisScreen() {
        //Check reading perminssions & writing permission to populate the HEX files & show program list
        if (mPrefs.getBoolean("firstrun", true)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED)) {
                PopUp.show(MBApp.getContext(),
                        getString(R.string.storage_permission_for_samples),
                        getString(R.string.permissions_needed_title),
                        R.drawable.message_face, R.drawable.blue_btn, PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_CHOICE,
                        diskStoragePermissionOKHandler,
                        diskStoragePermissionCancelHandler);
            } else {
                if (mPrefs.getBoolean("firstrun", true)) {
                    mPrefs.edit().putBoolean("firstrun", false).commit();
                    //First Run. Install the Sample applications
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.installSamples();
                        }
                    }).start();
                }
            }
        }
    }

    @Override
    public void onResume() {
        if (debug) logi("onResume() :: ");
        super.onResume();
        MBApp.setContext(this);
        findViewById(R.id.homeHelloAnimationGifView).animate();
    }
}
