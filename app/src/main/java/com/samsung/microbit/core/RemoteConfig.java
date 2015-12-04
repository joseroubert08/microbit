package com.samsung.microbit.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.samsung.microbit.MBApp;

import java.net.URL;

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
    private String m_LastQueryTimeString = null ;
    private String m_maxAge = null ;



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

    public void init()
    {
        Context context = MBApp.getContext();
        if (context == null){
            Log.d("RemoteConfig", "Context is null. cannot get the config");
            return;
        }
        if (!getStoredValues(context))
        {
            Log.e("RemoteConfig", "Could not read from shared preference");
            return;
        }

        if (shouldRefreshValue())
        {

        }
        //remote_config_url
        //Check if it is time to update using Cache-Control: max-age

    }

    private boolean getStoredValues(Context context)
    {
        //Get values previously stored in the sharedpreference
        mPreferences = context.getSharedPreferences(REMOTE_CONFIG_PREFERENCE_KEY, Context.MODE_PRIVATE);
        if (mPreferences == null){
            Log.d("RemoteConfig", "SharedPreferences is null. cannot get the config");
            return false;
        }
        m_AppStatus = mPreferences.getString(RC_ENDPOINT_ABOUT,"Off");
        m_aboutURL = mPreferences.getString(RC_APPSTATUS_KEY,blankValue);
        m_TOUURL = mPreferences.getString(RC_ENDPOINT_TOU,blankValue);
        m_PPURL = mPreferences.getString(RC_ENDPOINT_PP,blankValue);
        m_sendToEmail = mPreferences.getString(RC_CONFIG_EMAIL,blankValue);
        m_Etag = mPreferences.getString(RC_ETAG,blankValue);
        m_LastQueryTimeString = mPreferences.getString(RC_LAST_QUERY,blankValue);
        m_maxAge = mPreferences.getString(RC_LAST_QUERY,blankValue);

        return true;
    }

    private boolean shouldRefreshValue()
    {

        return true;
    }
}