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
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.transform.Result;

public class RemoteConfig
{
    private static volatile RemoteConfig instance;
    private SharedPreferences mPreferences;


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
  }

    */

    public static final String REMOTE_CONFIG_PREFERENCE_KEY = "com.samsung.microbit.remote_config_preferences";


    public static final String RC_APPSTATUS_KEY = "com.samsung.microbit.appStatus";
    public static final String RC_EXCEPTIONTITLE_KEY = "com.samsung.microbit.exceptiontitle";
    public static final String RC_EXCEPTIONMSG_KEY = "com.samsung.microbit.exceptionMessage";


    public static final String RC_ENDPOINT_ABOUT = "com.samsung.microbit.about";
    public static final String RC_ENDPOINT_CREATECODE = "com.samsung.microbit.createcode";
    public static final String RC_ENDPOINT_DISCOVER = "com.samsung.microbit.discover";
    public static final String RC_APPSTATUS_MYSCRIPTS = "com.samsung.microbit.myscripts";

    public static final String RC_ENDPOINT_TOU = "com.samsung.microbit.termsOfUse";
    public static final String RC_ENDPOINT_PP = "com.samsung.microbit.privacyPolicy";
    public static final String RC_CONFIG_EMAIL = "com.samsung.microbit.feedbackEmailAddress";

    public static final String RC_ETAG = "com.samsung.microbit.ETag";
    public static final String RC_LAST_MODIFIED_STRING = "com.samsung.microbit.lastmodifiedString";
    public static final String RC_LAST_QUERY = "com.samsung.microbit.lastQuery";
    public static final String RC_MAX_AGE = "com.samsung.microbit.maxage";

    private static final String TAG = RemoteConfig.class.getSimpleName();

    //Store for shared preference
    private String blankValue = "";

    private String m_AppStatus = null ;
    private String m_exceptionTitle = null ;
    private String m_exceptionMsg = null ;
    private String m_aboutURL = null ;
    private String m_createCodeURL = null ;
    private String m_discoverURL = null ;
    private String m_myScriptsURL = null ;
    private String m_TOUURL = null ;
    private String m_PPURL = null ;
    private String m_sendToEmail = null ;
    private String m_Etag = null ;
    private long   m_LastQueryTime = 0 ;
    private long   m_maxAge = 0 ;

    public static RemoteConfig getInstance()
    {
        if (instance == null) {
            synchronized (RemoteConfig.class) {
                if (instance == null) {
                    instance = new RemoteConfig();
                }
            }
        }
        instance.getStoredValues(MBApp.getContext());
        return instance;
    }

    public void init() {
        Context context = MBApp.getContext();
        if (context == null) {
            Log.d(TAG, "Context is null. cannot get the config");
            return;
        }
        if (!getStoredValues(context)) {
            Log.e(TAG, "Could not read from shared preference");
            return;
        }
        //Install the HTTP Cache
        try {
            File httpCacheDir = new File(MBApp.getContext().getCacheDir(), "https");
            long httpCacheSize = 3 * 1024 * 1024; // 3 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.e(TAG, "HTTP response cache installation failed: " + e.toString());
        }
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null){
            //   If cache is present, flush it to the filesystem.
            cache.flush();
        }

        //Check if we should get new config file
        if (shouldRefreshValue()) {
            new RetrieveConfigFile().execute("");
        }
    }


    public void destroy() {
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
                Log.e(TAG, "HTTP response cache close failed: " + e.toString());
            }
        }
    }

    private boolean getStoredValues(Context context)
    {
        //Get values previously stored in the sharedpreference
        if (mPreferences == null){
            mPreferences = context.getSharedPreferences(REMOTE_CONFIG_PREFERENCE_KEY, Context.MODE_PRIVATE);
            return false;
        }
        //Update unconditionally
        m_AppStatus = mPreferences.getString(RC_APPSTATUS_KEY,"Off");
        m_aboutURL = mPreferences.getString(RC_ENDPOINT_ABOUT,blankValue);
        m_TOUURL = mPreferences.getString(RC_ENDPOINT_TOU,blankValue);
        m_PPURL = mPreferences.getString(RC_ENDPOINT_PP,blankValue);
        m_sendToEmail = mPreferences.getString(RC_CONFIG_EMAIL,blankValue);
        m_Etag = mPreferences.getString(RC_ETAG, blankValue);
        m_LastQueryTime = mPreferences.getLong(RC_LAST_QUERY, 0);
        m_maxAge = mPreferences.getLong(RC_LAST_QUERY, 0);

        m_exceptionTitle = mPreferences.getString(RC_EXCEPTIONTITLE_KEY, blankValue);
        m_exceptionMsg = mPreferences.getString(RC_EXCEPTIONMSG_KEY,blankValue);
        m_createCodeURL = mPreferences.getString(RC_ENDPOINT_CREATECODE,blankValue);
        m_discoverURL = mPreferences.getString(RC_ENDPOINT_DISCOVER, blankValue);
        m_myScriptsURL = mPreferences.getString(RC_APPSTATUS_MYSCRIPTS,blankValue);
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
        if (m_aboutURL.isEmpty()){
            m_aboutURL = MBApp.getContext().getString(R.string.about_url);
        }
        return m_aboutURL;
    }

    public String getTermsOfUseURL() {
        if (m_TOUURL.isEmpty()){
            m_TOUURL = MBApp.getContext().getString(R.string.terms_of_use_url);
        }
        return m_TOUURL;
    }

    public String getPrivacyURL() {
        if (m_PPURL.isEmpty()){
            m_PPURL = MBApp.getContext().getString(R.string.privacy_policy_url);
        }
        return m_PPURL;
    }

    public String getSendEmailAddress() {
        if (m_sendToEmail.isEmpty()){
            m_sendToEmail = MBApp.getContext().getString(R.string.feedback_email_address);
        }
        return m_sendToEmail;
    }

    public String getExceptionTitle() {
        if (m_exceptionTitle.isEmpty()){
            m_exceptionTitle = MBApp.getContext().getString(R.string.general_error_title);
        }
        return m_exceptionTitle;
    }

    public String getExceptionMsg() {
        if (m_exceptionMsg.isEmpty()){
            m_exceptionMsg = MBApp.getContext().getString(R.string.general_error_msg);
        }
        return m_exceptionMsg;
    }

    public String getCreateCodeURL() {
        if (m_createCodeURL.isEmpty()){
            m_createCodeURL = MBApp.getContext().getString(R.string.createCodeURL);
        }
        return m_createCodeURL;
    }

    public String getDiscoverURL() {
        if (m_discoverURL.isEmpty()){
            m_discoverURL = MBApp.getContext().getString(R.string.discoverURL);
        }
        return m_discoverURL;
    }

    public String getMyScriptsURL() {
        if (m_myScriptsURL.isEmpty()){
            m_myScriptsURL = MBApp.getContext().getString(R.string.myScriptsURL);
        }
        return m_myScriptsURL;
    }

    public boolean isAppStatusOn()
    {
        getStoredValues(MBApp.getContext());
        m_AppStatus = mPreferences.getString(RC_APPSTATUS_KEY,"On");
        //Get extended message if needed
        if (m_AppStatus.equalsIgnoreCase("OFF"))
        {
            Log.e(TAG, "isAppStatusOn: OFF");
            return false;
        }
        return true;
    }

    class RetrieveConfigFile extends AsyncTask<String, Void, Result> {

        @Override
        protected Result doInBackground(String... urls) {
            String version = "0.1.0" ;
            if (BuildConfig.DEBUG) {
                //Hardcoding the version for Debug builds
                version = "1.3.6" ;
                Log.d(TAG, "Using config file for version :  " + version);
            }else{
                PackageManager manager = MBApp.getContext().getPackageManager();
                PackageInfo info;
                try {
                    info = manager.getPackageInfo(MBApp.getContext().getPackageName(), 0);
                    version = info.versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, e.toString());
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
                    editor.putString(RC_ETAG, eTag).apply();
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

                } else if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED){
                    Log.d(TAG, "Content not modified");
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return null;
        }

        private void readAppStatus(JSONObject reader, SharedPreferences.Editor editor)
        {
            JSONObject status;
            try {
                status = reader.getJSONObject("status");
                String appStatus = status.getString("appStatus");
                editor.putString(RC_APPSTATUS_KEY, appStatus).apply();
                //Get extended message if needed
                if (appStatus.equalsIgnoreCase("OFF"))
                {
                    String title = status.getString("title");
                    String message = status.getString("message");
                    editor.putString(RC_EXCEPTIONTITLE_KEY, title).apply();
                    editor.putString(RC_EXCEPTIONMSG_KEY, message).apply();
                }
            } catch (JSONException e) {
                Log.e(TAG, "readAppStatus: failed with exception " + e.toString());
            }
        }

        private void readEndPoints(JSONObject reader, SharedPreferences.Editor editor)
        {
            try {
                JSONObject endpoints  = reader.getJSONObject("endpoints");
                String about = endpoints.getString("about");
                String tou = endpoints.getString("termsOfUse");
                String pp = endpoints.getString("privacyPolicy");
                String createcode = endpoints.getString("createCode");
                String discover = endpoints.getString("discover");
                String myscripts = endpoints.getString("myScripts");

                editor.putString(RC_ENDPOINT_ABOUT , about).apply();
                editor.putString(RC_ENDPOINT_TOU, tou).apply();
                editor.putString(RC_ENDPOINT_PP, pp).apply();
                editor.putString(RC_ENDPOINT_CREATECODE , createcode).apply();
                editor.putString(RC_ENDPOINT_DISCOVER, discover).apply();
                editor.putString(RC_APPSTATUS_MYSCRIPTS, myscripts).apply();

            } catch (JSONException e) {
                Log.e(TAG, "readEndPoints: failed with exception " + e.toString());
            }
        }

        private void readConfig(JSONObject reader, SharedPreferences.Editor editor)
        {
            try {
                JSONObject config  = reader.getJSONObject("config");
                String email = config.getString("feedbackEmailAddress");
                editor.putString(RC_CONFIG_EMAIL, email).apply();
            } catch (JSONException e) {
                Log.e(TAG, "readConfig: failed with exception " + e.toString());
            }
        }
        private void readFromJsonAndStore(String jsonFile) {
            try {
                SharedPreferences.Editor editor = mPreferences.edit();
                JSONObject reader = new JSONObject(jsonFile);
                //Read the application status
                readAppStatus(reader, editor);
                //Read end points
                readEndPoints(reader, editor);
                //Read end points
                readConfig(reader, editor);
            } catch (JSONException e) {
                Log.e(TAG, "readFromJsonAndStore: failed with exception " + e.toString());
            }
        }
        protected void onPostExecute(Result result) {
            getStoredValues(MBApp.getContext());
        }
    }
}