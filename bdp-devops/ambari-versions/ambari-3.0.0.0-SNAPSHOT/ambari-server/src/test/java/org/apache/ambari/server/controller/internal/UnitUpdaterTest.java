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

import static org.apache.ambari.server.testutils.TestCollectionUtils.map;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Rule;
import org.junit.Test;

public class UnitUpdaterTest extends EasyMockSupport {
  public static final String HEAPSIZE = "oozie_heapsize";
  @Rule public EasyMockRule mocks = new EasyMockRule(this);
  public static final String OOZIE = "OOZIE";
  public static final String OOZIE_ENV = "oozie-env";
  private Map<String, Stack.ConfigProperty> stackConfigWithMetadata = new HashMap<>();
  private UnitUpdater unitUpdater;
  private @Mock ClusterTopology clusterTopology;
  private @Mock Blueprint blueprint;
  private @Mock Stack stack;

  @Test
  public void testStackUnitIsAppendedWhereUnitIsNotDefined() throws Exception {
    stackUnitIs(HEAPSIZE, "GB");
    assertEquals("1g", updateUnit(OOZIE, OOZIE_ENV, HEAPSIZE, "1"));
  }

  @Test
  public void testDefaultMbStackUnitIsAppendedWhereUnitIsNotDefined() throws Exception {
    assertEquals("4096m", updateUnit(OOZIE, OOZIE_ENV, HEAPSIZE, "4096"));
  }

  @Test
  public void testNoUnitIsAppendedWhenPropertyAlreadyHasTheStackUnit() throws Exception {
    stackUnitIs(HEAPSIZE, "MB");
    assertEquals("128m", updateUnit(OOZIE, OOZIE_ENV, HEAPSIZE, "128m"));
  }

  @Test
  public void testNoUnitIsAppendedIfStackUnitIsInBytes() throws Exception {
    stackUnitIs(HEAPSIZE, "Bytes");
    assertEquals("128", updateUnit(OOZIE, OOZIE_ENV, HEAPSIZE, "128"));
  }

  @Test
  public void testUnitSuffixIsCaseInsenitiveAndWhiteSpaceTolerant() throws Exception {
    stackUnitIs(HEAPSIZE, "GB");
    assertEquals("1g", updateUnit(OOZIE, OOZIE_ENV, HEAPSIZE, " 1G "));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRejectValuesWhereStackUnitDoesNotMatchToGiveUnit() throws Exception {
    stackUnitIs(HEAPSIZE, "MB");
    updateUnit(OOZIE, OOZIE_ENV, HEAPSIZE, "2g");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRejectEmptyPropertyValue() throws Exception {
    updateUnit(OOZIE, OOZIE_ENV, HEAPSIZE, "");
  }

  @Test
  public void updateUnits() {
    stackUnitIs(HEAPSIZE, "MB");
    setUpStack(OOZIE, OOZIE_ENV);

    Map<String, Map<String, String>> properties = map(
      OOZIE_ENV, map(HEAPSIZE, "1024"),
      "core-site", map("fs.trash.interval", "360"));
    Configuration configuration = new Configuration(properties, new HashMap<>());

    UnitUpdater.updateUnits(configuration, stack);

    Map<String, Map<String, String>> expected = map(
      OOZIE_ENV, map(HEAPSIZE, "1024m"),
      "core-site", map("fs.trash.interval", "360"));

    assertEquals(expected, configuration.getProperties());
  }

  @Test
  public void removeUnits() {
    stackUnitIs(HEAPSIZE, "MB");
    setUpStack(OOZIE, OOZIE_ENV);

    Map<String, Map<String, String>> properties = map(
      OOZIE_ENV, map(HEAPSIZE, "1024m"),
      "core-site", map("fs.trash.interval", "360"));
    Configuration configuration = new Configuration(properties, new HashMap<>());

    UnitUpdater.removeUnits(configuration, stack);

    Map<String, Map<String, String>> expected = map(
      OOZIE_ENV, map(HEAPSIZE, "1024"),
      "core-site", map("fs.trash.interval", "360"));

    assertEquals(expected, configuration.getProperties());
  }

  private void stackUnitIs(String name, String unit) {
    ValueAttributesInfo propertyValueAttributes = new ValueAttributesInfo();
    propertyValueAttributes.setUnit(unit);
    stackConfigWithMetadata.put(name, configProperty(name, unit));
  }

  public static Stack.ConfigProperty configProperty(String name, String unit) {
    ValueAttributesInfo propertyValueAttributes = new ValueAttributesInfo();
    propertyValueAttributes.setUnit(unit);
    return new Stack.ConfigProperty(new StackConfigurationResponse(
      name,
      "any",
      "any",
      "any",
      "any",
      true,
      Collections.emptySet(),
      Collections.emptyMap(),
      propertyValueAttributes,
      Collections.emptySet()));
  }

  private void setUpStack(String serviceName, String configType) {
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata(serviceName, configType)).andReturn(stackConfigWithMetadata).anyTimes();
    replayAll();
  }

  private String updateUnit(String serviceName, String configType, String propName, String propValue) {
    UnitUpdater updater = new UnitUpdater(serviceName, configType);
    setUpStack(serviceName, configType);
    return updater.updateForClusterCreate(propName, propValue, Collections.emptyMap(), clusterTopology);
  }

}