package com.avaya.android.vantage.aaadevbroadcast.views.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.GoogleAnalyticsUtils;
import com.avaya.android.vantage.aaadevbroadcast.PhotoLoadUtility;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.NameContactComparator;
import com.avaya.android.vantage.aaadevbroadcast.csdk.LocalContactInfo;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Favorite contacts viewAdapter.
 */
public class MyFavoritesRecyclerViewAdapter extends RecyclerView.Adapter<MyFavoritesRecyclerViewAdapter.ItemViewHolder> {

    private static final String TAG = MyFavoritesRecyclerViewAdapter.class.getSimpleName();

    private final List<ContactData> mAllFavoriteContacts = new ArrayList<>();
    private final Context mContext;
    private final OnContactInteractionListener mListener;
    private boolean mFirstNameFirst;
    private boolean mSortFirstName;
    private boolean addParticipant = false;

    /**
     * Constuctor
     *
     * @param items    list of favorite items
     * @param listener contact listener used when user clicks on list item
     * @param mContext Fragment context
     */
    public MyFavoritesRecyclerViewAdapter(List<ContactData> items, OnContactInteractionListener listener, Context mContext) {
        this.mContext = mContext;
        mListener = listener;

        mFirstNameFirst = ContactsFragment.isFirstNameFirst();
        mSortFirstName = ContactsFragment.shouldSortByFirstName();

        setItems(items);
    }

