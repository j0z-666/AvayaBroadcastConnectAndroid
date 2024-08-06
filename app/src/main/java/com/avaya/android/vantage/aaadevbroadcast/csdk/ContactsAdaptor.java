package com.avaya.android.vantage.aaadevbroadcast.csdk;

import android.content.Context;
import android.util.Log;

import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.contacts.EnterpriseContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.clientservices.contact.AddContactCompletionHandler;
import com.avaya.clientservices.contact.Contact;
import com.avaya.clientservices.contact.ContactError;
import com.avaya.clientservices.contact.ContactException;
import com.avaya.clientservices.contact.ContactService;
import com.avaya.clientservices.contact.ContactServiceListener;
import com.avaya.clientservices.contact.ContactSourceType;
import com.avaya.clientservices.contact.EditableContact;
import com.avaya.clientservices.contact.UpdateContactCompletionHandler;
import com.avaya.clientservices.contact.fields.ContactPhoneNumberType;
import com.avaya.clientservices.contact.fields.EditableContactPhoneField;
import com.avaya.clientservices.user.User;

import java.util.ArrayList;

import static com.avaya.android.vantage.aaadevbroadcast.csdk.ErrorManager.AADS_GENERAL_ERROR;
import static com.avaya.android.vantage.aaadevbroadcast.model.ContactData.PhoneType;
import static com.avaya.clientservices.contact.ContactError.NOT_FOUND;
import static com.avaya.clientservices.contact.ContactError.OPERATION_INPROGRESS;
import static com.avaya.clientservices.contact.ContactError.PROVIDER_DUPLICATE_CONTACT;

public class ContactsAdaptor implements ContactServiceListener {

    private static final String TAG = ContactsAdaptor.class.getSimpleName();

    private final Context mContext;
    private User mUser;
    private boolean mIsListeningToContactService = false;

    public ContactsAdaptor(Context context) {
        this.mContext = context;
    }

    /**
     * Get current method name for purpose of logging
     *
     * @return String with name of method
     */
    private static String getCurrentMethodName() {
        final StackTraceElement e = Thread.currentThread().getStackTrace()[3];
        final String s = e.getClassName();
        return s.substring(s.lastIndexOf('.') + 1) + "." + e.getMethodName();
    }

    /**
     * Used in Enterprise contact edit to retrieve SDK Contact object.
     *
     * @return User object
     */
    public User getUser() {
        return mUser;
    }

    /**
     * Set {@link User}
     *
     * @param user {@link User}
     */
    public void setUser(User user) {
        if (user == null && mUser != null && mIsListeningToContactService) {
            mUser.getContactService().removeListener(this);
            mIsListeningToContactService = false;
        }
        this.mUser = user;
        if (mUser != null && !mIsListeningToContactService) {
            mUser.getContactService().addListener(this);
            mIsListeningToContactService = true;
        }
    }

