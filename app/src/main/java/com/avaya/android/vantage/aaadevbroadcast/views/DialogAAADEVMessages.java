package com.avaya.android.vantage.aaadevbroadcast.views;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.avaya.android.vantage.aaadevbroadcast.R;

import java.util.ArrayList;

public class DialogAAADEVMessages extends Dialog {
    public static ArrayList<String> messagesList = new ArrayList<>();
    private ListView lv_messages;
    private Button btn_dialog_clear;
    private ArrayAdapter adapterMessages;

    public DialogAAADEVMessages(@NonNull Context context) {
        super(context);
    }

    public static void addMessages(String msg){
        messagesList.add(0,msg);
    }

    public void updateMessages(){
        adapterMessages.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_aaadev_messages);

        lv_messages = findViewById(R.id.lv_dialogMessages);
        btn_dialog_clear = findViewById(R.id.btn_dialog_clear);
        adapterMessages = new ArrayAdapter(getContext(), R.layout.list_unit_aaadev_message,messagesList);
        lv_messages.setAdapter(adapterMessages);

        btn_dialog_clear.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("Do you want to delete all messages?")
                    .setPositiveButton("OK", (dialog, which) -> {
                        if(messagesList != null) {
                            messagesList.clear();
                            adapterMessages.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("CANCEL", (dialog, which) -> {}).show();
        });

    }
}
