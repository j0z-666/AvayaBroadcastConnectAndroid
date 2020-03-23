package com.avaya.android.vantage.aaadevbroadcast.contacts;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.avaya.android.vantage.aaadevbroadcast.GoogleAnalyticsUtils;
import com.avaya.android.vantage.aaadevbroadcast.PhotoLoadUtility;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

public class ViewHolder extends RecyclerView.ViewHolder {

    private final static int CALL_AUDIO = 1;
    private final static int CALL_VIDEO = 2;
    private final static int ADD_PARTICIPANT = 3;
    final TextView mName;
    final TextView mLocation;
    final ImageView mFavorite;
    final ImageView mSyncContact;
    final TextView mDirectoryInfo;
    private final View itemView;
    private final TextView mPhoto;
    private final ImageView mPhotoImage;
    private final ImageView mCallAudio;
    private final ImageView mCallVideo;
    private final ImageView mAddParticipant;
    private final boolean addParticipant;

    ViewHolder(View view, boolean addParticipant, boolean isLandscape) {
        super(view);
        this.addParticipant = addParticipant;

        itemView = view;
        mName = view.findViewById(R.id.contact_name);
        mLocation = view.findViewById(R.id.contact_location);
        mFavorite = view.findViewById(R.id.contact_is_favorite);
        mPhoto = view.findViewById(R.id.initials);
        mCallAudio = view.findViewById(R.id.call_audio);
        mCallVideo = view.findViewById(R.id.call_video);
        mAddParticipant = view.findViewById(R.id.add_participant);
        mDirectoryInfo = view.findViewById(R.id.directory_info);
        mSyncContact = view.findViewById(R.id.contact_is_sync);
        mPhotoImage = view.findViewById(R.id.photo);
        if (addParticipant) {
            android.view.ViewGroup.LayoutParams params = mLocation.getLayoutParams();
            params.width = 176;
            mLocation.setLayoutParams(params);
            mCallAudio.setVisibility(View.INVISIBLE);
            mCallVideo.setVisibility(View.INVISIBLE);
            mAddParticipant.setVisibility(View.VISIBLE);
        }

        if (isLandscape) {
            android.view.ViewGroup.LayoutParams params = mLocation.getLayoutParams();
            params.width = 332;
            mLocation.setLayoutParams(params);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " '" + mName.getText() + "'";
    }

    void bind(final ContactData contactData, boolean firstNameFirst, final ParentAdapter parentAdapter) {
        if ((contactData.mCategory == ContactData.Category.ENTERPRISE) && (contactData.mPhoto != null)) {
            PhotoLoadUtility.setPhoto(contactData, mPhotoImage);
            mPhoto.setVisibility(View.INVISIBLE);
            mPhotoImage.setVisibility(View.VISIBLE);
        } else {
            mPhoto.setVisibility(View.VISIBLE);
            mPhotoImage.setVisibility(View.INVISIBLE);
            PhotoLoadUtility.setThumbnail(contactData, mPhoto, firstNameFirst);
        }

        final OnContactInteractionListener listener = parentAdapter.getContactInteractionListener();

        itemView.setOnClickListener(view -> {
            if (listener != null && !parentAdapter.isBlockedClick()) {
                parentAdapter.getHandler().postDelayed(() -> {
                    if (contactData.mCategory == ContactData.Category.DIRECTORY) {
                        ContactData tmpData = LocalContactsRepository.getInstance().fillDirectoryPhoneNumbers(contactData);
                        listener.onContactsFragmentInteraction(tmpData);
                    } else {
                        listener.onContactsFragmentInteraction(contactData);
                    }
                }, 50);
            }
        });

        if (contactData.mHasPhone) {
            mCallAudio.setOnClickListener(view -> {
                handleButtonClick(parentAdapter, contactData, CALL_AUDIO);
                GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_CONTACTS_EVENT);
            });
        } else {
            mCallAudio.setAlpha(0.5f);
            mCallVideo.setAlpha(0.5f);
        }

        if (!addParticipant) {
            setVideoCallButton(parentAdapter, contactData);
        }

        mAddParticipant.setOnClickListener(v -> handleButtonClick(parentAdapter, contactData, ADD_PARTICIPANT));
    }

    private void setVideoCallButton(final ParentAdapter parentAdapter, final ContactData contactData) {
        if (!Utils.isCameraSupported()) {
            mCallVideo.setVisibility(View.INVISIBLE);
            return;
        }

        if (!SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
            mCallVideo.setAlpha(0.5f);
            mCallVideo.setEnabled(false);
            return;
        }

        mCallVideo.setEnabled(true);
        mCallVideo.setAlpha(1f);
        mCallVideo.setVisibility(View.VISIBLE);
        mCallVideo.setOnClickListener(v -> {
            handleButtonClick(parentAdapter, contactData, CALL_VIDEO);
            //Call from Contacts main page
            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_CONTACTS_EVENT);
        });
    }

    private void handleButtonClick(final ParentAdapter parentAdapter, final ContactData contactData, int type) {
        if (Utils.isLandScape()) {
            parentAdapter.removeSearchResults();
        }

        if (parentAdapter.isBlockedClick() || parentAdapter.getContactInteractionListener() == null) {
            return;
        }

        if (contactData.mCategory == ContactData.Category.DIRECTORY) {
            ContactData tmpData = LocalContactsRepository.getInstance().fillDirectoryPhoneNumbers(contactData);
            performListenerAction(parentAdapter.getContactInteractionListener(), tmpData, type);
        } else {
            performListenerAction(parentAdapter.getContactInteractionListener(), contactData, type);
        }
    }

    private void performListenerAction(OnContactInteractionListener listener, ContactData contactItem, int type) {
        switch (type) {
            case CALL_AUDIO:
                listener.onCallContactAudio(contactItem, null);
                break;
            case CALL_VIDEO:
                listener.onCallContactVideo(contactItem, null);
                break;
            case ADD_PARTICIPANT:
                listener.onCallAddParticipant(contactItem);
                break;
            default:
                break;
        }
    }

    interface ParentAdapter {
        OnContactInteractionListener getContactInteractionListener();

        Handler getHandler();

        boolean isBlockedClick();

        void setBlockedClick(boolean blockedClick);

        void removeSearchResults();

        Context getContext();

        void setAddParticipant(boolean addParticipant);

        void refreshData();
    }
}