/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.testing;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.State;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class DBInconsistencyTests {

  private static final Logger LOG = LoggerFactory.getLogger(DBInconsistencyTests.class);

  @Inject
  private Injector injector;
  @Inject
  private OrmTestHelper helper;
  @Inject
  private Clusters clusters;
  @Inject
  private ServiceFactory serviceFactory;
  @Inject
  private ServiceComponentFactory serviceComponentFactory;
  @Inject
  private MpackManagerFactory mpackManagerFactory;
  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;
  @Inject
  private HostComponentDesiredStateDAO hostComponentDesiredStateDAO;
  @Inject
  private ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;
  @Inject
  private ClusterDAO clusterDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testOrphanedSCHDesiredEntityReAdd() throws Exception {
    Long clusterId = helper.createCluster();
    Assert.assertNotNull(clusterId);

    Cluster cluster = clusters.getCluster(OrmTestHelper.CLUSTER_NAME);
    Assert.assertNotNull(cluster);

    helper.addHost(clusters, cluster, "h1");

    helper.initializeClusterWithStack(cluster);

    helper.installHdfsService(cluster, serviceFactory,
      serviceComponentFactory, serviceComponentHostFactory, "h1");

    Collection<ServiceComponentHost> schList = clusters.getCluster(
      OrmTestHelper.CLUSTER_NAME).getServiceComponentHosts("HDFS", "DATANODE");
    Assert.assertNotNull(schList);

    Collection<ServiceComponent> scList = cluster.getService("HDFS").getServiceComponents().values();
    Assert.assertNotNull(schList);

    cluster.deleteService("HDFS", new DeleteHostComponentStatusMetaData());

    List<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities =
      hostComponentDesiredStateDAO.findAll();
    Assert.assertTrue(hostComponentDesiredStateEntities == null ||
      hostComponentDesiredStateEntities.isEmpty());

    List<ServiceComponentDesiredStateEntity> serviceComponentDesiredStateEntities =
      serviceComponentDesiredStateDAO.findAll();
    Assert.assertTrue(serviceComponentDesiredStateEntities == null ||
      serviceComponentDesiredStateEntities.isEmpty());

    EntityManager em = helper.getEntityManager();
    final EntityTransaction txn = em.getTransaction();

    txn.begin();

    for (ServiceComponentHost sch : schList) {
      sch.setDesiredState(State.DISABLED);
    }

    for (ServiceComponent sc : scList) {
      sc.setDesiredState(State.DISABLED);
    }

    txn.commit();

    hostComponentDesiredStateEntities = hostComponentDesiredStateDAO.findAll();
    Assert.assertTrue(hostComponentDesiredStateEntities == null || hostComponentDesiredStateEntities.isEmpty());

    serviceComponentDesiredStateEntities = serviceComponentDesiredStateDAO.findAll();
    Assert.assertTrue(serviceComponentDesiredStateEntities == null ||
      serviceComponentDesiredStateEntities.isEmpty());
  }

  @Ignore // This non-functional in terms of actual code path
  @Test
  public void testRefreshInSameTxn() throws Exception {
    Long clusterId = helper.createCluster();
    Assert.assertNotNull(clusterId);

    Cluster cluster = clusters.getCluster(OrmTestHelper.CLUSTER_NAME);
    Assert.assertNotNull(cluster);

    EntityManager em = helper.getEntityManager();
    final EntityTransaction txn = em.getTransaction();

    txn.begin();

    ClusterEntity entity = clusterDAO.findById(clusterId);
    entity.setProvisioningState(State.DISABLED);
    clusterDAO.merge(entity);

    Assert.assertEquals(State.DISABLED, entity.getProvisioningState());

    entity = clusterDAO.findById(clusterId);

    Assert.assertEquals(State.DISABLED, entity.getProvisioningState());

    entity.setProvisioningState(State.INIT);

    txn.commit();

    entity = clusterDAO.findById(clusterId);

    Assert.assertEquals(State.INIT, entity.getProvisioningState());
  }
}
