package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.view.View;
import android.widget.ToggleButton;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;

public class ActiveCallFragmentK155 extends ActiveCallFragment {

    private ToggleButton mMoreButtonLand = null;

    /**
     * Required empty constructor
     */
    public ActiveCallFragmentK155() {
        // Required empty public constructor
    }

    @Override
    void initView(View view){
        mMoreButtonLand = view.findViewById(R.id.more);
        mMoreButtonLand.setOnClickListener( new OnMoreButtonClickListener());
        transducerButton = view.findViewById(R.id.transducer_button);
        offHook = view.findViewById(R.id.off_hook);
        Common.initView(view, (BaseActivity) getActivity(), mCallbackoffHookTransduceButtonInterface);
    }

    @Override
    void setOffhookButtonParameters(){
        Common.setOffhookButtonParameters(getActivity(), offHook);
    }

    @Override
    void changeUIOnTouch(){
        Common.changeUIOnTouch(getActivity());
    }


    @Override
    void changeUIForBackArrowClick(){
        Common.changeUIForBackArrowClick(getActivity());
    }


    @Override
    void setMoreButtonVisibility(int visibility){
        Common.setMoreButtonVisibility(visibility, mMoreButtonLand);
    }



    @Override
    void changeUIonConferenceListClicked(){
        Common.changeUIonConferenceListClicked(getActivity());
    }

    @Override
    void changeUIonTransferClicked(){
        Common.changeUIonTransferClicked(getActivity());
    }



    @Override
    void setMoreButtonEnabled(boolean isEnabled) {
        Common.setMoreButtonEnabled(isEnabled, mMoreButtonLand);
    }


    @Override
    public void cancelFullScreenMode(){
        Common.cancelFullScreenMode(getActivity());
    }



}
