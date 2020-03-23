package com.avaya.android.vantage.aaadevbroadcast.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.views.interfaces.FinishCallDialerActivityInterface;

import java.lang.ref.WeakReference;

import static com.avaya.android.vantage.aaadevbroadcast.Constants.DigitKeys;

/**
 * Activity responsible for showing and working with call dialer which is used for
 * call transfers or adding additional person to call and creating conference call.
 */

public class CallDialerActivity extends AppCompatActivity implements FinishCallDialerActivityInterface {

    // TODO: Rename and change types of parameters
    private static final String TAG = "CallDialerActivity";
    private String mNumber = "";
    private TextView mDigitsView;
    private TextView mNameView;
    private ImageView mDelete;
    private HorizontalScrollView mTextScroll;
    private Handler mHandler;
    private int mCallActiveCallID = -1;
    private final long fontNormal = 56;
    private final long fontSmall = 42;
    private final long fontSmaller = 28;
    private String mRequestName;

    private boolean isToLockPressButton = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.callDialerActivityRef = new WeakReference<>(this);

        setContentView(R.layout.call_dialpad_layout);
        boolean isConferenceCall = getIntent().getBooleanExtra(Constants.IS_CONFERENCE, false);
        mCallActiveCallID = getIntent().getIntExtra(Constants.CALL_ID, -1);
        setupFullscreen();

        mDigitsView = findViewById(R.id.call_digits);
        mNameView = findViewById(R.id.call_name);
        if (mDigitsView != null) {
            mDigitsView.setText("");
        }
        if (mNameView != null) {
            mNameView.setText("");
        }
        mTextScroll = findViewById(R.id.scroll_call_digits);
        mTextScroll.setFocusable(false);
        mTextScroll.setFocusableInTouchMode(false);

        TableLayout dialerGrid = findViewById(R.id.activity_dialer_pad);
        if (dialerGrid != null) {
            dialerGrid.setClickable(false);
        }
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


        if (dialerGrid != null) {
            for (int i = 0; i < buttonIds.length; i++) {
                configureButton(dialerGrid.findViewById(buttonIds[i]), digits[i], letters[i]);
            }
        }

        mDelete = findViewById(R.id.call_delete);
        if (mDelete != null) {
            mDelete.setVisibility(View.INVISIBLE);
            mDelete.setOnClickListener(v -> {
                if (mNumber.length() > 0) {
                    mNumber = mNumber.substring(0, mNumber.length() - 1);
                    mDigitsView.setText(mNumber);
                }
            });
            mDelete.setOnLongClickListener(v -> {
                mNumber = "";
                mDigitsView.setText("");
                return true;
            });
        }

        configureRedialButton();

        TextView mAudioCallButton = findViewById(R.id.audio_call_button);
        if (isConferenceCall) {
            if (mAudioCallButton != null) {
                mAudioCallButton.setText(getText(R.string.feature_dialog_conference));
            }
            mRequestName = getResources().getString(R.string.merge_complete);
        } else {
            if (mAudioCallButton != null) {
                mAudioCallButton.setText(getText(R.string.feature_dialog_transfer));
            }
            mRequestName = getResources().getString(R.string.trasfer_complete);
        }

        if (mAudioCallButton != null) {
            mAudioCallButton.setOnClickListener(v -> {
                if (!mDigitsView.getText().toString().equalsIgnoreCase("")) {
                    Utils.sendSnackBarData(v.getContext(), mRequestName, Utils.SNACKBAR_LONG);
                    Intent data = new Intent(getPackageName() + Constants.ACTION_TRANSFER);
                    data.putExtra(Constants.CALL_ID, mCallActiveCallID);
                    data.putExtra(Constants.TARGET, mDigitsView.getText().toString());
                    Log.d(TAG, "Call ID: " + mCallActiveCallID + ", Number: " + mDigitsView.getText().toString());
                    setResult(RESULT_OK, data);
                    finish();
                } else {
                    Utils.sendSnackBarData(v.getContext(), getString(R.string.no_number_message), Utils.SNACKBAR_LONG);
                }
            });
        }

