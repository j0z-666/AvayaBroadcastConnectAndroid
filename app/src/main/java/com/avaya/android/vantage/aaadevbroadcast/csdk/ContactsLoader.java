package com.avaya.android.vantage.aaadevbroadcast.csdk;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.contacts.LocalContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.model.DirectoryData;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.BROADSOFT_CONTACT_TYPE;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.IPO_CONTACT_TYPE;

/**
 * This class is used to get all local contacts from android device. All contacts are added in
 * ContactData List using Loader. First we load contact address information, store it by contact ID in
 * sparse array, than we load contacts firs and last name and store it in Sparse array, and at the end,
 * we load other information. We do this because all those information is held in different databases
 * and we cannot load them with one cursor.
 */
public class ContactsLoader implements LoaderManager.LoaderCallbacks<Cursor> {
    // constants
    private static final String TAG = "ContactsLoader";
    private static final String NAME_SORT_PREFERENCE = Constants.NAME_SORT_PREFERENCE;
    private static final String USER_PREFERENCE = Constants.USER_PREFERENCE;
    private static final int FIRST_NAME_FIRST = Constants.FIRST_NAME_FIRST;
    private static final int DIRCTORY_SEARCH_LIMIT = 50;
    private static final String DEFERRED_SNIPPETING_KEY = "deferred_snippeting";
    private static final int DEFAULT_IS_PRIMARY_VALUE = 0;
    private static final int DEFAULT_NUMBER_TYPE = 1;
    private static final String[] PROJECTION_PHONE = {
            ContactsContract.Contacts._ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.CONTACT_PRESENCE,
            ContactsContract.CommonDataKinds.Phone.CONTACT_STATUS,
            ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_ID,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_ALTERNATIVE,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
    };
    // activity we will use for weak reference
    private final Activity mActivity;
    private final WeakReference<Activity> weakContext;
    // other objects
    private final List<ContactData> mContactDataList = new ArrayList<>();
    private final SharedPreferences mUserPreference;
    // sparse arrays used to store contact data by contact ID
    private final SparseArray<String> mapAddr = new SparseArray<>();
    private final SparseArray<String[]> mapName = new SparseArray<>();
    private final SparseArray<List<ContactData.PhoneNumber>> mapPhones = new SparseArray<>();
    private String mFirstName = "";
    private String mLastName = "";
    private LocalContactsRepository mLocalContactsRepository;

