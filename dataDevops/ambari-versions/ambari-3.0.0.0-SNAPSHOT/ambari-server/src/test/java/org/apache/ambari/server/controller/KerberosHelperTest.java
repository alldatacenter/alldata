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

package org.apache.ambari.server.controller;

import static org.apache.ambari.server.controller.KerberosHelper.DIRECTIVE_COMPONENTS;
import static org.apache.ambari.server.controller.KerberosHelper.DIRECTIVE_HOSTS;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.utilities.KerberosChecker;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.scheduler.ExecutionSchedulerImpl;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.serveraction.ActionLog;
import org.apache.ambari.server.serveraction.kerberos.ConfigureAmbariIdentitiesServerAction;
import org.apache.ambari.server.serveraction.kerberos.CreateKeytabFilesServerAction;
import org.apache.ambari.server.serveraction.kerberos.CreatePrincipalsServerAction;
import org.apache.ambari.server.serveraction.kerberos.KDCType;
import org.apache.ambari.server.serveraction.kerberos.KerberosConfigDataFileWriterFactory;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationHandler;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationHandlerFactory;
import org.apache.ambari.server.serveraction.kerberos.PreconfigureServiceType;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalType;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.jpa.AmbariJpaPersistService;


@SuppressWarnings("unchecked")
public class KerberosHelperTest extends EasyMockSupport {

  private static Injector injector;
  private final ClusterController clusterController = createStrictMock(ClusterController.class);
  private final KerberosDescriptorFactory kerberosDescriptorFactory = createStrictMock(KerberosDescriptorFactory.class);
  private final KerberosConfigDataFileWriterFactory kerberosConfigDataFileWriterFactory = createStrictMock(KerberosConfigDataFileWriterFactory.class);
  private final AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
  private final TopologyManager topologyManager = createMock(TopologyManager.class);
  private final Configuration configuration = createMock(Configuration.class);
  private final AmbariCustomCommandExecutionHelper customCommandExecutionHelperMock = createNiceMock(AmbariCustomCommandExecutionHelper.class);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    reset(clusterController);
    reset(metaInfo);

    final KerberosOperationHandlerFactory kerberosOperationHandlerFactory = createMock(KerberosOperationHandlerFactory.class);

    expect(kerberosOperationHandlerFactory.getKerberosOperationHandler(KDCType.NONE))
        .andReturn(null)
        .anyTimes();

    expect(kerberosOperationHandlerFactory.getKerberosOperationHandler(KDCType.MIT_KDC))
        .andReturn(new KerberosOperationHandler() {
          @Override
          public void open(PrincipalKeyCredential administratorCredentials, String defaultRealm, Map<String, String> kerberosConfiguration) throws KerberosOperationException {
            setAdministratorCredential(administratorCredentials);
            setDefaultRealm(defaultRealm);
            setOpen(true);
          }

          @Override
          public void close() throws KerberosOperationException {

          }

          @Override
          public boolean principalExists(String principal, boolean service) throws KerberosOperationException {
            return "principal".equals(principal);
          }

          @Override
          public Integer createPrincipal(String principal, String password, boolean service) throws KerberosOperationException {
            return null;
          }

          @Override
          public Integer setPrincipalPassword(String principal, String password, boolean service) throws KerberosOperationException {
            return null;
          }

          @Override
          public boolean removePrincipal(String principal, boolean service) throws KerberosOperationException {
            return false;
          }

          @Override
          public boolean createKeytabFile(Keytab keytab, File destinationKeytabFile) throws KerberosOperationException {
            return true;
          }
        })
        .anyTimes();

    Method methodGetConfiguredTemporaryDirectory = KerberosHelperImpl.class.getDeclaredMethod("getConfiguredTemporaryDirectory");

    final KerberosHelperImpl kerberosHelper = createMockBuilder(KerberosHelperImpl.class)
        .addMockedMethod(methodGetConfiguredTemporaryDirectory)
        .createMock();

    expect(kerberosHelper.getConfiguredTemporaryDirectory()).andReturn(temporaryFolder.getRoot()).anyTimes();

    injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        PartialNiceMockBinder.newBuilder().addActionDBAccessorConfigsBindings().addFactoriesInstallBinding()
            .addPasswordEncryptorBindings().build().configure(binder());

