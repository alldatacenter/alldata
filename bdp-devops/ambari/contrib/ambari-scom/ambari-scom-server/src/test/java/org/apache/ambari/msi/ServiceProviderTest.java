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

package org.apache.ambari.msi;

import junit.framework.Assert;
import org.apache.ambari.scom.TestClusterDefinitionProvider;
import org.apache.ambari.scom.TestHostInfoProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;

/**
 *
 */
public class ServiceProviderTest {

  @Test
  public void testGetResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    ServiceProvider provider = new ServiceProvider(clusterDefinition);
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);
    Assert.assertEquals(7, resources.size());
  }

  @Test
  public void testGetResourcesWithPredicate() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    ServiceProvider provider = new ServiceProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(ServiceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("MAPREDUCE").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(1, resources.size());

    predicate = new PredicateBuilder().property(ServiceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("HDFS").or().
        property(ServiceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("FLUME").toPredicate();
    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(2, resources.size());

    predicate = new PredicateBuilder().property(ServiceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("NO SERVICE").toPredicate();
    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertTrue(resources.isEmpty());
  }

  @Test
  public void testGetResourcesCheckState() throws Exception {
    TestStateProvider stateProvider = new TestStateProvider();
    ClusterDefinition clusterDefinition = new ClusterDefinition(stateProvider, new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    ServiceProvider provider = new ServiceProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(ServiceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("MAPREDUCE").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("STARTED", resource.getPropertyValue(ServiceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID));

    stateProvider.setHealthy(false);

    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(1, resources.size());

    resource = resources.iterator().next();
    Assert.assertEquals("INSTALLED", resource.getPropertyValue(ServiceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID));
  }

  @Test
  public void testGetResourcesCheckStateFromCategory() throws Exception {
    TestStateProvider stateProvider = new TestStateProvider();
    ClusterDefinition clusterDefinition = new ClusterDefinition(stateProvider, new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    ServiceProvider provider = new ServiceProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(ServiceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("MAPREDUCE").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest("ServiceInfo"), predicate);
    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("STARTED", resource.getPropertyValue(ServiceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID));

    stateProvider.setHealthy(false);

    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(1, resources.size());

    resource = resources.iterator().next();
    Assert.assertEquals("INSTALLED", resource.getPropertyValue(ServiceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID));
  }

  @Test
  public void testCreateResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    ServiceProvider provider = new ServiceProvider(clusterDefinition);

    try {
      provider.createResources(PropertyHelper.getReadRequest());
      Assert.fail("Expected UnsupportedOperationException.");
    } catch (UnsupportedOperationException e) {
      //expected
    }
  }

  @Test
  public void testUpdateResources() throws Exception {
    TestStateProvider stateProvider = new TestStateProvider();
    ClusterDefinition clusterDefinition = new ClusterDefinition(stateProvider, new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    ServiceProvider provider = new ServiceProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(ServiceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("MAPREDUCE").toPredicate();

    HashMap<String, Object> properties = new HashMap<String, Object>();

    properties.put(ServiceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "STARTED");

    Request updateRequest = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(updateRequest, predicate);

    Assert.assertEquals(StateProvider.State.Running, stateProvider.getState());

    properties.put(ServiceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "INSTALLED");

    updateRequest = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(updateRequest, predicate);

    Assert.assertEquals(StateProvider.State.Stopped, stateProvider.getState());

    properties.put(ServiceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "STARTED");

    updateRequest = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(updateRequest, predicate);

    Assert.assertEquals(StateProvider.State.Running, stateProvider.getState());
  }

  @Test
  public void testDeleteResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    ServiceProvider provider = new ServiceProvider(clusterDefinition);

    try {
      provider.deleteResources(null);
      Assert.fail("Expected UnsupportedOperationException.");
    } catch (UnsupportedOperationException e) {
      //expected
    }
  }
}
