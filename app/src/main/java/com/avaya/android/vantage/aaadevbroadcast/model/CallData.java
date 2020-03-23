package com.avaya.android.vantage.aaadevbroadcast.model;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.clientservices.calllog.CallLogActionType;
import com.avaya.clientservices.calllog.CallLogItem;
import com.avaya.clientservices.contact.Contact;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An Object Representing CallData
 */
public class CallData implements Comparable {

    // enum used for call category.
    public enum CallCategory {
        ALL("All History", R.string.recent_call_all), MISSED("Missed Calls", R.string.recent_call_missed),
        OUTGOING("Outgoing Calls", R.string.recent_call_outgoing), INCOMING("Incoming Calls", R.string.recent_call_incoming),
        DELETE("Delete All History", R.string.recent_call_delete_history);
        final String mName;
        final int mId;

        CallCategory(String name, int resource_id) {
            mName = name;
            mId = resource_id;
        }

        @Override
        public String toString() {
            return mName;
        }

        public String getName() {
            return mName;
        }

        public int getResourceId() {
            return mId;
        }
    }

    public String mName;
    public final CallCategory mCategory;
    public final String mCallDate;
    public final long mCallDateTimestamp;
    public final String mCallTime;
    public final String mCallDuration;
    public String mPhone;
    public byte[] mPhoto;
    private String mURI;
    private String mPhotoThumbnailURI;
    public final String mRemoteNumber;
    public final boolean isFromPaired;
    public final boolean isNonCallableConference;
    private final CallLogItem callLogItem;
    public ContactData.Category mContactCategory;
    public String mAccountType;
    public List<ContactData.PhoneNumber> mPhones;
    public String mFirstName;
    public String mLastName;
    private String mUUID;
    public String mLocation;
    public String mCity;
    public String mCompany;
    public String mPosition;
    public Contact mRefObject;

    public CallData(
            String name,
            @NonNull CallCategory category,
            String date,
            long timestamp,
            String time,
            String duration,
            String phone,
            String uri,
            String photoThumbnailURI,
            String remoteNumber,
            boolean isFromPaired,
            boolean isNonCallableConference,
            CallLogItem callLogItem,
            ContactData.Category contactCategory,
            String uuID) {
        this.mName = name;
        this.mCategory = category;
        this.mCallDate = date;
        this.mCallDateTimestamp = timestamp;
        this.mCallTime = time;
        this.mCallDuration = duration;
        this.mPhone = phone;
        this.mURI = uri;
        this.mPhotoThumbnailURI = photoThumbnailURI;
        this.mRemoteNumber = remoteNumber;
        this.isFromPaired = isFromPaired;
        this.isNonCallableConference = isNonCallableConference;
        this.callLogItem = callLogItem; // only for server logs
        this.mContactCategory = contactCategory;
        this.mUUID = uuID;
    }

    public CallData(CallLogItem callLogItem) {
        this.callLogItem = callLogItem;

        mCategory = convertCategoryFromServer(callLogItem.getCallLogAction());

        mName = callLogItem.getRemoteNumber();
        if (!callLogItem.isConference() && callLogItem.getRemoteParticipants() != null && callLogItem.getRemoteParticipants().size() > 0) {
            String displayName = callLogItem.getRemoteParticipants().get(0).getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                mName = displayName;
            }
        }

