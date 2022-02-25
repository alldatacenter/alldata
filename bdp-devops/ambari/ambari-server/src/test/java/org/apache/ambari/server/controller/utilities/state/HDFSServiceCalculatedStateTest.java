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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.junit.Assert;
import org.junit.Test;

public class HDFSServiceCalculatedStateTest extends GeneralServiceCalculatedStateTest{
  @Override
  protected String getServiceName() {
    return "HDFS";
  }

  @Override
  protected ServiceCalculatedState getServiceCalculatedStateObject() {
    return new HDFSServiceCalculatedState();
  }

  @Override
  protected void createComponentsAndHosts() throws Exception {
    ServiceComponent masterComponent = service.addServiceComponent("NAMENODE");
    ServiceComponent masterComponent1 = service.addServiceComponent("SECONDARY_NAMENODE");
    ServiceComponent clientComponent = service.addServiceComponent("HDFS_CLIENT");

    for (String hostName: hosts){
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6.3");
      host.setHostAttributes(hostAttributes);
      host.setState(HostState.HEALTHY);
      clusters.mapHostToCluster(hostName, clusterName);

      ServiceComponentHost sch = masterComponent.addServiceComponentHost(hostName);
      sch.setVersion("2.1.1.0");
      sch.setState(State.STARTED);

      ServiceComponentHost sch1 = masterComponent1.addServiceComponentHost(hostName);
      sch1.setVersion("2.1.1.0");
      sch1.setState(State.STARTED);

      sch = clientComponent.addServiceComponentHost(hostName);
      sch.setVersion("2.1.1.0");
      sch.setState(State.INSTALLED);
    }
  }

  @Override
  public void testServiceState_STARTED() throws Exception {
    updateServiceState(State.STARTED);

    State state = serviceCalculatedState.getState(clusterName, getServiceName());
    Assert.assertEquals(State.STARTED, state);
  }

  @Override
  public void testServiceState_STOPPED() throws Exception {
   updateServiceState(State.INSTALLED);

    State state = serviceCalculatedState.getState(clusterName, getServiceName());
    Assert.assertEquals(State.INSTALLED, state);
  }

  //Should be in INSTALLED state when all NNs for at least one NS is INSTALLED
  @Test
  public void testServiceState_STOPPED_WITH_TWO_NS() throws Exception {
    simulateNNFederation();
    ServiceComponent nnComponent = service.getServiceComponent("NAMENODE");

    updateServiceState(State.STARTED);

    nnComponent.getServiceComponentHost("h3").setState(State.INSTALLED);
    nnComponent.getServiceComponentHost("h4").setState(State.INSTALLED);

    State state = serviceCalculatedState.getState(clusterName, getServiceName());
    Assert.assertEquals(State.INSTALLED, state);
  }

  //Should be in STARTED state when at least one NN for each NS is STARTED
  @Test
  public void testServiceState_STARTED_WITH_TWO_NS() throws Exception {
    simulateNNFederation();
    ServiceComponent nnComponent = service.getServiceComponent("NAMENODE");

    updateServiceState(State.STARTED);

    nnComponent.getServiceComponentHost("h1").setState(State.INSTALLED);
    nnComponent.getServiceComponentHost("h4").setState(State.INSTALLED);

    State state = serviceCalculatedState.getState(clusterName, getServiceName());
    Assert.assertEquals(State.STARTED, state);
  }

  private void simulateNNFederation() throws AmbariException {
    HashMap<String, String> hdfsSiteProperties = new HashMap<>();
    hdfsSiteProperties.put("dfs.internal.nameservices", "ns1,ns2");
    hdfsSiteProperties.put("dfs.ha.namenodes.ns1", "nn1,nn2");
    hdfsSiteProperties.put("dfs.ha.namenodes.ns2", "nn3,nn4");
    hdfsSiteProperties.put("dfs.namenode.http-address.ns1.nn1", "h1:1234");
    hdfsSiteProperties.put("dfs.namenode.http-address.ns1.nn2", "h2:1234");
    hdfsSiteProperties.put("dfs.namenode.http-address.ns2.nn3", "h3:1234");
    hdfsSiteProperties.put("dfs.namenode.http-address.ns2.nn4", "h4:1234");

    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
    Config config = configFactory.createNew(cluster, "hdfs-site", "version1",
        hdfsSiteProperties, new HashMap<>());
    cluster.addDesiredConfig("_test", Collections.singleton(config));

    ServiceComponent nnComponent = service.getServiceComponent("NAMENODE");
    ServiceComponent clientComponent = service.getServiceComponent("HDFS_CLIENT");
    ServiceComponent jnComponent = service.addServiceComponent("JOURNALNODE");

    List<String> newHosts = new ArrayList<>();
    newHosts.add("h3");
    newHosts.add("h4");

    for (String hostName: newHosts){
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6.3");
      host.setHostAttributes(hostAttributes);
      host.setState(HostState.HEALTHY);
      clusters.mapHostToCluster(hostName, clusterName);

      ServiceComponentHost sch = nnComponent.addServiceComponentHost(hostName);
      sch.setVersion("2.1.1.0");
      sch.setState(State.STARTED);

      sch = jnComponent.addServiceComponentHost(hostName);
      sch.setVersion("2.1.1.0");
      sch.setState(State.INSTALLED);

      sch = clientComponent.addServiceComponentHost(hostName);
      sch.setVersion("2.1.1.0");
      sch.setState(State.INSTALLED);
    }
  }
}
