package com.avaya.android.vantage.aaadevbroadcast.csdk;

import com.avaya.android.vantage.aaadevbroadcast.model.UIAudioDevice;

/**
 * {@link AudioDeviceAdaptorListener} interface providing communication from {@link AudioDeviceAdaptor}
 * and {@link com.avaya.android.vantage.aaadevbroadcast.adaptors.UIAudioDeviceViewAdaptor}
 */

public interface AudioDeviceAdaptorListener {
    /**
     * Notify if the device's status changed to active or not active
     * @param device {@link UIAudioDevice}
     * @param active true if Audio Device is active
     */
    void onDeviceChanged(UIAudioDevice device, boolean active);
}