        boolean nonCallableConference = false;
        boolean isIPOEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE);
        if (callLogItem.isConference() && (callLogItem.getCallEvents().size() > 0) && isIPOEnabled) {
            mName = Objects.requireNonNull(ElanApplication.getContext()).getResources().getString(R.string.conference);
            nonCallableConference = true;
        }

        if (mName.equalsIgnoreCase("WITHHELD")) {
            if (mCategory == CallData.CallCategory.INCOMING) {
                mName = Objects.requireNonNull(ElanApplication.getContext()).getResources().getString(R.string.private_address);
                nonCallableConference = true;
            } else if (callLogItem.getRemoteNumber() != null) {
                mName = callLogItem.getRemoteNumber();
            }
        }

        isNonCallableConference = nonCallableConference;

        isFromPaired = false;

        mCallDate = callLogItem.getStartTime().toString();
        mCallDateTimestamp = callLogItem.getStartTime().getTime();
        mCallTime = callLogItem.getStartTime().toString();

        mCallDuration = Long.toString(callLogItem.getDurationInSeconds());

        mPhone = mRemoteNumber = callLogItem.getRemoteNumber();

        // Neutral category until matching is done
        mContactCategory = ContactData.Category.ALL;
    }

    @Override
    public String toString() {
        return mName +
                mCategory +
                mCallDate +
                mCallTime +
                mCallDuration +
                mPhone;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 2 * result + mName.hashCode();
        result = 3 * result + mRemoteNumber.hashCode();
        result = 5 * result + mCategory.hashCode();
        result = 7 * result + mCallDate.hashCode();
        result = 11 * result + mCallTime.hashCode();
        result = 13 * result + (int) (mCallDateTimestamp % 1000007L);
        if (mCallDuration != null) {
            result = 17 * result + mCallDuration.hashCode();
        }
        result = 19 * result + mPhone.hashCode();
        if (mPhoto != null) {
            result = 23 *  result + Arrays.hashCode(mPhoto);
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        CallData callData = (CallData) obj;
        return mName.equals(callData.mName) &&
                mRemoteNumber.equals(callData.mRemoteNumber) &&
                mCallDate.equals(callData.mCallDate) &&
                mCallDateTimestamp == callData.mCallDateTimestamp &&
                mPhone.equals(callData.mPhone) &&
                mCategory == callData.mCategory &&
                mCallTime.equals(callData.mCallTime) &&
                Arrays.equals(mPhoto, callData.mPhoto);
    }

    /**
     * If the phoneNumber argument contains '@', the part after this character
     * including '@' will be removed.
     *
     * @param phoneNumber String that represents a phone number
     * @return String phoneNumber
     */
    public static String parsePhone(String phoneNumber) {
        if (phoneNumber != null && phoneNumber.length() > 1 && phoneNumber.indexOf("@") > 1) {
            phoneNumber = phoneNumber.substring(0, phoneNumber.indexOf("@"));
        }
        return phoneNumber;
    }

    /**
     * Method used to generate dummy call data
     *
     * @return dummy call data
     */
    public static CallData getDummyContactForPendingUpdate() {
        return new CallData("", CallCategory.INCOMING, "", 0, "", "", "", "", "", "", false, false, null, ContactData.Category.ALL, "");
    }

    public String getPhotoThumbnailURI() {
        return mPhotoThumbnailURI;
    }

    public void setURI(String mURI) {
        this.mURI = mURI;
    }

    public String getURI() {
        return mURI;
    }

    public void setUUID(String uuID) {
        this.mUUID = uuID;
    }

    public String getUUID() {
        return mUUID;
    }

    public void setPhotoThumbnailURI(String mPhotoThumbnailURI) {
        this.mPhotoThumbnailURI = mPhotoThumbnailURI;
    }

    public CallLogItem getCallLogItem() {
        return callLogItem;
    }

    public static CallData.CallCategory convertCategoryFromServer(CallLogActionType type) {
        return (type == CallLogActionType.MISSED) ? CallData.CallCategory.MISSED :
                (type == CallLogActionType.OUTGOING) ? CallData.CallCategory.OUTGOING : CallData.CallCategory.INCOMING;
    }

    public void setPhoneNumbers(List<ContactData.PhoneNumber> phoneNumbers) {
        this.mPhones = phoneNumbers;
    }

    public List<ContactData.PhoneNumber> getPhoneNumbers() {
        return mPhones;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return Long.compare(mCallDateTimestamp, ((CallData)o).mCallDateTimestamp);
    }
}
