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

import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_PERMISSION_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_RESOURCE_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_INFRA_NEW_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_INFRA_OLD_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_SEQUENCES_SEQUENCE_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_SEQUENCES_SEQUENCE_VALUE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_SEQUENCES_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.COMPONENT_DESIRED_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.COMPONENT_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.COMPONENT_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_HOSTCOMPONENTDESIREDSTATE_COMPONENT_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_HOSTCOMPONENTSTATE_COMPONENT_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_KKP_HOST_ID;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_KKP_KEYTAB_PATH;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_KKP_PRINCIPAL_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_KKP_SERVICE_PRINCIPAL;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_SERVICECOMPONENTDESIREDSTATE_SERVICE_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.HOSTS_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.HOST_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KERBEROS_KEYTAB_PRINCIPAL_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KERBEROS_KEYTAB_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KERBEROS_PRINCIPAL_HOST_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KERBEROS_PRINCIPAL_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KEYTAB_PATH_FIELD;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KKP_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KKP_MAPPING_SERVICE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.MEMBERS_GROUP_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.MEMBERS_MEMBER_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.MEMBERS_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.MEMBERS_USER_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.PK_KERBEROS_KEYTAB;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.PK_KKP;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.PK_KKP_MAPPING_SERVICE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.PRINCIPAL_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_APPLICABLE_SERVICES_FOREIGN_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_APPLICABLE_SERVICES_REPO_DEFINITION_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_APPLICABLE_SERVICES_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_BASE_URL_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_COMPONENTS_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_DISTRIBUTION_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_FOREIGN_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_MIRRORS_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_PRIMARY_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_REPO_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_REPO_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_REPO_OS_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_DEFINITION_UNIQUE_REPO_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_OS_AMBARI_MANAGED_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_OS_FAMILY_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_OS_FOREIGN_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_OS_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_OS_PRIMARY_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_OS_REPO_VERSION_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_OS_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_TAGS_FOREIGN_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_TAGS_REPO_DEFINITION_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_TAGS_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_TAGS_TAG_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_VERSION_REPOSITORIES_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_VERSION_REPO_VERSION_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REPO_VERSION_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REQUEST_DISPLAY_STATUS_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REQUEST_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REQUEST_USER_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.SECURITY_STATE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.SERVICE_COMPONENT_DESIRED_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.SERVICE_DESIRED_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.SERVICE_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.STAGE_DISPLAY_STATUS_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.STAGE_STATUS_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.STAGE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.UNIQUE_USERS_0_INDEX;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.UNI_KKP;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_CONSECUTIVE_FAILURES_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_CREATE_TIME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_DISPLAY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_LDAP_USER_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_LOCAL_USERNAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_USER_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_USER_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_USER_PASSWORD_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_USER_TYPE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_VERSION_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_CREATE_TIME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_PRIMARY_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_UPDATE_TIME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_USER_AUTHENTICATION_USERS_FOREIGN_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_USER_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.WIDGET_TABLE;
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
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.startsWith;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.actionmanager.StageFactoryImpl;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.AuditLoggerDefaultImpl;
import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.KerberosHelperImpl;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.serveraction.kerberos.PrepareKerberosIdentitiesServerAction;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.easymock.Capture;
import org.easymock.CaptureType;
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
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.persist.UnitOfWork;

@RunWith(EasyMockRunner.class)
public class UpgradeCatalog270Test {
  public static final Gson GSON = new Gson();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock(type = MockType.STRICT)
  private Provider<EntityManager> entityManagerProvider;

  @Mock(type = MockType.NICE)
  private Injector injector;

  @Mock(type = MockType.NICE)
  private EntityManager entityManager;

  @Mock(type = MockType.DEFAULT)
  private DBAccessor dbAccessor;

  @Mock(type = MockType.NICE)
  private OsFamily osFamily;

  @Mock(type = MockType.NICE)
  private Config config;

  @Mock(type = MockType.NICE)
  private ActionManager actionManager;

  @Mock(type = MockType.NICE)
  private Clusters clusters;

  @Mock(type = MockType.NICE)
  private Cluster cluster;

  @Mock(type = MockType.NICE)
  AmbariConfigurationDAO ambariConfigurationDao;

  @Mock(type = MockType.NICE)
  ArtifactDAO artifactDAO;

  @Mock(type = MockType.NICE)
  private AmbariManagementController ambariManagementController;

  @Before
  public void init() {
    reset(entityManagerProvider, injector);

    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();

    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();

    replay(entityManagerProvider, injector);
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method showHcatDeletedUserMessage = UpgradeCatalog270.class.getDeclaredMethod("showHcatDeletedUserMessage");
    Method setStatusOfStagesAndRequests = UpgradeCatalog270.class.getDeclaredMethod("setStatusOfStagesAndRequests");
    Method updateLogSearchConfigs = UpgradeCatalog270.class.getDeclaredMethod("updateLogSearchConfigs");
    Method updateKerberosConfigurations = UpgradeCatalog270.class.getDeclaredMethod("updateKerberosConfigurations");
    Method upgradeLdapConfiguration = UpgradeCatalog270.class.getDeclaredMethod("moveAmbariPropertiesToAmbariConfiguration");
    Method createRoleAuthorizations = UpgradeCatalog270.class.getDeclaredMethod("createRoleAuthorizations");
    Method addUserAuthenticationSequence = UpgradeCatalog270.class.getDeclaredMethod("addUserAuthenticationSequence");
    Method renameAmbariInfra = UpgradeCatalog270.class.getDeclaredMethod("renameAmbariInfra");
    Method updateKerberosDescriptorArtifacts = UpgradeCatalog270.class.getSuperclass().getDeclaredMethod("updateKerberosDescriptorArtifacts");
    Method updateSolrConfigurations = UpgradeCatalog270.class.getDeclaredMethod("updateSolrConfigurations");
    Method updateAmsConfigurations = UpgradeCatalog270.class.getDeclaredMethod("updateAmsConfigs");
    Method updateStormConfigurations = UpgradeCatalog270.class.getDeclaredMethod("updateStormConfigs");
    Method clearHadoopMetrics2Content = UpgradeCatalog270.class.getDeclaredMethod("clearHadoopMetrics2Content");

    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class)
        .addMockedMethod(showHcatDeletedUserMessage)
        .addMockedMethod(addNewConfigurationsFromXml)
        .addMockedMethod(setStatusOfStagesAndRequests)
        .addMockedMethod(updateLogSearchConfigs)
        .addMockedMethod(updateKerberosConfigurations)
        .addMockedMethod(upgradeLdapConfiguration)
        .addMockedMethod(createRoleAuthorizations)
        .addMockedMethod(addUserAuthenticationSequence)
        .addMockedMethod(renameAmbariInfra)
        .addMockedMethod(updateKerberosDescriptorArtifacts)
        .addMockedMethod(updateSolrConfigurations)
        .addMockedMethod(updateAmsConfigurations)
        .addMockedMethod(updateStormConfigurations)
        .addMockedMethod(clearHadoopMetrics2Content)
        .createMock();


    upgradeCatalog270.addNewConfigurationsFromXml();
    expectLastCall().once();

    upgradeCatalog270.showHcatDeletedUserMessage();
    expectLastCall().once();

    upgradeCatalog270.createRoleAuthorizations();
    expectLastCall().once();

    upgradeCatalog270.setStatusOfStagesAndRequests();
    expectLastCall().once();

    upgradeCatalog270.updateLogSearchConfigs();

    upgradeCatalog270.updateKerberosConfigurations();
    expectLastCall().once();

    upgradeCatalog270.moveAmbariPropertiesToAmbariConfiguration();
    expectLastCall().once();

    upgradeCatalog270.addUserAuthenticationSequence();
    expectLastCall().once();

    upgradeCatalog270.renameAmbariInfra();
    expectLastCall().once();

    upgradeCatalog270.updateKerberosDescriptorArtifacts();
    expectLastCall().once();

    upgradeCatalog270.updateSolrConfigurations();
    expectLastCall().once();

    upgradeCatalog270.updateAmsConfigs();
    expectLastCall().once();

    upgradeCatalog270.updateStormConfigs();
    expectLastCall().once();

    upgradeCatalog270.clearHadoopMetrics2Content();
    expectLastCall().once();

    replay(upgradeCatalog270);

    upgradeCatalog270.executeDMLUpdates();

    verify(upgradeCatalog270);
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    // dropBrokenFKs
    dbAccessor.dropFKConstraint(COMPONENT_DESIRED_STATE_TABLE, FK_HOSTCOMPONENTDESIREDSTATE_COMPONENT_NAME);
    expectLastCall().once();
    dbAccessor.dropFKConstraint(COMPONENT_STATE_TABLE, FK_HOSTCOMPONENTSTATE_COMPONENT_NAME);
    expectLastCall().once();
    dbAccessor.dropFKConstraint(SERVICE_COMPONENT_DESIRED_STATE_TABLE, FK_SERVICECOMPONENTDESIREDSTATE_SERVICE_NAME);
    expectLastCall().once();

