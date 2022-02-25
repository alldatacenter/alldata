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

package org.apache.ambari.server.controller.utilities.state;

import java.sql.SQLException;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public abstract class GeneralServiceCalculatedStateTest {

  final protected String[] hosts = {"h1", "h2"};
  final protected String clusterName = "c1";

  protected ServiceCalculatedState serviceCalculatedState;
  protected Injector injector;
  protected Cluster cluster;
  protected Service service;

  @Inject
  protected Clusters clusters;

  @Inject
  private OrmTestHelper ormTestHelper;

  @Before
  public void setup() throws Exception {
    final StackId stack211 = new StackId("HDP-2.1.1");
    final String version = "2.1.1-1234";

    injector = Guice.createInjector(Modules.override(
      new InMemoryDefaultTestModule()).with(new Module() {
      @Override
      public void configure(Binder binder) {

      }
    }));

    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    RepositoryVersionEntity repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(stack211,
        version);

    clusters.addCluster(clusterName, stack211);
    cluster = clusters.getCluster(clusterName);

    service = cluster.addService(getServiceName(), repositoryVersion);

    createComponentsAndHosts();

    TopologyManager topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);
    ActionManager.setTopologyManager(topologyManager);

    serviceCalculatedState = getServiceCalculatedStateObject();
  }

  /**
   * @return service name which would be added to the test cluster
   */
  protected abstract String getServiceName();

  /**
   * @return subject module to be tested
   */
  protected abstract ServiceCalculatedState getServiceCalculatedStateObject();

  /**
   * Create component structure
   * @throws Exception
   */
  protected abstract void createComponentsAndHosts() throws Exception;



  @After
  public void after() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;
  }

  protected void updateServiceState(State newState) throws Exception{
    Service service = cluster.getService(getServiceName());
    Map<String, ServiceComponent> serviceComponentMap = service.getServiceComponents();

    // Set all components to newState state
    for (ServiceComponent serviceComponent: serviceComponentMap.values()){
      Map<String, ServiceComponentHost> serviceComponentHostMap = serviceComponent.getServiceComponentHosts();
      for (ServiceComponentHost serviceComponentHost: serviceComponentHostMap.values()){
        if (serviceComponentHost.getServiceComponentName().contains("_CLIENT")) {
          serviceComponentHost.setState(State.INSTALLED);
        } else {  // assume that component without "CLIENT" postfix are services
          switch (newState){
            case STARTED:
              serviceComponentHost.setState(State.STARTED);
              break;
            default:
              serviceComponentHost.setState(State.INSTALLED);
              break;
          }
        }
      }
    }
  }

  @Test
  public abstract void testServiceState_STARTED() throws Exception;

  @Test
  public abstract void testServiceState_STOPPED() throws Exception;

}
