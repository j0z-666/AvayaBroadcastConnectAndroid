package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.avaya.android.vantage.aaadevbroadcast.BuildConfig;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.GoogleAnalyticsUtils;
import com.avaya.android.vantage.aaadevbroadcast.OnCallDigitCollectionCompletedListener;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.EnterpriseContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.contacts.LocalContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.csdk.VoiceMessageAdaptorListener;
import com.avaya.android.vantage.aaadevbroadcast.fragments.settings.ConfigChangeApplier;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.model.UICall;
import com.avaya.clientservices.client.Client;
import com.avaya.deskphoneservices.DeskPhoneServiceLibrary;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnDialerInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DialerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
abstract public class DialerFragment extends androidx.fragment.app.Fragment implements ConfigChangeApplier, VoiceMessageAdaptorListener, OnCallDigitCollectionCompletedListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = DialerFragment.class.getSimpleName();
    private static final String NUMBER = "number";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MODE = "mode";
    static final String REDIAL_NUMBER = "redialNumber";

    public static final int DELAY = 0;
    private static final int PERIOD = 60*1000;
    //    private Handler mHandler;
    private ToggleButton mVoicemail;
    private boolean isFirstNameFirst;
    private ImageButton mAudioButton;
    ImageButton mRedialButton;
    public EditText mSubject;

    public ToggleButton transducerButton;
    public ToggleButton offHook;
    OffHookTransduceButtonInterface mCallback;

    // TODO: Rename and change types of parameters
    String mNumber = "";
    private String mName = "";
    private String mType = "";
    private String mAutoCompleteNumber;
    TextView mDigitsView;
    TextView mNameView;
    private ImageView mDelete, mVideoCall;

    OnDialerInteractionListener mListener;
    private Timer mDisplayTimer;
    private HorizontalScrollView mTextScroll;
    SharedPreferences mSharedPref;
    DialMode mMode;

    boolean enableRedial = true;
    long mLastClickTime = 0;

    private TextView dateUndwerClock;
    private LinearLayout clockWrapper;

    protected ImageButton mUriDialing;

    //custom
    TextView mCSDLibraryView;
    TextView mDeskphoneLibraryView;
    TextView mAppVersionView;

    /**
     * Called when all saved state has been restored into the view hierarchy
     * of the fragment.  This can be used to do initialization based on saved
     * state that you are letting the view hierarchy track itself, such as
     * whether check box widgets are currently checked.  This is called
     * after {@link #onActivityCreated(Bundle)} and before
     * {@link #onStart()}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        assert getArguments() != null;
        mNumber = getArguments().getString(NUMBER);
        mName = getArguments().getString(NAME);
        mType = getArguments().getString(TYPE);
        mMode = (DialMode) getArguments().getSerializable(MODE);
        View root = getView();
        if (root != null) {
            mDigitsView = root.findViewById(R.id.digits);
            mNameView = root.findViewById(R.id.name);
            dateUndwerClock = root.findViewById(R.id.date);

            if (!mNumber.isEmpty()) {
//                mDigitsView.setText(SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()));
//                mNameView.setText(SimpleDateFormat.getDateInstance(DateFormat.FULL).format(new Date()));
            } else {
                mDigitsView.setText(mNumber);
                mNameView.setText(mName);
            }

            DialMode mode = isOffhook() ? DialMode.OFF_HOOK : DialMode.EDIT;
            setMode(mode);
        }
    }

    private boolean isOffhook(){
        return SDKManager.getInstance().getCallAdaptor().getOffhookCallId() != 0;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        clockWrapper = view.findViewById(R.id.clock_wrapper);

        if (getView() != null) {
            mVoicemail = getView().findViewById(R.id.voicemail);
            mVoicemail.setOnClickListener(view1 -> {
                Log.e(TAG, "voiceMail number " + SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber());
                if (mListener != null && (SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber() != null)) {
                    mListener.onDialerInteraction(SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber(), ACTION.AUDIO);
                }
            });
        }
        onMessageWaitingStatusChanged(SDKManager.getInstance().getVoiceMessageAdaptor().getVoiceState());
        onVoicemailNumberChanged(SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber());

        setOffHookButtonResource();

        mSharedPref = Objects.requireNonNull(this.getActivity()).getSharedPreferences(REDIAL_NUMBER, Context.MODE_PRIVATE);
        isFirstNameFirst = ContactsFragment.isFirstNameFirst();

        mTextScroll = getView().findViewById(R.id.textScroll);
        mTextScroll.setFocusable(false);
        mTextScroll.setFocusableInTouchMode(false);

        if (mUriDialing != null) {
            mUriDialing.setOnClickListener(view1 -> {
                showJoinMeetingFragment();
            });
        }
        mDigitsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                SDKManager.getInstance().getCallAdaptor().setDigitsDialed(mNumber);

                if (mNumber != null && mNumber.length() > 0) {
                    new GetPhoneNumberAsync(DialerFragment.this).execute(mNumber);
                } else {
                    mNameView.setText("");
                }
                if (mNumber != null && mNumber.equals("")) {
                    clockWrapper.setVisibility(View.VISIBLE);
                    if (SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber() != null)
                        mVoicemail.setVisibility(View.VISIBLE);
                    mDelete.setVisibility(View.INVISIBLE);
                    mNameView.setVisibility(View.INVISIBLE);

                    setRedialButtonVisibility(enableRedial);
                } else {
                    mVoicemail.setVisibility(View.INVISIBLE);
                    if (mMode == DialMode.OFF_HOOK) {
                        clockWrapper.setVisibility(View.INVISIBLE);
                        mDelete.setVisibility(View.INVISIBLE);
                        mNameView.setVisibility(View.INVISIBLE);
                        setRedialButtonVisibility(true);
                    } else {
                        clockWrapper.setVisibility(View.INVISIBLE);
                        mDelete.setVisibility(View.VISIBLE);
                        mNameView.setVisibility(View.INVISIBLE);
                        setRedialButtonVisibility(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // resizing text in TextView depending of a character number
                int mTextLength = mDigitsView.getText().length();
                afterTextChangedLogic(mTextLength);

                if (mTextScroll != null){
                    mTextScroll.postDelayed(new TextScrollRunnable(mTextScroll), 100L);
                }
            }
        });
    }

    private void showJoinMeetingFragment() {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        JoinMeetingFragment fragment = (JoinMeetingFragment)fm.findFragmentById(R.id.join_meeting);
        if (fragment == null){
            fragment = JoinMeetingFragment.newInstance();
            ft.add(R.id.join_meeting, fragment, fragment.getClass().getSimpleName());
        }
        else{
            ft.show(fragment);
            fragment.getView().setVisibility(View.VISIBLE);
        }
        ft.commit();
    }

    void setOffHookButtonResource(){}

    void setRedialButtonVisibility(boolean enableRedial){}

    abstract void afterTextChangedLogic(int mTextLength);

    /**
     * Called to ask the fragment to save its current dynamic state, so it
     * can later be reconstructed in a new instance of its process is
     * restarted.  If a new instance of the fragment later needs to be
     * created, the data you place in the Bundle here will be available
     * in the Bundle given to {@link #onCreate(Bundle)},
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, and
     * {@link #onActivityCreated(Bundle)}.
     * <p/>
     * Activity.onSaveInstanceState(Bundle)} and most of the discussion there
     * applies here as well.  Note however: <em>this method may be called
     * at any time before {@link #onDestroy()}</em>.  There are many situations
     * where a fragment may be mostly torn down (such as when placed on the
     * back stack with no UI showing), but its state will not be saved until
     * its owning activity actually needs to save its state.
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(NAME, mName);
        outState.putString(NUMBER, mNumber);
        outState.putString(TYPE, mType);
        outState.putSerializable(MODE, mMode);
    }

    public DialerFragment() {
        // Required empty public constructor
    }

    private final Handler mHandler = new Handler();

    @Override
    public void onResume() {
        super.onResume();
        //startClockTimer();

        Runnable runnable = new DateUpdateRunnable(dateUndwerClock, mHandler);
        mHandler.post(runnable);

        int visibility = Utils.isCameraSupported() ? View.VISIBLE : View.GONE;
        setVideoButtonVisibility(visibility);

        onMessageWaitingStatusChanged(SDKManager.getInstance().getVoiceMessageAdaptor().getVoiceState());
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
        if (mDisplayTimer != null) {
            mDisplayTimer.cancel();
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param number number to preload the dialer
     * @param name   number to preload the dialer
     * @param type   contact type
     * @param mode   dial mode
     * @return A new instance of fragment DialerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DialerFragment newInstance(String number, String name, String type, DialMode mode) {
        DialerFragment fragment = ElanApplication.getDeviceFactory().getDialerFragment();
        Bundle args = new Bundle();
        args.putString(NUMBER, number);
        args.putString(NAME, name);
        args.putString(TYPE, type);
        args.putSerializable(MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNumber = getArguments().getString(NUMBER);
            mName = getArguments().getString(NAME);
            mType = getArguments().getString(TYPE);
            mMode = (DialMode) getArguments().getSerializable(MODE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        int resId = R.layout.main_dialer;
        View root = inflater.inflate(resId, container, false);
        mUriDialing = root.findViewById(R.id.uri_dialing);
        configureButtons(root);

        mDelete = root.findViewById(R.id.delete);
        mDelete.setOnClickListener(v -> deleteDigit());
        mDelete.setOnLongClickListener(v -> {
            clear();
            return true;
        });

        root.findViewById(R.id.redialButton).setOnClickListener(v -> {
            String redialNumber = mSharedPref.getString(REDIAL_NUMBER, "");
            assert redialNumber != null;
            if (redialNumber.length() > 0) {
                mNumber = redialNumber;
                mDigitsView.setText(mNumber);
            }
        });

        if(isAdded()) {
            mRedialButton = root.findViewById(R.id.redialButton);
        }

        configureCallControls(root);

        configureRedialButton(root);

        mAudioButton = root.findViewById(R.id.audioButton);
        mVideoCall = root.findViewById(R.id.contact_item_call_video);
        mSubject = root.findViewById(R.id.subject_txt);
//Subject

        configureTransducerButtons(root);

        enableVideo(SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled());

        mAudioButton.setOnClickListener(v -> {
            String dialPattern;

            mSharedPref = Objects.requireNonNull(this.getActivity()).getSharedPreferences(REDIAL_NUMBER, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString("subject", mSubject.getText().toString());
            editor.apply();


            /*if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE))
            /*String dialPattern;
            if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE))
                dialPattern = IPO_AUDIO_DIAL_PATTERN;
            else
                dialPattern = AURA_AUDIO_DIAL_PATTERN;*/
            if (mNumber.length() > 0 /*&& mNumber.matches(dialPattern)*/) {
                doAction(mNumber, ACTION.AUDIO);
                GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_DIALER_EVENT);
            } else {
                mNumber.length();
                if (enableRedial) {
                    String redialNumber = mSharedPref.getString(REDIAL_NUMBER, "");
                    assert redialNumber != null;
                    if (redialNumber.length() > 0) {
                        mNumber = redialNumber;
                        mDigitsView.setText(mNumber);
                        if (mDelete != null) {
                            clockWrapper.setVisibility(mMode == DialMode.EDIT ? View.INVISIBLE : View.VISIBLE);
                            mDelete.setVisibility(mMode == DialMode.EDIT ? View.VISIBLE : View.INVISIBLE);
                            mNameView.setVisibility(mMode == DialMode.EDIT ? View.VISIBLE : View.INVISIBLE );
                            setRedialButtonVisibility(mMode != DialMode.EDIT);
                        }
                    }
                }
            }
        });

        setDisplayInternal(root);
