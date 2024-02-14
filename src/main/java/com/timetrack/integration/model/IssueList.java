package com.timetrack.integration.model;

import java.util.List;

public class IssueList extends AbstractDto {


    private List<Issue> issues;

    public List<Issue> getIssues() {
        return issues;
    }

    public IssueList setIssues(List<Issue> issues) {
        this.issues = issues;
        return this;
    }
}
