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
package org.apache.ambari.server.checks;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO.LastServiceCheckDTO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;

public class ServiceCheckValidityCheckTest {
  private static final String CLUSTER_NAME = "cluster1";
  private static final long CLUSTER_ID = 1L;
  private static final String SERVICE_NAME = "HDFS";
  private static final long CONFIG_CREATE_TIMESTAMP = 1461518722202L;
  private static final long SERVICE_CHECK_START_TIME = CONFIG_CREATE_TIMESTAMP - 2000L;
  private static final String SERVICE_COMPONENT_NAME = "service component";
  private ServiceCheckValidityCheck serviceCheckValidityCheck;

  private ServiceConfigDAO serviceConfigDAO;
  private HostRoleCommandDAO hostRoleCommandDAO;
  private Service service;
  private AmbariMetaInfo ambariMetaInfo;
  private ActionMetadata actionMetadata;

  @Before
  public void setUp() throws Exception {
    final Clusters clusters = mock(Clusters.class);
    service = mock(Service.class);
    serviceConfigDAO = mock(ServiceConfigDAO.class);
    hostRoleCommandDAO = mock(HostRoleCommandDAO.class);
    ambariMetaInfo = mock(AmbariMetaInfo.class);
    actionMetadata = new ActionMetadata();

    serviceCheckValidityCheck = new ServiceCheckValidityCheck();
    serviceCheckValidityCheck.hostRoleCommandDAOProvider = new Provider<HostRoleCommandDAO>() {
      @Override
      public HostRoleCommandDAO get() {
        return hostRoleCommandDAO;
      }
    };
    serviceCheckValidityCheck.serviceConfigDAOProvider = new Provider<ServiceConfigDAO>() {
      @Override
      public ServiceConfigDAO get() {
        return serviceConfigDAO;
      }
    };
    serviceCheckValidityCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };
    serviceCheckValidityCheck.actionMetadataProvider = new Provider<ActionMetadata>() {
      @Override
      public ActionMetadata get() {
        return actionMetadata;
      }
    };

    Cluster cluster = mock(Cluster.class);
    when(clusters.getCluster(CLUSTER_NAME)).thenReturn(cluster);
    when(cluster.getClusterId()).thenReturn(CLUSTER_ID);
    when(cluster.getServices()).thenReturn(ImmutableMap.of(SERVICE_NAME, service));
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP", "2.2"));
    when(service.getName()).thenReturn(SERVICE_NAME);
    when(service.getDesiredStackId()).thenReturn(new StackId("HDP", "2.2"));


    serviceCheckValidityCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return ambariMetaInfo;
      }
    };

    when(ambariMetaInfo.isServiceWithNoConfigs(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString())).thenReturn(false);

    actionMetadata.addServiceCheckAction("HDFS");
  }

  @Test
  public void testWithNullCommandDetailAtCommand() throws AmbariException {
    ServiceComponent serviceComponent = mock(ServiceComponent.class);
    when(serviceComponent.isVersionAdvertised()).thenReturn(true);

    when(service.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    when(service.getServiceComponents()).thenReturn(ImmutableMap.of(SERVICE_COMPONENT_NAME, serviceComponent));

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(SERVICE_NAME);
    serviceConfigEntity.setCreateTimestamp(CONFIG_CREATE_TIMESTAMP);

    LastServiceCheckDTO lastServiceCheckDTO1 = new LastServiceCheckDTO(Role.ZOOKEEPER_QUORUM_SERVICE_CHECK.name(), SERVICE_CHECK_START_TIME);
    LastServiceCheckDTO lastServiceCheckDTO2 = new LastServiceCheckDTO(Role.HDFS_SERVICE_CHECK.name(), SERVICE_CHECK_START_TIME);

    when(serviceConfigDAO.getLastServiceConfig(eq(CLUSTER_ID), eq(SERVICE_NAME))).thenReturn(serviceConfigEntity);
    when(hostRoleCommandDAO.getLatestServiceChecksByRole(any(Long.class))).thenReturn(asList(lastServiceCheckDTO1, lastServiceCheckDTO2));

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, null, null, null);

    try {
      UpgradeCheckResult result = serviceCheckValidityCheck.perform(request);
      Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());
    } catch (NullPointerException ex){
      Assert.fail("serviceCheckValidityCheck failed due to null at start_time were not handled");
    }
  }

  @Test
  public void testFailWhenServiceWithOutdatedServiceCheckExists() throws AmbariException {
    ServiceComponent serviceComponent = mock(ServiceComponent.class);
    when(serviceComponent.isVersionAdvertised()).thenReturn(true);

    when(service.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    when(service.getServiceComponents()).thenReturn(ImmutableMap.of(SERVICE_COMPONENT_NAME, serviceComponent));

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(SERVICE_NAME);
    serviceConfigEntity.setCreateTimestamp(CONFIG_CREATE_TIMESTAMP);

    LastServiceCheckDTO lastServiceCheckDTO = new LastServiceCheckDTO(Role.HDFS_SERVICE_CHECK.name(), SERVICE_CHECK_START_TIME);

    when(serviceConfigDAO.getLastServiceConfig(eq(CLUSTER_ID), eq(SERVICE_NAME))).thenReturn(serviceConfigEntity);
    when(hostRoleCommandDAO.getLatestServiceChecksByRole(any(Long.class))).thenReturn(singletonList(lastServiceCheckDTO));

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, null, null, null);

    UpgradeCheckResult result =  serviceCheckValidityCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());
  }


  @Test
  public void testFailWhenServiceWithNoServiceCheckExists() throws AmbariException {
    ServiceComponent serviceComponent = mock(ServiceComponent.class);
    when(serviceComponent.isVersionAdvertised()).thenReturn(true);

    when(service.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    when(service.getServiceComponents()).thenReturn(ImmutableMap.of(SERVICE_COMPONENT_NAME, serviceComponent));

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(SERVICE_NAME);
    serviceConfigEntity.setCreateTimestamp(CONFIG_CREATE_TIMESTAMP);

    when(serviceConfigDAO.getLastServiceConfig(eq(CLUSTER_ID), eq(SERVICE_NAME))).thenReturn(serviceConfigEntity);
    when(hostRoleCommandDAO.getLatestServiceChecksByRole(any(Long.class))).thenReturn(Collections.<LastServiceCheckDTO>emptyList());

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, null, null, null);

    UpgradeCheckResult result =  serviceCheckValidityCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());
  }

  @Test
  public void testFailWhenServiceWithOutdatedServiceCheckExistsRepeated() throws AmbariException {
    ServiceComponent serviceComponent = mock(ServiceComponent.class);
    when(serviceComponent.isVersionAdvertised()).thenReturn(true);

    when(service.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    when(service.getServiceComponents()).thenReturn(ImmutableMap.of(SERVICE_COMPONENT_NAME, serviceComponent));

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(SERVICE_NAME);
    serviceConfigEntity.setCreateTimestamp(CONFIG_CREATE_TIMESTAMP);

    LastServiceCheckDTO lastServiceCheckDTO1 = new LastServiceCheckDTO(Role.HDFS_SERVICE_CHECK.name(), SERVICE_CHECK_START_TIME);
    LastServiceCheckDTO lastServiceCheckDTO2 = new LastServiceCheckDTO(Role.HDFS_SERVICE_CHECK.name(), CONFIG_CREATE_TIMESTAMP - 1L);

    when(serviceConfigDAO.getLastServiceConfig(eq(CLUSTER_ID), eq(SERVICE_NAME))).thenReturn(serviceConfigEntity);
    when(hostRoleCommandDAO.getLatestServiceChecksByRole(any(Long.class))).thenReturn(asList(lastServiceCheckDTO1, lastServiceCheckDTO2));

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, null, null, null);

    UpgradeCheckResult result =  serviceCheckValidityCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());
  }

  /**
   * Tests that old, oudated service checks for the FOO2 service doesn't cause
   * problems when checking values for the FOO service.
   * <p/>
   * The specific test case here is that the FOO2 service was added a long time
   * ago and then removed. We don't want old service checks for FOO2 to match
   * when querying for FOO.
   *
   * @throws AmbariException
   */
  @Test
  public void testPassWhenSimilarlyNamedServiceIsOutdated() throws AmbariException {
    ServiceComponent serviceComponent = mock(ServiceComponent.class);
    when(serviceComponent.isVersionAdvertised()).thenReturn(true);

    when(service.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    when(service.getServiceComponents()).thenReturn(ImmutableMap.of(SERVICE_COMPONENT_NAME, serviceComponent));

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(SERVICE_NAME);
    serviceConfigEntity.setCreateTimestamp(CONFIG_CREATE_TIMESTAMP);

    String hdfsRole = Role.HDFS_SERVICE_CHECK.name();
    String hdfs2Role = hdfsRole.replace("HDFS", "HDFS2");

    LastServiceCheckDTO lastServiceCheckDTO1 = new LastServiceCheckDTO(hdfsRole, SERVICE_CHECK_START_TIME);
    LastServiceCheckDTO lastServiceCheckDTO2 = new LastServiceCheckDTO(hdfs2Role, CONFIG_CREATE_TIMESTAMP - 1L);

    when(serviceConfigDAO.getLastServiceConfig(eq(CLUSTER_ID), eq(SERVICE_NAME))).thenReturn(serviceConfigEntity);
    when(hostRoleCommandDAO.getLatestServiceChecksByRole(any(Long.class))).thenReturn(asList(lastServiceCheckDTO1, lastServiceCheckDTO2));

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, null, null, null);

    UpgradeCheckResult result =  serviceCheckValidityCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());  }
}
