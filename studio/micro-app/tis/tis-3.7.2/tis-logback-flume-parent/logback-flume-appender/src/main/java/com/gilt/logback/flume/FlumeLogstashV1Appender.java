package com.gilt.logback.flume;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.apache.commons.lang.StringUtils;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.apache.flume.event.EventBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;

public class FlumeLogstashV1Appender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    protected static final Charset UTF_8 = Charset.forName("UTF-8");

    private FlumeAvroManager flumeManager;

    private String flumeAgents;

    private String flumeProperties;

    private Long reportingWindow;

    private Integer batchSize;

    private Integer reporterMaxThreadPoolSize;

    private Integer reporterMaxQueueSize;

    private Map<String, String> additionalAvroHeaders;

    private String application;

    protected Layout<ILoggingEvent> layout;

    private String hostname;

    private String type;

    public void setType(String type) {
        this.type = type;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public void setFlumeAgents(String flumeAgents) {
        this.flumeAgents = flumeAgents;
    }

    public void setFlumeProperties(String flumeProperties) {
        this.flumeProperties = flumeProperties;
    }

    public void setAdditionalAvroHeaders(String additionalHeaders) {
        this.additionalAvroHeaders = extractProperties(additionalHeaders);
    }

    public void setBatchSize(String batchSizeStr) {
        try {
            this.batchSize = Integer.parseInt(batchSizeStr);
        } catch (NumberFormatException nfe) {
            addWarn("Cannot set the batchSize to " + batchSizeStr, nfe);
        }
    }

    public void setReportingWindow(String reportingWindowStr) {
        try {
            this.reportingWindow = Long.parseLong(reportingWindowStr);
        } catch (NumberFormatException nfe) {
            addWarn("Cannot set the reportingWindow to " + reportingWindowStr, nfe);
        }
    }

    public void setReporterMaxThreadPoolSize(String reporterMaxThreadPoolSizeStr) {
        try {
            this.reporterMaxThreadPoolSize = Integer.parseInt(reporterMaxThreadPoolSizeStr);
        } catch (NumberFormatException nfe) {
            addWarn("Cannot set the reporterMaxThreadPoolSize to " + reporterMaxThreadPoolSizeStr,
                    nfe);
        }
    }

    public void setReporterMaxQueueSize(String reporterMaxQueueSizeStr) {
        try {
            this.reporterMaxQueueSize = Integer.parseInt(reporterMaxQueueSizeStr);
        } catch (NumberFormatException nfe) {
            addWarn("Cannot set the reporterMaxQueueSize to " + reporterMaxQueueSizeStr, nfe);
        }
    }

    @Override
    public void start() {
        if (layout == null) {
            addWarn("Layout was not defined, will only log the message, no stack traces or custom layout");
        }
        if (StringUtils.isEmpty(application)) {
            application = resolveApplication();
        }

        if (StringUtils.isNotEmpty(flumeAgents)) {
            String[] agentConfigs = flumeAgents.split(",");

            List<RemoteFlumeAgent> agents = new ArrayList<RemoteFlumeAgent>(agentConfigs.length);
            for (String conf : agentConfigs) {
                RemoteFlumeAgent agent = RemoteFlumeAgent.fromString(conf.trim());
                if (agent != null) {
                    agents.add(agent);
                } else {
                    addWarn("Cannot build a Flume agent config for '" + conf + "'");
                }
            }
            Properties overrides = new Properties();
            overrides.putAll(extractProperties(flumeProperties));
            flumeManager = FlumeAvroManager.create(agents, overrides, batchSize, reportingWindow,
                    reporterMaxThreadPoolSize, reporterMaxQueueSize, this);
        } else {
            addError("Cannot configure a flume agent with an empty configuration");
            throw new RuntimeException("Cannot configure a flume agent with an empty configuration");
        }
        super.start();

    }

    private Map<String, String> extractProperties(String propertiesAsString) {
        final Map<String, String> props = new HashMap<String, String>();// createHeaders();
        if (StringUtils.isNotEmpty(propertiesAsString)) {
            final String[] segments = propertiesAsString.split(";");
            for (final String segment : segments) {
                final String[] pair = segment.split("=");
                if (pair.length == 2) {
                    final String key = StringUtils.strip(pair[0]);
                    final String value = StringUtils.strip(pair[1]);
                    if (StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value)) {
                        props.put(key, value);
                    } else {
                        addWarn("Empty key or value not accepted: " + segment);
                    }
                } else {
                    addWarn("Not a valid {key}:{value} format: " + segment);
                }
            }
        } else {
            addInfo("Not overriding any flume agent properties");
        }

        return props;
    }

    @Override
    public void stop() {
        try {
            if (flumeManager != null) {
                flumeManager.stop();
            }
        } catch (FlumeException fe) {
            addWarn(fe.getMessage(), fe);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {

        if (flumeManager != null) {
            try {
                String body = layout != null ? layout.doLayout(eventObject)
                        : eventObject.getFormattedMessage();
                Map<String, String> headers = createHeaders();
                if (additionalAvroHeaders != null) {
                    headers.putAll(additionalAvroHeaders);
                }
                headers.putAll(extractHeaders(eventObject));

                Event event = EventBuilder.withBody(StringUtils.strip(body), UTF_8, headers);
                preSend(event);
                flumeManager.send(event);
            } catch (Exception e) {
                addError(e.getLocalizedMessage(), e);
            }
        }

    }

    protected void preSend(Event event) {

    }

    protected HashMap<String, String> createHeaders() {
        return new HashMap<String, String>();
    }

    protected Map<String, String> extractHeaders(ILoggingEvent eventObject) {
        Map<String, String> headers = new HashMap<String, String>(10);
        headers.put("timestamp", Long.toString(eventObject.getTimeStamp()));
        headers.put("type", eventObject.getLevel().toString());
        headers.put("logger", eventObject.getLoggerName());
        headers.put("message", eventObject.getMessage());
        headers.put("level", eventObject.getLevel().toString());
        try {
            headers.put("host", resolveHostname());
        } catch (UnknownHostException e) {
            addWarn(e.getMessage());
        }
        headers.put("thread", eventObject.getThreadName());
        if (StringUtils.isNotEmpty(application)) {
            headers.put("application", application);
        }

        if (StringUtils.isNotEmpty(type)) {
            headers.put("type", type);
        }

        return headers;
    }

    protected String resolveHostname() throws UnknownHostException {
        return hostname != null ? hostname : InetAddress.getLocalHost().getHostName();
    }

    private String resolveApplication() {
        return System.getProperty("application.name");
    }
}
