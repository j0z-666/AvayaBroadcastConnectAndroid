package com.avaya.android.vantage.aaadevbroadcast.csdk;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.provider.ContactsContract.AUTHORITY;

/**
 * This class is used to make changes to local contacts.
 */
public class LocalContactsManager {

    private static final String TAG = "LocalContactsManager";

    private static final int TYPE_HOME = 1;
    private static final int TYPE_MOBILE = 2;
    private static final int TYPE_WORK = 3;
    private static final int TYPE_FAX_WORK = 4;
    private static final int TYPE_PAGER = 6;
    private static final int TYPE_OTHER = 7;
    private static final int TYPE_MAIN = 12;
    private static final int TYPE_ASSISTANT = 19;

    private final WeakReference<Context> mContextReference;

    /**
     * constructor
     *
     * @param context context is needed for contentResolver
     */
    public LocalContactsManager(Context context) {
        mContextReference = new WeakReference<>(context);
    }

    /**
     * Execute background task that will create new contact entry
     *
     * @param contactData ContactData
     * @param imageUri    Uri
     */
    public void createContact(final ContactData contactData, final Uri imageUri) {
        AsyncTask.execute(() -> {
            try {
                if (mContextReference.get() == null) {
                    Log.e(TAG, "create contact, context is null");
                    return;
                }

                ContentResolver cr = mContextReference.get().getContentResolver();
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                // Adding support for BroadSoft account type. Currently we are storing it like IPO
                // because it's directory type
                if ((contactData.mCategory == ContactData.Category.IPO) || (contactData.mCategory == ContactData.Category.BROADSOFT)) {
                    // Adding IPO contact to the account
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, contactData.mAccountType)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contactData.mAccountName)
                            .build());

                    // Keep entire name input in the single field as IPO has only one field (duplicate name to both given and family name to handle sort correctly)
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contactData.mName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contactData.mFirstName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contactData.mLastName)
                            .build());

