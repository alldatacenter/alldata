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
package org.apache.ambari.server.controller.utilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.stack.Metric;
import org.junit.Assert;
import org.junit.Test;


/**
 * Property helper tests.
 */
public class PropertyHelperTest {

  @Test
  public void testGetPropertyId() {
    Assert.assertEquals("foo", PropertyHelper.getPropertyId("", "foo"));
    Assert.assertEquals("foo", PropertyHelper.getPropertyId(null, "foo"));
    Assert.assertEquals("foo", PropertyHelper.getPropertyId(null, "foo/"));

    Assert.assertEquals("cat", PropertyHelper.getPropertyId("cat", ""));
    Assert.assertEquals("cat", PropertyHelper.getPropertyId("cat", null));
    Assert.assertEquals("cat", PropertyHelper.getPropertyId("cat/", null));

    Assert.assertEquals("cat/foo", PropertyHelper.getPropertyId("cat", "foo"));
    Assert.assertEquals("cat/sub/foo", PropertyHelper.getPropertyId("cat/sub", "foo"));
    Assert.assertEquals("cat/sub/foo", PropertyHelper.getPropertyId("cat/sub", "foo/"));
  }

  @Test
  public void testGetJMXPropertyIds() {

    //version 1
    Map<String, Map<String, PropertyInfo>> metrics = PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent);
    Map<String, PropertyInfo> componentMetrics = metrics.get("HISTORYSERVER");
    Assert.assertNull(componentMetrics);
    componentMetrics = metrics.get("NAMENODE");
    Assert.assertNotNull(componentMetrics);
    PropertyInfo info = componentMetrics.get("metrics/jvm/memHeapUsedM");
    Assert.assertNotNull(info);
    Assert.assertEquals("Hadoop:service=NameNode,name=jvm.memHeapUsedM", info.getPropertyId());

  }

  @Test
  public void testGetPropertyCategory() {
    String propertyId = "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsRunning";
    String category = PropertyHelper.getPropertyCategory(propertyId);
    Assert.assertEquals("metrics/yarn/Queue/$1", category);

    category = PropertyHelper.getPropertyCategory(category);
    Assert.assertEquals("metrics/yarn/Queue", category);

    category = PropertyHelper.getPropertyCategory(category);
    Assert.assertEquals("metrics/yarn", category);

    category = PropertyHelper.getPropertyCategory(category);
    Assert.assertEquals("metrics", category);

    category = PropertyHelper.getPropertyCategory(category);
    Assert.assertNull(category);
  }

  @Test
  public void testGetCategories() {
    String propertyId = "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsRunning";

    Set<String> categories = PropertyHelper.getCategories(Collections.singleton(propertyId));

    Assert.assertTrue(categories.contains("metrics/yarn/Queue/$1"));
    Assert.assertTrue(categories.contains("metrics/yarn/Queue"));
    Assert.assertTrue(categories.contains("metrics/yarn"));
    Assert.assertTrue(categories.contains("metrics"));

    String propertyId2 = "foo/bar/baz";
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(propertyId);
    propertyIds.add(propertyId2);

    categories = PropertyHelper.getCategories(propertyIds);

    Assert.assertTrue(categories.contains("metrics/yarn/Queue/$1"));
    Assert.assertTrue(categories.contains("metrics/yarn/Queue"));
    Assert.assertTrue(categories.contains("metrics/yarn"));
    Assert.assertTrue(categories.contains("metrics"));
    Assert.assertTrue(categories.contains("foo/bar"));
    Assert.assertTrue(categories.contains("foo"));
  }

  @Test
  public void testContainsArguments() {
    Assert.assertFalse(PropertyHelper.containsArguments("foo"));
    Assert.assertFalse(PropertyHelper.containsArguments("foo/bar"));
    Assert.assertFalse(PropertyHelper.containsArguments("foo/bar/baz"));

    Assert.assertTrue(PropertyHelper.containsArguments("foo/bar/$1/baz"));
    Assert.assertTrue(PropertyHelper.containsArguments("foo/bar/$1/baz/$2"));
    Assert.assertTrue(PropertyHelper.containsArguments("$1/foo/bar/$2/baz"));
    Assert.assertTrue(PropertyHelper.containsArguments("$1/foo/bar/$2/baz/$3"));

    Assert.assertTrue(PropertyHelper.containsArguments("metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)"));

    Assert.assertFalse(PropertyHelper.containsArguments("$X/foo/bar/$Y/baz/$Z"));
  }

  /**
   * Test to make sure that point in time metrics are not in both JMX and Ganglia.
   * A metric marked as point in time should not be available from both JMX
   * and Ganglia.  The preference is to get point in time metrics from JMX
   * but they may come from Ganglia if not available from JMX.
   *
   * If there is a legitimate exception and the point in time metric should
   * be available from both property providers then please add an exception to
   * this test.
   */
  @Test
  public void testDuplicatePointInTimeMetrics() {
    TreeSet<String> set = new TreeSet<>();

    for (Resource.Type type : Resource.Type.values()) {

      Map<String, Map<String, PropertyInfo>> gids = PropertyHelper.getMetricPropertyIds(type);

      Map<String, Map<String, PropertyInfo>> jids = PropertyHelper.getJMXPropertyIds(type);

      if (gids != null && jids != null) {

        gids = normalizeMetricNames(gids);
        jids = normalizeMetricNames(jids);

        for (Map.Entry<String, Map<String, PropertyInfo>> gComponentEntry : gids.entrySet()) {

          String gComponent = gComponentEntry.getKey();

          Set<Map.Entry<String, PropertyInfo>> gComponentEntries = gComponentEntry.getValue().entrySet();

          for (Map.Entry<String, PropertyInfo> gMetricEntry : gComponentEntries) {

            Map<String, PropertyInfo> jMetrics = jids.get(gComponent);

            if (jMetrics != null) {
              String gMetric = gMetricEntry.getKey();
              PropertyInfo jProperty = jMetrics.get(gMetric);

              if (jProperty != null) {
                PropertyInfo gProperty = gMetricEntry.getValue();
                if (gProperty.isPointInTime()) {
                  String s = type + " : " + gComponent + " : " + gMetric + " : " + gProperty.getPropertyId();
                  set.add(s);
                }
              }
            }
          }
        }
      }
    }
    if (set.size() > 0) {
      System.out.println("The following point in time metrics are defined for both JMX and Ganglia.");
      System.out.println("The preference is to get point in time metrics from JMX only if possible.");
      System.out.println("If the metric can be obtained from JMX then set \"pointInTime\" : false for ");
      System.out.println("the metric in the Ganglia properties definition, otherwise remove the metric ");
      System.out.println("from the JMX properties definition.\n");
      for (String s : set) {
        System.out.println(s);
      }
      Assert.fail("Found duplicate point in time metrics.");
    }
  }

  /**
   * Test to make sure that any metrics that are marked as temporal only in Ganglia are available
   * as point in time from JMX.  If a metric can not be provided by JMX it may be marked
   * as point in time from Ganglia.
   *
   * If there is a legitimate exception and the metric should be temporal only then please add an
   * exception to this test.
   */
  @Test
  public void testTemporalOnlyMetrics() {
    TreeSet<String> set = new TreeSet<>();

    for (Resource.Type type : Resource.Type.values()) {

      Map<String, Map<String, PropertyInfo>> gids = PropertyHelper.getMetricPropertyIds(type);

      Map<String, Map<String, PropertyInfo>> jids = PropertyHelper.getJMXPropertyIds(type);

      if (gids != null && jids != null) {

        gids = normalizeMetricNames(gids);
        jids = normalizeMetricNames(jids);

        for (Map.Entry<String, Map<String, PropertyInfo>> gComponentEntry : gids.entrySet()) {

          String gComponent = gComponentEntry.getKey();

          Set<Map.Entry<String, PropertyInfo>> gComponentEntries = gComponentEntry.getValue().entrySet();

          for (Map.Entry<String, PropertyInfo> gMetricEntry : gComponentEntries) {

            Map<String, PropertyInfo> jMetrics = jids.get(gComponent);

            if (jMetrics != null) {

              String gMetric = gMetricEntry.getKey();
              PropertyInfo gProperty = gMetricEntry.getValue();

              if (!gProperty.isPointInTime()) {

                PropertyInfo jProperty = jMetrics.get(gMetric);

                if (jProperty == null || !jProperty.isPointInTime()) {

                  String s = type + " : " + gComponent + " : " + gMetric + " : " + gProperty.getPropertyId();

                  set.add(s);
                }
              }
            }
          }
        }
      }
    }
    if (set.size() > 0) {
      System.out.println("The following metrics are marked as temporal only for Ganglia ");
      System.out.println("but are not defined for JMX.");
      System.out.println("The preference is to get point in time metrics from JMX if possible.");
      System.out.println("If the metric can be obtained from JMX then add it to the JMX properties");
      System.out.println("definition, otherwise set set \"pointInTime\" : true for the metric in ");
      System.out.println("the Ganglia properties definition.\n");
      for (String s : set) {
        System.out.println(s);
      }
      Assert.fail("Found temporal only metrics.");
    }
  }

  /**
   * Test to make sure that no JMX metrics are marked as point in time.
   */
  @Test
  public void testJMXTemporal() {
    TreeSet<String> set = new TreeSet<>();

    for (Resource.Type type : Resource.Type.values()) {

      Map<String, Map<String, PropertyInfo>> jids = PropertyHelper.getJMXPropertyIds(type);

      if (jids != null) {

        for (Map.Entry<String, Map<String, PropertyInfo>> jComponentEntry : jids.entrySet()) {

          String jComponent = jComponentEntry.getKey();

          Set<Map.Entry<String, PropertyInfo>> jComponentEntries = jComponentEntry.getValue().entrySet();
          for (Map.Entry<String, PropertyInfo> jMetricEntry : jComponentEntries) {

            String jMetric = jMetricEntry.getKey();

            PropertyInfo jProperty = jMetricEntry.getValue();

            if (jProperty.isTemporal()) {
              String s = type + " : " + jComponent + " : " + jMetric + " : " + jProperty.getPropertyId();

              set.add(s);
            }
          }
        }
      }
    }

    if (set.size() > 0) {
      System.out.println("The following metrics are marked as temporal JMX.");
      System.out.println("JMX can provide point in time metrics only.\n");
      for (String s : set) {
        System.out.println(s);
      }
      Assert.fail("Found temporal JMX metrics.");
    }
  }

  @Test
  public void testInsertTagIntoMetricName() {
    Assert.assertEquals("rpc.rpc.client.CallQueueLength",
      PropertyHelper.insertTagInToMetricName("client", "rpc.rpc.CallQueueLength", "metrics/rpc/"));

    Assert.assertEquals("rpc.rpc.client.CallQueueLength",
      PropertyHelper.insertTagInToMetricName("client", "rpc.rpc.CallQueueLength", "metrics/rpc/"));

    Assert.assertEquals("metrics/rpc/client/CallQueueLen",
      PropertyHelper.insertTagInToMetricName("client", "metrics/rpc/CallQueueLen", "metrics/rpc/"));

    Assert.assertEquals("metrics/rpc/client/CallQueueLen",
      PropertyHelper.insertTagInToMetricName("client", "metrics/rpc/CallQueueLen", "metrics/rpc/"));

    Assert.assertEquals("rpcdetailed.rpcdetailed.client.FsyncAvgTime",
      PropertyHelper.insertTagInToMetricName("client", "rpcdetailed.rpcdetailed.FsyncAvgTime", "metrics/rpc/"));
    Assert.assertEquals("metrics/rpcdetailed/client/fsync_avg_time",
      PropertyHelper.insertTagInToMetricName("client", "metrics/rpcdetailed/fsync_avg_time", "metrics/rpc/"));
  }

  @Test
  public void testProcessRpcMetricDefinition() {
    //ganglia metric
    org.apache.ambari.server.state.stack.Metric metric =
      new org.apache.ambari.server.state.stack.Metric("rpcdetailed.rpcdetailed.FsyncAvgTime", false, true, false, "unitless");
    Map<String, Metric> replacementMap = PropertyHelper.processRpcMetricDefinition("ganglia", "NAMENODE", "metrics/rpcdetailed/fsync_avg_time", metric);
    Assert.assertNotNull(replacementMap);
    Assert.assertEquals(3, replacementMap.size());
    Assert.assertTrue(replacementMap.containsKey("metrics/rpcdetailed/client/fsync_avg_time"));
    Assert.assertTrue(replacementMap.containsKey("metrics/rpcdetailed/datanode/fsync_avg_time"));
    Assert.assertTrue(replacementMap.containsKey("metrics/rpcdetailed/healthcheck/fsync_avg_time"));
    Assert.assertEquals("rpcdetailed.rpcdetailed.client.FsyncAvgTime", replacementMap.get("metrics/rpcdetailed/client/fsync_avg_time").getName());
    Assert.assertEquals("rpcdetailed.rpcdetailed.datanode.FsyncAvgTime", replacementMap.get("metrics/rpcdetailed/datanode/fsync_avg_time").getName());
    Assert.assertEquals("rpcdetailed.rpcdetailed.healthcheck.FsyncAvgTime", replacementMap.get("metrics/rpcdetailed/healthcheck/fsync_avg_time").getName());

    //jmx metric
    metric =
      new org.apache.ambari.server.state.stack.Metric("Hadoop:service=NameNode,name=RpcDetailedActivity.FsyncAvgTime", true, false, false, "unitless");
    replacementMap = PropertyHelper.processRpcMetricDefinition("jmx", "NAMENODE", "metrics/rpcdetailed/fsync_avg_time", metric);
    Assert.assertNotNull(replacementMap);
    Assert.assertEquals(3, replacementMap.size());
    Assert.assertTrue(replacementMap.containsKey("metrics/rpcdetailed/client/fsync_avg_time"));
    Assert.assertTrue(replacementMap.containsKey("metrics/rpcdetailed/datanode/fsync_avg_time"));
    Assert.assertTrue(replacementMap.containsKey("metrics/rpcdetailed/healthcheck/fsync_avg_time"));
    Assert.assertEquals("Hadoop:service=NameNode,name=RpcDetailedActivity,tag=client.FsyncAvgTime", replacementMap.get("metrics/rpcdetailed/client/fsync_avg_time").getName());
    Assert.assertEquals("Hadoop:service=NameNode,name=RpcDetailedActivity,tag=datanode.FsyncAvgTime", replacementMap.get("metrics/rpcdetailed/datanode/fsync_avg_time").getName());
    Assert.assertEquals("Hadoop:service=NameNode,name=RpcDetailedActivity,tag=healthcheck.FsyncAvgTime", replacementMap.get("metrics/rpcdetailed/healthcheck/fsync_avg_time").getName());

    //jmx metric 2
    metric =
      new org.apache.ambari.server.state.stack.Metric("Hadoop:service=NameNode,name=RpcActivity.RpcQueueTime_avg_time", true, false, false, "unitless");
    replacementMap = PropertyHelper.processRpcMetricDefinition("jmx", "NAMENODE", "metrics/rpc/RpcQueueTime_avg_time", metric);
    Assert.assertNotNull(replacementMap);
    Assert.assertEquals(3, replacementMap.size());
    Assert.assertTrue(replacementMap.containsKey("metrics/rpc/client/RpcQueueTime_avg_time"));
    Assert.assertTrue(replacementMap.containsKey("metrics/rpc/datanode/RpcQueueTime_avg_time"));
    Assert.assertTrue(replacementMap.containsKey("metrics/rpc/healthcheck/RpcQueueTime_avg_time"));
    Assert.assertEquals("Hadoop:service=NameNode,name=RpcActivity,tag=client.RpcQueueTime_avg_time", replacementMap.get("metrics/rpc/client/RpcQueueTime_avg_time").getName());
    Assert.assertEquals("Hadoop:service=NameNode,name=RpcActivity,tag=datanode.RpcQueueTime_avg_time", replacementMap.get("metrics/rpc/datanode/RpcQueueTime_avg_time").getName());
    Assert.assertEquals("Hadoop:service=NameNode,name=RpcActivity,tag=healthcheck.RpcQueueTime_avg_time", replacementMap.get("metrics/rpc/healthcheck/RpcQueueTime_avg_time").getName());

  }

  // remove any replacement tokens (e.g. $1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)) in the metric names
  private static Map<String, Map<String, PropertyInfo>> normalizeMetricNames(Map<String, Map<String, PropertyInfo>> gids) {

    Map<String, Map<String, PropertyInfo>> returnMap = new HashMap<>();

    for (Map.Entry<String, Map<String, PropertyInfo>> gComponentEntry : gids.entrySet()) {

      String gComponent = gComponentEntry.getKey();
      Map<String, PropertyInfo> newMap = new HashMap<>();

      Set<Map.Entry<String, PropertyInfo>> gComponentEntries = gComponentEntry.getValue().entrySet();

      for (Map.Entry<String, PropertyInfo> gMetricEntry : gComponentEntries) {

        String gMetric = gMetricEntry.getKey();
        PropertyInfo propertyInfo = gMetricEntry.getValue();

        newMap.put(gMetric.replaceAll("\\$\\d+(\\.\\S+\\(\\S+\\))*", "*"), propertyInfo);
      }
      returnMap.put(gComponent, newMap);
    }
    return returnMap;
  }
}

