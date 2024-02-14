package com.timetrack.integration.model;

import java.util.List;

public class WorkLogList extends AbstractDto {

    List<WorkLog> worklogs;

    public List<WorkLog> getWorklogs() {
        return worklogs;
    }

    public WorkLogList setWorklogs(List<WorkLog> worklogs) {
        this.worklogs = worklogs;
        return this;
    }
}
