package com.avaya.android.vantage.aaadevbroadcast.contacts;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.clientservices.common.DataCollectionChangeType;
import com.avaya.clientservices.common.DataRetrievalCancelledException;
import com.avaya.clientservices.common.DataRetrievalWatcher;
import com.avaya.clientservices.common.DataRetrievalWatcherListener;
import com.avaya.clientservices.contact.Contact;
import com.avaya.clientservices.contact.ContactCompletionHandler;
import com.avaya.clientservices.contact.ContactException;
import com.avaya.clientservices.contact.ContactProviderSourceType;
import com.avaya.clientservices.contact.ContactSearchLocationType;
import com.avaya.clientservices.contact.ContactSearchScopeType;
import com.avaya.clientservices.contact.ContactService;
import com.avaya.clientservices.contact.ContactSourceType;
import com.avaya.clientservices.contact.fields.ContactEmailAddressFieldList;
import com.avaya.clientservices.contact.fields.ContactPhoneField;
import com.avaya.clientservices.contact.fields.ContactPhoneFieldList;
import com.avaya.clientservices.user.User;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.avaya.clientservices.contact.ContactError.OPERATION_INPROGRESS;

public class EnterpriseContactsRepository {

    private static final String TAG = EnterpriseContactsRepository.class.getSimpleName();
    private static EnterpriseContactsRepository mInstance;

    private final Object lock = new Object();
    private final Set<WeakReference<EnterpriseContactsRepository.Listener>> mListeners;
    private final DataRetrievalWatcher<Contact> mSearchWatcher;
    private final List<ContactData> mEnterpriseContacts;
    private final DataRetrievalWatcher<Contact> mDataRetrievalWatcher;
    private final String mAADSLabel;
    private ConcurrentHashMap<String, ContactData> mLookupEnterpriseContacts;

    private EnterpriseContactsRepository() {
        mEnterpriseContacts = new ArrayList<>();
        List<ContactData> mEnterpriseSearchContacts = new ArrayList<>();
        mLookupEnterpriseContacts = new ConcurrentHashMap<>();

        mListeners = new HashSet<>(1);

        mAADSLabel = Objects.requireNonNull(ElanApplication.getContext()).getResources().getString(R.string.aads);

        mDataRetrievalWatcher = new DataRetrievalWatcher<>();
        ContactDataRetrievalWatcherListener mContactDataRetrievalWatcherListener = new ContactDataRetrievalWatcherListener(mEnterpriseContacts, mDataRetrievalWatcher);
        mDataRetrievalWatcher.addListener(mContactDataRetrievalWatcherListener);

        mSearchWatcher = new DataRetrievalWatcher<>();
        SearchDataRetrievalWatcherListener mSearchDataRetrievalWatcherListener = new SearchDataRetrievalWatcherListener(mEnterpriseSearchContacts, mSearchWatcher);
        mSearchWatcher.addListener(mSearchDataRetrievalWatcherListener);
    }

    public static EnterpriseContactsRepository getInstance() {
        if (mInstance == null) {
            mInstance = new EnterpriseContactsRepository();
        }
        return mInstance;
    }

    public ConcurrentHashMap<String, ContactData> getLookupEnterpriseContacts() {
        return mLookupEnterpriseContacts;
    }

    void setLookupEnterpriseContacts(ConcurrentHashMap<String, ContactData> contactData) {
        mLookupEnterpriseContacts = contactData;
    }

    List<ContactData> getEnterpriseContacts() {
        return mEnterpriseContacts;
    }

    public ContactData getByPhone(String phone) {
        if (phone != null) {
            int lengthOfNumber = phone.indexOf("@");
            if (lengthOfNumber > 0) {
                phone = phone.substring(0, lengthOfNumber);
            }
            phone = phone.replaceAll("\\D+", "");
            if (mLookupEnterpriseContacts.containsKey(phone)) {
                return mLookupEnterpriseContacts.get(phone);
            }
        }
        return null;
    }

