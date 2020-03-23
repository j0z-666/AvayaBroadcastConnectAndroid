package com.avaya.android.vantage.aaadevbroadcast.views.adapters;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.model.EditablePhoneNumber;

import java.util.List;

/**
 * Adapter used to show list of phone numbers in a fragment where we edit contact
 */
public class ContactEditPhoneListAdapter extends ArrayAdapter<EditablePhoneNumber> {

    private static final String TAG = "ContactDetailsPhoneList";

    // Possible phone types {"Work", "Mobile", "Home", "Handle", "Fax", "Pager", "Assistant", "Other", "Primary"}
    private final String[] phoneTypes;

    private final List<EditablePhoneNumber> mContactPhones;
    private final OnContactEditPhoneChangeListener mPhoneChangedInterface;
    private int viewWithFocusPosition = -1;

    public List<EditablePhoneNumber> getmContactPhones() {
        return mContactPhones;
    }

    /**
     * Constructor
     *
     * @param context             fragment context
     * @param contactData         data containing contact information
     * @param mPhoneNumberChanged did we change phone number?
     */
    public ContactEditPhoneListAdapter(Context context, List<EditablePhoneNumber> contactData, final OnContactEditPhoneChangeListener mPhoneNumberChanged) {
        super(context, R.layout.contact_edit_phone_layout);
        mContactPhones = contactData;
        this.mPhoneChangedInterface = mPhoneNumberChanged;
        phoneTypes = context.getResources().getStringArray(R.array.phone_type_array);
    }

    @Override
    public boolean isEnabled(int position) {
        // No list select functionality is used but it interferes with USB mouse clicks so it is disabled for now
        return false;
    }

    /**
     * Interface used to tell fragment that phone number has been changed
     */
    public interface OnContactEditPhoneChangeListener {

        void itemRemoved();

        void phoneNumberValid(boolean valid);

        void phoneTypeClicked(int itemPosition, int listItemPosition);

        void requestFocus(EditText editText);

    }

    /**
     * Obtaining size of phone list
     *
     * @return number of phone lists
     */
    @Override
    public int getCount() {
        if (mContactPhones != null) {
            return mContactPhones.size();
        } else {
            Log.d(TAG, "no phone numbers connected to account");
            return 0;
        }
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @SuppressWarnings("NullableProblems") ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.contact_edit_phone_layout, parent, false);
            // initialize the view holder
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // making sure we get data from correct list
        EditablePhoneNumber phoneNumber = mContactPhones.get(position);
        //Fill up data
        if (phoneNumber.isPrimary() || mContactPhones.size() == 1) {
            viewHolder.primaryNumber.setImageDrawable(ContextCompat.getDrawable(viewHolder.primaryNumber.getContext(), R.drawable.ic_contact_details_default_on));
        } else {
            viewHolder.primaryNumber.setImageDrawable(ContextCompat.getDrawable(viewHolder.primaryNumber.getContext(), R.drawable.ic_contact_details_default_off));
        }

        final boolean editAllowed;
        editAllowed = !phoneNumber.getType().equals(ContactData.PhoneType.HANDLE);

        viewHolder.phoneType.setText(phoneTypes[phoneNumber.getType().getValue()]);
        viewHolder.phoneNumber.setText(phoneNumber.getNumber());
        if (!editAllowed) {
            viewHolder.phoneNumber.setEnabled(false);
            viewHolder.phoneDelete.setVisibility(View.INVISIBLE);
            viewHolder.phoneClear.setVisibility(View.INVISIBLE);
        } else {
            viewHolder.phoneNumber.setEnabled(true);
            viewHolder.phoneDelete.setVisibility(View.VISIBLE);
            viewHolder.phoneClear.setVisibility(View.VISIBLE);
        }