    /**
     * Constructor
     *
     * @param activity - Loader needs activity, so we pass it here
     */
    public ContactsLoader(Activity activity) {
        weakContext = new WeakReference<>(activity);
        mActivity = activity;
        mUserPreference = activity.getSharedPreferences(USER_PREFERENCE, MODE_PRIVATE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mActivity.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED || mActivity.checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            Log.w(TAG, "permission to contacts is denied");
            // TODO: request permissions
            return null;
        }
        switch (id) {
            case Constants.LOCAL_CONTACTS_LOADER: // loading contact basic information
                return getContactsLoader();
            case Constants.LOCAL_NAME_LOADER: // loading first and last name
                return getNameLoader();
            case Constants.LOCAL_ADDRESS_LOADER: // loading addresses
                return getAddressLoader();
            case Constants.LOCAL_PHONE_LOADER:
                return getPhonesLoader();
            case Constants.DIRECTORY_LOADER:
                return getDirectoryLoader();
        }

        if (id > Constants.DIRECTORY_CONTACT_SEARCH_LOADER) {
            return getDirectoryContactsLoader(id - Constants.DIRECTORY_CONTACT_SEARCH_LOADER, args);
        }

        return null;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
        Log.d(TAG, "ContactLoad Start. Loader ID: " + loader.getId());

        if (loader.getId() > Constants.DIRECTORY_CONTACT_SEARCH_LOADER) {
            setDirectoryContacts(loader.getId() - Constants.DIRECTORY_CONTACT_SEARCH_LOADER, cursor);
        }

        if (cursor == null) {
            Log.e(TAG, "null cursor - probably no contacts db exist");
            return;
        }

        switch (loader.getId()) {
            case Constants.LOCAL_CONTACTS_LOADER:
                mLocalContactsRepository.setLocalContacts(getContactData(cursor));
                break;
            case Constants.LOCAL_NAME_LOADER:
                updateContactsNames(cursor);
                break;
            case Constants.LOCAL_ADDRESS_LOADER:
                updateContactsAddresses(cursor);
            case Constants.LOCAL_PHONE_LOADER:
                updateContactsPhones(cursor);
                break;
            case Constants.DIRECTORY_LOADER:
                setDirectories(cursor);
                break;
        }

        Log.d(TAG, "ContactLoad: END " + loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset: ");
    }

    public void setLocalContactsRepository(LocalContactsRepository mLocalContactsRepository) {
        this.mLocalContactsRepository = mLocalContactsRepository;
    }

    public void loadContacts() {
        mActivity.getLoaderManager().restartLoader(Constants.LOCAL_ADDRESS_LOADER, null, this);
    }

    public void searchDirectoryContacts(String query, int directoryID) {
        Bundle args = new Bundle();
        args.putString(Constants.DIRECTORY_CONTACT_QUERY_PARAM, query);
        mActivity.getLoaderManager().restartLoader(Constants.DIRECTORY_CONTACT_SEARCH_LOADER + directoryID, args, this);
    }

    public void stopSearchDirectoryContacts(int directoryID) {
        mActivity.getLoaderManager().destroyLoader(Constants.DIRECTORY_CONTACT_SEARCH_LOADER + directoryID);
    }

    private Uri getDirectorySearchUri(Uri baseUri, String directoryId, String searchTerm) {
        baseUri = baseUri
                .buildUpon()
                .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        directoryId).build();
        baseUri = baseUri
                .buildUpon()
                .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                        String.valueOf(DIRCTORY_SEARCH_LIMIT)).build();
        baseUri = baseUri
                .buildUpon()
                .appendQueryParameter(
                        DEFERRED_SNIPPETING_KEY, "1")
                .build();

        baseUri = Uri.withAppendedPath(baseUri, searchTerm)
                .buildUpon().build();

