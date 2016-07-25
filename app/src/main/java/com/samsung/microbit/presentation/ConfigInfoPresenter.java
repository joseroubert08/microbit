package com.samsung.microbit.presentation;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.common.ConfigInfo;
import com.samsung.microbit.common.SaveConfigInfoTask;

import java.io.File;
import java.io.IOException;

/**
 * Provides abilities to present config information.
 */
public class ConfigInfoPresenter implements Presenter {
    /* Sample config file
  Normal case
  {
	"status": {
		"appStatus": "on"
	},
	"endpoints": {
		"about": "https://www.microbit.co.uk/about",
		"createCode": "https://www.microbit.co.uk/create-code",
		"discover": "https://www.microbit.co.uk/#mainContent",
		"myScripts": "https://www.microbit.co.uk/app/",
		"termsOfUse": "https://www.microbit.co.uk/terms-of-use",
		"privacyPolicy": "https://www.microbit.co.uk/privacy"
	},
	"config": {
		"feedbackEmailAddress": "learning.makeitdigital@bbc.co.uk"
	}
  }
  //Exceptional case
  {
    "status": {
        "appStatus": "off",
        "title": "Application unavailable",
        "message": "A description of why the application is unavailable."
    }
  } */

    private Context appContext;
    private ConfigInfo configInfo;

    private AsyncTask<String, Void, ?> reInitConfigInfoTask;

    public ConfigInfoPresenter() {
        appContext = MBApp.getApp();
        configInfo = MBApp.getApp().getConfigInfo();
    }

    @Override
    public void start() {
        if(appContext == null) {
            Log.d("RemoteConfig", "Context is null. cannot get the config");
            return;
        }
        //Install the HTTP Cache
        try {
            File httpCacheDir = new File(MBApp.getApp().getCacheDir(), "https");
            long httpCacheSize = 3 * 1024 * 1024; // 3 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch(IOException e) {
            Log.i("RemoteConfig", "HTTP response cache installation failed:" + e);
        }

        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if(cache != null) {
            //   If cache is present, flush it to the filesystem.
            cache.flush();
        }

        //Check if we should get new config file
        if(shouldRefreshValue()) {
            if(reInitConfigInfoTask != null) {
                reInitConfigInfoTask.cancel(true);
            }

            reInitConfigInfoTask = new ReInitConfigInfoTask(configInfo.getPreferences(), configInfo);

            reInitConfigInfoTask.execute();
        }
    }

    private boolean shouldRefreshValue() {
        if(configInfo.getLastQueryTime() + configInfo.getMaxAge() * 1000 < System.currentTimeMillis()) {
            return true;
        }
        return true;
    }

    @Override
    public void stop() {
        if(reInitConfigInfoTask != null && reInitConfigInfoTask.getStatus() == AsyncTask.Status.RUNNING) {
            reInitConfigInfoTask.cancel(true);
            reInitConfigInfoTask = null;
        }
    }

    @Override
    public void destroy() {
        stop();

        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if(cache != null) {
            try {
                cache.close();
            } catch(IOException e) {
                Log.i("RemoteConfig", "HTTP response cache close failed:" + e);
            }
        }
    }

    /**
     * An asynchronous task that retrieves app information in background.
     */
    private static class ReInitConfigInfoTask extends SaveConfigInfoTask {
        private ConfigInfo configInfo;

        ReInitConfigInfoTask(SharedPreferences preferences, ConfigInfo configInfo) {
            super(preferences);
            this.configInfo = configInfo;
            setEtag(configInfo.getEtag());
            setLastQueryTime(configInfo.getLastQueryTime());

        }

        @Override
        protected void onPostExecute() {
            configInfo.reInit();
        }
    }
}
