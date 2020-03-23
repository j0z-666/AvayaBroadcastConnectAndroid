package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.UICallViewAdaptor;
import com.avaya.android.vantage.aaadevbroadcast.model.UICall;


/**
 * {@link VideoCallFragment} responsible for showing video call surface and processing video call data
 */
abstract public class VideoCallFragment extends ActiveCallFragment {

    final String LOG_TAG = this.getClass().getSimpleName();
    private boolean mDeferInit = false;
    private ViewGroup mVideoViewGroup;
    ViewGroup mCallControls;
    ViewGroup mTopLayout;


    @Override
    public void init(UICall call, UICallViewAdaptor callViewAdaptor) {
        super.init(call, callViewAdaptor);
        if(mDeferInit) {
            Log.w(LOG_TAG, "init: performing deferred initialization");
            mCallViewAdaptor.initVideoCall(getActivity(), mCallId);
            mDeferInit = false;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (mCallViewAdaptor != null) {
            mCallViewAdaptor.initVideoCall(getActivity(), mCallId);
        }
        else {
            Log.e(LOG_TAG, "onCreateView: init was not called yet");
            mDeferInit = true;
        }

        // Inflate the layout for this fragment
        View view = super.onCreateView(inflater, container, savedInstanceState);
        assert view != null;
        mVideoViewGroup = view.findViewById(R.id.video_layout);
        mCallControls = view.findViewById(R.id.call_controls);
        ImageView contactImage = view.findViewById(R.id.contact_image);
        contactImage.setVisibility(View.GONE);

        makeUIChangesForDevice(view);
        return view;
    }

    abstract void makeUIChangesForDevice(View view);


    @Override
    public void onStart() {
        super.onStart();

        if (getView() != null) {
            if (mCallViewAdaptor == null) {
             new UICallViewAdaptor().startVideo(mVideoViewGroup, mCallId);
            }
            else {
                mCallViewAdaptor.startVideo(mVideoViewGroup, mCallId);
            }
        }

        changeVisibilityForVideoButtonsHandler();

    }

    void changeVisibilityForVideoButtonsHandler(){}


    @Override
    public void onStop() {
        super.onStop();
        if(mCallViewAdaptor == null) {
            new UICallViewAdaptor().stopVideo(mCallId);
        }
        else {
            mCallViewAdaptor.stopVideo(mCallId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(LOG_TAG, "onDestroyView()");

        if(mCallViewAdaptor == null) {
         new UICallViewAdaptor().onDestroyVideoView(mCallId);
        }
        else {
            mCallViewAdaptor.onDestroyVideoView(mCallId);
        }

        cancelFullScreenMode();
    }
}
