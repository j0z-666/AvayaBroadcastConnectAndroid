package com.avaya.android.vantage.aaadevbroadcast.callshistory;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.PhotoLoadUtility;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.Utils;
import com.avaya.android.vantage.aaadevbroadcast.activities.BaseActivity;
import com.avaya.android.vantage.aaadevbroadcast.contacts.ContactsFragment;
import com.avaya.android.vantage.aaadevbroadcast.contacts.EnterpriseContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.contacts.LocalContactsRepository;
import com.avaya.android.vantage.aaadevbroadcast.csdk.SDKManager;
import com.avaya.android.vantage.aaadevbroadcast.fragments.JoinMeetingFragment;
import com.avaya.android.vantage.aaadevbroadcast.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.aaadevbroadcast.model.CallData;
import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.avaya.clientservices.call.CallType;
import com.avaya.clientservices.calllog.CallLogItem;

import java.util.ArrayList;
import java.util.List;
import java.lang.*;

import static com.avaya.android.vantage.aaadevbroadcast.Utils.getSimpleDateString;
import static com.avaya.android.vantage.aaadevbroadcast.Utils.getTimeString;
import static com.avaya.android.vantage.aaadevbroadcast.fragments.JoinMeetingFragment.JOIN_MEETING_FRAGMENT;
import static com.avaya.android.vantage.aaadevbroadcast.model.ContactData.PhoneType.WORK;

/**
 * CallHistoryRecyclerViewAdapter is responsible for showing and properly rendering Recent Calls
 */

public class CallHistoryRecyclerViewAdapter extends ListAdapter<CallData, CallHistoryRecyclerViewAdapter.ViewHolder> implements Filterable {

    private final static String UNKNOWN_PHONE_TYPE = "-";
    private boolean addParticipant = false;

    private List<CallData> logItems;
    private List<CallData> logFilterItems;
    private final Context mContext;
    private CallTypeFilter mCallTypeFilter;
    private CharSequence mFilterConstraint;
    private boolean mFirstNameFirst;
    private final OnContactInteractionListener contactInteractionListener;

    private final CallHistoryAdapterListener mCallHistoryAdapterListener;

    CallHistoryRecyclerViewAdapter(List<CallData> logItems, Context mContext, OnContactInteractionListener onContactInteractionListener, CallHistoryAdapterListener callHistoryAdapterListener) {
        super(new CallLogsDiffCallback());

        this.logItems = logItems;
        this.mContext = mContext;
        logFilterItems = logItems;
        this.contactInteractionListener = onContactInteractionListener;
        this.mCallHistoryAdapterListener = callHistoryAdapterListener;
        mFirstNameFirst = ContactsFragment.isFirstNameFirst();
    }

    /**
     * From provided duration String which represent duration of call in milliseconds
     * and is parsed in proper format as H:MM:SS
     *
     * @param duration String representation of call duration in milliseconds
     * @return String representation of properly formatted time
     */
    private String getDurationTimeString(int duration) {
        int seconds = duration % 60;
        int minutes = (duration - seconds) / 60;

        return mContext.getString(R.string.time_format, minutes, seconds);
    }

    private String getDurationTimeString(String strDuration) {
        int intDuration = 0;
        if (strDuration != null && !strDuration.isEmpty()) {
            intDuration = Integer.parseInt(strDuration);
        }

        return getDurationTimeString(intDuration);
    }

    public void setAddParticipant(boolean addParticipant) {
        this.addParticipant = addParticipant;
    }

