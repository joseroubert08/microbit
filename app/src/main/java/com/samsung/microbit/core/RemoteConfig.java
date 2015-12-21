package com.samsung.microbit.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.util.Log;

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.transform.Result;

public class RemoteConfig
{
    private static volatile RemoteConfig instance;
    private URL mConfigLocation;
    private long mUpdateTime;
    private SharedPreferences mPreferences;
    private Context mContext;


    public static final String REMOTE_CONFIG_PREFERENCE_KEY = "com.samsung.microbit.remote_config_preferences";
    public static final String RC_APPSTATUS_KEY = "com.samsung.microbit.appStatus";
    public static final String RC_ENDPOINT_ABOUT = "com.samsung.microbit.about";
    public static final String RC_ENDPOINT_TOU = "com.samsung.microbit.termsOfUse";
    public static final String RC_ENDPOINT_PP = "com.samsung.microbit.privacyPolicy";
    public static final String RC_CONFIG_EMAIL = "com.samsung.microbit.feedbackEmailAddress";
    public static final String RC_ETAG = "com.samsung.microbit.ETag";
    public static final String RC_LAST_MODIFIED_STRING = "com.samsung.microbit.lastmodifiedString";
    public static final String RC_LAST_QUERY = "com.samsung.microbit.lastQuery";
    public static final String RC_MAX_AGE = "com.samsung.microbit.maxage";

    //Store for shared preference
    private String blankValue = "";

    private String m_AppStatus = null ;
    private String m_aboutURL = null ;
    private String m_TOUURL = null ;
    private String m_PPURL = null ;
    private String m_sendToEmail = null ;
    private String m_Etag = null ;
    private String m_lastModifiedString = null ;
    private long   m_LastQueryTime = 0 ;
    private long   m_maxAge = 0 ;

    private HttpResponseCache m_cache = null;

    public static RemoteConfig getInstance()
    {
        if (instance == null) {
            synchronized (RemoteConfig.class) {
                if (instance == null) {
                    instance = new RemoteConfig();
                }
            }
        }
        return instance;
    }

    public void init() {
        Context context = MBApp.getContext();
        if (context == null) {
            Log.d("RemoteConfig", "Context is null. cannot get the config");
            return;
        }
        if (!getStoredValues(context)) {
            Log.e("RemoteConfig", "Could not read from shared preference");
            return;
        }
        //Install the HTTP Cache
        try {
            File httpCacheDir = new File(MBApp.getContext().getCacheDir(), "https");
            long httpCacheSize = 3 * 1024 * 1024; // 3 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i("RemoteConfig", "HTTP response cache installation failed:" + e);
        }
        m_cache = HttpResponseCache.getInstalled();
        if (m_cache!= null){
            //   If cache is present, flush it to the filesystem.
            m_cache.flush();
        }

        //Check if we should get new config file
        if (shouldRefreshValue()) {
            new RetrieveConfigFile().execute("");
        }
    }

    private boolean getStoredValues(Context context)
    {
        //Get values previously stored in the sharedpreference
        mPreferences = context.getSharedPreferences(REMOTE_CONFIG_PREFERENCE_KEY, Context.MODE_PRIVATE);
        if (mPreferences == null){
            Log.d("RemoteConfig", "SharedPreferences is null. cannot get the config");
            return false;
        }
        m_AppStatus = mPreferences.getString(RC_APPSTATUS_KEY,"Off");
        m_aboutURL = mPreferences.getString(RC_ENDPOINT_ABOUT,blankValue);
        m_TOUURL = mPreferences.getString(RC_ENDPOINT_TOU,blankValue);
        m_PPURL = mPreferences.getString(RC_ENDPOINT_PP,blankValue);
        m_sendToEmail = mPreferences.getString(RC_CONFIG_EMAIL,blankValue);
        m_Etag = mPreferences.getString(RC_ETAG,blankValue);
        m_LastQueryTime = mPreferences.getLong(RC_LAST_QUERY, 0);
        m_maxAge = mPreferences.getLong(RC_LAST_QUERY, 0);
        return true;
    }

    private boolean shouldRefreshValue()
    {
        if (m_LastQueryTime + m_maxAge*1000 < System.currentTimeMillis()  ){
            return true;
        }
        return true;
    }

    ///Getters
    public String getAppStatus() {
        return m_AppStatus;
    }

    public String getAboutURL() {
        return m_aboutURL;
    }

    public String getTermsOfUseURL() {
        return m_TOUURL;
    }

    public String getPrivacyURL() {
        return m_PPURL;
    }

    public String getSendEmailAddress() {
        return m_sendToEmail;
    }
    class RetrieveConfigFile extends AsyncTask<String, Void, Result> {

        @Override
        protected Result doInBackground(String... urls) {
            String version = "0.1.0" ;
            if (BuildConfig.DEBUG) {
                //Hardcoding the version for Debug builds
                version = "1.3.6" ;
                Log.d("RemoteConfig", "Using config file for version :  " + version);
            }else{
                PackageManager manager = MBApp.getContext().getPackageManager();
                PackageInfo info = null;
                try {
                    info = manager.getPackageInfo(MBApp.getContext().getPackageName(), 0);
                    version = info.versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            //Get the new config file
            String urlString =  String.format(MBApp.getContext().getResources().getString(R.string.remote_config_url), version);
            HttpsURLConnection urlConnection = null ;
            try {
                URL remoteConfigURL = new URL(urlString);
                urlConnection = (HttpsURLConnection) remoteConfigURL.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Connection", "keep-alive");
                urlConnection.addRequestProperty("Cache-Control", "max-stale=" + 600);
                urlConnection.setUseCaches(true);
                if (m_LastQueryTime!= 0) urlConnection.setIfModifiedSince(m_LastQueryTime);
                if (!m_Etag.equals(blankValue)) urlConnection.setRequestProperty("If-None-Match", m_Etag);

                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream stream = urlConnection.getInputStream();
                    //Process the response headers and populate the shared preference
                    SharedPreferences.Editor editor = mPreferences.edit();
                    String eTag = urlConnection.getHeaderField("ETag");
                    editor.putString(RC_ETAG, eTag).commit();
                    editor.putLong(RC_LAST_QUERY, System.currentTimeMillis()).commit();
                    //Get the data
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    if (!response.toString().isEmpty())
                        readFromJsonAndStore(response.toString());

                } else if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED){
                    Log.d("RemoteConfig" , "Content not modified");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return null;
        }

        private void readFromJsonAndStore(String jsonFile) {
            try {
                SharedPreferences.Editor editor = mPreferences.edit();
                JSONObject reader = new JSONObject(jsonFile);

                JSONObject status  = reader.getJSONObject("status");
                String appStatus = status.getString("appStatus");
                editor.putString(RC_APPSTATUS_KEY, appStatus).commit();

                JSONObject endpoints  = reader.getJSONObject("endpoints");
                String about = endpoints.getString("about");
                String tou = endpoints.getString("termsOfUse");
                String pp = endpoints.getString("privacyPolicy");

                editor.putString(RC_ENDPOINT_ABOUT , about).commit();
                editor.putString(RC_ENDPOINT_TOU, tou).commit();
                editor.putString(RC_ENDPOINT_PP, pp).commit();

                JSONObject config  = reader.getJSONObject("config");
                String email = config.getString("feedbackEmailAddress");

                editor.putString(RC_CONFIG_EMAIL, email).commit();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        protected void onPostExecute(Result result) {
            getStoredValues(MBApp.getContext());
        }
    }
}