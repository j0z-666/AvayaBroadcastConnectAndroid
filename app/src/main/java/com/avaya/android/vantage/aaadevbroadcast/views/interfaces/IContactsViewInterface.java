package com.avaya.android.vantage.aaadevbroadcast.views.interfaces;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;

import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.util.List;

/**
 * Interface used in classes that load contact information. It provides communication between
 * {@link com.avaya.android.vantage.aaadevbroadcast.views.adapters.MyFavoritesRecyclerViewAdapter}
 */
public interface IContactsViewInterface {
    void recreateData(List<ContactData> items, ContactData.Category contactCategory);

    void updateItem(ContactData item);

    void removeItem(int position);

    void removeItem(ContactData contact);

    void addItem(ContactData item);

    void cacheContactDrawable(String mUUID, RoundedBitmapDrawable circularBitmapDrawable);

    boolean isPhotoCached(String uuid);

    int getIndexOf(ContactData item);
}
