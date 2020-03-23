package com.avaya.android.vantage.aaadevbroadcast.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.model.UICall;
import com.avaya.android.vantage.aaadevbroadcast.model.UICallState;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import static com.avaya.android.vantage.aaadevbroadcast.csdk.CallAdaptor.MAX_NUM_CALLS;
import static com.avaya.android.vantage.aaadevbroadcast.notifications.NotificationRaiser.IDLE_TAG;
import static com.avaya.android.vantage.aaadevbroadcast.notifications.NotificationRaiser.MISSED_CALLS_NOTIFICATION_ID;

/**
 * Class used to create call
 */

public class CallNotificationFactory {

    private static WeakReference<CallNotificationFactory> sInstance;

    private Notification.Builder mStatusNotificationBuilder;
    private Notification.Builder mMissedCallsNotificationBuilder;
    private final String TAG = CallNotificationFactory.class.getSimpleName();
    private final Context mContext;

    private final NotificationRaiser mNotificationRaiser;
    private final HashMap<Integer, Integer> callMap = new HashMap<>(MAX_NUM_CALLS);
    private String mStatusChannelId;
    private String mChannelId;

    public static synchronized CallNotificationFactory getInstance(Context activityContext) {
        if (sInstance == null) {
            sInstance = new WeakReference<>(new CallNotificationFactory(activityContext));
        }
        return sInstance.get();
    }

    private CallNotificationFactory(Context context) {
        mContext = context;

        mNotificationRaiser = new NotificationRaiser(context.getApplicationContext());
        mNotificationRaiser.bindNotificationService();

        mStatusChannelId = createNotificationChannel(context, "Status");
        mChannelId = createNotificationChannel(context, "");
        mStatusNotificationBuilder = new Notification.Builder(context, mStatusChannelId);
        mStatusNotificationBuilder
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentTitle(context.getString(R.string.app_name))
                .setCategory(Notification.CATEGORY_CALL)
                .setLargeIcon(Icon.createWithResource(context, R.mipmap.ic_launcher));

    }

