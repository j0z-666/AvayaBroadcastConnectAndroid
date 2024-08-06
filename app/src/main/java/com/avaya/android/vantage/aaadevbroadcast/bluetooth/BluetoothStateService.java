package com.avaya.android.vantage.aaadevbroadcast.bluetooth;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.activities.MainActivity;
import com.avaya.android.vantage.aaadevbroadcast.fragments.settings.BlueHelper;


public class BluetoothStateService extends Service {

    /* related to {@link BlueHelper#ENABLE_BT_CONTACTS_SYNC & BlueHelper#ENABLE_BT_CALLLOG_SYNC} */
    private static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED";
    private SharedPreferences mConnectionPref;
    private final BroadcastReceiver mBluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BlueHelper.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                final int connectionState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                putConnectionStateToPreferences(connectionState);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                putBondStateToPreferences(device);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int adapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (adapterState == BluetoothAdapter.STATE_TURNING_OFF) {
                    putConnectionStateToPreferences(0);
                }

                Intent adapterStateChange = new Intent(Constants.BLUETOOTH_STATE_CHANGE);
                LocalBroadcastManager.getInstance(context).sendBroadcast(adapterStateChange);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        mConnectionPref = getApplicationContext()
                .getSharedPreferences(Constants.CONNECTION_PREFS, Context.MODE_PRIVATE);
        registerReceiver(mBluetoothBroadcastReceiver, getBluetoothIntentFilter());

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        startForeground(1521, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "VantageBroadcast Channel";
        String channelName = "Bluetooth Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBluetoothBroadcastReceiver);
    }

    /**
     * Saves connection state to SharedPreferences.<br>
     * However, state is preserved as boolean so it is true only for connected state.<br>
     * No particular intermittent state is saved.
     *
     * @param connectionState connection state such as {@link BluetoothProfile#STATE_CONNECTED}
     */
    private void putConnectionStateToPreferences(int connectionState) {
        final boolean isConnected = connectionState == BluetoothProfile.STATE_CONNECTED;
        SharedPreferences.Editor editor = getBaseContext().getSharedPreferences(Constants.CONNECTION_PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(Constants.BLUETOOTH_CONNECTED, isConnected);
        editor.apply();
    }

    /**
     * Saves Bluetooth device bond state to SharedPreferences.<br>
     * This is to be done through BroadcastReceiver listening to Bluetooth intent.<br>
     * see {@link BluetoothStateService}
     *
     * @param bondState should be one of:<br>
     *                  - {@link BluetoothDevice#BOND_BONDED}
     *                  - {@link BluetoothDevice#BOND_BONDING}
     *                  - {@link BluetoothDevice#BOND_NONE}
     */
    private void putBondStateToPreferences(int bondState) {
        if (mConnectionPref == null) return;

        final boolean isBonded = bondState == BluetoothDevice.BOND_BONDED;
        SharedPreferences.Editor editor = mConnectionPref.edit();
        editor.putBoolean(Constants.BLUETOOTH_BOUNDED, isBonded);
        editor.apply();
    }

    /**
     * Does same as {@link BlueHelper#putBondStateToPreferences(int)}, only this method<br>
     * takes bond state from (@link {@link BluetoothDevice#getBondState()})
     *
     * @param bondedDevice {@link BluetoothDevice} who's bond state is saved to preferences
     */
    private void putBondStateToPreferences(BluetoothDevice bondedDevice) {
        if (bondedDevice == null) return;

        putBondStateToPreferences(bondedDevice.getBondState());
    }

    /**
     * Creates an {@link IntentFilter} with following actions:<br>
     * - {@link BluetoothDevice#ACTION_BOND_STATE_CHANGED}<br>
     * - {@link #ACTION_CONNECTION_STATE_CHANGED}
     *
     * @return {@link IntentFilter} with the above mentioned actions
     */
    private IntentFilter getBluetoothIntentFilter() {
        final IntentFilter bluetoothIntentFilter = new IntentFilter();
        bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothIntentFilter.addAction(ACTION_CONNECTION_STATE_CHANGED);
        bluetoothIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return bluetoothIntentFilter;
    }
}
