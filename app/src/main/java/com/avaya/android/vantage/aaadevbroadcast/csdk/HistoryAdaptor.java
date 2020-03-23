package com.avaya.android.vantage.aaadevbroadcast.csdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.callshistory.CallLogsRepository;
import com.avaya.android.vantage.aaadevbroadcast.model.CallData;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.clientservices.calllog.CallLogActionType;
import com.avaya.clientservices.calllog.CallLogCompletionHandler;
import com.avaya.clientservices.calllog.CallLogItem;
import com.avaya.clientservices.calllog.CallLogService;
import com.avaya.clientservices.calllog.CallLogServiceListener;
import com.avaya.clientservices.common.Capability;
import com.avaya.clientservices.common.DataCollectionChangeType;
import com.avaya.clientservices.common.DataRetrievalSearchFailException;
import com.avaya.clientservices.common.DataRetrievalWatcher;
import com.avaya.clientservices.common.DataRetrievalWatcherListener;
import com.avaya.clientservices.contact.Contact;
import com.avaya.clientservices.contact.ContactSearchLocationType;
import com.avaya.clientservices.contact.ContactService;
import com.avaya.clientservices.contact.ResolveContactsRequest;
import com.avaya.clientservices.contact.ResolveContactsScopeType;
import com.avaya.clientservices.user.User;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

/**
 * {@link HistoryAdaptor} is used for data retrieval and processing
 */

