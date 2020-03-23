package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.os.SystemClock;
import android.view.View;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;

public class DialerFragmentK155 extends DialerFragment {


    @Override
    void setOffHookButtonResource(){
        if (getActivity()!=null) {
            int resId = ((BaseActivity) getActivity()).getDeviceResIdFromSharedPref();
            offHook.setBackgroundResource(resId);
        }
    }

    @Override
    void setRedialButtonVisibility(boolean enableRedial){
        if(isAdded() && mRedialButton!=null) {
            if(enableRedial)
                mRedialButton.setVisibility(View.VISIBLE);
            else
                mRedialButton.setVisibility(View.INVISIBLE);
        }
    }

    void afterTextChangedLogic(int mTextLength){
        if (mTextLength >= 10 && mTextLength < 14) {
            long fontSmallLand = 44;
            mDigitsView.setTextSize(fontSmallLand);
        } else if (mTextLength >= 14) {
            long fontSmallerLand = 36;
            mDigitsView.setTextSize(fontSmallerLand);
        } else {
            long fontNormalLand = 54;
            mDigitsView.setTextSize(fontNormalLand);
        }
    }


    @Override
    void configureTransducerButtons(View root){
        transducerButton = root.findViewById(R.id.transducer_button);
        transducerButton.setOnClickListener(v -> {
            mLastClickTime = SystemClock.elapsedRealtime();

            mCallback.triggerTransducerButton(v);
        });

        offHook = root.findViewById(R.id.off_hook);
        offHook.setOnClickListener(v -> {
            if(getActivity() !=null)
                ((BaseActivity) getActivity()).setOffhookButtosChecked(offHook.isChecked());

            mLastClickTime = SystemClock.elapsedRealtime();

            mCallback.triggerOffHookButton(v);
        });

        if(getActivity() !=null) {
            boolean checked = ((BaseActivity)getActivity()).isOffhookChecked();
            offHook.setChecked(checked);
        }
    }


    public void dialFromKeyboard(String number) {

            if(number.equalsIgnoreCase("+") && isFirstDigitInDial && mDigitsView.getText().toString().length() == 1){
                mNumber = "+";
                isFirstDigitInDial = false;
            }else  if(!number.equalsIgnoreCase("+")) {
                mNumber += number;
            }
            mDigitsView.setText(mNumber);
            mNameView.setText(getRedialName());
    }

    @Override
    void setRedialButtonVisibility(){
        if(isAdded()&&mRedialButton!=null) {
            enableRedial = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_REDIAL);
            if(enableRedial)
                mRedialButton.setVisibility(mMode == DialMode.EDIT ? View.INVISIBLE : View.VISIBLE);
            else
                mRedialButton.setVisibility(View.INVISIBLE);
        }

    }

}
