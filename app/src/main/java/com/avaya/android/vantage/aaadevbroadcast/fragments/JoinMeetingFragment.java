package com.avaya.android.vantage.aaadevbroadcast.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.UIUnifiedPortalViewAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.views.interfaces.IUnifiedPortalViewInterface;


public class JoinMeetingFragment extends Fragment implements IUnifiedPortalViewInterface {

    public static final String JOIN_MEETING_FRAGMENT = "JoinMeetingFragment";

    private EditText mName;
    private EditText mMeetingId;
    private EditText mMeetingAddress;
    private Button   mJoinMeetingButton;
    private TextView mBackButton;
    //private CheckBox mCallMeBack;
    //private View mCallMeBackLayout;
    //private EditText mCallMeBackNumber;

    protected TextWatcher mJoinMeetingTextWatcher=null;

    UIUnifiedPortalViewAdaptor mUnifiedPortalViewAdaptor;

    public JoinMeetingFragment() {
        // Required empty public constructor
    }

    @NonNull
    public static JoinMeetingFragment newInstance() {
        return new JoinMeetingFragment();
    }


    public void setMeetingParameters(String meetingName, String meetingId, String meetingAddress){
        if (mName != null && mMeetingId != null && mMeetingAddress != null){
            mName.setText(meetingName);
            mMeetingId.setText(meetingId);
            mMeetingAddress.setText(meetingAddress);
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUnifiedPortalViewAdaptor = new UIUnifiedPortalViewAdaptor();
        SDKManager.getInstance().getUnifiedPortalAdaptor().registerListener(mUnifiedPortalViewAdaptor);
        mUnifiedPortalViewAdaptor.setDeviceViewInterface(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_join_meeting, container, false);

        mName = view.findViewById(R.id.name);

        mMeetingId = view.findViewById(R.id.meeting_id);
        mMeetingAddress = view.findViewById(R.id.meeting_address);
        mMeetingAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String meetingAddress = mMeetingAddress.getText().toString();
                String meetingId;
                if (meetingAddress.contains("/?ID=")){
                    meetingId=meetingAddress.substring(meetingAddress.indexOf("/?ID=")+5);
                    if (meetingId.contains("/"))
                        meetingId=meetingId.substring(0,meetingId.indexOf("/"));
                    mMeetingId.setText(meetingId);
                }
                ShouldJoinMeetingButtonBeEnabled();
            }
        });
        mJoinMeetingButton = view.findViewById(R.id.join_meeting);
        mJoinMeetingButton.setOnClickListener(view1 ->{
            if (getActivity() != null)
                Utils.hideKeyboard(getActivity());
            joinMeeting();
        });
        mBackButton = view.findViewById(R.id.back);
        mBackButton.setOnClickListener(view1 -> onBackPressed());

/*      Call Me Back feature is a future requirement. It will be implemented in later phase, so for now, the code is commented.

        mCallMeBack = view.findViewById(R.id.call_me_back);
        mCallMeBackLayout = view.findViewById(R.id.call_me_back_layout);
        mCallMeBack.setOnClickListener(view1 ->{
            if (mCallMeBack.isChecked()){
                mCallMeBackLayout.setVisibility(View.VISIBLE);
            }
            else{
                mCallMeBackLayout.setVisibility(View.GONE);
            }

        });
        mCallMeBackNumber = view.findViewById(R.id.call_me_back_number);
*/

        return view;
    }

    public void onBackPressed(){
        if (getActivity() != null)
            Utils.hideKeyboard(getActivity());
        View view = getView();
        if (view != null){
            view.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mJoinMeetingTextWatcher = checkRequiredButtonsState();
        mMeetingId.addTextChangedListener(mJoinMeetingTextWatcher);
        mName.addTextChangedListener(mJoinMeetingTextWatcher);

        view.setVisibility(View.GONE);
        Utils.hideKeyboard(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        String sipusername = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.SIP_USER_DISPLAY_NAME);
        if (mName.getText().toString().isEmpty())
            mName.setText(sipusername);
    }

    public void joinMeeting(String meetingAddress, String meetingId, String meetingName, String callMeBackNumber){
        createLoadingSpinnerDialog();
        mUnifiedPortalViewAdaptor.requestToJoinMeeting(meetingAddress, meetingId, meetingName, false, callMeBackNumber, "");
    }

    private void joinMeeting(){
        if (mName.getText().toString().isEmpty() || mMeetingAddress.getText().toString().isEmpty() || mMeetingId.getText().toString().isEmpty() ){
            return;
        }

        //String callMeBackNumber = (mCallMeBack.isChecked() && mCallMeBackNumber != null) ?  mCallMeBackNumber.getText().toString() : "";

        joinMeeting(mMeetingAddress.getText().toString(), mMeetingId.getText().toString(), mName.getText().toString(), "");
    }

    private DialogFragment spinnerDialog;

    private void createLoadingSpinnerDialog() {
        final FragmentManager fm = getFragmentManager();
        if (fm != null){
            spinnerDialog = SpinnerFragment.newInstance();
            spinnerDialog.show(fm, "SpinnerFragment");
            spinnerDialog.setCancelable(false);
        }

    }

    private void dismissLoadingSpinnerDialog() {
        if (spinnerDialog != null && spinnerDialog.isAdded()) {
            spinnerDialog.dismissAllowingStateLoss();
            spinnerDialog = null;
        }
    }


    @Override
    public void handleJoinMeetingError(int message) {

        dismissLoadingSpinnerDialog();

        final JoinMeetingErrorFragment errorFragment = JoinMeetingErrorFragment.newInstance();
        errorFragment.show(getFragmentManager(), "JoinMeetingErrorFragment");

        //@StringRes int title=0;

//        switch (message) {
//            case R.string.ups_error_virtal_room_not_found:
//            case R.string.ups_error_virtal_room_disabled:
//                //title = R.string.ups_error_title_meeting_room_problem;
//                break;
//            case R.string.ups_error_tenant_not_found:
//                //title = R.string.ups_error_title_meeting_problem;
//                break;
//            default:
//                break;
//        }
        String errorMessage = getActivity().getString(message);
        //String errorTitle = getActivity().getString(title);
        errorFragment.setErrorText(errorMessage);
        //joinMeetingFragment.setErrorTitleText(errorTitle);
    }

    @Override
    public void handleJoinMeetingSuccess() {
        dismissLoadingSpinnerDialog();
    }

    private TextWatcher checkRequiredButtonsState(){
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                ShouldJoinMeetingButtonBeEnabled();
            }
        };
    }

    private void ShouldJoinMeetingButtonBeEnabled() {
        if (mName != null && !mName.getText().toString().isEmpty() &&
                mMeetingAddress != null && !mMeetingAddress.getText().toString().isEmpty() &&
                mMeetingId != null && !mMeetingId.getText().toString().isEmpty())
            mJoinMeetingButton.setEnabled(true);
        else {
            if (mJoinMeetingButton != null)
                mJoinMeetingButton.setEnabled(false);
        }
    }

}
