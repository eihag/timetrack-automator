package com.timetrack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TimeTrackCommandLineRunnerTest {

    @Test
    void parseDate() {
        String[] args = new String[2];
        args[1] = "2024-02-14";
        assertNotNull(TimeTrackCommandLineRunner.parseDate(args));

        args[1] = "2024-13-1";
        assertNull(TimeTrackCommandLineRunner.parseDate(args));

        args[1] = "2024-02-31";
        assertNull(TimeTrackCommandLineRunner.parseDate(args));
    }

}