    void setLogItems(List<CallData> logItems) {
        logFilterItems = logItems;
        if (mCallTypeFilter != null && mFilterConstraint != null) {
            mCallTypeFilter.filter(mFilterConstraint);
        } else {
            this.logItems = logItems;
            submitList(new ArrayList<>(logItems));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_call_item, parent, false);
        return new CallHistoryRecyclerViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CallData callData = getItem(position);

        if (TextUtils.isEmpty(callData.mFirstName) && TextUtils.isEmpty(callData.mLastName)) {
            callData.mFirstName = callData.mName;
            callData.mLastName = "";
        }

        String contactName;
        if (mFirstNameFirst) {
            contactName = callData.mFirstName + " " + callData.mLastName;
        } else {
            contactName = callData.mLastName + " " + callData.mFirstName;
        }

        holder.tvName.setText(contactName);
        String callDate = mContext.getString(R.string.recent_call_at, getSimpleDateString(callData.mCallDate), getTimeString(mContext, callData.mCallTime));
        holder.tvCallDate.setText(callDate);

        holder.ivCallType.setImageResource(getCallStateIconResources(callData));
        holder.ivCallType.setContentDescription(getCallStateStringDesc(callData));

        holder.tvPhoneType.setText(UNKNOWN_PHONE_TYPE);

        holder.tvCallDuration.setText(isMissedCall(callData) ?
                getDurationTimeString(0) : getDurationTimeString(callData.mCallDuration));

        if (holder.ivAddParticipant.getVisibility() != View.VISIBLE) {
            setVideoCallButtonState(holder, callData.isNonCallableConference);
            setAudioCallButtonState(holder, callData.isNonCallableConference);
        }

        holder.bind(callData, contactInteractionListener);

        CallLogItem logItem = callData.getCallLogItem();
        if (logItem != null && logItem.getCallType().equals(CallType.HTTP_MEETME_CALLTYPE)) {
            holder.ibCallAudio.setVisibility(View.INVISIBLE);
            holder.ibCallVideo.setVisibility(View.INVISIBLE);
            holder.ibJoinMeeting.setVisibility(View.VISIBLE);
        } else {
            holder.ibJoinMeeting.setVisibility(View.GONE);
        }
    }

    public int getCachedItemCount() {
        if (logItems == null) {
            return 0;
        }
        return logItems.size();
    }

    @Override
    public Filter getFilter() {
        if (mCallTypeFilter == null) {
            mCallTypeFilter = new CallTypeFilter();
        }
        return mCallTypeFilter;
    }

    void setFirstNameFirst(boolean mFirstNameFirst) {
        if (this.mFirstNameFirst ^ mFirstNameFirst) {
            this.mFirstNameFirst = mFirstNameFirst;
            notifyDataSetChanged();
        }
    }

    /**
     * Update call icon
     *
     * @param data call data item
     * @return icon to display
     */
    private int getCallStateIconResources(CallData data) {
        //TODO add icons for calls from paired device
        if (data.isFromPaired) {
            if (data.mCategory == CallData.CallCategory.INCOMING) {
                return R.drawable.ic_sync_incoming_grey;
            } else if (data.mCategory == CallData.CallCategory.OUTGOING) {
                return R.drawable.ic_sync_outgoing_grey;
            }
            return R.drawable.ic_sync_missed;
        } else {
            if (data.mCategory == CallData.CallCategory.INCOMING) {
                return R.drawable.ic_recents_audio_incoming;
            } else if (data.mCategory == CallData.CallCategory.OUTGOING) {
                return R.drawable.ic_recents_audio_outgoing;
            }
            return R.drawable.ic_recents_audio_missed;
        }
    }

    private String getCallStateStringDesc(CallData data) {

        StringBuilder sb = new StringBuilder();

        String mMobile = (mContext.getString(R.string.contact_details_mobile));

        if (data.isFromPaired) {
            if (data.mCategory == CallData.CallCategory.INCOMING) {
                return sb.append(mContext.getString(R.string.incoming_content_description)).append(" ").append(mMobile).toString();
            } else if (data.mCategory == CallData.CallCategory.OUTGOING) {
                return sb.append(mContext.getString(R.string.outgoing_content_description)).append(" ").append(mMobile).toString();
            }
            return sb.append(mContext.getString(R.string.missed_content_description)).append(" ").append(mMobile).toString();
        } else {
            if (data.mCategory == CallData.CallCategory.INCOMING) {
                return mContext.getString(R.string.incoming_content_description);
            } else if (data.mCategory == CallData.CallCategory.OUTGOING) {
                return mContext.getString(R.string.outgoing_content_description);
            }
            return mContext.getString(R.string.missed_content_description);
        }
    }

    /**
     * Check if call is missed category
     *
     * @param callData
     * @return true if call is in missed category, otherwise false.
     */
    private boolean isMissedCall(CallData callData) {
        return callData.mCategory == CallData.CallCategory.MISSED;
    }

