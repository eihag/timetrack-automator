package com.timetrack;

import com.timetrack.service.TimetrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import static org.apache.logging.log4j.util.Strings.isEmpty;

@Component
public class TimeTrackCommandLineRunner implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(TimeTrackCommandLineRunner.class);

    private final TimetrackService timetrackService;

    public TimeTrackCommandLineRunner(TimetrackService timetrackService) {
        this.timetrackService = timetrackService;
    }

    @Override
    public void run(String... args) {
        if (args == null || args.length == 0) {
            LOG.error("No argument supplied.");
            showHelp();
            return;
        }

        switch (args[0]) {
            case "dry-run",
                    "log-work" -> runTimeTrackService(args);
            case "report" -> runReport(args);
            default -> {
                LOG.error("Unknown argument: '{}'", args[0]);
                showHelp();
            }
        }
    }

    private void runTimeTrackService(String[] args) {
        LocalDate date = parseDate(args);
        if (date == null) {
            showHelp();
            return;
        }
        boolean dryRun = "dry-run".equalsIgnoreCase(args[0]);

        timetrackService.trackTime(date, dryRun);
    }

    private void runReport(String[] args) {
        if (args.length < 2 ||
                (!("month".equalsIgnoreCase(args[1]) ||
                        "year".equalsIgnoreCase(args[1])))) {
            showHelp();
            return;
        }
        timetrackService.reportTimeLogged(args[1]);
    }

    static LocalDate parseDate(String[] args) {
        if (args.length < 2 || isEmpty(args[1])) {
            return null;
        }
        if ("today".equalsIgnoreCase(args[1])) {
            return LocalDate.now();
        }
        return parseDate(args[1]);
    }

    public static LocalDate parseDate(String dateStr) {
        try {
            TemporalAccessor ta = DateTimeFormatter.ISO_DATE.parse(dateStr);
            return LocalDate.from(ta);
        } catch (Exception e) {
            LOG.error("Cannot parse date: '{}'", dateStr);
            return null;
        }
    }

    public void showHelp() {
        LOG.info("Arguments: ");
        LOG.info(" dry-run {<date> | today}");
        LOG.info(" log-work {<date> | today}");
        LOG.info(" report {month | year}");
        LOG.info("");
    }
}
