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

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class JSSimulatorFragment extends Fragment implements CordovaInterface {
    private boolean bound;
	private boolean volumeupBound;
    private boolean volumedownBound;
    
    String TAG = "JSSimulatorFragment";
    private CordovaPlugin activityResultCallback;
    private Object activityResultKeepRunning;
    private Object keepRunning;
    
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    
	CordovaWebView jsSimulatorView;
    public static JSSimulatorFragment newInstance() {
    	JSSimulatorFragment fragment = new JSSimulatorFragment();
        return fragment;
    }
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	LayoutInflater localInflater = inflater.cloneInContext(new CordovaContext(MBApp.getContext(), this));
    	View rootView = localInflater.inflate(R.layout.fragment_js_simulator, container, false);
    	jsSimulatorView = (CordovaWebView) rootView.findViewById(R.id.jsSimulatorView);
        jsSimulatorView.loadUrl("file:///android_asset/www/index.html"); //use this for running JS2JAVA simulator
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
        if (jsSimulatorView.pluginManager != null) {
        	jsSimulatorView.pluginManager.onDestroy();
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
