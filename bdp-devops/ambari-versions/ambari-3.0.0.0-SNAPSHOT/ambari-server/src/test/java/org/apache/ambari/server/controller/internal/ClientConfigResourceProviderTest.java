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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.state.ClientConfigFileDefinition;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UserGroupInfo;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * ClientConfigResourceProviderTest tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ClientConfigResourceProvider.class, StageUtils.class})
public class ClientConfigResourceProviderTest {
  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.ClientConfig;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID, "c1");
    properties.put(ClientConfigResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HDFS_CLIENT");
    properties.put(ClientConfigResourceProvider.COMPONENT_SERVICE_NAME_PROPERTY_ID, "HDFS");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    try {
      provider.createResources(request);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (SystemException e) {
      // expected
    }

    // verify
    verify(managementController, response);
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.ClientConfig;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder().property(
        ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals("c1").toPredicate();

    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (SystemException e) {
      // expected
    }

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResourcesForAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetResourcesForClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesForClusterOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testGetResourcesForServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetResourcesForServiceOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceOperator());
  }

  @Test
  public void testGetResourcesForClusterUser() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesForNoRoleUser() throws Exception {
    testGetResources(TestAuthenticationFactory.createNoRoleUser());
  }

  @Test
  public void testGetResourcesFromCommonServicesForAdministrator() throws Exception {
    testGetResourcesFromCommonServices(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetResourcesFromCommonServicesForClusterAdministrator() throws Exception {
    testGetResourcesFromCommonServices(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesFromCommonServicesForClusterOperator() throws Exception {
    testGetResourcesFromCommonServices(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testGetResourcesFromCommonServicesForServiceAdministrator() throws Exception {
    testGetResourcesFromCommonServices(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetResourcesFromCommonServicesForServiceOperator() throws Exception {
    testGetResourcesFromCommonServices(TestAuthenticationFactory.createServiceOperator());
  }

  @Test
  public void testGetResourcesFromCommonServicesForClusterUser() throws Exception {
    testGetResourcesFromCommonServices(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesFromCommonServicesForNoRoleUser() throws Exception {
    testGetResourcesFromCommonServices(TestAuthenticationFactory.createNoRoleUser());
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.ClientConfig;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Predicate predicate = new PredicateBuilder().property(
        ClientConfigResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("HDFS_CLIENT").toPredicate();
    try {
      provider.deleteResources(new RequestImpl(null, null, null, null), predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (SystemException e) {
      // expected
    }

    // verify
    verify(managementController);
  }

  private void testGetResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.ClientConfig;

    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);

    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    CommandScriptDefinition commandScriptDefinition = createNiceMock(CommandScriptDefinition.class);
    Config clusterConfig = createNiceMock(Config.class);
    DesiredConfig desiredConfig = createNiceMock(DesiredConfig.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    Configuration configuration = PowerMock.createStrictMockAndExpectNew(Configuration.class);

    File newFile = File.createTempFile("config", ".json", new File("/tmp/"));
    newFile.deleteOnExit();

    Runtime runtime = createMock(Runtime.class);
    Process process = createNiceMock(Process.class);

    Map<String, DesiredConfig> desiredConfigMap = new HashMap<>();
    desiredConfigMap.put("hive-site", desiredConfig);
    Map<String, Map<String, String>> allConfigTags = new HashMap<>();
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, Map<String, String>> configTags = new HashMap<>();
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();

    ClientConfigFileDefinition clientConfigFileDefinition = new ClientConfigFileDefinition();
    clientConfigFileDefinition.setDictionaryName("pig-env");
    clientConfigFileDefinition.setFileName("pig-env.sh");
    clientConfigFileDefinition.setType("env");
    List<ClientConfigFileDefinition> clientConfigFileDefinitionList = new LinkedList<>();
    clientConfigFileDefinitionList.add(clientConfigFileDefinition);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    String clusterName = "C1";
    String serviceName = "PIG";
    String componentName = "PIG";
    String displayName = "Pig Client";
    String hostName = "Host100";
    String publicHostname = "Host100";
    String desiredState = "INSTALLED";

    String stackName = "S1";
    String stackVersion = "V1";

    String stackRoot = "/tmp/stacks/S1/V1";
    String packageFolder = "PIG/package";

    if (System.getProperty("os.name").contains("Windows")) {
      stackRoot = "C:\\tmp\\stacks\\S1\\V1";
      packageFolder = "PIG\\package";
    }

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse(clusterName, serviceName,
        componentName, displayName, hostName, publicHostname, desiredState, "", null, null, null,
        null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<>();
    responses.add(shr1);

    Map<String, String> returnConfigMap = new HashMap<>();
    returnConfigMap.put(Configuration.SERVER_TMP_DIR.getKey(), Configuration.SERVER_TMP_DIR.getDefaultValue());
    returnConfigMap.put(Configuration.AMBARI_PYTHON_WRAP.getKey(), Configuration.AMBARI_PYTHON_WRAP.getDefaultValue());

    // set expectations
    expect(managementController.getConfigHelper()).andReturn(configHelper);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();
    expect(configHelper.getEffectiveConfigProperties(cluster, configTags)).andReturn(properties);
    expect(configHelper.getEffectiveConfigAttributes(cluster, configTags)).andReturn(attributes);
    expect(configuration.getConfigsMap()).andReturn(returnConfigMap);
    expect(configuration.getResourceDirPath()).andReturn(stackRoot);
    expect(configuration.getExternalScriptThreadPoolSize()).andReturn(Configuration.THREAD_POOL_SIZE_FOR_EXTERNAL_SCRIPT.getDefaultValue());
    expect(configuration.getExternalScriptTimeout()).andReturn(Configuration.EXTERNAL_SCRIPT_TIMEOUT.getDefaultValue());
    Map<String, String> props = new HashMap<>();
    props.put("key", "value");
    expect(clusterConfig.getProperties()).andReturn(props);
    expect(configHelper.getEffectiveDesiredTags(cluster, null)).andReturn(allConfigTags);
    expect(cluster.getClusterName()).andReturn(clusterName);
    expect(managementController.getHostComponents(EasyMock.anyObject())).andReturn(responses).anyTimes();

    PowerMock.mockStaticPartial(StageUtils.class, "getClusterHostInfo");
    Map<String, Set<String>> clusterHostInfo = new HashMap<>();
    Set<String> all_hosts = new HashSet<>(Arrays.asList("Host100", "Host101", "Host102"));
    Set<String> some_hosts = new HashSet<>(Arrays.asList("0-1", "2"));
    Set<String> ohter_hosts = Collections.singleton("0,1");
    Set<String> clusterHostTypes = new HashSet<>(Arrays.asList("nm_hosts", "hs_host",
        "namenode_host", "rm_host", "snamenode_host", "slave_hosts", "zookeeper_hosts"));
    for (String hostTypes : clusterHostTypes) {
      if (hostTypes.equals("slave_hosts")) {
        clusterHostInfo.put(hostTypes, ohter_hosts);
      } else {
        clusterHostInfo.put(hostTypes, some_hosts);
      }
    }
    clusterHostInfo.put("all_hosts", all_hosts);
    expect(StageUtils.getClusterHostInfo(cluster)).andReturn(clusterHostInfo);

    expect(stackId.getStackName()).andReturn(stackName).anyTimes();
    expect(stackId.getStackVersion()).andReturn(stackVersion).anyTimes();

    expect(ambariMetaInfo.getComponent(stackName, stackVersion, serviceName, componentName)).andReturn(componentInfo);
    expect(ambariMetaInfo.getService(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getServicePackageFolder()).andReturn(packageFolder);
    expect(ambariMetaInfo.getComponent(anyString(), anyString(),
        anyString(), anyString())).andReturn(componentInfo).anyTimes();
    expect(componentInfo.getCommandScript()).andReturn(commandScriptDefinition);
    expect(componentInfo.getClientConfigFiles()).andReturn(clientConfigFileDefinitionList);
    expect(cluster.getConfig("hive-site", null)).andReturn(clusterConfig);
    expect(clusterConfig.getType()).andReturn("hive-site").anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigMap);
    expect(clusters.getHost(hostName)).andReturn(host);

    expect(cluster.getService(serviceName)).andReturn(service).atLeastOnce();
    expect(service.getServiceComponent(componentName)).andReturn(serviceComponent).atLeastOnce();
    expect(serviceComponent.getDesiredStackId()).andReturn(stackId).atLeastOnce();

    HashMap<String, String> rcaParams = new HashMap<>();
    rcaParams.put("key", "value");
    expect(managementController.getRcaParameters()).andReturn(rcaParams).anyTimes();
    expect(ambariMetaInfo.getService(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getOsSpecifics()).andReturn(new HashMap<>()).anyTimes();
    Map<PropertyInfo, String> userProperties = new HashMap<>();
    Map<PropertyInfo, String> groupProperties = new HashMap<>();
    PropertyInfo userProperty = new PropertyInfo();
    userProperty.setFilename("hadoop-env.xml");
    userProperty.setName("hdfs-user");
    userProperty.setValue("hdfsUser");


    PropertyInfo groupProperty = new PropertyInfo();
    groupProperty.setFilename("hadoop-env.xml");
    groupProperty.setName("hdfs-group");
    groupProperty.setValue("hdfsGroup");
    ValueAttributesInfo valueAttributesInfo = new ValueAttributesInfo();
    valueAttributesInfo.setType("user");
    Set<UserGroupInfo> userGroupEntries = new HashSet<>();
    UserGroupInfo userGroupInfo = new UserGroupInfo();
    userGroupInfo.setType("hadoop-env");
    userGroupInfo.setName("hdfs-group");
    userGroupEntries.add(userGroupInfo);
    valueAttributesInfo.setUserGroupEntries(userGroupEntries);
    userProperty.setPropertyValueAttributes(valueAttributesInfo);
    userProperties.put(userProperty, "hdfsUser");
    groupProperties.put(groupProperty, "hdfsGroup");
    Map<String, Set<String>> userGroupsMap = new HashMap<>();
    userGroupsMap.put("hdfsUser", Collections.singleton("hdfsGroup"));
    expect(configHelper.getPropertiesWithPropertyType(
        stackId, PropertyInfo.PropertyType.USER, cluster, desiredConfigMap)).andReturn(userProperties).anyTimes();
    expect(configHelper.getPropertiesWithPropertyType(
        stackId, PropertyInfo.PropertyType.GROUP, cluster, desiredConfigMap)).andReturn(groupProperties).anyTimes();
    expect(configHelper.createUserGroupsMap(stackId, cluster, desiredConfigMap)).andReturn(userGroupsMap).anyTimes();

    PowerMock.expectNew(File.class, new Class<?>[]{String.class}, anyObject(String.class)).andReturn(newFile).anyTimes();
    PowerMock.mockStatic(File.class);
    expect(File.createTempFile(anyString(), anyString(), anyObject(File.class))).andReturn(newFile);

    String commandLine = "ambari-python-wrap /tmp/stacks/S1/V1/PIG/package/null generate_configs " + newFile +
        " /tmp/stacks/S1/V1/PIG/package /var/lib/ambari-server/tmp/structured-out.json " +
        "INFO /var/lib/ambari-server/tmp";

    if (System.getProperty("os.name").contains("Windows")) {
      commandLine = "ambari-python-wrap " + stackRoot +
          "\\PIG\\package\\null generate_configs null " +
          stackRoot + "\\PIG\\package /var/lib/ambari-server/tmp\\structured-out.json " +
          "INFO /var/lib/ambari-server/tmp";
    }

    ProcessBuilder processBuilder = PowerMock.createNiceMock(ProcessBuilder.class);
    PowerMock.expectNew(ProcessBuilder.class, Arrays.asList(commandLine.split("\\s+"))).andReturn(processBuilder).once();
    expect(processBuilder.start()).andReturn(process).once();
    InputStream inputStream = new ByteArrayInputStream("some logging info".getBytes());
    expect(process.getInputStream()).andReturn(inputStream);

    ClientConfigResourceProvider.TarUtils tarUtilMock = PowerMockito.mock(ClientConfigResourceProvider.TarUtils.class);
    whenNew(ClientConfigResourceProvider.TarUtils.class).withAnyArguments().thenReturn(tarUtilMock);
    tarUtilMock.tarConfigFiles();
    expectLastCall().once();

    // create the request
    Request request = PropertyHelper.getReadRequest(ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID, "c1",
        ClientConfigResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID,
        ClientConfigResourceProvider.COMPONENT_SERVICE_NAME_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID).
        equals("c1").and().property(ClientConfigResourceProvider.COMPONENT_SERVICE_NAME_PROPERTY_ID).equals("PIG").toPredicate();

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo, commandScriptDefinition,
        clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
        runtime, process);
    PowerMock.replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Set<Resource> resources = provider.getResources(request, predicate);
    assertFalse(resources.isEmpty());
    assertFalse(newFile.exists());

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo, commandScriptDefinition,
        clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
        runtime, process);
    PowerMock.verifyAll();
  }

  private void testGetResourcesFromCommonServices(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.ClientConfig;

    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);

    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    CommandScriptDefinition commandScriptDefinition = createNiceMock(CommandScriptDefinition.class);
    Config clusterConfig = createNiceMock(Config.class);
    DesiredConfig desiredConfig = createNiceMock(DesiredConfig.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    Configuration configuration = PowerMock.createStrictMockAndExpectNew(Configuration.class);

    File mockFile = PowerMock.createNiceMock(File.class);
    Runtime runtime = createMock(Runtime.class);
    Process process = createNiceMock(Process.class);

    Map<String, DesiredConfig> desiredConfigMap = new HashMap<>();
    desiredConfigMap.put("hive-site", desiredConfig);
    Map<String, Map<String, String>> allConfigTags = new HashMap<>();
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, Map<String, String>> configTags = new HashMap<>();
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();

    ClientConfigFileDefinition clientConfigFileDefinition = new ClientConfigFileDefinition();
    clientConfigFileDefinition.setDictionaryName("pig-env");
    clientConfigFileDefinition.setFileName("pig-env.sh");
    clientConfigFileDefinition.setType("env");
    List<ClientConfigFileDefinition> clientConfigFileDefinitionList = new LinkedList<>();
    clientConfigFileDefinitionList.add(clientConfigFileDefinition);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // create the request
    Request request = PropertyHelper.getReadRequest(ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID, "c1",
        ClientConfigResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID,
        ClientConfigResourceProvider.COMPONENT_SERVICE_NAME_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID).
        equals("c1").and().property(ClientConfigResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("PIG").
        and().property(ClientConfigResourceProvider.COMPONENT_SERVICE_NAME_PROPERTY_ID).equals("PIG").
        toPredicate();

    String clusterName = "C1";
    String serviceName = "PIG";
    String componentName = "PIG";
    String displayName = "Pig Client";
    String hostName = "Host100";
    String publicHostName = "Host100";
    String desiredState = "INSTALLED";

    String stackName = "S1";
    String stackVersion = "V1";

    String packageFolder = StackManager.COMMON_SERVICES + "/PIG/package";
    String commonServicesPath = "/var/lib/ambari-server/src/main/resources" + File.separator + "common-services";

    if (System.getProperty("os.name").contains("Windows")) {
      packageFolder = StackManager.COMMON_SERVICES + "\\PIG\\package";
    }

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse(clusterName, serviceName,
        componentName, displayName, hostName, publicHostName, desiredState, "", null, null, null,
        null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<>();
    responses.add(shr1);

    Map<String, String> returnConfigMap = new HashMap<>();
    returnConfigMap.put(Configuration.SERVER_TMP_DIR.getKey(), Configuration.SERVER_TMP_DIR.getDefaultValue());
    returnConfigMap.put(Configuration.AMBARI_PYTHON_WRAP.getKey(), Configuration.AMBARI_PYTHON_WRAP.getDefaultValue());

    // set expectations
    expect(managementController.getConfigHelper()).andReturn(configHelper);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();
    expect(configHelper.getEffectiveConfigProperties(cluster, configTags)).andReturn(properties);
    expect(configHelper.getEffectiveConfigAttributes(cluster, configTags)).andReturn(attributes);
    expect(configuration.getConfigsMap()).andReturn(returnConfigMap);
    expect(configuration.getResourceDirPath()).andReturn("/var/lib/ambari-server/src/main/resources");
    expect(configuration.getExternalScriptThreadPoolSize()).andReturn(Configuration.THREAD_POOL_SIZE_FOR_EXTERNAL_SCRIPT.getDefaultValue());
    expect(configuration.getExternalScriptTimeout()).andReturn(Configuration.EXTERNAL_SCRIPT_TIMEOUT.getDefaultValue());

    Map<String, String> props = new HashMap<>();
    props.put("key", "value");
    expect(clusterConfig.getProperties()).andReturn(props);
    expect(configHelper.getEffectiveDesiredTags(cluster, null)).andReturn(allConfigTags);
    expect(cluster.getClusterName()).andReturn(clusterName);
    expect(managementController.getHostComponents(EasyMock.anyObject())).andReturn(responses).anyTimes();

    PowerMock.mockStaticPartial(StageUtils.class, "getClusterHostInfo");
    Map<String, Set<String>> clusterHostInfo = new HashMap<>();
    Set<String> all_hosts = new HashSet<>(Arrays.asList("Host100", "Host101", "Host102"));
    Set<String> some_hosts = new HashSet<>(Arrays.asList("0-1", "2"));
    Set<String> ohter_hosts = Collections.singleton("0,1");
    Set<String> clusterHostTypes = new HashSet<>(Arrays.asList("nm_hosts", "hs_host",
        "namenode_host", "rm_host", "snamenode_host", "slave_hosts", "zookeeper_hosts"));
    for (String hostTypes : clusterHostTypes) {
      if (hostTypes.equals("slave_hosts")) {
        clusterHostInfo.put(hostTypes, ohter_hosts);
      } else {
        clusterHostInfo.put(hostTypes, some_hosts);
      }
    }
    clusterHostInfo.put("all_hosts", all_hosts);
    expect(StageUtils.getClusterHostInfo(cluster)).andReturn(clusterHostInfo);

    expect(stackId.getStackName()).andReturn(stackName).anyTimes();
    expect(stackId.getStackVersion()).andReturn(stackVersion).anyTimes();

    expect(ambariMetaInfo.getComponent(stackName, stackVersion, serviceName, componentName)).andReturn(componentInfo);
    expect(ambariMetaInfo.getService(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getServicePackageFolder()).andReturn(packageFolder);
    expect(ambariMetaInfo.getComponent(anyString(), anyString(),
        anyString(), anyString())).andReturn(componentInfo).anyTimes();
    expect(componentInfo.getCommandScript()).andReturn(commandScriptDefinition);
    expect(componentInfo.getClientConfigFiles()).andReturn(clientConfigFileDefinitionList);
    expect(cluster.getConfig("hive-site", null)).andReturn(clusterConfig);
    expect(clusterConfig.getType()).andReturn("hive-site").anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigMap);
    expect(clusters.getHost(hostName)).andReturn(host);

    expect(cluster.getService(serviceName)).andReturn(service).atLeastOnce();
    expect(service.getServiceComponent(componentName)).andReturn(serviceComponent).atLeastOnce();
    expect(serviceComponent.getDesiredStackId()).andReturn(stackId).atLeastOnce();

    HashMap<String, String> rcaParams = new HashMap<>();
    rcaParams.put("key", "value");
    expect(managementController.getRcaParameters()).andReturn(rcaParams).anyTimes();
    expect(ambariMetaInfo.getService(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getOsSpecifics()).andReturn(new HashMap<>()).anyTimes();
    PowerMock.expectNew(File.class, new Class<?>[]{String.class}, anyObject(String.class)).andReturn(mockFile).anyTimes();
    PowerMock.mockStatic(File.class);
    expect(mockFile.exists()).andReturn(true);
    expect(File.createTempFile(anyString(), anyString(), anyObject(File.class))).andReturn(PowerMock.createNiceMock(File.class));
    PowerMock.createNiceMockAndExpectNew(PrintWriter.class, anyObject());
    PowerMock.mockStatic(Runtime.class);
    String commandLine = "ambari-python-wrap " + commonServicesPath + "/PIG/package/null generate_configs null " +
        commonServicesPath + "/PIG/package /var/lib/ambari-server/tmp/structured-out.json " +
        "INFO /var/lib/ambari-server/tmp";

    if (System.getProperty("os.name").contains("Windows")) {
      commandLine = "ambari-python-wrap " + commonServicesPath +
          "\\PIG\\package\\null generate_configs null " +
          commonServicesPath + "\\PIG\\package /var/lib/ambari-server/tmp\\structured-out.json " +
          "INFO /var/lib/ambari-server/tmp";
    }

    ProcessBuilder processBuilder = PowerMock.createNiceMock(ProcessBuilder.class);
    PowerMock.expectNew(ProcessBuilder.class, Arrays.asList(commandLine.split("\\s+"))).andReturn(processBuilder).once();
    expect(processBuilder.start()).andReturn(process).once();
    InputStream inputStream = new ByteArrayInputStream("some logging info".getBytes());
    expect(process.getInputStream()).andReturn(inputStream);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo, commandScriptDefinition,
        clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
        runtime, process);
    PowerMock.replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Set<Resource> resources = provider.getResources(request, predicate);
    assertFalse(resources.isEmpty());

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo, commandScriptDefinition,
        clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
        runtime, process);
    PowerMock.verifyAll();
  }


}
