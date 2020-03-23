package com.avaya.android.vantage.aaadevbroadcast.contacts;

import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;

import java.util.Comparator;

public class NameContactComparator implements Comparator<ContactData> {

    private final boolean sortByFirstName;

    public NameContactComparator(boolean sortByFirstName) {
        this.sortByFirstName = sortByFirstName;
    }

    @Override
    public int compare(ContactData first, ContactData second) {
        if (sortByFirstName) {
            return sortByFirstName(first, second);
        } else {
            return sortByLastName(first, second);
        }
    }

    private int sortByFirstName(ContactData first, ContactData second) {
        if (first.mFirstName == null) {
            return -1;
        } else if (second.mFirstName == null) {
            return 1;
        }
        return first.mFirstName.toLowerCase().compareTo(second.mFirstName.toLowerCase());
    }

    private int sortByLastName(ContactData first, ContactData second) {
        if (first.mLastName == null) {
            return -1;
        } else if (second.mLastName == null) {
            return 1;
        }
        return first.mLastName.toLowerCase().compareTo(second.mLastName.toLowerCase());
    }
}