package com.avaya.android.vantage.aaadevbroadcast.activities;

import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.PermissionManager;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;

public class PermissionRequesterActivity extends AppCompatActivity {

    private static final String TAG = "PermissionRequester";
    private static int sRequestCode = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            final String[] requestedPermissions = intent.getStringArrayExtra(PermissionManager.REQUESTED_PERMISSIONS);
            assert requestedPermissions != null;
            if(sRequestCode == 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AvayaSimpleAlertDialog);
                builder
                        .setTitle(R.string.request_permissions_title)
                        .setMessage(getString(R.string.request_permissions_message))
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton(R.string.ok, (dialog, which) -> requestPermissions(requestedPermissions, sRequestCode++))
                        .create().show();


            }
            else {
                finish();
            }
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        sRequestCode--;
        boolean recreateCSDK = true;
        for (int result :
                grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                recreateCSDK = false;
                break;
            }
        }
        if (recreateCSDK && permissions.length > 0) {
            Log.i(TAG, "finally all permissions granted, recreating client");
            SDKManager.getInstance().getDeskPhoneServiceAdaptor().createUser(true);
        }

        final Intent intent = getIntent();
        final Intent relaunchIntent = intent.getParcelableExtra(PermissionManager.RELAUNCH_ACTIVITY_INTENT);
        if(relaunchIntent != null) {
            startActivity(relaunchIntent);
        }
        finish();
    }
}
