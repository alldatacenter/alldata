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
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.RequestFactory;
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
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
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
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceComponentImpl;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroupImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalType;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionImpl;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.persist.UnitOfWork;

/**
 * {@link UpgradeCatalog260} unit tests.
 */
@RunWith(EasyMockRunner.class)
public class UpgradeCatalog260Test {

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

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void init() {
    reset(entityManagerProvider);

    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();

    replay(entityManagerProvider);
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {

    List<Integer> current = new ArrayList<>();
    current.add(1);

    expect(dbAccessor.getConnection()).andReturn(connection).anyTimes();
    expect(connection.createStatement()).andReturn(statement).anyTimes();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).anyTimes();
    expect(configuration.getDatabaseType()).andReturn(Configuration.DatabaseType.POSTGRES).anyTimes();
    expect(dbAccessor.tableHasColumn(UpgradeCatalog260.CLUSTER_CONFIG_TABLE, UpgradeCatalog260.SERVICE_DELETED_COLUMN)).andReturn(true).anyTimes();


    Capture<String[]> scdcaptureKey = newCapture();
    Capture<String[]> scdcaptureValue = newCapture();
    expectGetCurrentVersionID(current, scdcaptureKey, scdcaptureValue);

    Capture<DBColumnInfo> scdstadd1 = newCapture();
    Capture<DBColumnInfo> scdstalter1 = newCapture();
    Capture<DBColumnInfo> scdstadd2 = newCapture();
    Capture<DBColumnInfo> scdstalter2 = newCapture();
    expectUpdateServiceComponentDesiredStateTable(scdstadd1, scdstalter1, scdstadd2, scdstalter2);

    Capture<DBColumnInfo> sdstadd = newCapture();
    Capture<DBColumnInfo> sdstalter = newCapture();
    expectUpdateServiceDesiredStateTable(sdstadd, sdstalter);

    Capture<DBColumnInfo> selectedColumnInfo = newCapture();
    Capture<DBColumnInfo> selectedmappingColumnInfo = newCapture();
    Capture<DBColumnInfo> selectedTimestampColumnInfo = newCapture();
    Capture<DBColumnInfo> createTimestampColumnInfo = newCapture();
    expectAddSelectedCollumsToClusterconfigTable(selectedColumnInfo, selectedmappingColumnInfo, selectedTimestampColumnInfo, createTimestampColumnInfo);

    expectUpdateHostComponentDesiredStateTable();
    expectUpdateHostComponentStateTable();

    Capture<DBColumnInfo> rvid = newCapture();
    Capture<DBColumnInfo> orchestration = newCapture();
    Capture<DBColumnInfo> revertAllowed = newCapture();
    expectUpdateUpgradeTable(rvid, orchestration, revertAllowed);

    Capture<List<DBAccessor.DBColumnInfo>> columns = newCapture();
    expectCreateUpgradeHistoryTable(columns);

    expectDropStaleTables();

    Capture<DBColumnInfo> repoVersionHiddenColumnCapture = newCapture();
    Capture<DBColumnInfo> repoVersionResolvedColumnCapture = newCapture();
    expectUpdateRepositoryVersionTableTable(repoVersionHiddenColumnCapture, repoVersionResolvedColumnCapture);

    Capture<DBColumnInfo> unapped = newCapture();
    expectRenameServiceDeletedColumn(unapped);

    expectAddViewUrlPKConstraint();
    expectRemoveStaleConstraints();

    replay(dbAccessor, configuration, connection, statement, resultSet);

    Injector injector = getInjector();
    UpgradeCatalog260 upgradeCatalog260 = injector.getInstance(UpgradeCatalog260.class);
    upgradeCatalog260.executeDDLUpdates();

    verify(dbAccessor);

    verifyGetCurrentVersionID(scdcaptureKey, scdcaptureValue);
    verifyUpdateServiceComponentDesiredStateTable(scdstadd1, scdstalter1, scdstadd2, scdstalter2);
    verifyUpdateServiceDesiredStateTable(sdstadd, sdstalter);
    verifyAddSelectedCollumsToClusterconfigTable(selectedColumnInfo, selectedmappingColumnInfo, selectedTimestampColumnInfo, createTimestampColumnInfo);
    verifyUpdateUpgradeTable(rvid, orchestration, revertAllowed);
    verifyCreateUpgradeHistoryTable(columns);
    verifyUpdateRepositoryVersionTableTable(repoVersionHiddenColumnCapture, repoVersionResolvedColumnCapture);
  }

  private void expectRemoveStaleConstraints() throws SQLException {
    dbAccessor.dropUniqueConstraint(eq(UpgradeCatalog260.USERS_TABLE), eq(UpgradeCatalog260.STALE_POSTGRESS_USERS_LDAP_USER_KEY));
  }

  private void expectAddViewUrlPKConstraint() throws SQLException {
    dbAccessor.dropPKConstraint(eq(UpgradeCatalog260.VIEWURL_TABLE), eq(UpgradeCatalog260.STALE_POSTGRESS_VIEWURL_PKEY));
    expectLastCall().once();
    dbAccessor.addPKConstraint(eq(UpgradeCatalog260.VIEWURL_TABLE), eq(UpgradeCatalog260.PK_VIEWURL), eq(UpgradeCatalog260.URL_ID_COLUMN));
    expectLastCall().once();
  }

  public void expectDropStaleTables() throws SQLException {
    dbAccessor.dropTable(eq(UpgradeCatalog260.CLUSTER_CONFIG_MAPPING_TABLE));
    expectLastCall().once();
    dbAccessor.dropTable(eq(UpgradeCatalog260.CLUSTER_VERSION_TABLE));
    expectLastCall().once();
    dbAccessor.dropTable(eq(UpgradeCatalog260.SERVICE_COMPONENT_HISTORY_TABLE));
    expectLastCall().once();
  }

  public void expectRenameServiceDeletedColumn(Capture<DBColumnInfo> unmapped) throws SQLException {
    dbAccessor.renameColumn(eq(UpgradeCatalog260.CLUSTER_CONFIG_TABLE), eq(UpgradeCatalog260.SERVICE_DELETED_COLUMN), capture(unmapped));
    expectLastCall().once();
  }

