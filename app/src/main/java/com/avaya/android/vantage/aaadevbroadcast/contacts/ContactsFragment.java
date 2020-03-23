package com.avaya.android.vantage.aaadevbroadcast.contacts;


import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;
import com.avaya.android.vantage.aaadevbroadcast.adaptors.RemoveSearchResultsContactsFragmentInterface;
import com.avaya.android.vantage.aaadevbroadcast.bluetooth.PairedDeviceSyncHelper;
import com.avaya.android.vantage.aaadevbroadcast.callshistory.CallHistoryFragment;
import com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.views.FastScrollRecyclerView;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.avaya.android.vantage.aaadevbroadcast.Constants.FIRST_NAME_FIRST;
import static com.avaya.android.vantage.aaadevbroadcast.Constants.LAST_NAME_FIRST;
import static com.avaya.android.vantage.aaadevbroadcast.csdk.ConfigParametersNames.ENABLE_IPOFFICE;

abstract public class ContactsFragment extends Fragment implements ContactsFragmentPresenter.ViewCallback, View.OnClickListener,
        RemoveSearchResultsContactsFragmentInterface {

    private static final String TAG = ContactsFragment.class.getSimpleName();
    private static final String ADD_PARTICIPANT = "addParticipant";
    /* Field below is made static to facilitate least-code-changes search term preservation.
     * One of the issues that needs to be addressed separately is extraordinary frequent
     * fragment instance creation, which requires task in itself.
     */
    public static String sSearchQuery = "";
    private static ContactData.Category mPreviousFilterSelection = ContactData.Category.ALL;
    final String enableContactEdit = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ENABLE_MODIFY_CONTACTS);
    public SearchView mSearchView;
    public LinearLayout searchLayout;
    public ImageView mAdd;
    boolean addParticipant;
    FastScrollRecyclerView mRecycleView;
    TextView mFilterView;
    LinearLayout mContactsBluetoothSyncLinear;
    PairedDeviceSyncHelper pairedDeviceSyncHelper;
    private boolean mIpoEnabled;
    private boolean mOpenSipEnabled;
    private boolean mUserVisibleHint;
    private SwipeRefreshLayout mSwipeRefresh;
    private TextView mEmptyView;
    private LinearLayout mContactsFilterLayout;
    private SlideAnimation mFilterAnimation;
    private ImageView mSelectedAllImage;
    private ImageView mSelectedEnterpriseImage;
    private ImageView mSelectedLocalImage;
    private FrameLayout frameContactsAll;
    private TextView mSelectedAllText, mSelectedEnterpriseText, mSelectedLocalText;
    private OnContactInteractionListener mContactInteractionListener;
    private ContactsFragmentPresenter mContactsFragmentPresenter;
    private ContactsRecyclerViewAdapter contactsRecyclerViewAdapter;
    private ProgressBar mSearchLoader;

    public ContactsFragment() {
    }

    public static ContactsFragment newInstance(boolean addParticipant) {
        ContactsFragment fragment = ElanApplication.getDeviceFactory().getContactsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ADD_PARTICIPANT, addParticipant);
        fragment.setArguments(args);
        return fragment;
    }

    public static boolean isFirstNameFirst() {
        Context context = ElanApplication.getContext();

        String adminNameDisplayOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);
        int defaultValue = FIRST_NAME_FIRST;
        if (adminNameDisplayOrder != null) {
            // doing this to prevent bug in case someone entered a value that is different from 1 or 0.
            if (adminNameDisplayOrder.equals("last,first")) {
                defaultValue = LAST_NAME_FIRST;
            }
        }

        if (context == null) {
            return defaultValue == FIRST_NAME_FIRST;
        }

        SharedPreferences sp = context.getSharedPreferences(Constants.USER_PREFERENCE, Context.MODE_PRIVATE);
        int nameDisplay = sp.getInt(Constants.NAME_DISPLAY_PREFERENCE, defaultValue);
        return nameDisplay == FIRST_NAME_FIRST;
    }

    public static boolean shouldSortByFirstName() {
        Context context = ElanApplication.getContext();

        String adminNameSortOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);
        int defaultValue = FIRST_NAME_FIRST;
        if (adminNameSortOrder != null) {
            // doing this to prevent bug in case someone entered a value that is different from 1 or 0.
            if (adminNameSortOrder.equals("last,first")) {
                defaultValue = LAST_NAME_FIRST;
            }
        }

        if (context == null) {
            return defaultValue == FIRST_NAME_FIRST;
        }

        SharedPreferences sp = context.getSharedPreferences(Constants.USER_PREFERENCE, Context.MODE_PRIVATE);
        int nameSort = sp.getInt(Constants.NAME_SORT_PREFERENCE, defaultValue);
        return nameSort == FIRST_NAME_FIRST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            addParticipant = getArguments().getBoolean(ADD_PARTICIPANT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contacts_list, container, false);
        mIpoEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ENABLE_IPOFFICE);
        mOpenSipEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().isOpenSipEnabled();

        bindViews(root);

        pairedDeviceSyncHelper = new PairedDeviceSyncHelper(Objects.requireNonNull(ElanApplication.getContext()), PairedDeviceSyncHelper.CONTACTS_TYPE);
        pairedDeviceSyncHelper.bindViews(root);
        pairedDeviceSyncHelper.setOnClickListener(this);

        mContactsFragmentPresenter = new ContactsFragmentPresenter(this, pairedDeviceSyncHelper);

        initSearch(root);

        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideMenus();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
                return true;
            }

            return false;
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPref = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        boolean value = sharedPref.getBoolean("is_return_ro_search_land", false);
        if (value) {
            mAdd.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mContactsFragmentPresenter != null) {
            mContactsFragmentPresenter.destroy();
        }

        if (pairedDeviceSyncHelper != null) {
            pairedDeviceSyncHelper.dismissAlertDialog();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnContactInteractionListener) {
            mContactInteractionListener = (OnContactInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContactsFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mContactsFragmentPresenter != null) {
            mContactsFragmentPresenter.setSortByFirstName(shouldSortByFirstName());
        }

        if (contactsRecyclerViewAdapter != null) {
            contactsRecyclerViewAdapter.setFirstNameFirst(isFirstNameFirst());
        }
        performSelectionByCategory(mPreviousFilterSelection);
        pairedDeviceSyncHelper.prepareSyncIcon();
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        mUserVisibleHint = menuVisible;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.filter:
                if (mContactsFilterLayout.getVisibility() == View.VISIBLE) {
                    mFilterAnimation.collapse(mContactsFilterLayout/*, Utils.isLandScape()*/);
                    mFilterView.setSelected(false);
                } else {
                    mFilterAnimation.expand(mContactsFilterLayout/*, Utils.isLandScape()*/);
                    frameContactsAll.setVisibility(View.VISIBLE);
                    mFilterView.setSelected(true);
                }
                break;
            case R.id.contacts_all_linear:
                performSelectionByCategory(ContactData.Category.ALL);
                mPreviousFilterSelection = ContactData.Category.ALL;
                break;
            case R.id.contacts_enterprise_linear:
                if (mOpenSipEnabled) {
                    mPreviousFilterSelection = ContactData.Category.BROADSOFT;
                } else {
                    mPreviousFilterSelection = ContactData.Category.ENTERPRISE;
                }
                performSelectionByCategory(mPreviousFilterSelection);
                break;
            case R.id.contacts_local_linear:
                performSelectionByCategory(ContactData.Category.LOCAL);
                mPreviousFilterSelection = ContactData.Category.LOCAL;
                break;
            case R.id.add:
                mContactInteractionListener.onCreateNewContact();
                break;
            case R.id.contacts_bluetooth_sync_linear:
            case R.id.sync_contacts:
                Utils.SyncState syncState = pairedDeviceSyncHelper.syncPairedDevice();
                if (syncState == Utils.SyncState.SYNC_OFF) {
                    mContactsFragmentPresenter.removePairedDeviceContacts();
                } else if (syncState == Utils.SyncState.SYNC_ON) {
                    mContactsFragmentPresenter.addPairedDeviceContacts();
                }

                break;
            default:
                hideMenus();
        }
    }

    void UIChangesForDevice() {
    }

    @Override
    public void onContactsDataChanged(List<ContactData> contacts) {
        if (contactsRecyclerViewAdapter != null) {
            contactsRecyclerViewAdapter.setItems(contacts);
        } else {
            mRecycleView.setIndexBarTransparentValue((float) 0.2);
            contactsRecyclerViewAdapter = new ContactsRecyclerViewAdapter(contacts, getActivity(),
                    this,
                    mContactInteractionListener);
            contactsRecyclerViewAdapter.setAddParticipant(addParticipant);
            mRecycleView.setAdapter(contactsRecyclerViewAdapter);
        }
        checkListCount();
    }

    @Override
    public void onExtraSearchResults(List<ContactData> contacts) {
        if (contactsRecyclerViewAdapter != null) {
            if (sSearchQuery != null && sSearchQuery.length() > 0) {
                Log.d(TAG, "onExtraSearchResults: " + contacts.size());
                contactsRecyclerViewAdapter.setSearchDirectoryItems(contacts);
            } else {
                Log.d(TAG, "onExtraSearchResults clear");
                contactsRecyclerViewAdapter.setSearchDirectoryItems(new ArrayList<>());
                mRecycleView.swapAdapter(contactsRecyclerViewAdapter, true);
            }
        }
        checkListCount();
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        if (mSwipeRefresh != null) {
            mSwipeRefresh.setRefreshing(refreshing);
        }
    }

    @Override
    public void showLoader() {
        if (isAdded() && mSearchLoader != null) {
            mSearchLoader.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideLoader() {
        if (isAdded() && mSearchLoader != null) {
            mSearchLoader.setVisibility(View.GONE);
        }
    }

    @Override
    public void showMessage(boolean removePairedContacts) {
        if (removePairedContacts) {
            Utils.sendSnackBarData(ElanApplication.getContext(),
                    Objects.requireNonNull(ElanApplication.getContext()).getResources().getString(R.string.removing_bt_contacts), false);
        } else {
            Utils.sendSnackBarData(ElanApplication.getContext(),
                    Objects.requireNonNull(ElanApplication.getContext()).getResources().getString(R.string.adding_bt_contacts), false);
        }
    }

    @Override
    public void refreshMatcherData() {
        BaseActivity activity = ((BaseActivity) getActivity());
        if (isAdded() && activity != null && activity.mSectionsPagerAdapter.isRecentTabPresent()) {
            CallHistoryFragment fragment = activity.mSectionsPagerAdapter.getFragmentCallHistory();
            if (fragment != null) {
                fragment.refreshMatcherData();
            }
        }
    }

    @Override
    public void removeSearchResults() {
        search("");
        setSearchVisibility(View.GONE);
    }

    public void setSearchVisibility(int visibility) {
    }

    @Override
    public void onSearchCountChanged(int count) {
        if (contactsRecyclerViewAdapter != null && mEmptyView != null) {
            if (contactsRecyclerViewAdapter.getItemCount() == 0) {
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Collapse contact filter menu
     */
    public void hideMenus() {
        if (mContactsFilterLayout.getVisibility() == View.VISIBLE) {
            mFilterAnimation.collapse(mContactsFilterLayout/*, Utils.isLandScape()*/);
            frameContactsAll.setVisibility(View.GONE);
            mFilterView.setSelected(false);
            UIChangesForDevice();
        }
    }

    /**
     * Restoring list position from {@link Parcelable}
     *
     * @param position {@link Parcelable}
     */
    public void restoreListPosition(Parcelable position) {
        pairedDeviceSyncHelper.prepareSyncIcon();

        if (position != null) {
            Objects.requireNonNull(mRecycleView.getLayoutManager()).onRestoreInstanceState(position);
        }
    }

    public void contactCreated() {
        if (mContactsFragmentPresenter != null) {
            mContactsFragmentPresenter.refresh();
        }
    }

    public void userSettingsChanged() {
        if (contactsRecyclerViewAdapter != null) {
            contactsRecyclerViewAdapter.setFirstNameFirst(isFirstNameFirst());
        }
    }

    public void PBAPRefreshState() {
        if (pairedDeviceSyncHelper != null) {
            pairedDeviceSyncHelper.updateSyncStatus();
        }

        if (mContactsFragmentPresenter != null) {
            mContactsFragmentPresenter.refreshContactsFromPairedDevice();
        }
    }

    public void contactTableUpdated() {
        if (mContactsFragmentPresenter != null) {
            mContactsFragmentPresenter.refresh();
        }
    }

    /**
     * Prepare {@link ContactsFragment} for adding to existing call
     *
     * @param add boolean which mark if we are preparing for adding participants in current call
     */
    public void setAddParticipantData(boolean add) {
        addParticipant = add;
        if (contactsRecyclerViewAdapter != null && mRecycleView != null) {
            contactsRecyclerViewAdapter.setAddParticipant(addParticipant);
            contactsRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Enables click on contact list
     */
    public void unblockListClick() {
        if (contactsRecyclerViewAdapter != null) {
            contactsRecyclerViewAdapter.disableBlockClick();
        }
    }

    /**
     * Hiding contacts filter in case there are no contacts
     */
    public void checkFilterVisibility() {
        if (mFilterView != null
                && mContactsFragmentPresenter.getContacts().isEmpty()) {
            setFilterViewVisibility(View.INVISIBLE);
        } else {
            if (mFilterView != null && sSearchQuery.isEmpty()) {
                setFilterViewVisibility(View.VISIBLE);
            }
        }
    }

    abstract void setFilterViewVisibility(int visibility);

    /**
     * This method will be called every time Contacts fragment is active
     */
    public void fragmentSelected() {
        unblockListClick();
        if (pairedDeviceSyncHelper != null) {
            pairedDeviceSyncHelper.prepareSyncIcon();
        }
        checkIfContactsLoaded();
    }

    public void checkIfContactsLoaded() {
        if (mContactsFragmentPresenter != null) {
            mContactsFragmentPresenter.checkIfContactsLoaded();
        }
    }

    /**
     * Return list position as {@link Parcelable}
     *
     * @return {@link Parcelable}
     */
    public Parcelable getListPosition() {
        if (mRecycleView != null) {
            return Objects.requireNonNull(mRecycleView.getLayoutManager()).onSaveInstanceState();
        } else {
            return null;
        }
    }

    /**
     * Sets a search string of the {@link SearchView} which will then <br>
     * in OnQueryTextListener trigger actual async search.
     * This method is meant to be called as a response to Intent.ACTION_SEARCH<br>
     * parameter should be set to true in order to trigger<br>
     * {@link androidx.appcompat.widget.SearchView.OnQueryTextListener#onQueryTextChange(String)}
     *
     * @param query String set to in {@link SearchView} to search for in Contacts
     */
    public void setQuery(String query) {
        if (mSearchView != null) {
            mSearchView.requestFocus();
            mSearchView.setQuery(query, false);
        }
    }

    /**
     * Perform search based on provided query
     *
     * @param query String based on which we perform search
     */
    void search(String query) {
        sSearchQuery = query;
        if (contactsRecyclerViewAdapter != null && contactsRecyclerViewAdapter.getFilter() != null) {
            mContactsFragmentPresenter.searchQueryChange(query);
            if (query == null || query.isEmpty()) {
                mRecycleView.swapAdapter(contactsRecyclerViewAdapter, true);
                contactsRecyclerViewAdapter.clearSearch();
            } else {
                contactsRecyclerViewAdapter.setSearchTerm(query);
            }
        }
    }

    public boolean isUserVisibleHint() {
        return mUserVisibleHint;
    }

    /**
     * This method is called when an incoming call arrives
     * so that the fragment adjust its state.
     */
    public void handleIncomingCall() {
        pairedDeviceSyncHelper.dismissAlertDialog();
    }

    /**
     * Get on screen location of {@link Rect}
     *
     * @return {@link Rect}
     */
    private Rect getLocationOnScreen() {
        Rect mRect = new Rect();
        int[] location = new int[2];

        mSearchView.getLocationOnScreen(location);

        mRect.left = location[0];
        mRect.top = location[1];
        mRect.right = location[0] + mSearchView.getWidth();
        mRect.bottom = location[1] + mSearchView.getHeight();

        return mRect;
    }

    public boolean isSearchLayoutVisible() {
        return searchLayout != null && searchLayout.getVisibility() == View.VISIBLE;
    }

    /**
     * Performing search
     *
     * @param root {@link View}
     */
    private void initSearch(View root) {
        searchLayout = root.findViewById(R.id.seacrh_layout);
        setLayoutSearchVisibility();

        mSearchLoader = root.findViewById(R.id.search_loader);

        mSearchView = root.findViewById(R.id.search);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) mSearchView.getContext().getSystemService(Context.SEARCH_SERVICE);
        // Assumes current activity is the searchable activity
        if (isAdded() && getActivity() != null) {
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(Objects.requireNonNull(getActivity()).getComponentName()));
        }
        // Do not iconify the widget; expand it by default
        mSearchView.setIconifiedByDefault(false);
        EditText searchEditText = mSearchView.findViewById(R.id.search_src_text);

        setImeOptions(searchEditText);

        searchEditText.setOnFocusChangeListener((view, b) -> {
            if (b) {
                mContactInteractionListener.onStartSearching(getLocationOnScreen());
                hideMenus();
            }
        });
        searchEditText.setOnClickListener(view -> {
            mContactInteractionListener.onStartSearching(getLocationOnScreen());
            hideMenus();
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdd.setVisibility(View.GONE);
                mFilterView.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    setFilterViewVisibility(View.VISIBLE);
                    setSyncContactViewVisibility(View.VISIBLE);

                    setAddVisibility();
                } else {
                    mAdd.setVisibility(View.GONE);
                    mFilterView.setVisibility(View.GONE);
                    setSyncContactViewVisibility(View.GONE);
                }

                if (newText != null) {
                    // do a search only if we have 200ms delay between keys entered. This is used to prevent doing a search in case user is still typing
                    search(newText);
                }
                return false;
            }
        });

        searchQueryInit(searchEditText);
    }

    void searchQueryInit(EditText searchEditText) {
    }

    abstract void setLayoutSearchVisibility();

    abstract void setAddVisibility();

    void setImeOptions(EditText searchEditText) {
    }

    abstract void setSyncContactViewVisibility(int visibility);

    private void performSelectionByCategory(ContactData.Category category) {
        mSelectedAllImage.setVisibility(getVisible(category == ContactData.Category.ALL));
        mSelectedEnterpriseImage.setVisibility(getVisible(category == ContactData.Category.ENTERPRISE
                || category == ContactData.Category.IPO));
        mSelectedLocalImage.setVisibility(getVisible(category == ContactData.Category.LOCAL));

        mSelectedAllText.setTextColor(getFilterItemColor(category == ContactData.Category.ALL));
        mSelectedEnterpriseText.setTextColor(getFilterItemColor(category == ContactData.Category.ENTERPRISE
                || category == ContactData.Category.IPO || category == ContactData.Category.BROADSOFT));
        mSelectedLocalText.setTextColor(getFilterItemColor(category == ContactData.Category.LOCAL));

        mFilterView.setText(getText(category.getLabelId()));

        UIChangesForDevice();

        if (contactsRecyclerViewAdapter != null) {
            contactsRecyclerViewAdapter.getFilter().filter(category.toString());
        }

        hideMenus();
    }

    private int getVisible(boolean visible) {
        return visible ? View.VISIBLE : View.INVISIBLE;
    }

    private int getFilterItemColor(boolean isSelected) {
        return isSelected ? Objects.requireNonNull(getActivity()).getColor(R.color.midBlue) : Objects.requireNonNull(getActivity()).getColor(R.color.primary);
    }

    private void bindViews(View root) {
        mSwipeRefresh = root.findViewById(R.id.swipe_refresh);
        mRecycleView = root.findViewById(R.id.list);
        mRecycleView.setmSwipeRefreshLayout(mSwipeRefresh);
        mEmptyView = root.findViewById(R.id.empty_contacts);
        mRecycleView.setIndexBarColor("#FFFFFF");
        mRecycleView.setIndexBarTextColor("#304050");
        mRecycleView.setIndexbarWidth(40);
        mRecycleView.setAlphaBarEnabled(true);// use this to show alpha bar if needed

        mSearchView = root.findViewById(R.id.search);

        mAdd = root.findViewById(R.id.add);
        mAdd.setOnClickListener(this);
        setAddVisibility();

        mFilterView = root.findViewById(R.id.filter);
        setFilterViewClickListener();
        initFilter(root);

        initSwipeToRefresh();
    }

    abstract void setFilterViewClickListener();

    /**
     * Checking if there is any data in the list. If not,
     * showing a message there there are no calls.
     */
    private void checkListCount() {
        if (contactsRecyclerViewAdapter != null && mEmptyView != null) {
            if (contactsRecyclerViewAdapter.getItemCount() == 0) {
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Setup swipe to refresh layout.
     */
    private void initSwipeToRefresh() {
        mSwipeRefresh.setOnRefreshListener(() -> {
            if (mContactsFragmentPresenter != null && mSearchView != null) {
                if (mSearchView.getQuery().toString().equalsIgnoreCase("")) {
                    mContactsFragmentPresenter.refresh();
                    mSearchView.clearFocus();
                    //refresh list of all contacts if query is not set.
                } else {
                    mSwipeRefresh.setRefreshing(false);
                    //if query exist do not refresh
                }
            } else {
                mSwipeRefresh.setRefreshing(false);
            }
        });
    }

    private void initFilter(View root) {

        mContactsFilterLayout = root.findViewById(R.id.select_contacts_filter);
        mFilterAnimation = ElanApplication.getDeviceFactory().getSlideAnimation();

        root.findViewById(R.id.contacts_all_linear).setOnClickListener(this);
        root.findViewById(R.id.contacts_enterprise_linear).setOnClickListener(this);
        root.findViewById(R.id.contacts_local_linear).setOnClickListener(this);

        if (mIpoEnabled) {
            ((TextView) root.findViewById(R.id.contacts_enterprise)).setText(R.string.personal_directory_contacts);
        }

        mSelectedAllImage = root.findViewById(R.id.contacts_all_icon);
        mSelectedEnterpriseImage = root.findViewById(R.id.contacts_enterprise_icon);
        mSelectedLocalImage = root.findViewById(R.id.contacts_local_icon);

        mSelectedAllText = root.findViewById(R.id.contacts_all);
        mSelectedEnterpriseText = root.findViewById(R.id.contacts_enterprise);
        mSelectedLocalText = root.findViewById(R.id.contacts_local);

        setContactsBluetoothSyncLinearClickListener(root);

        frameContactsAll = root.findViewById(R.id.frameContactsAll);
        frameContactsAll.setOnClickListener(this);

        mFilterAnimation.reDrawListener(mContactsFilterLayout);
    }

    void setContactsBluetoothSyncLinearClickListener(View root) {
    }
}