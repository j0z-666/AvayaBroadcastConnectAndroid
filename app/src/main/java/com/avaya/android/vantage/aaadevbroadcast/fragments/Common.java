package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;

import java.util.List;

import static com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity.ACTIVE_VIDEO_CALL_FRAGMENT;
import static com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity.CONTACTS_EDIT_FRAGMENT;

/**
 * Created by eabudy on 07/10/2018.
 */

class Common {

    private static final String TAG = Common.class.getSimpleName();

    static void initView(View view, BaseActivity activity, OffHookTransduceButtonInterface mCallbackoffHookTransduceButtonInterface){

        ToggleButton transducerButton = view.findViewById(R.id.transducer_button);
        transducerButton.setOnClickListener(mCallbackoffHookTransduceButtonInterface::triggerTransducerButton);

        ToggleButton offHook = view.findViewById(R.id.off_hook);
        offHook.setOnClickListener(v -> {
            if (activity != null) {
                ToggleButton offHook1 = view.findViewById(R.id.off_hook);
                activity.setOffhookButtosChecked(offHook1.isChecked());
            }
            mCallbackoffHookTransduceButtonInterface.triggerOffHookButton(v);
        });

        if(activity!=null) {
            activity.changeUiForFullScreenInLandscape(false);
        }
    }

    static void changeUIOnTouch(Activity activity){
        if(activity!=null) {
            if( ((BaseActivity) activity).isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT) ){
                ((BaseActivity) activity).changeUiForFullScreenInLandscape(true);
            }

            //after phone number is selected, we just hide invisible frame and list of phone numbers
            ((BaseActivity) activity).mSelectPhoneNumber.setVisibility(View.INVISIBLE);
            ((BaseActivity) activity).mFrameAll.setVisibility(View.GONE);
        }
    }

    static void setOffhookButtonParameters(Activity activity, ToggleButton offHook){
        if(activity!=null) {
            int resId = ((BaseActivity)activity).getDeviceResIdFromSharedPref();
            offHook.setBackgroundResource(resId);
            offHook.setChecked(true);
        }
    }

    static void changeUIForBackArrowClick(Activity activity){
        if (activity != null) {
            FragmentManager fragmentManager = ((BaseActivity) activity).getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible())
                    if (fragment instanceof ContactDetailsFragment || fragment instanceof ContactEditFragment || fragment instanceof VideoCallFragment) {
                        ((BaseActivity) activity).changeUiForFullScreenInLandscape(true);
                        break;
                    } else {
                        ((BaseActivity) activity).changeUiForFullScreenInLandscape(false);
                    }
            }
            ((BaseActivity)activity).changeButtonsVisibility(((BaseActivity)activity).getSelectedTab());
        }
    }


    static void changeUIonConferenceListClicked(Activity activity){
        if(activity !=null){

            if(((BaseActivity)activity).isFragmentVisible(CONTACTS_EDIT_FRAGMENT) && !((BaseActivity)activity).mSectionsPagerAdapter.isCallAddParticipant()){
                ((BaseActivity)activity).changeUiForFullScreenInLandscape(true);
            }else{
                ((BaseActivity)activity).changeUiForFullScreenInLandscape(false);
            }
        }
    }


    static void changeUIonTransferClicked(Activity activity){
        if(activity!=null){
            ((BaseActivity) activity).changeUiForFullScreenInLandscape(false);
        }
    }


    static public void cancelFullScreenMode(Activity activity){
        try {
            if (activity != null) {

                FragmentManager fragmentManager = ((BaseActivity) activity).getSupportFragmentManager();
                List<Fragment> fragments = fragmentManager.getFragments();
                for (Fragment fragment : fragments) {
                    if (fragment != null)
                        if ((fragment instanceof ContactDetailsFragment || fragment instanceof ContactEditFragment)
                                && !((BaseActivity) activity).mSectionsPagerAdapter.isCallAddParticipant()) {
                            ((BaseActivity) activity).changeUiForFullScreenInLandscape(true);
                        } else if (fragment instanceof ActiveCallFragmentK155) {
                            ((BaseActivity) activity).changeUiForFullScreenInLandscape(false);
                        } else if (fragment instanceof DialerFragment) {
                            ((BaseActivity) activity).changeUiForFullScreenInLandscape(false);
                        }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "cancelFullScreenMode", e);
        }
    }


    static void setMoreButtonVisibility(int visibility, View view){
        if (view != null)
            view.setVisibility(visibility);
    }

    static void setMoreButtonEnabled(boolean isEnabled, View view) {
        if (view != null)
            view.setEnabled(isEnabled);
    }


}
