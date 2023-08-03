/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.audit.destination;

import java.util.Collection;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Collectors;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.amazonaws.services.logs.model.ResourceNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.provider.MiscUtil;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Writes audit events to Amazon CloudWatch Logs.
 * <p>
 * Two properties are required: LogGroupName and LogStreamPrefix
 * <p>
 * Thread-safety is ensured by making the log method synchronized.
 * This is to avoid possible race condition on {@link #sequenceToken} which is required in PutLogEvents API.
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutLogEvents.html">PutLogEvents API Reference</a>
 * <p>
 * Note: Amazon CloudWatch has limits on the payload size and request rate.
 * Based on the traffic, adjust the batch size and flush interval accordingly.
 * <p>
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/cloudwatch_limits_cwl.html">Amazon CloudWatch Logs Service Limits</a>
 */
@ThreadSafe
public class AmazonCloudWatchAuditDestination extends AuditDestination {

    private static Log LOG = LogFactory.getLog(AmazonCloudWatchAuditDestination.class);

    public static final String PROP_LOG_GROUP_NAME = "log_group";
    public static final String PROP_LOG_STREAM_PREFIX = "log_stream_prefix";
    public static final String CONFIG_PREFIX = "ranger.audit.amazon_cloudwatch";
    public static final String PROP_REGION = "region";

    private String logGroupName;
    private String logStreamName;
    private AWSLogs logsClient;
    private String sequenceToken;
    private String regionName;

    @Override
    public void init(Properties props, String propPrefix) {
        LOG.info("init() called for CloudWatchAuditDestination");
        super.init(props, propPrefix);

        this.logGroupName = MiscUtil.getStringProperty(props, propPrefix + "." + PROP_LOG_GROUP_NAME, "ranger_audits");
        this.logStreamName = MiscUtil.getStringProperty(props, propPrefix + "." + PROP_LOG_STREAM_PREFIX) + MiscUtil.generateUniqueId();
        this.regionName = MiscUtil.getStringProperty(props, propPrefix + "." + PROP_REGION);

        logsClient = getClient(); // Initialize client
        createLogStream();
    }

    @Override
    public void stop() {
        super.stop();
        logStatus();
    }

    @Override
    synchronized public boolean log(Collection<AuditEventBase> collection) {
        boolean ret = false;
        AWSLogs client = getClient();

        PutLogEventsRequest req = new PutLogEventsRequest()
                .withLogEvents(toInputLogEvent(collection))
                .withLogGroupName(logGroupName)
                .withLogStreamName(logStreamName);

        if (StringUtils.isNotBlank(sequenceToken)) {
            req.setSequenceToken(sequenceToken);
        }

        try {
            sequenceToken = pushLogEvents(req, false, client);
            addSuccessCount(collection.size());
            ret = true;
        } catch (Throwable e) {
            addFailedCount(collection.size());
            LOG.error("Failed to send audit events", e);
        }

        return ret;
    }

    private String pushLogEvents(PutLogEventsRequest req,
                                  boolean retryingOnInvalidSeqToken,
                                  AWSLogs client) {
        String sequenceToken;
        try {
            PutLogEventsResult re = client.putLogEvents(req);
            sequenceToken = re.getNextSequenceToken();
        } catch (ResourceNotFoundException ex) {
            if (!retryingOnInvalidSeqToken) {
                createLogStream();
                return pushLogEvents(req, true, client);
            }
            throw ex;
        } catch (InvalidSequenceTokenException ex) {
            if (retryingOnInvalidSeqToken) {
                LOG.error("Unexpected invalid sequence token. Possible race condition occurred");
                throw ex;
            }

            // LogStream may exist before first push attempt, re-obtain the sequence token
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid sequence token. Plugin possibly restarted. Updating the sequence token and retrying");
            }
            sequenceToken = ex.getExpectedSequenceToken();
            req.setSequenceToken(sequenceToken);
            return pushLogEvents(req, true, client);
        }

        return sequenceToken;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.ranger.audit.provider.AuditProvider#flush()
     */
    @Override
    public void flush() {

    }

    static Collection<InputLogEvent> toInputLogEvent(Collection<AuditEventBase> collection) {
        return collection.stream()
                .map(e -> new InputLogEvent()
                        .withMessage(MiscUtil.stringify(e))
                        .withTimestamp(e.getEventTime().getTime()))
                .sorted(Comparator.comparingLong(InputLogEvent::getTimestamp))
                .collect(Collectors.toList());
    }

    private void createLogStream() {
        AWSLogs client = getClient();
        CreateLogStreamRequest req = new CreateLogStreamRequest()
                .withLogGroupName(logGroupName)
                .withLogStreamName(logStreamName);

        LOG.info(String.format("Creating Log Stream `%s` in Log Group `%s`", logStreamName, logGroupName));
        client.createLogStream(req);
    }

    private AWSLogs getClient() {
        if (logsClient == null) {
            synchronized (AmazonCloudWatchAuditDestination.class) {
                if (logsClient == null) {
                    logsClient = newClient();
                }
            }
        }

        return logsClient;
    }

    private AWSLogs newClient() {
        if (StringUtils.isBlank(regionName)) {
            return AWSLogsClientBuilder.standard().build();
        }
        return AWSLogsClientBuilder.standard().withRegion(regionName).build();
    }
}