        if (editAllowed) {
            viewHolder.phoneClear.setOnClickListener(view -> viewHolder.phoneNumber.setText(""));
        } else {
            viewHolder.phoneClear.setOnClickListener(null);
        }
        viewHolder.primaryNumber.setOnClickListener(view -> {
            for (int i = 0; i < mContactPhones.size(); i++) {
                mContactPhones.get(i).setPrimary(false);
            }
            mContactPhones.get(position).setPrimary(true);
            notifyDataSetChanged();
        });
        viewHolder.phoneNumber.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                viewWithFocusPosition = -1;
                viewHolder.phoneClear.setVisibility(View.INVISIBLE);
                if (mContactPhones.size() > position) {
                    mContactPhones.get(position).setNumber(viewHolder.phoneNumber.getText().toString());
                }
            } else {
                viewWithFocusPosition = position;
                viewHolder.phoneClear.setVisibility(View.VISIBLE);
            }
        });

        if (position == getCount() - 1) {
            new Handler().postDelayed(() -> {
                if (mPhoneChangedInterface != null) {
                    mPhoneChangedInterface.requestFocus(viewHolder.phoneNumber);
                }
            }, 200);
        }

        if (editAllowed) {
            viewHolder.phoneDelete.setOnClickListener(view -> {
                mPhoneChangedInterface.itemRemoved();
                mContactPhones.remove(position);
                notifyDataSetChanged();

            });
        } else {
            viewHolder.phoneDelete.setOnClickListener(null);
        }

        if (editAllowed) {
            viewHolder.phoneType.setOnClickListener(v -> {
                int[] location = new int[2];
                viewHolder.phoneType.getLocationOnScreen(location);
                mPhoneChangedInterface.phoneTypeClicked(location[1], position);
            });
        } else {
            viewHolder.phoneType.setOnClickListener(null);
        }

        return convertView;
    }

    /**
     * Return list position of focused phone number edittext view.
     *
     * @return -1 if non phone number is focused else list position
     */
    public int getViewPositionWithFocus() {
        return viewWithFocusPosition;
    }

    /**
     * sets phone type according to selection
     *
     * @param itemPosition item position in the list
     * @param phoneType    phone type to be set
     */
    public void setPhoneType(int itemPosition, ContactData.PhoneType phoneType) {
        mContactPhones.get(itemPosition).setType(phoneType);
        notifyDataSetChanged();
    }

    /**
     * Checks whether user has set any primary phone number
     *
     * @return boolean that represents if primary number exists
     */
    public boolean primaryPhoneExist() {
        boolean exist = false;

        // in case user has only one phone number, no need to force user to set primary
        if (mContactPhones.size() == 1) {
            return true;
        }

        // if there are multiple numbers, check if primary has been set
        for (int i = 0; i < mContactPhones.size(); i++) {
            if (mContactPhones.get(i).isPrimary()) {
                exist = true;
            }
        }
        return exist;
    }

    public void clearAllExceptFirst() {
        EditablePhoneNumber firstPhoneNumber = mContactPhones.size() > 0 ? mContactPhones.get(0) : null;
        mContactPhones.clear();
        if (firstPhoneNumber != null) {
            firstPhoneNumber.setType(ContactData.PhoneType.WORK);
            mContactPhones.add(firstPhoneNumber);
        }
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class
     */
    private class ViewHolder {
        final EditText phoneNumber;
        final ImageView primaryNumber;
        final ImageView phoneClear;
        final ImageView phoneDelete;
        final TextView phoneType;

        private ViewHolder(View view) {
            primaryNumber = view.findViewById(R.id.contact_edit_phone_default);
            phoneNumber = view.findViewById(R.id.contact_edit_phone_number);
            phoneClear = view.findViewById(R.id.contact_edit_phone_clear);
            phoneDelete = view.findViewById(R.id.contact_edit_phone_delete);
            phoneType = view.findViewById(R.id.phoneTypeText);
            phoneNumber.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s != null && s.toString().trim().length() > 0) {
                        mPhoneChangedInterface.phoneNumberValid(true);
                    } else {
                        mPhoneChangedInterface.phoneNumberValid(false);
                    }
                }
            });
        }
    }

}