/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.plugin.sources;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.sources.reader.SQLServerReader;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;

/**
 * Test cases for {@link SQLServerReader}.
 */
public class TestSQLServerConnect {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSQLServerConnect.class);

    /**
     * Just using in local test.
     */

    @Ignore
    public void testSqlServer() {
        JobProfile jobProfile = new JobProfile();
        jobProfile.set("job.sqlserverJob.hostname", "localhost");
        jobProfile.set("job.sqlserverJob.port", "1434");
        jobProfile.set("job.sqlserverJob.user", "sa");
        jobProfile.set("job.sqlserverJob.password", "123456");
        jobProfile.set("job.sqlserverJob.dbname", "inlong");
        jobProfile.set("job.sqlserverJob.serverName", "fullfillment");
        jobProfile.set(JobConstants.JOB_INSTANCE_ID, UUID.randomUUID().toString());
        jobProfile.set(PROXY_INLONG_GROUP_ID, UUID.randomUUID().toString());
        jobProfile.set(PROXY_INLONG_STREAM_ID, UUID.randomUUID().toString());
        SQLServerReader sqlServerReader = new SQLServerReader();
        sqlServerReader.init(jobProfile);
        while (true) {
            Message message = sqlServerReader.read();
            if (message != null) {
                LOGGER.info("event content: {}", message);
            }
        }
    }
}
