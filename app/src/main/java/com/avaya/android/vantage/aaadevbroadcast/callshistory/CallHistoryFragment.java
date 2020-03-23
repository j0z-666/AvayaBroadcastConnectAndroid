package com.avaya.android.vantage.aaadevbroadcast.callshistory;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.avaya.android.vantage.aaadevbroadcast.Constants;
import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;
import com.avaya.android.vantage.aaadevbroadcast.bluetooth.PairedDeviceSyncHelper;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.model.CallData;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimation;

import java.util.List;
import java.util.Objects;


public class CallHistoryFragment extends Fragment implements View.OnClickListener, CallHistoryFragmentPresenter.ViewCallback, CallHistoryRecyclerViewAdapter.CallHistoryAdapterListener {
    private final static String TAG = "CallHistoryFragment";

    private static final String ADD_PARTICIPANT = "addParticipant";
    private static final String ARG_COLUMN_COUNT = "column-count";
    private static CallData.CallCategory sCallCategory = CallData.CallCategory.ALL;
    private TextView mRecentFilter;
    private TextView mNoRecentCalls;
    private RecyclerView mRecentList;
    private View mFragmentRecentCallsHeader;
    private View mLineMenu;
    private LinearLayout mDeletePopUp;
    private LinearLayout mHistoryFilter;
    private ImageView mAllCheck, mMissedCallsCheck, mOutCheck, mInCheck;
    private View mDeleteAllLogsItem;
    private OnFilterCallsInteractionListener mFilterCallsInteractionListener;
    private SlideAnimation mFilterSlider;
    private Handler mHandler;
    private Runnable mLayoutCloseRunnable;
    private boolean isFilterMenuExpanded = false;
    private boolean addParticipant;
    private CallHistoryFragmentPresenter mCallHistoryFragmentPresenter;
    private PairedDeviceSyncHelper pairedDeviceSyncHelper;
    private DeleteLogHelper deleteLogHelper;
    private OnContactInteractionListener mContactInteractionListener;
    private CallHistoryRecyclerViewAdapter callsRecyclerViewAdapter;

    public static CallHistoryFragment newInstance(int columnCount, boolean addParticipant) {
        CallHistoryFragment fragment = new CallHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putBoolean(ADD_PARTICIPANT, addParticipant);
        fragment.setArguments(args);
        return fragment;
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
        View root = inflater.inflate(R.layout.fragment_recent_call_list, container, false);

        bindViews(root);

        mFilterSlider = ElanApplication.getDeviceFactory().getSlideAnimation();

        pairedDeviceSyncHelper = new PairedDeviceSyncHelper(Objects.requireNonNull(getActivity()), PairedDeviceSyncHelper.CALL_LOG_TYPE);
        pairedDeviceSyncHelper.bindViews(root);
        pairedDeviceSyncHelper.getSyncContactsView().setOnClickListener(this);

        handleLandscapeVisibility();

        mCallHistoryFragmentPresenter =
                new CallHistoryFragmentPresenter(getActivity(), this, pairedDeviceSyncHelper);
        deleteLogHelper = new DeleteLogHelper(mCallHistoryFragmentPresenter);
        deleteLogHelper.bindViews(root);

        mFilterSlider.reDrawListener(mHistoryFilter);

        //init landscape
        mHandler = new Handler();
        mLayoutCloseRunnable = this::run;
        mLayoutCloseRunnable = this::hideMenus;

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
        performSelectionByCategory(sCallCategory);

        if (isAdded() && addParticipant) {
            mLineMenu.setVisibility(View.GONE);
            mDeleteAllLogsItem.setVisibility(View.GONE);
        }

        enableDeleteCallLogs();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mCallHistoryFragmentPresenter != null) {
            mCallHistoryFragmentPresenter.destroy();
        }

