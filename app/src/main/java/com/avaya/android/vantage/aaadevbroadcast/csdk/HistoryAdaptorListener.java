package com.avaya.android.vantage.aaadevbroadcast.csdk;

import com.avaya.android.vantage.aaadevbroadcast.model.CallData;

import java.util.List;

/**
 * Interface providing communication for {@link HistoryAdaptor} and
 * {@link com.avaya.android.vantage.aaadevbroadcast.callshistory.CallHistoryFragmentPresenter}
 */

public interface HistoryAdaptorListener {

    void notifyServerLogsChanged(List<CallData> callDataArray);

    void onRemoveStarted(CallData callData);
}