//        mCurrentDate = SimpleDateFormat.getDateInstance(DateFormat.FULL).format(new Date());
//        mCurrentTime = DateUtils.formatDateTime(ElanApplication.getContext(), new Date().getTime(), DateUtils.FORMAT_SHOW_TIME);
//        updateDialerDisplay();
        if (mNameView != null) {
            mNameView.setOnClickListener(v -> {
                if (mNumber != null && mNumber.trim().length() > 0 && mAutoCompleteNumber != null) {
                    String number = mAutoCompleteNumber.replaceAll("\\D+", "");
                    mDigitsView.setText(number);
                    mNumber = number;

                    SDKManager.getInstance().getCallAdaptor().setDigitsDialed(mNumber);
                }
            });
        }
        //Custom
        mCSDLibraryView = root.findViewById(R.id.csdklibrary_lbl);
        mDeskphoneLibraryView = root.findViewById(R.id.dpslibrary_lbl);
        mAppVersionView = root.findViewById(R.id.appversion_lbl);

        mCSDLibraryView.setText(Client.getVersion());
        mDeskphoneLibraryView.setText(DeskPhoneServiceLibrary.getDeskPhoneServicesVersion());
        mAppVersionView.setText("App Version: " + BuildConfig.VERSION_NAME.toString() + "_aaadevbradcast");



        return root;
    }

    void configureButtons(View root){}

    void configureCallControls(View root){}

    void configureTransducerButtons(View root){}

    public void onHardKeyClick(String number){
        if (mMode == DialMode.OFF_HOOK) {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString(REDIAL_NUMBER, mNumber);
            editor.apply();
        }
        if(DialerFragment.this.mListener!=null) {
            boolean keepDigitAfterUsage = DialerFragment.this.mListener.onDialerInteraction(number, ACTION.DIGIT);
            if (!keepDigitAfterUsage) {
                deleteDigit();
            }
        }
    }

    /**
     * Removing digits from dialer
     */
    public void deleteDigit() {
        if (mNumber.length() > 0) {
            mNumber = mNumber.substring(0, mNumber.length() - 1);
            mDigitsView.setText(mNumber);
        }
        if (mNumber.length() == 0){
            isFirstDigitInDial = true;
        }
    }


    /**
     * Perform action on dialer
     *
     * @param number for which action is performed
     * @param action which is performed
     */
    private void doAction(String number, ACTION action) {
        if(mListener!=null) {
            mListener.onDialerInteraction(number, action);
            mNumber = "";
            mDigitsView.setText("");
        }
    }

    /**
     * Prepare display based on parameters provided
     *
     * @param root {@link View} for which data have to be set
     */
    private void setDisplayInternal(View root) {
        if (mDigitsView == null) {
            mDigitsView = root.findViewById(R.id.digits);
        }
        mDigitsView.setText(mNumber);
        if (mNameView == null) {
            mNameView = root.findViewById(R.id.name);
        }
        if (mName.length() > 0 && mType.length() > 0) {
            mNameView.setText(Html.fromHtml(getString(R.string.sample_dialer_display_name, mName, mType)));
        } else {
            mNameView.setText("");
        }
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String number) {
        if (mListener != null) {
            mListener.onDialerInteraction(number, ACTION.AUDIO);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDialerInteractionListener) {
            mListener = (OnDialerInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDialerInteractionListener");
        }
        try {
            mCallback = (OffHookTransduceButtonInterface) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "OnActiveCallInteractionListener cast failed");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (mDisplayTimer != null) {
            mDisplayTimer.cancel();
        }
        mDisplayTimer = null;
    }

    /**
     * Obtaining name to be redialed
     *
     * @return String representation of name to be redialed
     */
    String getRedialName() {
        return "Default Name";
    }

    /**
     * Processing configuration change
     */
    @Override
    public void applyConfigChange() {
        View root = getView();
        configureRedialButton(root);
        enableVideo(SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled());
    }
    boolean isFirstDigitInDial = true;
    /**
     * Get character from keyboard
     */

    abstract public void dialFromKeyboard(String number);

    /**
     * Processing message waiting status
     *
     * @param voiceMsgsAreWaiting boolean based on which we are showing or hiding voicemail button
     */
    @Override
    public void onMessageWaitingStatusChanged(boolean voiceMsgsAreWaiting) {
        Log.d(TAG, "onMessageWaitingStatusChanged " + voiceMsgsAreWaiting);
        mVoicemail.setChecked(voiceMsgsAreWaiting);
    }

    @Override
    public void onVoicemailNumberChanged(String voicemailNumber) {
        Log.d(TAG, "onVoicemailNumberChanged voicemailNumber="+voicemailNumber);
        if (voicemailNumber== null) {
            mVoicemail.setVisibility(View.INVISIBLE);
        }
        else {
            mVoicemail.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCallDigitCollectionCompleted(UICall call) {
        mMode = DialMode.EDIT;
    }


    public DialMode getMode() {
        return mMode;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDialerInteractionListener {
        boolean onDialerInteraction(String number, ACTION action);
    }

    /**
     * This method will be called every time Dialer fragment is active
     */
    public void fragmentSelected(Boolean voiceMail) {
        Log.e(TAG, "fragmentSelected: Dialer");
        onMessageWaitingStatusChanged(voiceMail);
    }

    private String getPhoneType(String type) {
        try {
            if ("WORK".equalsIgnoreCase(type)) {
                return Objects.requireNonNull(getContext()).getResources().getText(R.string.contact_details_work).toString();
            } else if ("MOBILE".equalsIgnoreCase(type)) {
                return Objects.requireNonNull(getContext()).getResources().getText(R.string.contact_details_mobile).toString();
            } else if ("HOME".equalsIgnoreCase(type)) {
                return Objects.requireNonNull(getContext()).getResources().getText(R.string.contact_details_home).toString();
            } else if ("HANDLE".equalsIgnoreCase(type)) {
                return Objects.requireNonNull(getContext()).getResources().getText(R.string.contact_details_handle).toString();
            } else if ("FAX".equalsIgnoreCase(type)) {
                return Objects.requireNonNull(getContext()).getResources().getText(R.string.contact_details_fax).toString();
            } else if ("PAGER".equalsIgnoreCase(type)) {
                return Objects.requireNonNull(getContext()).getResources().getText(R.string.contact_details_pager).toString();
            } else if ("ASSISTANT".equalsIgnoreCase(type)) {
                return Objects.requireNonNull(getContext()).getResources().getText(R.string.contact_details_assistant).toString();
            } else if ("OTHER".equalsIgnoreCase(type)) {
                return Objects.requireNonNull(getContext()).getResources().getText(R.string.contact_details_other).toString();
            }
        }catch (Exception e){
            return "";
        }
        return "";
    }

    /**
     * Setting up redial button
     *
     * @param root inflater
     */
    private void configureRedialButton(View root) {
        if (root != null) {
            enableRedial = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_REDIAL);
            ImageButton redial = root.findViewById(R.id.redialButton);
            if (redial != null) {
                redial.setEnabled(enableRedial);
                if(enableRedial)
                    redial.setVisibility(View.VISIBLE);
                else
                    redial.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Enabling video
     *
     * @param enable should we enable video
     */
    private void enableVideo(Boolean enable) {
        if (mVideoCall != null){
            mVideoCall.setEnabled(enable);
            if (enable) {
                mVideoCall.setAlpha(1f);
                mVideoCall.setOnClickListener(v -> {
                    mSharedPref = Objects.requireNonNull(this.getActivity()).getSharedPreferences(REDIAL_NUMBER, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = mSharedPref.edit();
                    editor.putString("subject", mSubject.getText().toString());
                    editor.apply();

                    if (mNumber.length() > 0 /*&& mNumber.matches(VIDEO_DIAL_PATTERN)*/) {
                        doAction(mNumber, ACTION.VIDEO);
                        GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_DIALER_EVENT);
                    } else {
                        mNumber.length();
                        if (enableRedial) {
                            String redialNumber = mSharedPref.getString(REDIAL_NUMBER, "");
                            assert redialNumber != null;
                            if (redialNumber.length() > 0) {
                                mNumber = redialNumber;
                                mDigitsView.setText(mNumber);
                                if (mMode == DialMode.EDIT) {
                                    clockWrapper.setVisibility(View.INVISIBLE);
                                    mDelete.setVisibility(View.VISIBLE);
                                    mNameView.setVisibility(View.VISIBLE);
                                    setRedialButtonVisibility(false);
                                }
                            }
                        }
                    }
                });
            } else {
                mVideoCall.setAlpha(0.5f);
                mVideoCall.setClickable(false);
            }
        }
        else {
            Log.e(TAG, "enableVideo : mVideoCall is null. Aborting.");
        }
    }

    /**
     * Setting up {@link DialMode}
     *
     * @param mode {@link DialMode} to be set
     */
    public void setMode(DialMode mode) {
        mMode = mode;
        mVideoCall.setEnabled(mMode == DialMode.EDIT);
        enableRedial = (mMode == DialMode.EDIT);
        View view = getView();
        if (view != null) {
            mAudioButton = view.findViewById(R.id.audioButton);
            mRedialButton = view.findViewById(R.id.redialButton);

            if (mAudioButton != null) {
                mAudioButton.setEnabled(mMode == DialMode.EDIT);
            }
            if (mRedialButton != null) {
                mRedialButton.setEnabled(mMode == DialMode.EDIT);
            }
            if (mDelete != null) {
                clockWrapper.setVisibility(mMode == DialMode.EDIT ?View.INVISIBLE: View.VISIBLE );
                mDelete.setVisibility(mMode == DialMode.EDIT ? View.VISIBLE : View.INVISIBLE);
                mNameView.setVisibility(mMode == DialMode.EDIT ? View.VISIBLE : View.INVISIBLE);
                setRedialButtonVisibility();
            }
        }
        if (mMode == DialMode.EDIT)
            clear();
        else {
            mDigitsView.setText("");
            isFirstDigitInDial = true;
        }
    }

    void setRedialButtonVisibility(){}

    /**
     * Clearing digits and number view on {@link DialerFragment}
     */
    public void clear() {
        mNumber = "";
        mDigitsView.setText("");
        isFirstDigitInDial = true;
    }

    public enum DialMode {OFF_HOOK, EDIT}

    public enum ACTION {AUDIO, VIDEO, REDIAL, DIGIT}

    /**
     * This Async Task will be used to get contact name by search query, and display it on TextView.
     * In case no contact is found under local contacts, we search by Enterprise contacts.
     */
    private static class GetPhoneNumberAsync extends AsyncTask<String, Void, String[]> {
        private final WeakReference<DialerFragment> mWeakFragment;
        private final boolean isFirstNameFirst;

        private GetPhoneNumberAsync(DialerFragment fragment) {
            mWeakFragment = new WeakReference<>(fragment);
            isFirstNameFirst = fragment.isFirstNameFirst;
        }

        @Override
        protected String[] doInBackground(String... strings) {
            String searchQuery = strings[0];

            // First search local contacts from cache
            List<ContactData> localContacts = new ArrayList<>(LocalContactsRepository.getInstance().getLocalContacts());
            for (ContactData contact : localContacts) {
                ContactData.PhoneNumber foundPhone = contact.findPhoneNumber(searchQuery);
                if (foundPhone != null) {
                    return new String[]{contact.getFormatedName(isFirstNameFirst), foundPhone.Number, foundPhone.Type.name()};
                }
            }

            // If no results try searching enterprise contacts from cache
            ConcurrentHashMap<String, ContactData> enterprisePhonesMap = EnterpriseContactsRepository.getInstance().getLookupEnterpriseContacts();
            for (String number : enterprisePhonesMap.keySet()) {
                if (number.replaceAll("[\\D]", "").startsWith(searchQuery)) {
                    ContactData contact = enterprisePhonesMap.get(number);
                    assert contact != null;
                    ContactData.PhoneNumber foundPhone = contact.findPhoneNumber(searchQuery);
                    if (foundPhone != null) {
                        return new String[]{contact.getFormatedName(isFirstNameFirst), number, foundPhone.Type.name()};
                    }
                }
            }

            // If no results return null
            return null;
        }

        @Override
        protected void onPostExecute(String[] searchResults) {
            super.onPostExecute(searchResults);
            DialerFragment fragment = mWeakFragment.get();
            if (fragment == null || !(fragment.isAdded())) {
                return;
            }

            if (fragment.mMode != DialMode.OFF_HOOK
                    && searchResults != null
                    && fragment.mNumber != null
                    && fragment.mNumber.length() > 0) {
                fragment.clockWrapper.setVisibility(View.INVISIBLE);
                fragment.mNameView.setVisibility(View.VISIBLE);
                fragment.mAutoCompleteNumber = searchResults[1];
                fragment.mNameView.setText(Html.fromHtml(fragment.getString(
                        R.string.sample_dialer_display_name, searchResults[0], fragment.getPhoneType(searchResults[2]))));
            } else {
                fragment.mNameView.setText("");
            }
        }
    }

    /**
     * Displays the number in the digits view and contact name if matched
     * @param number number to be called
     */
    public void setDialer(String number) {
        mNumber = number;
        mDigitsView.setText(mNumber);
    }

    /**
     * Sets visibility of the VideoCall button and adjust the look of
     * the audio button accordingly
     * @param visibility View visibility
     */
    public void setVideoButtonVisibility(int visibility){
        mVideoCall.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            mAudioButton.setImageResource(R.drawable.dialer_audio);
        }else if (visibility == View.GONE) {
            mAudioButton.setImageResource(R.drawable.dialer_audio_center);
        }
    }

    private static class TextScrollRunnable implements Runnable {
        final WeakReference<HorizontalScrollView> mTextScrollRef;

        private TextScrollRunnable(HorizontalScrollView textScroll) {
            this.mTextScrollRef = new WeakReference<>(textScroll);
        }

        @Override
        public void run() {
            if(mTextScrollRef.get() != null)
                mTextScrollRef.get().fullScroll(HorizontalScrollView.FOCUS_RIGHT);
        }
    }

    private static class DateUpdateRunnable implements Runnable {
        final WeakReference<TextView> dateUndwerClockRef;
        final WeakReference<Handler> handlerRef;

        private DateUpdateRunnable(TextView dateUndwerClock, Handler handler) {
            dateUndwerClockRef = new WeakReference<>(dateUndwerClock);
            handlerRef = new WeakReference<>(handler);
        }

        @Override
        public void run() {
            {
                if(dateUndwerClockRef.get() != null && handlerRef.get() != null) {
                    dateUndwerClockRef.get().setText(SimpleDateFormat.getDateInstance(DateFormat.FULL).format(new Date()));
                    handlerRef.get().postDelayed(new DateUpdateRunnable(dateUndwerClockRef.get(), handlerRef.get()), PERIOD);
                }
            }
        }
    }

    public String getNumber(){
        return mNumber;
    }
}
