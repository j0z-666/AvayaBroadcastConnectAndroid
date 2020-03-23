package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.PhotoLoadUtility;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactDetailsFragmentPresenter;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsRecyclerViewAdapter;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.LocalContactInfo;
import com.avaya.android.vantage.aaadevbroadcast.csdk.LocalContactsManager;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.views.adapters.ContactDetailsPhoneListAdapter;
import com.avaya.clientservices.contact.ContactCompletionHandler;
import com.avaya.clientservices.contact.ContactException;
import com.avaya.clientservices.contact.EditableContact;

import java.util.List;

import static com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment.isFirstNameFirst;
import static com.avaya.clientservices.contact.ContactError.NOT_FOUND;
import static com.avaya.clientservices.contact.ContactError.OPERATION_INPROGRESS;

/**
 * Contact Details Fragment which show to user all relevant data regarding selected contact
 */
abstract public class ContactDetailsFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = ContactDetailsFragment.class.getSimpleName();

    public LinearLayout mDeletePopUp;
    public OnContactDetailsInteractionListener mBackListener;
    public boolean isBackORDeletePressed = false;
    TextView mContactEdit;
    TextView mContactDelete;
    LinearLayout nameInfo;
    ImageView openCloseNameInfo;
    private TextView mContactNameText;
    private TextView mContactImage;
    private TextView mBackText;
    private TextView mCompanyText;
    private TextView mContactType;
    private TextView mContactLocation;
    private TextView mContactPosition;
    private ImageView mFavoriteImage;
    private ListView mContactPhoneList;
    private TextView mDeleteConfirme;
    private TextView mDeleteCancel;
    private OnContactInteractionListener mListener;
    private LocalContactsManager mLocalContactsManager;
    private Uri mContactUri;
    private boolean isNewContact;
    private boolean mIsEnterpriseEditable = false;
    private String mCanModifyContacts;
    private boolean isServerExisting = false;
    private EditableContact mEditableEnterpriseContact;
    private ContactData mContactData;
    private ContactDetailsFragmentPresenter mContactDetailsFragmentPresenter;
    private boolean isContactInfo = false;

    public ContactDetailsFragment() {
        mContactDetailsFragmentPresenter = new ContactDetailsFragmentPresenter();
    }

    public static ContactDetailsFragment newInstance(ContactData contactData) {
        ContactDetailsFragment fragment = ElanApplication.getDeviceFactory().getContactDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.CONTACT_DATA, contactData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contact_details, container, false);
        mBackText = root.findViewById(R.id.contact_details_back);
        mContactImage = root.findViewById(R.id.contact_details_contact_image);
        mFavoriteImage = root.findViewById(R.id.contact_details_contact_favorite);
        mContactPhoneList = root.findViewById(R.id.contact_details_phone_list);
        mCompanyText = root.findViewById(R.id.contact_details_contact_company);
        mContactEdit = root.findViewById(R.id.contact_details_edit);
        mContactDelete = root.findViewById(R.id.contact_details_delete);
        mContactType = root.findViewById(R.id.contact_type);
        assert getArguments() != null;
        mContactData = getArguments().getParcelable(Constants.CONTACT_DATA);
        mContactNameText = root.findViewById(R.id.contact_details_contact_name);
        mContactLocation = root.findViewById(R.id.contact_details_contact_location);
        mContactPosition = root.findViewById(R.id.contact_details_contact_position);
        mDeletePopUp = root.findViewById(R.id.contact_delete_confirmation);
        mDeleteConfirme = root.findViewById(R.id.contact_delete_yes);
        mDeleteCancel = root.findViewById(R.id.contact_delete_no);

        mCanModifyContacts = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParam(ConfigParametersNames.ENABLE_MODIFY_CONTACTS, true);

        configureParametersForDevice(root);

        setupContactData();

        changeUIForDevice();

        return root;
    }

    void configureParametersForDevice(View root) {
    }

    void changeUIForDevice() {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBackText = null;
        mFavoriteImage = null;
        mContactEdit = null;
        mContactDelete = null;
        mDeleteConfirme = null;
        mDeleteCancel = null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnContactDetailsInteractionListener) {
            mBackListener = (OnContactDetailsInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContactDetailsInteractionListener");
        }
        if (context instanceof OnContactInteractionListener) {
            mListener = (OnContactInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContactsFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBackListener = null;
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        setFavoriteColor();
        nameDisplaySet();
    }

    private void reportEditable(boolean isEditable) {
        mIsEnterpriseEditable = isEditable;
        if (isAdded() && mContactEdit != null && isEditable
                && "1".equals(mCanModifyContacts)) {
            mContactEdit.setVisibility(View.VISIBLE);
        }
    }

    private void reportDeletable(boolean isDeletable) {
        if (isAdded() && mContactDelete != null &&
                isDeletable && !isNewContact && "1".equals(mCanModifyContacts)) {
            mContactDelete.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.contact_details_back:
                isBackORDeletePressed = true;
                mDeletePopUp.setVisibility(View.GONE);
                mBackListener.back();
                break;
            case R.id.contact_details_contact_favorite:
                mDeletePopUp.setVisibility(View.GONE);
                updateFavorites();
                break;
            case R.id.contact_details_edit:
                mDeletePopUp.setVisibility(View.GONE);
                if (isServerExisting) {
                    SDKManager.getInstance().getContactsAdaptor().createEnterpriseContact(mContactData);
                    mBackListener.back();
                } else {
                    mBackListener.edit(mContactData, isNewContact);
                }
                break;
            case R.id.contact_details_delete:
                showDeleteConfirmation();
                break;
            case R.id.contact_delete_yes:
                isBackORDeletePressed = true;
                deleteContact();
                break;
            case R.id.contact_delete_no:
                mDeletePopUp.setVisibility(View.GONE);
                break;
            case R.id.open_close_name_info:
                if (!isContactInfo) {
                    openContactInfo();
                } else {
                    closeContactInfo();
                }
            default:
        }
    }

    /**
     * Fill up data that is presented to the user.
     */
    private void setupContactData() {
        if (mContactData != null && getContext() != null) {

            //preventing crash in case mURI is null
            if (mContactData.mURI != null) {
                mContactUri = Uri.parse(mContactData.mURI);
            }

            PhotoLoadUtility.setThumbnail(mContactData, mContactImage, isFirstNameFirst());

            // if local contact is loaded, get all the additional info from the local contact manager
            if ((mContactData.mCategory == ContactData.Category.LOCAL
                    || mContactData.mCategory == ContactData.Category.BROADSOFT)
                    && mContactUri != null && mContactUri.toString().trim().length() > 0) {
                String location = "";
                String position = "";
                String company = "";
                List<ContactData.PhoneNumber> mPhoneNumbers = LocalContactInfo.getPhoneNumbers(mContactUri, getContext());
                String contactID = LocalContactInfo.getContactID(mContactUri, getContext());
                mContactData.setIsFavorite(LocalContactInfo.getFavoriteStatus(mContactUri, getContext()));

                if (!TextUtils.isEmpty(contactID)) {
                    String[] companyInfo = LocalContactInfo.getCompanyInfo(contactID, getContext());
                    mCompanyText.setText(companyInfo[0]);
                    if (mContactData.mCategory == ContactData.Category.BROADSOFT) {
                        mContactType.setText(R.string.display_personal_directory_contact_information);
                    } else if (mContactData.mAccountType != null && mContactData.mAccountType.equals(ContactsRecyclerViewAdapter.PBAP_ACCOUNT)) {
                        mContactType.setText(R.string.display_mobile_contact_information);
                    } else {
                        mContactType.setText(R.string.display_local_contact_information);
                    }
                    mContactPosition.setText(companyInfo[1]);
                    location = LocalContactInfo.getContactAddress(contactID, getContext());
                    if (!TextUtils.isEmpty(location)) {
                        mContactLocation.setText(location);
                        mContactLocation.setVisibility(View.VISIBLE);
                    }
                    company = companyInfo[0];
                    position = companyInfo[1];
                }

                assert mContactData.mUUID != null;
                mContactData = new ContactData(
                        mContactData.mName,
                        mContactData.mFirstName,
                        mContactData.mLastName,
                        null,
                        mContactData.isFavorite(),
                        mContactData.mLocation,
                        location,
                        position,
                        company,
                        mPhoneNumbers,
                        mContactData.mCategory,
                        mContactData.mUUID,
                        mContactData.mURI,
                        mContactData.mPhotoThumbnailURI,
                        mContactData.mHasPhone,
                        mContactData.mEmail,
                        mContactData.mPhotoURI,
                        mContactData.mAccountType,
                        "", "");
            } else if (mContactData.mCategory == ContactData.Category.ENTERPRISE) {
                if (mContactData.mPhones != null && mContactData.mPhones.size() > 0 && mContactData.mPhones.get(0).Number != null) {
                    mContactType.setText(R.string.display_enterprise_contact_information);

                    if (mContactData != null && mContactData.mRefObject != null) {
                        reportDeletable(mContactData.mRefObject.getDeleteContactCapability().isAllowed());
                        reportEditable(mContactData.mRefObject.getUpdateContactCapability().isAllowed());

                        mEditableEnterpriseContact = SDKManager.getInstance().getContactsAdaptor().getUser().getContactService().createEditableContactFromContact(mContactData.mRefObject);
                    }
                }
            } else if (mContactData.mCategory == ContactData.Category.DIRECTORY) {
                mContactType.setText(R.string.display_directory_contact_information);
            } else if (mContactData.mCategory == ContactData.Category.PAIRED) {
                mContactType.setText(R.string.display_mobile_contact_information);
            } else {
                mContactType.setText("");
            }

            mBackText.setOnClickListener(this);
            mFavoriteImage.setOnClickListener(this);
            mContactEdit.setOnClickListener(this);
            mContactDelete.setOnClickListener(this);
            mDeleteConfirme.setOnClickListener(this);
            mDeleteCancel.setOnClickListener(this);

            if (!TextUtils.isEmpty(mContactData.mCompany)) {
                mCompanyText.setVisibility(View.VISIBLE);
                mCompanyText.setText(mContactData.mCompany);
            }
            if (!TextUtils.isEmpty(mContactData.mPosition)) {
                mContactPosition.setVisibility(View.VISIBLE);
                mContactPosition.setText(mContactData.mPosition);
            }

            if (!TextUtils.isEmpty(mContactData.mCity)) {
                mContactLocation.setVisibility(View.VISIBLE);
                mContactLocation.setText(mContactData.mCity);
            }

            if (mContactData.mPhones != null) {
                ContactDetailsPhoneListAdapter mAdapter = new ContactDetailsPhoneListAdapter(getContext(), mContactData, mListener);
                mContactPhoneList.setAdapter(mAdapter);
            }

            mLocalContactsManager = new LocalContactsManager(getActivity());

            if (mContactData.mCategory == ContactData.Category.BROADSOFT) {
                mFavoriteImage.setVisibility(View.INVISIBLE);
            } else {
                mFavoriteImage.setVisibility(View.VISIBLE);
            }
        }
    }

    private void deleteContact() {
        if (mContactData.mCategory == ContactData.Category.ENTERPRISE) {
            Log.e(TAG, "deleteContact: " + mContactData.mRefObject.getNativeDisplayName() + " - " + mContactData.mRefObject.getUniqueAddressForMatching());
            SDKManager.getInstance().getContactsAdaptor().getUser().getContactService().deleteContact(mContactData.mRefObject, new ContactCompletionHandler() {
                @Override
                public void onSuccess() {
                    Utils.sendSnackBarData(ElanApplication.getContext(), ElanApplication.getContext().getString(R.string.contact_deleted), true);
                }

                @Override
                public void onError(ContactException error) {
                    if (error.getError() == OPERATION_INPROGRESS) {
                        Utils.sendSnackBarData(ElanApplication.getContext(), ElanApplication.getContext().getString(R.string.contact_edit_operation_in_progress), true);
                    } else if (error.getError() == NOT_FOUND) {
                        Utils.sendSnackBarData(ElanApplication.getContext(), ElanApplication.getContext().getString(R.string.contact_not_found), true);
                    } else {
                        Utils.sendSnackBarData(ElanApplication.getContext(), ElanApplication.getContext().getString(R.string.contact_delete_error), true);
                    }
                    Log.e(TAG, "deleteContact: " + error.getMessage(), error);
                }
            });
        } else if (mContactData.mCategory == ContactData.Category.LOCAL || mContactData.mCategory == ContactData.Category.IPO
                || mContactData.mCategory == ContactData.Category.BROADSOFT) {
            mLocalContactsManager.deleteLocalContact(mContactData.mUUID);
        } else {
            Utils.sendSnackBarData(getActivity(), getString(R.string.contact_uneditable_error), true);
        }

        mBackListener.back();
    }

    /**
     * Managing name display method (first name first, last name first)
     */
    private void nameDisplaySet() {

        String displayName = mContactData.getFormatedName(ContactsFragment.isFirstNameFirst());
        mContactNameText.setText(displayName);

        // if this is non existing contact, we will change "edit" text to "Create contact". Also hiding favorites icon
        boolean isExistingContacts = mContactDetailsFragmentPresenter.findExistingContact(mContactData);
        if (!isExistingContacts) {
            Log.d(TAG, "Contact doesn't exist. Create new contact.");
            if ("1".equals(mCanModifyContacts)) {
                if (mContactData != null && mContactData.mCategory == ContactData.Category.ENTERPRISE) {
                    mContactEdit.setText(R.string.contact_add_to_label);
                    mContactEdit.setContentDescription(getString(R.string.contact_add_to_label));
                    isServerExisting = true;
                } else {
                    mContactEdit.setText(R.string.contact_add_label);
                    mContactEdit.setContentDescription(getString(R.string.contact_add_label));
                }
                setContactDeleteVisibility();
            } else {
                mContactEdit.setVisibility(View.INVISIBLE);
                mContactDelete.setVisibility(View.INVISIBLE);
            }

            mFavoriteImage.setVisibility(View.INVISIBLE);
            isNewContact = true;
        } else {
            mContactEdit.setText(getString(R.string.contact_details_edit));
            mContactEdit.setContentDescription(getString(R.string.contact_details_edit));

            setLayoutParamsFOrContactEdit();

            mContactDelete.setVisibility(View.VISIBLE);

            // Local and Personal contacts are editable/deletable by default
            // For Enterprise contacts we will wait response from CSDK via reportEditable and reportDeletable methods
            if ((mContactData.mCategory == ContactData.Category.LOCAL
                    || mContactData.mCategory == ContactData.Category.BROADSOFT
                    || mContactData.mCategory == ContactData.Category.DIRECTORY || mIsEnterpriseEditable)
                    && "1".equals(mCanModifyContacts)) {
                mContactEdit.setVisibility(View.VISIBLE);
                mContactDelete.setVisibility(View.VISIBLE);
            } else {
                mContactEdit.setVisibility(View.INVISIBLE);
                mContactDelete.setVisibility(View.INVISIBLE);
            }
        }
    }

    abstract void setContactDeleteVisibility();

    void setLayoutParamsFOrContactEdit() {
    }

    /**
     * Update on stat of favorite based on click
     */
    private void updateFavorites() {
        if (mContactData != null) {
            if ((!mIsEnterpriseEditable && mContactData.mCategory == ContactData.Category.ENTERPRISE)
                    || !("1".equals(mCanModifyContacts))) {
                Utils.sendSnackBarData(getContext(), getString(R.string.contact_uneditable_error), Utils.SNACKBAR_LONG);
            } else if (mContactData.mCategory == ContactData.Category.LOCAL) {
                if (mLocalContactsManager.setAsFavorite(mContactUri, !mContactData.isFavorite())) {
                    mContactData.setIsFavorite(!mContactData.isFavorite());
                    setFavoriteColor();
                } else {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_not_found), Utils.SNACKBAR_LONG);
                    mBackListener.back();
                }
            } else {
                if (mEditableEnterpriseContact != null) {
                    mContactData.setIsFavorite(!mContactData.isFavorite());
                    SDKManager.getInstance().getContactsAdaptor().startEnterpriseEditing(mContactData, mEditableEnterpriseContact);
                    setFavoriteColor();
                } else {
                    Log.e(TAG, "updateFavorites not possible. EditableContact missing");
                }
            }
        }
    }

    /**
     * Set color of favorite star icon properly
     */
    private void setFavoriteColor() {
        if (mFavoriteImage != null && ElanApplication.getContext() != null) {
            if (mContactData.isFavorite()) {
                mFavoriteImage.setColorFilter(ElanApplication.getContext().getColor(R.color.colorAccent));
                mFavoriteImage.setImageDrawable(ElanApplication.getContext().getDrawable(R.drawable.ic_favorite_selected48));
            } else {
                mFavoriteImage.setColorFilter(ElanApplication.getContext().getColor(R.color.colorPrimary));
                mFavoriteImage.setImageDrawable(ElanApplication.getContext().getDrawable(R.drawable.ic_favorites_selected));
            }
        }
    }

    /**
     * Changes UI state to show contact name info
     */
    private void openContactInfo() {
        isContactInfo = true;
        ViewGroup.LayoutParams params = nameInfo.getLayoutParams();
        params.height = 250;
        nameInfo.setLayoutParams(params);
        openCloseNameInfo.setImageResource(R.drawable.ic_expand_less);
    }

    /**
     * Changes UI state to hide contact name info
     */
    private void closeContactInfo() {
        isContactInfo = false;
        ViewGroup.LayoutParams params = nameInfo.getLayoutParams();
        params.height = 120;
        nameInfo.setLayoutParams(params);
        openCloseNameInfo.setImageResource(R.drawable.ic_expand_more);
    }

    /**
     * Show Delete Popup menu
     */
    private void showDeleteConfirmation() {
        mDeletePopUp.setVisibility(View.VISIBLE);
    }

    /**
     * Interface responsible for comunication between {@link ContactDetailsFragment} and
     * {@link com.avaya.android.vantage.aaadevbroadcast.activities.MainActivity}. We are giving information is
     * contact edited or we just pressed back in {@link ContactDetailsFragment}
     */
    public interface OnContactDetailsInteractionListener {
        void back();

        void edit(ContactData contactData, boolean isNewContact);
    }
}