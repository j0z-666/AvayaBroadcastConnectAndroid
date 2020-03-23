package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.view.View;
import android.widget.RelativeLayout;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;

import java.util.Objects;

import static com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity.ACTIVE_CALL_FRAGMENT;
import static com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity.ACTIVE_VIDEO_CALL_FRAGMENT;

public class ContactDetailsFragmentK155 extends ContactDetailsFragment {

    @Override
    void changeUIForDevice() {
        if (getActivity() != null) {
            ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(true);
            ((BaseActivity) getActivity()).showCallStatusAfterSearch();
        }
    }

    @Override
    void configureParametersForDevice(View root) {
        nameInfo = root.findViewById(R.id.name_info);
        openCloseNameInfo = root.findViewById(R.id.open_close_name_info);
        openCloseNameInfo.setImageResource(R.drawable.ic_expand_more);
        openCloseNameInfo.setOnClickListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (
                (!((BaseActivity) Objects.requireNonNull(getActivity())).isFragmentVisible(ACTIVE_CALL_FRAGMENT)
                        || ((BaseActivity) getActivity()).isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT))
                        && !isBackORDeletePressed) {
            //  ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(true);
        } else {
            ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(false);
        }
    }


    @Override
    void setContactDeleteVisibility() {
        mContactDelete.setVisibility(View.GONE);
    }

    @Override
    void setLayoutParamsFOrContactEdit() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mContactEdit.getLayoutParams();
        params.setMargins(0, 0, 200, 0); //substitute parameters for left, top, right, bottom
        mContactEdit.setLayoutParams(params);
    }

}
