package com.avaya.android.vantage.aaadevbroadcast.tutorial;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TutorialPagerAdapter extends PagerAdapter {

    private static final String TAG = TutorialPagerAdapter.class.getSimpleName();

    private final AppCompatActivity context;

    public TutorialPagerAdapter(AppCompatActivity context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return Screens.values().length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view,@NonNull Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Screens screeModel = Screens.values()[position];
        View screen = LayoutInflater.from(context).inflate(screeModel.layoutRes, container, false);
        container.addView(screen);

        Log.d(TAG, "instantiateItem() [position: " + position + "]");
        return screen;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        Log.d(TAG, "destroyItem() [position: " + position + "]");
    }
}