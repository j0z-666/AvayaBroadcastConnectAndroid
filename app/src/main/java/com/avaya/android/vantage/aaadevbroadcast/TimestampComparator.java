package com.avaya.android.vantage.aaadevbroadcast;

import com.avaya.android.vantage.aaadevbroadcast.model.CallData;

import java.util.Comparator;

/**
 * Comparator used for Call logs sorting. Sorting is done using timestamps.
 */
class TimestampComparator implements Comparator<CallData> {

    @Override
    public int compare(CallData lhs, CallData rhs) {
        return Long.compare(rhs.mCallDateTimestamp, lhs.mCallDateTimestamp);
    }
}
