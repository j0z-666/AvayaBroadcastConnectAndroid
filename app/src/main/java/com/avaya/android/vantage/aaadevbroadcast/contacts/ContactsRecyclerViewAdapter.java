package com.avaya.android.vantage.aaadevbroadcast.contacts;

import android.content.Context;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SectionIndexer;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.avaya.android.vantage.aaadevbroadcast.GoogleAnalyticsUtils;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.RemoveSearchResultsContactsFragmentInterface;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContactsRecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> implements ViewHolder.ParentAdapter,
        Filterable, SectionIndexer {

    public static final String PBAP_ACCOUNT = "com.android.bluetooth.pbapsink";
    private final Context mContext;
    private final OnContactInteractionListener mContactInteractionListener;
    private final Handler mHandler = new Handler();
    private final TextAppearanceSpan mHighlightTextSpan;
    private final RemoveSearchResultsContactsFragmentInterface mRemoveSearchResultsContactsFragmentInterface;
    private List<ContactData> items;
    private List<ContactData> filteredItems;
    private List<ContactData> mSearchDirectoryItems = new ArrayList<>();
    private boolean mBlockedClick = false;
    private boolean mAddParticipant = false;
    private CharSequence mFilterConstraint = ContactData.Category.ALL.toString();
    private ContactsTypeFilter contactsTypeFilter;
    private boolean mFirstNameFirst;
    private String mSearchTerm = null;
    private List<Integer> mSectionPositions;

    ContactsRecyclerViewAdapter(List<ContactData> items, Context context,
                                RemoveSearchResultsContactsFragmentInterface removeSearchResultsContactsFragmentInterface,
                                OnContactInteractionListener contactInteractionListener) {
        this.items = items;
        this.mContext = context;
        mHighlightTextSpan = new TextAppearanceSpan(context, R.style.searchTextHiglight);
        this.mRemoveSearchResultsContactsFragmentInterface = removeSearchResultsContactsFragmentInterface;
        this.mContactInteractionListener = contactInteractionListener;

        this.filteredItems = items;

        mFirstNameFirst = ContactsFragment.isFirstNameFirst();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_list_item, parent, false);
        return new ViewHolder(view, mAddParticipant, false);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        // Getting contact data
        ContactData data;
        if (position < filteredItems.size()) {
            data = filteredItems.get(position);
        } else {
            data = mSearchDirectoryItems.get(position - filteredItems.size());
        }

        String displayName;
        if (data.mCategory == ContactData.Category.IPO) {
            displayName = data.mFirstName;
        } else {
            displayName = data.getFormatedName(mFirstNameFirst);
        }

        if (data.isHeader()) {
            holder.mDirectoryInfo.setText(getContext().getString(R.string.directory_separator_display, data.mDirectoryName));
            holder.mDirectoryInfo.setVisibility(View.VISIBLE);
        } else {
            holder.mDirectoryInfo.setText("");
            holder.mDirectoryInfo.setVisibility(View.GONE);
        }

        // highlighting searched text
        final int startIndex = indexOfSearchQuery(displayName);
        if (startIndex == -1) {
            holder.mName.setText(displayName);
        } else {
            final SpannableString highlightedName = new SpannableString(displayName);
            highlightedName.setSpan(mHighlightTextSpan, startIndex,
                    startIndex + mSearchTerm.length(), 0);
            holder.mName.setText(highlightedName);
        }

        holder.mLocation.setText(data.mCity);
        holder.bind(data, mFirstNameFirst, this);
        holder.mFavorite.setVisibility(data.isFavorite() ? View.VISIBLE : View.INVISIBLE);
        holder.mSyncContact.setVisibility(data.mAccountType != null && data.mAccountType.equals(PBAP_ACCOUNT) ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return filteredItems.size() + mSearchDirectoryItems.size();
    }

    @Override
    public OnContactInteractionListener getContactInteractionListener() {
        return mContactInteractionListener;
    }

    @Override
    public Handler getHandler() {
        return mHandler;
    }

    @Override
    public boolean isBlockedClick() {
        return mBlockedClick;
    }

    @Override
    public void setBlockedClick(boolean blockedClick) {
        mBlockedClick = blockedClick;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public void setAddParticipant(boolean addParticipant) {
        mAddParticipant = addParticipant;
    }

    @Override
    public void refreshData() {
        notifyDataSetChanged();
        if (mContactInteractionListener != null) {
            mContactInteractionListener.checkFilterVisibility();
        }
    }

    @Override
    public void removeSearchResults() {
        mRemoveSearchResultsContactsFragmentInterface.removeSearchResults();
    }

    @Override
    public Filter getFilter() {
        if (contactsTypeFilter == null) {
            contactsTypeFilter = new ContactsTypeFilter();
        }
        return contactsTypeFilter;
    }

    @Override
    public Object[] getSections() {
        List<String> sections = new ArrayList<>(26);
        mSectionPositions = new ArrayList<>(26);
        if (filteredItems != null) {
            for (int i = 0, size = filteredItems.size(); i < size; ++i) {
                char firstLetter;
                if (mFirstNameFirst && filteredItems.get(i).mFirstName.length() > 0) {
                    firstLetter = filteredItems.get(i).mFirstName.charAt(0);
                } else if (!mFirstNameFirst && filteredItems.get(i).mLastName.length() > 0) {
                    firstLetter = filteredItems.get(i).mLastName.charAt(0);
                } else {
                    firstLetter = filteredItems.get(i).mName.charAt(0);
                }
                String section = String.valueOf(firstLetter).toUpperCase();
                if (!sections.contains(section)) {
                    sections.add(section);
                    mSectionPositions.add(i);
                }
            }
        }
        return sections.toArray(new String[0]);
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (mSectionPositions.size() > 0) {
            return mSectionPositions.get(sectionIndex);
        } else {
            return 0;
        }
    }

    @Override
    public int getSectionForPosition(int i) {
        return 0;
    }

    public void setItems(List<ContactData> items) {
        this.items = items;
        doFilter();
    }

    void setFirstNameFirst(boolean mFirstNameFirst) {
        if (this.mFirstNameFirst ^ mFirstNameFirst) {
            this.mFirstNameFirst = mFirstNameFirst;
            notifyDataSetChanged();
        }
    }

    void disableBlockClick() {
        mBlockedClick = false;
    }

    void setSearchDirectoryItems(List<ContactData> items) {
        if (items != null) {
            mSearchDirectoryItems = items;
        }
        notifyDataSetChanged();
    }

    void clearSearch() {
        mSearchTerm = "";
        mSearchDirectoryItems = new ArrayList<>();
        this.filteredItems = items;
        getFilter().filter(mSearchTerm);
    }

    void setSearchTerm(String term) {
        mSearchTerm = term.toLowerCase();
        getFilter().filter(mSearchTerm);
    }

    /**
     * Identifying start of search query
     *
     * @param displayName display name of the contact
     * @return starting position of a search string
     */
    private int indexOfSearchQuery(String displayName) {
        if (!TextUtils.isEmpty(mSearchTerm)) {
            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.SEARCH_CONTACTS_EVENT);
            return displayName.toLowerCase(Locale.getDefault()).indexOf(
                    mSearchTerm.toLowerCase(Locale.getDefault()));
        }
        return -1;
    }

    private void doFilter() {
        if (mFilterConstraint != null) {
            getFilter().filter(mFilterConstraint);
        } else {
            this.filteredItems = items;
        }
    }

    private class ContactsTypeFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();

            if (isFilteringByContactType(constraint)) {
                mFilterConstraint = constraint;
            }

            List<ContactData> filteredByTpeList = new ArrayList<>();
            if (mFilterConstraint == null || mFilterConstraint.length() == 0 || mFilterConstraint.equals(ContactData.Category.ALL.toString())) {
                filteredByTpeList = items;
            } else if (isFilteringByContactType(mFilterConstraint)) {
                for (ContactData contactData : items) {
                    if (mFilterConstraint.equals(contactData.mCategory.toString())) {
                        filteredByTpeList.add(contactData);
                    }
                }
            }

            if (mSearchTerm == null || mSearchTerm.isEmpty()) {
                filterResults.count = filteredByTpeList.size();
                filterResults.values = filteredByTpeList;
            } else {
                final boolean containsDigits = mSearchTerm.matches("[0-9]+");
                List<ContactData> filterList = new ArrayList<>();
                for (ContactData contactData : filteredByTpeList) {
                    boolean foundByName = false;
                    // Check if name matches
                    String name = contactData.mFirstName + " " + contactData.mLastName;
                    if (name.toLowerCase().contains(mSearchTerm)) {
                        filterList.add(contactData);
                        foundByName = true;
                    }
                    // Check if number matches if not found by name and search query contains digits
                    if (containsDigits && !foundByName) {
                        for (ContactData.PhoneNumber number : contactData.mPhones) {
                            assert number.Number != null;
                            if (number.Number.contains(mSearchTerm)) {
                                filterList.add(contactData);
                                break;
                            }
                        }
                    }
                }

                filterResults.count = filterList.size();
                filterResults.values = filterList;
            }

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            if (filterResults.values != null) {
                filteredItems = (List<ContactData>) filterResults.values;
                mRemoveSearchResultsContactsFragmentInterface.onSearchCountChanged(getItemCount());
                notifyDataSetChanged();
            }
        }

        private boolean isFilteringByContactType(CharSequence constraint) {
            return constraint.equals((ContactData.Category.LOCAL.toString()))
                    || constraint.equals((ContactData.Category.ENTERPRISE.toString()))
                    || constraint.equals((ContactData.Category.PAIRED.toString()))
                    || constraint.equals((ContactData.Category.DIRECTORY.toString()))
                    || constraint.equals((ContactData.Category.IPO.toString()))
                    || constraint.equals((ContactData.Category.BROADSOFT.toString()))
                    || constraint.equals((ContactData.Category.ALL.toString()));
        }
    }
}
