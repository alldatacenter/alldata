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

import org.apache.atlas.AtlasException;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.atlas.kafka.AtlasKafkaMessage;
import org.apache.atlas.kafka.KafkaNotification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.notification.HookNotification.HookNotificationType;
import org.apache.atlas.notification.NotificationInterface.NotificationType;
import org.apache.atlas.util.AtlasMetricsUtil;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityCreateRequest;
import org.apache.atlas.repository.converters.AtlasInstanceConverter;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.EntityStream;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.web.service.ServiceState;
import org.apache.commons.configuration.Configuration;
import org.apache.kafka.common.TopicPartition;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class NotificationHookConsumerTest {
    @Mock
    private NotificationInterface notificationInterface;

    @Mock
    private Configuration configuration;

    @Mock
    private ExecutorService executorService;

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

    @BeforeMethod
    public void setup() throws AtlasBaseException {
        MockitoAnnotations.initMocks(this);

        AtlasType                mockType   = mock(AtlasType.class);
        AtlasEntitiesWithExtInfo mockEntity = new AtlasEntitiesWithExtInfo(mock(AtlasEntity.class));

        when(typeRegistry.getType(anyString())).thenReturn(mockType);
        when(instanceConverter.toAtlasEntities(anyList())).thenReturn(mockEntity);

        EntityMutationResponse mutationResponse = mock(EntityMutationResponse.class);

        when(atlasEntityStore.createOrUpdate(any(EntityStream.class), anyBoolean())).thenReturn(mutationResponse);
    }

    @Test
    public void testConsumerCanProceedIfServerIsReady() throws Exception {
        NotificationHookConsumer              notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
        NotificationHookConsumer.HookConsumer hookConsumer             = notificationHookConsumer.new HookConsumer(mock(NotificationConsumer.class));
        NotificationHookConsumer.Timer        timer                    = mock(NotificationHookConsumer.Timer.class);

        when(serviceState.getState()).thenReturn(ServiceState.ServiceStateValue.ACTIVE);

        assertTrue(hookConsumer.serverAvailable(timer));

        verifyZeroInteractions(timer);
    }

    @Test
    public void testConsumerWaitsNTimesIfServerIsNotReadyNTimes() throws Exception {
        NotificationHookConsumer              notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
        NotificationHookConsumer.HookConsumer hookConsumer             = notificationHookConsumer.new HookConsumer(mock(NotificationConsumer.class));
        NotificationHookConsumer.Timer        timer                    = mock(NotificationHookConsumer.Timer.class);

        when(serviceState.getState())
                .thenReturn(ServiceState.ServiceStateValue.PASSIVE)
                .thenReturn(ServiceState.ServiceStateValue.PASSIVE)
                .thenReturn(ServiceState.ServiceStateValue.PASSIVE)
                .thenReturn(ServiceState.ServiceStateValue.ACTIVE);

        assertTrue(hookConsumer.serverAvailable(timer));

        verify(timer, times(3)).sleep(NotificationHookConsumer.SERVER_READY_WAIT_TIME_MS);
    }

    @Test
    public void testCommitIsCalledWhenMessageIsProcessed() throws AtlasServiceException, AtlasException {
        NotificationHookConsumer               notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
        NotificationConsumer                   consumer                 = mock(NotificationConsumer.class);
        NotificationHookConsumer.HookConsumer  hookConsumer             = notificationHookConsumer.new HookConsumer(consumer);
        EntityCreateRequest                    message                  = mock(EntityCreateRequest.class);
        Referenceable                          mock                     = mock(Referenceable.class);

        when(message.getUser()).thenReturn("user");
        when(message.getType()).thenReturn(HookNotificationType.ENTITY_CREATE);
        when(message.getEntities()).thenReturn(Arrays.asList(mock));

        hookConsumer.handleMessage(new AtlasKafkaMessage(message, -1, KafkaNotification.ATLAS_HOOK_TOPIC, -1));

        verify(consumer).commit(any(TopicPartition.class), anyLong());
    }

    @Test
    public void testCommitIsNotCalledEvenWhenMessageProcessingFails() throws AtlasServiceException, AtlasException, AtlasBaseException {
        NotificationHookConsumer              notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
        NotificationConsumer                  consumer                 = mock(NotificationConsumer.class);
        NotificationHookConsumer.HookConsumer hookConsumer             = notificationHookConsumer.new HookConsumer(consumer);
        EntityCreateRequest                   message                  = new EntityCreateRequest("user", Collections.singletonList(mock(Referenceable.class)));

        when(atlasEntityStore.createOrUpdate(any(EntityStream.class), anyBoolean())).thenThrow(new RuntimeException("Simulating exception in processing message"));

        hookConsumer.handleMessage(new AtlasKafkaMessage(message, -1, KafkaNotification.ATLAS_HOOK_TOPIC, -1));

        verifyZeroInteractions(consumer);
    }

    @Test
    public void testConsumerProceedsWithFalseIfInterrupted() throws Exception {
        NotificationHookConsumer              notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
        NotificationHookConsumer.HookConsumer hookConsumer             = notificationHookConsumer.new HookConsumer(mock(NotificationConsumer.class));
        NotificationHookConsumer.Timer        timer                    = mock(NotificationHookConsumer.Timer.class);

        doThrow(new InterruptedException()).when(timer).sleep(NotificationHookConsumer.SERVER_READY_WAIT_TIME_MS);
        when(serviceState.getState()).thenReturn(ServiceState.ServiceStateValue.PASSIVE);

        assertFalse(hookConsumer.serverAvailable(timer));
    }

    @Test
    public void testConsumersStartedIfHAIsDisabled() throws Exception {
        List<NotificationConsumer<Object>> consumers = new ArrayList();
        NotificationConsumer               notificationConsumerMock = mock(NotificationConsumer.class);

        consumers.add(notificationConsumerMock);

        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY, false)).thenReturn(false);
        when(configuration.getInt(NotificationHookConsumer.CONSUMER_THREADS_PROPERTY, 1)).thenReturn(1);
        when(notificationInterface.createConsumers(NotificationType.HOOK, 1)).thenReturn(consumers);
        NotificationHookConsumer notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
        notificationHookConsumer.startInternal(configuration, executorService);

        verify(notificationInterface).createConsumers(NotificationType.HOOK, 1);
        verify(executorService).submit(any(NotificationHookConsumer.HookConsumer.class));
    }

    @Test
    public void testConsumersAreNotStartedIfHAIsEnabled() throws Exception {
        List<NotificationConsumer<Object>> consumers = new ArrayList();
        NotificationConsumer               notificationConsumerMock = mock(NotificationConsumer.class);

        consumers.add(notificationConsumerMock);

        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getInt(NotificationHookConsumer.CONSUMER_THREADS_PROPERTY, 1)).thenReturn(1);
        when(notificationInterface.createConsumers(NotificationType.HOOK, 1)).thenReturn(consumers);
        NotificationHookConsumer notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);

        notificationHookConsumer.startInternal(configuration, executorService);

        verifyZeroInteractions(notificationInterface);
    }

    @Test
    public void testConsumersAreStartedWhenInstanceBecomesActive() throws Exception {
        List<NotificationConsumer<Object>> consumers = new ArrayList();
        NotificationConsumer               notificationConsumerMock = mock(NotificationConsumer.class);

        consumers.add(notificationConsumerMock);

        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getInt(NotificationHookConsumer.CONSUMER_THREADS_PROPERTY, 1)).thenReturn(1);
        when(notificationInterface.createConsumers(NotificationType.HOOK, 1)).thenReturn(consumers);

        NotificationHookConsumer notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);

        notificationHookConsumer.startInternal(configuration, executorService);
        notificationHookConsumer.instanceIsActive();

        verify(notificationInterface).createConsumers(NotificationType.HOOK, 1);
        verify(executorService).submit(any(NotificationHookConsumer.HookConsumer.class));
    }

    @Test
    public void testConsumersAreStoppedWhenInstanceBecomesPassive() throws Exception {
        List<NotificationConsumer<Object>> consumers = new ArrayList();
        NotificationConsumer               notificationConsumerMock = mock(NotificationConsumer.class);

        consumers.add(notificationConsumerMock);

        when(serviceState.getState()).thenReturn(ServiceState.ServiceStateValue.ACTIVE);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY, false)).thenReturn(true);
        when(configuration.getInt(NotificationHookConsumer.CONSUMER_THREADS_PROPERTY, 1)).thenReturn(1);
        when(notificationInterface.createConsumers(NotificationType.HOOK, 1)).thenReturn(consumers);
        final NotificationHookConsumer notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                notificationHookConsumer.consumers.get(0).start();
                Thread.sleep(500);

                return null;
            }
        }).when(executorService).submit(any(NotificationHookConsumer.HookConsumer.class));

        notificationHookConsumer.startInternal(configuration, executorService);
        notificationHookConsumer.instanceIsPassive();

        verify(notificationInterface).close();
        verify(executorService).shutdown();
        verify(notificationConsumerMock).wakeup();
    }

    @Test
    public void consumersStoppedBeforeStarting() throws Exception {
        List<NotificationConsumer<Object>> consumers                = new ArrayList();
        NotificationConsumer               notificationConsumerMock = mock(NotificationConsumer.class);

        consumers.add(notificationConsumerMock);

        when(serviceState.getState()).thenReturn(ServiceState.ServiceStateValue.ACTIVE);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY, false)).thenReturn(true);
        when(configuration.getInt(NotificationHookConsumer.CONSUMER_THREADS_PROPERTY, 1)).thenReturn(1);
        when(notificationInterface.createConsumers(NotificationType.HOOK, 1)).thenReturn(consumers);
        final NotificationHookConsumer notificationHookConsumer = new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);

        notificationHookConsumer.startInternal(configuration, executorService);
        notificationHookConsumer.instanceIsPassive();

        verify(notificationInterface).close();
        verify(executorService).shutdown();
    }

    @Test
    public void consumersThrowsIllegalStateExceptionThreadUsesPauseRetryLogic() throws Exception {
        final NotificationHookConsumer notificationHookConsumer = setupNotificationHookConsumer();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                notificationHookConsumer.consumers.get(0).start();
                Thread.sleep(1000);

                return null;
            }
        }).when(executorService).submit(any(NotificationHookConsumer.HookConsumer.class));

        notificationHookConsumer.startInternal(configuration, executorService);
        Thread.sleep(1000);

        assertTrue(notificationHookConsumer.consumers.get(0).isAlive());

        notificationHookConsumer.consumers.get(0).shutdown();
    }

    @Test
    public void consumersThrowsIllegalStateExceptionPauseRetryLogicIsInterrupted() throws Exception {
        final NotificationHookConsumer notificationHookConsumer = setupNotificationHookConsumer();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                notificationHookConsumer.consumers.get(0).start();
                Thread.sleep(500);

                return null;
            }
        }).when(executorService).submit(any(NotificationHookConsumer.HookConsumer.class));

        notificationHookConsumer.startInternal(configuration, executorService);
        Thread.sleep(500);

        notificationHookConsumer.consumers.get(0).shutdown();
        Thread.sleep(500);

        assertFalse(notificationHookConsumer.consumers.get(0).isAlive());
    }

    private NotificationHookConsumer setupNotificationHookConsumer() throws AtlasException {
        List<NotificationConsumer<Object>> consumers                = new ArrayList();
        NotificationConsumer               notificationConsumerMock = mock(NotificationConsumer.class);

        consumers.add(notificationConsumerMock);

        when(serviceState.getState()).thenReturn(ServiceState.ServiceStateValue.ACTIVE);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY, false)).thenReturn(true);
        when(configuration.getInt(NotificationHookConsumer.CONSUMER_THREADS_PROPERTY, 1)).thenReturn(1);
        when(notificationConsumerMock.receive()).thenThrow(new IllegalStateException());
        when(notificationInterface.createConsumers(NotificationType.HOOK, 1)).thenReturn(consumers);
        return new NotificationHookConsumer(notificationInterface, atlasEntityStore, serviceState, instanceConverter, typeRegistry, metricsUtil, null);
    }
}
