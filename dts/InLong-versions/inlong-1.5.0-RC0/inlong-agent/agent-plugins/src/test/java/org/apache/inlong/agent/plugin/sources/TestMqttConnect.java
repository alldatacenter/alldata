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
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.sources.reader.MqttReader;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

public class TestMqttConnect {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestMqttConnect.class);

    /**
     * Just using in local test.
     */
    @Ignore
    public void testMqttReader() throws Exception {
        JobProfile jobProfile = JobProfile.parseJsonStr("{}");
        jobProfile.set(MqttReader.JOB_MQTT_SERVER_URI, "tcp://broker.hivemq.com:1883");
        jobProfile.set(MqttReader.JOB_MQTT_CLIENT_ID_PREFIX, "mqtt_client");
        jobProfile.set(MqttReader.JOB_MQTT_USERNAME, "test");
        jobProfile.set(MqttReader.JOB_MQTT_PASSWORD, "test");
        jobProfile.set(MqttSource.JOB_MQTTJOB_TOPICS, "testtopic/mqtt/p1/ebr/delivered,testtopic/NARTU2");
        jobProfile.set(MqttReader.JOB_MQTT_QOS, "0");
        jobProfile.set("job.instance.id", "_1");

        final MqttSource source = new MqttSource();
        List<Reader> readers = source.split(jobProfile);
        ExecutorService threadPool = Executors.newFixedThreadPool(5);
        for (Reader reader : readers) {
            threadPool.submit(new Runnable() {

                @Override
                public void run() {
                    reader.init(jobProfile);
                    while (!reader.isFinished()) {
                        Message message = reader.read();
                        if (Objects.nonNull(message)) {
                            assertNotNull(message.getBody());
                            LOGGER.info("the mqtt reader header: {}, message: {}", message.getHeader(), message);
                        }
                    }
                }
            });
        }
        TimeUnit.SECONDS.sleep(60);
    }
}
