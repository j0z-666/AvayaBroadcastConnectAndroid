package com.avaya.android.vantage.aaadevbroadcast.contacts;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class EnterpriseLookupContacts {

    private static EnterpriseLookupContactsTask sEnterpriseLookupContactsTask = null;
    private final Callback mListener;
    private List<ContactData> mEnterpriseContacts;

    public EnterpriseLookupContacts(@NonNull Callback callback) {
        mListener = callback;
    }

    void setEnterpriseContactsHashMap(List<ContactData> enterpriseContacts) {
        mEnterpriseContacts = enterpriseContacts;

        if (sEnterpriseLookupContactsTask != null) {
            sEnterpriseLookupContactsTask.cancel(true);
        }

        sEnterpriseLookupContactsTask = new EnterpriseLookupContactsTask(this);
        sEnterpriseLookupContactsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    interface Callback {
        void onContactsMatchingChanged(ConcurrentHashMap<String, ContactData> enterpriseLookupContacts);
    }

    private static class EnterpriseLookupContactsTask extends AsyncTask<Void, Void, ConcurrentHashMap<String, ContactData>> {
        private final List<ContactData> mEnterpriseContacts;
        private final WeakReference<EnterpriseLookupContacts> enterpriseLookupContactsReference;

        EnterpriseLookupContactsTask(EnterpriseLookupContacts enterpriseLookupContacts) {
            this.enterpriseLookupContactsReference = new WeakReference<>(enterpriseLookupContacts);
            mEnterpriseContacts = enterpriseLookupContactsReference.get().mEnterpriseContacts;
        }

        @Override
        protected ConcurrentHashMap<String, ContactData> doInBackground(Void... voids) {
            if (enterpriseLookupContactsReference.get() == null) {
                return new ConcurrentHashMap<>();
            }

            ConcurrentHashMap<String, ContactData> lookupEnterpriseContacts = new ConcurrentHashMap<>();

            for (ContactData contactData : mEnterpriseContacts) {
                if (isCancelled()) {
                    return new ConcurrentHashMap<>();
                }

                if (contactData.mPhones != null) {
                    for (ContactData.PhoneNumber number : contactData.mPhones) {
                        String sipNumber = number.Number;
                        int lengthOfNumber = sipNumber.indexOf("@");
                        String phoneNumber;
                        if (lengthOfNumber == -1) {
                            phoneNumber = sipNumber;
                        } else {
                            phoneNumber = sipNumber.substring(0, lengthOfNumber);
                        }

                        lookupEnterpriseContacts.put(phoneNumber.replaceAll("\\D+", ""), contactData);
                    }
                }
            }

            return lookupEnterpriseContacts;
        }

        @Override
        protected void onPostExecute(ConcurrentHashMap<String, ContactData> lookupEnterpriseContacts) {
            super.onPostExecute(lookupEnterpriseContacts);
            EnterpriseLookupContacts matcherObject = enterpriseLookupContactsReference.get();
            if (matcherObject != null) {
                matcherObject.mListener.onContactsMatchingChanged(lookupEnterpriseContacts);
            }
        }
    }
}