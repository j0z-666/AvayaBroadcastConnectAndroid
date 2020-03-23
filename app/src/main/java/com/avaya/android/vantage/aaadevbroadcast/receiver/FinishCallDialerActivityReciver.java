package com.avaya.android.vantage.aaadevbroadcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.views.interfaces.FinishCallDialerActivityInterface;

import java.util.Objects;

/**
 * Created by dshar on 01/07/2018.
 */

public class FinishCallDialerActivityReciver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        if(Utils.callDialerActivityRef !=null && Utils.callDialerActivityRef.get() !=null && Objects.requireNonNull(intent.getAction()).equalsIgnoreCase("com.avaya.endpoint.FINISH_CALL_ACTIVITY")) {
            FinishCallDialerActivityInterface mOnEventListener = Utils.callDialerActivityRef.get();
            Log.e("TEST", "FinishCallDialerActivityReciver - onReceive");
            mOnEventListener.killCallDialerActivity();
            Utils.callDialerActivityRef = null;
        }
    }
}
