package com.samsung.microbit.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

import org.apache.cordova.CordovaChromeClient;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewClient;
import org.apache.cordova.LOG;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class CodeSectionFragment extends Fragment implements CordovaInterface {
    private boolean bound;
	private boolean volumeupBound;
    private boolean volumedownBound;
    
    String TAG = "CodeFragment";
    private CordovaPlugin activityResultCallback;
    private Object activityResultKeepRunning;
    private Object keepRunning;
    
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    
	CordovaWebView touchDevelopView = null;
    ProgressBar  touchDevelopProgress = null;
	TextView loadingText = null ;

    public static CodeSectionFragment newInstance() {
    	CodeSectionFragment fragment = new CodeSectionFragment();
        return fragment;
    }


	private class myWebViewClient extends CordovaWebViewClient {

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
	}

	private class myWebViewChromeClient extends CordovaChromeClient{

		public myWebViewChromeClient(CordovaInterface cordova) {
			super(cordova);
		}
		public void onProgressChanged(WebView view, int newProgress) {
			LOG.d(TAG, "onProgressChanged");
            CodeSectionFragment.this.setValue(newProgress);
			super.onProgressChanged(view, newProgress);
		}

		@Override
		public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
			LOG.d(TAG, "onJsAlert");
			return super.onJsAlert(view, url, message, result);
		}
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

    	LayoutInflater localInflater = inflater.cloneInContext(new CordovaContext(MBApp.getContext(), this));
    	View rootView = localInflater.inflate(R.layout.fragment_section_webview, container, false);
    	touchDevelopView = (CordovaWebView) rootView.findViewById(R.id.touchDevelopView);
        touchDevelopProgress = (ProgressBar) rootView.findViewById(R.id.progressBar);
		touchDevelopProgress.setProgress(0);
        touchDevelopProgress.setMax(100);
		loadingText = (TextView) rootView.findViewById(R.id.loadingText);
        //Load URL now
        touchDevelopView.loadUrl("https://live.microbit.co.uk/app/?fota=1");


        touchDevelopView.setWebChromeClient(new myWebViewChromeClient(this));
        touchDevelopView.setWebViewClient( new myWebViewClient(this,touchDevelopView));

    	return rootView;
    }

    public void setValue(int progress) {
        if (touchDevelopProgress != null)
            touchDevelopProgress.setProgress(progress);
    }

	public void bindBackButton(boolean override) {
        this.bound = override;
	}

	public void bindButton(String button, boolean override) {
      if (button.compareTo("volumeup")==0) {
        this.volumeupBound = override;
      }
      else if (button.compareTo("volumedown")==0) {
        this.volumedownBound = override;
      }
	}

	@Deprecated
	public void cancelLoadUrl() {
		// TODO Auto-generated method stub
		
	}
	public boolean isBackButtonBound() {
        return this.bound;
	}

	@Override
	public Object onMessage(String id, Object data) {
		LOG.d(TAG, "onMessage(" + id + "," + data + ")");
        switch(id){
            case "spinner":
                if (touchDevelopProgress != null ){
                    touchDevelopProgress.setVisibility(View.INVISIBLE);
					loadingText.setVisibility(View.INVISIBLE);
                }
                break;
        }
        return null;
	}

	@Override
	public ExecutorService getThreadPool() {
		return threadPool;
	}
	public void onDestroy() {
        super.onDestroy();
        if (touchDevelopView.pluginManager != null) {
        	touchDevelopView.pluginManager.onDestroy();
        }
    }
	@Override
	public void setActivityResultCallback(CordovaPlugin plugin) {
		this.activityResultCallback = plugin;
	}

	@Override
	public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
        this.activityResultCallback = command;
        this.activityResultKeepRunning = this.keepRunning;

        // If multitasking turned on, then disable it for activities that return results
        if (command != null) {
            this.keepRunning = false;
        }
        // Start activity
        super.startActivityForResult(intent, requestCode);
	}
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        CordovaPlugin callback = this.activityResultCallback;
        if (callback != null) {
            callback.onActivityResult(requestCode, resultCode, intent);
        }
    }
	
	//CordovaContext
	private static class CordovaContext extends ContextWrapper implements CordovaInterface
	{
	    CordovaInterface cordova;

	    public CordovaContext(Context base, CordovaInterface cordova) {
	        super(base);
	        this.cordova = cordova;
	    }
	    public void startActivityForResult(CordovaPlugin command,
	                                       Intent intent, int requestCode) {
	        cordova.startActivityForResult(command, intent, requestCode);
	    }
		public void setActivityResultCallback(CordovaPlugin plugin) {
	        cordova.setActivityResultCallback(plugin);
	    }
	    public Activity getActivity() {
	        return cordova.getActivity();
	    }
	    public Object onMessage(String id, Object data) {
	        return cordova.onMessage(id, data);
	    }
	    public ExecutorService getThreadPool() {
	        return (cordova.getThreadPool());
	    }
		public void bindBackButton(boolean arg0) {
			
		}
		public void bindButton(String arg0, boolean arg1) {
			// TODO Auto-generated method stub
			
		}
		@Deprecated
		public void cancelLoadUrl() {
			// TODO Auto-generated method stub
			
		}
		public boolean isBackButtonBound() {
			// TODO Auto-generated method stub
			return false;
		}
	}
}
