package com.avaya.android.vantage.aaadevbroadcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.notifications.CallNotificationFactory;

import java.util.Objects;

import static com.avaya.android.vantage.aaadevbroadcast.Constants.ACTION_USB_ATTACHED;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.ACTION_USB_DETACHED;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.KEY_SERVICE_REASON_EXTRA;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.KEY_SERVICE_RETRY_EXTRA;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.KEY_SERVICE_STATUS_EXTRA;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.KEY_SERVICE_TYPE_EXTRA;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.REFRESH_HISTORY_ICON;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.SERVICE_STATE_CHANGE;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.SUCCESS_STATUS;
import static com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager.AUTHENTICATION_ERROR;
import static com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager.CERTIFICATE_ERROR;
import static com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager.CONNECTION_ERROR;
import static com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager.DOMAIN_ERROR;
import static com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager.GENERAL_ERROR;
import static com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager.MAX_REGISTRATIONS_EXCEEDED_ERROR;
import static com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager.SERVER_ERROR;
import static com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager.UL_MISSING_CREDENTIALS_ERROR;

/**
 * Config receiver which inform {@link BaseActivity} and rest of application of changes in configuration
 * of device
 */
public class ConfigReceiver extends BroadcastReceiver {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private final static String DB_INIT_COMPLETE = "com.avaya.endpoint.intent.action.configurations.init.complete";
    private final static String LOGIN_STATE_CHANGED = "android.intent.action.LOGIN_STATE_CHANGED";
    private final static int REGISTERING = 1; // taken from Brio's LoginState.java
    private final static int UNREGISTERING = 0; // taken from Brio's LoginState.java

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(LOG_TAG, "ConfigReceiver::onReceive" + ((intent==null) ? "" : intent.getAction()));

