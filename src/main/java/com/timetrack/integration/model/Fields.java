package com.timetrack.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Fields extends AbstractDto {

    private Assignee assignee;

    @JsonProperty("timeoriginalestimate")
    private int timeOriginalEstimate;

    // Remaining time estimate
    @JsonProperty("timeestimate")
    private int timeEstimate;

    @JsonProperty("timespent")
    private int timeSpent;

    private String summary;

    public Assignee getAssignee() {
        return assignee;
    }

    public Fields setAssignee(Assignee assignee) {
        this.assignee = assignee;
        return this;
    }

    public int getTimeOriginalEstimate() {
        return timeOriginalEstimate;
    }

    public Fields setTimeOriginalEstimate(int timeOriginalEstimate) {
        this.timeOriginalEstimate = timeOriginalEstimate;
        return this;
    }

    public int getTimeEstimate() {
        return timeEstimate;
    }

    public Fields setTimeEstimate(int timeEstimate) {
        this.timeEstimate = timeEstimate;
        return this;
    }

    public int getTimeSpent() {
        return timeSpent;
    }

    public Fields setTimeSpent(int timeSpent) {
        this.timeSpent = timeSpent;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Fields setSummary(String summary) {
        this.summary = summary;
        return this;
    }
}