        pairedDeviceSyncHelper.dismissAlertDialog();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnContactInteractionListener) {
            mContactInteractionListener = (OnContactInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContactInteractionListener");
        }

        if (context instanceof OnFilterCallsInteractionListener) {
            mFilterCallsInteractionListener = (OnFilterCallsInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFilterCallsInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkListCount();
        pairedDeviceSyncHelper.prepareSyncIcon();

        if (callsRecyclerViewAdapter != null) {
            callsRecyclerViewAdapter.setFirstNameFirst(ContactsFragment.isFirstNameFirst());
        }

        mCallHistoryFragmentPresenter.updateLocalCallLogs();

        if (isAdded() && (getActivity() != null)) {
            ((BaseActivity) getActivity()).checkFilterButtonState();
        }
    }

    public void refreshMatcherData() {
        mCallHistoryFragmentPresenter.refreshMatcherData();
    }

    public void PBAPRefreshState() {
        pairedDeviceSyncHelper.updateSyncStatus();
        if (pairedDeviceSyncHelper.isConnectedAndEnabled()) {
            mCallHistoryFragmentPresenter.restartPairedDeviceLoader();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.filterRecent:
                handleFilterMenu();
                return;
            case R.id.containerAllHistory:
                performSelectionByCategory(CallData.CallCategory.ALL);
                break;
            case R.id.containerMissedCalls:
                performSelectionByCategory(CallData.CallCategory.MISSED);
                break;
            case R.id.containerOugtoingCalls:
                performSelectionByCategory(CallData.CallCategory.OUTGOING);
                break;
            case R.id.containerIncomingCalls:
                performSelectionByCategory(CallData.CallCategory.INCOMING);
                break;
            case R.id.containerDeleteAllHistory:
                mDeletePopUp.setVisibility(View.VISIBLE);
                break;
            case R.id.call_log_delete_yes:
                deleteAllItems();
                mDeletePopUp.setVisibility(View.GONE);
                break;
            case R.id.call_log_delete_no:
                mDeletePopUp.setVisibility(View.GONE);
                break;
            case R.id.containerHistoryBT:
            case R.id.sync_contacts:
                Utils.SyncState syncState = pairedDeviceSyncHelper.syncPairedDevice();
                if (syncState == Utils.SyncState.SYNC_OFF) {
                    mCallHistoryFragmentPresenter.removePairedDeviceLogs();
                } else if (syncState == Utils.SyncState.SYNC_ON) {
                    mCallHistoryFragmentPresenter.addPairedDeviceLogs();
                }
                break;
            default:
                break;
        }

        isFilterMenuExpanded = false;
        hideMenus();
    }

    private void handleFilterMenu() {
        if (!isFilterMenuExpanded) {
            mRecentFilter.setSelected(true);
            mFilterSlider.expand(mHistoryFilter);
            pairedDeviceSyncHelper.collapseSlider();
            deleteLogHelper.collapseDialog();
            mHandler.postDelayed(mLayoutCloseRunnable, Constants.LAYOUT_DISAPPEAR_TIME);
            isFilterMenuExpanded = true;
        } else {
            hideMenus();
        }
    }

    public boolean isFilterMenuExpanded() {
        return isFilterMenuExpanded;
    }

    @Override
    public void onCallDataChanged(List<CallData> callDataList) {
        if (callsRecyclerViewAdapter == null) {
            callsRecyclerViewAdapter = new CallHistoryRecyclerViewAdapter(callDataList, getContext(), mContactInteractionListener, this);
            callsRecyclerViewAdapter.setAddParticipant(addParticipant);
            LinearLayoutManager linearLayoutManager = new NoPredictiveAnimations(getContext());
            mRecentList.setLayoutManager(linearLayoutManager);
        }

        mRecentList.swapAdapter(callsRecyclerViewAdapter, false);
        callsRecyclerViewAdapter.setLogItems(callDataList);
        enableDeleteCallLogs();
        checkListCount();
    }

    @Override
    public void remoteItemAtPosition(final int position) {
        callsRecyclerViewAdapter.notifyItemRemoved(position);
    }

    @Override
    public void onSearchCountChanged() {
        checkListCount();
    }

    @Override
    public void onItemLongClicked(CallData item, RecyclerView.ViewHolder viewHolder) {
        if (item.isFromPaired) {
            Toast.makeText(getContext(), "Logs that are imported from the Paired device can not be deleted!", Toast.LENGTH_SHORT).show();
        } else {
            deleteLogHelper.expandDialog(item, viewHolder);
        }
    }

    /**
     * Set recent call log list based on current call category
     *
     * @param callDataCategory Current call data category
     */
    public void performSelectionByCategory(CallData.CallCategory callDataCategory) {
        if (callDataCategory == null) {
            callDataCategory = CallData.CallCategory.ALL;
        }
        setFilterItemChecked(callDataCategory);
        callsRecyclerViewAdapter = (CallHistoryRecyclerViewAdapter) mRecentList.getAdapter();
        if (callsRecyclerViewAdapter != null) {
            callsRecyclerViewAdapter.getFilter().filter(callDataCategory.toString());
        }
        mFilterCallsInteractionListener.onSaveSelectedCategoryRecentFragment(callDataCategory);
    }

    /**
     * Restore list position from {@link Parcelable}
     *
     * @param position
     */
    public void restoreListPosition(Parcelable position) {
        if (mRecentList != null && position != null) {
            Objects.requireNonNull(mRecentList.getLayoutManager()).onRestoreInstanceState(position);
        }
    }

    /**
     * This method will be called every time RecentCalls fragment is active.
     * Can be called even when application is being recreated so we need to
     * check if activity still exist.
     */
    public void fragmentSelected() {
        Log.e(TAG, "fragmentSelected: Recent");
        if (getActivity() != null) {
            pairedDeviceSyncHelper.prepareSyncIcon();
        }

        if (mCallHistoryFragmentPresenter != null) {
            mCallHistoryFragmentPresenter.checkIfHistoryLoaded();
        }
    }

    /**
     * Setting up is view open for adding participant from join conference call
     *
     * @param add
     */
    public void setAddParticipantData(boolean add) {
        addParticipant = add;
        if (callsRecyclerViewAdapter != null && mRecentList != null) {
            callsRecyclerViewAdapter.setAddParticipant(addParticipant);
            callsRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    public void callTableUpdated() {
        mCallHistoryFragmentPresenter.updateLocalCallLogs();
    }

    /**
     * Get list position for {@link CallHistoryFragment} in {@link Parcelable}
     *
     * @return {@link Parcelable}
     */
    public Parcelable getListPosition() {
        if (mRecentList != null) {
            return Objects.requireNonNull(mRecentList.getLayoutManager()).onSaveInstanceState();
        } else {
            return null;
        }
    }

    /**
     * This method is called when an incoming call arrives
     * so that the fragment adjust its state.
     */
    public void handleIncomingCall() {
        pairedDeviceSyncHelper.dismissAlertDialog();
    }

    private void setFilterItemChecked(CallData.CallCategory callDataCategory) {
        sCallCategory = callDataCategory;

        mRecentFilter.setText(getText(callDataCategory.getResourceId()));
        mAllCheck.setVisibility(callDataCategory == CallData.CallCategory.ALL ? View.VISIBLE : View.GONE);
        mInCheck.setVisibility(callDataCategory == CallData.CallCategory.INCOMING ? View.VISIBLE : View.GONE);
        mOutCheck.setVisibility(callDataCategory == CallData.CallCategory.OUTGOING ? View.VISIBLE : View.GONE);
        mMissedCallsCheck.setVisibility(callDataCategory == CallData.CallCategory.MISSED ? View.VISIBLE : View.GONE);
    }

    private void deleteAllItems() {
        mCallHistoryFragmentPresenter.deleteAllCallLogs();
        checkListCount();

        hideMenus();
        mFilterCallsInteractionListener.refreshHistoryIcon();
    }

    /**
     * Checking if there is any data in the list. If not,
     * showing a message there there are no calls.
     */
    private void checkListCount() {
        if (callsRecyclerViewAdapter != null && mNoRecentCalls != null) {
            if (callsRecyclerViewAdapter.getCachedItemCount() == 0) {
                mNoRecentCalls.setVisibility(View.VISIBLE);
            } else {
                mNoRecentCalls.setVisibility(View.GONE);
            }
        }
    }

    public void hideMenus() {
        mRecentFilter.setSelected(false);
        isFilterMenuExpanded = false;

        mHandler.removeCallbacks(mLayoutCloseRunnable);
        mFilterSlider.collapse(mHistoryFilter);
        deleteLogHelper.collapseDialog();
        mRecentFilter.setSelected(false);

        if (isAdded() && Utils.isLandScape() && getActivity() != null) {
            ((BaseActivity) getActivity()).checkFilterButtonState();
        }
    }

    private void handleLandscapeVisibility() {
        final int visibility = isAdded() && getResources().getBoolean(R.bool.is_landscape) ? View.GONE : View.VISIBLE;
        mFragmentRecentCallsHeader.setVisibility(visibility);
        mRecentFilter.setVisibility(visibility);
    }

    /**
     * Set all check views to INVISIBLE
     */
    private void bindViews(View root) {
        mFragmentRecentCallsHeader = root.findViewById(R.id.fragment_recent_calls_header);
        mRecentList = root.findViewById(R.id.list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecentList.setLayoutManager(layoutManager);

        mRecentFilter = root.findViewById(R.id.filterRecent);
        mDeletePopUp = root.findViewById(R.id.delete_all_call_history);
        TextView mDeleteConfirm = root.findViewById(R.id.call_log_delete_yes);
        TextView mDeleteCancel = root.findViewById(R.id.call_log_delete_no);
        mDeleteConfirm.setOnClickListener(this);
        mDeleteCancel.setOnClickListener(this);

        mRecentFilter = root.findViewById(R.id.filterRecent);
        mRecentFilter.setOnClickListener(this);

        mNoRecentCalls = root.findViewById(R.id.noRecentCalls);
        mNoRecentCalls.setVisibility(View.GONE);

        if (Utils.isLandScape()) {
            View landSyncLogs = root.findViewById(R.id.containerHistoryBT);
            landSyncLogs.setOnClickListener(this);
        }

        bindRecentFilterViews(root);
    }


    private void bindRecentFilterViews(View root) {
        mHistoryFilter = root.findViewById(R.id.select_history_filter);

        root.findViewById(R.id.containerAllHistory).setOnClickListener(this);
        root.findViewById(R.id.containerMissedCalls).setOnClickListener(this);
        root.findViewById(R.id.containerOugtoingCalls).setOnClickListener(this);
        root.findViewById(R.id.containerIncomingCalls).setOnClickListener(this);

        mLineMenu = root.findViewById(R.id.line_menu);

        mDeleteAllLogsItem = root.findViewById(R.id.containerDeleteAllHistory);
        mDeleteAllLogsItem.setEnabled(false);
        mDeleteAllLogsItem.setOnClickListener(this);

        mAllCheck = root.findViewById(R.id.recentAllCheck);
        mMissedCallsCheck = root.findViewById(R.id.recentMissedCheck);
        mOutCheck = root.findViewById(R.id.recentOutCheck);
        mInCheck = root.findViewById(R.id.recentIncomingCheck);
    }

    private void run() {
        hideMenus();
        if (isAdded() && Utils.isLandScape() && getActivity() != null) {
            ((BaseActivity) getActivity()).filterButton.setImageResource(R.drawable.ic_expand_more);
            ((BaseActivity) getActivity()).showingFirstRecent = true;
        }
    }

    private void enableDeleteCallLogs() {
        if (!isAdded() && mDeleteAllLogsItem == null) {
            return;
        }

        if (mCallHistoryFragmentPresenter == null || mCallHistoryFragmentPresenter.getServerLogs().isEmpty()) {
            mDeleteAllLogsItem.setEnabled(false);
        } else {
            mDeleteAllLogsItem.setEnabled(true);
        }
    }
}