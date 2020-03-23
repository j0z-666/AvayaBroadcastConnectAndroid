package com.avaya.android.vantage.aaadevbroadcast.csdk;


public interface UnifiedPortalAdaptorListener {

    void handleJoinMeetingError(int error);

    void handleJoinMeetingSuccess();
}
