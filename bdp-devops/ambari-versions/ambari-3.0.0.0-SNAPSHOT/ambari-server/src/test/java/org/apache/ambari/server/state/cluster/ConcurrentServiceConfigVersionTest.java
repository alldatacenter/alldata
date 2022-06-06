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

package org.apache.ambari.server.state.cluster;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.events.listeners.upgrade.HostVersionOutOfSyncListener;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Tests that concurrent threads attempting to create configurations don't cause
 * unique violations with the configuration version.
 */
public class ConcurrentServiceConfigVersionTest {
  private static final int NUMBER_OF_SERVICE_CONFIG_VERSIONS = 10;
  private static final int NUMBER_OF_THREADS = 2;

  @Inject
  private Injector injector;

  @Inject
  private Clusters clusters;

  @Inject
  private ServiceFactory serviceFactory;

  @Inject
  private ServiceComponentFactory serviceComponentFactory;

  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;

  @Inject
  private OrmTestHelper helper;

  @Inject
  private ServiceConfigDAO serviceConfigDAO;

  private StackId stackId = new StackId("HDP-0.1");

  /**
   * The cluster.
   */
  private Cluster cluster;

  private RepositoryVersionEntity repositoryVersion;

  /**
   * Creates a cluster and installs HDFS with NN and DN.
   *
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    helper.createStack(stackId);
    clusters.addCluster("c1", stackId);
    cluster = clusters.getCluster("c1");
    repositoryVersion = helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    String hostName = "c6401.ambari.apache.org";
    clusters.addHost(hostName);
    setOsFamily(clusters.getHost(hostName), "redhat", "6.4");
    clusters.mapHostToCluster(hostName, "c1");

    Service service = installService("HDFS");
    addServiceComponent(service, "NAMENODE");
    addServiceComponent(service, "DATANODE");

    createNewServiceComponentHost("HDFS", "NAMENODE", hostName);
    createNewServiceComponentHost("HDFS", "DATANODE", hostName);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  /**
   * Tests that creating service config versions from multiple threads doesn't
   * violate unique constraints.
   *
   * @throws Exception
   */
  @Test
  public void testConcurrentServiceConfigVersions() throws Exception {
    long nextVersion = serviceConfigDAO.findNextServiceConfigVersion(
        cluster.getClusterId(), "HDFS");

    Assert.assertEquals(nextVersion, 1);

    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      Thread thread = new ConcurrentServiceConfigThread(cluster);
      threads.add(thread);

      thread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    long maxVersion = NUMBER_OF_THREADS * NUMBER_OF_SERVICE_CONFIG_VERSIONS;
    nextVersion = serviceConfigDAO.findNextServiceConfigVersion(
        cluster.getClusterId(), "HDFS");

    Assert.assertEquals(maxVersion + 1, nextVersion);
  }

  private final static class ConcurrentServiceConfigThread extends Thread {

    private Cluster cluster = null;

    private ConcurrentServiceConfigThread(Cluster cluster) {
      this.cluster = cluster;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      try {
        for (int i = 0; i < NUMBER_OF_SERVICE_CONFIG_VERSIONS; i++) {
          ServiceConfigVersionResponse response = cluster.createServiceConfigVersion(
              "HDFS", null, getName() + "-serviceConfig" + i, null);

          Thread.sleep(100);
        }
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<>(2);
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);
    host.setHostAttributes(hostAttributes);
  }

  private ServiceComponentHost createNewServiceComponentHost(String svc,
      String svcComponent, String hostName) throws AmbariException {
    Assert.assertNotNull(cluster.getConfigGroups());
    Service s = installService(svc);
    ServiceComponent sc = addServiceComponent(s, svcComponent);

    ServiceComponentHost sch = serviceComponentHostFactory.createNew(sc,
        hostName);

    sc.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);

    return sch;
  }

  private Service installService(String serviceName) throws AmbariException {
    Service service = null;

    try {
      service = cluster.getService(serviceName);
    } catch (ServiceNotFoundException e) {
      service = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
      cluster.addService(service);
    }

    return service;
  }

  private ServiceComponent addServiceComponent(Service service,
      String componentName) throws AmbariException {
    ServiceComponent serviceComponent = null;
    try {
      serviceComponent = service.getServiceComponent(componentName);
    } catch (ServiceComponentNotFoundException e) {
      serviceComponent = serviceComponentFactory.createNew(service,
          componentName);
      service.addServiceComponent(serviceComponent);
      serviceComponent.setDesiredState(State.INSTALLED);
    }

    return serviceComponent;
  }

  /**
  *
  */
  private class MockModule implements Module {
    /**
    *
    */
    @Override
    public void configure(Binder binder) {
      // this listener gets in the way of actually testing the concurrency
      // between the threads; it slows them down too much, so mock it out
      binder.bind(HostVersionOutOfSyncListener.class).toInstance(
          EasyMock.createNiceMock(HostVersionOutOfSyncListener.class));
    }
  }
}
