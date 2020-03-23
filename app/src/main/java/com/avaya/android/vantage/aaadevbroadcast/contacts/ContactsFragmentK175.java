package com.avaya.android.vantage.aaadevbroadcast.contacts;


import android.view.View;
import android.widget.EditText;

public class ContactsFragmentK175 extends ContactsFragment {

    @Override
    void setFilterViewVisibility(int visibility) {
        mFilterView.setVisibility(visibility);
    }

    @Override
    void setLayoutSearchVisibility() {
        searchLayout.setVisibility(View.VISIBLE);
    }

    @Override
    void setAddVisibility() {
        //TODO: ETI - verify that this is the correct logic
        if ((enableContactEdit != null && enableContactEdit.equals("0")) || addParticipant) {
            mAdd.setVisibility(View.INVISIBLE);
        } else {
            mAdd.setVisibility(View.VISIBLE);
        }
    }

    @Override
    void setFilterViewClickListener() {
        mFilterView.setOnClickListener(this);
    }

    @Override
    void setSyncContactViewVisibility(int visibility) {
        pairedDeviceSyncHelper.getSyncContactsView().setVisibility(visibility);
    }

    @Override
    void searchQueryInit(EditText searchEditText) {
        if (sSearchQuery != null && !sSearchQuery.isEmpty()) {
            searchEditText.setText(sSearchQuery);
        }
        search(sSearchQuery);
    }
}
