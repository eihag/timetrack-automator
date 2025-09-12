package com.timetrack.service;

import com.timetrack.integration.model.Fields;
import com.timetrack.integration.model.Issue;
import com.timetrack.integration.model.IssueList;
import com.timetrack.integration.model.TimeTracking;
import com.timetrack.integration.model.WorkLog;
import com.timetrack.integration.model.WorkLogList;
import com.timetrack.integration.rest.JiraRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimetrackService {
    private static final Logger LOG = LoggerFactory.getLogger(TimetrackService.class);

    @Value("#{T(java.time.Duration).parse('${work_per_day}')}")
    private Duration workPerDay;
    @Value("#{T(java.time.Duration).parse('${minimum_work_logged}')}")
    private Duration minimumWorkLogged;

    private final JiraRestClient restClient;

    public TimetrackService(JiraRestClient restClient) {
        this.restClient = restClient;
    }

    public void reportTimeLogged(String reportPeriod) {
        final LocalDate now = LocalDate.now();
        LocalDate date;
        if ("month".equalsIgnoreCase(reportPeriod)) {
            date = now.withDayOfMonth(1);
        } else {
            date = now.withDayOfYear(1);
        }

        StringBuilder sb = new StringBuilder("\nDate\t\tLogged\tRemaining\n");

        while (date.isBefore(now)) {
            if (!isWeekend(date)) {
                long alreadyLoggedSeconds = getAlreadyLoggedWork(date);
                long remainingSeconds = workPerDay.toSeconds() - alreadyLoggedSeconds;
                sb.append(date)
                        .append('\t')
                        .append(alreadyLoggedSeconds / 60)
                        .append('\t')
                        .append(remainingSeconds / 60)
                        .append('\n');
            }
            date = date.plusDays(1);
        }

        LOG.info("{}", sb);
    }

    public void trackTime(LocalDate date, boolean dryRun) {
        LOG.info("Processing date {}", date);
        if (dryRun) {
            LOG.info("DRY-RUN - not actually logging work");
        }

        long alreadyLoggedSeconds = getAlreadyLoggedWork(date);
        long remainingSeconds = workPerDay.toSeconds() - alreadyLoggedSeconds;

        if (isWeekend(date)) {
            LOG.info("Sorry, I do not work on weekends");
        } else if (remainingSeconds == 0) {
            LOG.info("Work logged correctly");
        } else if (remainingSeconds < 0) {
            LOG.info("Already too much work logged ({}m over limit)", remainingSeconds / 60 * -1);
        } else {
            logRemainingWork(date, remainingSeconds, dryRun);
        }
    }

    private int getAlreadyLoggedWork(LocalDate date) {
        int alreadyLoggedSeconds = 0;
        IssueList issueList = restClient.getMyIssuesWithWorkLoggedForDate(date);
        if (issueList != null && issueList.getIssues() != null) {
            String myAccountId = null;
            for (Issue issue : issueList.getIssues()) {
                if (myAccountId == null) {
                    myAccountId = issue.getFields().getAssignee().getAccountId();
                }
                alreadyLoggedSeconds += getIssueWorkLogTotals(issue, date, myAccountId);
            }
            LOG.info("You already logged {}m of work in {} issue(s)", alreadyLoggedSeconds / 60, issueList.getIssues().size());
        } else {
            LOG.info("No work logged");
        }
        return alreadyLoggedSeconds;
    }

    private void logRemainingWork(LocalDate date, long remainingSeconds, boolean dryRun) {
        IssueList activeIssues = restClient.getMyActiveIssues();

        List<Issue> issues = activeIssues.getIssues();
        if (issues == null || issues.isEmpty()) {
            LOG.error("No active tasks - do not know where to log time");
            return;
        }
        for (Issue issue : issues) {
            Fields fields = issue.getFields();
            TimeTracking timeTracking = fields.getTimeTracking();
            LOG.info("{} ({}), Estimate: {}m, Spent: {}m, Remaining: {}m", issue.getKey(), fields.getSummary(),
                    timeTracking.getOriginalEstimate() / 60,
                    timeTracking.getTimeSpent() / 60,
                    (timeTracking.getOriginalEstimate() - timeTracking.getTimeSpent()) / 60);
        }

        LOG.info("Calculating new work....");
        LOG.info("Remaining: {}m, {} active tasks", remainingSeconds / 60, issues.size());
        Map<String, Long> workLog = calculateNewWork(remainingSeconds, issues);
        if (workLog == null) {
            return;
        }

        for (Map.Entry<String, Long> entry : workLog.entrySet()) {
            String issue = entry.getKey();
            long minutesSpent = entry.getValue() / 60;
            LOG.info("{} - new work logged: {}m", issue, minutesSpent);
            if (!dryRun) {
                restClient.createWorkLog(issue, date, minutesSpent);
            }
        }
    }

    private Map<String, Long> calculateNewWork(long remainingSeconds, List<Issue> issues) {
        long minimumWorkSeconds = minimumWorkLogged.toSeconds();

        Map<String, Long> workLog = new HashMap<>();
        while (remainingSeconds > 0) {
            boolean loggedWork = false;
            for (Issue issue : issues) {
                Fields fields = issue.getFields();
                TimeTracking timeTracking = fields.getTimeTracking();
                long newWorkForIssue = workLog.getOrDefault(issue.getKey(), 0L);
                long remainingOfEstimate = timeTracking.getOriginalEstimate() - timeTracking.getTimeSpent() - newWorkForIssue;

                if (remainingOfEstimate > minimumWorkSeconds) {
                    if (remainingSeconds > minimumWorkSeconds) {
                        newWorkForIssue += minimumWorkSeconds;
                        remainingSeconds -= minimumWorkSeconds;
                    } else {
                        // just log remaining time
                        newWorkForIssue += remainingSeconds;
                        remainingSeconds = 0;
                    }
                    workLog.put(issue.getKey(), newWorkForIssue);
                    loggedWork = true;
                }
            }
            if (!loggedWork) {
                LOG.error("Could not find active tasks with enough remaining time in estimate. Aborting");
                return null;
            }
        }
        return workLog;
    }

    private int getIssueWorkLogTotals(Issue issue, LocalDate date, String myAccountId) {
        WorkLogList workLogList = restClient.getWorkLogDetails(issue.getKey());

        if (workLogList != null && workLogList.getWorklogs() != null) {
            return workLogList.getWorklogs().stream()
                    .filter(w -> myAccountId.equals(w.getAuthor().getAccountId()))
                    .filter(w -> date.equals(w.getStarted().toLocalDate()))
                    .mapToInt(WorkLog::getTimeSpentSeconds)
                    .sum();
        }
        return 0;
    }

    private boolean isWeekend(LocalDate localDate) {
        return localDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                localDate.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

}
