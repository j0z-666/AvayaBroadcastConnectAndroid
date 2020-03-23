package com.avaya.android.vantage.aaadevbroadcast.contacts;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ContactsLoader;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.model.DirectoryData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LocalContactsRepository {
    private static final String[] PROJECTION_PRIMARY = new String[]{
            ContactsContract.CommonDataKinds.Phone._ID,                          // 0
            ContactsContract.CommonDataKinds.Phone.TYPE,                         // 1
            ContactsContract.CommonDataKinds.Phone.LABEL,                        // 2
            ContactsContract.CommonDataKinds.Phone.NUMBER,                       // 3
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,                   // 4
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,                   // 5
            ContactsContract.CommonDataKinds.Phone.PHOTO_ID,                     // 6
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,         // 7
            ContactsContract.CommonDataKinds.Phone.MIMETYPE,                     // 8
    };
    private static LocalContactsRepository mInstance;
    private final Object lock = new Object();
    private final Map<String, ContactData> mContactMap;
    private final Map<Integer, List<ContactData>> mDirectoryMapList;
    private List<ContactData> mLocalContacts;
    private List<ContactData> mLocalContactsPaired;
    private List<DirectoryData> mDirectoryList;
    private Set<WeakReference<Listener>> listeners;
    private ContactsLoader contactsLoader;

    private LocalContactsRepository() {
        mLocalContacts = new ArrayList<>();
        mLocalContactsPaired = new ArrayList<>();
        mContactMap = new HashMap<>();
        mDirectoryMapList = new HashMap<>();
        mDirectoryList = new ArrayList<>();
    }

    public static LocalContactsRepository getInstance() {
        if (mInstance == null) {
            mInstance = new LocalContactsRepository();
        }
        return mInstance;
    }

    public List<ContactData> getLocalContacts() {
        return (mLocalContacts == null) ? Collections.EMPTY_LIST : mLocalContacts;
    }

    public void setLocalContacts(List<ContactData> localContacts) {
        List<ContactData> local = new ArrayList<>();
        List<ContactData> localPaired = new ArrayList<>();
        mContactMap.clear();
        for (ContactData contact : localContacts) {
            assert contact.mAccountType != null;
            if (contact.mAccountType.equals(ContactsRecyclerViewAdapter.PBAP_ACCOUNT)) {
                localPaired.add(contact);
            } else {
                local.add(contact);
            }
            if (contact.mPhones != null) {
                for (ContactData.PhoneNumber number : contact.mPhones) {
                    assert number.Number != null;
                    mContactMap.put(number.Number.replaceAll("\\D+", ""), contact);
                }
            }
        }

        mLocalContacts = local;
        mLocalContactsPaired = localPaired;

        notifyContactsChanged();
    }

    ContactData fillDirectoryPhoneNumbers(ContactData item) {
        String lookupKey = item.mUUID;
        String idStr = item.mDirectoryID;

        Uri uri = ContactsContract.Contacts.CONTENT_LOOKUP_URI
                .buildUpon()
                .appendEncodedPath(lookupKey)
                .appendPath(ContactsContract.Contacts.Entity.CONTENT_DIRECTORY)
                .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY, idStr)
                .build();

        try (Cursor phones = Objects.requireNonNull(ElanApplication.getContext()).getContentResolver().query(
                uri, PROJECTION_PRIMARY, null, null, null)) {
            while (phones != null && phones.moveToNext()) {
                String mimeType = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.MIMETYPE));
                if (mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                    String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    if (number != null && number.length() > 0) {
                        item.mPhones.add(
                                new ContactData.PhoneNumber(
                                        number,
                                        ContactData.PhoneType.WORK,
                                        item.mPhones.size() == 0,
                                        String.valueOf(phones.getPosition())
                                )
                        );
                    }
                }
            }
        }

        return item;
    }

    List<ContactData> getLocalContactsPaired() {
        return mLocalContactsPaired;
    }

    public List<DirectoryData> getDirectories() {
        return mDirectoryList;
    }

    public void setDirectories(List<DirectoryData> directoryList) {
        mDirectoryList = directoryList;
    }

    public void setDirectorySearchResults(Integer id, List<ContactData> directoryContacts) {
        synchronized (lock) {
            if (listeners != null) {
                mDirectoryMapList.put(id, directoryContacts);
                List<ContactData> results = new ArrayList<>();
                for (List<ContactData> data : mDirectoryMapList.values()) {
                    results.addAll(data);
                }
                for (WeakReference<Listener> listener : listeners) {
                    if (listener != null && listener.get() != null) {
                        mDirectoryMapList.size();
                        mDirectoryMapList.size();
                        listener.get().onDirectorySearchResult(results, mDirectoryMapList.size() == mDirectoryList.size());
                    }
                }
            }
        }
    }

    public ContactData getByPhone(String phone) {
        if (phone != null) {
            phone = phone.replaceAll("\\D+", "");
            if (mContactMap.containsKey(phone)) {
                return mContactMap.get(phone);
            }
        }
        return null;
    }

    public List<ContactData.PhoneNumber> getPhoneNumbers(String phone) {
        ContactData contactData = getByPhone(phone);
        return contactData == null ? new ArrayList<>()
                : contactData.mPhones;
    }

    public DirectoryData getDirectoryDataByName(boolean isOpenSIPEnabled) {
        for (int i = 0; i < mDirectoryList.size(); i++) {
            if (isOpenSIPEnabled) {
                if (Constants.BROADSOFT_CONTACT_TYPE.equals(mDirectoryList.get(i).type)) {
                    return mDirectoryList.get(i);
                }
            } else {
                if (Constants.IPO_CONTACT_TYPE.equals(mDirectoryList.get(i).type)) {
                    return mDirectoryList.get(i);
                }
            }
        }

        return null;
    }

    public void setContactsLoader(ContactsLoader contactsLoader) {
        this.contactsLoader = contactsLoader;
        contactsLoader.setLocalContactsRepository(this);
    }

    public String getPhotoUri(String phoneNumber) {
        ContactData contactData = getByPhone(phoneNumber);
        if (contactData != null) {
            return contactData.mPhotoURI;
        }

        return "";
    }

    void attachListener(Listener listener) {
        if (listener == null) return;

        synchronized (lock) {
            if (listeners == null) {
                listeners = new HashSet<>(1);
            }
            listeners.add(new WeakReference<>(listener));
        }
    }

    void detachListener(Listener listener) {
        if (listener == null) return;
        synchronized (lock) {
            if (listeners != null) {
                Iterator<WeakReference<Listener>> iterator = listeners.iterator();
                while (iterator.hasNext()) {
                    WeakReference<Listener> callback = iterator.next();
                    if (callback.get() == listener) iterator.remove();
                }
            }
        }
    }

    void fetchContacts() {
        contactsLoader.loadContacts();
    }

    void searchDirectoryContacts(String query) {
        mDirectoryMapList.clear();
        if (mDirectoryList != null && !mDirectoryList.isEmpty()) {
            for (DirectoryData dd : mDirectoryList) {
                contactsLoader.searchDirectoryContacts(query, dd.directoryID);
            }
        }
    }

    void stopSearchDirectoryContacts() {
        mDirectoryMapList.clear();
        for (DirectoryData dd : mDirectoryList) {
            contactsLoader.stopSearchDirectoryContacts(dd.directoryID);
        }
    }

    private void notifyContactsChanged() {
        synchronized (lock) {
            if (listeners != null) {
                for (WeakReference<Listener> listener : listeners) {
                    if (listener != null && listener.get() != null) {
                        listener.get().onLocalContactsChanged();
                    }
                }
            }
        }
    }

    public interface Listener {
        void onLocalContactsChanged();

        void onDirectorySearchResult(List<ContactData> contactData, boolean done);
    }
}