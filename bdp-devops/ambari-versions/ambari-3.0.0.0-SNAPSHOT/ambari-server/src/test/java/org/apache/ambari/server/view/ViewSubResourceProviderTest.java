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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.predicate.AlwaysPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.apache.ambari.view.NoSuchResourceException;
import org.apache.ambari.view.ReadRequest;
import org.apache.ambari.view.ResourceAlreadyExistsException;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.UnsupportedPropertyException;
import org.junit.Assert;
import org.junit.Test;

/**
 * ViewSubResourceProvider tests.
 */
public class ViewSubResourceProviderTest {



  private static String xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <resource>\n" +
      "        <name>resource</name>\n" +
      "        <plural-name>resources</plural-name>\n" +
      "        <id-property>id</id-property>\n" +
      "        <resource-class>org.apache.ambari.server.view.ViewSubResourceProviderTest$MyResource</resource-class>\n" +
      "        <provider-class>org.apache.ambari.server.view.ViewSubResourceProviderTest$MyResourceProvider</provider-class>\n" +
      "        <service-class>org.apache.ambari.server.view.ViewSubResourceProviderTest$MyResourceService</service-class>\n" +
      "    </resource>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "    </instance>\n" +
      "</view>";


  @Test
  public void testGetResources() throws Exception {

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml);
    ViewEntity viewEntity = ViewRegistryTest.getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewRegistryTest.getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    Map<Resource.Type, ViewSubResourceDefinition> resourceDefinitions = viewEntity.getResourceDefinitions();

    Assert.assertEquals(1, resourceDefinitions.size());

    Resource.Type type = resourceDefinitions.keySet().iterator().next();

    ViewSubResourceProvider viewSubResourceProvider = new ViewSubResourceProvider(type, MyResource.class, "id", viewEntity);

    Request request = PropertyHelper.getReadRequest("id", "properties", "metrics/myMetric");
    Predicate predicate = new AlwaysPredicate();

    Set<Resource> resources = viewSubResourceProvider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());

    predicate = new PredicateBuilder().property("metrics/myMetric").greaterThan(1).toPredicate();

    resources = viewSubResourceProvider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());

    Assert.assertTrue(((Integer) resources.iterator().next().getPropertyValue("metrics/myMetric")) > 1);
  }

  @Test
  public void testGetResources_temporal() throws Exception {

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml);
    ViewEntity viewEntity = ViewRegistryTest.getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewRegistryTest.getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    Map<Resource.Type, ViewSubResourceDefinition> resourceDefinitions = viewEntity.getResourceDefinitions();

    Assert.assertEquals(1, resourceDefinitions.size());

    Resource.Type type = resourceDefinitions.keySet().iterator().next();

    ViewSubResourceProvider viewSubResourceProvider = new ViewSubResourceProvider(type, MyResource.class, "id", viewEntity);

    Set<String> requestProperties = new HashSet<>();
    requestProperties.add("metrics/myMetric");

    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    TemporalInfo temporalInfo = new TemporalInfoImpl(1000L, 1100L, 10L);
    temporalInfoMap.put("metrics/myMetric", temporalInfo);

    Request request = PropertyHelper.getReadRequest(requestProperties, temporalInfoMap);
    Predicate predicate = new AlwaysPredicate();

    Set<Resource> resources = viewSubResourceProvider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
  }

  public static class MyResource {
    private String id;
    private String property;
    private Map<String, Object> metrics;

    public MyResource() {
    }

    public MyResource(String id, String property, Map<String, Object> metrics) {
      this.id = id;
      this.property = property;
      this.metrics = metrics;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getProperty() {
      return property;
    }

    public void setProperty(String property) {
      this.property = property;
    }

    public Map<String, Object> getMetrics() {
      return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
      this.metrics = metrics;
    }
  }

  public static class MyResourceProvider implements ResourceProvider<MyResource> {

    @Override
    public MyResource getResource(String resourceId, Set<String> properties)
        throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
      return null;
    }

    @Override
    public Set<MyResource> getResources(ReadRequest request)
        throws SystemException, NoSuchResourceException, UnsupportedPropertyException {

      Set<MyResource> resources = new HashSet<>();
      resources.add(new MyResource("1", "foo", getMetricsValue(1, request, "myMetric")));
      resources.add(new MyResource("2", "bar", getMetricsValue(2, request, "myMetric")));

      return resources;
    }

    private Map<String, Object> getMetricsValue(Number value, ReadRequest request, String metricName) {

      ReadRequest.TemporalInfo temporalInfo = request.getTemporalInfo("metrics/" + metricName);
      if (temporalInfo != null) {
        int steps = (int) ((temporalInfo.getEndTime() - temporalInfo.getStartTime()) / temporalInfo.getStep());

        Number[][] datapointsArray = new Number[steps][2];

        for (int i = 0; i < steps; ++i) {
          datapointsArray[i][0] = temporalInfo.getStartTime() + i * temporalInfo.getStep();
          datapointsArray[i][1] = value;
        }
        return Collections.singletonMap(metricName, datapointsArray);
      }
      return Collections.singletonMap(metricName, value);
    }

    @Override
    public void createResource(String resourceId, Map<String, Object> properties)
        throws SystemException, ResourceAlreadyExistsException, NoSuchResourceException, UnsupportedPropertyException {
    }

    @Override
    public boolean updateResource(String resourceId, Map<String, Object> properties)
        throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
      return false;
    }

    @Override
    public boolean deleteResource(String resourceId)
        throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
      return false;
    }
  }

  public static class MyResourceService {
    // nothing
  }
}
