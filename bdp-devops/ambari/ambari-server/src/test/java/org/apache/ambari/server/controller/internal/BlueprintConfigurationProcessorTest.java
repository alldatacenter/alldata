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

import static com.google.common.collect.Iterators.peekingIterator;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.state.ConfigHelper.CLUSTER_ENV_STACK_FEATURES_PROPERTY;
import static org.apache.ambari.server.state.ConfigHelper.CLUSTER_ENV_STACK_NAME_PROPERTY;
import static org.apache.ambari.server.state.ConfigHelper.CLUSTER_ENV_STACK_PACKAGES_PROPERTY;
import static org.apache.ambari.server.state.ConfigHelper.CLUSTER_ENV_STACK_ROOT_PROPERTY;
import static org.apache.ambari.server.state.ConfigHelper.CLUSTER_ENV_STACK_TOOLS_PROPERTY;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.topology.AdvisedConfiguration;
import org.apache.ambari.server.topology.AmbariContext;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.ClusterTopologyImpl;
import org.apache.ambari.server.topology.Component;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupImpl;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.TopologyRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;

/**
 * BlueprintConfigurationProcessor unit tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AmbariServer.class)
public class BlueprintConfigurationProcessorTest extends EasyMockSupport {

  private static final Configuration EMPTY_CONFIG = new Configuration(emptyMap(), emptyMap());
  private final Map<String, Collection<String>> serviceComponents = new HashMap<>();
  private final Map<String, String> defaultClusterEnvProperties = new HashMap<>();

  private final String STACK_NAME = "testStack";
  private final String STACK_VERSION = "1";

  private final String CLUSTER_ENV_PROP = "cluster-env";

  private final Map<String, Map<String, String>> stackProperties = new HashMap<>();

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private AmbariContext ambariContext;

  @Mock(type = MockType.NICE)
  private Blueprint bp;

  @Mock(type = MockType.NICE)
  private ServiceInfo serviceInfo;

  @Mock(type = MockType.NICE)
  private Stack stack;

  @Mock(type = MockType.NICE)
  private AmbariManagementController controller;

  @Mock(type = MockType.NICE)
  private KerberosHelper kerberosHelper;

  @Mock(type = MockType.NICE)
  private KerberosDescriptor kerberosDescriptor;

  @Mock(type = MockType.NICE)
  private Clusters clusters;

  @Mock(type = MockType.NICE)
  private Cluster cluster;

  @Mock
  private TopologyRequest topologyRequestMock;

  @Mock(type = MockType.NICE)
  private ConfigHelper configHelper;

  @Before
  public void init() throws Exception {
    expect(bp.getStack()).andReturn(stack).anyTimes();
    expect(bp.getName()).andReturn("test-bp").anyTimes();
    expect(bp.getServiceInfos()).andReturn(Collections.emptyList()).anyTimes();

    expect(stack.getName()).andReturn(STACK_NAME).atLeastOnce();
    expect(stack.getVersion()).andReturn(STACK_VERSION).atLeastOnce();
    // return false for all components since for this test we don't care about the value
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata(anyObject(String.class), anyObject(String.class))).andReturn(emptyMap()).anyTimes();
    expect(stack.getConfiguration()).andReturn(Configuration.newEmpty()).anyTimes();

    expect(serviceInfo.getRequiredProperties()).andReturn(
      emptyMap()).anyTimes();
    expect(serviceInfo.getRequiredServices()).andReturn(Collections.emptyList()).anyTimes();

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("SECONDARY_NAMENODE");
    hdfsComponents.add("DATANODE");
    hdfsComponents.add("HDFS_CLIENT");
    serviceComponents.put("HDFS", hdfsComponents);

    Collection<String> yarnComponents = new HashSet<>();
    yarnComponents.add("RESOURCEMANAGER");
    yarnComponents.add("NODEMANAGER");
    yarnComponents.add("YARN_CLIENT");
    yarnComponents.add("APP_TIMELINE_SERVER");
    yarnComponents.add("TIMELINE_READER");
    serviceComponents.put("YARN", yarnComponents);

    Collection<String> mrComponents = new HashSet<>();
    mrComponents.add("MAPREDUCE2_CLIENT");
    mrComponents.add("HISTORY_SERVER");
    serviceComponents.put("MAPREDUCE2", mrComponents);

    Collection<String> zkComponents = new HashSet<>();
    zkComponents.add("ZOOKEEPER_SERVER");
    zkComponents.add("ZOOKEEPER_CLIENT");
    serviceComponents.put("ZOOKEEPER", zkComponents);

    Collection<String> hiveComponents = new HashSet<>();
    hiveComponents.add("MYSQL_SERVER");
    hiveComponents.add("HIVE_METASTORE");
    hiveComponents.add("HIVE_SERVER");
    serviceComponents.put("HIVE", hiveComponents);

    Collection<String> falconComponents = new HashSet<>();
    falconComponents.add("FALCON_SERVER");
    falconComponents.add("FALCON_CLIENT");
    serviceComponents.put("FALCON", falconComponents);

    Collection<String> gangliaComponents = new HashSet<>();
    gangliaComponents.add("GANGLIA_SERVER");
    gangliaComponents.add("GANGLIA_CLIENT");
    serviceComponents.put("GANGLIA", gangliaComponents);

    Collection<String> kafkaComponents = new HashSet<>();
    kafkaComponents.add("KAFKA_BROKER");
    serviceComponents.put("KAFKA", kafkaComponents);

    Collection<String> knoxComponents = new HashSet<>();
    knoxComponents.add("KNOX_GATEWAY");
    serviceComponents.put("KNOX", knoxComponents);

    Collection<String> oozieComponents = new HashSet<>();
    oozieComponents.add("OOZIE_SERVER");
    oozieComponents.add("OOZIE_CLIENT");
    serviceComponents.put("OOZIE", oozieComponents);

    Collection<String> hbaseComponents = new HashSet<>();
    hbaseComponents.add("HBASE_MASTER");
    serviceComponents.put("HBASE", hbaseComponents);

    Collection<String> atlasComponents = new HashSet<>();
    atlasComponents.add("ATLAS_SERVER");
    atlasComponents.add("ATLAS_CLIENT");
    serviceComponents.put("ATLAS", atlasComponents);

    Collection<String> amsComponents = new HashSet<>();
    amsComponents.add("METRICS_COLLECTOR");
    serviceComponents.put("AMBARI_METRICS", amsComponents);

    Collection<String> stormComponents = new HashSet<>();
    stormComponents.add("NIMBUS");
    serviceComponents.put("STORM", stormComponents);

    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");
    rangerComponents.add("RANGER_TAGSYNC");
    serviceComponents.put("RANGER",rangerComponents);

    for (Map.Entry<String, Collection<String>> entry : serviceComponents.entrySet()) {
      String service = entry.getKey();
      for (String component : entry.getValue()) {
        expect(stack.getServiceForComponent(component)).andReturn(service).anyTimes();
      }
    }

    expect(stack.getCardinality("MYSQL_SERVER")).andReturn(new Cardinality("0-1")).anyTimes();

    Set<String> emptySet = Collections.emptySet();
    expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();

    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();
    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();

   stackProperties.put(ConfigHelper.CLUSTER_ENV, defaultClusterEnvProperties);


    expect(ambariContext.isClusterKerberosEnabled(1)).andReturn(true).once();
    expect(ambariContext.getClusterName(1L)).andReturn("clusterName").anyTimes();
    PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(controller).anyTimes();
    PowerMock.replay(AmbariServer.class);
    expect(clusters.getCluster("clusterName")).andReturn(cluster).anyTimes();
    expect(controller.getKerberosHelper()).andReturn(kerberosHelper).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(kerberosHelper.getKerberosDescriptor(cluster, false)).andReturn(kerberosDescriptor).anyTimes();
    Set<String> properties = new HashSet<>();
    properties.add("core-site/hadoop.security.auth_to_local");
    expect(kerberosDescriptor.getAllAuthToLocalProperties()).andReturn(properties).anyTimes();
  }

  @After
  public void tearDown() {
    reset(bp, serviceInfo, stack, ambariContext, configHelper);
  }

  @Test
  public void testDoUpdateForBlueprintExport_SingleHostProperty() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("yarn.resourcemanager.hostname", "testhost");
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    String updatedVal = properties.get("yarn-site").get("yarn.resourcemanager.hostname");
    assertEquals("%HOSTGROUP::group1%", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_FilterProperties() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> kerberosEnvProps = new HashMap<>();
    kerberosEnvProps.put("admin_server_host", "test");
    kerberosEnvProps.put("kdc_hosts", "test");
    kerberosEnvProps.put("realm", "test");
    kerberosEnvProps.put("kdc_type", "test");
    kerberosEnvProps.put("ldap-url", "test");
    kerberosEnvProps.put("container_dn", "test");
    properties.put("kerberos-env", kerberosEnvProps);

    Map<String, String> krb5ConfProps = new HashMap<>();
    krb5ConfProps.put("domains", "test");
    properties.put("krb5-conf", krb5ConfProps);

    Map<String, String> tezSiteConfProps = new HashMap<>();
    tezSiteConfProps.put("tez.tez-ui.history-url.base", "test");
    properties.put("tez-site", tezSiteConfProps);


    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals(properties.size(), 3);
    assertEquals(((Map) properties.get("kerberos-env")).size(), 0);
    assertEquals(((Map) properties.get("krb5-conf")).size(), 0);
    assertEquals(((Map) properties.get("tez-site")).size(), 0);
  }

  @Test
  public void testDoUpdateForBlueprintExportRangerHAPolicyMgrExternalUrlProperty() throws Exception {
    // Given
    Map<String, String> rangerAdminProperties = Maps.newHashMap();
    rangerAdminProperties.put("DB_FLAVOR", "test_db_flavor");
    rangerAdminProperties.put("policymgr_external_url", "test_policymgr_external_url");


    Map<String, Map<String, String>> properties =
      ImmutableMap.of("admin-properties", rangerAdminProperties);


    Configuration clusterConfig = new Configuration(properties, ImmutableMap.of());

    Collection<String> hostGroup1Components = ImmutableSet.of("RANGER_ADMIN");
    TestHostGroup group1 = new TestHostGroup("group1", hostGroup1Components, Collections.singleton("testhost1"));

    Collection<String> hostGroup2Components = ImmutableSet.of("RANGER_ADMIN");
    TestHostGroup group2 = new TestHostGroup("group2", hostGroup2Components, Collections.singleton("testhost2"));


    Collection<TestHostGroup> hostGroups = ImmutableSet.of(group1, group2);


    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    // Then
    assertEquals("policymgr_external_url property's original value should be exported when Ranger Admin is deployed to multiple hosts.", "test_policymgr_external_url", properties.get("admin-properties").get("policymgr_external_url"));

  }

  @Test
  public void testDoUpdateForBlueprintExport_SingleHostProperty_specifiedInParentConfig() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> yarnSiteProps = new HashMap<>();
    yarnSiteProps.put("yarn.resourcemanager.hostname", "testhost");
    properties.put("yarn-site", yarnSiteProps);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Map<String, String> parentYarnSiteProps = new HashMap<>();
    parentYarnSiteProps.put("yarn.resourcemanager.resource-tracker.address", "testhost");
    parentProperties.put("yarn-site", parentYarnSiteProps);

    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());

    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("%HOSTGROUP::group1%", clusterConfig.getPropertyValue("yarn-site", "yarn.resourcemanager.hostname"));
    assertEquals("%HOSTGROUP::group1%", clusterConfig.getPropertyValue("yarn-site", "yarn.resourcemanager.resource-tracker.address"));
  }

  @Test
  public void testDoUpdateForBlueprintExport_SingleHostProperty_hostGroupConfiguration() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("yarn.resourcemanager.hostname", "testhost");
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");

    Map<String, Map<String, String>> group2Properties = new HashMap<>();
    Map<String, String> group2YarnSiteProps = new HashMap<>();
    group2YarnSiteProps.put("yarn.resourcemanager.resource-tracker.address", "testhost");
    group2YarnSiteProps.put("yarn.resourcemanager.webapp.https.address", "{{rm_host}}");
    group2Properties.put("yarn-site", group2YarnSiteProps);
    // host group config -> BP config -> cluster scoped config
    Configuration group2BPConfiguration = new Configuration(emptyMap(),
      emptyMap(), clusterConfig);

    Configuration group2Configuration = new Configuration(group2Properties,
      emptyMap(), group2BPConfiguration);

    // set config on hostgroup
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2,
      Collections.singleton("testhost2"), group2Configuration);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("%HOSTGROUP::group1%", properties.get("yarn-site").get("yarn.resourcemanager.hostname"));
    assertEquals("%HOSTGROUP::group1%",
      group2Configuration.getPropertyValue("yarn-site", "yarn.resourcemanager.resource-tracker.address"));
    assertNotNull("Placeholder property should not have been removed.",
      group2Configuration.getPropertyValue("yarn-site", "yarn.resourcemanager.webapp.https.address"));
  }

  @Test
  public void testDoUpdateForBlueprintExport_SingleHostProperty__withPort() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("fs.defaultFS", "testhost:8020");
    properties.put("core-site", typeProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    String updatedVal = properties.get("core-site").get("fs.defaultFS");
    assertEquals("%HOSTGROUP::group1%:8020", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_SingleHostProperty__ExternalReference() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("yarn.resourcemanager.hostname", "external-host");
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertFalse(properties.get("yarn-site").containsKey("yarn.resourcemanager.hostname"));
  }

  @Test
  public void testDoUpdateForBlueprintExport_MultiHostProperty() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("hbase.zookeeper.quorum", "testhost,testhost2,testhost2a,testhost2b");
    properties.put("hbase-site", typeProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    String updatedVal = properties.get("hbase-site").get("hbase.zookeeper.quorum");
    assertEquals("%HOSTGROUP::group1%,%HOSTGROUP::group2%", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_MultiHostProperty__WithPorts() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("templeton.zookeeper.hosts", "testhost:5050,testhost2:9090,testhost2a:9090,testhost2b:9090");
    properties.put("webhcat-site", typeProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    String updatedVal = properties.get("webhcat-site").get("templeton.zookeeper.hosts");
    assertEquals("%HOSTGROUP::group1%:5050,%HOSTGROUP::group2%:9090", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_MultiHostProperty__WithPrefixAndPorts() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("atlas.server.bind.address",
      "http://testhost:21000,http://testhost2:21000,http://testhost2a:21000,http://testhost2b:21000");
    properties.put("application-properties", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = Sets.newHashSet("NAMENODE", "SECONDARY_NAMENODE", "ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = Sets.newHashSet("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_SERVER");
    Set<String> hosts2 = Sets.newHashSet("testhost2", "testhost2a", "testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = Sets.newHashSet("HDFS_CLIENT", "ZOOKEEPER_CLIENT");
    Set<String> hosts3 = Sets.newHashSet("testhost3", "testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = Sets.newHashSet(group1, group2, group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    String updatedVal = properties.get("application-properties").get("atlas.server.bind.address");
    assertEquals("http://%HOSTGROUP::group1%:21000,http://%HOSTGROUP::group2%:21000", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_MultiHostProperty__YAML() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("storm.zookeeper.servers", "['testhost:5050','testhost2:9090','testhost2a:9090','testhost2b:9090']");
    typeProps.put("drpc_server_host", "['testhost:5050']");
    typeProps.put("storm_ui_server_host", "['testhost:5050']");
    typeProps.put("supervisor_hosts", "['testhost:5050','testhost2:9090']");

    properties.put("storm-site", typeProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    hgComponents.add("DRPC_SERVER");
    hgComponents.add("STORM_UI_SERVER");
    hgComponents.add("SUPERVISOR");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    hgComponents2.add("SUPERVISOR");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    String updatedVal = properties.get("storm-site").get("storm.zookeeper.servers");
    assertEquals("['%HOSTGROUP::group1%:5050','%HOSTGROUP::group2%:9090']", updatedVal);

    String updatedVa2 = properties.get("storm-site").get("drpc_server_host");
    assertEquals("['%HOSTGROUP::group1%:5050']", updatedVa2);

    String updatedVa3 = properties.get("storm-site").get("storm_ui_server_host");
    assertEquals("['%HOSTGROUP::group1%:5050']", updatedVa3);

    String updatedVa4 = properties.get("storm-site").get("supervisor_hosts");
    assertEquals("['%HOSTGROUP::group1%:5050','%HOSTGROUP::group2%:9090']", updatedVa4);
  }

  @Test
  public void testDoUpdateForBlueprintExport_DBHostProperty() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hiveSiteProps = new HashMap<>();
    hiveSiteProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://testhost/hive?createDatabaseIfNotExist=true");
    properties.put("hive-site", hiveSiteProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    hgComponents.add("MYSQL_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    String updatedVal = properties.get("hive-site").get("javax.jdo.option.ConnectionURL");
    assertEquals("jdbc:mysql://%HOSTGROUP::group1%/hive?createDatabaseIfNotExist=true", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_DBHostProperty__External() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://external-host/hive?createDatabaseIfNotExist=true");
    properties.put("hive-site", typeProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertFalse(properties.get("hive-site").containsKey("javax.jdo.option.ConnectionURL"));
  }

  @Test
  public void testDoUpdateForBlueprintExport_PasswordFilterApplied() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("REPOSITORY_CONFIG_PASSWORD", "test-password-one");
    typeProps.put("SSL_KEYSTORE_PASSWORD", "test-password-two");
    typeProps.put("SSL_TRUSTSTORE_PASSWORD", "test-password-three");
    typeProps.put("XAAUDIT.DB.PASSWORD", "test-password-four");
    typeProps.put("test.ssl.password", "test-password-five");
    typeProps.put("test.password.should.be.included", "test-another-pwd");

    //Checking functionality for fields marked as SECRET
    Map<String, String> secretProps = new HashMap<>();
    secretProps.put("knox_master_secret", "test-secret-one");
    secretProps.put("test.secret.should.be.included", "test-another-secret");
    // create a custom config type, to verify that the filters can
    // be applied across all config types
    Map<String, String> customProps = new HashMap<>();
    customProps.put("my_test_PASSWORD", "should be excluded");
    customProps.put("PASSWORD_mytest", "should be included");

    customProps.put("my_test_SECRET", "should be excluded");
    customProps.put("SECRET_mytest", "should be included");
    properties.put("ranger-yarn-plugin-properties", typeProps);
    properties.put("custom-test-properties", customProps);
    properties.put("secret-test-properties", secretProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);


    assertEquals("Exported properties map was not of the expected size", 2,
      properties.get("custom-test-properties").size());
    assertEquals("ranger-yarn-plugin-properties config type was not properly exported", 1,
      properties.get("ranger-yarn-plugin-properties").size());
    assertEquals("Exported secret properties map was not of the expected size", 1,
      properties.get("secret-test-properties").size());

    // verify that the following password properties matching the "*_PASSWORD" rule have been excluded
    assertFalse("Password property should have been excluded",
      properties.get("ranger-yarn-plugin-properties").containsKey("REPOSITORY_CONFIG_PASSWORD"));
    assertFalse("Password property should have been excluded",
      properties.get("ranger-yarn-plugin-properties").containsKey("SSL_KEYSTORE_PASSWORD"));
    assertFalse("Password property should have been excluded",
      properties.get("ranger-yarn-plugin-properties").containsKey("SSL_TRUSTSTORE_PASSWORD"));
    assertFalse("Password property should have been excluded",
      properties.get("ranger-yarn-plugin-properties").containsKey("XAAUDIT.DB.PASSWORD"));
    assertFalse("Password property should have been excluded",
      properties.get("ranger-yarn-plugin-properties").containsKey("test.ssl.password"));


    // verify that the property that does not match the "*_PASSWORD" rule is still included
    assertTrue("Expected password property not found",
      properties.get("ranger-yarn-plugin-properties").containsKey("test.password.should.be.included"));

    // verify that the following password properties matching the "*_SECRET" rule have been excluded
    assertFalse("Secret property should have been excluded",
      properties.get("secret-test-properties").containsKey("knox_master_secret"));
    // verify that the property that does not match the "*_SECRET" rule is still included
    assertTrue("Expected secret property not found",
      properties.get("secret-test-properties").containsKey("test.secret.should.be.included"));
    // verify the custom properties map has been modified by the filters
    assertEquals("custom-test-properties type was not properly exported",
      2, properties.get("custom-test-properties").size());

    // verify that the following password properties matching the "*_PASSWORD" rule have been excluded
    assertFalse("Password property should have been excluded",
      properties.get("custom-test-properties").containsKey("my_test_PASSWORD"));

    // verify that the property that does not match the "*_PASSWORD" rule is still included
    assertTrue("Expected password property not found",
      properties.get("custom-test-properties").containsKey("PASSWORD_mytest"));
    assertEquals("Expected password property should not have been modified",
      "should be included", properties.get("custom-test-properties").get("PASSWORD_mytest"));

  }


  @Test
  public void testFalconConfigExport() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> falconStartupProperties = new HashMap<>();
    configProperties.put("falcon-startup.properties", falconStartupProperties);

    // setup properties that include host information
    falconStartupProperties.put("*.broker.url", expectedHostName + ":" + expectedPortNum);
    falconStartupProperties.put("*.falcon.service.authentication.kerberos.principal", "falcon/" + expectedHostName + "@EXAMPLE.COM");
    falconStartupProperties.put("*.falcon.http.authentication.kerberos.principal", "HTTP/" + expectedHostName + "@EXAMPLE.COM");

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("FALCON_SERVER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("Falcon Broker URL property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), falconStartupProperties.get("*.broker.url"));

  }

  @Test
  public void testTezConfigExport() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> tezSiteProperties = new HashMap<>();
    configProperties.put("tez-site", tezSiteProperties);

    // set the UI property, to simulate the case of a UI-created cluster with TEZ
    tezSiteProperties.put("tez.tez-ui.history-url.base", "http://host:port/TEZ/TEZ_VIEW");

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("TEZ_CLIENT");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertFalse("tez.tez-ui.history-url.base should not be present in exported blueprint in tez-site",
      tezSiteProperties.containsKey("tez.tez-ui.history-url.base"));
  }

  /**
   * There is no support currently for deploying a fully Kerberized
   * cluster with Blueprints.  This test verifies the current treatment
   * of Kerberos-related properties in a Blueprint export.
   *
   * @throws Exception
   */
  @Test
  public void testKerberosConfigExport() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> kerberosEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    configProperties.put("kerberos-env", kerberosEnvProperties);
    configProperties.put("core-site", coreSiteProperties);

    // simulate the case of a Kerberized cluster, including config
    // added by the Kerberos service
    kerberosEnvProperties.put("admin_server_host", expectedHostName);
    kerberosEnvProperties.put("kdc_hosts", expectedHostName + ",secondary.kdc.org");
    kerberosEnvProperties.put("master_kdc", expectedHostName);
    coreSiteProperties.put("hadoop.proxyuser.yarn.hosts", expectedHostName);
    coreSiteProperties.put("hadoop.security.auth_to_local", "RULE:clustername");

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("TEZ_CLIENT");
    groupComponents.add("RESOURCEMANAGER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    // verify that these properties are filtered out of the exported configuration
    assertFalse("admin_server_host should not be present in exported blueprint in kerberos-env",
      kerberosEnvProperties.containsKey("admin_server_host"));
    assertFalse("kdc_hosts should not be present in exported blueprint in kerberos-env",
      kerberosEnvProperties.containsKey("kdc_hosts"));
    assertFalse("master_kdc should not be present in exported blueprint in kerberos-env",
      kerberosEnvProperties.containsKey("master_kdc"));
    assertEquals("hadoop.proxyuser.yarn.hosts was not exported correctly",
      createExportedHostName("host_group_1"), coreSiteProperties.get("hadoop.proxyuser.yarn.hosts"));
    assertFalse("hadoop.security.auth_to_local should not be present in exported blueprint in core-site",
      coreSiteProperties.containsKey("hadoop.security.auth_to_local"));
  }

  @Test
  public void testDoNameNodeHighAvailabilityExportWithHAEnabled() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();

    configProperties.put("hdfs-site", hdfsSiteProperties);
    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);

    // setup hdfs config for test
    hdfsSiteProperties.put("dfs.internal.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);

    // setup properties that include host information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, expectedHostName + ":" + expectedPortNum);

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("NAMENODE");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));
  }

  @Test
  public void testDoNameNodeHighAvailabilityExportWithHAEnabledPrimaryNamePreferenceNotExported() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();

    configProperties.put("hdfs-site", hdfsSiteProperties);
    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);
    configProperties.put("hadoop-env", hadoopEnvProperties);

    // setup hdfs config for test
    hdfsSiteProperties.put("dfs.internal.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);

    // setup properties that include host information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, expectedHostName + ":" + expectedPortNum);

    // setup primary & secondary name node preference
    hadoopEnvProperties.put("dfs_ha_initial_namenode_active", expectedHostName);
    hadoopEnvProperties.put("dfs_ha_initial_namenode_standby", expectedHostName);

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("NAMENODE");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));

    assertNull("Initial NameNode HA property exported although should not have", hadoopEnvProperties.get("dfs_ha_initial_namenode_active"));
    assertNull("Initial NameNode HA property exported although should not have", hadoopEnvProperties.get("dfs_ha_initial_namenode_standby"));

    Map<String, String> clusterEnv = clusterConfig.getProperties().get("cluster-env");
    assertTrue("Initial NameNode HA property exported although should not have", clusterEnv == null || clusterEnv.get("dfs_ha_initial_namenode_active") == null);
    assertTrue("Initial NameNode HA property exported although should not have", clusterEnv == null || clusterEnv.get("dfs_ha_initial_namenode_standby") == null);
  }

  @Test
  public void testDoNameNodeHighAvailabilityExportWithHAEnabledNameServicePropertiesIncluded() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);
    configProperties.put("accumulo-site", accumuloSiteProperties);

    // configure fs.defaultFS to include a nameservice name, rather than a host name
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);
    // configure hbase.rootdir to include a nameservice name, rather than a host name
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/apps/hbase/data");
    // configure instance.volumes to include a nameservice name, rather than a host name
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/apps/accumulo/data");

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("RESOURCEMANAGER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup("group1", groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    // verify that any properties that include nameservices are not removed from the exported blueprint's configuration
    assertEquals("Property containing an HA nameservice (fs.defaultFS), was not correctly exported by the processor",
      "hdfs://" + expectedNameService, coreSiteProperties.get("fs.defaultFS"));
    assertEquals("Property containing an HA nameservice (hbase.rootdir), was not correctly exported by the processor",
      "hdfs://" + expectedNameService + "/apps/hbase/data", hbaseSiteProperties.get("hbase.rootdir"));
    assertEquals("Property containing an HA nameservice (instance.volumes), was not correctly exported by the processor",
      "hdfs://" + expectedNameService + "/apps/accumulo/data", accumuloSiteProperties.get("instance.volumes"));
  }

  @Test
  public void testDoNameNodeHighAvailabilityExportWithHANotEnabled() throws Exception {
    // hdfs-site config for this test will not include an HA values
    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    configProperties.put("hdfs-site", hdfsSiteProperties);

    assertEquals("Incorrect initial state for hdfs-site config",
      0, hdfsSiteProperties.size());

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("NAMENODE");
    TestHostGroup group = new TestHostGroup("group1", groupComponents, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("Incorrect state for hdfs-site config after HA call in non-HA environment, should be zero",
      0, hdfsSiteProperties.size());
  }

  @Test
  public void testDoNameNodeHighAvailabilityExportWithHAEnabledMultipleServices() throws Exception {
    final String expectedNameServiceOne = "mynameserviceOne";
    final String expectedNameServiceTwo = "mynameserviceTwo";
    final String expectedHostNameOne = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";

    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    configProperties.put("hdfs-site", hdfsSiteProperties);

    // setup hdfs config for test
    hdfsSiteProperties.put("dfs.internal.nameservices", expectedNameServiceOne + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.nameservices", expectedNameServiceOne + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceOne, expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeOne + ", " + expectedNodeTwo);

    // setup properties that include host information for nameservice one
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceOne + "." + expectedNodeOne, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceOne + "." + expectedNodeTwo, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceOne + "." + expectedNodeOne, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceOne + "." + expectedNodeTwo, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceOne + "." + expectedNodeOne, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceOne + "." + expectedNodeTwo, expectedHostNameOne + ":" + expectedPortNum);

    // setup properties that include host information for nameservice two
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeOne, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeTwo, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeOne, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeTwo, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeOne, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeTwo, expectedHostNameTwo + ":" + expectedPortNum);

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("NAMENODE");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostNameOne);
    hosts.add(expectedHostNameTwo);
