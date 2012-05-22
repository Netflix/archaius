package com.netflix.config;

public interface PollListener {

    public enum EventType {
        POLL_SUCCESS, POLL_FAILURE
    }
    public void handleEvent(EventType eventType, PollResult lastResult, Throwable exception);
}
