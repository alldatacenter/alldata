package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackLevelConfigurationRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.state.DependencyInfo;
import org.easymock.EasyMockSupport;
import org.junit.Test;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

@SuppressWarnings("unchecked")
public class BaseBlueprintProcessorTest {

  //todo: Move these tests to the correct location.
  //todo: BaseBluprintProcess no longer exists.
  @Test
  public void testStackRegisterConditionalDependencies() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController = mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(
        Collections.<StackServiceResponse> emptySet());

    expect(mockMgmtController.getStackLevelConfigurations((Set<StackLevelConfigurationRequest>) anyObject())).andReturn(
        Collections.emptySet()).anyTimes();

        // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack = new Stack("HDP", "2.1", mockMgmtController) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }
    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 5,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));

    mockSupport.verifyAll();
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoHCAT() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController = mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(
        Collections.<StackServiceResponse> emptySet());

    expect(mockMgmtController.getStackLevelConfigurations((Set<StackLevelConfigurationRequest>) anyObject())).andReturn(
        Collections.emptySet()).anyTimes();

    // test dependencies
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack = new Stack("HDP", "2.1", mockMgmtController) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }
    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));

    mockSupport.verifyAll();
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoYarnClient()
      throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController = mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(
        Collections.<StackServiceResponse> emptySet());

    expect(mockMgmtController.getStackLevelConfigurations((Set<StackLevelConfigurationRequest>) anyObject())).andReturn(
        Collections.emptySet()).anyTimes();

    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack = new Stack("HDP", "2.1", mockMgmtController) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }
    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));

    mockSupport.verifyAll();
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoTezClient()
      throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController = mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(
        Collections.<StackServiceResponse> emptySet());

    expect(mockMgmtController.getStackLevelConfigurations((Set<StackLevelConfigurationRequest>) anyObject())).andReturn(
        Collections.emptySet()).anyTimes();

    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack = new Stack("HDP", "2.1", mockMgmtController) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }
    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));

    mockSupport.verifyAll();
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoMapReduceClient()
      throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController = mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(
        Collections.<StackServiceResponse> emptySet());

    expect(mockMgmtController.getStackLevelConfigurations((Set<StackLevelConfigurationRequest>) anyObject())).andReturn(
        Collections.emptySet()).anyTimes();

    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack = new Stack("HDP", "2.1", mockMgmtController) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }

    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));

    mockSupport.verifyAll();
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoOozieClient()
      throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController = mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(
        Collections.<StackServiceResponse> emptySet());

    expect(mockMgmtController.getStackLevelConfigurations((Set<StackLevelConfigurationRequest>) anyObject())).andReturn(
        Collections.emptySet()).anyTimes();

    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack = new Stack("HDP", "2.1", mockMgmtController) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }

    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));

    mockSupport.verifyAll();
  }


 //todo: validate method moved
//  @Test
//  public void testValidationOverrideForSecondaryNameNodeWithHA() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//
//    AmbariManagementController mockController =
//      mockSupport.createMock(AmbariManagementController.class);
//
//    AmbariMetaInfo mockMetaInfo =
//      mockSupport.createMock(AmbariMetaInfo.class);
//
//    BaseBlueprintProcessor.stackInfo = mockMetaInfo;
//
//    ServiceInfo serviceInfo = new ServiceInfo();
//    serviceInfo.setName("HDFS");
//
//    StackServiceResponse stackServiceResponse =
//      new StackServiceResponse(serviceInfo);
//
//    ComponentInfo componentInfo = new ComponentInfo();
//    componentInfo.setName("SECONDARY_NAMENODE");
//    // simulate the stack requirements that there
//    // always be one SECONDARY_NAMENODE per cluster
//    componentInfo.setCardinality("1");
//
//    StackServiceComponentResponse stackComponentResponse =
//      new StackServiceComponentResponse(componentInfo);
//
//    ComponentInfo componentInfoNameNode = new ComponentInfo();
//    componentInfoNameNode.setName("NAMENODE");
//    componentInfo.setCardinality("1-2");
//    StackServiceComponentResponse stackServiceComponentResponseTwo =
//      new StackServiceComponentResponse(componentInfoNameNode);
//
//    Set<StackServiceComponentResponse> responses =
//      new HashSet<StackServiceComponentResponse>();
//    responses.add(stackComponentResponse);
//    responses.add(stackServiceComponentResponseTwo);
//
//    expect(mockController.getStackServices(isA(Set.class))).andReturn(
//      Collections.singleton(stackServiceResponse));
//    expect(mockController.getStackComponents(isA(Set.class))).andReturn(
//      responses);
//    expect(mockController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(mockController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(mockMetaInfo.getComponentDependencies("HDP", "2.0.6", "HDFS", "SECONDARY_NAMENODE")).andReturn(Collections.<DependencyInfo>emptyList());
//    expect(mockMetaInfo.getComponentDependencies("HDP", "2.0.6", "HDFS", "NAMENODE")).andReturn(Collections.<DependencyInfo>emptyList());
//
//
//    mockSupport.replayAll();
//
//    BaseBlueprintProcessor baseBlueprintProcessor =
//      new BaseBlueprintProcessor(Collections.<String>emptySet(), Collections.<Resource.Type, String>emptyMap(), mockController) {
//        @Override
//        protected Set<String> getPKPropertyIds() {
//          return null;
//        }
//
//        @Override
//        public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
//          return null;
//        }
//
//        @Override
//        public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
//          return null;
//        }
//
//        @Override
//        public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
//          return null;
//        }
//
//        @Override
//        public RequestStatus deleteResources(Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
//          return null;
//        }
//      };
//
//    HostGroupComponentEntity hostGroupComponentEntity =
//      new HostGroupComponentEntity();
//    // don't include the SECONDARY_NAMENODE in this entity
//    hostGroupComponentEntity.setName("NAMENODE");
//
//    HostGroupEntity hostGroupEntity =
//      new HostGroupEntity();
//    hostGroupEntity.setName("host-group-one");
//    hostGroupEntity.setComponents(Collections.singleton(hostGroupComponentEntity));
//    hostGroupEntity.setConfigurations(Collections.<HostGroupConfigEntity>emptyList());
//
//    // setup config entity to simulate the case of NameNode HA being enabled
//    BlueprintConfigEntity configEntity =
//      new BlueprintConfigEntity();
//    configEntity.setConfigData("{\"dfs.nameservices\":\"mycluster\",\"key4\":\"value4\"}");
//    configEntity.setType("hdfs-site");
//
//    BlueprintEntity testEntity =
//      new BlueprintEntity();
//    testEntity.setBlueprintName("test-blueprint");
//    testEntity.setStackName("HDP");
//    testEntity.setStackVersion("2.0.6");
//    testEntity.setHostGroups(Collections.singleton(hostGroupEntity));
//    testEntity.setConfigurations(Collections.singleton(configEntity));
//
//    baseBlueprintProcessor.validateTopology(testEntity);
//
//    mockSupport.verifyAll();
//  }

