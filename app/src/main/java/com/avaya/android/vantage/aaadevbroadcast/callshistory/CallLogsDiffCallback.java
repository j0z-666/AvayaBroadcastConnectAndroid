package com.avaya.android.vantage.aaadevbroadcast.callshistory;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.avaya.android.vantage.aaadevbroadcast.model.CallData;

class CallLogsDiffCallback extends DiffUtil.ItemCallback<CallData> {

    @Override
    public boolean areItemsTheSame(@NonNull CallData oldCallData, @NonNull CallData newCallData) {
        return oldCallData.hashCode() == newCallData.hashCode();
    }

    @Override
    public boolean areContentsTheSame(@NonNull CallData oldCallData, @NonNull CallData newCallData) {
        return oldCallData.equals(newCallData);
    }
}
