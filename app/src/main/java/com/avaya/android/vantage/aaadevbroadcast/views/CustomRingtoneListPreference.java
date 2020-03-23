package com.avaya.android.vantage.aaadevbroadcast.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.app.AlertDialog;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.R;
import android.util.AttributeSet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CustomRingtoneListPreference extends ListPreference implements Runnable {

    private static final String TAG = CustomRingtoneListPreference.class.getSimpleName();

    private RingtoneManager mRingtoneManager;
    private Handler mHandler;

    private static final int POS_UNKNOWN = -1;
    /* Position of Default Ringtone*/
    private static final int POS_DEFAULT = 0;
    /* Position of Silent (None) Ringtone.*/
    private static final int POS_SILENT = 1;

    /** The titles of the ringtones */
    private CharSequence[] mRingtonesTitles;
    /** The URIs of the ringtones */
    private CharSequence[] mRingtonesURIs;
    /* Index of current selected ringtone */
    private int mClickedDialogEntryIndex = POS_UNKNOWN;
    /* Uri of current selected ringtone */
    private Uri mCurrentValue;
    /* Uri of default ringtone */
    private Uri mDefaultValue;
    /**
     * A Ringtone for the default ringtone. In most cases, the RingtoneManager
     * will stop the previous ringtone. However, the RingtoneManager doesn't
     * manage the default ringtone for us, so we should stop this one manually.
     */
    private Ringtone mDefaultRingtone;
    /**
     * The ringtone that's currently playing, unless the currently playing one is the default
     * ringtone.
     */
    private Ringtone mCurrentRingtone;
    @SuppressLint("CustomRingtoneListPreference")
    public CustomRingtoneListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    @SuppressLint("CustomRingtoneListPreference")
    public CustomRingtoneListPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateDialogView() {
        mHandler = new Handler();
        mRingtoneManager = new RingtoneManager(getContext());
        mDefaultValue = getDefaultRingtoneUri();
        mCurrentValue = onRestoreRingtone();

        initializeEntriesAndEntryValues();

        return super.onCreateDialogView();
    }

    /* Returns Default ringtone URI*/
    private Uri getDefaultRingtoneUri() {
        return Settings.System.DEFAULT_RINGTONE_URI;
    }

    /* Initializing a list of Ringtones Titles and a list of Ringtones URIs.
    * Pushing Avaya Ringtones to the top of the list.
    * Adding an option for Default Ringtone.
    * Adding an potion for Silent (None) ringtone.
    */
    private void initializeEntriesAndEntryValues() {
        List<String> ringtonesTitlesList = new ArrayList<>();
        List<String> ringtonesURIsList = new ArrayList<>();
        Cursor ringtoneManagerCursor = mRingtoneManager.getCursor();
        int avayaRingtonePushIndex = 0;

        if (ringtoneManagerCursor != null){
            while (ringtoneManagerCursor.moveToNext()){
                String ringtoneTitle = ringtoneManagerCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                String ringtoneUriString = ringtoneManagerCursor.getString(RingtoneManager.URI_COLUMN_INDEX);
                String ringtoneUri = ContentUris.withAppendedId(Uri.parse(ringtoneUriString), ringtoneManagerCursor
                        .getLong(RingtoneManager.ID_COLUMN_INDEX)).toString();

                if (ringtoneTitle.toLowerCase().contains("avaya") && doesFileExist(ringtoneUri)){
                    ringtonesTitlesList.add(avayaRingtonePushIndex,ringtoneTitle);
                    ringtonesURIsList.add(avayaRingtonePushIndex, ringtoneUri);
                    avayaRingtonePushIndex++;
                } else {
                    ringtonesTitlesList.add(ringtoneTitle);
                    ringtonesURIsList.add(ringtoneUri);
                }
            }
            ringtoneManagerCursor.close();
        }

        addSilentAndDefaultItems(ringtonesTitlesList, ringtonesURIsList);

        mRingtonesTitles = ringtonesTitlesList.toArray(new CharSequence[0]);
        mRingtonesURIs = ringtonesURIsList.toArray(new CharSequence[0]);

        setEntries(mRingtonesTitles);
        setEntryValues(mRingtonesURIs);
    }

    private boolean doesFileExist(String contentUri) {
        ContentResolver cr = getContext().getContentResolver();
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cur = cr.query(Uri.parse(contentUri), projection, null, null, null);
        boolean result = false;
        if (cur != null) {
            if (cur.moveToFirst()) {
                String filePath = cur.getString(0);
                result = new File(filePath).exists();
            }
            cur.close();
        }

        return result;
    }

    /*
    * Adds "Default ringtone" and "None" (Silent) options to the list.
    */
    private void addSilentAndDefaultItems(List<String> ringtonesTitlesList, List<String> ringtonesURIsList) {
        ringtonesTitlesList.add(POS_DEFAULT, getContext().getString(R.string.ringtone_default));
        ringtonesURIsList.add(POS_DEFAULT, mDefaultValue.toString());

        ringtonesTitlesList.add(POS_SILENT, getContext().getString(R.string.ringtone_silent));
        ringtonesURIsList.add(POS_SILENT, "");


    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        if (mRingtonesTitles == null || mRingtonesURIs == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        mClickedDialogEntryIndex = getCurrentRingtoneIndex(mCurrentValue);
        builder.setSingleChoiceItems(mRingtonesTitles, mClickedDialogEntryIndex,
                (dialog, which) -> {
                    mClickedDialogEntryIndex = which;
                    playRingtone();
                });

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            Uri selectedRingtoneUri = mRingtonesURIs[mClickedDialogEntryIndex] != null ? Uri.parse(mRingtonesURIs[mClickedDialogEntryIndex].toString()) : null;
            onSaveRingtone(selectedRingtoneUri);
            callChangeListener(selectedRingtoneUri != null ? selectedRingtoneUri.toString() : "");
        });
    }

    /* Returns index of selected ringtone */
    private int getCurrentRingtoneIndex(Uri mCurrentValue) {
        int currentRingtoneIndex = POS_UNKNOWN;
        if(RingtoneManager.isDefault(mCurrentValue)){
            currentRingtoneIndex = POS_DEFAULT;
        }
        if (currentRingtoneIndex == POS_UNKNOWN && mCurrentValue == null){
            currentRingtoneIndex = POS_SILENT;
        }
        if (currentRingtoneIndex == POS_UNKNOWN){
            currentRingtoneIndex = findIndexOfValue(mCurrentValue.toString());
        }
        if (currentRingtoneIndex == POS_UNKNOWN){
            currentRingtoneIndex = POS_DEFAULT;
        }
        return currentRingtoneIndex;
    }

    /* Saving URI of selected ringtone to Shared Preferences*/
    private void onSaveRingtone(Uri selectedRingtoneUri) {
        persistString(selectedRingtoneUri != null ? selectedRingtoneUri.toString() : "");
    }

    private void playRingtone() {
        mHandler.removeCallbacks(this);
        mHandler.postDelayed(this, 0);
    }

    public void run() {
        stopAnyPlayingRingtone();
        if (mClickedDialogEntryIndex == POS_SILENT) {
            return;
        }

        Ringtone ringtone;
        if (mClickedDialogEntryIndex == POS_DEFAULT) {
            if (mDefaultRingtone == null) {
                mDefaultRingtone = RingtoneManager.getRingtone(getContext(), mDefaultValue);
            }
           /*
            * Stream type of mDefaultRingtone is not set explicitly here.
            * It should be set in accordance with mRingtoneManager of this Activity.
            */
            if (mDefaultRingtone != null) {
                mDefaultRingtone.setStreamType(mRingtoneManager.inferStreamType());
            }
            ringtone = mDefaultRingtone;
            mCurrentRingtone = null;
        } else {
            ringtone = RingtoneManager.getRingtone(getContext(), Uri.parse(mRingtonesURIs[mClickedDialogEntryIndex].toString()));
            mCurrentRingtone = ringtone;
        }
        assert ringtone != null;
        ringtone.play();
    }

    /* Stopping any playing ringtone */
    private void stopAnyPlayingRingtone() {
        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            mDefaultRingtone.stop();
        }

        if (mCurrentRingtone != null && mCurrentRingtone.isPlaying()) {
            mCurrentRingtone.stop();
        }

        if (mRingtoneManager != null) {
            mRingtoneManager.stopPreviousRingtone();
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        /* Prevent from breaking fullscreen*/
        ((Activity)getContext()).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        AlertDialog dialog = (AlertDialog)getDialog();
        ListView listView = dialog.getListView();
        /* Disabling scrollbar fading to make sure it is more clear that the list is scrollable */
        listView.setScrollbarFadingEnabled(false);
    }


    /**
     * Called when the chooser is about to be shown and the current ringtone
     * should be marked. Can return null to not mark any ringtone.
     * <p>
     * By default, this restores the previous ringtone URI from the persistent
     * storage.
     *
     * @return The ringtone to be marked as the current ringtone.
     */
    private Uri onRestoreRingtone() {
        Uri restoredRingtone;
        final String uriString = getPersistedString(null);
        Log.d(TAG, "onRestoreRingtone() uriString = " + uriString);

        if (!TextUtils.isEmpty(uriString)){
            restoredRingtone = Uri.parse(uriString);
        }
        else {
            SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.ADMIN_RINGTONE_PREFERENCES, Context.MODE_PRIVATE);
            String adminChoiceRingtone =  sharedPref.getString(Constants.ADMIN_RINGTONE_PREFERENCES, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
            Log.d(TAG, "onRestoreRingtone() adminChoiceRingtone = " + adminChoiceRingtone);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            String strRingtonePreference = prefs.getString(Constants.CUSTOM_RINGTONE_PREFERENCES, adminChoiceRingtone);
            Log.d(TAG, "onRestoreRingtone() strRingtonePreference = " + strRingtonePreference);
            restoredRingtone = !TextUtils.isEmpty(strRingtonePreference) ? Uri.parse(strRingtonePreference) : null;
        }


        return restoredRingtone;
    }

    /* Stopping any playing ringtone on dialog dismiss*/
    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        stopAnyPlayingRingtone();
    }
}

