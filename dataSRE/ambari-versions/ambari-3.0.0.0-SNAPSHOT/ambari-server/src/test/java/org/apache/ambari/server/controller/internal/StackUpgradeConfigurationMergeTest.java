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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.AuditLoggerDefaultImpl;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.eclipse.jetty.server.session.SessionHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Tests that
 * {@link UpgradeHelper#updateDesiredRepositoriesAndConfigs}
 * works correctly.
 */
public class StackUpgradeConfigurationMergeTest extends EasyMockSupport {

  private Injector m_injector;
  private AmbariMetaInfo m_metainfo;

  /**
   * @throws Exception
   */
  @Before
  public void before() throws Exception {
    m_metainfo = createNiceMock(AmbariMetaInfo.class);

    MockModule mockModule = new MockModule();

    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(mockModule);
    reset(m_injector.getInstance(StageDAO.class));
  }

  /**
   *
   */
  @After
  public void teardown() {
  }

  /**
   * Tests that properties which were explicitely removed in the current
   * configurations by stack advisor are not re-introduced as "new" properties
   * accidentally.
   * <p/>
   *
   * HDP 2.1 defaults
   * <ul>
   * <li>foo-site/foo-property-1</li>
   * <li>foo-site/foo-property-2</li>
   * <li>bar-site/bar-property-1</li>
   * </ul>
   *
   * HDP 2.2 defaults
   * <ul>
   * <li>foo-site/foo-property-1</li>
   * <li>foo-site/foo-property-2</li>
   * <li>bar-site/bar-property-1</li>
   * <li>bar-site/bar-property-2</li>
   * </ul>
   *
   * CURRENT 2.1 configs
   * <ul>
   * <li>foo-site/foo-property-1</li>
   * <li>foo-site/foo-property-99</li>
   * <li>bar-site/bar-property-1</li>
   * <li>bar-site/bar-property-20</li>
   * <li>bar-site/bar-property-99</li>
   * </ul>
   *
   * The final merged configurations should detect that {{foo-property-2}}
   * exists in both stacks but is not in the current configs and was therefore
   * purposefully removed. It shoudl also detect that {{bar-property-2}} was
   * added in the new stack and should be added in.
   *
   * @throws Exception
   */
  @Test
  public void testMergedConfigurationsDoNotAddExplicitelyRemovedProperties() throws Exception {
    RepositoryVersionEntity repoVersion211 = createNiceMock(RepositoryVersionEntity.class);
    RepositoryVersionEntity repoVersion220 = createNiceMock(RepositoryVersionEntity.class);

    StackId stack211 = new StackId("HDP-2.1.1");
    StackId stack220 = new StackId("HDP-2.2.0");

    String version211 = "2.1.1.0-1234";
    String version220 = "2.2.0.0-1234";

    expect(repoVersion211.getStackId()).andReturn(stack211).atLeastOnce();
    expect(repoVersion211.getVersion()).andReturn(version211).atLeastOnce();

    expect(repoVersion220.getStackId()).andReturn(stack220).atLeastOnce();
    expect(repoVersion220.getVersion()).andReturn(version220).atLeastOnce();

    Map<String, Map<String, String>> stack211Configs = new HashMap<>();
    Map<String, String> stack211FooType = new HashMap<>();
    Map<String, String> stack211BarType = new HashMap<>();
    stack211Configs.put("foo-site", stack211FooType);
    stack211Configs.put("bar-site", stack211BarType);
    stack211FooType.put("foo-property-1", "stack-211-original");
    stack211FooType.put("foo-property-2", "stack-211-original");
    stack211BarType.put("bar-property-1", "stack-211-original");

    Map<String, Map<String, String>> stack220Configs = new HashMap<>();
    Map<String, String> stack220FooType = new HashMap<>();
    Map<String, String> stack220BarType = new HashMap<>();
    stack220Configs.put("foo-site", stack220FooType);
    stack220Configs.put("bar-site", stack220BarType);
    stack220FooType.put("foo-property-1", "stack-220-original");
    stack220FooType.put("foo-property-2", "stack-220-original");
    stack220BarType.put("bar-property-1", "stack-220-original");
    stack220BarType.put("bar-property-2", "stack-220-original");

    Map<String, String> existingFooType = new HashMap<>();
    Map<String, String> existingBarType = new HashMap<>();

    ClusterConfigEntity fooConfigEntity = createNiceMock(ClusterConfigEntity.class);
    ClusterConfigEntity barConfigEntity = createNiceMock(ClusterConfigEntity.class);

    expect(fooConfigEntity.getType()).andReturn("foo-site");
    expect(barConfigEntity.getType()).andReturn("bar-site");

    Config fooConfig = createNiceMock(Config.class);
    Config barConfig = createNiceMock(Config.class);

    existingFooType.put("foo-property-1", "my-foo-property-1");
    existingBarType.put("bar-property-1", "stack-211-original");

    expect(fooConfig.getType()).andReturn("foo-site").atLeastOnce();
    expect(barConfig.getType()).andReturn("bar-site").atLeastOnce();
    expect(fooConfig.getProperties()).andReturn(existingFooType);
    expect(barConfig.getProperties()).andReturn(existingBarType);

    Map<String, DesiredConfig> desiredConfigurations = new HashMap<>();
    desiredConfigurations.put("foo-site", null);
    desiredConfigurations.put("bar-site", null);

    Service zookeeper = createNiceMock(Service.class);
    expect(zookeeper.getName()).andReturn("ZOOKEEPER").atLeastOnce();
    expect(zookeeper.getServiceComponents()).andReturn(
      new HashMap<>()).once();
    zookeeper.setDesiredRepositoryVersion(repoVersion220);
    expectLastCall().once();

    Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getCurrentStackVersion()).andReturn(stack211).atLeastOnce();
    expect(cluster.getDesiredStackVersion()).andReturn(stack220);
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigurations);
    expect(cluster.getDesiredConfigByType("foo-site")).andReturn(fooConfig);
    expect(cluster.getDesiredConfigByType("bar-site")).andReturn(barConfig);
    expect(cluster.getService("ZOOKEEPER")).andReturn(zookeeper);
    expect(cluster.getDesiredConfigByType("foo-type")).andReturn(fooConfig);
    expect(cluster.getDesiredConfigByType("bar-type")).andReturn(barConfig);

    ConfigHelper configHelper = m_injector.getInstance(ConfigHelper.class);

    expect(configHelper.getDefaultProperties(stack211, "ZOOKEEPER")).andReturn(
        stack211Configs).anyTimes();

    expect(configHelper.getDefaultProperties(stack220, "ZOOKEEPER")).andReturn(
        stack220Configs).anyTimes();

    Capture<Map<String, Map<String, String>>> expectedConfigurationsCapture = EasyMock.newCapture();

    expect(configHelper.createConfigTypes(EasyMock.anyObject(Cluster.class),
        EasyMock.anyObject(StackId.class), EasyMock.anyObject(AmbariManagementController.class),
        EasyMock.capture(expectedConfigurationsCapture), EasyMock.anyObject(String.class),
        EasyMock.anyObject(String.class))).andReturn(true);

    // mock the service config DAO and replay it
    ServiceConfigEntity zookeeperServiceConfig = createNiceMock(ServiceConfigEntity.class);
    expect(zookeeperServiceConfig.getClusterConfigEntities()).andReturn(
        Lists.newArrayList(fooConfigEntity, barConfigEntity));

    ServiceConfigDAO serviceConfigDAOMock = m_injector.getInstance(ServiceConfigDAO.class);
    List<ServiceConfigEntity> latestServiceConfigs = Lists.newArrayList(zookeeperServiceConfig);
    expect(serviceConfigDAOMock.getLastServiceConfigsForService(EasyMock.anyLong(),
        eq("ZOOKEEPER"))).andReturn(latestServiceConfigs).once();

    UpgradeContext context = createNiceMock(UpgradeContext.class);
    expect(context.getCluster()).andReturn(cluster).atLeastOnce();
    expect(context.getType()).andReturn(UpgradeType.ROLLING).atLeastOnce();
    expect(context.getDirection()).andReturn(Direction.UPGRADE).atLeastOnce();
    expect(context.getRepositoryVersion()).andReturn(repoVersion220).anyTimes();
    expect(context.getSupportedServices()).andReturn(Sets.newHashSet("ZOOKEEPER")).atLeastOnce();
    expect(context.getSourceRepositoryVersion(EasyMock.anyString())).andReturn(repoVersion211).atLeastOnce();
    expect(context.getTargetRepositoryVersion(EasyMock.anyString())).andReturn(repoVersion220).atLeastOnce();
    expect(context.getOrchestrationType()).andReturn(RepositoryType.STANDARD).anyTimes();
    expect(context.getHostRoleCommandFactory()).andStubReturn(m_injector.getInstance(HostRoleCommandFactory.class));
    expect(context.getRoleGraphFactory()).andStubReturn(m_injector.getInstance(RoleGraphFactory.class));

    replayAll();

    UpgradeHelper upgradeHelper = m_injector.getInstance(UpgradeHelper.class);
    upgradeHelper.updateDesiredRepositoriesAndConfigs(context);

    Map<String, Map<String, String>> expectedConfigurations = expectedConfigurationsCapture.getValue();
    Map<String, String> expectedFooType = expectedConfigurations.get("foo-site");
    Map<String, String> expectedBarType = expectedConfigurations.get("bar-site");

    // As the upgrade pack did not have any Flume updates, its configs should
    // not be updated.
    assertEquals(2, expectedConfigurations.size());

    assertEquals("my-foo-property-1", expectedFooType.get("foo-property-1"));
    assertEquals(null, expectedFooType.get("foo-property-2"));
    assertEquals("stack-220-original", expectedBarType.get("bar-property-1"));
    assertEquals("stack-220-original", expectedBarType.get("bar-property-2"));
  }

  /**
   * Tests that any read-only properties are not taken from the existing
   * configs, but from the new stack value.
   *
   * @throws Exception
   */
  @Test
  public void testReadOnlyPropertyIsTakenFromTargetStack() throws Exception {
    RepositoryVersionEntity repoVersion211 = createNiceMock(RepositoryVersionEntity.class);
    RepositoryVersionEntity repoVersion220 = createNiceMock(RepositoryVersionEntity.class);

    StackId stack211 = new StackId("HDP-2.1.1");
    StackId stack220 = new StackId("HDP-2.2.0");

    String version211 = "2.1.1.0-1234";
    String version220 = "2.2.0.0-1234";

    expect(repoVersion211.getStackId()).andReturn(stack211).atLeastOnce();
    expect(repoVersion211.getVersion()).andReturn(version211).atLeastOnce();

    expect(repoVersion220.getStackId()).andReturn(stack220).atLeastOnce();
    expect(repoVersion220.getVersion()).andReturn(version220).atLeastOnce();

    String fooSite = "foo-site";
    String fooPropertyName = "foo-property-1";
    String serviceName = "ZOOKEEPER";

    Map<String, Map<String, String>> stack211Configs = new HashMap<>();
    Map<String, String> stack211FooType = new HashMap<>();
    stack211Configs.put(fooSite, stack211FooType);
    stack211FooType.put(fooPropertyName, "stack-211-original");

    Map<String, Map<String, String>> stack220Configs = new HashMap<>();
    Map<String, String> stack220FooType = new HashMap<>();
    stack220Configs.put(fooSite, stack220FooType);
    stack220FooType.put(fooPropertyName, "stack-220-original");

    PropertyInfo readOnlyProperty = new PropertyInfo();
    ValueAttributesInfo valueAttributesInfo = new ValueAttributesInfo();
    valueAttributesInfo.setReadOnly(true);
    readOnlyProperty.setName(fooPropertyName);
    readOnlyProperty.setFilename(fooSite + ".xml");
    readOnlyProperty.setPropertyValueAttributes(null);
    readOnlyProperty.setPropertyValueAttributes(valueAttributesInfo);

    expect(m_metainfo.getServiceProperties(stack211.getStackName(), stack211.getStackVersion(),
        serviceName)).andReturn(Sets.newHashSet(readOnlyProperty)).atLeastOnce();

    Map<String, String> existingFooType = new HashMap<>();

    ClusterConfigEntity fooConfigEntity = createNiceMock(ClusterConfigEntity.class);

    expect(fooConfigEntity.getType()).andReturn(fooSite);

    Config fooConfig = createNiceMock(Config.class);

    existingFooType.put(fooPropertyName, "my-foo-property-1");

    expect(fooConfig.getType()).andReturn(fooSite).atLeastOnce();
    expect(fooConfig.getProperties()).andReturn(existingFooType);

    Map<String, DesiredConfig> desiredConfigurations = new HashMap<>();
    desiredConfigurations.put(fooSite, null);

    Service zookeeper = createNiceMock(Service.class);
    expect(zookeeper.getName()).andReturn(serviceName).atLeastOnce();
    expect(zookeeper.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>()).once();
    zookeeper.setDesiredRepositoryVersion(repoVersion220);
    expectLastCall().once();

    Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getCurrentStackVersion()).andReturn(stack211).atLeastOnce();
    expect(cluster.getDesiredStackVersion()).andReturn(stack220);
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigurations);
    expect(cluster.getDesiredConfigByType(fooSite)).andReturn(fooConfig);
    expect(cluster.getService(serviceName)).andReturn(zookeeper);

    ConfigHelper configHelper = m_injector.getInstance(ConfigHelper.class);

    expect(configHelper.getDefaultProperties(stack211, serviceName)).andReturn(stack211Configs).anyTimes();
    expect(configHelper.getDefaultProperties(stack220, serviceName)).andReturn(stack220Configs).anyTimes();

    Capture<Map<String, Map<String, String>>> expectedConfigurationsCapture = EasyMock.newCapture();

    expect(configHelper.createConfigTypes(EasyMock.anyObject(Cluster.class),
        EasyMock.anyObject(StackId.class), EasyMock.anyObject(AmbariManagementController.class),
        EasyMock.capture(expectedConfigurationsCapture), EasyMock.anyObject(String.class),
        EasyMock.anyObject(String.class))).andReturn(true);

    // mock the service config DAO and replay it
    ServiceConfigEntity zookeeperServiceConfig = createNiceMock(ServiceConfigEntity.class);
    expect(zookeeperServiceConfig.getClusterConfigEntities()).andReturn(
        Lists.newArrayList(fooConfigEntity));

    ServiceConfigDAO serviceConfigDAOMock = m_injector.getInstance(ServiceConfigDAO.class);
    List<ServiceConfigEntity> latestServiceConfigs = Lists.newArrayList(zookeeperServiceConfig);
    expect(serviceConfigDAOMock.getLastServiceConfigsForService(EasyMock.anyLong(),
        eq(serviceName))).andReturn(latestServiceConfigs).once();

    UpgradeContext context = createNiceMock(UpgradeContext.class);
    expect(context.getCluster()).andReturn(cluster).atLeastOnce();
    expect(context.getType()).andReturn(UpgradeType.ROLLING).atLeastOnce();
    expect(context.getDirection()).andReturn(Direction.UPGRADE).atLeastOnce();
    expect(context.getRepositoryVersion()).andReturn(repoVersion220).anyTimes();
    expect(context.getSupportedServices()).andReturn(Sets.newHashSet(serviceName)).atLeastOnce();
    expect(context.getSourceRepositoryVersion(EasyMock.anyString())).andReturn(repoVersion211).atLeastOnce();
    expect(context.getTargetRepositoryVersion(EasyMock.anyString())).andReturn(repoVersion220).atLeastOnce();
    expect(context.getOrchestrationType()).andReturn(RepositoryType.STANDARD).anyTimes();
    expect(context.getHostRoleCommandFactory()).andStubReturn(m_injector.getInstance(HostRoleCommandFactory.class));
    expect(context.getRoleGraphFactory()).andStubReturn(m_injector.getInstance(RoleGraphFactory.class));

    replayAll();

    UpgradeHelper upgradeHelper = m_injector.getInstance(UpgradeHelper.class);
    upgradeHelper.updateDesiredRepositoriesAndConfigs(context);

    Map<String, Map<String, String>> expectedConfigurations = expectedConfigurationsCapture.getValue();
    Map<String, String> expectedFooType = expectedConfigurations.get(fooSite);

    // As the upgrade pack did not have any Flume updates, its configs should
    // not be updated.
    assertEquals(1, expectedConfigurations.size());
    assertEquals(1, expectedFooType.size());

    assertEquals("stack-220-original", expectedFooType.get(fooPropertyName));
  }

  private class MockModule implements Module {

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Binder binder) {
      StageDAO stageDAO = createNiceMock(StageDAO.class);
      PartialNiceMockBinder.newBuilder(StackUpgradeConfigurationMergeTest.this)
          .addActionDBAccessorConfigsBindings().addPasswordEncryptorBindings().build().configure(binder);

      binder.bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
      binder.bind(StageDAO.class).toInstance(stageDAO);
      binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
      binder.bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
      binder.bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
      binder.bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
      binder.bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementController.class));
      binder.bind(ClusterController.class).toInstance(createNiceMock(ClusterController.class));
      binder.bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
      binder.bind(SessionHandler.class).toInstance(createNiceMock(SessionHandler.class));
      binder.bind(RequestExecutionFactory.class).toInstance(createNiceMock(RequestExecutionFactory.class));
      binder.bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
      binder.bind(RequestFactory.class).toInstance(createNiceMock(RequestFactory.class));
      binder.bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
      binder.install(new FactoryModuleBuilder().build(RoleGraphFactory.class));
      binder.bind(AbstractRootServiceResponseFactory.class).toInstance(createNiceMock(AbstractRootServiceResponseFactory.class));
      binder.bind(ConfigFactory.class).toInstance(createNiceMock(ConfigFactory.class));
      binder.bind(ConfigGroupFactory.class).toInstance(createNiceMock(ConfigGroupFactory.class));
      binder.bind(ServiceFactory.class).toInstance(createNiceMock(ServiceFactory.class));
      binder.bind(ServiceComponentFactory.class).toInstance(createNiceMock(ServiceComponentFactory.class));
      binder.bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
      binder.bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
      binder.bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelper.class));
      binder.bind(Users.class).toInstance(createNiceMock(Users.class));
      binder.bind(ConfigHelper.class).toInstance(createNiceMock(ConfigHelper.class));
      binder.bind(RepositoryVersionDAO.class).toInstance(createNiceMock(RepositoryVersionDAO.class));
      binder.bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
      binder.bind(HookService.class).toInstance(createMock(HookService.class));
      binder.bind(ServiceConfigDAO.class).toInstance(createNiceMock(ServiceConfigDAO.class));
      binder.install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
      binder.bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
      binder.bind(AmbariMetaInfo.class).toInstance(m_metainfo);
      binder.bind(PersistedState.class).toInstance(createNiceMock(PersistedState.class));
      binder.bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
      binder.bind(CredentialStoreService.class).toInstance(createNiceMock(CredentialStoreService.class));
      binder.bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
      binder.bind(AuditLogger.class).toInstance(createNiceMock(AuditLoggerDefaultImpl.class));
      binder.bind(MetadataHolder.class).toInstance(createNiceMock(MetadataHolder.class));
      binder.bind(AgentConfigsHolder.class).toInstance(createNiceMock(AgentConfigsHolder.class));
      binder.bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
      binder.bind(AmbariLdapConfigurationProvider.class).toInstance(createMock(AmbariLdapConfigurationProvider.class));

      binder.requestStaticInjection(UpgradeResourceProvider.class);

      binder.install(new FactoryModuleBuilder().build(ConfigureClusterTaskFactory.class));

      expect(stageDAO.getLastRequestId()).andReturn(1L).anyTimes();
      replay(stageDAO);
    }
  }
}
