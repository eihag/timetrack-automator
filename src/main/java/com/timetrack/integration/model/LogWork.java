package com.timetrack.integration.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class LogWork extends AbstractDto {

    private String timeSpent;

    private Date started;

    public LogWork() {
    }

    public LogWork(String timeSpent, LocalDate localDate) {
        this.timeSpent = timeSpent;
        if (localDate != null) {
            this.started = Date.from(localDate
                    .atStartOfDay(ZoneId.of("UTC"))
                    .withHour(8)
                    .toInstant());
        }
    }

    public String getTimeSpent() {
        return timeSpent;
    }

    public LogWork setTimeSpent(String timeSpent) {
        this.timeSpent = timeSpent;
        return this;
    }

    public Date getStarted() {
        return started;
    }

    public LogWork setStarted(Date started) {
        this.started = started;
        return this;
    }
}
