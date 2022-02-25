/*
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

package org.apache.ambari.server.configuration;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DATANODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.agent.HeartbeatTestHelper;
import org.apache.ambari.server.agent.RecoveryConfig;
import org.apache.ambari.server.agent.RecoveryConfigComponent;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Test RecoveryConfigHelper class
 */
public class RecoveryConfigHelperTest {
  private Injector injector;

  private InMemoryDefaultTestModule module;

  @Inject
  private HeartbeatTestHelper heartbeatTestHelper;

  @Inject
  private RecoveryConfigHelper recoveryConfigHelper;

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private OrmTestHelper helper;

  private final String STACK_VERSION = "0.1";
  private final String REPO_VERSION = "0.1-1234";
  private final StackId stackId = new StackId("HDP", STACK_VERSION);
  private final String dummyClusterName = "cluster1";
  private final Long dummyClusterId = 1L;

  @Before
  public void setup() throws Exception {
    module = HeartbeatTestHelper.getTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    // Synchronize the publisher (AmbariEventPublisher) and subscriber (RecoveryConfigHelper),
    // so that the events get handled as soon as they are published, allowing the tests to
    // verify the methods under test.
    EventBus synchronizedBus = EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);
    synchronizedBus.register(recoveryConfigHelper);
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  /**
   * Install a component with auto start enabled. Verify that the old config was
   * invalidated.
   *
   * @throws Exception
   */
  @Test
  public void testServiceComponentInstalled() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(cluster);
    Service hdfs = cluster.addService(HDFS, repositoryVersion);

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    // Get the recovery configuration
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(Lists.newArrayList(
       new RecoveryConfigComponent(DATANODE, HDFS, State.INIT)
      ), recoveryConfig.getEnabledComponents());

    // Install HDFS::NAMENODE to trigger a component installed event
    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);

    // Verify the new config
    recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(Lists.newArrayList(
      new RecoveryConfigComponent(DATANODE, HDFS, State.INIT),
      new RecoveryConfigComponent(NAMENODE, HDFS, State.INIT)
      ), recoveryConfig.getEnabledComponents());
  }

  /**
   * Uninstall a component and verify that the config is stale.
   *
   * @throws Exception
   */
  @Test
  public void testServiceComponentUninstalled()
      throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(cluster);
    Service hdfs = cluster.addService(HDFS, repositoryVersion);

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);

    // Get the recovery configuration
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(Lists.newArrayList(
      new RecoveryConfigComponent(DATANODE, HDFS, State.INIT),
      new RecoveryConfigComponent(NAMENODE, HDFS, State.INIT)
    ), recoveryConfig.getEnabledComponents());

    // Uninstall HDFS::DATANODE from host1
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).delete(new DeleteHostComponentStatusMetaData());

    // Verify the new config
    recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(Lists.newArrayList(
      new RecoveryConfigComponent(NAMENODE, HDFS, State.INIT)
    ), recoveryConfig.getEnabledComponents());
  }

  /**
   * Change the maintenance mode of a service component host and verify that
   * config is stale.
   *
   * @throws Exception
   */
  @Test
  public void testMaintenanceModeChanged() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(cluster);
    Service hdfs = cluster.addService(HDFS, repositoryVersion);

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);

    // Get the recovery configuration
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(Lists.newArrayList(
      new RecoveryConfigComponent(DATANODE, HDFS, State.INIT),
      new RecoveryConfigComponent(NAMENODE, HDFS, State.INIT)
    ), recoveryConfig.getEnabledComponents());

    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setMaintenanceState(MaintenanceState.ON);

    // Only NAMENODE is left
    recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(Lists.newArrayList(
      new RecoveryConfigComponent(NAMENODE, HDFS, State.INIT)
    ), recoveryConfig.getEnabledComponents());
  }

  /**
   * Disable recovery on a component and verify that the config is stale.
   *
   * @throws Exception
   */
  @Test
  public void testServiceComponentRecoveryChanged() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(cluster);
    Service hdfs = cluster.addService(HDFS, repositoryVersion);

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    // Get the recovery configuration
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(Lists.newArrayList(
      new RecoveryConfigComponent(DATANODE, HDFS, State.INIT)
    ), recoveryConfig.getEnabledComponents());

    // Turn off auto start for HDFS::DATANODE
    hdfs.getServiceComponent(DATANODE).setRecoveryEnabled(false);

    // Get the latest config. DATANODE should not be present.
    recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(new ArrayList<RecoveryConfigComponent>(), recoveryConfig.getEnabledComponents());
  }

  /**
   * Test a cluster with two hosts. The first host gets the configuration during
   * registration. The second host gets it during it's first heartbeat.
   *
   * @throws Exception
   */
  @Test
  public void testMultiNodeCluster()
      throws Exception {
    Set<String> hostNames = new HashSet<String>() {{
      add("Host1");
      add("Host2");
    }};

    // Create a cluster with 2 hosts
    Cluster cluster = getDummyCluster(hostNames);

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(cluster);

    // Add HDFS service with DATANODE component to the cluster
    Service hdfs = cluster.addService(HDFS, repositoryVersion);

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);

    // Add SCH to Host1 and Host2
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost("Host1");
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost("Host2");

    // Simulate registration for Host1: Get the recovery configuration right away for Host1.
    // It makes an entry for cluster name and Host1 in the timestamp dictionary.
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), "Host1");
    assertEquals(Lists.newArrayList(
      new RecoveryConfigComponent(DATANODE, HDFS, State.INIT)
    ), recoveryConfig.getEnabledComponents());

    // Simulate heartbeat for Host2: When second host heartbeats, it first checks if config stale.
    // This should return true since it did not get the configuration during registration.
    // There is an entry for the cluster name, made by Host1, but no entry for Host2 in the timestamp
    // dictionary since we skipped registration. Lookup for cluster name will succeed but lookup for Host2
    // will return null.
    boolean isConfigStale = recoveryConfigHelper.isConfigStale(cluster.getClusterName(), "Host2", -1);
    assertTrue(isConfigStale);
  }

  private Cluster getDummyCluster(Set<String> hostNames)
      throws Exception {

    Map<String, String> configProperties = new HashMap<String, String>() {{
      put(RecoveryConfigHelper.RECOVERY_ENABLED_KEY, "true");
      put(RecoveryConfigHelper.RECOVERY_TYPE_KEY, "AUTO_START");
      put(RecoveryConfigHelper.RECOVERY_MAX_COUNT_KEY, "4");
      put(RecoveryConfigHelper.RECOVERY_LIFETIME_MAX_COUNT_KEY, "10");
      put(RecoveryConfigHelper.RECOVERY_WINDOW_IN_MIN_KEY, "23");
      put(RecoveryConfigHelper.RECOVERY_RETRY_GAP_KEY, "2");
    }};

    Cluster cluster = heartbeatTestHelper.getDummyCluster(dummyClusterName, dummyClusterId, stackId, REPO_VERSION,
        configProperties, hostNames);

    return cluster;
  }
}
