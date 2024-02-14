package com.timetrack.integration.model;


import java.time.ZonedDateTime;

public class WorkLog extends AbstractDto {

    private Author author;

    private int timeSpentSeconds;

    private ZonedDateTime started;


    public Author getAuthor() {
        return author;
    }

    public WorkLog setAuthor(Author author) {
        this.author = author;
        return this;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public WorkLog setTimeSpentSeconds(int timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
        return this;
    }

    public ZonedDateTime getStarted() {
        return started;
    }

    public WorkLog setStarted(ZonedDateTime started) {
        this.started = started;
        return this;
    }
}
