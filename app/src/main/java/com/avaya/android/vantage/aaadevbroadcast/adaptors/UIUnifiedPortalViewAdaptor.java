package com.avaya.android.vantage.aaadevbroadcast.adaptors;

import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.csdk.UnifiedPortalAdaptorListener;
import com.avaya.android.vantage.aaadevbroadcast.views.interfaces.IUnifiedPortalViewInterface;

public class UIUnifiedPortalViewAdaptor implements UnifiedPortalAdaptorListener {

    IUnifiedPortalViewInterface mUnifiedPortalViewInterface;

    public void setDeviceViewInterface(IUnifiedPortalViewInterface viewInterface) {
        this.mUnifiedPortalViewInterface = viewInterface;
    }

    @Override
    public void handleJoinMeetingError(int error) {
        if (mUnifiedPortalViewInterface != null)
            mUnifiedPortalViewInterface.handleJoinMeetingError(error);
    }

    @Override
    public void handleJoinMeetingSuccess() {
        if (mUnifiedPortalViewInterface != null)
            mUnifiedPortalViewInterface.handleJoinMeetingSuccess();
    }

    public void requestToJoinMeeting(String serverAddress, String conferenceId, String meetMeUserName, boolean isPresentationOnlyMode, String callBackNumber, String oneTimePin){
        SDKManager.getInstance().getUnifiedPortalAdaptor().requestToJoinMeeting(serverAddress, conferenceId, meetMeUserName, isPresentationOnlyMode, callBackNumber, oneTimePin);
    }
}
