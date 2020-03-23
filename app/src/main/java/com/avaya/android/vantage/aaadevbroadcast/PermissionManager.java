package com.avaya.android.vantage.aaadevbroadcast;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.activities.PermissionRequesterActivity;

import java.util.LinkedList;
import java.util.List;

public class PermissionManager {
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final String TAG = PermissionManager.class.getSimpleName();
    public static final String REQUESTED_PERMISSIONS = "REQUESTED_PERMISSIONS";
    public static final String RELAUNCH_ACTIVITY_INTENT = "RELAUNCH_ACTIVITY_INTENT";

    public static boolean somePermissionsDenied(Context context, Intent intent) {
        List<String> requestPermissions = new LinkedList<>();
        for (String permission :
                REQUIRED_PERMISSIONS) {
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(permission);
            }
        }
        if (!requestPermissions.isEmpty()) {
            Log.w(TAG, "requesting permissions for "+requestPermissions);
            Intent permissionRequester = new Intent(context, PermissionRequesterActivity.class);
            permissionRequester.putExtra(RELAUNCH_ACTIVITY_INTENT, intent);
            permissionRequester.putExtra(REQUESTED_PERMISSIONS, requestPermissions.toArray(new String[0]));
            context.startActivity(permissionRequester);
            return true;
        }
        else {
            return false;
        }


    }

    public static String getMissingPermissionMessage(Context context) {
        List<String> requestPermissions = new LinkedList<>();
        StringBuilder sb = new StringBuilder(context.getString(R.string.error_messge_permission));
        for (String permission :
                REQUIRED_PERMISSIONS) {
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(permission);
                try {
                    PermissionInfo info = context.getPackageManager().getPermissionInfo(permission, 0);
                    sb.append(info.name).append(", ");
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, permission + " not found",e);
                }
            }
        }

        final int end = sb.lastIndexOf(", ");
        return end==-1 ? context.getString(R.string.error_message_general): sb.substring(0, end);
    }
}
