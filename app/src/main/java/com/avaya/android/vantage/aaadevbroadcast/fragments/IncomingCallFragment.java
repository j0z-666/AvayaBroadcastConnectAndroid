package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.BuildConfig;
import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.PhotoLoadUtility;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.RingerService;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.UICallViewAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.EnterpriseContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.contacts.LocalContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.model.UIAudioDevice;
import com.avaya.android.vantage.aaadevbroadcast.model.UICall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.POWER_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 */
public class IncomingCallFragment extends DialogFragment {

    private static final String TAG = "IncomingCallFragment";
    private static final int DIALOG_POSITION = 64;
    private View mView;
    //Context mContext;
    private UICallViewAdaptor mCallViewAdaptor = null;
    private final Map<Integer, View> map = new HashMap<>();
    private PowerManager.WakeLock mScreenLock;
    private boolean mIsDismissed=false;

    public IncomingCallFragment() {
    }

    public void init(UICallViewAdaptor callViewAdaptor) {
        //mContext=context;
        mCallViewAdaptor=callViewAdaptor;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(Objects.requireNonNull(getActivity()), getTheme()){
            @Override
            public void onBackPressed() {
                //do nothing if back was pressed during incoming call
                return;
            }
        };
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.incoming_call_list_layout, container, false);

        IncomingCallInteraction mCallBack = (IncomingCallInteraction) getActivity();

