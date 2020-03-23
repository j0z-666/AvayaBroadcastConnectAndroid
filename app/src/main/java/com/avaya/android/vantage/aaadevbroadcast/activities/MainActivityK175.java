package com.avaya.android.vantage.aaadevbroadcast.activities;

import android.content.Context;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.views.DialogAAADEVMessages;


public class MainActivityK175 extends BaseActivity {


    private static final String TAG = "MainActivityK175";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);

        Utils.overrideFontScaleAndDensityK175(this);
    }

    @Override
    void setVideoMuteisibility(){

        if (false == Utils.isCameraSupported()) {
            mVideoMute.setVisibility(View.GONE);
        } else {
            mVideoMute.setVisibility(View.VISIBLE);
        }
    }

    protected void onPageScrolledLogic(){
        mViewPager.setEnabledSwipe(true);
    }

    protected void onPageSelectedCondition(){}

    void fullScreenViewResizeLogic(int startDimension){
        if(mViewPager!=null && mViewPager.getLayoutParams()!=null)
            mViewPager.getLayoutParams().height = startDimension - 136;
    }


    int onKeyDownDeviceLogic(int keyCode, KeyEvent event){
        if (mViewPager!=null && mViewPager.getCurrentItem() == 0) {
            try {
                int keyunicode = event.getUnicodeChar(event.getMetaState());
                char character = (char) keyunicode;
                String digit = "" + character;
                if (isFragmentVisible(DIALER_FRAGMENT) && mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                    if (digit.matches("[\\d]") || digit.matches("#") || digit.matches("\\*")) {
                        mSectionsPagerAdapter.getDialerFragment().dialFromKeyboard(digit);
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        mSectionsPagerAdapter.getDialerFragment().deleteDigit();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        return -1;
    }

    boolean onDialerInteractionDeviceLogic(String number){
        mCallViewAdaptor.addDigitToOffHookDialCall(number.charAt(0));
        return true;
    }


    void expandPhoneNumberSlide(){
        mSlideSelectPhoneNumber.expand(mSelectPhoneNumber);
    }


    void onClickUser(){

        if(mPickContacts.getVisibility()==View.VISIBLE) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        mUser.setContentDescription(mLoggedUserExtension.getText().toString() + " " + mLoggedUserNumber.getText().toString() + " " + getString(R.string.user_content_description));

        mSlideSelecAudioDevice.collapse(mToggleAudioMenu);

        if (mListPreferences.getVisibility() == View.GONE || mListPreferences.getVisibility() == View.INVISIBLE) {

            mSlideUserPreferences.expand(mListPreferences);

            mFrameAll.setVisibility(View.VISIBLE);
            mHandler.postDelayed(mLayoutCloseRunnable, Constants.LAYOUT_DISAPPEAR_TIME);
            mOpenUser.setImageDrawable(getDrawable(R.drawable.ic_expand_less));
        } else {
            mOpenUser.setImageDrawable(getDrawable(R.drawable.ic_expand_more));

            mSlideUserPreferences.collapse(mListPreferences);

            mHandler.removeCallbacks(mLayoutCloseRunnable);
        }

    }

    void onClickTransducerButton(){

        mSlideUserPreferences.collapse(mListPreferences);

        if (mToggleAudioMenu.getVisibility() == View.VISIBLE) {
            mSlideSelecAudioDevice.collapse(mToggleAudioMenu);
            mHandler.removeCallbacks(mLayoutCloseRunnable);
        } else {
            mSlideSelecAudioDevice.expand(mToggleAudioMenu);
            mFrameAll.setVisibility(View.VISIBLE);
            mHandler.postDelayed(mLayoutCloseRunnable, Constants.LAYOUT_DISAPPEAR_TIME);
        }
    }

    void onClickSearchButton(){}


    void collapseSlideSelecAudioDevice(){
        mSlideSelecAudioDevice.collapse(mToggleAudioMenu);
    }

    void collapseSlideUserPreferences(){
        mSlideUserPreferences.collapse(mListPreferences);
    }

    void collapseSlideSelectPhoneNumber(){
        mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber);
    }


    @Override
    void changeAudioVideoMuteButtonsVisibility(boolean isMuteEnabled, boolean isVideoEnabled){
        if (isMuteEnabled)
            mAudioMute.setVisibility(View.VISIBLE);
        else
            mAudioMute.setVisibility(View.INVISIBLE);

        if (Utils.isCameraSupported() && isMuteEnabled & isVideoEnabled)
            mVideoMute.setVisibility(View.VISIBLE);
        else
            mVideoMute.setVisibility(View.GONE);
    }

}