//    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    // verify results for name service one
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameServiceOne + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameServiceOne + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameServiceOne + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameServiceOne + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameServiceOne + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameServiceOne + "." + expectedNodeTwo));


    // verify results for name service two
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeTwo));
  }

  @Test
  public void testYarnConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> yarnSiteProperties = new HashMap<>();
    configProperties.put("yarn-site", yarnSiteProperties);

    // setup properties that include host information
    yarnSiteProperties.put("yarn.log.server.url", "http://" + expectedHostName + ":19888/jobhistory/logs");
    yarnSiteProperties.put("yarn.resourcemanager.hostname", expectedHostName);
    yarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.scheduler.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.admin.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.https.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.log.server.web-service.url", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.reader.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.reader.webapp.https.address", expectedHostName + ":" + expectedPortNum);

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("RESOURCEMANAGER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("Yarn Log Server URL was incorrectly exported",
      "http://" + "%HOSTGROUP::" + expectedHostGroupName + "%" + ":19888/jobhistory/logs", yarnSiteProperties.get("yarn.log.server.url"));
    assertEquals("Yarn ResourceManager hostname was incorrectly exported",
      createExportedHostName(expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.hostname"));
    assertEquals("Yarn ResourceManager tracker address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.resource-tracker.address"));
    assertEquals("Yarn ResourceManager webapp address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.webapp.address"));
    assertEquals("Yarn ResourceManager scheduler address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.scheduler.address"));
    assertEquals("Yarn ResourceManager address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.address"));
    assertEquals("Yarn ResourceManager admin address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.admin.address"));
    assertEquals("Yarn ResourceManager timeline-service address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.timeline-service.address"));
    assertEquals("Yarn ResourceManager timeline webapp address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.timeline-service.webapp.address"));
    assertEquals("Yarn ResourceManager timeline webapp HTTPS address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.timeline-service.webapp.https.address"));
    assertEquals("Yarn ResourceManager timeline web service url was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.log.server.web-service.url"));
    assertEquals("Yarn ResourceManager timeline reader webapp address was incorrectly exported",
        createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.timeline-service.reader.webapp.address"));
    assertEquals("Yarn ResourceManager timeline reader webapp HTTPS address was incorrectly exported",
        createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.timeline-service.reader.webapp.https.address"));
  }

  @Test
  public void testYarnConfigExportedWithDefaultZeroHostAddress() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> yarnSiteProperties = new HashMap<>();
    configProperties.put("yarn-site", yarnSiteProperties);

    // setup properties that include host information
    yarnSiteProperties.put("yarn.log.server.url", "http://" + expectedHostName + ":19888/jobhistory/logs");
    yarnSiteProperties.put("yarn.resourcemanager.hostname", expectedHostName);
    yarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.scheduler.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.admin.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.address", "0.0.0.0" + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.address", "0.0.0.0" + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.https.address", "0.0.0.0" + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.reader.webapp.address", "0.0.0.0" + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.reader.webapp.https.address", "0.0.0.0" + ":" + expectedPortNum);

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("RESOURCEMANAGER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("Yarn Log Server URL was incorrectly exported",
      "http://" + "%HOSTGROUP::" + expectedHostGroupName + "%" + ":19888/jobhistory/logs", yarnSiteProperties.get("yarn.log.server.url"));
    assertEquals("Yarn ResourceManager hostname was incorrectly exported",
      createExportedHostName(expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.hostname"));
    assertEquals("Yarn ResourceManager tracker address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.resource-tracker.address"));
    assertEquals("Yarn ResourceManager webapp address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.webapp.address"));
    assertEquals("Yarn ResourceManager scheduler address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.scheduler.address"));
    assertEquals("Yarn ResourceManager address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.address"));
    assertEquals("Yarn ResourceManager admin address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.admin.address"));
    assertEquals("Yarn ResourceManager timeline-service address was incorrectly exported",
      "0.0.0.0" + ":" + expectedPortNum, yarnSiteProperties.get("yarn.timeline-service.address"));
    assertEquals("Yarn ResourceManager timeline webapp address was incorrectly exported",
      "0.0.0.0" + ":" + expectedPortNum, yarnSiteProperties.get("yarn.timeline-service.webapp.address"));
    assertEquals("Yarn ResourceManager timeline webapp HTTPS address was incorrectly exported",
      "0.0.0.0" + ":" + expectedPortNum, yarnSiteProperties.get("yarn.timeline-service.webapp.https.address"));
    assertEquals("Yarn ResourceManager timeline reader webapp address was incorrectly exported",
      "0.0.0.0" + ":" + expectedPortNum, yarnSiteProperties.get("yarn.timeline-service.reader.webapp.address"));
    assertEquals("Yarn ResourceManager timeline reader webapp HTTPS address was incorrectly exported",
      "0.0.0.0" + ":" + expectedPortNum, yarnSiteProperties.get("yarn.timeline-service.reader.webapp.https.address"));
  }

  @Test
  public void testHDFSConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    configProperties.put("hdfs-site", hdfsSiteProperties);
    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);
    configProperties.put("accumulo-site", accumuloSiteProperties);

    // setup properties that include host information
    hdfsSiteProperties.put("dfs.http.address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.https.address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.https-address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.secondary.http.address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir", expectedHostName + ":" + expectedPortNum);

    coreSiteProperties.put("fs.default.name", expectedHostName + ":" + expectedPortNum);
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedHostName + ":" + expectedPortNum);

    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedHostName + ":" + expectedPortNum + "/apps/hbase/data");

    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedHostName + ":" + expectedPortNum + "/apps/accumulo/data");

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("NAMENODE");
    groupComponents.add("SECONDARY_NAMENODE");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.http.address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.https.address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.secondary.http.address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.secondary.http-address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.shared.edits.dir"));

    assertEquals("hdfs config in core-site not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), coreSiteProperties.get("fs.default.name"));
    assertEquals("hdfs config in core-site not exported properly",
      "hdfs://" + createExportedAddress(expectedPortNum, expectedHostGroupName), coreSiteProperties.get("fs.defaultFS"));

    assertEquals("hdfs config in hbase-site not exported properly",
      "hdfs://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + "/apps/hbase/data", hbaseSiteProperties.get("hbase.rootdir"));

    assertEquals("hdfs config in accumulo-site not exported properly",
      "hdfs://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + "/apps/accumulo/data", accumuloSiteProperties.get("instance.volumes"));
  }

  @Test
  public void testHiveConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> hiveSiteProperties = new HashMap<>();
    Map<String, String> hiveEnvProperties = new HashMap<>();
    Map<String, String> webHCatSiteProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();

    configProperties.put("hive-site", hiveSiteProperties);
    configProperties.put("hive-env", hiveEnvProperties);
    configProperties.put("webhcat-site", webHCatSiteProperties);
    configProperties.put("core-site", coreSiteProperties);

    // setup properties that include host information
    hiveSiteProperties.put("hive.metastore.uris", "thrift://" + expectedHostName + ":" + expectedPortNum);
    hiveSiteProperties.put("javax.jdo.option.ConnectionURL", expectedHostName + ":" + expectedPortNum);
    hiveSiteProperties.put("hive.zookeeper.quorum", expectedHostName + ":" + expectedPortNum + "," + expectedHostNameTwo + ":" + expectedPortNum);
    hiveSiteProperties.put("hive.cluster.delegation.token.store.zookeeper.connectString", expectedHostName + ":" + expectedPortNum + "," + expectedHostNameTwo + ":" + expectedPortNum);

    webHCatSiteProperties.put("templeton.hive.properties", expectedHostName + "," + expectedHostNameTwo);
    webHCatSiteProperties.put("templeton.kerberos.principal", expectedHostName);

    coreSiteProperties.put("hadoop.proxyuser.hive.hosts", expectedHostName + "," + expectedHostNameTwo);
    coreSiteProperties.put("hadoop.proxyuser.HTTP.hosts", expectedHostName + "," + expectedHostNameTwo);
    coreSiteProperties.put("hadoop.proxyuser.hcat.hosts", expectedHostName + "," + expectedHostNameTwo);

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("HIVE_SERVER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<String> groupComponents2 = new HashSet<>();
    groupComponents2.add("HIVE_CLIENT");
    Collection<String> hosts2 = new ArrayList<>();
    hosts2.add(expectedHostNameTwo);
    hosts2.add("serverFour");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, groupComponents2, hosts2);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("hive property not properly exported",
      "thrift://" + createExportedAddress(expectedPortNum, expectedHostGroupName), hiveSiteProperties.get("hive.metastore.uris"));
    assertEquals("hive property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hiveSiteProperties.get("javax.jdo.option.ConnectionURL"));
    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      webHCatSiteProperties.get("templeton.hive.properties"));

    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.hive.hosts"));

    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.HTTP.hosts"));

    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.hcat.hosts"));

    assertEquals("hive zookeeper quorum property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName) + "," + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo),
      hiveSiteProperties.get("hive.zookeeper.quorum"));

    assertEquals("hive zookeeper connectString property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName) + "," + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo),
      hiveSiteProperties.get("hive.cluster.delegation.token.store.zookeeper.connectString"));

  }

  @Test
  public void testHiveConfigExportedMultipleHiveMetaStoreServers() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    Map<String, Map<String, String>> configProperties =
      new HashMap<>();

    Map<String, String> hiveSiteProperties =
      new HashMap<>();
    Map<String, String> hiveEnvProperties =
      new HashMap<>();
    Map<String, String> webHCatSiteProperties =
      new HashMap<>();
    Map<String, String> coreSiteProperties =
      new HashMap<>();

    configProperties.put("hive-site", hiveSiteProperties);
    configProperties.put("hive-env", hiveEnvProperties);
    configProperties.put("webhcat-site", webHCatSiteProperties);
    configProperties.put("core-site", coreSiteProperties);

    // setup properties that include host information
    hiveSiteProperties.put("hive.metastore.uris", "thrift://" + expectedHostName + ":" + expectedPortNum + "," + "thrift://" + expectedHostNameTwo + ":" + expectedPortNum);
    hiveSiteProperties.put("hive.server2.authentication.ldap.url", "ldap://myexternalhost.com:1389");
    hiveSiteProperties.put("javax.jdo.option.ConnectionURL", expectedHostName + ":" + expectedPortNum);
    hiveSiteProperties.put("hive.zookeeper.quorum", expectedHostName + ":" + expectedPortNum + "," + expectedHostNameTwo + ":" + expectedPortNum);
    hiveSiteProperties.put("hive.cluster.delegation.token.store.zookeeper.connectString", expectedHostName + ":" + expectedPortNum + "," + expectedHostNameTwo + ":" + expectedPortNum);

    webHCatSiteProperties.put("templeton.hive.properties", expectedHostName + "," + expectedHostNameTwo);
    webHCatSiteProperties.put("templeton.kerberos.principal", expectedHostName);

    coreSiteProperties.put("hadoop.proxyuser.hive.hosts", expectedHostName + "," + expectedHostNameTwo);
    coreSiteProperties.put("hadoop.proxyuser.HTTP.hosts", expectedHostName + "," + expectedHostNameTwo);
    coreSiteProperties.put("hadoop.proxyuser.hcat.hosts", expectedHostName + "," + expectedHostNameTwo);

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("NAMENODE");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<String> groupComponents2 = new HashSet<>();
    groupComponents2.add("DATANODE");
    Collection<String> hosts2 = new ArrayList<>();
    hosts2.add(expectedHostNameTwo);
    hosts2.add("serverThree");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, groupComponents2, hosts2);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    System.out.println("RWN: exported value of hive.metastore.uris = " + hiveSiteProperties.get("hive.metastore.uris"));

    assertEquals("hive property not properly exported",
      "thrift://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + "," + "thrift://" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo), hiveSiteProperties.get("hive.metastore.uris"));
    assertEquals("hive property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hiveSiteProperties.get("javax.jdo.option.ConnectionURL"));
    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      webHCatSiteProperties.get("templeton.hive.properties"));

    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.hive.hosts"));

    assertFalse("hive.server2.authentication.ldap.url should not have been present in the exported configuration",
      hiveSiteProperties.containsKey("hive.server2.authentication.ldap.url"));
    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.HTTP.hosts"));

    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.hcat.hosts"));

    assertEquals("hive zookeeper quorum property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName) + "," + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo),
      hiveSiteProperties.get("hive.zookeeper.quorum"));

    assertEquals("hive zookeeper connectString property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName) + "," + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo),
      hiveSiteProperties.get("hive.cluster.delegation.token.store.zookeeper.connectString"));
  }

  @Test
  public void testOozieConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedExternalHost = "c6408.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> oozieSiteProperties = new HashMap<>();
    Map<String, String> oozieEnvProperties = new HashMap<>();
    Map<String, String> hiveEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();

    configProperties.put("oozie-site", oozieSiteProperties);
    configProperties.put("oozie-env", oozieEnvProperties);
    configProperties.put("hive-env", hiveEnvProperties);
    configProperties.put("core-site", coreSiteProperties);

    oozieSiteProperties.put("oozie.base.url", expectedHostName);
    oozieSiteProperties.put("oozie.authentication.kerberos.principal", expectedHostName);
    oozieSiteProperties.put("oozie.service.HadoopAccessorService.kerberos.principal", expectedHostName);
    oozieSiteProperties.put("oozie.service.JPAService.jdbc.url", "jdbc:mysql://" + expectedExternalHost + "/ooziedb");

    oozieEnvProperties.put("oozie_existing_mysql_host", expectedExternalHost);
    hiveEnvProperties.put("hive_existing_oracle_host", expectedExternalHost);
    oozieEnvProperties.put("oozie_heapsize", "1024m");
    oozieEnvProperties.put("oozie_permsize", "2048m");

    coreSiteProperties.put("hadoop.proxyuser.oozie.hosts", expectedHostName + "," + expectedHostNameTwo);

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // note: test hostgroups may not accurately reflect the required components for the config properties
    // which are mapped to them.  Only the hostgroup name is used for hostgroup resolution an the components
    // are not validated
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("OOZIE_SERVER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<String> groupComponents2 = new HashSet<>();
    groupComponents2.add("OOZIE_SERVER");
    Collection<String> hosts2 = new ArrayList<>();
    hosts2.add(expectedHostNameTwo);
    hosts2.add("serverFour");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, groupComponents2, hosts2);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);
    hostGroups.add(group2);

    if (BlueprintConfigurationProcessor.singleHostTopologyUpdaters != null &&
      BlueprintConfigurationProcessor.singleHostTopologyUpdaters.containsKey("oozie-site")) {
      BlueprintConfigurationProcessor.singleHostTopologyUpdaters.get("oozie-site").remove("oozie.service.JPAService.jdbc.url");
    }

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    // check that jdbc url and related properties are removed if oozie external db is on host which not included to cluster
    assertFalse(BlueprintConfigurationProcessor.singleHostTopologyUpdaters.get("oozie-site").containsKey("oozie.service.JPAService.jdbc.url"));
    assertTrue(configProcessor.getRemovePropertyUpdaters().get("oozie-site").containsKey("oozie.service.JPAService.jdbc.url"));

    assertEquals("oozie property not exported correctly",
      createExportedHostName(expectedHostGroupName), oozieSiteProperties.get("oozie.base.url"));
    assertEquals("oozie property not exported correctly",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.oozie.hosts"));

    // verify that the oozie properties that can refer to an external DB are not included in the export
    assertFalse("oozie_existing_mysql_host should not have been present in the exported configuration",
      oozieEnvProperties.containsKey("oozie_existing_mysql_host"));
    assertFalse("hive_existing_oracle_host should not have been present in the exported configuration",
      hiveEnvProperties.containsKey("hive_existing_oracle_host"));
    assertFalse("oozie.service.JPAService.jdbc.url should not have been present in the exported configuration",
      oozieSiteProperties.containsKey("oozie.service.JPAService.jdbc.url"));

    // verify that oozie-env heapsize properties are not removed from the configuration
    assertEquals("oozie_heapsize should have been included in exported configuration",
      "1024m", oozieEnvProperties.get("oozie_heapsize"));
    assertEquals("oozie_permsize should have been included in exported configuration",
      "2048m", oozieEnvProperties.get("oozie_permsize"));

  }

  @Test
  public void testOozieJDBCPropertiesNotRemoved() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedPortNum = "80000";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> oozieSiteProperties = new HashMap<>();

    configProperties.put("oozie-site", oozieSiteProperties);

    oozieSiteProperties.put("oozie.service.JPAService.jdbc.url", "jdbc:mysql://" + expectedHostNameTwo + "/ooziedb");

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("OOZIE_SERVER");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("OOZIE_SERVER");
    hgComponents2.add("ZOOKEEPER_SERVER");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("OOZIE_SERVER")).andReturn(new Cardinality("1+")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor blueprintConfigurationProcessor = new BlueprintConfigurationProcessor(topology);

    assertTrue(BlueprintConfigurationProcessor.singleHostTopologyUpdaters.get("oozie-site").containsKey("oozie.service.JPAService.jdbc.url"));
    assertNull(blueprintConfigurationProcessor.getRemovePropertyUpdaters().get("oozie-site"));
  }

  @Test
  public void testOozieJDBCPropertyAddedToSingleHostMapDuringImport() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedPortNum = "80000";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> oozieSiteProperties = new HashMap<>();

    configProperties.put("oozie-site", oozieSiteProperties);

    oozieSiteProperties.put("oozie.service.JPAService.jdbc.url", "jdbc:mysql://" + "%HOSTGROUP::group1%" + "/ooziedb");

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("OOZIE_SERVER");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("OOZIE_SERVER");
    hgComponents2.add("ZOOKEEPER_SERVER");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("OOZIE_SERVER")).andReturn(new Cardinality("1+")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor blueprintConfigurationProcessor = new BlueprintConfigurationProcessor(topology);

    assertTrue(BlueprintConfigurationProcessor.singleHostTopologyUpdaters.get("oozie-site").containsKey("oozie.service.JPAService.jdbc.url"));
    assertNull(blueprintConfigurationProcessor.getRemovePropertyUpdaters().get("oozie-site"));
  }

  @Test
  public void testZookeeperConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedPortNumberOne = "2112";
    final String expectedPortNumberTwo = "1221";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> webHCatSiteProperties = new HashMap<>();
    Map<String, String> sliderClientProperties = new HashMap<>();
    Map<String, String> yarnSiteProperties = new HashMap<>();
    Map<String, String> kafkaBrokerProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);
    configProperties.put("webhcat-site", webHCatSiteProperties);
    configProperties.put("slider-client", sliderClientProperties);
    configProperties.put("yarn-site", yarnSiteProperties);
    configProperties.put("kafka-broker", kafkaBrokerProperties);
    configProperties.put("accumulo-site", accumuloSiteProperties);

    coreSiteProperties.put("ha.zookeeper.quorum", expectedHostName + "," + expectedHostNameTwo);
    hbaseSiteProperties.put("hbase.zookeeper.quorum", expectedHostName + "," + expectedHostNameTwo);
    webHCatSiteProperties.put("templeton.zookeeper.hosts", expectedHostName + "," + expectedHostNameTwo);
    yarnSiteProperties.put("hadoop.registry.zk.quorum", createHostAddress(expectedHostName, expectedPortNumberOne) + "," + createHostAddress(expectedHostNameTwo, expectedPortNumberTwo));
    sliderClientProperties.put("slider.zookeeper.quorum", createHostAddress(expectedHostName, expectedPortNumberOne) + "," + createHostAddress(expectedHostNameTwo, expectedPortNumberTwo));
    kafkaBrokerProperties.put("zookeeper.connect", createHostAddress(expectedHostName, expectedPortNumberOne) + "," + createHostAddress(expectedHostNameTwo, expectedPortNumberTwo));
    accumuloSiteProperties.put("instance.zookeeper.host", createHostAddress(expectedHostName, expectedPortNumberOne) + "," + createHostAddress(expectedHostNameTwo, expectedPortNumberTwo));

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    // test hostgroups may not accurately reflect the required components for the config properties which are mapped to them
    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("ZOOKEEPER_SERVER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<String> groupComponents2 = new HashSet<>();
    groupComponents2.add("ZOOKEEPER_SERVER");
    Collection<String> hosts2 = new ArrayList<>();
    hosts2.add(expectedHostNameTwo);
    hosts2.add("serverFour");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, groupComponents2, hosts2);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      coreSiteProperties.get("ha.zookeeper.quorum"));
    assertEquals("zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      hbaseSiteProperties.get("hbase.zookeeper.quorum"));
    assertEquals("zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      webHCatSiteProperties.get("templeton.zookeeper.hosts"));
    assertEquals("yarn-site zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName, expectedPortNumberOne) + "," + createExportedHostName(expectedHostGroupNameTwo, expectedPortNumberTwo),
      yarnSiteProperties.get("hadoop.registry.zk.quorum"));
    assertEquals("kafka zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName, expectedPortNumberOne) + "," + createExportedHostName(expectedHostGroupNameTwo, expectedPortNumberTwo),
      kafkaBrokerProperties.get("zookeeper.connect"));
    assertEquals("accumulo-site zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName, expectedPortNumberOne) + "," + createExportedHostName(expectedHostGroupNameTwo, expectedPortNumberTwo),
      accumuloSiteProperties.get("instance.zookeeper.host"));
  }

  @Test
  public void testKnoxSecurityConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    Map<String, Map<String, String>> configProperties =
      new HashMap<>();

    Map<String, String> coreSiteProperties =
      new HashMap<>();
    Map<String, String> webHCatSiteProperties =
      new HashMap<>();
    Map<String, String> oozieSiteProperties =
      new HashMap<>();

    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("webhcat-site", webHCatSiteProperties);
    configProperties.put("oozie-site", oozieSiteProperties);

    coreSiteProperties.put("hadoop.proxyuser.knox.hosts", expectedHostName + "," + expectedHostNameTwo);
    webHCatSiteProperties.put("webhcat.proxyuser.knox.hosts", expectedHostName + "," + expectedHostNameTwo);
    oozieSiteProperties.put("hadoop.proxyuser.knox.hosts", expectedHostName + "," + expectedHostNameTwo);
    oozieSiteProperties.put("oozie.service.ProxyUserService.proxyuser.knox.hosts", expectedHostName + "," + expectedHostNameTwo);

//    multiCoreSiteMap.put("hadoop.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));
//    multiWebhcatSiteMap.put("webhcat.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));
//    multiOozieSiteMap.put("hadoop.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("KNOX_GATEWAY");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<String> groupComponents2 = new HashSet<>();
    groupComponents2.add("KNOX_GATEWAY");
    Collection<String> hosts2 = new ArrayList<>();
    hosts2.add(expectedHostNameTwo);
    hosts2.add("serverFour");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, groupComponents2, hosts2);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("Knox for core-site config not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      coreSiteProperties.get("hadoop.proxyuser.knox.hosts"));
    assertEquals("Knox config for WebHCat not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      webHCatSiteProperties.get("webhcat.proxyuser.knox.hosts"));
    assertEquals("Knox config for Oozie not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      oozieSiteProperties.get("hadoop.proxyuser.knox.hosts"));
    assertEquals("Knox config for Oozie not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      oozieSiteProperties.get("oozie.service.ProxyUserService.proxyuser.knox.hosts"));
  }

  @Test
  public void testKafkaConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedPortNumberOne = "2112";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> kafkaBrokerProperties = new HashMap<>();
    configProperties.put("kafka-broker", kafkaBrokerProperties);
    kafkaBrokerProperties.put("kafka.ganglia.metrics.host", createHostAddress(expectedHostName, expectedPortNumberOne));

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("KAFKA_BROKER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<String> groupComponents2 = new HashSet<>();
    groupComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup("group2", groupComponents2, Collections.singleton("group2Host"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("kafka Ganglia config not properly exported",
      createExportedHostName(expectedHostGroupName, expectedPortNumberOne),
      kafkaBrokerProperties.get("kafka.ganglia.metrics.host"));
  }

  @Test
  public void testPropertyWithUndefinedHostisExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();

    Map<String, String> properties = new HashMap<>();
    configProperties.put("storm-site", properties);

    // setup properties that include host information including undefined host properties
    properties.put("storm.zookeeper.servers", expectedHostName);
    properties.put("nimbus.childopts", "undefined");
    properties.put("worker.childopts", "some other info, undefined, more info");

    Configuration clusterConfig = new Configuration(configProperties,
      emptyMap());

    Collection<String> groupComponents = new HashSet<>();
    groupComponents.add("ZOOKEEPER_SERVER");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("serverTwo");
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, groupComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("Property was incorrectly exported",
      "%HOSTGROUP::" + expectedHostGroupName + "%", properties.get("storm.zookeeper.servers"));
    assertEquals("Property with undefined host was incorrectly exported",
      "undefined", properties.get("nimbus.childopts"));
    assertEquals("Property with undefined host was incorrectly exported",
      "some other info, undefined, more info", properties.get("worker.childopts"));
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__defaultValue() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    Map<String, String> typeProps2 = new HashMap<>();
    typeProps.put("yarn.resourcemanager.hostname", "localhost");
    typeProps2.put("oozie_heapsize", "1024");
    typeProps2.put("oozie_permsize", "128");
    properties.put("oozie-env", typeProps2);
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> group1Components = new HashSet<>();
    group1Components.add("NAMENODE");
    group1Components.add("SECONDARY_NAMENODE");
    group1Components.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", group1Components, Collections.singleton("testhost"));

    Collection<String> group2Components = new HashSet<>();
    group2Components.add("DATANODE");
    group2Components.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", group2Components, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> configTypesUpdated =
      updater.doUpdateForClusterCreate();

    String updatedVal = properties.get("yarn-site").get("yarn.resourcemanager.hostname");
    assertEquals("testhost", updatedVal);
    String updatedVal1 = properties.get("oozie-env").get("oozie_heapsize");
    assertEquals("1024m", updatedVal1);
    String updatedVal2 = properties.get("oozie-env").get("oozie_permsize");
    assertEquals("128m", updatedVal2);

    assertEquals("Incorrect number of config types updated",
      3, configTypesUpdated.size());
    assertTrue("Expected config type not updated",
      configTypesUpdated.contains("oozie-env"));
    assertTrue("Expected config type not updated",
      configTypesUpdated.contains("yarn-site"));
    assertTrue("Expected config type not updated",
      configTypesUpdated.contains("cluster-env"));
  }

  @Test
  public void testDoUpdateForClusterCreate_HostgroupReplacement_DefaultUpdater() throws Exception {
    Map<String, Map<String, String>> stackProperties = new HashMap<>(ImmutableMap.of(
      "hdfs-site", new HashMap<>(ImmutableMap.of(
        "dfs.http.address", "testhost2")),
      "myservice-env", new HashMap<>(ImmutableMap.of(
          "myservice_master_address", "%HOSTGROUP::group1%:8080",
          "myservice_some_other_property", "some_value"))));

    Map<String, Map<String, String>> hostGroupProperties = new HashMap<>(ImmutableMap.of(
      "hdfs-site", new HashMap<>(ImmutableMap.of(
        "dfs.https.address", "testhost3")),
      "myservice-site", new HashMap<>(ImmutableMap.of(
        "myservice_slave_address", "%HOSTGROUP::group1%:8080"))));
    hostGroupProperties.get("hdfs-site").put("null_property", null);

    Configuration clusterConfig = new Configuration(new HashMap<>(), new HashMap<>());
    clusterConfig.setParentConfiguration(new Configuration(stackProperties, emptyMap()));
    Configuration hostGroupConfig = new Configuration(hostGroupProperties, emptyMap());

    TestHostGroup group1 = new TestHostGroup("group1",
      Sets.newHashSet("NAMENODE", "SECONDARY_NAMENODE", "RESOURCEMANAGER"),
      Collections.singleton("testhost"),
      hostGroupConfig);

    Collection<TestHostGroup> hostGroups = Sets.newHashSet(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> configTypesUpdated =
      updater.doUpdateForClusterCreate();

    assertTrue(configTypesUpdated.containsAll(ImmutableList.of("myservice-env", "myservice-site")));
    // updater called on dfs.http.address and dfs.http.address but didn't change their values
    assertTrue(!configTypesUpdated.contains("hdfs-site"));
    Map<String, String> clusterHdfsSite = clusterConfig.getProperties().get("hdfs-site");
    // the value of dfs.http.address didn't change, yet it was added to the cluster config as this property has a
    // registered updater
    assertTrue(clusterHdfsSite.containsKey("dfs.http.address"));
    Map<String, String> myserviceEnv = clusterConfig.getProperties().get("myservice-env");
    // default updater (to replace %HOSTGROUP::name%) was called on myservice_master_address and myservice_some_other_property
    // - myservice_master_address was added to the cluster config as its value was updated by the default updater
    // - the value of myservice_some_other_property didn't change and this property does not have a registered updater,
    //   so it wasn't added to the cluster config
    assertEquals(ImmutableMap.of("myservice_master_address", "testhost:8080"), myserviceEnv);

    // %HOSTGROUP::name% has been replaced by the default updater in hostgroup config
    Map<String, String> myserviceSite = hostGroupConfig.getProperties().get("myservice-site");
    assertEquals(myserviceSite, ImmutableMap.of("myservice_slave_address", "testhost:8080"));
  }

  @Test
  public void testHostgroupUpdater() throws Exception {
    Set<String> components = ImmutableSet.of("NAMENODE", "SECONDARY_NAMENODE", "RESOURCEMANAGER");
    Configuration hostGroupConfig = new Configuration(emptyMap(), emptyMap());
    TestHostGroup group1 = new TestHostGroup("master1", components, ImmutableList.of("master1_host"), hostGroupConfig);
    TestHostGroup group2 = new TestHostGroup("master2", components, ImmutableList.of("master2_host"), hostGroupConfig);
    TestHostGroup group3 =
      new TestHostGroup("master3", components, ImmutableList.of("master3_host1", "master3_host2"), hostGroupConfig);
    Configuration clusterConfig = new Configuration(new HashMap<>(), new HashMap<>());
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, ImmutableList.of(group1, group2, group3));

    BlueprintConfigurationProcessor.HostGroupUpdater updater = BlueprintConfigurationProcessor.HostGroupUpdater.INSTANCE;

    assertEquals(
      "master1_host:8080",
      updater.updateForClusterCreate(
        "mycomponent.url",
        "%HOSTGROUP::master1%:8080",
        clusterConfig.getProperties(),
        topology));

    assertEquals(
      "master1_host:8080,master2_host:8080",
      updater.updateForClusterCreate(
        "mycomponent.urls",
        "%HOSTGROUP::master1%:8080,%HOSTGROUP::master2%:8080",
        clusterConfig.getProperties(),
        topology));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHostgroupUpdater_NonExistingHostgroup() throws Exception {
    Set<String> components = ImmutableSet.of("NAMENODE", "SECONDARY_NAMENODE", "RESOURCEMANAGER");
    Configuration hostGroupConfig = new Configuration(emptyMap(), emptyMap());
    TestHostGroup group1 = new TestHostGroup("master1", components, ImmutableList.of("master1_host"), hostGroupConfig);
    Configuration clusterConfig = new Configuration(new HashMap<>(), new HashMap<>());
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, ImmutableList.of(group1));
    BlueprintConfigurationProcessor.HostGroupUpdater updater = BlueprintConfigurationProcessor.HostGroupUpdater.INSTANCE;

    updater.updateForClusterCreate(
      "mycomponent.urls",
      "%HOSTGROUP::master2%:8080",
      clusterConfig.getProperties(),
      topology);
  }

  @Test(expected = IllegalStateException.class)
  public void testHostgroupUpdater_SameGroupMultipleTimes() throws Exception {
    Set<String> components = ImmutableSet.of("NAMENODE", "SECONDARY_NAMENODE", "RESOURCEMANAGER");
    Configuration hostGroupConfig = new Configuration(emptyMap(), emptyMap());
    TestHostGroup group1 = new TestHostGroup("master1", components, ImmutableList.of("master1_host1", "master1_host2"), hostGroupConfig);
    Configuration clusterConfig = new Configuration(new HashMap<>(), new HashMap<>());
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, ImmutableList.of(group1));
    BlueprintConfigurationProcessor.HostGroupUpdater updater = BlueprintConfigurationProcessor.HostGroupUpdater.INSTANCE;

    updater.updateForClusterCreate(
      "mycomponent.urls",
      "%HOSTGROUP::master1%:8080,%HOSTGROUP::master1%:8090",
      clusterConfig.getProperties(),
      topology);
  }

  @Test
  public void testHostgroupUpdater_getRequiredHostgroups() throws Exception {
    Set<String> components = ImmutableSet.of("NAMENODE", "SECONDARY_NAMENODE", "RESOURCEMANAGER");
    Configuration hostGroupConfig = new Configuration(emptyMap(), emptyMap());
    TestHostGroup group1 = new TestHostGroup("master1", components, ImmutableList.of("master1_host"), hostGroupConfig);
    TestHostGroup group2 = new TestHostGroup("master2", components, ImmutableList.of("master2_host"), hostGroupConfig);
    TestHostGroup group3 =
      new TestHostGroup("master3", components, ImmutableList.of("master3_host"), hostGroupConfig);
    Configuration clusterConfig = new Configuration(new HashMap<>(), new HashMap<>());
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, ImmutableList.of(group1, group2, group3));

    BlueprintConfigurationProcessor.HostGroupUpdater updater = BlueprintConfigurationProcessor.HostGroupUpdater.INSTANCE;

    assertEquals(
      ImmutableSet.of("master1"),
      ImmutableSet.copyOf(
        updater.getRequiredHostGroups(
          "mycomponent.url",
          "%HOSTGROUP::master1%:8080",
          clusterConfig.getProperties(),
          topology)));

    assertEquals(
      ImmutableSet.of("master1", "master2"),
      ImmutableSet.copyOf(
        updater.getRequiredHostGroups(
          "mycomponent.urls",
          "%HOSTGROUP::master1%:8080,%HOSTGROUP::master2%:8080",
          clusterConfig.getProperties(),
          topology)));

    assertEquals(
      ImmutableSet.of("master3"),
      ImmutableSet.copyOf(
        updater.getRequiredHostGroups(
          "mycomponent.urls",
          "%HOSTGROUP::master3%:8080",
          clusterConfig.getProperties(),
          topology)));

    assertEquals(emptyList(),
      updater.getRequiredHostGroups("mycomponent.urls", null, clusterConfig.getProperties(), topology));
  }

  @Test
  public void testDoUpdateForClusterCreate_OnlyOneUpdaterForEachProperty() throws Exception {
    BlueprintConfigurationProcessor bpConfigProcessor = new BlueprintConfigurationProcessor(
      createClusterTopology(bp, new Configuration(emptyMap(), emptyMap()), emptyList()));
    Collection<Map<String, Map<String, BlueprintConfigurationProcessor.PropertyUpdater>>> updaters =
      bpConfigProcessor.createCollectionOfUpdaters();

    // Make a list of all (configType, propertyName) pairs that have updaters
    List<Pair<String, String>> propertiesWithUpdaters = updaters.stream().
      flatMap(map -> map.entrySet().stream()).
      flatMap(entry -> {
        String configType = entry.getKey();
        return entry.getValue().keySet().stream().map(propertyName -> Pair.of(configType, propertyName));
      }).
      sorted().
      collect(toList());

    // Look for duplicates in the list
    Set<Pair<String, String>> duplicates = new HashSet<>();
    for (PeekingIterator<Pair<String, String>> it = peekingIterator(propertiesWithUpdaters.iterator()); it.hasNext(); ) {
      Pair<String, String> property = it.next();
      if (it.hasNext() && it.peek().equals(property)) {
        duplicates.add(property);
      }
    }
    assertTrue("There are properties with multiple updaters: " + duplicates, duplicates.isEmpty());
  }


  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__defaultValue_providedInParent() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> yarnSiteProps = new HashMap<>();
    yarnSiteProps.put("yarn.resourcemanager.hostname", "localhost");
    properties.put("yarn-site", yarnSiteProps);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Map<String, String> parentYarnSiteProps = new HashMap<>();
    parentYarnSiteProps.put("yarn.resourcemanager.resource-tracker.address", "localhost");
    parentProperties.put("yarn-site", parentYarnSiteProps);

    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());

    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> group1Components = new HashSet<>();
    group1Components.add("NAMENODE");
    group1Components.add("SECONDARY_NAMENODE");
    group1Components.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", group1Components, Collections.singleton("testhost"));

    Collection<String> group2Components = new HashSet<>();
    group2Components.add("DATANODE");
    group2Components.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", group2Components, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    assertEquals("testhost", clusterConfig.getPropertyValue("yarn-site", "yarn.resourcemanager.hostname"));
    assertEquals("testhost", clusterConfig.getPropertyValue("yarn-site", "yarn.resourcemanager.resource-tracker.address"));
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__defaultValue_hostGroupConfig() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> yarnSiteProps = new HashMap<>();
    yarnSiteProps.put("yarn.resourcemanager.hostname", "localhost");
    properties.put("yarn-site", yarnSiteProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> group1Components = new HashSet<>();
    group1Components.add("NAMENODE");
    group1Components.add("SECONDARY_NAMENODE");
    group1Components.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", group1Components, Collections.singleton("testhost"));

    Collection<String> group2Components = new HashSet<>();
    group2Components.add("DATANODE");
    group2Components.add("HDFS_CLIENT");

    Map<String, Map<String, String>> group2Properties = new HashMap<>();
    Map<String, String> group2YarnSiteProperties = new HashMap<>();
    group2YarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address", "localhost");
    group2Properties.put("yarn-site", group2YarnSiteProperties);
    // group 2 host group configuration
    // HG config -> BP HG config -> cluster scoped config
    Configuration group2BPConfig = new Configuration(emptyMap(),
      emptyMap(), clusterConfig);

    Configuration group2Config = new Configuration(group2Properties,
      emptyMap(), group2BPConfig);
    // set config on HG
    TestHostGroup group2 = new TestHostGroup("group2", group2Components, Collections.singleton("testhost2"), group2Config);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    assertEquals("testhost", clusterConfig.getPropertyValue("yarn-site", "yarn.resourcemanager.hostname"));
    assertEquals("testhost", group2Config.getProperties().get("yarn-site").get("yarn.resourcemanager.resource-tracker.address"));
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__defaultValue_BPHostGroupConfig() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> yarnSiteProps = new HashMap<>();
    yarnSiteProps.put("yarn.resourcemanager.hostname", "localhost");
    properties.put("yarn-site", yarnSiteProps);

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> group1Components = new HashSet<>();
    group1Components.add("NAMENODE");
    group1Components.add("SECONDARY_NAMENODE");
    group1Components.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", group1Components, Collections.singleton("testhost"));

    Collection<String> group2Components = new HashSet<>();
    group2Components.add("DATANODE");
    group2Components.add("HDFS_CLIENT");

    Map<String, Map<String, String>> group2BPProperties = new HashMap<>();
    Map<String, String> group2YarnSiteProperties = new HashMap<>();
    group2YarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address", "localhost");
    group2BPProperties.put("yarn-site", group2YarnSiteProperties);
    // group 2 host group configuration
    // HG config -> BP HG config -> cluster scoped config
    Configuration group2BPConfig = new Configuration(group2BPProperties,
      emptyMap(), clusterConfig);

    // can't set parent here because it is reset in cluster topology
    Configuration group2Config = new Configuration(new HashMap<>(),
      emptyMap());
    // set config on HG
    TestHostGroup group2 = new TestHostGroup("group2", group2Components, Collections.singleton("testhost2"), group2Config);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    // todo: set as BP hostgroup
    topology.getHostGroupInfo().get("group2").getConfiguration().setParentConfiguration(group2BPConfig);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    assertEquals("testhost", clusterConfig.getPropertyValue("yarn-site", "yarn.resourcemanager.hostname"));
    assertEquals("testhost", group2Config.getProperties().get("yarn-site").get("yarn.resourcemanager.resource-tracker.address"));
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__MissingComponent() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    properties.put("mapred-site",
      new HashMap<>(ImmutableMap.of("mapreduce.job.hdfs-servers", "localhost")));
    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> group1Components = new HashSet<>();
    group1Components.add("SECONDARY_NAMENODE");
    group1Components.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", group1Components, Collections.singleton("testhost"));

    Collection<String> group2Components = new HashSet<>();
    group2Components.add("DATANODE");
    group2Components.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", group2Components, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    //todo: should throw a checked exception, not the exception expected by the api
    try {
      updater.doUpdateForClusterCreate();
      fail("IllegalArgumentException should have been thrown");
    } catch (IllegalArgumentException illegalArgumentException) {
      // expected exception
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__MissingComponent_NoValidationForFqdn() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    properties.put("mapred-site",
      new HashMap<>(ImmutableMap.of("mapreduce.job.hdfs-servers", "www.externalnamenode.org")));
    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> group1Components = new HashSet<>();
    group1Components.add("SECONDARY_NAMENODE");
    group1Components.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", group1Components, Collections.singleton("testhost"));

    Collection<String> group2Components = new HashSet<>();
    group2Components.add("DATANODE");
    group2Components.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", group2Components, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // No exception this time as SingleHostTopologyUpdater does no validation for fqdn's
    updater.doUpdateForClusterCreate();
    // No change as fqdn's are not updated
    assertEquals(
      "www.externalnamenode.org",
      clusterConfig.getPropertyValue("mapred-site", "mapreduce.job.hdfs-servers"));
  }


  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__MultipleMatchingHostGroupsError() throws Exception {

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    properties.put("mapred-site",
      new HashMap<>(ImmutableMap.of("mapreduce.job.hdfs-servers", "localhost")));

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> group1Components = new HashSet<>();
    group1Components.add("NAMENODE");
    group1Components.add("SECONDARY_NAMENODE");
    group1Components.add("RESOURCEMANAGER");
    group1Components.add("APP_TIMELINE_SERVER");
    group1Components.add("TIMELINE_READER");
    TestHostGroup group1 = new TestHostGroup("group1", group1Components, Collections.singleton("testhost"));

    Collection<String> group2Components = new HashSet<>();
    group2Components.add("NAMENODE");
    group2Components.add("DATANODE");
    group2Components.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", group2Components, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("0-1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    try {
      updater.doUpdateForClusterCreate();
      fail("IllegalArgumentException should have been thrown");
    } catch (IllegalArgumentException illegalArgumentException) {
      // expected exception
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__MultipleAppTimelineServer() throws Exception {

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();

    typeProps.put("yarn.timeline-service.address", "testhost:10200");
    typeProps.put("yarn.timeline-service.webapp.address", "testhost:8188");
    typeProps.put("yarn.timeline-service.webapp.https.address", "testhost:8190");
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> group1Components = new HashSet<>();
    group1Components.add("NAMENODE");
    group1Components.add("SECONDARY_NAMENODE");
    group1Components.add("RESOURCEMANAGER");
    group1Components.add("APP_TIMELINE_SERVER");
    group1Components.add("TIMELINE_READER");
    TestHostGroup group1 = new TestHostGroup("group1", group1Components, Collections.singleton("testhost"));

    Collection<String> group2Components = new HashSet<>();
    group2Components.add("DATANODE");
    group2Components.add("HDFS_CLIENT");
    group2Components.add("APP_TIMELINE_SERVER");
    group2Components.add("TIMELINE_READER");
    TestHostGroup group2 = new TestHostGroup("group2", group2Components, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("APP_TIMELINE_SERVER")).andReturn(new Cardinality("0-1")).anyTimes();
    expect(stack.getCardinality("TIMELINE_READER")).andReturn(new Cardinality("0-1")).anyTimes();


    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("yarn-site").get("yarn.timeline-service.address");
    assertEquals("Timeline Server config property should not have been updated", "testhost:10200", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__MissingOptionalComponent() throws Exception {
    final String expectedHostName = "localhost";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("yarn.timeline-service.address", expectedHostName);
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> group1Components = new HashSet<>();
    group1Components.add("NAMENODE");
    group1Components.add("SECONDARY_NAMENODE");
    group1Components.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", group1Components, Collections.singleton("testhost"));

    Collection<String> group2Components = new HashSet<>();
    group2Components.add("DATANODE");
    group2Components.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", group2Components, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("APP_TIMELINE_SERVER")).andReturn(new Cardinality("0-1")).anyTimes();
    expect(stack.getCardinality("TIMELINE_READER")).andReturn(new Cardinality("0-1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("yarn-site").get("yarn.timeline-service.address");
    assertEquals("Timeline Server config property should not have been updated", expectedHostName, updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__defaultValue__WithPort() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("fs.defaultFS", "localhost:5050");
    properties.put("core-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("core-site").get("fs.defaultFS");
    assertEquals("testhost:5050", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__defaultValues() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("hbase.zookeeper.quorum", "localhost");
    properties.put("hbase-site", typeProps);
    Map<String, String> livyConf = new HashMap<>();
    livyConf.put("livy.server.recovery.state-store.url", "/livy2-recovery");
    properties.put("livy2-conf", livyConf);
    Map<String, String> originalLivyConf = ImmutableMap.copyOf(livyConf);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();
    Map<String, Map<String, String>> fullProperties = topology.getConfiguration().getFullProperties();
    String updatedVal = fullProperties.get("hbase-site").get("hbase.zookeeper.quorum");
    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<>();
    expectedHosts.add("testhost");
    expectedHosts.add("testhost2");
    expectedHosts.add("testhost2a");
    expectedHosts.add("testhost2b");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }

    assertEquals(originalLivyConf, fullProperties.get("livy2-conf"));
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__defaultValues___withPorts() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("templeton.zookeeper.hosts", "localhost:9090");
    properties.put("webhcat-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("webhcat-site").get("templeton.zookeeper.hosts");
    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<>();
    expectedHosts.add("testhost:9090");
    expectedHosts.add("testhost2:9090");
    expectedHosts.add("testhost2a:9090");
    expectedHosts.add("testhost2b:9090");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testMultipleHostTopologyUpdater__localhost__singleHost() throws Exception {

    final String typeName = "hbase-site";
    final String propertyName = "hbase.zookeeper.quorum";
    final String originalValue = "localhost";
    final String component1 = "ZOOKEEPER_SERVER";
    final String component2 = "ZOOKEEPER_CLIENT";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put(propertyName, originalValue);
    properties.put(typeName, typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add(component1);
    Set<String> hosts1 = new HashSet<>();
    hosts1.add("testhost1a");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, hosts1);

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add(component2);
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);

    BlueprintConfigurationProcessor.MultipleHostTopologyUpdater mhtu = new BlueprintConfigurationProcessor.MultipleHostTopologyUpdater(component1);
    String newValue = mhtu.updateForClusterCreate(propertyName, originalValue, properties, topology);

    assertEquals("testhost1a", newValue);
  }

  @Test
  public void testMultipleHostTopologyUpdater__localhost__singleHostGroup() throws Exception {

    final String typeName = "hbase-site";
    final String propertyName = "hbase.zookeeper.quorum";
    final String originalValue = "localhost";
    final String component1 = "ZOOKEEPER_SERVER";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put(propertyName, originalValue);
    properties.put(typeName, typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add(component1);
    Set<String> hosts1 = new HashSet<>();
    hosts1.add("testhost1a");
    hosts1.add("testhost1b");
    hosts1.add("testhost1c");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, hosts1);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);

    BlueprintConfigurationProcessor.MultipleHostTopologyUpdater mhtu = new BlueprintConfigurationProcessor.MultipleHostTopologyUpdater(component1);
    String newValue = mhtu.updateForClusterCreate(propertyName, originalValue, properties, topology);

    List<String> hostArray = Arrays.asList(newValue.split(","));
    Assert.assertTrue(hostArray.containsAll(hosts1) && hosts1.containsAll(hostArray));
  }

  @Test
  public void testMultipleHostTopologyUpdater__hostgroup__singleHostGroup() throws Exception {

    final String typeName = "hbase-site";
    final String propertyName = "hbase.zookeeper.quorum";
    final String originalValue = "%HOSTGROUP::group1%";
    final String component1 = "ZOOKEEPER_SERVER";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put(propertyName, originalValue);
    properties.put(typeName, typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add(component1);
    Set<String> hosts1 = new HashSet<>();
    hosts1.add("testhost1a");
    hosts1.add("testhost1b");
    hosts1.add("testhost1c");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, hosts1);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);

    BlueprintConfigurationProcessor.MultipleHostTopologyUpdater mhtu = new BlueprintConfigurationProcessor.MultipleHostTopologyUpdater(component1);
    String newValue = mhtu.updateForClusterCreate(propertyName, originalValue, properties, topology);

    List<String> hostArray = Arrays.asList(newValue.split(","));
    Assert.assertTrue(hostArray.containsAll(hosts1) && hosts1.containsAll(hostArray));
  }

  @Test
  public void testMultipleHostTopologyUpdater__hostgroup__multipleHostGroups() throws Exception {

    final String typeName = "application-properties";
    final String propertyName = "atlas.rest.address";
    final String originalValue = "http://%HOSTGROUP::group1%:21000,http://%HOSTGROUP::group2%:21000";
    final String component = "ATLAS_SERVER";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put(propertyName, originalValue);
    properties.put(typeName, typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Set<String> components = ImmutableSet.of(component);

    Set<String> group1Hosts = ImmutableSet.of("testhost1a", "testhost1b", "testhost1c");
    Set<String> group2Hosts = ImmutableSet.of("testhost2a", "testhost2b", "testhost2c");

    TestHostGroup group1 = new TestHostGroup("group1", components, group1Hosts);
    TestHostGroup group2 = new TestHostGroup("group2", components, group2Hosts);

    Collection<TestHostGroup> hostGroups = ImmutableSet.of(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);

    BlueprintConfigurationProcessor.MultipleHostTopologyUpdater mhtu =
      new BlueprintConfigurationProcessor.MultipleHostTopologyUpdater(component, ',', true, true, true);
    String newValue = mhtu.updateForClusterCreate(propertyName, originalValue, properties, topology);

    Set<String> expectedAddresses =
      Sets.union(group1Hosts, group2Hosts).stream().map(host -> "http://" + host + ":21000").collect(toSet());
    Set<String> replacedAddresses = ImmutableSet.copyOf(newValue.split(","));
    assertEquals(expectedAddresses, replacedAddresses);
  }

  @Test
  public void testDoUpdateForClusterVerifyRetrySettingsDefault() throws Exception {
    Map<String, Map<String, String>> configProperties =
      new HashMap<>();

    HashMap<String, String> clusterEnvProperties = new HashMap<>();
    configProperties.put("cluster-env", clusterEnvProperties);

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());

    TestHostGroup testHostGroup = new TestHostGroup("test-host-group-one", Collections.emptySet(), Collections.emptySet());
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, Collections.singleton(testHostGroup));

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes =
      updater.doUpdateForClusterCreate();

    // after update, verify that the retry properties for commands and installs are set as expected
    assertEquals("Incorrect number of properties added to cluster-env for retry",
      3, clusterEnvProperties.size());
    assertEquals("command_retry_enabled was not set to the expected default",
      "true", clusterEnvProperties.get("command_retry_enabled"));
    assertEquals("commands_to_retry was not set to the expected default",
      "INSTALL,START", clusterEnvProperties.get("commands_to_retry"));
    assertEquals("command_retry_max_time_in_sec was not set to the expected default",
      "600", clusterEnvProperties.get("command_retry_max_time_in_sec"));

    assertEquals("Incorrect number of config types updated by this operation",
      1, updatedConfigTypes.size());

    assertTrue("Expected type not included in the updated set",
      updatedConfigTypes.contains("cluster-env"));
  }

  @Test
  public void testDoUpdateForClusterVerifyRetrySettingsCustomized() throws Exception {
    Map<String, Map<String, String>> configProperties =
      new HashMap<>();

    HashMap<String, String> clusterEnvProperties = new HashMap<>();
    configProperties.put("cluster-env", clusterEnvProperties);

    clusterEnvProperties.put("command_retry_enabled", "false");
    clusterEnvProperties.put("commands_to_retry", "TEST");
    clusterEnvProperties.put("command_retry_max_time_in_sec", "1");


    Configuration clusterConfig = new Configuration(configProperties, emptyMap());

    TestHostGroup testHostGroup = new TestHostGroup("test-host-group-one", Collections.emptySet(), Collections.emptySet());
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, Collections.singleton(testHostGroup));

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes =
      updater.doUpdateForClusterCreate();

    // after update, verify that the retry properties for commands and installs are set as expected
    // in this case, the customer-provided overrides should be honored, rather than the retry defaults
    assertEquals("Incorrect number of properties added to cluster-env for retry",
      3, clusterEnvProperties.size());
    assertEquals("command_retry_enabled was not set to the expected default",
      "false", clusterEnvProperties.get("command_retry_enabled"));
    assertEquals("commands_to_retry was not set to the expected default",
      "TEST", clusterEnvProperties.get("commands_to_retry"));
    assertEquals("command_retry_max_time_in_sec was not set to the expected default",
      "1", clusterEnvProperties.get("command_retry_max_time_in_sec"));

    assertEquals("Incorrect number of config types updated",
      0, updatedConfigTypes.size());
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeHAEnabledSpecifyingHostNamesDirectly() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "server-two";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    configProperties.put("hdfs-site", hdfsSiteProperties);
    configProperties.put("hadoop-env", hadoopEnvProperties);
    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);
    configProperties.put("accumulo-site", accumuloSiteProperties);

    // setup hdfs HA config for test
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);

    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createHostAddress(expectedHostName, expectedPortNum));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createHostAddress(expectedHostNameTwo, expectedPortNum));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createHostAddress(expectedHostName, expectedPortNum));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createHostAddress(expectedHostNameTwo, expectedPortNum));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createHostAddress(expectedHostName, expectedPortNum));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createHostAddress(expectedHostNameTwo, expectedPortNum));

    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup("host-group-2", hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();

    // verify that dfs.internal.nameservices was added
    assertEquals("dfs.internal.nameservices wasn't added", expectedNameService, hdfsSiteProperties.get("dfs.internal.nameservices"));

    // verify that the expected hostname was substituted for the host group name in the config
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));

    // verify that the Blueprint config processor has set the internal required properties
    // that determine the active and standby node hostnames for this HA setup
    // one of the two hosts should be set to active and the other to standby
    Map<String, String> clusterEnv = clusterConfig.getProperties().get("cluster-env");
    String activeHost = clusterEnv.get("dfs_ha_initial_namenode_active");
    if (activeHost.equals(expectedHostName)) {
      assertEquals("Standby Namenode hostname was not set correctly",
        expectedHostNameTwo, clusterEnv.get("dfs_ha_initial_namenode_standby"));
    } else if (activeHost.equals(expectedHostNameTwo)) {
      assertEquals("Standby Namenode hostname was not set correctly",
        expectedHostName, clusterEnv.get("dfs_ha_initial_namenode_standby"));
    } else {
      fail("Active Namenode hostname was not set correctly: " + activeHost);
    }

    assertEquals("fs.defaultFS should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService, coreSiteProperties.get("fs.defaultFS"));

    assertEquals("hbase.rootdir should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/hbase/test/root/dir", hbaseSiteProperties.get("hbase.rootdir"));

    assertEquals("instance.volumes should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes", accumuloSiteProperties.get("instance.volumes"));
  }

  @Test
  public void testHiveConfigClusterUpdateCustomValueSpecifyingHostNamesMetaStoreHA() throws Exception {
    final String expectedHostGroupName = "host_group_1";

    final String expectedPropertyValue =
      "hive.metastore.local=false,hive.metastore.uris=thrift://headnode0.ivantestcluster2-ssh.d1.internal.cloudapp.net:9083,hive.user.install.directory=/user";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> webHCatSiteProperties = new HashMap<>();
    configProperties.put("webhcat-site", webHCatSiteProperties);

    // setup properties that include host information
    webHCatSiteProperties.put("templeton.hive.properties", expectedPropertyValue);

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton("some-host"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    TestHostGroup group2 = new TestHostGroup("host_group_2", hgComponents2, Collections.singleton("some-host2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();

    assertEquals("Unexpected config update for templeton.hive.properties",
      expectedPropertyValue,
      webHCatSiteProperties.get("templeton.hive.properties"));
  }

  @Test
  public void testHiveConfigClusterUpdateSpecifyingHostNamesHiveServer2HA() throws Exception {
    final String expectedHostGroupName = "host_group_1";

    final String expectedMetaStoreURIs = "thrift://c6401.ambari.apache.org:9083,thrift://c6402.ambari.apache.org:9083";

    Map<String, Map<String, String>> configProperties =
      new HashMap<>();

    Map<String, String> hiveEnvProperties =
      new HashMap<>();
    Map<String, String> hiveSiteProperties =
      new HashMap<>();

    configProperties.put("hive-env", hiveEnvProperties);
    configProperties.put("hive-site", hiveSiteProperties);

    // simulate HA mode, since this property must be present in HiveServer2 HA
    hiveSiteProperties.put("hive.server2.support.dynamic.service.discovery", "true");

    // set MetaStore URIs property to reflect an HA environment for HIVE_METASTORE

    hiveSiteProperties.put("hive.metastore.uris", expectedMetaStoreURIs);

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("HIVE_SERVER");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton("some-host"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("HIVE_SERVER");
    TestHostGroup group2 = new TestHostGroup("host_group_2", hgComponents2, Collections.singleton("some-host2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("HIVE_SERVER")).andReturn(new Cardinality("1+")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();

    assertEquals("Unexpected config update for hive.metastore.uris",
      expectedMetaStoreURIs,
      hiveSiteProperties.get("hive.metastore.uris"));
  }

  @Test
  public void testHiveConfigClusterUpdateUsingExportedNamesHiveServer2HA() throws Exception {
    final String expectedHostGroupNameOne = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    final String expectedHostNameOne =
      "c6401.ambari.apache.org";

    final String expectedHostNameTwo =
      "c6402.ambari.apache.org";


    // use exported HOSTGROUP syntax for this property, to make sure the
    // config processor updates this as expected
    final String inputMetaStoreURIs = "thrift://" + createExportedAddress("9083", expectedHostGroupNameOne) + "," + "thrift://" + createExportedAddress("9083", expectedHostGroupNameTwo);

    final String expectedMetaStoreURIs = "thrift://c6401.ambari.apache.org:9083,thrift://c6402.ambari.apache.org:9083";


    Map<String, Map<String, String>> configProperties =
      new HashMap<>();

    Map<String, String> hiveEnvProperties =
      new HashMap<>();
    Map<String, String> hiveSiteProperties =
      new HashMap<>();

    configProperties.put("hive-env", hiveEnvProperties);
    configProperties.put("hive-site", hiveSiteProperties);

    // simulate HA mode, since this property must be present in HiveServer2 HA
    hiveSiteProperties.put("hive.server2.support.dynamic.service.discovery", "true");

    // set MetaStore URIs property to reflect an HA environment for HIVE_METASTORE

    hiveSiteProperties.put("hive.metastore.uris", inputMetaStoreURIs);

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("HIVE_SERVER");
    hgComponents.add("HIVE_METASTORE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupNameOne, hgComponents, Collections.singleton(expectedHostNameOne));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("HIVE_SERVER");
    hgComponents2.add("HIVE_METASTORE");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("HIVE_SERVER")).andReturn(new Cardinality("1+")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();

    assertEquals("Unexpected config update for hive.metastore.uris",
      expectedMetaStoreURIs,
      hiveSiteProperties.get("hive.metastore.uris"));
  }

  @Test
  public void testHivePropertiesLocalhostReplacedComma() throws Exception {
    testHiveMetastoreHA(",");
  }

  @Test
  public void testHivePropertiesLocalhostReplacedCommaSpace() throws Exception {
    testHiveMetastoreHA(", ");
  }

  @Test
  public void testHivePropertiesLocalhostReplacedSpaceComma() throws Exception {
    testHiveMetastoreHA(" ,");
  }

  @Test
  public void testHivePropertiesLocalhostReplacedSpaceCommaSpace() throws Exception {
    testHiveMetastoreHA(" , ");
  }

  private void testHiveMetastoreHA(String separator) throws InvalidTopologyException, ConfigurationTopologyException {
    final String[] parts = new String[]{
      "hive.metastore.local=false",
      "hive.metastore.uris=" + getThriftURI("localhost"),
      "hive.metastore.sasl.enabled=false"
    };
    final String[] hostNames = new String[]{"c6401.ambari.apache.org", "example.com", "c6402.ambari.apache.org"};
    final Set<String> expectedUris = new HashSet<>();
    for (String hostName : hostNames) {
      expectedUris.add(getThriftURI(hostName));
    }

    final String initialPropertyValue = StringUtils.join(parts, separator);

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> webHCatSiteProperties = new HashMap<>();

    configProperties.put("webhcat-site", webHCatSiteProperties);

    // setup properties that include host information
    String propertyKey = "templeton.hive.properties";
    webHCatSiteProperties.put(propertyKey, initialPropertyValue);

    Map<String, Map<String, Map<String, String>>> attributes = emptyMap();
    Configuration clusterConfig = new Configuration(configProperties, attributes);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    for (int i = 0; i < hostNames.length; ++i) {
      Collection<String> components = new HashSet<>(Collections.singleton("HIVE_METASTORE"));
      hostGroups.add(new TestHostGroup("host_group_" + i, components, Collections.singleton(hostNames[i])));
    }

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();
    String updatedValue = webHCatSiteProperties.get(propertyKey);

    // verify that the host name for the metastore.uris property has been updated, and
    // that all MetaStore Server URIs are included, using the required Hive Syntax
    // Depends on hashing, string representation can be different
    String prefix = parts[0] + ",";
    assertTrue(updatedValue, updatedValue.startsWith(prefix));

    String suffix = "," + parts[2];
    assertTrue(updatedValue, updatedValue.endsWith(suffix));

    String part1 = updatedValue.replace(prefix, "").replace(suffix, "");
    String key = "hive.metastore.uris=";
    assertTrue(part1, part1.startsWith(key));

    Set<String> updatedUris = new HashSet<>(Arrays.asList(part1.replace(key, "").split("\\\\,")));
    assertEquals(expectedUris, updatedUris);
  }

  private static String getThriftURI(String hostName) {
    return "thrift://" + hostName + ":9933";
  }

  @Test
  public void testHiveInteractiveLlapZookeeperConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String llapZkProperty = "hive.llap.zk.sm.connectionString";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> hiveInteractiveSiteProperties = new HashMap<>();

    configProperties.put("hive-interactive-site", hiveInteractiveSiteProperties);

    hiveInteractiveSiteProperties.put(llapZkProperty, createHostAddress(expectedHostName, "2181") + "," + createHostAddress(expectedHostNameTwo, "2181"));

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("ZOOKEEPER_SERVER");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    final String expectedPropertyValue = createExportedAddress("2181", expectedHostGroupName) + "," + createExportedAddress("2181", expectedHostGroupNameTwo);
    assertEquals("hive.llap.zk.sm.connectionString property not updated correctly", expectedPropertyValue, hiveInteractiveSiteProperties.get(llapZkProperty));
  }

  @Test
  public void testOozieConfigClusterUpdateHAEnabledSpecifyingHostNamesDirectly() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedExternalHost = "c6408.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> oozieSiteProperties = new HashMap<>();
    Map<String, String> oozieEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();

    configProperties.put("oozie-site", oozieSiteProperties);
    configProperties.put("oozie-env", oozieEnvProperties);
    configProperties.put("hive-env", oozieEnvProperties);
    configProperties.put("core-site", coreSiteProperties);

    oozieSiteProperties.put("oozie.base.url", expectedHostName);
    oozieSiteProperties.put("oozie.authentication.kerberos.principal", expectedHostName);
    oozieSiteProperties.put("oozie.service.HadoopAccessorService.kerberos.principal", expectedHostName);
    oozieSiteProperties.put("oozie.service.JPAService.jdbc.url", "jdbc:mysql://" + expectedExternalHost + "/ooziedb");

    // simulate the Oozie HA configuration
    oozieSiteProperties.put("oozie.services.ext",
      "org.apache.oozie.service.ZKLocksService,org.apache.oozie.service.ZKXLogStreamingService,org.apache.oozie.service.ZKJobsConcurrencyService,org.apache.oozie.service.ZKUUIDService");

    oozieEnvProperties.put("oozie_existing_mysql_host", expectedExternalHost);

    coreSiteProperties.put("hadoop.proxyuser.oozie.hosts", expectedHostName + "," + expectedHostNameTwo);

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("OOZIE_SERVER");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton("host1"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("OOZIE_SERVER");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("OOZIE_SERVER")).andReturn(new Cardinality("1+")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();

    assertEquals("oozie property not updated correctly",
      expectedHostName, oozieSiteProperties.get("oozie.base.url"));
    assertEquals("oozie property not updated correctly",
      expectedHostName, oozieSiteProperties.get("oozie.authentication.kerberos.principal"));
    assertEquals("oozie property not updated correctly",
      expectedHostName, oozieSiteProperties.get("oozie.service.HadoopAccessorService.kerberos.principal"));
    assertEquals("oozie property not updated correctly",
      expectedHostName + "," + expectedHostNameTwo, coreSiteProperties.get("hadoop.proxyuser.oozie.hosts"));
  }

  @Test
  public void testOozieHAEnabledExport() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedExternalHost = "c6408.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedPortNum = "80000";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> oozieSiteProperties = new HashMap<>();
    Map<String, String> oozieEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();

    configProperties.put("oozie-site", oozieSiteProperties);
    configProperties.put("oozie-env", oozieEnvProperties);
    configProperties.put("hive-env", oozieEnvProperties);
    configProperties.put("core-site", coreSiteProperties);


    oozieSiteProperties.put("oozie.base.url", expectedHostName + ":" + expectedPortNum);
    oozieSiteProperties.put("oozie.authentication.kerberos.principal", expectedHostName);
    oozieSiteProperties.put("oozie.service.HadoopAccessorService.kerberos.principal", expectedHostName);
    oozieSiteProperties.put("oozie.service.JPAService.jdbc.url", "jdbc:mysql://" + expectedExternalHost + "/ooziedb");

    // simulate the Oozie HA configuration
    oozieSiteProperties.put("oozie.services.ext",
      "org.apache.oozie.service.ZKLocksService,org.apache.oozie.service.ZKXLogStreamingService,org.apache.oozie.service.ZKJobsConcurrencyService,org.apache.oozie.service.ZKUUIDService");
    oozieSiteProperties.put("oozie.zookeeper.connection.string", createHostAddress(expectedHostName, "2181") + "," + createHostAddress(expectedHostNameTwo, "2181"));


    oozieEnvProperties.put("oozie_existing_mysql_host", expectedExternalHost);

    coreSiteProperties.put("hadoop.proxyuser.oozie.hosts", expectedHostName + "," + expectedHostNameTwo);

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("OOZIE_SERVER");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("OOZIE_SERVER");
    hgComponents2.add("ZOOKEEPER_SERVER");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("OOZIE_SERVER")).andReturn(new Cardinality("1+")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertEquals("oozie property not updated correctly",
      createExportedHostName(expectedHostGroupName, expectedPortNum), oozieSiteProperties.get("oozie.base.url"));
    assertEquals("oozie property not updated correctly",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.oozie.hosts"));
    assertEquals("oozie property not updated correctly",
      createExportedAddress("2181", expectedHostGroupName) + "," + createExportedAddress("2181", expectedHostGroupNameTwo), oozieSiteProperties.get("oozie.zookeeper.connection.string"));
  }

  @Test
  public void testYarnHighAvailabilityConfigClusterUpdateSpecifyingHostNamesDirectly() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> yarnSiteProperties = new HashMap<>();
    configProperties.put("yarn-site", yarnSiteProperties);

    // setup properties that include host information
    yarnSiteProperties.put("yarn.log.server.url", "http://" + expectedHostName + ":19888/jobhistory/logs");
    yarnSiteProperties.put("yarn.resourcemanager.hostname", expectedHostName);
    yarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.scheduler.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.admin.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.https.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.reader.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.reader.webapp.https.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.ha.enabled", "true");
    yarnSiteProperties.put("yarn.resourcemanager.ha.rm-ids", "rm1, rm2");

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("RESOURCEMANAGER");
    hgComponents.add("APP_TIMELINE_SERVER");
    hgComponents.add("TIMELINE_READER");
    hgComponents.add("HISTORYSERVER");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("RESOURCEMANAGER");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("RESOURCEMANAGER")).andReturn(new Cardinality("1-2")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();

    // verify that the properties with hostname information was correctly preserved
    assertEquals("Yarn Log Server URL was incorrectly updated",
      "http://" + expectedHostName + ":19888/jobhistory/logs", yarnSiteProperties.get("yarn.log.server.url"));
    assertEquals("Yarn ResourceManager hostname was incorrectly exported",
      expectedHostName, yarnSiteProperties.get("yarn.resourcemanager.hostname"));
    assertEquals("Yarn ResourceManager tracker address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.resource-tracker.address"));
    assertEquals("Yarn ResourceManager webapp address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.webapp.address"));
    assertEquals("Yarn ResourceManager scheduler address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.scheduler.address"));
    assertEquals("Yarn ResourceManager address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.address"));
    assertEquals("Yarn ResourceManager admin address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.admin.address"));
    assertEquals("Yarn ResourceManager timeline-service address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.address"));
    assertEquals("Yarn ResourceManager timeline webapp address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.webapp.address"));
    assertEquals("Yarn ResourceManager timeline webapp HTTPS address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.webapp.https.address"));
    assertEquals("Yarn ResourceManager timeline reader webapp address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.reader.webapp.address"));
    assertEquals("Yarn ResourceManager timeline reader webapp HTTPS address was incorrectly updated",
      createHostAddress(expectedHostName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.reader.webapp.https.address"));
  }

  @Test
  public void testYarnHighAvailabilityExport() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> yarnSiteProperties = new HashMap<>();
    configProperties.put("yarn-site", yarnSiteProperties);

    // setup properties that include host information
    yarnSiteProperties.put("yarn.log.server.url", "http://" + expectedHostName + ":19888/jobhistory/logs");
    yarnSiteProperties.put("yarn.resourcemanager.hostname", expectedHostName);
    yarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.scheduler.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.admin.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.https.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.reader.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.reader.webapp.https.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.ha.enabled", "true");
    yarnSiteProperties.put("yarn.resourcemanager.ha.rm-ids", "rm1, rm2");
    yarnSiteProperties.put("yarn.resourcemanager.hostname.rm1", expectedHostName);
    yarnSiteProperties.put("yarn.resourcemanager.hostname.rm2", expectedHostNameTwo);
    yarnSiteProperties.put("yarn.resourcemanager.address.rm1", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.address.rm2", expectedHostNameTwo + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.admin.address.rm1", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.admin.address.rm2", expectedHostNameTwo + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address.rm1", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address.rm2", expectedHostNameTwo + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.scheduler.address.rm1", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.scheduler.address.rm2", expectedHostNameTwo + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.address.rm1", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.address.rm2", expectedHostNameTwo + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.https.address.rm1", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.https.address.rm2", expectedHostNameTwo + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.zk-address", expectedHostName + ":" + "2181" + "," + expectedHostNameTwo + ":" + "2181");
    yarnSiteProperties.put("yarn.resourcemanager.webapp.https.address", expectedHostName + ":" + "8080");


    Configuration clusterConfig = new Configuration(configProperties, emptyMap());
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("RESOURCEMANAGER");
    hgComponents.add("APP_TIMELINE_SERVER");
    hgComponents.add("TIMELINE_READER");
    hgComponents.add("HISTORYSERVER");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("RESOURCEMANAGER");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("RESOURCEMANAGER")).andReturn(new Cardinality("1-2")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    // verify that the properties with hostname information was correctly preserved
    assertEquals("Yarn Log Server URL was incorrectly updated",
      "http://" + createExportedAddress("19888", expectedHostGroupName) + "/jobhistory/logs", yarnSiteProperties.get("yarn.log.server.url"));
    assertEquals("Yarn ResourceManager hostname was incorrectly updated",
      createExportedHostName(expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.hostname"));
    assertEquals("Yarn ResourceManager tracker address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.resource-tracker.address"));
    assertEquals("Yarn ResourceManager webapp address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.webapp.address"));
    assertEquals("Yarn ResourceManager scheduler address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.scheduler.address"));
    assertEquals("Yarn ResourceManager address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.address"));
    assertEquals("Yarn ResourceManager admin address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.resourcemanager.admin.address"));
    assertEquals("Yarn ResourceManager timeline-service address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.address"));
    assertEquals("Yarn ResourceManager timeline webapp address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.webapp.address"));
    assertEquals("Yarn ResourceManager timeline webapp HTTPS address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.webapp.https.address"));
    assertEquals("Yarn ResourceManager timeline reader webapp address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.reader.webapp.address"));
    assertEquals("Yarn ResourceManager timeline reader ebapp HTTPS address was incorrectly updated",
      createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get("yarn.timeline-service.reader.webapp.https.address"));

    // verify that dynamically-named RM HA properties are exported as expected
    List<String> properties = Arrays.asList(
      "yarn.resourcemanager.address",
      "yarn.resourcemanager.admin.address",
      "yarn.resourcemanager.resource-tracker.address",
      "yarn.resourcemanager.scheduler.address",
      "yarn.resourcemanager.webapp.address",
      "yarn.resourcemanager.webapp.https.address"
    );
    for (String property : properties) {
      String propertyWithID = property + ".rm1";
      assertEquals(propertyWithID, createExportedHostName(expectedHostGroupName, expectedPortNum), yarnSiteProperties.get(propertyWithID));
      propertyWithID = property + ".rm2";
      assertEquals(propertyWithID, createExportedHostName(expectedHostGroupNameTwo, expectedPortNum), yarnSiteProperties.get(propertyWithID));
    }

    assertEquals("Yarn Zookeeper address property not exported properly",
      createExportedHostName(expectedHostGroupName, "2181") + "," + createExportedHostName(expectedHostGroupNameTwo, "2181"),
      yarnSiteProperties.get("yarn.resourcemanager.zk-address"));

    assertEquals("Yarn RM webapp address not exported properly",
      createExportedHostName(expectedHostGroupName, "8080"), yarnSiteProperties.get("yarn.resourcemanager.webapp.https.address"));

  }

  @Test
  public void testHDFSConfigClusterUpdateQuorumJournalURLSpecifyingHostNamesDirectly() throws Exception {
    final String expectedHostNameOne = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedQuorumJournalURL = "qjournal://" + createHostAddress(expectedHostNameOne, expectedPortNum) + ";" +
      createHostAddress(expectedHostNameTwo, expectedPortNum) + "/mycluster";

    Map<String, Map<String, String>> configProperties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    configProperties.put("hdfs-site", hdfsSiteProperties);

    // setup properties that include host information
    // setup shared edit property, that includes a qjournal URL scheme

    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir", expectedQuorumJournalURL);

    Configuration clusterConfig = new Configuration(configProperties, emptyMap());
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton("host1"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.doUpdateForClusterCreate();

    // expect that all servers are included in configuration property without changes, and that the qjournal URL format is preserved
    assertEquals("HDFS HA shared edits directory property should not have been modified, since FQDNs were specified.",
      expectedQuorumJournalURL,
      hdfsSiteProperties.get("dfs.namenode.shared.edits.dir"));
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__defaultValues___YAML() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("storm.zookeeper.servers", "['localhost']");
    properties.put("storm-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);


    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("storm-site").get("storm.zookeeper.servers");
    assertTrue(updatedVal.startsWith("["));
    assertTrue(updatedVal.endsWith("]"));
    // remove the surrounding brackets
    updatedVal = updatedVal.replaceAll("[\\[\\]]", "");

    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<>();
    expectedHosts.add("'testhost'");
    expectedHosts.add("'testhost2'");
    expectedHosts.add("'testhost2a'");
    expectedHosts.add("'testhost2b'");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_Storm_Nimbus_HA_Enabled__defaultValues_YAML() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("nimbus.seeds", "localhost");
    properties.put("storm-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NIMBUS");

    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NIMBUS");

    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");

    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("storm-site").get("nimbus.seeds");
    assertTrue("Updated YAML value should start with bracket", updatedVal.startsWith("["));
    assertTrue("Updated YAML value should end with bracket", updatedVal.endsWith("]"));
    // remove the surrounding brackets
    updatedVal = updatedVal.replaceAll("[\\[\\]]", "");

    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<>();
    expectedHosts.add("testhost");
    expectedHosts.add("testhost2");

    assertEquals("Incorrect number of hosts found in updated Nimbus config property", 2, hosts.length);
    for (String host : hosts) {
      assertTrue("Expected host name = " + host + " not found in updated Nimbus config property", expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_Storm_Nimbus_HA_Enabled__FQDN_ValuesSpecified_YAML() throws Exception {
    final String expectedValue = "[c6401.ambari.apache.org, c6402.ambari.apache.org]";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("nimbus.seeds", expectedValue);
    properties.put("storm-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NIMBUS");

    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NIMBUS");

    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");

    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("storm-site").get("nimbus.seeds");

    assertEquals("nimbus.seeds property should not be updated when FQDNs are specified in configuration",
      expectedValue, updatedVal);
  }


  @Test
  public void testDoUpdateForClusterCreate_MProperty__defaultValues() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("hbase_master_heapsize", "512m");
    properties.put("hbase-env", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("hbase-env").get("hbase_master_heapsize");
    assertEquals("512m", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_MProperty__missingM() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("hbase_master_heapsize", "512");
    properties.put("hbase-env", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("hbase-env").get("hbase_master_heapsize");
    assertEquals("512m", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__exportedValue() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("yarn.resourcemanager.hostname", "%HOSTGROUP::group1%");
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("yarn-site").get("yarn.resourcemanager.hostname");
    assertEquals("testhost", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__exportedValue_UsingMinusSymbolInHostGroupName() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("yarn.resourcemanager.hostname", "%HOSTGROUP::os-amb-r6-secha-1427972156-hbaseha-3-6%");
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("os-amb-r6-secha-1427972156-hbaseha-3-6", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("yarn-site").get("yarn.resourcemanager.hostname");
    assertEquals("testhost", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__exportedValue_WithPort_UsingMinusSymbolInHostGroupName() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("yarn.resourcemanager.hostname", "%HOSTGROUP::os-amb-r6-secha-1427972156-hbaseha-3-6%:2180");
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("os-amb-r6-secha-1427972156-hbaseha-3-6", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("yarn-site").get("yarn.resourcemanager.hostname");
    assertEquals("testhost:2180", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__exportedValue__WithPort() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("fs.defaultFS", "%HOSTGROUP::group1%:5050");
    properties.put("core-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("core-site").get("fs.defaultFS");
    assertEquals("testhost:5050", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__exportedValues() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("hbase.zookeeper.quorum", "%HOSTGROUP::group1%,%HOSTGROUP::group2%");
    properties.put("hbase-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("hbase-site").get("hbase.zookeeper.quorum");
    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<>();
    expectedHosts.add("testhost");
    expectedHosts.add("testhost2");
    expectedHosts.add("testhost2a");
    expectedHosts.add("testhost2b");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__exportedValues___withPorts() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("templeton.zookeeper.hosts", "%HOSTGROUP::group1%:9090,%HOSTGROUP::group2%:9091");
    properties.put("webhcat-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("webhcat-site").get("templeton.zookeeper.hosts");
    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<>();
    expectedHosts.add("testhost:9090");
    expectedHosts.add("testhost2:9091");
    expectedHosts.add("testhost2a:9091");
    expectedHosts.add("testhost2b:9091");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__exportedValues___withPorts_UsingMinusSymbolInHostGroupName() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("ha.zookeeper.quorum", "%HOSTGROUP::os-amb-r6-secha-1427972156-hbaseha-3-6%:2181,%HOSTGROUP::os-amb-r6-secha-1427972156-hbaseha-3-5%:2181,%HOSTGROUP::os-amb-r6-secha-1427972156-hbaseha-3-7%:2181");
    properties.put("core-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("os-amb-r6-secha-1427972156-hbaseha-3-6", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("os-amb-r6-secha-1427972156-hbaseha-3-5", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("os-amb-r6-secha-1427972156-hbaseha-3-7", hgComponents3, hosts3);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("core-site").get("ha.zookeeper.quorum");
    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<>();
    expectedHosts.add("testhost:2181");
    expectedHosts.add("testhost2:2181");
    expectedHosts.add("testhost2a:2181");
    expectedHosts.add("testhost2b:2181");
    expectedHosts.add("testhost3:2181");
    expectedHosts.add("testhost3a:2181");

    assertEquals(6, hosts.length);
    for (String host : hosts) {
      assertTrue("Expected host :" + host + "was not included in the multi-server list in this property.", expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty_exportedValues_withPorts_singleHostValue() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> yarnSiteConfig = new HashMap<>();

    yarnSiteConfig.put("hadoop.registry.zk.quorum", "%HOSTGROUP::host_group_1%:2181");
    properties.put("yarn-site", yarnSiteConfig);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    TestHostGroup group1 = new TestHostGroup("host_group_1", hgComponents, Collections.singleton("testhost"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    assertEquals("Multi-host property with single host value was not correctly updated for cluster create.",
      "testhost:2181", topology.getConfiguration().getFullProperties().get("yarn-site").get("hadoop.registry.zk.quorum"));
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__exportedValues___YAML() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("storm.zookeeper.servers", "['%HOSTGROUP::group1%:9090','%HOSTGROUP::group2%:9091']");
    typeProps.put("nimbus.seeds", "[%HOSTGROUP::group1%, %HOSTGROUP::group4%]");
    properties.put("storm-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    hgComponents.add("NIMBUS");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    hgComponents2.add("NIMBUS");
    Set<String> hosts2 = new HashSet<>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, hosts2);

    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, hosts3);

    Collection<String> hgComponents4 = new HashSet<>();
    hgComponents4.add("NIMBUS");
    Set<String> hosts4 = new HashSet<>();
    hosts4.add("testhost4");
    TestHostGroup group4 = new TestHostGroup("group4", hgComponents4, hosts4);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("storm-site").get("storm.zookeeper.servers");
    assertTrue(updatedVal.startsWith("["));
    assertTrue(updatedVal.endsWith("]"));
    // remove the surrounding brackets
    updatedVal = updatedVal.replaceAll("[\\[\\]]", "");

    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<>();
    expectedHosts.add("'testhost:9090'");
    expectedHosts.add("'testhost2:9091'");
    expectedHosts.add("'testhost2a:9091'");
    expectedHosts.add("'testhost2b:9091'");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }


    String updatedNimbusSeedsVal = topology.getConfiguration().getFullProperties().get("storm-site").get("nimbus.seeds");
    assertTrue("Updated YAML value should start with bracket", updatedNimbusSeedsVal.startsWith("["));
    assertTrue("Updated YAML value should end with bracket", updatedNimbusSeedsVal.endsWith("]"));
    // remove the surrounding brackets
    updatedNimbusSeedsVal = updatedNimbusSeedsVal.replaceAll("[\\[\\]]", "");

    String[] nimbusHosts = updatedNimbusSeedsVal.split(",");

    Collection<String> expectedNimbusHosts = new HashSet<>();
    expectedNimbusHosts.add("testhost");
    expectedNimbusHosts.add("testhost4");

    assertEquals("Incorrect number of hosts found in updated Nimbus config property", 2, nimbusHosts.length);
    for (String host : nimbusHosts) {
      assertTrue("Expected Nimbus host = " + host + " not found in nimbus.seeds property value", expectedNimbusHosts.contains(host));
      expectedHosts.remove(host);
    }


  }

  @Test
  public void testDoUpdateForClusterCreate_DBHostProperty__defaultValue() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hiveSiteProps = new HashMap<>();
    hiveSiteProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://localhost/hive?createDatabaseIfNotExist=true");
    Map<String, String> hiveEnvProps = new HashMap<>();
    hiveEnvProps.put("hive_database", "New MySQL Database");
    properties.put("hive-site", hiveSiteProps);
    properties.put("hive-env", hiveEnvProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    hgComponents.add("MYSQL_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("hive-site").get("javax.jdo.option.ConnectionURL");
    assertEquals("jdbc:mysql://testhost/hive?createDatabaseIfNotExist=true", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_DBHostProperty__exportedValue() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hiveSiteProps = new HashMap<>();
    hiveSiteProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://%HOSTGROUP::group1%/hive?createDatabaseIfNotExist=true");
    Map<String, String> hiveEnvProps = new HashMap<>();
    hiveEnvProps.put("hive_database", "New MySQL Database");
    properties.put("hive-site", hiveSiteProps);
    properties.put("hive-env", hiveEnvProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    hgComponents.add("MYSQL_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("hive-site").get("javax.jdo.option.ConnectionURL");
    assertEquals("jdbc:mysql://testhost/hive?createDatabaseIfNotExist=true", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_DBHostProperty__external() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://myHost.com/hive?createDatabaseIfNotExist=true");
    typeProps.put("hive_database", "Existing MySQL Database");
    properties.put("hive-env", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
    String updatedVal = topology.getConfiguration().getFullProperties().get("hive-env").get("javax.jdo.option.ConnectionURL");
    assertEquals("jdbc:mysql://myHost.com/hive?createDatabaseIfNotExist=true", updatedVal);
  }

  @Test
  public void testExcludedPropertiesShouldBeAddedWhenServiceIsInBlueprint() throws Exception {
    reset(stack);

    // defaults from init() method that we need
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();

    // customized stack calls for this test only
    expect(stack.getExcludedConfigurationTypes("FALCON")).andReturn(Collections.singleton("oozie-site"));
    expect(stack.getExcludedConfigurationTypes("OOZIE")).andReturn(Collections.emptySet());
    expect(stack.getConfigurationProperties("FALCON", "oozie-site")).andReturn(Collections.singletonMap("oozie.service.ELService.ext.functions.coord-job-submit-instances", "testValue")).anyTimes();
    expect(stack.getServiceForConfigType("oozie-site")).andReturn("OOZIE").anyTimes();

    Map<String, Map<String, String>> properties = new HashMap<>();
    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("FALCON_SERVER");
    hgComponents.add("FALCON_CLIENT");
    hgComponents.add("OOZIE_SERVER");
    hgComponents.add("OOZIE_CLIENT");
    List<String> hosts = new ArrayList<>();
    hosts.add("c6401.apache.ambari.org");
    hosts.add("serverTwo");
    TestHostGroup group1 = new TestHostGroup("host_group_1", hgComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    assertEquals("Falcon Broker URL property not properly exported",
      "testValue", clusterConfig.getPropertyValue("oozie-site", "oozie.service.ELService.ext.functions.coord-job-submit-instances"));
  }

  @Test
  public void testExcludedPropertiesShouldBeIgnoredWhenServiceIsNotInBlueprint() throws Exception {
    reset(stack);

    // defaults from init() method that we need
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();

    // customized stack calls for this test only
    expect(stack.getExcludedConfigurationTypes("FALCON")).andReturn(Collections.singleton("oozie-site")).anyTimes();
    expect(stack.getConfigurationProperties("FALCON", "oozie-site")).andReturn(Collections.singletonMap("oozie.service.ELService.ext.functions.coord-job-submit-instances", "testValue")).anyTimes();
    expect(stack.getServiceForConfigType("oozie-site")).andReturn("OOZIE").anyTimes();

    Map<String, Map<String, String>> properties = new HashMap<>();
    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("FALCON_SERVER");
    hgComponents.add("FALCON_CLIENT");
    List<String> hosts = new ArrayList<>();
    hosts.add("c6401.apache.ambari.org");
    hosts.add("serverTwo");
    TestHostGroup group1 = new TestHostGroup("host_group_1", hgComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    Assert.assertNull("Excluded properties shouldn't be added in this setup!", clusterConfig.getPropertyValue("oozie-site", "oozie.service.ELService.ext.functions.coord-job-submit-instances"));
  }

  @Test
  public void testAddExcludedPropertiesAreOverwrittenByBlueprintConfigs() throws Exception {
    reset(stack);

    // defaults from init() method that we need
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata(anyObject(String.class), anyObject(String.class))).andReturn(emptyMap()).anyTimes();

    // customized stack calls for this test only
    expect(stack.getExcludedConfigurationTypes("FALCON")).andReturn(Collections.singleton("oozie-site")).anyTimes();
    expect(stack.getConfigurationProperties("FALCON", "oozie-site")).andReturn(Collections.singletonMap("oozie.service.ELService.ext.functions.coord-job-submit-instances", "testValue")).anyTimes();

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("oozie.service.ELService.ext.functions.coord-job-submit-instances", "overridedValue");
    properties.put("oozie-site", typeProps);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("FALCON_SERVER");
    hgComponents.add("FALCON_CLIENT");
    List<String> hosts = new ArrayList<>();
    hosts.add("c6401.apache.ambari.org");
    hosts.add("serverTwo");
    TestHostGroup group1 = new TestHostGroup("host_group_1", hgComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    assertEquals("Falcon Broker URL property not properly exported",
      "overridedValue", clusterConfig.getPropertyValue("oozie-site", "oozie.service.ELService.ext.functions.coord-job-submit-instances"));
  }

  @Test
  public void testExcludedPropertiesHandlingWhenExcludedConfigServiceIsNotFoundInStack() throws Exception {
    reset(stack);

    // defaults from init() method that we need
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();

    // customized stack calls for this test only
    Set<String> excludedConfigTypes = new HashSet<>();
    excludedConfigTypes.add("oozie-site");
    excludedConfigTypes.add("storm-site");
    expect(stack.getExcludedConfigurationTypes("FALCON")).andReturn(excludedConfigTypes);
    expect(stack.getExcludedConfigurationTypes("OOZIE")).andReturn(Collections.emptySet());
    expect(stack.getConfigurationProperties("FALCON", "oozie-site")).andReturn(Collections.singletonMap("oozie.service.ELService.ext.functions.coord-job-submit-instances", "testValue")).anyTimes();
    expect(stack.getServiceForConfigType("oozie-site")).andReturn("OOZIE").anyTimes();
    // simulate the case where the STORM service has been removed manually from the stack definitions
    expect(stack.getServiceForConfigType("storm-site")).andThrow(new IllegalArgumentException("TEST: Configuration not found in stack definitions!"));

    Map<String, Map<String, String>> properties = new HashMap<>();
    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("FALCON_SERVER");
    hgComponents.add("FALCON_CLIENT");
    hgComponents.add("OOZIE_SERVER");
    hgComponents.add("OOZIE_CLIENT");
    List<String> hosts = new ArrayList<>();
    hosts.add("c6401.apache.ambari.org");
    hosts.add("serverTwo");
    TestHostGroup group1 = new TestHostGroup("host_group_1", hgComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    assertEquals("Falcon Broker URL property not properly exported",
      "testValue", clusterConfig.getPropertyValue("oozie-site", "oozie.service.ELService.ext.functions.coord-job-submit-instances"));
  }

  @Test
  public void testFalconConfigClusterUpdate() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";


    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> falconStartupProperties = new HashMap<>();
    properties.put("falcon-startup.properties", falconStartupProperties);

    // setup properties that include host information
    falconStartupProperties.put("*.broker.url", createExportedAddress(expectedPortNum, expectedHostGroupName));
    falconStartupProperties.put("*.falcon.service.authentication.kerberos.principal", "falcon/" + createExportedHostName(expectedHostGroupName) + "@EXAMPLE.COM");
    falconStartupProperties.put("*.falcon.http.authentication.kerberos.principal", "HTTP/" + createExportedHostName(expectedHostGroupName) + "@EXAMPLE.COM");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("FALCON_SERVER");
    hgComponents.add("FALCON_CLIENT");
    List<String> hosts = new ArrayList<>();
    hosts.add("c6401.apache.ambari.org");
    hosts.add("server-two");
    TestHostGroup group1 = new TestHostGroup("host_group_1", hgComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    assertEquals("Falcon Broker URL property not properly exported",
      expectedHostName + ":" + expectedPortNum, falconStartupProperties.get("*.broker.url"));

  }

  @Test
  //todo: fails because there are multiple hosts mapped to the hostgroup
  //todo: but error message states that the component is mapped to 0 or multiple host groups
  //todo: This test fails now but passed before
  public void testFalconConfigClusterUpdateDefaultConfig() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> falconStartupProperties = new HashMap<>();
    properties.put("falcon-startup.properties", falconStartupProperties);

    // setup properties that include host information
    falconStartupProperties.put("*.broker.url", "localhost:" + expectedPortNum);
    falconStartupProperties.put("*.falcon.service.authentication.kerberos.principal", "falcon/" + "localhost" + "@EXAMPLE.COM");
    falconStartupProperties.put("*.falcon.http.authentication.kerberos.principal", "HTTP/" + "localhost" + "@EXAMPLE.COM");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("FALCON_SERVER");
    hgComponents.add("FALCON_CLIENT");
    List<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add("server-two");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    updater.doUpdateForClusterCreate();

    assertEquals("Falcon Broker URL property not properly exported",
      expectedHostName + ":" + expectedPortNum, falconStartupProperties.get("*.broker.url"));
  }

  @Test
  public void testHiveConfigClusterUpdateCustomValue() throws Exception {
    final String expectedHostGroupName = "host_group_1";

    final String expectedPropertyValue =
      "hive.metastore.local=false,hive.metastore.uris=thrift://headnode0.ivantestcluster2-ssh.d1.internal.cloudapp.net:9083,hive.user.install.directory=/user";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> webHCatSiteProperties = new HashMap<>();
    properties.put("webhcat-site", webHCatSiteProperties);

    // setup properties that include host information
    webHCatSiteProperties.put("templeton.hive.properties", expectedPropertyValue);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    List<String> hosts = new ArrayList<>();
    hosts.add("some-hose");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);


    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level cluster config update method
    updater.doUpdateForClusterCreate();

    assertEquals("Unexpected config update for templeton.hive.properties",
      expectedPropertyValue,
      webHCatSiteProperties.get("templeton.hive.properties"));
  }

  @Test
  public void testHiveConfigClusterUpdatePropertiesFilterAuthenticationOff() throws Exception {
    // reset the stack mock, since we need more than the default behavior for this test
    reset(stack);

    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hiveSiteProperties = new HashMap<>();
    properties.put("hive-site", hiveSiteProperties);

    // setup properties for Hive to simulate the case of Hive Authentication being off
    hiveSiteProperties.put("hive.server2.authentication", "NONE");
    hiveSiteProperties.put("hive.server2.authentication.kerberos.keytab", " ");
    hiveSiteProperties.put("hive.server2.authentication.kerberos.principal", " ");

    Map<String, Stack.ConfigProperty> mapOfMetadata =
      new HashMap<>();

    // simulate the stack dependencies for these Hive properties, that are dependent upon
    // hive.server2.authorization being enabled
    Stack.ConfigProperty configProperty1 =
      new Stack.ConfigProperty("hive-site", "hive.server2.authentication.kerberos.keytab", " ") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hive-site", "hive.server2.authentication");
          return Collections.singleton(dependencyInfo);
        }
      };

    Stack.ConfigProperty configProperty2 =
      new Stack.ConfigProperty("hive-site", "hive.server2.authentication.kerberos.principal", " ") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hive-site", "hive.server2.authentication");
          return Collections.singleton(dependencyInfo);
        }
      };

    mapOfMetadata.put("hive.server2.authentication.kerberos.keytab", configProperty1);
    mapOfMetadata.put("hive.server2.authentication.kerberos.principal", configProperty2);

    // defaults from init() method that we need
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();
    Set<String> emptySet = Collections.emptySet();
    expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();

    // customized stack calls for this test only
    expect(stack.getServiceForConfigType("hive-site")).andReturn("HIVE").atLeastOnce();
    expect(stack.getConfigurationPropertiesWithMetadata("HIVE", "hive-site")).andReturn(mapOfMetadata).atLeastOnce();

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    List<String> hosts = new ArrayList<>();
    hosts.add("some-hose");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);


    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level cluster config update method
    updater.doUpdateForClusterCreate();

    assertFalse("hive.server2.authentication.kerberos.keytab should have been filtered out of configuration",
      hiveSiteProperties.containsKey("hive.server2.authentication.kerberos.keytab"));
    assertFalse("hive.server2.authentication.kerberos.principal should have been filtered out of configuration",
      hiveSiteProperties.containsKey("hive.server2.authentication.kerberos.principal"));
  }

  @Test
  public void testHiveConfigClusterUpdatePropertiesFilterAuthenticationOffFilterThrowsError() throws Exception {
    // reset the stack mock, since we need more than the default behavior for this test
    reset(stack);

    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hiveSiteProperties = new HashMap<>();
    properties.put("hive-site", hiveSiteProperties);

    // setup properties for Hive to simulate the case of Hive Authentication being off
    hiveSiteProperties.put("hive.server2.authentication", "NONE");
    hiveSiteProperties.put("hive.server2.authentication.kerberos.keytab", " ");
    hiveSiteProperties.put("hive.server2.authentication.kerberos.principal", " ");

    Map<String, Stack.ConfigProperty> mapOfMetadata =
      new HashMap<>();

    // simulate the stack dependencies for these Hive properties, that are dependent upon
    // hive.server2.authorization being enabled
    Stack.ConfigProperty configProperty1 =
      new Stack.ConfigProperty("hive-site", "hive.server2.authentication.kerberos.keytab", " ") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hive-site", "hive.server2.authentication");
          return Collections.singleton(dependencyInfo);
        }
      };

    Stack.ConfigProperty configProperty2 =
      new Stack.ConfigProperty("hive-site", "hive.server2.authentication.kerberos.principal", " ") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hive-site", "hive.server2.authentication");
          return Collections.singleton(dependencyInfo);
        }
      };

    mapOfMetadata.put("hive.server2.authentication.kerberos.keytab", configProperty1);
    mapOfMetadata.put("hive.server2.authentication.kerberos.principal", configProperty2);

    // defaults from init() method that we need
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();
    Set<String> emptySet = Collections.emptySet();
    expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();

    // customized stack calls for this test only
    // simulate the case of the stack object throwing a RuntimeException, to indicate a config error
    expect(stack.getServiceForConfigType("hive-site")).andThrow(new RuntimeException("Expected Test Error")).once();
    expect(stack.getConfigurationPropertiesWithMetadata("HIVE", "hive-site")).andReturn(mapOfMetadata).atLeastOnce();

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    List<String> hosts = new ArrayList<>();
    hosts.add("some-hose");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);


    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level cluster config update method
    updater.doUpdateForClusterCreate();

    assertTrue("hive.server2.authentication.kerberos.keytab should not have been filtered, due to error condition",
      hiveSiteProperties.containsKey("hive.server2.authentication.kerberos.keytab"));
    assertTrue("hive.server2.authentication.kerberos.principal should not have been filtered, due to error condition",
      hiveSiteProperties.containsKey("hive.server2.authentication.kerberos.principal"));
  }

  @Test
  public void testHiveConfigClusterUpdatePropertiesFilterAuthenticationOn() throws Exception {
    // reset the stack mock, since we need more than the default behavior for this test
    reset(stack);

    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hiveSiteProperties = new HashMap<>();
    properties.put("hive-site", hiveSiteProperties);

    // setup properties for Hive to simulate the case of Hive Authentication being on,
    // and set to KERBEROS
    hiveSiteProperties.put("hive.server2.authentication", "KERBEROS");
    hiveSiteProperties.put("hive.server2.authentication.kerberos.keytab", " ");
    hiveSiteProperties.put("hive.server2.authentication.kerberos.principal", " ");

    Map<String, Stack.ConfigProperty> mapOfMetadata =
      new HashMap<>();

    // simulate the stack dependencies for these Hive properties, that are dependent upon
    // hive.server2.authorization being enabled
    Stack.ConfigProperty configProperty1 =
      new Stack.ConfigProperty("hive-site", "hive.server2.authentication.kerberos.keytab", " ") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hive-site", "hive.server2.authentication");
          return Collections.singleton(dependencyInfo);
        }
      };

    Stack.ConfigProperty configProperty2 =
      new Stack.ConfigProperty("hive-site", "hive.server2.authentication.kerberos.principal", " ") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hive-site", "hive.server2.authentication");
          return Collections.singleton(dependencyInfo);
        }
      };

    mapOfMetadata.put("hive.server2.authentication.kerberos.keytab", configProperty1);
    mapOfMetadata.put("hive.server2.authentication.kerberos.principal", configProperty2);

    // defaults from init() method that we need
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();
    Set<String> emptySet = Collections.emptySet();
    expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();

    // customized stack calls for this test only
    expect(stack.getServiceForConfigType("hive-site")).andReturn("HIVE").atLeastOnce();
    expect(stack.getConfigurationPropertiesWithMetadata("HIVE", "hive-site")).andReturn(mapOfMetadata).atLeastOnce();

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    List<String> hosts = new ArrayList<>();
    hosts.add("some-hose");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);


    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level cluster config update method
    updater.doUpdateForClusterCreate();

    assertTrue("hive.server2.authentication.kerberos.keytab should have been included in configuration",
      hiveSiteProperties.containsKey("hive.server2.authentication.kerberos.keytab"));
    assertTrue("hive.server2.authentication.kerberos.principal should have been included in configuration",
      hiveSiteProperties.containsKey("hive.server2.authentication.kerberos.principal"));
  }

  @Test
  public void testHBaseConfigClusterUpdatePropertiesFilterAuthorizationOff() throws Exception {
    // reset the stack mock, since we need more than the default behavior for this test
    reset(stack);

    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    properties.put("hbase-site", hbaseSiteProperties);

    // setup properties for HBase to simulate the case of authorization being off
    hbaseSiteProperties.put("hbase.security.authorization", "false");
    hbaseSiteProperties.put("hbase.coprocessor.regionserver.classes", " ");
    hbaseSiteProperties.put("hbase.coprocessor.master.classes", "");

    Map<String, Stack.ConfigProperty> mapOfMetadata =
      new HashMap<>();

    // simulate the stack dependencies for these Hive properties, that are dependent upon
    // hbase.security.authorization being enabled
    Stack.ConfigProperty configProperty1 =
      new Stack.ConfigProperty("hbase-site", "hbase.coprocessor.regionserver.classes", " ") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hbase-site", "hbase.security.authorization");
          return Collections.singleton(dependencyInfo);
        }
      };

    Stack.ConfigProperty configProperty2 =
      new Stack.ConfigProperty("hbase-site", "hbase.coprocessor.master.classes", "") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hbase-site", "hbase.security.authorization");
          return Collections.singleton(dependencyInfo);
        }
      };

    Stack.ConfigProperty configProperty3 =
      new Stack.ConfigProperty("hbase-site", "hbase.coprocessor.region.classes", "") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hbase-site", "hbase.security.authorization");
          return Collections.singleton(dependencyInfo);
        }
      };

    mapOfMetadata.put("hbase.coprocessor.regionserver.classes", configProperty1);
    mapOfMetadata.put("hbase.coprocessor.master.classes", configProperty2);
    mapOfMetadata.put("hbase.coprocessor.region.classes", configProperty3);

    // defaults from init() method that we need
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();

    Set<String> emptySet = Collections.emptySet();
    expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();

    // customized stack calls for this test only
    expect(stack.getServiceForConfigType("hbase-site")).andReturn("HBASE").atLeastOnce();
    expect(stack.getConfigurationPropertiesWithMetadata("HBASE", "hbase-site")).andReturn(mapOfMetadata).atLeastOnce();

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    List<String> hosts = new ArrayList<>();
    hosts.add("some-hose");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);


    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level cluster config update method
    updater.doUpdateForClusterCreate();

    assertFalse("hbase.coprocessor.regionserver.classes should have been filtered out of configuration",
      hbaseSiteProperties.containsKey("hbase.coprocessor.regionserver.classes"));
    assertTrue("hbase.coprocessor.master.classes should not have been filtered out of configuration",
      hbaseSiteProperties.containsKey("hbase.coprocessor.master.classes"));
    assertTrue("hbase.coprocessor.region.classes should not have been filtered out of configuration",
      hbaseSiteProperties.containsKey("hbase.coprocessor.master.classes"));
  }

  @Test
  public void testHBaseConfigClusterUpdatePropertiesFilterAuthorizationOn() throws Exception {
    // reset the stack mock, since we need more than the default behavior for this test
    reset(stack);

    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    properties.put("hbase-site", hbaseSiteProperties);

    // setup properties for HBase to simulate the case of authorization being off
    hbaseSiteProperties.put("hbase.security.authorization", "true");
    hbaseSiteProperties.put("hbase.coprocessor.regionserver.classes", " ");

    Map<String, Stack.ConfigProperty> mapOfMetadata =
      new HashMap<>();

    // simulate the stack dependencies for these Hive properties, that are dependent upon
    // hive.server2.authorization being enabled
    Stack.ConfigProperty configProperty1 =
      new Stack.ConfigProperty("hbase-site", "hbase.coprocessor.regionserver.classes", " ") {
        @Override
        Set<PropertyDependencyInfo> getDependsOnProperties() {
          PropertyDependencyInfo dependencyInfo = new PropertyDependencyInfo("hbase-site", "hbase.security.authorization");
          return Collections.singleton(dependencyInfo);
        }
      };

    mapOfMetadata.put("hbase.coprocessor.regionserver.classes", configProperty1);

    // defaults from init() method that we need
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();
    Set<String> emptySet = Collections.emptySet();
    expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();

    // customized stack calls for this test only
    expect(stack.getServiceForConfigType("hbase-site")).andReturn("HBASE").atLeastOnce();
    expect(stack.getConfigurationPropertiesWithMetadata("HBASE", "hbase-site")).andReturn(mapOfMetadata).atLeastOnce();

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    List<String> hosts = new ArrayList<>();
    hosts.add("some-hose");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);


    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level cluster config update method
    updater.doUpdateForClusterCreate();

    assertTrue("hbase.coprocessor.regionserver.classes should have been included in configuration",
      hbaseSiteProperties.containsKey("hbase.coprocessor.regionserver.classes"));

  }

  @Test
  public void testHiveConfigClusterUpdateDefaultValue() throws Exception {
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostName = "c6401.ambari.apache.org";

    final String expectedPropertyValue =
      "hive.metastore.local=false,hive.metastore.uris=thrift://localhost:9933,hive.metastore.sasl.enabled=false";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> webHCatSiteProperties = new HashMap<>();
    properties.put("webhcat-site", webHCatSiteProperties);

    // setup properties that include host information
    webHCatSiteProperties.put("templeton.hive.properties",
      expectedPropertyValue);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("HIVE_METASTORE");
    List<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level cluster config update method
    updater.doUpdateForClusterCreate();

    // verify that the host name for the metastore.uris property has been updated
    assertEquals("Unexpected config update for templeton.hive.properties",
      "hive.metastore.local=false,hive.metastore.uris=thrift://" + expectedHostName + ":9933,hive.metastore.sasl.enabled=false",
      webHCatSiteProperties.get("templeton.hive.properties"));
  }

  @Test
  public void testAtlas() throws Exception {
    final String expectedHostGroupName = "host_group_1";
    final String zkHostGroupName = "zk_host_group";
    final String host1 = "c6401.ambari.apache.org";
    final String host2 = "c6402.ambari.apache.org";
    final String host3 = "c6403.ambari.apache.org";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> atlasProperties = new HashMap<>();
    properties.put("application-properties", atlasProperties);

    // setup properties that include host information
    atlasProperties.put("atlas.kafka.bootstrap.servers", "localhost:6667");
    atlasProperties.put("atlas.kafka.zookeeper.connect", "localhost:2181");
    atlasProperties.put("atlas.graph.index.search.solr.zookeeper-url", "localhost:2181/ambari-solr");
    atlasProperties.put("atlas.graph.storage.hostname", "localhost");
    atlasProperties.put("atlas.audit.hbase.zookeeper.quorum", "localhost");


    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hg1Components = new HashSet<>();
    hg1Components.add("KAFKA_BROKER");
    hg1Components.add("HBASE_MASTER");
    List<String> hosts = new ArrayList<>();
    hosts.add(host1);
    hosts.add(host2);
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hg1Components, hosts);

    // Place ZOOKEEPER_SERVER in separate host group/host other
    // than ATLAS
    Collection<String> zkHostGroupComponents = new HashSet<>();
    zkHostGroupComponents.add("ZOOKEEPER_SERVER");

    TestHostGroup group2 = new TestHostGroup(zkHostGroupName, zkHostGroupComponents, Collections.singletonList(host3));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level cluster config update method
    updater.doUpdateForClusterCreate();

    List<String> hostArray =
      Arrays.asList(atlasProperties.get("atlas.kafka.bootstrap.servers").split(","));
    List<String> expected =
      Arrays.asList("c6401.ambari.apache.org:6667", "c6402.ambari.apache.org:6667");

    Assert.assertTrue(hostArray.containsAll(expected) && expected.containsAll(hostArray));

    hostArray = Arrays.asList(atlasProperties.get("atlas.kafka.zookeeper.connect").split(","));
    expected =
      Arrays.asList("c6403.ambari.apache.org:2181");
    Assert.assertTrue(hostArray.containsAll(expected) && expected.containsAll(hostArray));


    hostArray = Arrays.asList(atlasProperties.get("atlas.graph.index.search.solr.zookeeper-url").split(","));
    expected =
      Arrays.asList("c6403.ambari.apache.org:2181/ambari-solr");
    Assert.assertTrue(hostArray.containsAll(expected) && expected.containsAll(hostArray));

    hostArray = Arrays.asList(atlasProperties.get("atlas.graph.storage.hostname").split(","));
    expected =
      Arrays.asList("c6403.ambari.apache.org");
    Assert.assertTrue(hostArray.containsAll(expected) && expected.containsAll(hostArray));

    hostArray = Arrays.asList(atlasProperties.get("atlas.audit.hbase.zookeeper.quorum").split(","));
    expected =
      Arrays.asList("c6403.ambari.apache.org");
    Assert.assertTrue(hostArray.containsAll(expected) && expected.containsAll(hostArray));
  }

  @Test
  public void testHiveConfigClusterUpdateExportedHostGroupValue() throws Exception {
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostName = "c6401.ambari.apache.org";

    // simulate the case of this property coming from an exported Blueprint
    final String expectedPropertyValue =
      "hive.metastore.local=false,hive.metastore.uris=thrift://%HOSTGROUP::host_group_1%:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true";


    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> webHCatSiteProperties = new HashMap<>();
    properties.put("webhcat-site", webHCatSiteProperties);

    // setup properties that include host information
    webHCatSiteProperties.put("templeton.hive.properties",
      expectedPropertyValue);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("HIVE_METASTORE");
    List<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level cluster config update method
    updater.doUpdateForClusterCreate();

    // verify that the host name for the metastore.uris property has been updated
    assertEquals("Unexpected config update for templeton.hive.properties",
      "hive.metastore.local=false,hive.metastore.uris=thrift://" + expectedHostName + ":9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true",
      webHCatSiteProperties.get("templeton.hive.properties"));
  }

  @Test
  public void testStormAndKafkaConfigClusterUpdateWithoutGangliaServer() throws Exception {
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> stormSiteProperties = new HashMap<>();
    Map<String, String> kafkaBrokerProperties = new HashMap<>();

    properties.put("storm-site", stormSiteProperties);
    properties.put("kafka-broker", kafkaBrokerProperties);

    stormSiteProperties.put("worker.childopts", "localhost");
    stormSiteProperties.put("supervisor.childopts", "localhost");
    stormSiteProperties.put("nimbus.childopts", "localhost");

    kafkaBrokerProperties.put("kafka.ganglia.metrics.host", "localhost");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("HIVE_METASTORE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton("testserver"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    expect(stack.getCardinality("GANGLIA_SERVER")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    updater.doUpdateForClusterCreate();

    // verify that the server name is not replaced, since the GANGLIA_SERVER
    // component is not available
    assertEquals("worker startup settings not properly handled by cluster create",
      "localhost", stormSiteProperties.get("worker.childopts"));

    assertEquals("supervisor startup settings not properly handled by cluster create",
      "localhost", stormSiteProperties.get("supervisor.childopts"));

    assertEquals("nimbus startup settings not properly handled by cluster create",
      "localhost", stormSiteProperties.get("nimbus.childopts"));

    assertEquals("Kafka ganglia host property not properly handled by cluster create",
      "localhost", kafkaBrokerProperties.get("kafka.ganglia.metrics.host"));
  }

  @Test
  public void testStormandKafkaConfigClusterUpdateWithGangliaServer() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> stormSiteProperties = new HashMap<>();
    Map<String, String> kafkaBrokerProperties = new HashMap<>();

    properties.put("storm-site", stormSiteProperties);
    properties.put("kafka-broker", kafkaBrokerProperties);

    stormSiteProperties.put("worker.childopts", "localhost");
    stormSiteProperties.put("supervisor.childopts", "localhost");
    stormSiteProperties.put("nimbus.childopts", "localhost");

    kafkaBrokerProperties.put("kafka.ganglia.metrics.host", "localhost");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("GANGLIA_SERVER");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    updater.doUpdateForClusterCreate();

    // verify that the server name is not replaced, since the GANGLIA_SERVER
    // component is not available
    assertEquals("worker startup settings not properly handled by cluster create",
      expectedHostName, stormSiteProperties.get("worker.childopts"));

    assertEquals("supervisor startup settings not properly handled by cluster create",
      expectedHostName, stormSiteProperties.get("supervisor.childopts"));

    assertEquals("nimbus startup settings not properly handled by cluster create",
      expectedHostName, stormSiteProperties.get("nimbus.childopts"));

    assertEquals("Kafka ganglia host property not properly handled by cluster create",
      expectedHostName, kafkaBrokerProperties.get("kafka.ganglia.metrics.host"));
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeHAEnabled() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "server-two";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);

    // setup hdfs HA config for test
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);

    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));

    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup("host-group-2", hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes =
      updater.doUpdateForClusterCreate();

    // verify that dfs.internal.nameservices was added
    assertEquals("dfs.internal.nameservices wasn't added", expectedNameService, hdfsSiteProperties.get("dfs.internal.nameservices"));

    // verify that the expected hostname was substituted for the host group name in the config
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));

    // verify that the Blueprint config processor has set the internal required properties
    // that determine the active and standby node hostnames for this HA setup.
    // one host should be active and the other standby
    Map<String, String> clusterEnv = clusterConfig.getProperties().get("cluster-env");
    String initialActiveHost = clusterEnv.get("dfs_ha_initial_namenode_active");
    String expectedStandbyHost = null;
    if (initialActiveHost.equals(expectedHostName)) {
      expectedStandbyHost = expectedHostNameTwo;
    } else if (initialActiveHost.equals(expectedHostNameTwo)) {
      expectedStandbyHost = expectedHostName;
    } else {
      fail("Active Namenode hostname was not set correctly");
    }
    assertEquals("Standby Namenode hostname was not set correctly",
      expectedStandbyHost, clusterEnv.get("dfs_ha_initial_namenode_standby"));


    assertEquals("fs.defaultFS should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService, coreSiteProperties.get("fs.defaultFS"));

    assertEquals("hbase.rootdir should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/hbase/test/root/dir", hbaseSiteProperties.get("hbase.rootdir"));

    assertEquals("instance.volumes should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes", accumuloSiteProperties.get("instance.volumes"));

    // verify that the non-HA properties are filtered out in HA mode
    assertFalse("dfs.namenode.http-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.http-address"));
    assertFalse("dfs.namenode.https-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.https-address"));
    assertFalse("dfs.namenode.rpc-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.rpc-address"));


    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site"), updatedConfigTypes);
  }


  @Test(expected = ConfigurationTopologyException.class)
  public void testDoUpdateForClusterWithNameNodeHAEnabled_insufficientNameNodes() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "server-two";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);

    // setup hdfs HA config for test
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);

    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));

    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    // Only one namenode, two is expected
    Collection<String> hgComponents = Sets.newHashSet("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = Sets.newHashSet("DATANODE");
    TestHostGroup group2 = new TestHostGroup("host-group-2", hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeHAEnabled_externalNameNodes() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "server-two";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);

    // setup hdfs HA config for test
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);

    // need to set these to fqdn's for external namenodes
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, "externalhost1:" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, "externalhost2:" + expectedPortNum);

    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    // No namenodes. This will not be an error as external namenodes are assumed in this case.
    Collection<String> hgComponents = Sets.newHashSet("JOURNALNODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = Sets.newHashSet("DATANODE");
    TestHostGroup group2 = new TestHostGroup("host-group-2", hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // Zero namenodes means external namenodes are assumed.
    // Validation will pass as namenode rpc addresses are set to external fqdn addresses
    Set<String> updatedConfigTypes = updater.doUpdateForClusterCreate();

    // verify that doNameNodeHAUpdateOnClusterCreation() made no updates in the abscence of namenodes
    assertFalse("dfs.internal.nameservices shouldn't be set", hdfsSiteProperties.containsKey("dfs.internal.nameservices"));
    Map<String, String> clusterEnv = clusterConfig.getProperties().get("cluster-env");
    assertFalse("dfs_ha_initial_namenode_active shouldn't be set", clusterEnv.containsKey("dfs_ha_initial_namenode_active"));
    assertFalse("dfs_ha_initial_namenode_standby shouldn't be set", clusterEnv.containsKey("dfs_ha_initial_namenode_standby"));

    assertEquals("fs.defaultFS should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService, coreSiteProperties.get("fs.defaultFS"));

    assertEquals("hbase.rootdir should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/hbase/test/root/dir", hbaseSiteProperties.get("hbase.rootdir"));

    assertEquals("instance.volumes should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes", accumuloSiteProperties.get("instance.volumes"));

    // verify that the non-HA properties are filtered out in HA mode
    assertFalse("dfs.namenode.http-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.http-address"));
    assertFalse("dfs.namenode.https-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.https-address"));
    assertFalse("dfs.namenode.rpc-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.rpc-address"));

    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site"), updatedConfigTypes);
  }

  @Test(expected = ConfigurationTopologyException.class)
  public void testDoUpdateForClusterWithNameNodeHAEnabled_externalNameNodes_invalid() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "server-two";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);

    // setup hdfs HA config for test
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);

    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    // No namenodes. This will not be an error as external namenodes are assumed in this case.
    Collection<String> hgComponents = Sets.newHashSet("JOURNALNODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = Sets.newHashSet("DATANODE");
    TestHostGroup group2 = new TestHostGroup("host-group-2", hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // Zero namenodes means external namenodes are assumed.
    // validation will fail as namenode rpc addresses are not set (must be set to external fqdn's)
    Set<String> updatedConfigTypes = updater.doUpdateForClusterCreate();
  }


  @Test
  public void testDoUpdateForClusterWithNameNodeHAEnabledThreeNameNodes() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "server-two";
    final String expectedHostNameThree = "server-three";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);

    // setup hdfs HA config for test
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);

    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));

    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup("host-group-2", hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup("host-group-3", Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes =
      updater.doUpdateForClusterCreate();

    // verify that dfs.internal.nameservices was added
    assertEquals("dfs.internal.nameservices wasn't added", expectedNameService, hdfsSiteProperties.get("dfs.internal.nameservices"));

    // verify that the expected hostname was substituted for the host group name in the config
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("fs.defaultFS should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService, coreSiteProperties.get("fs.defaultFS"));

    assertEquals("hbase.rootdir should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/hbase/test/root/dir", hbaseSiteProperties.get("hbase.rootdir"));

    assertEquals("instance.volumes should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes", accumuloSiteProperties.get("instance.volumes"));

    // verify that the non-HA properties are filtered out in HA mode
    assertFalse("dfs.namenode.http-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.http-address"));
    assertFalse("dfs.namenode.https-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.https-address"));
    assertFalse("dfs.namenode.rpc-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.rpc-address"));


    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site"), updatedConfigTypes);
  }


  @Test
  public void testDoUpdateForClusterWithNameNodeFederationEnabled() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedNameServiceTwo = "mynameservicetwo";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedHostNameThree = "c6403.apache.ambari.org";
    final String expectedHostNameFour = "c6404.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedNodeThree = "nn3";
    final String expectedNodeFour = "nn4";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedHostGroupNameThree = "host-group-3";
    final String expectedHostGroupNameFour = "host-group-4";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);

    // setup multiple nameservices, to indicate NameNode Federation will be used
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeThree + "," + expectedNodeFour);

    //setup nameservice-specific properties
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameService,
                           "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns1");
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameServiceTwo,
                           "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns2");


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));



    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));


    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third and fourth hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup(expectedHostGroupNameThree, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));
    TestHostGroup group4 = new TestHostGroup(expectedHostGroupNameFour, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameFour));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes =
      updater.doUpdateForClusterCreate();

    // verify that dfs.internal.nameservices was added
    assertEquals("dfs.internal.nameservices wasn't added", expectedNameService + "," + expectedNameServiceTwo, hdfsSiteProperties.get("dfs.internal.nameservices"));

    // verify that the expected hostname was substituted for the host group name in the config
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("servicerpc-address property not handled properly",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("servicerpc-address property not handled properly",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeTwo));



    assertEquals("fs.defaultFS should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService, coreSiteProperties.get("fs.defaultFS"));

    assertEquals("hbase.rootdir should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/hbase/test/root/dir", hbaseSiteProperties.get("hbase.rootdir"));

    assertEquals("instance.volumes should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes", accumuloSiteProperties.get("instance.volumes"));

    // verify that the non-HA properties are filtered out in HA mode
    assertFalse("dfs.namenode.http-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.http-address"));
    assertFalse("dfs.namenode.https-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.https-address"));
    assertFalse("dfs.namenode.rpc-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.rpc-address"));

    // verify that the namservice-specific shared.edits properties are handled correctly
    // expect that all servers are included in the updated config, and that the qjournal URL format is preserved
    assertEquals("HDFS HA shared edits directory property not properly updated for cluster create.",
      "qjournal://" + createHostAddress(expectedHostName, expectedPortNum) + ";" + createHostAddress(expectedHostNameTwo, expectedPortNum) + "/ns1",
      hdfsSiteProperties.get("dfs.namenode.shared.edits.dir" + "." + expectedNameService));

    // expect that all servers are included in the updated config, and that the qjournal URL format is preserved
    assertEquals("HDFS HA shared edits directory property not properly updated for cluster create.",
      "qjournal://" + createHostAddress(expectedHostName, expectedPortNum) + ";" + createHostAddress(expectedHostNameTwo, expectedPortNum) + "/ns2",
      hdfsSiteProperties.get("dfs.namenode.shared.edits.dir" + "." + expectedNameServiceTwo));


    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site"), updatedConfigTypes);

    Map<String, String> clusterEnv = clusterConfig.getProperties().get("cluster-env");

    // verify that the standard, single-nameservice HA properties are
    // NOT set in this configuration
    assertFalse("Single-node nameservice config should not have been set",
                clusterEnv.containsKey("dfs_ha_initial_namenode_active"));
    assertFalse("Single-node nameservice config should not have been set",
      clusterEnv.containsKey("dfs_ha_initial_namenode_standby"));

    // verify that the config processor sets the expected properties for
    // the sets of active and standby hostnames for NameNode deployment
    assertTrue("Expected active set not found in hadoop-env",
      clusterEnv.containsKey("dfs_ha_initial_namenode_active_set"));
    assertTrue("Expected standby set not found in hadoop-env",
      clusterEnv.containsKey("dfs_ha_initial_namenode_standby_set"));
    assertTrue("Expected clusterId not found in hadoop-env",
      clusterEnv.containsKey("dfs_ha_initial_cluster_id"));

    // verify that the clusterID is set by default to the cluster name
    assertEquals("Expected clusterId was not set to expected value",
      "clusterName", clusterEnv.get("dfs_ha_initial_cluster_id"));

    // verify that the expected hostnames are included in the active set
    String[] activeHostNames = clusterEnv.get("dfs_ha_initial_namenode_active_set").split(",");
    assertEquals("NameNode active set did not contain the expected number of hosts",
                 2, activeHostNames.length);
    Set<String> setOfActiveHostNames = new HashSet<String>(Arrays.asList(activeHostNames));
    assertTrue("Expected host name not found in the active map",
                setOfActiveHostNames.contains(expectedHostName));
    assertTrue("Expected host name not found in the active map",
                setOfActiveHostNames.contains(expectedHostNameThree));


    // verify that the expected hostnames are included in the standby set
    String[] standbyHostNames = clusterEnv.get("dfs_ha_initial_namenode_standby_set").split(",");
    assertEquals("NameNode standby set did not contain the expected number of hosts",
                 2, standbyHostNames.length);
    Set<String> setOfStandbyHostNames = new HashSet<String>(Arrays.asList(standbyHostNames));
    assertTrue("Expected host name not found in the standby map",
               setOfStandbyHostNames.contains(expectedHostNameTwo));
    assertTrue("Expected host name not found in the standby map",
               setOfStandbyHostNames.contains(expectedHostNameFour));
  }

  @Test(expected = ConfigurationTopologyException.class)
  public void testDoUpdateForClusterWithNameNodeFederationEnabledErrorClusterNameNotFound() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedNameServiceTwo = "mynameservicetwo";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedHostNameThree = "c6403.apache.ambari.org";
    final String expectedHostNameFour = "c6404.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedNodeThree = "nn3";
    final String expectedNodeFour = "nn4";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedHostGroupNameThree = "host-group-3";
    final String expectedHostGroupNameFour = "host-group-4";

    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariContext mockAmbariContext = mockSupport.createMock(AmbariContext.class);

    // configure mock to return null cluster name (error condition)
    expect(mockAmbariContext.getClusterName(1)).andReturn(null).anyTimes();

    mockSupport.replayAll();

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);

    // setup multiple nameservices, to indicate NameNode Federation will be used
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeThree + "," + expectedNodeFour);

    //setup nameservice-specific properties
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameService,
      "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns1");
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameServiceTwo,
      "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns2");


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));



    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));


    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third and fourth hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup(expectedHostGroupNameThree, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));
    TestHostGroup group4 = new TestHostGroup(expectedHostGroupNameFour, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameFour));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups, mockAmbariContext);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeFederationEnabledWithCustomizedActiveStandbyHostSets() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedNameServiceTwo = "mynameservicetwo";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedHostNameThree = "c6403.apache.ambari.org";
    final String expectedHostNameFour = "c6404.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedNodeThree = "nn3";
    final String expectedNodeFour = "nn4";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedHostGroupNameThree = "host-group-3";
    final String expectedHostGroupNameFour = "host-group-4";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);


    // configure the active/standy host lists to a custom set of hostnames
    hadoopEnvProperties.put("dfs_ha_initial_namenode_active_set", "test-server-five,test-server-six");
    hadoopEnvProperties.put("dfs_ha_initial_namenode_standby_set", "test-server-seven,test-server-eight");
    hadoopEnvProperties.put("dfs_ha_initial_cluster_id", "my-custom-cluster-name");


    // setup multiple nameservices, to indicate NameNode Federation will be used
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeThree + "," + expectedNodeFour);


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));

    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));

    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third and fourth hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup(expectedHostGroupNameThree, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));
    TestHostGroup group4 = new TestHostGroup(expectedHostGroupNameFour, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameFour));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes =
      updater.doUpdateForClusterCreate();

    // verify that dfs.internal.nameservices was added
    assertEquals("dfs.internal.nameservices wasn't added", expectedNameService + "," + expectedNameServiceTwo, hdfsSiteProperties.get("dfs.internal.nameservices"));

    // verify that the expected hostname was substituted for the host group name in the config
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostNameTwo + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("fs.defaultFS should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService, coreSiteProperties.get("fs.defaultFS"));

    assertEquals("hbase.rootdir should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/hbase/test/root/dir", hbaseSiteProperties.get("hbase.rootdir"));

    assertEquals("instance.volumes should not be modified by cluster update when NameNode HA is enabled.",
      "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes", accumuloSiteProperties.get("instance.volumes"));

    // verify that the non-HA properties are filtered out in HA mode
    assertFalse("dfs.namenode.http-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.http-address"));
    assertFalse("dfs.namenode.https-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.https-address"));
    assertFalse("dfs.namenode.rpc-address should have been filtered out of this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.rpc-address"));


    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hadoop-env", "hdfs-site"), updatedConfigTypes);

    Map<String, String> clusterEnv = clusterConfig.getProperties().get("cluster-env");

    // verify that the standard, single-nameservice HA properties are
    // NOT set in this configuration
    assertFalse("Single-node nameservice config should not have been set",
      clusterEnv.containsKey("dfs_ha_initial_namenode_active"));
    assertFalse("Single-node nameservice config should not have been set",
      clusterEnv.containsKey("dfs_ha_initial_namenode_standby"));

    // verify that the config processor sets the expected properties for
    // the sets of active and standby hostnames for NameNode deployment
    assertTrue("Expected active set not found in hadoop-env",
      clusterEnv.containsKey("dfs_ha_initial_namenode_active_set"));
    assertTrue("Expected standby set not found in hadoop-env",
      clusterEnv.containsKey("dfs_ha_initial_namenode_standby_set"));
    assertTrue("Expected clusterId not found in hadoop-env",
      clusterEnv.containsKey("dfs_ha_initial_cluster_id"));

    // verify that the clusterID is not set by processor, since user has already customized it
    assertEquals("Expected clusterId was not set to expected value",
      "my-custom-cluster-name", clusterEnv.get("dfs_ha_initial_cluster_id"));

    // verify that the expected hostnames are included in the active set
    String[] activeHostNames = clusterEnv.get("dfs_ha_initial_namenode_active_set").split(",");
    assertEquals("NameNode active set did not contain the expected number of hosts",
      2, activeHostNames.length);
    Set<String> setOfActiveHostNames = new HashSet<String>(Arrays.asList(activeHostNames));
    assertTrue("Expected host name not found in the active map",
      setOfActiveHostNames.contains("test-server-five"));
    assertTrue("Expected host name not found in the active map",
      setOfActiveHostNames.contains("test-server-six"));


    // verify that the expected hostnames are included in the standby set
    String[] standbyHostNames = clusterEnv.get("dfs_ha_initial_namenode_standby_set").split(",");
    assertEquals("NameNode standby set did not contain the expected number of hosts",
      2, standbyHostNames.length);
    Set<String> setOfStandbyHostNames = new HashSet<String>(Arrays.asList(standbyHostNames));
    assertTrue("Expected host name not found in the standby map",
      setOfStandbyHostNames.contains("test-server-seven"));
    assertTrue("Expected host name not found in the standby map",
      setOfStandbyHostNames.contains("test-server-eight"));
  }

  @Test(expected = ConfigurationTopologyException.class)
  public void testDoUpdateForClusterWithNameNodeFederationEnabledErrorRPCAddressNotSpecified() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedNameServiceTwo = "mynameservicetwo";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedHostNameThree = "c6403.apache.ambari.org";
    final String expectedHostNameFour = "c6404.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedNodeThree = "nn3";
    final String expectedNodeFour = "nn4";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedHostGroupNameThree = "host-group-3";
    final String expectedHostGroupNameFour = "host-group-4";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);

    // setup multiple nameservices, to indicate NameNode Federation will be used
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeThree + "," + expectedNodeFour);


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));

    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));

    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third and fourth hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup(expectedHostGroupNameThree, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));
    TestHostGroup group4 = new TestHostGroup(expectedHostGroupNameFour, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameFour));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // this should fail with the expected exception
    Set<String> updatedConfigTypes =
      updater.doUpdateForClusterCreate();
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeHANotEnabled() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "serverTwo";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);

    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    TestHostGroup group2 = new TestHostGroup("host-group-2", hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes =
      updater.doUpdateForClusterCreate();

    // verify that the non-HA properties are not filtered out in a non-HA cluster
    assertTrue("dfs.namenode.http-address should have been included in this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.http-address"));
    assertTrue("dfs.namenode.https-address should have been included in this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.https-address"));
    assertTrue("dfs.namenode.rpc-address should have been included in this HA configuration",
      hdfsSiteProperties.containsKey("dfs.namenode.rpc-address"));

    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site"), updatedConfigTypes);
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeHAEnabledAndActiveNodeSet() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "server-three";
    final String expectedHostNameTwo = "server-four";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);

    // setup hdfs HA config for test
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);

    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));

    // set hadoop-env properties to explicitly configure the initial
    // active and stanbdy namenodes
    hadoopEnvProperties.put("dfs_ha_initial_namenode_active", expectedHostName);
    hadoopEnvProperties.put("dfs_ha_initial_namenode_standby", expectedHostNameTwo);

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    Collection<String> hosts = new ArrayList<>();
    hosts.add(expectedHostName);
    hosts.add(expectedHostNameTwo);
    TestHostGroup group = new TestHostGroup(expectedHostGroupName, hgComponents, hosts);

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    // verify that the expected hostname was substituted for the host group name in the config.
    // all of these dynamic props will be set to the same host in this case where there is a single host group
    // with multiple hosts.  This may not be correct and a Jira is being filed to track this issue.
    String expectedPropertyValue = hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne);
    if (!expectedPropertyValue.equals(expectedHostName + ":" + expectedPortNum) &&
      !expectedPropertyValue.equals(expectedHostNameTwo + ":" + expectedPortNum)) {
      fail("HTTPS address HA property not properly exported");
    }
    assertEquals("HTTPS address HA property not properly exported", expectedPropertyValue,
      hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported", expectedPropertyValue,
      hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported", expectedPropertyValue,
      hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported", expectedPropertyValue,
      hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported", expectedPropertyValue,
      hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));

    // verify that the Blueprint config processor has not overridden
    // the user's configuration to determine the active and
    // standby nodes in this NameNode HA cluster
    Map<String, String> clusterEnv = clusterConfig.getProperties().get("cluster-env");

    assertEquals("Active Namenode hostname was not set correctly",
      expectedHostName, clusterEnv.get("dfs_ha_initial_namenode_active"));

    assertEquals("Standby Namenode hostname was not set correctly",
      expectedHostNameTwo, clusterEnv.get("dfs_ha_initial_namenode_standby"));
  }

  private Map<String, String> defaultStackProps() {
    return Maps.newHashMap(ImmutableMap.of(
      CLUSTER_ENV_STACK_NAME_PROPERTY, STACK_NAME,
      CLUSTER_ENV_STACK_ROOT_PROPERTY, "/usr/" + STACK_NAME,
      CLUSTER_ENV_STACK_TOOLS_PROPERTY, "{ some tools... }",
      CLUSTER_ENV_STACK_FEATURES_PROPERTY, "{ some features... }",
      CLUSTER_ENV_STACK_PACKAGES_PROPERTY, "{ some packages... }"
    ));
  }

  @Test
  public void testSetStackToolsAndFeatures_ClusterEnvDidNotChange() throws Exception {
    defaultClusterEnvProperties.putAll(defaultStackProps());
    Map<String, Map<String, String>> blueprintProps = Maps.newHashMap(ImmutableMap.of(
      "cluster-env", defaultStackProps()
    ));
    Configuration clusterConfig = new Configuration(blueprintProps, emptyMap());

    TestHostGroup group = new TestHostGroup("groups1", Sets.newHashSet("NAMENODE"), ImmutableSet.of("host1"));
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, ImmutableSet.of(group));

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> configTypesUpdated = Sets.newHashSet();
    updater.setStackToolsAndFeatures(clusterConfig, configTypesUpdated);
    assertEquals("cluster-env should NOT have been updated", ImmutableSet.of(), configTypesUpdated);
  }


  @Test
  public void testSetStackToolsAndFeatures_ClusterEnvChanged() throws Exception {
    defaultClusterEnvProperties.putAll(defaultStackProps());
    Map<String, String> blueprintClusterEnv = defaultStackProps();
    // change something to trigger cluter-env added to the changed configs
    blueprintClusterEnv.put(CLUSTER_ENV_STACK_ROOT_PROPERTY, "/opt/" + STACK_NAME);

    Map<String, Map<String, String>> blueprintProps = Maps.newHashMap(ImmutableMap.of(
      "cluster-env", blueprintClusterEnv
    ));
    Configuration clusterConfig = new Configuration(blueprintProps, emptyMap());

    TestHostGroup group = new TestHostGroup("groups1", Sets.newHashSet("NAMENODE"), ImmutableSet.of("host1"));
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, ImmutableSet.of(group));

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> configTypesUpdated = Sets.newHashSet();
    updater.setStackToolsAndFeatures(clusterConfig, configTypesUpdated);
    assertEquals("cluster-env should have been updated", ImmutableSet.of("cluster-env"), configTypesUpdated);
  }

  @Test
  public void testSetStackToolsAndFeatures_ClusterEnvChanged_TrimmedValuesEqual() throws Exception {
    defaultClusterEnvProperties.putAll(defaultStackProps());
    Map<String, String> blueprintClusterEnv = defaultStackProps();
    // This change should not be considered as an update to cluster-env as trimmed values are still equal
    blueprintClusterEnv.put(
      CLUSTER_ENV_STACK_ROOT_PROPERTY,
      blueprintClusterEnv.get(CLUSTER_ENV_STACK_ROOT_PROPERTY) + "       \n");

    Map<String, Map<String, String>> blueprintProps = Maps.newHashMap(ImmutableMap.of(
      "cluster-env", blueprintClusterEnv
    ));
    Configuration clusterConfig = new Configuration(blueprintProps, emptyMap());

    TestHostGroup group = new TestHostGroup("groups1", Sets.newHashSet("NAMENODE"), ImmutableSet.of("host1"));
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, ImmutableSet.of(group));

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> configTypesUpdated = Sets.newHashSet();
    updater.setStackToolsAndFeatures(clusterConfig, configTypesUpdated);
    assertEquals("cluster-env should NOT have been updated", ImmutableSet.of(), configTypesUpdated);
  }


  @Test
  public void testParseNameServices() throws Exception {
    Map<String, String> hdfsSiteConfigMap =
      new HashMap<>();
    hdfsSiteConfigMap.put("dfs.nameservices", "serviceOne");

    // verify that a dfs.internal.nameservices parsing falls back to dfs.nameservices
    String[] result = BlueprintConfigurationProcessor.parseNameServices(hdfsSiteConfigMap);

    assertNotNull("Resulting array was null",
      result);
    assertEquals("Incorrect array size",
      1, result.length);
    assertEquals("Incorrect value for returned name service",
      "serviceOne", result[0]);

    hdfsSiteConfigMap.put("dfs.internal.nameservices", "serviceTwo");

    // verify that a single service is parsed correctly
    result = BlueprintConfigurationProcessor.parseNameServices(hdfsSiteConfigMap);

    assertNotNull("Resulting array was null",
      result);
    assertEquals("Incorrect array size",
      1, result.length);
    assertEquals("Incorrect value for returned name service",
      "serviceTwo", result[0]);

    // verify that multiple services are parsed correctly
    hdfsSiteConfigMap.put("dfs.internal.nameservices", " serviceTwo, serviceThree, serviceFour");

    String[] resultTwo = BlueprintConfigurationProcessor.parseNameServices(hdfsSiteConfigMap);

    assertNotNull("Resulting array was null",
      resultTwo);
    assertEquals("Incorrect array size",
      3, resultTwo.length);
    assertEquals("Incorrect value for returned name service",
      "serviceTwo", resultTwo[0]);
    assertEquals("Incorrect value for returned name service",
      "serviceThree", resultTwo[1]);
    assertEquals("Incorrect value for returned name service",
      "serviceFour", resultTwo[2]);
  }

  @Test
  public void testParseNameNodes() throws Exception {
    final String expectedServiceName = "serviceOne";
    Map<String, String> hdfsSiteConfigMap =
      new HashMap<>();
    hdfsSiteConfigMap.put("dfs.ha.namenodes." + expectedServiceName, "node1");

    // verify that a single name node is parsed correctly
    String[] result =
      BlueprintConfigurationProcessor.parseNameNodes(expectedServiceName, hdfsSiteConfigMap);

    assertNotNull("Resulting array was null",
      result);
    assertEquals("Incorrect array size",
      1, result.length);
    assertEquals("Incorrect value for returned name nodes",
      "node1", result[0]);

    // verify that multiple name nodes are parsed correctly
    hdfsSiteConfigMap.put("dfs.ha.namenodes." + expectedServiceName, " nodeSeven, nodeEight, nodeNine");

    String[] resultTwo =
      BlueprintConfigurationProcessor.parseNameNodes(expectedServiceName, hdfsSiteConfigMap);

    assertNotNull("Resulting array was null",
      resultTwo);
    assertEquals("Incorrect array size",
      3, resultTwo.length);
    assertEquals("Incorrect value for returned name node",
      "nodeSeven", resultTwo[0]);
    assertEquals("Incorrect value for returned name node",
      "nodeEight", resultTwo[1]);
    assertEquals("Incorrect value for returned name node",
      "nodeNine", resultTwo[2]);
  }

  @Test
  public void testHDFSConfigClusterUpdateQuorumJournalURL() throws Exception {
    final String expectedHostNameOne = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    properties.put("hdfs-site", hdfsSiteProperties);

    // setup properties that include host information
    // setup shared edit property, that includes a qjournal URL scheme
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir", "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/mycluster");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents1, Collections.singleton(expectedHostNameOne));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    updater.doUpdateForClusterCreate();

    // expect that all servers are included in the updated config, and that the qjournal URL format is preserved
    assertEquals("HDFS HA shared edits directory property not properly updated for cluster create.",
      "qjournal://" + createHostAddress(expectedHostNameOne, expectedPortNum) + ";" + createHostAddress(expectedHostNameTwo, expectedPortNum) + "/mycluster",
      hdfsSiteProperties.get("dfs.namenode.shared.edits.dir"));
  }

  @Test
  public void testHDFSConfigClusterUpdateQuorumJournalURL_UsingMinusSymbolInHostName() throws Exception {
    final String expectedHostNameOne = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host-group-1";
    final String expectedHostGroupNameTwo = "host-group-2";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    properties.put("hdfs-site", hdfsSiteProperties);

    // setup properties that include host information
    // setup shared edit property, that includes a qjournal URL scheme
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir", "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/mycluster");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents1, Collections.singleton(expectedHostNameOne));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    updater.doUpdateForClusterCreate();

    // expect that all servers are included in the updated config, and that the qjournal URL format is preserved
    assertEquals("HDFS HA shared edits directory property not properly updated for cluster create.",
      "qjournal://" + createHostAddress(expectedHostNameOne, expectedPortNum) + ";" + createHostAddress(expectedHostNameTwo, expectedPortNum) + "/mycluster",
      hdfsSiteProperties.get("dfs.namenode.shared.edits.dir"));
  }

  @Test
  public void testHadoopHaNameNode() throws Exception {
    // Given
    final String configType = "cluster-env";
    Map<String, Map<String, String>> properties = new HashMap<>();

    // enable HA
    Map<String, String> hdfsSite = new HashMap<>();
    hdfsSite.put("dfs.nameservices", "mycluster");
    hdfsSite.put("dfs.ha.namenodes.mycluster", "nn1,nn2");
    hdfsSite.put("dfs.namenode.http-address", "%HOSTGROUP::master_1%:50070");
    hdfsSite.put("dfs.namenode.http-address.mycluster.nn1", "%HOSTGROUP::master_1%:50070");
    hdfsSite.put("dfs.namenode.http-address.mycluster.nn2", "%HOSTGROUP::master_2%:50070");
    hdfsSite.put("dfs.namenode.https-address", "%HOSTGROUP::master_1%:50470");
    hdfsSite.put("dfs.namenode.https-address.mycluster.nn1", "%HOSTGROUP::master_1%:50470");
    hdfsSite.put("dfs.namenode.https-address.mycluster.nn2", "%HOSTGROUP::master_2%:50470");
    hdfsSite.put("dfs.namenode.rpc-address.mycluster.nn1", "%HOSTGROUP::master_1%:8020");
    hdfsSite.put("dfs.namenode.rpc-address.mycluster.nn2", "%HOSTGROUP::master_2%:8020");
    hdfsSite.put("dfs.namenode.shared.edits.dir", "qjournal://%HOSTGROUP::master_1%:8485;%HOSTGROUP::master_2%:8485;%HOSTGROUP::master_2%:8485/mycluster");
    hdfsSite.put("dfs.ha.automatic-failover.enabled", "true");
    hdfsSite.put("dfs.ha.fencing.methods", "shell(/bin/true)");
    properties.put("hdfs-site", hdfsSite);

    Map<String, String> hadoopEnv = new HashMap<>();
    hadoopEnv.put("dfs_ha_initial_namenode_active", "%HOSTGROUP::master_1%");
    hadoopEnv.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP::master_2%");
    properties.put("hadoop-env", hadoopEnv);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    TestHostGroup group1 = new TestHostGroup("master_1", ImmutableSet.of("DATANODE", "NAMENODE"), Collections.singleton("node_1"));
    TestHostGroup group2 = new TestHostGroup("master_2", ImmutableSet.of("DATANODE", "NAMENODE"), Collections.singleton("node_2"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("node_1", clusterConfig.getPropertyValue(configType, "dfs_ha_initial_namenode_active"));
    assertEquals("node_2", clusterConfig.getPropertyValue(configType, "dfs_ha_initial_namenode_standby"));
  }

  @Test
  public void testGetRequiredHostGroups___validComponentCountOfZero() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hiveSite = new HashMap<>();
    properties.put("hive-site", hiveSite);
    Map<String, String> hiveEnv = new HashMap<>();
    properties.put("hive-env", hiveEnv);

    hiveSite.put("javax.jdo.option.ConnectionURL", "localhost:1111");
    // not the exact string but we are only looking for "New"
    hiveEnv.put("hive_database", "New Database");


    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("HIVE_SERVER");
    hgComponents1.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    Collection<String> requiredGroups = updater.getRequiredHostGroups();
    assertEquals(0, requiredGroups.size());
  }

  @Test
  public void testGetRequiredHostGroups___invalidComponentCountOfZero() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> coreSiteMap = new HashMap<>();
    properties.put("core-site", coreSiteMap);

    coreSiteMap.put("fs.defaultFS", "localhost");

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("HIVE_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    Collection<String> requiredGroups = updater.getRequiredHostGroups();
    assertEquals(0, requiredGroups.size());
  }

  @Test
  public void testGetRequiredHostGroups___multipleGroups() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> coreSiteMap = new HashMap<>();
    properties.put("core-site", coreSiteMap);

    coreSiteMap.put("fs.defaultFS", "localhost");

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("HIVE_SERVER");
    hgComponents1.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    hgComponents2.add("DATANODE");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    Collection<String> requiredGroups = updater.getRequiredHostGroups();
    assertEquals(2, requiredGroups.size());
    assertTrue(requiredGroups.containsAll(Arrays.asList("group1", "group2")));
  }

  @Test
  public void testGetRequiredHostGroups___defaultUpdater() throws Exception {
    // given
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> coreSiteMap = new HashMap<>();
    coreSiteMap.put("fs.defaultFS", "localhost");
    properties.put("core-site", coreSiteMap);

    properties.put("myservice-site",
      ImmutableMap.of(
        "myservice.slave.urls", "%HOSTGROUP::group4%:8080,%HOSTGROUP::group4%:8080,%HOSTGROUP::group5%:8080"));

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    TestHostGroup group1 = new TestHostGroup("group1",
      ImmutableSet.of("HIVE_SERVER", "NAMENODE"),
      Collections.singleton("host1"));

    TestHostGroup group2 = new TestHostGroup("group2",
      ImmutableSet.of("NAMENODE", "DATANODE"),
      Collections.singleton("host2"));

    Configuration group3Configuration = new Configuration(
      ImmutableMap.of(
        "myservice-site",
        ImmutableMap.of("myservice.master.url", "%HOSTGROUP::group3%:8080")),
      emptyMap());
    TestHostGroup group3 = new TestHostGroup("group3",
      ImmutableSet.of(),
      Collections.singleton("host3"),
      group3Configuration);

    TestHostGroup group4 = new TestHostGroup("group4",
      ImmutableSet.of(),
      ImmutableSet.of("host4a", "host4b"));

    TestHostGroup group5 = new TestHostGroup("group5",
      ImmutableSet.of(),
      Collections.singleton("host5"));

    TestHostGroup group6 = new TestHostGroup("group6",
      ImmutableSet.of(),
      Collections.singleton("host6"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2, group3, group4, group5, group6);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // when
    Set<String> requiredGroups = updater.getRequiredHostGroups();

    // then
    // - group1 and group2 due to the SingleHostTopologyUpdater defined for core-site/fs.defaultFS and NAMENODE
    // - group3 due to myservice-site/myservice.master.url in group3's host group config and the default property updater
    // - group4 and group5 due to myservice-site/myservice.slave.urls in cluster config and the default property updater
    assertEquals(ImmutableSet.of("group1", "group2", "group3", "group4", "group5"), requiredGroups);
  }

  @Test
  public void testAllDefaultUserAndGroupProxyPropertiesSet() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> oozieEnvProperties = new HashMap<>();
    Map<String, String> hiveEnvProperties = new HashMap<>();
    Map<String, String> hbaseEnvProperties = new HashMap<>();
    Map<String, String> falconEnvProperties = new HashMap<>();

    properties.put("oozie-env", oozieEnvProperties);
    properties.put("hive-env", hiveEnvProperties);
    properties.put("hbase-env", hbaseEnvProperties);
    properties.put("falcon-env", falconEnvProperties);

    oozieEnvProperties.put("oozie_user", "test-oozie-user");

    hiveEnvProperties.put("hive_user", "test-hive-user");
    hiveEnvProperties.put("webhcat_user", "test-hcat-user");

    hbaseEnvProperties.put("hbase_user", "test-hbase-user");

    falconEnvProperties.put("falcon_user", "test-falcon-user");

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("DATANODE");
    hgComponents1.add("OOZIE_SERVER");
    hgComponents1.add("HIVE_SERVER");
    hgComponents1.add("HBASE_MASTER");
    hgComponents1.add("FALCON_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-oozie-user.hosts"));
    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-oozie-user.groups"));

    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-hive-user.hosts"));
    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-hive-user.groups"));

    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-hcat-user.hosts"));
    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-hcat-user.groups"));

    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-hbase-user.hosts"));
    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-hbase-user.groups"));

    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-falcon-user.hosts"));
    assertEquals("*", properties.get("core-site").get("hadoop.proxyuser.test-falcon-user.groups"));
  }

  @Test
  public void testRelevantDefaultUserAndGroupProxyPropertiesSet() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> oozieEnvProperties = new HashMap<>();
    Map<String, String> falconEnvProperties = new HashMap<>();

    properties.put("oozie-env", oozieEnvProperties);
    properties.put("falcon-env", falconEnvProperties);

    oozieEnvProperties.put("oozie_user", "test-oozie-user");

    falconEnvProperties.put("falcon_user", "test-falcon-user");

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("DATANODE");
    hgComponents1.add("OOZIE_SERVER");
    hgComponents1.add("FALCON_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    Map<String, String> coreSiteProperties = properties.get("core-site");
    assertEquals(4, coreSiteProperties.size());

    assertEquals("*", coreSiteProperties.get("hadoop.proxyuser.test-oozie-user.hosts"));
    assertEquals("*", coreSiteProperties.get("hadoop.proxyuser.test-oozie-user.groups"));

    assertEquals("*", coreSiteProperties.get("hadoop.proxyuser.test-falcon-user.hosts"));
    assertEquals("*", coreSiteProperties.get("hadoop.proxyuser.test-falcon-user.groups"));
  }

  @Test
  public void testDefaultUserAndGroupProxyPropertiesSetWhenNotProvided() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> oozieEnvProperties = new HashMap<>();
    Map<String, String> falconEnvProperties = new HashMap<>();

    properties.put("core-site", coreSiteProperties);
    properties.put("oozie-env", oozieEnvProperties);
    properties.put("falcon-env", falconEnvProperties);

    coreSiteProperties.put("hadoop.proxyuser.test-oozie-user.hosts", "testOozieHostsVal");
    coreSiteProperties.put("hadoop.proxyuser.test-oozie-user.groups", "testOozieGroupsVal");

    oozieEnvProperties.put("oozie_user", "test-oozie-user");

    falconEnvProperties.put("falcon_user", "test-falcon-user");

    Configuration clusterConfig = new Configuration(properties,
      emptyMap());

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("DATANODE");
    hgComponents1.add("OOZIE_SERVER");
    hgComponents1.add("FALCON_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals(4, coreSiteProperties.size());

    assertEquals("testOozieHostsVal", coreSiteProperties.get("hadoop.proxyuser.test-oozie-user.hosts"));
    assertEquals("testOozieGroupsVal", coreSiteProperties.get("hadoop.proxyuser.test-oozie-user.groups"));

    assertEquals("*", coreSiteProperties.get("hadoop.proxyuser.test-falcon-user.hosts"));
    assertEquals("*", coreSiteProperties.get("hadoop.proxyuser.test-falcon-user.groups"));
  }

  @Test
  public void testDefaultUserAndGroupProxyPropertiesSetWhenNotProvided2() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> falconEnvProperties = new HashMap<>();
    properties.put("falcon-env", falconEnvProperties);
    falconEnvProperties.put("falcon_user", "test-falcon-user");

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Map<String, String> oozieEnvProperties = new HashMap<>();
    parentProperties.put("oozie-env", oozieEnvProperties);
    oozieEnvProperties.put("oozie_user", "test-oozie-user");
    Map<String, String> coreSiteProperties = new HashMap<>();
    parentProperties.put("core-site", coreSiteProperties);
    coreSiteProperties.put("hadoop.proxyuser.test-oozie-user.hosts", "testOozieHostsVal");

    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());

    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("DATANODE");
    hgComponents1.add("OOZIE_SERVER");
    hgComponents1.add("FALCON_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    Map<String, String> leafConfigCoreSiteProps = properties.get("core-site");
    // because "hadoop.proxyuser.test-oozie-user.hosts" is provided in the parent config, it shouldn't be added
    assertEquals(3, leafConfigCoreSiteProps.size());

    // ensure that explicitly set value is unchanged
    assertEquals("testOozieHostsVal", clusterConfig.getPropertyValue("core-site", "hadoop.proxyuser.test-oozie-user.hosts"));

    assertEquals("*", leafConfigCoreSiteProps.get("hadoop.proxyuser.test-oozie-user.groups"));

    assertEquals("*", leafConfigCoreSiteProps.get("hadoop.proxyuser.test-falcon-user.hosts"));
    assertEquals("*", leafConfigCoreSiteProps.get("hadoop.proxyuser.test-falcon-user.groups"));
  }

  @Test
  public void testHiveWithoutAtlas() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hiveProperties = new HashMap<>();
    hiveProperties.put("hive.exec.post.hooks", "");
    properties.put("hive-site", hiveProperties);

    Map<String, String> hiveEnv = new HashMap<>();
    hiveEnv.put("hive.atlas.hook", "false");
    properties.put("hive-env", hiveEnv);


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("HIVE_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals(null, clusterConfig.getPropertyValue("hive-site", "atlas.cluster.name"));
    assertEquals(null, clusterConfig.getPropertyValue("hive-site", "atlas.rest.address"));
  }

  @Test
  public void testAtlasHiveProperties() throws Exception {
    Map<String, Map<String, String>> properties = getAtlasHivePropertiesForTestCase();
    validateAtlasHivePropertiesForTestCase(properties);
  }

  /**
   * If the Hive Exec Hooks property doesn't contain the Atlas Hook, then add it.
   *
   * @throws Exception
   */
  @Test
  public void testAtlasHivePropertiesWithHiveHookSpace() throws Exception {
    Map<String, Map<String, String>> properties = getAtlasHivePropertiesForTestCase();

    Map<String, String> hiveProperties = properties.get("hive-site");
    hiveProperties.put("hive.exec.post.hooks", " ");
    properties.put("hive-site", hiveProperties);
    validateAtlasHivePropertiesForTestCase(properties);
  }

  /***
   * If the Atlas Hook already exists, don't append it.
   * @throws Exception
   */
  @Test
  public void testAtlasHivePropertiesWithAtlasHookAlreadyExist() throws Exception {
    Map<String, Map<String, String>> properties = getAtlasHivePropertiesForTestCase();

    Map<String, String> hiveProperties = properties.get("hive-site");
    hiveProperties.put("hive.exec.post.hooks", "org.apache.atlas.hive.hook.HiveHook");
    properties.put("hive-site", hiveProperties);
    validateAtlasHivePropertiesForTestCase(properties);
  }

  /**
   * Generate sample collection of properties for some of the test cases.
   *
   * @return Map of sample properties
   */
  private Map<String, Map<String, String>> getAtlasHivePropertiesForTestCase() {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> atlasProperties = new HashMap<>();
    atlasProperties.put("atlas.enableTLS", "false");
    atlasProperties.put("atlas.server.bind.address", "localhost");
    atlasProperties.put("atlas.server.http.port", "21000");
    properties.put("application-properties", atlasProperties);

    Map<String, String> atlasEnv = new HashMap<>();
    properties.put("atlas-env", atlasEnv);

    Map<String, String> hiveProperties = new HashMap<>();
    hiveProperties.put("hive.exec.post.hooks", "");
    properties.put("hive-site", hiveProperties);

    Map<String, String> hiveEnv = new HashMap<>();
    properties.put("hive-env", hiveEnv);

    return properties;
  }

  /**
   * For several test cases, validate that org.apache.atlas.hive.hook.HiveHook has the correct value.
   *
   * @param properties Map of properties to validate
   * @throws Exception
   */
  private void validateAtlasHivePropertiesForTestCase(Map<String, Map<String, String>> properties) throws Exception {
    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("ATLAS_SERVER");
    hgComponents1.add("HIVE_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology1 = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology1);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("org.apache.atlas.hive.hook.HiveHook", clusterConfig.getPropertyValue("hive-site", "hive.exec.post.hooks"));
    assertEquals(null, clusterConfig.getPropertyValue("hive-site", "atlas.cluster.name"));
    assertEquals(null, clusterConfig.getPropertyValue("hive-site", "atlas.rest.address"));
    assertEquals("host1", clusterConfig.getPropertyValue("application-properties", "atlas.server.bind.address"));
  }

  @Test
  public void testAtlasHivePropertiesWithHTTPS() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> atlasProperties = new HashMap<>();
    properties.put("application-properties", atlasProperties);
    // use https
    atlasProperties.put("atlas.enableTLS", "true");
    atlasProperties.put("atlas.server.bind.address", "localhost");
    atlasProperties.put("atlas.server.https.port", "99999");
    Map<String, String> atlasEnv = new HashMap<>();

    properties.put("atlas-env", atlasEnv);
    Map<String, String> hiveProperties = new HashMap<>();
    // default hook registered
    hiveProperties.put("hive.exec.post.hooks", "foo");
    properties.put("hive-site", hiveProperties);

    Map<String, String> hiveEnv = new HashMap<>();
    hiveEnv.put("hive.atlas.hook", "false");
    properties.put("hive-env", hiveEnv);


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("ATLAS_SERVER");
    hgComponents1.add("HIVE_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("foo,org.apache.atlas.hive.hook.HiveHook", clusterConfig.getPropertyValue("hive-site", "hive.exec.post.hooks"));
    assertEquals(null, clusterConfig.getPropertyValue("hive-site", "atlas.cluster.name"));
    assertEquals(null, clusterConfig.getPropertyValue("hive-site", "atlas.rest.address"));
  }

  @Test
  public void testStormAmsPropertiesDefault() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> stormSite = new HashMap<>();
    //default
    stormSite.put("metrics.reporter.register", "");
    properties.put("storm-site", stormSite);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("METRICS_COLLECTOR");
    hgComponents1.add("NIMBUS");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter",
      clusterConfig.getPropertyValue("storm-site", "metrics.reporter.register"));
  }

  @Test
  public void testStormAmsPropertiesUserDefinedReporter() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> stormSite = new HashMap<>();
    //default
    stormSite.put("metrics.reporter.register", "user.Reporter");
    properties.put("storm-site", stormSite);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("METRICS_COLLECTOR");
    hgComponents1.add("NIMBUS");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("user.Reporter",
      clusterConfig.getPropertyValue("storm-site", "metrics.reporter.register"));
  }

  @Test
  public void testKafkaAmsProperties() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> stormSite = new HashMap<>();
    //default
    stormSite.put("kafka.metrics.reporters", "");
    properties.put("kafka-broker", stormSite);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("METRICS_COLLECTOR");
    hgComponents1.add("KAFKA_BROKER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter",
      clusterConfig.getPropertyValue("kafka-broker", "kafka.metrics.reporters"));

  }

  @Test
  public void testKafkaAmsPropertiesMultipleReporters() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> stormSite = new HashMap<>();
    //default
    stormSite.put("kafka.metrics.reporters", "user.Reporter");
    properties.put("kafka-broker", stormSite);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("METRICS_COLLECTOR");
    hgComponents1.add("KAFKA_BROKER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("user.Reporter,org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter",
      clusterConfig.getPropertyValue("kafka-broker", "kafka.metrics.reporters"));

  }

  @Test
  public void testRecommendConfiguration_applyStackDefaultsOnly() throws Exception {
    // GIVEN
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> coreSiteMap = new HashMap<>();
    properties.put("core-site", coreSiteMap);
    coreSiteMap.put("fs.default.name", expectedHostName + ":" + expectedPortNum);
    coreSiteMap.put("fs.defaultFS", "hdfs://" + expectedHostName + ":" + expectedPortNum);
    coreSiteMap.put("fs.stackDefault.key2", "dummyValue");

    Map<String, String> dummySiteMap = new HashMap<>();
    properties.put("dummy-site", dummySiteMap);
    dummySiteMap.put("dummy.prop", "dummyValue2");

    Map<String, Map<String, String>> parentProperties = new HashMap<>();

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton(expectedHostGroupName));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    Configuration parentConfig = new Configuration(parentProperties,
      emptyMap(), createStackDefaults());

    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentConfig);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    topology.getAdvisedConfigurations().putAll(createAdvisedConfigMap());
    topology.setConfigRecommendationStrategy(ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    reset(stack);
    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();
    expect(stack.getConfiguration(bp.getServices())).andReturn(createStackDefaults()).anyTimes();

    Set<String> emptySet = Collections.emptySet();
    expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();
    replay(stack);

    // WHEN
    Set<String> configTypeUpdated = configProcessor.doUpdateForClusterCreate();
    // THEN
    assertEquals(expectedHostName + ":" + expectedPortNum, clusterConfig.getPropertyValue("core-site", "fs.default.name"));
    assertEquals("stackDefaultUpgraded", clusterConfig.getPropertyValue("core-site", "fs.stackDefault.key1"));
    // verify that fs.stackDefault.key2 is removed
    assertNull(clusterConfig.getPropertyValue("core-site", "fs.stackDefault.key2"));
    // verify that fs.notStackDefault is filtered out
    assertNull(clusterConfig.getPropertyValue("core-site", "fs.notStackDefault"));
    assertTrue(configTypeUpdated.contains("dummy-site"));
  }

  @Test
  public void testRecommendConfiguration_EmptyConfiguration_applyStackDefaultsOnly() throws Exception {
    // GIVEN
    //final String expectedHostName = "c6401.apache.ambari.org";
    //final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    //Map<String, String> coreSiteMap = new HashMap<String, String>();
    //properties.put("core-site", coreSiteMap);

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton(expectedHostGroupName));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();

    Configuration parentConfig = new Configuration(parentProperties,
      emptyMap(), createStackDefaults());

    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentConfig);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    topology.getAdvisedConfigurations().putAll(createAdvisedConfigMap());
    topology.setConfigRecommendationStrategy(ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    reset(stack);
    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();
    expect(stack.getConfiguration(bp.getServices())).andReturn(createStackDefaults()).anyTimes();

    Set<String> emptySet = Collections.emptySet();
    expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();

    replay(stack);

    // WHEN
    configProcessor.doUpdateForClusterCreate();
    // THEN
    assertEquals("stackDefaultUpgraded", clusterConfig.getPropertyValue("core-site", "fs.stackDefault.key1"));
    // verify that fs.stackDefault.key2 is removed
    assertNull(clusterConfig.getPropertyValue("core-site", "fs.stackDefault.key2"));
    // verify that fs.notStackDefault is filtered out
    assertNull(clusterConfig.getPropertyValue("core-site", "fs.notStackDefault"));
  }

  @Test
  public void testRecommendConfiguration_applyAlways() throws Exception {
    // GIVEN
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> coreSiteMap = new HashMap<>();
    properties.put("core-site", coreSiteMap);
    coreSiteMap.put("fs.default.name", expectedHostName + ":" + expectedPortNum);
    coreSiteMap.put("fs.defaultFS", "hdfs://" + expectedHostName + ":" + expectedPortNum);
    coreSiteMap.put("fs.stackDefault.key2", "dummyValue");

    Map<String, String> dummySiteMap = new HashMap<>();
    properties.put("dummy-site", dummySiteMap);
    dummySiteMap.put("dummy.prop", "dummyValue");
    Map<String, String> dummy2SiteMap = new HashMap<>();
    properties.put("dummy2-site", dummy2SiteMap);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton(expectedHostGroupName));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap(), createStackDefaults());

    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    topology.getAdvisedConfigurations().putAll(createAdvisedConfigMap());
    topology.setConfigRecommendationStrategy(ConfigRecommendationStrategy.ALWAYS_APPLY);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    // WHEN
    Set<String> configTypes = configProcessor.doUpdateForClusterCreate();
    // THEN
    assertEquals(expectedHostName + ":" + expectedPortNum, clusterConfig.getPropertyValue("core-site", "fs.default.name"));
    assertEquals("stackDefaultUpgraded", clusterConfig.getPropertyValue("core-site", "fs.stackDefault.key1"));
    // verify that fs.stackDefault.key2 is removed
    assertNull(clusterConfig.getPropertyValue("core-site", "fs.stackDefault.key2"));
    // verify that fs.notStackDefault is not filtered out
    assertNotNull(clusterConfig.getPropertyValue("core-site", "fs.notStackDefault"));
    assertEquals(3, topology.getAdvisedConfigurations().size());
    assertFalse(configTypes.contains("dummy-site"));
    assertFalse(configTypes.contains("dummy2-site"));
  }

  @Test
  public void testRecommendConfiguration_neverApply() throws Exception {
    // GIVEN
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> coreSiteMap = new HashMap<>();
    properties.put("core-site", coreSiteMap);
    coreSiteMap.put("fs.default.name", expectedHostName + ":" + expectedPortNum);
    coreSiteMap.put("fs.defaultFS", "hdfs://" + expectedHostName + ":" + expectedPortNum);
    coreSiteMap.put("fs.stackDefault.key2", "dummyValue");

    Map<String, Map<String, String>> parentProperties = new HashMap<>();

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton(expectedHostGroupName));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap(), createStackDefaults());

    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    topology.getAdvisedConfigurations().putAll(createAdvisedConfigMap());
    topology.setConfigRecommendationStrategy(ConfigRecommendationStrategy.NEVER_APPLY);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    // WHEN
    configProcessor.doUpdateForClusterCreate();
    // THEN
    assertEquals(expectedHostName + ":" + expectedPortNum, clusterConfig.getPropertyValue("core-site", "fs.default.name"));
    // verify that no any value added/upgraded/removed
    assertNull(clusterConfig.getPropertyValue("core-site", "fs.notStackDefault"));
    assertEquals("stackDefaultValue1", clusterConfig.getPropertyValue("core-site", "fs.stackDefault.key1"));
    assertNotNull(clusterConfig.getPropertyValue("core-site", "fs.stackDefault.key2"));
  }


  @Test
  public void testRangerAdminProperties() throws Exception {
    // Given

    final String rangerAdminConfigType = "admin-properties";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> rangerAdminProperties = new HashMap<>();
    properties.put(rangerAdminConfigType, rangerAdminProperties);
    rangerAdminProperties.put("policymgr_external_url", "http://%HOSTGROUP::group1%:100");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");
    TestHostGroup group1 = new TestHostGroup("group1", rangerComponents, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("http://host1:100", clusterConfig.getPropertyValue(rangerAdminConfigType, "policymgr_external_url"));
  }

  @Test
  public void testRangerAdminProperties_defaults() throws Exception {
    // Given

    final String rangerAdminConfigType = "admin-properties";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> rangerAdminProperties = new HashMap<>();
    properties.put(rangerAdminConfigType, rangerAdminProperties);
    rangerAdminProperties.put("policymgr_external_url", "http://localhost:100");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");
    TestHostGroup group1 = new TestHostGroup("group1", rangerComponents, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("http://host1:100", clusterConfig.getPropertyValue(rangerAdminConfigType, "policymgr_external_url"));
  }

  @Test
  public void testRangerAdminProperties_HA() throws Exception {
    // Given

    final String rangerAdminConfigType = "admin-properties";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> rangerAdminProperties = new HashMap<>();
    properties.put(rangerAdminConfigType, rangerAdminProperties);
    rangerAdminProperties.put("policymgr_external_url", "http://my.ranger.loadbalancer.com");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");
    TestHostGroup group1 = new TestHostGroup("group1", rangerComponents, Collections.singleton("host1"));
    TestHostGroup group2 = new TestHostGroup("group2", rangerComponents, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("http://my.ranger.loadbalancer.com", clusterConfig.getPropertyValue(rangerAdminConfigType, "policymgr_external_url"));
  }

  @Test
  public void testRangerEnv_defaults() throws Exception {
    // Given
    List<String> configTypesWithRangerHdfsAuditDir = ImmutableList.of(
      "ranger-env",
      "ranger-yarn-audit",
      "ranger-hdfs-audit",
      "ranger-hbase-audit",
      "ranger-hive-audit",
      "ranger-knox-audit",
      "ranger-kafka-audit",
      "ranger-storm-audit",
      "ranger-atlas-audit"
    );
    Map<String, Map<String, String>> clusterConfigProperties = new HashMap<>();

    for (String configType : configTypesWithRangerHdfsAuditDir) {
      Map<String, String> configProperties = new HashMap<>();
      configProperties.put("xasecure.audit.destination.hdfs.dir", "hdfs://localhost:100");

      clusterConfigProperties.put(configType, configProperties);
    }


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties, new HashMap<>());
    Configuration clusterConfig = new Configuration(clusterConfigProperties, new HashMap<>(), parentClusterConfig);

    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");

    TestHostGroup group1 = new TestHostGroup("group1", rangerComponents, Collections.singleton("host1"));
    group1.components.add("DATANODE");


    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("nn_host"));


    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    String expectedAuditHdfsDir = "hdfs://nn_host:100";

    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-env", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-yarn-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hdfs-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hbase-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hive-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-knox-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-kafka-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-storm-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-atlas-audit", "xasecure.audit.destination.hdfs.dir"));
  }

  @Test
  public void testRangerEnv_defaults_NO_HDFS() throws Exception {
    // Given
    List<String> configTypesWithRangerHdfsAuditDir = ImmutableList.of(
      "ranger-env",
      "ranger-yarn-audit",
      "ranger-hdfs-audit",
      "ranger-hbase-audit",
      "ranger-hive-audit",
      "ranger-knox-audit",
      "ranger-kafka-audit",
      "ranger-storm-audit",
      "ranger-atlas-audit"
    );
    Map<String, Map<String, String>> clusterConfigProperties = new HashMap<>();

    for (String configType : configTypesWithRangerHdfsAuditDir) {
      Map<String, String> configProperties = new HashMap<>();
      configProperties.put("xasecure.audit.destination.hdfs.dir", "hdfs://localhost:100");

      clusterConfigProperties.put(configType, configProperties);
    }

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties, new HashMap<>());
    Configuration clusterConfig = new Configuration(clusterConfigProperties, new HashMap<>(), parentClusterConfig);

    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");

    TestHostGroup group1 = new TestHostGroup("group1", rangerComponents, Collections.singleton("host1"));
    group1.components.add("OOZIE_SERVER");


    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1+")).anyTimes();

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1);//, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    String expectedAuditHdfsDir = "hdfs://localhost:100";

    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-env", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-yarn-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hdfs-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hbase-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hive-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-knox-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-kafka-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-storm-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-atlas-audit", "xasecure.audit.destination.hdfs.dir"));
  }


  @Test
  public void testRangerEnv() throws Exception {
    // Given
    List<String> configTypesWithRangerHdfsAuditDir = ImmutableList.of(
      "ranger-env",
      "ranger-yarn-audit",
      "ranger-hdfs-audit",
      "ranger-hbase-audit",
      "ranger-hive-audit",
      "ranger-knox-audit",
      "ranger-kafka-audit",
      "ranger-storm-audit",
      "ranger-atlas-audit"
    );
    Map<String, Map<String, String>> clusterConfigProperties = new HashMap<>();

    for (String configType : configTypesWithRangerHdfsAuditDir) {
      Map<String, String> configProperties = new HashMap<>();
      configProperties.put("xasecure.audit.destination.hdfs.dir", "hdfs://%HOSTGROUP::group2%:100");

      clusterConfigProperties.put(configType, configProperties);
    }


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties, new HashMap<>());
    Configuration clusterConfig = new Configuration(clusterConfigProperties, new HashMap<>(), parentClusterConfig);

    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");

    TestHostGroup group1 = new TestHostGroup("group1", rangerComponents, Collections.singleton("host1"));
    group1.components.add("DATANODE");


    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("nn_host"));


    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    String expectedAuditHdfsDir = "hdfs://nn_host:100";

    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-env", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-yarn-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hdfs-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hbase-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hive-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-knox-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-kafka-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-storm-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-atlas-audit", "xasecure.audit.destination.hdfs.dir"));
  }


  @Test
  public void testRangerEnvWithHdfsHA() throws Exception {
    // Given
    List<String> configTypesWithRangerHdfsAuditDir = ImmutableList.of(
      "ranger-env",
      "ranger-yarn-audit",
      "ranger-hdfs-audit",
      "ranger-hbase-audit",
      "ranger-hive-audit",
      "ranger-knox-audit",
      "ranger-kafka-audit",
      "ranger-storm-audit",
      "ranger-atlas-audit"
    );
    Map<String, Map<String, String>> clusterConfigProperties = new HashMap<>();

    for (String configType : configTypesWithRangerHdfsAuditDir) {
      Map<String, String> configProperties = new HashMap<>();
      configProperties.put("xasecure.audit.destination.hdfs.dir", "hdfs://my_name_service:100");

      clusterConfigProperties.put(configType, configProperties);
    }


    // DFS name service
    final String hdfsSiteConfigType = "hdfs-site";
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    clusterConfigProperties.put(hdfsSiteConfigType, hdfsSiteProperties);
    hdfsSiteProperties.put("dfs.nameservices", "my_name_service");
    hdfsSiteProperties.put("dfs.ha.namenodes.my_name_service", "nn1,nn2");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties, new HashMap<>());
    Configuration clusterConfig = new Configuration(clusterConfigProperties, new HashMap<>(), parentClusterConfig);

    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");

    TestHostGroup group1 = new TestHostGroup("group1", rangerComponents, Collections.singleton("host1"));
    group1.components.addAll(hdfsComponents);


    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host2"));


    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    String expectedAuditHdfsDir = "hdfs://my_name_service:100";

    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-env", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-yarn-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hdfs-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hbase-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hive-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-knox-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-kafka-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-storm-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-atlas-audit", "xasecure.audit.destination.hdfs.dir"));
  }


  @Test
  public void testRangerEnvBlueprintExport() throws Exception {
    // Given
    List<String> configTypesWithRangerHdfsAuditDir = ImmutableList.of(
      "ranger-env",
      "ranger-yarn-audit",
      "ranger-hdfs-audit",
      "ranger-hbase-audit",
      "ranger-hive-audit",
      "ranger-knox-audit",
      "ranger-kafka-audit",
      "ranger-storm-audit",
      "ranger-atlas-audit"
    );
    Map<String, Map<String, String>> clusterConfigProperties = new HashMap<>();

    for (String configType : configTypesWithRangerHdfsAuditDir) {
      Map<String, String> configProperties = new HashMap<>();
      configProperties.put("xasecure.audit.destination.hdfs.dir", "hdfs://nn_host:100");

      clusterConfigProperties.put(configType, configProperties);
    }


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties, new HashMap<>());
    Configuration clusterConfig = new Configuration(clusterConfigProperties, new HashMap<>(), parentClusterConfig);

    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");

    TestHostGroup group1 = new TestHostGroup("group1", rangerComponents, Collections.singleton("host1"));
    group1.components.add("DATANODE");


    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("nn_host"));


    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    // Then
    String expectedAuditHdfsDir = "hdfs://%HOSTGROUP::group2%:100";

    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-env", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-yarn-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hdfs-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hbase-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hive-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-knox-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-kafka-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-storm-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-atlas-audit", "xasecure.audit.destination.hdfs.dir"));
  }

  @Test
  public void testRangerEnvExportBlueprintWithHdfsHA() throws Exception {
    // Given
    List<String> configTypesWithRangerHdfsAuditDir = ImmutableList.of(
      "ranger-env",
      "ranger-yarn-audit",
      "ranger-hdfs-audit",
      "ranger-hbase-audit",
      "ranger-hive-audit",
      "ranger-knox-audit",
      "ranger-kafka-audit",
      "ranger-storm-audit",
      "ranger-atlas-audit"
    );
    Map<String, Map<String, String>> clusterConfigProperties = new HashMap<>();

    for (String configType : configTypesWithRangerHdfsAuditDir) {
      Map<String, String> configProperties = new HashMap<>();
      configProperties.put("xasecure.audit.destination.hdfs.dir", "hdfs://my_name_service:100");

      clusterConfigProperties.put(configType, configProperties);
    }

    // DFS name service
    final String hdfsSiteConfigType = "hdfs-site";
    Map<String, String> hdfsSiteProperties = new HashMap<>();
    clusterConfigProperties.put(hdfsSiteConfigType, hdfsSiteProperties);
    hdfsSiteProperties.put("dfs.nameservices", "my_name_service");
    hdfsSiteProperties.put("dfs.ha.namenodes.my_name_service", "nn1,nn2");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties, new HashMap<>());
    Configuration clusterConfig = new Configuration(clusterConfigProperties, new HashMap<>(), parentClusterConfig);

    Collection<String> rangerComponents = new HashSet<>();
    rangerComponents.add("RANGER_ADMIN");
    rangerComponents.add("RANGER_USERSYNC");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");

    TestHostGroup group1 = new TestHostGroup("group1", rangerComponents, Collections.singleton("host1"));
    group1.components.addAll(hdfsComponents);


    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host2"));


    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    // Then
    String expectedAuditHdfsDir = "hdfs://my_name_service:100";

    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-env", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-yarn-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hdfs-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hbase-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-hive-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-knox-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-kafka-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-storm-audit", "xasecure.audit.destination.hdfs.dir"));
    assertEquals(expectedAuditHdfsDir, clusterConfig.getPropertyValue("ranger-atlas-audit", "xasecure.audit.destination.hdfs.dir"));
  }

  @Test
  public void testRangerKmsServerProperties() throws Exception {
    // Given

    final String kmsSiteConfigType = "kms-site";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> kmsSiteProperties = new HashMap<>();
    properties.put(kmsSiteConfigType, kmsSiteProperties);
    kmsSiteProperties.put("hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string",
      createHostAddress("%HOSTGROUP::group1%", "2181") + "," + createHostAddress("%HOSTGROUP::group2%", "2181"));
    kmsSiteProperties.put("hadoop.kms.key.provider.uri", "dbks://http@localhost:9292/kms");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> kmsServerComponents = new HashSet<>();
    kmsServerComponents.add("RANGER_KMS_SERVER");

    TestHostGroup group1 = new TestHostGroup("group1", kmsServerComponents, Collections.singleton("host1"));
    TestHostGroup group2 = new TestHostGroup("group2", kmsServerComponents, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("host1:2181,host2:2181", clusterConfig.getPropertyValue(kmsSiteConfigType, "hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string"));
    assertEquals("dbks://http@localhost:9292/kms", clusterConfig.getPropertyValue(kmsSiteConfigType, "hadoop.kms.key.provider.uri"));
  }


  @Test
  public void testRangerKmsServerProperties_default() throws Exception {
    // Given

    final String kmsSiteConfigType = "kms-site";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> kmsSiteProperties = new HashMap<>();
    properties.put(kmsSiteConfigType, kmsSiteProperties);
    kmsSiteProperties.put("hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string",
      createHostAddress("%HOSTGROUP::group1%", "2181"));


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> kmsServerComponents = new HashSet<>();
    kmsServerComponents.add("RANGER_KMS_SERVER");

    TestHostGroup group1 = new TestHostGroup("group1", kmsServerComponents, Collections.singleton("host1"));


    Collection<TestHostGroup> hostGroups = Collections.singleton(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("host1:2181", clusterConfig.getPropertyValue(kmsSiteConfigType,
      "hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string"));
  }


  @Test
  public void testHdfsWithRangerKmsServer() throws Exception {
    // Given
    final String configType = "hdfs-site";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> configProperties = new HashMap<>();

    properties.put(configType, configProperties);
    configProperties.put("dfs.encryption.key.provider.uri", "kms://http@%HOSTGROUP::group1%;%HOSTGROUP::group2%:9292/kms");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> kmsServerComponents = new HashSet<>();
    kmsServerComponents.add("RANGER_KMS_SERVER");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");


    TestHostGroup group1 = new TestHostGroup("group1", kmsServerComponents, Collections.singleton("host1"));
    group1.components.add("DATANODE");

    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    String updatedVal = clusterConfig.getPropertyValue(configType, "dfs.encryption.key.provider.uri");
    Assert.assertTrue(updatedVal.startsWith("kms://http@"));
    Assert.assertTrue(updatedVal.endsWith(":9292/kms"));
    String hostsString = updatedVal.substring(11, updatedVal.length() - 9);

    List<String> hostArray = Arrays.asList(hostsString.split(";"));
    List<String> expected = Arrays.asList("host1", "host2");

    // Then
    Assert.assertTrue(hostArray.containsAll(expected) && expected.containsAll(hostArray));
  }


  @Test
  public void testHdfsWithNoRangerKmsServer() throws Exception {
    // Given

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("DATANODE")).andReturn(new Cardinality("1+")).anyTimes();
    expect(stack.getCardinality("RANGER_KMS_SERVER")).andReturn(new Cardinality("1+")).anyTimes();


    final String configType = "hdfs-site";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> configProperties = new HashMap<>();

    properties.put(configType, configProperties);
    configProperties.put("dfs.encryption.key.provider.uri", "leave_untouched");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");


    TestHostGroup group1 = new TestHostGroup("group1", Collections.singletonList("DATANODE"), Collections.singleton("host1"));

    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);


    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("leave_untouched", clusterConfig.getPropertyValue(configType, "dfs.encryption.key.provider.uri"));
  }


  @Test
  public void testHdfsWithRangerKmsServer_default() throws Exception {
    // Given
    final String configType = "hdfs-site";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> configProperties = new HashMap<>();

    properties.put(configType, configProperties);
    configProperties.put("dfs.encryption.key.provider.uri", "kms://http@localhost:9292/kms");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> kmsServerComponents = new HashSet<>();
    kmsServerComponents.add("RANGER_KMS_SERVER");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");


    TestHostGroup group1 = new TestHostGroup("group1", kmsServerComponents, Collections.singleton("host1"));
    group1.components.add("DATANODE");

    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("kms://http@host1:9292/kms", clusterConfig.getPropertyValue(configType, "dfs.encryption.key.provider.uri"));
  }

  @Test
  public void testHdfsWithRangerKmsServer__multiple_hosts__localhost() throws Exception {
    // Given
    final String configType = "hdfs-site";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> configProperties = new HashMap<>();

    properties.put(configType, configProperties);
    configProperties.put("dfs.encryption.key.provider.uri", "kms://http@localhost:9292/kms");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> kmsServerComponents = new HashSet<>();
    kmsServerComponents.add("RANGER_KMS_SERVER");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");

    Collection<String> hosts = new HashSet<>();
    hosts.add("host1");
    hosts.add("host2");

    TestHostGroup group1 = new TestHostGroup("group1", kmsServerComponents, hosts);
    group1.components.add("DATANODE");

    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host3"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    String updatedVal = clusterConfig.getPropertyValue(configType, "dfs.encryption.key.provider.uri");
    Assert.assertTrue(updatedVal.startsWith("kms://http@"));
    Assert.assertTrue(updatedVal.endsWith(":9292/kms"));
    String hostsString = updatedVal.substring(11, updatedVal.length() - 9);

    List<String> hostArray = Arrays.asList(hostsString.split(";"));
    List<String> expected = Arrays.asList("host1", "host2");

    // Then
    Assert.assertTrue(hostArray.containsAll(expected) && expected.containsAll(hostArray));
  }

  @Test
  public void testHdfsWithRangerKmsServer__multiple_hosts__hostgroup() throws Exception {
    // Given
    final String configType = "hdfs-site";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> configProperties = new HashMap<>();

    properties.put(configType, configProperties);
    configProperties.put("dfs.encryption.key.provider.uri", "kms://http@%HOSTGROUP::group1%:9292/kms");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> kmsServerComponents = new HashSet<>();
    kmsServerComponents.add("RANGER_KMS_SERVER");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");

    Collection<String> hosts = new HashSet<>();
    hosts.add("host1");
    hosts.add("host2");

    TestHostGroup group1 = new TestHostGroup("group1", kmsServerComponents, hosts);
    group1.components.add("DATANODE");

    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host3"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    String updatedVal = clusterConfig.getPropertyValue(configType, "dfs.encryption.key.provider.uri");
    Assert.assertTrue(updatedVal.startsWith("kms://http@"));
    Assert.assertTrue(updatedVal.endsWith(":9292/kms"));
    String hostsString = updatedVal.substring(11, updatedVal.length() - 9);

    List<String> hostArray = Arrays.asList(hostsString.split(";"));
    List<String> expected = Arrays.asList("host1", "host2");

    // Then
    Assert.assertTrue(hostArray.containsAll(expected) && expected.containsAll(hostArray));
  }


  @Test
  public void testResolutionOfDRPCServerAndNN() throws Exception {
    // Given
    final String stormConfigType = "storm-site";
    final String mrConfigType = "mapred-site";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> stormConfigProperties = new HashMap<>();
    Map<String, String> mrConfigProperties = new HashMap<>();

    properties.put(stormConfigType, stormConfigProperties);
    properties.put(mrConfigType, mrConfigProperties);
    stormConfigProperties.put("drpc.servers", "['%HOSTGROUP::group1%']");
    mrConfigProperties.put("mapreduce.job.hdfs-servers", "['%HOSTGROUP::group2%']");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> stormComponents = new HashSet<>();
    stormComponents.add("NIMBUS");
    stormComponents.add("DRPC_SERVER");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");


    TestHostGroup group1 = new TestHostGroup("group1", stormComponents, Collections.singleton("host1"));
    group1.components.add("DATANODE");

    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("['host1']", clusterConfig.getPropertyValue(stormConfigType, "drpc.servers"));
    assertEquals("['host2']", clusterConfig.getPropertyValue(mrConfigType, "mapreduce.job.hdfs-servers"));
  }

  @Test
  public void testHadoopWithRangerKmsServer() throws Exception {
    // Given
    final String configType = "core-site";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> configProperties = new HashMap<>();

    properties.put(configType, configProperties);
    configProperties.put("hadoop.security.key.provider.path", "kms://http@%HOSTGROUP::group1%;%HOSTGROUP::group2%:9292/kms");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> kmsServerComponents = new HashSet<>();
    kmsServerComponents.add("RANGER_KMS_SERVER");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");


    TestHostGroup group1 = new TestHostGroup("group1", kmsServerComponents, Collections.singleton("host1"));
    group1.components.add("DATANODE");

    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("kms://http@host1;host2:9292/kms", clusterConfig.getPropertyValue(configType, "hadoop.security.key.provider.path"));
  }


  @Test
  public void testHadoopWithNoRangerKmsServer() throws Exception {
    // Given
    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("DATANODE")).andReturn(new Cardinality("1+")).anyTimes();
    expect(stack.getCardinality("RANGER_KMS_SERVER")).andReturn(new Cardinality("1+")).anyTimes();

    final String configType = "core-site";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> configProperties = new HashMap<>();

    properties.put(configType, configProperties);
    configProperties.put("hadoop.security.key.provider.path", "leave_untouched");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");


    TestHostGroup group1 = new TestHostGroup("group1", Collections.singletonList("DATANODE"), Collections.singleton("host1"));

    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);


    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("leave_untouched", clusterConfig.getPropertyValue(configType, "hadoop.security.key.provider.path"));
  }


  @Test
  public void testHadoopWithRangerKmsServer_default() throws Exception {
    // Given
    final String configType = "core-site";
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> configProperties = new HashMap<>();

    properties.put(configType, configProperties);
    configProperties.put("hadoop.security.key.provider.path", "kms://http@localhost:9292/kms");


    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);


    Collection<String> kmsServerComponents = new HashSet<>();
    kmsServerComponents.add("RANGER_KMS_SERVER");

    Collection<String> hdfsComponents = new HashSet<>();
    hdfsComponents.add("NAMENODE");
    hdfsComponents.add("DATANODE");


    TestHostGroup group1 = new TestHostGroup("group1", kmsServerComponents, Collections.singleton("host1"));
    group1.components.add("DATANODE");

    TestHostGroup group2 = new TestHostGroup("group2", hdfsComponents, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = Lists.newArrayList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    // When
    configProcessor.doUpdateForClusterCreate();

    // Then
    assertEquals("kms://http@host1:9292/kms", clusterConfig.getPropertyValue(configType, "hadoop.security.key.provider.path"));
  }

  @Test
  public void testYamlMultiValueWithSingleQuoteFlowStyleFormatSingleValue() throws Exception {
    // Given
    BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator yamlMultiValuePropertyDecorator = new BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator(null);
    String originalValue = "test_value";


    // When
    String newValue = yamlMultiValuePropertyDecorator.doFormat(originalValue);

    // Then
    String expectedValue = "['test_value']";
    assertEquals(expectedValue, newValue);
  }

  @Test
  public void testYamlMultiValueWithPlainFlowStyleFormatSingleValue() throws Exception {
    // Given
    BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator yamlMultiValuePropertyDecorator = new BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator(null, BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator.FlowStyle.PLAIN);
    String originalValue = "test_value";


    // When
    String newValue = yamlMultiValuePropertyDecorator.doFormat(originalValue);

    // Then
    String expectedValue = "[test_value]";
    assertEquals(expectedValue, newValue);
  }

  @Test
  public void testYamlMultiValueWithSingleQuoteFlowStyleFormatMultiValue() throws Exception {
    // Given
    BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator yamlMultiValuePropertyDecorator = new BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator(null);
    String originalValue = "test_value1,test_value2";


    // When
    String newValue = yamlMultiValuePropertyDecorator.doFormat(originalValue);

    // Then
    String expectedValue = "['test_value1','test_value2']";
    assertEquals(expectedValue, newValue);
  }

  @Test
  public void testYamlMultiValueWithPlainFlowStyleFormatMultiValue() throws Exception {
    // Given
    BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator yamlMultiValuePropertyDecorator = new BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator(null, BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator.FlowStyle.PLAIN);
    String originalValue = "test_value1,test_value2";


    // When
    String newValue = yamlMultiValuePropertyDecorator.doFormat(originalValue);

    // Then
    String expectedValue = "[test_value1,test_value2]";
    assertEquals(expectedValue, newValue);
  }

  @Test
  public void testYamlMultiValueWithSingleQuoteFlowStyleFormatSingleValueInSquareBrackets() throws Exception {
    // Given
    BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator yamlMultiValuePropertyDecorator = new BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator(null);
    String originalValue = "['test_value']";


    // When
    String newValue = yamlMultiValuePropertyDecorator.doFormat(originalValue);

    // Then
    String expectedValue = "['test_value']";
    assertEquals(expectedValue, newValue);
  }


  @Test
  public void testYamlMultiValueFormatWithPlainFlowStyleSingleValueInSquareBrackets() throws Exception {
    // Given
    BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator yamlMultiValuePropertyDecorator = new BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator(null, BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator.FlowStyle.PLAIN);
    String originalValue = "[test_value]";


    // When
    String newValue = yamlMultiValuePropertyDecorator.doFormat(originalValue);

    // Then
    String expectedValue = "[test_value]";
    assertEquals(expectedValue, newValue);
  }


  @Test
  public void testYamlMultiValueWithSingleQuoteFlowStyleFormatMultiValueInSquareBrackets() throws Exception {
    // Given
    BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator yamlMultiValuePropertyDecorator = new BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator(null);
    String originalValue = "['test_value1','test_value2']";


    // When
    String newValue = yamlMultiValuePropertyDecorator.doFormat(originalValue);

    // Then
    String expectedValue = "['test_value1','test_value2']";
    assertEquals(expectedValue, newValue);
  }

  @Test
  public void testYamlMultiValueWithPlainFlowStyleFormatMultiValueInSquareBrackets() throws Exception {
    // Given
    BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator yamlMultiValuePropertyDecorator = new BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator(null, BlueprintConfigurationProcessor.YamlMultiValuePropertyDecorator.FlowStyle.PLAIN);
    String originalValue = "[test_value1,test_value2]";


    // When
    String newValue = yamlMultiValuePropertyDecorator.doFormat(originalValue);

    // Then
    String expectedValue = "[test_value1,test_value2]";
    assertEquals(expectedValue, newValue);
  }

  @Test
  public void testMultipleHostTopologyUpdaterWithYamlPropertySingleHostValue() throws Exception {
    // Given
    String component = "test_component";
    BlueprintConfigurationProcessor.MultipleHostTopologyUpdater mhtu = new BlueprintConfigurationProcessor.MultipleHostTopologyUpdater(component);

    String propertyOriginalValue1 = "['%HOSTGROUP::group_1%']";
    String propertyOriginalValue2 = "[%HOSTGROUP::group_1%]";

    // When
    String updatedValue1 = mhtu.resolveHostGroupPlaceholder(propertyOriginalValue1, ImmutableList.of("host1:100"));
    String updatedValue2 = mhtu.resolveHostGroupPlaceholder(propertyOriginalValue2, ImmutableList.of("host1:100"));

    // Then
    assertEquals("host1:100", updatedValue1);

    assertEquals("host1:100", updatedValue2);
  }


  @Test
  public void testMultipleHostTopologyUpdaterWithYamlPropertyMultiHostValue() throws Exception {
    // Given
    String component = "test_component";
    BlueprintConfigurationProcessor.MultipleHostTopologyUpdater mhtu = new BlueprintConfigurationProcessor.MultipleHostTopologyUpdater(component);

    String propertyOriginalValue1 = "['%HOSTGROUP::group_1%', '%HOSTGROUP::group_2%']";
    String propertyOriginalValue2 = "[%HOSTGROUP::group_1%, %HOSTGROUP::group_2%]";

    // When
    String updatedValue1 = mhtu.resolveHostGroupPlaceholder(propertyOriginalValue1, ImmutableList.of("host1:100", "host2:200"));
    String updatedValue2 = mhtu.resolveHostGroupPlaceholder(propertyOriginalValue2, ImmutableList.of("host1:100", "host2:200"));

    // Then
    assertEquals("host1:100,host2:200", updatedValue1);

    assertEquals("host1:100,host2:200", updatedValue2);
  }


  @Test
  public void testMultipleHostTopologyUpdaterWithSingleHostWithSuffixValue() throws Exception {
    // Given
    String component = "test_component";
    BlueprintConfigurationProcessor.MultipleHostTopologyUpdater mhtu = new BlueprintConfigurationProcessor.MultipleHostTopologyUpdater(component);

    String propertyOriginalValue = "http://%HOSTGROUP::group_1%#";

    // When
    String updatedValue = mhtu.resolveHostGroupPlaceholder(propertyOriginalValue, ImmutableList.of("host1:100"));

    // Then
    assertEquals("http://host1:100#", updatedValue);
  }

  @Test
  public void testMultipleHostTopologyUpdaterWithMultiHostWithSuffixValue() throws Exception {
    // Given
    String component = "test_component";
    BlueprintConfigurationProcessor.MultipleHostTopologyUpdater mhtu = new BlueprintConfigurationProcessor.MultipleHostTopologyUpdater(component);

    String propertyOriginalValue = "http://%HOSTGROUP::group_1,HOSTGROUP::group_2%/resource";

    // When
    String updatedValue = mhtu.resolveHostGroupPlaceholder(propertyOriginalValue, ImmutableList.of("host1:100", "host2:200"));

    // Then
    assertEquals("http://host1:100,host2:200/resource", updatedValue);
  }

  @Test
  public void testMultipleHostTopologyUpdaterWithMultiHostValue() throws Exception {
    // Given
    String component = "test_component";
    BlueprintConfigurationProcessor.MultipleHostTopologyUpdater mhtu = new BlueprintConfigurationProcessor.MultipleHostTopologyUpdater(component);

    String propertyOriginalValue = "%HOSTGROUP::group_1%:11,%HOSTGROUP::group_2%:11";

    // When
    String updatedValue = mhtu.resolveHostGroupPlaceholder(propertyOriginalValue, ImmutableList.of("host1:100", "host2:200"));

    // Then
    assertEquals("host1:100,host2:200", updatedValue);
  }

  @Test
  public void testHawqConfigClusterUpdate() throws Exception {
    final String expectedHostNameHawqMaster = "c6401.apache.ambari.org";
    final String expectedHostNameHawqStandby = "c6402.apache.ambari.org";
    final String expectedHostNameNamenode = "c6403.apache.ambari.org";
    final String expectedPortNamenode = "8020";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hawqSite = new HashMap<>();
    properties.put("hawq-site", hawqSite);

    // setup properties that include host information
    hawqSite.put("hawq_master_address_host", "localhost");
    hawqSite.put("hawq_standby_address_host", "localhost");
    hawqSite.put("hawq_dfs_url", createHostAddress("localhost", expectedPortNamenode) + "/hawq_data");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    //Host group which has NAMENODE
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton(expectedHostNameNamenode));

    //Host group which has HAWQMASTER
    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("HAWQMASTER");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton(expectedHostNameHawqMaster));

    //Host group which has HAWQSTANDBY
    Collection<String> hgComponents3 = new HashSet<>();
    hgComponents3.add("HAWQSTANDBY");
    TestHostGroup group3 = new TestHostGroup("group3", hgComponents3, Collections.singleton(expectedHostNameHawqStandby));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    assertEquals(expectedHostNameHawqMaster, hawqSite.get("hawq_master_address_host"));
    assertEquals(expectedHostNameHawqStandby, hawqSite.get("hawq_standby_address_host"));
    assertEquals(createHostAddress(expectedHostNameNamenode, expectedPortNamenode) + "/hawq_data", hawqSite.get("hawq_dfs_url"));
  }

  @Test
  public void testHawqNonHaConfigClusterUpdate() throws Exception {
    final String expectedHostNameHawqMaster = "c6401.apache.ambari.org";

    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> hawqSite = new HashMap<>();
    properties.put("hawq-site", hawqSite);

    // setup properties that include host information
    hawqSite.put("hawq_master_address_host", "localhost");
    hawqSite.put("hawq_standby_address_host", "localhost");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    //Host group which has HAWQMASTER
    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("HAWQMASTER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton(expectedHostNameHawqMaster));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    updater.doUpdateForClusterCreate();

    assertEquals(expectedHostNameHawqMaster, hawqSite.get("hawq_master_address_host"));
    assertFalse("hawq_standby_address_host should have been filtered out of this non-HAWQ HA configuration",
      hawqSite.containsKey("hawq_standby_address_host"));
  }

  @Test
  public void testDoUpdateForBlueprintExport_NonTopologyProperty__AtlasClusterName() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("ATLAS_SERVER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    Long clusterId = topology.getClusterId();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("atlas.cluster.name", String.valueOf(clusterId));
    properties.put("hive-site", typeProps);

    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    String updatedVal = properties.get("hive-site").get("atlas.cluster.name");
    assertEquals("primary", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_NonTopologyProperty() throws Exception {
    String someString = "String.To.Represent.A.String.Value";
    Map<String, Map<String, String>> properties = new HashMap<>();

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("ATLAS_SERVER");
    hgComponents.add("HIVE_SERVER");
    hgComponents.add("KAFKA_BROKER");
    hgComponents.add("NIMBUS");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    Long clusterId = topology.getClusterId();

    Map<String, String> hiveSiteProps = new HashMap<>();
    hiveSiteProps.put("hive.exec.post.hooks", someString);
    properties.put("hive-site", hiveSiteProps);

    Map<String, String> kafkaBrokerProps = new HashMap<>();
    kafkaBrokerProps.put("kafka.metrics.reporters", someString);
    properties.put("kafka-broker", kafkaBrokerProps);

    Map<String, String> stormSiteProps = new HashMap<>();
    stormSiteProps.put("metrics.reporter.register", someString);
    properties.put("storm-site", stormSiteProps);

    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    String hiveExecPostHooks = properties.get("hive-site").get("hive.exec.post.hooks");
    String kafkaMetricsReporters = properties.get("kafka-broker").get("kafka.metrics.reporters");
    String metricsReporterRegister = properties.get("storm-site").get("metrics.reporter.register");
    assertEquals(someString, hiveExecPostHooks);
    assertEquals(someString, kafkaMetricsReporters);
    assertEquals(someString, metricsReporterRegister);
  }

  @Test
  public void druidProperties() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> druidCommon = new HashMap<>();
    String connectUriKey = "druid.metadata.storage.connector.connectURI";
    String metastoreHostnameKey = "metastore_hostname";
    String connectUriTemplate = "jdbc:mysql://%s:3306/druid?createDatabaseIfNotExist=true";
    druidCommon.put(connectUriKey, String.format(connectUriTemplate, "%HOSTGROUP::group1%"));
    druidCommon.put(metastoreHostnameKey, "%HOSTGROUP::group1%");
    properties.put("druid-common", druidCommon);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties, emptyMap());
    Configuration clusterConfig = new Configuration(properties, emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = Sets.newHashSet("DRUID_COORDINATOR");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<String> hgComponents2 = Sets.newHashSet("DRUID_BROKER", "DRUID_OVERLORD", "DRUID_ROUTER");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = Arrays.asList(group1, group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals(String.format(connectUriTemplate, "host1"), clusterConfig.getPropertyValue("druid-common", connectUriKey));
    assertEquals("host1", clusterConfig.getPropertyValue("druid-common", metastoreHostnameKey));
  }

  @Test
  public void testAmsPropertiesDefault() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> amsSite = new HashMap<>();
    //default
    amsSite.put("timeline.metrics.service.webapp.address", "localhost:6188");
    properties.put("ams-site", amsSite);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("METRICS_COLLECTOR");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("0.0.0.0:6188",
      clusterConfig.getPropertyValue("ams-site", "timeline.metrics.service.webapp.address"));
  }

  @Test
  public void testAmsPropertiesSpecialAddress() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> amsSite = new HashMap<>();
    //default
    amsSite.put("timeline.metrics.service.webapp.address", "0.0.0.0:6188");
    properties.put("ams-site", amsSite);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    hgComponents1.add("METRICS_COLLECTOR");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("0.0.0.0:6188",
      clusterConfig.getPropertyValue("ams-site", "timeline.metrics.service.webapp.address"));
  }

  @Test
  public void testAmsPropertiesSpecialAddressMultipleCollectors() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> amsSite = new HashMap<>();
    //default
    amsSite.put("timeline.metrics.service.webapp.address", "0.0.0.0:6188");
    properties.put("ams-site", amsSite);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents1.add("METRICS_COLLECTOR");
    hgComponents2.add("METRICS_COLLECTOR");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents1, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = new LinkedList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals("0.0.0.0:6188",
      clusterConfig.getPropertyValue("ams-site", "timeline.metrics.service.webapp.address"));
  }

  @Test
  public void testStackPasswordPropertyFilter() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> rangerAdminSiteProps = new HashMap<>();
    rangerAdminSiteProps.put("ranger.service.https.attrib.keystore.pass", "SECRET:admin-prp:1:ranger.service.pass");
    properties.put("ranger-admin-site", rangerAdminSiteProps);
    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());

    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);
    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents, Collections.singleton("testhost"));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("testhost2"));
    Collection<TestHostGroup> hostGroups = new HashSet<>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    expect(stack.isPasswordProperty((String) anyObject(), (String) anyObject(), (String) anyObject())).andReturn(true).once();
    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(BlueprintExportType.FULL);

    assertFalse(properties.get("ranger-admin-site").containsKey("ranger.service.https.attrib.keystore.pass"));
  }

  private Map<String, AdvisedConfiguration> createAdvisedConfigMap() {
    Map<String, AdvisedConfiguration> advMap = new HashMap<>();
    Map<String, String> confProp = new HashMap<>();
    confProp.put("fs.stackDefault.key1", "stackDefaultUpgraded");
    confProp.put("fs.notStackDefault", "notStackDefault");
    Map<String, ValueAttributesInfo> valueAttributesInfoMap = new HashMap<>();
    ValueAttributesInfo vaInfo1 = new ValueAttributesInfo();
    vaInfo1.setDelete("true");
    ValueAttributesInfo vaInfo2 = new ValueAttributesInfo();
    vaInfo2.setMaximum("150");
    ValueAttributesInfo vaInfo3 = new ValueAttributesInfo();
    vaInfo3.setMinimum("100");
    valueAttributesInfoMap.put("fs.stackDefault.key2", vaInfo1);
    valueAttributesInfoMap.put("fs.notStackDefault", vaInfo2);
    valueAttributesInfoMap.put("fs.stackDefault.key3", vaInfo3);
    advMap.put("core-site", new AdvisedConfiguration(confProp, valueAttributesInfoMap));
    Map<String, String> dummyConfProp = new HashMap<>();
    dummyConfProp.put("dummy.prop", "dummyValue");
    advMap.put("dummy-site", new AdvisedConfiguration(dummyConfProp, new HashMap<>()));
    Map<String, ValueAttributesInfo> dummy2attrMap = new HashMap<>();
    ValueAttributesInfo dummy2valInfo = new ValueAttributesInfo();
    dummy2valInfo.setDelete("true");
    dummy2attrMap.put("dummy2.property", dummy2valInfo);
    advMap.put("dummy2-site", new AdvisedConfiguration(new HashMap<>(), dummy2attrMap));
    return advMap;
  }

  @Test
  public void testValuesTrimming() throws Exception {
    reset(stack);

    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSite = new HashMap<>();
    //default
    hdfsSite.put("test.spaces", " spaces at    the end should be deleted      ");
    hdfsSite.put("test.directories", "  /all/spaces , should/be  , deleted  ");
    hdfsSite.put("test.password", "  stays,   same    ");
    hdfsSite.put("test.single.space", " ");
    hdfsSite.put("test.host", " https://just.trims ");
    properties.put("hdfs-site", hdfsSite);
    Map<String, Stack.ConfigProperty> propertyConfigs = new HashMap<>();

    ValueAttributesInfo valueAttributesInfoDirs = new ValueAttributesInfo();
    valueAttributesInfoDirs.setType("directories");
    ValueAttributesInfo valueAttributesInfoHost = new ValueAttributesInfo();
    valueAttributesInfoHost.setType("host");

    propertyConfigs.put("test.directories", new Stack.ConfigProperty(
      new StackConfigurationResponse(null, null, null, null, "hdfs-site", null, null, null, valueAttributesInfoDirs, null)));
    propertyConfigs.put("test.password", new Stack.ConfigProperty(
      new StackConfigurationResponse(null, null, null, null, "hdfs-site", null, Collections.singleton(PropertyInfo.PropertyType.PASSWORD), null, null, null)));
    propertyConfigs.put("test.host", new Stack.ConfigProperty(
      new StackConfigurationResponse(null, null, null, null, "hdfs-site", null, null, null, valueAttributesInfoHost, null)));

    expect(stack.getServiceForConfigType("hdfs-site")).andReturn("HDFS").anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata("HDFS", "hdfs-site")).andReturn(propertyConfigs).anyTimes();

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Configuration parentClusterConfig = new Configuration(parentProperties,
      emptyMap());
    Configuration clusterConfig = new Configuration(properties,
      emptyMap(), parentClusterConfig);

    Collection<String> hgComponents1 = new HashSet<>();
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<TestHostGroup> hostGroups = Collections.singletonList(group1);

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

    configProcessor.doUpdateForClusterCreate();

    assertEquals(" spaces at    the end should be deleted",
      clusterConfig.getPropertyValue("hdfs-site", "test.spaces"));
    assertEquals("/all/spaces,should/be,deleted",
      clusterConfig.getPropertyValue("hdfs-site", "test.directories"));
    assertEquals("  stays,   same    ",
      clusterConfig.getPropertyValue("hdfs-site", "test.password"));
    assertEquals(" https://just.trims ".trim(),
      clusterConfig.getPropertyValue("hdfs-site", "test.host"));
    assertEquals(" ",
      clusterConfig.getPropertyValue("hdfs-site", "test.single.space"));
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeFederationEnabledTagsyncEnabledDefaultRepoName() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedNameServiceTwo = "mynameservicetwo";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedHostNameThree = "c6403.apache.ambari.org";
    final String expectedHostNameFour = "c6404.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedNodeThree = "nn3";
    final String expectedNodeFour = "nn4";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedHostGroupNameThree = "host-group-3";
    final String expectedHostGroupNameFour = "host-group-4";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();
    Map<String, String> rangerHDFSPluginProperties = new HashMap<>();
    Map<String, String> rangerHDFSSecurityProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);
    properties.put("ranger-hdfs-plugin-properties", rangerHDFSPluginProperties);
    properties.put("ranger-hdfs-security", rangerHDFSSecurityProperties);

    // setup multiple nameservices, to indicate NameNode Federation will be used
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeThree + "," + expectedNodeFour);

    //setup nameservice-specific properties
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameService,
            "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns1");
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameServiceTwo,
            "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns2");


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));

    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));


    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    rangerHDFSPluginProperties.put("hadoop.rpc.protection","authentication");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_USERNAME","hadoop");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_PASSWORD","hadoop");
    rangerHDFSPluginProperties.put("ranger-hdfs-plugin-enabled","Yes");

    rangerHDFSSecurityProperties.put("ranger.plugin.hdfs.service.name","{{repo_name}}");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("RANGER_ADMIN");
    hgComponents.add("RANGER_USERSYNC");
    hgComponents.add("RANGER_TAGSYNC");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    hgComponents2.add("ATLAS_SERVER");
    hgComponents2.add("ATLAS_CLIENT");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third and fourth hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup(expectedHostGroupNameThree, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));
    TestHostGroup group4 = new TestHostGroup(expectedHostGroupNameFour, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameFour));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes =
            updater.doUpdateForClusterCreate();

    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site", "ranger-tagsync-site"), updatedConfigTypes);


    Map<String, String> updatedRangerTagsyncSiteConfigurations = clusterConfig.getProperties().get("ranger-tagsync-site");
    assertTrue("Expected property ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservice.ranger.service not found.", updatedRangerTagsyncSiteConfigurations.containsKey("ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservice.ranger.service"));
    assertTrue("Expected property ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservicetwo.ranger.service not found.", updatedRangerTagsyncSiteConfigurations.containsKey("ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservicetwo.ranger.service"));
    assertTrue("Expected property ranger.tagsync.atlas.hdfs.instance.clusterName.ranger.service not found.", updatedRangerTagsyncSiteConfigurations.containsKey("ranger.tagsync.atlas.hdfs.instance.clusterName.ranger.service"));
    assertEquals("Expected name service clusterName_hadoop_mynameservice not found", "clusterName_hadoop_mynameservice",updatedRangerTagsyncSiteConfigurations.get("ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservice.ranger.service"));
    assertEquals("Expected name service clusterName_hadoop_mynameservicetwo not found", "clusterName_hadoop_mynameservicetwo",updatedRangerTagsyncSiteConfigurations.get("ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservicetwo.ranger.service"));
    assertEquals("Expected name service clusterName_hadoop_mynameservicetwo not found", "clusterName_hadoop_mynameservice",updatedRangerTagsyncSiteConfigurations.get("ranger.tagsync.atlas.hdfs.instance.clusterName.ranger.service"));
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeFederationEnabledTagsyncEnabledCustomRepoName() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedNameServiceTwo = "mynameservicetwo";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedHostNameThree = "c6403.apache.ambari.org";
    final String expectedHostNameFour = "c6404.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedNodeThree = "nn3";
    final String expectedNodeFour = "nn4";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedHostGroupNameThree = "host-group-3";
    final String expectedHostGroupNameFour = "host-group-4";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();
    Map<String, String> rangerHDFSPluginProperties = new HashMap<>();
    Map<String, String> rangerHDFSSecurityProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);
    properties.put("ranger-hdfs-plugin-properties", rangerHDFSPluginProperties);
    properties.put("ranger-hdfs-security", rangerHDFSSecurityProperties);

    // setup multiple nameservices, to indicate NameNode Federation will be used
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeThree + "," + expectedNodeFour);

    //setup nameservice-specific properties
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameService,
            "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns1");
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameServiceTwo,
            "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns2");


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));

    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));


    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    rangerHDFSPluginProperties.put("hadoop.rpc.protection","authentication");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_USERNAME","hadoop");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_PASSWORD","hadoop");
    rangerHDFSPluginProperties.put("ranger-hdfs-plugin-enabled","Yes");

    rangerHDFSSecurityProperties.put("ranger.plugin.hdfs.service.name","hdfs_service");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("RANGER_ADMIN");
    hgComponents.add("RANGER_USERSYNC");
    hgComponents.add("RANGER_TAGSYNC");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    hgComponents2.add("ATLAS_SERVER");
    hgComponents2.add("ATLAS_CLIENT");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third and fourth hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup(expectedHostGroupNameThree, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));
    TestHostGroup group4 = new TestHostGroup(expectedHostGroupNameFour, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameFour));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes =
            updater.doUpdateForClusterCreate();

    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site", "ranger-tagsync-site"), updatedConfigTypes);


    Map<String, String> updatedRangerTagsyncSiteConfigurations = clusterConfig.getProperties().get("ranger-tagsync-site");
    assertTrue("Expected property ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservice.ranger.service not found.", updatedRangerTagsyncSiteConfigurations.containsKey("ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservice.ranger.service"));
    assertTrue("Expected property ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservicetwo.ranger.service not found.", updatedRangerTagsyncSiteConfigurations.containsKey("ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservicetwo.ranger.service"));
    assertTrue("Expected property ranger.tagsync.atlas.hdfs.instance.clusterName.ranger.service not found.", updatedRangerTagsyncSiteConfigurations.containsKey("ranger.tagsync.atlas.hdfs.instance.clusterName.ranger.service"));
    assertEquals("Expected name service hdfs_service_mynameservice not found", "hdfs_service_mynameservice",updatedRangerTagsyncSiteConfigurations.get("ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservice.ranger.service"));
    assertEquals("Expected name service hdfs_service_mynameservicetwo not found", "hdfs_service_mynameservicetwo",updatedRangerTagsyncSiteConfigurations.get("ranger.tagsync.atlas.hdfs.instance.clusterName.nameservice.mynameservicetwo.ranger.service"));
    assertEquals("Expected name service hdfs_service_mynameservicetwo not found", "hdfs_service_mynameservice",updatedRangerTagsyncSiteConfigurations.get("ranger.tagsync.atlas.hdfs.instance.clusterName.ranger.service"));
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeFederationEnabledTagsyncEnabledPluginDisabled() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedNameServiceTwo = "mynameservicetwo";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedHostNameThree = "c6403.apache.ambari.org";
    final String expectedHostNameFour = "c6404.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedNodeThree = "nn3";
    final String expectedNodeFour = "nn4";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedHostGroupNameThree = "host-group-3";
    final String expectedHostGroupNameFour = "host-group-4";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();
    Map<String, String> rangerHDFSPluginProperties = new HashMap<>();
    Map<String, String> rangerHDFSSecurityProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);
    properties.put("ranger-hdfs-plugin-properties", rangerHDFSPluginProperties);
    properties.put("ranger-hdfs-security", rangerHDFSSecurityProperties);

    // setup multiple nameservices, to indicate NameNode Federation will be used
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeThree + "," + expectedNodeFour);

    //setup nameservice-specific properties
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameService,
      "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns1");
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameServiceTwo,
      "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns2");


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));

    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));


    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    rangerHDFSPluginProperties.put("hadoop.rpc.protection","authentication");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_USERNAME","hadoop");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_PASSWORD","hadoop");
    rangerHDFSPluginProperties.put("ranger-hdfs-plugin-enabled","No");

    rangerHDFSSecurityProperties.put("ranger.plugin.hdfs.service.name","{{repo_name}}");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("RANGER_ADMIN");
    hgComponents.add("RANGER_USERSYNC");
    hgComponents.add("RANGER_TAGSYNC");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    hgComponents2.add("ATLAS_SERVER");
    hgComponents2.add("ATLAS_CLIENT");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third and fourth hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup(expectedHostGroupNameThree, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));
    TestHostGroup group4 = new TestHostGroup(expectedHostGroupNameFour, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameFour));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes = updater.doUpdateForClusterCreate();

    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site"), updatedConfigTypes);


    Map<String, String> updatedRangerTagsyncSiteConfigurations = clusterConfig.getProperties().get("ranger-tagsync-site");
    assertNull(updatedRangerTagsyncSiteConfigurations);


  }

  @Test
  public void testDoUpdateForClusterWithNameNodeFederationEnabledTagsyncEnabledPluginEnabledNoAtlas() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedNameServiceTwo = "mynameservicetwo";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedHostNameThree = "c6403.apache.ambari.org";
    final String expectedHostNameFour = "c6404.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedNodeThree = "nn3";
    final String expectedNodeFour = "nn4";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedHostGroupNameThree = "host-group-3";
    final String expectedHostGroupNameFour = "host-group-4";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();
    Map<String, String> rangerHDFSPluginProperties = new HashMap<>();
    Map<String, String> rangerHDFSSecurityProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);
    properties.put("ranger-hdfs-plugin-properties", rangerHDFSPluginProperties);
    properties.put("ranger-hdfs-security", rangerHDFSSecurityProperties);

    // setup multiple nameservices, to indicate NameNode Federation will be used
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeThree + "," + expectedNodeFour);

    //setup nameservice-specific properties
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameService,
      "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns1");
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameServiceTwo,
      "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns2");


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));

    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));


    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    rangerHDFSPluginProperties.put("hadoop.rpc.protection","authentication");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_USERNAME","hadoop");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_PASSWORD","hadoop");
    rangerHDFSPluginProperties.put("ranger-hdfs-plugin-enabled","Yes");

    rangerHDFSSecurityProperties.put("ranger.plugin.hdfs.service.name","{{repo_name}}");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("RANGER_ADMIN");
    hgComponents.add("RANGER_USERSYNC");
    hgComponents.add("RANGER_TAGSYNC");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third and fourth hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup(expectedHostGroupNameThree, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));
    TestHostGroup group4 = new TestHostGroup(expectedHostGroupNameFour, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameFour));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes = updater.doUpdateForClusterCreate();

    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site"), updatedConfigTypes);


    Map<String, String> updatedRangerTagsyncSiteConfigurations = clusterConfig.getProperties().get("ranger-tagsync-site");
    assertNull(updatedRangerTagsyncSiteConfigurations);
  }

  @Test
  public void testDoUpdateForClusterWithNameNodeFederationEnabledTagsyncEnabledPluginEnabledNoTagsync() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedNameServiceTwo = "mynameservicetwo";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";
    final String expectedHostNameThree = "c6403.apache.ambari.org";
    final String expectedHostNameFour = "c6404.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedNodeThree = "nn3";
    final String expectedNodeFour = "nn4";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedHostGroupNameThree = "host-group-3";
    final String expectedHostGroupNameFour = "host-group-4";

    Map<String, Map<String, String>> properties = new HashMap<>();

    Map<String, String> hdfsSiteProperties = new HashMap<>();
    Map<String, String> hbaseSiteProperties = new HashMap<>();
    Map<String, String> hadoopEnvProperties = new HashMap<>();
    Map<String, String> coreSiteProperties = new HashMap<>();
    Map<String, String> accumuloSiteProperties = new HashMap<>();
    Map<String, String> rangerHDFSPluginProperties = new HashMap<>();
    Map<String, String> rangerHDFSSecurityProperties = new HashMap<>();

    properties.put("hdfs-site", hdfsSiteProperties);
    properties.put("hadoop-env", hadoopEnvProperties);
    properties.put("core-site", coreSiteProperties);
    properties.put("hbase-site", hbaseSiteProperties);
    properties.put("accumulo-site", accumuloSiteProperties);
    properties.put("ranger-hdfs-plugin-properties", rangerHDFSPluginProperties);
    properties.put("ranger-hdfs-security", rangerHDFSSecurityProperties);

    // setup multiple nameservices, to indicate NameNode Federation will be used
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeThree + "," + expectedNodeFour);

    //setup nameservice-specific properties
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameService,
      "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns1");
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir" + "." + expectedNameServiceTwo,
      "qjournal://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + ";" + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo) + "/ns2");


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupNameTwo));

    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeThree, createExportedAddress(expectedPortNum, expectedHostGroupNameThree));
    hdfsSiteProperties.put("dfs.namenode.servicerpc-address." + expectedNameServiceTwo + "." + expectedNodeFour, createExportedAddress(expectedPortNum, expectedHostGroupNameFour));


    // add properties that require the SECONDARY_NAMENODE, which
    // is not included in this test
    hdfsSiteProperties.put("dfs.secondary.http.address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", "localhost:8080");


    // add properties that are used in non-HA HDFS NameNode settings
    // to verify that these are eventually removed by the filter
    hdfsSiteProperties.put("dfs.namenode.http-address", "localhost:8080");
    hdfsSiteProperties.put("dfs.namenode.https-address", "localhost:8081");
    hdfsSiteProperties.put("dfs.namenode.rpc-address", "localhost:8082");

    // configure the defaultFS to use the nameservice URL
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);

    // configure the hbase rootdir to use the nameservice URL
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/hbase/test/root/dir");

    // configure the hbase rootdir to use the nameservice URL
    accumuloSiteProperties.put("instance.volumes", "hdfs://" + expectedNameService + "/accumulo/test/instance/volumes");

    rangerHDFSPluginProperties.put("hadoop.rpc.protection","authentication");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_USERNAME","hadoop");
    rangerHDFSPluginProperties.put("REPOSITORY_CONFIG_PASSWORD","hadoop");
    rangerHDFSPluginProperties.put("ranger-hdfs-plugin-enabled","Yes");

    rangerHDFSSecurityProperties.put("ranger.plugin.hdfs.service.name","{{repo_name}}");

    Configuration clusterConfig = new Configuration(properties, emptyMap());

    Collection<String> hgComponents = new HashSet<>();
    hgComponents.add("NAMENODE");
    hgComponents.add("RANGER_ADMIN");
    hgComponents.add("RANGER_USERSYNC");
    TestHostGroup group1 = new TestHostGroup(expectedHostGroupName, hgComponents, Collections.singleton(expectedHostName));

    Collection<String> hgComponents2 = new HashSet<>();
    hgComponents2.add("NAMENODE");
    hgComponents2.add("ATLAS_SERVER");
    hgComponents2.add("ATLAS_CLIENT");
    TestHostGroup group2 = new TestHostGroup(expectedHostGroupNameTwo, hgComponents2, Collections.singleton(expectedHostNameTwo));

    // add third and fourth hostgroup with NAMENODE, to simulate HDFS NameNode Federation
    TestHostGroup group3 = new TestHostGroup(expectedHostGroupNameThree, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameThree));
    TestHostGroup group4 = new TestHostGroup(expectedHostGroupNameFour, Collections.singleton("NAMENODE"), Collections.singleton(expectedHostNameFour));

    Collection<TestHostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);
    hostGroups.add(group4);

    expect(stack.getCardinality("NAMENODE")).andReturn(new Cardinality("1-2")).anyTimes();
    expect(stack.getCardinality("SECONDARY_NAMENODE")).andReturn(new Cardinality("1")).anyTimes();

    ClusterTopology topology = createClusterTopology(bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    Set<String> updatedConfigTypes = updater.doUpdateForClusterCreate();

    // verify that correct configuration types were listed as updated in the returned set
    assertEquals(ImmutableSet.of("cluster-env", "hdfs-site"), updatedConfigTypes);


    Map<String, String> updatedRangerTagsyncSiteConfigurations = clusterConfig.getProperties().get("ranger-tagsync-site");
    assertNull(updatedRangerTagsyncSiteConfigurations);
  }

  @Test
  public void defaultConfigs() {
    Configuration stackConfig = createTestStack();
    Configuration clusterConfig = stackConfig.copy();
    Configuration customConfig = Configuration.newEmpty();

    ClusterTopology topology = createNiceMock(ClusterTopology.class);
    Stack stack = createNiceMock(Stack.class);
    Set<String> services = ImmutableSet.of("HDFS");
    expect(stack.getServices()).andReturn(services).anyTimes();
    expect(stack.getConfiguration()).andReturn(stackConfig).anyTimes();
    expect(topology.getConfiguration()).andReturn(clusterConfig).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(emptyMap()).anyTimes();
    replay(stack, topology);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.applyTypeSpecificFilter(BlueprintExportType.MINIMAL, clusterConfig, stackConfig, services);

    assertEquals(customConfig.getProperties(), clusterConfig.getProperties());
  }

  @Test
  public void customConfigs() {
    Configuration stackConfig = createTestStack();
    Configuration clusterConfig = stackConfig.copy();
    Configuration customConfig = Configuration.newEmpty();
    customize(clusterConfig, customConfig, "core-site", "hadoop.security.authorization", "true");
    customize(clusterConfig, customConfig, "core-site", "fs.trash.interval", "0");
    customize(clusterConfig, customConfig, "hdfs-site", "dfs.webhdfs.enabled",  "false");

    ClusterTopology topology = createNiceMock(ClusterTopology.class);
    Stack stack = createNiceMock(Stack.class);
    Set<String> services = ImmutableSet.of("HDFS");
    expect(stack.getServices()).andReturn(services).anyTimes();
    expect(stack.getConfiguration()).andReturn(stackConfig).anyTimes();
    expect(topology.getConfiguration()).andReturn(clusterConfig).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(emptyMap()).anyTimes();
    replay(stack, topology);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);
    updater.applyTypeSpecificFilter(BlueprintExportType.MINIMAL, clusterConfig, stackConfig, services);

    assertEquals(customConfig.getProperties(), clusterConfig.getProperties());
  }

  private static Configuration createTestStack() {
    Configuration stackConfig = Configuration.newEmpty();
    stackConfig.setProperty("core-site", "io.file.buffer.size",  "131072");
    stackConfig.setProperty("core-site", "hadoop.security.authorization",  "false");
    stackConfig.setProperty("core-site", "fs.trash.interval",  "360");
    stackConfig.setProperty("hdfs-site", "dfs.namenode.name.dir",  "/hadoop/hdfs/namenode");
    stackConfig.setProperty("hdfs-site", "dfs.datanode.data.dir",  "/hadoop/hdfs/data");
    stackConfig.setProperty("hdfs-site", "dfs.webhdfs.enabled",  "true");
    return stackConfig;
  }

  private static void customize(Configuration clusterConfig, Configuration customConfig, String configType, String propertyName, String value) {
    clusterConfig.setProperty(configType, propertyName, value);
    customConfig.setProperty(configType, propertyName, value);
  }

  private static String createExportedAddress(String expectedPortNum, String expectedHostGroupName) {
    return createExportedHostName(expectedHostGroupName, expectedPortNum);
  }

  private static String createExportedHostName(String expectedHostGroupName, String expectedPortNumber) {
    return createExportedHostName(expectedHostGroupName) + ":" + expectedPortNumber;
  }


  private static String createExportedHostName(String expectedHostGroupName) {
    return "%HOSTGROUP::" + expectedHostGroupName + "%";
  }

  private static String createHostAddress(String hostName, String portNumber) {
    return hostName + ":" + portNumber;
  }

  private Configuration createStackDefaults() {
    Map<String, Map<String, String>> stackDefaultProps =
      new HashMap<>();
    Map<String, String> coreSiteDefault = new HashMap<>();
    coreSiteDefault.put("fs.stackDefault.key1", "stackDefaultValue1");
    coreSiteDefault.put("fs.stackDefault.key2", "stackDefaultValue2");
    stackDefaultProps.put("core-site", coreSiteDefault);

    Map<String, String> dummySiteDefaults =
      new HashMap<>();
    dummySiteDefaults.put("dummy.prop", "dummyValue");
    stackDefaultProps.put("dummy-site", dummySiteDefaults);

    Map<String, Map<String, Map<String, String>>> stackDefaultAttributes =
      new HashMap<>();
    return new Configuration(stackDefaultProps, stackDefaultAttributes);
  }

  private ClusterTopology createClusterTopology(Blueprint blueprint, Configuration configuration,
                                                Collection<TestHostGroup> hostGroups) throws InvalidTopologyException {
    return createClusterTopology(blueprint, configuration, hostGroups, null);
  }

  private ClusterTopology createClusterTopology(Blueprint blueprint, Configuration configuration,
                                                Collection<TestHostGroup> hostGroups, AmbariContext ambariContextReplacement)
    throws InvalidTopologyException {


    replay(stack, serviceInfo, ambariContext, configHelper, controller, kerberosHelper, kerberosDescriptor, clusters, cluster);

    Map<String, HostGroupInfo> hostGroupInfo = new HashMap<>();
    Collection<String> allServices = new HashSet<>();
    Map<String, HostGroup> allHostGroups = new HashMap<>();

    for (TestHostGroup hostGroup : hostGroups) {
      HostGroupInfo groupInfo = new HostGroupInfo(hostGroup.name);
      groupInfo.addHosts(hostGroup.hosts);
      //todo: HG configs
      groupInfo.setConfiguration(hostGroup.configuration);

      List<Component> componentList = new ArrayList<>();
      for (String componentName : hostGroup.components) {
        componentList.add(new Component(componentName));
      }

      //create host group which is set on topology
      allHostGroups.put(hostGroup.name, new HostGroupImpl(hostGroup.name, "test-bp", stack,
        componentList, EMPTY_CONFIG, "1"));

      hostGroupInfo.put(hostGroup.name, groupInfo);

      for (String component : hostGroup.components) {
        for (Map.Entry<String, Collection<String>> serviceComponentsEntry : serviceComponents.entrySet()) {
          if (serviceComponentsEntry.getValue().contains(component)) {
            allServices.add(serviceComponentsEntry.getKey());
          }
        }
      }
    }

    expect(bp.getServices()).andReturn(allServices).anyTimes();

    for (HostGroup group : allHostGroups.values()) {
      expect(bp.getHostGroup(group.getName())).andReturn(group).anyTimes();
    }

    expect(bp.getHostGroups()).andReturn(allHostGroups).anyTimes();

    expect(topologyRequestMock.getClusterId()).andReturn(1L).anyTimes();
    expect(topologyRequestMock.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topologyRequestMock.getConfiguration()).andReturn(configuration).anyTimes();
    expect(topologyRequestMock.getHostGroupInfo()).andReturn(hostGroupInfo).anyTimes();

    replay(bp, topologyRequestMock);

    if (ambariContextReplacement != null) {
      // override the mock AmbariContext setup in the default Before method
      // Note, this should only be used in a small number of test cases to verify exception
      // behavior when the AmbariContext returns an unexpected value
      ambariContext = ambariContextReplacement;
    }

    ClusterTopology topology = new ClusterTopologyImpl(ambariContext, topologyRequestMock);
    topology.setConfigRecommendationStrategy(ConfigRecommendationStrategy.NEVER_APPLY);

    return topology;
  }

  private class TestHostGroup {
    private String name;
    private Collection<String> components;
    private Collection<String> hosts;
    private Configuration configuration;

    public TestHostGroup(String name, Collection<String> components, Collection<String> hosts) {
      this.name = name;
      this.components = components;
      this.hosts = hosts;
      configuration = new Configuration(emptyMap(),
        emptyMap());
    }

    public TestHostGroup(String name, Collection<String> components, Collection<String> hosts, Configuration configuration) {
      this.name = name;
      this.components = components;
      this.hosts = hosts;
      this.configuration = configuration;
    }
  }
}
