package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import java.io.IOException;
import java.util.Objects;

/**
 * {@link ContactEditFragmentK175} is responsible for process of editing contact data.
 */
public class ContactEditFragmentK175 extends ContactEditFragment {


    @Override
    void rotateImageForDevice() throws IOException {
        rotateImage(mTempPhotoPath);
    }

    @Override
    void onLayoutChangeForDevice() {
        InputMethodManager imm = (InputMethodManager) Objects.requireNonNull(getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mTextToFocus, InputMethodManager.SHOW_IMPLICIT);
    }
}
