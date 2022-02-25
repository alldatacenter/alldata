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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Abstract metric provider tests.
 */
public class AbstractPropertyProviderTest {


  @Test
  public void testGetComponentMetrics() {
    Map<String, Map<String, PropertyInfo>> componentMetrics = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    AbstractPropertyProvider provider = new TestPropertyProvider(componentMetrics);
    Assert.assertEquals(componentMetrics, provider.getComponentMetrics());
  }

  @Test
  public void testGetPropertyInfoMap() {
    AbstractPropertyProvider provider = new TestPropertyProvider(PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent));

    // specific property
    Map<String, PropertyInfo> propertyInfoMap = provider.getPropertyInfoMap("NAMENODE", "metrics/cpu/cpu_aidle");
    Assert.assertEquals(1, propertyInfoMap.size());
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/cpu/cpu_aidle"));

    // category
    propertyInfoMap = provider.getPropertyInfoMap("NAMENODE", "metrics/disk");
    Assert.assertEquals(3, propertyInfoMap.size());
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/disk/disk_free"));
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/disk/disk_total"));
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/disk/part_max_used"));
  }

  @Test
  public void testGetJMXPropertyInfoMap() {
    AbstractPropertyProvider provider = new TestPropertyProvider(
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent));

    // top level category
    Map<String, PropertyInfo> propertyInfoMap = provider.getPropertyInfoMap("DATANODE", "metrics");
    Assert.assertEquals(86, propertyInfoMap.size());

    // specific property
    propertyInfoMap = provider.getPropertyInfoMap("DATANODE", "metrics/rpc/RpcQueueTime_avg_time");
    Assert.assertEquals(1, propertyInfoMap.size());
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/rpc/RpcQueueTime_avg_time"));

    // category
    propertyInfoMap = provider.getPropertyInfoMap("DATANODE", "metrics/rpc/");
    Assert.assertEquals(12, propertyInfoMap.size());
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/rpc/RpcQueueTime_avg_time"));
  }

  @Test
  public void testSubstituteArguments() throws Exception
  {
    //simple substitute
    String newPropertyId = AbstractPropertyProvider.substituteArgument("category/name1/$1/name2/$2", "$1", "foo");
    Assert.assertEquals("category/name1/foo/name2/$2", newPropertyId);

    newPropertyId = AbstractPropertyProvider.substituteArgument("category/name1/$1/name2/$2", "$2", "bar");
    Assert.assertEquals("category/name1/$1/name2/bar", newPropertyId);

    //substitute with method
    newPropertyId = AbstractPropertyProvider.substituteArgument(
        "category/name1/$1.toLowerCase()/name2/$2.toUpperCase()", "$1", "FOO");
    Assert.assertEquals("category/name1/foo/name2/$2.toUpperCase()", newPropertyId);

    newPropertyId = AbstractPropertyProvider.substituteArgument(
        "category/name1/$1.toLowerCase()/name2/$2.toUpperCase()", "$2", "bar");
    Assert.assertEquals("category/name1/$1.toLowerCase()/name2/BAR", newPropertyId);

    //substitute with chained methods
    newPropertyId = AbstractPropertyProvider.substituteArgument(
        "category/name1/$1.toLowerCase().substring(1)/name2", "$1", "FOO");
    Assert.assertEquals("category/name1/oo/name2", newPropertyId);

    newPropertyId = AbstractPropertyProvider.substituteArgument(
        "category/name1/$1.toLowerCase().substring(1).concat(\"_post\")/name2/$2.concat(\"_post\")", "$1", "FOO");
    newPropertyId = AbstractPropertyProvider.substituteArgument(newPropertyId, "$2", "bar");
    Assert.assertEquals("category/name1/oo_post/name2/bar_post", newPropertyId);
  }

  @Test
  public void testUpdateComponentMetricMapHDP1() {
    Map<String, Map<String, PropertyInfo>> componentMetrics =
      PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);

    AbstractPropertyProvider provider = new TestPropertyProvider(componentMetrics);

    Map<String, PropertyInfo> flumeMetrics = provider.getComponentMetrics().get(
      "FLUME_HANDLER");

    int metricsBefore = flumeMetrics.size();
    String specificMetric = "metrics/flume/arg1/CHANNEL/arg2/ChannelCapacity";
    String specificPropertyInfoId = "arg1.CHANNEL.arg2.ChannelCapacity";
    Map<String, PropertyInfo> componentMetricMap =
      provider.getComponentMetrics().get("FLUME_HANDLER");

    Assert.assertNull(flumeMetrics.get(specificMetric));

    provider.updateComponentMetricMap(componentMetricMap, specificMetric);

    Assert.assertEquals(metricsBefore + 1, flumeMetrics.size());
    Assert.assertNotNull(flumeMetrics.get(specificMetric));
    Assert.assertEquals(specificPropertyInfoId,
      flumeMetrics.get(specificMetric).getPropertyId());
  }


  private static class TestPropertyProvider extends AbstractPropertyProvider {

    public TestPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics) {
      super(componentMetrics);
    }

    @Override
    public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) throws SystemException {
      return null;
    }
  }
}
