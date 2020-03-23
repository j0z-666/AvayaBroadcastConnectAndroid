package com.avaya.android.vantage.aaadevbroadcast.views.interfaces;

import com.avaya.android.vantage.aaadevbroadcast.model.UIAudioDevice;

/**
 * Audio device change interface responsible for connecting {@link com.avaya.android.vantage.aaadevbroadcast.activities.MainActivity}
 *  and {@link com.avaya.android.vantage.aaadevbroadcast.adaptors.UIAudioDeviceViewAdaptor}
 */
public interface IDeviceViewInterface {

    void onDeviceChanged(UIAudioDevice device, boolean active);
}
