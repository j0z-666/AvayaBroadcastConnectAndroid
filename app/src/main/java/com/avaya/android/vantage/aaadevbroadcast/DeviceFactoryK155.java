package com.avaya.android.vantage.aaadevbroadcast;

import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.activities.MainActivityK155;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragmentK155;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ActiveCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ActiveCallFragmentK155;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactDetailsFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactDetailsFragmentK155;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactEditFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactEditFragmentK155;
import com.avaya.android.vantage.aaadevbroadcast.fragments.DialerFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.DialerFragmentK155;
import com.avaya.android.vantage.aaadevbroadcast.fragments.VideoCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.VideoCallFragmentK155;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimation;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimationK155;

/**
 * Created by eabudy on 13/09/2018.
 */

public class DeviceFactoryK155 implements IDeviceFactory {

    private static final String TAG = "DeviceFactoryK155";

    @Override
    public Class getMainActivityClass() {
        Log.d(TAG, "Return MainActivityK155.class");
        return MainActivityK155.class;
    }

    @Override
    public ActiveCallFragment getActiveCallFragment() {
        Log.d(TAG, "Return ActiveCallFragmentK155");
        return new ActiveCallFragmentK155();
    }

    @Override
    public VideoCallFragment getVideoCallFragment() {
        Log.d(TAG, "Return ActiveCallFragmentK155");
        return new VideoCallFragmentK155();
    }

    @Override
    public DialerFragment getDialerFragment() {
        Log.d(TAG, "Return DialerFragmentK155");
        return new DialerFragmentK155();
    }

    @Override
    public ContactEditFragment getContactEditFragment() {
        Log.d(TAG, "Return ContactEditFragmentK155");
        return new ContactEditFragmentK155();
    }

    @Override
    public ContactDetailsFragment getContactDetailsFragment() {
        Log.d(TAG, "Return ContactDetailsFragmentK155");
        return new ContactDetailsFragmentK155();
    }

    @Override
    public SlideAnimation getSlideAnimation() {
        Log.d(TAG, "Return SlideAnimationK155");
        return new SlideAnimationK155();
    }

    @Override
    public ContactsFragment getContactsFragment() {
        Log.d(TAG, "Return ContactsFragmentK155");
        return new ContactsFragmentK155();
    }
}
