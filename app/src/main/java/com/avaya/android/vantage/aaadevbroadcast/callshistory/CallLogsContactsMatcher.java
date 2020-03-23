package com.avaya.android.vantage.aaadevbroadcast.callshistory;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.EnterpriseContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.contacts.LocalContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.model.CallData;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class CallLogsContactsMatcher {

    private static ContactsMatcherTask sContactsMatcherTask = null;
    private final Callback mListener;

    CallLogsContactsMatcher(Callback listener) {
        mListener = listener;
    }

    void setCallLogs(List<CallData> callLogs, boolean isServerMatching) {
        if (sContactsMatcherTask != null) {
            sContactsMatcherTask.cancel(true);
        }

        sContactsMatcherTask = new ContactsMatcherTask(this,
                new ArrayList<>(callLogs),
                isServerMatching);
        sContactsMatcherTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    interface Callback {
        void onContactsMatchingChanged(List<CallData> callLogs, boolean isServerMatching);
    }

    /**
     * Match call logs with the current repository data. First we try to match locally,
     * then match with enterprise contacts. At the end if cached data isn't available
     * match call logs with the latest fetched server data.
     */
    private static class ContactsMatcherTask extends AsyncTask<Void, Void, Integer> {

        private List<CallData> mCallLogs;
        private final WeakReference<CallLogsContactsMatcher> callLogsContactsMatcherReference;
        private final boolean mIsServerMatching;

        ContactsMatcherTask(CallLogsContactsMatcher callLogsContactsMatcherReference,
                            List<CallData> callLogs, boolean isServerMatching) {
            mCallLogs = callLogs;
            mIsServerMatching = isServerMatching;
            this.callLogsContactsMatcherReference = new WeakReference<>(callLogsContactsMatcherReference);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            if (callLogsContactsMatcherReference.get() == null) {
                return 0;
            }
            boolean firstNameFirst = ContactsFragment.isFirstNameFirst();

            int countOfMatched = 1;

            if (mCallLogs == null) {
                return 0;
            }

            for (CallData mCallLog : mCallLogs) {
                if (isCancelled()) {
                    return 0;
                }
                ContactData contactData = LocalContactsRepository.getInstance().getByPhone(mCallLog.mRemoteNumber);
                if (contactData != null) {
                    mCallLog.mName = contactData.getFormatedName(firstNameFirst);
                    mCallLog.setPhotoThumbnailURI(contactData.mPhotoThumbnailURI);
                    mCallLog.mPhone = contactData.mPhones.get(0).Number;
                    mCallLog.mContactCategory = contactData.mCategory;
                    mCallLog.mFirstName = contactData.mFirstName;
                    mCallLog.mLastName = contactData.mLastName;
                    mCallLog.mAccountType = contactData.mAccountType;
                    mCallLog.setURI(contactData.mURI);
                    mCallLog.setUUID(contactData.mUUID);
                    ++countOfMatched;
                } else {
                    if (mCallLog.getCallLogItem() == null) {
                        continue;
                    }

                    ContactData contact = EnterpriseContactsRepository.getInstance().getLookupEnterpriseContacts().get(mCallLog.mRemoteNumber);
                    if (contact != null) {
                        mCallLog.mName = contact.getFormatedName(firstNameFirst);
                        mCallLog.mFirstName = TextUtils.isEmpty(contact.mFirstName) ? "" : contact.mFirstName;
                        mCallLog.mLastName = TextUtils.isEmpty(contact.mLastName) ? "" : contact.mLastName;
                        mCallLog.mContactCategory = ContactData.Category.ENTERPRISE;
                        mCallLog.mPhoto = contact.mPhoto;
                        mCallLog.mLocation = contact.mLocation;
                        mCallLog.mCity = contact.mCity;
                        mCallLog.mCompany = contact.mCompany;
                        mCallLog.mPosition = contact.mPosition;
                        mCallLog.setUUID(contact.mUUID);
                        mCallLog.setPhoneNumbers(contact.mPhones);
                        mCallLog.mRefObject = contact.mRefObject;
                    } else {
                        // We should revert call data to initial state if deleted locally
                        CallData callData = CallLogsRepository.getInstance().getCallDataByPhoneNumber(mCallLog.mRemoteNumber);
                        if (callData != null) {
                            mCallLog.mName = callData.mName;
                            mCallLog.setPhotoThumbnailURI(callData.getPhotoThumbnailURI());
                            mCallLog.mPhone = callData.mPhone;
                            mCallLog.mContactCategory = callData.mContactCategory;
                            mCallLog.mFirstName = callData.mFirstName;
                            mCallLog.mLastName = callData.mLastName;
                            mCallLog.mAccountType = callData.mAccountType;
                            mCallLog.mPhoto = callData.mPhoto;
                            mCallLog.setURI(callData.getURI());
                            mCallLog.setUUID(callData.getUUID());
                            mCallLog.mRefObject = callData.mRefObject;
                        }
                    }

                    // If comma is there split it to get first and last name
                    if (mCallLog.mName.contains(",")) {
                        String[] fullName = mCallLog.mName.split(",");
                        mCallLog.mFirstName = fullName[0].trim();
                        if (fullName.length > 1) {
                            mCallLog.mLastName = fullName[1].trim();
                        } else {
                            mCallLog.mLastName = "";
                        }
                    }
                    ++countOfMatched;
                }
            }

            mCallLogs = Utils.mergeSort(mCallLogs);

            return countOfMatched;
        }

        @Override
        protected void onPostExecute(Integer countOfMatched) {
            super.onPostExecute(countOfMatched);
            CallLogsContactsMatcher matcherObject = callLogsContactsMatcherReference.get();
            if (matcherObject != null && countOfMatched > 0) {
                matcherObject.mListener.onContactsMatchingChanged(mCallLogs, mIsServerMatching);
            }
        }
    }
}