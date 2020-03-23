package com.avaya.android.vantage.aaadevbroadcast;

import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ActiveCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactDetailsFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactEditFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.DialerFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.VideoCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimation;

/**
 * Created by eabudy on 13/09/2018.
 */

public interface IDeviceFactory {

    Class getMainActivityClass();

    ActiveCallFragment getActiveCallFragment();

    VideoCallFragment getVideoCallFragment();

    DialerFragment getDialerFragment();

    ContactEditFragment getContactEditFragment();

    ContactDetailsFragment getContactDetailsFragment();

    SlideAnimation getSlideAnimation();

    ContactsFragment getContactsFragment();

}
