package com.avaya.android.vantage.aaadevbroadcast.callshistory;

import com.avaya.android.vantage.aaadevbroadcast.model.CallData;

/**
 * {@link OnFilterCallsInteractionListener} is interface responsible for communication of {@link CallHistoryFragment}
 * and {@link com.avaya.android.vantage.aaadevbroadcast.activities.MainActivity}.
 */

public interface OnFilterCallsInteractionListener {
    void onSaveSelectedCategoryRecentFragment(CallData.CallCategory callCategory);

    void refreshHistoryIcon();
}
