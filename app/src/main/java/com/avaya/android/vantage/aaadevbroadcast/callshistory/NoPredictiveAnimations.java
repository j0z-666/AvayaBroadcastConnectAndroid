package com.avaya.android.vantage.aaadevbroadcast.callshistory;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;

class NoPredictiveAnimations extends LinearLayoutManager {


    NoPredictiveAnimations(Context context) {
        super(context);
    }

    /**
     * Disable predictive animations. There is a bug in RecyclerView which causes views that
     * are being reloaded to pull invalid ViewHolders from the internal recycler stack if the
     * adapter size has decreased since the ViewHolder was recycled.
     */
    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }
}