    /**
     * Method that sets state of video call button. If camera isn't supported
     * we should hide video button. Otherwise, check if video is enabled
     * or muted and set visibility. Also, for a while, conference can not be called
     * from history.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *               item at the given position in the data set.
     */
    private void setVideoCallButtonState(ViewHolder holder, boolean isNonCallableConference) {
        if (!Utils.isCameraSupported() || isNonCallableConference) {
            holder.ibCallVideo.setVisibility(View.INVISIBLE);
            return;
        }

        if (!SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
            holder.ibCallVideo.setAlpha(0.5f);
            holder.ibCallVideo.setEnabled(false);
        } else {
            holder.ibCallVideo.setEnabled(true);
            holder.ibCallVideo.setAlpha(1f);
        }

        holder.ibCallVideo.setVisibility(View.VISIBLE);
    }

    /**
     * Method that sets state of audio call button state. For a while,
     * conference can not be called from history
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *               item at the given position in the data set.
     */
    private void setAudioCallButtonState(ViewHolder holder, boolean isNonCallableConference) {
        if (isNonCallableConference) {
            holder.ibCallAudio.setVisibility(View.INVISIBLE);
        } else {
            holder.ibCallAudio.setVisibility(View.VISIBLE);
        }
    }

    interface CallHistoryAdapterListener {
        void onSearchCountChanged();

        void onItemLongClicked(CallData item, RecyclerView.ViewHolder viewHolder);
    }

    private class CallTypeFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();

            if (constraint == null || constraint.length() == 0 || constraint.equals(CallData.CallCategory.ALL.toString())) {
                filterResults.count = logFilterItems.size();
                filterResults.values = logFilterItems;
            } else {
                List<CallData> filterList = new ArrayList<>();
                for (CallData callData : logFilterItems) {
                    if (constraint.equals(callData.mCategory.toString())) {
                        filterList.add(callData);
                    }
                }

                filterResults.count = filterList.size();
                filterResults.values = filterList;
            }

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mFilterConstraint = charSequence;
            logItems = (List<CallData>) filterResults.values;

            if (logItems != null){
                submitList(new ArrayList<>(logItems));
            }

            mCallHistoryAdapterListener.onSearchCountChanged();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatar;

        final TextView tvName;
        final TextView tvCallDate;

        final ImageView ivCallType;

        final TextView tvCallDuration;
        final TextView tvPhoneType;
        final ImageButton ibCallAudio;
        final ImageButton ibCallVideo;
        final ImageView ivAddParticipant;
        final ImageButton ibJoinMeeting;

        ViewHolder(View itemView) {
            super(itemView);

            tvAvatar = itemView.findViewById(R.id.initials);
            tvName = itemView.findViewById(R.id.recent_name);
            tvCallDate = itemView.findViewById(R.id.recent_call_date);

            ivCallType = itemView.findViewById(R.id.recent_call_type);

            tvPhoneType = itemView.findViewById(R.id.recent_number_type);
            tvCallDuration = itemView.findViewById(R.id.recent_call_duration);

            ibCallAudio = itemView.findViewById(R.id.call_audio);
            ibCallVideo = itemView.findViewById(R.id.call_video);
            ibJoinMeeting = itemView.findViewById(R.id.join_meeting);
            ivAddParticipant = itemView.findViewById(R.id.add_participant);
            if (addParticipant) {
                ibCallAudio.setVisibility(View.INVISIBLE);
                ibCallVideo.setVisibility(View.INVISIBLE);
                ivAddParticipant.setVisibility(View.VISIBLE);
            }
        }

