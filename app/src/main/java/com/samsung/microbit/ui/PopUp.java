package com.samsung.microbit.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import com.samsung.microbit.ui.activity.PopUpActivity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Semaphore;

/*
How to use:

To show a popup from an application/activity context, call the below.
To show a popup from a Plugin class or service context, use showFromService function.

PopUp.show(context,
        "Accept Audio Recording?\nClick Yes to allow", //message
        "Privacy", //title
        R.drawable.recording, //image icon res id (pass 0 to use default icon)
		0, //image icon background res id (pass 0 if there is no background)
        PopUp.TYPE_CHOICE, //type of popup.
        new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
                //Write your own code
            }
        },//override click listener for ok button
        null);//pass null to use default listener
 */
public class PopUp {

    static public final int TYPE_CHOICE = 0;//2 buttons type
    static public final int TYPE_ALERT = 1;//1 button type
    static public final int TYPE_PROGRESS = 2;//0 button progress bar type
	static public final int TYPE_NOBUTTON = 3;//No button type
    static public final int TYPE_SPINNER = 4;//0 button type spinner
    static public final int TYPE_INPUTTEXT = 5;//2 buttons type + edittext //TODO:deprecated
    static public final int TYPE_PROGRESS_NOT_CANCELABLE = 6;//0 button progress bar type not cancelable (backpress disabled)
    static public final int TYPE_SPINNER_NOT_CANCELABLE = 7;//0 button type spinner not cancelable (backpress disabled)
    static public final int TYPE_MAX = 8;

    //constants that indicate the type of request for which each type involves specific handling
    //see processNextPendingRequest
    static private final short REQUEST_TYPE_SHOW = 0;
    static private final short REQUEST_TYPE_HIDE = 1;
    static private final short REQUEST_TYPE_UPDATE_PROGRESS = 2;
    static private final short REQUEST_TYPE_MAX = 3;

    static private int current_type = TYPE_MAX;//current type of displayed popup  (TYPE_CHOICE, ...)

    static private Context ctx = null;

    static private boolean is_current_request_pending = false;

    static class PendingRequest {
        public Intent intent;
        public View.OnClickListener okListener;
        public View.OnClickListener cancelListener;
        public short type;//type of request (REQUEST_TYPE_SHOW, ...)

        public PendingRequest(Intent intent, View.OnClickListener okListener,
                              View.OnClickListener cancelListener,
                              short type) {
            this.intent = intent;
            this.okListener = okListener != null ? okListener : defaultPressListener;
            this.cancelListener = cancelListener != null ? cancelListener : defaultPressListener;
            this.type = type;
        }
    }
    //FIFO queue for pending requests
    //This queue is a solution to handle the asynchronous behaviour
    // of Activity startActivity/onCreate
    static private Deque<PendingRequest> pendingQueue = new ArrayDeque<>();