    /**
     * Called when the contact service becomes available.
     *
     * @param contactService The contact service associated with the callback.
     */
    @Override
    public void onContactServiceAvailable(ContactService contactService) {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when the contact service becomes unavailable.
     *
     * @param contactService The contact service associated with the callback.
     */
    @Override
    public void onContactServiceUnavailable(ContactService contactService) {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when a contact provider failed.
     *
     * @param contactService The contact service associated with the callback.
     * @param sourceType     The contact source type provided by this provider.
     * @param providerError  The error code for the failure.
     */
    @Override
    public void onContactServiceProviderFailed(ContactService contactService, ContactSourceType sourceType, ContactError providerError) {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when contact available provider list changes.
     */
    @Override
    public void onContactServiceAvailableProviderListChanged() {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when contact services add or search capability changed.
     */
    @Override
    public void onContactServiceCapabilitiesChanged() {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when loading of contacts for a contact provider has been completed successfully.
     *
     * @param contactService         The contact service associated with the callback.
     * @param contactSourceType      The contact source type provided by this provider.
     * @param contactLoadingComplete is contact loading completed
     */
    @Override
    public void onContactServiceLoadingComplete(ContactService contactService, ContactSourceType contactSourceType, boolean contactLoadingComplete) {
        Log.d(TAG, getCurrentMethodName());
        ErrorManager.getInstance().removeErrorFromList(AADS_GENERAL_ERROR);
        EnterpriseContactsRepository.getInstance().fetchContacts();
    }

    /**
     * Called when loading of contacts for a contact provider has been completed successfully.
     *
     * @param contactService         The contact service associated with the callback.
     * @param contactSourceType      The contact source type provided by this provider.
     * @param contactLoadingComplete is contact loading complete
     * @param providerError          The error code for the failure.
     */
    @Override
    public void onContactServiceLoadingFailed(ContactService contactService, ContactSourceType contactSourceType, boolean contactLoadingComplete, ContactError providerError) {
        Log.d(TAG, getCurrentMethodName());
        Log.d(TAG, "AADS contact retrieval failed with error code: " + providerError.toString());
        Utils.sendSnackBarData(mContext, mContext.getResources().getString(R.string.error_message_service_not_available), Utils.SNACKBAR_LONG);
        ErrorManager.getInstance().addErrorToList(AADS_GENERAL_ERROR);
    }

    @Override
    public void onContactGroupsLoadingFailed(ContactService contactService, ContactSourceType contactSourceType, ContactError contactError) {

    }

    @Override
    public void onContactServiceSelfContactUpdated(ContactService contactService, Contact contact) {

    }

    /**
     * Start editing of enterprise contacts
     *
     * @param contactData     {@link ContactData}
     * @param editableContact {@link EditableContact}
     */
    public void startEnterpriseEditing(ContactData contactData, EditableContact editableContact) {

        if (mContext == null) {
            return;
        }

        if (mUser == null) {
            Utils.sendSnackBarData(mContext, mContext.getResources().getString(R.string.contact_edit_user_not_found), Utils.SNACKBAR_LONG);
            return;
        }

        if (editableContact == null) {
            Utils.sendSnackBarData(mContext, mContext.getResources().getString(R.string.contact_edit_server_not_available), Utils.SNACKBAR_LONG);
            Log.e(TAG, "editable contact is null");
            return;
        }

        if (editableContact.getNativeFirstName().getCapability().isAllowed()) {
            editableContact.getNativeFirstName().setValue(contactData.mFirstName);
        } else {
            Log.d(TAG, "First name cannot be changed");
        }
        if (editableContact.getNativeLastName().getCapability().isAllowed()) {
            editableContact.getNativeLastName().setValue(contactData.mLastName);
        } else {
            Log.d(TAG, "Last name cannot be changed");
        }

        if (editableContact.getNativeDisplayName().getCapability().isAllowed()) {
            editableContact.getNativeDisplayName().setValue(contactData.mName);
        } else {
            Log.d(TAG, "Nickname cannot be changed");
        }

        if (editableContact.getCompany().getCapability().isAllowed()) {
            editableContact.getCompany().setValue(contactData.mCompany);
        } else {
            Log.d(TAG, "Company name cannot be changed");
        }

        if (editableContact.getTitle().getCapability().isAllowed()) {
            editableContact.getTitle().setValue(contactData.mPosition);
        } else {
            Log.d(TAG, "Job title cannot be changed");
        }

        if (editableContact.getCity().getCapability().isAllowed()) {
            editableContact.getCity().setValue(contactData.mCity);
        } else {
            Log.d(TAG, "City cannot be changed");
        }

        // Add created phone item to array and set it for editable contact
        ArrayList<EditableContactPhoneField> phoneNumbers = new ArrayList<>();

        // Create phone item
        for (int i = 0; i < contactData.mPhones.size(); ++i) {
            EditableContactPhoneField contactPhoneField = new EditableContactPhoneField();
            contactPhoneField.setPhoneNumber(contactData.mPhones.get(i).Number);
            contactPhoneField.setType(getConvertedPhoneType(contactData.mPhones.get(i).Type));
            contactPhoneField.setDefault(contactData.mPhones.get(i).Primary);
            phoneNumbers.add(contactPhoneField);
        }

        if (editableContact.getPhoneNumbers().getCapability().isAllowed()) {
            editableContact.getPhoneNumbers().setValues(phoneNumbers);
        } else {
            Log.d(TAG, "Contact phones cannot be changed");
        }

        editableContact.isFavorite().setValue(contactData.isFavorite());

        boolean contactSavable = editableContact.isContactSavable();

        if (!contactSavable) {
            Utils.sendSnackBarData(mContext, mContext.getResources().getString(R.string.contact_edit_not_savable_toast), Utils.SNACKBAR_LONG);
            Log.e(TAG, "Contact not savable");
            return;
        }

        mUser.getContactService().updateContact(editableContact, new UpdateContactCompletionHandler() {

            @Override
            public void onSuccess(Contact contact) {
                Utils.sendSnackBarData(mContext, mContext.getResources().getString(R.string.contact_edit_success), Utils.SNACKBAR_LONG);
            }

            @Override
            public void onError(ContactException error) {
                if (error.getError() == OPERATION_INPROGRESS) {
                    Utils.sendSnackBarData(ElanApplication.getContext(), ElanApplication.getContext().getString(R.string.contact_edit_operation_in_progress), true);
                } else if (error.getError() == NOT_FOUND) {
                    Utils.sendSnackBarData(ElanApplication.getContext(), ElanApplication.getContext().getString(R.string.contact_not_found), true);
                } else {
                    Utils.sendSnackBarData(ElanApplication.getContext(), ElanApplication.getContext().getString(R.string.contact_edit_general_error), true);
                }
                Log.e(TAG, "Failed to update contact: ", error);
            }
        });
    }

    /**
     * Method we use to create new enterprise cotnact
     *
     * @param contactData standard contact data object
     */
    public void createEnterpriseContact(final ContactData contactData) {
        if (mUser == null) {
            Log.e(TAG, "Cannot retrieve contact service, user is not set");
            Utils.sendSnackBarData(mContext, mContext.getString(R.string.enterprise_contact_service_not_available), Utils.SNACKBAR_LONG);
            return;
        }

        boolean addContactCapability = mUser.getContactService().getAddContactCapability().isAllowed();

        if (!addContactCapability) {
            Log.d(TAG, "Contact cannot be added");
            Utils.sendSnackBarData(mContext, mContext.getString(R.string.contact_edit_not_savable_toast), Utils.SNACKBAR_LONG);
        } else {
            mUser.getContactService().addContact(convertContactDataToEditable(contactData), new AddContactCompletionHandler() {
                @Override
                public void onSuccess(Contact contact, boolean b) {
                    String message = mContext.getString(R.string.contact_create_with_name);
                    String name = contactData.mName;
                    String displayMessage = message + name;
                    Utils.sendSnackBarData(mContext, displayMessage, Utils.SNACKBAR_LONG);
                    Log.d(TAG, "Contact has been added");
                }

                @Override
                public void onError(ContactException error) {
                    if (error.getError() == PROVIDER_DUPLICATE_CONTACT) {
                        Utils.sendSnackBarData(ElanApplication.getContext(), ElanApplication.getContext().getString(R.string.contact_duplicate), true);
                    } else {
                        Utils.sendSnackBarData(ElanApplication.getContext(), ElanApplication.getContext().getString(R.string.enterprise_contact_create_error), true);
                    }
                    Log.e(TAG, "Failed to add contact: " + error.getMessage(), error);
                }
            });
        }
    }

    /**
     * Used to convert regular contact data to editable contact
     *
     * @param contactData regular contact data
     * @return editable contact with all added information
     */
    private EditableContact convertContactDataToEditable(ContactData contactData) {
        EditableContact editableContact = mUser.getContactService().createEditableContact();
        editableContact.getNativeDisplayName().setValue(contactData.mName);
        editableContact.getNativeFirstName().setValue(contactData.mFirstName);
        editableContact.getNativeLastName().setValue(contactData.mLastName);
        editableContact.getTitle().setValue(contactData.mPosition);
        editableContact.getCompany().setValue(contactData.mCompany);
        editableContact.getCity().setValue(contactData.mCity);
        editableContact.isFavorite().setValue(contactData.isFavorite());

        for (ContactData.PhoneNumber phoneNumber : contactData.mPhones) {
            EditableContactPhoneField number = new EditableContactPhoneField();
            number.setPhoneNumber(phoneNumber.Number);
            number.setDefault(phoneNumber.Primary);
            number.setType(getConvertedPhoneType(phoneNumber.Type));
            editableContact.getPhoneNumbers().getValues().add(number);
        }
        return editableContact;
    }

    /**
     * From provided {@link PhoneType} obtain {@link ContactPhoneNumberType}
     *
     * @param phoneType {@link PhoneType}
     * @return {@link ContactPhoneNumberType}
     */
    private ContactPhoneNumberType getConvertedPhoneType(PhoneType phoneType) {
        if (ContactData.PhoneType.WORK.equals(phoneType)) {
            return ContactPhoneNumberType.WORK;
        } else if (ContactData.PhoneType.MOBILE.equals(phoneType)) {
            return ContactPhoneNumberType.MOBILE;
        } else if (ContactData.PhoneType.HOME.equals(phoneType)) {
            return ContactPhoneNumberType.HOME;
        } else if (ContactData.PhoneType.HANDLE.equals(phoneType)) {
            return ContactPhoneNumberType.HANDLE;
        } else if (ContactData.PhoneType.FAX.equals(phoneType)) {
            return ContactPhoneNumberType.FAX;
        } else if (ContactData.PhoneType.PAGER.equals(phoneType)) {
            return ContactPhoneNumberType.PAGER;
        } else if (ContactData.PhoneType.ASSISTANT.equals(phoneType)) {
            return ContactPhoneNumberType.ASSISTANT;
        } else if (ContactData.PhoneType.OTHER.equals(phoneType)) {
            return ContactPhoneNumberType.OTHER;
        }
        return ContactPhoneNumberType.WORK;
    }
}