public class HistoryAdaptor extends DataRetrievalWatcher<CallData>
        implements DataRetrievalWatcherListener<CallData>, CallLogServiceListener {

    private static final String TAG = "HistoryAdaptor";
    private static final String MISSED_CALL_ACTION = "MISSED";
    private List<CallLogItem> mCallLogItems = new ArrayList<>();
    private CallLogService mCallLogService;
    private WeakReference<HistoryAdaptorListener> mUiObj;
    private final SharedPreferences mCallPreference;
    private final SharedPreferences mFirstTimeLoggedInPreference;

    private final Context mContext;

    private long mLastTimeStampMissedCall;
    private int mNumberOfflineMissedCalls;
    private final DataRetrievalWatcher<Contact> mDataRetrievalWatcher;
    private final DataRetrievalWatcherListener<Contact> mDataRetrievalWatcherListener;

    private static final String REDIAL_NUMBER = "redialNumber";

    public HistoryAdaptor(Context context) {
        mContext = context;
        mCallPreference = context.getSharedPreferences(Constants.CALL_PREFS, MODE_PRIVATE);
        mFirstTimeLoggedInPreference = context.getSharedPreferences(Constants.FIRST_TIME_LOGGIN, MODE_PRIVATE);
        mDataRetrievalWatcherListener = new ContactsDataRetrievalWatcherListener();
        mDataRetrievalWatcher = new DataRetrievalWatcher<>();

        registerWatcherListener(mDataRetrievalWatcher);
    }

    private void registerWatcherListener(DataRetrievalWatcher<Contact> mDataRetrievalWatcher) {
        if (mDataRetrievalWatcherListener != null) {
            mDataRetrievalWatcher.addListener(mDataRetrievalWatcherListener);
        }
    }

    /**
     * Registering new {@link HistoryAdaptorListener}
     *
     * @param uiObj
     */
    public void registerListener(HistoryAdaptorListener uiObj) {
        mUiObj = new WeakReference<>(uiObj);
    }

    /**
     * Set {@link CallLogService}
     *
     * @param callLogService {@link CallLogService}
     */
    public void setLogService(CallLogService callLogService) {
        Log.d(TAG, "setLogService");

        mCallLogService = callLogService;

        if (mCallLogService != null) {
            callLogService.addListener(this);
        }
        updateCallLogs();
    }

    /**
     * Get full list of call logs {@link CallData}
     *
     * @return {@link CallData}
     */
    private ArrayList<CallData> getCallLogs() {
        if (mCallLogService == null) {
            Log.d(TAG, "No log calls found.");
            mCallLogItems.clear();
        } else {
            // getting the call log from service and notify the adapter about it to update the view.
            Log.d(TAG, "Call logs found. Updating list");
            mCallLogItems = mCallLogService.getCallLogs();
        }

        ArrayList<CallData> callDataArray = new ArrayList<>();
        for (CallLogItem callLogItem : mCallLogItems) {
            callDataArray.add(new CallData(callLogItem));
        }

        matchServerContacts(mCallLogItems);

        return callDataArray;
    }

    private void matchServerContacts(List<CallLogItem> callLogItems) {
        User user = SDKManager.getInstance().getContactsAdaptor().getUser();
        if (user == null) {
            return;
        }

        ContactService contactService = user.getContactService();
        Capability capability = contactService.getResolveEnterpriseContactsCapability();
        if (!capability.isAllowed()) return;

        List<Pair<DataRetrievalWatcher<Contact>, String>> searches = new ArrayList<>();
        for (CallLogItem callLogItem : callLogItems) {
            searches.add(new Pair<>(mDataRetrievalWatcher, callLogItem.getRemoteNumber()));
        }

        ResolveContactsRequest resolveContactsRequest = new ResolveContactsRequest(searches, ResolveContactsScopeType.NUMBER, ContactSearchLocationType.NETWORK);
        contactService.resolveContacts(resolveContactsRequest);
    }

    public void deleteCallLog(final CallData callData) {
        CallLogItem logItem = callData.getCallLogItem();
        if (logItem == null) {
            return;
        }

        // TODO Consider removing unused values
        final String name = logItem.getLocalUserName();
        final CallData.CallCategory callCategory = CallData.convertCategoryFromServer(logItem.getCallLogAction());

        ArrayList<CallLogItem> logs = new ArrayList<>();
        logs.add(logItem);

        mCallLogService.removeCallLogs(logs, new CallLogCompletionHandler() {
            // local implementation of the CallLogCompletionHandler
            @Override
            public void onSuccess() {
                Log.d(TAG, "Call log item deleting started");
                if (mUiObj.get() != null) {
                    mUiObj.get().onRemoveStarted(callData);
                }
                Utils.sendSnackBarData(mContext, mContext.getString(R.string.call_log_deleted), Utils.SNACKBAR_SHORT);
            }

            @Override
            public void onError() {
                Log.e(TAG, "Call log item cannot be deleted. ");
                Utils.sendSnackBarData(mContext, mContext.getString(R.string.call_log_delete_failed), Utils.SNACKBAR_SHORT);
            }
        });
    }

    /**
     * Delete all data from call log history
     */
    public void deleteAllCallLogs() {
        if (mCallLogService == null) {
            Toast.makeText(mContext, "Call log service .", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No calls in history ");
            return;
        }
        mCallLogService.removeAllCallLogs(new CallLogCompletionHandler() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Call log empty " + getCallLogs().size());
                CallLogsRepository.getInstance().removeServerCallLogs();
                removeLastRedialNumber();
                Utils.sendSnackBarData(mContext, mContext.getString(R.string.all_call_log_deleted), Utils.SNACKBAR_SHORT);
            }

            @Override
            public void onError() {
                Log.d(TAG, "Call log error while removing " + getCallLogs().size());
                Utils.sendSnackBarData(mContext, mContext.getString(R.string.all_call_log_delete_failed), Utils.SNACKBAR_SHORT);
                updateCallLogs();
            }
        });
    }

    /**
     * After the call logs are successfully deleted on the server, we should
     * also clear last redial number from SharedPreferences.
     */
    private void removeLastRedialNumber() {
        SharedPreferences mRedialNumberPreference = mContext.getSharedPreferences(REDIAL_NUMBER, MODE_PRIVATE);
        SharedPreferences.Editor editor = mRedialNumberPreference.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceCallLogItemsAdded(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceCallLogItemsAdded");
        updateCallLogs();
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceCallLogItemsResynchronized(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceCallLogItemsResynchronized");
        updateCallLogs();
        checkForOfflineMissedCalls(callLogItems);
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceCallLogItemsRemoved(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceCallLogItemsRemoved " + mCallLogItems.size());
        updateCallLogs();
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceCallLogItemsUpdated(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceCallLogItemsUpdated");
        callLogService.resynchronizeCallLogs(new CallLogCompletionHandler() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Resynchronizing complete");
                updateCallLogs();
            }

            @Override
            public void onError() {
                Log.e(TAG, "Error while resynchronizing call logs ");
            }
        });
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceLoaded(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceLoaded");
        updateCallLogs();
    }

    @Override
    public void onCallLogServiceLoadFailed(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceLoadFailed");
    }

    @Override
    public void onCallLogServiceCapabilitiesChanged(CallLogService callLogService) {
        Log.d(TAG, "onCallLogServiceCapabilitiesChanged");
    }

    /**
     * If extension has enabled offline call log from SMGR, we should check if there are any
     * new missed calls. We should save the last timestamp for the new missed call and
     * update number of missed calls.
     */
    private void checkForOfflineMissedCalls(List<CallLogItem> callLogItems) {
        boolean mIsFirstTimeLoggedUser = mFirstTimeLoggedInPreference.getBoolean(Constants.KEY_CHECK_NEW_CALL, false);
        // We should save last missed call only once after log in
        if (mIsFirstTimeLoggedUser) {
            saveOfflineMissedCalls(callLogItems);
        }
    }

    /**
     * Update history call logs
     */
    public void updateCallLogs() {
        ArrayList<CallData> callDataArrayList = getCallLogs();

        if (mUiObj == null || mUiObj.get() == null) {
            return;
        }

        mUiObj.get().notifyServerLogsChanged(callDataArrayList);
        Log.d(TAG, "updateCallLogs");
    }

    /**
     * Convert from {@link CallLogItem} to {@link CallData}
     *
     * @param callLogItem {@link CallLogItem}
     * @return {@link CallData}
     */
    private CallData convertCallData(CallLogItem callLogItem) {
        if (callLogItem == null) {
            return null;
        }

        CallData.CallCategory callCategory = (callLogItem.getCallLogAction() == CallLogActionType.MISSED) ? CallData.CallCategory.MISSED :
                (callLogItem.getCallLogAction() == CallLogActionType.OUTGOING) ? CallData.CallCategory.OUTGOING : CallData.CallCategory.INCOMING;

        String name = callLogItem.getRemoteNumber();
        if (!callLogItem.isConference() && callLogItem.getRemoteParticipants() != null && callLogItem.getRemoteParticipants().size() > 0) {
            String displayName = callLogItem.getRemoteParticipants().get(0).getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                name = displayName;
            }
        }

        boolean nonCallableConference = false;
        // for ad-hoc IPO conference - put the hard-coded name CONFERENCE (note, that for ad-hoc conference number of the events on the call
        // will be greater than 0 since there is transfer event during ad-hoc conference creation, while for meet-me there is no events
        if (callLogItem.isConference() && (callLogItem.getCallEvents().size() > 0) && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE)) {
            name = Objects.requireNonNull(ElanApplication.getContext()).getResources().getString(R.string.conference);
            nonCallableConference = true;
        }

        if (name.equalsIgnoreCase("WITHHELD")) {
            if (callCategory == CallData.CallCategory.INCOMING) {
                name = Objects.requireNonNull(ElanApplication.getContext()).getResources().getString(R.string.private_address);
                nonCallableConference = true;
            } else if (callLogItem.getRemoteNumber() != null) {
                name = callLogItem.getRemoteNumber();
            }
        }

        return new CallData(name,
                callCategory,
                callLogItem.getStartTime().toString(),
                callLogItem.getStartTime().getTime(),
                callLogItem.getStartTime().toString(),
                Long.toString(callLogItem.getDurationInSeconds()), callLogItem.getRemoteNumber(),
                "", "", callLogItem.getRemoteNumber(), false
                , nonCallableConference, callLogItem, null, null);
    }

    /**
     * Obtaining {@link ContactData.Category} for provided {@link Contact}
     *
     * @param logItemContact {@link Contact}
     * @return {@link ContactData.Category}
     */
    private ContactData.Category getCategory(Contact logItemContact) {
        return ContactData.Category.fromContactSourceType(logItemContact.getPhoneNumbers()
                .getContactProviderSourceType());
    }

    /**
     * We should store number of new offline missed calls and time of the last missed call.
     *
     * @param callLogItems List of synced call logs
     */
    private void saveOfflineMissedCalls(List<CallLogItem> callLogItems) {
        if (callLogItems == null || callLogItems.isEmpty()) {
            return;
        }

        // Call logs time should be unique for the returned list of the call log
        HashSet<Long> callLogsItemSet = new HashSet<>();
        for (CallLogItem callLogItem : callLogItems) {
            if (callLogItem.getCallLogAction().equals(CallLogActionType.MISSED)) {
                callLogsItemSet.add(callLogItem.getStartTime().getTime());
            }
        }

        boolean mIsOfflineMissedCallsChanged = false;
        mNumberOfflineMissedCalls = mCallPreference.getInt(Constants.KEY_UNSEEN_MISSED_CALLS, 0);
        mLastTimeStampMissedCall = mCallPreference.getLong(Constants.KEY_CALL_TIMESTAMP, 0);

        for (Long timestamp : callLogsItemSet) {
            if (timestamp > mLastTimeStampMissedCall) {
                mNumberOfflineMissedCalls++;
                mIsOfflineMissedCallsChanged = true;
            }
        }

        // We should set maximum timestamp of all missed calls
        if (!callLogsItemSet.isEmpty())
            mLastTimeStampMissedCall = Collections.max(callLogsItemSet);

        saveFirstTimeLoggedInState();

        if (mIsOfflineMissedCallsChanged) {
            saveCallPreferenceState();
            // We should send broadcast only in case there are new offline missed calls
            Utils.refreshHistoryIcon(mContext, mNumberOfflineMissedCalls);
        }
    }

    /**
     * If we checked offline call logs when user is logged in
     * and call logs are resynced, no need to check again.
     * For that reason set the flag to false.
     */
    private void saveFirstTimeLoggedInState() {
        SharedPreferences.Editor editor = mFirstTimeLoggedInPreference.edit();
        editor.putBoolean(Constants.KEY_CHECK_NEW_CALL, false);
        editor.apply();
    }

    /**
     * We should save timestamp of last offline missed call and
     * also refresh numbers of offline missed calls.
     */
    private void saveCallPreferenceState() {
        SharedPreferences.Editor editor = mCallPreference.edit();
        editor.putInt(Constants.KEY_UNSEEN_MISSED_CALLS, mNumberOfflineMissedCalls);
        editor.putLong(Constants.KEY_CALL_TIMESTAMP, mLastTimeStampMissedCall);
        editor.apply();
    }

    @Override
    public void onRetrievalProgress(DataRetrievalWatcher<CallData> watcher, boolean determinate, int numRetrieved, int total) {

    }

    @Override
    public void onRetrievalCompleted(DataRetrievalWatcher<CallData> watcher) {

    }

    @Override
    public void onRetrievalFailed(DataRetrievalWatcher<CallData> watcher, Exception failure) {

    }

    @Override
    public void onCollectionChanged(DataRetrievalWatcher<CallData> watcher, DataCollectionChangeType changeType, List<CallData> changedItems) {

    }

    private class ContactsDataRetrievalWatcherListener implements DataRetrievalWatcherListener<Contact> {

        @Override
        public void onRetrievalProgress(DataRetrievalWatcher<Contact> watcher, boolean determinate, int numRetrieved, int total) {
            Log.d(TAG, "onRetrievalProgress()");
        }

        @Override
        public void onRetrievalCompleted(DataRetrievalWatcher<Contact> watcher) {
            List<Contact> contacts = watcher.getSnapshot();

            Log.d(TAG, "Contacts size: " + contacts.size());
        }

        @Override
        public void onRetrievalFailed(DataRetrievalWatcher<Contact> watcher, Exception failure) {
            DataRetrievalSearchFailException dataRetrievalSearchFailException = (DataRetrievalSearchFailException) failure;
            Log.d(TAG, "Failure reason: " + dataRetrievalSearchFailException.getFailureReason().toString());
        }

        @Override
        public void onCollectionChanged(DataRetrievalWatcher<Contact> watcher, DataCollectionChangeType changeType, List<Contact> changedItems) {
            Log.d(TAG, "onCollectionChanged()");
        }
    }
}
