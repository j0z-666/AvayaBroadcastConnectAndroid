package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;

public class DialerFragmentK175 extends DialerFragment {

    void afterTextChangedLogic(int mTextLength){
        if (isAdded()) {
            if (mTextLength >= 10 && mTextLength < 14) {
                long fontSmall = 56;
                mDigitsView.setTextSize(fontSmall);
            } else if (mTextLength >= 14) {
                long fontSmaller = 42;
                mDigitsView.setTextSize(fontSmaller);
            } else {
                long fontNormal = 70;
                mDigitsView.setTextSize(fontNormal);
            }
        }
    }

    @Override
    void configureButtons(View root){
        TableLayout dialerGrid = root.findViewById(R.id.activity_dialer_pad);
        String[] digits = getResources().getStringArray(R.array.dialer_numbers);
        String[] letters = getResources().getStringArray(R.array.dialer_letters);

        // dialpad buttons ID's
        int buttonIds[] = {
                R.id.mb1,
                R.id.mb2,
                R.id.mb3,
                R.id.mb4,
                R.id.mb5,
                R.id.mb6,
                R.id.mb7,
                R.id.mb8,
                R.id.mb9,
                R.id.mba,
                R.id.mbz,
                R.id.mbp
        };

        if (isAdded()) {
            dialerGrid.setClickable(false);
            for (int i = 0; i < buttonIds.length; i++) {
                configureButton(dialerGrid.findViewById(buttonIds[i]), digits[i], letters[i]);
            }
        }
    }

    @Override
    void configureCallControls(View root){
        if(isAdded()) {
            LinearLayout callControls = root.findViewById(R.id.call_controls);
            final int top = ((getActivity() != null && ((BaseActivity) getActivity()).isAccessibilityEnabled == true && ((BaseActivity) getActivity()).isExploreByTouchEnabled == true)) ? 26 : 66;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) callControls.getLayoutParams();

            params.setMargins(params.leftMargin, top, params.rightMargin, params.bottomMargin); //substitute parameters for left, top, right, bottom
            callControls.setLayoutParams(params);

            params = ((RelativeLayout.LayoutParams) mUriDialing.getLayoutParams());
            params.setMargins(params.leftMargin, top, params.rightMargin, params.bottomMargin); //substitute parameters for left, top, right, bottom

        }
    }


    /**
     * Preparing and configuring buttons onClickListeners
     *
     * @param button       {@link View} for which onClickListener have to be set
     * @param digitString  text to be set on button view
     * @param letterString text to be set under number in button
     */
    private void configureButton(View button, final String digitString, String letterString) {
        final TextView digit = button.findViewById(R.id.digit);
        digit.setText(digitString);
        TextView letters = button.findViewById(R.id.letters);
        letters.setText(letterString);
        digit.setEnabled(false);
        letters.setEnabled(false);
        button.setContentDescription(digitString);
        letters.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        digit.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (!Character.isDigit(digitString.charAt(0))) {
            letters.setVisibility(View.GONE);
        }
        button.setOnClickListener(v -> {
            mNumber += digitString;
            mDigitsView.setText(mNumber);
            //scrollRight();
            if (mMode == DialMode.OFF_HOOK) {
                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putString(REDIAL_NUMBER, mNumber);
                editor.apply();
                if(DialerFragmentK175.this.mListener!=null)
                    DialerFragmentK175.this.mListener.onDialerInteraction(digitString, ACTION.DIGIT);
            }


        });

        // display + if user is holding 0
        if (digitString.equals("0")) {
            button.setOnLongClickListener(v -> {
                if (mNumber.equalsIgnoreCase("")) {
                    mNumber += "+";
                    mDigitsView.setText(mNumber);
                    //scrollRight();
                    if (mMode == DialMode.OFF_HOOK) {
                        SharedPreferences.Editor editor = mSharedPref.edit();
                        editor.putString(REDIAL_NUMBER, mNumber);
                        editor.apply();
                        if(DialerFragmentK175.this.mListener!=null)
                            DialerFragmentK175.this.mListener.onDialerInteraction("+", ACTION.DIGIT);
                    }
                    return true;
                } else {
                    return false;
                }

            });
        }
    }


    /**
     * Get character from keyboard
     */
    public void dialFromKeyboard(String number) {
            mNumber += number;
            mDigitsView.setText(mNumber);
            mNameView.setText(getRedialName());
    }

}
