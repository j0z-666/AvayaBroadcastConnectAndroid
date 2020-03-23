package com.avaya.android.vantage.aaadevbroadcast.model;

import java.util.Calendar;

/**
 * {@link UICall} skeleton class with encapsulating methods
 */

public class UICall {

    final private int mCallId;
    private final UICallState mCallState;
    final private String mRemoteDisplayName, mRemoteNumber, mRemoteSubject;
    private final boolean mIsVideo, mIsRemote;
    private final boolean mIsEmergency;
    private final long mEstablishedTimeMillis;
    private final long mHeldTimeMillis;

    public UICall(
            int callId,
            UICallState state,
            String remoteDisplayName,
            String remoteNumber,
            String subject,
            boolean isVideo,
            boolean isEmergency,
            boolean isRemote,
            long establishedTimeMillis,
            long heldTimeMillis) {

        mCallId = callId;
        mCallState = state;
        mRemoteDisplayName = remoteDisplayName;
        mRemoteNumber = remoteNumber;
        mRemoteSubject = subject;
        mIsVideo = isVideo;
        mIsEmergency = isEmergency;
        mEstablishedTimeMillis = establishedTimeMillis;
        mHeldTimeMillis = heldTimeMillis;
        mIsRemote=isRemote;

    }

    public boolean isVideo() {
        return mIsVideo;
    }

    public UICallState getState() {
        return mCallState;
    }

    public int getCallId() {
        return mCallId;
    }

    public String getRemoteSubject() {
        return mRemoteSubject;
    }


    public String getRemoteDisplayName() {
        return mRemoteDisplayName;
    }

    public String getRemoteNumber() {
        return mRemoteNumber;
    }

    public boolean isEmergency() {
        return mIsEmergency;
    }

    public boolean isRemote() {
        return mIsRemote;
    }

    public long getStateStartTime() {
        if (mCallState == UICallState.ESTABLISHED)
            return mEstablishedTimeMillis;
        else if (mCallState == UICallState.HELD)
            return mHeldTimeMillis;
        else if(mEstablishedTimeMillis>0 && (mEstablishedTimeMillis < Calendar.getInstance().getTimeInMillis())) {
            // sometimes established call moves into failed state if it loses connection with the SIP Proxy. The media continues and the timer shall show
            // the time from the moment the call was established
            return mEstablishedTimeMillis;
        }
        return Calendar.getInstance().getTimeInMillis();
    }

    public boolean isMissedCall() {
        return mEstablishedTimeMillis == 0;
    }
}
