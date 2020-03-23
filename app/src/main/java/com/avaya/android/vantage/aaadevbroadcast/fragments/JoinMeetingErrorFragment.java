package com.avaya.android.vantage.aaadevbroadcast.fragments;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.R;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


public class JoinMeetingErrorFragment extends DialogFragment {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private TextView mErrorText, mErrorTitleText;
    private String mErrorString=null;
    private String mErrorTitleString=null;;

    static JoinMeetingErrorFragment newInstance() {
        return new JoinMeetingErrorFragment();
    }


    public JoinMeetingErrorFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=  inflater.inflate(R.layout.fragment_join_meeting_error, container, false);

        mErrorText = view.findViewById(R.id.error_text);
        mErrorTitleText = view.findViewById(R.id.error_title);
        if (mErrorString != null)
            mErrorText.setText(mErrorString);

        if (mErrorTitleString != null)
            mErrorTitleText.setText(mErrorTitleString);

        final TextView dismiss = view.findViewById(R.id.dismiss);
        dismiss.setOnClickListener(v -> dismiss());

        return view;
    }

    public void setErrorText(String errorText) {
        mErrorString = errorText;

        if (mErrorText != null)
            this.mErrorText.setText(errorText);
    }

    public void setErrorTitleText(String errorTitle) {
        mErrorTitleString = errorTitle;

        if (mErrorTitleText != null)
            this.mErrorTitleText.setText(errorTitle);
    }


    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag).addToBackStack(null);
            ft.commitAllowingStateLoss();
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "IllegalStateException: exception", e);
        }

    }




}