                    // Adding only first phone number as IPO supports only one phone number with type of Work
                    if (contactData.mPhones.size() > 0 && contactData.mPhones.get(0).Number != null) {
                        //int phoneType = convertPhoneType(contactData.mPhones.get(0).Type);
                        ops.add(ContentProviderOperation
                                .newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contactData.mPhones.get(0).Number)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, TYPE_WORK)
                                .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, true)
                                .build());
                    }
                } else {
                    // creating contact
                    ops.add(ContentProviderOperation
                            .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, contactData.mAccountType)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contactData.mAccountName)
                            .withValue(ContactsContract.RawContacts.STARRED, contactData.isFavorite() ? "1" : "0")
                            .build());

                    // first and last name
                    ops.add(ContentProviderOperation
                            .newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contactData.mFirstName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contactData.mLastName)
                            .build());

                    // phone numbers
                    for (int i = 0; i < contactData.mPhones.size(); i++) {
                        if (contactData.mPhones.get(i).Number != null) {
                            int phoneType = convertPhoneType(contactData.mPhones.get(i).Type);
                            ops.add(ContentProviderOperation
                                    .newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                    .withValue(ContactsContract.Data.MIMETYPE,
                                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contactData.mPhones.get(i).Number)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                            phoneType)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, contactData.mPhones.get(i).Primary)
                                    .build());
                        }
                    }

                    //Organization
                    assert contactData.mPosition != null;
                    assert contactData.mCompany != null;
                    if (!contactData.mCompany.equals("") || !contactData.mPosition.equals("")) {
                        ops.add(ContentProviderOperation
                                .newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, contactData.mCompany)
                                .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                                .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, contactData.mPosition)
                                .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                                .build());
                    }

                    //Address
                    assert contactData.mCity != null;
                    if (!contactData.mCity.equals("") && contactData.mCity.trim().length() > 0) {
                        ops.add(ContentProviderOperation
                                .newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.CommonDataKinds.StructuredPostal.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.MIMETYPE,
                                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, contactData.mCity)
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
                                .build());
                    }

                    //Photo
                    if (imageUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContextReference.get().getContentResolver(), imageUri);
                            //Perform check if we have to check for image rotation or not. If there is no path in database
                            //image is most likely provided directly from CSDK and we can ignore image rotation
                            bitmap = Utils.checkAndPerformBitmapRotation(bitmap, mContextReference.get().getContentResolver(), imageUri);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
                            outputStream.flush();
                            outputStream.close();
                            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, outputStream.toByteArray())
                                    .build());
                        } catch (Exception e) {
                            Log.e(TAG, "photo uri saving failed", e);
                        }
                    } else {
                        try {
                            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, null)
                                    .build());
                        } catch (Exception e) {
                            Log.e(TAG, "photo uri saving failed", e);
                        }
                    }
                }
                cr.applyBatch(AUTHORITY, ops);

                // long operation, check for context instance again
                if (mContextReference.get() != null) {
                    String message = mContextReference.get().getString(R.string.contact_create_with_name);
                    String name = contactData.mName;
                    String displayMessage = message + name;
                    Utils.sendSnackBarData(mContextReference.get(), displayMessage, Utils.SNACKBAR_LONG);
                }
                Log.i(TAG, "Contact successfully created");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create contact", e);
            }
        });
    }

    /**
     * Execute background task that will update contact data
     *
     * @param contactData ContactData
     * @param imageUri    Uri
     */
    public void updateContact(final ContactData contactData, final Uri imageUri) {
        // just checking if URI is OK
        if (contactData.mURI == null || contactData.mURI.trim().length() < 1) {
            return;
        }

        AsyncTask.execute(() -> {
            // getting much needed ID and RawId
            String contactID = getContactID(contactData.mURI);

            if (contactID == null) {
                Log.e(TAG, "Contact ID not found");
                return;
            }
            int rawId = getContactRawID(contactID);
            String contactRawId = rawId != -1 ? Integer.toString(rawId) : "";

            boolean operationResult = false;
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            if (contactData.mCategory == ContactData.Category.IPO) {
                ops.add(setContactNameOperation(contactID, contactData));
                ops.add(updatePhone(contactID, contactData.mPhones.get(0).phoneNumberId, contactData.mPhones.get(0).Number, TYPE_WORK, true));
            } else {
                ops.add(setContactNameOperation(contactID, contactData));
                ops.add(setAsFavoriteOperation(contactRawId, contactData.isFavorite()));
                ops.addAll(updateContactPhonesOperations(contactData.mURI, contactID, contactRawId, contactData.mPhones));
                ops.add(updateContactAddressOperation(contactID, contactData.mCity));
                ContentProviderOperation imageOp = updateContactImageOperation(contactRawId, imageUri);
                if (imageOp != null) {
                    ops.add(imageOp);
                }
                ops.add(updateCompanyInfoOperation(contactID, contactRawId, contactData.mCompany, contactData.mPosition));
            }

            try {
                mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
                operationResult = true;
            } catch (Exception e) {
                Log.e(TAG, "Error updating contact", e);
            }

            // Possible long operation before check for context again
            if (mContextReference.get() != null) {
                if (operationResult) {
                    Utils.sendSnackBarData(mContextReference.get(), mContextReference.get().getString(R.string.contact_edit_success), true);
                } else {
                    Utils.sendSnackBarData(mContextReference.get(), mContextReference.get().getString(R.string.contact_edit_general_error), true);
                }
            }
        });
    }

    /**
     * Execute background task that will delete contact data
     *
     * @param contactID String
     */
    public void deleteLocalContact(final String contactID) {
        AsyncTask.execute(() -> {
            boolean operationResult = false;
            Log.d(TAG, "Deleting contact with ID: " + contactID);
            if (contactID != null && contactID.trim().length() > 0) {
                String rawID = String.valueOf(getContactRawID(contactID));

                String selection = ContactsContract.RawContacts._ID + " = ?";
                String[] selectionArgs = new String[]{rawID};

                int count = mContextReference.get().getContentResolver().delete(ContactsContract.RawContacts.CONTENT_URI, selection, selectionArgs);
                operationResult = count > 0;
            } else {
                Log.d(TAG, "Contact cannot be deleted. Contact ID: " + contactID);
            }

            if (operationResult) {
                Utils.sendSnackBarData(mContextReference.get(), mContextReference.get().getString(R.string.contact_deleted), true);
            } else {
                Utils.sendSnackBarData(mContextReference.get(), mContextReference.get().getString(R.string.contact_undeletable_error), true);
            }
        });
    }

    /**
     * Method used to add contact to favorites
     *
     * @param isFavorite Contact favorite value (true: add to favorite, false remove from favorite)
     */
    public boolean setAsFavorite(Uri mContactUri, Boolean isFavorite) {
        String setFav = isFavorite ? "1" : "0";
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Contacts.STARRED, setFav);
        if (mContextReference.get() != null) {
            return mContextReference.get().getContentResolver().update(mContactUri, values, null, null) > 0;
        }
        return false;
    }

    /**
     * getting contact ID from URI.
     *
     * @param contactUri Android contact URI.
     * @return Contact ID.
     */
    private String getContactID(String contactUri) {
        Cursor cursor = null;
        String id = null;
        if (mContextReference.get() != null) {
            try {
                cursor = mContextReference.get().getContentResolver().query(Uri.parse(contactUri), null, null, null, null);
                int idx;
                if (cursor != null && cursor.moveToFirst()) {
                    idx = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                    id = cursor.getString(idx);
                }
            } catch (Exception e) {
                Log.e(TAG, "getContactID failed", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return id;
    }

    /**
     * get contact RAW ID to be able to add phones
     *
     * @param contactID contact id
     * @return raw contact ID
     */
    private int getContactRawID(String contactID) {

        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.RawContacts.CONTACT_ID + "=?";
        String[] selectionArgs = new String[]{contactID};

        int rawContactId = -1;
        if (mContextReference.get() != null) {
            try (Cursor c = mContextReference.get().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null)) {

                if (c != null && c.moveToFirst()) {
                    rawContactId = c.getInt(c.getColumnIndex(ContactsContract.RawContacts._ID));
                }
            } catch (Exception e) {
                Log.e(TAG, "getContactRawID failed", e);
            }
        }
        return rawContactId;
    }

    /**
     * @param contactID String
     * @param contact   ContactData
     * @return ContentProviderOperation
     */
    private ContentProviderOperation setContactNameOperation(String contactID, ContactData contact) {
        /*  Avaya model has [DisplayName, FirstName and LastName] this means that:
         *  - there is an issue of handling other name parts [NamePrefix, MiddleName and NameSuffix]
         *    for Google Contacts model has [DisplayName, NamePrefix, FirstName, MiddleName, LastName, NameSuffix]
         *    and Google Contacts can add/edit/remove those additional fields.
         */

        String selection = ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "=?";
        String[] selectionArgs = new String[]{contactID, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};

        return ContentProviderOperation
                .newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(selection, selectionArgs)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.mFirstName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.mLastName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.mName)
                .build();
    }

    /**
     * Method used to add contact to favorites
     *
     * @param id         Contact Unique ID
     * @param isFavorite Contact favorite value (true: add to favorite, false remove from favorite)
     */
    private ContentProviderOperation setAsFavoriteOperation(String id, Boolean isFavorite) {
        String selection = ContactsContract.RawContacts._ID + "=?";
        String[] selectionArgs = new String[]{id};

        return ContentProviderOperation
                .newUpdate(ContactsContract.Contacts.CONTENT_URI)
                .withSelection(selection, selectionArgs)
                .withValue(ContactsContract.RawContacts.STARRED, isFavorite ? "1" : "0")
                .build();
    }

    /**
     * Method used to update contact address
     *
     * @param rawId      contact unique RawID
     * @param newAddress new contact address
     */
    private ContentProviderOperation updateContactAddressOperation(String rawId, String newAddress) {
        if (hasAddressInfo(rawId)) {

            String selection = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] selectionArgs = new String[]{rawId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};

            return ContentProviderOperation
                    .newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(selection, selectionArgs)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, newAddress)
                    .build();
        } else {
            return ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI).withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, newAddress)
                    .build();
        }
    }

    /**
     * getting contact address info. If we do not find any info, we add dummy data so we can use
     * update option. Otherwise, changes would not take effect
     *
     * @param rawId contact unique rawId
     * @return boolean does address exist for contact
     */
    private boolean hasAddressInfo(String rawId) {
        boolean hasAddressInfo = false;

        if (mContextReference.get() != null) {

            String selection = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] selectionArgs = new String[]{rawId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};

            Cursor cursor = mContextReference.get().getContentResolver()
                    .query(ContactsContract.Data.CONTENT_URI, null, selection, selectionArgs, null);

            if (cursor != null) {
                try {
                    hasAddressInfo = cursor.getCount() > 0;
                } catch (Exception e) {
                    Log.e(TAG, "getAddressInfo failed", e);
                } finally {
                    cursor.close();
                }
            }
        }

        return hasAddressInfo;
    }

    /**
     * Setup new contact photo
     *
     * @param contactRawId Contact unique ID
     * @param imageUri     Image Uri
     */
    private ContentProviderOperation updateContactImageOperation(String contactRawId, Uri imageUri) {
        if (imageUri != null) {
            String selection = ContactsContract.Data.RAW_CONTACT_ID + " = ?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] selectionArgs = new String[]{String.valueOf(contactRawId), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};

            if (imageUri.toString().trim().length() > 0) {
                ByteArrayOutputStream outputStream = prepareImage(imageUri);
                if (hasImage(contactRawId)) {
                    return ContentProviderOperation
                            .newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(selection, selectionArgs)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, outputStream.toByteArray())
                            .build();
                } else {
                    return ContentProviderOperation
                            .newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, contactRawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, outputStream.toByteArray())
                            .build();
                }
            } else {
                return ContentProviderOperation
                        .newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(selection, selectionArgs)
                        .build();
            }
        } else {
            return null;
        }
    }

    /**
     * @param contactId String
     * @return boolean
     */
    private boolean hasImage(String contactId) {
        boolean hasImage = false;

        if (mContextReference.get() != null) {
            String orgWhere = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] orgWhereParams = new String[]{contactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};

            try (Cursor orgCur = mContextReference.get().getContentResolver()
                    .query(ContactsContract.Data.CONTENT_URI, null, orgWhere, orgWhereParams, null)) {

                hasImage = orgCur != null && orgCur.getCount() > 0;
            } catch (Exception e) {
                Log.e(TAG, "hasImage failed", e);
            }
        }

        return hasImage;
    }

    private ByteArrayOutputStream prepareImage(Uri imageUri) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContextReference.get().getContentResolver(), imageUri);
            //Perform check if we have to check for image rotation or not. If there is no path in database
            //image is most likely provided directly from CSDK and we can ignore image rotation
            bitmap = Utils.checkAndPerformBitmapRotation(bitmap, mContextReference.get().getContentResolver(), imageUri);
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
            }

            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            Log.e(TAG, "prepareImage failed", e);
        }

        return outputStream;
    }

    /**
     * Adds contact phone to specific contact.
     *
     * @param rawID       contact unique raw ID
     * @param phoneNumber contact phone that needs to be added
     * @param type        Phone number type to be added
     */
    private ContentProviderOperation addContactPhone(String rawID, String phoneNumber, ContactData.PhoneType type, boolean primary) {
        int phoneType = convertPhoneType(type);

        return ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, primary ? 1 : 0)
                .build();
    }


    /**
     * Method used to update phone number.
     *
     * @param id           contact unique ID
     * @param phoneId      phone number unique ID
     * @param newPhone     new phone number we are gonna add instead of the old one
     * @param newPhoneType phone type we need to assign to edited phone number
     */
    private ContentProviderOperation updatePhone(String id, String phoneId, String newPhone, Object newPhoneType, boolean primary) {
        String selection = ContactsContract.Data.CONTACT_ID + "=?" + " AND "
                + ContactsContract.Data.MIMETYPE + "=?" + " AND "
                + ContactsContract.CommonDataKinds.Phone._ID + "=?";
        String selectionArgs[] = {
                String.valueOf(id), // contact ID
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, // content type
                phoneId}; // ID to search

        return ContentProviderOperation
                .newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(selection, selectionArgs)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, newPhoneType)
                .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, primary ? 1 : 0)
                .build();
    }

    /**
     * This method gets updates contacts phone numbers. We get contact URI and new list of phone
     * numbers and update contact data on android device. Logic behind this is to get new list of
     * phone numbers and compare list sizes. Then we do a search by phone number ID and update
     * that phone number information.
     * In case list sizes are the same, we just update old information
     * In case lists sizes are different, we have another logic
     * 1. we search through new phone number list and add all phone numbers that do not have ID.
     * By logic, if phone number does not have ID, than it is new phone number.
     * 2. Than we compare old and new list. Search if there is any phone number ID that is removed.
     * If there is, we just delete that phone number
     * 3. Compare old and new list and check what ID's are matching, and just pass new information
     * to the content provider (update phone number information)
     *
     * @param uri           contact unique URI
     * @param contactId     contact unique ID
     * @param newPhonesList new list of phone numbers
     */
    private ArrayList<ContentProviderOperation> updateContactPhonesOperations(String uri, String contactId, String rawId, List<ContactData.PhoneNumber> newPhonesList) {

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // just doing necessary checks to avoid crashes
        if (newPhonesList == null) {
            return ops;
        }

        List<ContactData.PhoneNumber> oldPhoneList = getPhoneNumbers(uri); // we get old phone list so we can update it
        int oldSize = oldPhoneList.size();
        int newSize = newPhonesList.size();

        // easiest option. Lists are the same, so we just update phone IDs with new information
        if (oldSize == newSize) {
            Log.i(TAG, "Lists are the same: ");
            for (int i = 0; i < oldSize; i++) {
                ops.add(updatePhone(
                        contactId, oldPhoneList.get(i).phoneNumberId,
                        newPhonesList.get(i).Number,
                        convertPhoneType(newPhonesList.get(i).Type),
                        newPhonesList.get(i).Primary));
            }
        }

        // since old phone sizes differ, we need to know what to modify and what to delete
        if (oldSize != newSize) {
            Log.i(TAG, "Lists are not the same");

            // ID lists used to ease up the search
            List<String> newIds = new ArrayList<>();
            List<String> oldIds = new ArrayList<>();


            // we fill in newIds list with the new IDs that we will use to find what phone number has been deleted
            for (int i = 0; i < newSize; i++) {
                newIds.add(newPhonesList.get(i).phoneNumberId);
            }

            // we fill in newIds list with the new IDs that we will use to find what phone number has been deleted
            for (int i = 0; i < oldSize; i++) {
                oldIds.add(oldPhoneList.get(i).phoneNumberId);
            }

            // We add all phone numbers from new list that do not have ID or ID is null
            for (int i = 0; i < newPhonesList.size(); i++) {
                if (newPhonesList.get(i).phoneNumberId == null || newPhonesList.get(i).phoneNumberId.trim().length() < 1) {
                    ops.add(addContactPhone(rawId, newPhonesList.get(i).Number, newPhonesList.get(i).Type, newPhonesList.get(i).Primary));
                }
            }

            // search through the new list and check what phone number has to be deleted
            for (int i = 0; i < oldSize; i++) {
                String phoneId = oldPhoneList.get(i).phoneNumberId;
                if (!newIds.contains(phoneId)) {
                    ops.add(deleteContactPhone(contactId, phoneId));
                }
            }

            // finally, we update old phones with new changes
            for (int i = 0; i < newSize; i++) {
                String phoneId = newPhonesList.get(i).phoneNumberId;
                if (oldIds.contains(phoneId)) {
                    ops.add(updatePhone(contactId, phoneId, newPhonesList.get(i).Number, convertPhoneType(newPhonesList.get(i).Type), newPhonesList.get(i).Primary));
                }
            }
        }

        return ops;
    }

    /**
     * Deletes all contact phones from specific contact
     *
     * @param id      String
     * @param phoneId String
     * @return ContentProviderOperation
     */
    private ContentProviderOperation deleteContactPhone(String id, String phoneId) {

        String selection =
                ContactsContract.Data.CONTACT_ID + "=?" + " AND "
                        + ContactsContract.Data.MIMETYPE + "=?" + " AND "
                        + ContactsContract.CommonDataKinds.Phone._ID + "=?";
        String selectionArgs[] = {
                String.valueOf(id),
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                phoneId};

        return ContentProviderOperation
                .newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(selection, selectionArgs)
                .build();
    }

    /**
     * This method will get contact URI and return phone list data for specific contact. We use this
     * to be able to get old list of phones, and update it with new list.
     *
     * @param uri contact URI
     * @return list oh phone numbers
     */
    private List<ContactData.PhoneNumber> getPhoneNumbers(String uri) {
        List<ContactData.PhoneNumber> phoneNumbersAll = new ArrayList<>();

        if (mContextReference.get() != null) {
            try (Cursor phonesCursor = mContextReference.get().getContentResolver().query(Uri.parse(uri), null, null, null, null)) {

                if (phonesCursor != null && phonesCursor.moveToNext()) {
                    int columnIndex_ID = phonesCursor.getColumnIndex(ContactsContract.Contacts._ID);
                    String contactID = phonesCursor.getString(columnIndex_ID);

                    int columnIndex_HASPHONENUMBER = phonesCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                    String stringHasPhoneNumber = phonesCursor.getString(columnIndex_HASPHONENUMBER);

                    if (stringHasPhoneNumber.equalsIgnoreCase("1")) {
                        try (Cursor numCursor = mContextReference.get().getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactID,
                                null,
                                null)) {

                            if (numCursor != null) {
                                //Get the first phone number
                                while (numCursor.moveToNext()) {
                                    int columnIndex_number = numCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                    int columnIndex_Id = numCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
                                    String phoneNumber = numCursor.getString(columnIndex_number);
                                    String phoneId = numCursor.getString(columnIndex_Id);
                                    ContactData.PhoneNumber phone = new ContactData.PhoneNumber(phoneNumber, ContactData.PhoneType.OTHER, false, phoneId);
                                    phoneNumbersAll.add(phone);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to retrieve phone numbers", e);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve contact phones", e);
            }
        }
        return phoneNumbersAll;
    }

    /**
     * We use this method to convert out phone type to Google phone type
     *
     * @param type our phone type
     * @return int google phone type
     */
    private int convertPhoneType(ContactData.PhoneType type) {
        switch (type) {
            case WORK:
                return TYPE_WORK;
            case MOBILE:
                return TYPE_MOBILE;
            case HOME:
                return TYPE_HOME;
            case FAX:
                return TYPE_FAX_WORK;
            case PAGER:
                return TYPE_PAGER;
            case ASSISTANT:
                return TYPE_ASSISTANT;
            default:
                return TYPE_OTHER;
        }
    }

    /**
     * Updating company info. Since update will not work unless we have some data already, we are
     * performing checks to getCompanyInfo method. And if some value is null, we will add our dummy value.
     * After this is done, edit can be performed normally
     *
     * @param id       contact unique id
     * @param rawId    contact unique rawID
     * @param company  company name
     * @param position position
     */
    private ContentProviderOperation updateCompanyInfoOperation(String id, String rawId, String company, String position) {
        if (hasCompanyInfo(id)) {

            String orgWhere = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] orgWhereParams = new String[]{rawId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};

            return ContentProviderOperation
                    .newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(orgWhere, orgWhereParams)
                    .withValue(ContactsContract.CommonDataKinds.Organization.DATA, company)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, position)
                    .build();
        } else {
            return ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Organization.DATA, company)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, position)
                    .build();
        }
    }

    /**
     * Getting company information
     *
     * @param id contact unique ID
     */
    private boolean hasCompanyInfo(String id) {
        boolean hasCompanyInfo = false;

        if (mContextReference.get() != null) {
            String orgWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] orgWhereParams = new String[]{id, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};

            try (Cursor orgCur = mContextReference.get().getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, orgWhere, orgWhereParams, null)) {
                hasCompanyInfo = orgCur != null && orgCur.getCount() > 0;
            } catch (Exception e) {
                Log.e(TAG, "getCompanyInfo failed", e);
            }
        }

        return hasCompanyInfo;
    }
}