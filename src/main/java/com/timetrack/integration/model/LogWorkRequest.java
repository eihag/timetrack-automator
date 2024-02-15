package com.timetrack.integration.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class LogWorkRequest extends AbstractDto {

    private String timeSpent;

    private Date started;

    public LogWorkRequest(String timeSpent, LocalDate localDate) {
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

    public LogWorkRequest setTimeSpent(String timeSpent) {
        this.timeSpent = timeSpent;
        return this;
    }

    public Date getStarted() {
        return started;
    }

    public LogWorkRequest setStarted(Date started) {
        this.started = started;
        return this;
    }
}
