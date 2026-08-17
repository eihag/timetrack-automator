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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimetrackService {
    private static final Logger LOG = LoggerFactory.getLogger(TimetrackService.class);

    @Value("#{T(java.time.Duration).parse('${work_per_day}')}")
    private Duration workPerDay;
    @Value("#{T(java.time.Duration).parse('${minimum_work_logged}')}")
    private Duration minimumWorkLogged;
    @Value("#{T(java.time.Duration).parse('${small_task_threshold:PT2H}')}")
    private Duration smallTaskThreshold;

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

    Map<String, Long> calculateNewWork(long remainingSeconds, List<Issue> issues) {
        long minimumWorkSeconds = minimumWorkLogged.toSeconds();
        long smallTaskSeconds = smallTaskThreshold.toSeconds();

        Map<String, Long> workLog = new LinkedHashMap<>();

        // First pass: close small tasks completely, smallest first, so they do not
        // linger for days while the hours keep flowing to the big estimates
        List<Issue> smallestFirst = issues.stream()
                .sorted(Comparator.comparingLong(issue -> remainingOfEstimate(issue, workLog)))
                .toList();
        for (Issue issue : smallestFirst) {
            if (remainingSeconds <= 0) {
                break;
            }
            long remainingOfEstimate = remainingOfEstimate(issue, workLog);
            if (remainingOfEstimate > 0 && remainingOfEstimate <= smallTaskSeconds) {
                remainingSeconds -= addWork(workLog, issue, Math.min(remainingOfEstimate, remainingSeconds));
            }
        }

        // Second pass: spread what is left over the bigger tasks in minimum sized chunks
        while (remainingSeconds > 0) {
            boolean loggedWork = false;
            for (Issue issue : issues) {
                if (remainingSeconds <= 0) {
                    break;
                }
                long remainingOfEstimate = remainingOfEstimate(issue, workLog);
                if (remainingOfEstimate >= minimumWorkSeconds) {
                    long newWork = Math.min(Math.min(minimumWorkSeconds, remainingSeconds), remainingOfEstimate);
                    remainingSeconds -= addWork(workLog, issue, newWork);
                    loggedWork |= newWork > 0;
                }
            }
            if (!loggedWork) {
                LOG.error("Could not find active tasks with enough remaining time in estimate. Aborting");
                return null;
            }
        }
        return workLog;
    }

    private static long addWork(Map<String, Long> workLog, Issue issue, long newWork) {
        workLog.merge(issue.getKey(), newWork, Long::sum);
        return newWork;
    }

    private static long remainingOfEstimate(Issue issue, Map<String, Long> workLog) {
        TimeTracking timeTracking = issue.getFields().getTimeTracking();
        long alreadyPlanned = workLog.getOrDefault(issue.getKey(), 0L);
        return timeTracking.getOriginalEstimate() - timeTracking.getTimeSpent() - alreadyPlanned;
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
