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

import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;

import static org.apache.ranger.audit.destination.AmazonCloudWatchAuditDestination.CONFIG_PREFIX;

public class AmazonCloudWatchAuditDestinationTest {

    @Test
    @Ignore // For manual execution only
    public void testWrite() {
        AmazonCloudWatchAuditDestination amazonCloudWatchAuditDestination = new AmazonCloudWatchAuditDestination();
        Properties properties = new Properties();
        properties.put(CONFIG_PREFIX + "." + AmazonCloudWatchAuditDestination.PROP_LOG_GROUP_NAME, "test-log-group");
        properties.put(CONFIG_PREFIX + "." + AmazonCloudWatchAuditDestination.PROP_LOG_STREAM_PREFIX, "test-log-stream");

        amazonCloudWatchAuditDestination.init(properties, CONFIG_PREFIX);

        assert amazonCloudWatchAuditDestination.log(Arrays.asList(getAuthzAuditEvent()));
    }

    private AuthzAuditEvent getAuthzAuditEvent() {
        AuthzAuditEvent event = new AuthzAuditEvent();
        event.setAccessResult((short) 1);
        event.setAccessType("");
        event.setAclEnforcer("");
        event.setAction("");
        event.setAdditionalInfo("");
        event.setAgentHostname("");
        event.setAgentId("");
        event.setClientIP("");
        event.setClusterName("");
        event.setClientType("");
        event.setEventCount(1);
        event.setEventDurationMS(1);
        event.setEventId("");
        event.setEventTime(new Date());
        event.setLogType("");
        event.setPolicyId(1);
        event.setPolicyVersion(1l);
        event.setRepositoryName("");
        event.setRequestData("");
        event.setRepositoryType(1);
        event.setResourcePath("");
        event.setResultReason("");
        event.setSeqNum(1);
        event.setSessionId("");
        event.setTags(new HashSet<>());
        event.setUser("");
        event.setZoneName("");
        return event;
    }
}
