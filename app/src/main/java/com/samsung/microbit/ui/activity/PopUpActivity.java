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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samsung.microbit.R;
import com.samsung.microbit.ui.PopUp;

/**
 * Created by fma on 28/07/15.
 */
public class PopUpActivity extends Activity implements View.OnClickListener{

    //intent from PopUpActivity to PopUp
    static public final String INTENT_ACTION_OK_PRESSED = "PopUpActivity.OK_PRESSED";
    static public final String INTENT_ACTION_CANCEL_PRESSED = "PopUpActivity.CANCEL_PRESSED";
    //intent from PopUp to PopUpActivity
    static public final String INTENT_ACTION_CLOSE = "PopUpActivity.CLOSE";
    static public final String INTENT_ACTION_UPDATE_PROGRESS = "PopUpActivity.UPDATE_PROGRESS";

    static public final String INTENT_EXTRA_TYPE = "type";
    static public final String INTENT_EXTRA_TITLE = "title";
    static public final String INTENT_EXTRA_MESSAGE = "message";
    static public final String INTENT_EXTRA_INPUTTEXT = "inputText";
    static public final String INTENT_EXTRA_ICON = "imageIcon";
    static public final String INTENT_EXTRA_ICONBG = "imageIconBg";
    static public final String INTENT_EXTRA_PROGRESS = "progress";

    private ImageView imageIcon = null;
    private TextView titleTxt = null;
    private ProgressBar progressBar = null;
    private ProgressBar spinnerBar = null;
    private TextView messageTxt = null;
    private EditText inputText = null;
    private ImageButton okButton = null;
    private ImageButton cancelButton = null;
    private ImageButton button = null;

    private RelativeLayout layoutBottom = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_ACTION_CLOSE)) {
                finish();
            }
            else if (intent.getAction().equals(INTENT_ACTION_UPDATE_PROGRESS)) {
                if (progressBar != null)
                    progressBar.setProgress(intent.getIntExtra(INTENT_EXTRA_PROGRESS,0));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_popup);

        imageIcon = (ImageView)findViewById(R.id.imageIcon);
        titleTxt = (TextView)findViewById(R.id.titleTxt);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        spinnerBar = (ProgressBar) findViewById(R.id.spinnerBar);
        messageTxt = (TextView)findViewById(R.id.messageTxt);
        inputText = (EditText) findViewById(R.id.inputText);

        layoutBottom = (RelativeLayout) findViewById(R.id.bottomLayout);

        okButton = (ImageButton) findViewById(R.id.imageButtonOk);
        cancelButton = (ImageButton) findViewById(R.id.imageButtonCancel);
        button = (ImageButton) findViewById(R.id.imageButton);

        setLayout(getIntent());
        //listen for close or update progress request
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_CLOSE));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_UPDATE_PROGRESS));
    }

    private void setLayout(Intent intent)
    {
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

        inputText.setText(intent.getStringExtra(INTENT_EXTRA_INPUTTEXT));
        inputText.setSelection(inputText.getText().length());

        int imageResId = intent.getIntExtra(INTENT_EXTRA_ICON, 0);
        int imageBackgroundResId = intent.getIntExtra(INTENT_EXTRA_ICONBG, 0);
        if (imageResId != 0) {
            imageIcon.setImageResource(imageResId);
        }
        if (imageBackgroundResId != 0) {
            imageIcon.setBackgroundResource(imageBackgroundResId);
        }

        switch (intent.getIntExtra(INTENT_EXTRA_TYPE, PopUp.TYPE_MAX)) {
            case PopUp.TYPE_CHOICE:
                layoutBottom.setVisibility(View.VISIBLE);
                okButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_ALERT:
                layoutBottom.setVisibility(View.VISIBLE);
                button.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_PROGRESS:
                progressBar.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_NOBUTTON:
                break;
            case PopUp.TYPE_SPINNER:
                spinnerBar.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_INPUTTEXT:
                layoutBottom.setVisibility(View.VISIBLE);
                okButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                inputText.setVisibility(View.VISIBLE);
                break;
            default:
                //TODO: handle Error
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(INTENT_ACTION_CANCEL_PRESSED));
    }

    @Override
    public void onClick(View v) {

        finish();

        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_INPUTTEXT, inputText.getText().toString());

        switch (v.getId()) {
            case R.id.imageButtonOk:
                intent.setAction(INTENT_ACTION_OK_PRESSED);
                break;

            case R.id.imageButtonCancel:
            case R.id.imageButton:
                intent.setAction(INTENT_ACTION_CANCEL_PRESSED);
                break;
        }

        LocalBroadcastManager.getInstance(this).sendBroadcastSync(intent);
    }
}
