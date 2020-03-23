package com.avaya.android.vantage.aaadevbroadcast.views.adapters;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.model.ErrorNotificationAlert;

import java.util.List;

/**
 * Used to present error notification alerts.
 */
public class ErrorNotificationListAdapter extends RecyclerView.Adapter<ErrorNotificationListAdapter.ItemViewHolder> {

    private static final String TAG = "ErrorNotificationListAdapter";
    private final List<ErrorNotificationAlert> mErrorNotificationList;

    public ErrorNotificationListAdapter(List<ErrorNotificationAlert> mErrorNotificationList) {
        this.mErrorNotificationList = mErrorNotificationList;
    }

    @Override
    public ErrorNotificationListAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.error_notification_alert_item, parent, false);
        return new ErrorNotificationListAdapter.ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ErrorNotificationListAdapter.ItemViewHolder holder, int position) {

        holder.mTitle.setText(mErrorNotificationList.get(position).getTitle());
        holder.mDescription.setText(mErrorNotificationList.get(position).getDescription());
    }

    @Override
    public int getItemCount() {
        return mErrorNotificationList.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView mTitle;
        final TextView mDescription;

        ItemViewHolder(View view) {
            super(view);

            mTitle = view.findViewById(R.id.error_notification_title);
            mDescription = view.findViewById(R.id.error_notification_description);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitle.getText() + "'";
        }
    }

}
