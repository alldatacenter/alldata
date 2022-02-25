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
package org.apache.ambari.server.checks;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariCustomCommandExecutionHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.scheduler.ExecutionSchedulerImpl;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

@RunWith(EasyMockRunner.class)
public class KerberosAdminPersistedCredentialCheckTest extends EasyMockSupport {
  @Mock
  private UpgradeHelper upgradeHelper;

  @Test
  public void testMissingCredentialStoreKerberosEnabledManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, true, false, false);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());
    Assert.assertTrue(result.getFailReason().startsWith("Ambari's credential store has not been configured."));
  }

  @Test
  public void testMissingCredentialStoreKerberosEnabledNotManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, false, false, false);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testMissingCredentialStoreKerberosNotEnabled() throws Exception {
    UpgradeCheckResult result = executeCheck(false, false, false, false);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testMissingCredentialKerberosEnabledManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, true, true, false);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());
    Assert.assertTrue(result.getFailReason().startsWith("The KDC administrator credential has not been stored in the persisted credential store."));
  }

  @Test
  public void testMissingCredentialKerberosEnabledNotManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, false, true, false);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testMissingCredentialKerberosNotEnabled() throws Exception {
    UpgradeCheckResult result = executeCheck(false, true, true, false);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testCredentialsSetKerberosNotEnabled() throws Exception {
    UpgradeCheckResult result = executeCheck(false, false, true, true);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testCredentialsSetKerberosEnabledNotManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, false, true, true);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testCredentialsSetKerberosEnabledManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, true, true, true);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }

  private UpgradeCheckResult executeCheck(boolean kerberosEnabled, boolean manageIdentities, boolean credentialStoreInitialized, boolean credentialSet) throws Exception {

    String clusterName = "c1";

    Map<String, String> checkProperties = new HashMap<>();
    RepositoryVersion repositoryVersion = createNiceMock(RepositoryVersion.class);

    ClusterInformation clusterInformation = new ClusterInformation(clusterName, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        repositoryVersion, checkProperties, null);

    expect(upgradeHelper.suggestUpgradePack(eq(clusterName), anyObject(), anyObject(), eq(Direction.UPGRADE), eq(UpgradeType.ROLLING), anyObject()))
      .andReturn(upgradePackWithRegenKeytab()).anyTimes();

    DesiredConfig desiredKerberosEnv = createMock(DesiredConfig.class);
    expect(desiredKerberosEnv.getTag()).andReturn("tag").anyTimes();

    Map<String, DesiredConfig> desiredConfigs = Collections.singletonMap("kerberos-env", desiredKerberosEnv);

    Config kerberosEnv = createMock(Config.class);
    expect(kerberosEnv.getProperties()).andReturn(Collections.singletonMap("manage_identities", manageIdentities ? "true" : "false")).anyTimes();

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getSecurityType()).andReturn(kerberosEnabled ? SecurityType.KERBEROS : SecurityType.NONE).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigs).anyTimes();
    expect(cluster.getConfig("kerberos-env", "tag")).andReturn(kerberosEnv).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(createNiceMock(StackId.class)).anyTimes();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();

    Credential credential = createMock(Credential.class);

    Injector injector = getInjector();
    CredentialStoreService credentialStoreProvider = injector.getInstance(CredentialStoreService.class);
    expect(credentialStoreProvider.isInitialized(CredentialStoreType.PERSISTED)).andReturn(credentialStoreInitialized).anyTimes();
    expect(credentialStoreProvider.getCredential(clusterName, KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS, CredentialStoreType.PERSISTED)).andReturn(credentialSet ? credential : null).anyTimes();

    Provider<Clusters> clustersProvider = () -> clusters;

    replayAll();

    injector.getInstance(AmbariMetaInfo.class).init();

    KerberosAdminPersistedCredentialCheck check = new KerberosAdminPersistedCredentialCheck();
    injector.injectMembers(check);

    check.clustersProvider = clustersProvider;
    UpgradeCheckResult result = check.perform(request);

    verifyAll();

    return result;
  }

  private UpgradePack upgradePackWithRegenKeytab() {
    UpgradePack upgradePack = createMock(UpgradePack.class);
    expect(upgradePack.anyGroupTaskMatch(anyObject())).andReturn(true).anyTimes();
    return upgradePack;
  }

  Injector getInjector() {
    return Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        PartialNiceMockBinder.newBuilder().addActionDBAccessorConfigsBindings().addFactoriesInstallBinding()
            .addPasswordEncryptorBindings().addLdapBindings().build().configure(binder());

        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionSchedulerImpl.class));
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
        bind(HostRoleCommandFactory.class).toInstance(createNiceMock(HostRoleCommandFactoryImpl.class));
        bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
        bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
        bind(HookService.class).to(UserHookService.class);
        bind(PersistedState.class).to(PersistedStateImpl.class);
        bind(SecurityHelper.class).toInstance(createNiceMock(SecurityHelper.class));
        bind(AmbariCustomCommandExecutionHelper.class).toInstance(createNiceMock(AmbariCustomCommandExecutionHelper.class));
        bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementController.class));
        bind(AmbariMetaInfo.class).toInstance(createNiceMock(AmbariMetaInfo.class));
        bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
        bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
        bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        bind(ConfigHelper.class).toInstance(createNiceMock(ConfigHelper.class));
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
        bind(ArtifactDAO.class).toInstance(createNiceMock(ArtifactDAO.class));
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(UpgradeHelper.class).toInstance(upgradeHelper);
        bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelper.class));
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));

        bind(CredentialStoreService.class).toInstance(createMock(CredentialStoreService.class));
      }
    });
  }
}
