package com.samsung.microbit.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

import org.apache.cordova.Config;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebViewActivity extends Activity implements CordovaInterface {

    private CordovaWebView touchDevelopView = null;
    private ProgressBar touchDevelopProgress = null;
    private TextView loadingTxt = null, titleTxt = null;
    private RelativeLayout topBar = null;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private CordovaPlugin activityResultCallback;
    GestureDetector gestureScanner;
    private static final int SWIPE_THRESHOLD = 10;
    private static final int SWIPE_VELOCITY_THRESHOLD = 10;

    String TAG = "WebViewActivity";

    private class myGestureScanner implements GestureDetector.OnGestureListener{

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float v, float v1) {

            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    //Do nothing
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(v) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            if(topBar.getVisibility() == View.GONE) {
                                topBar.setVisibility(View.VISIBLE);
                                topBar.animate()
                                        .translationY(0)
                                        .alpha(1.0f)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                            }
                                        });
                            }
                        } else {
                            if(topBar.getVisibility() == View.VISIBLE) {
                                topBar.animate()
                                        .translationY(-topBar.getHeight())
                                        .alpha(0.0f)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                topBar.setVisibility(View.GONE);
                                            }
                                        });
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MBApp.setContext(this);

        setContentView(R.layout.activity_webview);

        touchDevelopView = (CordovaWebView) findViewById(R.id.touchDevelopView);
        touchDevelopView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        touchDevelopView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        touchDevelopView.getSettings().setJavaScriptEnabled(true);

        Config.init(this);

        loadingTxt = (TextView) findViewById(R.id.loadingTxt);
        titleTxt = (TextView) findViewById(R.id.title);
        topBar = (RelativeLayout) findViewById(R.id.topBar);

        titleTxt.setText(getString(R.string.web_view_demo));

        touchDevelopProgress = (ProgressBar) findViewById(R.id.progressBar);
        touchDevelopProgress.setMax(100);
        gestureScanner = new GestureDetector(new myGestureScanner());

        String url = "file:///android_asset/www/index_fb.html";
        touchDevelopView.loadUrl(url);

        loadingTxt.setVisibility(View.GONE);
        touchDevelopView.setVisibility(View.VISIBLE);
        touchDevelopProgress.setVisibility(View.INVISIBLE);

        topBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                return gestureScanner.onTouchEvent(me);
            }
        });

        touchDevelopView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent me){
                if(touchDevelopView.getScrollY() == 0 || touchDevelopView.getScrollY() == v.getMeasuredHeight()){
                    return gestureScanner.onTouchEvent(me);
                } else if(touchDevelopView.getScrollY() > 20) {
                    if(topBar.getVisibility() == View.VISIBLE) {
                        topBar.animate()
                                .translationY(-topBar.getHeight())
                                .alpha(0.0f)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        topBar.setVisibility(View.GONE);
                                    }
                                });
                    }
                }
                return false;
            }
        });
    }

    public void onBtnClicked(View v){
        if(v.getId() == R.id.homeBtn){
            finish();
        } else if(v.getId() == R.id.refreshBtn){
            touchDevelopView.reload();
        }
    }

    public void setValue(int progress) {
        if(progress == 100) {
            loadingTxt.setVisibility(View.GONE);
            touchDevelopView.setVisibility(View.VISIBLE);
        }
        this.touchDevelopProgress.setProgress(progress);
    }

    @Override
    public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {

    }

    @Override
    public void setActivityResultCallback(CordovaPlugin cordovaPlugin) {

    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public Object onMessage(String id, Object data) {

        LOG.d(TAG, "onMessage(" + id + "," + data + ")");
        switch(id){
            case "spinner":
                if (touchDevelopProgress != null ){
                    touchDevelopProgress.setVisibility(View.INVISIBLE);
                    //loadingTxt.setVisibility(View.INVISIBLE);
                }
                break;
        }
        return null;
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }
}