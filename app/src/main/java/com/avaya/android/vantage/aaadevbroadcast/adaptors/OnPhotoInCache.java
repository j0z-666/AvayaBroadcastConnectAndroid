package com.avaya.android.vantage.aaadevbroadcast.adaptors;

import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

/**
 * Interface which provide method for setting photos. It is used in
 * {@link com.avaya.android.vantage.aaadevbroadcast.csdk.ContactsAdaptor}
 */

public interface OnPhotoInCache {
    /**
     * Setting photo to contact {@link ContactData} from parameters provided
     *
     * @param contactData {@link ContactData}
     */
    void setPhoto(ContactData contactData);
}