        if ( getDialog().getWindow() != null ) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            getDialog().setCanceledOnTouchOutside(false);
            WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.y = DIALOG_POSITION;
            getDialog().getWindow().setAttributes(params);
        }
        assert mCallBack != null;
        mCallBack.onIncomingCallStarted();

        PowerManager pm = ((PowerManager) Objects.requireNonNull(getContext()).getSystemService(POWER_SERVICE));
        if (!pm.isInteractive()) {
            mScreenLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, BuildConfig.APPLICATION_ID + ":" + TAG);
            mScreenLock.acquire(10*60*1000L /*10 minutes*/);
        }

        setupFullscreen();

        return mView;
    }

    public boolean isDismissed() {
       return mIsDismissed;
    }

    /**
     * Sets IncomingCallView child elements with the values form the current call:
     * name, number, contact image
     * @param view View Incoming Call View object
     * @param call Current UICall object
     */
    private void setParameters(View view, UICall call){

        TextView incomingName = view.findViewById(R.id.incoming_dialog_name);
        TextView incomingNumber = view.findViewById(R.id.incoming_dialog_number);
        TextView incomingSubject = view.findViewById(R.id.incoming_dialog_subject);

        String contactName = "";
        ContactData contactData = LocalContactsRepository.getInstance().getByPhone(call.getRemoteNumber());
        if (contactData == null) {
            contactData = EnterpriseContactsRepository.getInstance().getByPhone(call.getRemoteNumber());
        }
        if (contactData != null) {
            contactName = contactData.getFormatedName(ContactsFragment.isFirstNameFirst());
        }

        if (mCallViewAdaptor.isCMConferenceCall(call.getCallId()) || contactName.trim().length() == 0) {
            contactName = Utils.getContactName(call.getRemoteNumber(), mCallViewAdaptor.getRemoteDisplayName(call.getCallId()),
                                                mCallViewAdaptor.isCMConferenceCall(call.getCallId()), mCallViewAdaptor.isConferenceCall(call.getCallId()));
        }

        if (incomingName != null)
            incomingName.setText(contactName);
        if (incomingNumber !=null)
            incomingNumber.setText(call.getRemoteNumber());
        if (incomingSubject !=null)
            incomingSubject.setText(call.getRemoteSubject());





        boolean isVideo =  call.isVideo() && SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled();
        setListeners(view, call.getCallId(), isVideo);

        ImageView incomingImage = view.findViewById(R.id.incoming_dialog_image);
        if (mCallViewAdaptor.isCMConferenceCall(call.getCallId())) {
            incomingImage.setBackgroundResource(R.drawable.ic_common_avatar_group_124);
        } else if (contactData != null) {
            PhotoLoadUtility.setThumbnail(contactData, incomingImage);
        } else {
            incomingImage.setBackgroundResource(R.drawable.ic_avatar_generic);
        }
    }

    /**
     * Implements on click listener for the accept and reject buttons of the
     * Incoming Call View
     * @param view Incoming Call View
     * @param callId ID of the current call
     * @param isVideo true if this is a video call
     */
    private void setListeners(View view, final int callId, boolean isVideo) {
        View reject = view.findViewById(R.id.reject_call);
        reject.setOnClickListener(view13 -> {
            Log.i(TAG, "Dismiss activity");
            mCallViewAdaptor.denyCall(callId);
            removeCall(callId);
        });
        View accept = view.findViewById(R.id.accept_audio);
        accept.setOnClickListener(view12 -> {
            Log.i(TAG, "Activate another call");
            setSharedPrefDevice();
            mCallViewAdaptor.acceptCall(callId, false);
            removeCall(callId);
        });

        if (isVideo) {
            View acceptVideo = view.findViewById(R.id.accept_video);
            acceptVideo.setOnClickListener(view1 -> {
                Log.i(TAG, "Activate another call");
                setSharedPrefDevice();
                mCallViewAdaptor.acceptCall(callId, true);
                removeCall(callId);
            });
        }
    }

    private void setSharedPrefDevice(){
        String device = Objects.requireNonNull(ElanApplication.getContext()).getSharedPreferences("selectedAudioOption", Context.MODE_PRIVATE).getString(Constants.AUDIO_PREF_KEY, (UIAudioDevice.SPEAKER).toString());
        assert device != null;
        UIAudioDevice prefDevice = UIAudioDevice.valueOf(device.toUpperCase());
        SDKManager.getInstance().getAudioDeviceAdaptor().setUserRequestedDevice(prefDevice);
    }

    /**
     * Accepts the specified Call and removes it from the list of incoming calls
     * @param callId ID of the call
     */
    public void acceptAudioCall(int callId){
        if (map.get(callId) != null) {
            mCallViewAdaptor.acceptCall(callId, false);
            removeCall(callId);
        }
    }

    /**
     * Removes the specified call from the map of the incoming calls,
     * Stops playing the ringtone
     * @param callId id of the Call to be removed
     */
    synchronized public void removeCall(int callId){

        Log.d(TAG, "remove call " + callId);
        View view = map.get(callId);
        if (view ==null) {
            return;
        }
        view.setVisibility(View.GONE);
        map.remove(callId);
        if (map.isEmpty()) {
            if (mView != null) {
                mIsDismissed = true;
                dismissAllowingStateLoss();
            }
            stopPlayRingtone();
        }

        FragmentManager fragmentManager = getFragmentManager();
        assert fragmentManager != null;
        List<Fragment> fragments = fragmentManager.getFragments();
        for(Fragment fragment : fragments){
            if(fragment != null && fragment.isVisible() && fragment instanceof VideoCallFragment ) {
                if(getActivity() !=null) {
                    ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(true);
                }
             }
        }

    }

    /**
     * If the specified call is still valid and actual incoming call,
     * it'll be assigned the IncomingCallView and be added in to the map
     * of incoming calls.
     *
     * @param call {@link UICall} object of the new incoming call.
     */
    synchronized public void addCall(UICall call){
        Log.d(TAG, "add call " + call.getCallId());

        closeSystemDialogs();

        if (map.get(call.getCallId()) != null) {
            Log.i(TAG, "Call already exists");
            return;
        }

        // prevent addition of the call that was ended meanwhile
        if (SDKManager.getInstance().getCallAdaptor().getCall(call.getCallId()) == null) {
            Log.i(TAG, "Call already ended");
            return;
        }

        boolean isVideo =  call.isVideo() && SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled();
        View view = getIncomingView(isVideo);

        if (map.isEmpty()){
            startPlayRingtone();
        }
        view.setVisibility(View.VISIBLE);
        setParameters(view, call);
        map.put(call.getCallId(), view);
    }

    /**
     * This is broadcast when a user action should request a temporary system dialog to dismiss. Some examples of temporary system dialogs are the notification window-shade and the recent tasks dialog.
     */
    private void closeSystemDialogs() {
        Intent closeSystemDialogsIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        Activity activity = getActivity();
        if (activity != null){
            activity.sendBroadcast(closeSystemDialogsIntent);
        }
    }

    /**
     * The Fragment can accommodate up to two incoming call views. This method
     * returns the first or the second View object (or their variation
     * if this is a video call) depending on the current status of incoming calls.
     * @param isVideo true if this is a video call
     * @return View object that for the incoming call dialog
     */
    private View getIncomingView(boolean isVideo){

        View incomingVideo2 = mView.findViewById(R.id.incoming_video2);
        View incomingAudio2 = mView.findViewById(R.id.incoming2);

        if (map.isEmpty() || incomingVideo2.getVisibility() == View.VISIBLE || incomingAudio2.getVisibility() == View.VISIBLE){
            if (isVideo)
                return mView.findViewById(R.id.incoming_video1);
            else
                return mView.findViewById(R.id.incoming1);
        }
        else {
            if (isVideo)
                return incomingVideo2;
            else
                return incomingAudio2;
        }

    }

    /**
     * Change screen params to fullscreen preferences.
     */
    private void setupFullscreen() {
        if (mView != null) {
            mView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    /**
     * Stops playing the ringtone
     */
    private void stopPlayRingtone(){
        if (getContext() == null)
            return;
        getContext().stopService(new Intent(getContext(), RingerService.class));
    }

    /**
     * Starts playing the ringtone
     */
    private void startPlayRingtone(){
        // loading admin ringtone settings
        if (getContext() == null)
            return;
        getContext().startService(new Intent(getContext(), RingerService.class));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(mScreenLock != null && mScreenLock.isHeld()) {
            mScreenLock.release();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (!map.isEmpty()) {
            Log.d(TAG, "onDetach map is NOT empty");
            stopPlayRingtone();
        }
    }

    /**
     * Updates the corresponding Incoming call View with the remote contact name.
     * @param call {@link UICall} reference
     * @param newDisplayName String. Name of teh remote contact.
     */
    public void setNewRemoteName(UICall call, final String newDisplayName) {

        if (call == null)
            return;

        View view = map.get(call.getCallId());
        if (view == null)
            return;

        setParameters(view, call);
    }

    public interface IncomingCallInteraction {
        void onIncomingCallEnded();
        void onIncomingCallStarted();
    }

    public void rejectIncomingCall(int callId) {
        mCallViewAdaptor.denyCall(callId);
        removeCall(callId);
    }
}