    public void fetchContacts() {
        mDataRetrievalWatcher.cancel();

        User user = SDKManager.getInstance().getContactsAdaptor().getUser();

        if (user != null) {
            ContactService contactService = user.getContactService();
            if (contactService == null) {
                Log.e(TAG, "retrieveContacts -> ContactService is null");
            } else if (!contactService.isServiceAvailable()) {
                Log.e(TAG, "retrieveContacts -> ContactService not null but unavailable");
            } else {
                contactService.getContacts(mDataRetrievalWatcher, ContactSourceType.ENTERPRISE);
            }
        } else {
            Log.e(TAG, "retrieveContacts -> User is null");
        }
    }

    void searchDirectoryContacts(String query) {
        stopSearchDirectoryContacts();

        User user = SDKManager.getInstance().getContactsAdaptor().getUser();

        if (user != null && user.getContactService().isServiceAvailable()
                && user.getContactService().getNetworkSearchContactCapability().isAllowed()) {
            user.getContactService().searchContacts(mSearchWatcher, query, ContactSearchScopeType.ALL, ContactSearchLocationType.NETWORK, 50, 50);
        } else {
            notifyDirectorySearchChanged(new ArrayList<>());
        }
    }

    void stopSearchDirectoryContacts() {
        mSearchWatcher.cancel();
    }

    void attachListener(EnterpriseContactsRepository.Listener listener) {
        if (listener == null) return;

        synchronized (lock) {
            mListeners.add(new WeakReference<>(listener));
        }
    }

    void detachListener(EnterpriseContactsRepository.Listener listener) {
        if (listener == null) return;

        synchronized (lock) {
            Iterator<WeakReference<EnterpriseContactsRepository.Listener>> iterator = mListeners.iterator();
            while (iterator.hasNext()) {
                WeakReference<EnterpriseContactsRepository.Listener> callback = iterator.next();
                if (callback.get() == listener) iterator.remove();
            }
        }
    }

    private void notifyDirectorySearchChanged(List<ContactData> items) {
        synchronized (lock) {
            for (WeakReference<EnterpriseContactsRepository.Listener> listener : mListeners) {
                if (listener != null && listener.get() != null) {
                    listener.get().onEnterpriseSearchResult(items, true);
                }
            }
        }
    }

    public interface Listener {
        void onEnterpriseContactsChanged(List<ContactData> contactDataList, ContactData.Category contactCategory);

        void onEnterpriseContactsRetrievalFailed(Exception failure);

        void onEnterpriseSearchResult(List<ContactData> contactData, boolean done);
    }

    private class ContactDataRetrievalWatcherListener implements DataRetrievalWatcherListener<Contact> {
        final List<ContactData> mContacts;
        final DataRetrievalWatcher mWatcher;
        int mImagesToLoad = 0;

        ContactDataRetrievalWatcherListener(@NonNull List<ContactData> contacts, @NonNull DataRetrievalWatcher watcher) {
            mContacts = contacts;
            mWatcher = watcher;
        }

        @Override
        public void onRetrievalProgress(DataRetrievalWatcher<Contact> watcher, boolean determinate, int numRetrieved, int total) {
        }

        @Override
        public void onRetrievalCompleted(DataRetrievalWatcher<Contact> watcher) {
        }

        @Override
        public void onRetrievalFailed(DataRetrievalWatcher<Contact> watcher, Exception failure) {
            Log.d(TAG, "ContactDataRetrievalWatcherListener -> onRetrievalFailed");
            if (!(failure instanceof DataRetrievalCancelledException)) {
                return;
            }

            notifyFailure(failure);
        }

