package com.avaya.android.vantage.aaadevbroadcast.views;


import android.view.View;

public class SlideAnimationK155 extends SlideAnimation{


    @Override
    public void collapse(final View view){
        if (view != null)
            view.setVisibility(View.INVISIBLE);
    }
}
