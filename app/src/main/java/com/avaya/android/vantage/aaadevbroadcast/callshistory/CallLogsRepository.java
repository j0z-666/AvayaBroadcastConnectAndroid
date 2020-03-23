package com.avaya.android.vantage.aaadevbroadcast.callshistory;

import com.avaya.android.vantage.aaadevbroadcast.model.CallData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CallLogsRepository {

    private static CallLogsRepository mInstance;
    private List<CallData> mMatchedServerLogs;
    private List<CallData> mPairedDeviceLogs;
    private final HashMap<String, CallData> mServerLogs;

    private CallLogsRepository() {
        mMatchedServerLogs = new ArrayList<>();
        mPairedDeviceLogs = new ArrayList<>();
        mServerLogs = new HashMap<>();
    }

    public synchronized static CallLogsRepository getInstance() {
        if (mInstance == null) {
            mInstance = new CallLogsRepository();
        }
        return mInstance;
    }

    public void setMatchedServerLogs(List<CallData> callLogs) {
        mMatchedServerLogs = callLogs;
    }

    public void setServerCallLogs(List<CallData> callLogs) {
        mServerLogs.clear();
        for (CallData callData : callLogs) {
            mServerLogs.put(callData.getCallLogItem().getRemoteNumber(), new CallData(callData.getCallLogItem()));
        }
    }

    public CallData getCallDataByPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            if (mServerLogs.containsKey(phoneNumber)) {
                return mServerLogs.get(phoneNumber);
            }
        }

        return null;
    }

    public void setMatchedLocalLogs(List<CallData> callLogs) {
        mPairedDeviceLogs = callLogs;
    }

    public void removeServerCallLogs() {
        mMatchedServerLogs.clear();
    }

    public void removeServerCallLog(CallData removeItem) {
        mMatchedServerLogs.remove(removeItem);
    }

    public List<CallData> getServerCallLogsCached() {
        return mMatchedServerLogs == null ?
                Collections.EMPTY_LIST : mMatchedServerLogs;
    }

    public List<CallData> getPairedDeviceLogsCallLogsCached() {
        return mPairedDeviceLogs == null ?
                Collections.EMPTY_LIST : mPairedDeviceLogs;
    }

    public void removeLocalCallLogsCached() {
        mPairedDeviceLogs.clear();
    }
}
