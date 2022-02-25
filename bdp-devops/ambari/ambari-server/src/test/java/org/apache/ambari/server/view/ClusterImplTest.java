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

package org.apache.ambari.server.view;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.junit.Assert;
import org.junit.Test;

/**
 * ClusterImpl tests.
 */
public class ClusterImplTest {

  @Test
  public void testGetName() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);

    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    replay(cluster);

    ClusterImpl clusterImpl = new ClusterImpl(cluster);

    Assert.assertEquals("c1", clusterImpl.getName());

    verify(cluster);
  }

  @Test
  public void testGetConfigurationValue() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    Config config = createNiceMock(Config.class);

    Map<String, String> properties = new HashMap<>();

    properties.put("foo", "bar");

    expect(cluster.getDesiredConfigByType("core-site")).andReturn(config).anyTimes();
    expect(config.getProperties()).andReturn(properties).anyTimes();

    replay(cluster, config);

    ClusterImpl clusterImpl = new ClusterImpl(cluster);

    Assert.assertEquals("bar", clusterImpl.getConfigurationValue("core-site", "foo"));

    verify(cluster, config);
  }

  @Test
  public void testGetHostsForServiceComponent() {
    Cluster cluster = createNiceMock(Cluster.class);

    String service = "SERVICE";
    String component = "COMPONENT";

    List<ServiceComponentHost> components = new ArrayList<>();

    ServiceComponentHost component1 = createNiceMock(ServiceComponentHost.class);
    expect(component1.getHostName()).andReturn("host1");
    components.add(component1);

    ServiceComponentHost component2 = createNiceMock(ServiceComponentHost.class);
    expect(component2.getHostName()).andReturn("host2");
    components.add(component2);

    expect(cluster.getServiceComponentHosts(service,component)).andReturn(components);

    replay(cluster, component1, component2);

    ClusterImpl clusterImpl = new ClusterImpl(cluster);

    List<String> hosts = clusterImpl.getHostsForServiceComponent(service,component);
    Assert.assertEquals(2, hosts.size());
    Assert.assertEquals("host1",hosts.get(0));
    Assert.assertEquals("host2",hosts.get(1));

    verify(cluster, component1, component2);
  }
}
