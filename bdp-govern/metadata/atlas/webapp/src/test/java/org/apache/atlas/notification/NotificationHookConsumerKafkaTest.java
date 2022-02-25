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

package org.apache.atlas.notification;

import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasException;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.kafka.*;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.util.AtlasMetricsUtil;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.notification.HookNotificationV1;
import org.apache.atlas.repository.converters.AtlasInstanceConverter;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.EntityStream;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.web.service.ServiceState;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import org.apache.commons.configuration.Configuration;
import org.apache.atlas.ApplicationProperties;
import static org.testng.Assert.*;



public class NotificationHookConsumerKafkaTest {
    public static final String NAME           = "name";
    public static final String DESCRIPTION    = "description";
    public static final String QUALIFIED_NAME = "qualifiedName";

    private NotificationInterface notificationInterface = null;
    private EmbeddedKafkaServer   kafkaServer           = null;
    private KafkaNotification     kafkaNotification     = null;


    @Mock
    private AtlasEntityStore atlasEntityStore;

    @Mock
    private ServiceState serviceState;

    @Mock
    private AtlasInstanceConverter instanceConverter;

    @Mock
    private AtlasTypeRegistry typeRegistry;

    @Mock
    private AtlasMetricsUtil metricsUtil;

    @BeforeTest
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        AtlasType                mockType   = mock(AtlasType.class);
        AtlasEntitiesWithExtInfo mockEntity = new AtlasEntitiesWithExtInfo(createV2Entity());

        when(typeRegistry.getType(anyString())).thenReturn(mockType);

        when(instanceConverter.toAtlasEntities(anyList())).thenReturn(mockEntity);