    private String createNotificationChannel(Context context, String suffix) {
        String channelId = "VantageConnectChannelId"+suffix;
        String channelName = "Vantage Connect"+" "+suffix;
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW);
        chan.setShowBadge(false);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = context.getSystemService(NotificationManager.class);
        service.createNotificationChannel(chan);
        return channelId;
    }

    /**
     * Showing the call
     *
     * @param call call to show
     */
    public void show(UICall call) {
        Intent actionIntent = new Intent(mContext, ElanApplication.getDeviceFactory().getMainActivityClass());
        actionIntent.putExtra(Constants.IS_VIDEO, false);
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        actionIntent.setAction(BaseActivity.SHOW_CALL_INTENT);
        actionIntent.putExtra(Constants.CALL_ID, call.getCallId());

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 7, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final String remoteDisplayName = Utils.getContactName(call.getRemoteNumber(), call.getRemoteDisplayName(),  SDKManager.getInstance().getCallAdaptor().isCMConferenceCall(call.getCallId()),
                                                                SDKManager.getInstance().getCallAdaptor().isConferenceCall(call.getCallId()));
        mStatusNotificationBuilder.setContentText(remoteDisplayName)
                .setSubText("")
                .setShowWhen(true)
                .setContentIntent(pendingIntent)
                .setWhen(call.getStateStartTime())
                .setSmallIcon(call.getState() == UICallState.HELD ? R.drawable.ic_pause_white_24dp : R.drawable.ic_call_white_24dp)
                .setUsesChronometer(true);

        // get notification id associated with the call
        int notificationId = getNotificationIdForCall(call.getCallId());
        mNotificationRaiser.raiseNotification(notificationId, mStatusNotificationBuilder.build());
    }

    /**
     * Since the notification service is foreground, at least one notification shall be always shown
     * This function shows this notification: it is either "on line" message (if no ongoing calls exists)
     * or theninfo about ongoing calls.
     * @param text - the text to be shown
     */
    public void showOnLine(String text) {

        Intent actionIntent = new Intent(mContext, ElanApplication.getDeviceFactory().getMainActivityClass());
        actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 7, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mStatusNotificationBuilder.setOngoing(true)
                .setContentTitle(mContext.getString(R.string.app_name))
                .setContentText(text)
                .setContentInfo("")
                .setShowWhen(false)
                .setContentIntent(pendingIntent)
                .setLargeIcon(Icon.createWithResource(mContext, R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.ic_check)
                .setUsesChronometer(false);

        mNotificationRaiser.raiseNotification(IDLE_TAG, mStatusNotificationBuilder.build());
    }

    /**
     * Removing specific call
     *
     * @param callId call ID
     */
    public void remove(int callId) {

        // get the id of the call's notification
        Integer notifId = callMap.get(callId);
        // shall never be null, but just to protect
        if (notifId == null)
            return;

        // if this is last call - go to idle
        if (callMap.size() == 1) {
            // just replace the content of the call's notification with idle "online" message
            String sipusername = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getCredential(ConfigParametersNames.SIPUSERNAME);
            if(mContext != null) {
                showOnLine("anonymous".equals(sipusername) ? mContext.getString(R.string.logged_out) : mContext.getString(R.string.notification_online));
            }
        } else {
            // this is not the last call

            // if the call's notification appears under the IDLE id,
            if (notifId == IDLE_TAG) {
                notifId = duplicateOtherSlot(callId);
            }
            mNotificationRaiser.cancelNotification(notifId);
        }

        callMap.remove(callId);
    }

    /**
     * Removing all calls
     */
    public void removeAll() {
        mNotificationRaiser.cancelNotification();
    }

    /**
     * Copy the notification of the slot that does not match the given call to IDLE slot
     * @param callId
     * @return the notification id of other slot
     */
    private int duplicateOtherSlot(int callId) {

        // first find the id of notification that is not under IDLE slot
        int otherNotifId = -1;
        for( int value : callMap.values())
        {
            if (value != IDLE_TAG) {
                otherNotifId = value;
                break;
            }
        }

        if(otherNotifId==-1) {
            Log.e(TAG, "Can not find other slot for callId " + callId);
            return -1;
        }

        // copy content of other notification to be under IDLE_TAG
        mNotificationRaiser.copy(otherNotifId, IDLE_TAG);

        // find the Id of the call for which the notification now appears under IDLE slot
        int otherCallId = -1;
        for (int key : callMap.keySet())
        {
            if (callMap.get(key) == otherNotifId) {
                otherCallId = key;
                break;
            }
        }

        if(otherCallId==-1) {
            Log.e(TAG, "Can not find other call for callId " + callId);
            return -1;
        }

        // replace the two calls in the map
        callMap.put(callId, otherNotifId);
        callMap.put(otherCallId, IDLE_TAG);

        return otherNotifId;
    }

    /**
     * CCreates unique notification identifier that refers to the Call.
     * @param callId call identifier
     * @return Notification identifier
     */
    private int getNotificationIdForCall( int callId) {

        // by default the notification id is identical to the call's id
        int notificationId = callId;

        // there are special cases in which notification id differs from call's id
        if (callMap.get(callId) != null) {
            // if there is some ongoing notification for this call, there is already notification id associated with this call
            notificationId = callMap.get(callId);
        } else if (callMap.isEmpty()) {
            // if there no ongoing notifications for any call, the notification for this call shall replace "online" message
            // at the IDLE slot
            notificationId = IDLE_TAG;
        }

        // store the notification id associated with the specified call
        callMap.put(callId, notificationId);

        return notificationId;
    }

    /**
     * Builds and posts the missed calls notification.
     *
     * @param missedCallsNotification number of the missed calls
     */
    //ELAN-1000
    public void showMissedCallsNotification(int missedCallsNotification) {


        Intent actionIntent = new Intent(mContext, ElanApplication.getDeviceFactory().getMainActivityClass());
        actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        actionIntent.setAction(Intent.ACTION_VIEW);
        actionIntent.putExtra(Constants.GO_TO_FRAGMENT,Constants.GO_TO_FRAGMENT_MISSED_CALLS);
        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, uniqueInt, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mMissedCallsNotificationBuilder = new Notification.Builder(mContext, mChannelId);
        mMissedCallsNotificationBuilder
                .setContentTitle(mContext.getText(R.string.recent_call_missed))
                .setContentText(String.valueOf(missedCallsNotification)+" "+mContext.getText(R.string.recent_call_missed))
                .setSubText("")
                .setAutoCancel(true)
                .setOngoing(false)
                .setShowWhen(false)
                .setLargeIcon(Icon.createWithResource(mContext, R.mipmap.ic_launcher))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_recents_audio_missed);

        if(missedCallsNotification>0)
            mNotificationRaiser.raiseNotification(MISSED_CALLS_NOTIFICATION_ID, mMissedCallsNotificationBuilder.build());
    }

    public void unbindNotificationService() {
        if (mNotificationRaiser != null) mNotificationRaiser.unbindNotificationService();
    }
}