  public void verifyCreateUpgradeHistoryTable(Capture<List<DBColumnInfo>> columns) {
    List<DBColumnInfo> columnsValue = columns.getValue();
    Assert.assertEquals(columnsValue.size(), 6);

    DBColumnInfo id = columnsValue.get(0);
    Assert.assertEquals(UpgradeCatalog260.ID_COLUMN, id.getName());
    Assert.assertEquals(Long.class, id.getType());
    Assert.assertEquals(null, id.getLength());
    Assert.assertEquals(null, id.getDefaultValue());
    Assert.assertEquals(false, id.isNullable());

    DBColumnInfo upgradeId = columnsValue.get(1);
    Assert.assertEquals(UpgradeCatalog260.UPGRADE_ID_COLUMN, upgradeId.getName());
    Assert.assertEquals(Long.class, upgradeId.getType());
    Assert.assertEquals(null, upgradeId.getLength());
    Assert.assertEquals(null, upgradeId.getDefaultValue());
    Assert.assertEquals(false, upgradeId.isNullable());

    DBColumnInfo serviceName = columnsValue.get(2);
    Assert.assertEquals(UpgradeCatalog260.SERVICE_NAME_COLUMN, serviceName.getName());
    Assert.assertEquals(String.class, serviceName.getType());
    Assert.assertEquals(Integer.valueOf(255), serviceName.getLength());
    Assert.assertEquals(null, serviceName.getDefaultValue());
    Assert.assertEquals(false, serviceName.isNullable());

    DBColumnInfo componentName = columnsValue.get(3);
    Assert.assertEquals(UpgradeCatalog260.COMPONENT_NAME_COLUMN, componentName.getName());
    Assert.assertEquals(String.class, componentName.getType());
    Assert.assertEquals(Integer.valueOf(255), componentName.getLength());
    Assert.assertEquals(null, componentName.getDefaultValue());
    Assert.assertEquals(false, componentName.isNullable());

    DBColumnInfo fromRepoID = columnsValue.get(4);
    Assert.assertEquals(UpgradeCatalog260.FROM_REPO_VERSION_ID_COLUMN, fromRepoID.getName());
    Assert.assertEquals(Long.class, fromRepoID.getType());
    Assert.assertEquals(null, fromRepoID.getLength());
    Assert.assertEquals(null, fromRepoID.getDefaultValue());
    Assert.assertEquals(false, fromRepoID.isNullable());

    DBColumnInfo targetRepoID = columnsValue.get(5);
    Assert.assertEquals(UpgradeCatalog260.TARGET_REPO_VERSION_ID_COLUMN, targetRepoID.getName());
    Assert.assertEquals(Long.class, targetRepoID.getType());
    Assert.assertEquals(null, targetRepoID.getLength());
    Assert.assertEquals(null, targetRepoID.getDefaultValue());
    Assert.assertEquals(false, targetRepoID.isNullable());
  }

  public void expectCreateUpgradeHistoryTable(Capture<List<DBColumnInfo>> columns) throws SQLException {
    dbAccessor.createTable(eq(UpgradeCatalog260.UPGRADE_HISTORY_TABLE), capture(columns));
    expectLastCall().once();

    dbAccessor.addPKConstraint(eq(UpgradeCatalog260.UPGRADE_HISTORY_TABLE), eq(UpgradeCatalog260.PK_UPGRADE_HIST), eq(UpgradeCatalog260.ID_COLUMN));
    expectLastCall().once();

    dbAccessor.addFKConstraint(eq(UpgradeCatalog260.UPGRADE_HISTORY_TABLE), eq(UpgradeCatalog260.FK_UPGRADE_HIST_UPGRADE_ID), eq(UpgradeCatalog260.UPGRADE_ID_COLUMN), eq(UpgradeCatalog260.UPGRADE_TABLE), eq(UpgradeCatalog260.UPGRADE_ID_COLUMN), eq(false));
    expectLastCall().once();
    dbAccessor.addFKConstraint(eq(UpgradeCatalog260.UPGRADE_HISTORY_TABLE), eq(UpgradeCatalog260.FK_UPGRADE_HIST_FROM_REPO), eq(UpgradeCatalog260.FROM_REPO_VERSION_ID_COLUMN), eq(UpgradeCatalog260.REPO_VERSION_TABLE), eq(UpgradeCatalog260.REPO_VERSION_ID_COLUMN), eq(false));
    expectLastCall().once();
    dbAccessor.addFKConstraint(eq(UpgradeCatalog260.UPGRADE_HISTORY_TABLE), eq(UpgradeCatalog260.FK_UPGRADE_HIST_TARGET_REPO), eq(UpgradeCatalog260.TARGET_REPO_VERSION_ID_COLUMN), eq(UpgradeCatalog260.REPO_VERSION_TABLE), eq(UpgradeCatalog260.REPO_VERSION_ID_COLUMN), eq(false));
    expectLastCall().once();
    dbAccessor.addUniqueConstraint(eq(UpgradeCatalog260.UPGRADE_HISTORY_TABLE), eq(UpgradeCatalog260.UQ_UPGRADE_HIST), eq(UpgradeCatalog260.UPGRADE_ID_COLUMN), eq(UpgradeCatalog260.COMPONENT_NAME_COLUMN), eq(UpgradeCatalog260.SERVICE_NAME_COLUMN));
    expectLastCall().once();
  }

  public void verifyUpdateUpgradeTable(Capture<DBColumnInfo> rvid,
                                       Capture<DBColumnInfo> orchestration, Capture<DBColumnInfo> revertAllowed) {
    DBColumnInfo rvidValue = rvid.getValue();
    Assert.assertEquals(UpgradeCatalog260.REPO_VERSION_ID_COLUMN, rvidValue.getName());
    Assert.assertEquals(Long.class, rvidValue.getType());
    Assert.assertEquals(null, rvidValue.getLength());
    Assert.assertEquals(null, rvidValue.getDefaultValue());
    Assert.assertEquals(false, rvidValue.isNullable());

    DBColumnInfo orchestrationValue = orchestration.getValue();
    Assert.assertEquals(UpgradeCatalog260.ORCHESTRATION_COLUMN, orchestrationValue.getName());
    Assert.assertEquals(String.class, orchestrationValue.getType());
    Assert.assertEquals(Integer.valueOf(255), orchestrationValue.getLength());
    Assert.assertEquals(UpgradeCatalog260.STANDARD, orchestrationValue.getDefaultValue());
    Assert.assertEquals(false, orchestrationValue.isNullable());

    DBColumnInfo revertAllowedValue = revertAllowed.getValue();
    Assert.assertEquals(UpgradeCatalog260.ALLOW_REVERT_COLUMN, revertAllowedValue.getName());
    Assert.assertEquals(Short.class, revertAllowedValue.getType());
    Assert.assertEquals(null, revertAllowedValue.getLength());
    Assert.assertEquals(0, revertAllowedValue.getDefaultValue());
    Assert.assertEquals(false, revertAllowedValue.isNullable());
  }

