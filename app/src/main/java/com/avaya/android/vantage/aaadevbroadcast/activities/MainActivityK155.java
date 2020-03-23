package com.avaya.android.vantage.aaadevbroadcast.activities;

import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.annotation.NonNull;

import com.avaya.android.vantage.aaadevbroadcast.views.DialogAAADEVMessages;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;
import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.callshistory.CallHistoryFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.csdk.CallAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ActiveCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.CallStatusFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactDetailsFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactEditFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.DialerFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.VideoCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.JoinMeetingFragment;
import com.avaya.android.vantage.aaadevbroadcast.model.UIAudioDevice;
import com.avaya.android.vantage.aaadevbroadcast.model.UICallState;
import com.avaya.android.vantage.aaadevbroadcast.views.adapters.CallStateEventHandler;
import com.avaya.android.vantage.aaadevbroadcast.views.interfaces.IHardButtonListener;
import com.avaya.deskphoneservices.HardButtonType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.avaya.android.vantage.aaadevbroadcast.Constants.DigitKeys;
import static com.avaya.android.vantage.aaadevbroadcast.model.UICallState.ESTABLISHED;
import static com.avaya.android.vantage.aaadevbroadcast.model.UICallState.FAILED;
import static com.avaya.android.vantage.aaadevbroadcast.model.UICallState.REMOTE_ALERTING;
import static com.avaya.android.vantage.aaadevbroadcast.model.UICallState.TRANSFERRING;


public class MainActivityK155 extends BaseActivity implements IHardButtonListener {

