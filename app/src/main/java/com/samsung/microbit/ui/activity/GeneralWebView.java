package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.samsung.microbit.R;
import com.samsung.microbit.model.Constants;

public class GeneralWebView extends Activity {

    WebView webView = null ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_general_web_view);
        webView = (WebView) findViewById(R.id.generalView);
        TextView title = (TextView) findViewById(R.id.titleTxt);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);


        //Check parameters Before load
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        String titleString = intent.getStringExtra("title");

        title.setText(titleString);
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public void onClick(final View v) {
        if(v.getId() == R.id.backBtn){
            finish();
        }
    }
}
