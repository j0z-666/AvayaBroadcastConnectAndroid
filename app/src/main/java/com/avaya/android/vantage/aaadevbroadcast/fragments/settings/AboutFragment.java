package com.avaya.android.vantage.aaadevbroadcast.fragments.settings;


import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.avaya.android.vantage.aaadevbroadcast.BuildConfig;
import com.avaya.android.vantage.aaadevbroadcast.R;

/**
 * Showing basic software information.
 */
public class AboutFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.user_about);
        findPreference("version").setSummary(BuildConfig.AVAYA_VERSION);
        findPreference("build").setSummary(getBuildNumber());
        findPreference("build_date").setSummary(BuildConfig.buildTime.toString());
        findPreference("csdk_version").setSummary(BuildConfig.CSDK_VERSION);
    }

    /**
     * Digits after the last "." in VersionName
     */
    private String getBuildNumber() {
        return BuildConfig.AVAYA_BUILD_NUMBER;
    }
}
