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
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;

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
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
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
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.UnitOfWork;

/**
 * {@link UpgradeCatalog251} unit tests.
 */
@RunWith(EasyMockRunner.class)
public class UpgradeCatalog251Test {

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
    dbAccessor.addColumn(eq(UpgradeCatalog251.HOST_ROLE_COMMAND_TABLE), capture(hrcBackgroundColumnCapture));

    expect(dbAccessor.getConnection()).andReturn(connection).anyTimes();
    expect(connection.createStatement()).andReturn(statement).anyTimes();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).anyTimes();
    expect(configuration.getDatabaseType()).andReturn(Configuration.DatabaseType.POSTGRES).anyTimes();

    replay(dbAccessor, configuration, connection, statement, resultSet);

    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(osFamily);
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(Configuration.class).toInstance(configuration);
      }
    };

    Injector injector = getInjector(createNiceMock(Clusters.class), createNiceMock(AmbariManagementControllerImpl.class));//Guice.createInjector(module);
    UpgradeCatalog251 upgradeCatalog251 = injector.getInstance(UpgradeCatalog251.class);
    upgradeCatalog251.executeDDLUpdates();

    verify(dbAccessor);

    DBColumnInfo captured = hrcBackgroundColumnCapture.getValue();
    Assert.assertEquals(UpgradeCatalog251.HRC_IS_BACKGROUND_COLUMN, captured.getName());
    Assert.assertEquals(Integer.valueOf(0), captured.getDefaultValue());
    Assert.assertEquals(Short.class, captured.getType());
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateKAFKAConfigs = UpgradeCatalog251.class.getDeclaredMethod("updateKAFKAConfigs");
    Method updateSTORMConfigs = UpgradeCatalog251.class.getDeclaredMethod("updateSTORMConfigs");

    UpgradeCatalog251 upgradeCatalog251 = createMockBuilder(UpgradeCatalog251.class)
            .addMockedMethod(addNewConfigurationsFromXml)
            .addMockedMethod(updateKAFKAConfigs)
            .addMockedMethod(updateSTORMConfigs)
            .createMock();

    upgradeCatalog251.addNewConfigurationsFromXml();
    expectLastCall().once();

    Field field = AbstractUpgradeCatalog.class.getDeclaredField("dbAccessor");
    field.set(upgradeCatalog251, dbAccessor);

    upgradeCatalog251.updateKAFKAConfigs();
    expectLastCall().once();

    upgradeCatalog251.updateSTORMConfigs();
    expectLastCall().once();

    replay(upgradeCatalog251, dbAccessor);

    upgradeCatalog251.executeDMLUpdates();

    verify(upgradeCatalog251, dbAccessor);
  }


  @Test
  public void testUpdateKAFKAConfigs() throws Exception{
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    Map<String, String> initialProperties = Collections.singletonMap("listeners", "PLAINTEXT://localhost:6667,SSL://localhost:6666");
    Map<String, String> expectedUpdates = Collections.singletonMap("listeners", "PLAINTEXTSASL://localhost:6667,SSL://localhost:6666");

    final Config kafkaBroker = easyMockSupport.createNiceMock(Config.class);
    expect(kafkaBroker.getProperties()).andReturn(initialProperties).times(1);
    // Re-entrant test
    expect(kafkaBroker.getProperties()).andReturn(expectedUpdates).times(1);

    final Injector mockInjector = getInjector(mockClusters, mockAmbariManagementController);

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).atLeastOnce();
    expect(mockClusters.getClusters()).andReturn(Collections.singletonMap("normal", mockClusterExpected)).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("kafka-broker")).andReturn(kafkaBroker).atLeastOnce();
    expect(mockClusterExpected.getSecurityType()).andReturn(SecurityType.KERBEROS).atLeastOnce();
    expect(mockClusterExpected.getServices()).andReturn(Collections.singletonMap("KAFKA", null)).atLeastOnce();

    UpgradeCatalog251 upgradeCatalog251 = createMockBuilder(UpgradeCatalog251.class)
        .withConstructor(Injector.class)
        .withArgs(mockInjector)
        .addMockedMethod("updateConfigurationProperties", String.class,
            Map.class, boolean.class, boolean.class)
        .createMock();


    // upgradeCatalog251.updateConfigurationProperties is only expected to execute once since no changes are
    // expected when the relevant data have been previously changed
    upgradeCatalog251.updateConfigurationProperties("kafka-broker", expectedUpdates, true, false);
    expectLastCall().once();

    easyMockSupport.replayAll();
    replay(upgradeCatalog251);

    // Execute the first time... upgrading to Ambari 2.4.0
    upgradeCatalog251.updateKAFKAConfigs();

    // Test reentry... upgrading from Ambari 2.4.0
    upgradeCatalog251.updateKAFKAConfigs();

    easyMockSupport.verifyAll();
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
        binder.bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
      }
    };
    return Guice.createInjector(module);
  }
}
