package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.PhotoLoadUtility;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.LocalContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.csdk.LocalContactInfo;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.model.DirectoryData;
import com.avaya.android.vantage.aaadevbroadcast.model.EditablePhoneNumber;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimation;
import com.avaya.android.vantage.aaadevbroadcast.views.adapters.ContactEditPhoneListAdapter;
import com.avaya.clientservices.contact.EditableContact;
import com.avaya.clientservices.contact.fields.ContactPhoneNumberType;
import com.avaya.clientservices.contact.fields.EditableContactBoolField;
import com.avaya.clientservices.contact.fields.EditableContactStringField;
import com.avaya.clientservices.user.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.FIRST_NAME_FIRST;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.IPO_CONTACT_NAME_LIMIT;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.LAST_NAME_FIRST;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.NAME_DISPLAY_PREFERENCE;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.NEW_CONTACT_PREF;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.USER_PREFERENCE;
import static com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment.isFirstNameFirst;

/**
 * {@link ContactEditFragment} is responsible for process of editing contact data.
 */
abstract public class ContactEditFragment extends Fragment implements ContactEditPhoneListAdapter.OnContactEditPhoneChangeListener, View.OnClickListener {

    private static final String LOG_TAG = "ContactEditFragment";

    private static final int REQUEST_CAMERA_PHOTO = 501;
    private static final int PICK_PHOTO = 502;
    private static final String TAG = "ContactEditFragment";

    private static final int LOCAL_CONTACT = 0;
    private static final int ENTERPRISE_CONTACT = 1;
    private static final int BROADSOFT_CONTACT = 3;

    private static final String LOCAL_CONTACT_PREF = "0";
    private static final String ENTERPRISE_CONTACT_PREF = "1";
    private final Map<ContactPhoneNumberType, Pair<Integer, Integer>> mEnterprisePhoneTypesMap = new HashMap<ContactPhoneNumberType, Pair<Integer, Integer>>() {{
        put(ContactPhoneNumberType.WORK, new Pair<>(R.id.type_work, R.string.contact_details_work));
        put(ContactPhoneNumberType.HOME, new Pair<>(R.id.type_home, R.string.contact_details_home));
        put(ContactPhoneNumberType.MOBILE, new Pair<>(R.id.type_mobile, R.string.contact_details_mobile));
        put(ContactPhoneNumberType.FAX, new Pair<>(R.id.type_fax, R.string.contact_details_fax));
        put(ContactPhoneNumberType.PAGER, new Pair<>(R.id.type_pager, R.string.contact_details_pager));
        put(ContactPhoneNumberType.OTHER, new Pair<>(R.id.type_other, R.string.contact_details_other));
    }};
    private final Map<ContactPhoneNumberType, ContactData.PhoneType> mEnterprisePhoneToLocalMap = new HashMap<ContactPhoneNumberType, ContactData.PhoneType>() {{
        put(ContactPhoneNumberType.WORK, ContactData.PhoneType.WORK);
        put(ContactPhoneNumberType.HOME, ContactData.PhoneType.HOME);
        put(ContactPhoneNumberType.MOBILE, ContactData.PhoneType.MOBILE);
        put(ContactPhoneNumberType.FAX, ContactData.PhoneType.FAX);
        put(ContactPhoneNumberType.PAGER, ContactData.PhoneType.PAGER);
        put(ContactPhoneNumberType.OTHER, ContactData.PhoneType.OTHER);
    }};
    private final SlideAnimation menuSlide = ElanApplication.getDeviceFactory().getSlideAnimation();
    public AlertDialog alertDialog;
    String mTempPhotoPath;
    EditText mTextToFocus;
    private OnContactEditInteractionListener mEditListener;
    private ContactData mContactData;
    private EditableContact mEditableEnterpriseContact;
    private Uri mImageUri;
    private TextView mCancel, mDone, mContactImage, mEditTitle, mNewContactLocal, mNewContactEnterprise;
    private EditText mContactFirstName, mContactLastName, mContactJobTitle, mContactCompany, mContactAddress;
    private ImageView mFavoriteImage, mContactFirstNameClear, mContactLastNameClear, mContactJobTitleClear, mContactCompanyClear, mContactAddressClear;
    private ListView mContactPhoneList;
    private LinearLayout mAddContactNumber;
    private LinearLayout mCurrentPhoneTypeMenuHolder;
    private LinearLayout mPhoneTypeLocalMenuHolder;
    private LinearLayout mPhoneTypeEntepriseMenuHolder;
    private LinearLayout mContactTypeHolder;
    private FrameLayout contact_edit_holder;
    private ContactEditPhoneListAdapter mAdapter;
    private List<EditablePhoneNumber> mPhoneNumbers;
    private boolean mFavoriteSelected, isAddingNewContact, phoneNumberValid, disallowFavoriteStatusChange, saveContactClicked;
    private int currentPhoneListPosition = 0;
    //phone type selection textViews
    private TextView typeWork, typeMobile, typeHome, typeFax, typePager, typeOther;
    private SharedPreferences mUserPreference;
    private Handler mHandler;
    private Runnable mLayoutCloseRunnable;
    private int mContactType = 0;
    private String mAccountType = null;
    private String mAccountName = null;
    private Set<ContactPhoneNumberType> mSupportedPhoneNumberTypesTypes;

    public ContactEditFragment() {
        // Default constructor
    }