    // updateStageTable
    Capture<DBAccessor.DBColumnInfo> updateStageTableCaptures = newCapture(CaptureType.ALL);
    dbAccessor.addColumn(eq(STAGE_TABLE), capture(updateStageTableCaptures));
    expectLastCall().once();
    dbAccessor.addColumn(eq(STAGE_TABLE), capture(updateStageTableCaptures));
    expectLastCall().once();
    dbAccessor.addColumn(eq(REQUEST_TABLE), capture(updateStageTableCaptures));
    expectLastCall().once();

    // updateRequestTable
    Capture<DBAccessor.DBColumnInfo> updateRequestTableCapture = newCapture(CaptureType.ALL);
    dbAccessor.addColumn(eq(REQUEST_TABLE), capture(updateRequestTableCapture));
    expectLastCall().once();

    // updateWidgetTable
    Capture<DBAccessor.DBColumnInfo> updateWidgetTableCapture = newCapture(CaptureType.ALL);
    dbAccessor.addColumn(eq(WIDGET_TABLE), capture(updateWidgetTableCapture));
    expectLastCall().once();

    // addOpsDisplayNameColumnToHostRoleCommand
    Capture<DBAccessor.DBColumnInfo> hrcOpsDisplayNameColumn = newCapture();
    dbAccessor.addColumn(eq(UpgradeCatalog270.HOST_ROLE_COMMAND_TABLE), capture(hrcOpsDisplayNameColumn));
    expectLastCall().once();

    Capture<DBAccessor.DBColumnInfo> lastValidColumn = newCapture();
    dbAccessor.addColumn(eq(UpgradeCatalog270.COMPONENT_STATE_TABLE), capture(lastValidColumn));

    // removeSecurityState
    dbAccessor.dropColumn(COMPONENT_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
    expectLastCall().once();
    dbAccessor.dropColumn(COMPONENT_STATE_TABLE, SECURITY_STATE_COLUMN);
    expectLastCall().once();
    dbAccessor.dropColumn(SERVICE_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
    expectLastCall().once();

    // addAmbariConfigurationTable
    Capture<List<DBAccessor.DBColumnInfo>> ambariConfigurationTableColumns = newCapture();
    dbAccessor.createTable(eq(AMBARI_CONFIGURATION_TABLE), capture(ambariConfigurationTableColumns));
    expectLastCall().once();
    dbAccessor.addPKConstraint(AMBARI_CONFIGURATION_TABLE, "PK_ambari_configuration", AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN);
    expectLastCall().once();

    //upgradeUserTable - converting users.create_time to long
    Capture<DBAccessor.DBColumnInfo> temporaryColumnCreationCapture = newCapture(CaptureType.ALL);
    Capture<DBAccessor.DBColumnInfo> temporaryColumnRenameCapture = newCapture(CaptureType.ALL);

    // upgradeUserTable - create user_authentication table
    Capture<List<DBAccessor.DBColumnInfo>> createUserAuthenticationTableCaptures = newCapture(CaptureType.ALL);
    Capture<List<DBAccessor.DBColumnInfo>> createMembersTableCaptures = newCapture(CaptureType.ALL);
    Capture<List<DBAccessor.DBColumnInfo>> createAdminPrincipalTableCaptures = newCapture(CaptureType.ALL);
    Capture<DBAccessor.DBColumnInfo> updateUserTableCaptures = newCapture(CaptureType.ALL);
    Capture<DBAccessor.DBColumnInfo> alterUserTableCaptures = newCapture(CaptureType.ALL);

    Capture<List<DBAccessor.DBColumnInfo>> addRepoOsTableCapturedColumns = newCapture(CaptureType.ALL);
    Capture<List<DBAccessor.DBColumnInfo>> addRepoDefinitionTableCapturedColumns = newCapture(CaptureType.ALL);
    Capture<List<DBAccessor.DBColumnInfo>> addRepoTagsTableCapturedColumns = newCapture(CaptureType.ALL);
    Capture<List<DBAccessor.DBColumnInfo>> addRepoApplicableServicesTableCapturedColumns = newCapture(CaptureType.ALL);
    Capture<String[]> insertRepoOsTableRowColumns = newCapture(CaptureType.ALL);
    Capture<String[]> insertRepoOsTableRowValues = newCapture(CaptureType.ALL);
    Capture<String[]> insertAmbariSequencesRowColumns = newCapture(CaptureType.ALL);
    Capture<String[]> insertAmbariSequencesRowValues = newCapture(CaptureType.ALL);

    // Any return value will work here as long as a SQLException is not thrown.
    expect(dbAccessor.getColumnType(USERS_TABLE, USERS_USER_TYPE_COLUMN)).andReturn(0).anyTimes();

    prepareConvertingUsersCreationTime(dbAccessor, temporaryColumnCreationCapture, temporaryColumnRenameCapture);
    prepareCreateUserAuthenticationTable(dbAccessor, createUserAuthenticationTableCaptures);
    prepareUpdateGroupMembershipRecords(dbAccessor, createMembersTableCaptures);
    prepareUpdateAdminPrivilegeRecords(dbAccessor, createAdminPrincipalTableCaptures);
    prepareUpdateUsersTable(dbAccessor, updateUserTableCaptures, alterUserTableCaptures);
    prepareUpdateRepoTables(dbAccessor, addRepoOsTableCapturedColumns, addRepoDefinitionTableCapturedColumns, addRepoTagsTableCapturedColumns,
      addRepoApplicableServicesTableCapturedColumns, insertRepoOsTableRowColumns, insertRepoOsTableRowValues, insertAmbariSequencesRowColumns, insertAmbariSequencesRowValues);

    // upgradeKerberosTables
    Capture<List<DBAccessor.DBColumnInfo>> kerberosKeytabColumnsCapture = newCapture();
    dbAccessor.createTable(eq(KERBEROS_KEYTAB_TABLE), capture(kerberosKeytabColumnsCapture));
    expectLastCall().once();
    dbAccessor.addPKConstraint(KERBEROS_KEYTAB_TABLE, PK_KERBEROS_KEYTAB, KEYTAB_PATH_FIELD);
    expectLastCall().once();

    Capture<List<DBAccessor.DBColumnInfo>> kerberosKeytabPrincipalColumnsCapture = newCapture();
    dbAccessor.createTable(eq(KERBEROS_KEYTAB_PRINCIPAL_TABLE), capture(kerberosKeytabPrincipalColumnsCapture));
    expectLastCall().once();
    dbAccessor.addPKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, PK_KKP, KKP_ID_COLUMN);
    expectLastCall().once();
    dbAccessor.addUniqueConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, UNI_KKP, KEYTAB_PATH_FIELD, PRINCIPAL_NAME_COLUMN, HOST_ID_COLUMN);
    expectLastCall().once();

