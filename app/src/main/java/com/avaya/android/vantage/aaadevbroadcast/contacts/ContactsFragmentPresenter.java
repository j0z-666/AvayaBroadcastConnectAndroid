package com.avaya.android.vantage.aaadevbroadcast.contacts;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.avaya.android.vantage.aaadevbroadcast.bluetooth.PairedDeviceSyncHelper;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ContactsFragmentPresenter implements LocalContactsRepository.Listener,
        EnterpriseContactsRepository.Listener, EnterpriseLookupContacts.Callback {

    private static final String TAG = ContactsFragmentPresenter.class.getSimpleName();
    private final LocalContactsRepository mLocalContactsRepository;
    private final EnterpriseContactsRepository mEnterpriseContactsRepository;
    private final ViewCallback mViewCallback;
    private final PairedDeviceSyncHelper mPairedDeviceSyncHelper;
    private final List<ContactData> mContacts;
    private final EnterpriseLookupContacts mEnterpriseLookupContacts;
    private List<ContactData> mDirectorySearchResult;
    private List<ContactData> mEnterpriseSearchResult;
    private boolean mIncludePairedContacts = false;
    private boolean mSortByFirstName;
    private boolean mDirectorySearchDone = false;
    private boolean mEnterpriseSearchDone = false;
    private String mLastSearchQuery = "";

    public ContactsFragmentPresenter(@NonNull ViewCallback viewCallback, PairedDeviceSyncHelper pairedDeviceSyncHelper) {
        mViewCallback = viewCallback;
        mPairedDeviceSyncHelper = pairedDeviceSyncHelper;

        mContacts = new ArrayList<>();

        mDirectorySearchResult = new ArrayList<>();
        mEnterpriseSearchResult = new ArrayList<>();

        mSortByFirstName = ContactsFragment.shouldSortByFirstName();

        mLocalContactsRepository = LocalContactsRepository.getInstance();
        if (mLocalContactsRepository != null) {
            mLocalContactsRepository.attachListener(this);
        }

        mEnterpriseContactsRepository = EnterpriseContactsRepository.getInstance();
        if (mEnterpriseContactsRepository != null) {
            mEnterpriseContactsRepository.attachListener(this);
        }

        mEnterpriseLookupContacts = new EnterpriseLookupContacts(this);

        setIncludePairedContacts();
        buildContactsList();
    }

    @Override
    public void onLocalContactsChanged() {
        setIncludePairedContacts();
        buildContactsList();

        mViewCallback.refreshMatcherData();
    }

    @Override
    public void onDirectorySearchResult(List<ContactData> contacts, boolean done) {
        if (mLastSearchQuery.length() > 0) {
            mDirectorySearchResult = contacts.stream()
                    .filter(c -> c.mName.toLowerCase().contains(mLastSearchQuery.toLowerCase()) || c.mFirstName.toLowerCase().contains(mLastSearchQuery.toLowerCase()) || c.mLastName.toLowerCase().contains(mLastSearchQuery.toLowerCase()))
                    .collect(Collectors.toList());
            mDirectorySearchDone = done;
            buildSearchResults();
        } else {
            mDirectorySearchResult = new ArrayList<>();
            mDirectorySearchDone = true;
        }
    }

    @Override
    public void onEnterpriseSearchResult(List<ContactData> contacts, boolean done) {
        if (mLastSearchQuery.length() > 0) {
            mEnterpriseSearchResult = contacts;
            mEnterpriseSearchDone = done;
            buildSearchResults();
        } else {
            mEnterpriseSearchResult = new ArrayList<>();
            mEnterpriseSearchDone = true;
        }
    }

    @Override
    public void onEnterpriseContactsChanged(List<ContactData> contactDataList, ContactData.Category contactCategory) {
        buildContactsList();
    }

    @Override
    public void onEnterpriseContactsRetrievalFailed(Exception failure) {
        Log.d(TAG, "Failed to retrieve enterprise contacts. " + failure.getMessage());
        if (mViewCallback != null) {
            mViewCallback.setRefreshing(false);
        }
    }

    @Override
    public void onContactsMatchingChanged(ConcurrentHashMap<String, ContactData> enterpriseLookupContacts) {
        mEnterpriseContactsRepository.setLookupEnterpriseContacts(enterpriseLookupContacts);
        mViewCallback.refreshMatcherData();
    }

    public void refresh() {
        refreshData();
    }

    public void destroy() {
        if (mLocalContactsRepository != null) {
            mLocalContactsRepository.detachListener(this);
        }

        if (mEnterpriseContactsRepository != null) {
            mEnterpriseContactsRepository.detachListener(this);
        }
    }

    public List<ContactData> getContacts() {
        return mContacts;
    }

    void searchQueryChange(String query) {
        mLastSearchQuery = query;
        mDirectorySearchDone = false;
        mEnterpriseSearchDone = false;

        mEnterpriseSearchResult.clear();
        mDirectorySearchResult.clear();

        if (query == null || query.isEmpty()) {
            mLocalContactsRepository.stopSearchDirectoryContacts();
            mEnterpriseContactsRepository.stopSearchDirectoryContacts();

            if (mViewCallback != null) {
                mViewCallback.hideLoader();
            }
        } else {
            if (mViewCallback != null) {
                mViewCallback.showLoader();
            }

            mLocalContactsRepository.searchDirectoryContacts(query);
            mEnterpriseContactsRepository.searchDirectoryContacts(query);
        }
    }

    void checkIfContactsLoaded() {
        if (mEnterpriseContactsRepository != null &&
                mEnterpriseContactsRepository.getEnterpriseContacts().size() == 0) {
            mEnterpriseContactsRepository.fetchContacts();
        }
    }

    void removePairedDeviceContacts() {
        if (!mPairedDeviceSyncHelper.isBluetoothEnabled()) return;
        if (mLocalContactsRepository.getLocalContactsPaired() != null && !mLocalContactsRepository.getLocalContactsPaired().isEmpty()) {
            mViewCallback.showMessage(true);
        }

        mIncludePairedContacts = false;
        buildContactsList();
    }

    void addPairedDeviceContacts() {
        if (!mPairedDeviceSyncHelper.isBluetoothEnabled()) return;

        mViewCallback.showMessage(false);
        mIncludePairedContacts = true;
        buildContactsList();
    }

    void refreshContactsFromPairedDevice() {
        if (mLocalContactsRepository != null) {
            mLocalContactsRepository.fetchContacts();
        }
    }

    void setSortByFirstName(boolean mSortByFirstName) {
        if (this.mSortByFirstName ^ mSortByFirstName) {
            this.mSortByFirstName = mSortByFirstName;
            Collections.sort(mContacts, new NameContactComparator(mSortByFirstName));
            notifyContactDataChanged();
        }
    }

    private void setIncludePairedContacts() {
        mIncludePairedContacts = mPairedDeviceSyncHelper != null
                && mPairedDeviceSyncHelper.getPairedItemsEnabledStatus();
    }

    private void buildSearchResults() {
        List<ContactData> result = new ArrayList<>();

        result.addAll(mEnterpriseSearchResult);
        result.addAll(mDirectorySearchResult);

        if (mViewCallback != null) {
            mViewCallback.onExtraSearchResults(result);
            if (mDirectorySearchDone || mEnterpriseSearchDone) {
                // Delayed hide of loader to approximate alignment with result display
                new Handler().postDelayed(mViewCallback::hideLoader, 1000);
            }
        }
    }

    private void buildContactsList() {
        mContacts.clear();

        List<ContactData> contactDataList = mEnterpriseContactsRepository.getEnterpriseContacts();
        if (!contactDataList.isEmpty()) {
            mEnterpriseLookupContacts.setEnterpriseContactsHashMap(new ArrayList<>(contactDataList));
        }

        mContacts.addAll(mLocalContactsRepository.getLocalContacts());
        if (mIncludePairedContacts) {
            mContacts.addAll(mLocalContactsRepository.getLocalContactsPaired());
        }
        mContacts.addAll(contactDataList);

        mContacts.sort(new NameContactComparator(mSortByFirstName));

        notifyContactDataChanged();
    }

    private void refreshData() {
        if (mEnterpriseContactsRepository != null) {
            mEnterpriseContactsRepository.fetchContacts();
        }
        if (mLocalContactsRepository != null) {
            mLocalContactsRepository.fetchContacts();
        }
    }

    private void notifyContactDataChanged() {
        if (mViewCallback != null) {
            mViewCallback.onContactsDataChanged(mContacts);
            mViewCallback.setRefreshing(false);
        }
    }

    public interface ViewCallback {
        void onExtraSearchResults(List<ContactData> contacts);

        void onContactsDataChanged(List<ContactData> contacts);

        void setRefreshing(boolean refreshing);

        void showLoader();

        void hideLoader();

        void showMessage(boolean removePairedContacts);

        void refreshMatcherData();
    }
}