        void bind(final CallData item, final OnContactInteractionListener listener) {
            String photoUri = LocalContactsRepository.getInstance().getPhotoUri(item.mRemoteNumber);

            ContactData contactData = new ContactData(item.mName, item.mFirstName, item.mLastName,
                    item.mPhoto, false, item.mLocation, item.mCity, item.mPosition,
                    item.mCompany, getPhoneNumbers(item), item.mContactCategory, item.getUUID(), item.getURI(),
                    item.getPhotoThumbnailURI(), true, "", photoUri,
                    item.mAccountType, "", "");

            contactData.mRefObject = item.mRefObject;
            PhotoLoadUtility.setThumbnail(contactData, tvAvatar, mFirstNameFirst);

            itemView.setOnClickListener(v -> {
                if (!addParticipant) {

                    CallLogItem callLogItem = item.getCallLogItem();
                    if (callLogItem != null && callLogItem.getCallType().equals(CallType.HTTP_MEETME_CALLTYPE)) {
                        showJoinMeetingFragment(item.getCallLogItem().getLocalUserName(), item.getCallLogItem().getConferenceId(), item.getCallLogItem().getPortalURL());
                    } else {
                        final Handler handler = new Handler();
                        handler.postDelayed(() -> listener.onContactsFragmentInteraction(contactData), 50);
                    }
                }
            });

            itemView.setOnLongClickListener(view -> {
                mCallHistoryAdapterListener.onItemLongClicked(item, this);
                return true;
            });

            ibCallAudio.setOnClickListener(view -> {
                // TODO This should be reworked
                List<ContactData.PhoneNumber> phoneNumbers = LocalContactsRepository
                        .getInstance()
                        .getPhoneNumbers(item.mRemoteNumber);
                if (phoneNumbers == null || phoneNumbers.isEmpty()) {
                    ContactData tmp = EnterpriseContactsRepository.getInstance()
                            .getByPhone(item.mRemoteNumber);
                    if (tmp != null) {
                        phoneNumbers = tmp.mPhones;
                    }
                }

                if (phoneNumbers == null || phoneNumbers.isEmpty()) {
                    phoneNumbers = new ArrayList<>();
                    phoneNumbers.add(new ContactData.PhoneNumber(item.mRemoteNumber, WORK, true, "0"));
                }

                contactData.mPhones = phoneNumbers;
                listener.onCallContactAudio(contactData, null);
            });

            ibCallVideo.setOnClickListener(view -> {
                if (!Utils.isCameraSupported()) {
                    ibCallVideo.setVisibility(View.INVISIBLE);
                    return;
                }
                if (!SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled() ||
                        SDKManager.getInstance().getCallAdaptor().ismVideoMuted()) {
                    ibCallVideo.setAlpha(0.5f);
                    ibCallVideo.setEnabled(false);
                } else {
                    ibCallVideo.setEnabled(true);
                    ibCallVideo.setAlpha(1f);
                    if (null != listener) {
                        listener.onCallContactVideo(contactData, item.mPhone);
                    }
                }
            });

            ibJoinMeeting.setOnClickListener(view -> {

                CallLogItem callLogItem = item.getCallLogItem();
                if (callLogItem != null && callLogItem.getCallType().equals(CallType.HTTP_MEETME_CALLTYPE)) {
                    //SDKManager.getInstance().getUnifiedPortalAdaptor().requestToJoinMeeting(item.getCallLogItem().getPortalURL(), item.getCallLogItem().getConferenceId(), item.getCallLogItem().getLocalUserName(), false, "", "");
                    JoinMeetingFragment fragment = getJoinMeetingFragment();
                    fragment.joinMeeting(item.getCallLogItem().getPortalURL(), item.getCallLogItem().getConferenceId(), item.getCallLogItem().getLocalUserName(), "");
                }
            });

            ivAddParticipant.setOnClickListener(view -> {
                listener.onCallAddParticipant(contactData);
                setAddParticipant(false);
            });
        }

        private void showJoinMeetingFragment(String meetingName, String meetingId, String meetingAddress) {
            FragmentManager fm = ((BaseActivity)(mContext)).getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            JoinMeetingFragment fragment = (JoinMeetingFragment)fm.findFragmentByTag(JOIN_MEETING_FRAGMENT);
            fragment.setMeetingParameters(meetingName, meetingId, meetingAddress);
            ft.show(fragment);
            fragment.getView().setVisibility(View.VISIBLE);
            ft.commit();
        }

        private JoinMeetingFragment getJoinMeetingFragment() {
            FragmentManager fm = ((BaseActivity)(mContext)).getSupportFragmentManager();
            return (JoinMeetingFragment)fm.findFragmentByTag(JOIN_MEETING_FRAGMENT);
        }


        private List<ContactData.PhoneNumber> getPhoneNumbers(CallData item) {
            if (item.mPhones != null) {
                return item.mPhones;
            } else {
                ArrayList<ContactData.PhoneNumber> phone = new ArrayList<>();
                ContactData.PhoneNumber number = new ContactData.PhoneNumber(item.mPhone, ContactData.PhoneType.WORK, true, null);
                phone.add(number);
                return phone;
            }
        }
    }
}