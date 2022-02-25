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
package org.apache.ambari.server.testutils;

import static org.easymock.EasyMock.createNiceMock;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.actionmanager.StageFactoryImpl;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.AuditLoggerDefaultImpl;
import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.hooks.AmbariEventFactory;
import org.apache.ambari.server.hooks.HookContext;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.PostUserCreationHookContext;
import org.apache.ambari.server.hooks.users.UserCreatedEvent;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.scheduler.ExecutionSchedulerImpl;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.EncryptionService;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceComponentImpl;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroupImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionImpl;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
import org.easymock.EasyMockSupport;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.persist.UnitOfWork;

public class PartialNiceMockBinder implements Module {

  private final List<Configurer> configurers;
  private final EasyMockSupport easyMockSupport;

  private PartialNiceMockBinder(EasyMockSupport easyMockSupport) {
    configurers = new ArrayList<>();
    this.easyMockSupport = easyMockSupport;
  }

  public static PartialNiceMockBinder.Builder newBuilder() {
    return new PartialNiceMockBinder(new EasyMockSupport()).new Builder();
  }

  public static PartialNiceMockBinder.Builder newBuilder(EasyMockSupport easyMockSupport) {
    return new PartialNiceMockBinder(easyMockSupport).new Builder();
  }

  @Override
  public void configure(Binder binder) {
    configurers.forEach(configurer -> configurer.configure(binder));
  }

  public class Builder {
    public Builder() {

    }

    public Builder addAlertDefinitionBinding() {
      configurers.add((Binder binder) -> {
          binder.bind(Cluster.class).toInstance(createNiceMock(Cluster.class));
          binder.bind(DaoUtils.class).toInstance(createNiceMock(DaoUtils.class));
          binder.bind(AlertDefinitionDAO.class).toInstance(createNiceMock(AlertDefinitionDAO.class));
      });
      addDBAccessorBinding();
      addAmbariMetaInfoBinding();
      return this;
    }

    public Builder addAmbariMetaInfoBinding(AmbariManagementController ambariManagementController) {
      configurers.add((Binder binder) -> {
          binder.bind(PersistedState.class).toInstance(easyMockSupport.createNiceMock(PersistedState.class));
          binder.bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
          binder.bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
          binder.bind(UnitOfWork.class).toInstance(easyMockSupport.createNiceMock(UnitOfWork.class));
          binder.bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
          binder.bind(StageFactory.class).to(StageFactoryImpl.class);
          binder.bind(AuditLogger.class).toInstance(easyMockSupport.createNiceMock(AuditLoggerDefaultImpl.class));
          binder.bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
          binder.bind(HookService.class).to(UserHookService.class);
          binder.bind(ServiceComponentHostFactory.class).toInstance(easyMockSupport.createNiceMock(ServiceComponentHostFactory.class));
          binder.bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
          binder.bind(CredentialStoreService.class).toInstance(easyMockSupport.createNiceMock(CredentialStoreService.class));
          binder.bind(AmbariManagementController.class).toInstance(ambariManagementController);
          binder.bind(ExecutionScheduler.class).to(ExecutionSchedulerImpl.class);
          binder.bind(KerberosHelper.class).toInstance(easyMockSupport.createNiceMock(KerberosHelper.class));
      });
      addConfigsBindings();
      addFactoriesInstallBinding();
      addPasswordEncryptorBindings();
      return this;
    }

    public Builder addAmbariMetaInfoBinding() {
      return addAmbariMetaInfoBinding(easyMockSupport.createNiceMock(AmbariManagementController.class));
    }

    public Builder addAlertDefinitionDAOBinding() {
      addAmbariMetaInfoBinding();
      return this;
    }

    public Builder addClustersBinding(AmbariManagementController ambariManagementController) {
      addAmbariMetaInfoBinding(ambariManagementController);
      return this;
    }

    public Builder addClustersBinding() {
      addAmbariMetaInfoBinding();
      return this;
    }

    public Builder addDBAccessorBinding(DBAccessor dbAccessor) {
      configurers.add((Binder binder) -> {
          binder.bind(StackManagerFactory.class).toInstance(easyMockSupport.createNiceMock(StackManagerFactory.class));
          binder.bind(MpackManagerFactory.class).toInstance(easyMockSupport.createNiceMock(MpackManagerFactory.class));
          binder.bind(EntityManager.class).toInstance(easyMockSupport.createNiceMock(EntityManager.class));
          binder.bind(DBAccessor.class).toInstance(dbAccessor);
          binder.bind(Clusters.class).toInstance(easyMockSupport.createNiceMock(Clusters.class));
          binder.bind(OsFamily.class).toInstance(easyMockSupport.createNiceMock(OsFamily.class));
      });
      return this;
    }

