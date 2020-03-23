package com.avaya.android.vantage.aaadevbroadcast.fragments;

import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;

import static com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity.ACTIVE_VIDEO_CALL_FRAGMENT;
import static com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity.CONTACTS_DETAILS_FRAGMENT;

/**
 * {@link ContactEditFragmentK155} is responsible for process of editing contact data.
 */
public class ContactEditFragmentK155 extends ContactEditFragment {


    @Override
    void changeUIForDevice() {
        if (getActivity() != null) {
            ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(true);
        }
    }

    @Override
    void additonalUIchangesforDevice() {
        if (getActivity() != null) {
            ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(false);
            ((BaseActivity) getActivity()).mViewPager.setEnabledSwipe(true);
        }
    }

    @Override
    void cancelOnClickListenerUIChanges() {
        if (getActivity() != null) {
            if (((BaseActivity) getActivity()).isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)) {

                ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(true);
            } else {
                if (((BaseActivity) getActivity()).isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT))
                    ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(true);
                else
                    ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(false);
            }
        }
    }
}
