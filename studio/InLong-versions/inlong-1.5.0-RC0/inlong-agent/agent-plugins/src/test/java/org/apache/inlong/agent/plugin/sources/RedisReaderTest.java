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
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.sources.reader.RedisReader;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for redis reader
 */
public class RedisReaderTest {

    private static Logger LOGGER = LoggerFactory.getLogger(RedisReaderTest.class);

    /**
     * this test is used for testing collect data from redis in unit test
     * just use in local test
     */
    @Ignore
    public void redisLoadTest() {
        JobProfile jobProfile = new JobProfile();
        jobProfile.set(RedisReader.JOB_REDIS_HOSTNAME, "localhost");
        jobProfile.set(RedisReader.JOB_REDIS_PORT, "6379");
        jobProfile.set(RedisReader.JOB_REDIS_OFFSET, "7945");
        jobProfile.set(RedisReader.JOB_REDIS_REPLID, "33a016fc98ad27f36218c7648a7a0774a79547d8");
        jobProfile.set("job.instance.id", "_1");
        RedisReader redisReader = new RedisReader();
        redisReader.init(jobProfile);
        while (true) {
            Message message = redisReader.read();
            if (message != null) {
                LOGGER.info("read message is {}", message);
                break;
            }
        }
    }

}