    public Builder addDBAccessorBinding() {
      configurers.add((Binder binder) -> {
          binder.bind(StackManagerFactory.class).toInstance(easyMockSupport.createNiceMock(StackManagerFactory.class));
          binder.bind(MpackManagerFactory.class).toInstance(easyMockSupport.createNiceMock(MpackManagerFactory.class));
          binder.bind(EntityManager.class).toInstance(easyMockSupport.createNiceMock(EntityManager.class));
          binder.bind(DBAccessor.class).toInstance(easyMockSupport.createNiceMock(DBAccessor.class));
          binder.bind(Clusters.class).toInstance(easyMockSupport.createNiceMock(Clusters.class));
          binder.bind(OsFamily.class).toInstance(easyMockSupport.createNiceMock(OsFamily.class));
      });
      return this;
    }

    public Builder addConfigsBindings() {
      addHostRoleCommandsConfigsBindings();
      addActionSchedulerConfigsBindings();
      addActionDBAccessorConfigsBindings();
      return this;
    }

    public Builder addPasswordEncryptorBindings() {
      configurers.add((Binder binder) -> {
        binder.bind(EncryptionService.class).toInstance(easyMockSupport.createNiceMock(EncryptionService.class));
        binder.bind(new TypeLiteral<Encryptor<Config>>() {}).annotatedWith(Names.named("ConfigPropertiesEncryptor")).toInstance(Encryptor.NONE);
        binder.bind(new TypeLiteral<Encryptor<AgentConfigsUpdateEvent>>() {}).annotatedWith(Names.named("AgentConfigEncryptor")).toInstance(Encryptor.NONE);
        binder.bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
      });
      return this;
    }

    public Builder addHostRoleCommandsConfigsBindings() {
      configurers.add((Binder binder) -> {
          binder.bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_ENABLED)).to(true);
          binder.bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_SIZE)).to(10000L);
          binder.bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES)).to(30L);
      });
      return this;
    }

    public Builder addActionSchedulerConfigsBindings() {
      configurers.add((Binder binder) -> {
          binder.bindConstant().annotatedWith(Names.named("actionTimeout")).to(600000L);
          binder.bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(1L);
      });
      return this;
    }

    public Builder addActionDBAccessorConfigsBindings() {
      configurers.add((Binder binder) ->
          binder.bindConstant().annotatedWith(Names.named("executionCommandCacheSize")).to(10000L)
      );
      return this;
    }
    
    public Builder addLdapBindings() {
      configurers.add((Binder binder) -> {
        binder.bind(LdapFacade.class).toInstance(easyMockSupport.createNiceMock(LdapFacade.class));
        binder.bind(AmbariLdapConfigurationProvider.class).toInstance(easyMockSupport.createNiceMock(AmbariLdapConfigurationProvider.class));
      });
      return this;
    }

    public Builder addFactoriesInstallBinding() {
      configurers.add((Binder binder) -> {
          binder.install(new FactoryModuleBuilder().build(ConfigureClusterTaskFactory.class));
          binder.install(new FactoryModuleBuilder().implement(Config.class, ConfigImpl.class).build(ConfigFactory.class));
          binder.install(new FactoryModuleBuilder().build(RequestFactory.class));
          binder.install(new FactoryModuleBuilder().implement(HookContext.class, PostUserCreationHookContext.class)
              .build(HookContextFactory.class));
          binder.install(new FactoryModuleBuilder().implement(
              ServiceComponent.class, ServiceComponentImpl.class).build(
              ServiceComponentFactory.class));
          binder.install(new FactoryModuleBuilder().build(RoleGraphFactory.class));
          binder.install(new FactoryModuleBuilder().implement(RequestExecution.class,
              RequestExecutionImpl.class).build(RequestExecutionFactory.class));
          binder.install(new FactoryModuleBuilder().implement(
              ConfigGroup.class, ConfigGroupImpl.class).build(ConfigGroupFactory.class));
          binder.install(new FactoryModuleBuilder().implement(AmbariEvent.class, Names.named("userCreated"), UserCreatedEvent.class)
              .build(AmbariEventFactory.class));

          binder.install(new FactoryModuleBuilder().implement(
              Host.class, HostImpl.class).build(HostFactory.class));
          binder.install(new FactoryModuleBuilder().implement(
              Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
          binder.install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
          binder.install(new FactoryModuleBuilder().implement(
              Service.class, ServiceImpl.class).build(ServiceFactory.class));
      });
      return this;
    }

    public PartialNiceMockBinder build() {
      return PartialNiceMockBinder.this;
    }

  }

  private interface Configurer {
    void configure(Binder binder);
  }
}
