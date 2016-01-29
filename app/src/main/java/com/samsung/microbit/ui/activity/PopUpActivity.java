package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.ui.PopUp;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class PopUpActivity extends Activity implements View.OnClickListener {

    //intent from PopUpActivity to PopUp
    static public final String INTENT_ACTION_OK_PRESSED = "PopUpActivity.OK_PRESSED";
    static public final String INTENT_ACTION_CANCEL_PRESSED = "PopUpActivity.CANCEL_PRESSED";
    static public final String INTENT_ACTION_DESTROYED = "PopUpActivity.DESTROYED";
    static public final String INTENT_ACTION_CREATED = "PopUpActivity.CREATED";

    //intent from PopUp to PopUpActivity
    static public final String INTENT_ACTION_CLOSE = "PopUpActivity.CLOSE";
    static public final String INTENT_ACTION_UPDATE_PROGRESS = "PopUpActivity.UPDATE_PROGRESS";
    static public final String INTENT_ACTION_UPDATE_LAYOUT = "PopUpActivity.UPDATE_LAYOUT";

    static public final String INTENT_EXTRA_TYPE = "type";
    static public final String INTENT_EXTRA_TITLE = "title";
    static public final String INTENT_EXTRA_MESSAGE = "message";
    static public final String INTENT_EXTRA_ICON = "imageIcon";
    static public final String INTENT_EXTRA_ICONBG = "imageIconBg";
    static public final String INTENT_EXTRA_PROGRESS = "progress.xml";
    static public final String INTENT_EXTRA_CANCELABLE = "cancelable";
    static public final String INTENT_GIFF_ANIMATION_CODE = "giffAnimationCode";

    private WebView animationWebview = null;
    // Animations - Loading Error & Flash states
    private GifImageView gifImageView;
    private GifDrawable giffDrawable;

    private ImageView imageIcon = null;
    private TextView titleTxt = null;
    private ProgressBar progressBar = null;
    private ProgressBar spinnerBar = null;
    private TextView messageTxt = null;
    private Button okButton = null;
    private Button cancelButton = null;
    private Button affirmationOKButton = null;
    private LinearLayout layoutBottom = null;

    private boolean isCancelable = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {

            if (intent.getAction().equals(INTENT_ACTION_CLOSE)) {
                Log.d("PopUpActivity", "BroadcastReceiver.INTENT_ACTION_CLOSE");
                finish();
            } else if (intent.getAction().equals(INTENT_ACTION_UPDATE_PROGRESS)) {
                if (progressBar != null)
                    progressBar.setProgress(intent.getIntExtra(INTENT_EXTRA_PROGRESS, 0));
            } else if (intent.getAction().equals(INTENT_ACTION_UPDATE_LAYOUT)) {
                Log.d("PopUpActivity", "BroadcastReceiver.INTENT_ACTION_UPDATE_LAYOUT");
                isCancelable = intent.getBooleanExtra(INTENT_EXTRA_CANCELABLE, true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        clearLayout();
                        setLayout(intent);
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("PopUpActivity", "onCreate() popuptype = " + getIntent().getIntExtra(INTENT_EXTRA_TYPE, PopUp.TYPE_MAX));
        setContentView(R.layout.activity_popup);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //  animationWebview = (WebView) findViewById(R.id.giff_animation_webview);
        imageIcon = (ImageView) findViewById(R.id.image_icon);
        titleTxt = (TextView) findViewById(R.id.flash_projects_title_txt);
        titleTxt.setTypeface(MBApp.getApp().getTypeface());
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        spinnerBar = (ProgressBar) findViewById(R.id.spinnerBar);
        messageTxt = (TextView) findViewById(R.id.messageTxt);
        messageTxt.setTypeface(MBApp.getApp().getTypeface());

        layoutBottom = (LinearLayout) findViewById(R.id.popup_bottom_layout);

        okButton = (Button) findViewById(R.id.imageButtonOk);
        cancelButton = (Button) findViewById(R.id.imageButtonCancel);
        affirmationOKButton = (Button) findViewById(R.id.affirmationOKBtn);
        affirmationOKButton.setTypeface(MBApp.getApp().getTypeface());

        isCancelable = getIntent().getBooleanExtra(INTENT_EXTRA_CANCELABLE, true);

        setLayout(getIntent());
        //listen for close or update progress.xml request
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_CLOSE));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_UPDATE_PROGRESS));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_UPDATE_LAYOUT));

        //notify creation of activity to calling code PopUp class
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(INTENT_ACTION_CREATED));


        gifImageView = (GifImageView) findViewById(R.id.pop_up_gif_image_view);


    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure sure animation remains loading
        //    animationWebview.onResume();
        //  gifImageView;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ensure animation pauses
        //   animationWebview.onPause();
        //    gifImageView.stop();

    }

    private void clearLayout() {
        //   gifFromAssets.reset(); // tODO
        ; // ~ TODO check it doesn't screw up giff animation
        imageIcon.setImageResource(R.drawable.overwrite_face);
        imageIcon.setBackgroundResource(0);
        titleTxt.setVisibility(View.GONE);
        messageTxt.setVisibility(View.GONE);
        layoutBottom.setVisibility(View.GONE);
        okButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        affirmationOKButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        spinnerBar.setVisibility(View.GONE);
    }

    private void setLayout(Intent intent) {
        String title = intent.getStringExtra(INTENT_EXTRA_TITLE);

        if (!title.isEmpty()) {
            titleTxt.setText(title);
            titleTxt.setVisibility(View.VISIBLE);
        }

        String message = intent.getStringExtra(INTENT_EXTRA_MESSAGE);
        if (!message.isEmpty()) {
            messageTxt.setText(message);
            messageTxt.setVisibility(View.VISIBLE);
        }

        int imageResId = intent.getIntExtra(INTENT_EXTRA_ICON, 0);
        int imageBackgroundResId = intent.getIntExtra(INTENT_EXTRA_ICONBG, 0);
        if (imageResId != 0) {
            imageIcon.setImageResource(imageResId);
        }
        if (imageBackgroundResId != 0) {
            imageIcon.setBackgroundResource(imageBackgroundResId);
        }

        // Loading the Giff only if the animation code isn't default 0
        int imageGiffAnimationCode = intent.getIntExtra(INTENT_GIFF_ANIMATION_CODE, 0); // Default value is 0 (no animation ) 2 = flash, set to 2 for testing
        if (imageGiffAnimationCode != 0) {
            switch (imageGiffAnimationCode) {
                // Flashing screen
                case 1:
                    imageIcon.setVisibility(View.GONE);

                    //asset file
                    findViewById(R.id.pop_up_gif_image_view).setBackgroundResource(R.drawable.testing_flashing_microbit);
                    findViewById(R.id.pop_up_gif_image_view).setVisibility(View.VISIBLE);

                    break;

                // Error screen
                case 2:
                    imageIcon.setVisibility(View.GONE);

                    //asset file
                    GifImageView giffImageViewFail = (GifImageView) findViewById(R.id.pop_up_gif_image_view);
                    giffImageViewFail.setBackgroundResource(R.drawable.fail_flashing_microbit);
                    break;
            }
            // Set default plain icon
        } else {
            imageIcon.setVisibility(View.VISIBLE);
            giffDrawable.setVisible(false, false);

        }

        switch (intent.getIntExtra(INTENT_EXTRA_TYPE, PopUp.TYPE_MAX)) {
            case PopUp.TYPE_CHOICE:
                layoutBottom.setVisibility(View.VISIBLE);
                okButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_ALERT:
                layoutBottom.setVisibility(View.VISIBLE);
                affirmationOKButton.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_PROGRESS:
            case PopUp.TYPE_PROGRESS_NOT_CANCELABLE:
                progressBar.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_NOBUTTON:
                break;
            case PopUp.TYPE_SPINNER:
            case PopUp.TYPE_SPINNER_NOT_CANCELABLE:
                spinnerBar.setVisibility(View.VISIBLE);
                break;

            default:
                //TODO: handle Error
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("PopUpActivity", "onDestroy()");
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(INTENT_ACTION_DESTROYED));
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onBackPressed() {
        Log.d("PopUpActivity", "onBackPressed IsCancelable " + isCancelable);
        if (!isCancelable)
            return;

        //Do not call super.onBackPressed() because we let the calling PopUp code to issue a "hide" call.
        //PopUp code is the master code which decides when to destroy or create PopUpActivity.
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(INTENT_ACTION_CANCEL_PRESSED));
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        //       intent.putExtra(INTENT_EXTRA_INPUTTEXT, inputText.getText().toString());

        switch (v.getId()) {
            case R.id.imageButtonOk:
                intent.setAction(INTENT_ACTION_OK_PRESSED);
                break;

            case R.id.imageButtonCancel:
            case R.id.affirmationOKBtn:
                intent.setAction(INTENT_ACTION_CANCEL_PRESSED);
                break;
        }

        LocalBroadcastManager.getInstance(this).sendBroadcastSync(intent);
    }
}