        bind(AmbariJpaPersistService.class).toInstance(createNiceMock(AmbariJpaPersistService.class));
        bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
        bind(ExecutionScheduler.class).to(ExecutionSchedulerImpl.class);
        bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
        bind(HookService.class).to(UserHookService.class);
        bind(PersistedState.class).to(PersistedStateImpl.class);
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(SecurityHelper.class).toInstance(createNiceMock(SecurityHelper.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(AmbariCustomCommandExecutionHelper.class).toInstance(customCommandExecutionHelperMock);
        bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementController.class));
        bind(AmbariMetaInfo.class).toInstance(metaInfo);
        bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
        bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
        bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        bind(ConfigHelper.class).toInstance(createNiceMock(ConfigHelper.class));
        bind(KerberosOperationHandlerFactory.class).toInstance(kerberosOperationHandlerFactory);
        bind(ClusterController.class).toInstance(clusterController);
        bind(KerberosDescriptorFactory.class).toInstance(kerberosDescriptorFactory);
        bind(KerberosConfigDataFileWriterFactory.class).toInstance(kerberosConfigDataFileWriterFactory);
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(KerberosHelper.class).toInstance(kerberosHelper);
        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
        bind(CreatePrincipalsServerAction.class).toInstance(createMock(CreatePrincipalsServerAction.class));
        bind(CreateKeytabFilesServerAction.class).toInstance(createMock(CreateKeytabFilesServerAction.class));
        bind(ConfigureAmbariIdentitiesServerAction.class).toInstance(createMock(ConfigureAmbariIdentitiesServerAction.class));
        bind(StackAdvisorHelper.class).toInstance(createMock(StackAdvisorHelper.class));
        bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
        bind(ArtifactDAO.class).toInstance(createNiceMock(ArtifactDAO.class));
        bind(KerberosPrincipalDAO.class).toInstance(createNiceMock(KerberosPrincipalDAO.class));
        bind(KerberosKeytabPrincipalDAO.class).toInstance(createNiceMock(KerberosKeytabPrincipalDAO.class));
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
        bind(LdapFacade.class).toInstance(createNiceMock(LdapFacade.class));
        requestStaticInjection(KerberosChecker.class);
      }
    });

    //todo: StageUtils shouldn't be called for this test
    StageUtils.setTopologyManager(topologyManager);
    expect(topologyManager.getPendingHostComponents()).andReturn(
        Collections.emptyMap()).anyTimes();

    StageUtils.setConfiguration(configuration);
    expect(configuration.getApiSSLAuthentication()).andReturn(false).anyTimes();
    expect(configuration.getClientApiPort()).andReturn(8080).anyTimes();
    expect(configuration.getServerTempDir()).andReturn(temporaryFolder.getRoot().getAbsolutePath()).anyTimes();

    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    if (!credentialStoreService.isInitialized(CredentialStoreType.TEMPORARY)) {
      ((CredentialStoreServiceImpl) credentialStoreService).initializeTemporaryCredentialStore(10, TimeUnit.MINUTES, false);
    }
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test(expected = AmbariException.class)
  public void testMissingClusterEnv() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    Cluster cluster = createNiceMock(Cluster.class);
    RequestStageContainer requestStageContainer = createNiceMock(RequestStageContainer.class);

    replayAll();
    kerberosHelper.toggleKerberos(cluster, SecurityType.KERBEROS, requestStageContainer, true);
    verifyAll();
  }

  @Test(expected = AmbariException.class)
  public void testMissingKrb5Conf() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final Map<String, String> kerberosEnvProperties = createMock(Map.class);
    expect(kerberosEnvProperties.get("ldap_url")).andReturn("").once();
    expect(kerberosEnvProperties.get("container_dn")).andReturn("").once();

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).once();

    final Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getDesiredConfigByType("kerberos-env")).andReturn(kerberosEnvConfig).once();

    replayAll();
    kerberosHelper.toggleKerberos(cluster, SecurityType.KERBEROS, null, true);
    verifyAll();
  }

  @Test(expected = AmbariException.class)
  public void testMissingKerberosEnvConf() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final Map<String, String> kerberosEnvProperties = createMock(Map.class);
    expect(kerberosEnvProperties.get(KerberosHelper.DEFAULT_REALM)).andReturn("EXAMPLE.COM").once();
    expect(kerberosEnvProperties.get("kdc_hosts")).andReturn("10.0.100.1").once();

    final Map<String, String> krb5ConfProperties = createMock(Map.class);
    expect(krb5ConfProperties.get("kadmin_host")).andReturn("10.0.100.1").once();

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).once();

    final Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getDesiredConfigByType("krb5-conf")).andReturn(krb5ConfConfig).once();

    replayAll();
    kerberosHelper.toggleKerberos(cluster, SecurityType.KERBEROS, null, true);
    verifyAll();
  }

  @Test
  public void testEnableKerberos() throws Exception {
    testEnableKerberos(new PrincipalKeyCredential("principal", "password"), "mit-kdc", "true");
  }

  @Test
  public void testEnableKerberos_ManageIdentitiesFalseKdcNone() throws Exception {
    testEnableKerberos(new PrincipalKeyCredential("principal", "password"), "none", "false");
  }

  @Test(expected = AmbariException.class)
  public void testEnableKerberos_ManageIdentitiesTrueKdcNone() throws Exception {
    testEnableKerberos(new PrincipalKeyCredential("principal", "password"), "none", "true");
  }

  @Test(expected = KerberosInvalidConfigurationException.class)
  public void testEnableKerberos_ManageIdentitiesTrueKdcNull() throws Exception {
    testEnableKerberos(new PrincipalKeyCredential("principal", "password"), null, "true");
  }

  @Test(expected = KerberosMissingAdminCredentialsException.class)
  public void testEnableKerberosMissingCredentials() throws Exception {
    try {
      testEnableKerberos(null, "mit-kdc", "true");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().startsWith("Missing KDC administrator credentials"));
      throw e;
    }
  }

  @Test(expected = KerberosMissingAdminCredentialsException.class)
  public void testEnableKerberosInvalidCredentials() throws Exception {
    try {
      testEnableKerberos(new PrincipalKeyCredential("invalid_principal", "password"), "mit-kdc", "true");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().startsWith("Invalid KDC administrator credentials"));
      throw e;
    }
  }

  @Test
  public void testEnableKerberos_GetKerberosDescriptorFromCluster() throws Exception {
    testEnableKerberos(new PrincipalKeyCredential("principal", "password"), "mit-kdc", "true");
  }

  @Test
  public void testEnableKerberos_GetKerberosDescriptorFromStack() throws Exception {
    testEnableKerberos(new PrincipalKeyCredential("principal", "password"), "mit-kdc", "true");
  }

  @Test
  public void testEnsureIdentities() throws Exception {
    testEnsureIdentities(new PrincipalKeyCredential("principal", "password"), null);
  }

  @Test(expected = KerberosMissingAdminCredentialsException.class)
  public void testEnsureIdentitiesMissingCredentials() throws Exception {
    try {
      testEnsureIdentities(null, null);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().startsWith("Missing KDC administrator credentials"));
      throw e;
    }
  }

  @Test(expected = KerberosMissingAdminCredentialsException.class)
  public void testEnsureIdentitiesInvalidCredentials() throws Exception {
    try {
      testEnsureIdentities(new PrincipalKeyCredential("invalid_principal", "password"), null);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().startsWith("Invalid KDC administrator credentials"));
      throw e;
    }
  }

  @Test
  public void testEnsureIdentities_FilteredHosts() throws Exception {
    testEnsureIdentities(new PrincipalKeyCredential("principal", "password"), Collections.singleton("hostA"));
  }

  @Test
  public void testDeleteIdentities() throws Exception {
    testDeleteIdentities(new PrincipalKeyCredential("principal", "password"));
  }

  @Test(expected = KerberosMissingAdminCredentialsException.class)
  public void testDeleteIdentitiesMissingCredentials() throws Exception {
    try {
      testDeleteIdentities(null);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().startsWith("Missing KDC administrator credentials"));
      throw e;
    }
  }

  @Test(expected = KerberosMissingAdminCredentialsException.class)
  public void testDeleteIdentitiesInvalidCredentials() throws Exception {
    try {
      testDeleteIdentities(new PrincipalKeyCredential("invalid_principal", "password"));
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().startsWith("Invalid KDC administrator credentials"));
      throw e;
    }
  }

  @Test
  public void testExecuteCustomOperationsInvalidOperation() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final Cluster cluster = createNiceMock(Cluster.class);

    try {
      kerberosHelper.executeCustomOperations(cluster,
          Collections.singletonMap("invalid_operation", "false"), null, true);
    } catch (Throwable t) {
      Assert.fail("Exception should not have been thrown");
    }
  }

  @Test(expected = AmbariException.class)
  public void testRegenerateKeytabsInvalidValue() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final Cluster cluster = createNiceMock(Cluster.class);

    kerberosHelper.executeCustomOperations(cluster,
        Collections.singletonMap(KerberosHelper.DIRECTIVE_REGENERATE_KEYTABS, "false"), null, true);
    Assert.fail("AmbariException should have failed");
  }

  @Test
  public void testRegenerateKeytabsValidateRequestStageContainer() throws Exception {
    testRegenerateKeytabs(new PrincipalKeyCredential("principal", "password"), true, false);
  }

  @Test
  public void testRegenerateKeytabsValidateSkipInvalidHost() throws Exception {
    testRegenerateKeytabs(new PrincipalKeyCredential("principal", "password"), true, true);
  }

  @Test
  public void testRegenerateKeytabs() throws Exception {
    testRegenerateKeytabs(new PrincipalKeyCredential("principal", "password"), false, false);
  }

  /**
   * Tests that when regenerating keytabs for an upgrade, that the retry allowed
   * boolean is set on the tasks created.
   *
   * @throws Exception
   */
  @Test
  public void testRegenerateKeytabsWithRetryAllowed() throws Exception {
    Capture<ActionExecutionContext> captureContext = Capture.newInstance();
    customCommandExecutionHelperMock.addExecutionCommandsToStage(capture(captureContext),
        anyObject(Stage.class), anyObject(), eq(null));

    expectLastCall().atLeastOnce();

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(KerberosHelper.DIRECTIVE_REGENERATE_KEYTABS, "true");
    requestMap.put(KerberosHelper.ALLOW_RETRY, "true");

    RequestStageContainer requestStageContainer = testRegenerateKeytabs(
        new PrincipalKeyCredential("principal", "password"), requestMap, false, false);

    assertNotNull(requestStageContainer);

    ActionExecutionContext capturedContext = captureContext.getValue();
    assertTrue(capturedContext.isRetryAllowed());
  }

  @Test
  public void testDisableKerberos() throws Exception {
    testDisableKerberos(new PrincipalKeyCredential("principal", "password"));
  }

  @Test
  public void testCreateTestIdentity_ManageIdentitiesDefault() throws Exception {
    testCreateTestIdentity(new PrincipalKeyCredential("principal", "password"), null);
  }

  @Test
  public void testCreateTestIdentity_ManageIdentitiesTrue() throws Exception {
    testCreateTestIdentity(new PrincipalKeyCredential("principal", "password"), Boolean.TRUE);
  }

  @Test
  public void testCreateTestIdentity_ManageIdentitiesFalse() throws Exception {
    testCreateTestIdentity(new PrincipalKeyCredential("principal", "password"), Boolean.FALSE);
  }

  @Test(expected = KerberosMissingAdminCredentialsException.class)
  public void testCreateTestIdentityNoCredentials_ManageIdentitiesDefault() throws Exception {
    testCreateTestIdentity(null, null);
  }

  @Test(expected = KerberosMissingAdminCredentialsException.class)
  public void testCreateTestIdentityNoCredentials_ManageIdentitiesTrue() throws Exception {
    testCreateTestIdentity(null, Boolean.TRUE);
  }

  @Test
  public void testCreateTestIdentityNoCredentials_ManageIdentitiesFalse() throws Exception {
    testCreateTestIdentity(null, Boolean.FALSE);
  }

  @Test
  public void testDeleteTestIdentity() throws Exception {
    testDeleteTestIdentity(new PrincipalKeyCredential("principal", "password"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetActiveIdentities_MissingCluster() throws Exception {
    testGetActiveIdentities(null, null, null, null, true, SecurityType.KERBEROS);
  }

  @Test
  public void testGetActiveIdentities_SecurityTypeKerberos_All() throws Exception {
    testGetActiveIdentities_All(SecurityType.KERBEROS);
  }

  @Test
  public void testGetActiveIdentities_SecurityTypeNone_All() throws Exception {
    testGetActiveIdentities_All(SecurityType.NONE);
  }

  @Test
  public void testGetActiveIdentities_SingleHost() throws Exception {
    Map<String, Collection<KerberosIdentityDescriptor>> identities = testGetActiveIdentities("c1", "host1", null, null, true, SecurityType.KERBEROS);

    Assert.assertNotNull(identities);
    Assert.assertEquals(1, identities.size());

    Collection<KerberosIdentityDescriptor> hostIdentities;

    hostIdentities = identities.get("host1");
    Assert.assertNotNull(hostIdentities);
    Assert.assertEquals(3, hostIdentities.size());

    validateIdentities(hostIdentities, new HashMap<String, Map<String, Object>>() {{
      put("identity1", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host1@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/component1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/component1.keytab.file");
          put("keytab_cachable", false);
        }
      });

      put("identity2", new HashMap<String, Object>() {
        {
          put("principal_name", "component2/host1@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service2-site/component2.kerberos.principal");
          put("principal_local_username", "service2");
          put("keytab_file", "${keytab_dir}/service2.keytab");
          put("keytab_owner_name", "service2");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service2-site/component2.keytab.file");
          put("keytab_cachable", false);
        }
      });

      put("identity3", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host1@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/service1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.service.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/service1.keytab.file");
          put("keytab_cachable", false);
        }
      });
    }});
  }

  @Test
  public void addAmbariServerIdentity_CreateAmbariPrincipal() throws Exception {
    addAmbariServerIdentity(Collections.singletonMap(KerberosHelper.CREATE_AMBARI_PRINCIPAL, "true"));
  }

  @Test
  public void addAmbariServerIdentity_DoNotCreateAmbariPrincipal() throws Exception {
    addAmbariServerIdentity(Collections.singletonMap(KerberosHelper.CREATE_AMBARI_PRINCIPAL, "false"));
  }

  @Test
  public void addAmbariServerIdentity_MissingProperty() throws Exception {
    addAmbariServerIdentity(Collections.singletonMap("not_create_ambari_principal", "value"));
  }

  @Test
  public void addAmbariServerIdentity_MissingKerberosEnv() throws Exception {
    addAmbariServerIdentity(null);
  }

  @Test
  public void testGetActiveIdentities_SingleService() throws Exception {
    Map<String, Collection<KerberosIdentityDescriptor>> identities = testGetActiveIdentities("c1", null, "SERVICE1", null, true, SecurityType.KERBEROS);

    Assert.assertNotNull(identities);
    Assert.assertEquals(3, identities.size());

    Collection<KerberosIdentityDescriptor> hostIdentities;

    hostIdentities = identities.get("host1");
    Assert.assertNotNull(hostIdentities);
    Assert.assertEquals(2, hostIdentities.size());

    validateIdentities(hostIdentities, new HashMap<String, Map<String, Object>>() {{
      put("identity1", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host1@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/component1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/component1.keytab.file");
          put("keytab_cachable", false);
        }
      });

      put("identity3", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host1@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/service1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.service.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/service1.keytab.file");
          put("keytab_cachable", false);
        }
      });
    }});

    hostIdentities = identities.get("host2");
    Assert.assertNotNull(hostIdentities);
    Assert.assertEquals(2, hostIdentities.size());

    validateIdentities(hostIdentities, new HashMap<String, Map<String, Object>>() {{
      put("identity1", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host2@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/component1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/component1.keytab.file");
          put("keytab_cachable", false);
        }
      });

      put("identity3", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host2@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/service1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.service.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/service1.keytab.file");
          put("keytab_cachable", false);
        }
      });
    }});
  }

  @Test
  public void testGetActiveIdentities_SingleServiceSingleHost() throws Exception {
    Map<String, Collection<KerberosIdentityDescriptor>> identities = testGetActiveIdentities("c1", "host2", "SERVICE1", null, true, SecurityType.KERBEROS);

    Assert.assertNotNull(identities);
    Assert.assertEquals(1, identities.size());

    Collection<KerberosIdentityDescriptor> hostIdentities;

    hostIdentities = identities.get("host2");
    Assert.assertNotNull(hostIdentities);
    Assert.assertEquals(2, hostIdentities.size());

    validateIdentities(hostIdentities, new HashMap<String, Map<String, Object>>() {{
      put("identity1", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host2@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/component1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/component1.keytab.file");
          put("keytab_cachable", false);
        }
      });

      put("identity3", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host2@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/service1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.service.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/service1.keytab.file");
          put("keytab_cachable", false);
        }
      });
    }});
  }

  @Test
  public void testGetActiveIdentities_SingleComponent() throws Exception {
    Map<String, Collection<KerberosIdentityDescriptor>> identities = testGetActiveIdentities("c1", null, null, "COMPONENT2", true, SecurityType.KERBEROS);

    Assert.assertNotNull(identities);
    Assert.assertEquals(3, identities.size());

    Collection<KerberosIdentityDescriptor> hostIdentities;

    hostIdentities = identities.get("host1");
    Assert.assertNotNull(hostIdentities);
    Assert.assertEquals(1, hostIdentities.size());

    validateIdentities(hostIdentities, new HashMap<String, Map<String, Object>>() {{
      put("identity2", new HashMap<String, Object>() {
        {
          put("principal_name", "component2/host1@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service2-site/component2.kerberos.principal");
          put("principal_local_username", "service2");
          put("keytab_file", "${keytab_dir}/service2.keytab");
          put("keytab_owner_name", "service2");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service2-site/component2.keytab.file");
          put("keytab_cachable", false);
        }
      });
    }});

    hostIdentities = identities.get("host2");
    Assert.assertNotNull(hostIdentities);
    Assert.assertEquals(1, hostIdentities.size());

    validateIdentities(hostIdentities, new HashMap<String, Map<String, Object>>() {{
      put("identity2", new HashMap<String, Object>() {
        {
          put("principal_name", "component2/host2@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service2-site/component2.kerberos.principal");
          put("principal_local_username", "service2");
          put("keytab_file", "${keytab_dir}/service2.keytab");
          put("keytab_owner_name", "service2");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service2-site/component2.keytab.file");
          put("keytab_cachable", false);
        }
      });
    }});
  }

  private void testGetActiveIdentities_All(SecurityType clusterSecurityType) throws Exception {
    Map<String, Collection<KerberosIdentityDescriptor>> identities = testGetActiveIdentities("c1", null, null, null, true, clusterSecurityType);

    Assert.assertNotNull(identities);
    Assert.assertEquals(3, identities.size());

    Collection<KerberosIdentityDescriptor> hostIdentities;

    hostIdentities = identities.get("host1");
    Assert.assertNotNull(hostIdentities);
    Assert.assertEquals(3, hostIdentities.size());

    validateIdentities(hostIdentities, new HashMap<String, Map<String, Object>>() {{
      put("identity1", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host1@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/component1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/component1.keytab.file");
          put("keytab_cachable", false);
        }
      });

      put("identity2", new HashMap<String, Object>() {
        {
          put("principal_name", "component2/host1@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service2-site/component2.kerberos.principal");
          put("principal_local_username", "service2");
          put("keytab_file", "${keytab_dir}/service2.keytab");
          put("keytab_owner_name", "service2");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service2-site/component2.keytab.file");
          put("keytab_cachable", false);
        }
      });

      put("identity3", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host1@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/service1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.service.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/service1.keytab.file");
          put("keytab_cachable", false);
        }
      });
    }});

    hostIdentities = identities.get("host2");
    Assert.assertNotNull(hostIdentities);
    Assert.assertEquals(3, hostIdentities.size());

    validateIdentities(hostIdentities, new HashMap<String, Map<String, Object>>() {{
      put("identity1", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host2@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/component1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/component1.keytab.file");
          put("keytab_cachable", false);
        }
      });

      put("identity2", new HashMap<String, Object>() {
        {
          put("principal_name", "component2/host2@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service2-site/component2.kerberos.principal");
          put("principal_local_username", "service2");
          put("keytab_file", "${keytab_dir}/service2.keytab");
          put("keytab_owner_name", "service2");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service2-site/component2.keytab.file");
          put("keytab_cachable", false);
        }
      });

      put("identity3", new HashMap<String, Object>() {
        {
          put("principal_name", "service1/host2@EXAMPLE.COM");
          put("principal_type", KerberosPrincipalType.SERVICE);
          put("principal_configuration", "service1-site/service1.kerberos.principal");
          put("principal_local_username", "service1");
          put("keytab_file", "${keytab_dir}/service1.service.keytab");
          put("keytab_owner_name", "service1");
          put("keytab_owner_access", "rw");
          put("keytab_group_name", "hadoop");
          put("keytab_group_access", "");
          put("keytab_configuration", "service1-site/service1.keytab.file");
          put("keytab_cachable", false);
        }
      });
    }});
  }

  private void validateIdentities(Collection<KerberosIdentityDescriptor> identities, HashMap<String, Map<String, Object>> expectedDataMap) {

    Assert.assertEquals(expectedDataMap.size(), identities.size());

    for (KerberosIdentityDescriptor identity : identities) {
      Map<String, Object> expectedData = expectedDataMap.get(identity.getName());

      Assert.assertNotNull(expectedData);

      KerberosPrincipalDescriptor principal = identity.getPrincipalDescriptor();
      Assert.assertNotNull(principal);
      Assert.assertEquals(expectedData.get("principal_name"), principal.getName());
      Assert.assertEquals(expectedData.get("principal_type"), principal.getType());
      Assert.assertEquals(expectedData.get("principal_configuration"), principal.getConfiguration());
      Assert.assertEquals(expectedData.get("principal_local_username"), principal.getLocalUsername());

      KerberosKeytabDescriptor keytab = identity.getKeytabDescriptor();
      Assert.assertNotNull(keytab);
      Assert.assertEquals(expectedData.get("keytab_file"), keytab.getFile());
      Assert.assertEquals(expectedData.get("keytab_owner_name"), keytab.getOwnerName());
      Assert.assertEquals(expectedData.get("keytab_owner_access"), keytab.getOwnerAccess());
      Assert.assertEquals(expectedData.get("keytab_group_name"), keytab.getGroupName());
      Assert.assertEquals(expectedData.get("keytab_group_access"), keytab.getGroupAccess());
      Assert.assertEquals(expectedData.get("keytab_configuration"), keytab.getConfiguration());
      Assert.assertEquals(Boolean.TRUE.equals(expectedData.get("keytab_cachable")), keytab.isCachable());
    }
  }

  private void testEnableKerberos(final PrincipalKeyCredential PrincipalKeyCredential,
                                  String kdcType,
                                  String manageIdentities) throws Exception {

    StackId stackId = new StackId("HDP", "2.2");

    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);
    boolean identitiesManaged = (manageIdentities == null) || !"false".equalsIgnoreCase(manageIdentities);

    final ServiceComponentHost schKerberosClient = createMock(ServiceComponentHost.class);
    expect(schKerberosClient.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClient.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(schKerberosClient.getHostName()).andReturn("host1").anyTimes();
    expect(schKerberosClient.getState()).andReturn(State.INSTALLED).anyTimes();

    final ServiceComponentHost sch1 = createMock(ServiceComponentHost.class);
    expect(sch1.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(sch1.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();
    expect(sch1.getHostName()).andReturn("host1").anyTimes();
    expect(sch1.getState()).andReturn(State.INSTALLED).anyTimes();

    final ServiceComponentHost sch2 = createMock(ServiceComponentHost.class);
    expect(sch2.getServiceName()).andReturn("SERVICE2").anyTimes();
    expect(sch2.getServiceComponentName()).andReturn("COMPONENT2").anyTimes();
    expect(sch2.getHostName()).andReturn("host1").anyTimes();
    expect(sch2.getState()).andReturn(State.INSTALLED).anyTimes();

    final Host host = createMockHost("host1");

    final ServiceComponent serviceComponentKerberosClient = createNiceMock(ServiceComponent.class);
    expect(serviceComponentKerberosClient.getName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(serviceComponentKerberosClient.getServiceComponentHosts()).andReturn(Collections.singletonMap("host1", schKerberosClient)).anyTimes();

    final Service serviceKerberos = createNiceMock(Service.class);
    expect(serviceKerberos.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(serviceKerberos.getName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(serviceKerberos.getServiceComponents())
        .andReturn(Collections.singletonMap(Role.KERBEROS_CLIENT.name(), serviceComponentKerberosClient))
        .anyTimes();

    final Service service1 = createNiceMock(Service.class);
    expect(service1.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(service1.getName()).andReturn("SERVICE1").anyTimes();
    expect(service1.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Service service2 = createNiceMock(Service.class);
    expect(service2.getName()).andReturn("SERVICE2").anyTimes();
    expect(service2.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(service2.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Map<String, String> kerberosEnvProperties = createMock(Map.class);
    expect(kerberosEnvProperties.get(KerberosHelper.KDC_TYPE)).andReturn(kdcType).anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.MANAGE_IDENTITIES)).andReturn(manageIdentities).anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.DEFAULT_REALM)).andReturn("FOOBAR.COM").anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.CREATE_AMBARI_PRINCIPAL)).andReturn("false").anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.PRECONFIGURE_SERVICES)).andReturn(PreconfigureServiceType.DEFAULT.name()).anyTimes();

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).anyTimes();

    final Map<String, String> krb5ConfProperties = createMock(Map.class);

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).anyTimes();

    final Cluster cluster = createMockCluster("c1", Collections.singleton(host), SecurityType.KERBEROS, krb5ConfConfig, kerberosEnvConfig);
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(cluster.getServices())
        .andReturn(new HashMap<String, Service>() {
          {
            put(Service.Type.KERBEROS.name(), serviceKerberos);
            put("SERVICE1", service1);
            put("SERVICE2", service2);
          }
        })
        .anyTimes();
    expect(cluster.getServiceComponentHosts("host1"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(schKerberosClient);
            add(sch1);
            add(sch2);
          }
        })
        .once();
    expect(cluster.getServiceComponentHosts("KERBEROS", "KERBEROS_CLIENT"))
        .andReturn(Collections.singletonList(schKerberosClient))
        .once();

    if (identitiesManaged) {
      final Clusters clusters = injector.getInstance(Clusters.class);
      expect(clusters.getHost("host1"))
          .andReturn(host)
          .once();
    }

    final AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, null))
        .andReturn(Collections.emptyMap())
        .once();
    expect(ambariManagementController.getRoleCommandOrder(cluster))
        .andReturn(createMock(RoleCommandOrder.class))
        .once();

    final KerberosServiceDescriptor serviceDescriptor1 = createMock(KerberosServiceDescriptor.class);

    final KerberosServiceDescriptor serviceDescriptor2 = createMock(KerberosServiceDescriptor.class);

    final KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getService("KERBEROS")).andReturn(null).once();
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).once();
    expect(kerberosDescriptor.getService("SERVICE2")).andReturn(serviceDescriptor2).once();

    setupKerberosDescriptor(kerberosDescriptor);
    setupStageFactory();

    // This is a STRICT mock to help ensure that the end result is what we want.
    final RequestStageContainer requestStageContainer = createStrictMock(RequestStageContainer.class);
    // Create Preparation Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();

    if (identitiesManaged) {
      // Create Principals Stage
      expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(anyObject(List.class));
      expectLastCall().once();
      // Create Keytabs Stage
      expect(requestStageContainer.getLastStageId()).andReturn(0L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(anyObject(List.class));
      expectLastCall().once();
      // Distribute Keytabs Stage
      expect(requestStageContainer.getLastStageId()).andReturn(1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(anyObject(List.class));
      expectLastCall().once();
    }
    // Update Configs Stage
    expect(requestStageContainer.getLastStageId()).andReturn(2L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();

    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();
    // TODO: Add more of these when more stages are added.
    // Clean-up/Finalize Stage
    expect(requestStageContainer.getLastStageId()).andReturn(3L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();

    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    credentialStoreService.setCredential(cluster.getClusterName(), KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
        PrincipalKeyCredential, CredentialStoreType.TEMPORARY);

    kerberosHelper.toggleKerberos(cluster, SecurityType.KERBEROS, requestStageContainer, null);

    verifyAll();
  }

  private void testDisableKerberos(final PrincipalKeyCredential PrincipalKeyCredential) throws Exception {

    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final ServiceComponentHost schKerberosClient = createMock(ServiceComponentHost.class);
    expect(schKerberosClient.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClient.getHostName()).andReturn("host1").anyTimes();
    expect(schKerberosClient.getState()).andReturn(State.INSTALLED).anyTimes();

    final ServiceComponentHost sch1 = createMock(ServiceComponentHost.class);
    expect(sch1.getServiceName()).andReturn("SERVICE1").times(1);
    expect(sch1.getHostName()).andReturn("host1").anyTimes();
    expect(sch1.getState()).andReturn(State.INSTALLED).anyTimes();

    final ServiceComponentHost sch2 = createMock(ServiceComponentHost.class);
    expect(sch2.getServiceName()).andReturn("SERVICE2").times(1);
    expect(sch2.getHostName()).andReturn("host1").anyTimes();
    expect(sch2.getState()).andReturn(State.INSTALLED).anyTimes();

    final Host host = createMockHost("host1");

    final ServiceComponent serviceComponentKerberosClient = createNiceMock(ServiceComponent.class);
    expect(serviceComponentKerberosClient.getName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(serviceComponentKerberosClient.getServiceComponentHosts()).andReturn(Collections.singletonMap("host1", schKerberosClient)).anyTimes();

    final Service serviceKerberos = createNiceMock(Service.class);
    expect(serviceKerberos.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(serviceKerberos.getName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(serviceKerberos.getServiceComponents())
        .andReturn(Collections.singletonMap(Role.KERBEROS_CLIENT.name(), serviceComponentKerberosClient))
        .anyTimes();

    final Service service1 = createNiceMock(Service.class);
    expect(service1.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service1.getName()).andReturn("SERVICE1").anyTimes();
    expect(service1.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Service service2 = createNiceMock(Service.class);
    expect(service2.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service2.getName()).andReturn("SERVICE2").anyTimes();
    expect(service2.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Map<String, String> kerberosEnvProperties = createMock(Map.class);
    expect(kerberosEnvProperties.get(KerberosHelper.KDC_TYPE)).andReturn("mit-kdc").anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.DEFAULT_REALM)).andReturn("FOOBAR.COM").anyTimes();

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).anyTimes();

    final Map<String, String> krb5ConfProperties = createMock(Map.class);

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).anyTimes();

    final Cluster cluster = createMockCluster("c1", Collections.singleton(host), SecurityType.NONE, krb5ConfConfig, kerberosEnvConfig);
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(cluster.getServices())
        .andReturn(new HashMap<String, Service>() {
          {
            put(Service.Type.KERBEROS.name(), serviceKerberos);
            put("SERVICE1", service1);
            put("SERVICE2", service2);
          }
        })
        .anyTimes();
    expect(cluster.getServiceComponentHosts("host1"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(schKerberosClient);
            add(sch1);
            add(sch2);
          }
        })
        .once();
    expect(cluster.getServiceComponentHosts("KERBEROS", "KERBEROS_CLIENT"))
        .andReturn(Collections.singletonList(schKerberosClient))
        .once();

    final Clusters clusters = injector.getInstance(Clusters.class);
    expect(clusters.getHost("host1"))
        .andReturn(host)
        .once();

    final AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, null))
        .andReturn(Collections.emptyMap())
        .once();
    expect(ambariManagementController.getRoleCommandOrder(cluster))
        .andReturn(createMock(RoleCommandOrder.class))
        .once();

    final KerberosServiceDescriptor serviceDescriptor1 = createMock(KerberosServiceDescriptor.class);

    final KerberosServiceDescriptor serviceDescriptor2 = createMock(KerberosServiceDescriptor.class);

    final KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getService("KERBEROS")).andReturn(null).atLeastOnce();
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).atLeastOnce();
    expect(kerberosDescriptor.getService("SERVICE2")).andReturn(serviceDescriptor2).atLeastOnce();

    setupKerberosDescriptor(kerberosDescriptor);
    setupStageFactory();

    // This is a STRICT mock to help ensure that the end result is what we want.
    final RequestStageContainer requestStageContainer = createStrictMock(RequestStageContainer.class);
    expect(cluster.getService("ZOOKEEPER")).andReturn(service1).anyTimes();
    // Hook Stage
    expect(requestStageContainer.getLastStageId()).andReturn(2L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();
    // StopZk Stage
    expect(requestStageContainer.getLastStageId()).andReturn(2L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();
    // Preparation Stage
    expect(requestStageContainer.getLastStageId()).andReturn(2L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Update Configs Stage
    expect(requestStageContainer.getLastStageId()).andReturn(2L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Destroy Principals Stage
    expect(requestStageContainer.getLastStageId()).andReturn(2L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Delete Keytabs Stage
    expect(requestStageContainer.getLastStageId()).andReturn(2L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Finalize Stage
    expect(requestStageContainer.getLastStageId()).andReturn(3L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();

    // Cleanup Stage
    expect(requestStageContainer.getLastStageId()).andReturn(3L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    credentialStoreService.setCredential(cluster.getClusterName(), KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
        PrincipalKeyCredential, CredentialStoreType.TEMPORARY);

    kerberosHelper.toggleKerberos(cluster, SecurityType.NONE, requestStageContainer, true);

    verifyAll();
  }

  private RequestStageContainer testRegenerateKeytabs(final PrincipalKeyCredential principalKeyCredential, boolean mockRequestStageContainer, final boolean testInvalidHost) throws Exception {
    return testRegenerateKeytabs(principalKeyCredential, Collections.singletonMap(KerberosHelper.DIRECTIVE_REGENERATE_KEYTABS, "true"), mockRequestStageContainer, testInvalidHost);
  }

  private RequestStageContainer testRegenerateKeytabs(final PrincipalKeyCredential PrincipalKeyCredential, Map<String,String> requestMap, boolean mockRequestStageContainer, final boolean testInvalidHost) throws Exception {

    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final ServiceComponentHost schKerberosClient = createMock(ServiceComponentHost.class);
    expect(schKerberosClient.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClient.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(schKerberosClient.getHostName()).andReturn("host1").anyTimes();
    expect(schKerberosClient.getState()).andReturn(State.INSTALLED).anyTimes();

    final ServiceComponentHost sch1 = createMock(ServiceComponentHost.class);
    expect(sch1.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(sch1.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();
    expect(sch1.getHostName()).andReturn("host1").anyTimes();

    final ServiceComponentHost sch2 = createMock(ServiceComponentHost.class);
    expect(sch2.getServiceName()).andReturn("SERVICE2").anyTimes();
    expect(sch2.getServiceComponentName()).andReturn("COMPONENT2").anyTimes();
    expect(sch2.getHostName()).andReturn("host1").anyTimes();

    final Host host = createMockHost("host1");

    final ServiceComponentHost schKerberosClientInvalid;
    final ServiceComponentHost sch1a;
    final Host hostInvalid;
    if (testInvalidHost) {
      schKerberosClientInvalid = createMock(ServiceComponentHost.class);
      expect(schKerberosClientInvalid.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
      expect(schKerberosClientInvalid.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
      expect(schKerberosClientInvalid.getHostName()).andReturn("host2").anyTimes();
      expect(schKerberosClientInvalid.getState()).andReturn(State.INIT).anyTimes();

      sch1a = createMock(ServiceComponentHost.class);
      expect(sch1a.getServiceName()).andReturn("SERVICE1").anyTimes();
      expect(sch1a.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();
      expect(sch1a.getHostName()).andReturn("host2").anyTimes();

      hostInvalid = createMockHost("host1");
    } else {
      schKerberosClientInvalid = null;
      hostInvalid = null;
    }

    Map<String, ServiceComponentHost> map = new HashMap<>();
    final ServiceComponent serviceComponentKerberosClient = createNiceMock(ServiceComponent.class);
    map.put("host1", schKerberosClient);
    expect(serviceComponentKerberosClient.getName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();

    if (testInvalidHost) {
      map.put("host2", schKerberosClientInvalid);
    }

    expect(serviceComponentKerberosClient.getServiceComponentHosts()).andReturn(map).anyTimes();

    final Service serviceKerberos = createStrictMock(Service.class);
    expect(serviceKerberos.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(serviceKerberos.getName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(serviceKerberos.getServiceComponents())
        .andReturn(Collections.singletonMap(Role.KERBEROS_CLIENT.name(), serviceComponentKerberosClient))
        .anyTimes();

    final Service service1 = createStrictMock(Service.class);
    expect(service1.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service1.getName()).andReturn("SERVICE1").anyTimes();
    expect(service1.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Service service2 = createStrictMock(Service.class);
    expect(service2.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service2.getName()).andReturn("SERVICE2").anyTimes();
    expect(service2.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Map<String, String> kerberosEnvProperties = createMock(Map.class);
    expect(kerberosEnvProperties.get(KerberosHelper.KDC_TYPE)).andReturn("mit-kdc").anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.DEFAULT_REALM)).andReturn("FOOBAR.COM").anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.CREATE_AMBARI_PRINCIPAL)).andReturn("false").anyTimes();

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).anyTimes();

    final Map<String, String> krb5ConfProperties = createMock(Map.class);

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).anyTimes();

    final Collection<Host> hosts = (testInvalidHost)
        ? Arrays.asList(host, hostInvalid)
        : Collections.singleton(host);

    final Cluster cluster = createMockCluster("c1", hosts, SecurityType.KERBEROS, krb5ConfConfig, kerberosEnvConfig);
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(cluster.getServices())
        .andReturn(new HashMap<String, Service>() {
          {
            put(Service.Type.KERBEROS.name(), serviceKerberos);
            put("SERVICE1", service1);
            put("SERVICE2", service2);
          }
        })
        .anyTimes();
    expect(cluster.getServiceComponentHosts("host1"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(schKerberosClient);
            add(sch1);
            add(sch2);
          }
        })
        .atLeastOnce();
    expect(cluster.getServiceComponentHosts("KERBEROS", "KERBEROS_CLIENT"))
        .andReturn(Collections.singletonList(schKerberosClient))
        .once();

    final AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, null))
        .andReturn(Collections.emptyMap())
        .once();
    expect(ambariManagementController.getRoleCommandOrder(cluster))
        .andReturn(createMock(RoleCommandOrder.class))
        .once();

    final KerberosServiceDescriptor serviceDescriptor1 = createMock(KerberosServiceDescriptor.class);

    final KerberosServiceDescriptor serviceDescriptor2 = createMock(KerberosServiceDescriptor.class);

    final KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getService("KERBEROS")).andReturn(null).atLeastOnce();
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).atLeastOnce();
    expect(kerberosDescriptor.getService("SERVICE2")).andReturn(serviceDescriptor2).atLeastOnce();

    setupKerberosDescriptor(kerberosDescriptor);
    setupStageFactory();

    final RequestStageContainer requestStageContainer;
    if (mockRequestStageContainer) {
      // This is a STRICT mock to help ensure that the end result is what we want.
      requestStageContainer = createStrictMock(RequestStageContainer.class);
      // Create Preparation Stage
      expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Create Principals Stage
      expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Create Keytabs Stage
      expect(requestStageContainer.getLastStageId()).andReturn(0L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Distribute Keytabs Stage
      expect(requestStageContainer.getLastStageId()).andReturn(1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Update Configurations Stage
      expect(requestStageContainer.getLastStageId()).andReturn(1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Clean-up/Finalize Stage
      expect(requestStageContainer.getLastStageId()).andReturn(3L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
    } else {
      requestStageContainer = null;
    }

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    credentialStoreService.setCredential(cluster.getClusterName(), KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
        PrincipalKeyCredential, CredentialStoreType.TEMPORARY);

    RequestStageContainer returnValue = kerberosHelper.executeCustomOperations(cluster,
        requestMap, requestStageContainer, true);

    Assert.assertNotNull(returnValue);

    verifyAll();

    return returnValue;
  }

  @Test
  public void testIsClusterKerberosEnabled_false() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    Cluster cluster = createStrictMock(Cluster.class);

    expect(cluster.getSecurityType()).andReturn(SecurityType.NONE);

    replay(cluster);
    assertFalse(kerberosHelper.isClusterKerberosEnabled(cluster));
    verify(cluster);
  }

  @Test
  public void testIsClusterKerberosEnabled_true() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    Cluster cluster = createStrictMock(Cluster.class);

    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS);

    replay(cluster);
    assertTrue(kerberosHelper.isClusterKerberosEnabled(cluster));
    verify(cluster);
  }

  @Test
  public void testGetManageIdentitiesDirective_NotSet() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    assertEquals(null, kerberosHelper.getManageIdentitiesDirective(null));
    assertEquals(null, kerberosHelper.getManageIdentitiesDirective(Collections.emptyMap()));

    assertEquals(null, kerberosHelper.getManageIdentitiesDirective(
        new HashMap<String, String>() {
          {
            put(KerberosHelper.DIRECTIVE_MANAGE_KERBEROS_IDENTITIES, null);
            put("some_directive_0", "false");
            put("some_directive_1", null);
          }
        }
    ));

    assertEquals(null, kerberosHelper.getManageIdentitiesDirective(
        new HashMap<String, String>() {
          {
            put("some_directive_0", "false");
            put("some_directive_1", null);
          }
        }
    ));
  }

  @Test
  public void testGetManageIdentitiesDirective_True() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    assertEquals(Boolean.TRUE, kerberosHelper.getManageIdentitiesDirective(Collections.singletonMap(KerberosHelper.DIRECTIVE_MANAGE_KERBEROS_IDENTITIES, "true")));
    assertEquals(Boolean.TRUE, kerberosHelper.getManageIdentitiesDirective(Collections.singletonMap(KerberosHelper.DIRECTIVE_MANAGE_KERBEROS_IDENTITIES, "not_false")));

    assertEquals(Boolean.TRUE, kerberosHelper.getManageIdentitiesDirective(
        new HashMap<String, String>() {
          {
            put(KerberosHelper.DIRECTIVE_MANAGE_KERBEROS_IDENTITIES, "true");
            put("some_directive_0", "false");
            put("some_directive_1", null);
          }
        }
    ));
  }

  @Test
  public void testGetManageIdentitiesDirective_False() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    assertEquals(Boolean.FALSE, kerberosHelper.getManageIdentitiesDirective(Collections.singletonMap(KerberosHelper.DIRECTIVE_MANAGE_KERBEROS_IDENTITIES, "false")));

    assertEquals(Boolean.FALSE, kerberosHelper.getManageIdentitiesDirective(
        new HashMap<String, String>() {
          {
            put(KerberosHelper.DIRECTIVE_MANAGE_KERBEROS_IDENTITIES, "false");
            put("some_directive_0", "false");
            put("some_directive_1", null);
          }
        }
    ));
  }

  @Test
  public void testGetForceToggleKerberosDirective_NotSet() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    assertEquals(false, kerberosHelper.getForceToggleKerberosDirective(null));
    assertEquals(false, kerberosHelper.getForceToggleKerberosDirective(Collections.emptyMap()));

    assertEquals(false, kerberosHelper.getForceToggleKerberosDirective(
        new HashMap<String, String>() {
          {
            put(KerberosHelper.DIRECTIVE_FORCE_TOGGLE_KERBEROS, null);
            put("some_directive_0", "false");
            put("some_directive_1", null);
          }
        }
    ));

    assertEquals(false, kerberosHelper.getForceToggleKerberosDirective(
        new HashMap<String, String>() {
          {
            put("some_directive_0", "false");
            put("some_directive_1", null);
          }
        }
    ));
  }

  @Test
  public void testGetForceToggleKerberosDirective_True() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    assertEquals(true, kerberosHelper.getForceToggleKerberosDirective(Collections.singletonMap(KerberosHelper.DIRECTIVE_FORCE_TOGGLE_KERBEROS, "true")));
    assertEquals(false, kerberosHelper.getForceToggleKerberosDirective(Collections.singletonMap(KerberosHelper.DIRECTIVE_FORCE_TOGGLE_KERBEROS, "not_true")));

    assertEquals(true, kerberosHelper.getForceToggleKerberosDirective(
        new HashMap<String, String>() {
          {
            put(KerberosHelper.DIRECTIVE_FORCE_TOGGLE_KERBEROS, "true");
            put("some_directive_0", "false");
            put("some_directive_1", null);
          }
        }
    ));
  }

  @Test
  public void testGetForceToggleKerberosDirective_False() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    assertEquals(false, kerberosHelper.getForceToggleKerberosDirective(Collections.singletonMap(KerberosHelper.DIRECTIVE_FORCE_TOGGLE_KERBEROS, "false")));

    assertEquals(false, kerberosHelper.getForceToggleKerberosDirective(
        new HashMap<String, String>() {
          {
            put(KerberosHelper.DIRECTIVE_FORCE_TOGGLE_KERBEROS, "false");
            put("some_directive_0", "false");
            put("some_directive_1", null);
          }
        }
    ));
  }

  @Test
  public void testSetAuthToLocalRules() throws Exception {
    testSetAuthToLocalRules(false);
  }

  @Test
  public void testSetAuthToLocalRulesWithPreconfiguredServices() throws Exception {
    testSetAuthToLocalRules(true);
  }

  private void testSetAuthToLocalRules(boolean includePreconfiguredServices) throws Exception {
    final KerberosPrincipalDescriptor principalDescriptor1 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor1.getValue()).andReturn("principal1/host1@EXAMPLE.COM").times(1);
    expect(principalDescriptor1.getLocalUsername()).andReturn("principal1_user").times(1);

    final KerberosPrincipalDescriptor principalDescriptor2 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor2.getValue()).andReturn("principal2/host2@EXAMPLE.COM").times(1);
    expect(principalDescriptor2.getLocalUsername()).andReturn("principal2_user").times(1);

    final KerberosPrincipalDescriptor principalDescriptor3 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor3.getValue()).andReturn("principal3/host3@EXAMPLE.COM").times(1);
    expect(principalDescriptor3.getLocalUsername()).andReturn("principal3_user").times(1);

    final KerberosIdentityDescriptor identityDescriptor1 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor1.getPrincipalDescriptor()).andReturn(principalDescriptor1).times(1);
    expect(identityDescriptor1.shouldInclude(EasyMock.anyObject())).andReturn(true).anyTimes();

    final KerberosIdentityDescriptor identityDescriptor2 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor2.getPrincipalDescriptor()).andReturn(principalDescriptor2).times(1);
    expect(identityDescriptor2.shouldInclude(EasyMock.anyObject())).andReturn(true).anyTimes();

    final KerberosIdentityDescriptor identityDescriptor3 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor3.getPrincipalDescriptor()).andReturn(principalDescriptor3).times(1);
    expect(identityDescriptor3.shouldInclude(EasyMock.anyObject())).andReturn(true).anyTimes();

    final KerberosComponentDescriptor componentDescriptor1 = createMockComponentDescriptor(
        "COMPONENT1",
        Collections.singletonList(identityDescriptor3),
        null);

    final KerberosServiceDescriptor serviceDescriptor1 = createMock(KerberosServiceDescriptor.class);
    expect(serviceDescriptor1.getName()).andReturn("SERVICE1").times((includePreconfiguredServices) ? 2 : 1);
    if (includePreconfiguredServices) {
      expect(serviceDescriptor1.shouldPreconfigure()).andReturn(false).times(2);
    }
    expect(serviceDescriptor1.getIdentities(eq(true), EasyMock.anyObject())).andReturn(Arrays.asList(
        identityDescriptor1,
        identityDescriptor2
    )).times(1);
    expect(serviceDescriptor1.getComponents()).andReturn(Collections.singletonMap("COMPONENT1", componentDescriptor1)).times(1);
    expect(serviceDescriptor1.getAuthToLocalProperties()).andReturn(new HashSet<>(Arrays.asList(
        "default",
        "explicit_multiple_lines|new_lines",
        "explicit_multiple_lines_escaped|new_lines_escaped",
        "explicit_single_line|spaces",
        "service-site/default",
        "service-site/explicit_multiple_lines|new_lines",
        "service-site/explicit_multiple_lines_escaped|new_lines_escaped",
        "service-site/explicit_single_line|spaces"
    ))).times(1);

    Map<String, KerberosServiceDescriptor> serviceDescriptorMap = new HashMap<>();
    serviceDescriptorMap.put("SERVICE1", serviceDescriptor1);

    Map<String, ServiceComponent> component1Map = new HashMap<>();

    Service service1 = createMockService("SERVICE1", component1Map);

    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("SERVICE1", service1);

    Map<String, String> serviceSiteProperties = new HashMap<>();
    serviceSiteProperties.put("default", "RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\nDEFAULT");
    serviceSiteProperties.put("explicit_multiple_lines", "RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\nDEFAULT");
    serviceSiteProperties.put("explicit_multiple_lines_escaped", "RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\\\nDEFAULT");
    serviceSiteProperties.put("explicit_single_line", "RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/ DEFAULT");

    Map<String, Map<String, String>> existingConfigs = new HashMap<>();
    existingConfigs.put("kerberos-env", new HashMap<String, String>());
    existingConfigs.put("service-site", serviceSiteProperties);

    if (includePreconfiguredServices) {
      final KerberosPrincipalDescriptor principalDescriptor4 = createMock(KerberosPrincipalDescriptor.class);
      expect(principalDescriptor4.getValue()).andReturn("${preconfig-site/service_user}/_HOST@EXAMPLE.COM").times(1);
      expect(principalDescriptor4.getLocalUsername()).andReturn("principal4_user").times(1);

      final KerberosPrincipalDescriptor principalDescriptor5 = createMock(KerberosPrincipalDescriptor.class);
      expect(principalDescriptor5.getValue()).andReturn("${preconfig-site/component_property1}/_HOST@EXAMPLE.COM").times(1);
      expect(principalDescriptor5.getLocalUsername()).andReturn("principal5_user").times(1);

      final KerberosIdentityDescriptor identityDescriptor4 = createMock(KerberosIdentityDescriptor.class);
      expect(identityDescriptor4.getPrincipalDescriptor()).andReturn(principalDescriptor4).times(1);
      expect(identityDescriptor4.shouldInclude(anyObject(Map.class))).andReturn(true).anyTimes();

      final KerberosIdentityDescriptor identityDescriptor5 = createMock(KerberosIdentityDescriptor.class);
      expect(identityDescriptor5.getPrincipalDescriptor()).andReturn(principalDescriptor5).times(1);
      expect(identityDescriptor5.shouldInclude(anyObject(Map.class))).andReturn(true).anyTimes();

      final KerberosComponentDescriptor componentDescriptor2 = createMockComponentDescriptor(
          "PRECONFIGURE_SERVICE_MASTER",
          Collections.singletonList(identityDescriptor5),
          null);

      final KerberosServiceDescriptor serviceDescriptor2 = createMock(KerberosServiceDescriptor.class);
      expect(serviceDescriptor2.getName()).andReturn("PRECONFIGURE_SERVICE").times(2);
      expect(serviceDescriptor2.shouldPreconfigure()).andReturn(true).times(2);
      expect(serviceDescriptor2.getIdentities(eq(true), anyObject(Map.class)))
          .andReturn(Collections.singletonList(identityDescriptor4))
          .times(1);
      expect(serviceDescriptor2.getComponents()).andReturn(Collections.singletonMap("PRECONFIGURE_SERVICE_MASTER", componentDescriptor2)).times(1);
      expect(serviceDescriptor2.getAuthToLocalProperties()).andReturn(Collections.<String>emptySet()).times(1);

      // Expected to have been added by a previous call to org.apache.ambari.server.controller.KerberosHelper.getKerberosDescriptor,
      // where includePreconfigureData = true
      serviceDescriptorMap.put("PRECONFIGURE_SERVICE", serviceDescriptor2);

      ComponentInfo preconfigureComponentInfo = createMock(ComponentInfo.class);
//      expect(preconfigureComponentInfo.getName()).andReturn("PRECONFIGURE_SERVICE_MASTER").once();

      PropertyInfo preconfigureServiceUser = createMockPropertyInfo("preconfig-site.xml", "service_user", "principal4");
      PropertyInfo preconfigureComponentProperty1 = createMockPropertyInfo("preconfig-site.xml", "component_property1", "principal5");

      List<PropertyInfo> preconfigureServiceProperties = Arrays.asList(preconfigureComponentProperty1, preconfigureServiceUser);

      ServiceInfo preconfigureServiceInfo = createMock(ServiceInfo.class);
      expect(preconfigureServiceInfo.getProperties()).andReturn(preconfigureServiceProperties).anyTimes();
      expect(preconfigureServiceInfo.getComponents()).andReturn(Collections.singletonList(preconfigureComponentInfo)).anyTimes();

      AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
      expect(ambariMetaInfo.isValidService("HDP", "2.2", "PRECONFIGURE_SERVICE")).andReturn(true).anyTimes();
      expect(ambariMetaInfo.getService("HDP", "2.2", "PRECONFIGURE_SERVICE")).andReturn(preconfigureServiceInfo).anyTimes();
    }

    final KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getProperty("additional_realms")).andReturn(null).times(1);
    expect(kerberosDescriptor.getIdentities(eq(true), EasyMock.anyObject())).andReturn(null).times(1);
    expect(kerberosDescriptor.getAuthToLocalProperties()).andReturn(null).times(1);
    expect(kerberosDescriptor.getServices()).andReturn(serviceDescriptorMap).times((includePreconfiguredServices) ? 2 : 1);

    final Cluster cluster = createMockCluster("c1", Collections.<Host>emptyList(), SecurityType.KERBEROS, null, null);
    if (includePreconfiguredServices) {
      expect(cluster.getServices()).andReturn(serviceMap).once();
    }

    Map<String, Set<String>> installedServices = Collections.singletonMap("SERVICE1", Collections.singleton("COMPONENT1"));

    Map<String, Map<String, String>> kerberosConfigurations = new HashMap<>();

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);
    kerberosHelper.setAuthToLocalRules(cluster, kerberosDescriptor, "EXAMPLE.COM", installedServices, existingConfigs, kerberosConfigurations, includePreconfiguredServices);

    verifyAll();

    Map<String, String> configs;

    configs = kerberosConfigurations.get("");
    assertNotNull(configs);
    if (includePreconfiguredServices) {
      assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\n" +
              "RULE:[2:$1@$0](principal4@EXAMPLE.COM)s/.*/principal4_user/\n" +
              "RULE:[2:$1@$0](principal5@EXAMPLE.COM)s/.*/principal5_user/\n" +
              "DEFAULT",
          configs.get("default"));
      assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\n" +
              "RULE:[2:$1@$0](principal4@EXAMPLE.COM)s/.*/principal4_user/\n" +
              "RULE:[2:$1@$0](principal5@EXAMPLE.COM)s/.*/principal5_user/\n" +
              "DEFAULT",
          configs.get("explicit_multiple_lines"));
      assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\\\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\\\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\\\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\\\n" +
              "RULE:[2:$1@$0](principal4@EXAMPLE.COM)s/.*/principal4_user/\\\n" +
              "RULE:[2:$1@$0](principal5@EXAMPLE.COM)s/.*/principal5_user/\\\n" +
              "DEFAULT",
          configs.get("explicit_multiple_lines_escaped"));
      assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*// " +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/ " +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/ " +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/ " +
              "RULE:[2:$1@$0](principal4@EXAMPLE.COM)s/.*/principal4_user/ " +
              "RULE:[2:$1@$0](principal5@EXAMPLE.COM)s/.*/principal5_user/ " +
              "DEFAULT",
          configs.get("explicit_single_line"));
    } else {
      assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\n" +
              "DEFAULT",
          configs.get("default"));
      assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\n" +
              "DEFAULT",
          configs.get("explicit_multiple_lines"));
      assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\\\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\\\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\\\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\\\n" +
              "DEFAULT",
          configs.get("explicit_multiple_lines_escaped"));
      assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*// " +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/ " +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/ " +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/ " +
              "DEFAULT",
          configs.get("explicit_single_line"));
    }

    configs = kerberosConfigurations.get("service-site");
    assertNotNull(configs);
    if (includePreconfiguredServices) {
      assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\n" +
              "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\n" +
              "RULE:[2:$1@$0](principal4@EXAMPLE.COM)s/.*/principal4_user/\n" +
              "RULE:[2:$1@$0](principal5@EXAMPLE.COM)s/.*/principal5_user/\n" +
              "DEFAULT",
          configs.get("default"));
      assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\n" +
              "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\n" +
              "RULE:[2:$1@$0](principal4@EXAMPLE.COM)s/.*/principal4_user/\n" +
              "RULE:[2:$1@$0](principal5@EXAMPLE.COM)s/.*/principal5_user/\n" +
              "DEFAULT",
          configs.get("explicit_multiple_lines"));
      assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\\\n" +
              "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\\\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\\\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\\\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\\\n" +
              "RULE:[2:$1@$0](principal4@EXAMPLE.COM)s/.*/principal4_user/\\\n" +
              "RULE:[2:$1@$0](principal5@EXAMPLE.COM)s/.*/principal5_user/\\\n" +
              "DEFAULT",
          configs.get("explicit_multiple_lines_escaped"));
      assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/ " +
              "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*// " +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/ " +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/ " +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/ " +
              "RULE:[2:$1@$0](principal4@EXAMPLE.COM)s/.*/principal4_user/ " +
              "RULE:[2:$1@$0](principal5@EXAMPLE.COM)s/.*/principal5_user/ " +
              "DEFAULT",
          configs.get("explicit_single_line"));
    } else {
      assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\n" +
              "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\n" +
              "DEFAULT",
          configs.get("default"));
      assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\n" +
              "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\n" +
              "DEFAULT",
          configs.get("explicit_multiple_lines"));
      assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\\\n" +
              "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\\\n" +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\\\n" +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\\\n" +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/\\\n" +
              "DEFAULT",
          configs.get("explicit_multiple_lines_escaped"));
      assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/ " +
              "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*// " +
              "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/ " +
              "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/ " +
              "RULE:[2:$1@$0](principal3@EXAMPLE.COM)s/.*/principal3_user/ " +
              "DEFAULT",
          configs.get("explicit_single_line"));
    }
  }

  private PropertyInfo createMockPropertyInfo(String filename, String propertyName, String value) {
    PropertyInfo propertyInfo = createMock(PropertyInfo.class);
    expect(propertyInfo.getFilename()).andReturn(filename).anyTimes();
    expect(propertyInfo.getName()).andReturn(propertyName).anyTimes();
    expect(propertyInfo.getValue()).andReturn(value).anyTimes();
    return propertyInfo;
  }

  @Test
  public void testSettingAuthToLocalRulesForUninstalledServiceComponents() throws Exception {
    final KerberosPrincipalDescriptor principalDescriptor1 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor1.getValue()).andReturn("principal1/host1@EXAMPLE.COM").times(2);
    expect(principalDescriptor1.getLocalUsername()).andReturn("principal1_user").times(2);

    final KerberosPrincipalDescriptor principalDescriptor2 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor2.getValue()).andReturn("principal2/host2@EXAMPLE.COM").times(1);
    expect(principalDescriptor2.getLocalUsername()).andReturn("principal2_user").times(1);

    final KerberosIdentityDescriptor identityDescriptor1 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor1.getPrincipalDescriptor()).andReturn(principalDescriptor1).times(2);
    expect(identityDescriptor1.shouldInclude(EasyMock.anyObject())).andReturn(true).anyTimes();

    final KerberosIdentityDescriptor identityDescriptor2 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor2.getPrincipalDescriptor()).andReturn(principalDescriptor2).times(1);
    expect(identityDescriptor2.shouldInclude(EasyMock.anyObject())).andReturn(true).anyTimes();

    final KerberosComponentDescriptor componentDescriptor1 = createMockComponentDescriptor("COMPONENT1", Collections.singletonList(identityDescriptor1), null); //only this is installed
    final KerberosComponentDescriptor componentDescriptor2 = createMockComponentDescriptor("COMPONENT2", Collections.singletonList(identityDescriptor2), null);

    final KerberosServiceDescriptor serviceDescriptor1 = createMock(KerberosServiceDescriptor.class);
    expect(serviceDescriptor1.getName()).andReturn("SERVICE1").anyTimes();
    expect(serviceDescriptor1.getIdentities(eq(true), EasyMock.anyObject())).andReturn(Arrays.asList(identityDescriptor1)).times(1);
    final Map<String, KerberosComponentDescriptor> kerberosComponents = new HashMap<>();
    kerberosComponents.put("COMPONENT1", componentDescriptor1);
    kerberosComponents.put("COMPONENT2", componentDescriptor2);
    expect(serviceDescriptor1.getComponents()).andReturn(kerberosComponents).times(1);
    expect(serviceDescriptor1.getAuthToLocalProperties()).andReturn(new HashSet<>(Arrays.asList(
        "default",
        "explicit_multiple_lines|new_lines",
        "explicit_multiple_lines_escaped|new_lines_escaped",
        "explicit_single_line|spaces",
        "service-site/default",
        "service-site/explicit_multiple_lines|new_lines",
        "service-site/explicit_multiple_lines_escaped|new_lines_escaped",
        "service-site/explicit_single_line|spaces"
    ))).times(1);

    final Map<String, KerberosServiceDescriptor> serviceDescriptorMap = new HashMap<>();
    serviceDescriptorMap.put("SERVICE1", serviceDescriptor1);

    final Service service1 = createMockService("SERVICE1", new HashMap<>());

    final Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("SERVICE1", service1);

    final Map<String, String> serviceSiteProperties = new HashMap<>();
    serviceSiteProperties.put("default", "RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\nDEFAULT");
    serviceSiteProperties.put("explicit_multiple_lines", "RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\nDEFAULT");
    serviceSiteProperties.put("explicit_multiple_lines_escaped", "RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\\\nDEFAULT");
    serviceSiteProperties.put("explicit_single_line", "RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/ DEFAULT");

    final Map<String, Map<String, String>> existingConfigs = new HashMap<>();
    existingConfigs.put("kerberos-env", new HashMap<String, String>());
    existingConfigs.get("kerberos-env").put(KerberosHelper.INCLUDE_ALL_COMPONENTS_IN_AUTH_TO_LOCAL_RULES, "true");
    existingConfigs.put("service-site", serviceSiteProperties);

    final KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getProperty("additional_realms")).andReturn(null).times(1);
    expect(kerberosDescriptor.getIdentities(eq(true), EasyMock.anyObject())).andReturn(null).times(1);
    expect(kerberosDescriptor.getAuthToLocalProperties()).andReturn(null).times(1);
    expect(kerberosDescriptor.getServices()).andReturn(serviceDescriptorMap).times(1);

    final Cluster cluster = createMockCluster("c1", Collections.<Host>emptyList(), SecurityType.KERBEROS, null, null);
    final Map<String, Set<String>> installedServices = Collections.singletonMap("SERVICE1", Collections.singleton("COMPONENT1"));
    final Map<String, Map<String, String>> kerberosConfigurations = new HashMap<>();

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    injector.getInstance(KerberosHelper.class).setAuthToLocalRules(cluster, kerberosDescriptor, "EXAMPLE.COM", installedServices, existingConfigs, kerberosConfigurations, false);

    verifyAll();

    Map<String, String> configs = kerberosConfigurations.get("");
    assertNotNull(configs);

    //asserts that the rules contain COMPONENT2 related rules too (with principal2) even if COMPONENT2 is not installed (see installedServices declaration above)
    assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
            "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
            "DEFAULT",
        configs.get("default"));
    assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
            "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
            "DEFAULT",
        configs.get("explicit_multiple_lines"));
    assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\\\n" +
            "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\\\n" +
            "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\\\n" +
            "DEFAULT",
        configs.get("explicit_multiple_lines_escaped"));
    assertEquals("RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*// " +
            "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/ " +
            "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/ " +
            "DEFAULT",
        configs.get("explicit_single_line"));

  configs = kerberosConfigurations.get("service-site");
  assertNotNull(configs);

  assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\n" +
          "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
          "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
          "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
          "DEFAULT",
      configs.get("default"));
  assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\n" +
          "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
          "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\n" +
          "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\n" +
          "DEFAULT",
      configs.get("explicit_multiple_lines"));
  assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/\\\n" +
          "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\\\n" +
          "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/\\\n" +
          "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/\\\n" +
          "DEFAULT",
      configs.get("explicit_multiple_lines_escaped"));
  assertEquals("RULE:[1:$1@$0](service_site@EXAMPLE.COM)s/.*/service_user/ " +
          "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*// " +
          "RULE:[2:$1@$0](principal1@EXAMPLE.COM)s/.*/principal1_user/ " +
          "RULE:[2:$1@$0](principal2@EXAMPLE.COM)s/.*/principal2_user/ " +
          "DEFAULT",
      configs.get("explicit_single_line"));
    }


  @Test
  public void testMergeConfigurationsForPreconfiguring() throws Exception {
    Service existingService = createMockService("EXISTING_SERVICE", null);

    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("EXISTING_SERVICE");
    serviceNames.add("PRECONFIGURE_SERVICE");

    Map<String, Set<String>> hostMap = new HashMap<>();

    Map<String, Service> services = new HashMap<>();

    Cluster cluster = createMockCluster("c1", Collections.<Host>emptyList(), SecurityType.KERBEROS, null, null);
    expect(cluster.getServices()).andReturn(services).times(2);
    expect(cluster.getServiceComponentHostMap(null, serviceNames)).andReturn(hostMap).once();

    KerberosDescriptor kerberosDescriptor = createKerberosDescriptor();

    ComponentInfo preconfigureComponentInfo = createMock(ComponentInfo.class);
    expect(preconfigureComponentInfo.getName()).andReturn("PRECONFIGURE_SERVICE_MASTER").once();

    List<PropertyInfo> preconfigureServiceProperties = Collections.singletonList(createMockPropertyInfo("preconfigure-service-env.xml", "service_user", "preconfigure_user"));

    ServiceInfo preconfigureServiceInfo = createMock(ServiceInfo.class);
    expect(preconfigureServiceInfo.getProperties()).andReturn(preconfigureServiceProperties).anyTimes();
    expect(preconfigureServiceInfo.getComponents()).andReturn(Collections.singletonList(preconfigureComponentInfo)).anyTimes();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    expect(ambariMetaInfo.isValidService("HDP", "2.2", "PRECONFIGURE_SERVICE")).andReturn(true).anyTimes();
    expect(ambariMetaInfo.getService("HDP", "2.2", "PRECONFIGURE_SERVICE")).andReturn(preconfigureServiceInfo).anyTimes();

    Set<Map<String, String>> host1Components = new HashSet<>();
    host1Components.add(Collections.singletonMap("name", "EXISTING_SERVICE_MASTER"));
    host1Components.add(Collections.singletonMap("name", "PRECONFIGURE_SERVICE_MASTER"));

    Set<Map<String, String>> host2Components = new HashSet<>();
    host2Components.add(Collections.singletonMap("name", "EXISTING_SERVICE_MASTER"));

    RecommendationResponse.HostGroup hostGroup1 = createMock(RecommendationResponse.HostGroup.class);
    expect(hostGroup1.getName()).andReturn("host1").once();
    expect(hostGroup1.getComponents()).andReturn(host1Components).once();

    RecommendationResponse.HostGroup hostGroup2 = createMock(RecommendationResponse.HostGroup.class);
    expect(hostGroup2.getName()).andReturn("host2").once();
    expect(hostGroup2.getComponents()).andReturn(host2Components).once();

    Set<RecommendationResponse.HostGroup> hostGroups = new HashSet<>();
    hostGroups.add(hostGroup1);
    hostGroups.add(hostGroup2);

    RecommendationResponse.Blueprint blueprint = createMock(RecommendationResponse.Blueprint.class);
    expect(blueprint.getHostGroups()).andReturn(hostGroups).once();

    RecommendationResponse.BindingHostGroup bindHostGroup1 = createMock(RecommendationResponse.BindingHostGroup.class);
    expect(bindHostGroup1.getName()).andReturn("host1").once();
    expect(bindHostGroup1.getHosts()).andReturn(Collections.singleton(Collections.singletonMap("fqdn", "host1"))).once();

    RecommendationResponse.BindingHostGroup bindHostGroup2 = createMock(RecommendationResponse.BindingHostGroup.class);
    expect(bindHostGroup2.getName()).andReturn("host2").once();
    expect(bindHostGroup2.getHosts()).andReturn(Collections.singleton(Collections.singletonMap("fqdn", "host2"))).once();

    Set<RecommendationResponse.BindingHostGroup> bindingHostGroups = new HashSet<>();
    bindingHostGroups.add(bindHostGroup1);
    bindingHostGroups.add(bindHostGroup2);

    RecommendationResponse.BlueprintClusterBinding binding = createMock(RecommendationResponse.BlueprintClusterBinding.class);
    expect(binding.getHostGroups()).andReturn(bindingHostGroups).once();

    RecommendationResponse.Recommendation recommendation = createMock(RecommendationResponse.Recommendation.class);
    expect(recommendation.getBlueprint()).andReturn(blueprint).once();
    expect(recommendation.getBlueprintClusterBinding()).andReturn(binding).once();

    RecommendationResponse response = createMock(RecommendationResponse.class);
    expect(response.getRecommendations()).andReturn(recommendation).once();

    StackAdvisorHelper stackAdvisorHelper = injector.getInstance(StackAdvisorHelper.class);
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(response).once();

    replayAll();

    services.put(existingService.getName(), existingService);

    Map<String, Map<String, String>> existingConfigurations = new HashMap<>();
    existingConfigurations.put("core-site", new HashMap<>(Collections.singletonMap("core-property1", "original_value")));
    existingConfigurations.put("hadoop-env", new HashMap<>(Collections.singletonMap("proxyuser_group", "hadoop")));

    Map<String, Map<String, String>> replacements = new HashMap<>(existingConfigurations);

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    Map<String, Map<String, String>> configurations = kerberosHelper.processPreconfiguredServiceConfigurations(existingConfigurations, replacements, cluster, kerberosDescriptor);

    verifyAll();

    Assert.assertNotNull(configurations);
    Assert.assertEquals(2, configurations.size());
    Assert.assertNotNull(configurations.get("core-site"));
    Assert.assertNotNull(configurations.get("hadoop-env"));
    Assert.assertEquals("hadoop", configurations.get("core-site").get("hadoop.proxyuser.preconfigure_user.groups"));
    Assert.assertEquals("host1", configurations.get("core-site").get("hadoop.proxyuser.preconfigure_user.hosts"));

  }

  @Test
  public void testGetServiceConfigurationUpdates() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final Host hostA = createMockHost("hostA");
    final Host hostB = createMockHost("hostB");
    final Host hostC = createMockHost("hostC");

    Collection<Host> hosts = Arrays.asList(hostA, hostB, hostC);

    final Map<String, String> kerberosEnvProperties = new HashMap<String, String>() {
      {
        put(KerberosHelper.KDC_TYPE, "mit-kdc");
        put(KerberosHelper.DEFAULT_REALM, "FOOBAR.COM");
        put("case_insensitive_username_rules", "false");
        put(KerberosHelper.CREATE_AMBARI_PRINCIPAL, "false");
      }
    };

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).atLeastOnce();

    final Map<String, String> krb5ConfProperties = createMock(Map.class);

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).atLeastOnce();

    final KerberosPrincipalDescriptor principalDescriptor1 = createMockPrincipalDescriptor(
        "service1/_HOST@${realm}", KerberosPrincipalType.SERVICE, "service1user", "service1-site/service.kerberos.principal");
    final KerberosPrincipalDescriptor principalDescriptor1a = createMockPrincipalDescriptor(
        "component1a/_HOST@${realm}", KerberosPrincipalType.SERVICE, "service1user", "service1-site/component1a.kerberos.principal");
    final KerberosPrincipalDescriptor principalDescriptor1b = createMockPrincipalDescriptor(
        "component1b/_HOST@${realm}", KerberosPrincipalType.SERVICE, "service1user", "service1-site/component1b.kerberos.principal");
    final KerberosPrincipalDescriptor principalDescriptor2a = createMockPrincipalDescriptor(
        "component2a/_HOST@${realm}", KerberosPrincipalType.SERVICE, "service2user", "service2-site/component2a.kerberos.principal");
    final KerberosPrincipalDescriptor principalDescriptor2b = createMockPrincipalDescriptor(
        "component2b/_HOST@${realm}", KerberosPrincipalType.SERVICE, "service2user", "service2-site/component2b.kerberos.principal");
    final KerberosPrincipalDescriptor principalDescriptor3a = createMockPrincipalDescriptor(
        "component3a/_HOST@${realm}", KerberosPrincipalType.SERVICE, "service3user", "service3-site/component3a.kerberos.principal");

    final KerberosKeytabDescriptor keytabDescriptor1 = createMockKeytabDescriptor(
        "keytab1", "service1-site/service.kerberos.keytab");
    final KerberosKeytabDescriptor keytabDescriptor1a = createMockKeytabDescriptor(
        "keytab1a", "service1-site/component1a.kerberos.keytab");
    final KerberosKeytabDescriptor keytabDescriptor1b = createMockKeytabDescriptor(
        "keytab1b", "service1-site/component1b.kerberos.keytab");
    final KerberosKeytabDescriptor keytabDescriptor2a = createMockKeytabDescriptor(
        "keytab2a", "service2-site/component2a.kerberos.keytab");
    final KerberosKeytabDescriptor keytabDescriptor2b = createMockKeytabDescriptor(
        "keytab2b", "service2-site/component2b.kerberos.keytab");
    final KerberosKeytabDescriptor keytabDescriptor3a = createMockKeytabDescriptor(
        "keytab3a", "service3-site/component3a.kerberos.keytab");

    final KerberosIdentityDescriptor identityDescriptor1 = createMockIdentityDescriptor(
        "identity1", principalDescriptor1, keytabDescriptor1);
    final KerberosIdentityDescriptor identityDescriptor1a = createMockIdentityDescriptor(
        "identity1a", principalDescriptor1a, keytabDescriptor1a);
    final KerberosIdentityDescriptor identityDescriptor1b = createMockIdentityDescriptor(
        "identity1b", principalDescriptor1b, keytabDescriptor1b);
    final KerberosIdentityDescriptor identityDescriptor2a = createMockIdentityDescriptor(
        "identity2a", principalDescriptor2a, keytabDescriptor2a);
    final KerberosIdentityDescriptor identityDescriptor2b = createMockIdentityDescriptor(
        "identity2b", principalDescriptor2b, keytabDescriptor2b);
    final KerberosIdentityDescriptor identityDescriptor3a = createMockIdentityDescriptor(
        "identity3a", principalDescriptor3a, keytabDescriptor3a);

    final KerberosComponentDescriptor componentDescriptor1a = createMockComponentDescriptor(
        "COMPONENT1A",
        new ArrayList<KerberosIdentityDescriptor>() {
          {
            add(identityDescriptor1a);
          }
        },
        new HashMap<String, KerberosConfigurationDescriptor>() {
          {
            put("service1-site", createMockConfigurationDescriptor(
                Collections.singletonMap("component1a.property", "${replacement1}")
            ));
          }
        });
    final KerberosComponentDescriptor componentDescriptor1b = createMockComponentDescriptor(
        "COMPONENT1B",
        new ArrayList<KerberosIdentityDescriptor>() {
          {
            add(identityDescriptor1b);
          }
        },
        new HashMap<String, KerberosConfigurationDescriptor>() {
          {
            put("service1-site", createMockConfigurationDescriptor(
                Collections.singletonMap("component1b.property", "${type1/replacement1}")
            ));
          }
        });
    final KerberosComponentDescriptor componentDescriptor2a = createMockComponentDescriptor(
        "COMPONENT2A",
        new ArrayList<KerberosIdentityDescriptor>() {
          {
            add(identityDescriptor2a);
          }
        },
        new HashMap<String, KerberosConfigurationDescriptor>() {
          {
            put("service2-site", createMockConfigurationDescriptor(
                Collections.singletonMap("component2a.property", "${type1/replacement2}")
            ));
          }
        });
    final KerberosComponentDescriptor componentDescriptor2b = createMockComponentDescriptor(
        "COMPONENT2B",
        new ArrayList<KerberosIdentityDescriptor>() {
          {
            add(identityDescriptor2b);
          }
        },
        new HashMap<String, KerberosConfigurationDescriptor>() {
          {
            put("service2-site", createMockConfigurationDescriptor(
                Collections.singletonMap("component2b.property", "${type2/replacement1}")
            ));
          }
        });
    final KerberosComponentDescriptor componentDescriptor3a = createMockComponentDescriptor(
        "COMPONENT3A",
        new ArrayList<KerberosIdentityDescriptor>() {
          {
            add(identityDescriptor3a);
          }
        },
        new HashMap<String, KerberosConfigurationDescriptor>() {
          {
            put("service3-site", createMockConfigurationDescriptor(
                Collections.singletonMap("component3a.property", "${type3/replacement1}")
            ));
            put("core-site", createMockConfigurationDescriptor(
                Collections.singletonMap("component3b.property", "${type3/replacement2}")
            ));
          }
        });

    final KerberosServiceDescriptor serviceDescriptor1 = createMockServiceDescriptor(
        "SERVICE1",
        new HashMap<String, KerberosComponentDescriptor>() {
          {
            put("COMPONENT1A", componentDescriptor1a);
            put("COMPONENT1B", componentDescriptor1b);
          }
        },
        new ArrayList<KerberosIdentityDescriptor>() {
          {
            add(identityDescriptor1);
          }
        },
        false);
    expect(serviceDescriptor1.getComponent("COMPONENT1A")).andReturn(componentDescriptor1a).times(2);
    expect(serviceDescriptor1.getComponent("COMPONENT1B")).andReturn(componentDescriptor1b).times(2);

    final KerberosServiceDescriptor serviceDescriptor2 = createMockServiceDescriptor(
        "SERVICE2",
        new HashMap<String, KerberosComponentDescriptor>() {
          {
            put("COMPONENT2A", componentDescriptor2a);
            put("COMPONENT2B", componentDescriptor2b);
          }
        },
        Collections.<KerberosIdentityDescriptor>emptyList(),
        false);
    expect(serviceDescriptor2.getComponent("COMPONENT2A")).andReturn(componentDescriptor2a).times(1);
    expect(serviceDescriptor2.getComponent("COMPONENT2B")).andReturn(componentDescriptor2b).times(1);

    final KerberosServiceDescriptor serviceDescriptor3 = createMockServiceDescriptor(
        "SERVICE3",
        new HashMap<String, KerberosComponentDescriptor>() {
          {
            put("COMPONENT3A", componentDescriptor3a);
          }
        },
        Collections.<KerberosIdentityDescriptor>emptyList(), false);
    expect(serviceDescriptor3.getComponent("COMPONENT3A")).andReturn(componentDescriptor3a).times(2);

    Map<String, KerberosServiceDescriptor> serviceDescriptorMap = new HashMap<>();
    serviceDescriptorMap.put("SERVICE1", serviceDescriptor1);
    serviceDescriptorMap.put("SERVICE2", serviceDescriptor2);
    serviceDescriptorMap.put("SERVICE3", serviceDescriptor3);

    final Map<String, String> kerberosDescriptorProperties = new HashMap<>();
    kerberosDescriptorProperties.put(KerberosHelper.DEFAULT_REALM, "${kerberos-env/realm}");

    final KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getProperties()).andReturn(kerberosDescriptorProperties).atLeastOnce();
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).atLeastOnce();
    expect(kerberosDescriptor.getService("SERVICE2")).andReturn(serviceDescriptor2).atLeastOnce();
    expect(kerberosDescriptor.getService("SERVICE3")).andReturn(serviceDescriptor3).atLeastOnce();
    expect(kerberosDescriptor.getServices()).andReturn(serviceDescriptorMap).atLeastOnce();
    expect(kerberosDescriptor.getProperty("additional_realms")).andReturn(null).atLeastOnce();
    expect(kerberosDescriptor.getIdentities(eq(true), EasyMock.anyObject())).andReturn(null).atLeastOnce();
    expect(kerberosDescriptor.getAuthToLocalProperties()).andReturn(Collections.singleton("core-site/auth.to.local")).atLeastOnce();

    setupKerberosDescriptor(kerberosDescriptor);

    RecommendationResponse.BlueprintConfigurations coreSiteRecommendation = createNiceMock(RecommendationResponse
        .BlueprintConfigurations.class);
    expect(coreSiteRecommendation.getProperties()).andReturn(Collections.singletonMap("newPropertyRecommendation", "newPropertyRecommendation"));

    RecommendationResponse.BlueprintConfigurations newTypeRecommendation = createNiceMock(RecommendationResponse.BlueprintConfigurations.class);
    expect(newTypeRecommendation.getProperties()).andReturn(Collections.singletonMap("newTypeRecommendation", "newTypeRecommendation"));

    RecommendationResponse.BlueprintConfigurations type1Recommendation = createNiceMock(RecommendationResponse.BlueprintConfigurations.class);
    expect(type1Recommendation.getProperties()).andReturn(Collections.singletonMap("replacement1", "not replaced"));

    RecommendationResponse.BlueprintConfigurations service1SiteRecommendation = createNiceMock(RecommendationResponse.BlueprintConfigurations.class);
    expect(service1SiteRecommendation.getProperties()).andReturn(Collections.singletonMap("component1b.property", "replaced value"));

    Map<String, RecommendationResponse.BlueprintConfigurations> configurations = new HashMap<>();
    configurations.put("core-site", coreSiteRecommendation);
    configurations.put("new-type", newTypeRecommendation);
    configurations.put("type1", type1Recommendation);
    configurations.put("service1-site", service1SiteRecommendation);

    RecommendationResponse.Blueprint blueprint = createMock(RecommendationResponse.Blueprint.class);
    expect(blueprint.getConfigurations()).andReturn(configurations).once();

    RecommendationResponse.Recommendation recommendations = createMock(RecommendationResponse.Recommendation.class);
    expect(recommendations.getBlueprint()).andReturn(blueprint).once();

    RecommendationResponse recommendationResponse = createMock(RecommendationResponse.class);
    expect(recommendationResponse.getRecommendations()).andReturn(recommendations).once();

    StackAdvisorHelper stackAdvisorHelper = injector.getInstance(StackAdvisorHelper.class);
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(null).once();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(recommendationResponse).once();

    final Service service1 = createMockService("SERVICE1",
        new HashMap<String, ServiceComponent>() {
          {
            put("COMPONENT1A", createMockComponent("COMPONENT1A", true,
                new HashMap<String, ServiceComponentHost>() {
                  {
                    put("hostA", createMockServiceComponentHost(State.INSTALLED));
                  }
                }));
            put("COMPONENT1B", createMockComponent("COMPONENT1B", false,
                new HashMap<String, ServiceComponentHost>() {
                  {
                    put("hostB", createMockServiceComponentHost(State.INSTALLED));
                    put("hostC", createMockServiceComponentHost(State.INSTALLED));
                  }
                }));
          }
        });
    final Service service2 = createMockService("SERVICE2",
        new HashMap<String, ServiceComponent>() {
          {
            put("COMPONENT2A", createMockComponent("COMPONENT2A", true,
                new HashMap<String, ServiceComponentHost>() {
                  {
                    put("hostA", createMockServiceComponentHost(State.INSTALLED));
                  }
                }));
            put("COMPONENT2B", createMockComponent("COMPONENT2B", false,
                new HashMap<String, ServiceComponentHost>() {
                  {
                    put("hostB", createMockServiceComponentHost(State.INSTALLED));
                    put("hostC", createMockServiceComponentHost(State.INSTALLED));
                  }
                }));
          }
        });
    final Service service3 = createMockService("SERVICE3",
        new HashMap<String, ServiceComponent>() {
          {
            put("COMPONENT3A", createMockComponent("COMPONENT3A", true,
                new HashMap<String, ServiceComponentHost>() {
                  {
                    put("hostA", createMockServiceComponentHost(State.INSTALLED));
                  }
                }));
          }
        });

    Map<String, Service> services = new HashMap<>();
    services.put("SERVICE1", service1);
    services.put("SERVICE2", service2);
    services.put("SERVICE3", service3);

    Map<String, Set<String>> serviceComponentHostMap = new HashMap<>();
    serviceComponentHostMap.put("COMPONENT1A", new TreeSet<>(Arrays.asList("hostA")));
    serviceComponentHostMap.put("COMPONENT1B", new TreeSet<>(Arrays.asList("hostB", "hostC")));
    serviceComponentHostMap.put("COMPONENT2A", new TreeSet<>(Arrays.asList("hostA")));
    serviceComponentHostMap.put("COMPONENT2B", new TreeSet<>(Arrays.asList("hostB", "hostC")));
    serviceComponentHostMap.put("COMPONEN3A", new TreeSet<>(Arrays.asList("hostA")));

    final Cluster cluster = createMockCluster("c1", hosts, SecurityType.KERBEROS, krb5ConfConfig, kerberosEnvConfig);
    expect(cluster.getServices()).andReturn(services).anyTimes();
    expect(cluster.getServiceComponentHostMap(EasyMock.anyObject(), EasyMock.anyObject())).andReturn(serviceComponentHostMap).anyTimes();

    final Map<String, Map<String, String>> existingConfigurations = new HashMap<String, Map<String, String>>() {
      {
        put("kerberos-env", kerberosEnvProperties);
        put("", new HashMap<String, String>() {
          {
            put("replacement1", "value1");
          }
        });
        put("type1", new HashMap<String, String>() {
          {
            put("replacement1", "value2");
            put("replacement2", "value3");
          }
        });
        put("type2", new HashMap<String, String>() {
          {
            put("replacement1", "value4");
            put("replacement2", "value5");
          }
        });
        put("type3", new HashMap<String, String>() {
          {
            put("replacement1", "value6");
            put("replacement2", "value7");
          }
        });
      }
    };

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    HashMap<String, Set<String>> installedServices1 = new HashMap<>();
    installedServices1.put("SERVICE1", new HashSet<>(Arrays.asList("COMPONENT1A", "COMPONENT1B")));
    installedServices1.put("SERVICE2", new HashSet<>(Arrays.asList("COMPONENT2A", "COMPONENT2B")));
    installedServices1.put("SERVICE3", Collections.singleton("COMPONENT3A"));

    Map<String, Map<String, String>> updates1 = kerberosHelper.getServiceConfigurationUpdates(
        cluster, existingConfigurations, installedServices1, null, null, true, true);

    HashMap<String, Set<String>> installedServices2 = new HashMap<>();
    installedServices2.put("SERVICE1", new HashSet<>(Arrays.asList("COMPONENT1A", "COMPONENT1B")));
    installedServices2.put("SERVICE3", Collections.singleton("COMPONENT3A"));

    Map<String, Collection<String>> serviceFilter2 = new HashMap<>();
    serviceFilter2.put("SERVICE1", new HashSet<>(Arrays.asList("COMPONENT1A", "COMPONENT1B")));
    serviceFilter2.put("SERVICE3", Collections.singleton("COMPONENT3A"));

    Map<String, Map<String, String>> updates2 = kerberosHelper.getServiceConfigurationUpdates(
        cluster, existingConfigurations, installedServices2, serviceFilter2, null, true, true);

    verifyAll();

    Map<String, Map<String, String>> expectedUpdates = new HashMap<String, Map<String, String>>() {{
      put("service1-site", new HashMap<String, String>() {
        {
          put("service.kerberos.principal", "service1/_HOST@FOOBAR.COM");
          put("service.kerberos.keytab", "keytab1");
          put("component1a.kerberos.principal", "component1a/_HOST@FOOBAR.COM");
          put("component1a.kerberos.keytab", "keytab1a");
          put("component1a.property", "value1");
          put("component1b.kerberos.principal", "component1b/_HOST@FOOBAR.COM");
          put("component1b.kerberos.keytab", "keytab1b");
          put("component1b.property", "value2");
        }
      });
      put("service2-site", new HashMap<String, String>() {
        {
          put("component2a.kerberos.principal", "component2a/_HOST@FOOBAR.COM");
          put("component2a.kerberos.keytab", "keytab2a");
          put("component2a.property", "value3");
          put("component2b.kerberos.principal", "component2b/_HOST@FOOBAR.COM");
          put("component2b.kerberos.keytab", "keytab2b");
          put("component2b.property", "value4");
        }
      });
      put("service3-site", new HashMap<String, String>() {
        {
          put("component3a.kerberos.principal", "component3a/_HOST@FOOBAR.COM");
          put("component3a.kerberos.keytab", "keytab3a");
          put("component3a.property", "value6");
        }
      });
      put("core-site", new HashMap<String, String>() {
        {
          put("auth.to.local", "RULE:[1:$1@$0](.*@FOOBAR.COM)s/@.*//\n" +
              "RULE:[2:$1@$0](component1a@FOOBAR.COM)s/.*/service1user/\n" +
              "RULE:[2:$1@$0](component1b@FOOBAR.COM)s/.*/service1user/\n" +
              "RULE:[2:$1@$0](component2a@FOOBAR.COM)s/.*/service2user/\n" +
              "RULE:[2:$1@$0](component2b@FOOBAR.COM)s/.*/service2user/\n" +
              "RULE:[2:$1@$0](component3a@FOOBAR.COM)s/.*/service3user/\n" +
              "RULE:[2:$1@$0](service1@FOOBAR.COM)s/.*/service1user/\n" +
              "DEFAULT");
          put("component3b.property", "value7");
        }
      });
    }};

    assertEquals(expectedUpdates, updates1);

    expectedUpdates.remove("service2-site");
    expectedUpdates.get("core-site").put("newPropertyRecommendation", "newPropertyRecommendation");
    expectedUpdates.get("core-site").put("auth.to.local", "RULE:[1:$1@$0](.*@FOOBAR.COM)s/@.*//\n" +
        "RULE:[2:$1@$0](component1a@FOOBAR.COM)s/.*/service1user/\n" +
        "RULE:[2:$1@$0](component1b@FOOBAR.COM)s/.*/service1user/\n" +
        "RULE:[2:$1@$0](component3a@FOOBAR.COM)s/.*/service3user/\n" +
        "RULE:[2:$1@$0](service1@FOOBAR.COM)s/.*/service1user/\n" +
        "DEFAULT");
    expectedUpdates.get("service1-site").put("component1b.property", "replaced value");
    expectedUpdates.put("new-type", new HashMap<String, String>() {
      {
        put("newTypeRecommendation", "newTypeRecommendation");
      }
    });

    assertEquals(expectedUpdates, updates2);

    // Make sure the existing configurations remained unchanged
    Map<String, Map<String, String>> expectedExistingConfigurations = new HashMap<String, Map<String, String>>() {
      {
        put("kerberos-env", new HashMap<String, String>() {
          {
            put(KerberosHelper.KDC_TYPE, "mit-kdc");
            put(KerberosHelper.DEFAULT_REALM, "FOOBAR.COM");
            put("case_insensitive_username_rules", "false");
            put(KerberosHelper.CREATE_AMBARI_PRINCIPAL, "false");
          }
        });
        put("", new HashMap<String, String>() {
          {
            put("replacement1", "value1");
          }
        });
        put("type1", new HashMap<String, String>() {
          {
            put("replacement1", "value2");
            put("replacement2", "value3");
          }
        });
        put("type2", new HashMap<String, String>() {
          {
            put("replacement1", "value4");
            put("replacement2", "value5");
          }
        });
        put("type3", new HashMap<String, String>() {
          {
            put("replacement1", "value6");
            put("replacement2", "value7");
          }
        });
      }
    };

    assertEquals(expectedExistingConfigurations, existingConfigurations);
  }

  @Test
  public void testEnsureHeadlessIdentities() throws Exception {
    testEnsureHeadlessIdentities(false, false);
  }

  @Test
  public void testEnsureHeadlessAndAmbariIdentitiesAsUser() throws Exception {
    testEnsureHeadlessIdentities(true, false);
  }

  @Test
  public void testEnsureHeadlessAndAmbariIdentitiesAsService() throws Exception {
    testEnsureHeadlessIdentities(true, true);
  }

  private void testEnsureHeadlessIdentities(boolean createAmbariIdentities, boolean ambariServerPrincipalAsService) throws Exception {
    String clusterName = "c1";
    String realm = "EXAMPLE.COM";
    String ambariServerHostname = StageUtils.getHostName();
    String ambariServerPrincipalName;
    String ambariServerKeytabFilePath = new File("ambari.server.keytab").getAbsolutePath();
    KerberosPrincipalType ambariServerPrincipalType;
    String ambariServerPrincipalNameExpected;

    if (ambariServerPrincipalAsService) {
      ambariServerPrincipalName = "ambari-server${principal_suffix}/_HOST@${realm}";
      ambariServerPrincipalType = KerberosPrincipalType.SERVICE;
      ambariServerPrincipalNameExpected = String.format("ambari-server-%s/%s@%s", clusterName, ambariServerHostname, realm);
    } else {
      ambariServerPrincipalName = "ambari-server${principal_suffix}@${realm}";
      ambariServerPrincipalType = KerberosPrincipalType.USER;
      ambariServerPrincipalNameExpected = String.format("ambari-server-%s@%s", clusterName, realm);
    }

    Map<String, String> propertiesKrb5Conf = new HashMap<>();

    Map<String, String> propertiesKerberosEnv = new HashMap<>();
    propertiesKerberosEnv.put(KerberosHelper.DEFAULT_REALM, realm);
    propertiesKerberosEnv.put(KerberosHelper.KDC_TYPE, "mit-kdc");
    propertiesKerberosEnv.put("password_length", "20");
    propertiesKerberosEnv.put("password_min_lowercase_letters", "1");
    propertiesKerberosEnv.put("password_min_uppercase_letters", "1");
    propertiesKerberosEnv.put("password_min_digits", "1");
    propertiesKerberosEnv.put("password_min_punctuation", "0");
    propertiesKerberosEnv.put("password_min_whitespace", "0");
    propertiesKerberosEnv.put(KerberosHelper.CREATE_AMBARI_PRINCIPAL, (createAmbariIdentities) ? "true" : "false");

    Config configKrb5Conf = createMock(Config.class);
    expect(configKrb5Conf.getProperties()).andReturn(propertiesKrb5Conf).times(1);

    Config configKerberosEnv = createMock(Config.class);
    expect(configKerberosEnv.getProperties()).andReturn(propertiesKerberosEnv).times(1);

    Host host1 = createMockHost("host1");
    Host host2 = createMockHost("host3");
    Host host3 = createMockHost("host2");

    Map<String, ServiceComponentHost> service1Component1HostMap = new HashMap<>();
    service1Component1HostMap.put("host1", createMockServiceComponentHost(State.INSTALLED));

    Map<String, ServiceComponentHost> service2Component1HostMap = new HashMap<>();
    service2Component1HostMap.put("host2", createMockServiceComponentHost(State.INSTALLED));

    Map<String, ServiceComponent> service1ComponentMap = new HashMap<>();
    service1ComponentMap.put("COMPONENT11", createMockComponent("COMPONENT11", true, service1Component1HostMap));

    Map<String, ServiceComponent> service2ComponentMap = new HashMap<>();
    service2ComponentMap.put("COMPONENT21", createMockComponent("COMPONENT21", true, service2Component1HostMap));

    Service service1 = createMockService("SERVICE1", service1ComponentMap);
    Service service2 = createMockService("SERVICE2", service2ComponentMap);

    Map<String, Service> servicesMap = new HashMap<>();
    servicesMap.put("SERVICE1", service1);
    servicesMap.put("SERVICE2", service2);

    Cluster cluster = createMockCluster(clusterName, Arrays.asList(host1, host2, host3), SecurityType.KERBEROS, configKrb5Conf, configKerberosEnv);
    expect(cluster.getServices()).andReturn(servicesMap).anyTimes();

    Map<String, String> kerberosDescriptorProperties = new HashMap<>();
    kerberosDescriptorProperties.put("additional_realms", "");
    kerberosDescriptorProperties.put("keytab_dir", "/etc/security/keytabs");
    kerberosDescriptorProperties.put(KerberosHelper.DEFAULT_REALM, "${kerberos-env/realm}");
    kerberosDescriptorProperties.put("principal_suffix", "-${cluster_name|toLower()}");

    ArrayList<KerberosIdentityDescriptor> service1Component1Identities = new ArrayList<>();
    service1Component1Identities.add(createMockIdentityDescriptor(
        "s1c1_1.user",
        createMockPrincipalDescriptor("s1c1_1@${realm}", KerberosPrincipalType.USER, "s1c1", null),
        createMockKeytabDescriptor("s1c1_1.user.keytab", null)
    ));
    service1Component1Identities.add(createMockIdentityDescriptor(
        "s1c1_1.service",
        createMockPrincipalDescriptor("s1c1_1/_HOST@${realm}", KerberosPrincipalType.SERVICE, "s1c1", null),
        createMockKeytabDescriptor("s1c1_1.service.keytab", null)
    ));

    HashMap<String, KerberosComponentDescriptor> service1ComponentDescriptorMap = new HashMap<>();
    service1ComponentDescriptorMap.put("COMPONENT11", createMockComponentDescriptor("COMPONENT11", service1Component1Identities, null));

    List<KerberosIdentityDescriptor> service1Identities = new ArrayList<>();
    service1Identities.add(createMockIdentityDescriptor(
        "s1_1.user",
        createMockPrincipalDescriptor("s1_1@${realm}", KerberosPrincipalType.USER, "s1", null),
        createMockKeytabDescriptor("s1_1.user.keytab", null)
    ));
    service1Identities.add(createMockIdentityDescriptor(
        "s1_1.service",
        createMockPrincipalDescriptor("s1/_HOST@${realm}", KerberosPrincipalType.SERVICE, "s1", null),
        createMockKeytabDescriptor("s1.service.keytab", null)
    ));

    KerberosServiceDescriptor service1KerberosDescriptor = createMockServiceDescriptor("SERVICE1", service1ComponentDescriptorMap, service1Identities, false);

    ArrayList<KerberosIdentityDescriptor> service2Component1Identities = new ArrayList<>();
    service2Component1Identities.add(createMockIdentityDescriptor(
        "s2_1.user",
        createMockPrincipalDescriptor("s2_1@${realm}", KerberosPrincipalType.USER, "s2", null),
        createMockKeytabDescriptor("s2_1.user.keytab", null)
    ));
    service2Component1Identities.add(createMockIdentityDescriptor(
        "s2c1_1.service",
        createMockPrincipalDescriptor("s2c1_1/_HOST@${realm}", KerberosPrincipalType.SERVICE, "s2c1", null),
        createMockKeytabDescriptor("s2c1_1.service.keytab", null)
    ));

    HashMap<String, KerberosComponentDescriptor> service2ComponentDescriptorMap = new HashMap<>();
    service2ComponentDescriptorMap.put("COMPONENT21", createMockComponentDescriptor("COMPONENT21", service2Component1Identities, null));

    KerberosServiceDescriptor service2KerberosDescriptor = createMockServiceDescriptor("SERVICE2", service2ComponentDescriptorMap, null, false);

    KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getProperties()).andReturn(kerberosDescriptorProperties);
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(service1KerberosDescriptor).times(1);
    expect(kerberosDescriptor.getService("SERVICE2")).andReturn(service2KerberosDescriptor).times(1);

    Capture<ResolvedKerberosPrincipal> spnegoPrincipalCapture = newCapture(CaptureType.LAST);
    Capture<ResolvedKerberosPrincipal> ambariPrincipalCapture = newCapture(CaptureType.LAST);
    String spnegoPrincipalNameExpected = String.format("HTTP/%s@%s", ambariServerHostname, realm);
    if (createAmbariIdentities) {
      ArrayList<KerberosIdentityDescriptor> ambarServerComponent1Identities = new ArrayList<>();
      ambarServerComponent1Identities.add(createMockIdentityDescriptor(
          KerberosHelper.AMBARI_SERVER_KERBEROS_IDENTITY_NAME,
          createMockPrincipalDescriptor(ambariServerPrincipalName, ambariServerPrincipalType, "ambari", null),
          createMockKeytabDescriptor(ambariServerKeytabFilePath, null)));

      ambarServerComponent1Identities.add(createMockIdentityDescriptor(
          "spnego",
          createMockPrincipalDescriptor("HTTP/_HOST@${realm}", KerberosPrincipalType.SERVICE, null, null),
          createMockKeytabDescriptor("spnego.service.keytab", null)));

      KerberosComponentDescriptor ambariServerComponentKerberosDescriptor = createMockComponentDescriptor(RootComponent.AMBARI_SERVER.name(), ambarServerComponent1Identities, null);

      HashMap<String, KerberosComponentDescriptor> ambariServerComponentDescriptorMap = new HashMap<>();
      ambariServerComponentDescriptorMap.put(RootComponent.AMBARI_SERVER.name(), ambariServerComponentKerberosDescriptor);

      KerberosServiceDescriptor ambariServiceKerberosDescriptor = createMockServiceDescriptor(RootService.AMBARI.name(), ambariServerComponentDescriptorMap, null, false);
      expect(ambariServiceKerberosDescriptor.getComponent(RootComponent.AMBARI_SERVER.name())).andReturn(ambariServerComponentKerberosDescriptor).once();

      expect(kerberosDescriptor.getService(RootService.AMBARI.name())).andReturn(ambariServiceKerberosDescriptor).once();

      ConfigureAmbariIdentitiesServerAction configureAmbariIdentitiesServerAction = injector.getInstance(ConfigureAmbariIdentitiesServerAction.class);

      expect(configureAmbariIdentitiesServerAction.installAmbariServerIdentity(capture(ambariPrincipalCapture), anyString(), eq(ambariServerKeytabFilePath),
          eq("user1"), eq("rw"), eq("groupA"), eq("r"), (ActionLog) eq(null)))
          .andReturn(true)
          .once();
      expect(configureAmbariIdentitiesServerAction.installAmbariServerIdentity(capture(spnegoPrincipalCapture), anyString(), eq("spnego.service.keytab"),
          eq("user1"), eq("rw"), eq("groupA"), eq("r"), (ActionLog) eq(null)))
          .andReturn(true)
          .once();

      configureAmbariIdentitiesServerAction.configureJAAS(ambariServerPrincipalNameExpected, ambariServerKeytabFilePath, null);
      expectLastCall().once();
    }

    setupKerberosDescriptor(kerberosDescriptor);

    Map<String, Map<String, String>> existingConfigurations = new HashMap<>();
    existingConfigurations.put("kerberos-env", propertiesKerberosEnv);

    Set<String> services = new HashSet<String>() {
      {
        add("SERVICE1");
        add("SERVICE2");
      }
    };

    Capture<? extends String> capturePrincipal = newCapture(CaptureType.ALL);
    Capture<? extends String> capturePrincipalForKeytab = newCapture(CaptureType.ALL);

    CreatePrincipalsServerAction createPrincipalsServerAction = injector.getInstance(CreatePrincipalsServerAction.class);
    expect(createPrincipalsServerAction.createPrincipal(capture(capturePrincipal), eq(false), EasyMock.anyObject(), anyObject(KerberosOperationHandler.class), eq(false), isNull(ActionLog.class)))
        .andReturn(new CreatePrincipalsServerAction.CreatePrincipalResult("anything", "password", 1))
        .times(3);

    if (createAmbariIdentities) {
      if (ambariServerPrincipalAsService) {
        expect(createPrincipalsServerAction.createPrincipal(capture(capturePrincipal), eq(true), EasyMock.anyObject(), anyObject(KerberosOperationHandler.class), eq(false), isNull(ActionLog.class)))
            .andReturn(new CreatePrincipalsServerAction.CreatePrincipalResult("anything", "password", 1))
            .times(2);
      } else {
        expect(createPrincipalsServerAction.createPrincipal(capture(capturePrincipal), eq(true), EasyMock.anyObject(), anyObject(KerberosOperationHandler.class), eq(false), isNull(ActionLog.class)))
            .andReturn(new CreatePrincipalsServerAction.CreatePrincipalResult("anything", "password", 1))
            .times(1);
        expect(createPrincipalsServerAction.createPrincipal(capture(capturePrincipal), eq(false), EasyMock.anyObject(), anyObject(KerberosOperationHandler.class), eq(false), isNull(ActionLog.class)))
            .andReturn(new CreatePrincipalsServerAction.CreatePrincipalResult("anything", "password", 1))
            .times(1);
      }
    }

    CreateKeytabFilesServerAction createKeytabFilesServerAction = injector.getInstance(CreateKeytabFilesServerAction.class);
    expect(createKeytabFilesServerAction.createKeytab(capture(capturePrincipalForKeytab), eq("password"), eq(1), anyObject(KerberosOperationHandler.class), eq(true), eq(true), isNull(ActionLog.class)))
        .andReturn(new Keytab())
        .times(createAmbariIdentities ? 5 : 3);

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    credentialStoreService.setCredential(cluster.getClusterName(), KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
        new PrincipalKeyCredential("principal", "password"), CredentialStoreType.TEMPORARY);

    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);
    kerberosHelper.ensureHeadlessIdentities(cluster, existingConfigurations, services);

    verifyAll();

    if (createAmbariIdentities) {
      assertEquals(ambariPrincipalCapture.getValue().getPrincipal(), ambariServerPrincipalNameExpected);
      assertEquals(spnegoPrincipalCapture.getValue().getPrincipal(), spnegoPrincipalNameExpected);
    }

    List<? extends String> capturedPrincipals = capturePrincipal.getValues();
    assertEquals(createAmbariIdentities ? 5 : 3, capturedPrincipals.size());
    assertTrue(capturedPrincipals.contains("s1_1@EXAMPLE.COM"));
    assertTrue(capturedPrincipals.contains("s1c1_1@EXAMPLE.COM"));
    assertTrue(capturedPrincipals.contains("s2_1@EXAMPLE.COM"));

    List<? extends String> capturedPrincipalsForKeytab = capturePrincipalForKeytab.getValues();
    assertEquals(createAmbariIdentities ? 5 : 3, capturedPrincipalsForKeytab.size());
    assertTrue(capturedPrincipalsForKeytab.contains("s1_1@EXAMPLE.COM"));
    assertTrue(capturedPrincipalsForKeytab.contains("s1c1_1@EXAMPLE.COM"));
    assertTrue(capturedPrincipalsForKeytab.contains("s2_1@EXAMPLE.COM"));

    if (createAmbariIdentities) {
      String spnegoPrincipalName = String.format("HTTP/%s@EXAMPLE.COM", ambariServerHostname);

      assertTrue(capturedPrincipals.contains(ambariServerPrincipalNameExpected));
      assertTrue(capturedPrincipals.contains(spnegoPrincipalName));

      assertTrue(capturedPrincipalsForKeytab.contains(ambariServerPrincipalNameExpected));
      assertTrue(capturedPrincipalsForKeytab.contains(spnegoPrincipalName));
    }
  }

  /**
   * Test that a Kerberos Descriptor that contains a Service with 0 Components does not raise any exceptions.
   */
  @Test
  public void testServiceWithoutComponents() throws Exception {
    Map<String, String> propertiesKrb5Conf = new HashMap<>();

    Map<String, String> propertiesKerberosEnv = new HashMap<>();
    propertiesKerberosEnv.put(KerberosHelper.DEFAULT_REALM, "EXAMPLE.COM");
    propertiesKerberosEnv.put(KerberosHelper.KDC_TYPE, "mit-kdc");
    propertiesKerberosEnv.put(KerberosHelper.CREATE_AMBARI_PRINCIPAL, "false");

    Config configKrb5Conf = createMock(Config.class);
    expect(configKrb5Conf.getProperties()).andReturn(propertiesKrb5Conf).times(1);

    Config configKerberosEnv = createMock(Config.class);
    expect(configKerberosEnv.getProperties()).andReturn(propertiesKerberosEnv).times(1);

    // Create a Service (SERVICE1) with one Component (COMPONENT11)
    Host host1 = createMockHost("host1");

    Map<String, ServiceComponentHost> service1Component1HostMap = new HashMap<>();
    service1Component1HostMap.put("host1", createMockServiceComponentHost(State.INSTALLED));

    Map<String, ServiceComponent> service1ComponentMap = new HashMap<>();
    service1ComponentMap.put("COMPONENT11", createMockComponent("COMPONENT11", true, service1Component1HostMap));

    Service service1 = createMockService("SERVICE1", service1ComponentMap);

    Map<String, Service> servicesMap = new HashMap<>();
    servicesMap.put("SERVICE1", service1);

    Cluster cluster = createMockCluster("c1", Collections.singletonList(host1), SecurityType.KERBEROS, configKrb5Conf, configKerberosEnv);
    expect(cluster.getServices()).andReturn(servicesMap).anyTimes();

    Map<String, String> kerberosDescriptorProperties = new HashMap<>();
    kerberosDescriptorProperties.put("additional_realms", "");
    kerberosDescriptorProperties.put("keytab_dir", "/etc/security/keytabs");
    kerberosDescriptorProperties.put(KerberosHelper.DEFAULT_REALM, "${kerberos-env/realm}");

    // Notice that this map is empty, hence it has 0 Components in the kerberosDescriptor.
    HashMap<String, KerberosComponentDescriptor> service1ComponentDescriptorMap = new HashMap<>();

    List<KerberosIdentityDescriptor> service1Identities = new ArrayList<>();
    KerberosServiceDescriptor service1KerberosDescriptor = createMockServiceDescriptor("SERVICE1", service1ComponentDescriptorMap, service1Identities, false);

    KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getProperties()).andReturn(kerberosDescriptorProperties);
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(service1KerberosDescriptor).anyTimes();

    setupKerberosDescriptor(kerberosDescriptor);

    Map<String, Map<String, String>> existingConfigurations = new HashMap<>();
    existingConfigurations.put("kerberos-env", propertiesKerberosEnv);

    Set<String> services = new HashSet<String>() {
      {
        add("SERVICE1");
      }
    };

    Capture<? extends String> capturePrincipal = newCapture(CaptureType.ALL);
    Capture<? extends String> capturePrincipalForKeytab = newCapture(CaptureType.ALL);

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    credentialStoreService.setCredential(cluster.getClusterName(), KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
        new PrincipalKeyCredential("principal", "password"), CredentialStoreType.TEMPORARY);

    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);
    // Validate that it works with 0 Components.
    kerberosHelper.ensureHeadlessIdentities(cluster, existingConfigurations, services);

    verifyAll();

    List<? extends String> capturedPrincipals = capturePrincipal.getValues();
    assertEquals(0, capturedPrincipals.size());

    List<? extends String> capturedPrincipalsForKeytab = capturePrincipalForKeytab.getValues();
    assertEquals(0, capturedPrincipalsForKeytab.size());
  }

  @Test
  public void testFiltersParsing(){
    Map<String, String> requestProperties = new HashMap<String, String>() {{
      put(DIRECTIVE_HOSTS, "host1,host2,host3");
      put(DIRECTIVE_COMPONENTS, "SERVICE1:COMPONENT1;COMPONENT2,SERVICE2:COMPONENT1;COMPONENT2;COMPONENT3");
    }};

    Set<String> expectedHosts = new HashSet<>(Arrays.asList("host1", "host2", "host3"));
    Set<String> hosts = KerberosHelperImpl.parseHostFilter(requestProperties);

    assertEquals(expectedHosts, hosts);

    Map<String, Set<String>> expectedComponents = new HashMap<String, Set<String>>(){{
      put("SERVICE1", new HashSet<String>(){{
        add("COMPONENT1");
        add("COMPONENT2");
      }});
      put("SERVICE2", new HashSet<String>(){{
        add("COMPONENT1");
        add("COMPONENT2");
        add("COMPONENT3");
      }});
    }};
    Map<String, Set<String>> components = KerberosHelperImpl.parseComponentFilter(requestProperties);

    assertEquals(expectedComponents, components);
  }

  private void setupKerberosDescriptor(KerberosDescriptor kerberosDescriptor) throws Exception {
    // cluster.getCurrentStackVersion expectation is already specified in main test method
    AmbariMetaInfo metaInfo = injector.getInstance(AmbariMetaInfo.class);
    expect(metaInfo.
        getKerberosDescriptor("HDP", "2.2", false)).andReturn(kerberosDescriptor).anyTimes();
    expect(kerberosDescriptor.principals()).andReturn(Collections.<String, String>emptyMap()).anyTimes();
  }

  private void setupStageFactory() {
    final StageFactory stageFactory = injector.getInstance(StageFactory.class);
    expect(stageFactory.createNew(anyLong(), anyObject(String.class), anyObject(String.class),
        anyLong(), anyObject(String.class), anyObject(String.class),
        anyObject(String.class)))
        .andAnswer(new IAnswer<Stage>() {
          @Override
          public Stage answer() throws Throwable {
            Stage stage = createNiceMock(Stage.class);

            expect(stage.getHostRoleCommands())
                .andReturn(Collections.emptyMap())
                .anyTimes();
            replay(stage);
            return stage;
          }
        })
        .anyTimes();
  }

  private void testEnsureIdentities(final PrincipalKeyCredential PrincipalKeyCredential, Set<String> filteredHosts) throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final ServiceComponentHost schKerberosClientA = createMock(ServiceComponentHost.class);
    expect(schKerberosClientA.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClientA.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(schKerberosClientA.getHostName()).andReturn("hostA").anyTimes();
    expect(schKerberosClientA.getState()).andReturn(State.INSTALLED).anyTimes();

    final ServiceComponentHost schKerberosClientB = createMock(ServiceComponentHost.class);
    expect(schKerberosClientB.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClientB.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(schKerberosClientB.getHostName()).andReturn("hostB").anyTimes();
    expect(schKerberosClientB.getState()).andReturn(State.INSTALLED).anyTimes();

    final ServiceComponentHost schKerberosClientC = createMock(ServiceComponentHost.class);
    expect(schKerberosClientC.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClientC.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(schKerberosClientC.getHostName()).andReturn("hostC").anyTimes();
    expect(schKerberosClientC.getState()).andReturn(State.INSTALLED).anyTimes();

    final ServiceComponentHost sch1A = createMock(ServiceComponentHost.class);
    expect(sch1A.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(sch1A.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();
    expect(sch1A.getHostName()).andReturn("hostA").anyTimes();

    final ServiceComponentHost sch1B = createMock(ServiceComponentHost.class);
    expect(sch1B.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(sch1B.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();
    expect(sch1B.getHostName()).andReturn("hostB").anyTimes();

    final ServiceComponentHost sch1C = createMock(ServiceComponentHost.class);
    expect(sch1C.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(sch1C.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();
    expect(sch1C.getHostName()).andReturn("hostC").anyTimes();

    final ServiceComponentHost sch2 = createMock(ServiceComponentHost.class);
    expect(sch2.getServiceName()).andReturn("SERVICE2").anyTimes();
    expect(sch2.getServiceComponentName()).andReturn("COMPONENT3").anyTimes();
    expect(sch2.getHostName()).andReturn("hostA").anyTimes();

    final ServiceComponentHost sch3 = createMock(ServiceComponentHost.class);
    expect(sch3.getServiceName()).andReturn("SERVICE3").anyTimes();
    expect(sch3.getServiceComponentName()).andReturn("COMPONENT3").anyTimes();
    expect(sch3.getHostName()).andReturn("hostA").anyTimes();

    final Host hostA = createMockHost("hostA");
    final Host hostB = createMockHost("hostB");
    final Host hostC = createMockHost("hostC");

    final ServiceComponent serviceComponentKerberosClient = createNiceMock(ServiceComponent.class);
    expect(serviceComponentKerberosClient.getName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(serviceComponentKerberosClient.getServiceComponentHosts()).andReturn(
        new HashMap<String, ServiceComponentHost>() {
          {
            put("hostA", schKerberosClientA);
            put("hostB", schKerberosClientB);
            put("hostC", schKerberosClientC);
          }
        }
    ).anyTimes();

    final Service serviceKerberos = createStrictMock(Service.class);
    expect(serviceKerberos.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(serviceKerberos.getName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(serviceKerberos.getServiceComponents())
        .andReturn(Collections.singletonMap(Role.KERBEROS_CLIENT.name(), serviceComponentKerberosClient))
        .anyTimes();

    final Service service1 = createStrictMock(Service.class);
    expect(service1.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service1.getName()).andReturn("SERVICE1").anyTimes();
    expect(service1.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Service service2 = createStrictMock(Service.class);
    expect(service2.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service2.getName()).andReturn("SERVICE2").anyTimes();
    expect(service2.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Map<String, String> kerberosEnvProperties = createMock(Map.class);
    expect(kerberosEnvProperties.get(KerberosHelper.KDC_TYPE)).andReturn("mit-kdc").anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.DEFAULT_REALM)).andReturn("FOOBAR.COM").anyTimes();

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).anyTimes();

    final Map<String, String> krb5ConfProperties = createMock(Map.class);

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).anyTimes();

    final Cluster cluster = createMockCluster("c1", Arrays.asList(hostA, hostB, hostC), SecurityType.KERBEROS, krb5ConfConfig, kerberosEnvConfig);
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(cluster.getServices())
        .andReturn(new HashMap<String, Service>() {
          {
            put(Service.Type.KERBEROS.name(), serviceKerberos);
            put("SERVICE1", service1);
            put("SERVICE2", service2);
          }
        })
        .anyTimes();

    expect(cluster.getServiceComponentHosts("hostA"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(sch1A);
            add(sch2);
            add(sch3);
            add(schKerberosClientA);
          }
        })
        .once();

    expect(cluster.getServiceComponentHosts("hostB"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(sch1B);
            add(schKerberosClientB);
          }
        })
        .once();

    expect(cluster.getServiceComponentHosts("hostC"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(sch1C);
            add(schKerberosClientC);
          }
        })
        .once();

    expect(cluster.getServiceComponentHosts("KERBEROS", "KERBEROS_CLIENT"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(schKerberosClientA);
            add(schKerberosClientB);
            add(schKerberosClientC);
          }
        })
        .anyTimes();

    final Clusters clusters = injector.getInstance(Clusters.class);
    if ((filteredHosts == null) || filteredHosts.contains("hostA")) {
      expect(clusters.getHost("hostA"))
          .andReturn(hostA)
          .anyTimes();
    }
    if ((filteredHosts == null) || filteredHosts.contains("hostB")) {
      expect(clusters.getHost("hostB"))
          .andReturn(hostB)
          .anyTimes();
    }
    if ((filteredHosts == null) || filteredHosts.contains("hostC")) {
      expect(clusters.getHost("hostC"))
          .andReturn(hostC)
          .anyTimes();
    }

    final AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, null))
        .andReturn(Collections.emptyMap())
        .times(3);
    expect(ambariManagementController.getRoleCommandOrder(cluster))
        .andReturn(createMock(RoleCommandOrder.class))
        .once();

    final KerberosPrincipalDescriptor principalDescriptor1a = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor1a.getValue()).andReturn("component1a/_HOST@${realm}").anyTimes();
    expect(principalDescriptor1a.getType()).andReturn(KerberosPrincipalType.SERVICE).anyTimes();
    expect(principalDescriptor1a.getLocalUsername()).andReturn(null).anyTimes();
    expect(principalDescriptor1a.getConfiguration()).andReturn("service1b-site/component1.kerberos.principal").anyTimes();

    final KerberosPrincipalDescriptor principalDescriptor1b = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor1b.getValue()).andReturn("component1b/_HOST@${realm}").anyTimes();
    expect(principalDescriptor1b.getType()).andReturn(KerberosPrincipalType.SERVICE).anyTimes();
    expect(principalDescriptor1b.getLocalUsername()).andReturn(null).anyTimes();
    expect(principalDescriptor1b.getConfiguration()).andReturn("service1b-site/component1.kerberos.principal").anyTimes();

    final KerberosPrincipalDescriptor principalDescriptor3 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor3.getValue()).andReturn("component3/${host}@${realm}").anyTimes();
    expect(principalDescriptor3.getType()).andReturn(KerberosPrincipalType.SERVICE).anyTimes();
    expect(principalDescriptor3.getLocalUsername()).andReturn(null).anyTimes();
    expect(principalDescriptor3.getConfiguration()).andReturn("service3-site/component3.kerberos.principal").anyTimes();

    final KerberosKeytabDescriptor keytabDescriptor1 = createMock(KerberosKeytabDescriptor.class);

    final KerberosKeytabDescriptor keytabDescriptor3 = createMock(KerberosKeytabDescriptor.class);

    final KerberosIdentityDescriptor identityDescriptor1a = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor1a.getName()).andReturn("identity1a").anyTimes();
    expect(identityDescriptor1a.getPrincipalDescriptor()).andReturn(principalDescriptor1a).anyTimes();
    expect(identityDescriptor1a.getKeytabDescriptor()).andReturn(keytabDescriptor1).anyTimes();

    final KerberosIdentityDescriptor identityDescriptor1b = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor1b.getName()).andReturn("identity1b").anyTimes();
    expect(identityDescriptor1b.getPrincipalDescriptor()).andReturn(principalDescriptor1b).anyTimes();

    final KerberosIdentityDescriptor identityDescriptor3 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor3.getName()).andReturn("identity3").anyTimes();
    expect(identityDescriptor3.getPrincipalDescriptor()).andReturn(principalDescriptor3).anyTimes();
    expect(identityDescriptor3.getKeytabDescriptor()).andReturn(keytabDescriptor3).anyTimes();

    final KerberosServiceDescriptor serviceDescriptor1 = createMock(KerberosServiceDescriptor.class);

    final KerberosServiceDescriptor serviceDescriptor3 = createMock(KerberosServiceDescriptor.class);

    final KerberosDescriptor kerberosDescriptor = createStrictMock(KerberosDescriptor.class);
    if ((filteredHosts == null) || filteredHosts.contains("hostA")) {
      expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).times(1);
      expect(kerberosDescriptor.getService("SERVICE3")).andReturn(serviceDescriptor3).times(1);
    }

    if ((filteredHosts == null) || filteredHosts.contains("hostB")) {
      expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).times(1);
    }

    if ((filteredHosts == null) || filteredHosts.contains("hostC")) {
      expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).times(1);
    }

    setupKerberosDescriptor(kerberosDescriptor);
    setupStageFactory();

    // This is a STRICT mock to help ensure that the end result is what we want.
    final RequestStageContainer requestStageContainer = createStrictMock(RequestStageContainer.class);
    // Preparation Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Getting missing keytabs
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Create Principals Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Create Keytabs Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Distribute Keytabs Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Clean-up/Finalize Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    Map<String, Collection<String>> serviceComponentFilter = new HashMap<>();
    Collection<String> identityFilter = Arrays.asList("identity1a", "identity3");

    serviceComponentFilter.put("SERVICE3", Collections.singleton("COMPONENT3"));
    serviceComponentFilter.put("SERVICE1", null);

    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    credentialStoreService.setCredential(cluster.getClusterName(), KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
        PrincipalKeyCredential, CredentialStoreType.TEMPORARY);

    kerberosHelper.ensureIdentities(cluster, serviceComponentFilter, filteredHosts, identityFilter, null, requestStageContainer, true);

    verifyAll();
  }

  private void testDeleteIdentities(final PrincipalKeyCredential PrincipalKeyCredential) throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final ServiceComponentHost schKerberosClient = createMock(ServiceComponentHost.class);
    expect(schKerberosClient.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClient.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(schKerberosClient.getHostName()).andReturn("host1").anyTimes();
    expect(schKerberosClient.getState()).andReturn(State.INSTALLED).anyTimes();

    final ServiceComponentHost sch1 = createMock(ServiceComponentHost.class);
    expect(sch1.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(sch1.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();
    expect(sch1.getHostName()).andReturn("host1").anyTimes();

    final ServiceComponentHost sch2 = createMock(ServiceComponentHost.class);
    expect(sch2.getServiceName()).andReturn("SERVICE2").anyTimes();
    expect(sch2.getServiceComponentName()).andReturn("COMPONENT3").anyTimes();

    final ServiceComponentHost sch3 = createMock(ServiceComponentHost.class);
    expect(sch3.getServiceName()).andReturn("SERVICE3").anyTimes();
    expect(sch3.getServiceComponentName()).andReturn("COMPONENT3").anyTimes();
    expect(sch3.getHostName()).andReturn("host1").anyTimes();

    final Host host = createMockHost("host1");

    final ServiceComponent serviceComponentKerberosClient = createNiceMock(ServiceComponent.class);
    expect(serviceComponentKerberosClient.getName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(serviceComponentKerberosClient.getServiceComponentHosts()).andReturn(Collections.singletonMap("host1", schKerberosClient)).anyTimes();

    final Service serviceKerberos = createStrictMock(Service.class);
    expect(serviceKerberos.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(serviceKerberos.getName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(serviceKerberos.getServiceComponents())
        .andReturn(Collections.singletonMap(Role.KERBEROS_CLIENT.name(), serviceComponentKerberosClient))
        .anyTimes();

    final Service service1 = createStrictMock(Service.class);
    expect(service1.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service1.getName()).andReturn("SERVICE1").anyTimes();
    expect(service1.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Service service2 = createStrictMock(Service.class);
    expect(service2.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service2.getName()).andReturn("SERVICE2").anyTimes();
    expect(service2.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Map<String, String> kerberosEnvProperties = createMock(Map.class);
    expect(kerberosEnvProperties.get(KerberosHelper.KDC_TYPE)).andReturn("mit-kdc").anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.DEFAULT_REALM)).andReturn("FOOBAR.COM").anyTimes();

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).anyTimes();

    final Map<String, String> krb5ConfProperties = createMock(Map.class);

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).anyTimes();

    final Cluster cluster = createMockCluster("c1", Collections.singleton(host), SecurityType.KERBEROS, krb5ConfConfig, kerberosEnvConfig);
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(cluster.getServices())
        .andReturn(new HashMap<String, Service>() {
          {
            put(Service.Type.KERBEROS.name(), serviceKerberos);
            put("SERVICE1", service1);
            put("SERVICE2", service2);
          }
        })
        .anyTimes();
    expect(cluster.getServiceComponentHosts("host1"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(sch1);
            add(sch2);
            add(sch3);
            add(schKerberosClient);
          }
        })
        .once();
    expect(cluster.getServiceComponentHosts("KERBEROS", "KERBEROS_CLIENT"))
        .andReturn(Collections.singletonList(schKerberosClient))
        .once();

    final Clusters clusters = injector.getInstance(Clusters.class);
    expect(clusters.getHost("host1"))
        .andReturn(host)
        .once();


    final AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, null))
        .andReturn(Collections.emptyMap())
        .once();
    expect(ambariManagementController.getRoleCommandOrder(cluster))
        .andReturn(createMock(RoleCommandOrder.class))
        .once();

    final KerberosPrincipalDescriptor principalDescriptor1a = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor1a.getValue()).andReturn("component1a/_HOST@${realm}").anyTimes();
    expect(principalDescriptor1a.getType()).andReturn(KerberosPrincipalType.SERVICE).anyTimes();
    expect(principalDescriptor1a.getLocalUsername()).andReturn(null).anyTimes();
    expect(principalDescriptor1a.getConfiguration()).andReturn("service1b-site/component1.kerberos.principal").anyTimes();

    final KerberosPrincipalDescriptor principalDescriptor1b = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor1b.getValue()).andReturn("component1b/_HOST@${realm}").anyTimes();
    expect(principalDescriptor1b.getType()).andReturn(KerberosPrincipalType.SERVICE).anyTimes();
    expect(principalDescriptor1b.getLocalUsername()).andReturn(null).anyTimes();
    expect(principalDescriptor1b.getConfiguration()).andReturn("service1b-site/component1.kerberos.principal").anyTimes();

    final KerberosPrincipalDescriptor principalDescriptor3 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor3.getValue()).andReturn("component3/${host}@${realm}").anyTimes();
    expect(principalDescriptor3.getType()).andReturn(KerberosPrincipalType.SERVICE).anyTimes();
    expect(principalDescriptor3.getLocalUsername()).andReturn(null).anyTimes();
    expect(principalDescriptor3.getConfiguration()).andReturn("service3-site/component3.kerberos.principal").anyTimes();

    final KerberosKeytabDescriptor keytabDescriptor1 = createMock(KerberosKeytabDescriptor.class);

    final KerberosKeytabDescriptor keytabDescriptor3 = createMock(KerberosKeytabDescriptor.class);

    final KerberosIdentityDescriptor identityDescriptor1a = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor1a.getName()).andReturn("identity1a").anyTimes();
    expect(identityDescriptor1a.getPrincipalDescriptor()).andReturn(principalDescriptor1a).anyTimes();
    expect(identityDescriptor1a.getKeytabDescriptor()).andReturn(keytabDescriptor1).anyTimes();

    final KerberosIdentityDescriptor identityDescriptor1b = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor1b.getName()).andReturn("identity1b").anyTimes();
    expect(identityDescriptor1b.getPrincipalDescriptor()).andReturn(principalDescriptor1b).anyTimes();

    final KerberosIdentityDescriptor identityDescriptor3 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor3.getName()).andReturn("identity3").anyTimes();
    expect(identityDescriptor3.getPrincipalDescriptor()).andReturn(principalDescriptor3).anyTimes();
    expect(identityDescriptor3.getKeytabDescriptor()).andReturn(keytabDescriptor3).anyTimes();

    final KerberosServiceDescriptor serviceDescriptor1 = createMock(KerberosServiceDescriptor.class);

    final KerberosServiceDescriptor serviceDescriptor3 = createMock(KerberosServiceDescriptor.class);

    final KerberosDescriptor kerberosDescriptor = createStrictMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).times(1);
    expect(kerberosDescriptor.getService("SERVICE3")).andReturn(serviceDescriptor3).times(1);

    setupKerberosDescriptor(kerberosDescriptor);
    setupStageFactory();

    // This is a STRICT mock to help ensure that the end result is what we want.
    final RequestStageContainer requestStageContainer = createStrictMock(RequestStageContainer.class);
    // Preparation Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Delete Principals Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Delete Keytabs Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Clean-up
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Finalize Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    Map<String, Collection<String>> serviceComponentFilter = new HashMap<>();
    Collection<String> identityFilter = Arrays.asList("identity1a", "identity3");

    serviceComponentFilter.put("SERVICE3", Collections.singleton("COMPONENT3"));
    serviceComponentFilter.put("SERVICE1", null);

    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    credentialStoreService.setCredential(cluster.getClusterName(), KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
        PrincipalKeyCredential, CredentialStoreType.TEMPORARY);

    kerberosHelper.deleteIdentities(cluster, serviceComponentFilter, null, identityFilter, requestStageContainer, true);

    verifyAll();
  }

  private void testCreateTestIdentity(final PrincipalKeyCredential PrincipalKeyCredential, Boolean manageIdentities) throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);
    KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO = injector.getInstance(KerberosKeytabPrincipalDAO.class);
    expect(kerberosKeytabPrincipalDAO.findOrCreate(anyObject(), anyObject(), anyObject())).andReturn(createNiceMock(KerberosKeytabPrincipalEntity.class)).anyTimes();
    boolean managingIdentities = !Boolean.FALSE.equals(manageIdentities);

    final Map<String, String> kerberosEnvProperties = new HashMap<>();
    kerberosEnvProperties.put(KerberosHelper.KDC_TYPE, "mit-kdc");
    kerberosEnvProperties.put(KerberosHelper.DEFAULT_REALM, "FOOBAR.COM");
    kerberosEnvProperties.put("manage_identities",
        (manageIdentities == null)
            ? null
            : ((manageIdentities) ? "true" : "false"));

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).anyTimes();

    final Map<String, String> krb5ConfProperties = new HashMap<>();

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).anyTimes();

    final Map<String, Object> attributeMap = new HashMap<>();

    final Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getDesiredConfigByType("krb5-conf")).andReturn(krb5ConfConfig).anyTimes();
    expect(cluster.getDesiredConfigByType("kerberos-env")).andReturn(kerberosEnvConfig).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("HDP-2.2")).anyTimes();

    // This is a STRICT mock to help ensure that the end result is what we want.
    final RequestStageContainer requestStageContainer = createStrictMock(RequestStageContainer.class);

    if (managingIdentities) {
      final Host host = createMockHost("host1");

      expect(host.getHostId()).andReturn(1l).anyTimes();

      expect(cluster.getHosts()).andReturn(Collections.singleton(host)).anyTimes();

      final ServiceComponentHost schKerberosClient = createMock(ServiceComponentHost.class);
      expect(schKerberosClient.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
      expect(schKerberosClient.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
      expect(schKerberosClient.getHostName()).andReturn("host1").anyTimes();
      expect(schKerberosClient.getState()).andReturn(State.INSTALLED).anyTimes();
      expect(schKerberosClient.getHost()).andReturn(host).anyTimes();

      final ServiceComponent serviceComponentKerberosClient = createNiceMock(ServiceComponent.class);
      expect(serviceComponentKerberosClient.getName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
      expect(serviceComponentKerberosClient.getServiceComponentHosts()).andReturn(Collections.singletonMap("host1", schKerberosClient)).anyTimes();

      final Service serviceKerberos = createNiceMock(Service.class);
      expect(serviceKerberos.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
      expect(serviceKerberos.getName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
      expect(serviceKerberos.getServiceComponents())
          .andReturn(Collections.singletonMap(Role.KERBEROS_CLIENT.name(), serviceComponentKerberosClient))
          .anyTimes();

      final Service service1 = createNiceMock(Service.class);
      expect(service1.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
      expect(service1.getName()).andReturn("SERVICE1").anyTimes();
      expect(service1.getServiceComponents())
          .andReturn(Collections.emptyMap())
          .anyTimes();

      final Service service2 = createNiceMock(Service.class);
      expect(service2.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
      expect(service2.getName()).andReturn("SERVICE2").anyTimes();
      expect(service2.getServiceComponents())
          .andReturn(Collections.emptyMap())
          .anyTimes();


      expect(cluster.getClusterName()).andReturn("c1").anyTimes();
      expect(cluster.getServices())
          .andReturn(new HashMap<String, Service>() {
            {
              put(Service.Type.KERBEROS.name(), serviceKerberos);
              put("SERVICE1", service1);
              put("SERVICE2", service2);
            }
          })
          .anyTimes();
      expect(cluster.getServiceComponentHosts(Service.Type.KERBEROS.name(), Role.KERBEROS_CLIENT.name()))
          .andReturn(new ArrayList<ServiceComponentHost>() {
            {
              add(schKerberosClient);
            }
          })
          .anyTimes();
      expect(cluster.getCurrentStackVersion())
          .andReturn(new StackId("HDP", "2.2"))
          .anyTimes();
      expect(cluster.getSessionAttributes()).andReturn(attributeMap).anyTimes();

      cluster.setSessionAttribute(anyObject(String.class), anyObject());
      expectLastCall().andAnswer(new IAnswer<Object>() {
        @Override
        public Object answer() throws Throwable {
          Object[] args = getCurrentArguments();
          attributeMap.put((String) args[0], args[1]);
          return null;
        }
      }).anyTimes();

      final Clusters clusters = injector.getInstance(Clusters.class);
      expect(clusters.getHost("host1"))
          .andReturn(host)
          .anyTimes();

      final AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
      expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, null))
          .andReturn(Collections.emptyMap())
          .once();
      expect(ambariManagementController.getRoleCommandOrder(cluster))
          .andReturn(createMock(RoleCommandOrder.class))
          .once();

      final ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
      expect(configHelper.calculateExistingConfigurations(eq(ambariManagementController), anyObject(Cluster.class), EasyMock.anyObject()))
          .andReturn(new HashMap<String, Map<String, String>>() {
            {
              put("cluster-env", new HashMap<String, String>() {{
                put("kerberos_domain", "FOOBAR.COM");
              }});
            }
          })
          .times(1);

      final KerberosDescriptor kerberosDescriptor = createStrictMock(KerberosDescriptor.class);
      expect(kerberosDescriptor.getProperties()).andReturn(null).once();

      setupKerberosDescriptor(kerberosDescriptor);
      setupStageFactory();

      // Preparation Stage
      expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Getting missing keytabs
      expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Create Principals Stage
      expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();

      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Create Keytabs Stage
      expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Distribute Keytabs Stage
      expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
      // Clean-up/Finalize Stage
      expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
      expect(requestStageContainer.getId()).andReturn(1L).once();
      requestStageContainer.addStages(EasyMock.anyObject());
      expectLastCall().once();
    }

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    Map<String, String> commandParamsStage = new HashMap<>();
    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    credentialStoreService.setCredential(cluster.getClusterName(), KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
        PrincipalKeyCredential, CredentialStoreType.TEMPORARY);
    kerberosHelper.createTestIdentity(cluster, commandParamsStage, requestStageContainer);

    verifyAll();

    if (managingIdentities) {
      Assert.assertTrue(commandParamsStage.containsKey("principal_name"));
      Assert.assertEquals("${kerberos-env/service_check_principal_name}@${realm}", commandParamsStage.get("principal_name"));

      Assert.assertTrue(commandParamsStage.containsKey("keytab_file"));
      Assert.assertEquals("${keytab_dir}/kerberos.service_check." + new SimpleDateFormat("MMddyy").format(new Date()) + ".keytab",
          commandParamsStage.get("keytab_file"));
    }
  }

  private void testDeleteTestIdentity(final PrincipalKeyCredential PrincipalKeyCredential) throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);
    KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO = injector.getInstance(KerberosKeytabPrincipalDAO.class);
    expect(kerberosKeytabPrincipalDAO.findOrCreate(anyObject(), anyObject(), anyObject())).andReturn(createNiceMock(KerberosKeytabPrincipalEntity.class)).anyTimes();
    Host host1 = createMock(Host.class);
    expect(host1.getHostId()).andReturn(1l).anyTimes();

    final ServiceComponentHost schKerberosClient = createMock(ServiceComponentHost.class);
    expect(schKerberosClient.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClient.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(schKerberosClient.getHostName()).andReturn("host1").anyTimes();
    expect(schKerberosClient.getState()).andReturn(State.INSTALLED).anyTimes();
    expect(schKerberosClient.getHost()).andReturn(host1).anyTimes();

    final ServiceComponentHost sch1 = createMock(ServiceComponentHost.class);
    expect(sch1.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(sch1.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();
    expect(sch1.getHostName()).andReturn("host1").anyTimes();

    final ServiceComponentHost sch2 = createStrictMock(ServiceComponentHost.class);
    expect(sch2.getServiceName()).andReturn("SERVICE2").anyTimes();
    expect(sch2.getServiceComponentName()).andReturn("COMPONENT3").anyTimes();

    final ServiceComponentHost sch3 = createStrictMock(ServiceComponentHost.class);
    expect(sch3.getServiceName()).andReturn("SERVICE3").anyTimes();
    expect(sch3.getServiceComponentName()).andReturn("COMPONENT3").anyTimes();
    expect(sch3.getHostName()).andReturn("host1").anyTimes();

    final Host host = createMockHost("host1");

    final ServiceComponent serviceComponentKerberosClient = createNiceMock(ServiceComponent.class);
    expect(serviceComponentKerberosClient.getName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(serviceComponentKerberosClient.getServiceComponentHosts()).andReturn(Collections.singletonMap("host1", schKerberosClient)).anyTimes();

    final Service serviceKerberos = createNiceMock(Service.class);
    expect(serviceKerberos.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(serviceKerberos.getName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(serviceKerberos.getServiceComponents())
        .andReturn(Collections.singletonMap(Role.KERBEROS_CLIENT.name(), serviceComponentKerberosClient))
        .anyTimes();

    final Service service1 = createNiceMock(Service.class);
    expect(service1.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service1.getName()).andReturn("SERVICE1").anyTimes();
    expect(service1.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Service service2 = createNiceMock(Service.class);
    expect(service2.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service2.getName()).andReturn("SERVICE2").anyTimes();
    expect(service2.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Map<String, String> kerberosEnvProperties = createMock(Map.class);
    expect(kerberosEnvProperties.get(KerberosHelper.KDC_TYPE)).andReturn("mit-kdc").anyTimes();
    expect(kerberosEnvProperties.get(KerberosHelper.DEFAULT_REALM)).andReturn("FOOBAR.COM").anyTimes();
    expect(kerberosEnvProperties.get("manage_identities")).andReturn(null).anyTimes();

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).anyTimes();

    final Map<String, String> krb5ConfProperties = createMock(Map.class);

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).anyTimes();

    final Cluster cluster = createMockCluster("c1", Collections.singleton(host), SecurityType.KERBEROS, krb5ConfConfig, kerberosEnvConfig);
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(cluster.getServices())
        .andReturn(new HashMap<String, Service>() {
          {
            put(Service.Type.KERBEROS.name(), serviceKerberos);
            put("SERVICE1", service1);
            put("SERVICE2", service2);
          }
        })
        .anyTimes();
    expect(cluster.getServiceComponentHosts(Service.Type.KERBEROS.name(), Role.KERBEROS_CLIENT.name()))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(schKerberosClient);
          }
        })
        .anyTimes();

    final Clusters clusters = injector.getInstance(Clusters.class);
    expect(clusters.getHost("host1"))
        .andReturn(host)
        .once();

    final AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, null))
        .andReturn(Collections.emptyMap())
        .once();
    expect(ambariManagementController.getRoleCommandOrder(cluster))
        .andReturn(createMock(RoleCommandOrder.class))
        .once();

    final ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    expect(configHelper.calculateExistingConfigurations(eq(ambariManagementController), anyObject(Cluster.class), EasyMock.anyObject()))
        .andReturn(new HashMap<String, Map<String, String>>() {
          {
            put("cluster-env", new HashMap<String, String>() {
              {
                put("kerberos_domain", "FOOBAR.COM");
              }
            });
          }
        })
        .times(1);

    final KerberosDescriptor kerberosDescriptor = createStrictMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getProperties()).andReturn(null).once();

    setupKerberosDescriptor(kerberosDescriptor);
    setupStageFactory();

    // This is a STRICT mock to help ensure that the end result is what we want.
    final RequestStageContainer requestStageContainer = createStrictMock(RequestStageContainer.class);
    // Preparation Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Delete Principals Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Delete Keytabs Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Clean-up
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();
    // Finalize Stage
    expect(requestStageContainer.getLastStageId()).andReturn(-1L).anyTimes();
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(EasyMock.anyObject());
    expectLastCall().once();

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    Map<String, String> commandParamsStage = new HashMap<>();
    commandParamsStage.put("principal_name", "${cluster-env/smokeuser}@${realm}");
    commandParamsStage.put("keytab_file", "${keytab_dir}/kerberos.service_check.keytab");

    CredentialStoreService credentialStoreService = injector.getInstance(CredentialStoreService.class);
    credentialStoreService.setCredential(cluster.getClusterName(), KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
        PrincipalKeyCredential, CredentialStoreType.TEMPORARY);
    kerberosHelper.deleteTestIdentity(cluster, commandParamsStage, requestStageContainer);

    verifyAll();
  }

  private Map<String, Collection<KerberosIdentityDescriptor>> testGetActiveIdentities(String clusterName,
                                                                                      String hostName,
                                                                                      String serviceName,
                                                                                      String componentName,
                                                                                      boolean replaceHostNames,
                                                                                      SecurityType clusterSecurityType)
      throws Exception {

    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final ServiceComponentHost schKerberosClient1 = createMock(ServiceComponentHost.class);
    expect(schKerberosClient1.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClient1.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();

    final ServiceComponentHost schKerberosClient2 = createMock(ServiceComponentHost.class);
    expect(schKerberosClient2.getServiceName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(schKerberosClient2.getServiceComponentName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();

    final ServiceComponentHost sch1a = createMock(ServiceComponentHost.class);
    expect(sch1a.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(sch1a.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();

    final ServiceComponentHost sch1b = createMock(ServiceComponentHost.class);
    expect(sch1b.getServiceName()).andReturn("SERVICE2").anyTimes();
    expect(sch1b.getServiceComponentName()).andReturn("COMPONENT2").anyTimes();

    final ServiceComponentHost sch2a = createMock(ServiceComponentHost.class);
    expect(sch2a.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(sch2a.getServiceComponentName()).andReturn("COMPONENT1").anyTimes();

    final ServiceComponentHost sch2b = createMock(ServiceComponentHost.class);
    expect(sch2b.getServiceName()).andReturn("SERVICE2").anyTimes();
    expect(sch2b.getServiceComponentName()).andReturn("COMPONENT2").anyTimes();

    final Host host1 = createMockHost("host1");
    final Host host2 = createMockHost("host2");

    final ServiceComponent serviceComponentKerberosClient = createNiceMock(ServiceComponent.class);
    expect(serviceComponentKerberosClient.getName()).andReturn(Role.KERBEROS_CLIENT.name()).anyTimes();
    expect(serviceComponentKerberosClient.getServiceComponentHosts()).andReturn(Collections.singletonMap("host1", schKerberosClient1)).anyTimes();

    final Service serviceKerberos = createNiceMock(Service.class);
    expect(serviceKerberos.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(serviceKerberos.getName()).andReturn(Service.Type.KERBEROS.name()).anyTimes();
    expect(serviceKerberos.getServiceComponents())
        .andReturn(Collections.singletonMap(Role.KERBEROS_CLIENT.name(), serviceComponentKerberosClient))
        .anyTimes();

    final Service service1 = createNiceMock(Service.class);
    expect(service1.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service1.getName()).andReturn("SERVICE1").anyTimes();
    expect(service1.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Service service2 = createNiceMock(Service.class);
    expect(service2.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service2.getName()).andReturn("SERVICE2").anyTimes();
    expect(service2.getServiceComponents())
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final Map<String, Host> hostMap = new HashMap<String, Host>() {
      {
        put("host1", host1);
        put("host2", host2);
      }
    };
    final Collection<Host> hosts = hostMap.values();

    final Cluster cluster = createMock(Cluster.class);
    expect(cluster.getSecurityType()).andReturn(clusterSecurityType).anyTimes();
    expect(cluster.getClusterName()).andReturn(clusterName).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getServiceComponentHosts("host1"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(schKerberosClient1);
            add(sch1a);
            add(sch1b);
          }
        })
        .anyTimes();
    expect(cluster.getServiceComponentHosts("host2"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(schKerberosClient2);
            add(sch2a);
            add(sch2b);
          }
        })
        .anyTimes();
    expect(cluster.getServiceComponentHosts(InetAddress.getLocalHost().getCanonicalHostName().toLowerCase()))
        .andReturn(new ArrayList<>())
        .anyTimes();

    final Map<String, String> kerberosEnvProperties = new HashMap<String, String>() {
      {
        put(KerberosHelper.KDC_TYPE, "mit-kdc");
        put(KerberosHelper.DEFAULT_REALM, "FOOBAR.COM");
        put("case_insensitive_username_rules", "false");
        put(KerberosHelper.CREATE_AMBARI_PRINCIPAL, "false");
      }
    };

    final Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(kerberosEnvProperties).anyTimes();

    final Map<String, String> krb5ConfProperties = createMock(Map.class);

    final Config krb5ConfConfig = createMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).anyTimes();

    expect(cluster.getDesiredConfigByType("krb5-conf"))
        .andReturn(krb5ConfConfig)
        .anyTimes();

    expect(cluster.getDesiredConfigByType("kerberos-env"))
        .andReturn(kerberosEnvConfig)
        .anyTimes();

    expect(cluster.getCurrentStackVersion())
        .andReturn(new StackId("HDP", "2.2"))
        .anyTimes();
    expect(cluster.getServices())
        .andReturn(new HashMap<String, Service>() {
          {
            put(Service.Type.KERBEROS.name(), serviceKerberos);
            put("SERVICE1", service1);
            put("SERVICE2", service2);
          }
        })
        .anyTimes();
    expect(cluster.getHosts())
        .andReturn(hosts)
        .anyTimes();


    final Clusters clusters = injector.getInstance(Clusters.class);
    expect(clusters.getCluster(clusterName)).andReturn(cluster).times(1);

    if (hostName == null) {
      expect(clusters.getHostsForCluster(clusterName))
          .andReturn(hostMap)
          .once();
    }

    final AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, "host1"))
        .andReturn(Collections.emptyMap())
        .anyTimes();
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, "host2"))
        .andReturn(Collections.emptyMap())
        .anyTimes();
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, null))
        .andReturn(Collections.emptyMap())
        .anyTimes();

    final ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    expect(configHelper.calculateExistingConfigurations(eq(ambariManagementController), anyObject(Cluster.class), EasyMock.anyObject()))
        .andReturn(new HashMap<String, Map<String, String>>() {
          {
            put("cluster-env", new HashMap<String, String>() {{
              put("kerberos_domain", "FOOBAR.COM");
            }});
          }
        })
        .anyTimes();

    final KerberosPrincipalDescriptor principalDescriptor1 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor1.getValue()).andReturn("service1/_HOST@${realm}").anyTimes();
    expect(principalDescriptor1.getType()).andReturn(KerberosPrincipalType.SERVICE).anyTimes();
    expect(principalDescriptor1.getConfiguration()).andReturn("service1-site/component1.kerberos.principal").anyTimes();
    expect(principalDescriptor1.getLocalUsername()).andReturn("service1").anyTimes();

    final KerberosPrincipalDescriptor principalDescriptor2 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor2.getValue()).andReturn("component2/${host}@${realm}").anyTimes();
    expect(principalDescriptor2.getType()).andReturn(KerberosPrincipalType.SERVICE).anyTimes();
    expect(principalDescriptor2.getConfiguration()).andReturn("service2-site/component2.kerberos.principal").anyTimes();
    expect(principalDescriptor2.getLocalUsername()).andReturn("service2").anyTimes();

    final KerberosPrincipalDescriptor principalDescriptorService1 = createMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptorService1.getValue()).andReturn("service1/_HOST@${realm}").anyTimes();
    expect(principalDescriptorService1.getType()).andReturn(KerberosPrincipalType.SERVICE).anyTimes();
    expect(principalDescriptorService1.getConfiguration()).andReturn("service1-site/service1.kerberos.principal").anyTimes();
    expect(principalDescriptorService1.getLocalUsername()).andReturn("service1").anyTimes();

    final KerberosKeytabDescriptor keytabDescriptor1 = createMock(KerberosKeytabDescriptor.class);
    expect(keytabDescriptor1.getFile()).andReturn("${keytab_dir}/service1.keytab").anyTimes();
    expect(keytabDescriptor1.getOwnerName()).andReturn("service1").anyTimes();
    expect(keytabDescriptor1.getOwnerAccess()).andReturn("rw").anyTimes();
    expect(keytabDescriptor1.getGroupName()).andReturn("hadoop").anyTimes();
    expect(keytabDescriptor1.getGroupAccess()).andReturn("").anyTimes();
    expect(keytabDescriptor1.getConfiguration()).andReturn("service1-site/component1.keytab.file").anyTimes();
    expect(keytabDescriptor1.isCachable()).andReturn(false).anyTimes();

    final KerberosKeytabDescriptor keytabDescriptor2 = createMock(KerberosKeytabDescriptor.class);
    expect(keytabDescriptor2.getFile()).andReturn("${keytab_dir}/service2.keytab").anyTimes();
    expect(keytabDescriptor2.getOwnerName()).andReturn("service2").anyTimes();
    expect(keytabDescriptor2.getOwnerAccess()).andReturn("rw").anyTimes();
    expect(keytabDescriptor2.getGroupName()).andReturn("hadoop").anyTimes();
    expect(keytabDescriptor2.getGroupAccess()).andReturn("").anyTimes();
    expect(keytabDescriptor2.getConfiguration()).andReturn("service2-site/component2.keytab.file").anyTimes();
    expect(keytabDescriptor2.isCachable()).andReturn(false).anyTimes();

    final KerberosKeytabDescriptor keytabDescriptorService1 = createMock(KerberosKeytabDescriptor.class);
    expect(keytabDescriptorService1.getFile()).andReturn("${keytab_dir}/service1.service.keytab").anyTimes();
    expect(keytabDescriptorService1.getOwnerName()).andReturn("service1").anyTimes();
    expect(keytabDescriptorService1.getOwnerAccess()).andReturn("rw").anyTimes();
    expect(keytabDescriptorService1.getGroupName()).andReturn("hadoop").anyTimes();
    expect(keytabDescriptorService1.getGroupAccess()).andReturn("").anyTimes();
    expect(keytabDescriptorService1.getConfiguration()).andReturn("service1-site/service1.keytab.file").anyTimes();
    expect(keytabDescriptorService1.isCachable()).andReturn(false).anyTimes();

    final KerberosIdentityDescriptor identityDescriptor1 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor1.getName()).andReturn("identity1").anyTimes();
    expect(identityDescriptor1.getReference()).andReturn(null).anyTimes();
    expect(identityDescriptor1.getPrincipalDescriptor()).andReturn(principalDescriptor1).anyTimes();
    expect(identityDescriptor1.getKeytabDescriptor()).andReturn(keytabDescriptor1).anyTimes();
    expect(identityDescriptor1.shouldInclude(EasyMock.anyObject())).andReturn(true).anyTimes();
    expect(identityDescriptor1.getWhen()).andReturn(null).anyTimes();

    final KerberosIdentityDescriptor identityDescriptor2 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor2.getName()).andReturn("identity2").anyTimes();
    expect(identityDescriptor2.getReference()).andReturn(null).anyTimes();
    expect(identityDescriptor2.getPrincipalDescriptor()).andReturn(principalDescriptor2).anyTimes();
    expect(identityDescriptor2.getKeytabDescriptor()).andReturn(keytabDescriptor2).anyTimes();
    expect(identityDescriptor2.shouldInclude(EasyMock.anyObject())).andReturn(true).anyTimes();
    expect(identityDescriptor2.getWhen()).andReturn(null).anyTimes();

    final KerberosIdentityDescriptor identityDescriptorService1 = createMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptorService1.getName()).andReturn("identity3").anyTimes();
    expect(identityDescriptorService1.getReference()).andReturn(null).anyTimes();
    expect(identityDescriptorService1.getPrincipalDescriptor()).andReturn(principalDescriptorService1).anyTimes();
    expect(identityDescriptorService1.getKeytabDescriptor()).andReturn(keytabDescriptorService1).anyTimes();
    expect(identityDescriptorService1.shouldInclude(EasyMock.anyObject())).andReturn(true).anyTimes();
    expect(identityDescriptorService1.getWhen()).andReturn(null).anyTimes();

    final KerberosComponentDescriptor componentDescriptor1 = createMock(KerberosComponentDescriptor.class);
    expect(componentDescriptor1.getIdentities(eq(true), EasyMock.anyObject())).andReturn(Collections.singletonList(identityDescriptor1)).anyTimes();

    final KerberosComponentDescriptor componentDescriptor2 = createMock(KerberosComponentDescriptor.class);
    expect(componentDescriptor2.getIdentities(eq(true), EasyMock.anyObject())).andReturn(Collections.singletonList(identityDescriptor2)).anyTimes();

    final KerberosServiceDescriptor serviceDescriptor1 = createMock(KerberosServiceDescriptor.class);
    expect(serviceDescriptor1.getComponent("COMPONENT1")).andReturn(componentDescriptor1).anyTimes();
    expect(serviceDescriptor1.getIdentities(eq(true), EasyMock.anyObject())).andReturn(Collections.singletonList(identityDescriptorService1)).anyTimes();

    final KerberosServiceDescriptor serviceDescriptor2 = createMock(KerberosServiceDescriptor.class);
    expect(serviceDescriptor2.getComponent("COMPONENT2")).andReturn(componentDescriptor2).anyTimes();
    expect(serviceDescriptor2.getIdentities(eq(true), EasyMock.anyObject())).andReturn(null).anyTimes();

    final KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getProperties()).andReturn(new HashMap<String, String>() {
      {
        put(KerberosHelper.DEFAULT_REALM, "EXAMPLE.COM");
      }
    }).anyTimes();
    expect(kerberosDescriptor.getService("KERBEROS")).andReturn(null).anyTimes();
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).anyTimes();
    expect(kerberosDescriptor.getService("SERVICE2")).andReturn(serviceDescriptor2).anyTimes();

    setupKerberosDescriptor(kerberosDescriptor);

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    Map<String, Collection<KerberosIdentityDescriptor>> identities;
    identities = kerberosHelper.getActiveIdentities(clusterName, hostName, serviceName, componentName, replaceHostNames);

    verifyAll();

    return identities;
  }

  private void addAmbariServerIdentity(Map<String, String> kerberosEnvProperties) throws Exception {

    KerberosHelperImpl kerberosHelper = injector.getInstance(KerberosHelperImpl.class);

    boolean createAmbariIdentities = kerberosHelper.createAmbariIdentities(kerberosEnvProperties);

    KerberosIdentityDescriptor ambariKerberosIdentity = null;

    KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    if (createAmbariIdentities) {
      String ambariServerPrincipalName = "ambari-server${principal_suffix}@${realm}";
      KerberosPrincipalType ambariServerPrincipalType = KerberosPrincipalType.USER;
      String ambariServerKeytabFilePath = new File("ambari.server.keytab").getAbsolutePath();

      ambariKerberosIdentity = createMockIdentityDescriptor(
          KerberosHelper.AMBARI_SERVER_KERBEROS_IDENTITY_NAME,
          createMockPrincipalDescriptor(ambariServerPrincipalName, ambariServerPrincipalType, "ambari", null),
          createMockKeytabDescriptor(ambariServerKeytabFilePath, null));

      ArrayList<KerberosIdentityDescriptor> ambarServerComponent1Identities = new ArrayList<>();
      ambarServerComponent1Identities.add(ambariKerberosIdentity);

      ambarServerComponent1Identities.add(createMockIdentityDescriptor(
          "ambari-server_spnego",
          createMockPrincipalDescriptor("HTTP/_HOST@${realm}", KerberosPrincipalType.SERVICE, null, null),
          createMockKeytabDescriptor("spnego.service.keytab", null)));

      HashMap<String, KerberosComponentDescriptor> ambariServerComponentDescriptorMap = new HashMap<>();
      KerberosComponentDescriptor componentDescrptor = createMockComponentDescriptor(RootComponent.AMBARI_SERVER.name(), ambarServerComponent1Identities, null);
      ambariServerComponentDescriptorMap.put(RootComponent.AMBARI_SERVER.name(), componentDescrptor);

      KerberosServiceDescriptor ambariServiceKerberosDescriptor = createMockServiceDescriptor(RootService.AMBARI.name(), ambariServerComponentDescriptorMap, null, false);
      expect(ambariServiceKerberosDescriptor.getComponent(RootComponent.AMBARI_SERVER.name())).andReturn(componentDescrptor).once();

      expect(kerberosDescriptor.getService(RootService.AMBARI.name())).andReturn(ambariServiceKerberosDescriptor).once();
    }

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    List<KerberosIdentityDescriptor> identities = (createAmbariIdentities)
        ? kerberosHelper.getAmbariServerIdentities(kerberosDescriptor)
        : new ArrayList<>();

    verifyAll();

    if (createAmbariIdentities) {
      Assert.assertEquals(2, identities.size());
      Assert.assertSame(ambariKerberosIdentity, identities.get(0));
    } else {
      Assert.assertTrue(identities.isEmpty());
    }
  }

  private KerberosConfigurationDescriptor createMockConfigurationDescriptor(Map<String, String> properties) {
    KerberosConfigurationDescriptor descriptor = createMock(KerberosConfigurationDescriptor.class);
    expect(descriptor.getProperties()).andReturn(properties).anyTimes();
    return descriptor;
  }

  private KerberosKeytabDescriptor createMockKeytabDescriptor(String file, String configuration) {
    KerberosKeytabDescriptor descriptor = createMock(KerberosKeytabDescriptor.class);
    expect(descriptor.getFile()).andReturn(file).anyTimes();
    expect(descriptor.getConfiguration()).andReturn(configuration).anyTimes();
    expect(descriptor.getOwnerName()).andReturn("user1").anyTimes();
    expect(descriptor.getOwnerAccess()).andReturn("rw").anyTimes();
    expect(descriptor.getGroupName()).andReturn("groupA").anyTimes();
    expect(descriptor.getGroupAccess()).andReturn("r").anyTimes();
    return descriptor;
  }

  private KerberosPrincipalDescriptor createMockPrincipalDescriptor(String value,
                                                                    KerberosPrincipalType type,
                                                                    String localUsername,
                                                                    String configuration) {
    KerberosPrincipalDescriptor descriptor = createMock(KerberosPrincipalDescriptor.class);
    expect(descriptor.getValue()).andReturn(value).anyTimes();
    expect(descriptor.getType()).andReturn(type).anyTimes();
    expect(descriptor.getLocalUsername()).andReturn(localUsername).anyTimes();
    expect(descriptor.getConfiguration()).andReturn(configuration).anyTimes();
    return descriptor;
  }

  private KerberosServiceDescriptor createMockServiceDescriptor(String serviceName,
                                                                HashMap<String, KerberosComponentDescriptor> componentMap,
                                                                List<KerberosIdentityDescriptor> identities,
                                                                boolean shouldPreconfigure)
      throws AmbariException {
    KerberosServiceDescriptor descriptor = createMock(KerberosServiceDescriptor.class);
    expect(descriptor.getName()).andReturn(serviceName).anyTimes();
    expect(descriptor.getComponents()).andReturn(componentMap).anyTimes();
    expect(descriptor.getIdentities(eq(true), EasyMock.anyObject())).andReturn(identities).anyTimes();
    expect(descriptor.getAuthToLocalProperties()).andReturn(null).anyTimes();
    expect(descriptor.shouldPreconfigure()).andReturn(shouldPreconfigure).anyTimes();
    return descriptor;
  }

  private KerberosIdentityDescriptor createMockIdentityDescriptor(String name,
                                                                  KerberosPrincipalDescriptor principalDescriptor,
                                                                  KerberosKeytabDescriptor keytabDescriptor) {
    KerberosIdentityDescriptor descriptor = createMock(KerberosIdentityDescriptor.class);
    expect(descriptor.getName()).andReturn(name).anyTimes();
    expect(descriptor.getPrincipalDescriptor()).andReturn(principalDescriptor).anyTimes();
    expect(descriptor.getKeytabDescriptor()).andReturn(keytabDescriptor).anyTimes();
    return descriptor;
  }

  private KerberosComponentDescriptor createMockComponentDescriptor(String componentName,
                                                                    List<KerberosIdentityDescriptor> identities,
                                                                    Map<String, KerberosConfigurationDescriptor> configurations)
      throws AmbariException {
    KerberosComponentDescriptor descriptor = createMock(KerberosComponentDescriptor.class);
    expect(descriptor.getName()).andReturn(componentName).anyTimes();
    expect(descriptor.getIdentities(eq(true), EasyMock.anyObject())).andReturn(identities).anyTimes();
    expect(descriptor.getConfigurations(true)).andReturn(configurations).anyTimes();
    expect(descriptor.getAuthToLocalProperties()).andReturn(null).anyTimes();
    return descriptor;
  }

  private ServiceComponentHost createMockServiceComponentHost(State state) {
    ServiceComponentHost serviceComponentHost = createMock(ServiceComponentHost.class);
    expect(serviceComponentHost.getDesiredState()).andReturn(state).anyTimes();
    return serviceComponentHost;
  }

  private ServiceComponent createMockComponent(String componentName, boolean isMasterComponent, Map<String, ServiceComponentHost> hosts) {
    ServiceComponent component = createMock(ServiceComponent.class);
    expect(component.getName()).andReturn(componentName).anyTimes();
    expect(component.isMasterComponent()).andReturn(isMasterComponent).anyTimes();
    expect(component.isClientComponent()).andReturn(!isMasterComponent).anyTimes();
    expect(component.getServiceComponentHosts()).andReturn(hosts).anyTimes();
    return component;
  }

  private Service createMockService(String serviceName, Map<String, ServiceComponent> componentMap) {
    Service service = createMock(Service.class);
    expect(service.getDesiredStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
    expect(service.getName()).andReturn(serviceName).anyTimes();
    expect(service.getServiceComponents()).andReturn(componentMap).anyTimes();
    return service;
  }

  private Host createMockHost(String hostname) {
    Host host = createMock(Host.class);
    expect(host.getHostName()).andReturn(hostname).anyTimes();
    expect(host.getState()).andReturn(HostState.HEALTHY).anyTimes();
    expect(host.getCurrentPingPort()).andReturn(1).anyTimes();
    expect(host.getRackInfo()).andReturn("rack1").anyTimes();
    expect(host.getIPv4()).andReturn("1.2.3.4").anyTimes();
    return host;
  }

  private Cluster createMockCluster(String clusterName, Collection<Host> hosts, SecurityType securityType, Config krb5ConfConfig, Config kerberosEnvConfig) {
    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getHosts()).andReturn(hosts).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getSecurityType()).andReturn(securityType).anyTimes();
    expect(cluster.getDesiredConfigByType("krb5-conf")).andReturn(krb5ConfConfig).anyTimes();
    expect(cluster.getDesiredConfigByType("kerberos-env")).andReturn(kerberosEnvConfig).anyTimes();
    expect(cluster.getClusterName()).andReturn(clusterName).anyTimes();
    expect(cluster.getCurrentStackVersion())
        .andReturn(new StackId("HDP", "2.2"))
        .anyTimes();

    return cluster;
  }

  private KerberosDescriptor createKerberosDescriptor() throws AmbariException {
    String json = "{" +
        "  \"services\": [" +
        "    {" +
        "      \"name\": \"EXISTING_SERVICE\"," +
        "      \"components\": [" +
        "        {" +
        "          \"name\": \"EXISTING_SERVICE_MASTER\"," +
        "          \"identities\": [" +
        "            {" +
        "              \"name\": \"existing_service_principal\"," +
        "              \"principal\": {" +
        "                \"value\": \"${existing-service-env/service_user}/_HOST@${realm}\"," +
        "                \"type\": \"service\"," +
        "                \"configuration\": \"existing-service-env/service_principal_name\"," +
        "                \"local_username\": \"${existing-service-env/service_user}\"" +
        "              }," +
        "              \"keytab\": {" +
        "                \"file\": \"${keytab_dir}/existing_service.service.keytab\"," +
        "                \"owner\": {" +
        "                  \"name\": \"${existing-service-env/service_user}\"," +
        "                  \"access\": \"r\"" +
        "                }," +
        "                \"group\": {" +
        "                  \"name\": \"${cluster-env/user_group}\"," +
        "                  \"access\": \"\"" +
        "                }," +
        "                \"configuration\": \"existing-service-env/service_keytab_path\"" +
        "              }" +
        "            }" +
        "          ]," +
        "          \"configurations\": [" +
        "            {" +
        "              \"existing-service-site\": {" +
        "                \"kerberos.secured\": \"true\"" +
        "              }" +
        "            }," +
        "            {" +
        "              \"core-site\": {" +
        "                \"hadoop.proxyuser.${existing-service-env/service_user}.groups\": \"${hadoop-env/proxyuser_group}\"," +
        "                \"hadoop.proxyuser.${existing-service-env/service_user}.hosts\": \"${clusterHostInfo/existing_service_master_hosts}\"" +
        "              }" +
        "            }" +
        "          ]" +
        "        }" +
        "      ]" +
        "    }," +
        "    {" +
        "      \"name\": \"PRECONFIGURE_SERVICE\"," +
        "      \"preconfigure\": true," +
        "      \"components\": [" +
        "        {" +
        "          \"name\": \"PRECONFIGURE_SERVICE_MASTER\"," +
        "          \"identities\": [" +
        "            {" +
        "              \"name\": \"preconfigure_service_principal\"," +
        "              \"principal\": {" +
        "                \"value\": \"${preconfigure-service-env/service_user}/_HOST@${realm}\"," +
        "                \"type\": \"service\"," +
        "                \"configuration\": \"preconfigure-service-env/service_principal_name\"," +
        "                \"local_username\": \"${preconfigure-service-env/service_user}\"" +
        "              }," +
        "              \"keytab\": {" +
        "                \"file\": \"${keytab_dir}/preconfigure_service.service.keytab\"," +
        "                \"owner\": {" +
        "                  \"name\": \"${preconfigure-service-env/service_user}\"," +
        "                  \"access\": \"r\"" +
        "                }," +
        "                \"group\": {" +
        "                  \"name\": \"${cluster-env/user_group}\"," +
        "                  \"access\": \"\"" +
        "                }," +
        "                \"configuration\": \"preconfigure-service-env/service_keytab_path\"" +
        "              }" +
        "            }" +
        "          ]," +
        "          \"configurations\": [" +
        "            {" +
        "              \"preconfigure-service-site\": {" +
        "                \"kerberos.secured\": \"true\"" +
        "              }" +
        "            }," +
        "            {" +
        "              \"core-site\": {" +
        "                \"hadoop.proxyuser.${preconfigure-service-env/service_user}.groups\": \"${hadoop-env/proxyuser_group}\"," +
        "                \"hadoop.proxyuser.${preconfigure-service-env/service_user}.hosts\": \"${clusterHostInfo/preconfigure_service_master_hosts}\"" +
        "              }" +
        "            }" +
        "          ]" +
        "        }" +
        "      ]" +
        "    }" +
        "  ]" +
        "}";

    return new KerberosDescriptorFactory().createInstance(json);
  }
}
