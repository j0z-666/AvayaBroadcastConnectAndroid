package com.avaya.android.vantage.aaadevbroadcast.fragments.settings;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.text.format.Formatter;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.avaya.android.vantage.aaadevbroadcast.BuildConfig;
import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.SystemPropertiesProxy;

import java.io.File;
import java.util.Locale;

/**
 * Showing basic preference options
 */
public class SupportFragment extends PreferenceFragment {

    private SupportItemSelected mListener;

    /**
     * Interface used for callback
     */
    public interface SupportItemSelected {
        void fragmentSwitcher(String fragmentKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.user_support);

        PreferenceScreen rateScreen = this.getPreferenceScreen();
        Preference pref = new Preference(rateScreen.getContext());
        pref.setTitle(getString(R.string.rate_us));
        pref.setKey("rate");
        rateScreen.addPreference(pref);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (SupportFragment.SupportItemSelected) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SupportItemSelected");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        switch (preference.getKey()) {
            case "about": // starting about
                mListener.fragmentSwitcher(preference.getKey());
                break;
            case "tutorial": // starting Tutorial
                mListener.fragmentSwitcher(preference.getKey());
                break;
            case "legal": // starting legal
                mListener.fragmentSwitcher(preference.getKey());
                break;
            case "rate":
                startDialog(preference.toString());
                break;
            case "feedback":
                feedbackDialog(preference.toString());
                break;
            case "survey":
                mListener.fragmentSwitcher(preference.getKey());
                break;
            case "pin_app":

                break;
            default:
                // do nothing
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Display Rate us dialog
     *
     * @param preferenceName preference name to set as title
     */
    private void startDialog(final String preferenceName) {

        final Dialog dialog = new Dialog(getActivity());

        // making sure immersive mode stays on - part 1
        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        dialog.setContentView(R.layout.alert_rate_us);
        TextView mTitle = dialog.findViewById(R.id.textViewAlertTitle);
        Button mCancel = dialog.findViewById(R.id.buttonAlertCancel);
        Button mSubmit = dialog.findViewById(R.id.buttonAlertSubmit);

        mTitle.setText(preferenceName);

        mSubmit.setOnClickListener(v -> {
            String url = "https://play.google.com/store/apps/details?id=com.avaya.android.vantage.basic";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
            dialog.dismiss();
        });

        mCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        // making sure immersive mode stays on - part 3
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

    }

    /**
     * Display Feedback dialog
     *
     * @param preferenceName name of preference used to set the title
     */
    private void feedbackDialog(final String preferenceName) {
        if (isAdded() && (getContext() != null)) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
            alertDialogBuilder.setTitle(preferenceName);
            alertDialogBuilder
                    .setMessage(R.string.share_feedback_description)
                    .setPositiveButton(getResources().getString(R.string.ok), (dialog, id) -> shareFeedback())
                    .setNegativeButton(getResources().getString(R.string.cancel), (dialog, id) -> {
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    /**
     * Start Email activity with filled fields
     */
    private void shareFeedback() {

        final String emailSubject = getResources().getString(R.string.email_subject);
        Intent Email = new Intent(Intent.ACTION_SENDTO);
        Email.setType("message/rfc822");
        Email.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.email_to)});
        Email.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        Email.setData(Uri.parse("mailto:"));

        Email.putExtra(Intent.EXTRA_TEXT, getEmailBody());
        if (Email.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(Intent.createChooser(Email, getResources().getString(R.string.send_feedback)));
        }else{
            // If there are no email client installed in this device
            Toast.makeText(getContext(),R.string.support_no_email_client_message,Toast.LENGTH_SHORT).show();
        }


    }

    /**
     * @return a body of email
     */
    private String getEmailBody() {
        String emailBody = "";
        final String companyName = getResources().getString(R.string.email_company_name);
        final String description = getResources().getString(R.string.email_descrition);
        final String details = getResources().getString(R.string.email_details);

        String applicationVersion = getResources().getString(R.string.email_application_version) + " "
                + BuildConfig.VERSION_NAME + "."+ BuildConfig.AVAYA_BUILD_NUMBER +"\n";
        String locale = getResources().getString(R.string.email_locale) + " "
                + Locale.getDefault().toString() + "\n";
        String model = getResources().getString(R.string.email_model) + " " + SystemPropertiesProxy.get(Constants.AVAYA_PRODUCT_MODEL, Build.MODEL) + "\n";
        String os = getResources().getString(R.string.email_os) + " " + Build.VERSION.RELEASE + "\n";
        String deviceStorage = getResources().getString(R.string.email_device_storage)
                + " " + getFreeSpace() + "\n";
        String connection = getResources().getString(R.string.email_connection) + " " + getConnection() + "\n";
        String perms = getResources().getString(R.string.email_missing_permissions)
                + " " + getPermissions() + "\n";

        return emailBody.concat(companyName + description + details + applicationVersion + locale + model
                + os + deviceStorage + connection + perms);
    }

    /**
     * @return Free space on the card in GBs
     */
    private String getFreeSpace() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        @SuppressWarnings("deprecation") long blockSize = stat.getBlockSize();
        @SuppressWarnings("deprecation") long availableBlocks = stat.getAvailableBlocks();
        return Formatter.formatFileSize(getContext(), availableBlocks * blockSize);
    }

    /**
     * @return type of connection WI-FI or Ethernet
     */
    private String getConnection() {
        String type = "";
        ConnectivityManager mConnectivity = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // no connection, or background data disabled
        NetworkInfo info = mConnectivity.getActiveNetworkInfo();
        //noinspection deprecation
        if (info == null || !mConnectivity.getBackgroundDataSetting()) {
            type = getResources().getString(R.string.email_no_connection);
        } else {
            // WiFi or Ethernet connection
            int netType = info.getType();
            if (netType == ConnectivityManager.TYPE_WIFI) {
                type = getResources().getString(R.string.email_wifi);
            } else if (netType == ConnectivityManager.TYPE_ETHERNET) {
                type = getResources().getString(R.string.email_ethernet);
            }
        }
        return type;
    }

    /**
     * @return Permissions which been disabled
     */
    private String getPermissions() {
        StringBuilder missingPermissions = new StringBuilder();
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
        };

        for (String item : permissions) {
            int access = ContextCompat.checkSelfPermission(getContext(), item);
            if (access != PackageManager.PERMISSION_GRANTED) missingPermissions.append(item).append(" ");
        }
        return missingPermissions.toString();
    }
}

