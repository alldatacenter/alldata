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

package org.apache.atlas.web.setup;

import com.google.common.base.Charsets;
import org.apache.atlas.AtlasConstants;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.atlas.setup.SetupException;
import org.apache.atlas.setup.SetupStep;
import org.apache.atlas.web.service.CuratorFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertTrue;

public class SetupStepsTest {

    @Mock
    private CuratorFactory curatorFactory;

    @Mock
    private Configuration configuration;

    @Mock
    private CuratorFramework client;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        System.setProperty(AtlasConstants.SYSTEM_PROPERTY_APP_PORT, AtlasConstants.DEFAULT_APP_PORT_STR);
    }

    @AfterMethod
    public void tearDown() {
        System.getProperties().remove(AtlasConstants.SYSTEM_PROPERTY_APP_PORT);
    }

    @Test
    public void shouldRunRegisteredSetupSteps() throws Exception {
        Set<SetupStep> steps = new LinkedHashSet<>();
        SetupStep setupStep1 = mock(SetupStep.class);
        SetupStep setupStep2 = mock(SetupStep.class);
        steps.add(setupStep1);
        steps.add(setupStep2);

        when(configuration.
                getString(HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);
        setupServerIdSelectionMocks();
        setupSetupInProgressPathMocks(ZooDefs.Ids.OPEN_ACL_UNSAFE);

        InterProcessMutex lock = mock(InterProcessMutex.class);
        when(curatorFactory.lockInstance(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(lock);
        SetupSteps setupSteps = new SetupSteps(steps, curatorFactory, configuration);
        setupSteps.runSetup();

        verify(setupStep1).run();
        verify(setupStep2).run();
    }

    private Pair<CreateBuilder, DeleteBuilder> setupSetupInProgressPathMocks(List<ACL> acls) throws Exception {
        return setupSetupInProgressPathMocks(acls, null);
    }

    private Pair<CreateBuilder, DeleteBuilder> setupSetupInProgressPathMocks(List<ACL> acls, Stat stat) throws Exception {
        when(curatorFactory.clientInstance()).thenReturn(client);
        CreateBuilder createBuilder = mock(CreateBuilder.class);
        when(createBuilder.withACL(acls)).thenReturn(createBuilder);
        when(client.create()).thenReturn(createBuilder);
        DeleteBuilder deleteBuilder = mock(DeleteBuilder.class);
        when(client.delete()).thenReturn(deleteBuilder);
        Pair<CreateBuilder, DeleteBuilder> pair = Pair.of(createBuilder, deleteBuilder);
        ExistsBuilder existsBuilder = mock(ExistsBuilder.class);
        when(client.checkExists()).thenReturn(existsBuilder);
        when(existsBuilder.forPath(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT+SetupSteps.SETUP_IN_PROGRESS_NODE)).
                thenReturn(stat);
        return pair;
    }


    private void setupServerIdSelectionMocks() {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(false);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] {"id1", "id2"});
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:31000");
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id2")).thenReturn("127.0.0.1:21000");
    }

    @Test
    public void shouldRunSetupStepsUnderLock() throws Exception {
        Set<SetupStep> steps = new LinkedHashSet<>();
        SetupStep setupStep1 = mock(SetupStep.class);
        SetupStep setupStep2 = mock(SetupStep.class);
        steps.add(setupStep1);
        steps.add(setupStep2);

        when(configuration.
                getString(HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);
        setupServerIdSelectionMocks();
        setupSetupInProgressPathMocks(ZooDefs.Ids.OPEN_ACL_UNSAFE);

        InterProcessMutex lock = mock(InterProcessMutex.class);
        when(curatorFactory.lockInstance(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(lock);
        InOrder inOrder = inOrder(lock, setupStep1, setupStep2);

        SetupSteps setupSteps = new SetupSteps(steps, curatorFactory, configuration);
        setupSteps.runSetup();

        inOrder.verify(lock).acquire();
        inOrder.verify(setupStep1).run();
        inOrder.verify(setupStep2).run();
        inOrder.verify(lock).release();
    }

    @Test
    public void shouldReleaseLockOnException() throws Exception {
        Set<SetupStep> steps = new LinkedHashSet<>();
        SetupStep setupStep1 = mock(SetupStep.class);
        steps.add(setupStep1);

        when(configuration.
                getString(HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);
        setupServerIdSelectionMocks();
        setupSetupInProgressPathMocks(ZooDefs.Ids.OPEN_ACL_UNSAFE);

        doThrow(new RuntimeException("Simulating setup failure.")).when(setupStep1).run();

        InterProcessMutex lock = mock(InterProcessMutex.class);
        when(curatorFactory.lockInstance(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(lock);
        InOrder inOrder = inOrder(lock, setupStep1);

        SetupSteps setupSteps = new SetupSteps(steps, curatorFactory, configuration);
        try {
            setupSteps.runSetup();
        } catch (Exception e) {
            assertTrue(e instanceof SetupException);
        }

        inOrder.verify(lock).acquire();
        inOrder.verify(setupStep1).run();
        inOrder.verify(lock).release();
    }

    @Test
    public void shouldCreateSetupInProgressNode() throws Exception {
        Set<SetupStep> steps = new LinkedHashSet<>();
        SetupStep setupStep1 = mock(SetupStep.class);
        steps.add(setupStep1);

        when(configuration.
                getString(HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);
        when(configuration.getString(HAConfiguration.HA_ZOOKEEPER_ACL)).thenReturn("digest:user:pwd");

        List<ACL> aclList = Arrays.asList(new ACL(ZooDefs.Perms.ALL, new Id("digest", "user:pwd")));
        setupServerIdSelectionMocks();
        CreateBuilder createBuilder = setupSetupInProgressPathMocks(aclList).getLeft();

        InterProcessMutex lock = mock(InterProcessMutex.class);
        when(curatorFactory.lockInstance(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(lock);
        SetupSteps setupSteps = new SetupSteps(steps, curatorFactory, configuration);
        setupSteps.runSetup();

        verify(createBuilder).withACL(aclList);
        verify(createBuilder).forPath(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT+SetupSteps.SETUP_IN_PROGRESS_NODE,
                "id2".getBytes(Charsets.UTF_8));
    }

    @Test
    public void shouldDeleteSetupInProgressNodeAfterCompletion() throws Exception {
        Set<SetupStep> steps = new LinkedHashSet<>();
        SetupStep setupStep1 = mock(SetupStep.class);
        steps.add(setupStep1);

        when(configuration.
                getString(HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);
        when(configuration.getString(HAConfiguration.HA_ZOOKEEPER_ACL)).thenReturn("digest:user:pwd");

        List<ACL> aclList = Arrays.asList(new ACL(ZooDefs.Perms.ALL, new Id("digest", "user:pwd")));
        setupServerIdSelectionMocks();
        DeleteBuilder deleteBuilder = setupSetupInProgressPathMocks(aclList).getRight();

        InterProcessMutex lock = mock(InterProcessMutex.class);
        when(curatorFactory.lockInstance(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(lock);
        SetupSteps setupSteps = new SetupSteps(steps, curatorFactory, configuration);
        setupSteps.runSetup();

        verify(deleteBuilder).forPath(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT+SetupSteps.SETUP_IN_PROGRESS_NODE);
    }

    @Test
    public void shouldThrowSetupExceptionAndNotDoSetupIfSetupInProgressNodeExists() throws Exception {
        Set<SetupStep> steps = new LinkedHashSet<>();
        SetupStep setupStep1 = mock(SetupStep.class);
        steps.add(setupStep1);

        when(configuration.
                getString(HAConfiguration.ATLAS_SERVER_HA_ZK_ROOT_KEY, HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT);
        setupServerIdSelectionMocks();
        setupSetupInProgressPathMocks(ZooDefs.Ids.OPEN_ACL_UNSAFE, mock(Stat.class));

        InterProcessMutex lock = mock(InterProcessMutex.class);
        when(curatorFactory.lockInstance(HAConfiguration.ATLAS_SERVER_ZK_ROOT_DEFAULT)).
                thenReturn(lock);
        SetupSteps setupSteps = new SetupSteps(steps, curatorFactory, configuration);

        try {
            setupSteps.runSetup();
        } catch (Exception e) {
            assertTrue(e instanceof SetupException);
        }

        verifyZeroInteractions(setupStep1);
    }
}
