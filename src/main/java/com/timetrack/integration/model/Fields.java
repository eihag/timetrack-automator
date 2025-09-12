package com.timetrack.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Fields extends AbstractDto {

    private Assignee assignee;

    private String summary;

    @JsonProperty("timetracking")
    private TimeTracking timeTracking;


    public Assignee getAssignee() {
        return assignee;
    }

    public Fields setAssignee(Assignee assignee) {
        this.assignee = assignee;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Fields setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public TimeTracking getTimeTracking() {
        return timeTracking;
    }

    public Fields setTimeTracking(TimeTracking timeTracking) {
        this.timeTracking = timeTracking;
        return this;
    }
}
