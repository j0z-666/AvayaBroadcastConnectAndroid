package com.avaya.android.vantage.aaadevbroadcast.bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import androidx.annotation.IntDef;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.VantageDBHelper;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimation;

import java.lang.annotation.Retention;
import java.util.Objects;

import static com.avaya.android.vantage.aaadevbroadcast.Constants.USER_PREFERENCE;
import static java.lang.annotation.RetentionPolicy.SOURCE;


public class PairedDeviceSyncHelper {
    public static final int CONTACTS_TYPE = 0;
    public static final int CALL_LOG_TYPE = 1;
    private final Context mContext;
    private LinearLayout mSyncDialog;
    private TextView mSyncPopupText;
    private TextView mSyncPopupTitle;
    private TextView mOkSyncPopUp;
    private TextView mCancelSyncPopUp;
    private ImageView mSyncContacts;
    private Utils.SyncState mSyncState;
    private final SharedPreferences mUserPreference;
    private final SharedPreferences mConnectionPreference;
    private final SlideAnimation mSyncUpSlider;
    private AlertDialog alertDialog;
    @SyncType
    private final int mType;

    public PairedDeviceSyncHelper(Context context, int syncType) {
        this.mContext = context;
        mUserPreference = context.getSharedPreferences(USER_PREFERENCE, Context.MODE_PRIVATE);
        mConnectionPreference = context.getSharedPreferences(Constants.CONNECTION_PREFS, Context.MODE_PRIVATE);
        mType = syncType;

        mSyncUpSlider = ElanApplication.getDeviceFactory().getSlideAnimation();
    }

    public void bindViews(View root) {

        mSyncContacts = root.findViewById(R.id.sync_contacts);
        if (Utils.isLandScape() && mSyncContacts != null) {
            mSyncContacts.setVisibility(View.GONE);
            mSyncContacts = root.findViewById(R.id.sync_contacts2);
        } else if (mSyncContacts != null) {
            mSyncContacts.setVisibility(View.VISIBLE);
        }

        mSyncDialog = root.findViewById(R.id.sync_pop_up);
        mSyncDialog.setVisibility(View.GONE);

        mSyncPopupText = root.findViewById(R.id.sync_dialog_description);
        mSyncPopupTitle = root.findViewById(R.id.sync_dialog_title);
        mCancelSyncPopUp = root.findViewById(R.id.sync_dialog_cancel);
        mOkSyncPopUp = root.findViewById(R.id.sync_dialog_ok);

        mSyncUpSlider.reDrawListener(mSyncDialog);
    }

    public Utils.SyncState syncPairedDevice() {
        if (!isConnectedAndEnabled()) {
            if (mSyncState == Utils.SyncState.SYNC_ON) {
                setSyncOnNotConnected();
                broadcastSyncTypeStatus(false);
            } else {
                setSyncOffNotConnected();
            }
            syncPopupAlertDialog2();
        } else if ((mSyncState == Utils.SyncState.SYNC_ON)) {
            broadcastSyncTypeStatus(false);
            setSyncStateOff();
        } else {
            setSyncStateOn();
            broadcastSyncTypeStatus(true);
        }

        return mSyncState;
    }

    public void updateSyncStatus() {
        if (mSyncState == Utils.SyncState.SYNC_OFF) {
            if (isConnectedAndEnabled()) {
                setSyncStateOff();
            } else {
                setSyncOffNotConnected();
            }
        } else {
            if (isConnectedAndEnabled()) {
                setSyncStateOn();
            } else {
                setSyncOnNotConnected();
            }
        }
    }

    public void prepareSyncIcon() {
        if (mUserPreference == null || mContext == null) {
            return;
        }

        if (isConnectedAndEnabled()) {
            if ((isSyncEnabled())) {
                setSyncStateOn();
            } else {
                setSyncStateOff();
            }
        } else if (getSyncState().equals(Utils.SyncState.SYNC_ON.getStateName())) {
            setSyncOnNotConnected();
        } else {
            setSyncOffNotConnected();
        }
    }

    public void collapseSlider() {
        mSyncUpSlider.collapse(mSyncDialog);
    }

