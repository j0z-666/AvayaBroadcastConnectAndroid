package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.os.Handler;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;


/**
 * {@link VideoCallFragmentK155} responsible for showing video call surface and processing video call data
 */
public class VideoCallFragmentK155 extends VideoCallFragment {

    private final Slide mSlideBottom;
    private final Slide mSlideTop;
    private boolean inTransition=false;
    private ToggleButton mMoreButtonLand = null;

    public VideoCallFragmentK155() {
        // Required empty public constructor
        mSlideTop = new Slide();
        mSlideBottom = new Slide();
    }


    void makeUIChangesForDevice(View view){
        mTopLayout = view.findViewById(R.id.top_layout);
        view.findViewById(R.id.contact_name).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.call_state).setVisibility(View.INVISIBLE);
        TextView contactName = view.findViewById(R.id.video_contact_name);
        contactName.setVisibility(View.VISIBLE);
        contactName.setText(mContactName.getText());
        mContactName = contactName;
        TextView callStateView = view.findViewById(R.id.video_call_state);
        callStateView.setText(mCallStateView.getText());
        callStateView.setVisibility(View.VISIBLE);
        mCallStateView = callStateView;
        ViewGroup audioViewGroup = view.findViewById(R.id.audio_layout);
        audioViewGroup.setOnTouchListener((view1, event) -> {
            int visibility = mCallControls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            changeVisibilityForVideoButtons(visibility);

            if (event.getAction() == MotionEvent.ACTION_UP) {
                view1.performClick();
                return true;
            }
            return false;
        });

        RelativeLayout.LayoutParams paramsactiveCallRoot = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        if(getActivity() !=null) {
            ((BaseActivity) getActivity()).changeUiForFullScreenInLandscape(true);
            paramsactiveCallRoot.height = 700;
            activeCallRoot.setLayoutParams(paramsactiveCallRoot);
        }
    }

    private void changeVisibilityForVideoButtons(int visibility){
        mContactName.setVisibility(visibility);
        mCallStateView.setVisibility(visibility);

        addSlideAnimation(visibility);
    }



    @Override
    void changeVisibilityForVideoButtonsHandler(){
        new Handler().postDelayed(() -> changeVisibilityForVideoButtons(View.INVISIBLE), 4000);
    }

    /**
     * Starts slide animation for mCallControls and mTopLayout
     * @param visibility View visibility
     */
    private void addSlideAnimation(int visibility){
        Log.d(LOG_TAG, "addSlideAnimation visibility=" + visibility);

        if (inTransition){
            Log.d(LOG_TAG, "Transition not finished. Aborting");
            return;
        }
        try {
            if (isAdded()) {

                //slide.setDuration(300);
                mSlideBottom.addListener(new Transition.TransitionListener() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        inTransition=true;
                        Log.d(LOG_TAG, "onTransitionStart");
                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        inTransition=false;
                        Log.d(LOG_TAG, "onTransitionEnd");
                    }

                    @Override
                    public void onTransitionCancel(Transition transition) {

                    }

                    @Override
                    public void onTransitionPause(Transition transition) {

                    }

                    @Override
                    public void onTransitionResume(Transition transition) {

                    }
                });

                mSlideTop.setSlideEdge(Gravity.TOP);
                TransitionManager.beginDelayedTransition(mTopLayout, mSlideTop);
                mSlideBottom.setSlideEdge(Gravity.BOTTOM);
                TransitionManager.beginDelayedTransition(mCallControls, mSlideBottom);
                mCallControls.setVisibility(visibility);
                mTopLayout.setVisibility(visibility);
            }
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
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
    public void onDestroyView() {
        TransitionManager.endTransitions(mCallControls);
        super.onDestroyView();
    }
}
