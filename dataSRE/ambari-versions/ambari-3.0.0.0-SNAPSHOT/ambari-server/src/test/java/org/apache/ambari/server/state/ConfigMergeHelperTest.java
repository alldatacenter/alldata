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
package org.apache.ambari.server.state;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.ConfigMergeHelper.ThreeWayValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Tests the {@link ConfigMergeHelper} class
 */
public class ConfigMergeHelperTest {

  private static final StackId currentStackId = new StackId("HDP-2.1.1");
  private static final StackId newStackId = new StackId("HPD-2.2.0");

  private Injector injector;
  private Clusters clustersMock;
  private AmbariMetaInfo ambariMetaInfoMock;

  @Before
  public void before() throws Exception {
    clustersMock = createNiceMock(Clusters.class);
    ambariMetaInfoMock = createNiceMock(AmbariMetaInfo.class);

    final InMemoryDefaultTestModule injectorModule = new InMemoryDefaultTestModule() {
      @Override
      protected void configure() {
        super.configure();
      }
    };

    MockModule mockModule = new MockModule();
    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(injectorModule).with(mockModule));
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testGetConflicts() throws Exception {
    Cluster clusterMock = createNiceMock(Cluster.class);

    expect(clustersMock.getCluster(anyString())).andReturn(clusterMock);

    expect(clusterMock.getCurrentStackVersion()).andReturn(currentStackId);
    expect(clusterMock.getServices()).andReturn(new HashMap<String, Service>() {{
      put("HDFS", createNiceMock(Service.class));
      put("ZK", createNiceMock(Service.class));
    }});


    Set<PropertyInfo> currentHDFSProperties = new HashSet<PropertyInfo>() {{
      add(createPropertyInfo("hdfs-env.xml", "equal.key", "equal-value"));
    }};

    Set<PropertyInfo> currentZKProperties = new HashSet<PropertyInfo>() {{
      add(createPropertyInfo("zk-env.xml", "different.key", "different-value-1"));
    }};

    Set<PropertyInfo> currentStackProperties = new HashSet<PropertyInfo>() {{
      add(createPropertyInfo("hadoop-env.xml", "equal.key", "modified.value"));
    }};

    expect(ambariMetaInfoMock.getServiceProperties(currentStackId.getStackName(),
      currentStackId.getStackVersion(), "HDFS")).andReturn(currentHDFSProperties);
    expect(ambariMetaInfoMock.getServiceProperties(currentStackId.getStackName(),
      currentStackId.getStackVersion(), "ZK")).andReturn(currentZKProperties);
    expect(ambariMetaInfoMock.getStackProperties(currentStackId.getStackName(),
      currentStackId.getStackVersion())).andReturn(currentStackProperties);

    Set<PropertyInfo> newHDFSProperties = new HashSet<PropertyInfo>() {{
      add(createPropertyInfo("hdfs-env.xml", "equal.key", "equal-value"));
      add(createPropertyInfo("new-hdfs-config.xml", "equal.key", "equal-value"));
    }};

    Set<PropertyInfo> newZKProperties = new HashSet<PropertyInfo>() {{
      add(createPropertyInfo("zk-env.xml", "equal.key", "different-value-2"));
      add(createPropertyInfo("zk-env.xml", "new.key", "new-value-2"));
    }};

    Set<PropertyInfo> newStackProperties = new HashSet<PropertyInfo>() {{
      add(createPropertyInfo("hadoop-env.xml", "equal.key", "another.value"));
    }};

    expect(ambariMetaInfoMock.getServiceProperties(newStackId.getStackName(),
      newStackId.getStackVersion(), "HDFS")).andReturn(newHDFSProperties);
    expect(ambariMetaInfoMock.getServiceProperties(newStackId.getStackName(),
      newStackId.getStackVersion(), "ZK")).andReturn(newZKProperties);
    expect(ambariMetaInfoMock.getStackProperties(newStackId.getStackName(),
      newStackId.getStackVersion())).andReturn(newStackProperties);

    // desired config of hdfs-env.xml
    Map<String, String> desiredHdfsEnvProperties = new HashMap<>();
    expect(clusterMock.getDesiredConfigByType("hdfs-env.xml")).andReturn(
      createConfigMock(desiredHdfsEnvProperties)
    );

    // desired config of zk-env.xml
    Map<String, String> desiredZkEnvProperties = new HashMap<>();
    expect(clusterMock.getDesiredConfigByType("hdfs-env.xml")).andReturn(
      createConfigMock(desiredZkEnvProperties)
    );

    // desired config of hadoop-env.xml
    Map<String, String> desiredHadoopEnvProperties = new HashMap<>();
    expect(clusterMock.getDesiredConfigByType("hadoop-env.xml")).andReturn(
      createConfigMock(desiredHadoopEnvProperties)
    );

    replay(clusterMock, clustersMock, ambariMetaInfoMock);

    ConfigMergeHelper configMergeHelper = injector.getInstance(ConfigMergeHelper.class);

    Map<String, Map<String, ThreeWayValue>> conflicts = configMergeHelper.getConflicts(
      "clustername", newStackId);

    assertNotNull(conflicts);
    assertEquals(2, conflicts.size());
    for (String key : conflicts.keySet()) {
      if (key.equals("hdfs-env")) {
        Map<String, ThreeWayValue> stringThreeWayValueMap = conflicts.get(key);
        assertEquals(1, stringThreeWayValueMap.size());
        assertEquals("equal-value", stringThreeWayValueMap.get("equal.key").oldStackValue);
        assertEquals("equal-value", stringThreeWayValueMap.get("equal.key").newStackValue);
        assertEquals("", stringThreeWayValueMap.get("equal.key").savedValue);
      } else if (key.equals("hadoop-env")) {
        Map<String, ThreeWayValue> stringThreeWayValueMap = conflicts.get(key);
        assertEquals(1, stringThreeWayValueMap.size());
        assertEquals("modified.value", stringThreeWayValueMap.get("equal.key").oldStackValue);
        assertEquals("another.value", stringThreeWayValueMap.get("equal.key").newStackValue);
        assertEquals("", stringThreeWayValueMap.get("equal.key").savedValue);
      } else {
        fail("Unexpected key");
      }
    }
    assertEquals(2, conflicts.size());
  }