    public void dismissAlertDialog() {
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    public ImageView getSyncContactsView() {
        return mSyncContacts;
    }

    private void expandSlider() {
        mSyncUpSlider.expand(mSyncDialog);
    }

    private void setSyncStateOff() {
        mSyncState = Utils.SyncState.SYNC_OFF;
        mSyncContacts.setImageResource(R.drawable.ic_sync_paired_off_grey);
        mSyncContacts.setContentDescription(mContext.getResources().getString(R.string.mobile_sync_off));
        setSyncState(Utils.SyncState.SYNC_OFF);
    }

    private void setSyncOnNotConnected() {
        mSyncState = Utils.SyncState.SYNC_ON;
        mSyncContacts.setImageResource(R.drawable.ic_sync_unpaired_grey);
        mSyncContacts.setContentDescription(mContext.getResources().getString(R.string.bt_off));

        setSyncState(Utils.SyncState.SYNC_ON);
    }

    private void setSyncOffNotConnected() {
        mSyncState = Utils.SyncState.SYNC_OFF;
        mSyncContacts.setImageResource(R.drawable.ic_sync_unpaired_grey);
        mSyncContacts.setContentDescription(mContext.getResources().getString(R.string.bt_off));
        setSyncState(Utils.SyncState.SYNC_OFF);
    }

    private void setSyncStateOn() {
        mSyncState = Utils.SyncState.SYNC_ON;
        mSyncContacts.setImageResource(R.drawable.ic_sync_paired_on);
        mSyncContacts.setContentDescription(mContext.getResources().getString(R.string.mobile_sync_on));
        setSyncState(Utils.SyncState.SYNC_ON);
    }

    private String getSyncState() {
        return mUserPreference.getString(mType == CALL_LOG_TYPE ? Constants.SYNC_HISTORY : Constants.SYNC_CONTACTS, Utils.SyncState.SYNC_OFF.getStateName());
    }

    public void setOnClickListener(View.OnClickListener listener) {
        if (mSyncContacts != null) {
            mSyncContacts.setOnClickListener(listener);
        }
    }

    private void setSyncState(Utils.SyncState syncState) {
        SharedPreferences.Editor editor = mUserPreference.edit();
        editor.putString(mType == CALL_LOG_TYPE ? Constants.SYNC_HISTORY : Constants.SYNC_CONTACTS, syncState.getStateName());
        editor.apply();
    }

    private void broadcastSyncTypeStatus(String status) {
        Intent intent = new Intent(Utils.PBAP_URL);
        intent.putExtra(Utils.SYNC_TYPE, Utils.CALLHISTORY);
        intent.putExtra(Utils.STATUS, status);
        mContext.sendBroadcast(intent);
    }

    private boolean isSyncEnabled() {
        String syncEnabled = VantageDBHelper.getParameter(mContext.getContentResolver(),
                mType == CALL_LOG_TYPE ? VantageDBHelper.ENABLE_BT_CALLLOG_SYNC : VantageDBHelper.ENABLE_BT_CONTACTS_SYNC);
        return "1".equals(syncEnabled);
    }

    public boolean isConnectedAndEnabled() {
        return mConnectionPreference.getBoolean(Constants.BLUETOOTH_CONNECTED, false)
                && isBluetoothEnabled();
    }

    private boolean isPaired() {
        return mConnectionPreference.getBoolean(Constants.BLUETOOTH_BOUNDED, false);
    }

    private void broadcastSyncTypeStatus(boolean enable) {
        Intent intent = new Intent(Utils.PBAP_URL);
        intent.putExtra(mType == CALL_LOG_TYPE ? Utils.CALLHISTORY : Utils.CONTACT, enable ? Utils.ENABLED : Utils.DISABLED);
        mContext.sendBroadcast(intent);
    }

    public boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter.isEnabled();
    }

    public boolean getPairedItemsEnabledStatus() {
        return (!isSyncOffOrNotPaired()) && isConnectedAndEnabled();
    }

    private boolean isSyncOffOrNotPaired() {
        return getSyncState().equals(Utils.SyncState.SYNC_OFF.getStateName())
                || getSyncState().equals(Utils.SyncState.NOT_PAIRED.getStateName());
    }

    private void syncPopupAlertDialog(View dialogView) {
        if (mContext.getResources().getBoolean(R.bool.is_landscape)) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
            dialogBuilder.setView(dialogView);

            mOkSyncPopUp.setOnClickListener(v -> {
                collapseSlider();
                Intent openBluetoothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                mContext.startActivity(openBluetoothIntent);
            });

            mCancelSyncPopUp.setOnClickListener(v -> alertDialog.dismiss());

            alertDialog = dialogBuilder.create();
            Objects.requireNonNull(alertDialog.getWindow()).clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            alertDialog.show();
        } else {
            mOkSyncPopUp.setOnClickListener(v -> {
                collapseSlider();
                Intent openBluetoothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                mContext.startActivity(openBluetoothIntent);
            });

            mCancelSyncPopUp.setOnClickListener(v -> collapseSlider());
            expandSlider();
        }
        mSyncPopupText.setText(mType == CALL_LOG_TYPE ? R.string.sync_call_history_text : R.string.sync_contact_text);
        mSyncPopupTitle.setText(mType == CALL_LOG_TYPE ? R.string.sync_call_history_title : R.string.sync_contact_title);
    }

    private void syncPopupAlertDialog2() {
        mOkSyncPopUp.setOnClickListener(v -> {
            collapseSlider();
            Intent openBluetoothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            mContext.startActivity(openBluetoothIntent);
        });

        mCancelSyncPopUp.setOnClickListener(v -> collapseSlider());
        expandSlider();

        mSyncPopupText.setText(mType == CALL_LOG_TYPE ? R.string.sync_call_history_text : R.string.sync_contact_text);
        mSyncPopupTitle.setText(mType == CALL_LOG_TYPE ? R.string.sync_call_history_title : R.string.sync_contact_title);
    }

    @Retention(SOURCE)
    @IntDef({CONTACTS_TYPE, CALL_LOG_TYPE})
    private @interface SyncType {
    }
}
