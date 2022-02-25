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

package org.apache.ambari.server.topology;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verify;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.controller.internal.BlueprintResourceProvider;
import org.apache.ambari.server.controller.internal.BlueprintResourceProviderTest;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * BlueprintFactory unit tests.
 */
@SuppressWarnings("unchecked")
public class BlueprintFactoryTest {

  private static final String BLUEPRINT_NAME = "test-blueprint";

  BlueprintFactory factory = new BlueprintFactory();
  Stack stack = createNiceMock(Stack.class);
  BlueprintFactory testFactory = new TestBlueprintFactory(stack);
  BlueprintDAO dao = createStrictMock(BlueprintDAO.class);
  BlueprintEntity entity = createStrictMock(BlueprintEntity.class);
  BlueprintConfigEntity configEntity = createStrictMock(BlueprintConfigEntity.class);


  @Before
  public void init() throws Exception {
    setPrivateField(factory, "blueprintDAO", dao);

    Map<String, Collection<String>> componentMap = new HashMap<>();
    Collection<String> components1 = new HashSet<>();
    componentMap.put("test-service1", components1);
    components1.add("component1");
    Collection<String> components2 = new HashSet<>();
    componentMap.put("test-service2", components2);
    components2.add("component2");

    expect(stack.getComponents()).andReturn(componentMap).anyTimes();
    expect(stack.isMasterComponent("component1")).andReturn(true).anyTimes();
    expect(stack.isMasterComponent("component2")).andReturn(false).anyTimes();
    expect(stack.getServiceForComponent("component1")).andReturn("test-service1").anyTimes();
    expect(stack.getServiceForComponent("component2")).andReturn("test-service2").anyTimes();
  }

  @After
  public void tearDown() {
    reset(stack, dao, entity, configEntity);
  }

  //todo: implement
//  @Test
//  public void testGetBlueprint() throws Exception {
//
//    Collection<BlueprintConfigEntity> configs = new ArrayList<BlueprintConfigEntity>();
//    configs.add(configEntity);
//
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(entity).once();
//    expect(entity.getBlueprintName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
//    expect(entity.getConfigurations()).andReturn(configs).atLeastOnce();
//
//    replay(dao, entity);
//
//    Blueprint blueprint = factory.getBlueprint(BLUEPRINT_NAME);
//
//
//  }

  @Test
  public void testGetBlueprint_NotFound() throws Exception {
    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null).once();
    replay(dao, entity, configEntity);

    assertNull(factory.getBlueprint(BLUEPRINT_NAME));
  }

  @Test
  public void testCreateBlueprint() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();

    replay(stack, dao, entity, configEntity);
    Blueprint blueprint = testFactory.createBlueprint(props, null);

    assertEquals(BLUEPRINT_NAME, blueprint.getName());
    assertSame(stack, blueprint.getStack());
    assertEquals(2, blueprint.getHostGroups().size());

    Map<String, HostGroup> hostGroups = blueprint.getHostGroups();
    HostGroup group1 = hostGroups.get("group1");
    assertEquals("group1", group1.getName());
    assertEquals("1", group1.getCardinality());
    Collection<String> components = group1.getComponentNames();
    assertEquals(2, components.size());
    assertTrue(components.contains("component1"));
    assertTrue(components.contains("component2"));
    Collection<String> services = group1.getServices();
    assertEquals(2, services.size());
    assertTrue(services.contains("test-service1"));
    assertTrue(services.contains("test-service2"));
    assertTrue(group1.containsMasterComponent());
    //todo: add configurations/attributes to properties
    Configuration configuration = group1.getConfiguration();
    assertTrue(configuration.getProperties().isEmpty());
    assertTrue(configuration.getAttributes().isEmpty());

    HostGroup group2 = hostGroups.get("group2");
    assertEquals("group2", group2.getName());
    assertEquals("2", group2.getCardinality());
    components = group2.getComponentNames();
    assertEquals(1, components.size());
    assertTrue(components.contains("component1"));
    services = group2.getServices();
    assertEquals(1, services.size());
    assertTrue(services.contains("test-service1"));
    assertTrue(group2.containsMasterComponent());
    //todo: add configurations/attributes to properties
    //todo: test both v1 and v2 config syntax
    configuration = group2.getConfiguration();
    assertTrue(configuration.getProperties().isEmpty());
    assertTrue(configuration.getAttributes().isEmpty());

    verify(dao, entity, configEntity);
  }

  @Test(expected=NoSuchStackException.class)
  public void testCreateInvalidStack() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    StackFactory mockStackFactory =
      mockSupport.createMock(StackFactory.class);

    // setup mock to throw exception, to simulate invalid stack request
    expect(mockStackFactory.createStack("null", "null", null)).andThrow(new ObjectNotFoundException("Invalid Stack"));

    mockSupport.replayAll();

    BlueprintFactory factoryUnderTest =
      new BlueprintFactory(mockStackFactory);
    factoryUnderTest.createStack(new HashMap<>());

    mockSupport.verifyAll();
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreate_NoBlueprintName() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();
    props.remove(BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID);

    replay(stack, dao, entity, configEntity);
    testFactory.createBlueprint(props, null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreate_NoHostGroups() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();
    // remove all host groups
    ((Set<Map<String, Object>>) props.get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).clear();

    replay(stack, dao, entity, configEntity);
    testFactory.createBlueprint(props, null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreate_MissingHostGroupName() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();
    // remove the name property for one of the host groups
    ((Set<Map<String, Object>>) props.get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).iterator().next().remove("name");

    replay(stack, dao, entity, configEntity);
    testFactory.createBlueprint(props, null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreate_HostGroupWithNoComponents() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();
    // remove the components for one of the host groups
    ((Set<Map<String, Object>>) props.get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).
        iterator().next().remove(BlueprintResourceProvider.COMPONENT_PROPERTY_ID);

    replay(stack, dao, entity, configEntity);
    testFactory.createBlueprint(props, null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreate_HostGroupWithInvalidComponent() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();
    // change a component name to an invalid name
    ((Set<Map<String, Object>>) ((Set<Map<String, Object>>) props.get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).
        iterator().next().get(BlueprintResourceProvider.COMPONENT_PROPERTY_ID)).iterator().next().put("name", "INVALID_COMPONENT");

    replay(stack, dao, entity, configEntity);
    testFactory.createBlueprint(props, null);
  }

  private class TestBlueprintFactory extends BlueprintFactory {
    private Stack stack;

    public TestBlueprintFactory(Stack stack) {
      this.stack = stack;
    }

    @Override
    protected Stack createStack(Map<String, Object> properties) throws NoSuchStackException {
      return stack;
    }
  }


  private void setPrivateField(Object o, String field, Object value) throws Exception {
    Class<?> c = o.getClass();
    Field f = c.getDeclaredField(field);
    f.setAccessible(true);
    f.set(o, value);
  }
}