  public void expectUpdateUpgradeTable(Capture<DBColumnInfo> rvid,
                                       Capture<DBColumnInfo> orchestration, Capture<DBColumnInfo> revertAllowed)
      throws SQLException {

    dbAccessor.clearTableColumn(eq(UpgradeCatalog260.CLUSTERS_TABLE), eq(UpgradeCatalog260.UPGRADE_ID_COLUMN),
        eq(null));
    expectLastCall().once();

    dbAccessor.clearTable(eq(UpgradeCatalog260.UPGRADE_TABLE));
    expectLastCall().once();

    dbAccessor.dropFKConstraint(eq(UpgradeCatalog260.UPGRADE_TABLE), eq(UpgradeCatalog260.FK_UPGRADE_FROM_REPO_ID));
    expectLastCall().once();

    dbAccessor.dropFKConstraint(eq(UpgradeCatalog260.UPGRADE_TABLE), eq(UpgradeCatalog260.FK_UPGRADE_TO_REPO_ID));
    expectLastCall().once();

    dbAccessor.dropColumn(eq(UpgradeCatalog260.UPGRADE_TABLE), eq(UpgradeCatalog260.FROM_REPO_VERSION_ID_COLUMN));
    expectLastCall().once();

    dbAccessor.dropColumn(eq(UpgradeCatalog260.UPGRADE_TABLE), eq(UpgradeCatalog260.TO_REPO_VERSION_ID_COLUMN));
    expectLastCall().once();

    dbAccessor.addColumn(eq(UpgradeCatalog260.UPGRADE_TABLE), capture(rvid));
    expectLastCall().once();

    dbAccessor.addColumn(eq(UpgradeCatalog260.UPGRADE_TABLE), capture(orchestration));
    expectLastCall().once();

    dbAccessor.addColumn(eq(UpgradeCatalog260.UPGRADE_TABLE), capture(revertAllowed));
    expectLastCall().once();

    dbAccessor.addFKConstraint(eq(UpgradeCatalog260.UPGRADE_TABLE), eq(UpgradeCatalog260.FK_UPGRADE_REPO_VERSION_ID), eq(UpgradeCatalog260.REPO_VERSION_ID_COLUMN), eq(UpgradeCatalog260.REPO_VERSION_TABLE), eq(UpgradeCatalog260.REPO_VERSION_ID_COLUMN), eq(false));
    expectLastCall().once();
  }

  public void expectUpdateHostComponentStateTable() throws SQLException {
    dbAccessor.dropFKConstraint(eq(UpgradeCatalog260.HOST_COMPONENT_STATE_TABLE), eq(UpgradeCatalog260.FK_HCS_CURRENT_STACK_ID));
    expectLastCall().once();
    dbAccessor.dropColumn(eq(UpgradeCatalog260.HOST_COMPONENT_STATE_TABLE), eq(UpgradeCatalog260.CURRENT_STACK_ID_COLUMN));
    expectLastCall().once();
  }

  public void expectUpdateHostComponentDesiredStateTable() throws SQLException {
    dbAccessor.dropFKConstraint(eq(UpgradeCatalog260.HOST_COMPONENT_DESIRED_STATE_TABLE), eq(UpgradeCatalog260.FK_HCDS_DESIRED_STACK_ID));
    expectLastCall().once();
    dbAccessor.dropColumn(eq(UpgradeCatalog260.HOST_COMPONENT_DESIRED_STATE_TABLE), eq(UpgradeCatalog260.DESIRED_STACK_ID_COLUMN));
    expectLastCall().once();
  }

  public void verifyAddSelectedCollumsToClusterconfigTable(Capture<DBColumnInfo> selectedColumnInfo, Capture<DBColumnInfo> selectedmappingColumnInfo, Capture<DBColumnInfo> selectedTimestampColumnInfo, Capture<DBColumnInfo> createTimestampColumnInfo) {
    DBColumnInfo selectedColumnInfoValue = selectedColumnInfo.getValue();
    Assert.assertEquals(UpgradeCatalog260.SELECTED_COLUMN, selectedColumnInfoValue.getName());
    Assert.assertEquals(Short.class, selectedColumnInfoValue.getType());
    Assert.assertEquals(null, selectedColumnInfoValue.getLength());
    Assert.assertEquals(0, selectedColumnInfoValue.getDefaultValue());
    Assert.assertEquals(false, selectedColumnInfoValue.isNullable());

    DBColumnInfo selectedmappingColumnInfoValue = selectedmappingColumnInfo.getValue();
    Assert.assertEquals(UpgradeCatalog260.SELECTED_COLUMN, selectedmappingColumnInfoValue.getName());
    Assert.assertEquals(Integer.class, selectedmappingColumnInfoValue.getType());
    Assert.assertEquals(null, selectedmappingColumnInfoValue.getLength());
    Assert.assertEquals(0, selectedmappingColumnInfoValue.getDefaultValue());
    Assert.assertEquals(false, selectedmappingColumnInfoValue.isNullable());

    DBColumnInfo selectedTimestampColumnInfoValue = selectedTimestampColumnInfo.getValue();
    Assert.assertEquals(UpgradeCatalog260.SELECTED_TIMESTAMP_COLUMN, selectedTimestampColumnInfoValue.getName());
    Assert.assertEquals(Long.class, selectedTimestampColumnInfoValue.getType());
    Assert.assertEquals(null, selectedTimestampColumnInfoValue.getLength());
    Assert.assertEquals(0, selectedTimestampColumnInfoValue.getDefaultValue());
    Assert.assertEquals(false, selectedTimestampColumnInfoValue.isNullable());

    DBColumnInfo createTimestampColumnInfoValue = createTimestampColumnInfo.getValue();
    Assert.assertEquals(UpgradeCatalog260.CREATE_TIMESTAMP_COLUMN, createTimestampColumnInfoValue.getName());
    Assert.assertEquals(Long.class, createTimestampColumnInfoValue.getType());
    Assert.assertEquals(null, createTimestampColumnInfoValue.getLength());
    Assert.assertEquals(null, createTimestampColumnInfoValue.getDefaultValue());
    Assert.assertEquals(false, createTimestampColumnInfoValue.isNullable());
  }

  public void expectAddSelectedCollumsToClusterconfigTable(Capture<DBColumnInfo> selectedColumnInfo, Capture<DBColumnInfo> selectedmappingColumnInfo, Capture<DBColumnInfo> selectedTimestampColumnInfo, Capture<DBColumnInfo> createTimestampColumnInfo) throws SQLException {
    dbAccessor.copyColumnToAnotherTable(eq(UpgradeCatalog260.CLUSTER_CONFIG_MAPPING_TABLE), capture(selectedmappingColumnInfo),
        eq(UpgradeCatalog260.CLUSTER_ID_COLUMN), eq(UpgradeCatalog260.TYPE_NAME_COLUMN), eq(UpgradeCatalog260.VERSION_TAG_COLUMN), eq(UpgradeCatalog260.CLUSTER_CONFIG_TABLE), capture(selectedColumnInfo),
        eq(UpgradeCatalog260.CLUSTER_ID_COLUMN), eq(UpgradeCatalog260.TYPE_NAME_COLUMN), eq(UpgradeCatalog260.VERSION_TAG_COLUMN), eq(UpgradeCatalog260.SELECTED_COLUMN), eq(UpgradeCatalog260.SELECTED), eq(0));
    expectLastCall().once();

    dbAccessor.copyColumnToAnotherTable(eq(UpgradeCatalog260.CLUSTER_CONFIG_MAPPING_TABLE), capture(createTimestampColumnInfo),
        eq(UpgradeCatalog260.CLUSTER_ID_COLUMN), eq(UpgradeCatalog260.TYPE_NAME_COLUMN), eq(UpgradeCatalog260.VERSION_TAG_COLUMN), eq(UpgradeCatalog260.CLUSTER_CONFIG_TABLE), capture(selectedTimestampColumnInfo),
        eq(UpgradeCatalog260.CLUSTER_ID_COLUMN), eq(UpgradeCatalog260.TYPE_NAME_COLUMN), eq(UpgradeCatalog260.VERSION_TAG_COLUMN), eq(UpgradeCatalog260.SELECTED_COLUMN), eq(UpgradeCatalog260.SELECTED), eq(0));
    expectLastCall().once();
  }

