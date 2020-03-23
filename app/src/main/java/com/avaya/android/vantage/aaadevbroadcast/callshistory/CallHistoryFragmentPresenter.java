package com.avaya.android.vantage.aaadevbroadcast.callshistory;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.bluetooth.PairedDeviceSyncHelper;
import com.avaya.android.vantage.aaadevbroadcast.csdk.HistoryAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.csdk.HistoryAdaptorListener;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.model.CallData;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.avaya.android.vantage.aaadevbroadcast.Constants.BRIO_CALL_LOGS_URI;

public class CallHistoryFragmentPresenter implements HistoryAdaptorListener, CallLogsContactsMatcher.Callback {

    private static final String TAG = "CallHistoryFragmentPresenter";
    private final List<CallData> mCallLogs;

    private final Context mContext;

    private final ViewCallback mViewCallback;

    private final PairedDeviceDataLoader mPairedDeviceDataLoader;

    private final HistoryAdaptor mHistoryAdaptor;

    private final PairedDeviceSyncHelper mPairedDeviceSyncHelper;

    private final CallLogsContactsMatcher mMatcher;

    private final CallLogsRepository mCallLogsRepository;

    private boolean mIncludePairedCallLogs = false;

    CallHistoryFragmentPresenter(Context context, ViewCallback viewCallback, PairedDeviceSyncHelper pairedDeviceSyncHelper) {
        this.mContext = context;

        mViewCallback = viewCallback;

        mCallLogs = Collections.synchronizedList(new ArrayList<>());
        mHistoryAdaptor = SDKManager.getInstance().getHistoryAdaptor();

        mMatcher = new CallLogsContactsMatcher(this);

        mHistoryAdaptor.registerListener(this);

        mPairedDeviceSyncHelper = pairedDeviceSyncHelper;

        mPairedDeviceDataLoader = new PairedDeviceDataLoader();
        restartPairedDeviceLoader();

        mCallLogsRepository = CallLogsRepository.getInstance();

        setIncludePairedCallLogs();

        buildCallLogsList();
        notifyCallDataChanged();
    }

    void updateLocalCallLogs() {
        //TODO We should check use case for this scenario
        setIncludePairedCallLogs();
        buildCallLogsList();

        notifyCallDataChanged();
    }

    void restartPairedDeviceLoader() {
        ((FragmentActivity) mContext).getSupportLoaderManager().restartLoader(Constants.SYNC_CALLS_LOADER, null, mPairedDeviceDataLoader);
    }

    void removePairedDeviceLogs() {
        if (!mPairedDeviceSyncHelper.isBluetoothEnabled()) return;
        if (!mCallLogsRepository.getPairedDeviceLogsCallLogsCached()
                .isEmpty()) {
            Utils.sendSnackBarData(mContext,
                    mContext.getResources().getString(R.string.removing_bt_call_logs), false);
        }
        mIncludePairedCallLogs = false;
        buildCallLogsList();

        notifyCallDataChanged();
    }

    void addPairedDeviceLogs() {
        if (!mPairedDeviceSyncHelper.isBluetoothEnabled()) return;
        if (!mCallLogsRepository.getPairedDeviceLogsCallLogsCached()
                .isEmpty()) {
            Utils.sendSnackBarData(mContext,
                    mContext.getResources().getString(R.string.adding_bt_call_logs), false);
        }
        mIncludePairedCallLogs = true;
        buildCallLogsList();

        notifyCallDataChanged();
    }

    void deleteAllCallLogs() {
        mCallLogs.removeAll(mCallLogsRepository.getServerCallLogsCached());
        SDKManager.getInstance().getHistoryAdaptor().deleteAllCallLogs();

        notifyCallDataChanged();
    }

    void deleteCallLog(final CallData callLogItem, final RecyclerView.ViewHolder viewHolder) {
        mHistoryAdaptor.deleteCallLog(callLogItem);
        mViewCallback.remoteItemAtPosition(viewHolder.getAdapterPosition());
    }

    void checkIfHistoryLoaded() {
        if (mCallLogsRepository != null && mCallLogsRepository.getServerCallLogsCached().size() == 0) {
            SDKManager.getInstance().getHistoryAdaptor().updateCallLogs();
        }
    }

    public void refreshMatcherData() {
        //mMatcher.setCallLogs(mCallLogsRepository.getServerCallLogsCached(), true);
        SDKManager.getInstance().getHistoryAdaptor().updateCallLogs();
    }

