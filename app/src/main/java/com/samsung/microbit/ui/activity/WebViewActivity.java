package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.Constants;

import org.apache.cordova.Config;
import org.apache.cordova.CordovaChromeClient;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebViewActivity extends Activity implements CordovaInterface {

    private CordovaWebView touchDevelopView = null;
    private ProgressBar progress = null;
    private TextView loadingTxt = null;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        MBApp.setContext(this);

        setContentView(R.layout.activity_webview);

        touchDevelopView = (CordovaWebView) findViewById(R.id.touchDevelopView);
        touchDevelopView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        Config.init(this);

        loadingTxt = (TextView) findViewById(R.id.loadingTxt);

        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setMax(100);

        Intent intent = getIntent();
        String url = intent.getStringExtra(Constants.URL);

        //Load URL now
        if(url == null) {
            touchDevelopView.loadUrl(getString(R.string.touchDevURLNew));
        } else {
            touchDevelopView.loadUrl(url);
        }

        touchDevelopView.setWebChromeClient(new CordovaChromeClient(this) {
            public void onProgressChanged(WebView view, int newProgress) {
                WebViewActivity.this.setValue(newProgress);
                super.onProgressChanged(view, newProgress);
            }
        });

        touchDevelopView.setWebViewClient(new CordovaWebViewClient(this, touchDevelopView) {

        });

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
        this.progress.setProgress(progress);
    }

    @Override
    public void startActivityForResult(CordovaPlugin cordovaPlugin, Intent intent, int i) {

    }

    @Override
    public void setActivityResultCallback(CordovaPlugin cordovaPlugin) {

    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public Object onMessage(String s, Object o) {
        return null;
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }
}