package com.avaya.android.vantage.aaadevbroadcast.csdk;

import android.app.Application;

import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.clientservices.client.Client;
import com.avaya.clientservices.client.ClientListener;

import com.avaya.clientservices.user.User;

import android.util.Log;

/**
 * {@link SDKManager} is responsible for initialization and communication with CSDK
 */

public class SDKManager implements  ClientListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    // Singleton instance of SDKManager
    private static volatile SDKManager instance=null;

    private CallAdaptor           mCallAdaptor;
    private DeskPhoneServiceAdaptor mDeskPhoneServiceAdapter;
    private ContactsAdaptor       mContactsAdaptor;
    private HistoryAdaptor        mHistoryAdaptor;
    private VoiceMessageAdaptor   mVoiceMessageAdaptor;
    private AudioDeviceAdaptor    mAudioDeviceAdaptor;
    private UnifiedPortalAdaptor  mUnifiedPortalAdaptor;
    private boolean mIsCameraSupported;

    private SDKManager() {
        Log.d(LOG_TAG, "In constructor of SDKManager");
    }

    /**
     * Obtaining instance of {@link SDKManager}
     * @return instance of {@link SDKManager}
     */
    public static SDKManager getInstance() {
        if (instance == null) {
            synchronized (SDKManager.class) {
                if (instance == null) {
                    instance = new SDKManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize SDK manager
     * @param application
     */
    public void initializeSDK(Application application) {
        Log.d(LOG_TAG, "initializeSDK");

        mDeskPhoneServiceAdapter = new DeskPhoneServiceAdaptor(application);
        mCallAdaptor = new CallAdaptor(application.getApplicationContext());
        mContactsAdaptor = new ContactsAdaptor(application.getApplicationContext());
        mHistoryAdaptor = new HistoryAdaptor(application.getApplicationContext());
        mVoiceMessageAdaptor = new VoiceMessageAdaptor();
        mAudioDeviceAdaptor = new AudioDeviceAdaptor();
        mUnifiedPortalAdaptor = new UnifiedPortalAdaptor();

        mDeskPhoneServiceAdapter.setupClient();
        mDeskPhoneServiceAdapter.setupUserConfiguration();
        mAudioDeviceAdaptor.init();
        mIsCameraSupported = Utils.getDeviceCameraSupport();
    }

    /**
     * Obtain {@link Client}
     * @return {@link Client}
     */
    public Client getClient() { return getDeskPhoneServiceAdaptor().getClient(); }

    /**
     * Obtain {@link CallAdaptor}
     * @return {@link CallAdaptor}
     */
    public CallAdaptor getCallAdaptor() { return mCallAdaptor; }


    public UnifiedPortalAdaptor getUnifiedPortalAdaptor() { return mUnifiedPortalAdaptor; }

    /**
     * Obtain {@link DeskPhoneServiceAdaptor}
     * @return {@link DeskPhoneServiceAdaptor}
     */
    public DeskPhoneServiceAdaptor getDeskPhoneServiceAdaptor() { return mDeskPhoneServiceAdapter; }

    /**
     * Obtain {@link ContactsAdaptor}
     * @return {@link ContactsAdaptor}
     */
    public ContactsAdaptor getContactsAdaptor() { return mContactsAdaptor; }

    /**
     * Obtain {@link HistoryAdaptor}
     * @return {@link HistoryAdaptor}
     */
    public HistoryAdaptor getHistoryAdaptor() { return mHistoryAdaptor; }

    /**
     * Obtain {@link VoiceMessageAdaptor}
     * @return {@link VoiceMessageAdaptor}
     */
    public VoiceMessageAdaptor getVoiceMessageAdaptor() { return mVoiceMessageAdaptor; }

    /**
     * Obtain {@link AudioDeviceAdaptor}
     * @return {@link AudioDeviceAdaptor}
     */
    public AudioDeviceAdaptor getAudioDeviceAdaptor() {return  mAudioDeviceAdaptor;}

    /**
     * Processing client shutdown process
     * @param client {@link Client}
     */
    @Override
    public void onClientShutdown(Client client) {
        Log.d(LOG_TAG, "onClientShutdown");
        Log.d(LOG_TAG, "Recreating the client");
        if (getClient() != client) {
            Log.e(LOG_TAG, "Wrong client was shutdown.");
        } else {
            client.removeListener(this);
        }

        if ( ! mDeskPhoneServiceAdapter.isLogoutInProgress()) {
            Log.d(LOG_TAG, "onClientShutdown not during logout");
            mDeskPhoneServiceAdapter.setupClient();
            mDeskPhoneServiceAdapter.setupUserConfiguration();
            mAudioDeviceAdaptor.init();
            try {
                mDeskPhoneServiceAdapter.createUser(false);
            }
            catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "onClientShutdown: could not create user", e);
            }
        }
        else { //cleanup after logout
            Log.d(LOG_TAG, "onClientShutdown during logout");
            mDeskPhoneServiceAdapter.setLogoutInProgress(false);
        }

    }

    /**
     * Processing creation of {@link User} for {@link Client}
     * @param client {@link Client} for which {@link User} is created
     * @param user {@link User} which is created for {@link Client}
     */
    @Override
    public void onClientUserCreated(Client client, User user) {
        Log.d(LOG_TAG, "onClientUserCreated");
        mContactsAdaptor.setUser(user);
    }

    /**
     * Processing removal of {@link User} from {@link Client}
     * @param client {@link Client} for which {@link User} is removed
     * @param user {@link User} which is removed for {@link Client}
     */
    @Override
    public void onClientUserRemoved(Client client, User user) {
        Log.d(LOG_TAG, "onClientUserRemoved");
        mDeskPhoneServiceAdapter.onUserRemoved(user);
        mContactsAdaptor.setUser(null);
    }

    @Override
    public void onIdentityCertificateEnrollmentFailed(Client client, int errorCode, String errorType, String message) {

    }

    public boolean isCameraSupported() {
        return mIsCameraSupported;
    }

    public void setCameraSupported(boolean isSupported) {
        mIsCameraSupported=isSupported;
    }
}