        assert intent != null;
        if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase(DB_INIT_COMPLETE)) {
            Log.d(LOG_TAG, "ConfigReceiver::onReceive DB_INIT_COMPLETE received");
            Log.d(LOG_TAG, "Starting ELAN");
            Intent i = new Intent(context, ElanApplication.getDeviceFactory().getMainActivityClass());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(Constants.CONFIG_RECEIVER, true);
            context.startActivity(i);
        }

        if (intent.getAction().equalsIgnoreCase(LOGIN_STATE_CHANGED)) {
            // only if we are now start the registering stage, pop-up the BaseActivity
            int state = intent.getIntExtra("loginstate", REGISTERING);
            Log.d(LOG_TAG, "ConfigReceiver::onReceive LOGIN_STATE_CHANGED received state = " + state);
            if (state == REGISTERING || state == UNREGISTERING) {
                Log.d(LOG_TAG, "Starting ELAN");
                Intent i = new Intent(context, ElanApplication.getDeviceFactory().getMainActivityClass());
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(Constants.CONFIG_RECEIVER, true);
                context.startActivity(i);
            }

            if (state == UNREGISTERING) {
                CallNotificationFactory.getInstance(ElanApplication.getContext())
                        .removeAll();
            }

            saveLoggedInUserState(context, state);
        }

        if (intent.getAction().equalsIgnoreCase(SERVICE_STATE_CHANGE)) {
            //To simulate sussesful login by CSDK application use below intent:
            // adb shell am broadcast -a com.avaya.endpoint.SERVICE_STATE_CHANGE --es status "SUCCESS" --es serviceType "SIP"
            String serviceType = intent.getStringExtra(KEY_SERVICE_TYPE_EXTRA);
            String status = intent.getStringExtra(KEY_SERVICE_STATUS_EXTRA);
            String reason = intent.getStringExtra(KEY_SERVICE_REASON_EXTRA);
            boolean retry = intent.getBooleanExtra(KEY_SERVICE_RETRY_EXTRA, false);
            Log.i(LOG_TAG, "Received REGISTRATION_STATE_CHANGE:serviceType=" + serviceType
                    + ":status=" + status
                    + ":reason=" + reason
                    + ":retry=" + retry);

            if (!SUCCESS_STATUS.equals(status)) {
                handleLoginError(context, serviceType, reason);
            } else {
                ErrorManager.getInstance().removeAllErrors();
                Intent configChanged = new Intent(Constants.LOCAL_CONFIG_CHANGE);
                configChanged.putExtra(Constants.CONFIG_CHANGE_STATUS, true);
                LocalBroadcastManager.getInstance(context).sendBroadcast(configChanged);
            }
        }

        if (intent.getAction().equalsIgnoreCase(REFRESH_HISTORY_ICON)) {
            int numberOfMissedCalls = intent.getIntExtra(Constants.EXTRA_UNSEEN_CALLS, 0);
            Intent intentMainActivity = new Intent(context, ElanApplication.getDeviceFactory().getMainActivityClass());
            intentMainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intentMainActivity.putExtra(Constants.EXTRA_UNSEEN_CALLS, numberOfMissedCalls);
            intentMainActivity.setAction(BaseActivity.NON_SERVICE_IMPACTING_CHANGE);
            context.startActivity(intentMainActivity);
        }

        else if (intent.getAction().equalsIgnoreCase(ACTION_USB_ATTACHED)){
            Log.d(LOG_TAG, "USB attached");
            //check if the usb device attached is camera
            if ((((UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)).getDeviceClass() & UsbConstants.USB_CLASS_VIDEO) != 0){
                //usb device is camera - so video button should be visible
                SDKManager.getInstance().setCameraSupported(true);

                if(ElanApplication.isMainActivityVisible()){
                    Intent intentMainActivity = new Intent(context, ElanApplication.getDeviceFactory().getMainActivityClass());
                    intentMainActivity.setAction(BaseActivity.ACTION_CAMERA_ATTACHED);
                    context.startActivity(intentMainActivity);
                }
            }
        }
        else if (intent.getAction().equalsIgnoreCase(ACTION_USB_DETACHED)){
            Log.d(LOG_TAG, "USB detached");
            //check if the usb device detached is camera
            if (intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) != null && (((UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)).getDeviceClass() & UsbConstants.USB_CLASS_VIDEO) != 0){
                //check if we have internal camera
                if (Utils.getDeviceCameraSupport() == false) {
                    //we don't have any camera (not usb and not internal) - so video button should not be visible
                    SDKManager.getInstance().setCameraSupported(false);
                    if(ElanApplication.isMainActivityVisible()){
                        Intent intentMainActivity = new Intent(context, ElanApplication.getDeviceFactory().getMainActivityClass());
                        intentMainActivity.setAction(BaseActivity.ACTION_CAMERA_DETACHED);
                        context.startActivity(intentMainActivity);
                    }
                }
            }
        }
    }

    /**
     * Handle errors received from SIP or AADS.
     * Sends Local Broadcast to {@link BaseActivity} if its user visible.
     *
     * @param context     Used to send Local Broadcast.
     * @param serviceType Type of service (SIP, UL, AADS).
     * @param reason      Error type.
     */
    private void handleLoginError(Context context, String serviceType, String reason) {
        // For testing use following adb command
        // adb shell am broadcast -a com.avaya.endpoint.SERVICE_STATE_CHANGE --es status "FAIL" --es serviceType "UL" --es reason "AUTHENTICATION_ERROR"

        ErrorManager errorManager = ErrorManager.getInstance();
        if (TextUtils.equals("UL", serviceType)) {
            if (TextUtils.equals("MISSING_CREDENTIALS", reason)) {
                errorManager.addErrorToList(UL_MISSING_CREDENTIALS_ERROR);
            }
        } else if (TextUtils.equals("SIP", serviceType)) {
            if (TextUtils.equals("AUTHENTICATION_ERROR", reason)) {
                errorManager.addErrorToList(AUTHENTICATION_ERROR);
            } else if (TextUtils.equals("GENERAL_ERROR", reason)
                    || TextUtils.equals("UNDEFINED", reason)
                    || TextUtils.equals("INVALID_STATE_ERROR", reason)
                    || TextUtils.equals("SUBSCRIPTION_ERROR", reason)
                    || TextUtils.equals("REDIRECTED_ERROR", reason)) {
                errorManager.addErrorToList(GENERAL_ERROR);
            } else if (TextUtils.equals("DOMAIN_ERROR", reason)
                    || TextUtils.equals("INVALID_SIP_DOMAIN", reason)) {
                errorManager.addErrorToList(DOMAIN_ERROR);
            } else if (TextUtils.equals("CONNECTION_ERROR", reason)) {
                errorManager.addErrorToList(CONNECTION_ERROR);
            } else if (TextUtils.equals("SERVER_ERROR", reason)) {
                errorManager.addErrorToList(SERVER_ERROR);
            } else if (TextUtils.equals("UNRECOGNIZED_SERVER_NAME", reason)
                    || TextUtils.equals("SERVER_UNTRUSTED_ERROR", reason)
                    || TextUtils.equals("SSL_FATAL_ALERT", reason)
                    || TextUtils.equals("INVALID_SERVER_IDENTITY", reason)
                    || TextUtils.equals("SERVER_CERTIFICATE_CHAIN_REVOKED", reason)) {
                errorManager.addErrorToList(CERTIFICATE_ERROR);
            } else if (TextUtils.equals("MAX_REGISTRATIONS_EXCEEDED_ERROR", reason)) {
                errorManager.addErrorToList(MAX_REGISTRATIONS_EXCEEDED_ERROR);
            }
        }

        Intent configChanged = new Intent(Constants.LOCAL_CONFIG_CHANGE);
        configChanged.putExtra(Constants.CONFIG_CHANGE_STATUS, false);
        LocalBroadcastManager.getInstance(context).sendBroadcast(configChanged);
    }

    /**
     * We should store user login state.
     * This value we use when we sync call logs data.
     *
     * @param context
     * @param state
     */
    private void saveLoggedInUserState(Context context, int state) {
        SharedPreferences firstTimeLogginPreference = context.getSharedPreferences(Constants.FIRST_TIME_LOGGIN, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = firstTimeLogginPreference.edit();
        if (state == REGISTERING) {
            editor.putBoolean(Constants.KEY_CHECK_NEW_CALL, true);
        } else if (state == UNREGISTERING) {
            editor.putBoolean(Constants.KEY_CHECK_NEW_CALL, false);
        }
        editor.apply();
    }
}
