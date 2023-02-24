/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.kafka;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.notification.NotificationConsumer;
import org.apache.atlas.notification.NotificationInterface;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityCreateRequest;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.apache.atlas.model.notification.HookNotification;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class KafkaNotificationTest {
    private EmbeddedKafkaServer kafkaServer;
    private KafkaNotification kafkaNotification;

    @BeforeClass
    public void setup() throws Exception {
        startNotificationServicesWithRetry();
    }

    @AfterClass
    public void shutdown() throws Exception {
        cleanUpNotificationService();
    }

    @Test
    public void testReceiveKafkaMessages() throws Exception {
        kafkaNotification.send(NotificationInterface.NotificationType.HOOK, new EntityCreateRequest("u1", new Referenceable("type")));
        kafkaNotification.send(NotificationInterface.NotificationType.HOOK, new EntityCreateRequest("u2", new Referenceable("type")));
        kafkaNotification.send(NotificationInterface.NotificationType.HOOK, new EntityCreateRequest("u3", new Referenceable("type")));
        kafkaNotification.send(NotificationInterface.NotificationType.HOOK, new EntityCreateRequest("u4", new Referenceable("type")));

        NotificationConsumer<Object>    consumer  = kafkaNotification.createConsumers(NotificationInterface.NotificationType.HOOK, 1).get(0);
        List<AtlasKafkaMessage<Object>> messages  = null ;
        long                            startTime = System.currentTimeMillis(); //fetch starting time

        while ((System.currentTimeMillis() - startTime) < 10000) {
             messages = consumer.receive();

            if (messages.size() > 0) {
                break;
            }
        }

        int i = 1;
        for (AtlasKafkaMessage<Object> msg :  messages){
            HookNotification message =  (HookNotification) msg.getMessage();

            assertEquals(message.getUser(), "u"+i++);
        }

        consumer.close();
    }

    // retry starting notification services every 2 mins for total of 20 mins
    // running parallel tests will keep the notification service ports occupied, hence retry
    void startNotificationServicesWithRetry() throws Exception {
        long totalTime = 0;
        long sleepTime = 2 * 60 * 1000; // 2 mins
        long maxTime   = 20 * 60 * 1000; // 20 mins

        while (true) {
            try {
                initNotificationService();
                break;
            } catch (Exception ex) {
                cleanUpNotificationService();

                if (totalTime >= maxTime) {
                    throw ex;
                }

                Thread.sleep(sleepTime);

                totalTime = totalTime + sleepTime;
            }
        }
    }

    void initNotificationService() throws Exception {
        Configuration applicationProperties = ApplicationProperties.get();

        applicationProperties.setProperty("atlas.kafka.data", "target/" + RandomStringUtils.randomAlphanumeric(5));

        kafkaServer       = new EmbeddedKafkaServer(applicationProperties);
        kafkaNotification = new KafkaNotification(applicationProperties);

        kafkaServer.start();
        kafkaNotification.start();

        Thread.sleep(2000);
    }

    void cleanUpNotificationService() throws Exception {
        if (kafkaNotification != null) {
            kafkaNotification.close();
            kafkaNotification.stop();
        }

        if (kafkaServer != null) {
            kafkaServer.stop();
        }
    }
}
