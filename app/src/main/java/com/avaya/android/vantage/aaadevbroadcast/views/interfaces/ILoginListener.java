package com.avaya.android.vantage.aaadevbroadcast.views.interfaces;

/**
 * SIP Login listener responsible for connecting {@link com.avaya.android.vantage.aaadevbroadcast.adaptors.UIDeskPhoneServiceAdaptor}
 * and {@link com.avaya.android.vantage.aaadevbroadcast.activities.MainActivity}
 */

public interface ILoginListener {
    void onSuccessfulLogin(String name, String extension);
}
