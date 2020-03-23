package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.view.View;
import android.widget.ImageView;

import com.avaya.android.vantage.aaadevbroadcast.R;

public class ActiveCallFragmentK175 extends ActiveCallFragment {

    private ImageView mMoreButton = null;

    /**
     * Required empty constructor
     */
    public ActiveCallFragmentK175() {
        // Required empty public constructor
    }


    @Override
    void initView(View view){
        mMoreButton = view.findViewById(R.id.more);
        mMoreButton.setOnClickListener(new OnMoreButtonClickListener());
    }


    @Override
    void setMoreButtonVisibility(int visibility){
        Common.setMoreButtonVisibility(visibility, mMoreButton);
    }


    @Override
    void setMoreButtonEnabled(boolean isEnabled) {
        Common.setMoreButtonEnabled(isEnabled, mMoreButton);
    }

}