        @Override
        public void onCollectionChanged(DataRetrievalWatcher<Contact> watcher, DataCollectionChangeType changeType, List<Contact> changedItems) {
            Log.d(TAG, "ContactDataRetrievalWatcherListener -> onCollectionChanged");

            mImagesToLoad = 0;
            if (mWatcher == watcher) {
                switch (changeType) {
                    case COLLECTION_CLEARED:
                        Log.d(TAG, "onCollectionChanged -> COLLECTION_CLEARED");
                        mContacts.clear();
                        return;
                    case ITEMS_ADDED:
                        Log.d(TAG, "onCollectionChanged -> ITEMS_ADDED");
                        addData(changedItems);
                        break;
                    case ITEMS_UPDATED:
                        Log.d(TAG, "onCollectionChanged -> ITEMS_UPDATED");
                        updateData(changedItems);
                        break;
                    case ITEMS_DELETED:
                        Log.d(TAG, "onCollectionChanged -> ITEMS_DELETED");
                        removeData(changedItems);
                        break;
                }
            } else {
                if (changeType == DataCollectionChangeType.ITEMS_ADDED) {
                    Log.d(TAG, "onCollectionChanged -> ITEMS_ADDED (new watcher)");
                    addData(changedItems);
                }
            }

            // In case we have images to load don't post to UI yet to avoid re renders
            if (mImagesToLoad == 0) {
                notifyChanges();
            }
        }

        private void addData(@NonNull final List<Contact> contacts) {
            for (Contact contact : contacts) {
                if ((TextUtils.isEmpty(contact.getNativeDisplayName().getValue()) &&
                        TextUtils.isEmpty(contact.getNativeFirstName().getValue()) &&
                        TextUtils.isEmpty(contact.getNativeLastName().getValue())) &&
                        contact.getPhoneNumbers().getValues().size() == 0) {
                    continue;
                }
                if (!contact.getPhoneNumbers().getContactProviderSourceType().equals(ContactProviderSourceType.LOCAL)) {
                    mContacts.add(getContactData(contact));
                }
            }
        }

        private void updateData(List<Contact> contacts) {
            HashMap<String, Contact> updateMap = new HashMap<>();
            for (Contact c : contacts) {
                updateMap.put(c.getUniqueAddressForMatching(), c);
            }

            for (int i = 0; i < mContacts.size(); ++i) {
                String uuid = mEnterpriseContacts.get(i).mUUID;
                if (updateMap.containsKey(uuid)) {
                    mContacts.set(i, getContactData(updateMap.get(uuid)));

                    updateMap.remove(uuid);

                    if (updateMap.size() == 0) {
                        break;
                    }
                }
            }
        }

        private void removeData(@NonNull final List<Contact> contacts) {
            HashMap<String, Contact> updateMap = new HashMap<>();
            for (Contact c : contacts) {
                updateMap.put(c.getUniqueAddressForMatching(), c);
            }

            ListIterator<ContactData> iter = mContacts.listIterator();
            while (iter.hasNext()) {
                String uuid = iter.next().mUUID;

                if (updateMap.containsKey(uuid)) {
                    iter.remove();

                    updateMap.remove(uuid);

                    if (updateMap.size() == 0) {
                        break;
                    }
                }
            }
        }

