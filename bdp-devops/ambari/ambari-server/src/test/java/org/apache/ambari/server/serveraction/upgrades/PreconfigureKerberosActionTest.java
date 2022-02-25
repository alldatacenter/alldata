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

package org.apache.ambari.server.serveraction.upgrades;

import static org.apache.ambari.server.serveraction.upgrades.PreconfigureKerberosAction.UPGRADE_DIRECTION_KEY;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.newCapture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariCustomCommandExecutionHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.KerberosHelperImpl;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.hooks.AmbariEventFactory;
import org.apache.ambari.server.hooks.HookContext;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.PostUserCreationHookContext;
import org.apache.ambari.server.hooks.users.UserCreatedEvent;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.scheduler.ExecutionSchedulerImpl;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceComponentImpl;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroupImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.MapUtils;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

public class PreconfigureKerberosActionTest extends EasyMockSupport {

  private static final String CLUSTER_NAME = "c1";

  @Test
  public void testSkipWhenDowngrade() throws Exception {

    Injector injector = getInjector();

    Map<String, String> commandParams = getDefaultCommandParams();
    commandParams.put(UPGRADE_DIRECTION_KEY, Direction.DOWNGRADE.name());

    ExecutionCommand executionCommand = createMockExecutionCommand(commandParams);

    replayAll();

    injector.getInstance(AmbariMetaInfo.class).init();

    PreconfigureKerberosAction action = injector.getInstance(PreconfigureKerberosAction.class);
    ConcurrentMap<String, Object> context = new ConcurrentHashMap<>();
    action.setExecutionCommand(executionCommand);
    action.execute(context);

    verifyAll();
  }

  @Test
  public void testSkipWhenNotKerberos() throws Exception {
    Injector injector = getInjector();

    ExecutionCommand executionCommand = createMockExecutionCommand(getDefaultCommandParams());

    Cluster cluster = createMockCluster(SecurityType.NONE, Collections.<Host>emptyList(),
        Collections.<String, Service>emptyMap(), Collections.<String, List<ServiceComponentHost>>emptyMap(),
        createNiceMock(StackId.class), Collections.<String, Config>emptyMap());

    Clusters clusters = injector.getInstance(Clusters.class);
    expect(clusters.getCluster(CLUSTER_NAME)).andReturn(cluster).atLeastOnce();

    replayAll();

    injector.getInstance(AmbariMetaInfo.class).init();

    PreconfigureKerberosAction action = injector.getInstance(PreconfigureKerberosAction.class);
    ConcurrentMap<String, Object> context = new ConcurrentHashMap<>();
    action.setExecutionCommand(executionCommand);
    action.execute(context);

    verifyAll();
  }

  private Long hostId = 1L;
  private Host createMockHost(String hostname) {
    Host host = createNiceMock(Host.class);
    expect(host.getHostName()).andReturn(hostname).anyTimes();
    expect(host.getHostId()).andReturn(hostId).anyTimes();
    hostId++;
    return host;
  }

