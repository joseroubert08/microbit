package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.JsResult;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.DownloadFilesTask;
import com.samsung.microbit.core.DownloadManager;
import com.samsung.microbit.model.Constants;

import org.apache.cordova.Config;
import org.apache.cordova.CordovaChromeClient;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewClient;
import org.apache.cordova.LOG;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebViewActivity extends Activity implements CordovaInterface {

    private CordovaWebView touchDevelopView = null;
    private ProgressBar touchDevelopProgress = null;
    private TextView loadingTxt = null;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private CordovaPlugin activityResultCallback;

    String TAG = "WebViewActivity";

    private class myWebViewClient extends CordovaWebViewClient{

        public myWebViewClient(CordovaInterface cordova) {
            super(cordova);
        }
        public myWebViewClient(CordovaInterface cordova, CordovaWebView view){
            super(cordova,view);
        }
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            LOG.d(TAG, "onReceivedHttpAuthRequest");
            final WebView mView = view;
            final HttpAuthHandler mHandler = handler;

            mHandler.proceed("microbit", "bitbug42");
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.endsWith(".hex")) {
                new DownloadFilesTask().execute(url);
                return true;
            }
            return false;
        }
    }

    private class myWebViewChromeClient extends CordovaChromeClient{

        public myWebViewChromeClient(CordovaInterface cordova) {
            super(cordova);
        }
        public void onProgressChanged(WebView view, int newProgress) {
            LOG.d(TAG, "onProgressChanged");
            WebViewActivity.this.setValue(newProgress);
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            LOG.d(TAG, "onJsAlert");
            return super.onJsAlert(view, url, message, result);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        MBApp.setContext(this);

        setContentView(R.layout.activity_webview);

        touchDevelopView = (CordovaWebView) findViewById(R.id.touchDevelopView);
        touchDevelopView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        touchDevelopView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        touchDevelopView.getSettings().setJavaScriptEnabled(true);

        Config.init(this);

        loadingTxt = (TextView) findViewById(R.id.loadingTxt);

        touchDevelopProgress = (ProgressBar) findViewById(R.id.progressBar);
        touchDevelopProgress.setMax(100);

        Intent intent = getIntent();
        String url = intent.getStringExtra(Constants.URL);

        //Load URL now
        if(url == null) {
            touchDevelopView.loadUrl(getString(R.string.touchDevURLNew));
        } else {
            touchDevelopView.loadUrl(url);
        }
        touchDevelopView.setWebChromeClient(new myWebViewChromeClient(this));
        touchDevelopView.setWebViewClient( new myWebViewClient(this,touchDevelopView));

        touchDevelopView.setVisibility(View.INVISIBLE);
    }

    public void onBtnClicked(View v){
        if(v.getId() == R.id.homeBtn){
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
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
                    //loadingText.setVisibility(View.INVISIBLE);
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