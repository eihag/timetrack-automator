package com.timetrack;

import com.timetrack.service.TimetrackService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
class TimetrackServiceTest {

    @Inject
    private TimetrackService timetrackService;

    @Test
    void dryRunCurrentDate() {
        timetrackService.trackTime(LocalDate.now(), true);
    }

    @Test
    void dryRunNoData() {
        timetrackService.trackTime(TimeTrackCommandLineRunner.parseDate("2024-01-01"), true);
    }

    @Test
    void dryRunSomeDate() {
        timetrackService.trackTime(TimeTrackCommandLineRunner.parseDate("2024-02-01"), true);
    }

}
