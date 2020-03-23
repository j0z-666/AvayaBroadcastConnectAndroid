package com.avaya.android.vantage.aaadevbroadcast;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import 	androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import com.google.android.material.snackbar.Snackbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;
import com.avaya.android.vantage.aaadevbroadcast.activities.CallDialerActivity;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.LocalContactInfo;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static android.os.Build.DEVICE;
import static android.os.Build.PRODUCT;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.MILISECONDS_IN_SECOND;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.UUID_PREFIX;

/**
 * Utility class used for providing contact information and sending Snackbar broadcast
 */

public class Utils {

    public static final boolean SNACKBAR_LONG = true;
    public static final boolean SNACKBAR_SHORT = false;
    public static final String CONTACT = "CONTACT";
    public static final String CALLHISTORY = "CALLHISTORY";
    public static final String ENABLED = "ENABLED";
    public static final String DISABLED = "DISABLED";
    public static final String PBAP_URL = "avaya.intent.action.MODIFY_PBAP_SETTINGS";
    public static final String SYNC_TYPE = "syncType";
    public static final String STATUS = "status";
    private static final String TAG = "AvayaUtils";
    private static final float FONT_SCALE_LARGE = (float)1.15;
    private static final int DEFAULT_DENSITY_DPI_K175 = 213;
    private static final int DEFAULT_DENSITY_DPI_K155 = 294;

    private static boolean sIsLandScape;

    public static WeakReference<CallDialerActivity> callDialerActivityRef;

    /**
     * Method that get display name
     * If call is CM conference - it just returns the display name
     * Otherwise, it searches the contact in contact list using number and returns contact name (if not empty)
     *
     * @param displayNumber      Phone number
     * @param displayName        Phone display name
     * @param isCMConferenceCall boolean that displays if this is a conference call
     * @return Display name
     */
    public static String getContactName(String displayNumber, String displayName, boolean isCMConferenceCall, boolean isConference) {

        String contactName;
        if (TextUtils.isEmpty(displayName)) {
            displayName = displayNumber;
        }

        if (isCMConferenceCall) {
            contactName = displayName;
        } else {
            if (isConference && TextUtils.isEmpty(displayName) ) {
                // this is Local Conference
                contactName = Objects.requireNonNull(ElanApplication.getContext()).getString(R.string.conference) + " 2";
            } else {
                // this line of code will get display name by phone number
                String resolvedDisplayName = getFirstContact(displayNumber);
                //if contact doesn't exist in contact list, use display name
                contactName = TextUtils.equals(displayNumber, resolvedDisplayName) ? displayName : resolvedDisplayName;
            }
        }
        return contactName;
    }

    /**
     * Method for finding contact name for calling number
     *
     * @param number Phone number
     * @return Contact name if it exist in contact list or phone number
     */
    private static String getFirstContact(String number) {
        String[] searchResults = LocalContactInfo.phoneNumberSearch(number);

        if (searchResults != null && searchResults[0].trim().length() > 0 && searchResults[1].trim().length() > 0) {
            if (searchResults[1].equalsIgnoreCase(number)) {
                return searchResults[0];
            }
        }
        return number;
    }

    /**
     * Method for finding finding contact photo URI
     *
     * @param number Phone number
     * @return Photo URI if exists
     */
    public static String getPhotoURI(String number) {
        String[] searchResults = LocalContactInfo.phoneNumberSearch(number);

        String photoURI = null;
        if (searchResults != null && searchResults.length > 2 &&
                searchResults[3] != null && searchResults[3].trim().length() > 0) {
            photoURI = searchResults[3];
        }
        return photoURI;
    }


    /**
     * Sending local broadcast with data for Snackbar to be shown.
     * Broadcast is captured in {@link com.avaya.android.vantage.aaadevbroadcast.activities.MainActivity}
     *
     * @param context Activity context
     * @param message String to be shown in Snackbar
     * @param length  boolean length of {@link Snackbar}
     *                false Snackbar.LENGTH_SHORT
     *                true Snackbar.LENGTH_LONG
     */
    public static void sendSnackBarData(Context context, String message, boolean length) {
        Intent snackBarShow = new Intent(Constants.SNACKBAR_SHOW);
        snackBarShow.putExtra(Constants.SNACKBAR_MESSAGE, message);
        snackBarShow.putExtra(Constants.SNACKBAR_LENGTH, length);
        LocalBroadcastManager.getInstance(context).sendBroadcast(snackBarShow);
    }