    Capture<List<DBAccessor.DBColumnInfo>> mappingColumnsCapture = newCapture();
    dbAccessor.createTable(eq(KKP_MAPPING_SERVICE_TABLE), capture(mappingColumnsCapture));
    expectLastCall().once();
    dbAccessor.addPKConstraint(KKP_MAPPING_SERVICE_TABLE, PK_KKP_MAPPING_SERVICE, KKP_ID_COLUMN, SERVICE_NAME_COLUMN, COMPONENT_NAME_COLUMN);
    expectLastCall().once();

    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_KEYTAB_PATH, KEYTAB_PATH_FIELD, KERBEROS_KEYTAB_TABLE, KEYTAB_PATH_FIELD, false);
    expectLastCall().once();
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_HOST_ID,HOST_ID_COLUMN, HOSTS_TABLE, HOST_ID_COLUMN, false);
    expectLastCall().once();
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_PRINCIPAL_NAME, PRINCIPAL_NAME_COLUMN, KERBEROS_PRINCIPAL_TABLE, PRINCIPAL_NAME_COLUMN, false);
    expectLastCall().once();
    dbAccessor.addFKConstraint(KKP_MAPPING_SERVICE_TABLE, FK_KKP_SERVICE_PRINCIPAL, KKP_ID_COLUMN, KERBEROS_KEYTAB_PRINCIPAL_TABLE, KKP_ID_COLUMN, false);
    expectLastCall().once();

    Connection c = niceMock(Connection.class);
    Statement s = niceMock(Statement.class);
    expect(s.executeQuery(anyString())).andReturn(null).once();
    expect(c.createStatement()).andReturn(s).once();
    expect(dbAccessor.getConnection()).andReturn(c).once();

    dbAccessor.dropTable(KERBEROS_PRINCIPAL_HOST_TABLE);

    replay(dbAccessor);

    Injector injector = Guice.createInjector(getTestGuiceModule());
    UpgradeCatalog270 upgradeCatalog270 = injector.getInstance(UpgradeCatalog270.class);
    upgradeCatalog270.executeDDLUpdates();

    // Validate updateStageTableCaptures
    Assert.assertTrue(updateStageTableCaptures.hasCaptured());
    validateColumns(updateStageTableCaptures.getValues(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(STAGE_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false),
            new DBAccessor.DBColumnInfo(STAGE_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false),
            new DBAccessor.DBColumnInfo(REQUEST_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false))
    );

    // Validate updateRequestTableCapture
    Assert.assertTrue(updateRequestTableCapture.hasCaptured());
    validateColumns(updateRequestTableCapture.getValues(),
            Arrays.asList(new DBAccessor.DBColumnInfo(REQUEST_USER_NAME_COLUMN, String.class, 255))
    );

    DBAccessor.DBColumnInfo capturedOpsDisplayNameColumn = hrcOpsDisplayNameColumn.getValue();
    Assert.assertEquals(UpgradeCatalog270.HRC_OPS_DISPLAY_NAME_COLUMN, capturedOpsDisplayNameColumn.getName());
    Assert.assertEquals(null, capturedOpsDisplayNameColumn.getDefaultValue());
    Assert.assertEquals(String.class, capturedOpsDisplayNameColumn.getType());

    // Ambari configuration table addition...
    Assert.assertTrue(ambariConfigurationTableColumns.hasCaptured());
    validateColumns(ambariConfigurationTableColumns.getValue(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, String.class, 100, null, false),
            new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN, String.class, 100, null, false),
            new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN, String.class, 2048, null, true))
    );

    List<DBAccessor.DBColumnInfo> columns = ambariConfigurationTableColumns.getValue();
    Assert.assertEquals(3, columns.size());

    for (DBAccessor.DBColumnInfo column : columns) {
      String columnName = column.getName();

      if (AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN.equals(columnName)) {
        Assert.assertEquals(String.class, column.getType());
        Assert.assertEquals(Integer.valueOf(100), column.getLength());
        Assert.assertEquals(null, column.getDefaultValue());
        Assert.assertFalse(column.isNullable());
      } else if (AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN.equals(columnName)) {
        Assert.assertEquals(String.class, column.getType());
        Assert.assertEquals(Integer.valueOf(100), column.getLength());
        Assert.assertEquals(null, column.getDefaultValue());
        Assert.assertFalse(column.isNullable());
      } else if (AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN.equals(columnName)) {
        Assert.assertEquals(String.class, column.getType());
        Assert.assertEquals(Integer.valueOf(2048), column.getLength());
        Assert.assertEquals(null, column.getDefaultValue());
        Assert.assertTrue(column.isNullable());
      } else {
        Assert.fail("Unexpected column name: " + columnName);
      }
    }
    // Ambari configuration table addition...

    DBAccessor.DBColumnInfo capturedLastValidColumn = lastValidColumn.getValue();
    Assert.assertEquals(upgradeCatalog270.COMPONENT_LAST_STATE_COLUMN, capturedLastValidColumn.getName());
    Assert.assertEquals(State.UNKNOWN, capturedLastValidColumn.getDefaultValue());
    Assert.assertEquals(String.class, capturedLastValidColumn.getType());

    validateConvertingUserCreationTime(temporaryColumnCreationCapture,temporaryColumnRenameCapture);
    validateCreateUserAuthenticationTable(createUserAuthenticationTableCaptures);
    validateUpdateGroupMembershipRecords(createMembersTableCaptures);
    validateUpdateAdminPrivilegeRecords(createAdminPrincipalTableCaptures);
    validateUpdateUsersTable(updateUserTableCaptures, alterUserTableCaptures);
    validateCreateRepoOsTable(addRepoOsTableCapturedColumns, addRepoDefinitionTableCapturedColumns, addRepoTagsTableCapturedColumns,
        insertRepoOsTableRowColumns, insertRepoOsTableRowValues, insertAmbariSequencesRowColumns, insertAmbariSequencesRowValues);

    verify(dbAccessor);
  }

  private Module getTestGuiceModule() {
    Module module = new AbstractModule() {
      @Override
      public void configure() {
        PartialNiceMockBinder.newBuilder().addConfigsBindings().addPasswordEncryptorBindings().addLdapBindings().addFactoriesInstallBinding().build().configure(binder());

        bind(DBAccessor.class).toInstance(dbAccessor);
        bind(OsFamily.class).toInstance(osFamily);
        bind(EntityManager.class).toInstance(entityManager);
        bind(AmbariConfigurationDAO.class).toInstance(ambariConfigurationDao);
        bind(PersistedState.class).toInstance(mock(PersistedStateImpl.class));
        bind(Clusters.class).toInstance(mock(ClustersImpl.class));
        bind(SecurityHelper.class).toInstance(mock(SecurityHelper.class));
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(ActionDBAccessor.class).toInstance(createNiceMock(ActionDBAccessorImpl.class));
        bind(UnitOfWork.class).toInstance(createNiceMock(UnitOfWork.class));
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(StageFactory.class).to(StageFactoryImpl.class);
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLoggerDefaultImpl.class));
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
        bind(HookService.class).to(UserHookService.class);
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
        bind(CredentialStoreService.class).toInstance(createNiceMock(CredentialStoreService.class));
        bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementControllerImpl.class));
        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        bind(AmbariMetaInfo.class).toInstance(createNiceMock(AmbariMetaInfo.class));
        bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelperImpl.class));
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));

        install(new FactoryModuleBuilder().implement(
            Host.class, HostImpl.class).build(HostFactory.class));
        install(new FactoryModuleBuilder().implement(
            Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
        install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
        install(new FactoryModuleBuilder().implement(
            Service.class, ServiceImpl.class).build(ServiceFactory.class));
//        binder.bind(Configuration.class).toInstance(configuration);
//        binder.bind(AmbariManagementController.class).toInstance(ambariManagementController);
      }
    };
    return module;
  }

  private void prepareConvertingUsersCreationTime(DBAccessor dbAccessor, Capture<DBAccessor.DBColumnInfo> temporaryColumnCreationCapture,
      Capture<DBAccessor.DBColumnInfo> temporaryColumnRenameCapture) throws SQLException {
    expect(dbAccessor.getColumnType(USERS_TABLE, USERS_CREATE_TIME_COLUMN)).andReturn(Types.TIMESTAMP).once();
    final String temporaryColumnName = USERS_CREATE_TIME_COLUMN + "_numeric";
    expect(dbAccessor.tableHasColumn(USERS_TABLE, temporaryColumnName)).andReturn(Boolean.FALSE);
    dbAccessor.addColumn(eq(USERS_TABLE), capture(temporaryColumnCreationCapture));
    expect(dbAccessor.tableHasColumn(USERS_TABLE, USERS_CREATE_TIME_COLUMN)).andReturn(Boolean.TRUE);

    final Connection connectionMock = niceMock(Connection.class);
    expect(dbAccessor.getConnection()).andReturn(connectionMock).once();
    final PreparedStatement preparedStatementMock = niceMock(PreparedStatement.class);
    expect(connectionMock.prepareStatement(anyString())).andReturn(preparedStatementMock);
    final ResultSet resultSetMock = niceMock(ResultSet.class);
    expect(preparedStatementMock.executeQuery()).andReturn(resultSetMock);
    expect(resultSetMock.next()).andReturn(Boolean.TRUE).once();
    expect(resultSetMock.getInt(1)).andReturn(1).anyTimes();
    expect(resultSetMock.getTimestamp(2)).andReturn(new Timestamp(1l)).anyTimes();
    replay(connectionMock, preparedStatementMock, resultSetMock);

    expect(dbAccessor.updateTable(eq(USERS_TABLE), eq(temporaryColumnName), eq(1l), anyString())).andReturn(anyInt());

    dbAccessor.dropColumn(USERS_TABLE, USERS_CREATE_TIME_COLUMN);
    expectLastCall().once();

    dbAccessor.renameColumn(eq(USERS_TABLE), eq(temporaryColumnName), capture(temporaryColumnRenameCapture));
  }

  private void prepareCreateUserAuthenticationTable(DBAccessor dbAccessor, Capture<List<DBAccessor.DBColumnInfo>> capturedData)
      throws SQLException {

    String temporaryTableName = USER_AUTHENTICATION_TABLE + "_tmp";

    dbAccessor.dropTable(eq(temporaryTableName));
    expectLastCall().times(2);
    dbAccessor.createTable(eq(temporaryTableName), capture(capturedData));
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + temporaryTableName))).andReturn(1).once();
    expect(dbAccessor.executeUpdate(startsWith("update " + temporaryTableName))).andReturn(1).once();

    dbAccessor.createTable(eq(USER_AUTHENTICATION_TABLE), capture(capturedData));
    expectLastCall().once();
    dbAccessor.addPKConstraint(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_PRIMARY_KEY, USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN);
    expectLastCall().once();
    dbAccessor.addFKConstraint(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_USER_AUTHENTICATION_USERS_FOREIGN_KEY, USER_AUTHENTICATION_USER_ID_COLUMN, USERS_TABLE, USERS_USER_ID_COLUMN, false);
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + USER_AUTHENTICATION_TABLE))).andReturn(1).once();
  }

  private void validateConvertingUserCreationTime(Capture<DBAccessor.DBColumnInfo> temporaryColumnCreationCapture, Capture<DBAccessor.DBColumnInfo> temporaryColumnRenameCapture) {
    Assert.assertTrue(temporaryColumnCreationCapture.hasCaptured());
    Assert.assertTrue(temporaryColumnRenameCapture.hasCaptured());
    assertEquals(new DBAccessor.DBColumnInfo(USERS_CREATE_TIME_COLUMN, Long.class, null, null, false), temporaryColumnRenameCapture.getValue());
  }

  private void validateCreateUserAuthenticationTable(Capture<List<DBAccessor.DBColumnInfo>> capturedData) {
    Assert.assertTrue(capturedData.hasCaptured());
    List<List<DBAccessor.DBColumnInfo>> capturedValues = capturedData.getValues();
    Assert.assertEquals(2, capturedValues.size());
    for (List<DBAccessor.DBColumnInfo> capturedValue : capturedValues) {
      validateColumns(capturedValue,
          Arrays.asList(
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_USER_ID_COLUMN, Integer.class, null, null, false),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN, String.class, 50, null, false),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN, String.class, 2048, null, true),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_CREATE_TIME_COLUMN, Long.class, null, null, true),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_UPDATE_TIME_COLUMN, Long.class, null, null, true)
          )
      );
    }
  }

  private void prepareUpdateGroupMembershipRecords(DBAccessor dbAccessor, Capture<List<DBAccessor.DBColumnInfo>> capturedData)
      throws SQLException {
    String temporaryTableName = MEMBERS_TABLE + "_tmp";

    dbAccessor.dropTable(eq(temporaryTableName));
    expectLastCall().times(2);
    dbAccessor.createTable(eq(temporaryTableName), capture(capturedData));
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + temporaryTableName))).andReturn(1).once();

    dbAccessor.truncateTable(MEMBERS_TABLE);
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + MEMBERS_TABLE))).andReturn(1).once();
  }

  private void validateUpdateGroupMembershipRecords(Capture<List<DBAccessor.DBColumnInfo>> capturedData) {
    Assert.assertTrue(capturedData.hasCaptured());
    List<List<DBAccessor.DBColumnInfo>> capturedValues = capturedData.getValues();
    Assert.assertEquals(1, capturedValues.size());
    for (List<DBAccessor.DBColumnInfo> capturedValue : capturedValues) {
      validateColumns(capturedValue,
          Arrays.asList(
              new DBAccessor.DBColumnInfo(MEMBERS_MEMBER_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(MEMBERS_USER_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(MEMBERS_GROUP_ID_COLUMN, Long.class, null, null, false)
          )
      );
    }
  }

  private void prepareUpdateAdminPrivilegeRecords(DBAccessor dbAccessor, Capture<List<DBAccessor.DBColumnInfo>> capturedData)
      throws SQLException {
    String temporaryTableName = ADMINPRIVILEGE_TABLE + "_tmp";

    dbAccessor.dropTable(eq(temporaryTableName));
    expectLastCall().times(2);
    dbAccessor.createTable(eq(temporaryTableName), capture(capturedData));
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + temporaryTableName))).andReturn(1).once();

    dbAccessor.truncateTable(ADMINPRIVILEGE_TABLE);
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + ADMINPRIVILEGE_TABLE))).andReturn(1).once();
  }

  private void validateUpdateAdminPrivilegeRecords(Capture<List<DBAccessor.DBColumnInfo>> capturedData) {
    Assert.assertTrue(capturedData.hasCaptured());
    List<List<DBAccessor.DBColumnInfo>> capturedValues = capturedData.getValues();
    Assert.assertEquals(1, capturedValues.size());
    for (List<DBAccessor.DBColumnInfo> capturedValue : capturedValues) {
      validateColumns(capturedValue,
          Arrays.asList(
              new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PERMISSION_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_RESOURCE_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN, Long.class, null, null, false)
          )
      );
    }
  }

  private void prepareUpdateUsersTable(DBAccessor dbAccessor, Capture<DBAccessor.DBColumnInfo> updateUserTableCaptures, Capture<DBAccessor.DBColumnInfo> alterUserTableCaptures)
      throws SQLException {

    expect(dbAccessor.executeUpdate(startsWith("delete from " + USERS_TABLE))).andReturn(1).once();

    dbAccessor.dropUniqueConstraint(USERS_TABLE, UNIQUE_USERS_0_INDEX);
    expectLastCall().once();
    dbAccessor.dropColumn(USERS_TABLE, USERS_USER_TYPE_COLUMN);
    expectLastCall().once();
    dbAccessor.dropColumn(USERS_TABLE, USERS_LDAP_USER_COLUMN);
    expectLastCall().once();
    dbAccessor.dropColumn(USERS_TABLE, USERS_USER_PASSWORD_COLUMN);
    expectLastCall().once();

    dbAccessor.addColumn(eq(USERS_TABLE), capture(updateUserTableCaptures));
    expectLastCall().atLeastOnce();

    expect(dbAccessor.executeUpdate(startsWith("update " + USERS_TABLE))).andReturn(1).once();


    dbAccessor.alterColumn(eq(USERS_TABLE), capture(alterUserTableCaptures));
    expectLastCall().atLeastOnce();

    dbAccessor.addUniqueConstraint(USERS_TABLE, UNIQUE_USERS_0_INDEX, USERS_USER_NAME_COLUMN);
    expectLastCall().once();
  }

  private void prepareUpdateRepoTables(DBAccessor dbAccessor,
                                       Capture<List<DBAccessor.DBColumnInfo>> addRepoOsTableCapturedColumns,
                                       Capture<List<DBAccessor.DBColumnInfo>> addRepoDefinitionTableCapturedColumns,
                                       Capture<List<DBAccessor.DBColumnInfo>> addRepoTagsTableCapturedColumns,
                                       Capture<List<DBAccessor.DBColumnInfo>> addRepoApplicableServicesTableCapturedColumns,
                                       Capture<String[]> insertRepoOsTableRowColumns,
                                       Capture<String[]> insertRepoOsTableRowValues,
                                       Capture<String[]> insertAmbariSequencesRowColumns,
                                       Capture<String[]> insertAmbariSequencesRowValues)
      throws SQLException {

    dbAccessor.createTable(eq(REPO_OS_TABLE), capture(addRepoOsTableCapturedColumns));
    expectLastCall().once();
    dbAccessor.addPKConstraint(REPO_OS_TABLE, REPO_OS_PRIMARY_KEY, REPO_OS_ID_COLUMN);
    expectLastCall().once();
    dbAccessor.addFKConstraint(REPO_OS_TABLE, REPO_OS_FOREIGN_KEY, REPO_OS_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_REPO_VERSION_ID_COLUMN, false);
    expectLastCall().once();

    dbAccessor.createTable(eq(REPO_DEFINITION_TABLE), capture(addRepoDefinitionTableCapturedColumns));
    expectLastCall().once();
    dbAccessor.addPKConstraint(REPO_DEFINITION_TABLE, REPO_DEFINITION_PRIMARY_KEY, REPO_DEFINITION_ID_COLUMN);
    expectLastCall().once();
    dbAccessor.addFKConstraint(REPO_DEFINITION_TABLE, REPO_DEFINITION_FOREIGN_KEY, REPO_DEFINITION_REPO_OS_ID_COLUMN, REPO_OS_TABLE, REPO_OS_ID_COLUMN, false);
    expectLastCall().once();

    dbAccessor.createTable(eq(REPO_TAGS_TABLE), capture(addRepoTagsTableCapturedColumns));
    expectLastCall().once();
    dbAccessor.addFKConstraint(REPO_TAGS_TABLE, REPO_TAGS_FOREIGN_KEY, REPO_TAGS_REPO_DEFINITION_ID_COLUMN, REPO_DEFINITION_TABLE, REPO_DEFINITION_ID_COLUMN, false);
    expectLastCall().once();

    dbAccessor.createTable(eq(REPO_APPLICABLE_SERVICES_TABLE), capture(addRepoApplicableServicesTableCapturedColumns));
    expectLastCall().once();
    dbAccessor.addFKConstraint(REPO_APPLICABLE_SERVICES_TABLE, REPO_APPLICABLE_SERVICES_FOREIGN_KEY, REPO_APPLICABLE_SERVICES_REPO_DEFINITION_ID_COLUMN, REPO_DEFINITION_TABLE, REPO_DEFINITION_ID_COLUMN, false);
    expectLastCall().once();

    expect(dbAccessor.tableHasColumn(eq(REPO_VERSION_TABLE), eq(REPO_VERSION_REPOSITORIES_COLUMN))).andReturn(true).once();
    final Map<Long, String> repoMap = new HashMap<>();
    repoMap.put(1L, getSampleRepositoryData());
    expect(dbAccessor.getKeyToStringColumnMap(eq(REPO_VERSION_TABLE), eq(REPO_VERSION_REPO_VERSION_ID_COLUMN), eq(REPO_VERSION_REPOSITORIES_COLUMN), eq(null), eq(null), eq(true))).andReturn(repoMap).once();
    expect(dbAccessor.insertRowIfMissing(eq(REPO_OS_TABLE), capture(insertRepoOsTableRowColumns), capture(insertRepoOsTableRowValues), eq(false))).andReturn(true).once();

    expect(dbAccessor.insertRowIfMissing(eq(AMBARI_SEQUENCES_TABLE),
        capture(insertAmbariSequencesRowColumns),
        capture(insertAmbariSequencesRowValues),
        eq(false))).andReturn(true).once();
    expect(dbAccessor.insertRowIfMissing(eq(AMBARI_SEQUENCES_TABLE),
        capture(insertAmbariSequencesRowColumns),
        capture(insertAmbariSequencesRowValues),
        eq(false))).andReturn(true).once();

    dbAccessor.dropColumn(eq(REPO_VERSION_TABLE), eq(REPO_VERSION_REPOSITORIES_COLUMN));
    expectLastCall().once();
  }

  /**
   * @return sample JSON data from repo_version data (no repositories defined) where 'OperatingSystems/ambari_managed_repositories' is missing
   */
  private String getSampleRepositoryData() {
    return "[{\"repositories\":[],\"OperatingSystems/os_type\":\"redhat7\"}]";
  }

  private void validateUpdateUsersTable(Capture<DBAccessor.DBColumnInfo> updateUserTableCaptures, Capture<DBAccessor.DBColumnInfo> alterUserTableCaptures) {
    Assert.assertTrue(updateUserTableCaptures.hasCaptured());
    validateColumns(updateUserTableCaptures.getValues(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(USERS_CONSECUTIVE_FAILURES_COLUMN, Integer.class, null, 0, false),
            new DBAccessor.DBColumnInfo(USERS_DISPLAY_NAME_COLUMN, String.class, 255, null, true),
            new DBAccessor.DBColumnInfo(USERS_LOCAL_USERNAME_COLUMN, String.class, 255, null, true),
            new DBAccessor.DBColumnInfo(USERS_VERSION_COLUMN, Long.class, null, 0, false)
        )
    );

    Assert.assertTrue(alterUserTableCaptures.hasCaptured());
    validateColumns(alterUserTableCaptures.getValues(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(USERS_DISPLAY_NAME_COLUMN, String.class, 255, null, false),
            new DBAccessor.DBColumnInfo(USERS_LOCAL_USERNAME_COLUMN, String.class, 255, null, false)
        )
    );
  }

  private void validateCreateRepoOsTable(Capture<List<DBAccessor.DBColumnInfo>> addRepoOsTableCapturedColumns,
                                         Capture<List<DBAccessor.DBColumnInfo>> addRepoDefinitionTableCapturedColumns,
                                         Capture<List<DBAccessor.DBColumnInfo>> addRepoTagsTableCapturedColumns,
                                         Capture<String[]> insertRepoOsTableRowColumns, Capture<String[]> insertRepoOsTableRowValues,
                                         Capture<String[]> insertAmbariSequencesRowColumns, Capture<String[]> insertAmbariSequencesRowValues) {
    Assert.assertTrue(addRepoOsTableCapturedColumns.hasCaptured());
    validateColumns(addRepoOsTableCapturedColumns.getValue(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(REPO_OS_ID_COLUMN, Long.class, null, null, false),
            new DBAccessor.DBColumnInfo(REPO_OS_REPO_VERSION_ID_COLUMN, Long.class, null, null, false),
            new DBAccessor.DBColumnInfo(REPO_OS_FAMILY_COLUMN, String.class, 255, null, false),
            new DBAccessor.DBColumnInfo(REPO_OS_AMBARI_MANAGED_COLUMN, Integer.class, null, 1, true)
        )
    );

    Assert.assertTrue(addRepoDefinitionTableCapturedColumns.hasCaptured());
    validateColumns(addRepoDefinitionTableCapturedColumns.getValue(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(REPO_DEFINITION_ID_COLUMN, Long.class, null, null, false),
            new DBAccessor.DBColumnInfo(REPO_DEFINITION_REPO_OS_ID_COLUMN, Long.class, null, null, false),
            new DBAccessor.DBColumnInfo(REPO_DEFINITION_REPO_NAME_COLUMN, String.class, 255, null, false),
            new DBAccessor.DBColumnInfo(REPO_DEFINITION_REPO_ID_COLUMN, String.class, 255, null, false),
            new DBAccessor.DBColumnInfo(REPO_DEFINITION_BASE_URL_COLUMN, String.class, 2048, null, true),
            new DBAccessor.DBColumnInfo(REPO_DEFINITION_DISTRIBUTION_COLUMN, String.class, 2048, null, true),
            new DBAccessor.DBColumnInfo(REPO_DEFINITION_COMPONENTS_COLUMN, String.class, 2048, null, true),
            new DBAccessor.DBColumnInfo(REPO_DEFINITION_UNIQUE_REPO_COLUMN, Integer.class, 1, 1, true),
            new DBAccessor.DBColumnInfo(REPO_DEFINITION_MIRRORS_COLUMN, String.class, 2048, null, true)
        )
    );

    Assert.assertTrue(addRepoTagsTableCapturedColumns.hasCaptured());
    validateColumns(addRepoTagsTableCapturedColumns.getValue(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(REPO_TAGS_REPO_DEFINITION_ID_COLUMN, Long.class, null, null, false),
            new DBAccessor.DBColumnInfo(REPO_TAGS_TAG_COLUMN, String.class, 255, null, false)
        )
    );

    List<String[]> values;

    Assert.assertTrue(insertRepoOsTableRowColumns.hasCaptured());
    values = insertRepoOsTableRowColumns.getValues();
    Assert.assertEquals(1, values.size());
    Assert.assertArrayEquals(new String[] {REPO_OS_ID_COLUMN, REPO_OS_REPO_VERSION_ID_COLUMN, REPO_OS_AMBARI_MANAGED_COLUMN, REPO_OS_FAMILY_COLUMN}, values.get(0));

    Assert.assertTrue(insertRepoOsTableRowValues.hasCaptured());
    values = insertRepoOsTableRowValues.getValues();
    Assert.assertEquals(1, values.size());
    Assert.assertArrayEquals(new String[] {"1", "1", "1", "'redhat7'"}, values.get(0));

    Assert.assertTrue(insertAmbariSequencesRowColumns.hasCaptured());
    values = insertAmbariSequencesRowColumns.getValues();
    Assert.assertEquals(2, values.size());
    Assert.assertArrayEquals(new String[]{AMBARI_SEQUENCES_SEQUENCE_NAME_COLUMN, AMBARI_SEQUENCES_SEQUENCE_VALUE_COLUMN}, values.get(0));
    Assert.assertArrayEquals(new String[]{AMBARI_SEQUENCES_SEQUENCE_NAME_COLUMN, AMBARI_SEQUENCES_SEQUENCE_VALUE_COLUMN}, values.get(1));

    Assert.assertTrue(insertAmbariSequencesRowValues.hasCaptured());
    values = insertAmbariSequencesRowValues.getValues();
    Assert.assertEquals(2, values.size());
    Assert.assertArrayEquals(new String[]{"'repo_os_id_seq'", "2"}, values.get(0));
    Assert.assertArrayEquals(new String[]{"'repo_definition_id_seq'", "1"}, values.get(1));


  }

  private void validateColumns(List<DBAccessor.DBColumnInfo> capturedColumns, List<DBAccessor.DBColumnInfo> expectedColumns) {
    Assert.assertEquals(expectedColumns.size(), capturedColumns.size());

    // copy these so we can alter them...
    expectedColumns = new ArrayList<>(expectedColumns);
    capturedColumns = new ArrayList<>(capturedColumns);

    Iterator<DBAccessor.DBColumnInfo> capturedColumnIterator = capturedColumns.iterator();
    while (capturedColumnIterator.hasNext()) {
      DBAccessor.DBColumnInfo capturedColumnInfo = capturedColumnIterator.next();

      Iterator<DBAccessor.DBColumnInfo> expectedColumnIterator = expectedColumns.iterator();
      while (expectedColumnIterator.hasNext()) {
        DBAccessor.DBColumnInfo expectedColumnInfo = expectedColumnIterator.next();

        if (expectedColumnInfo.equals(capturedColumnInfo)) {
          expectedColumnIterator.remove();
          capturedColumnIterator.remove();
          break;
        }
      }
    }

    assertTrue("Not all captured columns were expected", capturedColumns.isEmpty());
    assertTrue("Not all expected columns were captured", expectedColumns.isEmpty());
  }


  @Test
  public void testLogSearchUpdateConfigs() throws Exception {
    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(ImmutableMap.of("normal", cluster)).once();

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .createNiceMock();
    ConfigHelper configHelper = createMockBuilder(ConfigHelper.class)
        .addMockedMethod("createConfigType", Cluster.class, StackId.class, AmbariManagementController.class,
            String.class, Map.class, String.class, String.class)
        .createMock();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(injector2.getInstance(ConfigHelper.class)).andReturn(configHelper).anyTimes();
    expect(injector2.getInstance(DBAccessor.class)).andReturn(dbAccessor).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    configHelper.createConfigType(anyObject(Cluster.class), anyObject(StackId.class), eq(controller),
        eq("logsearch-common-properties"), eq(Collections.emptyMap()), eq("ambari-upgrade"),
        eq("Updated logsearch-common-properties during Ambari Upgrade from 2.6.0 to 3.0.0"));
    expectLastCall().once();

    Map<String, String> oldLogSearchProperties = ImmutableMap.of(
        "logsearch.logfeeder.include.default.level", "FATAL,ERROR,WARN"
    );

    Map<String, String> expectedLogFeederProperties = ImmutableMap.of(
        "logfeeder.include.default.level", "FATAL,ERROR,WARN"
    );

    Config logFeederPropertiesConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logfeeder-properties")).andReturn(logFeederPropertiesConf).times(2);
    expect(logFeederPropertiesConf.getProperties()).andReturn(Collections.emptyMap()).once();
    Capture<Map<String, String>> logFeederPropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), eq("logfeeder-properties"), capture(logFeederPropertiesCapture),
        anyString(), EasyMock.anyObject())).andReturn(config).once();

    Config logSearchPropertiesConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-properties")).andReturn(logSearchPropertiesConf).times(2);
    expect(logSearchPropertiesConf.getProperties()).andReturn(oldLogSearchProperties).times(2);
    Capture<Map<String, String>> logSearchPropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), eq("logsearch-properties"), capture(logSearchPropertiesCapture),
        anyString(), EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogFeederLog4j = ImmutableMap.of(
        "content", "<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">");

    Map<String, String> expectedLogFeederLog4j = ImmutableMap.of(
        "content", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");

    Config logFeederLog4jConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logfeeder-log4j")).andReturn(logFeederLog4jConf).atLeastOnce();
    expect(logFeederLog4jConf.getProperties()).andReturn(oldLogFeederLog4j).anyTimes();
    Capture<Map<String, String>> logFeederLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logFeederLog4jCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogSearchLog4j = ImmutableMap.of(
        "content", "<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">");

    Map<String, String> expectedLogSearchLog4j = ImmutableMap.of(
        "content", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");

    Config logSearchLog4jConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-log4j")).andReturn(logSearchLog4jConf).atLeastOnce();
    expect(logSearchLog4jConf.getProperties()).andReturn(oldLogSearchLog4j).anyTimes();
    Capture<Map<String, String>> logSearchLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logSearchLog4jCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogSearchServiceLogsConf = ImmutableMap.of(
        "content", "<before/><requestHandler name=\"/admin/\"   class=\"solr.admin.AdminHandlers\" /><after/>");

    Map<String, String> expectedLogSearchServiceLogsConf = ImmutableMap.of(
        "content", "<before/><after/>");

    Config logSearchServiceLogsConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-service_logs-solrconfig")).andReturn(logSearchServiceLogsConf).atLeastOnce();
    expect(logSearchServiceLogsConf.getProperties()).andReturn(oldLogSearchServiceLogsConf).anyTimes();
    Capture<Map<String, String>> logSearchServiceLogsConfCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logSearchServiceLogsConfCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogSearchAuditLogsConf = ImmutableMap.of(
        "content", "<before/><requestHandler name=\"/admin/\"   class=\"solr.admin.AdminHandlers\" /><after/>");

    Map<String, String> expectedLogSearchAuditLogsConf = ImmutableMap.of(
        "content", "<before/><after/>");

    Config logSearchAuditLogsConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-audit_logs-solrconfig")).andReturn(logSearchAuditLogsConf).atLeastOnce();
    expect(logSearchAuditLogsConf.getProperties()).andReturn(oldLogSearchAuditLogsConf).anyTimes();
    Capture<Map<String, String>> logSearchAuditLogsConfCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logSearchAuditLogsConfCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogFeederOutputConf = ImmutableMap.of(
        "content",
        "      \"zk_connect_string\":\"{{logsearch_solr_zk_quorum}}{{logsearch_solr_zk_znode}}\",\n" +
            "      \"collection\":\"{{logsearch_solr_collection_service_logs}}\",\n" +
            "      \"number_of_shards\": \"{{logsearch_collection_service_logs_numshards}}\",\n" +
            "      \"splits_interval_mins\": \"{{logsearch_service_logs_split_interval_mins}}\",\n" +
            "\n" +
            "      \"zk_connect_string\":\"{{logsearch_solr_zk_quorum}}{{logsearch_solr_zk_znode}}\",\n" +
            "      \"collection\":\"{{logsearch_solr_collection_audit_logs}}\",\n" +
            "      \"number_of_shards\": \"{{logsearch_collection_audit_logs_numshards}}\",\n" +
            "      \"splits_interval_mins\": \"{{logsearch_audit_logs_split_interval_mins}}\",\n"
    );

    Map<String, String> expectedLogFeederOutputConf = ImmutableMap.of(
        "content",
        "      \"zk_connect_string\":\"{{logsearch_solr_zk_quorum}}{{logsearch_solr_zk_znode}}\",\n" +
            "      \"type\": \"service\",\n" +
            "\n" +
            "      \"zk_connect_string\":\"{{logsearch_solr_zk_quorum}}{{logsearch_solr_zk_znode}}\",\n" +
            "      \"type\": \"audit\",\n"
    );

    Config logFeederOutputConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logfeeder-output-config")).andReturn(logFeederOutputConf).atLeastOnce();
    expect(logFeederOutputConf.getProperties()).andReturn(oldLogFeederOutputConf).anyTimes();
    Capture<Map<String, String>> logFeederOutputConfCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logFeederOutputConfCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    String serviceConfigMapping = "serviceconfigmapping";
    String clusterConfig = "clusterconfig";
    dbAccessor.executeQuery(startsWith("DELETE FROM "+ serviceConfigMapping));
    expectLastCall().once();
    dbAccessor.executeQuery(startsWith("DELETE FROM "+ clusterConfig));
    expectLastCall().once();

    replay(clusters, cluster, dbAccessor);
    replay(controller, injector2);
    replay(logSearchPropertiesConf, logFeederPropertiesConf);
    replay(logFeederLog4jConf, logSearchLog4jConf);
    replay(logSearchServiceLogsConf, logSearchAuditLogsConf);
    replay(logFeederOutputConf);
    new UpgradeCatalog270(injector2).updateLogSearchConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> newLogFeederProperties = logFeederPropertiesCapture.getValue();
    assertTrue(Maps.difference(expectedLogFeederProperties, newLogFeederProperties).areEqual());

    Map<String, String> newLogSearchProperties = logSearchPropertiesCapture.getValue();
    assertTrue(Maps.difference(Collections.emptyMap(), newLogSearchProperties).areEqual());

    Map<String, String> updatedLogFeederLog4j = logFeederLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedLogFeederLog4j, updatedLogFeederLog4j).areEqual());

    Map<String, String> updatedLogSearchLog4j = logSearchLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedLogSearchLog4j, updatedLogSearchLog4j).areEqual());

    Map<String, String> updatedServiceLogsConf = logSearchServiceLogsConfCapture.getValue();
    assertTrue(Maps.difference(expectedLogSearchServiceLogsConf, updatedServiceLogsConf).areEqual());

    Map<String, String> updatedAuditLogsConf = logSearchAuditLogsConfCapture.getValue();
    assertTrue(Maps.difference(expectedLogSearchAuditLogsConf, updatedAuditLogsConf).areEqual());

    Map<String, String> updatedLogFeederOutputConf = logFeederOutputConfCapture.getValue();
    assertTrue(Maps.difference(expectedLogFeederOutputConf, updatedLogFeederOutputConf).areEqual());
  }

  @Test
  public void testUpdateKerberosConfigurations() throws AmbariException, NoSuchFieldException, IllegalAccessException {
    StackId stackId = new StackId("HDP", "2.6.0.0");

    Map<String, Cluster> clusterMap = new HashMap<>();

    Map<String, String> propertiesWithGroup = new HashMap<>();
    propertiesWithGroup.put("group", "ambari_managed_identities");
    propertiesWithGroup.put("kdc_host", "host1.example.com");
    propertiesWithGroup.put("realm", "example.com");

    Config newConfig = createMock(Config.class);
    expect(newConfig.getTag()).andReturn("version2").atLeastOnce();
    expect(newConfig.getType()).andReturn("kerberos-env").atLeastOnce();

    ServiceConfigVersionResponse response = createMock(ServiceConfigVersionResponse.class);

    Config configWithGroup = createMock(Config.class);
    expect(configWithGroup.getProperties()).andReturn(propertiesWithGroup).atLeastOnce();
    expect(configWithGroup.getPropertiesAttributes()).andReturn(Collections.emptyMap()).atLeastOnce();
    expect(configWithGroup.getTag()).andReturn("version1").atLeastOnce();

    Cluster cluster1 = createMock(Cluster.class);
    expect(cluster1.getDesiredConfigByType("kerberos-env")).andReturn(configWithGroup).atLeastOnce();
    expect(cluster1.getConfigsByType("kerberos-env")).andReturn(Collections.singletonMap("v1", configWithGroup)).atLeastOnce();
    expect(cluster1.getServiceByConfigType("kerberos-env")).andReturn("KERBEROS").atLeastOnce();
    expect(cluster1.getClusterName()).andReturn("c1").atLeastOnce();
    expect(cluster1.getDesiredStackVersion()).andReturn(stackId).atLeastOnce();
    expect(cluster1.getConfig(eq("kerberos-env"), anyString())).andReturn(newConfig).atLeastOnce();
    expect(cluster1.addDesiredConfig("ambari-upgrade", Collections.singleton(newConfig), "Updated kerberos-env during Ambari Upgrade from 2.6.2 to 2.7.0.")).andReturn(response).once();

    Map<String, String> propertiesWithoutGroup = new HashMap<>();
    propertiesWithoutGroup.put("kdc_host", "host2.example.com");
    propertiesWithoutGroup.put("realm", "example.com");

    Config configWithoutGroup = createMock(Config.class);
    expect(configWithoutGroup.getProperties()).andReturn(propertiesWithoutGroup).atLeastOnce();

    Cluster cluster2 = createMock(Cluster.class);
    expect(cluster2.getDesiredConfigByType("kerberos-env")).andReturn(configWithoutGroup).atLeastOnce();

    Cluster cluster3 = createMock(Cluster.class);
    expect(cluster3.getDesiredConfigByType("kerberos-env")).andReturn(null).atLeastOnce();

    clusterMap.put("c1", cluster1);
    clusterMap.put("c2", cluster2);
    clusterMap.put("c3", cluster3);

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getClusters()).andReturn(clusterMap).anyTimes();

    Capture<Map<String, String>> capturedProperties = newCapture();

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .addMockedMethod("getClusterMetadataOnConfigsUpdate", Cluster.class)
        .createMock();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(eq(cluster1), eq(stackId), eq("kerberos-env"), capture(capturedProperties), anyString(), anyObject(Map.class))).andReturn(newConfig).once();


    Injector injector = createNiceMock(Injector.class);
    ConfigHelper configHelper = createStrictMock(ConfigHelper.class);
    expect(injector.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(injector.getInstance(MetadataHolder.class)).andReturn(createNiceMock(MetadataHolder.class)).anyTimes();
    expect(injector.getInstance(AgentConfigsHolder.class)).andReturn(createNiceMock(AgentConfigsHolder.class)).anyTimes();
    expect(injector.getInstance(AmbariServer.class)).andReturn(createNiceMock(AmbariServer.class)).anyTimes();
    expect(injector.getInstance(ConfigHelper.class)).andReturn(configHelper).anyTimes();
    KerberosHelper kerberosHelperMock = createNiceMock(KerberosHelper.class);
    expect(kerberosHelperMock.createTemporaryDirectory()).andReturn(new File("/invalid/file/path")).times(2);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(kerberosHelperMock).anyTimes();

    configHelper.updateAgentConfigs(anyObject(Set.class));
    expectLastCall();

    replay(controller, clusters, cluster1, cluster2, configWithGroup, configWithoutGroup, newConfig, response, injector, kerberosHelperMock, configHelper);

    Field field = AbstractUpgradeCatalog.class.getDeclaredField("configuration");

    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class).addMockedMethod("getPrepareIdentityServerAction").addMockedMethod("executeInTransaction").createMock();
    PrepareKerberosIdentitiesServerAction mockAction = createNiceMock(PrepareKerberosIdentitiesServerAction.class);
    expect(upgradeCatalog270.getPrepareIdentityServerAction()).andReturn(mockAction).times(2);
    upgradeCatalog270.executeInTransaction(anyObject());
    expectLastCall().times(2);
    upgradeCatalog270.injector = injector;

    replay(upgradeCatalog270);

    field.set(upgradeCatalog270, createNiceMock(Configuration.class));
    upgradeCatalog270.updateKerberosConfigurations();

    verify(controller, clusters, cluster1, cluster2, configWithGroup, configWithoutGroup, newConfig, response, injector, upgradeCatalog270, configHelper);


    Assert.assertEquals(1, capturedProperties.getValues().size());

    Map<String, String> properties = capturedProperties.getValue();
    Assert.assertEquals(3, properties.size());
    Assert.assertEquals("ambari_managed_identities", properties.get("ipa_user_group"));
    Assert.assertEquals("host1.example.com", properties.get("kdc_host"));
    Assert.assertEquals("example.com", properties.get("realm"));

    Assert.assertEquals(3, propertiesWithGroup.size());
    Assert.assertEquals("ambari_managed_identities", propertiesWithGroup.get("group"));
    Assert.assertEquals("host1.example.com", propertiesWithGroup.get("kdc_host"));
    Assert.assertEquals("example.com", propertiesWithGroup.get("realm"));
  }

  @Test
  public void shouldSaveLdapConfigurationIfPropertyIsSetInAmbariProperties() throws Exception {
    final Module module = getTestGuiceModule();

    expect(entityManager.find(anyObject(), anyObject())).andReturn(null).anyTimes();
    final Map<String, String> properties = new HashMap<>();
    properties.put(AmbariServerConfigurationKey.LDAP_ENABLED.key(), "true");
    properties.put(AmbariServerConfigurationKey.AMBARI_MANAGES_LDAP_CONFIGURATION.key(), "true");
    properties.put(AmbariServerConfigurationKey.LDAP_ENABLED_SERVICES.key(), "AMBARI");

    expect(ambariConfigurationDao.reconcileCategory(AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName(), properties, false)).andReturn(true).once();
    replay(entityManager, ambariConfigurationDao);

    final Injector injector = Guice.createInjector(module);
    injector.getInstance(Configuration.class).setProperty("ambari.ldap.isConfigured", "true");
    final UpgradeCatalog270 upgradeCatalog270 = new UpgradeCatalog270(injector);
    upgradeCatalog270.moveAmbariPropertiesToAmbariConfiguration();
    verify(entityManager, ambariConfigurationDao);
  }

  @Test
  public void shouldNotSaveLdapConfigurationIfPropertyIsNotSetInAmbariProperties() throws Exception {
    final Module module = getTestGuiceModule();
    expect(entityManager.find(anyObject(), anyObject())).andReturn(null).anyTimes();
    final Map<String, String> properties = new HashMap<>();
    properties.put(AmbariServerConfigurationKey.LDAP_ENABLED.key(), "true");
    expect(ambariConfigurationDao.reconcileCategory(AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName(), properties, false)).andReturn(true).once();
    replay(entityManager, ambariConfigurationDao);

    final Injector injector = Guice.createInjector(module);
    final UpgradeCatalog270 upgradeCatalog270 = new UpgradeCatalog270(injector);
    upgradeCatalog270.moveAmbariPropertiesToAmbariConfiguration();

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Expectation failure on verify");
    verify(entityManager, ambariConfigurationDao);
  }

  @Test
  public void testupdateKerberosDescriptorArtifact() throws Exception {
    String kerberosDescriptorJson = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("org/apache/ambari/server/upgrade/kerberos_descriptor.json"), "UTF-8");

    //there is HIVE -> WEBHCAT_SERVER -> configurations -> core-site -> hadoop.proxyuser.HTTP.hosts
    assertTrue(kerberosDescriptorJson.contains("${clusterHostInfo/webhcat_server_host|append(core-site/hadoop.proxyuser.HTTP.hosts, \\\\\\\\,, true)}"));
    assertTrue(kerberosDescriptorJson.contains("${clusterHostInfo/rm_host}"));

    ArtifactEntity artifactEntity = new ArtifactEntity();
    artifactEntity.setArtifactName("kerberos_descriptor");
    artifactEntity.setArtifactData(GSON.<Map<String, Object>>fromJson(kerberosDescriptorJson, Map.class));

    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class).createMock();

    expect(artifactDAO.merge(artifactEntity)).andReturn(artifactEntity);

    replay(upgradeCatalog270);

    upgradeCatalog270.updateKerberosDescriptorArtifact(artifactDAO, artifactEntity);

    final String newKerberosDescriptorJson = GSON.toJson(artifactEntity.getArtifactData());

    int oldCount = substringCount(kerberosDescriptorJson, AMBARI_INFRA_OLD_NAME);
    int newCount = substringCount(newKerberosDescriptorJson, AMBARI_INFRA_NEW_NAME);
    assertThat(newCount, is(oldCount));

    assertTrue(newKerberosDescriptorJson.contains("${clusterHostInfo/webhcat_server_hosts|append(core-site/hadoop.proxyuser.HTTP.hosts, \\\\,, true)}"));
    assertTrue(newKerberosDescriptorJson.contains("${clusterHostInfo/resourcemanager_hosts}"));

    verify(upgradeCatalog270);
  }

  private int substringCount(String source, String substring) {
    int count = 0;
    int i = -1;
    while ((i = source.indexOf(substring, i + 1)) != -1) {
      ++count;
    }
    return count;
  }

  @Test
  public void testupdateLuceneMatchVersion() throws Exception {
    String solrConfigXml = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("org/apache/ambari/server/upgrade/solrconfig-v500.xml.j2"), "UTF-8");

    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class)
            .createMock();

    replay(upgradeCatalog270);

    String updated = upgradeCatalog270.updateLuceneMatchVersion(solrConfigXml,"7.3.1");

    assertThat(updated.contains("<luceneMatchVersion>7.3.1</luceneMatchVersion>"), is(true));
    assertThat(updated.contains("<luceneMatchVersion>5.0.0</luceneMatchVersion>"), is(false));
    verify(upgradeCatalog270);
  }

  @Test
  public void testupdateMergeFactor() throws Exception {
    String solrConfigXml = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("org/apache/ambari/server/upgrade/solrconfig-v500.xml.j2"), "UTF-8");

    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class)
            .createMock();

    replay(upgradeCatalog270);

    String updated = upgradeCatalog270.updateMergeFactor(solrConfigXml, "logsearch_service_logs_merge_factor");

    assertThat(updated.contains("<int name=\"maxMergeAtOnce\">{{logsearch_service_logs_merge_factor}}</int>"), is(true));
    assertThat(updated.contains("<int name=\"segmentsPerTier\">{{logsearch_service_logs_merge_factor}}</int>"), is(true));
    assertThat(updated.contains("<mergeFactor>{{logsearch_service_logs_merge_factor}}</mergeFactor>"), is(false));
    verify(upgradeCatalog270);
  }

  @Test
  public void testupdateInfraSolrEnv() {
    String solrConfigXml = "#SOLR_HOST=\"192.168.1.1\"\n" +
            "SOLR_HOST=\"192.168.1.1\"\n" +
            "SOLR_KERB_NAME_RULES=\"{{infra_solr_kerberos_name_rules}}\"\n" +
            "SOLR_AUTHENTICATION_CLIENT_CONFIGURER=\"org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer\"";

    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class)
            .createMock();

    replay(upgradeCatalog270);

    String updated = upgradeCatalog270.updateInfraSolrEnv(solrConfigXml);

    assertThat(updated, is("SOLR_HOST=`hostname -f`\nSOLR_HOST=`hostname -f`\n\nSOLR_AUTH_TYPE=\"kerberos\""));
    verify(upgradeCatalog270);
  }

  @Test
  public void testRemoveAdminHandlers() {
    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class)
            .createMock();

    replay(upgradeCatalog270);

    String updated = upgradeCatalog270.removeAdminHandlers("<requestHandler name=\"/admin/\"\n" +
            "                  class=\"solr.admin.AdminHandlers\"/>");

    assertThat(updated, is(""));
    verify(upgradeCatalog270);
  }

  @Test
  public void testUpdateAmsConfigs() throws Exception {

    Map<String, String> oldProperties = new HashMap<String, String>() {
      {
        put("timeline.metrics.service.default.result.limit", "15840");
        put("timeline.container-metrics.ttl", "2592000");
        put("timeline.metrics.cluster.aggregate.splitpoints", "cpu_user,mem_free");
        put("timeline.metrics.host.aggregate.splitpoints", "kafka.metric,nimbus.metric");
        put("timeline.metrics.downsampler.topn.metric.patterns", "dfs.NNTopUserOpCounts.windowMs=60000.op=__%.user=%," +
          "dfs.NNTopUserOpCounts.windowMs=300000.op=__%.user=%,dfs.NNTopUserOpCounts.windowMs=1500000.op=__%.user=%");
      }
    };
    Map<String, String> newProperties = new HashMap<String, String>() {
      {
        put("timeline.metrics.service.default.result.limit", "5760");
        put("timeline.container-metrics.ttl", "1209600");
        put("timeline.metrics.downsampler.topn.metric.patterns", StringUtils.EMPTY);
      }
    };

    Map<String, String> oldAmsHBaseSiteProperties = new HashMap<String, String>() {
      {
        put("hbase.snapshot.enabled", "false");
      }
    };

    Map<String, String> newAmsHBaseSiteProperties = new HashMap<String, String>() {
      {
        put("hbase.snapshot.enabled", "true");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockAmsSite = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-site")).andReturn(mockAmsSite).atLeastOnce();
    expect(mockAmsSite.getProperties()).andReturn(oldProperties).anyTimes();

    Config mockAmsHbaseSite = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ams-hbase-site")).andReturn(mockAmsHbaseSite).atLeastOnce();
    expect(mockAmsHbaseSite.getProperties()).andReturn(oldAmsHBaseSiteProperties).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();

    replay(injector, clusters, mockAmsSite, mockAmsHbaseSite, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
      .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map> propertiesCapture = EasyMock.newCapture(CaptureType.ALL);

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).times(2);

    replay(controller, injector2);
    new UpgradeCatalog270(injector2).updateAmsConfigs();
    easyMockSupport.verifyAll();

    assertEquals(propertiesCapture.getValues().size(), 2);

    Map<String, String> updatedProperties = propertiesCapture.getValues().get(0);
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());

    updatedProperties = propertiesCapture.getValues().get(1);
    assertTrue(Maps.difference(newAmsHBaseSiteProperties, updatedProperties).areEqual());

  }

  @Test
  public void testUpdateAmsConfigsWithNoContainerMetrics() throws Exception {

    Map<String, String> oldProperties = new HashMap<String, String>() {
      {
        put("timeline.metrics.service.default.result.limit", "15840");
        put("timeline.metrics.host.aggregate.splitpoints", "kafka.metric,nimbus.metric");
      }
    };
    Map<String, String> newProperties = new HashMap<String, String>() {
      {
        put("timeline.metrics.service.default.result.limit", "5760");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockAmsSite = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-site")).andReturn(mockAmsSite).atLeastOnce();
    expect(mockAmsSite.getProperties()).andReturn(oldProperties).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();

    replay(injector, clusters, mockAmsSite, cluster);

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
    new UpgradeCatalog270(injector2).updateAmsConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
  }

  @Test
  public void testStormConfigs() throws Exception {

    Map<String, String> stormProperties = new HashMap<String, String>() {
      {
        put("_storm.thrift.nonsecure.transport", "org.apache.storm.security.auth.SimpleTransportPlugin");
        put("_storm.thrift.secure.transport", "org.apache.storm.security.auth.kerberos.KerberosSaslTransportPlugin");
        put("storm.thrift.transport", "{{storm_thrift_transport}}");
        put("storm.zookeeper.port", "2181");
      }
    };
    Map<String, String> newStormProperties = new HashMap<String, String>() {
      {
        put("storm.thrift.transport", "org.apache.storm.security.auth.SimpleTransportPlugin");
        put("storm.zookeeper.port", "2181");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockStormSite = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("storm-site")).andReturn(mockStormSite).atLeastOnce();
    expect(cluster.getSecurityType()).andReturn(SecurityType.NONE).anyTimes();
    expect(mockStormSite.getProperties()).andReturn(stormProperties).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);

    replay(injector, clusters, mockStormSite, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
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
    new UpgradeCatalog270(injector2).updateStormConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newStormProperties, updatedProperties).areEqual());

  }

  @Test
  public void testStormConfigsWithKerberos() throws Exception {

    Map<String, String> stormProperties = new HashMap<String, String>() {
      {
        put("_storm.thrift.nonsecure.transport", "org.apache.storm.security.auth.SimpleTransportPlugin");
        put("_storm.thrift.secure.transport", "org.apache.storm.security.auth.kerberos.KerberosSaslTransportPlugin");
        put("storm.thrift.transport", "{{storm_thrift_transport}}");
        put("storm.zookeeper.port", "2181");
      }
    };
    Map<String, String> newStormProperties = new HashMap<String, String>() {
      {
        put("storm.thrift.transport", "org.apache.storm.security.auth.kerberos.KerberosSaslTransportPlugin");
        put("storm.zookeeper.port", "2181");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockStormSite = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("storm-site")).andReturn(mockStormSite).atLeastOnce();
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(mockStormSite.getProperties()).andReturn(stormProperties).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);

    replay(injector, clusters, mockStormSite, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
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
    new UpgradeCatalog270(injector2).updateStormConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newStormProperties, updatedProperties).areEqual());

  }

  @Test
  public void testClearHadoopMetrics2Content() throws Exception {

    Map<String, String> oldContentProperty = new HashMap<String, String>() {
      {
        put("content", "# Licensed to the Apache Software Foundation (ASF) under one or more...");
      }
    };
    Map<String, String> newContentProperty = new HashMap<String, String>() {
      {
        put("content", "");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockHadoopMetrics2Properties = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("hadoop-metrics2.properties")).andReturn(mockHadoopMetrics2Properties).atLeastOnce();
    expect(mockHadoopMetrics2Properties.getProperties()).andReturn(oldContentProperty).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);

    replay(injector, clusters, mockHadoopMetrics2Properties, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
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
    new UpgradeCatalog270(injector2).clearHadoopMetrics2Content();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newContentProperty, updatedProperties).areEqual());

  }
}
