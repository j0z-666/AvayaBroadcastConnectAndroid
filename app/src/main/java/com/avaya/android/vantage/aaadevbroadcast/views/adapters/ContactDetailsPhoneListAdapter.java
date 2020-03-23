package com.avaya.android.vantage.aaadevbroadcast.views.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.GoogleAnalyticsUtils;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.model.CallData;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter used to show contact phone numbers in contact details fragment
 */

public class ContactDetailsPhoneListAdapter extends ArrayAdapter<ContactData> {

    private static final String TAG = "ContactDetailsPhoneList";

    private final ContactData mContactData;
    private final OnContactInteractionListener mListener;
    private final boolean phonesUpdated = false;
    private final List<String> newPhoneNumbers = new ArrayList<>();

    /**
     * Constructor
     *
     * @param context     fragment context
     * @param contactData data file containing contact information
     * @param listener    listener we use to track if user clicked on item
     */
    public ContactDetailsPhoneListAdapter(Context context, ContactData contactData, OnContactInteractionListener listener) {
        super(context, R.layout.contact_details_phone_layout);
        mContactData = contactData;
        mListener = listener;
    }

    /**
     * Obtaining total number of phone numbers for contact
     *
     * @return int representing total number of phone numbers in list
     */
    @Override
    public int getCount() {
        if (mContactData != null && !phonesUpdated) {
            return mContactData.mPhones.size();
        } else if (phonesUpdated && mContactData != null) {
            return newPhoneNumbers.size();
        } else {
            Log.d(TAG, "no phone numbers connected to account");
            return 0;
        }
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @SuppressWarnings("NullableProblems") ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.contact_details_phone_layout, parent, false);
            // initialize the view holder
            viewHolder = new ViewHolder();
            viewHolder.defaultContact = convertView.findViewById(R.id.contact_detail_phone_default);
            viewHolder.phoneType = convertView.findViewById(R.id.contact_detail_phone_type);
            viewHolder.phoneNumber = convertView.findViewById(R.id.contact_detail_phone_number);
            viewHolder.phoneCallAudioButton = convertView.findViewById(R.id.contact_detail_phone_call_audio);
            viewHolder.phoneCallVideoButton = convertView.findViewById(R.id.contact_detail_phone_call_video);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // making sure we get data from correct list
        if (!phonesUpdated) {
            ContactData.PhoneNumber phoneNumber = mContactData.mPhones.get(position);
            //Fill up data
            if (phoneNumber.Primary) {
                viewHolder.defaultContact.setVisibility(View.VISIBLE);
            } else {
                viewHolder.defaultContact.setVisibility(View.INVISIBLE);
            }
            viewHolder.phoneType.setText(getPhoneType(phoneNumber.Type));
            // if contact has @ in its phone number, we only display numbers before @ symbol
            viewHolder.phoneNumber.setText(CallData.parsePhone(phoneNumber.Number));
        } else {
            viewHolder.phoneNumber.setText(newPhoneNumbers.get(position));
        }

        viewHolder.phoneCallAudioButton.setOnClickListener(v -> {
            if (null != mListener) {
                // Call from Faforites, Contacts or History  > contact details in case of audio call
                GoogleAnalyticsUtils.googleAnalyticsCallFromContactsDetailes(mListener);

                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener.onCallContactAudio(mContactData, viewHolder.phoneNumber.getText().toString());
            }
        });
        if (!Utils.isCameraSupported()) {
            viewHolder.phoneCallVideoButton.setVisibility(View.INVISIBLE);
        } else {
            if (!SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
                viewHolder.phoneCallVideoButton.setAlpha(0.5f);
                viewHolder.phoneCallVideoButton.setEnabled(false);
            } else {
                viewHolder.phoneCallVideoButton.setEnabled(true);
                viewHolder.phoneCallVideoButton.setAlpha(1f);
                viewHolder.phoneCallVideoButton.setOnClickListener(v -> {
                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        mListener.onCallContactVideo(mContactData, viewHolder.phoneNumber.getText().toString());
                    }
                    // Call from Faforites, Contacts or History  > contact details in case of video call
                    assert mListener != null;
                    GoogleAnalyticsUtils.googleAnalyticsCallFromContactsDetailes(mListener);
                });
            }
        }

        return convertView;
    }

    /**
     * Get user friendly name for phone type
     *
     * @param type ContactData.PhoneType enum value
     * @return String representing ContactData.PhoneType
     */
    private String getPhoneType(ContactData.PhoneType type) {
        if (ContactData.PhoneType.WORK.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_work).toString();
        } else if (ContactData.PhoneType.MOBILE.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_mobile).toString();
        } else if (ContactData.PhoneType.HOME.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_home).toString();
        } else if (ContactData.PhoneType.HANDLE.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_handle).toString();
        } else if (ContactData.PhoneType.FAX.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_fax).toString();
        } else if (ContactData.PhoneType.PAGER.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_pager).toString();
        } else if (ContactData.PhoneType.ASSISTANT.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_assistant).toString();
        } else if (ContactData.PhoneType.OTHER.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_other).toString();
        }
        return "";
    }


    /**
     * ViewHolder class
     */
    private static class ViewHolder {
        ImageView defaultContact;
        TextView phoneType;
        TextView phoneNumber;
        ImageButton phoneCallAudioButton;
        ImageButton phoneCallVideoButton;
    }

}