  private PropertyInfo createPropertyInfo(String fileName, String name, String value) {
    PropertyInfo result = new PropertyInfo();
    result.setFilename(fileName);
    result.setName(name);
    result.setValue(value);
    return result;
  }

  /**
   * Generates config that returns properties
   * @param properties properties that should be returned by config mock
   * @return mock
   */
  private Config createConfigMock(Map<String, String> properties) {
    Config result = createNiceMock(Config.class);
    expect(result.getProperties()).andReturn(properties);
    return result;
  }

  @Test
  public void testNormalizeValue() throws Exception{
    // If template not defined
    String normalizedValue = ConfigMergeHelper.normalizeValue(null, "2048m");
    assertEquals("2048m", normalizedValue);

    // Template does not define heap
    normalizedValue = ConfigMergeHelper.normalizeValue("3k", "2048");
    assertEquals("2048", normalizedValue);

    // Template - megabytes
    normalizedValue = ConfigMergeHelper.normalizeValue("1024m", "2048");
    assertEquals("2048m", normalizedValue);

    normalizedValue = ConfigMergeHelper.normalizeValue("1024M", "2048");
    assertEquals("2048M", normalizedValue);

    // Template - gigabytes
    normalizedValue = ConfigMergeHelper.normalizeValue("4g", "2");
    assertEquals("2g", normalizedValue);

    normalizedValue = ConfigMergeHelper.normalizeValue("4G", "2");
    assertEquals("2G", normalizedValue);
  }



  private class MockModule implements Module {

    @Override
    public void configure(Binder binder) {
      binder.bind(Clusters.class).toInstance(clustersMock);
      binder.bind(AmbariMetaInfo.class).toInstance(ambariMetaInfoMock);
    }
  }
}
