package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.avaya.android.vantage.aaadevbroadcast.R;


/**
 * {@link VideoCallFragmentK175} responsible for showing video call surface and processing video call data
 */
public class VideoCallFragmentK175 extends VideoCallFragment {

    private ImageView mMoreButton = null;

    public VideoCallFragmentK175() {
        // Required empty public constructor
    }


    void makeUIChangesForDevice(View view){
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, R.id.more);
        params.addRule(RelativeLayout.RIGHT_OF, R.id.back);
        params.setMargins(0, 24, 0, 0);
        mContactName.setLayoutParams(params);
        mContactName.setTextSize(20);
        mContactName.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        mContactName.setGravity(Gravity.CENTER_HORIZONTAL);
        mCallStateView.setTextSize(20);
        mCallStateView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    }


    @Override
    void setMoreButtonVisibility(int visibility){
        Common.setMoreButtonVisibility(visibility, mMoreButton);
    }


    @Override
    void setMoreButtonEnabled(boolean isEnabled) {
        Common.setMoreButtonEnabled(isEnabled, mMoreButton);
    }


    @Override
    void initView(View view){
        mMoreButton = view.findViewById(R.id.more);
        mMoreButton.setOnClickListener(new OnMoreButtonClickListener());
    }

}
