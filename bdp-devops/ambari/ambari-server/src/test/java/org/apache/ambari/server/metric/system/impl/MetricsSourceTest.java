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

package org.apache.ambari.server.metric.system.impl;

import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.metrics.system.MetricsSink;
import org.apache.ambari.server.metrics.system.SingleMetric;
import org.apache.ambari.server.metrics.system.impl.AmbariMetricSinkImpl;
import org.apache.ambari.server.metrics.system.impl.DatabaseMetricsSource;
import org.apache.ambari.server.metrics.system.impl.JvmMetricsSource;
import org.apache.ambari.server.metrics.system.impl.MetricsConfiguration;
import org.apache.ambari.server.state.alert.MetricSource;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import junit.framework.Assert;

@RunWith(EasyMockRunner.class)
public class MetricsSourceTest {

  @Test
  public void testJvmSourceInit_PreJVM1_8() {
    Assume.assumeThat(System.getProperty("java.version"), new LessThanVersionMatcher("1.8"));
    testJvmSourceInit(39);
  }

  @Test
  public void testJvmSourceInit_JVM1_8() {
    Assume.assumeThat(System.getProperty("java.version"), new VersionMatcher("1.8"));
    testJvmSourceInit(40);
  }

  private void testJvmSourceInit(int metricsSize) {
    JvmMetricsSource jvmMetricsSource = new JvmMetricsSource();
    MetricsConfiguration configuration = MetricsConfiguration.getMetricsConfiguration();
    MetricsSink sink = new TestAmbariMetricsSinkImpl();
    jvmMetricsSource.init(configuration, sink);
    org.junit.Assert.assertEquals(jvmMetricsSource.getMetrics().size(), metricsSize);
  }

  /* ****************************************************************
   * Matcher classes used in Assume checks
   * **************************************************************** */
  private class VersionMatcher extends BaseMatcher<String> {
    private final float version;

    VersionMatcher(String version) {
      this.version = Float.parseFloat(version);
    }

    @Override
    public boolean matches(Object o) {
      return parseVersion((String) o) == this.version;
    }

    float parseVersion(String versionString) {
      Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)).*");
      Matcher matcher = p.matcher(versionString);
      if (matcher.matches()) {
        return Float.parseFloat(matcher.group(1));
      } else {
        return 0f;
      }
    }

    @Override
    public void describeTo(Description description) {

    }

    public float getVersion() {
      return version;
    }
  }

  private class LessThanVersionMatcher extends VersionMatcher {

    LessThanVersionMatcher(String version) {
      super(version);
    }

    @Override
    public boolean matches(Object o) {
      return parseVersion((String) o) < getVersion();
    }

    @Override
    public void describeTo(Description description) {

    }
  }

  @Test(timeout=20000)
  public void testDatabaseMetricSourcePublish() throws InterruptedException {
    Map<String, Long> metricsMap = new HashMap<>();

    metricsMap.put("Timer.UpdateObjectQuery.HostRoleCommandEntity", 10000l); // Should be accepted.
    metricsMap.put("Timer.UpdateObjectQuery.HostRoleCommandEntity.SqlPrepare", 5000l); // Should be accepted.
    metricsMap.put("Timer.DirectReadQuery", 6000l); // Should be accepted.
    metricsMap.put("Timer.ReadAllQuery.StackEntity.StackEntity.findByNameAndVersion.SqlPrepare", 15000l); //Should be discarded

    metricsMap.put("Counter.UpdateObjectQuery.HostRoleCommandEntity", 10l); // Should be accepted & should add a computed metric.
    metricsMap.put("Counter.ReadObjectQuery.RequestEntity.request", 4330l); //Should be discarded
    metricsMap.put("Counter.ReadObjectQuery.MetainfoEntity.readMetainfoEntity.CacheMisses", 15l); // Should be accepted.

    DatabaseMetricsSource source = new DatabaseMetricsSource();

    MetricsConfiguration configuration = MetricsConfiguration.getSubsetConfiguration(
      MetricsConfiguration.getMetricsConfiguration(), "source.database.");

    MetricsSink sink = EasyMock.createMock(AmbariMetricSinkImpl.class);
    Capture<List<SingleMetric>> metricsCapture = EasyMock.newCapture();
    sink.publish(capture(metricsCapture));
    expectLastCall().once();

    replay(sink);
    source.init(configuration, sink);
    source.start();
    source.publish(metricsMap);
    Thread.sleep(5000l);
    verify(sink);

    Assert.assertTrue(metricsCapture.getValue().size() == 6);
  }

  @Test
  public void testDatabaseMetricsSourceAcceptMetric() {

    DatabaseMetricsSource source = new DatabaseMetricsSource();
    MetricsConfiguration configuration = MetricsConfiguration.getSubsetConfiguration(
      MetricsConfiguration.getMetricsConfiguration(), "source.database.");
    MetricsSink sink = new TestAmbariMetricsSinkImpl();
    source.init(configuration, sink);

    Assert.assertTrue(source.acceptMetric("Timer.UpdateObjectQuery.HostRoleCommandEntity.SqlPrepare"));
    Assert.assertFalse(source.acceptMetric("Counter.ReadObjectQuery.RequestEntity.request"));
    Assert.assertTrue(source.acceptMetric("Counter.ReadObjectQuery.MetainfoEntity.readMetainfoEntity.CacheMisses"));
  }

  @Test
  public void testJmxInfoSerialization() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
    MetricSource.JmxInfo jmxInfo = new MetricSource.JmxInfo();
    jmxInfo.setValue("custom");
    jmxInfo.setPropertyList(singletonList("prop1"));
    MetricSource.JmxInfo deserialized = mapper.readValue(mapper.writeValueAsString(jmxInfo), MetricSource.JmxInfo.class);
    assertEquals("custom", deserialized.getValue().toString());
    assertEquals(singletonList("prop1"), deserialized.getPropertyList());
  }
}
