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

package org.apache.ambari.server.upgrade;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.actionmanager.StageFactoryImpl;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.AuditLoggerDefaultImpl;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.KerberosHelperImpl;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.UnitOfWork;

/**
 * {@link org.apache.ambari.server.upgrade.UpgradeCatalog252} unit tests.
 */
@RunWith(EasyMockRunner.class)
public class UpgradeCatalog252Test {

  //  private Injector injector;
  @Mock(type = MockType.STRICT)
  private Provider<EntityManager> entityManagerProvider;

  @Mock(type = MockType.NICE)
  private EntityManager entityManager;

  @Mock(type = MockType.NICE)
  private DBAccessor dbAccessor;

  @Mock(type = MockType.NICE)
  private Configuration configuration;

  @Mock(type = MockType.NICE)
  private Connection connection;

  @Mock(type = MockType.NICE)
  private Statement statement;

  @Mock(type = MockType.NICE)
  private ResultSet resultSet;

  @Mock(type = MockType.NICE)
  private OsFamily osFamily;

  @Mock(type = MockType.NICE)
  private KerberosHelper kerberosHelper;

  @Mock(type = MockType.NICE)
  private ActionManager actionManager;

  @Mock(type = MockType.NICE)
  private Config config;

  @Mock(type = MockType.STRICT)
  private Service service;

  @Mock(type = MockType.NICE)
  private Clusters clusters;

  @Mock(type = MockType.NICE)
  private Cluster cluster;

  @Mock(type = MockType.NICE)
  private MetadataHolder metadataHolder;

  @Mock(type = MockType.NICE)
  private Injector injector;

  @Before
  public void init() {
    reset(entityManagerProvider, injector);

    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();

    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(kerberosHelper).anyTimes();

    replay(entityManagerProvider, injector);
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    Capture<DBColumnInfo> hrcBackgroundColumnCapture = newCapture();
    dbAccessor.addColumn(eq(UpgradeCatalog252.CLUSTERCONFIG_TABLE), capture(hrcBackgroundColumnCapture));

    expect(dbAccessor.getConnection()).andReturn(connection).anyTimes();
    expect(connection.createStatement()).andReturn(statement).anyTimes();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).anyTimes();
    expect(configuration.getDatabaseType()).andReturn(Configuration.DatabaseType.POSTGRES).anyTimes();

    replay(dbAccessor, configuration, connection, statement, resultSet);

    Injector injector = getInjector(createMock(Clusters.class), createNiceMock(AmbariManagementControllerImpl.class));
    UpgradeCatalog252 upgradeCatalog252 = injector.getInstance(UpgradeCatalog252.class);
    upgradeCatalog252.executeDDLUpdates();

    verify(dbAccessor);

