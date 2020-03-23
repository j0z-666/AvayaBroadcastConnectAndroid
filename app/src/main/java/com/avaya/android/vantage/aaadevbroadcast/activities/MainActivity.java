package com.avaya.android.vantage.aaadevbroadcast.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private final static String LOGIN_STATE_CHANGED = "android.intent.action.LOGIN_STATE_CHANGED";
    public static final String SERVICE_IMPACTING_CHANGE = "com.avaya.endpoint.action.SERVICE_IMPACTING_CHANGE";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Routing");

        getApplicationContext().startActivity(getIntent().setClass(getApplicationContext(), ElanApplication.getDeviceFactory().getMainActivityClass()));
        finish();
    }

}
