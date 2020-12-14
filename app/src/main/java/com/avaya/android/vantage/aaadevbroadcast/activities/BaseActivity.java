package com.avaya.android.vantage.aaadevbroadcast.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.avaya.android.vantage.aaadevbroadcast.BuildConfig;
import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.GoogleAnalyticsUtils;
import com.avaya.android.vantage.aaadevbroadcast.KeysNonVersionControl;
import com.avaya.android.vantage.aaadevbroadcast.MQMessageClass;
import com.avaya.android.vantage.aaadevbroadcast.PermissionManager;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.VantageDBHelper;
import com.avaya.android.vantage.aaadevbroadcast.VantageDBHelper.VantageDBObserver;
import com.avaya.android.vantage.aaadevbroadcast.VideoDialogFragment;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.ICallControlsInterface;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.IHookListener;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.INameExtensionVisibilityInterface;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.UIAudioDeviceViewAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.UICallViewAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.UIDeskPhoneServiceAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.UIVoiceMessageAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.bluetooth.BluetoothStateService;
import com.avaya.android.vantage.aaadevbroadcast.callshistory.CallHistoryFragment;
import com.avaya.android.vantage.aaadevbroadcast.callshistory.OnFilterCallsInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.LocalContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.csdk.CallAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ContactsLoader;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager;
import com.avaya.android.vantage.aaadevbroadcast.csdk.LocalContactsManager;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ActiveCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.CallStatusFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactDetailsFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactEditFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactViewAdaptorInterface;
import com.avaya.android.vantage.aaadevbroadcast.fragments.DialerFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.FavoritesFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.IncomingCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.JoinMeetingFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OffHookTransduceButtonInterface;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OnActiveCallInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.fragments.VideoCallFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.settings.ConfigChangeApplier;
import com.avaya.android.vantage.aaadevbroadcast.model.CallData;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.model.UIAudioDevice;
import com.avaya.android.vantage.aaadevbroadcast.model.UICall;
import com.avaya.android.vantage.aaadevbroadcast.model.UICallState;
import com.avaya.android.vantage.aaadevbroadcast.notifications.CallNotificationFactory;
import com.avaya.android.vantage.aaadevbroadcast.notifications.NotificationService;
import com.avaya.android.vantage.aaadevbroadcast.receiver.FinishCallDialerActivityReciver;
import com.avaya.android.vantage.aaadevbroadcast.views.DialogAAADEVMessages;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimation;
import com.avaya.android.vantage.aaadevbroadcast.views.adapters.CallStateEventHandler;
import com.avaya.android.vantage.aaadevbroadcast.views.adapters.NumberPickerAdapter;
import com.avaya.android.vantage.aaadevbroadcast.views.adapters.SectionsPagerAdapter;
import com.avaya.android.vantage.aaadevbroadcast.views.interfaces.IDeviceViewInterface;
import com.avaya.android.vantage.aaadevbroadcast.views.interfaces.ILoginListener;
import com.avaya.clientservices.contact.EditableContact;
import com.avaya.clientservices.credentials.Challenge;
import com.avaya.clientservices.credentials.CredentialCompletionHandler;
import com.avaya.clientservices.credentials.CredentialProvider;
import com.avaya.clientservices.credentials.UserCredential;
import com.avaya.clientservices.downloadservice.DownloadCompletionHandler;
import com.avaya.clientservices.downloadservice.DownloadService;
import com.avaya.clientservices.downloadservice.DownloadServiceConfiguration;
import com.avaya.clientservices.downloadservice.DownloadServiceError;
import com.avaya.clientservices.media.AudioDevice;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.app.ActivityManager.LOCK_TASK_MODE_LOCKED;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.ANONYMOUS_USER;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.BRIO_CALL_LOGS_URI;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.CALL_ID;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.CONFERENCE_REQUEST_CODE;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.DigitKeys;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.INCOMING_CALL_ACCEPT;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.IS_CONTACT;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.TARGET;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.TRANSFER_REQUEST_CODE;
import static com.avaya.android.vantage.aaadevbroadcast.UriUtil.getPhoneNumberFromTelURI;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

/**
 * Activity contains logic required for communication between mentioned fragments and
 * between fragments and CSDK.
 * MainActivity is base of Vantage Connect application which contains {@link DialerFragment},
 * {@link FavoritesFragment}, {@link ContactsFragment}, {@link ActiveCallFragment},
 * {@link CallHistoryFragment} and {@link ContactDetailsFragment}.
 */