    DBColumnInfo captured = hrcBackgroundColumnCapture.getValue();
    Assert.assertEquals(UpgradeCatalog252.SERVICE_DELETED_COLUMN, captured.getName());
    Assert.assertEquals(0, captured.getDefaultValue());
    Assert.assertEquals(Short.class, captured.getType());
  }

  @Test
  public void testFixLivySuperUsers() throws AmbariException {

    final Clusters clusters = createMock(Clusters.class);
    final Cluster cluster = createMock(Cluster.class);
    final Config zeppelinEnv = createMock(Config.class);
    final Config livyConf = createMock(Config.class);
    final Config livyConfNew = createMock(Config.class);
    final Config livy2Conf = createMock(Config.class);
    final Config livy2ConfNew = createMock(Config.class);
    final AmbariManagementController controller = createMock(AmbariManagementController.class);

    StackId stackId = new StackId("HDP", "2.2");


    Capture<? extends Map<String, String>> captureLivyConfProperties = newCapture();
    Capture<? extends Map<String, String>> captureLivy2ConfProperties = newCapture();

    expect(clusters.getClusters()).andReturn(Collections.singletonMap("c1", cluster)).once();

    expect(cluster.getClusterName()).andReturn("c1").atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).atLeastOnce();
    expect(cluster.getDesiredConfigByType("zeppelin-env")).andReturn(zeppelinEnv).atLeastOnce();
    expect(cluster.getServiceByConfigType("livy-conf")).andReturn("SPARK").atLeastOnce();
    expect(cluster.getDesiredConfigByType("livy-conf")).andReturn(livyConf).atLeastOnce();
    expect(cluster.getConfigsByType("livy-conf")).andReturn(Collections.singletonMap("tag1", livyConf)).atLeastOnce();
    expect(cluster.getConfig(eq("livy-conf"), anyString())).andReturn(livyConfNew).atLeastOnce();
    expect(cluster.getServiceByConfigType("livy2-conf")).andReturn("SPARK2").atLeastOnce();
    expect(cluster.getDesiredConfigByType("livy2-conf")).andReturn(livy2Conf).atLeastOnce();
    expect(cluster.getConfigsByType("livy2-conf")).andReturn(Collections.singletonMap("tag1", livy2Conf)).atLeastOnce();
    expect(cluster.getConfig(eq("livy2-conf"), anyString())).andReturn(livy2ConfNew).atLeastOnce();
    expect(cluster.addDesiredConfig(eq("ambari-upgrade"), anyObject(Set.class), anyString())).andReturn(null).atLeastOnce();

    expect(zeppelinEnv.getProperties()).andReturn(Collections.singletonMap("zeppelin.server.kerberos.principal", "zeppelin_user@AMBARI.LOCAL")).once();

    expect(livyConf.getProperties()).andReturn(Collections.singletonMap("livy.superusers", "zeppelin-c1, some_user")).atLeastOnce();
    expect(livyConf.getPropertiesAttributes()).andReturn(Collections.<String, Map<String, String>>emptyMap()).atLeastOnce();
    expect(livy2Conf.getProperties()).andReturn(Collections.<String, String>emptyMap()).atLeastOnce();
    expect(livy2Conf.getPropertiesAttributes()).andReturn(Collections.<String, Map<String, String>>emptyMap()).atLeastOnce();

    expect(controller.createConfig(eq(cluster), eq(stackId), eq("livy-conf"), capture(captureLivyConfProperties), anyString(), anyObject(Map.class)))
        .andReturn(livyConfNew)
        .once();
    expect(controller.createConfig(eq(cluster), eq(stackId), eq("livy2-conf"), capture(captureLivy2ConfProperties), anyString(), anyObject(Map.class)))
        .andReturn(livy2ConfNew)
        .once();

    replay(clusters, cluster, zeppelinEnv, livy2Conf, livyConf, controller, metadataHolder);

    Injector injector = getInjector(clusters, controller);

    final ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    configHelper.updateAgentConfigs(anyObject(Set.class));
    expectLastCall().times(2);

    replay(configHelper);

    UpgradeCatalog252 upgradeCatalog252 = injector.getInstance(UpgradeCatalog252.class);
    upgradeCatalog252.fixLivySuperusers();

    verify(clusters, cluster, zeppelinEnv, livy2Conf, livyConf, controller, configHelper);

    Assert.assertTrue(captureLivyConfProperties.hasCaptured());
    Assert.assertEquals("some_user,zeppelin_user", captureLivyConfProperties.getValue().get("livy.superusers"));

    Assert.assertTrue(captureLivy2ConfProperties.hasCaptured());
    Assert.assertEquals("zeppelin_user", captureLivy2ConfProperties.getValue().get("livy.superusers"));
  }

  @Test
  public void testUpdateKerberosDescriptorArtifact() throws AmbariException {
    String initialJson = "{" +
        "  \"services\": [" +
        "    {" +
        "      \"name\": \"SPARK\"," +
        "      \"configurations\": [" +
        "        {" +
        "          \"livy-conf\": {" +
        "            \"property1\": \"true\"," +
        "            \"property2\": \"true\"," +
        "            \"livy.superusers\": \"somevalue\"" +
        "          }" +
        "        }," +
        "        {" +
        "          \"some-env\": {" +
        "            \"groups\": \"${hadoop-env/proxyuser_group}\"," +
        "            \"hosts\": \"${clusterHostInfo/existing_service_master_hosts}\"" +
        "          }" +
        "        }" +
        "      ]" +
        "    }," +
        "    {" +
        "      \"name\": \"SPARK2\"," +
        "      \"configurations\": [" +
        "        {" +
        "          \"livy2-conf\": {" +
        "            \"property1\": \"true\"," +
        "            \"property2\": \"true\"," +
        "            \"livy.superusers\": \"somevalue\"" +
        "          }" +
        "        }," +
        "        {" +
        "          \"some2-env\": {" +
        "            \"groups\": \"${hadoop-env/proxyuser_group}\"," +
        "            \"hosts\": \"${clusterHostInfo/existing_service_master_hosts}\"" +
        "          }" +
        "        }" +
        "      ]" +
        "    }," +
        "    {" +
        "      \"name\": \"KNOX\"," +
        "      \"components\": [" +
        "        {" +
        "          \"name\": \"KNOX_GATEWAY\"," +
        "          \"configurations\": [" +
        "            {" +
        "              \"core-site\": {" +
        "                \"property1\": \"true\"," +
        "                \"property2\": \"true\"," +
        "                \"hadoop.proxyuser.knox.groups\": \"somevalue\"," +
        "                \"hadoop.proxyuser.knox.hosts\": \"somevalue\"" +
        "              }" +
        "            }," +
        "            {" +
        "              \"webhcat-site\": {" +
        "                \"webhcat.proxyuser.knox.groups\": \"somevalue\"," +
        "                \"webhcat.proxyuser.knox.hosts\": \"somevalue\"" +
        "              }" +
        "            }," +
        "            {" +
        "              \"oozie-site\": {" +
        "                \"oozie.service.ProxyUserService.proxyuser.knox.groups\": \"somevalue\"," +
        "                \"oozie.service.ProxyUserService.proxyuser.knox.hosts\": \"somevalue\"" +
        "              }" +
        "            }," +
        "            {" +
        "              \"falcon-runtime.properties\": {" +
        "                \"*.falcon.service.ProxyUserService.proxyuser.knox.groups\": \"somevalue\"," +
        "                \"*.falcon.service.ProxyUserService.proxyuser.knox.hosts\": \"somevalue\"" +
        "              }" +
        "            }," +
        "            {" +
        "              \"some-env\": {" +
        "                \"groups\": \"${hadoop-env/proxyuser_group}\"," +
        "                \"hosts\": \"${clusterHostInfo/existing_service_master_hosts}\"" +
        "              }" +
        "            }" +
        "          ]" +
        "        }" +
        "      ]" +
        "    }," +
        "    {" +
        "      \"name\": \"NOT_SPARK\"," +
        "      \"configurations\": [" +
        "        {" +
        "          \"not-livy-conf\": {" +
        "            \"property1\": \"true\"," +
        "            \"property2\": \"true\"," +
        "            \"livy.superusers\": \"somevalue\"" +
        "          }" +
        "        }," +
        "        {" +
        "          \"some2-env\": {" +
        "            \"groups\": \"${hadoop-env/proxyuser_group}\"," +
        "            \"hosts\": \"${clusterHostInfo/existing_service_master_hosts}\"" +
        "          }" +
        "        }" +
        "      ]" +
        "    }" +
        "  ]" +
        "}";


    Type type = new TypeToken<Map<String, Object>>() {
    }.getType();
    Map<String,Object> map = new Gson().fromJson(initialJson, type);

    Capture<? extends Map<String, Object>> captureMap = newCapture();
    ArtifactEntity artifactEntity = createMock(ArtifactEntity.class);

    expect(artifactEntity.getArtifactData()).andReturn(map).once();
    artifactEntity.setArtifactData(capture(captureMap));
    expectLastCall().once();

    ArtifactDAO artifactDAO = createMock(ArtifactDAO.class);
    expect(artifactDAO.merge(artifactEntity)).andReturn(artifactEntity).once();

    replay(artifactDAO, artifactEntity);

    Injector injector = getInjector(createMock(Clusters.class), createNiceMock(AmbariManagementControllerImpl.class));
    UpgradeCatalog252 upgradeCatalog252 = injector.getInstance(UpgradeCatalog252.class);
    upgradeCatalog252.updateKerberosDescriptorArtifact(artifactDAO, artifactEntity);

    verify(artifactDAO, artifactEntity);

    Assert.assertTrue(captureMap.hasCaptured());

    KerberosDescriptor result = new KerberosDescriptorFactory().createInstance(captureMap.getValue());

    Assert.assertNotNull(result.getService("SPARK"));
    Assert.assertNotNull(result.getService("SPARK").getConfiguration("livy-conf"));
    Assert.assertNotNull(result.getService("SPARK").getConfiguration("livy-conf").getProperties());
    Assert.assertFalse(result.getService("SPARK").getConfiguration("livy-conf").getProperties().containsKey("livy.superusers"));

    Assert.assertNotNull(result.getService("SPARK2"));
    Assert.assertNotNull(result.getService("SPARK2").getConfiguration("livy2-conf"));
    Assert.assertNotNull(result.getService("SPARK2").getConfiguration("livy2-conf").getProperties());
    Assert.assertFalse(result.getService("SPARK2").getConfiguration("livy2-conf").getProperties().containsKey("livy.superusers"));

    Assert.assertNotNull(result.getService("NOT_SPARK"));
    Assert.assertNotNull(result.getService("NOT_SPARK").getConfiguration("not-livy-conf"));
    Assert.assertNotNull(result.getService("NOT_SPARK").getConfiguration("not-livy-conf").getProperties());
    Assert.assertTrue(result.getService("NOT_SPARK").getConfiguration("not-livy-conf").getProperties().containsKey("livy.superusers"));

    Assert.assertNotNull(result.getService("KNOX"));

    KerberosComponentDescriptor knoxGateway = result.getService("KNOX").getComponent("KNOX_GATEWAY");
    Assert.assertNotNull(knoxGateway);
    Assert.assertNotNull(knoxGateway.getConfiguration("core-site"));
    Assert.assertNotNull(knoxGateway.getConfiguration("core-site").getProperties());
    Assert.assertTrue(knoxGateway.getConfiguration("core-site").getProperties().containsKey("property1"));
    Assert.assertFalse(knoxGateway.getConfiguration("core-site").getProperties().containsKey("hadoop.proxyuser.knox.groups"));
    Assert.assertFalse(knoxGateway.getConfiguration("core-site").getProperties().containsKey("hadoop.proxyuser.knox.hosts"));
    Assert.assertNull(knoxGateway.getConfiguration("oozie-site"));
    Assert.assertNull(knoxGateway.getConfiguration("webhcat-site"));
    Assert.assertNull(knoxGateway.getConfiguration("falcon-runtime.properties"));
    Assert.assertNotNull(knoxGateway.getConfiguration("some-env"));
  }

  private Injector getInjector(Clusters clusters, AmbariManagementController ambariManagementController) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        PartialNiceMockBinder.newBuilder().addConfigsBindings().addFactoriesInstallBinding()
        .addPasswordEncryptorBindings().addLdapBindings().build().configure(binder);

        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(osFamily);
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(Clusters.class).toInstance(clusters);
        binder.bind(AmbariManagementController.class).toInstance(ambariManagementController);
        binder.bind(ActionDBAccessor.class).toInstance(createNiceMock(ActionDBAccessorImpl.class));
        binder.bind(PersistedState.class).toInstance(createMock(PersistedStateImpl.class));
        binder.bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        binder.bind(AuditLogger.class).toInstance(createNiceMock(AuditLoggerDefaultImpl.class));
        binder.bind(StageFactory.class).to(StageFactoryImpl.class);
        binder.bind(UnitOfWork.class).toInstance(createNiceMock(UnitOfWork.class));
        binder.bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        binder.bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
        binder.bind(HookService.class).to(UserHookService.class);
        binder.bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        binder.bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
        binder.bind(CredentialStoreService.class).toInstance(createNiceMock(CredentialStoreService.class));
        binder.bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        binder.bind(AmbariMetaInfo.class).toInstance(createNiceMock(AmbariMetaInfo.class));
        binder.bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelperImpl.class));
        binder.bind(MetadataHolder.class).toInstance(metadataHolder);
        binder.bind(AgentConfigsHolder.class).toInstance(createNiceMock(AgentConfigsHolder.class));
        binder.bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        binder.bind(ConfigHelper.class).toInstance(createStrictMock(ConfigHelper.class));
        binder.bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
      }
    };
    return Guice.createInjector(module);
  }
}
