package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.avaya.android.vantage.aaadevbroadcast.R;

import androidx.fragment.app.DialogFragment;


public class SpinnerFragment extends DialogFragment {

    public static final String TAG = SpinnerFragment.class.getSimpleName();
    private TextView mCancel;

    public static SpinnerFragment newInstance() {
        return new SpinnerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_spinner, container, false);

        return view;
    }


    @Override
    public void onDestroyView() {
        if ((getDialog() != null) && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

}