        return baseUri;
    }

    private CursorLoader getContactsLoader() {
        String[] projection = {ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_URI,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts.STARRED};
        return new CursorLoader(mActivity, ContactsContract.Contacts.CONTENT_URI,
                projection, null, null, null);
    }

    private CursorLoader getAddressLoader() {
        String[] projection = {ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS};
        return new CursorLoader(mActivity, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                projection, null, null, null);
    }

    private CursorLoader getNameLoader() {
        String[] projection = {ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                ContactsContract.CommonDataKinds.StructuredName.ACCOUNT_TYPE_AND_DATA_SET
        };
        String whereName = ContactsContract.Data.MIMETYPE + " = ?";
        String[] whereNameParams = new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};
        return new CursorLoader(mActivity, ContactsContract.Data.CONTENT_URI,
                projection, whereName, whereNameParams, null);
    }

    private CursorLoader getPhonesLoader() {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                ContactsContract.CommonDataKinds.Phone._ID
        };
        return new CursorLoader(mActivity, ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null, null);
    }

    private CursorLoader getDirectoryLoader() {
        return new CursorLoader(mActivity, ContactsContract.Directory.CONTENT_URI,
                null, null, null, ContactsContract.Directory.DISPLAY_NAME);
    }

    private Loader<Cursor> getDirectoryContactsLoader(Integer id, Bundle args) {
        Uri contentsUri = getDirectorySearchUri(ContactsContract.Contacts.CONTENT_FILTER_URI,
                String.valueOf(id),
                args.getString(Constants.DIRECTORY_CONTACT_QUERY_PARAM));

        return new CursorLoader(mActivity, contentsUri, PROJECTION_PHONE, null, null, null);
    }

    /**
     * this method will get all information from the local contacts and add it to mContactDataList
     */
    private List<ContactData> getContactData(Cursor cur) {
        int defaultValue = Constants.LAST_NAME_FIRST;
        String adminSortOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);
        if (adminSortOrder != null) {
            // doing this to prevent bug in case someone entered a value that is different from required values.
            if (adminSortOrder.equals("first,last")) {
                defaultValue = FIRST_NAME_FIRST;
            } else {
            }
        }

        // making sure we take admin settings in consideration
        if (mUserPreference.contains(NAME_SORT_PREFERENCE)) {
            Log.d(TAG, "User has already set name sort preference, so we will not change it");
        } else {
            Log.d(TAG, "No settings found for name sort, so we take admin settings in consideration. "
                    + "New admin settings are " + adminSortOrder);
        }
        int nameSort = mUserPreference.getInt(NAME_SORT_PREFERENCE, defaultValue);


        mContactDataList.clear();
        // making sure our activity is alive. Otherwise, no need to download anything
        if (weakContext.get() == null) {
            return mContactDataList;
        }

        if (cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {
                // getting display name
                String displayName = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                // getting photo
                String mPhotoThumbnailURI = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
                String mPhotoURI = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));

                // getting ID
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));

                // getting URI
                Uri uri = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_URI, String.valueOf(id));
                String mURI = uri.toString();

                // making sure we add only contacts who have phone numbers
                if (Integer.parseInt(cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                    int idContact = Integer.parseInt(id);

                    // setting up first, last name and account type
                    String accountType = "";
                    String[] fullName = mapName.get(idContact);
                    if (fullName != null && fullName.length > 2) {
                        mFirstName = fullName[0];
                        mLastName = fullName[1];
                        accountType = fullName[3];
                    }

                    // in case we have first and last name empty, we will use nickname to generate first and last name
                    if (((mFirstName == null) || (mFirstName.trim().length() == 0)) && ((mLastName == null) || (mLastName.trim().length() == 0))
                            && displayName.trim().length() > 0) {
                        String nameArray[] = displayName.split(" ", 2);
                        // adding first name
                        if (nameArray.length > 0) {
                            mFirstName = nameArray[0];
                        }
                        // adding last name
                        if (nameArray.length > 1) {
                            mLastName = nameArray[1];
                        }
                    }

                    // Did not get name from any combination, invalid data just skip
                    if (mFirstName == null || mLastName == null) {
                        continue;
                    }

                    mFirstName = mFirstName.trim();
                    mLastName = mLastName.trim();

                    // getting name sort preference
                    String mName;
                    if (nameSort == FIRST_NAME_FIRST) {
                        mName = Utils.combinedName(weakContext.get(), mFirstName, mLastName);
                    } else {
                        mName = Utils.combinedName(weakContext.get(), mLastName, mFirstName);
                    }

                    // getting is favorite (1 is yes, 0 is no)
                    boolean mIsFavorite = Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.STARRED))) != 0;

                    List<ContactData.PhoneNumber> phoneNumbers;
                    // getting phone numbers
                    phoneNumbers = mapPhones.get(idContact);

                    // getting address
                    String mCity = mapAddr.get(idContact);
                    if (mCity == null || mCity.trim().length() < 1) {
                        mCity = "";
                    }

                    // creating contact data
                    String mLocation = "";
                    String mCompany = "";
                    String mPosition = "";

                    ContactData.Category category = getContactDataCategory(accountType);

                    ContactData cdata = new ContactData(
                            mName,
                            mFirstName,
                            mLastName,
                            null,
                            mIsFavorite,
                            mLocation,
                            mCity,
                            mPosition,
                            mCompany,
                            phoneNumbers,
                            category,
                            id,
                            mURI,
                            mPhotoThumbnailURI,
                            true,
                            "",
                            mPhotoURI,
                            accountType,
                            "",
                            "");
                    // clearing up first and last name to prevent bug in case next contact does not have one of those
                    mFirstName = "";
                    mLastName = "";

                    mContactDataList.add(cdata);
                } else {
                    Log.d(TAG, "getContactData: " + displayName + " does not have a phone number.");
                }
            }
        }
        return mContactDataList;
    }

    private ContactData.Category getContactDataCategory(String accountType) {
        ContactData.Category category = ContactData.Category.LOCAL;
        if (accountType != null
                && accountType.length() > 0
                && IPO_CONTACT_TYPE.contains(accountType.toLowerCase())) {
            category = ContactData.Category.IPO;
        } else if (accountType != null
                && accountType.length() > 0
                && BROADSOFT_CONTACT_TYPE.contains(accountType.toLowerCase())) {
            category = ContactData.Category.BROADSOFT;
        }

        return category;
    }

    /**
     * Updates phone lookup table
     */
    private void updateContactsPhones(Cursor phoneCursor) {
        mapPhones.clear();
        while (phoneCursor.moveToNext()) {
            String contactId = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

            String number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            String numberTypeString = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
            int numberType = numberTypeString != null ? Integer.parseInt(numberTypeString) : DEFAULT_NUMBER_TYPE;

            String primaryTypeString = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY));
            int primaryType = primaryTypeString != null ? Integer.parseInt(primaryTypeString) : DEFAULT_IS_PRIMARY_VALUE;

            String phoneId = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));

            int idContact = Integer.parseInt(contactId);

            ContactData.PhoneNumber phoneNumber = new ContactData.PhoneNumber(number, LocalContactInfo.convertPhoneType(numberType), primaryType > 0, phoneId);

            List<ContactData.PhoneNumber> phones = mapPhones.get(idContact);
            if (phones == null) {
                phones = new ArrayList<>();
                phones.add(phoneNumber);
                mapPhones.put(idContact, phones);
            } else {
                phones.add(phoneNumber);
            }
        }

        // names are loaded, now we need to load other contact information
        mActivity.getLoaderManager().restartLoader(Constants.LOCAL_CONTACTS_LOADER, null, this);
    }

    /**
     * Getting contact first, last, middle name and account type
     *
     * @param nameCur cursor we get from cursor loader
     */
    private void updateContactsNames(Cursor nameCur) {
        mapName.clear();
        String firstName, lastName, middleName, accountType;
        int contactId;
        while (nameCur.moveToNext()) {
            contactId = nameCur.getInt(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID));
            firstName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            if (firstName == null) firstName = "";
            lastName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
            if (lastName == null) lastName = "";
            middleName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
            if (middleName == null) middleName = "";
            accountType = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.ACCOUNT_TYPE_AND_DATA_SET));
            if (accountType == null) accountType = "";
            mapName.put(contactId, new String[]{firstName, lastName, middleName, accountType});
        }

        // for now , there is no need at this point to get all phones for the contact
        mActivity.getLoaderManager().restartLoader(Constants.LOCAL_PHONE_LOADER, null, this);
    }

    /**
     * getting all contacts addresses
     *
     * @param cursor we get this from loader
     */
    private void updateContactsAddresses(Cursor cursor) {
        mapAddr.clear();
        while (cursor.moveToNext()) {
            Integer contactID = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID));
            String address = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
            mapAddr.put(contactID, address);
        }
        // addresses are loaded, now we need to load names
        mActivity.getLoaderManager().restartLoader(Constants.LOCAL_NAME_LOADER, null, this);
    }

    private void setDirectories(Cursor cursor) {
        ArrayList<DirectoryData> directoryList = new ArrayList<>();
        while (cursor != null && cursor.moveToNext()) {
            String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Directory.DISPLAY_NAME));
            String accountName = cursor.getString(cursor.getColumnIndex(ContactsContract.Directory.ACCOUNT_NAME));
            int id = cursor.getInt(cursor.getColumnIndex(ContactsContract.Directory._ID));
            String uri = Uri.withAppendedPath(ContactsContract.Directory.CONTENT_URI, String.valueOf(id)).toString();
            String type = cursor.getString(cursor.getColumnIndex(ContactsContract.Directory.ACCOUNT_TYPE));
            String packageName = cursor.getString(cursor.getColumnIndex(ContactsContract.Directory.PACKAGE_NAME));

            if (id != ContactsContract.Directory.DEFAULT && id != ContactsContract.Directory.LOCAL_INVISIBLE) {
                // just making sure we avoid any NPEs
                if (displayName == null) {
                    displayName = "";
                }
                if (accountName == null) {
                    accountName = "";
                }
                DirectoryData directory = new DirectoryData(displayName, accountName, id, uri, type, packageName);
                Log.d(TAG, "Adding directory: " + displayName + " account: " + accountName + " id: " + id);
                directoryList.add(directory);
            } else {
                Log.d(TAG, "Directory with id: " + id + " and name: " + displayName + " with account name: " + accountName + " not added");
            }
        }
        mLocalContactsRepository.setDirectories(directoryList);
    }

    private void setDirectoryContacts(Integer directoryId, Cursor cursor) {
        if (cursor == null) {
            mLocalContactsRepository.setDirectorySearchResults(directoryId, new ArrayList<>());
            return;
        }

        ArrayList<ContactData> contactList = new ArrayList<>();
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY));

            ArrayList<ContactData.PhoneNumber> phoneNumbers = new ArrayList<>();
            if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().isOpenSipEnabled()) {
                setBroadSoftPhoneNumber(lookupKey, phoneNumbers);
            }

            String[] parts;
            DirectoryData dirData = new DirectoryData("", "",
                    0, "", "", "");
            List<DirectoryData> mDirectoryList = mLocalContactsRepository.getDirectories();
            for (int i = 0; i < mDirectoryList.size(); ++i) {
                if (mDirectoryList.get(i).directoryID == directoryId) {
                    dirData = mDirectoryList.get(i);
                    break;
                }
            }
            if (name != null && lookupKey != null) {
                String firstName;
                String lastName = "";
                if (name.contains(",")) {
                    parts = name.split(",", 2);
                } else {
                    parts = name.split(" ", 2);
                }
                if (parts.length > 1) {
                    firstName = parts[1].trim();
                    lastName = parts[0].trim();
                } else {
                    firstName = name;
                }

                ContactData directoryContact = new ContactData(
                        name, lastName, firstName, null, false, "", "", "", "", phoneNumbers,
                        ContactData.Category.DIRECTORY, lookupKey, "", "", true, "", "", "",
                        dirData.directoryName, dirData.accountName);
                directoryContact.mDirectoryID = String.valueOf(dirData.directoryID);

                contactList.add(directoryContact);
            }
        }

        if (contactList.size() > 0) {
            contactList.get(0).setIsHeader(true);
        }

        mLocalContactsRepository.setDirectorySearchResults(directoryId, contactList);
    }

    private void setBroadSoftPhoneNumber(String lookupKey, ArrayList<ContactData.PhoneNumber> phoneNumbers) {
        String decodePhoneLookup;
        String phoneNumber;
        try {
            decodePhoneLookup = URLDecoder.decode(lookupKey, "UTF-8");
            if (decodePhoneLookup == null) {
                return;
            }

            char STX = '\u0002';
            phoneNumber = decodePhoneLookup.substring(0, decodePhoneLookup.indexOf(STX));

            phoneNumbers.add(new ContactData.PhoneNumber(phoneNumber,
                    ContactData.PhoneType.WORK, true, String.valueOf(0)));
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "Named character encoding is not supported");
        }
    }
}
