package com.timetrack.rest;

import com.timetrack.TimeTrackCommandLineRunner;
import com.timetrack.integration.rest.JiraRestClient;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JiraRestClientTest {
    @Inject
    private JiraRestClient jiraRestClient;

    @Disabled("do not log work for each run")
    @Test
    void testLogWork() {
        jiraRestClient.createWorkLog("BP-641", TimeTrackCommandLineRunner.parseDate("2024-02-12"), 1);
    }

}