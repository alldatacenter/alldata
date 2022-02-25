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
package org.apache.ambari.server.agent.stomp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.apache.ambari.server.NullHostNameException;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;
import org.apache.ambari.server.agent.stomp.dto.TopologyComponent;
import org.apache.ambari.server.agent.stomp.dto.TopologyHost;
import org.apache.ambari.server.agent.stomp.dto.TopologyUpdateHandlingReport;
import org.apache.ambari.server.events.UpdateEventType;
import org.junit.Assert;
import org.junit.Test;

public class TopologyClusterTest {

  @Test
  public void testHandlingReportHostAdd() throws NullHostNameException {
    TopologyHost dummyHost = new TopologyHost(1L, "hostName1");
    TopologyHost hostToAddition = new TopologyHost(2L, "hostName2");

    TopologyCluster topologyCluster = new TopologyCluster(new HashSet<>(), new HashSet(){{add(dummyHost);}});

    TopologyUpdateHandlingReport report = new TopologyUpdateHandlingReport();

    topologyCluster.update(Collections.emptySet(), Collections.singleton(hostToAddition), UpdateEventType.UPDATE, report);

    Assert.assertEquals(1L, report.getUpdatedHostNames().size());
    Assert.assertEquals("hostName2", report.getUpdatedHostNames().iterator().next());

    Assert.assertEquals(2L, topologyCluster.getTopologyHosts().size());
  }

  @Test
  public void testHandlingReportHostDelete() throws NullHostNameException {
    TopologyHost dummyHost = new TopologyHost(1L, "hostName1");
    TopologyHost hostToDelete = new TopologyHost(2L, "hostName2");
    TopologyHost update = new TopologyHost(2L, "hostName2");

    TopologyCluster topologyCluster = new TopologyCluster(new HashSet<>(), new HashSet(){{add(dummyHost); add(hostToDelete);}});

    TopologyUpdateHandlingReport report = new TopologyUpdateHandlingReport();

    topologyCluster.update(Collections.emptySet(), Collections.singleton(update), UpdateEventType.DELETE, report);

    Assert.assertEquals(1L, report.getUpdatedHostNames().size());
    Assert.assertEquals("hostName2", report.getUpdatedHostNames().iterator().next());

    Assert.assertEquals(1L, topologyCluster.getTopologyHosts().size());
    Assert.assertEquals("hostName1", topologyCluster.getTopologyHosts().iterator().next().getHostName());
  }

  @Test
  public void testHandlingReportHostUpdate() throws NullHostNameException {
    TopologyHost dummyHost = new TopologyHost(1L, "hostName1");
    TopologyHost hostToUpdate = new TopologyHost(2L, "hostName2");
    TopologyHost update = new TopologyHost(2L, "hostName2", "rack","ipv4");

    TopologyCluster topologyCluster = new TopologyCluster(new HashSet<>(), new HashSet(){{add(dummyHost); add(hostToUpdate);}});

    TopologyUpdateHandlingReport report = new TopologyUpdateHandlingReport();

    topologyCluster.update(Collections.emptySet(), Collections.singleton(update), UpdateEventType.UPDATE, report);

    Assert.assertEquals(1L, report.getUpdatedHostNames().size());
    Assert.assertEquals("hostName2", report.getUpdatedHostNames().iterator().next());

    Assert.assertEquals(2L, topologyCluster.getTopologyHosts().size());
  }

  @Test
  public void testHandlingReportComponentAdd() throws NullHostNameException {
    TopologyComponent dummyComponent = createDummyTopologyComponent("comp1",
        new Long[]{1L, 2L}, new String[]{"hostName1", "hostName2"});
    TopologyComponent componentToAddition = createDummyTopologyComponent("comp2",
        new Long[]{1L, 3L}, new String[]{"hostName1", "hostName3"});

    TopologyCluster topologyCluster = new TopologyCluster(new HashSet(){{add(dummyComponent);}}, new HashSet<>());

    TopologyUpdateHandlingReport report = new TopologyUpdateHandlingReport();

    topologyCluster.update(Collections.singleton(componentToAddition), Collections.emptySet(), UpdateEventType.UPDATE, report);

    Assert.assertEquals(2L, report.getUpdatedHostNames().size());
    Assert.assertTrue(report.getUpdatedHostNames().contains("hostName1"));
    Assert.assertTrue(report.getUpdatedHostNames().contains("hostName3"));

    Assert.assertEquals(2L, topologyCluster.getTopologyComponents().size());
  }