    /**
     * Sending normal broadcast with data for number of missed calls
     * Broadcast is captured in {@link com.avaya.android.vantage.aaadevbroadcast.receiver.ConfigReceiver}
     *
     * @param context Activity context
     * @param numberOfMissedCalls Number of missed calls
     */
    public static void refreshHistoryIcon(Context context, int numberOfMissedCalls) {
        Intent refreshHistoryIcon = new Intent(Constants.REFRESH_HISTORY_ICON);
        refreshHistoryIcon.putExtra(Constants.EXTRA_UNSEEN_CALLS, numberOfMissedCalls);
        LocalBroadcastManager.getInstance(context).sendBroadcast(refreshHistoryIcon);
    }

    public static void sendSnackBarDataWithDelay(final Context context, final String message, final boolean length) {
        new Handler().postDelayed(() -> sendSnackBarData(context, message, length), MILISECONDS_IN_SECOND);
    }

    /**
     * Performing check if bitmap is provided by CSDK or we have just chosen image from gallery application
     * In case we are taking bitmap from gallery we have to perform check for rotation before we save such
     * bitmap as it can be incorrectly rotated.
     *
     * @param bitmap   Which have to be saved
     * @param resolver {@link ContentResolver}
     * @param uri      {@link Uri} of bitmap used
     * @return Bitmap
     */
    public static Bitmap checkAndPerformBitmapRotation(Bitmap bitmap, ContentResolver resolver, Uri uri) {
        if (!Utils.getRealPathFromURI(resolver, uri).isEmpty()) {
            try {
                File imageFile = new File(Utils.getRealPathFromURI(resolver, uri));
                ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                bitmap = Utils.prepareRotation(bitmap, orientation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /**
     * Perform check if Uri provided belong to some of images already saved on device and returning
     * physical patch to that image. In case that we can't find physical patch we are returning
     * empty string
     *
     * @param resolver {@link ContentResolver}
     * @param uri      {@link Uri} for requested image
     * @return String with physical patch to image or in case image is provided by CSDK empty string.
     */
    private static String getRealPathFromURI(ContentResolver resolver, Uri uri) {
        String path = "";
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                path = cursor.getString(idx);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * Check in which direction image have to be rotated adn calling image rotating methods with
     * adequate parameters
     *
     * @param source      Bitmap to be rotated
     * @param orientation int representing current rotation of image
     * @return Bitmap which is properly rotated or in case of failure return non changed image
     */
    private static Bitmap prepareRotation(Bitmap source, int orientation) {

        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                source = rotateImage(source, 90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                source = rotateImage(source, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                source = rotateImage(source, 270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:

            default:
                break;
        }
        return source;
    }

    /**
     * Rotatin provided bitmap for provided angle
     *
     * @param source Bitmap to be rotated
     * @param angle  float for which provided bitmap have to be rotated
     * @return prepared and rotated bitmap
     */
    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    /**
     * This function was provided by CSDK group. It tests whether word CONFERENCE appears in
     * in the provided string taken into account localization aspects of CM conference.
     * Note that the strings in this function can not be resolved via the device's localization
     * mechanism since CM's localization is not synchronized with the device's localization.
     *
     * @param sRemoteAddress Remote address of the call
     * @return prepared and rotated bitmap
     */
    public static boolean containsConferenceString(String sRemoteAddress) {

        if (sRemoteAddress == null || sRemoteAddress.isEmpty()) return false;

        // english or french
        if (sRemoteAddress.contains("CONFERENCE")) {
            return true;
        }

        // italian
        if (sRemoteAddress.contains("CONFERENZA")) {
            return true;
        }

        // spanish
        if (sRemoteAddress.contains("CONFERENCIA")) {
            return true;
        }

        // german
        if (sRemoteAddress.contains("Konferenz")) {
            return true;
        }

        // russian
        if ((sRemoteAddress.contains("Конференция")) || (sRemoteAddress.contains("КОНФЕРЕНЦИЯ"))) {
            return true;
        }

        // dutch
        if (sRemoteAddress.contains("CONFERENTIE")) {
            return true;
        }

        // portuguese
        if (sRemoteAddress.contains("CONFERENCIA")) {
            return true;
        }

        // korean
        if (sRemoteAddress.contains("회의")) {
            return true;
        }

        // japanese
        if (sRemoteAddress.contains("会議")) {
            return true;
        }

        // chinese
        return sRemoteAddress.contains("会议");

    }

    /**
     * @return true if a camera is available
     */
    public static boolean isCameraSupported() {
        return SDKManager.getInstance().isCameraSupported();
    }

    /**
     * Method that returns boolean value if camera is supported or not.
     *
     * @return true if at least one camera is supported on the device, otherwise false
     */
    public static boolean getDeviceCameraSupport() {
        if (ElanApplication.getContext() != null) {
            CameraManager cameraManager = ElanApplication.getContext().getSystemService(CameraManager.class);
            try {
                String[] cameraIdList = cameraManager.getCameraIdList();
                //return (cameraIdList != null && cameraIdList.length > 0);
                for (String aCameraIdList : cameraIdList) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(aCameraIdList);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT || facing == CameraCharacteristics.LENS_FACING_BACK)
                        return true;
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Camera Access Exception.", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed getting camera characteristics. ", e);
            }
        }
        return false;
    }

    public static void wakeDevice(Context context) {
        Log.d(TAG, "force waking device");
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, BuildConfig.APPLICATION_ID + ":" + TAG);
        synchronized (context) {
            if (wakeLock.isHeld())
                wakeLock.release();
            wakeLock.acquire(10*60*1000L /*10 minutes*/);
            wakeLock.release();
        }
    }

    /**
     * enum that defines that states of the contacts or call logs syncing with the
     * paired device.
     */
    public enum SyncState {
        NOT_PAIRED("not paired"), SYNC_OFF("sync off"), SYNC_ON("sync on");

        private final String mStateName;

        SyncState(String stateName) {
            mStateName = stateName;
        }

        public String getStateName() {
            return mStateName;
        }
    }

    /**
     * @param context Activity context
     * @return true if at least one BT device is connected.
     */
    public static boolean havePairedDevice(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return false;
        } else {
            // bluetooth is off
            if(!bluetoothAdapter.isEnabled() && bluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) != BluetoothHeadset.STATE_CONNECTED) {
                return false;
            } else {
                // Get bounded devices from BT adapter
               Set<BluetoothDevice> boundedDevices = bluetoothAdapter.getBondedDevices();
                if (boundedDevices.isEmpty()) {
                    return false;
                }
                boundedDevices.size();
                return true;
            }
        }
    }


    public static boolean hasAnyConnectedBluetoothDevice(final int profile){
        if(profile != BluetoothHeadset.HEADSET && profile != BluetoothHeadset.HEALTH && profile != BluetoothHeadset.A2DP){
            throw new IllegalArgumentException("unacceptable value for the Bluetooth profile: " + profile);
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothAdapter.getProfileConnectionState(profile) ==  BluetoothHeadset.STATE_CONNECTED;
    }


    /**
     * Hides Soft keyboard from the screen
     * @param activity reference to the Activity
     */
    public static void hideKeyboard(Activity activity){
        if (activity == null) return;
        View focus = activity.getCurrentFocus();
        if (focus != null) {
            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            ((BaseActivity)activity).backToFullScreen();
        }
    }

    public static void openKeyboard(Activity activity){
        if (activity == null) return;
        View focus = activity.getCurrentFocus();
        if (focus != null) {
            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(focus,0);
        }
    }
    /**
     * Creates UUID object from the MAC address of the devices ethernet interface.
     * @return {@link UUID} object
     */
    public static UUID uniqueDeviceUUID() {

        String mac = getMac();

        try {
            UUID uuid = UUID.fromString(UUID_PREFIX + mac);
            if(uuid!=null && uuid.toString().endsWith("-"))
                return uuid;
            else
                return UUID.randomUUID();
        }
        catch (Exception e) {
            Log.d(TAG, "Could not build UUID basing on MAC due to " + e.getMessage() + " Returning random.");
            return UUID.randomUUID();
        }
    }

    /**
     * @return String: MAC address of the devices ethernet interface.
     */
    private static String getMac(){

        String mac = SystemPropertiesProxy.get(Constants.AVAYA_ETHADDR, null);
        if (mac == null) {
            Log.e(TAG, "Could not get MAC");
            return null;
        }
        Log.d(TAG, "MAC="+mac);
        mac = mac.replace(":", "");

        return mac;
    }

    /**
     * @param context Activity context
     * @param firstName String: first name
     * @param lastName String: last name
     * @return String: last and first name connected based on the configured preferences.
     */
    public static String combinedName(Context context, String firstName, String lastName) {
        return String.format(context.getResources().getConfiguration().locale, "%s %s", firstName, lastName).trim();
    }

    /**
     * Helper method for providing proper date string in format Today, Yesterday or exact date
     *
     * @param dateString String representation of date
     * @return String representing value required
     */
    public static String getSimpleDateString(String dateString) {
        String result = "";
        SimpleDateFormat from = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzzzzzzz yyyy", Locale.ENGLISH);

        Date date;

        try {
            date = from.parse(dateString);
            long now = System.currentTimeMillis();
            result = DateUtils.getRelativeTimeSpanString(date.getTime(), now, DateUtils.DAY_IN_MILLIS).toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Helper method for providing proper time string in format hour:minute AM/PM
     *
     * @param dateString String representation of date
     * @return String representing value required as time
     */
    public static String getTimeString(Context context, String dateString) {
        String result = "";
        SimpleDateFormat fromDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzzzzzzz yyyy", Locale.ENGLISH);
        java.text.DateFormat toDate = android.text.format.DateFormat.getTimeFormat(context.getApplicationContext());
        Date dateNow;
        try {
            dateNow = fromDate.parse(dateString);
            result = toDate.format(dateNow);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    /** Sets the landscape flag indicator from the value from the resources
     * @param context Activity context
     */
    public static void setDeviceMode(Context context) {
        sIsLandScape = context.getResources().getBoolean(R.bool.is_landscape);
    }

    /**
     * @return true if the landscape mode is set.
     */
    public static boolean isLandScape() {
        return sIsLandScape;
    }

    public static boolean isK155() {
        return ((PRODUCT.contains("K155")) || (DEVICE.contains("K155")));
    }

    public static boolean isModifyContactsEnabled() {
        String isEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ENABLE_MODIFY_CONTACTS);
        return isEnabled == null || !isEnabled.equals("0");
    }

    public static IDeviceFactory getDeviceFactory(){
        if (isK155()){
            Log.d(TAG, "Return DeviceFactoryK155");
            return new DeviceFactoryK155();
        }
        Log.d(TAG, "Return DeviceFactoryK175");
        return  new DeviceFactoryK175();
    }

//    public static Class getMainActivityClass(){
//        if (isK155()){
//            Log.d(TAG, "Return MainActivityK155.class");
//            return MainActivityK155.class;
//        }
//        Log.d(TAG, "Return MainActivityK175.class");
//        return MainActivityK175.class;
//    }

//    public static ActiveCallFragment getActiveCallFragment(){
//        if (isK155()){
//            Log.d(TAG, "Return ActiveCallFragmentK155");
//            return new ActiveCallFragmentK155();
//        }
//        Log.d(TAG, "Return ActiveCallFragmentK175");
//        return  new ActiveCallFragmentK175();
//    }

    /**
     * We added custom merge sort due to bug in Java source code
     * in Timb sort which violates transitive contract
     *
     * @param m list to be sorted
     * @param <E> return sorted list
     * @return
     */
    public static <E extends Comparable<? super E>> List<E> mergeSort(List<E> m) {
        if (m.size() <= 1) return m;

        int middle = m.size() / 2;
        List<E> left = m.subList(0, middle);
        List<E> right = m.subList(middle, m.size());

        right = mergeSort(right);
        left = mergeSort(left);

        return merge(left, right);
    }

    private static <E extends Comparable<? super E>> List<E> merge(List<E> left, List<E> right) {
        List<E> result = new ArrayList<>();
        Iterator<E> it1 = left.iterator();
        Iterator<E> it2 = right.iterator();

        E x = it1.next();
        E y = it2.next();
        while (true) {
            //change the direction of this comparison to change the direction of the sort
            if (x.compareTo(y) >= 0) {
                result.add(x);
                if (it1.hasNext()) {
                    x = it1.next();
                } else {
                    result.add(y);
                    while (it2.hasNext()) {
                        result.add(it2.next());
                    }
                    break;
                }
            } else {
                result.add(y);
                if (it2.hasNext()) {
                    y = it2.next();
                } else {
                    result.add(x);
                    while (it1.hasNext()) {
                        result.add(it1.next());
                    }
                    break;
                }
            }
        }
        return result;
    }

    public static void overrideFontScaleAndDensityK175(Activity activity){
        final Configuration configuration = new Configuration();

        if (configuration.fontScale != FONT_SCALE_LARGE || configuration.densityDpi != DEFAULT_DENSITY_DPI_K175){

            configuration.fontScale = FONT_SCALE_LARGE;
            configuration.densityDpi = DEFAULT_DENSITY_DPI_K175;

            activity.applyOverrideConfiguration(configuration);
        }
    }

    public static void overrideFontScaleAndDensityK155(Activity activity){
        final Configuration configuration = new Configuration();

        if (configuration.fontScale != FONT_SCALE_LARGE || configuration.densityDpi != DEFAULT_DENSITY_DPI_K155){

            configuration.fontScale = FONT_SCALE_LARGE;
            configuration.densityDpi = DEFAULT_DENSITY_DPI_K155;

            activity.applyOverrideConfiguration(configuration);
        }
    }
}
