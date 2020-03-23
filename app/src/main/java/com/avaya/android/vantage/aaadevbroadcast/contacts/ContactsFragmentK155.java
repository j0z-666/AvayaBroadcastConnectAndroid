package com.avaya.android.vantage.aaadevbroadcast.contacts;


import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;
import com.avaya.android.vantage.aaadevbroadcast.fragments.ContactDetailsFragment;

import java.util.List;

public class ContactsFragmentK155 extends ContactsFragment {

    @Override
    void UIChangesForDevice() {
        if (getActivity() != null) {
            ((BaseActivity) getActivity()).tabSelector.setImageResource(R.drawable.triangle_copy);
            ((BaseActivity) getActivity()).showingFirst = false;
        }
    }

    @Override
    void setFilterViewVisibility(int visibility) {
        if (isAdded())
            mFilterView.setVisibility(View.GONE);
    }

    @Override
    void setLayoutSearchVisibility() {
        setSearchVisibility(View.GONE);
    }

    @Override
    void setImeOptions(EditText searchEditText) {
        searchEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        searchEditText.setPrivateImeOptions("nm");
    }

    @Override
    void setFilterViewClickListener() {
        mFilterView.setVisibility(View.GONE);
    }

    @Override
    void setAddVisibility() {
        mAdd.setVisibility(View.INVISIBLE);
        mRecycleView.setAlphaBarEnabled(false);
    }

    @Override
    void setContactsBluetoothSyncLinearClickListener(View root) {
        mContactsBluetoothSyncLinear = root.findViewById(R.id.contacts_bluetooth_sync_linear);
        mContactsBluetoothSyncLinear.setOnClickListener(this);
    }

    @Override
    void setSyncContactViewVisibility(int visibility) {

    }

    @Override
    public void setSearchVisibility(int visibility) {
        if (isAdded()) {
            searchLayout.setVisibility(visibility);

            if (getActivity() != null && visibility == View.GONE) {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    List<Fragment> fragments = fragmentManager.getFragments();
                    if (fragments != null) {
                        for (Fragment fragment : fragments) {
                            if (fragment != null && fragment.isVisible() && fragment instanceof ContactDetailsFragment) {
                                if (getActivity() != null) {
                                    ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(true);
                                    return;
                                }
                            }
                        }
                    }
                }
                ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(false);
            }
        }
    }
}