  @Test
  @Ignore("Update accordingly to changes")
  public void testUpgrade() throws Exception {
    Capture<? extends Map<String, String>> captureCoreSiteProperties = newCapture();

    Injector injector = getInjector();

    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    EntityManager entityManager = injector.getInstance(EntityManager.class);
    HostEntity hostEntityMock = createNiceMock(HostEntity.class);
    KerberosKeytabPrincipalEntity principalMock = createNiceMock(KerberosKeytabPrincipalEntity.class);
    expect(principalMock.getHostId()).andReturn(1L).anyTimes();
    expect(hostDAO.findByName(anyString())).andReturn(hostEntityMock).anyTimes();
    expect(hostDAO.findById(anyLong())).andReturn(hostEntityMock).anyTimes();
    expect(entityManager.find(eq(KerberosKeytabEntity.class), anyString())).andReturn(createNiceMock(KerberosKeytabEntity.class)).anyTimes();
//    expect(entityManager.find(eq(KerberosPrincipalHostEntity.class), anyObject())).andReturn(createNiceMock(KerberosPrincipalHostEntity.class)).anyTimes();
    expect(entityManager.find(eq(KerberosKeytabPrincipalEntity.class), anyObject())).andReturn(principalMock).anyTimes();

    ExecutionCommand executionCommand = createMockExecutionCommand(getDefaultCommandParams());

    UpgradeEntity upgradeProgress = createMock(UpgradeEntity.class);

    StackId targetStackId = createMock(StackId.class);
    expect(targetStackId.getStackId()).andReturn("HDP-2.6").anyTimes();
    expect(targetStackId.getStackName()).andReturn("HDP").anyTimes();
    expect(targetStackId.getStackVersion()).andReturn("2.6").anyTimes();

    final String hostName1 = "c6401.ambari.apache.org";
    final String hostName2 = "c6402.ambari.apache.org";
    final String hostName3 = "c6403.ambari.apache.org";

    final Host host1 = createMockHost(hostName1);
    Host host2 = createMockHost(hostName2);
    Host host3 = createMockHost(hostName3);
    Map<String, Host> hosts = new HashMap<>();
    hosts.put(hostName1, host1);
    hosts.put(hostName2, host2);
    hosts.put(hostName3, host3);

    Map<String, ServiceComponentHost> nnSchs = Collections.singletonMap(hostName1, createMockServiceComponentHost("HDFS", "NAMENODE", hostName1, host1));
    Map<String, ServiceComponentHost> rmSchs = Collections.singletonMap(hostName2, createMockServiceComponentHost("YARN", "RESOURCEMANAGER", hostName2, host2));
    Map<String, ServiceComponentHost> nmSchs = Collections.singletonMap(hostName2, createMockServiceComponentHost("YARN", "NODEMANAGER", hostName2, host2));
    Map<String, ServiceComponentHost> dnSchs = new HashMap<>();
    final Map<String, ServiceComponentHost> hcSchs = new HashMap<>();
    Map<String, ServiceComponentHost> zkSSchs = new HashMap<>();
    Map<String, ServiceComponentHost> zkCSchs = new HashMap<>();
    Map<String, List<ServiceComponentHost>> serviceComponentHosts = new HashMap<>();

    for (Map.Entry<String, Host> entry : hosts.entrySet()) {
      String hostname = entry.getKey();
      List<ServiceComponentHost> list = new ArrayList<>();
      ServiceComponentHost sch;

      sch = createMockServiceComponentHost("HDFS", "DATANODE", hostname, entry.getValue());
      dnSchs.put(hostname, sch);
      list.add(sch);

      sch = createMockServiceComponentHost("HDFS", "HDFS_CLIENT", hostname, entry.getValue());
      hcSchs.put(hostname, sch);
      list.add(sch);

      sch = createMockServiceComponentHost("ZOOKEEPER", "ZOOKEEPER_SERVER", hostname, entry.getValue());
      zkSSchs.put(hostname, sch);
      list.add(sch);

      sch = createMockServiceComponentHost("ZOOKEEPER", "ZOOKEEPER_CLIENT", hostname, entry.getValue());
      zkCSchs.put(hostname, sch);
      list.add(sch);

      serviceComponentHosts.put(hostname, list);
    }


    Map<String, ServiceComponent> hdfsComponents = new HashMap<>();
    hdfsComponents.put("NAMENODE", createMockServiceComponent("NAMENODE", false, nnSchs));
    hdfsComponents.put("DATANODE", createMockServiceComponent("DATANODE", false, dnSchs));
    hdfsComponents.put("HDFS_CLIENT", createMockServiceComponent("HDFS_CLIENT", true, hcSchs));

    Map<String, ServiceComponent> yarnComponents = new HashMap<>();
    yarnComponents.put("RESOURCEMANAGER", createMockServiceComponent("RESOURCEMANAGER", false, rmSchs));
    yarnComponents.put("NODEMANAGER", createMockServiceComponent("NODEMANAGER", false, nmSchs));

    Map<String, ServiceComponent> zkCompnents = new HashMap<>();
    yarnComponents.put("ZOOKEEPER_SERVER", createMockServiceComponent("ZOOKEEPER_SERVER", false, zkSSchs));
    yarnComponents.put("ZOOKEEPER_CLIENT", createMockServiceComponent("ZOOKEEPER_CLIENT", true, zkCSchs));

    Service hdfsService = createMockService("HDFS", hdfsComponents, targetStackId);
    Service yarnService = createMockService("YARN", yarnComponents, targetStackId);
    Service zkService = createMockService("ZOOKEEPER", zkCompnents, targetStackId);

    Map<String, Service> installedServices = new HashMap<>();
    installedServices.put("HDFS", hdfsService);
    installedServices.put("YARN", yarnService);
    installedServices.put("ZOOKEEPER", zkService);

    Map<String, Map<String, String>> clusterConfig = getClusterConfig();

    Map<String, Config> clusterConfigs = new HashMap<>();
    for (Map.Entry<String, Map<String, String>> entry : clusterConfig.entrySet()) {
      clusterConfigs.put(entry.getKey(), createMockConfig(entry.getValue()));
    }

    Cluster cluster = createMockCluster(SecurityType.KERBEROS, hosts.values(), installedServices, serviceComponentHosts, targetStackId, clusterConfigs);
    expect(cluster.getUpgradeInProgress()).andReturn(upgradeProgress).once();

    RepositoryVersionEntity targetRepositoryVersion = createMock(RepositoryVersionEntity.class);
    expect(targetRepositoryVersion.getStackId()).andReturn(targetStackId).atLeastOnce();

    UpgradeContext upgradeContext = createMock(UpgradeContext.class);
    expect(upgradeContext.getTargetRepositoryVersion(anyString())).andReturn(targetRepositoryVersion).atLeastOnce();

    UpgradeContextFactory upgradeContextFactory = injector.getInstance(UpgradeContextFactory.class);
    expect(upgradeContextFactory.create(cluster, upgradeProgress)).andReturn(upgradeContext).once();

    createMockClusters(injector, cluster);

    List<PropertyInfo> knoxProperties = Arrays.asList(
        crateMockPropertyInfo("knox-env.xml", "knox_user", "knox"),
        crateMockPropertyInfo("knox-env.xml", "knox_group", "knox"),
        crateMockPropertyInfo("knox-env.xml", "knox_principal_name", "KERBEROS_PRINCIPAL"),
        crateMockPropertyInfo("gateway-site.xml", "gateway.port", "8443"),
        crateMockPropertyInfo("gateway-site.xml", "gateway.path", "gateway")
    );

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    expect(ambariMetaInfo.getKerberosDescriptor("HDP", "2.6", false)).
        andReturn(getKerberosDescriptor(false)).once();
    expect(ambariMetaInfo.getKerberosDescriptor("HDP", "2.6", true)).
        andReturn(getKerberosDescriptor(true)).once();
    expect(ambariMetaInfo.isValidService("HDP", "2.6", "BEACON"))
        .andReturn(false).anyTimes();
    expect(ambariMetaInfo.isValidService("HDP", "2.6", "KNOX"))
        .andReturn(true).anyTimes();
    expect(ambariMetaInfo.getService("HDP", "2.6", "KNOX"))
        .andReturn(createMockServiceInfo("KNOX", knoxProperties, Collections.singletonList(createMockComponentInfo("KNOX_GATEWAY")))).anyTimes();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    expect(managementController.findConfigurationTagsWithOverrides(cluster, null))
        .andReturn(clusterConfig).once();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();

    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    expect(configHelper.getEffectiveConfigProperties(cluster, clusterConfig)).andReturn(clusterConfig).anyTimes();
    configHelper.updateConfigType(eq(cluster), eq(targetStackId), eq(managementController), eq("core-site"), capture(captureCoreSiteProperties), anyObject(Collection.class), eq("admin"), anyString());
    expectLastCall().once();

    TopologyManager topologyManager = injector.getInstance(TopologyManager.class);
    expect(topologyManager.getPendingHostComponents()).andReturn(Collections.<String, Collection<String>>emptyMap()).anyTimes();

    StackAdvisorHelper stackAdvisorHelper = injector.getInstance(StackAdvisorHelper.class);
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class)))
        .andAnswer(new IAnswer<RecommendationResponse>() {
          @Override
          public RecommendationResponse answer() throws Throwable {
            Object[] args = getCurrentArguments();
            StackAdvisorRequest request = (StackAdvisorRequest) args[0];
            StackAdvisorRequest.StackAdvisorRequestType requestType = request.getRequestType();

            if (requestType == StackAdvisorRequest.StackAdvisorRequestType.HOST_GROUPS) {
              RecommendationResponse.Blueprint blueprint = new RecommendationResponse.Blueprint();
              blueprint.setHostGroups(new HashSet<>(Arrays.asList(
                  createRecommendationHostGroup(hostName1,
                      Arrays.asList("ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT", "HDFS_CLIENT", "DATANODE", "NAMENODE", "KNOX_GATEWAY")),
                  createRecommendationHostGroup(hostName2,
                      Arrays.asList("ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT", "HDFS_CLIENT", "DATANODE", "RESOURCEMANAGER", "NODEMANAGER")),
                  createRecommendationHostGroup(hostName3,
                      Arrays.asList("ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT", "HDFS_CLIENT", "DATANODE"))
              )));
              Set<RecommendationResponse.BindingHostGroup> bindingHostGroups = new HashSet<>(Arrays.asList(
                  createBindingHostGroup(hostName1),
                  createBindingHostGroup(hostName2),
                  createBindingHostGroup(hostName3)
              ));

              RecommendationResponse.BlueprintClusterBinding binding = new RecommendationResponse.BlueprintClusterBinding();
              binding.setHostGroups(bindingHostGroups);

              RecommendationResponse.Recommendation recommendation = new RecommendationResponse.Recommendation();
              recommendation.setBlueprint(blueprint);
              recommendation.setBlueprintClusterBinding(binding);

              RecommendationResponse response = new RecommendationResponse();
              response.setRecommendations(recommendation);
              return response;
            } else {
              return null;
            }
          }
        })
        .anyTimes();

    replayAll();

    ambariMetaInfo.init();
    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    PreconfigureKerberosAction action = injector.getInstance(PreconfigureKerberosAction.class);
    ConcurrentMap<String, Object> context = new ConcurrentHashMap<>();
    action.setExecutionCommand(executionCommand);
    action.execute(context);

    verifyAll();

    Assert.assertTrue(captureCoreSiteProperties.hasCaptured());

    Map<String, String> capturedProperties = captureCoreSiteProperties.getValue();
    Assert.assertFalse(MapUtils.isEmpty(capturedProperties));


    String expectedAuthToLocalRules = "" +
        "RULE:[1:$1@$0](ambari-qa-c1@EXAMPLE.COM)s/.*/ambari-qa/\n" +
        "RULE:[1:$1@$0](hdfs-c1@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "RULE:[2:$1@$0](beacon@EXAMPLE.COM)s/.*/beacon/\n" +
        "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](knox@EXAMPLE.COM)s/.*/knox/\n" +
        "RULE:[2:$1@$0](nm@EXAMPLE.COM)s/.*/${yarn-env/yarn_user}/\n" +
        "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/${yarn-env/yarn_user}/\n" +
        "DEFAULT";

    Assert.assertEquals(3, capturedProperties.size());
    Assert.assertEquals("users", capturedProperties.get("hadoop.proxyuser.knox.groups"));
    Assert.assertEquals("c6401.ambari.apache.org", capturedProperties.get("hadoop.proxyuser.knox.hosts"));
    Assert.assertEquals(expectedAuthToLocalRules, capturedProperties.get("hadoop.security.auth_to_local"));
  }

  private RecommendationResponse.BindingHostGroup createBindingHostGroup(String hostName) {
    RecommendationResponse.BindingHostGroup bindingHostGroup = new RecommendationResponse.BindingHostGroup();
    bindingHostGroup.setName(hostName);
    bindingHostGroup.setHosts(Collections.singleton(Collections.singletonMap("fqdn", hostName)));
    return bindingHostGroup;
  }

  private RecommendationResponse.HostGroup createRecommendationHostGroup(String hostName, List<String> components) {
    Set<Map<String, String>> componentDetails = new HashSet<>();
    for (String component : components) {
      componentDetails.add(Collections.singletonMap("name", component));
    }

    RecommendationResponse.HostGroup hostGroup = new RecommendationResponse.HostGroup();
    hostGroup.setComponents(componentDetails);
    hostGroup.setName(hostName);
    return hostGroup;
  }

  private ComponentInfo createMockComponentInfo(String componentName) {
    ComponentInfo componentInfo = createMock(ComponentInfo.class);
    expect(componentInfo.getName()).andReturn(componentName).anyTimes();
    return componentInfo;
  }

  private PropertyInfo crateMockPropertyInfo(String fileName, String propertyName, String propertyValue) {
    PropertyInfo propertyInfo = createMock(PropertyInfo.class);
    expect(propertyInfo.getFilename()).andReturn(fileName).anyTimes();
    expect(propertyInfo.getName()).andReturn(propertyName).anyTimes();
    expect(propertyInfo.getValue()).andReturn(propertyValue).anyTimes();
    return propertyInfo;
  }

  private ServiceInfo createMockServiceInfo(String name, List<PropertyInfo> properties, List<ComponentInfo> components) {
    ServiceInfo serviceInfo = createMock(ServiceInfo.class);
    expect(serviceInfo.getName()).andReturn(name).anyTimes();
    expect(serviceInfo.getProperties()).andReturn(properties).anyTimes();
    expect(serviceInfo.getComponents()).andReturn(components).anyTimes();
    return serviceInfo;
  }

  private Map<String, Map<String, String>> getClusterConfig() throws URISyntaxException, FileNotFoundException {
    URL url = ClassLoader.getSystemResource("PreconfigureActionTest_cluster_config.json");
    return new Gson().fromJson(new FileReader(new File(url.toURI())),
        new TypeToken<Map<String, Map<String, String>>>() {
        }.getType());
  }

  private KerberosDescriptor getKerberosDescriptor(boolean includePreconfigureData) throws URISyntaxException, IOException {
    URL url;

    if (includePreconfigureData) {
      url = ClassLoader.getSystemResource("PreconfigureActionTest_kerberos_descriptor_stack_preconfigure.json");
    } else {
      url = ClassLoader.getSystemResource("PreconfigureActionTest_kerberos_descriptor_stack.json");
    }

    return new KerberosDescriptorFactory().createInstance(new File(url.toURI()));
  }

  private ServiceComponent createMockServiceComponent(String name, Boolean isClientComponent, Map<String, ServiceComponentHost> serviceComponentHostMap) throws AmbariException {
    ServiceComponent serviceComponent = createMock(ServiceComponent.class);
    expect(serviceComponent.getName()).andReturn(name).anyTimes();
    expect(serviceComponent.isClientComponent()).andReturn(isClientComponent).anyTimes();

    for (Map.Entry<String, ServiceComponentHost> entry : serviceComponentHostMap.entrySet()) {
      expect(serviceComponent.getServiceComponentHost(entry.getKey())).andReturn(serviceComponentHostMap.get(entry.getKey())).anyTimes();
    }

    expect(serviceComponent.getServiceComponentHosts()).andReturn(serviceComponentHostMap).anyTimes();

    return serviceComponent;
  }

  private ServiceComponentHost createMockServiceComponentHost(String serviceName, String componentName, String hostname, Host host) {
    ServiceComponentHost serviceComponentHost = createMock(ServiceComponentHost.class);
    expect(serviceComponentHost.getServiceName()).andReturn(serviceName).anyTimes();
    expect(serviceComponentHost.getServiceComponentName()).andReturn(componentName).anyTimes();
    expect(serviceComponentHost.getHostName()).andReturn(hostname).anyTimes();
    expect(serviceComponentHost.getHost()).andReturn(host).anyTimes();
    expect(serviceComponentHost.getComponentAdminState()).andReturn(HostComponentAdminState.INSERVICE).anyTimes();

    return serviceComponentHost;
  }

  private Service createMockService(String name, Map<String, ServiceComponent> components, StackId desiredStackId) {
    Service service = createMock(Service.class);
    expect(service.getName()).andReturn(name).anyTimes();
    expect(service.getServiceComponents()).andReturn(components).anyTimes();
    expect(service.getDesiredStackId()).andReturn(desiredStackId).anyTimes();
    return service;
  }

  private Clusters createMockClusters(Injector injector, Cluster cluster) throws AmbariException {
    Clusters clusters = injector.getInstance(Clusters.class);
    expect(clusters.getCluster(CLUSTER_NAME)).andReturn(cluster).atLeastOnce();
    return clusters;
  }

  private Cluster createMockCluster(SecurityType securityType, Collection<Host> hosts,
                                    Map<String, Service> services,
                                    Map<String, List<ServiceComponentHost>> serviceComponentHosts,
                                    StackId currentStackId, final Map<String, Config> clusterConfigs) {
    final Cluster cluster = createMock(Cluster.class);
    expect(cluster.getSecurityType()).andReturn(securityType).anyTimes();
    expect(cluster.getClusterName()).andReturn(CLUSTER_NAME).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hosts).anyTimes();
    expect(cluster.getServices()).andReturn(services).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(currentStackId).anyTimes();

    for (Map.Entry<String, List<ServiceComponentHost>> entry : serviceComponentHosts.entrySet()) {
      expect(cluster.getServiceComponentHosts(entry.getKey())).andReturn(entry.getValue()).atLeastOnce();
    }

    expect(cluster.getServiceComponentHostMap(null, new HashSet<>(Arrays.asList("HDFS", "ZOOKEEPER", "YARN", "KNOX"))))
        .andReturn(null)
        .anyTimes();
    expect(cluster.getServiceComponentHostMap(null, new HashSet<>(Arrays.asList("HDFS", "ZOOKEEPER", "YARN"))))
        .andReturn(null)
        .anyTimes();

    Map<String, String> configTypeService = new HashMap<>();
    configTypeService.put("hdfs-site", "HDFS");
    configTypeService.put("core-site", "HDFS");
    configTypeService.put("hadoop-env", "HDFS");
    configTypeService.put("cluster-env", null);
    configTypeService.put("kerberos-env", "KERBEROS");
    configTypeService.put("ranger-hdfs-audit", "RANGER");
    configTypeService.put("zookeeper-env", "ZOOKEEPER");
    configTypeService.put("gateway-site", "KNOX");

    for (Map.Entry<String, String> entry : configTypeService.entrySet()) {
      expect(cluster.getServiceByConfigType(entry.getKey())).andReturn(entry.getValue()).anyTimes();
    }

    for (Map.Entry<String, Config> entry : clusterConfigs.entrySet()) {
      expect(cluster.getDesiredConfigByType(entry.getKey())).andReturn(entry.getValue()).anyTimes();
      expect(cluster.getConfigsByType(entry.getKey())).andReturn(Collections.singletonMap(entry.getKey(), entry.getValue())).anyTimes();
      expect(cluster.getConfigPropertiesTypes(entry.getKey())).andReturn(Collections.<PropertyInfo.PropertyType, Set<String>>emptyMap()).anyTimes();
    }

    return cluster;
  }

  private Config createMockConfig(Map<String, String> properties) {
    Config config = createMock(Config.class);
    expect(config.getProperties()).andReturn(properties).anyTimes();
    expect(config.getPropertiesAttributes()).andReturn(Collections.<String, Map<String, String>>emptyMap()).anyTimes();
    return config;
  }

  private Map<String, String> getDefaultCommandParams() {
    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", CLUSTER_NAME);
    commandParams.put(UPGRADE_DIRECTION_KEY, Direction.UPGRADE.name());
    return commandParams;
  }

  private ExecutionCommand createMockExecutionCommand(Map<String, String> commandParams) {
    ExecutionCommand executionCommand = createMock(ExecutionCommand.class);
    expect(executionCommand.getCommandParams()).andReturn(commandParams).atLeastOnce();
    return executionCommand;
  }

  private Injector getInjector() {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        PartialNiceMockBinder.newBuilder(PreconfigureKerberosActionTest.this)
            .addActionDBAccessorConfigsBindings().addLdapBindings().addPasswordEncryptorBindings().build().configure(binder());

        bind(EntityManager.class).toInstance(createMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createMock(DBAccessor.class));
        bind(UpgradeContextFactory.class).toInstance(createMock(UpgradeContextFactory.class));
        bind(OsFamily.class).toInstance(createMock(OsFamily.class));
        bind(StackManagerFactory.class).toInstance(createMock(StackManagerFactory.class));
        bind(StageFactory.class).toInstance(createMock(StageFactory.class));
        bind(AmbariMetaInfo.class).toInstance(createMock(AmbariMetaInfo.class));
        bind(AmbariCustomCommandExecutionHelper.class).toInstance(createMock(AmbariCustomCommandExecutionHelper.class));
        bind(ActionManager.class).toInstance(createMock(ActionManager.class));
        bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
        bind(ArtifactDAO.class).toInstance(createNiceMock(ArtifactDAO.class));
        bind(KerberosPrincipalDAO.class).toInstance(createNiceMock(KerberosPrincipalDAO.class));
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(RoleGraphFactory.class).toInstance(createMock(RoleGraphFactory.class));
        bind(RequestFactory.class).toInstance(createMock(RequestFactory.class));
        bind(RequestExecutionFactory.class).toInstance(createMock(RequestExecutionFactory.class));
        bind(CredentialStoreService.class).toInstance(createMock(CredentialStoreService.class));
        bind(TopologyManager.class).toInstance(createNiceMock(TopologyManager.class));
        bind(ConfigFactory.class).toInstance(createMock(ConfigFactory.class));
        bind(PersistedState.class).toInstance(createMock(PersistedState.class));
        bind(ConfigureClusterTaskFactory.class).toInstance(createNiceMock(ConfigureClusterTaskFactory.class));
        bind(Configuration.class).toInstance(new Configuration(new Properties()));
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
        bind(HookService.class).to(UserHookService.class);
        bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));

        bind(AmbariManagementController.class).toInstance(createMock(AmbariManagementController.class));
        bind(KerberosHelper.class).to(KerberosHelperImpl.class);
        bind(Clusters.class).toInstance(createMock(Clusters.class));
        bind(StackAdvisorHelper.class).toInstance(createMock(StackAdvisorHelper.class));
        bind(ConfigHelper.class).toInstance(createMock(ConfigHelper.class));
        bind(HostDAO.class).toInstance(createMock(HostDAO.class));
        bind(ExecutionScheduler.class).to(ExecutionSchedulerImpl.class);
        bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);

        install(new FactoryModuleBuilder().implement(HookContext.class, PostUserCreationHookContext.class)
            .build(HookContextFactory.class));
        install(new FactoryModuleBuilder().implement(
            ServiceComponentHost.class, ServiceComponentHostImpl.class).build(
            ServiceComponentHostFactory.class));
        install(new FactoryModuleBuilder().implement(
            ServiceComponent.class, ServiceComponentImpl.class).build(
            ServiceComponentFactory.class));
        install(new FactoryModuleBuilder().implement(
            ConfigGroup.class, ConfigGroupImpl.class).build(ConfigGroupFactory.class));
        install(new FactoryModuleBuilder().implement(AmbariEvent.class, Names.named("userCreated"), UserCreatedEvent.class)
            .build(AmbariEventFactory.class));
        install(new FactoryModuleBuilder().implement(
            Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
        install(new FactoryModuleBuilder().implement(
            Host.class, HostImpl.class).build(HostFactory.class));
        install(new FactoryModuleBuilder().implement(
            Service.class, ServiceImpl.class).build(ServiceFactory.class));
      }
    });
  }
}