    /**
     * Return the view type of the item at <code>position</code> for the purposes
     * of view recycling.
     * <p>
     * <p>The default implementation of this method returns 0, making the assumption of
     * a single view type for the adapter. Unlike ListView adapters, types need not
     * be contiguous. Consider using id resources to uniquely identify item view types.
     *
     * @param position position to query
     * @return integer value identifying the type of the view needed to represent the item at
     * <code>position</code>. Type codes need not be contiguous.
     */
    @Override
    public int getItemViewType(int position) {
        return R.layout.favorite_list_item;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyFavoritesRecyclerViewAdapter.ItemViewHolder holder, int position) {

        ContactData data = mAllFavoriteContacts.get(position);
        holder.mItem = data;

        if ((data.mCategory == ContactData.Category.ENTERPRISE) && (data.mPhoto != null)) {
            PhotoLoadUtility.setPhoto(data, holder.mPhotoImage);
            holder.mPhoto.setVisibility(View.INVISIBLE);
            holder.mPhotoImage.setVisibility(View.VISIBLE);
        } else {
            holder.mPhoto.setVisibility(View.VISIBLE);
            holder.mPhotoImage.setVisibility(View.INVISIBLE);
            PhotoLoadUtility.setThumbnail(data, holder.mPhoto, mFirstNameFirst);
        }

        holder.mName.setText(data.getFormatedName(mFirstNameFirst));

        holder.mLocation.setText(data.mCity);

        if (!data.mHasPhone) {
            holder.mCallAudio.setAlpha(0.5f);
            holder.mCallVideo.setAlpha(0.5f);
        } else {
            holder.mCallAudio.setOnClickListener(v -> {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    holder.mItem.audioCall(mContext, mListener);
                    //Call from Favorites main page
                    GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_CONTACTS_EVENT);

                }
            });
            if (!Utils.isCameraSupported()) {
                holder.mCallVideo.setVisibility(View.INVISIBLE);
            } else {
                // Enable video
                if (!SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
                    holder.mCallVideo.setAlpha(0.5f);
                    holder.mCallVideo.setEnabled(false);
                } else {
                    holder.mCallVideo.setEnabled(true);
                    holder.mCallVideo.setAlpha(1f);
                    holder.mCallVideo.setOnClickListener(v -> {
                        if (null != mListener) {
                            holder.mItem.videoCall(mContext, mListener);
                            //Call from Favorites main page
                            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_CONTACTS_EVENT);
                        }
                    });
                }
            }
            holder.mAddParticipant.setOnClickListener(v -> {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    if (holder.mItem.mCategory == ContactData.Category.LOCAL) {
                        List<ContactData.PhoneNumber> phoneNumbers = LocalContactInfo.getPhoneNumbers(Uri.parse(holder.mItem.mURI), mContext);
                        ContactData contactItem = new ContactData(holder.mItem.mName, holder.mItem.mFirstName, holder.mItem.mLastName, null, holder.mItem.isFavorite(),
                                holder.mItem.mLocation, holder.mItem.mCity, holder.mItem.mPosition, holder.mItem.mCompany, phoneNumbers, holder.mItem.mCategory,
                                holder.mItem.mUUID, holder.mItem.mURI, holder.mItem.mPhotoThumbnailURI, holder.mItem.mHasPhone, holder.mItem.mEmail, holder.mItem.mPhotoURI, "", "", "");
                        mListener.onCallAddParticipant(contactItem);
                    } else {
                        mListener.onCallAddParticipant(holder.mItem);
                    }
                    setAddParticipant(false);
                    notifyDataSetChanged();
                }
            });
        }

        holder.mView.setOnClickListener(view -> {
            if (mListener != null) {

                // using handler to delay showing of a fragment a little bit and displaying ripple effect
                final Handler handler = new Handler();
                handler.postDelayed(() -> mListener.onContactsFragmentInteraction(holder.mItem), 50);
            }
        });
    }


    @Override
    public void onViewRecycled(@NonNull ItemViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.mPhoto != null) {
            Glide.with(holder.mPhoto.getContext()).clear(holder.mPhoto);
        }
    }

    @Override
    public int getItemCount() {
        return mAllFavoriteContacts.size();
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * should update the contents of the {ViewHolder#itemView} to reflect the item at
     * the given position.
     * <p>
     * Note that unlike {@link ListView}, RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only
     * use the <code>position</code> parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an item later
     * on (e.g. in a click listener), use {ViewHolder#getAdapterPosition()} which will
     * have the updated adapter position.
     * <p>
     * Partial bind vs full bind:
     * <p>
     * The payloads parameter is a merge list from {@link #notifyItemChanged(int, Object)} or
     * {@link #notifyItemRangeChanged(int, int, Object)}.  If the payloads list is not empty,
     * the ViewHolder is currently bound to old data and Adapter may run an efficient partial
     * update using the payload info.  If the payload is empty,  Adapter must run a full bind.
     * Adapter should not assume that the payload passed in notify methods will be received by
     * onBindViewHolder().  For example when the view is not attached to the screen, the
     * payload in notifyItemChange() will be simply dropped.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     * @param payloads A non-null list of merged payloads. Can be empty list if requires full
     */
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position, List<Object> payloads) {
        if (payloads != null && payloads.size() > 0) {
            if (payloads.get(0) instanceof RoundedBitmapDrawable) {
                holder.mPhoto.setBackground((RoundedBitmapDrawable) payloads.get(0));
                holder.mPhoto.setText("");
            }
        } else {
            assert payloads != null;
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    public void setItems(List<ContactData> items) {
        mAllFavoriteContacts.clear();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isFavorite()) {
                mAllFavoriteContacts.add(items.get(i));
            }
        }
        Collections.sort(mAllFavoriteContacts, new NameContactComparator(mSortFirstName));
        notifyDataSetChanged();
    }

    public void setFirstNameFirst(boolean mFirstNameFirst) {
        this.mFirstNameFirst = mFirstNameFirst;
    }

    public void setSortFirstName(boolean mSortFirstName) {
        this.mSortFirstName = mSortFirstName;
    }

    /**
     * Setting up is view open for adding participant from join conference call
     *
     * @param add are we adding participant
     */
    public void setAddParticipant(boolean add) {
        addParticipant = add;
    }

    /**
     * ViewHolder for recycler view adapter
     */
    class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView mName;
        private final View mView;
        private final TextView mLocation;
        private final TextView mPhoto;
        private final ImageView mPhotoImage;
        private final ImageView mCallAudio;
        private final ImageView mCallVideo;
        private final ImageView mAddParticipant;
        ContactData mItem;

        private ItemViewHolder(View view) {
            super(view);

            mView = view;
            mName = view.findViewById(R.id.contact_name);
            mLocation = view.findViewById(R.id.contact_location);
            mPhoto = view.findViewById(R.id.initials);
            mPhotoImage = view.findViewById(R.id.photo);
            mCallAudio = view.findViewById(R.id.call_audio);
            mCallVideo = view.findViewById(R.id.call_video);
            mAddParticipant = view.findViewById(R.id.add_participant);
            if (addParticipant) {
                mCallAudio.setVisibility(View.INVISIBLE);
                mCallVideo.setVisibility(View.INVISIBLE);
                mAddParticipant.setVisibility(View.VISIBLE);
            }

            if (mContext.getResources().getBoolean(R.bool.is_landscape)) {
                android.view.ViewGroup.LayoutParams params = mLocation.getLayoutParams();
                params.width = 484;
                mLocation.setLayoutParams(params);
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mName.getText() + "'";
        }
    }
}