  @Test
  public void testHandlingReportComponentDeletePartially() throws NullHostNameException {
    TopologyComponent dummyComponent = createDummyTopologyComponent("comp1",
        new Long[]{1L, 2L}, new String[]{"hostName1", "hostName2"});
    TopologyComponent componentToDelete = createDummyTopologyComponent("comp2",
        new Long[]{1L, 3L}, new String[]{"hostName1", "hostName3"});

    TopologyComponent update = createDummyTopologyComponent("comp2",
        new Long[]{1L}, new String[]{"hostName1"});

    TopologyCluster topologyCluster = new TopologyCluster(
        new HashSet(){{add(dummyComponent); add(componentToDelete);}}, new HashSet<>());

    TopologyUpdateHandlingReport report = new TopologyUpdateHandlingReport();

    topologyCluster.update(Collections.singleton(update), Collections.emptySet(), UpdateEventType.DELETE, report);

    Assert.assertEquals(1L, report.getUpdatedHostNames().size());
    Assert.assertTrue(report.getUpdatedHostNames().contains("hostName1"));

    Assert.assertEquals(2L, topologyCluster.getTopologyComponents().size());
  }

  @Test
  public void testHandlingReportComponentDeleteFully() throws NullHostNameException {
    TopologyComponent dummyComponent = createDummyTopologyComponent("comp1",
        new Long[]{1L, 2L}, new String[]{"hostName1", "hostName2"});
    TopologyComponent componentToDelete = createDummyTopologyComponent("comp2",
        new Long[]{1L, 3L}, new String[]{"hostName1", "hostName3"});

    TopologyComponent update = createDummyTopologyComponent("comp2",
        new Long[]{1L, 3L}, new String[]{"hostName1", "hostName3"});

    TopologyCluster topologyCluster = new TopologyCluster(
        new HashSet(){{add(dummyComponent); add(componentToDelete);}}, new HashSet<>());

    TopologyUpdateHandlingReport report = new TopologyUpdateHandlingReport();

    topologyCluster.update(Collections.singleton(update), Collections.emptySet(), UpdateEventType.DELETE, report);

    Assert.assertEquals(2L, report.getUpdatedHostNames().size());
    Assert.assertTrue(report.getUpdatedHostNames().contains("hostName1"));
    Assert.assertTrue(report.getUpdatedHostNames().contains("hostName3"));

    Assert.assertEquals(1L, topologyCluster.getTopologyComponents().size());
  }

  @Test
  public void testHandlingReportComponentUpdate() throws NullHostNameException {
    TopologyComponent dummyComponent = createDummyTopologyComponent("comp1",
        new Long[]{1L, 2L}, new String[]{"hostName1", "hostName2"});
    TopologyComponent componentToUpdate = createDummyTopologyComponent("comp2",
        new Long[]{1L, 3L}, new String[]{"hostName1", "hostName3"});

    TopologyComponent update = createDummyTopologyComponent("comp2",
        new Long[]{1L, 4L}, new String[]{"hostName1", "hostName4"});

    TopologyCluster topologyCluster = new TopologyCluster(
        new HashSet(){{add(dummyComponent); add(componentToUpdate);}}, new HashSet<>());

    TopologyUpdateHandlingReport report = new TopologyUpdateHandlingReport();

    topologyCluster.update(Collections.singleton(update), Collections.emptySet(), UpdateEventType.UPDATE, report);

    Assert.assertEquals(1L, report.getUpdatedHostNames().size());
    Assert.assertTrue(report.getUpdatedHostNames().contains("hostName4"));

    Assert.assertEquals(2L, topologyCluster.getTopologyComponents().size());
  }

  private TopologyComponent createDummyTopologyComponent(String componentName, Long[] hostIds, String[] hostNames) {
    return TopologyComponent.newBuilder()
        .setComponentName(componentName)
        .setServiceName("serviceName")
        .setHostIdentifiers(new HashSet<Long>(Arrays.asList(hostIds)), new HashSet<String>(Arrays.asList(hostNames)))
        .build();
  }
}
