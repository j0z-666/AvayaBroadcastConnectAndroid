package com.avaya.android.vantage.aaadevbroadcast.callshistory;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import com.avaya.android.vantage.aaadevbroadcast.ElanApplication;
import com.avaya.android.vantage.aaadevbroadcast.R;
import com.avaya.android.vantage.aaadevbroadcast.model.CallData;
import com.avaya.android.vantage.aaadevbroadcast.views.SlideAnimation;


class DeleteLogHelper implements View.OnClickListener {

    private RelativeLayout deleteDialog;

    private final SlideAnimation dialogSlider;

    private final CallHistoryFragmentPresenter callHistoryFragmentPresenter;


    DeleteLogHelper(CallHistoryFragmentPresenter presenter) {
        callHistoryFragmentPresenter = presenter;
        dialogSlider = ElanApplication.getDeviceFactory().getSlideAnimation();
    }

    void bindViews(View root) {
        deleteDialog = root.findViewById(R.id.delete_call_history);
        deleteDialog.setVisibility(View.GONE);

        dialogSlider.reDrawListener(deleteDialog);
    }

    void expandDialog(CallData callData, RecyclerView.ViewHolder viewHolder) {
        dialogSlider.expand(deleteDialog);

        View cancel = deleteDialog.findViewById(R.id.delete_callhistory_dialog_cancel);
        cancel.setOnClickListener(this);

        View confirmDelete = deleteDialog.findViewById(R.id.delete_callhistory_dialog_ok);
        confirmDelete.setTag(R.id.tagCallLogItem, callData);
        confirmDelete.setTag(R.id.tagViewHolder, viewHolder);
        confirmDelete.setOnClickListener(this);

        View exitDeleteDialog = deleteDialog.findViewById(R.id.call_features_close);
        exitDeleteDialog.setOnClickListener(this);
    }

    void collapseDialog() {
        dialogSlider.collapse(deleteDialog);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.delete_callhistory_dialog_ok:
                CallData logItem = (CallData) view.getTag(R.id.tagCallLogItem);
                RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) view.getTag(R.id.tagViewHolder);
                callHistoryFragmentPresenter.deleteCallLog(logItem, viewHolder);
            case R.id.call_features_close:
            case R.id.delete_callhistory_dialog_cancel:
            default:
                collapseDialog();
                break;
        }
    }

}
