package com.avaya.android.vantage.aaadevbroadcast.activities;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;

/**
 * Splash activity which we use for showing Avaya logo during initialisation process
 * while configuration process is performed
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = new Intent(this, ElanApplication.getDeviceFactory().getMainActivityClass());
            startActivity(intent);
            finish();
        }catch (Exception e){
            Log.e("SplashActivity", "could not launch MainActivity", e);
        }
    }
}