    @Override
    public void notifyServerLogsChanged(List<CallData> callDataArray) {
        mCallLogsRepository.setServerCallLogs(callDataArray);
        mMatcher.setCallLogs(callDataArray, true);
    }

    @Override
    public void onRemoveStarted(CallData callData) {
        mCallLogsRepository.removeServerCallLog(callData);
    }

    @Override
    public void onContactsMatchingChanged(List<CallData> callData, boolean isServerMatching) {
        if (isServerMatching) {
            mCallLogsRepository.setMatchedServerLogs(callData);
        } else {
            mCallLogsRepository.setMatchedLocalLogs(callData);
        }

        buildCallLogsList();
        notifyCallDataChanged();
    }

    public void destroy() {
        // NO OP
    }

    List<CallData> getServerLogs() {
        return mCallLogsRepository.getServerCallLogsCached();
    }

    private void loadBluetoothData(Cursor cursor) {
        List<CallData> newPairedDeviceLogs = addPairedDeviceCallData(cursor);
        if (newPairedDeviceLogs != null && !newPairedDeviceLogs.isEmpty()) {
            mMatcher.setCallLogs(newPairedDeviceLogs, false);
        }
    }

    private List<CallData> addPairedDeviceCallData(Cursor cursor) {
        List<CallData> logs = new ArrayList<>();
        int pairedUniqueId = 0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // We shouldn't add call logs which have empty or null date
                String callDate = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
                if (callDate == null || callDate.isEmpty()) {
                    continue;
                }

                logs.add(getPairedDeviceCallData(cursor, callDate, ++pairedUniqueId));
            } while (cursor.moveToNext());
        }
        return logs;
    }

    private CallData getPairedDeviceCallData(Cursor cursor, String callDate, int pairedUniqueId) {
        String phoneNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
        int callType = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
        Date callDayTime = new Date(Long.valueOf(callDate));
        String callDuration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));
        CallData.CallCategory callCategory;
        switch (callType) {
            case CallLog.Calls.OUTGOING_TYPE:
                callCategory = CallData.CallCategory.OUTGOING;
                break;

            case CallLog.Calls.INCOMING_TYPE:
                callCategory = CallData.CallCategory.INCOMING;
                break;

            case CallLog.Calls.MISSED_TYPE:
                callCategory = CallData.CallCategory.MISSED;
                break;
            default:
                callCategory = CallData.CallCategory.INCOMING;
                break;
        }

        String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));

        return new CallData(name != null ? name : phoneNumber,
                callCategory,
                callDayTime.toString(),
                callDayTime.getTime(),
                callDayTime.toString(),
                callDuration, phoneNumber,
                "", "", phoneNumber, true,
                false, null,
                ContactData.Category.PAIRED, String.valueOf(pairedUniqueId));
    }

    private void setIncludePairedCallLogs() {
        mIncludePairedCallLogs = mPairedDeviceSyncHelper != null
                && mPairedDeviceSyncHelper.getPairedItemsEnabledStatus();

        if (!mIncludePairedCallLogs) {
            mCallLogsRepository.removeLocalCallLogsCached();
        }
    }

    private void buildCallLogsList() {
        mCallLogs.clear();

        mCallLogs.addAll(mCallLogsRepository.getServerCallLogsCached());
        if (mIncludePairedCallLogs) {
            mCallLogs.addAll(mCallLogsRepository.getPairedDeviceLogsCallLogsCached());
        }
    }

    private void notifyCallDataChanged() {
        if (mViewCallback != null) {
            mViewCallback.onCallDataChanged(mCallLogs);
        }
    }

    interface ViewCallback {
        void onCallDataChanged(List<CallData> callDataList);

        void remoteItemAtPosition(int position);
    }

    class PairedDeviceDataLoader implements LoaderManager.LoaderCallbacks<Cursor> {

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String[] PROJECTION_BLUETOOTH_SYNCED_DATA = {
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.CACHED_PHOTO_URI
            };
            return new CursorLoader(mContext, BRIO_CALL_LOGS_URI, PROJECTION_BLUETOOTH_SYNCED_DATA, null, null, null);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
            if (loader.getId() == Constants.SYNC_CALLS_LOADER) {
                loadBluetoothData(cursor);
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {

        }
    }
}
