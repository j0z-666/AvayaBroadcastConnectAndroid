package com.avaya.android.vantage.aaadevbroadcast.model;

import com.avaya.android.vantage.aaadevbroadcast.R;

/**
 * {@link UIAudioDevice} enum representing states of all possible audio device
 */

public enum UIAudioDevice {
    WIRED_HEADSET("WIRED_HEADSET", R.id.container35Headset),
    WIRED_USB_HEADSET("WIRED_USB_HEADSET", R.id.containerUsbHeadset),
    HANDSET("HANDSET", R.id.containerHandset),
    SPEAKER("SPEAKER", R.id.containerSpeaker),
    BLUETOOTH_HEADSET("BLUETOOTH_HEADSET", R.id.containerBTHeadset),
    WIRED_SPEAKER("WIRED_SPEAKER", R.id.containerSpeaker),
    RJ9_HEADSET("RJ9_HEADSET", R.id.containerHeadset),
    WIRELESS_HANDSET("WIRELESS_HANDSET", R.id.containerHandset);

    private final String mName;
    private final int mUid;
    UIAudioDevice(String name, int id) {
        mName = name;
        mUid = id;
    }

    public int getUIId() {
        return  mUid;
    }

    public String toString() {
        return mName;
    }

}