        private ContactData getContactData(@NonNull final Contact contact) {

            String email = "";
            ContactEmailAddressFieldList contactEmailList = contact.getEmailAddresses();
            for (int i = 0; i < contactEmailList.getValues().size(); i++) {
                email = contactEmailList.getValues().get(i).getAddress();
            }

            int phoneId = 0;
            ContactPhoneFieldList phones = contact.getPhoneNumbers();
            ContactData.Category category = (phones.getContactProviderSourceType() == ContactProviderSourceType.LOCAL) ? ContactData.Category.LOCAL : ContactData.Category.ENTERPRISE;
            List<ContactData.PhoneNumber> uiphones = new ArrayList<>();
            for (ContactPhoneField phone : phones.getValues()) {
                ContactData.PhoneType type = ContactData.PhoneType.HOME;
                switch (phone.getType()) {

                    case WORK:
                        type = ContactData.PhoneType.WORK;
                        break;
                    case HOME:
                        type = ContactData.PhoneType.HOME;
                        break;
                    case MOBILE:
                        type = ContactData.PhoneType.MOBILE;
                        break;
                    case HANDLE:
                        type = ContactData.PhoneType.HANDLE;
                        break;
                    case FAX:
                        type = ContactData.PhoneType.FAX;
                        break;
                    case PAGER:
                        type = ContactData.PhoneType.PAGER;
                        break;
                    case ASSISTANT:
                        type = ContactData.PhoneType.ASSISTANT;
                        break;
                    case OTHER:
                        type = ContactData.PhoneType.OTHER;
                        break;
                }

                String phoneNumber = phone.getPhoneNumber();
                uiphones.add(new ContactData.PhoneNumber(phoneNumber, type, phone.isDefault(), String.valueOf(phoneId++)));
            }

            String displayName = contact.getNativeFirstName().getValue() + " " + contact.getNativeLastName().getValue();

            final ContactData ui_contact = new ContactData(displayName,
                    contact.getNativeFirstName().getValue(),
                    contact.getNativeLastName().getValue(),
                    null,
                    contact.isFavorite().getValue(),
                    contact.getLocation().getValue(),
                    contact.getCity().getValue(),
                    contact.getTitle().getValue(),
                    contact.getCompany().getValue(),
                    uiphones,
                    category,
                    contact.getUniqueAddressForMatching(),
                    "",
                    "",
                    true,
                    email,
                    "",
                    "",
                    "",
                    "");

            ui_contact.mRefObject = contact;

            if (contact.hasPicture()) {
                if (hasPictureData(contact)) {
                    ui_contact.mPhoto = contact.getPictureData();
                } else {
                    ++mImagesToLoad;
                    contact.retrievePicture(new ContactCompletionHandler() {

                        @Override
                        public void onSuccess() {
                            if (ui_contact.mRefObject == contact) {
                                ui_contact.mPhoto = contact.getPictureData();
                            }
                            onPhotoLoaded();
                        }

                        @Override
                        public void onError(ContactException error) {
                            // In case of racing condition where operation is in progress just requeue it again in half a sec
                            if (error.getError() == OPERATION_INPROGRESS) {
                                new Handler().postDelayed(() -> contact.retrievePicture(this), 500);
                            } else {
                                --mImagesToLoad;
                            }
                        }
                    });
                }
            }

            return ui_contact;
        }

        boolean hasPictureData(Contact contact) {
            byte[] data = contact.getPictureData();
            return (data != null) && (data.length > 0);
        }

        private void onPhotoLoaded() {
            // Do not update UI on every photo load, update it only when all images are ready
            if (((--mImagesToLoad % 100) == 0) || (mImagesToLoad < 0)) {
                notifyChanges();
            }
        }

        void notifyChanges() {
            synchronized (lock) {
                for (WeakReference<EnterpriseContactsRepository.Listener> listener : mListeners) {
                    if (listener != null && listener.get() != null) {
                        listener.get().onEnterpriseContactsChanged(mContacts, ContactData.Category.ENTERPRISE);
                    }
                }
            }
        }

        void notifyFailure(Exception failure) {
            synchronized (lock) {
                for (WeakReference<EnterpriseContactsRepository.Listener> listener : mListeners) {
                    if (listener != null && listener.get() != null) {
                        listener.get().onEnterpriseContactsRetrievalFailed(failure);
                    }
                }
            }
        }
    }

    private class SearchDataRetrievalWatcherListener extends ContactDataRetrievalWatcherListener {

        private SearchDataRetrievalWatcherListener(@NonNull List<ContactData> contacts, @NonNull DataRetrievalWatcher watcher) {
            super(contacts, watcher);
        }

        protected void notifyChanges() {
            synchronized (lock) {
                for (WeakReference<EnterpriseContactsRepository.Listener> listener : mListeners) {
                    if (listener != null && listener.get() != null) {
                        if (mContacts.size() > 0) {
                            mContacts.get(0).setIsHeader(true);
                            mContacts.get(0).mDirectoryName = mAADSLabel;
                        }
                        listener.get().onEnterpriseSearchResult(mContacts, true);
                    }
                }
            }
        }

        protected void notifyFailure(Exception failure) {
            synchronized (lock) {
                for (WeakReference<EnterpriseContactsRepository.Listener> listener : mListeners) {
                    if (listener != null && listener.get() != null) {
                        listener.get().onEnterpriseSearchResult(new ArrayList<>(), true);
                    }
                }
            }
        }
    }
}