  public void verifyUpdateServiceDesiredStateTable(Capture<DBColumnInfo> sdstadd, Capture<DBColumnInfo> sdstalter) {
    DBColumnInfo sdstaddValue = sdstadd.getValue();
    Assert.assertEquals(UpgradeCatalog260.DESIRED_REPO_VERSION_ID_COLUMN, sdstaddValue.getName());
    Assert.assertEquals(1, sdstaddValue.getDefaultValue());
    Assert.assertEquals(Long.class, sdstaddValue.getType());
    Assert.assertEquals(false, sdstaddValue.isNullable());
    Assert.assertEquals(null, sdstaddValue.getLength());

    DBColumnInfo sdstalterValue = sdstalter.getValue();
    Assert.assertEquals(UpgradeCatalog260.DESIRED_REPO_VERSION_ID_COLUMN, sdstalterValue.getName());
    Assert.assertEquals(null, sdstalterValue.getDefaultValue());
    Assert.assertEquals(Long.class, sdstalterValue.getType());
    Assert.assertEquals(false, sdstalterValue.isNullable());
    Assert.assertEquals(null, sdstalterValue.getLength());
  }

  public void expectUpdateServiceDesiredStateTable(Capture<DBColumnInfo> sdstadd, Capture<DBColumnInfo> sdstalter) throws SQLException {
    dbAccessor.addColumn(eq(UpgradeCatalog260.SERVICE_DESIRED_STATE_TABLE), capture(sdstadd));
    expectLastCall().once();
    dbAccessor.alterColumn(eq(UpgradeCatalog260.SERVICE_DESIRED_STATE_TABLE), capture(sdstalter));
    expectLastCall().once();

    dbAccessor.addFKConstraint(eq(UpgradeCatalog260.SERVICE_DESIRED_STATE_TABLE), eq(UpgradeCatalog260.FK_REPO_VERSION_ID), eq(UpgradeCatalog260.DESIRED_REPO_VERSION_ID_COLUMN), eq(UpgradeCatalog260.REPO_VERSION_TABLE), eq(UpgradeCatalog260.REPO_VERSION_ID_COLUMN), eq(false));
    expectLastCall().once();
    dbAccessor.dropFKConstraint(eq(UpgradeCatalog260.SERVICE_DESIRED_STATE_TABLE), eq(UpgradeCatalog260.FK_SDS_DESIRED_STACK_ID));
    expectLastCall().once();
    dbAccessor.dropColumn(eq(UpgradeCatalog260.SERVICE_DESIRED_STATE_TABLE), eq(UpgradeCatalog260.DESIRED_STACK_ID_COLUMN));
    expectLastCall().once();
  }

  public void verifyUpdateServiceComponentDesiredStateTable(Capture<DBColumnInfo> scdstadd1, Capture<DBColumnInfo> scdstalter1, Capture<DBColumnInfo> scdstadd2, Capture<DBColumnInfo> scdstalter2) {
    DBColumnInfo scdstaddValue1 = scdstadd1.getValue();
    Assert.assertEquals(UpgradeCatalog260.DESIRED_REPO_VERSION_ID_COLUMN, scdstaddValue1.getName());
    Assert.assertEquals(1, scdstaddValue1.getDefaultValue());
    Assert.assertEquals(Long.class, scdstaddValue1.getType());
    Assert.assertEquals(false, scdstaddValue1.isNullable());

    DBColumnInfo scdstalterValue1 = scdstalter1.getValue();
    Assert.assertEquals(UpgradeCatalog260.DESIRED_REPO_VERSION_ID_COLUMN, scdstalterValue1.getName());
    Assert.assertEquals(null, scdstalterValue1.getDefaultValue());
    Assert.assertEquals(Long.class, scdstalterValue1.getType());
    Assert.assertEquals(false, scdstalterValue1.isNullable());

    DBColumnInfo scdstaddValue2 = scdstadd2.getValue();
    Assert.assertEquals(UpgradeCatalog260.REPO_STATE_COLUMN, scdstaddValue2.getName());
    Assert.assertEquals(UpgradeCatalog260.CURRENT, scdstaddValue2.getDefaultValue());
    Assert.assertEquals(String.class, scdstaddValue2.getType());
    Assert.assertEquals(false, scdstaddValue2.isNullable());
    Assert.assertEquals(Integer.valueOf(255), scdstaddValue2.getLength());

    DBColumnInfo scdstalterValue2 = scdstalter2.getValue();
    Assert.assertEquals(UpgradeCatalog260.REPO_STATE_COLUMN, scdstalterValue2.getName());
    Assert.assertEquals(UpgradeCatalog260.NOT_REQUIRED, scdstalterValue2.getDefaultValue());
    Assert.assertEquals(String.class, scdstalterValue2.getType());
    Assert.assertEquals(false, scdstalterValue2.isNullable());
    Assert.assertEquals(Integer.valueOf(255), scdstaddValue2.getLength());
  }

  public void verifyGetCurrentVersionID(Capture<String[]> scdcaptureKey, Capture<String[]> scdcaptureValue) {
    assertTrue(Arrays.equals(scdcaptureKey.getValue(), new String[]{UpgradeCatalog260.STATE_COLUMN}));
    assertTrue(Arrays.equals(scdcaptureValue.getValue(), new String[]{UpgradeCatalog260.CURRENT}));
  }

  public void expectUpdateServiceComponentDesiredStateTable(Capture<DBColumnInfo> scdstadd1, Capture<DBColumnInfo> scdstalter1, Capture<DBColumnInfo> scdstadd2, Capture<DBColumnInfo> scdstalter2) throws SQLException {
    dbAccessor.addColumn(eq(UpgradeCatalog260.SERVICE_COMPONENT_DESIRED_STATE_TABLE), capture(scdstadd1));
    expectLastCall().once();

    dbAccessor.alterColumn(eq(UpgradeCatalog260.SERVICE_COMPONENT_DESIRED_STATE_TABLE), capture(scdstalter1));
    expectLastCall().once();

    dbAccessor.addColumn(eq(UpgradeCatalog260.SERVICE_COMPONENT_DESIRED_STATE_TABLE), capture(scdstadd2));
    expectLastCall().once();

    dbAccessor.alterColumn(eq(UpgradeCatalog260.SERVICE_COMPONENT_DESIRED_STATE_TABLE), capture(scdstalter2));
    expectLastCall().once();

    dbAccessor.addFKConstraint(eq(UpgradeCatalog260.SERVICE_COMPONENT_DESIRED_STATE_TABLE), eq(UpgradeCatalog260.FK_SCDS_DESIRED_REPO_ID), eq(UpgradeCatalog260.DESIRED_REPO_VERSION_ID_COLUMN), eq(UpgradeCatalog260.REPO_VERSION_TABLE), eq(UpgradeCatalog260.REPO_VERSION_ID_COLUMN), eq(false));
    expectLastCall().once();

    dbAccessor.dropFKConstraint(eq(UpgradeCatalog260.SERVICE_COMPONENT_DESIRED_STATE_TABLE), eq(UpgradeCatalog260.FK_SCDS_DESIRED_STACK_ID));
    expectLastCall().once();
    dbAccessor.dropColumn(eq(UpgradeCatalog260.SERVICE_COMPONENT_DESIRED_STATE_TABLE), eq(UpgradeCatalog260.DESIRED_STACK_ID_COLUMN));
    expectLastCall().once();
    dbAccessor.dropColumn(eq(UpgradeCatalog260.SERVICE_COMPONENT_DESIRED_STATE_TABLE), eq(UpgradeCatalog260.DESIRED_VERSION_COLUMN));
    expectLastCall().once();
  }

