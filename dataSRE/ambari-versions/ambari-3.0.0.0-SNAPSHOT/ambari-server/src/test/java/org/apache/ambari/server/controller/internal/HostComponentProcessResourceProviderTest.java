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
package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests the {@link HostComponentProcessResourceProvider} class
 * @author ncole
 *
 */
public class HostComponentProcessResourceProviderTest {

  
  @Test
  public void testGetResources() throws Exception {
    @SuppressWarnings("unchecked")
    ResourceProvider provider = init(
        new HashMap<String,String>() {{
          put("status", "RUNNING");
          put("name", "a1");
        }});

    PredicateBuilder pb = new PredicateBuilder().property(
        HostComponentProcessResourceProvider.CLUSTER_NAME).equals("c1").and();
    pb = pb.property(
        HostComponentProcessResourceProvider.HOST_NAME).equals("h1").and();
    Predicate predicate = pb.property(
        HostComponentProcessResourceProvider.COMPONENT_NAME).equals("comp1").toPredicate();
    
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());
    
    Set<Resource> resources = provider.getResources(request, predicate);
    
    Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(resources.size()));
    Resource res = resources.iterator().next();
    
    Assert.assertNotNull(res.getPropertyValue(
        HostComponentProcessResourceProvider.NAME));
    Assert.assertNotNull(res.getPropertyValue(
        HostComponentProcessResourceProvider.STATUS));
  }
  
  @Test
  public void testGetResources_none() throws Exception {

    @SuppressWarnings("unchecked")
    ResourceProvider provider = init();

    PredicateBuilder pb = new PredicateBuilder().property(
        HostComponentProcessResourceProvider.CLUSTER_NAME).equals("c1").and();
    pb = pb.property(
        HostComponentProcessResourceProvider.HOST_NAME).equals("h1").and();
    Predicate predicate = pb.property(
        HostComponentProcessResourceProvider.COMPONENT_NAME).equals("comp1").toPredicate();
    
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());
    
    Set<Resource> resources = provider.getResources(request, predicate);
    
    Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(resources.size()));
  }
  
  @Test
  public void testGetResources_many() throws Exception {

    @SuppressWarnings("unchecked")
    ResourceProvider provider = init(
        new HashMap<String,String>() {{
          put("status", "RUNNING");
          put("name", "a");
        }},
        new HashMap<String,String>() {{
          put("status", "RUNNING");
          put("name", "b");
        }},
        new HashMap<String,String>() {{
          put("status", "NOT_RUNNING");
          put("name", "c");
        }});        

    PredicateBuilder pb = new PredicateBuilder().property(
        HostComponentProcessResourceProvider.CLUSTER_NAME).equals("c1").and();
    pb = pb.property(
        HostComponentProcessResourceProvider.HOST_NAME).equals("h1").and();
    Predicate predicate = pb.property(
        HostComponentProcessResourceProvider.COMPONENT_NAME).equals("comp1").toPredicate();
    
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());
    
    Set<Resource> resources = provider.getResources(request, predicate);
    
    Assert.assertEquals(Integer.valueOf(3), Integer.valueOf(resources.size()));
    
    for (Resource r : resources) {
      Assert.assertNotNull(r.getPropertyValue(HostComponentProcessResourceProvider.NAME));
      Assert.assertNotNull(r.getPropertyValue(HostComponentProcessResourceProvider.STATUS));
    }
  }
  

  private ResourceProvider init(Map<String,String>... processes) throws Exception {
    Resource.Type type = Resource.Type.HostComponentProcess;
    
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    
    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    List<ServiceComponentHost> schList = Arrays.asList(sch);
    
    List<Map<String, String>> procList = Arrays.asList(processes);
    
    expect(amc.getClusters()).andReturn(clusters);
    expect(clusters.getCluster("c1")).andReturn(cluster);
    expect(cluster.getServiceComponentHosts((String) anyObject())).andReturn(schList);
    expect(sch.getServiceComponentName()).andReturn("comp1");
    expect(sch.getHostName()).andReturn("h1").anyTimes();
    expect(sch.getProcesses()).andReturn(procList);
    
    replay(amc, clusters, cluster, sch);
    
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        amc);
    
    return provider;
    
    
  }
  
}