    public static ContactEditFragment newInstance(ContactData contactData, boolean isNewContact) {
        ContactEditFragment fragment = ElanApplication.getDeviceFactory().getContactEditFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.CONTACT_DATA, contactData);
        args.putBoolean(Constants.CONTACT_EDITING, isNewContact);
        fragment.setArguments(args);
        return fragment;
    }

    public static ContactEditFragment newInstance() {
        ContactEditFragment fragment = ElanApplication.getDeviceFactory().getContactEditFragment();
        Bundle args = new Bundle();
        args.putBoolean(Constants.CONTACT_EDITING, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        User user = SDKManager.getInstance().getContactsAdaptor().getUser();
        if (user != null) {
            mSupportedPhoneNumberTypesTypes = user.getContactService().getContactLimits().getSupportedPhoneNumberTypes();
        } else {
            // Fail safe for rare case when service does not return value
            mSupportedPhoneNumberTypesTypes = new HashSet<>();
            mSupportedPhoneNumberTypesTypes.add(ContactPhoneNumberType.WORK);
        }

        assert getArguments() != null;
        mContactData = getArguments().getParcelable(Constants.CONTACT_DATA);
        isAddingNewContact = getArguments().getBoolean(Constants.CONTACT_EDITING);
        // If first name is detected as phone number delete it otherwise leave it as is it passed
        if (isAddingNewContact && mContactData != null && Patterns.PHONE.matcher(mContactData.mFirstName).matches()) {
            mContactData.mFirstName = "";
        }
        mPhoneNumbers = new ArrayList<>();
        if (mContactData != null) {
            if (mContactData.mPhones != null && mContactData.mPhones.size() > 0 && mContactData.mPhones.get(0).Number != null) {
                if (mContactData.mCategory == ContactData.Category.ENTERPRISE) {
                    findEnterpriseEditableContact();
                }
                phoneNumberValid = true;
            }
            if (mContactData.mPhones != null) {
                for (int i = 0; i < mContactData.mPhones.size(); i++) {
                    mPhoneNumbers.add(new EditablePhoneNumber(mContactData.mPhones.get(i).Number,
                            mContactData.mPhones.get(i).Type, mContactData.mPhones.get(i).Primary,
                            mContactData.mPhones.get(i).phoneNumberId));
                }
            }
            mFavoriteSelected = mContactData.isFavorite();
        } else {
            mContactData = ContactData.getEmptyContactData();
        }

        setContactDataCategory();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contact_edit, container, false);
        mCancel = root.findViewById(R.id.contact_edit_cancel);
        mDone = root.findViewById(R.id.contact_edit_done);
        mEditTitle = root.findViewById(R.id.contact_edit_title);
        mContactPhoneList = root.findViewById(R.id.contact_edit_phone_list);
        contact_edit_holder = root.findViewById(R.id.contact_edit_holder);
        mUserPreference = getActivity().getSharedPreferences(USER_PREFERENCE, MODE_PRIVATE);
        mContactTypeHolder = root.findViewById(R.id.contact_type_holder);

        mPhoneTypeEntepriseMenuHolder = root.findViewById(R.id.phone_enterprise_type_menu_holder);

        for (ContactPhoneNumberType t : mSupportedPhoneNumberTypesTypes) {
            Pair<Integer, Integer> type = mEnterprisePhoneTypesMap.get(t);
            if (type != null) {
                TextView tv = (TextView) inflater.inflate(R.layout.menu_phone_type_entry_enteprise, container, false);
                tv.setId(type.first);
                tv.setText(Objects.requireNonNull(getContext()).getResources().getString(type.second));
                tv.setContentDescription(getContext().getResources().getString(type.second));
                mPhoneTypeEntepriseMenuHolder.addView(tv);
                tv.setOnClickListener(this);
            }
        }
        menuSlide.reDrawListener(mPhoneTypeEntepriseMenuHolder);

        mPhoneTypeLocalMenuHolder = root.findViewById(R.id.phone_type_menu_holder);
        typeWork = mPhoneTypeLocalMenuHolder.findViewById(R.id.type_work);
        typeMobile = mPhoneTypeLocalMenuHolder.findViewById(R.id.type_mobile);
        typeHome = mPhoneTypeLocalMenuHolder.findViewById(R.id.type_home);
        typeFax = mPhoneTypeLocalMenuHolder.findViewById(R.id.type_fax);
        typePager = mPhoneTypeLocalMenuHolder.findViewById(R.id.type_pager);
        typeOther = mPhoneTypeLocalMenuHolder.findViewById(R.id.type_other);
        menuSlide.reDrawListener(mPhoneTypeLocalMenuHolder);

        // new contact type
        mNewContactLocal = mContactTypeHolder.findViewById(R.id.contact_type_local);
        mNewContactEnterprise = mContactTypeHolder.findViewById(R.id.contact_type_enterprise);

        final View header = inflater.inflate(R.layout.contact_edit_header, mContactPhoneList, false);
        mContactFirstName = header.findViewById(R.id.contact_edit_first_name);
        mContactLastName = header.findViewById(R.id.contact_edit_last_name);
        mContactJobTitle = header.findViewById(R.id.contact_edit_job_title);
        mContactCompany = header.findViewById(R.id.contact_edit_company);
        mContactAddress = header.findViewById(R.id.contact_edit_address);
        mContactImage = header.findViewById(R.id.contact_edit_contact_image);
        mFavoriteImage = header.findViewById(R.id.contact_edit_contact_favorite);
        mContactFirstNameClear = header.findViewById(R.id.contact_edit_first_name_clear);
        mContactLastNameClear = header.findViewById(R.id.contact_edit_last_name_clear);
        mContactJobTitleClear = header.findViewById(R.id.contact_edit_job_title_clear);
        mContactCompanyClear = header.findViewById(R.id.contact_edit_company_clear);
        mContactAddressClear = header.findViewById(R.id.contact_edit_address_clear);
        mAddContactNumber = header.findViewById(R.id.add_phone_number);

        mContactPhoneList.addHeaderView(header, null, false);
        mHandler = new Handler();
        mLayoutCloseRunnable = this::hideMenus;

        mNewContactEnterprise.setText(R.string.contact_details_add_contact_enterprise);

        setListeners();
        setData();
        changeUIForDevice();

        return root;
    }

    void changeUIForDevice() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnContactEditInteractionListener) {
            mEditListener = (ContactEditFragment.OnContactEditInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContactDetailsInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mEditListener = null;
    }

    /**
     * Processing removing of item and calling method to check if edit is valid
     */
    @Override
    public void itemRemoved() {

        // bellow code resolves a bug that when you click to delete phone number it deletes one bellow also
        View lastPhoneNumberItem = mContactPhoneList.getChildAt(mAdapter.getCount());
        if (lastPhoneNumberItem != null) {
            EditText phoneNumberEditText = lastPhoneNumberItem.findViewById(R.id.contact_edit_phone_number);
            if (phoneNumberEditText != null) {
                String phoneNumber = phoneNumberEditText.getText().toString();
                mAdapter.getmContactPhones().get(mAdapter.getmContactPhones().size() - 1).setNumber(phoneNumber);
            }
        }

        // validating if edit is allowed
        validateEdit();
    }

    /**
     * Processing information on phone number valid event and calling method to check if edit is valid
     *
     * @param phoneValid boolean
     */
    @Override
    public void phoneNumberValid(boolean phoneValid) {
        phoneNumberValid = phoneValid;

        // in case we get response from EditText that it is not valid, we just do general check
        // and see if any phone numbers are valid. If they are, we allow saving of contact
        if (!phoneValid) {
            phoneNumberValid = allowSavingPhones();
        }
        validateEdit();
    }

    /**
     * Processing click on phone type
     *
     * @param itemPosition     int position of item
     * @param listItemPosition int position in list of items
     */
    @Override
    public void phoneTypeClicked(int itemPosition, int listItemPosition) {
        if (mContactType == BROADSOFT_CONTACT) {
            return;
        }
        this.currentPhoneListPosition = listItemPosition;

        Utils.hideKeyboard(getActivity());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        int offset = mCurrentPhoneTypeMenuHolder.getId() == R.id.phone_enterprise_type_menu_holder ? 370 : 450;
        int position = (itemPosition - offset);
        if (position < 0) {
            position = 0;
        }
        params.setMargins(70, position, 0, 0);
        params.gravity = Gravity.TOP;
        contact_edit_holder.setClickable(true);
        contact_edit_holder.setVisibility(View.VISIBLE);
        mCurrentPhoneTypeMenuHolder.setLayoutParams(params);
        mCurrentPhoneTypeMenuHolder.setVisibility(View.VISIBLE);
        mHandler.postDelayed(mLayoutCloseRunnable, Constants.LAYOUT_DISAPPEAR_TIME);
    }

    /**
     * Requesting focus for {@link EditText}
     *
     * @param editText {@link EditText}
     */
    @Override
    public void requestFocus(final EditText editText) {
        mTextToFocus = editText;
        mTextToFocus.setFocusableInTouchMode(true);
        mTextToFocus.requestFocus();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.type_work:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.WORK);
                break;
            case R.id.type_mobile:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.MOBILE);
                break;
            case R.id.type_home:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.HOME);
                break;
            case R.id.type_fax:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.FAX);
                break;
            case R.id.type_pager:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.PAGER);
                break;
            case R.id.type_other:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.OTHER);
                break;
            case R.id.contact_type_local:
                setNewContactType(LOCAL_CONTACT);
                break;
            case R.id.contact_type_enterprise:
                if (isOpenSipEnabled()) {
                    setNewContactType(BROADSOFT_CONTACT);
                } else {
                    setNewContactType(ENTERPRISE_CONTACT);
                }
                break;
            default:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.OTHER);
        }
        hideMenus();
    }

    /**
     * Load settings from shared preferences and set new contact type according to settings
     */
    private void loadNewContactTypeSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String defaultNewContact = null;
        try {
            defaultNewContact = prefs.getString(NEW_CONTACT_PREF, ENTERPRISE_CONTACT_PREF);
        } catch (ClassCastException e) {
            Log.e(TAG, "loadNewContactTypeSettings", e);
            defaultNewContact = String.valueOf(prefs.getLong(NEW_CONTACT_PREF, Long.valueOf(ENTERPRISE_CONTACT_PREF)));
        }
        assert defaultNewContact != null;
        switch (defaultNewContact) {
            case LOCAL_CONTACT_PREF:
                setNewContactType(LOCAL_CONTACT);
                break;
            case ENTERPRISE_CONTACT_PREF:
                // if IPO is enabled, Enterprise is disabled. That is why we switch between these two.
                // Adding BroadSoft directory account type
                if (isOpenSipEnabled()) {
                    setNewContactType(BROADSOFT_CONTACT);
                } else {
                    setNewContactType(ENTERPRISE_CONTACT);
                }
                break;
            default:
                setNewContactType(LOCAL_CONTACT);
        }
    }

    /**
     * Hiding menus and setting them non clickable
     */
    private void hideMenus() {
        mHandler.removeCallbacks(mLayoutCloseRunnable);
        mCurrentPhoneTypeMenuHolder.setVisibility(View.INVISIBLE);
        mContactTypeHolder.setVisibility(View.INVISIBLE);
        if (isAddingNewContact) {
            mEditTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_expand_more, 0, 0, 0);
        }
        contact_edit_holder.setClickable(false);
        contact_edit_holder.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && getActivity() != null) {
            switch (requestCode) {
                case PICK_PHOTO:
                    mImageUri = data.getData();
                    if (mImageUri != null) {
                        setContactPhoto(mImageUri);
                    }
                    break;
                case REQUEST_CAMERA_PHOTO:
                    try {
                        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                        File f = new File(mTempPhotoPath);
                        rotateImageForDevice();
                        mImageUri = Uri.fromFile(f);
                        if (mImageUri != null) {
                            setContactPhoto(mImageUri);
                            mediaScanIntent.setData(mImageUri);
                            getActivity().sendBroadcast(mediaScanIntent);
                        }
                        mTempPhotoPath = null;
                    } catch (Exception e) {
                        Log.e(TAG, "onActivityResult", e);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    void rotateImageForDevice() throws IOException {
    }

    /**
     * Update on stat of favorite based on click
     */
    private void updateFavorites() {
        mFavoriteSelected = !mFavoriteSelected;
        setFavoriteColor();
    }

    /**
     * Setting color of favorites star properly for contact
     */
    private void setFavoriteColor() {
        if (mFavoriteImage != null) {
            if (mFavoriteSelected) {
                mFavoriteImage.setColorFilter(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.colorAccent));
                mFavoriteImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_selected48));
            } else {
                mFavoriteImage.setColorFilter(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.colorPrimary));
                mFavoriteImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorites_selected));
            }
        }
    }

    /**
     * Set listeners on all views.
     */
    private void setListeners() {
        typeWork.setOnClickListener(this);
        typeHome.setOnClickListener(this);
        typeMobile.setOnClickListener(this);
        typeFax.setOnClickListener(this);
        typePager.setOnClickListener(this);
        typeOther.setOnClickListener(this);

        // no need for click listeners if we are editing existing contact
        if (isAddingNewContact) {
            mNewContactLocal.setOnClickListener(this);
            mNewContactEnterprise.setOnClickListener(this);
            mEditTitle.setOnClickListener(view -> {
                contact_edit_holder.setClickable(true);
                contact_edit_holder.setVisibility(View.VISIBLE);
                mContactTypeHolder.setVisibility(View.VISIBLE);
                mEditTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_expand_less, 0, 0, 0);
            });

            mNewContactEnterprise.setEnabled(true);
            mNewContactEnterprise.setOnClickListener(this);
            mNewContactEnterprise.setTextColor(Objects.requireNonNull(getActivity()).getColor(R.color.primary));
        } else {
            mEditTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        contact_edit_holder.setOnClickListener(v -> hideMenus());

        mContactFirstNameClear.setOnClickListener(view -> mContactFirstName.setText(""));
        mContactLastNameClear.setOnClickListener(view -> mContactLastName.setText(""));
        mContactJobTitleClear.setOnClickListener(view -> mContactJobTitle.setText(""));
        mContactCompanyClear.setOnClickListener(view -> mContactCompany.setText(""));
        mContactAddressClear.setOnClickListener(view -> mContactAddress.setText(""));
        mContactFirstName.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                mContactFirstNameClear.setVisibility(View.INVISIBLE);
            } else {
                mContactFirstNameClear.setVisibility(View.VISIBLE);
            }
        });
        mContactLastName.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                mContactLastNameClear.setVisibility(View.INVISIBLE);
            } else {
                mContactLastNameClear.setVisibility(View.VISIBLE);
            }
        });

        mContactLastName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                if (mContactType != LOCAL_CONTACT) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        return true;
                    }
                }
            }
            return false;
        });
        mContactJobTitle.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                mContactJobTitleClear.setVisibility(View.INVISIBLE);
            } else {
                mContactJobTitleClear.setVisibility(View.VISIBLE);
            }
        });
        mContactCompany.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                mContactCompanyClear.setVisibility(View.INVISIBLE);
            } else {
                mContactCompanyClear.setVisibility(View.VISIBLE);
            }
        });
        mContactAddress.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                mContactAddressClear.setVisibility(View.INVISIBLE);
            } else {
                mContactAddressClear.setVisibility(View.VISIBLE);
            }
        });
        mFavoriteImage.setOnClickListener(view -> {
            if (!disallowFavoriteStatusChange) {
                updateFavorites();
            } else {
                Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_non_edit_favorite), Utils.SNACKBAR_LONG);
            }
        });
        mCancel.setOnClickListener(view -> cancelOnClickListener());
        mDone.setOnClickListener(view -> {
            if (saveContact()) {
                additonalUIchangesforDevice();
            }
        });
        mAddContactNumber.setOnClickListener(view -> {
            if (mAdapter != null && allowAddPhoneNumber()) {
                ContactData.PhoneType phoneType = ContactData.PhoneType.WORK;

                EditablePhoneNumber newNumber = new EditablePhoneNumber("", phoneType, false, null);
                mPhoneNumbers.add(newNumber);
                mContactPhoneList.setSelection(mPhoneNumbers.size() - 1);
                mAdapter.notifyDataSetChanged();
                validateEdit();
            }
        });
        mContactImage.setOnClickListener(view -> {
            if (mContactData.mCategory == ContactData.Category.LOCAL || mContactType == LOCAL_CONTACT) {
                showImagePicker();
            }
        });
        mContactFirstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                validateEdit();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        mContactLastName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                validateEdit();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    void onLayoutChangeForDevice() {
    }

    void additonalUIchangesforDevice() {
    }

    public void cancelOnClickListener() {
        mEditListener.cancelEdit();

        cancelOnClickListenerUIChanges();
    }

    void cancelOnClickListenerUIChanges() {
    }

    private boolean saveContact() {
        if (!mAdapter.primaryPhoneExist()) {
            // hiding the keyboard so that SnackBar can be visible
            Utils.hideKeyboard(getActivity());
            Utils.sendSnackBarData(getContext(), getString(R.string.must_set_primary), Utils.SNACKBAR_LONG);
            return false;
        }

        if (saveContactClicked) {
            return false;
        }
        saveContactClicked = true;

        if (!checkIfValid(true)) {
            saveContactClicked = false;
            return false;
        }

        if (mContactType == BROADSOFT_CONTACT && isDirectoryEnabled()) {
            if (!LocalContactsRepository.getInstance().getDirectories().isEmpty()) {
                DirectoryData directoryData = LocalContactsRepository.getInstance().
                        getDirectoryDataByName(isOpenSipEnabled());
                if (directoryData != null) {
                    mAccountType = directoryData.type;
                    mAccountName = directoryData.accountName;
                }
            } else {
                Utils.hideKeyboard(getActivity());
                Utils.sendSnackBarDataWithDelay(getActivity(), getString(R.string.not_enterprise_found), true);
                saveContactClicked = false;
                return false;
            }
        } else {
            mAccountName = null;
            mAccountType = null;
        }

        ContactData.Category category;

        if (isAddingNewContact) {
            switch (mContactType) {
                case LOCAL_CONTACT:
                    category = ContactData.Category.LOCAL;
                    break;
                case ENTERPRISE_CONTACT:
                    category = ContactData.Category.ENTERPRISE;
                    break;
                case BROADSOFT_CONTACT:
                    category = ContactData.Category.BROADSOFT;
                    break;
                default:
                    category = ContactData.Category.LOCAL;

            }
        } else {
            category = mContactData.mCategory;
        }
        List<ContactData.PhoneNumber> newPhoneNumbers = addNewPhoneNumbersToContact();

        final String firstName = mContactFirstName.getText().toString().trim();
        final String lastName = mContactLastName.getText().toString().trim();
        final String name = Utils.combinedName(getContext(), firstName, lastName);
        assert mContactData.mUUID != null;
        ContactData newContactData = new ContactData(name, firstName, lastName,
                mContactData.mPhoto,
                mFavoriteSelected,
                mContactData.mLocation,
                mContactAddress.getText().toString(),
                mContactJobTitle.getText().toString(),
                mContactCompany.getText().toString(),
                newPhoneNumbers,
                category,
                mContactData.mUUID,
                mContactData.mURI,
                mContactData.mPhotoThumbnailURI,
                mContactData.mHasPhone,
                mContactData.mEmail,
                mContactData.mPhotoURI,
                mAccountType,
                "",
                mAccountName);

        if (newContactData.mCategory == ContactData.Category.ENTERPRISE) {
            if (!isAddingNewContact && mEditableEnterpriseContact == null) {
                Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_enterprise_contact_not_found), Utils.SNACKBAR_LONG);
                findEnterpriseEditableContact();
            } else {
                mEditListener.confirmEnterpriseEdit(newContactData, mEditableEnterpriseContact, isAddingNewContact);
            }
        } else {
            mEditListener.confirmLocalContactEdit(newContactData, mImageUri, isAddingNewContact);
        }

        saveContactClicked = false;

        return true;
    }

    /**
     * Set edited contact phones.
     *
     * @return List of newly set contact phone numbers.
     */
    private List<ContactData.PhoneNumber> addNewPhoneNumbersToContact() {
        List<ContactData.PhoneNumber> newPhoneNumbers = new ArrayList<>();

        // Focused phone position. Used to fix bug with phone number not being saved
        // if EditText is focused in time of saving.
        int focusedPhonePosition = mAdapter.getViewPositionWithFocus();

        boolean phoneEditInProgress = focusedPhonePosition >= 0;

        for (int i = 0; i < mPhoneNumbers.size(); i++) {
            if (!TextUtils.isEmpty(mPhoneNumbers.get(i).getNumber()) || focusedPhonePosition == i) {
                String retrievedPhoneNumber = getPhoneNumber(i, phoneEditInProgress);
                if (!TextUtils.isEmpty(retrievedPhoneNumber)) {
                    newPhoneNumbers.add(new ContactData.PhoneNumber(retrievedPhoneNumber,
                            mPhoneNumbers.get(i).getType(), mPhoneNumbers.get(i).isPrimary(),
                            mPhoneNumbers.get(i).getPhoneNumberId()));
                }
            }
        }

        return newPhoneNumbers;
    }

    /**
     * Used to fix problem with phone number edit field that is in focus.
     * Field in focus wont update mPhoneNumbers list.
     *
     * @param phonePosition        Position of phone number in list.
     * @param phoneNumberIsFocused Edit text with phone number is focused.
     * @return phone number
     */
    private String getPhoneNumber(int phonePosition, boolean phoneNumberIsFocused) {
        String phoneNumber = mPhoneNumbers.get(phonePosition).getNumber();
        if (mContactPhoneList != null) {
            if (phoneNumberIsFocused) {
                View focusedPhoneNumberItem = mContactPhoneList.getChildAt(phonePosition + 1);
                if (focusedPhoneNumberItem != null) {
                    EditText phoneNumberEditText = focusedPhoneNumberItem.findViewById(R.id.contact_edit_phone_number);
                    if (phoneNumberEditText != null) {
                        phoneNumber = phoneNumberEditText.getText().toString();
                    }
                }
            }
        }
        return phoneNumber;
    }

    /**
     * Method is used to check if all phone number texts have phone numbers in them
     *
     * @return if some phone numbers are empty, do not allow adding of another phone number
     */
    private boolean allowAddPhoneNumber() {
        boolean allowAdd = true;
        if (mContactPhoneList != null) {
            // IPO and BroadSoft supports only one phone number, just making sure we notify user about that
            if (mContactType == BROADSOFT_CONTACT && mContactPhoneList.getAdapter().getCount() > 1) {
                Utils.hideKeyboard(getActivity()); // hiding keyboard to make sure user can see error message
                Utils.sendSnackBarData(getActivity(), getString(R.string.contact_details_ipo_only_one_number), true);
                return false;
            }

            for (int i = 0; i < mContactPhoneList.getAdapter().getCount(); i++) {
                View phoneNumberItem = mContactPhoneList.getChildAt(i);
                if (phoneNumberItem != null) {
                    EditText phoneNumberText = phoneNumberItem.findViewById(R.id.contact_edit_phone_number);
                    if (phoneNumberText != null && phoneNumberText.getText().toString().trim().length() == 0) {
                        allowAdd = false;
                        break;
                    }
                }
            }
        }
        return allowAdd;
    }

    /**
     * This method will go through all phone numbers and check if any phone number is valid
     *
     * @return if any phone number is valid, allow save
     */
    private boolean allowSavingPhones() {
        boolean allowSave = false;
        if (mContactPhoneList != null) {
            for (int i = 0; i < mContactPhoneList.getAdapter().getCount(); i++) {
                View phoneNumberItem = mContactPhoneList.getChildAt(i);
                if (phoneNumberItem != null) {
                    EditText phoneNumberText = phoneNumberItem.findViewById(R.id.contact_edit_phone_number);
                    if (phoneNumberText != null && phoneNumberText.getText().toString().trim().length() > 0) {
                        allowSave = true;
                    }
                }
            }
        }
        return allowSave;
    }

    private void validateEdit() {
        if (checkIfValid(false)) {
            mDone.setTextColor(getResources().getColor(R.color.midOrange, Objects.requireNonNull(getActivity()).getTheme()));
            mDone.setEnabled(true);
        } else {
            mDone.setTextColor(getResources().getColor(R.color.midGray, Objects.requireNonNull(getActivity()).getTheme()));
            mDone.setEnabled(false);
        }
    }

    private boolean checkIfValid(boolean notify) {
        // BroadSoft and IPO allow only name
        if (mContactType == BROADSOFT_CONTACT) {
            if (mContactFirstName.getText().toString().trim().matches("")) {
                if (notify) {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_name_required), Utils.SNACKBAR_LONG);
                    mContactFirstName.requestFocus();
                }
                return false;
            }
            // Aura, PPM require both first name and last name
        } else if (mContactType == ENTERPRISE_CONTACT) {
            if (mContactFirstName.getText().toString().trim().matches("") || mContactLastName.getText().toString().trim().matches("")) {
                if (notify) {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_name_required), Utils.SNACKBAR_LONG);
                    mContactFirstName.requestFocus();
                }
                return false;
            }
            // Local contacts require either first or last name
        } else {
            if (mContactFirstName.getText().toString().trim().matches("") && mContactLastName.getText().toString().trim().matches("")) {
                if (notify) {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_name_required), Utils.SNACKBAR_LONG);
                    mContactFirstName.requestFocus();
                }
                return false;
            }
        }

        if (mAdapter == null) {
            return false;
        }

        if (mAdapter.getCount() == 0 || !phoneNumberValid) {
            if (notify) {
                Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_number_required), Utils.SNACKBAR_LONG);
            }
            return false;
        }
        return true;
    }

    /**
     * Present user information to user.
     */
    private void setData() {
        mContactJobTitle.setText(mContactData.mPosition);
        mContactCompany.setText(mContactData.mCompany);
        mContactAddress.setText(mContactData.mCity);
        if (isAddingNewContact) {
            mEditTitle.setText(getString(R.string.contact_details_add_contact));
        }
        setFavoriteColor();

        mAdapter = new ContactEditPhoneListAdapter(getContext(), mPhoneNumbers, this);
        mContactPhoneList.setAdapter(mAdapter);
        validateEdit();
        mContactFirstName.requestFocus();

        PhotoLoadUtility.setThumbnail(mContactData, mContactImage, isFirstNameFirst());

        if (isAddingNewContact) {
            loadNewContactTypeSettings();
        }

        setNewContactType(mContactType);

        if (mContactType == BROADSOFT_CONTACT) {
            assert mContactData.mLastName != null;
            if (mContactData.mFirstName.trim().length() > 0 && mContactData.mLastName.trim().length() > 0) {
                mContactFirstName.setText(mContactData.mFirstName + " " + mContactData.mLastName);
            } else {
                mContactFirstName.setText(mContactData.mName);
            }
        } else {
            mContactFirstName.setText(mContactData.mFirstName);
            mContactLastName.setText(mContactData.mLastName);
        }
    }

    /**
     * Displaying contact photo
     *
     * @param photoURI contact photo URI
     */
    private void setContactPhoto(Uri photoURI) {
        try {
            Bitmap uriImage = MediaStore.Images.Media.getBitmap(Objects.requireNonNull(getActivity()).getContentResolver(), photoURI);
            //Perform check if we have to check for image rotation or not. If image is provided by CSDK
            //result of getRealPathFromURI will be empty string
            uriImage = Utils.checkAndPerformBitmapRotation(uriImage, getActivity().getContentResolver(), photoURI);
            RoundedBitmapDrawable contactPhoto =
                    RoundedBitmapDrawableFactory.create(getResources(), uriImage);
            contactPhoto.setCircular(true);
            mContactImage.setText("");
            mContactImage.setBackground(contactPhoto);
        } catch (IOException e) {
            Log.e(TAG, "setContactPhoto", e);
        }
    }

    /**
     * Set initials for contact. In case {@link ContactData} have photo url we we use it.
     * If there is no url we will create initial view from name of contact.
     *
     * @param photo       {@link TextView} in which we are showing initials.
     * @param contactData {@link ContactData} from which we are taking data.
     */
    private void setInitials(final TextView photo, ContactData contactData) {
        String name;
        String lastName = contactData.mLastName;
        String firstName = contactData.mFirstName;

        name = contactData.mName;
        int colors[] = photo.getResources().getIntArray(R.array.material_colors);
        photo.setBackgroundResource(R.drawable.empty_circle);
        assert name != null;
        ((GradientDrawable) photo.getBackground().mutate()).setColor(colors[Math.abs(name.hashCode() % colors.length)]);

        int nameDisplay = mUserPreference.getInt(NAME_DISPLAY_PREFERENCE, FIRST_NAME_FIRST);
        @SuppressWarnings("UnusedAssignment") String firstLetter = "";
        String secondLetter = "";

        if (nameDisplay == LAST_NAME_FIRST) {
            if (!TextUtils.isEmpty(lastName)) {
                assert lastName != null;
                firstLetter = String.valueOf(lastName.toUpperCase().charAt(0));
            }
            if (!TextUtils.isEmpty(firstName)) {
                secondLetter = String.valueOf(firstName.toUpperCase().charAt(0));
            }
        } else {
            if (!TextUtils.isEmpty(lastName)) {
                assert lastName != null;
                secondLetter = String.valueOf(lastName.toUpperCase().charAt(0));
            }

            if (!TextUtils.isEmpty(firstName)) {
                firstLetter = String.valueOf(firstName.toUpperCase().charAt(0));
            }
        }

        String initials = firstLetter + secondLetter;
        photo.setText(initials);
    }

    /**
     * Get EditableEnterpriseContact from CSDK since ContactData is immutable
     */
    private void findEnterpriseEditableContact() {
        if (mContactData != null && mContactData.mRefObject != null) {
            mEditableEnterpriseContact = SDKManager.getInstance().getContactsAdaptor()
                    .getUser().getContactService().createEditableContactFromContact(mContactData.mRefObject);
        }
    }

    /**
     * Prepare and show {@link android.app.AlertDialog} for choosing new image with options for
     * taking photo, choosing photo from device, deleting it or just cancel selection.
     */
    private void showImagePicker() {
        CharSequence[] items;

        if (Utils.isCameraSupported()) {
            items = new CharSequence[]{getString(R.string.contact_edit_take_photo), getString(R.string.contact_edit_choose_photo), getString(R.string.contact_edit_delete_photo)};
        } else {
            items = new CharSequence[]{getString(R.string.contact_edit_choose_photo), getString(R.string.contact_edit_delete_photo)};
        }

        if (isAdded() && (getContext() != null)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getContext()));
            builder.setTitle(getString(R.string.contact_edit_change_photo));
            builder.setItems(items, (dialog, item) -> {

                if (items[item].equals(getString(R.string.contact_edit_take_photo))) {
                    dispatchTakePictureIntent();
                } else if (items[item].equals(getString(R.string.contact_edit_choose_photo))) {
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, PICK_PHOTO);
                } else if (items[item].equals(getString(R.string.contact_edit_delete_photo))) {
                    // just adding empty string so that we can tell LocalContactManager to delete the image for this contact
                    mImageUri = Uri.parse("");
                    setInitials(mContactImage, mContactData);
                }
            });
            builder.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss());

            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    /**
     * Change new contact type
     *
     * @param newContactType type of new contact
     */
    private void setNewContactType(int newContactType) {
        switch (newContactType) {
            case LOCAL_CONTACT:
            default:
                mContactType = LOCAL_CONTACT;
                if (isAddingNewContact) {
                    mEditTitle.setText(getContext().getString(R.string.new_contact_header_text, Objects.requireNonNull(getContext()).getString(R.string.contact_details_add_contact_local).substring(0, 1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_local).substring(1).toLowerCase()));
                } else {
                    mEditTitle.setText(getContext().getString(R.string.edit_contact_header_text, Objects.requireNonNull(getContext()).getString(R.string.contact_details_add_contact_local).substring(0, 1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_local).substring(1).toLowerCase()));
                }
                mFavoriteImage.setVisibility(View.VISIBLE);

                enableEditText(mContactFirstName);
                enableEditText(mContactLastName);
                enableEditText(mContactAddress);
                enableEditText(mContactCompany);
                enableEditText(mContactJobTitle);

                if (mCurrentPhoneTypeMenuHolder != null) {
                    mCurrentPhoneTypeMenuHolder.setVisibility(View.INVISIBLE);
                }
                mCurrentPhoneTypeMenuHolder = mPhoneTypeLocalMenuHolder;
                break;
            case ENTERPRISE_CONTACT:
                mContactType = ENTERPRISE_CONTACT;
                if (isAddingNewContact) {
                    mEditTitle.setText(getContext().getString(R.string.new_contact_header_text, Objects.requireNonNull(getContext()).getString(R.string.contact_details_add_contact_enterprise).substring(0, 1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_enterprise).substring(1).toLowerCase()));
                } else {
                    mEditTitle.setText(getContext().getString(R.string.edit_contact_header_text, Objects.requireNonNull(getContext()).getString(R.string.contact_details_add_contact_enterprise).substring(0, 1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_enterprise).substring(1).toLowerCase()));
                }
                mFavoriteImage.setVisibility(View.VISIBLE);

                if (mCurrentPhoneTypeMenuHolder != null) {
                    mCurrentPhoneTypeMenuHolder.setVisibility(View.INVISIBLE);
                }
                mCurrentPhoneTypeMenuHolder = mPhoneTypeEntepriseMenuHolder;
                clearPhoto();

                // Create dummy placeholder EditableContact to get info which are writable and which are not
                if (SDKManager.getInstance().getContactsAdaptor().getUser() != null && isAddingNewContact) {
                    EditableContact placeholderContact = SDKManager.getInstance().getContactsAdaptor().getUser().getContactService().createEditableContact();
                    if (placeholderContact != null) {
                        setupEnterpriseFields(placeholderContact);
                    }
                } else if (mContactData.mRefObject != null) {
                    setupEnterpriseFields(mEditableEnterpriseContact);
                }
                break;
            case BROADSOFT_CONTACT:
                mContactType = BROADSOFT_CONTACT;
                if (isAddingNewContact) {
                    mEditTitle.setText(getContext().getString(R.string.new_contact_header_text, Objects.requireNonNull(getContext()).getString(R.string.contact_details_add_contact_personal_directory).substring(0, 1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_personal_directory).substring(1).toLowerCase()));
                } else {
                    mEditTitle.setText(getContext().getString(R.string.edit_contact_header_text, Objects.requireNonNull(getContext()).getString(R.string.contact_details_add_contact_personal_directory).substring(0, 1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_personal_directory).substring(1).toLowerCase()));
                }

                mContactFirstName.setHint(R.string.contact_edit_ipo_name_hint);

                mFavoriteImage.setVisibility(View.INVISIBLE);
                if (mContactFirstName.getText().length() > IPO_CONTACT_NAME_LIMIT) {
                    Utils.sendSnackBarData(getActivity(), String.format(Objects.requireNonNull(getActivity()).getString(R.string.contact_name_char_limit), String.valueOf(IPO_CONTACT_NAME_LIMIT)), true);
                    Utils.hideKeyboard(getActivity());
                }
                if (mCurrentPhoneTypeMenuHolder != null) {
                    mCurrentPhoneTypeMenuHolder.setVisibility(View.INVISIBLE);
                }
                mCurrentPhoneTypeMenuHolder = mPhoneTypeLocalMenuHolder;
                clearPhoto();

                hideEditText(mContactLastName);
                hideEditText(mContactAddress);
                hideEditText(mContactCompany);
                hideEditText(mContactJobTitle);

                mAdapter.clearAllExceptFirst();
                break;
        }
        validateEdit();
    }

    /**
     * If user selects Enterprise contact, we clear the photo and notify the photo it is not supported
     */
    private void clearPhoto() {
        if (mImageUri != null && mImageUri.toString().trim().length() > 0) {
            mContactImage.setText("");
            mContactImage.setBackground(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.ic_avatar_generic105));
            Utils.sendSnackBarData(getActivity(), getString(R.string.photo_not_supported), true);
        }
    }

    /**
     * Prepare Enterprise fields for editing process based on provided {@link EditableContact}
     *
     * @param editableContact {@link EditableContact}
     */
    private void setupEnterpriseFields(EditableContact editableContact) {
        Activity activity = getActivity();
        if (!isAdded() || activity == null) {
            return;
        }

        EditableContactStringField fieldName = editableContact.getNativeFirstName();
        if (!fieldName.getCapability().isAllowed()) {
            Log.d(LOG_TAG, "FirstName not editable");
            disableEditText(mContactFirstName, R.string.contact_edit_non_edit_first_name);
        }

        EditableContactStringField fieldLastName = editableContact.getNativeLastName();
        if (!fieldLastName.getCapability().isAllowed()) {
            Log.d(LOG_TAG, "LastName not editable");
            disableEditText(mContactLastName, R.string.contact_edit_non_edit_last_name);
        }

        EditableContactStringField fieldCompany = editableContact.getCompany();
        if (!fieldCompany.getCapability().isAllowed()) {
            Log.d(LOG_TAG, "mContactCompany name not editable");
            disableEditText(mContactCompany, R.string.contact_edit_non_edit_company);
        }

        EditableContactStringField fieldTitle = editableContact.getTitle();
        if (!fieldTitle.getCapability().isAllowed()) {
            Log.d(LOG_TAG, "mEditTitle name not editable");
            disableEditText(mContactJobTitle, R.string.contact_edit_non_edit_job);
        }

        EditableContactStringField fieldCity = editableContact.getCity();
        if (!fieldCity.getCapability().isAllowed()) {
            Log.d(LOG_TAG, "mContactAddress name not editable");
            disableEditText(mContactAddress, R.string.contact_edit_non_edit_address);
        }

        EditableContactBoolField fieldIsFavorite = editableContact.isFavorite();
        disallowFavoriteStatusChange = !fieldIsFavorite.getCapability().isAllowed();
    }

    private void enableEditText(EditText editText) {
        editText.setVisibility(View.VISIBLE);
        editText.setEnabled(true);
        editText.setFocusableInTouchMode(true);
        editText.setTextColor(getResources().getColor(R.color.colorBlack, Objects.requireNonNull(getActivity()).getTheme()));
        editText.setHintTextColor(getResources().getColor(R.color.colorDefaultHint, getActivity().getTheme()));
    }

    private void disableEditText(EditText editText, int errorString) {
        editText.setFocusable(false);
        if (isAddingNewContact) {
            editText.getText().clear();
        }
        editText.setTextColor(getResources().getColor(R.color.lightGray, Objects.requireNonNull(getActivity()).getTheme()));
        editText.setHintTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
        editText.setOnClickListener(view -> {
            Utils.hideKeyboard(getActivity());
            Utils.sendSnackBarData(getContext(), getString(errorString), Utils.SNACKBAR_LONG);
        });
    }

    private void hideEditText(EditText editText) {
        editText.setVisibility(View.INVISIBLE);
    }

    /**
     * Create file for camera image
     *
     * @return Empty file
     * @throws IOException IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Objects.requireNonNull(getActivity()).getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mTempPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Sending intent for taking picture
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(Objects.requireNonNull(getActivity()).getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "Error occurred while creating the File", ex);
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                        Constants.COM_AVAYA_ANDROID_VANTAGE_BASIC_PROVIDER,
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA_PHOTO);
            }
        }
    }

    private boolean isDirectoryEnabled() {
        return isOpenSipEnabled();
    }

    private boolean isOpenSipEnabled() {
        return SDKManager.getInstance().getDeskPhoneServiceAdaptor()
                .isOpenSipEnabled();
    }

    /**
     * Rotate image since camera default mode is landscape and we image needs to be in portrait mode
     *
     * @param file Image Uri
     * @throws IOException IOException
     */
    void rotateImage(String file) throws IOException {

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, bounds);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = BitmapFactory.decodeFile(file, opts);

        // on Avaya Vantage, camera is by default in landscape mode so we need to rotate image
        int rotationAngle = 90;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
        FileOutputStream fos = new FileOutputStream(file);
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();
    }

    private void setContactDataCategory() {
        if (mContactData.mCategory == ContactData.Category.LOCAL) {
            mContactType = LocalContactInfo.getAccountInfo(getContext(), mContactData.mUUID);
        } else if (mContactData.mCategory == ContactData.Category.ENTERPRISE) {
            mContactType = ENTERPRISE_CONTACT;
        } else if (mContactData.mCategory == ContactData.Category.BROADSOFT) {
            mContactType = BROADSOFT_CONTACT;
        }
    }

    /**
     * To be implemented in {@link com.avaya.android.vantage.aaadevbroadcast.activities.MainActivity} to
     * provide communication with {@link ContactsFragment}
     */
    public interface OnContactEditInteractionListener {
        void cancelEdit();

        void confirmLocalContactEdit(ContactData contactData, Uri imageUri, boolean isNewContact);

        void confirmEnterpriseEdit(ContactData contactData, EditableContact contact, boolean isNewContact);
    }
}