    private static final String TAG = "MainActivityK155";
    private RelativeLayout tabOne;
    private RelativeLayout tabSelectorWrapper;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);

        Utils.overrideFontScaleAndDensityK155(this);
    }


    /**
     * key up event received from platform via MEDIA_BUTTON intent
     *
     * @param hardButton
     */
    @Override
    public void onKeyUp(@NonNull HardButtonType hardButton) {
        //test for K155 special buttons:
        try {

            UIAudioDevice activeAudioDevice = mAudioDeviceViewAdaptor.getActiveAudioDevice();
            int activeCallId = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
            ToggleButton dialerOffHook = mSectionsPagerAdapter.getDialerFragment().offHook;

            KeyguardManager kgMgr = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            boolean isLocked = (kgMgr != null) && kgMgr.isDeviceLocked() && !ElanApplication.isPinAppLock;

            switch (hardButton) {

                case AUDIO_MUTE://video mute
                    if (mAudioMute.isEnabled()) {
                        Log.d(TAG, "PHYSICAL KEY AUDIO MUTE");
                        mAudioMute.performClick();
                    }
                    break;
                case VIDEO_MUTE://video mute
                    if (mVideoMute.isEnabled()) {
                        Log.d(TAG, "PHYSICAL KEY VIDEO MUTE");
                        mVideoMute.performClick();
                    }
                    break;
                case SPEAKER://speaker

                    Log.d(TAG, "PHYSICAL KEY SPEAKER HOOK");

                    if (isLocked && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0)) {
                        Log.v(TAG, "cancel off hook on lock");
                        return;
                    }

                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.SPEAKER);
                    updateAudioSelectionUI(UIAudioDevice.SPEAKER);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.SPEAKER.toString());

                    if (!dialerOffHook.isChecked() && (activeCallId == 0)) {
                        if (SDKManager.getInstance().getCallAdaptor().isAlertingCall() == 0) {
                            prepareOffHook();
                        }
                        try {
                            if (isFragmentVisible(DIALER_FRAGMENT)) {
                                ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.performClick();
                                changeUiForFullScreenInLandscape(false);
                                if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                    ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else if ((activeAudioDevice == UIAudioDevice.SPEAKER)) {


                        if (activeCallId > 0) {
                            SDKManager.getInstance().getCallAdaptor().endCall(activeCallId);
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT) && !isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)) {
                                    changeUiForFullScreenInLandscape(false);
                                    if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                        ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT))
                                    ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).setMode(DialerFragment.DialMode.EDIT);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        } else if (dialerOffHook.isChecked()) {
                            resetDialer();
                            mSectionsPagerAdapter.getDialerFragment().offHook.performClick();
                        }
                    } else {
                        Log.w(TAG, "onKeyUp SPEAKER: unexpected state activeCallId=" + activeCallId + " activeAudioDevice=" + activeAudioDevice + " dialerOffHook.isChecked()=" + dialerOffHook.isChecked());
                    }

                    break;
                case HEADSET://transducer

                    Log.d(TAG, "PHYSICAL KEY TRANSDUCER SELECTION");

                    if (isLocked && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0)) {
                        Log.v(TAG, "cancel off hook on lock");
                        return;
                    }

                    int device = getHeadsetByPriority();
                    String prefValue = mSharedPref.getString(Constants.AUDIO_PREF_KEY, (UIAudioDevice.SPEAKER).toString());
                    assert prefValue != null;
                    UIAudioDevice savedDevice = UIAudioDevice.valueOf(prefValue.toUpperCase());
                    List<UIAudioDevice> devices = Arrays.asList(UIAudioDevice.BLUETOOTH_HEADSET, UIAudioDevice.RJ9_HEADSET, UIAudioDevice.WIRED_HEADSET, UIAudioDevice.WIRED_USB_HEADSET);
                    if (devices.contains(savedDevice)) {
                        device = savedDevice.getUIId();
                    }
                    if (!dialerOffHook.isChecked() && (activeCallId == 0)) {
                        prepareOffHook();
                        this.onClick(findViewById(device));
                        try {
                            if (isFragmentVisible(DIALER_FRAGMENT)) {
                                ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.performClick();
                                changeUiForFullScreenInLandscape(false);
                                if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                    ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else if (dialerOffHook.isChecked() && (activeAudioDevice != UIAudioDevice.SPEAKER) && (activeAudioDevice != UIAudioDevice.HANDSET) && (activeAudioDevice != UIAudioDevice.WIRELESS_HANDSET)) { //there is active call via Headset
                        if (activeCallId > 0) {
                            SDKManager.getInstance().getCallAdaptor().endCall(activeCallId);
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT)) {
                                    changeUiForFullScreenInLandscape(false);
                                    if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                        ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        } else {
                            resetDialer();
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT)) {
                                    ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.performClick();
                                    changeUiForFullScreenInLandscape(false);
                                    if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                        ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (activeCallId > 0 && (activeAudioDevice == UIAudioDevice.SPEAKER || activeAudioDevice == UIAudioDevice.HANDSET || activeAudioDevice == UIAudioDevice.WIRELESS_HANDSET)) { // there is an active call on speaker and headset was pressed
                        View transientView = new View(this);
                        transientView.setId(device);
                        onClick(transientView);
                    } else if (activeCallId > 0 && SDKManager.getInstance().getCallAdaptor().getCall(activeCallId).getState() == UICallState.NOT_RELEVANT) {
                        SDKManager.getInstance().getCallAdaptor().endCall(activeCallId);
                    } else {
                        Log.w(TAG, "onKeyUp HEADSET: unexpected state activeCallId=" + activeCallId + " activeAudioDevice=" + activeAudioDevice + " dialerOffHook.isChecked()=" + dialerOffHook.isChecked());
                    }

                    break;
            }
        }catch (NullPointerException e){
            Log.e(TAG, "NPE in onKeyUp(@NonNull HardButtonType hardButton):", e);
        }
    }
    @NonNull
    private int getHeadsetByPriority() {
        /*Bluetooth headset (if paired and connected)
        USB wired headset (if connected)
        3.5mm wired headset (if connected)
        RJ9 headset (connection state is a don't care)*/
        int device = R.id.containerHeadset;
        for (UIAudioDevice uiAudioDevice : mAudioDeviceViewAdaptor.getAudioDeviceList()) {

            if (uiAudioDevice == UIAudioDevice.BLUETOOTH_HEADSET) {
                device = R.id.containerBTHeadset;
                break;
            }
            if (uiAudioDevice == UIAudioDevice.WIRED_USB_HEADSET) {
                device = R.id.containerUsbHeadset;
            }
            if ((device != R.id.containerUsbHeadset) && (uiAudioDevice == UIAudioDevice.WIRED_HEADSET)) {
                device = R.id.container35Headset;
            }


        }

        return device;
    }

    /**
     * key down event received from platform via MEDIA_BUTTON intent
     *
     * @param hardButton
     */
    @Override
    public void onKeyDown(@NonNull HardButtonType hardButton) {
        if (!ElanApplication.isMainActivityVisible()) {

            Intent intent = new Intent(this, MainActivityK155.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(MainActivityK155.BRING_TO_FOREGROUND_INTENT);
            intent.putExtra(HARD_BUTTON, hardButton);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "failed to activate MainActivity from pending intent while it was not visible");
            }
            setIntent(null);
        }
    }


    @Override
    void tabLayoutAddOnTabSelectedListener(){
        Log.d(TAG, "tabLayoutAddOnTabSelectedListener");

            mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    Log.v("TabTest", "onTabSelected");
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    Log.v("TabTest", "onTabUnselected");
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    contactsTabFilterMenuListnerSetup(tabSelectorWrapper);
                }

            });
            mVideoMute.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void initMoreViews() {
        tabOne = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        searchButton = findViewById(R.id.search_button);
        addcontactButton = findViewById(R.id.addcontact_button);
        filterButton = findViewById(R.id.filterRecent);
        filterButton.setImageResource(R.drawable.ic_expand_more);
    }

    /**
     * Modifies the UI to adopt screen orientation
     * @param show
     */
    @Override
    public void changeUiForFullScreenInLandscape(boolean show){
        try {
                if (show) {
                    mBrandView.setVisibility(View.GONE);
                    mUser.setVisibility(View.GONE);
                    mStatusLayout.setVisibility(View.GONE);
                    mOpenUser.setVisibility(View.GONE);
                    mErrorStatus.setVisibility(View.GONE);
                    appBar.setVisibility(View.INVISIBLE);
                    appBar.getLayoutParams().height = 0;
                    mTabLayout.setVisibility(View.GONE);
                    mTabLayout.getLayoutParams().height = 0;
                    dialerView.setVisibility(View.GONE);
                    mViewPager.getLayoutParams().height = 675;
                } else {
                    mBrandView.setVisibility(View.VISIBLE);
                    mUser.setVisibility(View.VISIBLE);
                    mStatusLayout.setVisibility(View.VISIBLE);
                    mOpenUser.setVisibility(View.VISIBLE);
                    mErrorStatus.setVisibility(View.VISIBLE);
                    appBar.setVisibility(View.VISIBLE);
                    appBar.getLayoutParams().height = 100;
                    mTabLayout.setVisibility(View.VISIBLE);
                    mTabLayout.getLayoutParams().height = 100;
                    dialerView.setVisibility(View.GONE);
                    mViewPager.getLayoutParams().height = 430;

                }
            checkForErrors();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void initViewPager() {
        mUIDeskphoneServiceAdaptor.setHardButtonListener(this);
        super.initViewPager();
    }

    protected void onPageScrolledLogic(){
        if  (mTabLayout.getVisibility()==View.GONE && (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) || isFragmentVisible(CONTACTS_EDIT_FRAGMENT)) )
            mViewPager.setEnabledSwipe(false);
        else
            mViewPager.setEnabledSwipe(true);
    }

    protected void onPageSelectedCondition() {
        if (mSectionsPagerAdapter.getFragmentContacts() != null && mSectionsPagerAdapter.getFragmentContacts().mSearchView != null && mSectionsPagerAdapter.getFragmentContacts().mSearchView.getVisibility() == View.VISIBLE) {
            if (isFragmentVisible(CONTACTS_FRAGMENT) && !isFragmentVisible(CONTACTS_EDIT_FRAGMENT))
                mSectionsPagerAdapter.getFragmentContacts().removeSearchResults();
        }
    }

    @Override
    public void changeButtonsVisibility(Tabs tab){
        switch (tab) {
            case Dialer:
            case Favorites:
                searchButton.setVisibility(View.INVISIBLE);
                addcontactButton.setVisibility(View.INVISIBLE);
                filterButton.setVisibility(View.INVISIBLE);
                return;
            case Contacts:
                searchButton.setVisibility(View.VISIBLE);
                setAddContactVisibility();
                filterButton.setVisibility(View.INVISIBLE);
                return;
            case History:
                searchButton.setVisibility(View.INVISIBLE);
                addcontactButton.setVisibility(View.INVISIBLE);
                filterButton.setVisibility(View.VISIBLE);
                return;
        }
    }


    @Override
    void setupMoreOnClickListenersForDevice(){
        addcontactButton.setOnClickListener(this);
        filterButton.setOnClickListener(this);
        searchButton.setOnClickListener(this);
    }

    void fullScreenViewResizeLogic(int startDimension){
        boolean isToResize = false;
        FragmentManager fragmentManager = MainActivityK155.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for(Fragment fragment : fragments){
            if(fragment != null && fragment.isVisible() && fragment instanceof ContactDetailsFragment ) {
                isToResize = true;
                break;
            }else if ( fragment != null && fragment.isVisible() && fragment instanceof ContactsFragment){
                if( ((ContactsFragment) fragment).isSearchLayoutVisible() ){
                    isToResize = true;
                    break;
                }
            }
        }
        if(!isToResize) {
            if(mViewPager!=null && mViewPager.getLayoutParams()!=null)
                mViewPager.getLayoutParams().height = 430;
        }
    }

    int onKeyDownDeviceLogic(int keyCode, KeyEvent event){
        if (!isToLockPressButton) {
        try {
            if(isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)||isFragmentVisible(CONTACTS_EDIT_FRAGMENT) || isFragmentVisible(JOIN_MEETING_FRAGMENT)){
                isToBlockBakcPress = true;
            }

            isToLockPressButton = true;
            int keyunicode = event.getUnicodeChar(event.getMetaState());
            char character = (char) keyunicode;
            String digit = "" + character;

            if(keyCode == KeyEvent.KEYCODE_BACK && !isLockState(this)) {


                if (mSelectPhoneNumber.getVisibility() == View.VISIBLE) {
                        mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber);
                }

                try {
                    if (((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mMoreCallFeatures.getVisibility() == View.VISIBLE) {
                        ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mMoreCallFeaturesClick();
                        return 1;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

                mViewPager.setEnabledSwipe(true);
                if (!isFragmentVisible(ACTIVE_CALL_FRAGMENT) && !isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)) {
                    changeUiForFullScreenInLandscape(false);


                    if (isFragmentVisible(CONTACTS_EDIT_FRAGMENT)) {
                        ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).cancelOnClickListener();
                        //changeUiForFullScreenInLandscape(true);
                    } else if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)) {
                        ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).mDeletePopUp.setVisibility(View.GONE);
                        ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).mBackListener.back();
                        changeUiForFullScreenInLandscape(false);
                    } else if (isFragmentVisible(JOIN_MEETING_FRAGMENT)){
                        ((JoinMeetingFragment) getVisibleFragment(JOIN_MEETING_FRAGMENT)).onBackPressed();
                    }

                    if(isFragmentVisible(CONTACTS_FRAGMENT) && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).isSearchLayoutVisible()){

                        ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).removeSearchResults();

                        CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                        assert callStatusFragment != null;
                        if( (Objects.requireNonNull(callStatusFragment.getView()).getVisibility()==View.INVISIBLE || callStatusFragment.getView().getVisibility()==View.GONE) && SDKManager.getInstance().getCallAdaptor().hasActiveHeldOrInitiatingCall()) {
                            callStatusFragment.showCallStatus();
                        }
                        return 0;
                    }
                }else if (mCallViewAdaptor.getNumOfCalls() < CallAdaptor.MAX_NUM_CALLS){
                    ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mBackArrowOnClickListener();
                    //changeUiForFullScreenInLandscape(false);
                    return 1;
                }
            }


            if(isFragmentVisible(DIALER_FRAGMENT) && mSectionsPagerAdapter!=null && mSectionsPagerAdapter.getDialerFragment()!=null ) {
                if (digit.matches("[\\d]") || digit.matches("#") || digit.matches("\\*")) {
                    if(!isFragmentVisible(CONTACTS_EDIT_FRAGMENT) && !isFragmentVisible(JOIN_MEETING_FRAGMENT))
                        mSectionsPagerAdapter.getDialerFragment().dialFromKeyboard(digit);
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    mSectionsPagerAdapter.getDialerFragment().deleteDigit();
                }
            }

            if (DigitKeys.contains(event.getKeyCode()) &&
                    (   (getVisibleFragment(CONTACTS_FRAGMENT))==null || (getVisibleFragment(CONTACTS_FRAGMENT))!=null &&((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).isSearchLayoutVisible() ==false   ) ) {

                if (keyCode != KeyEvent.KEYCODE_0) {
                    // mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(digit);
                    if( getVisibleFragment(ACTIVE_CALL_FRAGMENT) !=null && !((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mHoldCallButton.isChecked()) {
                        ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).sendDTMF(digit.charAt(0));
                    }else{
                        if(isFragmentVisible(CONTACTS_EDIT_FRAGMENT)==false)
                            mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(digit);
                    }
                }else {
                    zeroOrPlus = "0";
                }

                mViewPager.setCurrentItem(0, false);
                searchButton.setVisibility(View.INVISIBLE);
                addcontactButton.setVisibility(View.INVISIBLE);

                try {
                    if (isFragmentVisible(DIALER_FRAGMENT) && isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) ) {
                        changeUiForFullScreenInLandscape(false);
                        ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                if (isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT) || isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) || isFragmentVisible(CONTACTS_EDIT_FRAGMENT)) {
                    changeUiForFullScreenInLandscape(true);
                }

                event.startTracking();
                return 1;
            }


        }catch (Exception e){
            e.printStackTrace();
        }
    }
    return -1;
}


    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            try {
                if (keyCode == KeyEvent.KEYCODE_0) {
                    if (isFragmentVisible(DIALER_FRAGMENT) && mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                        mSectionsPagerAdapter.getDialerFragment().dialFromKeyboard("+");
                        zeroOrPlus = "+";
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        if(DigitKeys.contains(event.getKeyCode())) {
            return true;
        }else
            return super.onKeyLongPress(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

            isToLockPressButton = false;
            String key = KeyEvent.keyCodeToString(event.getKeyCode()).replace("KEYCODE_", "");
            sendAccessibilityEvent(key, findViewById(R.id.dialer_display));

            if (keyCode == KeyEvent.KEYCODE_0) {
                if(mSectionsPagerAdapter!=null && mSectionsPagerAdapter.getDialerFragment()!=null ) {
                    //mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(zeroOrPlus);
                    if(getVisibleFragment(ACTIVE_CALL_FRAGMENT) !=null && !((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mHoldCallButton.isChecked()) {
                        ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).sendDTMF(zeroOrPlus.charAt(0));
                    }else{
                        mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(zeroOrPlus);
                    }
                }
            }
        if(DigitKeys.contains(event.getKeyCode())) {
            if(isOnKeyDownHappened == false) {
                int keyunicode = event.getUnicodeChar(event.getMetaState());
                char character = (char) keyunicode;
                String digit = "" + character;

                if (isFragmentVisible(DIALER_FRAGMENT) && mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                    if (digit.matches("[\\d]") || digit.matches("#") || digit.matches("\\*")) {
                        if( (getVisibleFragment(CONTACTS_EDIT_FRAGMENT)) ==null
                                && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).isSearchLayoutVisible() ==false && getVisibleFragment(JOIN_MEETING_FRAGMENT) == null ) {
                            mSectionsPagerAdapter.getDialerFragment().dialFromKeyboard(digit);
                        }
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        mSectionsPagerAdapter.getDialerFragment().deleteDigit();
                    }
                }

                if (DigitKeys.contains(event.getKeyCode()) && getVisibleFragment(CONTACTS_FRAGMENT)!=null
                        && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).isSearchLayoutVisible() ==false
                        && !isFragmentVisible(CONTACTS_EDIT_FRAGMENT)) {

                    if (keyCode != KeyEvent.KEYCODE_0) {
                        //mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(digit);
                        if(getVisibleFragment(ACTIVE_CALL_FRAGMENT) !=null && !((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mHoldCallButton.isChecked()) {
                            ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).sendDTMF(digit.charAt(0));
                        }else {
                            mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(digit);
                        }
                    } else {
                        zeroOrPlus = "0";
                    }

                    mViewPager.setCurrentItem(0, false);
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.INVISIBLE);

                    try {
                        if (isFragmentVisible(DIALER_FRAGMENT) && isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)) {
                            changeUiForFullScreenInLandscape(false);
                            ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).mBackListener.back();
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                    event.startTracking();
                }
            }
            isOnKeyDownHappened = false;
            return true;
        }else {
            return super.onKeyUp(keyCode, event);
        }
    }


    boolean onDialerInteractionDeviceLogic(String number){
        FragmentManager fragmentManager = MainActivityK155.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible())
                if (fragment instanceof DialerFragment) {
                    mCallViewAdaptor.addDigitToOffHookDialCall(number.charAt(0));
                } else if (fragment instanceof ActiveCallFragment && (((ActiveCallFragment) fragment).getCallState() == UICallState.ESTABLISHED)) {
                    ((ActiveCallFragment) fragment).sendDTMF(number.charAt(0));
                    return false;
                }
        }
        return true;
    }


    void expandPhoneNumberSlide(){
        mSlideSelectPhoneNumber.expand(mSelectPhoneNumber);
    }


    void setTabIconsDeviceLogic(){
        if (tabOne == null)
            return;
        tabImage = tabOne.findViewById(R.id.tab_image);
        tabImage.setImageResource(R.drawable.ic_contacts);

        tabSelector = tabOne.findViewById(R.id.tab_selector);

        checkFilterButtonState();

        tabSelectorWrapper = tabOne.findViewById(R.id.filter);


        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) == true  && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CALL_LOG) == true){
            if ( (Objects.requireNonNull(mTabLayout.getTabAt(2)).getCustomView() == null && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0) || !mSectionsPagerAdapter.isCallAddParticipant()))
                Objects.requireNonNull(mTabLayout.getTabAt(2)).setCustomView(tabOne);
            else if (Objects.requireNonNull(mTabLayout.getTabAt(1)).getCustomView() == null && SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0 )
                Objects.requireNonNull(mTabLayout.getTabAt(1)).setCustomView(tabOne);
        }else if( SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) == true  && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CALL_LOG) != true ) {

            if ( ( mTabLayout.getTabAt(2)!=null && Objects.requireNonNull(mTabLayout.getTabAt(2)).getCustomView() == null && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0) || !mSectionsPagerAdapter.isCallAddParticipant()))
                Objects.requireNonNull(mTabLayout.getTabAt(2)).setCustomView(tabOne);
            else if ( mTabLayout.getTabAt(1)!=null && Objects.requireNonNull(mTabLayout.getTabAt(1)).getCustomView() == null && SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0 )
                Objects.requireNonNull(mTabLayout.getTabAt(1)).setCustomView(tabOne);

        }else if( SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) != true  && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CALL_LOG) == true ) {
            if ( ( mTabLayout.getTabAt(1)!=null && Objects.requireNonNull(mTabLayout.getTabAt(1)).getCustomView() == null && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0) || !mSectionsPagerAdapter.isCallAddParticipant()))
                Objects.requireNonNull(mTabLayout.getTabAt(1)).setCustomView(tabOne);
            else if ( mTabLayout.getTabAt(0)!=null && Objects.requireNonNull(mTabLayout.getTabAt(0)).getCustomView() == null && SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0 )
                Objects.requireNonNull(mTabLayout.getTabAt(0)).setCustomView(tabOne);
        }else if( SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) != true  && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CALL_LOG) != true ) {
            if ( ( mTabLayout.getTabAt(1)!=null && Objects.requireNonNull(mTabLayout.getTabAt(1)).getCustomView() == null && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0) || !mSectionsPagerAdapter.isCallAddParticipant()))
                Objects.requireNonNull(mTabLayout.getTabAt(1)).setCustomView(tabOne);
            else if ( mTabLayout.getTabAt(0)!=null && Objects.requireNonNull(mTabLayout.getTabAt(0)).getCustomView() == null && SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0 )
                Objects.requireNonNull(mTabLayout.getTabAt(0)).setCustomView(tabOne);
        }
    }

    private void contactsTabFilterMenuListnerSetup(View v){
        try{
            if(showingFirst == true){

                    FragmentManager fragmentManager = MainActivityK155.this.getSupportFragmentManager();
                    List<Fragment> fragments = fragmentManager.getFragments();
                for(Fragment fragment : fragments){
                    if(fragment != null && fragment.isVisible())
                        if (fragment instanceof ContactsFragment && ((ContactsFragment) fragment).isUserVisibleHint()) {
                            ((ContactsFragment) fragment).hideMenus();
                            tabSelector.setImageResource(R.drawable.triangle_copy);
                            showingFirst = false;
                            break;
                        }
                }
            }else{

                    try {
                        FragmentManager fragmentManager = MainActivityK155.this.getSupportFragmentManager();
                        List<Fragment> fragments = fragmentManager.getFragments();
                        for (Fragment fragment : fragments) {
                            if (fragment != null && fragment.isVisible())
                                if (fragment instanceof ContactsFragment && ((ContactsFragment) fragment).isUserVisibleHint() && !fragment.isDetached()) {
                                    ((ContactsFragment) fragment).onClick(v);
                                    tabSelector.setImageResource(R.drawable.triangle);
                                    showingFirst = true;
                                    break;
                                }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    void backDeviceLogic(Tabs selectedTab){
        CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
        int mNumActiveCalls = mCallStateEventHandler.mCalls.size();
        assert callStatusFragment != null;
        if( Objects.requireNonNull(callStatusFragment.getView()).getVisibility()!=View.VISIBLE  && mNumActiveCalls>0 && !isFragmentVisible(ACTIVE_CALL_FRAGMENT)) {
            callStatusFragment.showCallStatus();
        }
        if (selectedTab == Tabs.History) {
            mSectionsPagerAdapter.getFragmentCallHistory().hideMenus();
            filterButton.setVisibility(View.VISIBLE);
        }
    }


    void onClickUser(){

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

    @Override
    void onClickSearchButton(){
        if ((mToggleAudioMenu.getVisibility() == View.INVISIBLE || mToggleAudioMenu.getVisibility() == View.GONE && mListPreferences.getVisibility() == View.GONE)) {
            changeUiForFullScreenInLandscape(true);
            mSectionsPagerAdapter.getFragmentContacts().searchLayout.setVisibility(View.VISIBLE);
            mSectionsPagerAdapter.getFragmentContacts().mAdd.setVisibility(View.INVISIBLE);

            mSectionsPagerAdapter.getFragmentContacts().mSearchView.setQuery("", false);
            mSectionsPagerAdapter.getFragmentContacts().mSearchView.requestFocus();
            Utils.openKeyboard(this);

            mSectionsPagerAdapter.getFragmentContacts().hideMenus();
            tabSelector.setImageResource(R.drawable.triangle_copy);
            showingFirst = false;

            CallStatusFragment callStatusFragment_search_button = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
            assert callStatusFragment_search_button != null;
            if (Objects.requireNonNull(callStatusFragment_search_button.getView()).getVisibility() == View.VISIBLE) {
                callStatusFragment_search_button.hideCallStatus();
                Utils.hideKeyboard(this);
            }

            mViewPager.setEnabledSwipe(false);
        }
    }



    @Override
    void changeUIFor155(){
        FragmentManager fragmentManager = MainActivityK155.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible())
                if (fragment instanceof VideoCallFragment && !fragment.isDetached()) {
                    changeUiForFullScreenInLandscape(true);
                    break;
                } else {
                    changeUiForFullScreenInLandscape(false);
                }
        }
    }

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
    void setTransducerButtonCheckedFor155(){
        FragmentManager fragmentManager = MainActivityK155.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for(Fragment fragment : fragments){
            if(fragment != null && fragment.isVisible())
                if (fragment instanceof DialerFragment) {
                    ((DialerFragment) fragment).transducerButton.setChecked(false);
                }else if (fragment instanceof ActiveCallFragment){
                    ((ActiveCallFragment) fragment).transducerButton.setChecked(false);
                }
        }
    }



    @Override
    protected void setBackgroundResource(int resId){

        if (isFragmentVisible(DIALER_FRAGMENT)) {
            ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.setBackgroundResource(resId);
        }
        ActiveCallFragment fragment = (ActiveCallFragment) (getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG));
        if (fragment != null) {
            fragment.offHook.setBackgroundResource(resId);
        }
    }




    @Override
    void OnCallEndedChangeUIForDevice(){
        try {
            if (isFragmentVisible(DIALER_FRAGMENT)) {
                ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.setChecked(false);
                ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).setMode(DialerFragment.DialMode.EDIT);
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        searchAddFilterIconViewController();


        Tabs selectedTab = Tabs.Favorites;
        for (Tabs t : mTabIndexMap.keySet()) {
            if (mViewPager!=null && mViewPager.getCurrentItem() == mTabIndexMap.get(t)) {
                selectedTab = t;
            }
        }

        if(selectedTab== Tabs.Contacts) {
            if (isFragmentVisible(CONTACTS_FRAGMENT))
                mSectionsPagerAdapter.getFragmentContacts().removeSearchResults();
        }
        if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) || isFragmentVisible(CONTACTS_EDIT_FRAGMENT)){
                changeUiForFullScreenInLandscape(true);
        }
        else {
            changeUiForFullScreenInLandscape(false);
        }
    }

    @Override
    void OnCallEndedChangesForDevice(){
        hideMenus();
        checkFilterButtonState();
    }


    /**
     * Setting {@link ToggleButton} for audio selection on or off based on parameters provided
     *
     * @param isOn boolean based on which {@link ToggleButton} is set
     */
    @Override
    public void setOffhookButtosChecked(boolean isOn) {
        mSelectAudio.setChecked(isOn);

            if (mSectionsPagerAdapter.getDialerFragment() != null && mSectionsPagerAdapter.getDialerFragment().offHook != null){
                mSectionsPagerAdapter.getDialerFragment().offHook.setChecked(isOn);
            }

            ActiveCallFragment fragment = (ActiveCallFragment) (getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG));
            if (fragment != null && fragment.offHook != null) {
                fragment.offHook.setChecked(isOn);
            }
    }


    @Override
    public void setOffHookButtonsBasedCallState(int callId, UICallState state){
        boolean isChecked=false;

            int offhookCallId = SDKManager.getInstance().getCallAdaptor().getOffhookCallId();
            if (offhookCallId != 0 && offhookCallId != callId)
                isChecked=true;
            else if (state == ESTABLISHED || state == REMOTE_ALERTING || state ==FAILED || state==TRANSFERRING) {
                isChecked=true;
            }

            setOffhookButtosChecked(isChecked);
    }

    void onDeviceChangedDeviceLogic(int resId, boolean active){
        if (mSectionsPagerAdapter.getDialerFragment() != null && mSectionsPagerAdapter.getDialerFragment().offHook != null){
            if(isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)==true) {
                ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).isBackORDeletePressed = true;
            }

            mSectionsPagerAdapter.getDialerFragment().offHook.setChecked(active);
            mSectionsPagerAdapter.getDialerFragment().offHook.setBackgroundResource(resId);
        }
        ActiveCallFragment fragment = (ActiveCallFragment) (getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG));
        if (fragment != null && fragment.offHook != null) {
            fragment.offHook.setChecked(active);
            fragment.offHook.setBackgroundResource(resId);
        }
    }


    @Override
    void prepareOffhookChangeUIforDevice(){
        if(!isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)
                && isFragmentVisible(CONTACTS_FRAGMENT) == false
                && (getVisibleFragment(CONTACTS_FRAGMENT))!=null
                && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).isSearchLayoutVisible() ==false)
        {
            changeUiForFullScreenInLandscape(false);
        }
        if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) && (SDKManager.getInstance().getCallAdaptor().getActiveCallIdWithoutOffhook()) == 0)
            ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
    }

    @Override
    void onIncomingCallEndedCahngeUIForDevice(){
        searchAddFilterIconViewController();

        if( (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) || isFragmentVisible(CONTACTS_EDIT_FRAGMENT) || isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)) && !isFragmentVisible(DIALER_FRAGMENT) ){
            changeUiForFullScreenInLandscape(true);
            if( isFragmentVisible(ACTIVE_CALL_FRAGMENT) && !isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT))
                changeUiForFullScreenInLandscape(false);
        }else  if( (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) || isFragmentVisible(CONTACTS_EDIT_FRAGMENT) || isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)) ) {
            changeUiForFullScreenInLandscape(true);

        }else if (  mSectionsPagerAdapter.getFragmentContacts() != null && mSectionsPagerAdapter.getFragmentContacts().isSearchLayoutVisible()){
            changeUiForFullScreenInLandscape(true);
        }else{
            changeUiForFullScreenInLandscape(false);
        }
    }


    @Override
    void onIncomingCallStartedDeviceLogic(){
        hideMenus();
        checkFilterButtonState();

        if(isFragmentVisible(CONTACTS_FRAGMENT) && (getVisibleFragment(CONTACTS_FRAGMENT)) !=null){
            ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).handleIncomingCall();
        }

        if(isFragmentVisible(HISTORY_FRAGMENT) && getVisibleFragment(HISTORY_FRAGMENT) != null){
            ((CallHistoryFragment) getVisibleFragment(HISTORY_FRAGMENT)).handleIncomingCall();
        }
    }

    @Override
    boolean onBackPressedDeviceLogic(){
        if (isToBlockBakcPress == true){
            isToBlockBakcPress = false;
            return false;
        }
        return true;
    }

    @Override
    protected void setVideoControlsVisibility(int visible) {
        if (mSectionsPagerAdapter.getDialerFragment() != null)
            mSectionsPagerAdapter.getDialerFragment().setVideoButtonVisibility(visible);
    }

    /**
     * Set appropriate state for add contact button on K155 devices
     */
    private void setAddContactVisibility() {
        if (Utils.isModifyContactsEnabled() && !mSectionsPagerAdapter.isCallAddParticipant()) {
            addcontactButton.setVisibility(View.VISIBLE);
        } else {
            addcontactButton.setVisibility(View.INVISIBLE);
        }
    }
}
