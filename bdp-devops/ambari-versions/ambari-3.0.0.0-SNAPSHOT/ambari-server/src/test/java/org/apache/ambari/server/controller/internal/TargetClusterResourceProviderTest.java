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


import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.ivory.Cluster;
import org.apache.ambari.server.controller.ivory.IvoryService;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for TargetClusterResourceProvider.
 */
public class TargetClusterResourceProviderTest {


  private static Cluster.Interface interface1 = new Cluster.Interface("write", "hdfs://ec2.a.b.com:8020", "1.1.2.22");

  private static Map<String, String> interfaces = new HashMap<>();
  static {
    interfaces.put("type", interface1.getType());
    interfaces.put("endpoint", interface1.getEndpoint());
    interfaces.put("version", interface1.getVersion());
  }

  private static Cluster.Location location1 = new Cluster.Location("location1", "/mirrorthis");

  private static Map<String, String> locations  = new HashMap<>();
  static {
    locations.put("name", location1.getName());
    locations.put("path", location1.getPath());
  }

  @Test
  public void testCreateResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Set<Map<String, Object>> propertySet = new HashSet<>();

    Map<String, Object> properties = new HashMap<>();

    properties.put(TargetClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster1");
    properties.put(TargetClusterResourceProvider.CLUSTER_COLO_PROPERTY_ID, "Colo");
    properties.put(TargetClusterResourceProvider.CLUSTER_INTERFACES_PROPERTY_ID, Collections.singleton(interfaces));
    properties.put(TargetClusterResourceProvider.CLUSTER_LOCATIONS_PROPERTY_ID, Collections.singleton(locations));
    properties.put(TargetClusterResourceProvider.CLUSTER_PROPERTIES_PROPERTY_ID, Collections.singletonMap("P1", "V1"));

    // set expectations
    service.submitCluster(TargetClusterResourceProvider.getCluster("Cluster1", properties));

    // replay
    replay(service);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, Collections.emptyMap());

    TargetClusterResourceProvider provider = new TargetClusterResourceProvider(service);

    provider.createResources(request);

    // verify
    verify(service);
  }

  @Test
  public void testGetResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Set<Map<String, Object>> propertySet = new HashSet<>();

    Map<String, Object> properties = new HashMap<>();

    List<String> targetClusterNames = new LinkedList<>();
    targetClusterNames.add("Cluster1");
    targetClusterNames.add("Cluster2");
    targetClusterNames.add("Cluster3");

    Cluster.Interface interface1 = new Cluster.Interface("type", "endpoint", "version");
    Cluster.Location  location1  = new Cluster.Location("name", "path");

    Cluster targetCluster1 = new Cluster("Cluster1", "Colo", Collections.singleton(interface1),
        Collections.singleton(location1), Collections.singletonMap("P1", "V1"));
    Cluster targetCluster2 = new Cluster("Cluster2", "Colo", Collections.singleton(interface1),
        Collections.singleton(location1), Collections.singletonMap("P1", "V1"));
    Cluster targetCluster3 = new Cluster("Cluster3", "Colo", Collections.singleton(interface1),
        Collections.singleton(location1), Collections.singletonMap("P1", "V1"));

    // set expectations
    expect(service.getClusterNames()).andReturn(targetClusterNames);

    expect(service.getCluster("Cluster1")).andReturn(targetCluster1);
    expect(service.getCluster("Cluster2")).andReturn(targetCluster2);
    expect(service.getCluster("Cluster3")).andReturn(targetCluster3);

    // replay
    replay(service);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, Collections.emptyMap());

    TargetClusterResourceProvider provider = new TargetClusterResourceProvider(service);

    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(3, resources.size());

    // verify
    verify(service);
  }

  @Test
  public void testUpdateResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Set<Map<String, Object>> propertySet = new HashSet<>();

    Map<String, Object> properties = new HashMap<>();

    properties.put(TargetClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster1");
    properties.put(TargetClusterResourceProvider.CLUSTER_COLO_PROPERTY_ID, "Colo");
    properties.put(TargetClusterResourceProvider.CLUSTER_INTERFACES_PROPERTY_ID, Collections.singleton(interfaces));
    properties.put(TargetClusterResourceProvider.CLUSTER_LOCATIONS_PROPERTY_ID, Collections.singleton(locations));
    properties.put(TargetClusterResourceProvider.CLUSTER_PROPERTIES_PROPERTY_ID + "/P1", "V1");

    List<String> targetClusterNames = new LinkedList<>();
    targetClusterNames.add("Cluster1");

    Set<Cluster.Interface> interfaceSet = Collections.singleton(interface1);
    Set<Cluster.Location>  locationSet  = Collections.singleton(location1);

    Cluster targetCluster1 = new Cluster("Cluster1", "Colo", interfaceSet,
        locationSet, Collections.singletonMap("P1", "V1"));

    // set expectations
    expect(service.getClusterNames()).andReturn(targetClusterNames);

    expect(service.getCluster("Cluster1")).andReturn(targetCluster1);

    service.updateCluster(targetCluster1);

    // replay
    replay(service);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, Collections.emptyMap());

    TargetClusterResourceProvider provider = new TargetClusterResourceProvider(service);

    provider.updateResources(request, null);

    // verify
    verify(service);
  }

  @Test
  public void testDeleteResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    List<String> targetClusterNames = new LinkedList<>();
    targetClusterNames.add("Cluster1");

    Cluster.Interface interface1 = new Cluster.Interface("type", "endpoint", "version");
    Cluster.Location  location1  = new Cluster.Location("name", "path");

    Cluster targetCluster1 = new Cluster("Cluster1", "Colo", Collections.singleton(interface1),
        Collections.singleton(location1), Collections.singletonMap("P1", "V1"));

    // set expectations
    expect(service.getClusterNames()).andReturn(targetClusterNames);

    expect(service.getCluster("Cluster1")).andReturn(targetCluster1);

    service.deleteCluster("Cluster1");

    // replay
    replay(service);


    TargetClusterResourceProvider provider = new TargetClusterResourceProvider(service);

    Predicate predicate = new PredicateBuilder().property(TargetClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster1").toPredicate();

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    // verify
    verify(service);
  }

}