        ImageButton mContactItemCallVideo = findViewById(R.id.contact_item_call_video);
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
            if (mContactItemCallVideo != null) {
                mContactItemCallVideo.setEnabled(true);
                mContactItemCallVideo.setOnClickListener(v -> {
                });
            }
        } else {
            if (mContactItemCallVideo != null) {
                mContactItemCallVideo.setEnabled(false);
            }
        }

        ImageView mExitDialpad = findViewById(R.id.exit_dialpad);
        if (mExitDialpad != null) {
            mExitDialpad.setOnClickListener(view -> finish());
        }

        mDigitsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mNumber.equals("")) {
                    mDelete.setVisibility(View.INVISIBLE);
                } else {
                    mDelete.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // resizing text in TextView depending of a character number
                int mTextLength = mDigitsView.getText().length();

                if (mTextLength >= 10 && mTextLength < 13) {
                    mDigitsView.setTextSize(fontSmall);
                } else if (mTextLength >= 13) {
                    mDigitsView.setTextSize(fontSmaller);
                } else {
                    mDigitsView.setTextSize(fontNormal);
                }
            }
        });
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
        if (!Character.isDigit(digitString.charAt(0))) {
            letters.setVisibility(View.GONE);
        }
        button.setOnClickListener(v -> {
            mNumber += digitString;
            mDigitsView.setText(mNumber);
            scrollRight();
        });

        // display + if user is holding 0
        if (digitString.equals("0")) {
            button.setOnLongClickListener(v -> {
                if (mNumber.equalsIgnoreCase("")) {
                    mNumber += "+";
                    mDigitsView.setText(mNumber);
                    scrollRight();
                    return true;
                } else {
                    return false;
                }

            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (getResources().getBoolean(R.bool.is_landscape) && !isToLockPressButton) {

            isToLockPressButton = true;

            int keyunicode = event.getUnicodeChar(event.getMetaState());
            if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                if (mNumber.length() > 0) {
                    mNumber = mNumber.substring(0, mNumber.length() - 1);
                    mDigitsView.setText(mNumber);
                    mNameView.setText(getRedialName());
                }
                return true;
            } else if (DigitKeys.contains(event.getKeyCode())) {
                mNumber += "" + (char) keyunicode;
                mDigitsView.setText(mNumber);
                mNameView.setText(getRedialName());
                event.startTracking();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            if(keyCode == KeyEvent.KEYCODE_0 && mNumber.length() == 1){
                mNumber = mNumber.replace('0','+');
                mDigitsView.setText(mNumber);
            }
        }
        if(DigitKeys.contains(event.getKeyCode()))
            return true;
        else
            return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            isToLockPressButton = false;
        }
        if(DigitKeys.contains(event.getKeyCode()))
            return true;
        else
            return super.onKeyUp(keyCode, event);
    }

    /**
     * Return String representation of last number to be redialed
     *
     * @return String with representation of last dialed number
     */
    private String getRedialNumber() {
        return "";
    }

    /**
     * Return String representation of name of last user which have to be redialed
     *
     * @return String with representation of name of last user which have to be redialed
     */
    private String getRedialName() {
        return "";
    }

    /**
     * Change screen params to fullscreen preferences.
     */
    private void setupFullscreen() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    /**
     * Method responsible for scrolling text in TextView while we are typing number which have
     * to be called dialed
     */
    private void scrollRight() {

        getHandler().postDelayed(() -> {
            if (mTextScroll != null) {
                mTextScroll.fullScroll(View.FOCUS_RIGHT);
            }
        }, 100);
    }

    /**
     * Returning Handler require for performing some delayed jobs in {@link #scrollRight()}
     *
     * @return Handler required for use in {@link #scrollRight()}
     */
    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler();
        }
        return mHandler;
    }

    /**
     * Setting configuration for redial button. In case in class {@link ConfigParametersNames}
     * parameter {@link ConfigParametersNames#ENABLE_REDIAL} is set to true we will enable redial
     * functionality.
     */
    private void configureRedialButton() {

        boolean enableRedial = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_REDIAL);
        ImageButton mRedialButton = findViewById(R.id.redialButton);
        if (enableRedial) {
            if (mRedialButton != null) {
                mRedialButton.setOnClickListener(v -> {
                    String redialNumber = getRedialNumber();
                    if (redialNumber.length() > 0) {
                        mNumber = redialNumber;
                        mDigitsView.setText(mNumber);
                        mNameView.setText(getRedialName());
                    }
                });
            }
        }
        if (mRedialButton != null) {
            mRedialButton.setEnabled(enableRedial);
        }
    }

    @Override
    public void killCallDialerActivity() {
        finish();
    }
}