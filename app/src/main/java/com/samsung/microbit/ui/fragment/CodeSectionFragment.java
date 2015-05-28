package com.samsung.microbit.ui.fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

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
    
	CordovaWebView touchDevelopView;
    public static CodeSectionFragment newInstance() {
    	CodeSectionFragment fragment = new CodeSectionFragment();
        return fragment;
    }
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	LayoutInflater localInflater = inflater.cloneInContext(new CordovaContext(MBApp.getContext(), this));
    	View rootView = localInflater.inflate(R.layout.fragment_section_webview, container, false);
    	touchDevelopView = (CordovaWebView) rootView.findViewById(R.id.touchDevelopView);
		touchDevelopView.loadUrl("https://microbit:bitbug42@live.microbit.co.uk/");
    	//touchDevelopView.loadUrl("https://www.touchdevelop.com/app#");
        //touchDevelopView.loadUrl("file:///android_asset/www/index.html"); //use this for running JS2JAVA simulator                
    	//touchDevelopView.loadUrl("http://www.highwaycodeuk.co.uk/download-pdf.html");
    	return rootView;
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