  public void expectGetCurrentVersionID(List<Integer> current, Capture<String[]> scdcaptureKey, Capture<String[]> scdcaptureValue) throws SQLException {
    expect(dbAccessor.tableExists(eq("cluster_version"))).andReturn(true).once();
    expect(dbAccessor.getIntColumnValues(eq("cluster_version"), eq("repo_version_id"),
        capture(scdcaptureKey), capture(scdcaptureValue), eq(false))).andReturn(current).once();
  }

  @Test
  public void testRemoveDruidSuperset() throws Exception {

    List<Integer> current = new ArrayList<>();
    current.add(1);

    expect(dbAccessor.getConnection()).andReturn(connection).anyTimes();
    expect(connection.createStatement()).andReturn(statement).anyTimes();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).anyTimes();
    expect(configuration.getDatabaseType()).andReturn(Configuration.DatabaseType.POSTGRES).anyTimes();

    dbAccessor.executeQuery("DELETE FROM serviceconfigmapping WHERE config_id IN (SELECT config_id from clusterconfig where type_name like 'druid-superset%')");
    expectLastCall().once();
    dbAccessor.executeQuery("DELETE FROM clusterconfig WHERE type_name like 'druid-superset%'");
    expectLastCall().once();
    dbAccessor.executeQuery("DELETE FROM hostcomponentdesiredstate WHERE component_name = 'DRUID_SUPERSET'");
    expectLastCall().once();
    dbAccessor.executeQuery("DELETE FROM hostcomponentstate WHERE component_name = 'DRUID_SUPERSET'");
    expectLastCall().once();
    dbAccessor.executeQuery("DELETE FROM servicecomponentdesiredstate WHERE component_name = 'DRUID_SUPERSET'");
    expectLastCall().once();
    replay(dbAccessor, configuration, connection, statement, resultSet);

    Injector injector = getInjector();
    UpgradeCatalog260 upgradeCatalog260 = injector.getInstance(UpgradeCatalog260.class);
    upgradeCatalog260.executePreDMLUpdates();

