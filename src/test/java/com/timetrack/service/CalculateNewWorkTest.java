package com.timetrack.service;

import com.timetrack.integration.model.Fields;
import com.timetrack.integration.model.Issue;
import com.timetrack.integration.model.TimeTracking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Plain unit test of the work distribution - does not talk to JIRA.
 */
class CalculateNewWorkTest {

    private static final long MINUTE = 60;
    private static final long HOUR = 60 * MINUTE;

    private TimetrackService service;

    @BeforeEach
    void setUp() {
        service = new TimetrackService(null);
        ReflectionTestUtils.setField(service, "minimumWorkLogged", Duration.ofMinutes(15));
        ReflectionTestUtils.setField(service, "smallTaskThreshold", Duration.ofHours(2));
    }

    @Test
    void smallTaskIsClosedCompletelyAndRestGoesToTheBigTask() {
        Issue big = issue("BIG-1", 40 * HOUR, 0);
        Issue small = issue("SMALL-1", 30 * MINUTE, 0);

        Map<String, Long> workLog = service.calculateNewWork(4 * HOUR, List.of(big, small));

        assertEquals(30 * MINUTE, workLog.get("SMALL-1"));
        assertEquals(4 * HOUR - 30 * MINUTE, workLog.get("BIG-1"));
    }

    @Test
    void noMinimumSizedRemainderIsLeftOnAPartlyLoggedSmallTask() {
        // 1.5h estimate with 1h already spent - the old logic left the last 15m unlogged forever
        Issue almostDone = issue("SMALL-1", 90 * MINUTE, 60 * MINUTE);
        Issue big = issue("BIG-1", 40 * HOUR, 0);

        Map<String, Long> workLog = service.calculateNewWork(4 * HOUR, List.of(big, almostDone));

        assertEquals(30 * MINUTE, workLog.get("SMALL-1"));
    }

    @Test
    void smallTasksAreTakenSmallestFirst() {
        Issue oneHour = issue("SMALL-1", HOUR, 0);
        Issue halfHour = issue("SMALL-2", 30 * MINUTE, 0);

        // only room for the smallest task plus a bit
        Map<String, Long> workLog = service.calculateNewWork(45 * MINUTE, List.of(oneHour, halfHour));

        assertEquals(30 * MINUTE, workLog.get("SMALL-2"));
        assertEquals(15 * MINUTE, workLog.get("SMALL-1"));
    }

    @Test
    void neverLogsMoreThanTheDayHasLeft() {
        Issue small = issue("SMALL-1", 2 * HOUR, 0);

        Map<String, Long> workLog = service.calculateNewWork(10 * MINUTE, List.of(small));

        assertEquals(Map.of("SMALL-1", 10 * MINUTE), workLog);
    }

    @Test
    void abortsWhenEstimatesCannotCoverTheDay() {
        Issue small = issue("SMALL-1", 30 * MINUTE, 0);

        assertNull(service.calculateNewWork(4 * HOUR, List.of(small)));
    }

    private static Issue issue(String key, long estimateSeconds, long spentSeconds) {
        TimeTracking timeTracking = new TimeTracking()
                .setTimeOriginalEstimate((int) estimateSeconds)
                .setTimeSpent((int) spentSeconds);
        return new Issue()
                .setKey(key)
                .setFields(new Fields().setTimeTracking(timeTracking));
    }
}
