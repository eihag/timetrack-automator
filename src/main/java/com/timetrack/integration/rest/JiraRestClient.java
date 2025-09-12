package com.timetrack.integration.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.timetrack.integration.model.IssueList;
import com.timetrack.integration.model.LogWorkRequest;
import com.timetrack.integration.model.WorkLogList;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.util.UriEncoder;

import java.time.Duration;
import java.time.LocalDate;
import java.util.logging.Level;


@Component
public class JiraRestClient {
    private static final Logger LOG = LoggerFactory.getLogger(JiraRestClient.class);

    private static final String FIELD_LIST = "&fields=assignee,timetracking,summary&failFast=true";

    @Value("${jira_hostname}")
    private String hostname;

    @Value("${jira_username}")
    private String username;

    @Value("${jira_apikey}")
    private String apikey;

    @Value("${jira_active_state}")
    private String activeState;

    @Value("${rest_debug_log:false}")
    private boolean restClientDebug;


    @Value("#{T(java.time.Duration).parse('${rest_client_timeout}')}")
    private Duration restClientTimeout;


    private Client client;
    private String encodedCredentials;


    @PostConstruct
    private void init() {
        client = buildClient(restClientTimeout, restClientTimeout, restClientDebug);
        encodedCredentials = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + apikey).getBytes());
    }

    public IssueList getMyIssuesWithWorkLoggedForDate(LocalDate date) {
        return call("/rest/api/3/search/jql?jql=worklogAuthor='" + username + "'%20AND%20worklogDate='" + date + "'" + FIELD_LIST, null, HttpMethod.GET, IssueList.class);
    }

    public IssueList getMyActiveIssues() {
        return call("/rest/api/3/search/jql?jql=assignee='" + username + "'%20AND%20status='" + UriEncoder.encode(activeState) + "'" + FIELD_LIST, null, HttpMethod.GET, IssueList.class);
    }

    public WorkLogList getWorkLogDetails(String issueKey) {
        return call("/rest/api/3/issue/" + issueKey + "/worklog", null, HttpMethod.GET, WorkLogList.class);
    }

    public void createWorkLog(String issueKey, LocalDate workingDate, long minutesSpent) {
        if (isToday(workingDate)) {
            workingDate = null;
        }
        LogWorkRequest logWork = new LogWorkRequest(minutesSpent + "m", workingDate);
        call("/rest/internal/3/issue/" + issueKey + "/worklog?adjustEstimate=auto", Entity.json(logWork), HttpMethod.POST, Void.class);
    }

    private <T> T call(String endpoint, Entity<?> entity, String httpMethod, Class<T> clazz) {
        try {
            Invocation.Builder request = this.client.target("https://" + hostname + endpoint)
                    .request()
                    .header("authorization", encodedCredentials);
            Response response = request.method(httpMethod, entity);
            verifyHttpCode(response);
            return response.readEntity(clazz);
        } catch (Exception e) {
            LOG.info("Failed to make HTTP request: {}", e.getMessage());
            throw e;
        }
    }

    private void verifyHttpCode(Response response) {
        if (response.getStatus() != 200 && response.getStatus() != 201) {
            String message = "Unexpected HTTP code: " + response.getStatus();
            LOG.error(message);
            throw new RuntimeException(message);
        }
    }

    private boolean isToday(LocalDate workingDate) {
        LocalDate today = LocalDate.now();
        return workingDate.equals(today);
    }

    private Client buildClient(Duration clientReadTimeout, Duration clientConnectTimeout, boolean clientDebug) {
        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.CONNECT_TIMEOUT, (int) clientConnectTimeout.toMillis());
        config.property(ClientProperties.READ_TIMEOUT, (int) clientReadTimeout.toMillis());

        ClientBuilder clientBuilder = ClientBuilder
                .newBuilder()
                .withConfig(config);

        if (clientDebug) {
            int logEntrySize = 128 * 1024;
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(this.getClass().toString());
            clientBuilder.register(new LoggingFeature(logger, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_TEXT, logEntrySize));
        }
        Client newClient = clientBuilder.build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        StdDateFormat dateFormat = new StdDateFormat().withColonInTimeZone(false);
        mapper.setDateFormat(dateFormat);

        mapper.registerModule(new JavaTimeModule());

        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper(mapper);
        newClient.register(jacksonProvider);

        return newClient;
    }
}
