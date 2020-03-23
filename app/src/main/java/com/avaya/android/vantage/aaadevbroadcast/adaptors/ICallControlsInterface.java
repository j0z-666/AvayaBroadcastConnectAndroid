package com.avaya.android.vantage.aaadevbroadcast.adaptors;

import com.avaya.android.vantage.aaadevbroadcast.model.UICall;

/**
 * Interface responsible for providing communication from {@link UICallViewAdaptor}
 * and {@link com.avaya.android.vantage.aaadevbroadcast.activities.MainActivity}
 */
public interface ICallControlsInterface {

    void onVideoMuted(UICall uiCall, boolean muting);

    void onCallMissed();
}
