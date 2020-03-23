package com.avaya.android.vantage.aaadevbroadcast.model;

import android.os.Parcel;
import android.os.Parcelable;

public class DirectoryData implements Parcelable {
    public final String directoryName;
    public final String accountName;
    public final int directoryID;
    private final String directoryURI;
    public final String type;
    private final String packageName;

    public DirectoryData(String directoryName, String accountName, int directoryID, String directoryURI, String type, String packageName) {
        this.directoryName = directoryName;
        this.accountName = accountName;
        this.directoryID = directoryID;
        this.directoryURI = directoryURI;
        this.type = type;
        this.packageName = packageName;
    }

    private DirectoryData(Parcel in) {
        directoryName = in.readString();
        accountName = in.readString();
        directoryID = in.readInt();
        directoryURI = in.readString();
        type = in.readString();
        packageName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(directoryName);
        dest.writeString(accountName);
        dest.writeInt(directoryID);
        dest.writeString(directoryURI);
        dest.writeString(type);
        dest.writeString(packageName);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<DirectoryData> CREATOR = new Parcelable.Creator<DirectoryData>() {
        @Override
        public DirectoryData createFromParcel(Parcel in) {
            return new DirectoryData(in);
        }

        @Override
        public DirectoryData[] newArray(int size) {
            return new DirectoryData[size];
        }
    };
}