public abstract class BaseActivity extends AppCompatActivity implements DialerFragment.OnDialerInteractionListener, OnContactInteractionListener,
        ContactDetailsFragment.OnContactDetailsInteractionListener, ContactEditFragment.OnContactEditInteractionListener,
        ContactViewAdaptorInterface, OnActiveCallInteractionListener, ICallControlsInterface,
        IncomingCallFragment.IncomingCallInteraction, View.OnClickListener, ILoginListener, IDeviceViewInterface, IHookListener,INameExtensionVisibilityInterface,
        OnFilterCallsInteractionListener,OffHookTransduceButtonInterface/*, IHardButtonListener*/ {

    private static final long CLEAR_DIGITS_DELAY = 30000; //30 seconds
    static final String DIALER_FRAGMENT = "DialerFragment";
    private static final String FAVORITES_FRAGMENT = "FavoritesFragment";
    static final String CONTACTS_FRAGMENT = "ContactsFragment";
    static final String HISTORY_FRAGMENT = "HistoryFragment";
    public static final String CONTACTS_DETAILS_FRAGMENT = "DetailsFragment";
    public static final String CONTACTS_EDIT_FRAGMENT = "EditFragment";
    public static final String ACTIVE_CALL_FRAGMENT = "ActiveCallFragment";
    public static final String ACTIVE_VIDEO_CALL_FRAGMENT = "ActiveVideoCallFragment";
    public static final String JOIN_MEETING_FRAGMENT = "JoinMeetingFragment";

    private static final String TAG = "MainActivity";
    private static final int LAST_NAME_FIRST = Constants.LAST_NAME_FIRST;
    private static final String NAME_SORT_PREFERENCE = Constants.NAME_SORT_PREFERENCE;
    private static final String USER_PREFERENCE = Constants.USER_PREFERENCE;
    public static final String SERVICE_IMPACTING_CHANGE = "com.avaya.endpoint.action.SERVICE_IMPACTING_CHANGE";
    public static final String NON_SERVICE_IMPACTING_CHANGE = "com.avaya.endpoint.action.NON_SERVICE_IMPACTING_CHANGE";
    public static final String INCOMING_CALL_INTENT = "intent_incoming_call";
    static final String BRING_TO_FOREGROUND_INTENT = "action.BRING_TO_FORGROUND_INTENT";
    private static final String EMERGENCY_CALL_INTENT = "android.intent.action.CALL";
    private static final String TAB_POSITION = "mainActivityTabPosition";
    private static final String BRAND_PREF = "brand";
    //Declare what is minimal amount of tabs which can be shown and selected
    private static final int MINIMAL_AMOUNT_OF_TABS = 2;
    public static final String SHOW_CALL_INTENT = "com.avaya.android.vantage.action.SHOW_CALL";
    private static final String CONNECTION_STATE_CHANGED ="android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_CAMERA_ATTACHED  = "com.avaya.endpoint.CAMERA_DEVICE_ATTACHED";
    public static final String ACTION_CAMERA_DETACHED  = "com.avaya.endpoint.CAMERA_DEVICE_DETACHED";
    static final String HARD_BUTTON = "HARD_BUTTON";
    private boolean mFirstLoad = true;
    private boolean mFullyInitd = false;
    private PowerManager.WakeLock mScreenLock = null;

    // handle config changes
    private Locale mLocale;
    private float mFontScale;
    private boolean isConfigChanged;
    //private boolean isCallInProgress;
    private boolean callWhileCallInProgress;

    private String mAdminNameDisplayOrder;
    private String mAdminNameSortOrder;
    private String mAdminChoiceRingtone;
    private String mPreviousAdminNameDisplayOrder;
    private String mPreviousAdminNameSortOrder;
    private String mPreviousAadminChoiceRingtone;

    private TextView mClosePicker;
    private TextView mPickContactTitle;

    private UICall mCall;

    ImageView mBrandView;
    private boolean isAccessibility = false;

    private Tabs mContactCapableTab;


    //private RelativeLayout tabOne;
    ImageView tabImage;
    public ImageView tabSelector;
    public boolean showingFirst = false;
    public boolean isFilterOnContactTabEnabled = false;
    public boolean showingFirstRecent = true;
    //private RelativeLayout tabSelectorWrapper;

    ImageView searchButton;
    ImageView addcontactButton;
    public ImageView filterButton;

    long mLastClickTime = 0;

    boolean isToBlockBakcPress = false;
    boolean isOnKeyDownHappened = false;

    private FinishCallDialerActivityReciver finishCallDialerActivityReciver;
    private IntentFilter intentFilter;

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    public SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    public ElanCustomViewPager mViewPager;
    private TextView mOptionUserSettings, mOptionUserAbout, mNumberPickerContactName;
    UICallViewAdaptor mCallViewAdaptor;
    CallStateEventHandler mCallStateEventHandler;
    LinearLayout mToggleAudioMenu;
    private LinearLayout mOptionBTHeadset;
    private LinearLayout mOptionHeadset;
    private LinearLayout mOptionUsbHeadset;
    private LinearLayout mOptionHandset;
    private LinearLayout mOptionSpeaker;
    private LinearLayout mOption35Headset;
    LinearLayout mListPreferences;
    public LinearLayout mSelectPhoneNumber;
    ToggleButton mSelectAudio;
    ToggleButton mAudioMute;
    ToggleButton mVideoMute;
    private ToggleButton mToggleAudioButton;
    public FrameLayout mFrameAll;
    FrameLayout mUser;
    public FrameLayout mActiveCall;
    private FrameLayout mEditContact;
    private FrameLayout mPersistentControl;

    SharedPreferences mSharedPref;
    private SharedPreferences mSharedPrefs;
    private SharedPreferences mCallPreference;
    TextView mLoggedUserExtension;
    TextView mLoggedUserNumber;
    TabLayout mTabLayout;
    ImageView mOpenUser;
    ImageView mErrorStatus;
    public LinearLayout mPickContacts;
    private LinearLayout mBlureFrame;
    private ListView mNumberPickerList;


    LinearLayout mStatusLayout; //ELAN-1058
    private int mCallActiveCallID = -1;
    private boolean isConferenceCall;
    private int mActiveCallRequestCode;
    private boolean mIsOffHook = false;

    String zeroOrPlus = "0";
    private static final String DIAL_ACTION = "android.intent.action.DIAL";

    public enum Tabs {Dialer, Favorites, Contacts, History}

    final HashMap<Tabs, Integer> mTabIndexMap = new HashMap<>();

    // Rect used for canceling keyboard on contact search.
    private Rect mSearchArea;
    // Shows status of fullscreen
    private boolean isSearchInProgress;

    /**
     * loading slide class
     * Animation cannot be reused, so we have to create one for every slide we want to create
     */
    SlideAnimation mSlideSelecAudioDevice;
    SlideAnimation mSlideUserPreferences;
    SlideAnimation mSlideSelectPhoneNumber;


    private SharedPreferences mBrandPref;
    UIAudioDeviceViewAdaptor mAudioDeviceViewAdaptor;
    UIDeskPhoneServiceAdaptor mUIDeskphoneServiceAdaptor;
    private CoordinatorLayout mCoordinatorLayout;
    private RelativeLayout mActivityLayout;

    private VantageDBObserver mSipUserDisplayObserver;
    private VantageDBObserver mSipUserNumberDisplayObserver;


    Handler mHandler;
    Runnable mLayoutCloseRunnable;

    private boolean mEmergencyWithoutLogin = false;

    private CallNotificationFactory mNotifFactory;
    private RecentCallsAndContactObserver mRecentCallAndContactObserver;

    AppBarLayout appBar;

    ImageView dialerView;
    private CallData.CallCategory mSelectedCallCategory = CallData.CallCategory.ALL;

    boolean isToLockPressButton = false;

    boolean needCancelEdit = false;

    //Custom
    public  AlertDialog.Builder alertDialog;
    public String idiomadevice;
    public TextToSpeech textToSpeech;
    public MediaPlayer mediaPlayer = null;
    public String mensaje =  null;
    public String ttstext = null;
    public String ttslang = null;
    public ImageView messageview = null;
    public Socket socket;
    DialogAAADEVMessages dialogAAADEVMessages;
    //exo
    private PlayerView simpleExoPlayerView;
    private SimpleExoPlayer player;
    private TextView resolutionTextView;
    //exo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "OnCreate");
        NukeSSLCerts.nuke();

        super.onCreate(savedInstanceState);

        finishCallDialerActivityReciver = new FinishCallDialerActivityReciver();

        intentFilter = new IntentFilter("com.avaya.endpoint.FINISH_CALL_ACTIVITY");

        if (!firstActivationCheck(getIntent())) {
            return;
        }

        if (PermissionManager.somePermissionsDenied(this, getIntent())) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        UiChangeListener();
        handleFirstLoadParams();

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        Utils.setDeviceMode(getApplicationContext());

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mLoggedUserExtension = findViewById(R.id.extension);
        mLoggedUserNumber = findViewById(R.id.number);
        mListPreferences = findViewById(R.id.preferences);
        mSelectAudio = findViewById(R.id.off_hook);
        mAudioMute = findViewById(R.id.audio_mute);
        mVideoMute = findViewById(R.id.video_mute);
        mOptionHandset = findViewById(R.id.containerHandset);
        mOptionBTHeadset = findViewById(R.id.containerBTHeadset);
        mOptionHeadset = findViewById(R.id.containerHeadset);
        mOptionUsbHeadset = findViewById(R.id.containerUsbHeadset);
        mOption35Headset = findViewById(R.id.container35Headset);
        mOptionSpeaker = findViewById(R.id.containerSpeaker);
        mToggleAudioMenu = findViewById(R.id.selectAudioMenu);
        mOptionUserAbout = findViewById(R.id.containerAbout);
        mOptionUserSettings = findViewById(R.id.containerUserSettings);
        mFrameAll = findViewById(R.id.frameAll);

        mClosePicker = findViewById(R.id.pick_cancel);
        mPickContactTitle = findViewById(R.id.pick_contact_title);

        mUser = findViewById(R.id.user);
        ViewGroup.LayoutParams params = mUser.getLayoutParams();
        mUser.setVisibility(View.VISIBLE);

        mActiveCall = findViewById(R.id.active_call);
        mEditContact = findViewById(R.id.edit_contact_frame);

        mOpenUser = findViewById(R.id.open);
        mErrorStatus = findViewById(R.id.topBarError);
        mPickContacts = findViewById(R.id.pick_contacts);


        mToggleAudioButton = findViewById(R.id.transducer_button);

        mPersistentControl = findViewById(R.id.persistent_contrls_container);

        mBlureFrame = findViewById(R.id.blur_frame);

        mSelectPhoneNumber = findViewById(R.id.selectPhoneNumberContainer);
        mNumberPickerContactName = findViewById(R.id.pickerContactName);
        mNumberPickerList = findViewById(R.id.pickerContactList);

        mStatusLayout = findViewById(R.id.status_layout);//ELAN-1058

        appBar = findViewById(R.id.appbar);

        dialerView = findViewById(R.id.dialer_tab);

        //custom
        messageview = findViewById(R.id.messages_icon);

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        if (mViewPager != null) {
            mViewPager.setOffscreenPageLimit(4);
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }

        mCoordinatorLayout = findViewById(R.id.main_content);
        mActivityLayout = findViewById(R.id.main_activity_layout);
        // directory search variables
        ContactsLoader mContactsLoader = new ContactsLoader(this);

        initMoreViews();

        // setting up slide animations
        mSlideSelecAudioDevice = ElanApplication.getDeviceFactory().getSlideAnimation();
        mSlideUserPreferences = ElanApplication.getDeviceFactory().getSlideAnimation();
        mSlideSelectPhoneNumber =ElanApplication.getDeviceFactory().getSlideAnimation();
        mSlideUserPreferences.reDrawListener(mListPreferences);
        mSlideSelecAudioDevice.reDrawListener(mToggleAudioMenu);
        mSlideSelectPhoneNumber.reDrawListener(mSelectPhoneNumber);


        mSharedPref = getSharedPreferences("selectedAudioOption", MODE_PRIVATE);
        mCallPreference = getSharedPreferences(Constants.CALL_PREFS, MODE_PRIVATE);
        mSharedPrefs = getSharedPreferences(Constants.USER_PREFERENCE, Context.MODE_PRIVATE);
        mBrandPref = getSharedPreferences(BRAND_PREF, MODE_PRIVATE);
        SharedPreferences mConnectionPref = getSharedPreferences(Constants.CONNECTION_PREFS, MODE_PRIVATE);

        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
            mVideoMute.setEnabled(true);
        } else {
            mVideoMute.setEnabled(false);
        }

        mTabLayout = findViewById(R.id.tabs);

        tabLayoutAddOnTabSelectedListener();

        if (mTabLayout != null) {
            mTabLayout.setupWithViewPager(mViewPager);
        }

        createJoinMeetingFragment();

        setupOnClickListeners();
        // Refresh tab layout according to configuration
        mSectionsPagerAdapter.configureTabLayout();
        setTabIcons();
        loadBrand(getBrand());
        configureUserPreferenceAccess();
        // initialize the notification service
        initNotifications();
        initBluetoothChangeListener();
        initCSDK();
        loadAudioSelection();

        mAdminNameDisplayOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);
        mAdminNameSortOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);
        mAdminChoiceRingtone = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ADMIN_CHOICE_RINGTONE);

        mSectionsPagerAdapter.setLocalContacts(mContactsLoader);

        initLocalContactsDownload();
        LocalContactsRepository mLocalContactsRepository = LocalContactsRepository.getInstance();
        mLocalContactsRepository.setContactsLoader(mSectionsPagerAdapter.getLocalContacts());

        initViewPager();

        mSipUserDisplayObserver = new VantageDBObserver(new Handler(), new ParameterUpdateRunnable(mLoggedUserExtension, VantageDBHelper.SIPUSERNAME), VantageDBHelper.SIPUSERNAME);
        getContentResolver().registerContentObserver(mSipUserDisplayObserver.getUri(), true, mSipUserDisplayObserver);

        mSipUserNumberDisplayObserver = new VantageDBObserver(new Handler(), new ParameterUpdateRunnable(mLoggedUserNumber, VantageDBHelper.SIPUSERNUMBER), VantageDBHelper.SIPUSERNUMBER);
        getContentResolver().registerContentObserver(mSipUserNumberDisplayObserver.getUri(), true, mSipUserNumberDisplayObserver);

        // check if accessibility is on and if is enable fullscreen dimensions.
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            isAccessibility = true;
            fullScreenViewResize(1000);
        } else {
            isAccessibility = false;
            fullScreenViewResize(1056);
        }
        // check if accessibility state is changed.
        accessibilityManager.addAccessibilityStateChangeListener(b -> {
            isAccessibility = b;
            UiChangeListener();
        });

        mHandler = new Handler();
        mLayoutCloseRunnable = this::hideMenus;

        handleSpecialInitCases();

        mFullyInitd = true;

        mRecentCallAndContactObserver = new RecentCallsAndContactObserver(new Handler());

        try {
            getContentResolver().registerContentObserver(BRIO_CALL_LOGS_URI, true, mRecentCallAndContactObserver);
        }
        catch (SecurityException ex){
            Log.w(TAG, "Registering content observer for BRIO_CALL_LOGS_URI failed with exception: ", ex);
        }

        getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, mRecentCallAndContactObserver);


        ////Codigo para aaadevbroadcast
        dialogAAADEVMessages = new DialogAAADEVMessages(BaseActivity.this);
        messageview.setOnClickListener(view -> {
            if(DialogAAADEVMessages.messagesList.size()>0)
                dialogAAADEVMessages.show();
        });
        dialogAAADEVMessages.setOnDismissListener(dialog -> {
            if(DialogAAADEVMessages.messagesList.size() == 0){
                messageview.setBackground(null);
            }
        });

    }

    private void createJoinMeetingFragment() {
        FragmentManager fm = getSupportFragmentManager();
        JoinMeetingFragment fragment = (JoinMeetingFragment)fm.findFragmentById(R.id.join_meeting);
        if (fragment == null){
            FragmentTransaction ft = fm.beginTransaction();
            fragment = JoinMeetingFragment.newInstance();
            ft.add(R.id.join_meeting, fragment, fragment.getClass().getSimpleName());
            ft.commit();
        }
    }

    void setVideoMuteisibility(){}

    void initMoreViews(){}

    void tabLayoutAddOnTabSelectedListener(){}

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mNotifFactory != null) mNotifFactory.unbindNotificationService();
        if (mRecentCallAndContactObserver != null) getContentResolver().unregisterContentObserver(mRecentCallAndContactObserver);
        ContactsFragment.sSearchQuery = "";
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBrandView = findViewById(R.id.brand);
        if(finishCallDialerActivityReciver!=null && intentFilter!=null)
            LocalBroadcastManager.getInstance(this).registerReceiver(finishCallDialerActivityReciver, intentFilter);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mBroadcastReceiver, filter);
        applyLockSetting();

        //Codigo de Socket.io para aaadevbroadcast
        String regnumber;
        regnumber = VantageDBHelper.getParameter(getContentResolver(), VantageDBHelper.SIPUSERNUMBER);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

        new Thread(() -> {
            try {
                Gson gson = new Gson();
                SSLContext msslcontext;
                IO.Options opts = new IO.Options();
                opts.reconnection = true;
                opts.secure = true;
                opts.forceNew = true;
                /*try{
                    msslcontext = SSLContext.getInstance("TLS");
                    msslcontext.init(null, trustAllCerts, null);
                    opts.sslContext =msslcontext;
                    opts.hostnameVerifier = new HostnameVerifier() {
                        @Override
                        public boolean verify(String s, SSLSession sslSession) {
                            return true;
                        }
                    };
                } catch (Exception exc){
                    Log.e("Sockets", "error al crear TLS: " + exc.getMessage());
                }*/

                try {
                    socket = IO.socket("https://ccai-av-482f1fe9008cf4.uc.r.appspot.com/", opts);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                Socket finalSocket = socket;
                socket.on(Socket.EVENT_CONNECT, args -> {
                    //String regnumber;
                    //regnumber = VantageDBHelper.getParameter(getContentResolver(), VantageDBHelper.SIPUSERNUMBER);
                    String lastFourDigits = "";     //substring containing last 4 characters

                    if (regnumber.length() > 8)
                    {
                        lastFourDigits = regnumber.substring(regnumber.length() - 8);
                    }
                    else
                    {
                        lastFourDigits = regnumber;
                    }
                    finalSocket.emit("reg", lastFourDigits);
                    Log.d("Sockets", "conectado: ");
                }).on(Socket.EVENT_DISCONNECT, args -> {
                    Log.d("Sockets", "des-conectado: " + args[0]);
                }).on(Socket.EVENT_ERROR, args -> {
                    Log.d("Sockets", "Error : " + args[0]);
                }).on("p2p", args -> {
                    JSONObject obj = (JSONObject)args[0];
                    PowerManager pm = (PowerManager)BaseActivity.this.getSystemService(Context.POWER_SERVICE);
                    boolean isScreenOn = pm.isScreenOn();
                    try {
                        ttstext = obj.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String resourceScheme = "res";
                    Uri uri = new Uri.Builder()
                            .scheme(resourceScheme)
                            .path(String.valueOf(R.raw.dingo))
                            .build();
                    mBuilder.setSmallIcon(R.drawable.ic_check);
                    mBuilder.setContentTitle("Notificaciones");
                    mBuilder.setContentText(ttstext);
                    mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
                    mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    mBuilder.setSound(uri);
                    Intent notificationIntent = new Intent(BaseActivity.this.getApplicationContext(), MainActivity.class);
                    PendingIntent contentIntent = PendingIntent.getActivity(BaseActivity.this.getApplicationContext(), 0, notificationIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(contentIntent);

                    // Add as notification
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        String channelId = "aaadevbroadcast";
                        NotificationChannel channel = new NotificationChannel(
                                channelId,
                                "Mensaje",
                                NotificationManager.IMPORTANCE_HIGH);
                        manager.createNotificationChannel(channel);
                        mBuilder.setChannelId(channelId);
                    }
                    manager.notify(1, mBuilder.build());
                    try {
                        ttslang = obj.getString("extra");
                        ttstext = obj.getString("message");
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url("https://translation.googleapis.com/language/translate/v2?key="+ KeysNonVersionControl.KEY_API_TRANSLATE +"&q="+ttstext+"&target="+ttslang+"")
                                .build();
                        Response response = client.newCall(request).execute();
                        final JSONObject jsonObject = new JSONObject(response.body().string());
                        Log.d("Json", jsonObject.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText"));
                        mBuilder.setSmallIcon(R.drawable.ic_check);
                        mBuilder.setContentTitle("Notificaciones");
                        mBuilder.setContentText(jsonObject.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText"));
                        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
                        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                        mBuilder.setSound(uri);
                        mBuilder.setContentIntent(contentIntent);
                        // Add as notification
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        {
                            String channelId = "aaadevbroadcast";
                            NotificationChannel channel = new NotificationChannel(
                                    channelId,
                                    "Mensaje",
                                    NotificationManager.IMPORTANCE_HIGH);
                            manager.createNotificationChannel(channel);
                            mBuilder.setChannelId(channelId);
                        }
                        manager.notify(1, mBuilder.build());
                        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if (status == TextToSpeech.SUCCESS) {
                                    int ttsLang = textToSpeech.setLanguage(Locale.forLanguageTag(ttslang));
                                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                                        Log.e("TTS", "The Language is not supported!");
                                    } else {
                                        Log.i("TTS", "Language Supported.");
                                        int speechStatus = 0;
                                        try {
                                            speechStatus = textToSpeech.speak(jsonObject.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText"), TextToSpeech.QUEUE_FLUSH, null);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        if (speechStatus == TextToSpeech.ERROR) {
                                            Log.e("TTS", "Error in converting Text to Speech!");
                                        }
                                    }
                                    Log.i("TTS", "Initialization success.");
                                } else {
                                    Toast.makeText(BaseActivity.this.getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }).on("image", args -> {
                    final JSONObject obj = (JSONObject) args[0];
                    try {
                        mensaje = obj.getString("message");
                        runOnUiThread(() -> new DownloadImageTask((ImageView) findViewById(R.id.brand))
                                .execute(mensaje));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).on("alert", args -> {
                    Log.d("Sockets", "Evento de audio : " + args[0]);
                    final JSONObject obj = (JSONObject) args[0];
                    try {
                        mensaje = obj.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                    mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                        mp.reset();
                        return false;
                    });
                    mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                    try {
                        mediaPlayer.setDataSource(mensaje);
                        mediaPlayer.prepareAsync();
                    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                        e.printStackTrace();
                    }

                }).on("video", args -> {
                    final JSONObject obj = (JSONObject) args[0];
                    try {
                        mensaje = obj.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    FragmentManager fm = getSupportFragmentManager();
                    VideoDialogFragment editNameDialogFragment = VideoDialogFragment.newInstance("Some Title");
                    editNameDialogFragment.show(fm, "fragment_edit_name");
                    editNameDialogFragment.setOnCloseListener(() -> {
                        player.stop();
                    });

                    resolutionTextView = new TextView(this);
                    resolutionTextView = (TextView) findViewById(R.id.resolution_textView);
                    BaseActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(); //test

                            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
                            TrackSelector trackSelector =
                                    new DefaultTrackSelector(videoTrackSelectionFactory);

                            // 2. Create the player
                            player = ExoPlayerFactory.newSimpleInstance(BaseActivity.this.getApplicationContext(), trackSelector);
                            simpleExoPlayerView = new SimpleExoPlayerView(BaseActivity.this.getApplicationContext());
                            simpleExoPlayerView = (SimpleExoPlayerView) editNameDialogFragment.getView().findViewById(R.id.player_view);
                            ////Set media controller
                            simpleExoPlayerView.setUseController(false);//set to true or false to see controllers
                            simpleExoPlayerView.requestFocus();
                            // Bind the player to the view.
                            simpleExoPlayerView.setPlayer(player);
                            // MP4
                            // Produces DataSource instances through which media data is loaded.
                            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(),
                                    Util.getUserAgent(getApplicationContext(), "ExoPlayer"));

                            // Produces Extractor instances for parsing the media data.
                            final ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

                            // This is the MediaSource representing the media to be played.
                            MediaSource videoSource = new ExtractorMediaSource(Uri.parse(
                                    mensaje),
                                    dataSourceFactory, extractorsFactory, null, null);
                            final LoopingMediaSource loopingSource = new LoopingMediaSource(videoSource);

                            // Prepare the player with the source.
                            player.prepare(videoSource);
                            player.addListener(new ExoPlayer.EventListener() {


                                @Override
                                public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

                                }

                                @Override
                                public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                                    Log.v(TAG, "Listener-onTracksChanged... ");
                                }

                                @Override
                                public void onLoadingChanged(boolean isLoading) {

                                }

                                @Override
                                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                                    Log.v(TAG, "Listener-onPlayerStateChanged..." + playbackState + "|||isDrawingCacheEnabled():" + simpleExoPlayerView.isDrawingCacheEnabled());
                                }

                                @Override
                                public void onRepeatModeChanged(int repeatMode) {

                                }

                                @Override
                                public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

                                }

                                @Override
                                public void onPlayerError(ExoPlaybackException error) {
                                    Log.v(TAG, "Listener-onPlayerError...");
                                    player.stop();
                                    player.prepare(loopingSource);
                                    player.setPlayWhenReady(true);
                                }

                                @Override
                                public void onPositionDiscontinuity(int reason) {

                                }

                                @Override
                                public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

                                }

                                @Override
                                public void onSeekProcessed() {

                                }
                            });
                            player.setPlayWhenReady(true); //run file/link when ready to play.


                            //Player

                            //Player


                        }
                    });
                }).on("tts", args -> {
                    idiomadevice = Locale.getDefault().getLanguage();
                    OkHttpClient client = new OkHttpClient();
                    final JSONObject obj = (JSONObject) args[0];
                    try {
                        mensaje = obj.getString("message");
                        Request request = new Request.Builder()
                                .url("https://translation.googleapis.com/language/translate/v2?key="+ KeysNonVersionControl.KEY_API_TRANSLATE +"&q="+mensaje+"&target="+idiomadevice+"")
                                .build();
                        Response response = client.newCall(request).execute();
                        final JSONObject jsonObject = new JSONObject(response.body().string());
                        Log.d("JSON", mensaje);
                        Log.d("JSON", idiomadevice);
                        Log.d("JSON", jsonObject.toString());

                        Log.d("Json", jsonObject.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText"));

                        runOnUiThread(() -> {

                            try {
                                String msg = jsonObject.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");
                                Calendar c = Calendar.getInstance();
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String strDate = sdf.format(c.getTime());
                                DialogAAADEVMessages.addMessages(strDate+"\n\n"+msg);
                                messageview.setBackgroundResource(R.drawable.new_mail);
                                if(dialogAAADEVMessages.isShowing()){
                                    dialogAAADEVMessages.updateMessages();
                                } else {
                                    dialogAAADEVMessages.show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {}

                });
                socket.connect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBroadcastReceiver != null){
            try {
                unregisterReceiver(mBroadcastReceiver);
            }
            catch (Exception ex){
                Log.e(TAG, "Failed to unregister receiver.", ex);
            }
        }

        if(finishCallDialerActivityReciver!=null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(finishCallDialerActivityReciver);
    }

    public boolean isAccessibilityEnabled;
    public boolean isExploreByTouchEnabled;

    public void backToFullScreen(){
        if (!isAccessibility) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            fullScreenViewResize(1056);
        } else if (!ActiveCallFragment.IS_ACTIVE) {
            fullScreenViewResize(1000);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "OnResume");
        if (!isAccessibility) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            fullScreenViewResize(1056);
        } else if (!ActiveCallFragment.IS_ACTIVE) {
            fullScreenViewResize(1000);
        }

        super.onResume();

        if (PermissionManager.somePermissionsDenied(this, getIntent())) {
            finish();
        }

        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        isAccessibilityEnabled = am.isEnabled();
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.LOCAL_CONFIG_CHANGE);
        intentFilter.addAction(Constants.SNACKBAR_SHOW);
        intentFilter.addAction(Constants.BLUETOOTH_STATE_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalBroadCastReceiver, intentFilter);
        checkForErrors();
        ElanApplication.setIsMainActivityVisible(true);

        IntentFilter filterBluetooth = new IntentFilter();
        filterBluetooth.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filterBluetooth.addAction(CONNECTION_STATE_CHANGED);

        if (ElanApplication.isConfigChange()) {
            applyConfigChange();
            //reset config change flag
            ElanApplication.setApplyConfigChange(false);
            Log.v(TAG, "done applying postponed configchanges in onResume");
        }

        if (mCallStateEventHandler != null) {
            mCallStateEventHandler.onActivityResume();
        }
        if (mSipUserDisplayObserver != null) {
            getContentResolver().unregisterContentObserver(mSipUserDisplayObserver);
            getContentResolver().registerContentObserver(mSipUserDisplayObserver.getUri(), true, mSipUserDisplayObserver);
        }
        String name = VantageDBHelper.getParameter(getContentResolver(), VantageDBHelper.SIPUSERNAME);
        if ((name != null) && (mLoggedUserExtension != null)) {
            mLoggedUserExtension.setText(name);
        }

        if (mSipUserNumberDisplayObserver != null) {
            getContentResolver().unregisterContentObserver(mSipUserNumberDisplayObserver);
            getContentResolver().registerContentObserver(mSipUserNumberDisplayObserver.getUri(), true, mSipUserNumberDisplayObserver);
        }
        String number = VantageDBHelper.getParameter(getContentResolver(), VantageDBHelper.SIPUSERNUMBER);
        if ((number != null) && (mLoggedUserNumber != null)) {
            mLoggedUserNumber.setText(number);
        }

        String mActivePhoneApp = VantageDBHelper.getParameter(getContentResolver(), VantageDBHelper.ACTIVE_CSDK_BASED_PHONE_APP);
        if (mActivePhoneApp != null && !mActivePhoneApp.equals(BuildConfig.APPLICATION_ID)) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.default_app_closing))
                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> finish())
                    .setOnDismissListener(dialog -> finish())
                    .show();
        }

        SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceListener = (sharedPreferences, key) -> {
            if ((key.equals(Constants.NAME_DISPLAY_PREFERENCE) || key.equals(Constants.NAME_SORT_PREFERENCE))
                    && (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getFragmentContacts() != null)) {
                mSectionsPagerAdapter.getFragmentContacts().userSettingsChanged();
            } else if (key.equals(Constants.BLUETOOTH_CONNECTED)) {
                if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getFragmentContacts() != null) {
                    mSectionsPagerAdapter.getFragmentContacts().PBAPRefreshState();
                }
                if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getFragmentCallHistory() != null) {
                    mSectionsPagerAdapter.getFragmentCallHistory().PBAPRefreshState();
                }
            }
        };

        getSharedPreferences(Constants.CONNECTION_PREFS, MODE_PRIVATE).registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        if(mRecentCallAndContactObserver != null) {
            try {
                getContentResolver().registerContentObserver(BRIO_CALL_LOGS_URI, true, mRecentCallAndContactObserver);
            }
            catch (SecurityException ex){
                Log.w(TAG, "Registering content observer for BRIO_CALL_LOGS_URI failed with exception: ", ex);
            }

            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED ) {
                getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, mRecentCallAndContactObserver);
            } else {
                Log.w(TAG, "onResume: no permission to access contacts");
            }
        }

        restoreIncomingCalls();

        setVideoMuteisibility();

        mHandler.removeCallbacks(mClearDigitsRunnable);

    }

    @Override
    public void setNameExtensionVisibility(int extensionNameDisplayOption){

        ViewGroup.LayoutParams params = mUser.getLayoutParams();
        mUser.setVisibility(View.VISIBLE);

        Log.d(TAG, "setNameExtensionVisibility to " + extensionNameDisplayOption);
        switch (extensionNameDisplayOption) {
            case 0:
                mLoggedUserExtension.setVisibility(View.VISIBLE);
                mLoggedUserNumber.setVisibility(View.VISIBLE);
                break;
            case 1:
                mLoggedUserExtension.setVisibility(View.VISIBLE);
                mLoggedUserNumber.setVisibility(View.GONE);
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mUser.setLayoutParams(params);
                break;
            case 2:
                mLoggedUserExtension.setVisibility(View.GONE);
                mLoggedUserNumber.setVisibility(View.VISIBLE);
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mUser.setLayoutParams(params);
                break;
            case 3:
                mStatusLayout.setVisibility(View.GONE);
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mUser.setLayoutParams(params);
                break;
        }

    }

    /**
     * Modifies the UI to adopt screen orientation
     * @param show
     */
    public void changeUiForFullScreenInLandscape(boolean show){
        try {
            checkForErrors();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Show call status if there is an active call and user navigates
     * to contact details screen after search screen.
     *
     */
    public void showCallStatusAfterSearch() {
        if (!CallStatusFragment.isCallStatusVisible() && SDKManager.getInstance().getCallAdaptor().hasActiveHeldOrInitiatingCall()) {
            CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
            assert callStatusFragment != null;
            callStatusFragment.showCallStatus();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadCastReceiver);

        if (mSipUserDisplayObserver != null) {
            getContentResolver().unregisterContentObserver(mSipUserDisplayObserver);
        }
        if (mSipUserNumberDisplayObserver != null) {
            getContentResolver().unregisterContentObserver(mSipUserNumberDisplayObserver);
        }
        ElanApplication.setIsMainActivityVisible(false);

        if (mCallStateEventHandler != null) {
            mCallStateEventHandler.onActivityPause();
        }
        if(getContentResolver() != null && mRecentCallAndContactObserver != null)
            getContentResolver().unregisterContentObserver(mRecentCallAndContactObserver);

        mHandler.postDelayed(mClearDigitsRunnable, CLEAR_DIGITS_DELAY);
    }

    private final Runnable mClearDigitsRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSectionsPagerAdapter.getDialerFragment() != null)
                mSectionsPagerAdapter.getDialerFragment().clear();
        }
    };

    /**
     * Load contacts in case it is first run of application or in case it was already
     * started perform reloading of data
     */
    private void initLocalContactsDownload() {
        if (mFirstLoad) {
            getLoaderManager().initLoader(Constants.LOCAL_ADDRESS_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
            getLoaderManager().initLoader(Constants.DIRECTORY_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
            mFirstLoad = false;
        } else {
            getLoaderManager().restartLoader(Constants.LOCAL_CONTACTS_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
            getLoaderManager().restartLoader(Constants.LOCAL_ADDRESS_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
            getLoaderManager().restartLoader(Constants.LOCAL_NAME_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
        }
    }

    /**
     * Check if mUser has accepted EULA and if application is supported on device.
     * Same Preference is reused since mUser can't move to application without accepting EULA.
     *
     * @return Continue with application loading.
     * Application will be activated if mUser have accepted EULA.
     */
    private boolean firstActivationCheck(Intent intent) {
        SharedPreferences preferences = getSharedPreferences(Constants.EULA_PREFS_NAME, MODE_PRIVATE);
        boolean eulaAccepted = preferences.getBoolean(Constants.KEY_EULA_ACCEPTED, false);
        if (!eulaAccepted) {
            if (!isDeviceSupported()) {
                showDeviceNotSupportedAlert();
            } else {

                // check whether this is first time activation, but the emergency logic has to be activated
                if ((intent != null) && (intent.getAction() != null) && intent.getAction().equalsIgnoreCase(EMERGENCY_CALL_INTENT)) {
                    final Uri telData = intent.getData();
                    final String toNum = (telData == null) ? "" : PhoneNumberUtils
                            .stripSeparators(getPhoneNumberFromTelURI(Uri.decode(telData.toString())));
                    mEmergencyWithoutLogin = SDKManager.getInstance().getDeskPhoneServiceAdaptor().isEmergencyNumber(toNum);
                    if (mEmergencyWithoutLogin) {
                        // this is first time activation, but it is emergency case - proceed with the application without Legal part
                        return true;
                    }
                }

                checkForLegal();
            }
            return false;
        }
        // If application is running for the first time, shortcut will be made.
        return true;
    }

    /**
     * Show dialog alert if device is not supported in Client SDK.
     */
    private void showDeviceNotSupportedAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(BaseActivity.this);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder
                .setMessage(R.string.device_not_supported)
                .setCancelable(false)
                .setPositiveButton("Ok", (dialog, id) -> finish());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Disable application usage on non supported devices
     * Only K165 and K175 models are supported atm.
     *
     * @return Device is supported.
     */
    private boolean isDeviceSupported() {
        //noinspection RedundantIfStatement
        if (Build.MODEL.equals("K165") || Build.MODEL.equals("K175")
                || Build.MODEL.equals("Vantage")
                || Build.MODEL.startsWith("Avaya Vantage")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Close MainActivity and open EULA screen if mUser hasn't accepted EULA.
     */
    private void checkForLegal() {
        //if MainLegalActivity is already started, there is not need to start it again
        ActivityManager activityManager = getSystemService(ActivityManager.class);
        if (activityManager.getAppTasks().size() != 0 && activityManager.getAppTasks().get(0).getTaskInfo().baseActivity.getClassName().equals(MainLegalActivity.class.getName())) {
            finish();
            return;
        }
        Intent legalIntent = new Intent(this, MainLegalActivity.class);
        legalIntent.putExtra("startFromSettings", false);
        startActivity(legalIntent);
        finish();
    }

    /**
     * Checking which fragment is active
     */
    void initViewPager() {
        mUIDeskphoneServiceAdaptor.setHookListener(this);
        //mUIDeskphoneServiceAdaptor.setHardButtonListener(this);
        Intent intent = getIntent();
        int oldPosition = intent.getIntExtra(TAB_POSITION, 0);

        initViewPagerListener();

        //Make sure placeholder fragments are created
        for (int i = 0; i < mTabIndexMap.size(); i++) {
            mViewPager.setCurrentItem(i, false);
        }

        mViewPager.setCurrentItem(oldPosition, false);

    }

    private void initViewPagerListener() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                onPageScrolledLogic();
            }

            @Override
            public void onPageSelected(int position) {
                Utils.hideKeyboard(BaseActivity.this);
                Tabs selectedTab = Tabs.Dialer;
                for (Tabs t : mTabIndexMap.keySet()) {
                    if (position == mTabIndexMap.get(t)) {
                        selectedTab = t;
                    }
                }
                if(selectedTab!= Tabs.Dialer){
                    mContactCapableTab = selectedTab;
                }

                onPageSelectedCondition();

                switch (selectedTab) {
                    case Dialer:
                        if (mSectionsPagerAdapter.getDialerFragment() != null) {
                            mSectionsPagerAdapter.getDialerFragment().fragmentSelected(mSectionsPagerAdapter.getVoiceMessageAdaptor().voiceMsgsState());
                        }
                        changeButtonsVisibility(Tabs.Dialer);

                        break;
                    case Favorites:
                        if (mSectionsPagerAdapter.getFragmentFavorites() == null) {
                            mSectionsPagerAdapter.setFragmentFavorites(FavoritesFragment.newInstance(mSectionsPagerAdapter.isCallAddParticipant()));
                        }
                        mSectionsPagerAdapter.getFragmentFavorites().fragmentSelected();
                        changeButtonsVisibility(Tabs.Favorites);

                        break;
                    case Contacts:
                        if (mSectionsPagerAdapter.getFragmentContacts() == null) {
                            mSectionsPagerAdapter.setFragmentContacts(ContactsFragment.newInstance(mSectionsPagerAdapter.isCallAddParticipant()));
                        }
                        mSectionsPagerAdapter.getFragmentContacts().fragmentSelected();
                        changeButtonsVisibility(Tabs.Contacts);

                        break;
                    case History:
                        if (mSectionsPagerAdapter.getFragmentCallHistory() == null) {
                            mSectionsPagerAdapter.setFragmentCallHistory(CallHistoryFragment.newInstance(1, mSectionsPagerAdapter.isCallAddParticipant()));
                            // If its first load instantiate contacts tab as well
                            if (mSectionsPagerAdapter.getFragmentContacts() == null) {
                                mSectionsPagerAdapter.setFragmentContacts(ContactsFragment.newInstance(mSectionsPagerAdapter.isCallAddParticipant()));
                            }
                            new Handler().postDelayed(() -> {
                                if (mSectionsPagerAdapter.getFragmentContacts() != null) {
                                    mSectionsPagerAdapter.getFragmentContacts().checkIfContactsLoaded();
                                } else {
                                    Log.d(TAG, "Fragment contact not init yet");
                                }
                            }, 2000);
                        }
                        mSectionsPagerAdapter.getFragmentCallHistory().fragmentSelected();
                        if (mFullyInitd)
                            resetMissedCalls();
                        changeButtonsVisibility(Tabs.History);

                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    protected abstract void onPageScrolledLogic();

    protected abstract void onPageSelectedCondition();

    public void changeButtonsVisibility(Tabs tab){}

    public Tabs getSelectedTab() {
        for (Tabs t : mTabIndexMap.keySet()) {
            if (mViewPager != null && mViewPager.getCurrentItem() == mTabIndexMap.get(t)) {
                return t;
            }
        }
        return Tabs.Dialer;
    }

    /**
     * Reset TabLayout in case our fragment is null
     * due to unpredicted reasons.
     */
    private void tabLayoutReset() {
        try {
            DialerFragment dialerFragment = mSectionsPagerAdapter.getDialerFragment();
            mTabLayout.removeAllTabs();
            if (mSectionsPagerAdapter.getCount() != 0) {
                mSectionsPagerAdapter.setAllowReconfiguration(true);
                mSectionsPagerAdapter.notifyDataSetChanged();
                mSectionsPagerAdapter.setAllowReconfiguration(false);
            } else {
                mSectionsPagerAdapter.configureTabLayout();
            }
            mViewPager.setAdapter(mSectionsPagerAdapter);
            mTabLayout.setupWithViewPager(mViewPager);
            setTabIcons();

            if (mSectionsPagerAdapter.getDialerFragment() != null && dialerFragment != null) {
                mSectionsPagerAdapter.getDialerFragment().setMode(dialerFragment.getMode());
            }
            if (isFragmentVisible(CONTACTS_EDIT_FRAGMENT)) {
                ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).cancelOnClickListener();
            }
            changeButtonsVisibility(getSelectedTab());
        } catch (Exception e) {
            Log.e(TAG, "tabLayoutReset failed", e);
        }
    }

    /**
     * Change missed call counter to 0 and refresh tab icon.
     */
    private void resetMissedCalls() {
        if(mNotifFactory != null) {
            mNotifFactory.removeAll();
        }
        if (mCallPreference != null && mCallPreference.getInt(Constants.KEY_UNSEEN_MISSED_CALLS, 0) > 0) {
            SharedPreferences.Editor editor = mCallPreference.edit();
            editor.putInt(Constants.KEY_UNSEEN_MISSED_CALLS, 0);
            editor.apply();
            setTabIcons();
        }
    }

    /**
     * this method is used to setup onClickListener for objects
     */
    private void setupOnClickListeners() {

        mToggleAudioMenu.setOnClickListener(this);
        mSelectAudio.setOnClickListener(this);
        mOptionHandset.setOnClickListener(this);
        mOptionBTHeadset.setOnClickListener(this);
        mOptionHeadset.setOnClickListener(this);
        mOptionUsbHeadset.setOnClickListener(this);
        mOption35Headset.setOnClickListener(this);
        mOptionSpeaker.setOnClickListener(this);
        mOptionUserAbout.setOnClickListener(this);
        mOptionUserSettings.setOnClickListener(this);
        mFrameAll.setOnClickListener(this);
        mUser.setOnClickListener(this);
        mAudioMute.setOnClickListener(this);
        mVideoMute.setOnClickListener(this);
        mVideoMute.setClickable(false);
        mClosePicker.setOnClickListener(this);

        mToggleAudioButton.setOnClickListener(this);
        mErrorStatus.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), ErrorMessageActivity.class);
            startActivity(intent);
        });

        setupMoreOnClickListenersForDevice();
    }

    void setupMoreOnClickListenersForDevice(){}

    /**
     * Preparing and showing list of available audio devices
     *
     * @param view {@link View} to be shown
     */
    private void showAudioList(View view) {
        List<UIAudioDevice> deviceList = new ArrayList<>();
        boolean shouldDisplayHanset = false;
        boolean shouldDisplay35Headset = false;
        boolean shouldDisplayHeadset = false;
        boolean shouldDisplayUsbHeadset = false;
        boolean shouldDisplayBTHeadset = false;
        if(mAudioDeviceViewAdaptor.getAudioDeviceList()!=null){
            deviceList = mAudioDeviceViewAdaptor.getAudioDeviceList();
            shouldDisplayHanset = (deviceList.contains(UIAudioDevice.HANDSET) || deviceList.contains(UIAudioDevice.WIRELESS_HANDSET)) && mAudioDeviceViewAdaptor.isDeviceOffHook();
            shouldDisplay35Headset = deviceList.contains(UIAudioDevice.WIRED_HEADSET);
            shouldDisplayHeadset = deviceList.contains(UIAudioDevice.RJ9_HEADSET);
            shouldDisplayUsbHeadset = deviceList.contains(UIAudioDevice.WIRED_USB_HEADSET);
            shouldDisplayBTHeadset = deviceList.contains(UIAudioDevice.BLUETOOTH_HEADSET);

        }


        view.findViewById(R.id.containerHandset).setEnabled(shouldDisplayHanset);
        view.findViewById(R.id.handset_image_view).setEnabled(shouldDisplayHanset);
        view.findViewById(R.id.handset_text_view).setEnabled(shouldDisplayHanset);

        view.findViewById(R.id.container35Headset).setEnabled(shouldDisplay35Headset);
        view.findViewById(R.id.headset35_image_view).setEnabled(shouldDisplay35Headset);
        view.findViewById(R.id.headset35_text_view).setEnabled(shouldDisplay35Headset);

        view.findViewById(R.id.containerHeadset).setEnabled(shouldDisplayHeadset);
        view.findViewById(R.id.headset_image_view).setEnabled(shouldDisplayHeadset);
        view.findViewById(R.id.headset_text_view).setEnabled(shouldDisplayHeadset);

        view.findViewById(R.id.containerUsbHeadset).setEnabled(shouldDisplayUsbHeadset);
        view.findViewById(R.id.usb_headset_image_view).setEnabled(shouldDisplayUsbHeadset);
        final View usbHeadsetLabel = view.findViewById(R.id.usb_headset_text_view);
        usbHeadsetLabel.setEnabled(shouldDisplayUsbHeadset);
        if(shouldDisplayUsbHeadset) {
            final List<AudioDevice> devices = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getAudioInterface().getDevices();

            AudioDevice usb = null;
            for (AudioDevice device: devices) {
                if (device.getType() == AudioDevice.Type.USB_HEADSET){
                    usb = device;
                    break;
                }
            }
            ((TextView) usbHeadsetLabel).setText((usb != null) ? usb.getName() : getString(R.string.select_audio_dev_usb_headset));
        }
        else {
            ((TextView) usbHeadsetLabel).setText(getString(R.string.select_audio_dev_usb_headset));
        }

        view.findViewById(R.id.containerBTHeadset).setEnabled(shouldDisplayBTHeadset);
        view.findViewById(R.id.bt_headset_image_view).setEnabled(shouldDisplayBTHeadset);
        view.findViewById(R.id.bt_headset_text_view).setEnabled(shouldDisplayBTHeadset);
    }

    /**
     * Initializing and pre-loading all necessary CSDK listeners,
     * preparing adaptors and state handlers.
     */
    private void initCSDK() {

        Log.d(TAG, "initCSDK");

        mCallViewAdaptor = new UICallViewAdaptor();
        SDKManager.getInstance().getCallAdaptor().registerListener(mCallViewAdaptor);
        mCallViewAdaptor.setCallControlsInterface(this);
        mCallStateEventHandler = new CallStateEventHandler(getSupportFragmentManager(), mCallViewAdaptor, this);

        mUIDeskphoneServiceAdaptor = new UIDeskPhoneServiceAdaptor(getApplicationContext(), this,this);
        SDKManager.getInstance().getDeskPhoneServiceAdaptor().registerListener(mUIDeskphoneServiceAdaptor);

        mAudioDeviceViewAdaptor = new UIAudioDeviceViewAdaptor();
        SDKManager.getInstance().getAudioDeviceAdaptor().registerListener(mAudioDeviceViewAdaptor);
        mAudioDeviceViewAdaptor.setDeviceViewInterface(this);

        mSectionsPagerAdapter.setVoiceMessageAdaptor(new UIVoiceMessageAdaptor(mSectionsPagerAdapter.getDialerFragment()));
        SDKManager.getInstance().getVoiceMessageAdaptor().registerListener(mSectionsPagerAdapter.getVoiceMessageAdaptor());

        // TODO used for testing. Need to run check for errors from adapter on error appearance.
//        ErrorManager.getInstance().addErrorToList(3);
//        ErrorManager.getInstance().addErrorToList(8);
        checkForErrors();

    }

    /**
     * Setup starting params.
     * Used for config changes in runtime.
     */
    private void handleFirstLoadParams() {
        mLocale = getResources().getConfiguration().locale;
        mFontScale = getResources().getConfiguration().fontScale;
    }

    /**
     * Handle runtime config changes.
     * Restart all views by restarting activity.
     */
    private void handleConfigChanges() {
        if (mViewPager!=null && isConfigChanged && SDKManager.getInstance().getCallAdaptor().getActiveCallId() == 0) {
            Intent intent = getIntent();
            if (intent != null) {
                intent.putExtra(TAB_POSITION, mViewPager.getCurrentItem());
                finish();
                startActivity(intent);
            }
        }
    }

    /**
     * Set up Fullscreen mode of applications
     * Change dimensions of view
     */
    private void UiChangeListener() {
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                if (!isAccessibility) {
                    fullScreenViewResize(1056);
                }
            }
        });
    }

    /**
     * Change views dimension when fullscreen mode is setup
     *
     * @param startDimension start layout dimension in pixel
     */
    private void fullScreenViewResize(int startDimension) {
        if (true)  {
            // this resize is candidate to be removed so in the mean time just return;
            return;
        }
        if(mViewPager == null) {
            Log.w(TAG, "fullScreenViewResize was called before activity views were created");
            return;
        }

        fullScreenViewResizeLogic(startDimension);

        mCoordinatorLayout.getLayoutParams().height = startDimension - 48;
        mActiveCall.getLayoutParams().height = startDimension - 48;
        mFrameAll.getLayoutParams().height = startDimension - 48;
        mEditContact.getLayoutParams().height = startDimension - 28;

    }

    abstract void fullScreenViewResizeLogic(int startDimension);


    /**
     * Checks if any SDK error code is active.
     * If true, show error notification image on top.
     */
    void checkForErrors() {
        boolean[] errorList = ErrorManager.getInstance().getErrorList();

        if (mErrorStatus == null) {
            return;
        }
        mErrorStatus.setVisibility(View.GONE);
        for (int errorCode = 0; errorCode < errorList.length; errorCode++) {
            if (errorList[errorCode]) {
                mErrorStatus.setVisibility(View.VISIBLE);
                mNotifFactory.showOnLine(ErrorManager.getErrorMessage(getApplicationContext(), errorCode));
                return;
            }
        }
    }

    /**
     * Provide us with brand logo URL in form of String from Config Parameters
     *
     * @return String with logo URL in case is available or empty String if not available
     */
    @NonNull
    private String getBrand() {
        //TODO: get brand url from settings
        String brandUrl = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.BRAND_URL);
        if (TextUtils.isEmpty(brandUrl)) {
            return "";
        } else {
            return brandUrl;
        }
    }

    /**
     * Perform loading of brand logo image in brand logo ImageView
     *
     * @param url of logo image
     */
    private void loadBrand(String url) {
        Log.d(TAG, "loadBrand(), url = " + url);
        String cachedUrl;
        mBrandView = findViewById(R.id.brand);
        if (mBrandPref.contains(ConfigParametersNames.BRAND_URL.getName())) {
            cachedUrl = mBrandPref.getString(ConfigParametersNames.BRAND_URL.getName(), "");
            if (TextUtils.isEmpty(url)) {
                url = cachedUrl;
            }
        }
        assert url != null;
        if (!TextUtils.isEmpty(url) && url.startsWith("http")) {
            if (mBrandView != null) {
                downloadAndSetBrandingFile(url);
            }

            mBrandPref.edit().putString(ConfigParametersNames.BRAND_URL.getName(), url).apply();
        } else {
            if (mBrandView != null) {
                mBrandView.setImageResource(R.drawable.ic_branding_avaya);
            }
        }
    }

    private void downloadAndSetBrandingFile(String url){
        Log.d(TAG, "downloadAndSetBrandingFile()");

        DownloadService downloadService = SDKManager.getInstance().getClient().getDownloadService();
        if (downloadService != null) {
            DownloadServiceConfiguration downloadServiceConfiguration = new DownloadServiceConfiguration();
            downloadServiceConfiguration.setCredentialProvider(new CredentialProvider() {
                @Override
                public void onAuthenticationChallenge(Challenge challenge, CredentialCompletionHandler credentialCompletionHandler) {
                    Log.d(TAG, "downloadAndSetBrandingFile() : onAuthenticationChallenge()");

                    String username = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.HTTP_AUTH_USERNAME);
                    String password = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.HTTP_AUTH_PASSWORD);

                    if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                        credentialCompletionHandler.onCredentialProvided(new UserCredential(username, password));
                    } else {
                        Log.d(TAG, "downloadAndSetBrandingFile() : onAuthenticationChallenge() - no username or password in database");
                    }
                }

                @Override
                public void onCredentialAccepted(Challenge challenge) {

                }

                @Override
                public void onAuthenticationChallengeCancelled(Challenge challenge) {

                }

                @Override
                public boolean supportsPreEmptiveChallenge() {
                    return false;
                }
            });

            try {
                downloadService.retrieveDataFromUrl(downloadServiceConfiguration, new URL(url), new DownloadCompletionHandler<byte[]>() {
                    @Override
                    public void onSuccess(byte[] downloaded) {
                        setBrandWithGlide(url, downloaded);
                    }

                    @Override
                    public void onError(DownloadServiceError error) {
                        Log.d(TAG, "retrieveDataFromUrl(): onError " + error.toString());

                    }
                });
            } catch (MalformedURLException e) {
                Log.d(TAG, "Failed to create URL. " + e);
            }
        }
    }

    private void setBrandWithGlide(String url, byte[] downloaded){
        Log.d(TAG, "setBrandWithGlide(). url = " + url);
        Glide.with(mBrandView.getContext()).clear(mBrandView);
        if (url.endsWith("gif")) {
            Glide.with(this)
                    .asGif()
                    .apply(new RequestOptions().fitCenter().error(R.drawable.ic_branding_avaya).diskCacheStrategy(DiskCacheStrategy.DATA))
                    .load(downloaded)
                    .transition(withCrossFade())
                    .into(mBrandView);
        } else {
            Glide.with(this)
                    .asBitmap()
                    .apply(new RequestOptions().error(R.drawable.ic_branding_avaya).fitCenter().diskCacheStrategy(DiskCacheStrategy.DATA))
                    .load(downloaded)
                    .into(mBrandView);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        isOnKeyDownHappened = true;

        int intRes = onKeyDownDeviceLogic(keyCode, event);

        if (intRes != -1){
            return intRes == 1;
        }

        if(DigitKeys.contains(event.getKeyCode()))
            return true;

        if ((event.getKeyCode() == KeyEvent.KEYCODE_MUTE)) {
            return false;
        }

        if ((event.getKeyCode() == KeyEvent.KEYCODE_MENU)) {
            onClickUser();
            return true;
        }

        return super.onKeyDown(keyCode, event);

    }

    abstract int onKeyDownDeviceLogic(int keyCode, KeyEvent event);

    /**
     * @param fragmentName name of the fragment
     * @return instance of the fragment if it is visible
     */
    Fragment getVisibleFragment(String fragmentName){
        try {
            FragmentManager fragmentManager = BaseActivity.this.getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible()) {
                    if (fragmentName.equalsIgnoreCase(DIALER_FRAGMENT)) {
                        if (fragment instanceof DialerFragment) {
                            return fragment;
                        }
                    }else if (fragmentName.equalsIgnoreCase(FAVORITES_FRAGMENT)) {
                        if (fragment instanceof FavoritesFragment) {
                            return fragment;
                        }
                    }else if (fragmentName.equalsIgnoreCase(CONTACTS_FRAGMENT)) {
                        if (fragment instanceof ContactsFragment) {
                            return fragment;
                        }
                    }else if (fragmentName.equalsIgnoreCase(HISTORY_FRAGMENT)) {
                        if (fragment instanceof CallHistoryFragment) {
                            return fragment;
                        }
                    }else if (fragmentName.equalsIgnoreCase(CONTACTS_DETAILS_FRAGMENT)) {
                        if (fragment instanceof ContactDetailsFragment) {
                            return fragment;
                        }
                    }else if (fragmentName.equalsIgnoreCase(CONTACTS_EDIT_FRAGMENT)) {
                        if (fragment instanceof ContactEditFragment) {
                            return fragment;
                        }
                    }else if (fragmentName.equalsIgnoreCase(ACTIVE_CALL_FRAGMENT)) {
                        if (fragment instanceof ActiveCallFragment) {
                            return fragment;
                        }
                    }else if (fragmentName.equalsIgnoreCase(ACTIVE_VIDEO_CALL_FRAGMENT)) {
                        if (fragment instanceof VideoCallFragment) {
                            return fragment;
                        }
                    }else if (fragmentName.equalsIgnoreCase(JOIN_MEETING_FRAGMENT)) {
                        if (fragment instanceof JoinMeetingFragment) {
                            return fragment;
                        }
                    }
                }

            }
        }catch (Exception e){
            return null;
        }
        return null;
    }

    /**
     * @param fragmentName name of the fragment whose visibility is tested
     * @return true if the fragment is visible
     */
    public boolean isFragmentVisible(String fragmentName){
        try {
            FragmentManager fragmentManager = BaseActivity.this.getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible()) {
                    if (fragmentName.equalsIgnoreCase(DIALER_FRAGMENT)) {
                        if (fragment instanceof DialerFragment) {
                            return true;
                        }
                    }else if (fragmentName.equalsIgnoreCase(FAVORITES_FRAGMENT)) {
                        if (fragment instanceof FavoritesFragment) {
                            return true;
                        }
                    }else if (fragmentName.equalsIgnoreCase(CONTACTS_FRAGMENT)) {
                        if (fragment instanceof ContactsFragment) {
                            return true;
                        }
                    }else if (fragmentName.equalsIgnoreCase(HISTORY_FRAGMENT)) {
                        if (fragment instanceof CallHistoryFragment) {
                            return true;
                        }
                    }else if (fragmentName.equalsIgnoreCase(CONTACTS_DETAILS_FRAGMENT)) {
                        if (fragment instanceof ContactDetailsFragment) {
                            return true;
                        }
                    }else if (fragmentName.equalsIgnoreCase(CONTACTS_EDIT_FRAGMENT)) {
                        if (fragment instanceof ContactEditFragment) {
                            return true;
                        }
                    }else if (fragmentName.equalsIgnoreCase(ACTIVE_CALL_FRAGMENT)) {
                        if (fragment instanceof ActiveCallFragment) {
                            return true;
                        }
                    }else if (fragmentName.equalsIgnoreCase(ACTIVE_VIDEO_CALL_FRAGMENT)) {
                        if (fragment instanceof VideoCallFragment) {
                            return true;
                        }
                    }else if (fragmentName.equalsIgnoreCase(JOIN_MEETING_FRAGMENT)) {
                        if (fragment instanceof JoinMeetingFragment) {
                            return true;
                        }
                    }
                }

            }
        }catch (Exception e){
            return false;
        }
        return false;
    }


    void sendAccessibilityEvent(String speech, View source) {
        AccessibilityManager manager = (AccessibilityManager)this.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(manager.isEnabled()){
            AccessibilityEvent aevent = AccessibilityEvent.obtain();
            aevent.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            aevent.setClassName(getClass().getName());
            aevent.getText().add(speech);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                aevent.setSource(source);
            }
            manager.sendAccessibilityEvent(aevent);

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && !isAccessibility) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            fullScreenViewResize(1056);
        } else if (!ActiveCallFragment.IS_ACTIVE) {
            fullScreenViewResize(1000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Processing interaction witj {@link DialerFragment}
     *
     * @param number String representation of number entered
     * @param action {@link DialerFragment.ACTION} on {@link DialerFragment}
     *
     * @return true if the number shall be kept and presented to the user after it was used; false - otherwise
     */
    @Override
    public boolean onDialerInteraction(String number, DialerFragment.ACTION action) {

        boolean keepNumberAfterUsage = true;

        try {
            if (action == DialerFragment.ACTION.DIGIT) {
                keepNumberAfterUsage = onDialerInteractionDeviceLogic(number);
            } else {
                boolean isVideo = (action == DialerFragment.ACTION.VIDEO);
                mCallViewAdaptor.createCall(number, isVideo);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return keepNumberAfterUsage;
    }

    abstract boolean onDialerInteractionDeviceLogic(String number);

    /**
     * Handle onNewIntent() to inform the fragment manager that the
     * state is not saved.  If you are handling new intents and may be
     * making changes to the fragment state, you want to be sure to call
     * through to the super-class here first.  Otherwise, if your state
     * is saved but the activity is not stopped, you could get an
     * onNewIntent() call which happens before onResume() and trying to
     * perform fragment operations at that point will throw IllegalStateException
     * because the fragment manager thinks the state is still saved.
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Method responsible for handling received Intents
     * Intents which are processed are
     *
     * @param intent
     * @INCOMING_CALL_INTENT
     * @NON_SERVICE_IMPACTING_CHANGE
     * @NON_SERVICE_IMPACTING_CHANGE
     */
    private void handleIntent(Intent intent) {

        if (intent.getAction() == null) {
            return;
        }

        Log.d(TAG, "handleIntent. action=" + intent.getAction());

        switch (intent.getAction()) {
            case Intent.ACTION_SEARCH:
                String query = intent.getStringExtra(SearchManager.QUERY);
                mSectionsPagerAdapter.getFragmentContacts().setQuery(query);
                break;
            case SERVICE_IMPACTING_CHANGE:
                SDKManager.getInstance().getDeskPhoneServiceAdaptor().applyConfigChanges(true);
                break;
            case NON_SERVICE_IMPACTING_CHANGE:
                applyConfigChange();
                break;
            case EMERGENCY_CALL_INTENT:
                final Uri telData = intent.getData();
                final String toNum = (telData == null) ? "" : PhoneNumberUtils
                        .stripSeparators(getPhoneNumberFromTelURI(Uri.decode(telData.toString())));
                if (!TextUtils.isEmpty(toNum)) {
                    Log.d(TAG, "Emergency call to " + toNum);
                    onDialerInteraction(toNum, DialerFragment.ACTION.AUDIO);
                }
                break;
            case SHOW_CALL_INTENT:
                int id = intent.getIntExtra(Constants.CALL_ID, 0);
                if (mCallActiveCallID != id) {
                    CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                    if (callStatusFragment != null && callStatusFragment.getCallStatusClickListener() != null) {
                        callStatusFragment.getCallStatusClickListener().onClick(null);
                    }
                }
                break;
            case Intent.ACTION_VIEW:
                if ("tel".equals(intent.getScheme())) {
                    String[] telParts = Objects.requireNonNull(intent.getData()).getSchemeSpecificPart().split(";");
                    //TODO: decide if to dial right away or just load the dialer
                    //boolean isVideo = intent.getDataString().contains("video");
                    //onDialerInteraction(PhoneNumberUtils.stripSeparators(telParts[0]), isVideo ? DialerFragment.ACTION.VIDEO : DialerFragment.ACTION.AUDIO);
                    if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                        mSectionsPagerAdapter.getDialerFragment().setDialer(PhoneNumberUtils.stripSeparators(telParts[0]));
                    }
                }
                //ELAN-1000
                try {
                    if (intent.hasExtra(Constants.GO_TO_FRAGMENT) && Constants.GO_TO_FRAGMENT_MISSED_CALLS.equalsIgnoreCase(intent.getStringExtra(Constants.GO_TO_FRAGMENT))) {

                        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CONTACTS) == true) {

                            if(isFragmentVisible(ACTIVE_CALL_FRAGMENT) || isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)){
                                ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mBackArrowOnClickListener();
                            }

                            ContactsFragment contactsFragment = (ContactsFragment)getVisibleFragment(CONTACTS_FRAGMENT);
                            if (contactsFragment != null) {
                                contactsFragment.setSearchVisibility(View.GONE);
                            }
                            mViewPager.setCurrentItem(3, false);
                        } else {

                            if(isFragmentVisible(ACTIVE_CALL_FRAGMENT) || isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)){
                                ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mBackArrowOnClickListener();
                            }
                            mViewPager.setCurrentItem(2, false);

                        }

                        if(isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)) {
                            ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).mBackListener.back();
                            //TODO: ETI - check if necessary
                            changeUiForFullScreenInLandscape(false);
                        }

                        if(isFragmentVisible(CONTACTS_EDIT_FRAGMENT)) {
                            ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).cancelOnClickListener();
                            //TODO: ETI - check if necessary
                            changeUiForFullScreenInLandscape(false);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;

            case DIAL_ACTION:
                if ("tel".equals(intent.getScheme())) {
                    String[] telParts = Objects.requireNonNull(intent.getData()).getSchemeSpecificPart().split(";");
                    //TODO: decide if to dial right away or just load the dialer
                    //boolean isVideo = intent.getDataString().contains("video");
                    //onDialerInteraction(PhoneNumberUtils.stripSeparators(telParts[0]), isVideo ? DialerFragment.ACTION.VIDEO : DialerFragment.ACTION.AUDIO);
                    if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                        mSectionsPagerAdapter.getDialerFragment().setDialer(PhoneNumberUtils.stripSeparators(telParts[0]));
                    }
                }
                break;
            case MainActivityK155.BRING_TO_FOREGROUND_INTENT:
                KeyguardManager kgMgr = Objects.requireNonNull(ElanApplication.getContext()).getSystemService(KeyguardManager.class);
                boolean isLocked = (kgMgr != null) && kgMgr.isDeviceLocked() && !ElanApplication.isPinAppLock;
                if (isLocked && SDKManager.getInstance().getCallAdaptor().hasActiveHeldOrInitiatingCall()){
                    CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                    if (callStatusFragment != null && callStatusFragment.getCallStatusClickListener() != null) {
                        callStatusFragment.getCallStatusClickListener().onClick(null);
                    }
                }
                break;

            case ACTION_CAMERA_ATTACHED:
                Log.d(TAG, "USB attached");
                setVideoControlsVisibility(View.VISIBLE);
                break;
            case ACTION_CAMERA_DETACHED:
                Log.d(TAG, "USB detached");
                setVideoControlsVisibility(View.GONE);
                break;
        }
    }

    /**
     * Processing interaction with {@link ContactsFragment}
     *
     * @param item {@link ContactData} to be presented in fragment
     */
    @Override
    public void onContactsFragmentInteraction(final ContactData item) {
        if (!mSectionsPagerAdapter.isCallAddParticipant()) {
            Log.d(TAG, "contact selected - show contact detail");
            mSectionsPagerAdapter.setContactDetails(item);
            setTabIcons();
        } else {
            final List<ContactData.PhoneNumber> contactPhones = new ArrayList<>(item.mPhones);
            if (contactPhones.size() > 0) {
                try {
                    // we assign appropriate adapter to number picker list
                    NumberPickerAdapter mNumberPickerAdapter = new NumberPickerAdapter(this, item, item.mPhones);
                    mNumberPickerList.setAdapter(mNumberPickerAdapter);

                    // since we have list of phone numbers here, we can handle list click directly from main activity
                    mNumberPickerList.setOnItemClickListener((parent, view, position, id) -> {

                        //after phone number is selected, we just hide invisible frame and list of phone numbers
                        mSelectPhoneNumber.setVisibility(View.INVISIBLE);
                        mFrameAll.setVisibility(View.GONE);

                        changeUiForFullScreenInLandscape(false);

                        // since method, that is used to add participant or transfer a call, accepts only contact data item,
                        // we create new contact data item that contains only one contact number (the one we selected)
                        String selectedPhone = contactPhones.get(position).Number.replaceAll("\\D+", ""); // extracting digits from the phone number
                        ContactData.PhoneType selectedPhoneType = contactPhones.get(position).Type;
                        ContactData.PhoneNumber selectedPhoneNumber = new ContactData.PhoneNumber(selectedPhone, selectedPhoneType, false, "");
                        List<ContactData.PhoneNumber> newList = new ArrayList<>();
                        newList.add(selectedPhoneNumber);
                        ContactData newItem = item.createNew(null, newList, item.mAccountType, "", "");
                        onCallAddParticipant(newItem);
                    });

                    // displaying contact name
                    mNumberPickerContactName.setText(item.mName);
                    // showing popup menu with contact phone numbers
                    expandPhoneNumberSlide();

                    mFrameAll.setVisibility(View.VISIBLE);
                } catch (NullPointerException e) {
                    Log.e(TAG, "onContactsFragmentInteraction", e);
                }
            } else {
                Utils.sendSnackBarData(getApplicationContext(), getString(R.string.contact_has_no_phone_numbers), false);
            }
        }
    }

    abstract void expandPhoneNumberSlide();

    /**
     * Setting up icons and description for tabs
     */
    private void setTabIcons() {

        Log.d(TAG, "setTabIcons");
        int idx = 0;
        mTabIndexMap.clear();
        dialerView = findViewById(R.id.dialer_tab);

        if (!mSectionsPagerAdapter.isCallAddParticipant()) {
            mTabIndexMap.put(Tabs.Dialer, idx);
            //noinspection ConstantConditions,ConstantConditions,ConstantConditions
            ((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(idx).setContentDescription(getString(R.string.dialer_content_description));
            TabLayout.Tab tab = mTabLayout.getTabAt(idx++);
            if (tab != null) {
                tab.setIcon(R.drawable.ic_dialpad);
            }
        }

        if (mSectionsPagerAdapter.isFavoriteTabPresent()) {
            Log.d(TAG, "setTabIcons favorites for " + idx);
            mTabIndexMap.put(Tabs.Favorites, idx);
            //noinspection ConstantConditions
            ((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(idx).setContentDescription(getString(R.string.favorites_content_description));
            TabLayout.Tab tab = mTabLayout.getTabAt(idx++);
            if (tab != null) {
                tab.setIcon(R.drawable.ic_favorites);
            }
        }

        if (mSectionsPagerAdapter.isContactsTabPresent()) {
            Log.d(TAG, "setTabIcons contacts for " + idx);
            mTabIndexMap.put(Tabs.Contacts, idx);
            //noinspection ConstantConditions

            ((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(idx).setContentDescription(getString(R.string.contacts_content_description));
            TabLayout.Tab tab = mTabLayout.getTabAt(idx++);
            if (tab != null) {
                tab.setIcon(R.drawable.ic_contacts);
            }

            setTabIconsDeviceLogic();
        }
        else
            changeButtonsVisibility(Tabs.Dialer);

        if (mSectionsPagerAdapter.isRecentTabPresent()) {
            Log.d(TAG, "setTabIcons recents for " + idx);
            mTabIndexMap.put(Tabs.History, idx);
            ((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(idx).setContentDescription(getString(R.string.recent_calls_content_description));
            setHistoryIcon(idx);
        }


        if (dialerView != null) {
            if (mTabLayout.getTabCount() < MINIMAL_AMOUNT_OF_TABS) {
                mTabLayout.setVisibility(View.GONE);
                dialerView.setVisibility(View.VISIBLE);
            } else {
                dialerView.setVisibility(View.GONE);
                mTabLayout.setVisibility(View.VISIBLE);
            }
        }

        ColorStateList colors;
        if (Build.VERSION.SDK_INT >= 23) {
            colors = getResources().getColorStateList(R.color.tab_tint, getTheme());
        } else {
            colors = this.getColorStateList(R.color.tab_tint);
        }

        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            Drawable icon = null;
            if (tab != null) {
                icon = tab.getIcon();
            }
            if (icon != null) {
                icon = DrawableCompat.wrap(icon);
                DrawableCompat.setTintList(icon, colors);
            }
        }
    }

    void setTabIconsDeviceLogic(){}


    /**
     * Updating number of missed calls for History tab in tab view
     *
     * @param pageNumber number of tab where icon have to be placed
     */
    @SuppressLint("InflateParams")
    private void setHistoryIcon(int pageNumber) {
        //noinspection ConstantConditions
        try {
            int numberOfMissedCalls = mCallPreference.getInt(Constants.KEY_UNSEEN_MISSED_CALLS, 0);
            if (numberOfMissedCalls > 0) {
                View tabVIew = null;
                ImageView missedCallBackground = null;
                if (mTabLayout.getTabAt(pageNumber) != null) {
                    //noinspection ConstantConditions
                    tabVIew = mTabLayout.getTabAt(pageNumber).getCustomView();
                }
                if (tabVIew == null) {
                    tabVIew = getLayoutInflater().inflate(R.layout.recent_tab_view, null);
                }
                if (numberOfMissedCalls > 99) {
//                    missedCallBackground = (ImageView) tabVIew.findViewById(R.id.recent_tab_number_background);
//                    if (missedCallBackground != null) {
//                        ViewGroup.LayoutParams params = missedCallBackground.getLayoutParams();
//                        params.width = getResources().getInteger(R.integer.recent_tab_big_width);
//                        missedCallBackground.setLayoutParams(params);
//                    }
                }
                try {

                    TextView numberView = tabVIew.findViewById(R.id.recent_tab_number);
                    numberView.setText(String.valueOf(numberOfMissedCalls));

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                if (mTabLayout.getTabAt(pageNumber) != null) {
                    //noinspection ConstantConditions
                    mTabLayout.getTabAt(pageNumber).setCustomView(tabVIew);
                }
            } else {
                if (mTabLayout.getTabAt(pageNumber) != null) {
                    //noinspection ConstantConditions
                    mTabLayout.getTabAt(pageNumber).setCustomView(null);
                    //noinspection ConstantConditions
                    mTabLayout.getTabAt(pageNumber).setIcon(R.drawable.ic_history);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Processing audio call for {@link ContactData}
     *
     * @param item        {@link ContactData} for which audio call is started
     * @param phoneNumber String representation of number which is called
     */
    @Override
    public void onCallContactAudio(ContactData item, String phoneNumber) {
        // scroll back to the first page to be prepared to any other operations
        // that might be activated via the active call interface
        makeCall(item, false, phoneNumber);
    }

    /**
     * Processing event of adding participant to call
     *
     * @param item {@link ContactData} to be added to call
     */
    @Override
    public void onCallAddParticipant(ContactData item) {
        callAddParticipant(item);
    }

    /**
     * This method prepares data for Conference or Transfer and creates Intent
     * for {@link #onActivityResult(int, int, Intent)} method
     *
     * @param contactData The {@link ContactData} item which should be add to the existing call
     */
    private void callAddParticipant(ContactData contactData) {

        String mRequestName;
        if (isConferenceCall) {
            mRequestName = getResources().getString(R.string.merge_complete);
        } else {
            mRequestName = getResources().getString(R.string.trasfer_complete);
        }

        if (contactData.mPhones != null && contactData.mPhones.size() > 0) {
            String mContactNumber = setPhoneNumberPriority(contactData);
            Intent data = new Intent(getPackageName() + Constants.ACTION_TRANSFER);
            data.putExtra(Constants.CALL_ID, mCallActiveCallID);
            data.putExtra(Constants.IS_CONTACT, 1);
            data.putExtra(Constants.TARGET, mContactNumber);
            setResult(RESULT_OK, data);
            onActivityResult(mActiveCallRequestCode, RESULT_OK, data);
        } else {
            Snackbar.make(mActivityLayout, getResources().getString(R.string.contact_has_no_phone_numbers), Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Processing video call of contact
     *
     * @param item        {@link ContactData} which is called
     * @param phoneNumber String representation of phone number
     */
    @Override
    public void onCallContactVideo(ContactData item, String phoneNumber) {
        // scroll back to the first page to be prepared to any other operations
        // that might be activated via the active call interface
        makeCall(item, true, phoneNumber);
    }

    /**
     * Processing new contact creation with opening {@link ContactEditFragment}
     */
    @Override
    public void onCreateNewContact() {
        ContactEditFragment mContactEditFragment = ContactEditFragment.newInstance();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.edit_contact_frame, mContactEditFragment);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void checkFilterVisibility() {
        if (mSectionsPagerAdapter.getFragmentContacts() != null){
            mSectionsPagerAdapter.getFragmentContacts().checkFilterVisibility();
        }
    }

    /**
     * Processing search start event
     *
     * @param mSearchLayout {@link Rect}
     */
    @Override
    public void onStartSearching(Rect mSearchLayout) {
        mSearchArea = mSearchLayout;
        isSearchInProgress = true;
    }

    /**
     * Perform preparation for performing call.
     * Inform CSDK to perform call
     *
     * @param contactData {@link ContactData} for which call have to be done
     * @param isVideo     boolean is call video call
     * @param phoneNumber String phone number which have to be called
     */
    private void makeCall(ContactData contactData, boolean isVideo, String phoneNumber) {
        String number;

        if ((phoneNumber == null || phoneNumber.equals("")) && contactData.mPhones.size() > 0) {
            number = setPhoneNumberPriority(contactData);
        } else {
            number = phoneNumber;
        }

        Log.d(TAG, "contact audio call to Contact Name: " + contactData.mName + " Contact number: " + number);
        if (number != null && number.length() > 0) {
            SDKManager.getInstance().getCallAdaptor().createCall(number, isVideo, true);
        } else {
            Utils.sendSnackBarData(getApplicationContext(), getString(R.string.contact_has_no_phone_numbers), false);
        }
    }

    /**
     * Setting up a priority for numbers if contact have more than one
     *
     * @param contactData {@link ContactData} for which we are setting priority phone number
     * @return Phone number according to priority
     */
    private String setPhoneNumberPriority(ContactData contactData) {

        String number;
        String type;
        final String PRIORITY_WORK = "1";
        final String PRIORITY_MOBILE = "2";
        final String PRIORITY_CUSTOM = "3";
        final String PRIORITY_OTHER = "4";
        final String PRIORITY_HOME = "5";
        final String PRIORITY_DEFAULT = "6";

        List<String[]> priorityList = new ArrayList<>();

        for (ContactData.PhoneNumber phone : contactData.mPhones) {
            number = phone.Number;
            type = phone.Type.toString();

            // if we find primary number, just return that number
            if (phone.Primary) {
                return number;
            }

            // setting up priorities for phone numbers
            switch (type) {
                case "WORK":
                    priorityList.add(new String[]{PRIORITY_WORK, number});
                    break;
                case "MOBILE":
                    priorityList.add(new String[]{PRIORITY_MOBILE, number});
                    break;
                case "HOME":
                    priorityList.add(new String[]{PRIORITY_HOME, number});
                    break;
                case "OTHER":
                    priorityList.add(new String[]{PRIORITY_OTHER, number});
                    break;
                case "CUSTOM":
                    priorityList.add(new String[]{PRIORITY_CUSTOM, number});
                    break;
                default:
                    priorityList.add(new String[]{PRIORITY_DEFAULT, number});
            }
        }
        // sorting out the list according to priority
        Collections.sort(priorityList, (lhs, rhs) -> lhs[0].compareTo(rhs[0]));

        // returning first phone number
        return priorityList.get(0)[1];
    }

    @Override
    public void back() {
        mSectionsPagerAdapter.clearContactDetails();
        setTabIcons();
        Tabs selectedTab = getSelectedTab();
        switch (selectedTab) {
            case Favorites:
                mSectionsPagerAdapter.getFragmentFavorites().restoreListPosition(mSectionsPagerAdapter.getFavoritesListPosition());
                break;
            case Contacts:
                mSectionsPagerAdapter.getFragmentContacts().restoreListPosition(mSectionsPagerAdapter.getContactsListPosition());
                break;
            case History:
                mSectionsPagerAdapter.getFragmentCallHistory().restoreListPosition(mSectionsPagerAdapter.getRecentCallsListPosition());
                mSectionsPagerAdapter.getFragmentCallHistory().performSelectionByCategory(mSelectedCallCategory);

                //mSectionsPagerAdapter.getFragmentRecent().restoreListPosition(mSectionsPagerAdapter.getRecentCallsListPosition());
                //mSectionsPagerAdapter.getFragmentRecent().performSelectionByCategory(mSelectedCallCategory);
                //mSectionsPagerAdapter.getFragmentRecent().setLastVisibleItem(position);

                break;
            default:
                // default statement
        }

        backDeviceLogic(selectedTab);
    }

    void backDeviceLogic(Tabs selectedTab){}

    /**
     * Processing edit request for {@link ContactData} and starting new {@link ContactEditFragment}
     *
     * @param contactData  {@link ContactData} to be edited if exist
     * @param isNewContact boolean giving us information do we create new contact or we are editing
     *                     existing one
     */
    @Override
    public void edit(ContactData contactData, boolean isNewContact) {
        ContactEditFragment mContactEditFragment = ContactEditFragment.newInstance(contactData, isNewContact);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.edit_contact_frame, mContactEditFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    /**
     * Processing successful login made by user
     *
     * @param name      name of user
     * @param extension extension number of user
     */
    @Override
    public void onSuccessfulLogin(String name, String extension) {
        Log.d(TAG, "onSuccessfulLogin: name="+name + " extension="+extension);
        boolean mLoginGuardOneshot = true;
        if (isDestroyed()) {
            Log.e(TAG, "Activity is destroyed returning");
            return;
        }
        mLoggedUserNumber.setText(extension);
        mLoggedUserExtension.setText(name);
        loadBrand(getBrand(), true);

        applyLockSetting();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isSearchInProgress && mSearchArea != null && ev.getAction() == MotionEvent.ACTION_DOWN && !mSearchArea.contains((int) ev.getX(), (int) ev.getY())) {
            Utils.hideKeyboard(this);
        }
        try {
            return super.dispatchTouchEvent(ev);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    private String getDialedNumber(){
        if (mSectionsPagerAdapter.getDialerFragment() != null)
            return mSectionsPagerAdapter.getDialerFragment().getNumber();

        return null;
    }

    private void createCall(){
        String number = getDialedNumber();
        if (number != null && number.length() > 0) {
            mCallViewAdaptor.createCall(number, false);
            mSectionsPagerAdapter.getDialerFragment().clear();
        }
        else
            mCallViewAdaptor.createCall(false);
    }

    /**
     * Handling on clicks in simpler way
     *
     * @param clickView {@link View} of a clicked item
     */
    @Override
    public void onClick(View clickView) {
        try {
            switch (clickView.getId()) {
                case R.id.off_hook: // select audio button
                    if (clickView instanceof ToggleButton) {
                        if (((ToggleButton) clickView).isChecked()) {
                            int callId = SDKManager.getInstance().getCallAdaptor().isAlertingCall();
                            if (callId != 0) {
                                Intent intent = new Intent(INCOMING_CALL_ACCEPT);
                                intent.putExtra(Constants.CALL_ID, callId);
                                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                            } else if (SDKManager.getInstance().getCallAdaptor().getActiveCallId() == 0) {
                                if(isLockState(this)){
                                    setOffhookButtosChecked(false);
                                }
                                else {
                                    prepareOffHook();
                                    mAudioDeviceViewAdaptor.setUserRequestedDevice(mAudioDeviceViewAdaptor.getUserRequestedDevice());
                                    createCall();
                                }
                            }
                        } else {
                            int callId = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
                            if (callId == 0 && mCallStateEventHandler != null)
                                callId = mCallStateEventHandler.getCurretCallId();
                            mCallViewAdaptor.endCall(callId);
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT))
                                    ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).setMode(DialerFragment.DialMode.EDIT);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return;
                case R.id.audio_mute:
                    mCallViewAdaptor.audioMuteStateToggled();
                    break;
                case R.id.video_mute:
                    Log.d(TAG, "onClick: video_mute is " + mVideoMute.isChecked());
                    mVideoMute.setEnabled(false);
                    mVideoMute.setChecked(!((ToggleButton) clickView).isChecked());
                    mCallViewAdaptor.videoMuteStateToggled();
                    break;
                case R.id.containerBTHeadset:
                    Log.d(TAG, "onClick: BT Headset");
                    mSelectAudio.setBackgroundResource(R.drawable.pc_bluetooth);
                    setBackgroundResourceForDeviceId(clickView.getId());

                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.BLUETOOTH_HEADSET);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.BLUETOOTH_HEADSET.toString());
                    break;
                case R.id.containerHeadset:
                    Log.d(TAG, "onClick: headset");
                    mSelectAudio.setBackgroundResource(R.drawable.pc_headset);

                    setBackgroundResourceForDeviceId(clickView.getId());
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.RJ9_HEADSET);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.RJ9_HEADSET.toString());
                    break;
                case R.id.container35Headset:
                    Log.d(TAG, "onClick: 3.5 headset");
                    mSelectAudio.setBackgroundResource(R.drawable.pc_35mm);

                    setBackgroundResourceForDeviceId(clickView.getId());
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.WIRED_HEADSET);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.WIRED_HEADSET.toString());
                    break;
                case R.id.containerUsbHeadset:
                    Log.d(TAG, "onClick: USB headset");
                    mSelectAudio.setBackgroundResource(R.drawable.pc_usb_headset);

                    setBackgroundResourceForDeviceId(clickView.getId());
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.WIRED_USB_HEADSET);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.WIRED_USB_HEADSET.toString());
                    break;
                case R.id.containerHandset:
                    Log.d(TAG, "onClick: handset");
                    UIAudioDevice device = UIAudioDevice.HANDSET;
                    if (mAudioDeviceViewAdaptor.getAudioDeviceList() != null && mAudioDeviceViewAdaptor.getAudioDeviceList().contains(UIAudioDevice.WIRELESS_HANDSET)) {
                        device = UIAudioDevice.WIRELESS_HANDSET;
                    }
                    mSelectAudio.setBackgroundResource(R.drawable.pc_handset);

                    setBackgroundResourceForDeviceId(clickView.getId());
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(device);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, device.toString());
                    break;
                case R.id.containerSpeaker:
                    Log.d(TAG, "onClick: speaker");
                    mSelectAudio.setBackgroundResource(R.drawable.pc_off_hook);
                    setBackgroundResourceForDeviceId(clickView.getId());
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.SPEAKER);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.SPEAKER.toString());
                    break;
                case R.id.user:
                    if ( (SystemClock.elapsedRealtime() - mLastClickTime < 1000) ) {
                        return;
                    }
                    onClickUser();
                    return;
                case R.id.containerUserSettings:
                    Intent i = new Intent(BaseActivity.this, UserPreferencesActivity.class);
                    //i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(i);
                    break;
                case R.id.containerAbout:
                    Intent s = new Intent(BaseActivity.this, SupportActivity.class);
                    startActivity(s);
                    break;
                case R.id.pick_cancel:
                    cancelAddSomeOneScreen();

                    CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                    ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
                    int isActiveCall = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
                    int numOfCalls = SDKManager.getInstance().getCallAdaptor().getNumOfCalls();
                    int mNumCalls = mCallStateEventHandler.mCalls.size();
                    assert callStatusFragment != null;
                    if ((Objects.requireNonNull(callStatusFragment.getView()).getVisibility() == View.INVISIBLE || callStatusFragment.getView().getVisibility() == View.GONE) && isActiveCall != 0 && numOfCalls >= 1 && mNumCalls > 0) {
                        if (activeCallFragment != null && !activeCallFragment.isVisible()){
                            callStatusFragment.showCallStatus();
                        }
                    }
                    break;
                case R.id.transducer_button:
                    showAudioList(mToggleAudioMenu);
                    onClickTransducerButton();
                    // "return" must be used here to show the menus
                    return;

                case R.id.addcontact_button:
                    if (addcontactButton.getVisibility() == View.VISIBLE && !isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)) {

                        onCreateNewContact();

                        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("addcontact_button", true);
                        editor.apply();

                        return;
                    }
                    break;

                case R.id.filterRecent:
                    if (filterButton.getVisibility() == View.VISIBLE) {
                        CallHistoryFragment fragment = (CallHistoryFragment) getVisibleFragment(HISTORY_FRAGMENT);
                        if (fragment != null && fragment.isAdded()) {
                            fragment.onClick(clickView);
                            checkFilterButtonState();
                        }
                        return;
                    }
                    break;
                case R.id.search_button:
                    onClickSearchButton();
                    break;
                default:
                    hideMenus();
                    return;
            }
            hideMenus();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    abstract void onClickUser();

    abstract void onClickTransducerButton();

    abstract void onClickSearchButton();


    public void cancelAddSomeOneScreen(){

        cancelContactPicker();

        if (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0) {
            CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);

            if (callStatusFragment != null && SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 1) {
                callStatusFragment.hideCallStatus();
                Utils.hideKeyboard(this);
            }

            ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
            if (activeCallFragment != null) {
                activeCallFragment.setVisible();
            }
        }

        changeUIFor155();
    }

    void changeUIFor155(){}
    /**
     * Hiding all drop down and popup menus
     */
    void hideMenus() {
        mHandler.removeCallbacks(mLayoutCloseRunnable);
        if (mToggleAudioMenu.getVisibility() == View.VISIBLE) {
            collapseSlideSelecAudioDevice();
        }
        if (mListPreferences.getVisibility() == View.VISIBLE) {
            collapseSlideUserPreferences();
            mOpenUser.setImageDrawable(getDrawable(R.drawable.ic_expand_more));
        }

        if (mSelectPhoneNumber.getVisibility() == View.VISIBLE) {
            collapseSlideSelectPhoneNumber();
        }
        mFrameAll.setVisibility(View.GONE);
        mToggleAudioButton.setChecked(false);

        setTransducerButtonCheckedFor155();
    }

    abstract void collapseSlideSelecAudioDevice();

    abstract void collapseSlideUserPreferences();

    abstract void collapseSlideSelectPhoneNumber();

    void setTransducerButtonCheckedFor155(){}

    /**
     * Saving selected audio device
     *
     * @param audioKey   static value declared as AUDIO_PREF_KEY. Value "audioDevice"
     * @param audioValue value that determines what option is selected
     */

    void saveAudioSelection(String audioKey, String audioValue) {

        //get the current value from shared preference and compare it with request value
        String prefValue = mSharedPref.getString(Constants.AUDIO_PREF_KEY, (UIAudioDevice.SPEAKER).toString());
        if (audioValue.equals(prefValue)){
            //audio device has not been changed  - do nothing
            return;
        }

        if (audioKey != null) {
            //((AudioManager) getSystemService(AUDIO_SERVICE)).setSpeakerphoneOn(SPEAKER.equals(audioValue));
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString(audioKey, audioValue);
            editor.apply();

            mAudioMute.setChecked(false);
            mCallViewAdaptor.changeAudioMuteState(false);
        } else {
            Log.d(TAG, "audioKey is null");
        }
    }


    /**
     * Loading selected audio device based on {@link SharedPreferences} audio
     * preferences key
     */

    void updateAudioSelectionUI(UIAudioDevice device) {
        int resId = getResourceIdForDevice(device);
        mSelectAudio.setBackgroundResource(resId);
        setBackgroundResource(resId);
    }

    private int getResourceIdForDevice(UIAudioDevice device) {

        switch (device) {
            case SPEAKER:
                return R.drawable.pc_off_hook;

            case BLUETOOTH_HEADSET:
                return R.drawable.pc_bluetooth;

            case WIRED_HEADSET:
                return R.drawable.pc_35mm;

            case RJ9_HEADSET:
                return R.drawable.pc_headset;

            case WIRED_USB_HEADSET:
                return R.drawable.pc_usb_headset;

            case HANDSET:
            case WIRELESS_HANDSET:
                return R.drawable.pc_handset;

            case WIRED_SPEAKER:
                return R.drawable.pc_off_hook;

            default:
                return R.drawable.pc_off_hook;
        }
    }


    private int getResourceIdForDeviceId(int id) {

        switch (id) {
            case R.id.containerSpeaker:
                return R.drawable.pc_off_hook;

            case  R.id.containerBTHeadset:
                return R.drawable.pc_bluetooth;

            case R.id.container35Headset:
                return R.drawable.pc_35mm;

            case R.id.containerHeadset:
                return R.drawable.pc_headset;

            case R.id.containerUsbHeadset:
                return R.drawable.pc_usb_headset;

            case R.id.containerHandset:
                return R.drawable.pc_handset;

            default:
                return R.drawable.pc_off_hook;
        }
    }

    private void setBackgroundResourceForDeviceId(int id) {
        int resId = getResourceIdForDeviceId(id);
        setBackgroundResource(resId);
    }


    void setBackgroundResource(int resId){

    }



    /**
     * Loading last selected {@link UIAudioDevice} and updating selection for it
     */
    private void loadAudioSelection() {
        UIAudioDevice prefDevice = getDeviceFromSharedPref();
        Log.d(TAG, "setting audio device to " + prefDevice);
        mAudioDeviceViewAdaptor.setUserRequestedDevice(prefDevice);
        int resId = getResourceIdForDevice(prefDevice);
        mSelectAudio.setBackgroundResource(resId);
    }

    public UIAudioDevice getDeviceFromSharedPref() {
        String prefValue = mSharedPref.getString(Constants.AUDIO_PREF_KEY, (UIAudioDevice.SPEAKER).toString());
        assert prefValue != null;
        UIAudioDevice prefDevice = UIAudioDevice.valueOf(prefValue.toUpperCase());
        UIAudioDevice activeDevice = mAudioDeviceViewAdaptor.getActiveAudioDevice();
        Log.d(TAG, "prefDevice is:" + prefDevice + " and activeDevice is: " + activeDevice);
        if (prefDevice == UIAudioDevice.HANDSET && SDKManager.getInstance().getAudioDeviceAdaptor().isWirelessHandset()){
            Log.d(TAG, "setting prefDevice to WIRELESS_HANDSET");
            prefDevice = UIAudioDevice.WIRELESS_HANDSET;
            mSelectAudio.setChecked(true);
        }
        //if active device is handset and the device is indeed offhooked, need to ignore value read from shared preference
        if ((activeDevice == UIAudioDevice.HANDSET || activeDevice == UIAudioDevice.WIRELESS_HANDSET)  && SDKManager.getInstance().getAudioDeviceAdaptor().isDeviceOffHook()){
            Log.d(TAG, "setting prefDevice to active device " + activeDevice);
            prefDevice = activeDevice;
            mSelectAudio.setChecked(true);
        }
        if (mAudioDeviceViewAdaptor.getAudioDeviceList()!=null && mAudioDeviceViewAdaptor.getAudioDeviceList().contains(prefDevice)) {
            if (activeDevice != prefDevice && !isLockState(this)) {
                prefDevice = activeDevice;
            }
        } else {
            prefDevice = mAudioDeviceViewAdaptor.getUserRequestedDevice();
        }

        Log.d(TAG, "Device is " + prefDevice);
        saveAudioSelection(Constants.AUDIO_PREF_KEY, prefDevice.toString());

        return prefDevice;
    }

    /**
     * @return Resource ID of the Audio Device
     */
    public int getDeviceResIdFromSharedPref() {

        UIAudioDevice prefDevice = getDeviceFromSharedPref();
        return getResourceIdForDevice(prefDevice);
    }


    /**
     * Processing cancellation of editing contact
     */
    @Override
    public void cancelEdit() {
        Utils.hideKeyboard(this);
        if (!isFinishing()) {
            getSupportFragmentManager().popBackStack();
            if (getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame) != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.remove(Objects.requireNonNull(getSupportFragmentManager()
                        .findFragmentById(R.id.edit_contact_frame)))
                        .commitAllowingStateLoss();
            }
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (needCancelEdit) {
            cancelEditFragment();
            needCancelEdit = false;
        }
    }

    private void cancelEditFragment() {
        Fragment f = getVisibleFragment(CONTACTS_EDIT_FRAGMENT);
        if (f != null && f.getView() != null ) {
            View cancel = f.getView().findViewById(R.id.contact_edit_cancel);
            if (cancel != null) {
                cancel.performClick();
            }
        }
    }

    /**
     * Processing confirmation for local {@link ContactData} editing
     *
     * @param contactData  {@link ContactData}
     * @param imageUri     {@link Uri} for image which represent contact to be shown with contact
     * @param isNewContact boolean giving us information if this is new contact
     */
    @Override
    public void confirmLocalContactEdit(ContactData contactData, Uri imageUri, boolean isNewContact) {

        Utils.hideKeyboard(this);
        getSupportFragmentManager().popBackStack();
        if (getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame) != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame))).commit();
        }

        LocalContactsManager localContactsManager = new LocalContactsManager(this);
        if (!isNewContact) {
            localContactsManager.updateContact(contactData, imageUri);
        } else {
            localContactsManager.createContact(contactData, imageUri);
            if (isFragmentVisible(CONTACTS_FRAGMENT) && mSectionsPagerAdapter != null && mSectionsPagerAdapter.getFragmentContacts() != null){
                mSectionsPagerAdapter.getFragmentContacts().contactCreated();
            }
        }

        refreshSectionPager();
    }

    /**
     * Processing confirmation of enterprise {@link ContactData} editing.
     *
     * @param contactData     {@link ContactData}
     * @param editableContact {@link EditableContact}
     */
    @Override
    public void confirmEnterpriseEdit(ContactData contactData, EditableContact editableContact, boolean isNewContact) {
        Utils.hideKeyboard(this);
        getSupportFragmentManager().popBackStack();
        if (getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame) != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame))).commit();
        }

        if (!isNewContact){
            SDKManager.getInstance().getContactsAdaptor().startEnterpriseEditing(contactData, editableContact);
        } else {
            SDKManager.getInstance().getContactsAdaptor().createEnterpriseContact(contactData);
        }

        refreshSectionPager();
    }

    /**
     * After done with contact edit, return all fragments to previous stage
     */
    private void refreshSectionPager() {

        if (mSectionsPagerAdapter.getContactDetailsFragment() != null) {
            mSectionsPagerAdapter.clearContactDetails();
            setTabIcons();
            Tabs selectedTab = getSelectedTab();
            switch (selectedTab) {
                case Favorites:
                    mSectionsPagerAdapter.getFragmentFavorites().restoreListPosition(mSectionsPagerAdapter.getFavoritesListPosition());
                    break;
                case Contacts:
                    mSectionsPagerAdapter.getFragmentContacts().restoreListPosition(mSectionsPagerAdapter.getContactsListPosition());
                    break;
                case History:
                    mSectionsPagerAdapter.getFragmentCallHistory().restoreListPosition(mSectionsPagerAdapter.getRecentCallsListPosition());
                    break;
                default:
                    // default statement
            }

            backDeviceLogic(selectedTab);
        }
    }

    /**
     * Starting contact picker for call transfer process
     *
     * @param mCallId of active call to which we want to transfer picked contact
     */
    @Override
    public void startContactPickerForCallTransfer(int mCallId) {
        isConferenceCall = false;
        mCallActiveCallID = mCallId;
        setAddParticipant(true);
        mActiveCallRequestCode = Constants.TRANSFER_REQUEST_CODE;
        tabLayoutReset();
    }

    /**
     * Starting contact picker for call merge process
     *
     * @param mCallId of active call which we want to merge to conference with picked contact
     */
    @Override
    public void startContactPickerForConference(int mCallId) {
        isConferenceCall = true;
        mCallActiveCallID = mCallId;
        setAddParticipant(true);
        mActiveCallRequestCode = Constants.CONFERENCE_REQUEST_CODE;
        tabLayoutReset();
        mSelectAudio.setEnabled(false);
    }

    /**
     * Canceling contact picker we opened for {@link #startContactPickerForCallTransfer(int)}
     * or {@link #startContactPickerForConference(int)}
     */
    @Override
    public void cancelContactPicker() {
        if (mSectionsPagerAdapter.isCallAddParticipant()) {
            isConferenceCall = false;
            mCallActiveCallID = 0;
            setAddParticipant(false);
            mActiveCallRequestCode = 0;
            tabLayoutReset();
            mSelectAudio.setEnabled(true);
        }
    }


    /**
     * Preparing views when call is started. Based on parameter isVideo it will prepare audio only
     * or video call
     *
     * @param isVideo is call audio only or video call
     */
    @Override
    public void onCallStarted(boolean isVideo) {
        Utils.hideKeyboard(this);

        collapseSlideSelectPhoneNumber();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        boolean isMuteEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().isMuteEnabled();
        boolean isVideoEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled();

        mAudioMute.setEnabled(isMuteEnabled);

        mVideoMute.setEnabled(isMuteEnabled & isVideoEnabled);

        changeAudioVideoMuteButtonsVisibility(isMuteEnabled,isVideoEnabled);

        mVideoMute.setClickable(isMuteEnabled & isVideoEnabled);

        boolean isVideoMuted = SDKManager.getInstance().getCallAdaptor().ismVideoMuted();
        mVideoMute.setChecked(isVideoMuted);

        mSelectAudio.setChecked(true);

    }

    void changeAudioVideoMuteButtonsVisibility(boolean isMuteEnabled, boolean isVideoEnabled){}

    /**
     * Proces of preparation of  {@link MainActivity} where we are hiding unnecessary views, enabling
     * {@link ToggleButton} for video and audio mute. {@link UICallViewAdaptor} audio and video mute
     * state is set to false
     */
    @Override
    public void onCallEnded() {

        ActiveCallFragment activeCallFragment = (ActiveCallFragment) (getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG));
        if(activeCallFragment!=null && activeCallFragment.alertDialog!=null )
            activeCallFragment.alertDialog.dismiss();

        Intent intentKillCallDialerActivity = new Intent("com.avaya.endpoint.FINISH_CALL_ACTIVITY");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentKillCallDialerActivity);


        if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getFragmentContacts() != null){
            mSectionsPagerAdapter.getFragmentContacts().unblockListClick();
        }
        if (mEmergencyWithoutLogin) {
            // back to the login screen
            Log.d(TAG, "End emergency call during first time activation.");
            mEmergencyWithoutLogin = false;
            final Intent intent = new Intent();
            intent.setAction(Constants.SERVICE_STATE_CHANGE);
            intent.putExtra(Constants.KEY_SERVICE_TYPE_EXTRA, Constants.SIP_SERVICE_TYPE);
            intent.putExtra(Constants.KEY_SERVICE_STATUS_EXTRA, Constants.FAIL_STATUS);
            intent.putExtra(Constants.KEY_SERVICE_RETRY_EXTRA, 0);
            sendBroadcast(intent);
            finish();
            return;
        }

        if (SDKManager.getInstance().getCallAdaptor().getActiveCallId() == 0) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            if (isLogoutState()){
                onBackPressed();
            }

            if (mAudioMute != null) {
                mAudioMute.setEnabled(false);
                mAudioMute.setChecked(false);
            }
            mCallViewAdaptor.changeAudioMuteState(false);

            if (mVideoMute != null) {
                mVideoMute.setEnabled(false);
                mVideoMute.setChecked(false);
            }
            mCallViewAdaptor.changeVideoMuteState(false);

            mSelectAudio.setChecked(false);

            OnCallEndedChangeUIForDevice();

            // by some reason configuration that effects DialerFragment UI (like ENABLE_REDIAL) is not
            // always properly restored after ActiveCallFragment destroy
            if (mSectionsPagerAdapter.getDialerFragment() != null) {
                mSectionsPagerAdapter.getDialerFragment().applyConfigChange();
                mSectionsPagerAdapter.getDialerFragment().onMessageWaitingStatusChanged(SDKManager.getInstance().getVoiceMessageAdaptor().getVoiceState());
            }
            setFeatureMenuOpen(false);

            OnCallEndedChangesForDevice();
        }else{
            //Only for updating deskphoneservices (not changing the mute state)
            mCallViewAdaptor.changeAudioMuteState(mAudioMute.isChecked());
        }

        if(mActiveCall!=null)
            mActiveCall.setClickable(false);
    }

    void OnCallEndedChangeUIForDevice(){}

    void OnCallEndedChangesForDevice(){}


    private boolean isLogoutState(){
        String user = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getRegisteredUser();
        if (user == null)
            return true;

        return user.equals(ANONYMOUS_USER);

    }

    /**
     * In case that feature menu is opened in {@link ActiveCallFragment} we have to blur {@link MainActivity}
     * frame as it is not possible to do that directly from {@link ActiveCallFragment}
     *
     * @param isOpen boolean
     */
    @Override
    public void setFeatureMenuOpen(boolean isOpen) {
        if (mBlureFrame != null) {
            if (isOpen) {
                mBlureFrame.setVisibility(View.VISIBLE);
            } else {
                mBlureFrame.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Setting {@link ToggleButton} for audio selection on or off based on parameters provided
     *
     * @param isOn boolean based on which {@link ToggleButton} is set
     */
    @Override
    public void setOffhookButtosChecked(boolean isOn) {
        mSelectAudio.setChecked(isOn);
    }

    public boolean isOffhookChecked(){
        return mSelectAudio.isChecked();
    }

    public void setOffHookButtonsBasedCallState(int callId, UICallState state){
    }

    /**
     * handling result of transfer/conference action
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case TRANSFER_REQUEST_CODE:
                Log.d(TAG, "transfer request arrived");
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        int callId = data.getIntExtra(CALL_ID, 0);
                        String target = data.getStringExtra(TARGET);
                        Log.d(TAG, "Call transfered: " + callId + " Target: " + target);
                        int isContact = data.getIntExtra(IS_CONTACT, 0);
                        mCallViewAdaptor.transferCall(callId, target, isContact == 1);

                        GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.FEATURE_TRANSFER_EVENT);

                    } else {
                        Log.e(TAG, "not enough data to perform transfer");
                    }
                    ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
                    if (activeCallFragment != null) {
                        activeCallFragment.dismissMenu();
                    }
                }
                cancelAddSomeOneScreen();
                break;
            case CONFERENCE_REQUEST_CODE:
                Log.d(TAG, "conference request arrived");
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        int callId = data.getIntExtra(CALL_ID, 0);
                        String target = data.getStringExtra(TARGET);
                        mCallViewAdaptor.conferenceCall(callId, target);

                        GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.FEATURE_CONFERENCE_EVENT);

                    } else {
                        Log.e(TAG, "not enough data to perform conference");
                    }
                }
                ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
                if (activeCallFragment != null) {
                    activeCallFragment.dismissMenu();
                }
                if (mSectionsPagerAdapter.isCallAddParticipant()) {
                    if (activeCallFragment != null) {
                        activeCallFragment.setVisible();
                    }
                    cancelAddSomeOneScreen();
                    CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                    if (callStatusFragment != null) {
                        callStatusFragment.hideCallStatus();
                        Utils.hideKeyboard(this);
                    }
                }

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }


    /**
     *
     * Returns the Tab name (Favorites, Contacts, History) used for the access to the specific Contact details
     */
    @Override
    public String getContactCapableTab() {
        switch (mContactCapableTab) {
            case Favorites:
                return "Favorites";
            case Contacts:
                return "Contacts";
            case History:
                return "History";
            default:
                return null;

        }
    }

    @Override
    public void saveSelectedCallCategory(CallData.CallCategory callCategory) {
        mSelectedCallCategory = callCategory;
    }

    /**
     * Store this value in main activity because of the recent
     * fragment recreation.
     *
     * @param position last position of the visible items
     */
    @Override
    public void onPositionToBeSaved(Parcelable position) {
    }

    /**
     * Prepare configuration changes and inform fragments about it
     */
    private void applyConfigChange() {
        // check which tabs shall be present in the section selection bar
        if (mSectionsPagerAdapter.configureTabLayout()) {
            tabLayoutReset();
        }

        // inform all fragment that some change in UI representation might happen
        List<Fragment> allFragments = getSupportFragmentManager().getFragments();
        for (Fragment f : allFragments) {
            if (f instanceof ConfigChangeApplier) {
                ((ConfigChangeApplier) f).applyConfigChange();
                break;
            }
        }

        if (mSectionsPagerAdapter.getDialerFragment() != null) {
            mSectionsPagerAdapter.getDialerFragment().applyConfigChange();
        }

        loadBrand(getBrand(), true);

        configureUserPreferenceAccess();

        // working on refreshing fragments with new information regarding name display and name sort
        mAdminNameSortOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);
        mAdminNameDisplayOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);
        mAdminChoiceRingtone = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ADMIN_CHOICE_RINGTONE);

        // managing name sort preference
        if (mAdminNameSortOrder != null && !mAdminNameSortOrder.equals(mPreviousAdminNameSortOrder)) {
            SharedPreferences.Editor editor = mSharedPrefs.edit();
            editor.putBoolean(Constants.REFRESH_FAVORITES, true);
            editor.putBoolean(Constants.REFRESH_CONTACTS, true);
            editor.putBoolean(Constants.REFRESH_RECENTS, true);
            editor.apply();
            mPreviousAdminNameSortOrder = mAdminNameSortOrder;
        }

        // managing admin choice ringtone
        SharedPreferences adminRingPreference = getSharedPreferences(Constants.ADMIN_RINGTONE_PREFERENCES, MODE_PRIVATE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        //checking if user has already selected some ringtone
        boolean ringtoneSettingsExist = prefs.contains(Constants.CUSTOM_RINGTONE_PREFERENCES);
        // if there is a change to admin settings, but user has set his ringtone preference, we just
        // make sure admin ringtone is default ringtone
        if (mAdminChoiceRingtone != null && !mAdminChoiceRingtone.equals(mPreviousAadminChoiceRingtone) && ringtoneSettingsExist) {
            // settings exist, going back to default
            SharedPreferences.Editor editor = adminRingPreference.edit();
            editor.putString(Constants.ADMIN_RINGTONE_PREFERENCES, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
            editor.apply();
        }
        //if admin choice is empty and the user did not set his ringtone
        //make sure admin ringtone is default ringtone
        if (mAdminChoiceRingtone == null && !ringtoneSettingsExist){
            SharedPreferences.Editor editor = adminRingPreference.edit();
            editor.putString(Constants.ADMIN_RINGTONE_PREFERENCES, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
            editor.apply();
        }

        Log.d(TAG, "applyConfigChange() - mAdminChoiceRingtone = " + mAdminChoiceRingtone);
        Log.d(TAG, "applyConfigChange() - mPreviousAadminChoiceRingtone = " + mPreviousAadminChoiceRingtone);

        // if there is a change to admin settings and use did not change his ringtone settings,
        // we will use ringtone set by admin
        if (mAdminChoiceRingtone != null && !mAdminChoiceRingtone.equals(mPreviousAadminChoiceRingtone) && !ringtoneSettingsExist) {
            String ringtoneFound = "";
            RingtoneManager ringtoneMgr = new RingtoneManager(this);
            ringtoneMgr.setType(RingtoneManager.TYPE_RINGTONE);
            Cursor ringToneCursor = ringtoneMgr.getCursor();
            if (ringToneCursor != null){
                while (ringToneCursor.moveToNext()) {
                    int currentPosition = ringToneCursor.getPosition();
                    Uri ringtoneUri = ringtoneMgr.getRingtoneUri(currentPosition);
                    if (ringtoneUri != null) {
                        String ringtoneTitle = ringToneCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                        if (!TextUtils.isEmpty(ringtoneTitle) && mAdminChoiceRingtone.toLowerCase().equals(ringtoneTitle.toLowerCase())) {
                            SharedPreferences.Editor editor = adminRingPreference.edit();
                            editor.putString(Constants.ADMIN_RINGTONE_PREFERENCES, ringtoneUri.toString());
                            ringtoneFound = ringtoneUri.toString();
                            editor.apply();
                        }
                    }
                }
            }
            // if no matching found, we use default system ringtone
            if (ringtoneFound.equals("")) {
                SharedPreferences.Editor editor = adminRingPreference.edit();
                editor.putString(Constants.ADMIN_RINGTONE_PREFERENCES, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
                editor.apply();
            }
            mPreviousAadminChoiceRingtone = mAdminChoiceRingtone;
        }

        applyLockSetting();

    }

    /**
     * Starts or stops the LockTask depending on the current settings
     */
    private void applyLockSetting() {

        DevicePolicyManager devicePolicyManager = getSystemService(DevicePolicyManager.class);
        ActivityManager activityManager = getSystemService(ActivityManager.class);
        if (TextUtils.equals(getApplicationInfo().packageName, SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue("PIN_APP")) &&
                devicePolicyManager.isLockTaskPermitted(getApplicationInfo().packageName)) {
            SharedPreferences p = getSharedPreferences(getApplicationInfo().packageName + "_preferences", MODE_PRIVATE);
            String pinApp = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.PIN_APP);
            if (Arrays.asList(pinApp.split(",")).contains(getPackageName())) {
                boolean isPinned = p.getBoolean(Constants.EXIT_PIN, true);
                Log.d(TAG, String.format("applyLockSetting : isPinned=%b isRegistered=%b isAnonymous=%b",
                        isPinned, (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getRegisteredUser() != null), SDKManager.getInstance().getDeskPhoneServiceAdaptor().isAnonymous()));
                if (isPinned && (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getRegisteredUser() != null) && !SDKManager.getInstance().getDeskPhoneServiceAdaptor().isAnonymous()) {
                    p.edit().putBoolean(Constants.EXIT_PIN, true).apply();
                    if (activityManager.getLockTaskModeState() != LOCK_TASK_MODE_LOCKED) {
                        if (!isForeground()) {
                            Intent intentHome = new Intent(getApplicationContext(), ElanApplication.getDeviceFactory().getMainActivityClass());
                            intentHome.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentHome);
                        }
                        else {
                            startLockTask();
                            ElanApplication.isPinAppLock = true;
                        }
                    }
                } else if (activityManager.getLockTaskModeState() == LOCK_TASK_MODE_LOCKED) {
                    stopLockTask();
                    ElanApplication.isPinAppLock = false;
                }
            }
        }

    }

    /**
     * @return true if the Application is in the foreground
     */
    private boolean isForeground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
    }


    /**
     * Load logo of company for which application have to be branded
     *
     * @param brand
     * @param overrideCache should we override cache
     */
    private void loadBrand(String brand, boolean overrideCache) {
        if (overrideCache) {
            mBrandPref.edit().putString(ConfigParametersNames.BRAND_URL.getName(), brand).apply();
        }
        loadBrand(getBrand());
    }

    /**
     * Configure the accessibility of "User Preferences" menu
     */
    private void configureUserPreferenceAccess() {
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor()
                .getConfigBooleanParam(ConfigParametersNames.PROVIDE_OPTIONS_SCREEN)) {
            mOptionUserSettings.setVisibility(View.VISIBLE);
        } else {
            mOptionUserSettings.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * @param addParticipant Display or remove header for Picking contact depending on this param
     */
    private void setAddParticipant(boolean addParticipant) {
        if (mSectionsPagerAdapter == null) {
            Log.e(TAG, "error: setAddParticipant was called before mSectionsPagerAdapter was created");
            return;

        }

        mSectionsPagerAdapter.setCallAddParticipant(addParticipant);

        if (addParticipant) {
            mPickContacts.setVisibility(View.VISIBLE);

        } else {
            mPickContacts.setVisibility(View.GONE);
            // hiding contact picker in case phone call is ended during the action of picking a number to transfer to.
            collapseSlideSelectPhoneNumber();

            mFrameAll.setVisibility(View.GONE);
        }
        if (mSectionsPagerAdapter.getFragmentContacts() != null && mSectionsPagerAdapter.isContactsTabPresent()) {
            mSectionsPagerAdapter.getFragmentContacts().setAddParticipantData(addParticipant);
        }
        if (mSectionsPagerAdapter.getFragmentFavorites() != null && mSectionsPagerAdapter.isFavoriteTabPresent()) {
            mSectionsPagerAdapter.getFragmentFavorites().setAddParticipantData(addParticipant);
        }
        if (mSectionsPagerAdapter.getFragmentCallHistory() != null && mSectionsPagerAdapter.isRecentTabPresent()) {
            mSectionsPagerAdapter.getFragmentCallHistory().setAddParticipantData(addParticipant);
        }

        hideJoinMeetingFragment();
    }

    /**
     * Local broadcast receiver from local broadcast manager such as
     * SDK connectivity changes, snackbar showing, bluetooth adapter state change and etc.
     */
    private final BroadcastReceiver mLocalBroadCastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mConfigChangeAndSnackbarReceiver:onReceive -> " + intent.getAction());
            if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase(Constants.LOCAL_CONFIG_CHANGE)) {
                boolean loginSuccessful = intent.getBooleanExtra(Constants.CONFIG_CHANGE_STATUS, false);
                if (mErrorStatus == null
                        || loginSuccessful ^ mErrorStatus.getVisibility() == View.VISIBLE) {
                    return;
                }

                checkForErrors();
            } else if (intent.getAction().equalsIgnoreCase(Constants.SNACKBAR_SHOW)) {
                String message = intent.getStringExtra(Constants.SNACKBAR_MESSAGE);
                boolean msgLength = intent.getBooleanExtra(Constants.SNACKBAR_LENGTH, false);
                if (message != null && !message.isEmpty()) {
                    Snackbar.make(mActivityLayout, message, msgLength ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG).show();
                }
            } else if (intent.getAction().equalsIgnoreCase(Constants.BLUETOOTH_STATE_CHANGE)) {
                ContactsFragment contactsFragment = mSectionsPagerAdapter.getFragmentContacts();
                CallHistoryFragment callHistoryFragment = mSectionsPagerAdapter.getFragmentCallHistory();
                if (contactsFragment != null){
                    contactsFragment.PBAPRefreshState();
                }
                if (callHistoryFragment != null){
                    callHistoryFragment.PBAPRefreshState();
                }
            }
        }
    };

    /**
     * Local broadcast receiver for phone's unlock event.
     */
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase(Intent.ACTION_USER_PRESENT)) {
                Log.d(TAG, "Phone was unlocked");
                ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
                if (activeCallFragment != null) {
                    activeCallFragment.setEmergencyAndLockFeature(false);
                }
            }
        }
    };

    void setVideoControlsVisibility(int visible) {
        if (mSectionsPagerAdapter.getDialerFragment() != null)
            mSectionsPagerAdapter.getDialerFragment().setVideoButtonVisibility(visible);
        mVideoMute.setVisibility(visible);
    }


    /**
     * Processing device chnage logic based of {@link UIAudioDevice} provided in parameters and
     * boolean value which represent is device active
     *
     * @param device {@link UIAudioDevice}
     * @param active boolean representing is device active
     */
    @Override
    public void onDeviceChanged(UIAudioDevice device, boolean active) {
        Log.d(TAG, "onDeviceChanged. device=" + device + " active=" + active);
        saveAudioSelection(Constants.AUDIO_PREF_KEY, device.toString());


        int resId = getResourceIdForDevice(device);
        mSelectAudio.setBackgroundResource(resId);

        mSelectAudio.setChecked(active);

        onDeviceChangedDeviceLogic(resId, active);
        if (mToggleAudioMenu.getVisibility() == View.VISIBLE) {
            collapseSlideSelecAudioDevice();
            mToggleAudioButton.setChecked(false);
            setTransducerButtonCheckedFor155();
        }
    }

    void onDeviceChangedDeviceLogic(int resId, boolean active){}


    /**
     * Processing onHook event for provided handset type
     *
     * @param handsetType type of handset which changed state to on hook
     */
    @Override
    public void onOnHook(HandSetType handsetType) {
        mIsOffHook = false;
        resetDialer();
    }

    /**
     * Resets the DialerFragment
     */
    void resetDialer() {
        DialerFragment dialerFragment = mSectionsPagerAdapter.getDialerFragment();
        if (dialerFragment != null&&  SDKManager.getInstance().getCallAdaptor().getOffhookCallId() ==0) {
            dialerFragment.setMode(DialerFragment.DialMode.EDIT);
        }
        if (mScreenLock != null && mScreenLock.isHeld()) {
            mScreenLock.release();
        }
    }

    /**
     * Processing off hook event of provided handset type
     *
     * @param handsetType type of handset which changed state to off hook
     */
    @Override
    public void onOffHook(HandSetType handsetType) {
        if (!mCallStateEventHandler.hasIncomingCall()) {
            mIsOffHook = true;
            prepareOffHook();
        }
    }

    @Override
    public void onRejectEvent() {
        mCallStateEventHandler.rejectIncomingCall();
    }

    /**
     * Prepares app state for the off hook
     */
    void prepareOffHook() {
        if (mCallStateEventHandler.hasIncomingCall()) {
            Log.d(TAG, "prepareOffHook() is called for incomig call -> retrun");
            return;
        }

        if (isFragmentVisible(JOIN_MEETING_FRAGMENT)){
            ((JoinMeetingFragment)getVisibleFragment(JOIN_MEETING_FRAGMENT)).onBackPressed();
        }
        if ((SDKManager.getInstance().getCallAdaptor().getCall(SDKManager.getInstance().getCallAdaptor().getActiveCallIdWithoutOffhook()) == null) && !isLockState(this)) {
            if (mViewPager != null) {
                if (isForeground() && ElanApplication.isMainActivityVisible()) {
                    cancelEditFragment();
                } else {
                    needCancelEdit = true;
                }
                mViewPager.setCurrentItem(0, false);

            }
            try {
                if (mSectionsPagerAdapter.getDialerFragment() != null) {
                    mSectionsPagerAdapter.getDialerFragment().setMode(DialerFragment.DialMode.OFF_HOOK);
                }
            } catch (NullPointerException e) {
                Log.d(TAG, "prepareOffHook", e);
            }
            ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
            if (activeCallFragment != null && activeCallFragment.getView() != null && activeCallFragment.isVisible()) {
                ImageView back = activeCallFragment.getView().findViewById(R.id.back);
                back.performClick();
            }

            CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);

            final int heldCallId = SDKManager.getInstance().getCallAdaptor().getHeldCallId();
            if(callStatusFragment != null && heldCallId > 0 && callStatusFragment.getCallId() == heldCallId && !callStatusFragment.isVisible())
                callStatusFragment.showCallStatus();

        }

        PowerManager pm = ((PowerManager) getApplicationContext().getSystemService(POWER_SERVICE));
        if (!pm.isInteractive()) {
            mScreenLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, BuildConfig.APPLICATION_ID + ":" + TAG);
            mScreenLock.acquire(120*60*1000L /*120 minutes*/);
        }

        prepareOffhookChangeUIforDevice();
    }

    void prepareOffhookChangeUIforDevice(){}

    /**
     * @param context Activity context
     * @return true if the device is locked
     */
    boolean isLockState(Context context) {
        KeyguardManager kgMgr = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return (kgMgr != null) && kgMgr.isDeviceLocked() && !ElanApplication.isPinAppLock;
    }

    /**
     * Processing configuration change
     *
     * @param newConfig {@link Configuration} to be applied
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if ((mLocale != null && !(mLocale.equals(newConfig.locale))) || mFontScale != newConfig.fontScale) {
            isConfigChanged = true;
            handleConfigChanges();
        }
    }

    /**
     * Processing incoming call end notification
     */
    @Override
    public void onIncomingCallEnded() {

        if (mSectionsPagerAdapter != null) {
            if (mViewPager != null && mTabIndexMap.get(Tabs.History) != null
                    && mSectionsPagerAdapter.getFragmentCallHistory() == null) {
                mViewPager.setCurrentItem(mTabIndexMap.get(Tabs.History), false);
            }
            try {
                if (isFragmentVisible(DIALER_FRAGMENT)) {
                    ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).setMode(DialerFragment.DialMode.EDIT);
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }
        if (mTabIndexMap.get(Tabs.History) != null) {
            setHistoryIcon(mTabIndexMap.get(Tabs.History));
        }

        if (mCallViewAdaptor != null && mCallViewAdaptor.getNumOfCalls() == 0) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        onIncomingCallEndedCahngeUIForDevice();

        if(mActiveCall!=null)
            mActiveCall.setClickable(false);
    }

    void onIncomingCallEndedCahngeUIForDevice(){}

    /**
     * Controls visibility of the search, addContact and filter buttons.
     0     * based on the state of the viewPager.
     */
    void searchAddFilterIconViewController(){
        changeButtonsVisibility(getSelectedTab());
    }

    /**
     * Processing incoming call start notification
     */
    @Override
    public void onIncomingCallStarted() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (isLockState(this) && mViewPager != null) {
            mViewPager.setCurrentItem(mTabIndexMap.get(Tabs.Dialer), false);
        }

        if(isFragmentVisible(CONTACTS_EDIT_FRAGMENT) &&  ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).alertDialog!=null)
            ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).alertDialog.dismiss();

        onIncomingCallStartedDeviceLogic();
    }

    void onIncomingCallStartedDeviceLogic(){}

    public void checkFilterButtonState() {
        CallHistoryFragment fragment = (CallHistoryFragment) getVisibleFragment(HISTORY_FRAGMENT);
        if (fragment != null && fragment.isAdded() && filterButton != null) {
            if (fragment.isFilterMenuExpanded()) {
                filterButton.setImageResource(R.drawable.ic_expand_less);
            } else {
                filterButton.setImageResource(R.drawable.ic_expand_more);
            }
        }
    }

    /**
     * Processing on video muted event and setting adequate value for video mute {@link ToggleButton}
     *
     * @param uiCall {@link UICall}
     * @param muting boolean based on which {@link ToggleButton} is set
     */
    @Override
    public void onVideoMuted(UICall uiCall, boolean muting) {
        Log.d(TAG, "onVideoMuted: muting=" + muting);
        mVideoMute.setEnabled(true);
        mVideoMute.setChecked(muting);
    }

    /**
     * Process missed call and refresh history icon after onCallEnded in {@link CallAdaptor}
     * is finished executing
     */
    @Override
    public void onCallMissed() {
        onIncomingCallEnded();
    }

    /**
     * Runnable responsible for obtaining parameters from parameter store
     */
    private static class ParameterUpdateRunnable implements Runnable {
        private final TextView mBoundView;
        private final String mBoundParameter;

        ParameterUpdateRunnable(TextView username, String boundParameter) {
            mBoundView = username;
            mBoundParameter = boundParameter;
        }

        @Override
        public void run() {
            if (mBoundView != null) {
                String name = VantageDBHelper.getParameter(mBoundView.getContext().getContentResolver(), mBoundParameter);
                if (name != null) {
                    mBoundView.setText(name);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (!onBackPressedDeviceLogic())
            return;

        Log.d(TAG, "onBackPressed Called");
        ActivityManager am = getSystemService(ActivityManager.class);
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (am.getLockTaskModeState() != LOCK_TASK_MODE_LOCKED) {
            if (!isLockState(this) && (isFragmentVisible(ACTIVE_CALL_FRAGMENT) || isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT))) {
                ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mBackArrowOnClickListener();
                return;
            } else if (isFragmentVisible(JOIN_MEETING_FRAGMENT)){
                ((JoinMeetingFragment) getVisibleFragment(JOIN_MEETING_FRAGMENT)).onBackPressed();
            }
            else{
                startActivity(setIntent);
            }
        }
        else {
            String pinApp = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.PIN_APP);
            if(!TextUtils.isEmpty(pinApp)) {
                String KIOSK = "com.avaya.endpoint.avayakiosk";
                Intent launchIntentForPackage;
                if (Arrays.asList(pinApp.split(",")).contains(KIOSK)) {
                    launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(KIOSK);
                    if(launchIntentForPackage !=null) {
                        startActivity(launchIntentForPackage);
                    }
                    else {
                        startActivity(setIntent);
                    }
                }
                else {
                    String firstApp = pinApp.split(",")[0];
                    launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(firstApp);
                    if (launchIntentForPackage != null) {
                        startActivity(launchIntentForPackage);
                    } else {
                        startActivity(setIntent);
                    }
                }
            }

        }
    }

    boolean onBackPressedDeviceLogic(){
        return true;
    }

    @Override
    public void onSaveSelectedCategoryRecentFragment(CallData.CallCategory callCategory) {
        mSelectedCallCategory = callCategory;
    }

    /**
     * We should refresh history icon in case recent calls tab is shown
     * and missed call is logged. When user clears all calls from recent call tab,
     * we reset value and update icon.
     */
    @Override
    public void refreshHistoryIcon() {
        resetMissedCalls();
    }

    /**
     * Starts {@link NotificationService} and creates mNotifFactory.
     */
    private void initNotifications() {

        // fist check whether notifcation mechanism was started already
        if (mNotifFactory != null) {
            return;
        }
        // start the notification mechanism
        startService(new Intent(this, NotificationService.class));
        mNotifFactory = CallNotificationFactory.getInstance(getApplicationContext());
    }

    /**
     * Starts {@link BluetoothStateService}
     */
    private void initBluetoothChangeListener() {
        startService(new Intent(this, BluetoothStateService.class));
    }

    /**
     * Initialize the app after crash or manual launch.
     */
    private void handleSpecialInitCases() {

        // special handle for manual application launch after crash
        if ((getIntent() != null) && (getIntent().getAction() == null) && !getIntent().getBooleanExtra(Constants.CONFIG_RECEIVER, false)) {
            Log.d(TAG, "Start application from Launcher.");
            SDKManager.getInstance().getDeskPhoneServiceAdaptor().initAfterCrash();
        }

        // special handle for emergency during first activation
        if (mEmergencyWithoutLogin) {
            Log.i(TAG, "This is emergency during first time activation");
            handleIntent(getIntent());
        }


        // restore the calls if any ongoing calls exists
        if (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0) {
            mHandler.postDelayed(() -> SDKManager.getInstance().getCallAdaptor().restoreCalls(), 1000);
        }

        //restore offhook call
        if (SDKManager.getInstance().getCallAdaptor().getOffhookCallId() > 0){
            mSelectAudio.setChecked(true);
        }

        //ELAN-1000
        if ((getIntent() != null) && (getIntent().getAction() == Intent.ACTION_VIEW) && getIntent().hasExtra(Constants.GO_TO_FRAGMENT) && Constants.GO_TO_FRAGMENT_MISSED_CALLS.equalsIgnoreCase(getIntent().getStringExtra(Constants.GO_TO_FRAGMENT))){
            handleIntent(getIntent());
        }

        if (getIntent() != null) {
            handleIntent(getIntent());
        }
    }


    /**
     * Restores incoming calls
     */
    private void restoreIncomingCalls(){
        if (SDKManager.getInstance().getCallAdaptor().isAlertingCall() != 0 || SDKManager.getInstance().getCallAdaptor().isIncomingFailedCall() !=0 ){
            SDKManager.getInstance().getCallAdaptor().restoreIncomingCalls();
        }
    }

    /**
     * Receives call backs for changes to Recent calls and contacts
     */
    class RecentCallsAndContactObserver extends ContentObserver {

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        RecentCallsAndContactObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if(mSectionsPagerAdapter != null && mSectionsPagerAdapter.getFragmentCallHistory()!=null &&
                    mSectionsPagerAdapter.getFragmentContacts() != null) {
                // We should update contacts and call logs always when content is changed
                mSectionsPagerAdapter.getFragmentContacts().contactTableUpdated();
                mSectionsPagerAdapter.getFragmentCallHistory().callTableUpdated();
            }
        }
    }


    @Override
    public void triggerOffHookButton(View v) {
        onClick(v);
    }

    @Override
    public void triggerTransducerButton(View v) {
        onClick(v);
    }

    public void hideJoinMeetingFragment(){
        if (!isFragmentVisible(ACTIVE_CALL_FRAGMENT) && isFragmentVisible(JOIN_MEETING_FRAGMENT)){
            ((JoinMeetingFragment)getVisibleFragment(JOIN_MEETING_FRAGMENT)).onBackPressed();
        }
    }


    //Custom
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    public static class NukeSSLCerts {
        protected static final String TAG = "NukeSSLCerts";

        public static void nuke() {
            try {
                Log.e(TAG, "Try to nuke");
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                X509Certificate[] myTrustedAnchors = new X509Certificate[0];
                                return myTrustedAnchors;
                            }

                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
            } catch (Exception e) {
            }
        }
    }
}
