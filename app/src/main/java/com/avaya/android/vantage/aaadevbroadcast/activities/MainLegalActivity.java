package com.avaya.android.vantage.aaadevbroadcast.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.GoogleAnalyticsUtils;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

/**
 * EULA Screen. Displayed when the app starts for the first time or if picked from Settings.
 * Activity should be shown only once per user login application launch.
 */
public class MainLegalActivity extends AppCompatActivity {

    private static final String TAG = MainLegalActivity.class.getSimpleName();

    private static final String EULA_URL = "file:///android_asset/EULA_V2.htm";
    private static final String EULA_FILE_NAME = "EULA_SEP_2018.htm";

    private static final String KEY_EULA_ACCEPTED = "eula_accepted";
    private static final String EULA_PREFS_NAME = "eula_preferences";

    private static final String ACTION_REQUEST_LOGOUT = "com.avaya.endpoint.action.REQUEST_LOGOUT";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.legal_webview_layout);
        //hideSystemUI();
        boolean startedFromSettings = getIntent().getBooleanExtra("startFromSettings", false);
        // if application is download from Google Play don't create shortcut, it will be created automatically.
        // ELAN-705 - if application is installed by push application Shortcut will be installed by push manager (install via adb will not create shortcut)
        /*if(!DummyContent.isStoreVersion(this)) {
            addShortcutIcon();
        }*/
        setupUI(startedFromSettings);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return true;
    }

    /**
     * Load Webview with EULA.
     *
     * @param startedFromSettings show accept/decline buttons to user.
     */
    private void setupUI(boolean startedFromSettings) {

        LinearLayout containerLayout = findViewById(R.id.eula_container);

        if (startedFromSettings) {
            showToolbar();
            LinearLayout mButtonContainerLayout = findViewById(R.id.button_container);
            if (mButtonContainerLayout != null) {
                mButtonContainerLayout.setVisibility(View.GONE);
            }
            if (containerLayout != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) containerLayout.getLayoutParams();
                params.bottomMargin = 0;
                containerLayout.setLayoutParams(params);
            }
        } else {
            Button mAcceptLegalButton = findViewById(R.id.accept_legal);
            if (mAcceptLegalButton != null) {
                mAcceptLegalButton.setOnClickListener(view -> {
                    SharedPreferences preferences = getSharedPreferences(EULA_PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(KEY_EULA_ACCEPTED, true);
                    editor.apply();
                    startMainActivity();
                    GoogleAnalyticsUtils.logEvent(Utils.isK155() ? GoogleAnalyticsUtils.Event.K155_EULA_ACCEPT_EVENT : GoogleAnalyticsUtils.Event.K175_EULA_ACCEPT_EVENT);
                });
            }
            Button mDeclineLegalButton = findViewById(R.id.decline_legal);
            if (mDeclineLegalButton != null) {
                mDeclineLegalButton.setOnClickListener(view -> showLogoutDialog());
            }
        }

        TextView eulaTextView = new TextView(this);
        eulaTextView.setBackgroundColor(Color.TRANSPARENT);
        eulaTextView.setTextColor(Color.BLACK);
        eulaTextView.setMovementMethod(LinkMovementMethod.getInstance());
        eulaTextView.setPadding(10, 0, 10, 0);

        ScrollView eulaScrollView = new ScrollView(this);
        eulaScrollView.addView(eulaTextView);

        if (containerLayout != null) {
            containerLayout.addView(eulaScrollView);
            new LoadEULA(getEulaText(), eulaTextView).execute();
        }
    }

    private static class LoadEULA extends AsyncTask<Void, Void, Spanned> {
        private final StringBuilder mEulaStringBuilder;
        private final WeakReference<TextView> eulaTextViewRef;

        private LoadEULA(StringBuilder mEulaStringBuilder, TextView eulaTextView) {
            this.mEulaStringBuilder = mEulaStringBuilder;
            this.eulaTextViewRef = new WeakReference<>(eulaTextView);
        }

        @Override
        protected Spanned doInBackground(Void... voids) {
            return Html.fromHtml(mEulaStringBuilder.toString(), Html.FROM_HTML_MODE_COMPACT);
        }

        @Override
        protected void onPostExecute(Spanned eula) {
            super.onPostExecute(eula);
            if (eulaTextViewRef.get() != null)
                eulaTextViewRef.get().setText(eula);
        }
    }

    private StringBuilder getEulaText() {
        BufferedReader reader = null;
        StringBuilder text = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open(EULA_FILE_NAME)));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                text.append(mLine);
                text.append('\n');
            }
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
        return text;
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getApplicationContext().getString(R.string.logout));
        builder.setMessage(getApplicationContext().getString(R.string.logout_msg));
        // Set up the buttons
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            sendBroadcast(new Intent(ACTION_REQUEST_LOGOUT));
            finish();
        });
        builder.show();
    }


    /**
     * This method makes icon shortcut on homescreen
     * It will be call only when mUser first time run application, after he accept terms of use.
     * After application installation it will be visible on home screen and in application list.
     */
    private void addShortcutIcon() {
        Intent shortcutIntent = new Intent(getApplicationContext(),
                SplashActivity.class);

        shortcutIntent.setAction(Intent.ACTION_MAIN);
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.mipmap.ic_launcher));

        removeShortcut();

        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);

    }

    /**
     * Remove Home screen shortcut
     */
    private void removeShortcut() {
        //Deleting shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(getApplicationContext(),
                SplashActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.mipmap.ic_launcher));

        addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);
    }

    /**
     * Show toolbar with action bar on top of the screen
     */
    private void showToolbar() {

        // setting up toolbar
        Toolbar prefToolbar = findViewById(R.id.eula_toolbar);
        if (prefToolbar != null) {
            prefToolbar.setVisibility(View.VISIBLE);
        }
        setSupportActionBar(prefToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    /**
     * Lunch {@link MainActivity} on user agreement with EULA
     */
    private void startMainActivity() {
        final Intent intent = new Intent(this, ElanApplication.getDeviceFactory().getMainActivityClass());
        startActivity(intent);
        finish();
    }

    /**
     * Set window to immersive mode.
     */
    private void hideSystemUI() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}