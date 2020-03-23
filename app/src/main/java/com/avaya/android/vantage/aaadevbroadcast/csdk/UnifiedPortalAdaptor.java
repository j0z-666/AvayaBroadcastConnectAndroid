package com.avaya.android.vantage.aaadevbroadcast.csdk;


import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.clientservices.call.CallCreationInfo;
import com.avaya.clientservices.provider.unifiedportal.UnifiedPortalConfiguration;
import com.avaya.clientservices.unifiedportal.RequestToJoinMeetingCompletionHandler;
import com.avaya.clientservices.unifiedportal.UnifiedPortalError;
import com.avaya.clientservices.unifiedportal.UnifiedPortalMeetingInfo;
import com.avaya.clientservices.unifiedportal.UnifiedPortalService;
import com.avaya.clientservices.unifiedportal.UnifiedPortalServiceListener;

import java.lang.ref.WeakReference;

public class UnifiedPortalAdaptor implements UnifiedPortalServiceListener {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private WeakReference<UnifiedPortalAdaptorListener> mUiObj;
    private String mMeetineName=null;

    public int getCallId() {
        return mCallId;
    }

    public void setCallId(int mCallId) {
        this.mCallId = mCallId;
    }

    private int mCallId=-1;

    @Override
    public void onUnifiedPortalServiceAvailable(UnifiedPortalService unifiedPortalService) {

    }

    @Override
    public void onUnifiedPortalServiceUnavailable(UnifiedPortalService unifiedPortalService) {

    }


    public void requestToJoinMeeting(String serverAddress, String conferenceId, String meetMeUserName, boolean isPresentationOnlyMode, String callBackNumber, String oneTimePin) {

        UnifiedPortalService service = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getUnifiedPortalService();

        UnifiedPortalConfiguration configuration = new UnifiedPortalConfiguration();
        configuration.setServerURL(serverAddress);

        service.requestToJoinMeeting(configuration, conferenceId, meetMeUserName, isPresentationOnlyMode, callBackNumber, oneTimePin, new RequestToJoinMeetingCompletionHandler() {
            @Override
            public void onSuccess(UnifiedPortalMeetingInfo meetingInfo) {
                Log.d(LOG_TAG, "JoinMeeting with ServiceGatewayURL:{" + meetingInfo.getServiceGatewayURL() + " }, Portaltoken:{" + meetingInfo.getPortalToken() + "}, UCCP URL:{" + meetingInfo.getUCCPURL());

                mMeetineName = meetingInfo.getExtraProperties().get("meetingName");
                CallCreationInfo info = new CallCreationInfo(conferenceId, oneTimePin, meetingInfo.getPortalToken(), meetingInfo.getUCCPURL(), meetingInfo.getServiceGatewayURL(), serverAddress, meetMeUserName, conferenceId, isPresentationOnlyMode);

                mCallId = SDKManager.getInstance().getCallAdaptor().createUnifiedPortalConference(info);

                if (mCallId < 0){
                    if (mUiObj != null && mUiObj.get() != null)
                        mUiObj.get().handleJoinMeetingError(R.string.ups_error_create_meeting_failed);
                }
            }

            @Override
            public void onError(UnifiedPortalError error) {
                Log.w(LOG_TAG, "Error joining to portal meeting: {}" + error);

                int resError = convertUnifiedPortalErrorToResourceString(error);
                if (mUiObj != null && mUiObj.get() != null)
                    mUiObj.get().handleJoinMeetingError(resError);
            }
        });
    }

    public String getMeetingName(){
        return mMeetineName;
    }

    public void setMeetingName(String meetingName){
        mMeetineName=meetingName;
    }

    public void onCallStarted(){
        if (mUiObj != null && mUiObj.get() != null)
            mUiObj.get().handleJoinMeetingSuccess();
    }

    public static int convertUnifiedPortalErrorToResourceString(UnifiedPortalError error){
        switch (error){
            case FAILED:
                return R.string.ups_error_failed;
            case SEND_ERROR:
                return R.string.ups_error_send_error;
            case TIMEOUT:
                return R.string.ups_error_timeout;
            case AUTHENTICATION_FAILURE:
                return R.string.ups_error_authentication_failure;
            case ONE_TIME_PIN_REQUIRED:
                return R.string.enter_otp_title;
            case FORBIDDEN_ONE_TIME_PIN_REQUIRED:
                return R.string.participant_otp_error;
            case VIRTUAL_ROOM_NOT_FOUND:
                return R.string.ups_error_virtal_room_not_found;
            case TENANT_DOES_NOT_EXIST:
                return R.string.ups_error_tenant_not_found;
            case INVALID_PARAMETER:
                return R.string.ups_error_invalid_parameter;
            case CERTIFICATE_ERROR:
                return R.string.ups_error_certificate_error;
            case IDENTITY_NO_CERTIFICATE:
                return R.string.client_certificate_missing_message;
            case IDENTITY_BAD_CERTIFICATE:
                return R.string.client_certificate_bad_message;
            case IDENTITY_UNSUPPORTED_CERTIFICATE:
                return R.string.ups_error_identity_unsupported_certificate;
            case IDENTITY_REVOKED_CERTIFICATE:
                return R.string.client_certificate_revoked_message;
            case IDENTITY_EXPIRED_CERTIFICATE:
                return R.string.client_certificate_expired_message;
            case INVALID_IDENTITY_CERTIFICATE:
                return R.string.ups_error_certificate_error;
            case IDENTITY_UNKNOWN_CA:
                return R.string.ups_error_certificate_error;
            case SECURE_CONNECTION_ERROR:
                return R.string.ups_error_secure_connection_error;
            case SERVER_ERROR:
                return R.string.ups_error_server_error;
            case NOT_AUTHORIZED:
                return R.string.ups_error_not_authorized;
            case NOT_SUPPORTED:
                return R.string.ups_error_not_supported;
            case WRONG_CURRENT_CONFERENCE:
                return R.string.ups_error_wrong_conference_Id;
            case VIRTUAL_ROOM_DISABLED:
                return R.string.ups_error_virtal_room_disabled;
            case VIRTUAL_ROOM_DOES_NOT_ALLOW_INSTANT_MEETING:
                return R.string.ups_error_virtal_room_not_found;
            default:
                return R.string.ups_error_general;
        }
    }

    public void registerListener(UnifiedPortalAdaptorListener uiObj) {

        mUiObj = new WeakReference<>(uiObj);
    }
}
