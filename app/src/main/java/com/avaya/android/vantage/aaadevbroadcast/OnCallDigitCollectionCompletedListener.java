package com.avaya.android.vantage.aaadevbroadcast;

import com.avaya.android.vantage.aaadevbroadcast.model.UICall;

/**
 * Interface responsible for connecting and providing communication of {@link com.avaya.android.vantage.aaadevbroadcast.fragments.DialerFragment}
 * and {@link com.avaya.android.vantage.aaadevbroadcast.adaptors.UICallViewAdaptor}
 */
public interface OnCallDigitCollectionCompletedListener {
    void onCallDigitCollectionCompleted(UICall call);
}
