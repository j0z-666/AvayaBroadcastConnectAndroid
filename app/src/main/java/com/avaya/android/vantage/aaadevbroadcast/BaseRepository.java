package com.avaya.android.vantage.aaadevbroadcast;


public abstract class BaseRepository {
    //TODO implement Abstract Factory Pattern for local contacts
    //TODO and enterprise contacts repositories

    abstract <T> void attachListener(T listener);
    abstract <T> void detachListener(T listener);

    abstract <T> T getContacts();
}
