package com.avaya.android.vantage.aaadevbroadcast.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragmentPresenter;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.android.vantage.aaadevbroadcast.views.EmptyRecyclerView;
import com.avaya.android.vantage.aaadevbroadcast.views.adapters.MyFavoritesRecyclerViewAdapter;

import java.util.List;
import java.util.Objects;

/**
 * Fragment that shows list of favorite user contacts.
 */
public class FavoritesFragment extends Fragment implements ContactsFragmentPresenter.ViewCallback {

    private static final String TAG = FavoritesFragment.class.getSimpleName();

    private static final String ADD_PARTICIPANT = "addParticipant";
    private MyFavoritesRecyclerViewAdapter mFavoritesRecyclerViewAdapter;
    private EmptyRecyclerView mFavoriteList;
    private TextView mFavoritesTitle;
    private OnContactInteractionListener mListener;
    private boolean addParticipant;
    private SwipeRefreshLayout mSwipeRefresh;
    private ContactsFragmentPresenter mPresenter;

    public FavoritesFragment() {
        // Default constructor
    }

    public static FavoritesFragment newInstance(boolean addParticipant) {
        FavoritesFragment fragment = new FavoritesFragment();
        Bundle args = new Bundle();
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_favorites_list, container, false);
        mFavoriteList = root.findViewById(R.id.favorite_recycler_list);
        mFavoritesTitle = root.findViewById(R.id.favorites_title);
        if (isAdded() && getResources().getBoolean(R.bool.is_landscape)) {
            mFavoritesTitle.setVisibility(View.GONE);
        } else {
            mFavoritesTitle.setVisibility(View.VISIBLE);
        }

        mPresenter = new ContactsFragmentPresenter(this, null);

        TextView emptyView = root.findViewById(R.id.empty_favorites);

        // View with message when list is empty.
        mFavoriteList.setEmptyView(emptyView);

        mSwipeRefresh = root.findViewById(R.id.favorite_swipe_layout);
        mSwipeRefresh.setOnRefreshListener(() -> {
            if (mPresenter != null) {
                mPresenter.refresh();
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        setFavoritesCount();
        if (mFavoritesRecyclerViewAdapter != null) {
            mFavoritesRecyclerViewAdapter.setSortFirstName(ContactsFragment
                    .shouldSortByFirstName());
        }

        if (mFavoritesRecyclerViewAdapter != null) {
            mFavoritesRecyclerViewAdapter.setFirstNameFirst(ContactsFragment
                    .isFirstNameFirst());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPresenter != null) {
            mPresenter.destroy();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
        mListener = null;
    }

    @Override
    public void onContactsDataChanged(List<ContactData> contacts) {
        if (mFavoritesRecyclerViewAdapter != null) {
            mFavoritesRecyclerViewAdapter.setSortFirstName(ContactsFragment.shouldSortByFirstName());
            mFavoritesRecyclerViewAdapter.setFirstNameFirst(ContactsFragment.isFirstNameFirst());
            mFavoritesRecyclerViewAdapter.setItems(contacts);
            setFavoritesCount();
            mFavoritesRecyclerViewAdapter.notifyDataSetChanged();
        } else {
            mFavoritesRecyclerViewAdapter = new MyFavoritesRecyclerViewAdapter(contacts, mListener, getContext());
            mFavoritesRecyclerViewAdapter.setAddParticipant(addParticipant);
            mFavoriteList.setAdapter(mFavoritesRecyclerViewAdapter);
            mFavoriteList.setHasFixedSize(true);
            Log.i(TAG, "Recycler view initialized");
        }
    }

    @Override
    public void onExtraSearchResults(List<ContactData> contacts) {
        // Do nothing
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        if (mSwipeRefresh != null) {
            mSwipeRefresh.setRefreshing(refreshing);
        }
    }

    @Override
    public void showLoader() {

    }

    @Override
    public void hideLoader() {

    }

    @Override
    public void showMessage(boolean removePairedContacts) {

    }

    @Override
    public void refreshMatcherData() {

    }

    /**
     * Set a number of favorite items text.
     */
    private void setFavoritesCount() {
        if (getActivity() != null) {
            if (mFavoritesRecyclerViewAdapter != null) {
                int numberOfFavorites = mFavoritesRecyclerViewAdapter.getItemCount();
                if (mFavoritesTitle != null) {
                    mFavoritesTitle.setText(getText(R.string.favorites_title) + " (" + numberOfFavorites + ")");
                }
                Log.i(TAG, "HeaderView initialized");
            } else {
                Log.e(TAG, "HeaderView cannot be initialized, adapter missing");
            }
        }
    }

    /**
     * Get list position and return it in {@link Parcelable}
     *
     * @return {@link Parcelable}
     */
    public Parcelable getListPosition() {
        if (mFavoriteList != null) {
            return Objects.requireNonNull(mFavoriteList.getLayoutManager()).onSaveInstanceState();
        } else {
            return null;
        }
    }

    /**
     * Restoring list position from {@link Parcelable}
     *
     * @param position {@link Parcelable}
     */
    public void restoreListPosition(Parcelable position) {
        if (position != null) {
            Objects.requireNonNull(mFavoriteList.getLayoutManager()).onRestoreInstanceState(position);
        }
    }

    /**
     * This method will be called every time Favorites fragment is active.
     * Can be called even when application is being recreated so we need to
     * check if activity still exist.
     */
    public void fragmentSelected() {
        if (mFavoritesRecyclerViewAdapter != null) {
            mFavoritesRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Setting up is view open for adding participant from join conference call
     *
     * @param add
     */
    public void setAddParticipantData(boolean add) {
        addParticipant = add;
        if (mFavoritesRecyclerViewAdapter != null && mFavoriteList != null) {
            mFavoritesRecyclerViewAdapter.setAddParticipant(addParticipant);
            mFavoritesRecyclerViewAdapter.notifyDataSetChanged();
            mFavoriteList.setAdapter(mFavoritesRecyclerViewAdapter);
        }
    }
}