    static private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PopUpActivity.INTENT_ACTION_OK_PRESSED)) {
                if (okPressListener != null)
                    okPressListener.onClick(null);
            } else if (intent.getAction().equals(PopUpActivity.INTENT_ACTION_CANCEL_PRESSED)) {
                if (cancelPressListener != null)
                    cancelPressListener.onClick(null);
            } else if (intent.getAction().equals(PopUpActivity.INTENT_ACTION_DESTROYED)) {
                Log.d("PopUp", "INTENT_ACTION_DESTROYED size queue = " + pendingQueue.size());
                pendingQueue.poll();
                is_current_request_pending = false;
                current_type = TYPE_MAX;
                processNextPendingRequest();
            } else if (intent.getAction().equals(PopUpActivity.INTENT_ACTION_CREATED)) {
                Log.d("PopUp", "INTENT_ACTION_CREATED size queue = " + pendingQueue.size());
                PendingRequest request = pendingQueue.poll();
                current_type = request.intent.getIntExtra(PopUpActivity.INTENT_EXTRA_TYPE, 0);
                okPressListener = request.okListener;
                cancelPressListener = request.cancelListener;
                is_current_request_pending = false;
                processNextPendingRequest();
            }
        }
    };

    static private boolean registered = false;
    static private View.OnClickListener okPressListener = null;
    static private View.OnClickListener cancelPressListener = null;
    static private View.OnClickListener defaultPressListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            PopUp.hide();
        }
    };
    static private String inputText = "";//TODO: deprecated

    public static void hide()
    {
        Log.d("PopUp", "hide START");
        pendingQueue.add(new PendingRequest(new Intent(PopUpActivity.INTENT_ACTION_CLOSE),
                null, null, REQUEST_TYPE_HIDE));

        if (!is_current_request_pending) {
            processNextPendingRequest();
        }
    }

    public static void updateProgressBar(int val) {
        Intent intent = new Intent(PopUpActivity.INTENT_ACTION_UPDATE_PROGRESS);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_PROGRESS, val);

        pendingQueue.add(new PendingRequest(intent, null, null, REQUEST_TYPE_UPDATE_PROGRESS));
        if (!is_current_request_pending) {
            processNextPendingRequest();
        }
    }

    //TODO: deprecated
    public static String getInputText() {
        return inputText;
    }
    //TODO: deprecated
    public static void setInputText(String text) {
        inputText = text;
    }

    //Interface function for showing a popup inside a service plugin class
    //only supports TYPE_ALERT popup for now.
    public static void showFromService(Context context, String message, String title,
                                       int imageResId, int imageBackgroundResId, int type) {
        Log.d("PopUp", "showFromService");
        Intent intent = new Intent("com.samsung.microbit.core.SHOWFROMSERVICE");
        putIntentExtra(intent, message, title, imageResId, imageBackgroundResId, type);
        context.sendBroadcast(intent);
    }

    private static void putIntentExtra(Intent intent, String message, String title,
                                        int imageResId, int imageBackgroundResId, int type) {
        intent.putExtra(PopUpActivity.INTENT_EXTRA_TYPE, type);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_TITLE, title);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_MESSAGE, message);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_ICON, imageResId);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_ICONBG, imageBackgroundResId);
        switch (type) {
            case TYPE_PROGRESS_NOT_CANCELABLE:
            case TYPE_SPINNER_NOT_CANCELABLE:
                intent.putExtra(PopUpActivity.INTENT_EXTRA_CANCELABLE, false);
                break;
            default:
                intent.putExtra(PopUpActivity.INTENT_EXTRA_CANCELABLE, true);
        }
    }

    static private class PopUpTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private String message;
        private String title;
        private int imageResId;
        private int imageBackgroundResId;
        private int type;
        private View.OnClickListener okListener;
        private View.OnClickListener cancelListener;

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("PopUpTask", "doInBackground");
            PopUp.showInternal(context, message, title, imageResId, imageBackgroundResId, type,
                    okListener, cancelListener);
            return null;
        }

        public PopUpTask(Context context, String message, String title,
                              int imageResId, int imageBackgroundResId, int type,
                              View.OnClickListener okListener, View.OnClickListener cancelListener){
            this.context = context;
            this.message = message;
            this.title = title;
            this.imageResId = imageResId;
            this.imageBackgroundResId = imageBackgroundResId;
            this.type = type;
            this.okListener = okListener;
            this.cancelListener = cancelListener;
        }
    }

    //Interface function for showing popup inside an application activity
    //pass 0 to imageResId to use default icon
    //pass 0 to imageBackgroundResId if no background needed for icon
    public static boolean show(Context context, String message, String title,
                            int imageResId, int imageBackgroundResId, int type,
                            View.OnClickListener okListener, View.OnClickListener cancelListener) {
        new PopUpTask(context, message, title, imageResId, imageBackgroundResId, type,
                okListener, cancelListener).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        return true;
    }

    private static synchronized boolean showInternal(Context context, String message, String title,
                            int imageResId, int imageBackgroundResId, int type,
                            View.OnClickListener okListener, View.OnClickListener cancelListener)
    {
        Log.d("PopUp", "show START popup type " + type);
        if (!(context instanceof Activity)) {
            //TODO: throw exception?
            Log.e("PopUp","Cannot show popup because context is not an activity. PopUp.show must be called from an activity");
            return false;
        }
        ctx = context;

        if (registered == false) {

            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, new IntentFilter(PopUpActivity.INTENT_ACTION_OK_PRESSED));
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, new IntentFilter(PopUpActivity.INTENT_ACTION_CANCEL_PRESSED));
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, new IntentFilter(PopUpActivity.INTENT_ACTION_DESTROYED));
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, new IntentFilter(PopUpActivity.INTENT_ACTION_CREATED));
            registered = true;
        }

        Intent intent = new Intent(context, PopUpActivity.class);
        putIntentExtra(intent, message, title, imageResId, imageBackgroundResId, type);

        pendingQueue.add(new PendingRequest(intent, okListener, cancelListener, REQUEST_TYPE_SHOW));

        if (!is_current_request_pending) {
            processNextPendingRequest();
        }

        return true;
    }

    private static void processNextPendingRequest()
    {
        Log.d("PopUp", "processNextPendingRequest START size = " + pendingQueue.size());
        boolean done = false;
        //keep iterating until we find async. request (HIDE, SHOW) to process
        while (!pendingQueue.isEmpty() && !done) {
            PendingRequest request = pendingQueue.peek();

            switch (request.type) {
                case REQUEST_TYPE_SHOW: {
                    Log.d("PopUp", "processNextPendingRequest REQUEST_TYPE_SHOW");
                    if (current_type != TYPE_MAX) {
                        Log.d("Popup", "processNextPendingRequest Update existing layout instead of hiding");
                        request.intent.setAction(PopUpActivity.INTENT_ACTION_UPDATE_LAYOUT);
                        LocalBroadcastManager.getInstance(ctx).sendBroadcastSync(request.intent);
                        current_type = request.intent.getIntExtra(PopUpActivity.INTENT_EXTRA_TYPE, 0);
                        okPressListener = request.okListener;
                        cancelPressListener = request.cancelListener;
                        pendingQueue.poll();
                        continue;
                    }

                    done = true;
                    is_current_request_pending = true;
                    request.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(request.intent);
                    break;
                }
                case REQUEST_TYPE_HIDE: {
                    if (current_type == TYPE_MAX) {
                        Log.d("Popup", "processNextPendingRequest Nothing to hide");
                        pendingQueue.poll();
                        continue;
                    }

                    done = true;
                    is_current_request_pending = true;
                    LocalBroadcastManager.getInstance(ctx).sendBroadcastSync(request.intent);
                    break;
                }
                case REQUEST_TYPE_UPDATE_PROGRESS: {
                    LocalBroadcastManager.getInstance(ctx).sendBroadcastSync(request.intent);
                    pendingQueue.poll();
                    break;
                }
            }
        }
        Log.d("PopUp", "processNextPendingRequest END size = " + pendingQueue.size());
    }
}