        startNotificationServicesWithRetry();
    }

    @AfterTest
    public void shutdown() {
        cleanUpNotificationService();
    }

    @Test
    public void testConsumerConsumesNewMessageWithAutoCommitDisabled() throws AtlasException, InterruptedException, AtlasBaseException {
        produceMessage(new HookNotificationV1.EntityCreateRequest("test_user1", createEntity()));

        NotificationConsumer<HookNotification> consumer                 = createNewConsumer(kafkaNotification, false);
        NotificationHookConsumer               notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
        NotificationHookConsumer.HookConsumer  hookConsumer             = notificationHookConsumer.new HookConsumer(consumer);

        consumeOneMessage(consumer, hookConsumer);

        verify(atlasEntityStore).createOrUpdate(any(EntityStream.class), anyBoolean());

        // produce another message, and make sure it moves ahead. If commit succeeded, this would work.
        produceMessage(new HookNotificationV1.EntityCreateRequest("test_user2", createEntity()));
        consumeOneMessage(consumer, hookConsumer);

        verify(atlasEntityStore,times(2)).createOrUpdate(any(EntityStream.class), anyBoolean());
        reset(atlasEntityStore);
    }

    @Test (enabled = false)
    public void consumerConsumesNewMessageButCommitThrowsAnException_MessageOffsetIsRecorded() throws AtlasException, InterruptedException, AtlasBaseException {

        ExceptionThrowingCommitConsumer        consumer                 = createNewConsumerThatThrowsExceptionInCommit(kafkaNotification, true);
        NotificationHookConsumer               notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
        NotificationHookConsumer.HookConsumer  hookConsumer             = notificationHookConsumer.new HookConsumer(consumer);

        produceMessage(new HookNotificationV1.EntityCreateRequest("test_user2", createEntity()));

        try {
            produceMessage(new HookNotificationV1.EntityCreateRequest("test_user1", createEntity()));
            consumeOneMessage(consumer, hookConsumer);
            consumeOneMessage(consumer, hookConsumer);
        }
        catch(KafkaException ex) {
            assertTrue(true, "ExceptionThrowing consumer throws an excepion.");
        }

        consumer.disableCommitExpcetion();

        produceMessage(new HookNotificationV1.EntityCreateRequest("test_user1", createEntity()));
        consumeOneMessage(consumer, hookConsumer);
        consumeOneMessage(consumer, hookConsumer);

        reset(atlasEntityStore);
    }

    @Test(dependsOnMethods = "testConsumerConsumesNewMessageWithAutoCommitDisabled")
    public void testConsumerRemainsAtSameMessageWithAutoCommitEnabled() throws Exception {
        produceMessage(new HookNotificationV1.EntityCreateRequest("test_user3", createEntity()));

        NotificationConsumer<HookNotification> consumer = createNewConsumer(kafkaNotification, true);

        assertNotNull (consumer);

        NotificationHookConsumer              notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
        NotificationHookConsumer.HookConsumer hookConsumer             = notificationHookConsumer.new HookConsumer(consumer);

        consumeOneMessage(consumer, hookConsumer);
        verify(atlasEntityStore).createOrUpdate(any(EntityStream.class), anyBoolean());

        // produce another message, but this will not be consumed, as commit code is not executed in hook consumer.
        produceMessage(new HookNotificationV1.EntityCreateRequest("test_user4", createEntity()));

        consumeOneMessage(consumer, hookConsumer);
        verify(atlasEntityStore,times(2)).createOrUpdate(any(EntityStream.class), anyBoolean());
    }

    AtlasKafkaConsumer<HookNotification> createNewConsumer(KafkaNotification kafkaNotification, boolean autoCommitEnabled) {
        return (AtlasKafkaConsumer) kafkaNotification.createConsumers(NotificationInterface.NotificationType.HOOK, 1, autoCommitEnabled).get(0);
    }

    ExceptionThrowingCommitConsumer createNewConsumerThatThrowsExceptionInCommit(KafkaNotification kafkaNotification, boolean autoCommitEnabled) {
        Properties prop = kafkaNotification.getConsumerProperties(NotificationInterface.NotificationType.HOOK);

        prop.put("enable.auto.commit", autoCommitEnabled);

        KafkaConsumer consumer = kafkaNotification.getOrCreateKafkaConsumer(null, prop, NotificationInterface.NotificationType.HOOK, 0);
        return new ExceptionThrowingCommitConsumer(NotificationInterface.NotificationType.HOOK, consumer, autoCommitEnabled, 1000);
    }

    void consumeOneMessage(NotificationConsumer<HookNotification> consumer,
                           NotificationHookConsumer.HookConsumer hookConsumer) throws InterruptedException {
        try {
            long startTime = System.currentTimeMillis(); //fetch starting time

            while ((System.currentTimeMillis() - startTime) < 10000) {
                List<AtlasKafkaMessage<HookNotification>> messages = consumer.receive();

                for (AtlasKafkaMessage<HookNotification> msg : messages) {
                    hookConsumer.handleMessage(msg);
                }

                if (messages.size() > 0) {
                    break;
                }
            }
        } catch (AtlasServiceException | AtlasException e) {
            Assert.fail("Consumer failed with exception ", e);
        }
    }

    Referenceable createEntity() {
        final Referenceable entity = new Referenceable(AtlasClient.DATA_SET_SUPER_TYPE);

        entity.set(NAME, "db" + randomString());
        entity.set(DESCRIPTION, randomString());
        entity.set(QUALIFIED_NAME, randomString());

        return entity;
    }

    AtlasEntity createV2Entity() {
        final AtlasEntity entity = new AtlasEntity(AtlasClient.DATA_SET_SUPER_TYPE);

        entity.setAttribute(NAME, "db" + randomString());
        entity.setAttribute(DESCRIPTION, randomString());
        entity.setAttribute(QUALIFIED_NAME, randomString());

        return entity;
    }

    protected String randomString() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private void produceMessage(HookNotification message) throws NotificationException {
        kafkaNotification.send(NotificationInterface.NotificationType.HOOK, message);
    }

    // retry starting notification services every 2 mins for total of 30 mins
    // running parallel tests will keep the notification service ports occupied, hence retry
    void startNotificationServicesWithRetry() throws Exception {
        long totalTime = 0;
        long sleepTime = 2 * 60 * 1000; // 2 mins
        long maxTime   = 30 * 60 * 1000; // 30 mins

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

    void initNotificationService() throws AtlasException, InterruptedException {
        Configuration applicationProperties = ApplicationProperties.get();

        applicationProperties.setProperty("atlas.kafka.data", "target/" + RandomStringUtils.randomAlphanumeric(5));

        kafkaServer           = new EmbeddedKafkaServer(applicationProperties);
        kafkaNotification     = new KafkaNotification(applicationProperties);
        notificationInterface = kafkaNotification;

        kafkaServer.start();
        kafkaNotification.start();

        Thread.sleep(2000);
    }

    void cleanUpNotificationService() {
        if (kafkaNotification != null) {
            kafkaNotification.close();
            kafkaNotification.stop();
        }

        if (kafkaServer != null) {
            kafkaServer.stop();
        }
    }

    private static class ExceptionThrowingCommitConsumer extends AtlasKafkaConsumer {

        private boolean exceptionThrowingEnabled;

        public ExceptionThrowingCommitConsumer(NotificationInterface.NotificationType notificationType,
                                               KafkaConsumer kafkaConsumer, boolean autoCommitEnabled, long pollTimeoutMilliSeconds) {
            super(notificationType, kafkaConsumer, autoCommitEnabled, pollTimeoutMilliSeconds);
            exceptionThrowingEnabled = true;
        }

        @Override
        public void commit(TopicPartition partition, long offset) {
            if(exceptionThrowingEnabled) {
                throw new KafkaException("test case verifying exception");
            }
            else {
                super.commit(partition, offset);
            }
        }

        public void disableCommitExpcetion() {
            exceptionThrowingEnabled = false;
        }
    }
}
