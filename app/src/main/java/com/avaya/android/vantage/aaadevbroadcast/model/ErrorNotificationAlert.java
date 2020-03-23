package com.avaya.android.vantage.aaadevbroadcast.model;

/**
 * Error notification alert item.
 */
public class ErrorNotificationAlert {

    private final String title;
    private final String message;

    /**
     * Constructor
     *
     * @param title   error title
     * @param message error message
     */
    public ErrorNotificationAlert(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return message;
    }

}
