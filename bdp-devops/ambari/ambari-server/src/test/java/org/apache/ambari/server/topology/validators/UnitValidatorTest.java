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
package org.apache.ambari.server.topology.validators;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.expect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class UnitValidatorTest extends EasyMockSupport {
  private static final String CONFIG_TYPE = "config-type";
  private static final String SERVICE = "service";
  @Rule public EasyMockRule mocks = new EasyMockRule(this);
  private Map<String, Stack.ConfigProperty> stackConfigWithMetadata = new HashMap<>();
  private UnitValidator validator;
  private @Mock ClusterTopology clusterTopology;
  private @Mock Blueprint blueprint;
  private @Mock Stack stack;

  @Test(expected = IllegalArgumentException.class)
  public void rejectsPropertyWithDifferentUnitThanStackUnit() throws Exception {
    stackUnitIs("property1", "MB");
    propertyToBeValidatedIs("property1", "12G");
    validate("property1");
  }

  @Test
  public void acceptsPropertyWithSameUnitThanStackUnit() throws Exception {
    stackUnitIs("property1", "MB");
    propertyToBeValidatedIs("property1", "12m");
    validate("property1");
  }

  @Test
  public void skipsValidatingIrrelevantProperty() throws Exception {
    stackUnitIs("property1", "MB");
    propertyToBeValidatedIs("property1", "12g");
    validate("property2");
  }

  @Before
  public void setUp() throws Exception {
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(Collections.emptyMap()).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata(SERVICE, CONFIG_TYPE)).andReturn(stackConfigWithMetadata).anyTimes();
  }

  private void propertyToBeValidatedIs(String propertyName, String propertyValue) throws InvalidTopologyException, ConfigurationTopologyException {
    Map<String, Map<String, String>> propertiesToBeValidated = new HashMap<String, Map<String, String>>() {{
      put(CONFIG_TYPE, new HashMap<String, String>(){{
        put(propertyName, propertyValue);
      }});
    }};
    expect(clusterTopology.getConfiguration()).andReturn(new Configuration(propertiesToBeValidated, emptyMap())).anyTimes();
    replayAll();
  }

  private void validate(String propertyName) throws InvalidTopologyException {
    validator = new UnitValidator(newHashSet(new UnitValidatedProperty(SERVICE, CONFIG_TYPE, propertyName)));
    validator.validate(clusterTopology);
  }

  private void stackUnitIs(String name, String unit) {
    ValueAttributesInfo propertyValueAttributes = new ValueAttributesInfo();
    propertyValueAttributes.setUnit(unit);
    stackConfigWithMetadata.put(name, new Stack.ConfigProperty(new StackConfigurationResponse(
      name,
      "any",
      "any",
      "any",
      "any",
      true,
      Collections.emptySet(),
      Collections.emptyMap(),
      propertyValueAttributes,
      Collections.emptySet()
    )));
  }
}