//  @Test
//  public void testValidationOverrideForSecondaryNameNodeWithoutHA() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//
//    AmbariManagementController mockController =
//      mockSupport.createMock(AmbariManagementController.class);
//
//    AmbariMetaInfo mockMetaInfo =
//      mockSupport.createMock(AmbariMetaInfo.class);
//
//    BaseBlueprintProcessor.stackInfo = mockMetaInfo;
//
//    ServiceInfo serviceInfo = new ServiceInfo();
//    serviceInfo.setName("HDFS");
//
//    StackServiceResponse stackServiceResponse =
//      new StackServiceResponse(serviceInfo);
//
//    ComponentInfo componentInfo = new ComponentInfo();
//    componentInfo.setName("SECONDARY_NAMENODE");
//    // simulate the stack requirements that there
//    // always be one SECONDARY_NAMENODE per cluster
//    componentInfo.setCardinality("1");
//
//    StackServiceComponentResponse stackComponentResponse =
//      new StackServiceComponentResponse(componentInfo);
//
//    ComponentInfo componentInfoNameNode = new ComponentInfo();
//    componentInfoNameNode.setName("NAMENODE");
//    componentInfo.setCardinality("1-2");
//    StackServiceComponentResponse stackServiceComponentResponseTwo =
//      new StackServiceComponentResponse(componentInfoNameNode);
//
//    Set<StackServiceComponentResponse> responses =
//      new HashSet<StackServiceComponentResponse>();
//    responses.add(stackComponentResponse);
//    responses.add(stackServiceComponentResponseTwo);
//
//    expect(mockController.getStackServices(isA(Set.class))).andReturn(
//      Collections.singleton(stackServiceResponse));
//    expect(mockController.getStackComponents(isA(Set.class))).andReturn(
//      responses);
//    expect(mockController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(mockController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(mockMetaInfo.getComponentDependencies("HDP", "2.0.6", "HDFS", "SECONDARY_NAMENODE")).andReturn(Collections.<DependencyInfo>emptyList());
//    expect(mockMetaInfo.getComponentDependencies("HDP", "2.0.6", "HDFS", "NAMENODE")).andReturn(Collections.<DependencyInfo>emptyList());
//
//
//    mockSupport.replayAll();
//
//    BaseBlueprintProcessor baseBlueprintProcessor =
//      new BaseBlueprintProcessor(Collections.<String>emptySet(), Collections.<Resource.Type, String>emptyMap(), mockController) {
//        @Override
//        protected Set<String> getPKPropertyIds() {
//          return null;
//        }
//
//        @Override
//        public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
//          return null;
//        }
//
//        @Override
//        public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
//          return null;
//        }
//
//        @Override
//        public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
//          return null;
//        }
//
//        @Override
//        public RequestStatus deleteResources(Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
//          return null;
//        }
//      };
//
//    HostGroupComponentEntity hostGroupComponentEntity =
//      new HostGroupComponentEntity();
//    // don't include the SECONDARY_NAMENODE in this entity
//    hostGroupComponentEntity.setName("NAMENODE");
//
//    HostGroupEntity hostGroupEntity =
//      new HostGroupEntity();
//    hostGroupEntity.setName("host-group-one");
//    hostGroupEntity.setComponents(Collections.singleton(hostGroupComponentEntity));
//    hostGroupEntity.setConfigurations(Collections.<HostGroupConfigEntity>emptyList());
//
//
//
//    BlueprintEntity testEntity =
//      new BlueprintEntity();
//    testEntity.setBlueprintName("test-blueprint");
//    testEntity.setStackName("HDP");
//    testEntity.setStackVersion("2.0.6");
//    testEntity.setHostGroups(Collections.singleton(hostGroupEntity));
//    testEntity.setConfigurations(Collections.<BlueprintConfigEntity>emptyList());
//
//    try {
//      baseBlueprintProcessor.validateTopology(testEntity);
//      fail("IllegalArgumentException should have been thrown");
//    } catch (IllegalArgumentException expectedException) {
//      // expected exception
//    }
//
//    mockSupport.verifyAll();
//  }

  /**
   * Convenience class for easier setup/initialization of dependencies for unit
   * testing.
   */
  private static class TestDependencyInfo extends DependencyInfo {
    TestDependencyInfo(String dependencyName) {
      setName(dependencyName);
    }
  }
}