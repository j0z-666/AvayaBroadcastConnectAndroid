package com.avaya.android.vantage.aaadevbroadcast.csdk;

/**
 * Interface used for communication between {@link com.avaya.android.vantage.aaadevbroadcast.fragments.DialerFragment}
 * , {@link com.avaya.android.vantage.aaadevbroadcast.adaptors.UIVoiceMessageAdaptor} and {@link VoiceMessageAdaptor}
 */

public interface VoiceMessageAdaptorListener {

    void onMessageWaitingStatusChanged(boolean voiceMsgsAreWaiting);

    void onVoicemailNumberChanged(String voicemailNumber);
}
