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

package org.apache.atlas.web.service;

import org.apache.atlas.AtlasConstants;
import org.apache.atlas.AtlasException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.atlas.listener.ActiveStateChangeHandler;
import org.apache.atlas.util.AtlasMetricsUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

public class ActiveInstanceElectorServiceTest {

    @Mock
    private Configuration configuration;

    @Mock
    private CuratorFactory curatorFactory;

    @Mock
    private ActiveInstanceState activeInstanceState;

    @Mock
    private ServiceState serviceState;

    @Mock
    private AtlasMetricsUtil metricsUtil;

    @BeforeMethod
    public void setup() {
        System.setProperty(AtlasConstants.SYSTEM_PROPERTY_APP_PORT, "21000");
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLeaderElectionIsJoinedOnStart() throws Exception {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);
        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(), curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.start();

        verify(leaderLatch).start();
    }

    @Test
    public void testListenerIsAddedForActiveInstanceCallbacks() throws Exception {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);

        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(), curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.start();

        verify(leaderLatch).addListener(activeInstanceElectorService);
    }

    @Test
    public void testLeaderElectionIsNotStartedIfNotInHAMode() throws AtlasException {
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY, false)).thenReturn(false);

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(), curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.start();

        verifyZeroInteractions(curatorFactory);
    }

    @Test
    public void testLeaderElectionIsLeftOnStop() throws IOException, AtlasException {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);

        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(), curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.start();
        activeInstanceElectorService.stop();

        verify(leaderLatch).close();
    }

    @Test
    public void testCuratorFactoryIsClosedOnStop() throws AtlasException {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);

        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(), curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.start();
        activeInstanceElectorService.stop();

        verify(curatorFactory).close();
    }

    @Test
    public void testNoActionOnStopIfHAModeIsDisabled() {

        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY, false)).thenReturn(false);

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(), curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.stop();

        verifyZeroInteractions(curatorFactory);
    }

    @Test
    public void testRegisteredHandlersAreNotifiedWhenInstanceIsActive() throws AtlasException {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);

        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        Set<ActiveStateChangeHandler> changeHandlers = new HashSet<>();
        final ActiveStateChangeHandler handler1 = mock(ActiveStateChangeHandler.class);
        final ActiveStateChangeHandler handler2 = mock(ActiveStateChangeHandler.class);

        changeHandlers.add(handler1);
        changeHandlers.add(handler2);

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, changeHandlers, curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.start();
        activeInstanceElectorService.isLeader();

        verify(handler1).instanceIsActive();
        verify(handler2).instanceIsActive();
    }

    @Test
    public void testSharedStateIsUpdatedWhenInstanceIsActive() throws Exception {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);

        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(), curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);

        activeInstanceElectorService.start();
        activeInstanceElectorService.isLeader();

        verify(activeInstanceState).update("id1");
    }

    @Test
    public void testRegisteredHandlersAreNotifiedOfPassiveWhenStateUpdateFails() throws Exception {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);


        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        Set<ActiveStateChangeHandler> changeHandlers = new HashSet<>();
        final ActiveStateChangeHandler handler1 = mock(ActiveStateChangeHandler.class);
        final ActiveStateChangeHandler handler2 = mock(ActiveStateChangeHandler.class);

        changeHandlers.add(handler1);
        changeHandlers.add(handler2);

        doThrow(new AtlasBaseException()).when(activeInstanceState).update("id1");

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, changeHandlers, curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.start();
        activeInstanceElectorService.isLeader();

        verify(handler1).instanceIsPassive();
        verify(handler2).instanceIsPassive();
    }

    @Test
    public void testElectionIsRejoinedWhenStateUpdateFails() throws Exception {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);


        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        doThrow(new AtlasBaseException()).when(activeInstanceState).update("id1");

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(), curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);

        activeInstanceElectorService.start();
        activeInstanceElectorService.isLeader();

        InOrder inOrder = inOrder(leaderLatch, curatorFactory);
        inOrder.verify(leaderLatch).close();
        inOrder.verify(curatorFactory).leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);
        inOrder.verify(leaderLatch).addListener(activeInstanceElectorService);
        inOrder.verify(leaderLatch).start();
    }

    @Test
    public void testRegisteredHandlersAreNotifiedOfPassiveWhenInstanceIsPassive() throws AtlasException {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);


        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        Set<ActiveStateChangeHandler> changeHandlers = new HashSet<>();
        final ActiveStateChangeHandler handler1 = mock(ActiveStateChangeHandler.class);
        final ActiveStateChangeHandler handler2 = mock(ActiveStateChangeHandler.class);

        changeHandlers.add(handler1);
        changeHandlers.add(handler2);

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, changeHandlers, curatorFactory,
                        activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.start();
        activeInstanceElectorService.notLeader();

        verify(handler1).instanceIsPassive();
        verify(handler2).instanceIsPassive();
    }

    @Test
    public void testActiveStateSetOnBecomingLeader() {
        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(),
                        curatorFactory, activeInstanceState, serviceState, metricsUtil);

        activeInstanceElectorService.isLeader();

        InOrder inOrder = inOrder(serviceState);
        inOrder.verify(serviceState).becomingActive();
        inOrder.verify(serviceState).setActive();
    }

    @Test
    public void testPassiveStateSetOnLoosingLeadership() {
        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(),
                        curatorFactory, activeInstanceState, serviceState, metricsUtil);

        activeInstanceElectorService.notLeader();

        InOrder inOrder = inOrder(serviceState);
        inOrder.verify(serviceState).becomingPassive();
        inOrder.verify(serviceState).setPassive();
    }

    @Test
    public void testPassiveStateSetIfActivationFails() throws Exception {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(
                HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);


        LeaderLatch leaderLatch = mock(LeaderLatch.class);
        when(curatorFactory.leaderLatchInstance("id1", HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).thenReturn(leaderLatch);

        doThrow(new AtlasBaseException()).when(activeInstanceState).update("id1");

        ActiveInstanceElectorService activeInstanceElectorService =
                new ActiveInstanceElectorService(configuration, new HashSet<ActiveStateChangeHandler>(),
                        curatorFactory, activeInstanceState, serviceState, metricsUtil);
        activeInstanceElectorService.start();
        activeInstanceElectorService.isLeader();

        InOrder inOrder = inOrder(serviceState);
        inOrder.verify(serviceState).becomingActive();
        inOrder.verify(serviceState).becomingPassive();
        inOrder.verify(serviceState).setPassive();
    }
}
