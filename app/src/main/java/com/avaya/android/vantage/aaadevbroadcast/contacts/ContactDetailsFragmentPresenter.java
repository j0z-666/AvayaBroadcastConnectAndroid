package com.avaya.android.vantage.aaadevbroadcast.contacts;

import android.text.TextUtils;

import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.util.ArrayList;
import java.util.List;

public class ContactDetailsFragmentPresenter {

    private final EnterpriseContactsRepository mEnterpriseContactsRepository;
    private final List<ContactData> mContacts;

    public ContactDetailsFragmentPresenter() {
        mContacts = new ArrayList<>();

        mContacts.addAll(LocalContactsRepository.getInstance().getLocalContacts());

        mEnterpriseContactsRepository = EnterpriseContactsRepository.getInstance();
    }

    public boolean findExistingContact(ContactData contactData) {
        ContactData contactDataToFind = null;
        if (contactData.mCategory.equals(ContactData.Category.ENTERPRISE)) {
            for (ContactData con : mEnterpriseContactsRepository.getEnterpriseContacts()) {
                if (con.mRefObject.getUniqueAddressForMatching().equals(contactData.mRefObject.getUniqueAddressForMatching())) {
                    contactDataToFind = con;
                    break;
                }
            }
        } else {
            contactDataToFind = getLocalContact(contactData.mUUID);
        }
        return contactDataToFind != null;
    }

    private ContactData getLocalContact(String uniqueLocalContactId) {
        for (ContactData contactData : mContacts) {
            if (!TextUtils.isEmpty(uniqueLocalContactId) && contactData.mUUID.equals(uniqueLocalContactId)) {
                return contactData;
            }
        }
        return null;
    }
}
