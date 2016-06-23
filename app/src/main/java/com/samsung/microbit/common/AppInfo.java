package com.samsung.microbit.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.samsung.microbit.R;

import java.net.URL;

import static com.samsung.microbit.ConfigPreferenceNames.RC_APPSTATUS_KEY;
import static com.samsung.microbit.ConfigPreferenceNames.RC_APPSTATUS_MYSCRIPTS;
import static com.samsung.microbit.ConfigPreferenceNames.RC_CONFIG_EMAIL;
import static com.samsung.microbit.ConfigPreferenceNames.RC_ENDPOINT_ABOUT;
import static com.samsung.microbit.ConfigPreferenceNames.RC_ENDPOINT_CREATECODE;
import static com.samsung.microbit.ConfigPreferenceNames.RC_ENDPOINT_DISCOVER;
import static com.samsung.microbit.ConfigPreferenceNames.RC_ENDPOINT_PP;
import static com.samsung.microbit.ConfigPreferenceNames.RC_ENDPOINT_TOU;
import static com.samsung.microbit.ConfigPreferenceNames.RC_ETAG;
import static com.samsung.microbit.ConfigPreferenceNames.RC_EXCEPTIONMSG_KEY;
import static com.samsung.microbit.ConfigPreferenceNames.RC_EXCEPTIONTITLE_KEY;
import static com.samsung.microbit.ConfigPreferenceNames.RC_LAST_QUERY;
import static com.samsung.microbit.ConfigPreferenceNames.REMOTE_CONFIG_PREFERENCE_KEY;

public class AppInfo {
    public enum AppStatus {
        ON("On"), OFF("Off");

        private String value;

        AppStatus(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static AppStatus byName(String name) {
            for (AppStatus appStatus : values()) {
                if(appStatus.toString().equals(name)) {
                    return appStatus;
                }
            }
            return null;
        }
    }

    private URL configLocation;
    private long updateTime;

    private Context appContext;
    private SharedPreferences preferences;

    private AppStatus appStatus;
    private String exceptionTitle;
    private String exceptionMsg;
    private String aboutURL;
    private String createCodeURL;
    private String discoverURL;
    private String myScriptsURL;
    private String TOUURL;
    private String PPURL ;
    private String sendToEmail;

    private String Etag;
    private String lastModifiedString;
    private long LastQueryTime;
    private long maxAge;

    public AppInfo(Context appContext) {
        this.appContext = appContext;
        this.preferences = appContext.getSharedPreferences(REMOTE_CONFIG_PREFERENCE_KEY, Context.MODE_PRIVATE);

        searchStoredValues();
    }

    public void searchStoredValues() {
        appStatus = AppStatus.byName(preferences.getString(RC_APPSTATUS_KEY, AppStatus.OFF.toString()));
        aboutURL = preferences.getString(RC_ENDPOINT_ABOUT, "");
        TOUURL = preferences.getString(RC_ENDPOINT_TOU, "");
        PPURL = preferences.getString(RC_ENDPOINT_PP, "");
        sendToEmail = preferences.getString(RC_CONFIG_EMAIL, "");
        Etag = preferences.getString(RC_ETAG, "");
        LastQueryTime = preferences.getLong(RC_LAST_QUERY, 0);
        maxAge = preferences.getLong(RC_LAST_QUERY, 0);

        exceptionTitle = preferences.getString(RC_EXCEPTIONTITLE_KEY, "");
        exceptionMsg = preferences.getString(RC_EXCEPTIONMSG_KEY, "");
        createCodeURL = preferences.getString(RC_ENDPOINT_CREATECODE, "");
        discoverURL = preferences.getString(RC_ENDPOINT_DISCOVER, "");
        myScriptsURL = preferences.getString(RC_APPSTATUS_MYSCRIPTS, "");
    }

    ///Getters
    public AppStatus getAppStatus() {
        return appStatus;
    }

    public String getAboutURL() {
        if (aboutURL.isEmpty()) {
            aboutURL = appContext.getString(R.string.about_url);
        }
        return aboutURL;
    }

    public String getTermsOfUseURL() {
        if (TOUURL.isEmpty()) {
            TOUURL = appContext.getString(R.string.terms_of_use_url);
        }
        return TOUURL;
    }

    public String getPrivacyURL() {
        if (PPURL.isEmpty()) {
            PPURL = appContext.getString(R.string.privacy_policy_url);
        }
        return PPURL;
    }

    public String getSendEmailAddress() {
        if (sendToEmail.isEmpty()) {
            sendToEmail = appContext.getString(R.string.feedback_email_address);
        }
        return sendToEmail;
    }

    public String getExceptionTitle() {
        if (exceptionTitle.isEmpty()) {
            exceptionTitle = appContext.getString(R.string.general_error_title);
        }
        return exceptionTitle;
    }

    public String getExceptionMsg() {
        if (exceptionMsg.isEmpty()) {
            exceptionMsg = appContext.getString(R.string.general_error_msg);
        }
        return exceptionMsg;
    }

    public String getCreateCodeURL() {
        if (createCodeURL.isEmpty()) {
            createCodeURL = appContext.getString(R.string.createCodeURL);
        }
        return createCodeURL;
    }

    public String getDiscoverURL() {
        if (discoverURL.isEmpty()) {
            discoverURL = appContext.getString(R.string.discoverURL);
        }
        return discoverURL;
    }

    public String getMyScriptsURL() {
        if (myScriptsURL.isEmpty()) {
            myScriptsURL = appContext.getString(R.string.myScriptsURL);
        }
        return myScriptsURL;
    }

    public boolean isAppStatusOn() {
        searchStoredValues();
        appStatus = AppStatus.byName(preferences.getString(RC_APPSTATUS_KEY, AppStatus.ON.toString()));

        if (appStatus == AppStatus.OFF) {
            Log.e("RemoteConfig", "isAppStatusOn: OFF");
            return false;
        }
        return true;
    }

    public String getEtag() {
        return Etag;
    }

    public long getLastQueryTime() {
        return LastQueryTime;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public String getLastModifiedString() {
        return lastModifiedString;
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }
}
