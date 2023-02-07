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
import org.apache.inlong.agent.plugin.sources.reader.OracleReader;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;

/**
 * Test cases for {@link OracleReader}.
 */
public class TestOracleConnect {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestOracleConnect.class);

    @Ignore
    public void testOracle() {
        JobProfile jobProfile = new JobProfile();
        jobProfile.set("job.oracleJob.hostname", "localhost");
        jobProfile.set("job.oracleJob.port", "1521");
        jobProfile.set("job.oracleJob.user", "c##dbzuser");
        jobProfile.set("job.oracleJob.password", "dbz");
        jobProfile.set("job.oracleJob.sid", "ORCLCDB");
        jobProfile.set("job.oracleJob.dbname", "ORCLCDB");
        jobProfile.set("job.oracleJob.serverName", "server1");
        jobProfile.set(JobConstants.JOB_INSTANCE_ID, UUID.randomUUID().toString());
        jobProfile.set(PROXY_INLONG_GROUP_ID, UUID.randomUUID().toString());
        jobProfile.set(PROXY_INLONG_STREAM_ID, UUID.randomUUID().toString());
        OracleReader oracleReader = new OracleReader();
        oracleReader.init(jobProfile);
        while (true) {
            Message message = oracleReader.read();
            if (message != null) {
                LOGGER.info("event content: {}", message);
            }
        }
    }
}
