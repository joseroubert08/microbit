package com.samsung.microbit.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.transform.Result;

import static com.samsung.microbit.common.ConfigInfo.*;

/**
 * Base class for loading config data.
 */
public abstract class SaveConfigInfoTask extends AsyncTask<String, Void, Result> {
    private static final String TAG = SaveConfigInfoTask.class.getSimpleName();

    private final SharedPreferences preferences;
    private long LastQueryTime = 0;
    private String Etag = "";
    private Context appContext;

    public SaveConfigInfoTask(SharedPreferences preferences) {
        this.preferences = preferences;
        this.appContext = MBApp.getApp();
    }

    public void setEtag(String etag) {
        if (etag != null) {
            Etag = etag;
        } else {
            Etag = "";
        }
    }

    public void setLastQueryTime(long lastQueryTime) {
        LastQueryTime = lastQueryTime;
    }

    @Override
    protected final void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Result doInBackground(String... urls) {
        String version = "0.1.0";
        if (BuildConfig.DEBUG) {
            //Hardcoding the version for Debug builds
            version = "1.3.6";
            Log.d(TAG, "Using config file for version :  " + version);
        } else {
            PackageManager manager = appContext.getPackageManager();
            PackageInfo info;
            try {
                info = manager.getPackageInfo(appContext.getPackageName(), 0);
                version = info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, e.toString());
            }
        }
        //Get the new config file
        String urlString = String.format(appContext.getResources().getString(R.string.remote_config_url), version);
        HttpsURLConnection urlConnection = null;
        try {
            URL remoteConfigURL = new URL(urlString);
            urlConnection = (HttpsURLConnection) remoteConfigURL.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Connection", "keep-alive");
            urlConnection.addRequestProperty("Cache-Control", "max-stale=" + 600);
            urlConnection.setUseCaches(true);

            if (LastQueryTime != 0) {
                urlConnection.setIfModifiedSince(LastQueryTime);
            }

            if (!Etag.equals("")) {
                urlConnection.setRequestProperty("If-None-Match", Etag);
            }

            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream stream = urlConnection.getInputStream();
                //Process the response headers and populate the shared preference
                SharedPreferences.Editor editor = preferences.edit();
                String eTag = urlConnection.getHeaderField("ETag");
                editor.putString(RC_ETAG, eTag);
                editor.putLong(RC_LAST_QUERY, System.currentTimeMillis()).apply();
                //Get the data
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                if (!response.toString().isEmpty()) {
                    readFromJsonAndStore(response.toString());
                }

            } else if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.d(TAG, "Content not modified");
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    private void readFromJsonAndStore(String jsonFile) {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            JSONObject reader = new JSONObject(jsonFile);
            //Read the application status
            readAppStatus(reader, editor);
            //Read end points
            readEndPoints(reader, editor);
            //Read end points
            readFeedbackInfo(reader, editor);

            editor.apply();
        } catch (JSONException e) {
            Log.e(TAG, "readFromJsonAndStore: failed - " + e.toString());
        }
    }

    /**
     * Read and store application status. If it's allowed to running or no.
     *
     * @param reader Container of the data to read from.
     * @param editor Container of the data to read to.
     */
    private void readAppStatus(JSONObject reader, SharedPreferences.Editor editor) {
        try {
            JSONObject status = reader.getJSONObject("status");
            String appStatus = status.getString("appStatus");
            editor.putString(RC_APPSTATUS_KEY, appStatus).commit();
            //Get extended message if needed
            if (appStatus.equalsIgnoreCase(ConfigInfo.AppStatus.OFF.toString())) {
                String title = status.getString("title");
                String message = status.getString("message");
                editor.putString(RC_EXCEPTIONTITLE_KEY, title);
                editor.putString(RC_EXCEPTIONMSG_KEY, message);
            }
        } catch (JSONException e) {
            Log.e(TAG, "readAppStatus: failed - " + e.toString());
        }
    }

    /**
     * Read and store end points for communication with server to share statistic, etc...
     *
     * @param reader Container of the data to read from.
     * @param editor Container of the data to read to.
     */
    private void readEndPoints(JSONObject reader, SharedPreferences.Editor editor) {
        try {
            JSONObject endpoints = reader.getJSONObject("endpoints");
            String about = endpoints.getString("about");
            String tou = endpoints.getString("termsOfUse");
            String pp = endpoints.getString("privacyPolicy");
            String createcode = endpoints.getString("createCode");
            String discover = endpoints.getString("discover");
            String myscripts = endpoints.getString("myScripts");

            editor.putString(RC_ENDPOINT_ABOUT, about);
            editor.putString(RC_ENDPOINT_TOU, tou);
            editor.putString(RC_ENDPOINT_PP, pp);
            editor.putString(RC_ENDPOINT_CREATECODE, createcode);
            editor.putString(RC_ENDPOINT_DISCOVER, discover);
            editor.putString(RC_APPSTATUS_MYSCRIPTS, myscripts);

        } catch (JSONException e) {
            Log.e(TAG, "readEndPoints: failed - " + e.toString());
        }
    }

    /**
     * Read and store information for user feedback.
     *
     * @param reader Container of the data to read from.
     * @param editor Container of the data to read to.
     */
    private void readFeedbackInfo(JSONObject reader, SharedPreferences.Editor editor) {
        try {
            JSONObject config = reader.getJSONObject("config");
            String email = config.getString("feedbackEmailAddress");
            editor.putString(RC_CONFIG_EMAIL, email);
        } catch (JSONException e) {
            Log.e(TAG, "readFeedbackInfo: failed - " + e.toString());
        }
    }

    protected void onPostExecute(Result result) {
        onPostExecute();
    }

    protected abstract void onPostExecute();
}