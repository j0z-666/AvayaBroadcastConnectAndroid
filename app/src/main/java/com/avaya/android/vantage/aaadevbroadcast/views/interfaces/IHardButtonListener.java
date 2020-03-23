package com.avaya.android.vantage.aaadevbroadcast.views.interfaces;

import androidx.annotation.NonNull;

import com.avaya.deskphoneservices.HardButtonType;

public interface IHardButtonListener {
    /**
     * key up event received from platform via MEDIA_BUTTON intent
     * @param hardButton hard button type
     */
    void onKeyUp(@NonNull HardButtonType hardButton);
    /**
     * key down event received from platform via MEDIA_BUTTON intent
     * @param hardButton hard button type
     */
    void onKeyDown(@NonNull HardButtonType hardButton);
}
