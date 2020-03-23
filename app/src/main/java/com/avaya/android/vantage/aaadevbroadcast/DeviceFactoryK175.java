package com.avaya.android.vantage.aaadevbroadcast;

import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.activities.MainActivityK175;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragmentK175;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ActiveCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ActiveCallFragmentK175;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactDetailsFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactDetailsFragmentK175;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactEditFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactEditFragmentK175;
import com.avaya.android.vantage.aaadevbroadcast.fragments.DialerFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.DialerFragmentK175;
import com.avaya.android.vantage.aaadevbroadcast.fragments.VideoCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.VideoCallFragmentK175;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimation;

/**
 * Created by eabudy on 13/09/2018.
 */

public class DeviceFactoryK175 implements IDeviceFactory {

    private static final String TAG = "DeviceFactoryK175";

    @Override
    public Class getMainActivityClass() {
        Log.d(TAG, "Return MainActivityK175.class");
        return MainActivityK175.class;
    }

    @Override
    public ActiveCallFragment getActiveCallFragment() {
        Log.d(TAG, "Return ActiveCallFragmentK175");
        return new ActiveCallFragmentK175();
    }

    @Override
    public VideoCallFragment getVideoCallFragment() {
        Log.d(TAG, "Return ActiveCallFragmentK175");
        return new VideoCallFragmentK175();
    }

    @Override
    public DialerFragment getDialerFragment() {
        Log.d(TAG, "Return DialerFragmentK175");
        return new DialerFragmentK175();
    }

    @Override
    public ContactEditFragment getContactEditFragment() {
        Log.d(TAG, "Return ContactEditFragmentK175");
        return new ContactEditFragmentK175();
    }

    @Override
    public ContactDetailsFragment getContactDetailsFragment() {
        Log.d(TAG, "Return ContactDetailsFragmentK175");
        return new ContactDetailsFragmentK175();
    }

    @Override
    public SlideAnimation getSlideAnimation() {
        Log.d(TAG, "Return SlideAnimation");
        return new SlideAnimation();
    }

    @Override
    public ContactsFragment getContactsFragment() {
        Log.d(TAG, "Return ContactsFragmentK175");
        return new ContactsFragmentK175();
    }
}
