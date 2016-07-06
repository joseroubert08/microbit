package com.samsung.microbit.presentation;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.common.AppInfo;
import com.samsung.microbit.common.RetrieveConfigDataTask;

import java.io.File;
import java.io.IOException;

public class AppInfoPresenter implements Presenter {
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
    private AppInfo appInfo;

    private AsyncTask<String, Void, ?> retrieveAppInfoTask;

    public AppInfoPresenter() {
        appContext = MBApp.getApp();
        appInfo = MBApp.getApp().getAppInfo();
    }

    @Override
    public void start() {
        if (appContext == null) {
            Log.d("RemoteConfig", "Context is null. cannot get the config");
            return;
        }
        //Install the HTTP Cache
        try {
            File httpCacheDir = new File(MBApp.getApp().getCacheDir(), "https");
            long httpCacheSize = 3 * 1024 * 1024; // 3 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i("RemoteConfig", "HTTP response cache installation failed:" + e);
        }

        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            //   If cache is present, flush it to the filesystem.
            cache.flush();
        }

        //Check if we should get new config file
        if (shouldRefreshValue()) {
            if(retrieveAppInfoTask != null) {
                retrieveAppInfoTask.cancel(true);
            }

            retrieveAppInfoTask = new RetrieveAppInfoTask(appInfo.getPreferences(), appInfo);

            retrieveAppInfoTask.execute();
        }
    }

    private boolean shouldRefreshValue() {
        if (appInfo.getLastQueryTime() + appInfo.getMaxAge() * 1000 < System.currentTimeMillis()) {
            return true;
        }
        return true;
    }

    @Override
    public void stop() {
        if(retrieveAppInfoTask != null && retrieveAppInfoTask.getStatus() == AsyncTask.Status.RUNNING) {
            retrieveAppInfoTask.cancel(true);
            retrieveAppInfoTask = null;
        }
    }

    @Override
    public void destroy() {
       stop();

        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
                Log.i("RemoteConfig", "HTTP response cache close failed:" + e);
            }
        }
    }

    private static class RetrieveAppInfoTask extends RetrieveConfigDataTask {
        private AppInfo appInfo;

        RetrieveAppInfoTask(SharedPreferences preferences, AppInfo appInfo) {
            super(preferences);
            this.appInfo = appInfo;
            setEtag(appInfo.getEtag());
            setLastQueryTime(appInfo.getLastQueryTime());

        }

        @Override
        protected void onPostExecute() {
            appInfo.searchStoredValues();
        }
    }
}