    verify(dbAccessor);

  }

  /**
   * Sets expectations for DDL work on the
   * {@link UpgradeCatalog260#REPO_VERSION_TABLE}.
   *
   * @param hiddenColumnCapture
   * @throws SQLException
   */
  public void expectUpdateRepositoryVersionTableTable(Capture<DBColumnInfo> hiddenColumnCapture,
                                                      Capture<DBColumnInfo> repoVersionResolvedColumnCapture) throws SQLException {
    dbAccessor.addColumn(eq(UpgradeCatalog260.REPO_VERSION_TABLE), capture(hiddenColumnCapture));
    dbAccessor.addColumn(eq(UpgradeCatalog260.REPO_VERSION_TABLE), capture(repoVersionResolvedColumnCapture));
    expectLastCall().once();
  }

  public void verifyUpdateRepositoryVersionTableTable(Capture<DBColumnInfo> hiddenColumnCapture,
                                                      Capture<DBColumnInfo> resolvedColumnCapture) {
    DBColumnInfo hiddenColumn = hiddenColumnCapture.getValue();
    Assert.assertEquals(0, hiddenColumn.getDefaultValue());
    Assert.assertEquals(UpgradeCatalog260.REPO_VERSION_HIDDEN_COLUMN, hiddenColumn.getName());
    Assert.assertEquals(false, hiddenColumn.isNullable());

    DBColumnInfo resolvedColumn = resolvedColumnCapture.getValue();
    Assert.assertEquals(0, resolvedColumn.getDefaultValue());
    Assert.assertEquals(UpgradeCatalog260.REPO_VERSION_RESOLVED_COLUMN, resolvedColumn.getName());
    Assert.assertEquals(false, resolvedColumn.isNullable());
  }

  @Test
  public void testEnsureZeppelinProxyUserConfigs() throws AmbariException {

    Injector injector = getInjector();

    final Clusters clusters = injector.getInstance(Clusters.class);
    final ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    final Cluster cluster = createMock(Cluster.class);
    final Config zeppelinEnvConf = createMock(Config.class);
    final Config coreSiteConf = createMock(Config.class);
    final Config coreSiteConfNew = createMock(Config.class);
    final AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);

    Capture<? extends Map<String, String>> captureCoreSiteConfProperties = newCapture();

    configHelper.updateAgentConfigs(anyObject(Set.class));
    expectLastCall();

    expect(clusters.getClusters()).andReturn(Collections.singletonMap("c1", cluster)).once();

    expect(cluster.getClusterName()).andReturn("c1").atLeastOnce();
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("HDP-2.6")).atLeastOnce();
    expect(cluster.getDesiredConfigByType("zeppelin-env")).andReturn(zeppelinEnvConf).atLeastOnce();
    expect(cluster.getDesiredConfigByType("core-site")).andReturn(coreSiteConf).atLeastOnce();
    expect(cluster.getConfigsByType("core-site")).andReturn(Collections.singletonMap("tag1", coreSiteConf)).atLeastOnce();
    expect(cluster.getConfig(eq("core-site"), anyString())).andReturn(coreSiteConfNew).atLeastOnce();
    expect(cluster.getServiceByConfigType("core-site")).andReturn("HDFS").atLeastOnce();
    expect(cluster.addDesiredConfig(eq("ambari-upgrade"), anyObject(Set.class), anyString())).andReturn(null).atLeastOnce();

    expect(zeppelinEnvConf.getProperties()).andReturn(Collections.singletonMap("zeppelin_user", "zeppelin_user")).once();

    expect(coreSiteConf.getProperties()).andReturn(Collections.singletonMap("hadoop.proxyuser.zeppelin_user.hosts", "existing_value")).atLeastOnce();
    expect(coreSiteConf.getPropertiesAttributes()).andReturn(Collections.<String, Map<String, String>>emptyMap()).atLeastOnce();

    expect(controller.createConfig(eq(cluster), anyObject(StackId.class), eq("core-site"), capture(captureCoreSiteConfProperties), anyString(), anyObject(Map.class)))
        .andReturn(coreSiteConfNew)
        .once();

    replay(clusters, cluster, zeppelinEnvConf, coreSiteConf, coreSiteConfNew, controller, configHelper);

    UpgradeCatalog260 upgradeCatalog260 = injector.getInstance(UpgradeCatalog260.class);
    upgradeCatalog260.ensureZeppelinProxyUserConfigs();

    verify(clusters, cluster, zeppelinEnvConf, coreSiteConf, coreSiteConfNew, controller, configHelper);

    assertTrue(captureCoreSiteConfProperties.hasCaptured());
    Assert.assertEquals("existing_value", captureCoreSiteConfProperties.getValue().get("hadoop.proxyuser.zeppelin_user.hosts"));
    Assert.assertEquals("*", captureCoreSiteConfProperties.getValue().get("hadoop.proxyuser.zeppelin_user.groups"));
  }

  @Test
  public void testUpdateKerberosDescriptorArtifact() throws Exception {

    Injector injector = getInjector();

    URL systemResourceURL = ClassLoader.getSystemResource("kerberos/test_kerberos_descriptor_ranger_kms.json");
    Assert.assertNotNull(systemResourceURL);

    final KerberosDescriptor kerberosDescriptor = new KerberosDescriptorFactory().createInstance(new File(systemResourceURL.getFile()));
    Assert.assertNotNull(kerberosDescriptor);

    KerberosServiceDescriptor serviceDescriptor;
    serviceDescriptor = kerberosDescriptor.getService("RANGER_KMS");
    Assert.assertNotNull(serviceDescriptor);
    Assert.assertNotNull(serviceDescriptor.getIdentity("/smokeuser"));
    Assert.assertNotNull(serviceDescriptor.getIdentity("/spnego"));

    KerberosComponentDescriptor componentDescriptor;
    componentDescriptor = serviceDescriptor.getComponent("RANGER_KMS_SERVER");
    Assert.assertNotNull(componentDescriptor);
    Assert.assertNotNull(componentDescriptor.getIdentity("/smokeuser"));
    Assert.assertNotNull(componentDescriptor.getIdentity("/spnego"));
    Assert.assertNotNull(componentDescriptor.getIdentity("/spnego").getPrincipalDescriptor());
    Assert.assertEquals("invalid_name@${realm}", componentDescriptor.getIdentity("/spnego").getPrincipalDescriptor().getValue());

    ArtifactEntity artifactEntity = createMock(ArtifactEntity.class);

    expect(artifactEntity.getArtifactData()).andReturn(kerberosDescriptor.toMap()).once();

    Capture<Map<String, Object>> captureMap = newCapture();
    expect(artifactEntity.getForeignKeys()).andReturn(Collections.singletonMap("cluster", "2")).times(2);
    artifactEntity.setArtifactData(capture(captureMap));
    expectLastCall().once();

    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);

    configHelper.updateAgentConfigs(anyObject(Set.class));
    expectLastCall().once();

    ArtifactDAO artifactDAO = createMock(ArtifactDAO.class);
    expect(artifactDAO.merge(artifactEntity)).andReturn(artifactEntity).atLeastOnce();

    Map<String, String> properties = new HashMap<>();
    properties.put("ranger.ks.kerberos.principal", "correct_value@EXAMPLE.COM");
    properties.put("xasecure.audit.jaas.Client.option.principal", "wrong_value@EXAMPLE.COM");

    Config config = createMock(Config.class);
    expect(config.getProperties()).andReturn(properties).anyTimes();
    expect(config.getPropertiesAttributes()).andReturn(Collections.<String, Map<String, String>>emptyMap()).anyTimes();
    expect(config.getTag()).andReturn("version1").anyTimes();
    expect(config.getType()).andReturn("ranger-kms-audit").anyTimes();

    Map<String, String> hsiProperties = new HashMap<>();
    hsiProperties.put("hive.llap.daemon.keytab.file", "/etc/security/keytabs/hive.service.keytab");
    hsiProperties.put("hive.llap.zk.sm.keytab.file", "/etc/security/keytabs/hive.llap.zk.sm.keytab");

    Config hsiConfig = createMock(Config.class);
    expect(hsiConfig.getProperties()).andReturn(hsiProperties).anyTimes();
    expect(hsiConfig.getPropertiesAttributes()).andReturn(Collections.<String, Map<String, String>>emptyMap()).anyTimes();
    expect(hsiConfig.getTag()).andReturn("version1").anyTimes();
    expect(hsiConfig.getType()).andReturn("hive-interactive-site").anyTimes();

    Config newConfig = createMock(Config.class);
    expect(newConfig.getTag()).andReturn("version2").anyTimes();
    expect(newConfig.getType()).andReturn("ranger-kms-audit").anyTimes();

    Config newHsiConfig = createMock(Config.class);
    expect(newHsiConfig.getTag()).andReturn("version2").anyTimes();
    expect(newHsiConfig.getType()).andReturn("hive-interactive-site").anyTimes();

    ServiceConfigVersionResponse response = createMock(ServiceConfigVersionResponse.class);
    ServiceConfigVersionResponse response1 = createMock(ServiceConfigVersionResponse.class);

    StackId stackId = createMock(StackId.class);

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getDesiredConfigByType("dbks-site")).andReturn(config).anyTimes();
    expect(cluster.getDesiredConfigByType("ranger-kms-audit")).andReturn(config).anyTimes();
    expect(cluster.getConfigsByType("ranger-kms-audit")).andReturn(Collections.singletonMap("version1", config)).anyTimes();
    expect(cluster.getServiceByConfigType("ranger-kms-audit")).andReturn("RANGER").anyTimes();
    expect(cluster.getClusterName()).andReturn("cl1").anyTimes();
    expect(cluster.getConfig(eq("ranger-kms-audit"), anyString())).andReturn(newConfig).once();
    expect(cluster.addDesiredConfig("ambari-upgrade", Collections.singleton(newConfig), "Updated ranger-kms-audit during Ambari Upgrade from 2.5.2 to 2.6.0.")).andReturn(response).once();

    //HIVE
    expect(cluster.getDesiredConfigByType("hive-site")).andReturn(hsiConfig).anyTimes();
    expect(cluster.getDesiredConfigByType("hive-interactive-site")).andReturn(hsiConfig).anyTimes();
    expect(cluster.getConfigsByType("hive-interactive-site")).andReturn(Collections.singletonMap("version1", hsiConfig)).anyTimes();
    expect(cluster.getServiceByConfigType("hive-interactive-site")).andReturn("HIVE").anyTimes();
    expect(cluster.getConfig(eq("hive-interactive-site"), anyString())).andReturn(newHsiConfig).anyTimes();


    final Clusters clusters = injector.getInstance(Clusters.class);
    expect(clusters.getCluster(2L)).andReturn(cluster).anyTimes();

    Capture<? extends Map<String, String>> captureProperties = newCapture();

    AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);
    expect(controller.createConfig(eq(cluster), eq(stackId), eq("ranger-kms-audit"), capture(captureProperties), anyString(), anyObject(Map.class)))
        .andReturn(null)
        .once();

    Capture<? extends Map<String, String>> captureHsiProperties = newCapture();

    expect(controller.createConfig(eq(cluster), eq(stackId), eq("hive-interactive-site"), capture(captureHsiProperties), anyString(), anyObject(Map.class)))
            .andReturn(null)
            .anyTimes();

    replay(artifactDAO, artifactEntity, cluster, clusters, config, newConfig, hsiConfig, newHsiConfig, response, response1, controller, stackId, configHelper);

    UpgradeCatalog260 upgradeCatalog260 = injector.getInstance(UpgradeCatalog260.class);
    upgradeCatalog260.updateKerberosDescriptorArtifact(artifactDAO, artifactEntity);
    verify(artifactDAO, artifactEntity, cluster, clusters, config, newConfig, response, controller, stackId, configHelper);
    KerberosDescriptor kerberosDescriptorUpdated = new KerberosDescriptorFactory().createInstance(captureMap.getValue());
    Assert.assertNotNull(kerberosDescriptorUpdated);

    Assert.assertNull(kerberosDescriptorUpdated.getService("RANGER_KMS").getIdentity("/smokeuser"));
    Assert.assertNull(kerberosDescriptorUpdated.getService("RANGER_KMS").getComponent("RANGER_KMS_SERVER").getIdentity("/smokeuser"));

    KerberosIdentityDescriptor identity;

    Assert.assertNull(kerberosDescriptorUpdated.getService("RANGER_KMS").getIdentity("/spnego"));
    identity = kerberosDescriptorUpdated.getService("RANGER_KMS").getIdentity("ranger_kms_spnego");
    Assert.assertNotNull(identity);
    Assert.assertEquals("/spnego", identity.getReference());

    Assert.assertNull(kerberosDescriptorUpdated.getService("RANGER_KMS").getComponent("RANGER_KMS_SERVER").getIdentity("/spnego"));
    identity = kerberosDescriptorUpdated.getService("RANGER_KMS").getComponent("RANGER_KMS_SERVER").getIdentity("ranger_kms_ranger_kms_server_spnego");
    Assert.assertNotNull(identity);
    Assert.assertEquals("/spnego", identity.getReference());
    Assert.assertNotNull(identity.getPrincipalDescriptor());
    Assert.assertNull(identity.getPrincipalDescriptor().getValue());

    Assert.assertTrue(captureProperties.hasCaptured());
    Map<String, String> newProperties = captureProperties.getValue();
    Assert.assertEquals("correct_value@EXAMPLE.COM", newProperties.get("xasecure.audit.jaas.Client.option.principal"));

    // YARN's NodeManager identities (1). 'llap_zk_hive' and (2). 'llap_task_hive' checks after modifications.
    Map<String, List<String>> identitiesMap = new HashMap<>();
    identitiesMap.put("llap_zk_hive", new ArrayList<String>() {{
      add("hive-interactive-site/hive.llap.zk.sm.keytab.file");
      add("hive-interactive-site/hive.llap.zk.sm.principal");
    }});
    identitiesMap.put("llap_task_hive", new ArrayList<String>() {{
      add("hive-interactive-site/hive.llap.task.keytab.file");
      add("hive-interactive-site/hive.llap.task.principal");
    }});
    for (String llapIdentity : identitiesMap.keySet()) {
      KerberosIdentityDescriptor yarnKerberosIdentityDescriptor = kerberosDescriptorUpdated.getService("YARN").getComponent("NODEMANAGER").getIdentity(llapIdentity);
      Assert.assertNotNull(yarnKerberosIdentityDescriptor);
      Assert.assertEquals("/HIVE/HIVE_SERVER/hive_server_hive", yarnKerberosIdentityDescriptor.getReference());

      KerberosKeytabDescriptor yarnKerberosKeytabDescriptor = yarnKerberosIdentityDescriptor.getKeytabDescriptor();
      Assert.assertNotNull(yarnKerberosKeytabDescriptor);

      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getGroupAccess());
      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getGroupName());
      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getOwnerAccess());
      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getOwnerName());
      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getFile());
      Assert.assertEquals(identitiesMap.get(llapIdentity).get(0), yarnKerberosKeytabDescriptor.getConfiguration());

      KerberosPrincipalDescriptor yarnKerberosPrincipalDescriptor = yarnKerberosIdentityDescriptor.getPrincipalDescriptor();
      Assert.assertNotNull(yarnKerberosPrincipalDescriptor);
      Assert.assertEquals(null, yarnKerberosPrincipalDescriptor.getName());
      Assert.assertEquals(KerberosPrincipalType.SERVICE, yarnKerberosPrincipalDescriptor.getType());
      Assert.assertEquals(null, yarnKerberosPrincipalDescriptor.getValue());
      Assert.assertEquals(identitiesMap.get(llapIdentity).get(1), yarnKerberosPrincipalDescriptor.getConfiguration());
    }
  }

  @Test
  public void testUpdateAmsConfigs() throws Exception {

    Map<String, String> oldProperties = new HashMap<String, String>() {
      {
        put("ssl.client.truststore.location", "/some/location");
        put("ssl.client.truststore.alias", "test_alias");
      }
    };
    Map<String, String> newProperties = new HashMap<String, String>() {
      {
        put("ssl.client.truststore.location", "/some/location");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockAmsSslClient = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-ssl-client")).andReturn(mockAmsSslClient).atLeastOnce();
    expect(mockAmsSslClient.getProperties()).andReturn(oldProperties).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();

    replay(injector, clusters, mockAmsSslClient, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[] { })
        .addMockedMethod("createConfig")
        .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
        .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
        anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    replay(controller, injector2);
    new UpgradeCatalog260(injector2).updateAmsConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
  }

  @Test
  public void testUpdateHiveConfigs() throws Exception {

    Map<String, String> oldProperties = new HashMap<String, String>() {
      {
        put("hive.llap.zk.sm.keytab.file", "/etc/security/keytabs/hive.llap.zk.sm.keytab");
        put("hive.llap.daemon.keytab.file", "/etc/security/keytabs/hive.service.keytab");
        put("hive.llap.task.keytab.file", "/etc/security/keytabs/hive.llap.task.keytab");
      }
    };
    Map<String, String> newProperties = new HashMap<String, String>() {
      {
        put("hive.llap.zk.sm.keytab.file", "/etc/security/keytabs/hive.service.keytab");
        put("hive.llap.daemon.keytab.file", "/etc/security/keytabs/hive.service.keytab");
        put("hive.llap.task.keytab.file", "/etc/security/keytabs/hive.service.keytab");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockHsiConfigs = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("hive-interactive-site")).andReturn(mockHsiConfigs).atLeastOnce();
    expect(mockHsiConfigs.getProperties()).andReturn(oldProperties).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();

    replay(injector, clusters, mockHsiConfigs, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
            .addMockedMethod("createConfiguration")
            .addMockedMethod("getClusters", new Class[] { })
            .addMockedMethod("createConfig")
            .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
            .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
            anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();
    replay(controller, injector2);

    // This tests the update of HSI config 'hive.llap.daemon.keytab.file'.
    UpgradeCatalog260  upgradeCatalog260 = new UpgradeCatalog260(injector2);
    // Set 'isYarnKerberosDescUpdated' value to true, implying kerberos descriptor was updated.
    upgradeCatalog260.updateYarnKerberosDescUpdatedList("hive.llap.zk.sm.keytab.file");
    upgradeCatalog260.updateYarnKerberosDescUpdatedList("hive.llap.task.keytab.file");

    upgradeCatalog260.updateHiveConfigs();

    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
  }

  @Test
  public void testHDFSWidgetUpdate() throws Exception {
    final Clusters clusters = createNiceMock(Clusters.class);
    final Cluster cluster = createNiceMock(Cluster.class);
    final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    final Gson gson = new Gson();
    final WidgetDAO widgetDAO = createNiceMock(WidgetDAO.class);
    final AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    WidgetEntity widgetEntity = createNiceMock(WidgetEntity.class);
    StackId stackId = new StackId("HDP", "2.0.0");
    StackInfo stackInfo = createNiceMock(StackInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    Service service  = createNiceMock(Service.class);

    String widgetStr = "{\n" +
        "  \"layouts\": [\n" +
        "      {\n" +
        "      \"layout_name\": \"default_hdfs_heatmap\",\n" +
        "      \"display_name\": \"Standard HDFS HeatMaps\",\n" +
        "      \"section_name\": \"HDFS_HEATMAPS\",\n" +
        "      \"widgetLayoutInfo\": [\n" +
        "        {\n" +
        "          \"widget_name\": \"HDFS Bytes Read\",\n" +
        "          \"metrics\": [],\n" +
        "          \"values\": []\n" +
        "        }\n" +
        "      ]\n" +
        "    }\n" +
        "  ]\n" +
        "}";

    File dataDirectory = temporaryFolder.newFolder();
    File file = new File(dataDirectory, "hdfs_widget.json");
    FileUtils.writeStringToFile(file, widgetStr, Charset.defaultCharset());

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        PartialNiceMockBinder.newBuilder().addConfigsBindings().addFactoriesInstallBinding()
        .addPasswordEncryptorBindings().addLdapBindings().build().configure(binder());

        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(AmbariManagementController.class).toInstance(controller);
        bind(Clusters.class).toInstance(clusters);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Gson.class).toInstance(gson);
        bind(WidgetDAO.class).toInstance(widgetDAO);
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(AmbariMetaInfo.class).toInstance(metaInfo);
        bind(ActionDBAccessor.class).toInstance(createNiceMock(ActionDBAccessorImpl.class));
        bind(PersistedState.class).toInstance(mock(PersistedStateImpl.class));
        bind(UnitOfWork.class).toInstance(createNiceMock(UnitOfWork.class));
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(StageFactory.class).to(StageFactoryImpl.class);
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLoggerDefaultImpl.class));
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
        bind(HookService.class).to(UserHookService.class);
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
        bind(CredentialStoreService.class).toInstance(createNiceMock(CredentialStoreService.class));
        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        bind(STOMPUpdatePublisher.class).toInstance(createNiceMock(STOMPUpdatePublisher.class));
        bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelperImpl.class));
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
      }
    });
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).anyTimes();
    expect(cluster.getServices()).andReturn(Collections.singletonMap("HDFS", service)).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(service.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(stackInfo.getService("HDFS")).andReturn(serviceInfo);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(metaInfo.getStack("HDP", "2.0.0")).andReturn(stackInfo).anyTimes();
    expect(serviceInfo.getWidgetsDescriptorFile()).andReturn(file).anyTimes();

    expect(widgetDAO.findByName(1L, "HDFS Bytes Read", "ambari", "HDFS_HEATMAPS"))
        .andReturn(Collections.singletonList(widgetEntity));
    expect(widgetDAO.merge(widgetEntity)).andReturn(null);
    expect(widgetEntity.getWidgetName()).andReturn("HDFS Bytes Read").anyTimes();

    replay(clusters, cluster, controller, widgetDAO, metaInfo, widgetEntity, stackInfo, serviceInfo, service);

    mockInjector.getInstance(UpgradeCatalog260.class).updateHDFSWidgetDefinition();

    verify(clusters, cluster, controller, widgetDAO, widgetEntity, stackInfo, serviceInfo);
  }

  private Injector getInjector() {

    return Guice.createInjector(new Module() {
      @Override
      public void configure(Binder binder) {
        PartialNiceMockBinder.newBuilder().addPasswordEncryptorBindings().addLdapBindings().build().configure(binder);
        binder.bindConstant().annotatedWith(Names.named("actionTimeout")).to(600000L);
        binder.bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(1L);
        binder.bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_ENABLED)).to(true);
        binder.bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_SIZE)).to(10000L);
        binder.bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES)).to(30L);

        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(osFamily);
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(Clusters.class).toInstance(createMock(Clusters.class));
        binder.bind(AmbariManagementController.class).toInstance(createMock(AmbariManagementControllerImpl.class));
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
        binder.bind(MetadataHolder.class).toInstance(createNiceMock(MetadataHolder.class));
        binder.bind(AgentConfigsHolder.class).toInstance(createNiceMock(AgentConfigsHolder.class));
        binder.bind(ConfigHelper.class).toInstance(createStrictMock(ConfigHelper.class));
        binder.bind(MpackManagerFactory.class).toInstance(createStrictMock(MpackManagerFactory.class));


        binder.install(new FactoryModuleBuilder().build(RequestFactory.class));
        binder.install(new FactoryModuleBuilder().build(ConfigureClusterTaskFactory.class));
        binder.install(new FactoryModuleBuilder().implement(
            Config.class, ConfigImpl.class).build(ConfigFactory.class));
        binder.install(new FactoryModuleBuilder().build(StackManagerFactory.class));
        binder.install(new FactoryModuleBuilder().implement(HookContext.class, PostUserCreationHookContext.class)
            .build(HookContextFactory.class));
        binder.install(new FactoryModuleBuilder().build(RoleGraphFactory.class));
        binder.install(new FactoryModuleBuilder().implement(RequestExecution.class,
            RequestExecutionImpl.class).build(RequestExecutionFactory.class));
        binder.install(new FactoryModuleBuilder().implement(
            ConfigGroup.class, ConfigGroupImpl.class).build(ConfigGroupFactory.class));
        binder.install(new FactoryModuleBuilder().implement(AmbariEvent.class, Names.named("userCreated"), UserCreatedEvent.class)
            .build(AmbariEventFactory.class));
        binder.install(new FactoryModuleBuilder().implement(
            ServiceComponent.class, ServiceComponentImpl.class).build(
            ServiceComponentFactory.class));
        binder.install(new FactoryModuleBuilder().implement(
            Host.class, HostImpl.class).build(HostFactory.class));
        binder.install(new FactoryModuleBuilder().implement(
            Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
        binder.install(new FactoryModuleBuilder().implement(
            Service.class, ServiceImpl.class).build(ServiceFactory.class));
        binder.install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
      }
    });
  }

}
