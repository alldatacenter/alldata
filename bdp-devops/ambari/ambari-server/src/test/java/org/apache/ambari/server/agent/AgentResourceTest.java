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

package org.apache.ambari.server.agent;

import static org.easymock.EasyMock.createNiceMock;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.server.RandomPortJerseyTest;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapperFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.rest.AgentResource;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
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
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.scheduler.ExecutionSchedulerImpl;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.SecurityHelperImpl;
import org.apache.ambari.server.security.encryption.AESEncryptionService;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
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
import org.apache.ambari.server.state.ServiceComponentHost;
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
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.easymock.EasyMock;
import org.eclipse.jetty.server.session.SessionHandler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.test.framework.WebAppDescriptor;

import junit.framework.Assert;


public class AgentResourceTest extends RandomPortJerseyTest {
  static String PACKAGE_NAME = "org.apache.ambari.server.agent.rest";
  private static final Logger LOG = LoggerFactory.getLogger(AgentResourceTest.class);
  protected Client client;
  HeartBeatHandler handler;
  ActionManager actionManager;
  SessionHandler sessionHandler;
  Injector injector;
  AmbariMetaInfo ambariMetaInfo;
  OsFamily os_family;
  ActionDBAccessor actionDBAccessor;

  public AgentResourceTest() {
    super(new WebAppDescriptor.Builder(PACKAGE_NAME).servletClass(ServletContainer.class)
        .initParam("com.sun.jersey.api.json.POJOMappingFeature", "true")
        .build());
  }

  public static <T> T getJsonFormString(String json, Class<T> type) {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.serializeNulls();
    Gson gson = gsonBuilder.create();
    return gson.fromJson(json, type);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    handler = mock(HeartBeatHandler.class);
    injector = Guice.createInjector(new MockModule());
    injector.injectMembers(handler);
  }

  private JSONObject createDummyJSONRegister() throws JSONException {
    JSONObject json = new JSONObject();
    json.put("responseId", -1);
    json.put("timestamp", System.currentTimeMillis());
    json.put("hostname", "dummyHost");
    return json;
  }

  private JSONObject createDummyHeartBeat() throws JSONException {
    JSONObject json = new JSONObject();
    json.put("responseId", -1);
    json.put("timestamp", System.currentTimeMillis());
    json.put("hostname", "dummyHost");
    return json;
  }

  private JSONObject createDummyHeartBeatWithAgentEnv() throws JSONException {
    JSONObject json = new JSONObject();
    json.put("responseId", -1);
    json.put("timestamp", System.currentTimeMillis());
    json.put("hostname", "dummyHost");

    JSONObject agentEnv = new JSONObject();
    json.put("agentEnv", agentEnv);
    return json;
  }

  @Test
  public void agentRegistration() throws UniformInterfaceException, JSONException {
    RegistrationResponse response;
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    client = Client.create(clientConfig);
    WebResource webResource = client.resource(String.format("http://localhost:%d/register/dummyhost", getTestPort()));
    response = webResource.type(MediaType.APPLICATION_JSON)
        .post(RegistrationResponse.class, createDummyJSONRegister());
    LOG.info("Returned from Server responce=" + response);
    Assert.assertEquals(response.getResponseStatus(), RegistrationStatus.OK);
  }

  @Test
  public void agentHeartBeat() throws UniformInterfaceException, JSONException {
    HeartBeatResponse response;
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    client = Client.create(clientConfig);
    WebResource webResource = client.resource(String.format("http://localhost:%d/heartbeat/dummyhost", getTestPort()));
    response = webResource.type(MediaType.APPLICATION_JSON)
        .post(HeartBeatResponse.class, createDummyHeartBeat());
    LOG.info("Returned from Server: "
        + " response=" + response);
    Assert.assertEquals(response.getResponseId(), 0L);
  }

  @Test
  public void agentHeartBeatWithEnv() throws UniformInterfaceException, JSONException {
    HeartBeatResponse response;
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    client = Client.create(clientConfig);
    WebResource webResource = client.resource(String.format("http://localhost:%d/heartbeat/dummyhost", getTestPort()));
    response = webResource.type(MediaType.APPLICATION_JSON)
        .post(HeartBeatResponse.class, createDummyHeartBeatWithAgentEnv());
    LOG.info("Returned from Server: "
        + " response=" + response);
    Assert.assertEquals(response.getResponseId(), 0L);
  }

  @Test
  public void deserializeClasses() {
    String DirectoryJSON = "[{name:'/var/lib', type:'directory'},{name:'b', type:'directory'}]";
    String PackageDetailJSON = "[{name:'abc', version:'2.3', repoName:'HDP'},{name:'abc', version:'3.3', repoName:'HDP-epel'}]";
    String ExistingUserJSON = "[{name:'hdfs', homeDir:'/var/lib/hadoop', status:''}, " +
            "{name:'ambari_qa', homeDir:'/var/home/ambari_qa',status:'None'}]";
    String JavaProcJSON = "[{user:'root', pid:'355', hadoop:'True', command:'cmd'}, " +
            "{user:'hdfs', pid:'325', hadoop:'False', command:'cmd = 2'}]";
    String AlternativeJSON = "[{name:'/etc/alternatives/hdfs-conf', target:'/etc/hadoop/conf.dist'}, " +
            "{name:'abc', target:'def'}]";
    String AgentEnvJSON = "{\"alternatives\": " + AlternativeJSON +
            ", \"existingUsers\": "+ ExistingUserJSON +
            ", \"umask\": \"18\", \"installedPackages\": "+
            PackageDetailJSON +", \"stackFoldersAndFiles\": "+ DirectoryJSON +
            ", \"firewallRunning\": \"true\", \"firewallName\": \"iptables\", \"transparentHugePage\": \"never\", \"hasUnlimitedJcePolicy\" : true}";
    AgentEnv.Directory[] dirs = getJsonFormString(
            DirectoryJSON, AgentEnv.Directory[].class);
    Assert.assertEquals("/var/lib", dirs[0].getName());
    Assert.assertEquals("directory", dirs[1].getType());

    AgentEnv.PackageDetail[] pkgs = getJsonFormString(
        PackageDetailJSON, AgentEnv.PackageDetail[].class);
    Assert.assertEquals("abc", pkgs[0].getName());
    Assert.assertEquals("HDP", pkgs[0].getRepoName());
    Assert.assertEquals("3.3", pkgs[1].getVersion());

    AgentEnv.ExistingUser[] users = getJsonFormString(
        ExistingUserJSON, AgentEnv.ExistingUser[].class);
    Assert.assertEquals("hdfs", users[0].getUserName());
    Assert.assertEquals("/var/lib/hadoop", users[0].getUserHomeDir());
    Assert.assertEquals("None", users[1].getUserStatus());

    AgentEnv.JavaProc[] procs = getJsonFormString(
        JavaProcJSON, AgentEnv.JavaProc[].class);
    Assert.assertEquals("root", procs[0].getUser());
    Assert.assertEquals(355, procs[0].getPid());
    Assert.assertEquals("cmd = 2", procs[1].getCommand());
    Assert.assertEquals(false, procs[1].isHadoop());

    AgentEnv.Alternative[] alternatives = getJsonFormString(
        AlternativeJSON, AgentEnv.Alternative[].class);
    Assert.assertEquals("/etc/alternatives/hdfs-conf", alternatives[0].getName());
    Assert.assertEquals("/etc/hadoop/conf.dist", alternatives[0].getTarget());
    Assert.assertEquals("abc", alternatives[1].getName());
    Assert.assertEquals("def", alternatives[1].getTarget());

    AgentEnv agentEnv = getJsonFormString(
            AgentEnvJSON, AgentEnv.class);
    Assert.assertTrue(18 == agentEnv.getUmask());
    Assert.assertEquals("never", agentEnv.getTransparentHugePage());
    Assert.assertTrue(agentEnv.getHasUnlimitedJcePolicy());
    Assert.assertTrue(Boolean.TRUE == agentEnv.getFirewallRunning());
    Assert.assertEquals("iptables", agentEnv.getFirewallName());
    Assert.assertEquals("/etc/alternatives/hdfs-conf", agentEnv.getAlternatives()[0].getName());
    Assert.assertEquals("/etc/hadoop/conf.dist", agentEnv.getAlternatives()[0].getTarget());
    Assert.assertEquals("abc", agentEnv.getAlternatives()[1].getName());
    Assert.assertEquals("def", agentEnv.getAlternatives()[1].getTarget());
    Assert.assertEquals("abc", agentEnv.getInstalledPackages()[0].getName());
    Assert.assertEquals("HDP", agentEnv.getInstalledPackages()[0].getRepoName());
    Assert.assertEquals("3.3", agentEnv.getInstalledPackages()[1].getVersion());
    Assert.assertEquals("hdfs", agentEnv.getExistingUsers()[0].getUserName());
    Assert.assertEquals("/var/lib/hadoop", agentEnv.getExistingUsers()[0].getUserHomeDir());
    Assert.assertEquals("None", agentEnv.getExistingUsers()[1].getUserStatus());
    Assert.assertEquals("/var/lib", agentEnv.getStackFoldersAndFiles()[0].getName());
    Assert.assertEquals("directory", agentEnv.getStackFoldersAndFiles()[1].getType());
  }

  @Test
  public void agentComponents() {
    ComponentsResponse response;
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    client = Client.create(clientConfig);
    WebResource webResource = client.resource(String.format("http://localhost:%d/components/dummycluster", getTestPort()));
    response = webResource.get(ComponentsResponse.class);
    Assert.assertEquals(response.getClusterName(), "dummycluster");
  }

  public class MockModule extends AbstractModule {

    RegistrationResponse response = new RegistrationResponse();
    HeartBeatResponse hresponse = new HeartBeatResponse();
    ComponentsResponse componentsResponse = new ComponentsResponse();

    @Override
    protected void configure() {
      installDependencies();

      handler = mock(HeartBeatHandler.class);
      response.setResponseStatus(RegistrationStatus.OK);
      hresponse.setResponseId(0L);
      componentsResponse.setClusterName("dummycluster");
      try {
        when(handler.handleRegistration(any(Register.class))).thenReturn(
            response);
        when(handler.handleHeartBeat(any(HeartBeat.class))).thenReturn(
            hresponse);
        when(handler.handleComponents(any(String.class))).thenReturn(
            componentsResponse);
      } catch (Exception ex) {
        // The test will fail anyway
      }
      requestStaticInjection(AgentResource.class);
      os_family = mock(OsFamily.class);
      actionManager = mock(ActionManager.class);
      ambariMetaInfo = mock(AmbariMetaInfo.class);
      actionDBAccessor = mock(ActionDBAccessor.class);
      sessionHandler = mock(SessionHandler.class);
      bind(OsFamily.class).toInstance(os_family);
      bind(ActionDBAccessor.class).toInstance(actionDBAccessor);
      bind(ActionManager.class).toInstance(actionManager);
      bind(SessionHandler.class).toInstance(sessionHandler);
      bind(AgentCommand.class).to(ExecutionCommand.class);
      bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
      bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
      bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
      bind(HookService.class).to(UserHookService.class);
      bind(ExecutionScheduler.class).to(ExecutionSchedulerImpl.class);
      bind(HeartBeatHandler.class).toInstance(handler);
      bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
      bind(DBAccessor.class).toInstance(mock(DBAccessor.class));
      bind(HostRoleCommandDAO.class).toInstance(mock(HostRoleCommandDAO.class));
      bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
      bind(HostDAO.class).toInstance(createNiceMock(HostDAO.class));
      bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
      bind(PersistedState.class).toInstance(createNiceMock(PersistedState.class));
      bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
      bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementController.class));
      bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelper.class));
      bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
      bind(EncryptionService.class).to(AESEncryptionService.class);
      bind(new TypeLiteral<Encryptor<AgentConfigsUpdateEvent>>() {}).annotatedWith(Names.named("AgentConfigEncryptor")).toInstance(Encryptor.NONE);
      bind(new TypeLiteral<Encryptor<Config>>() {}).annotatedWith(Names.named("ConfigPropertiesEncryptor")).toInstance(Encryptor.NONE);
      bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
      bind(LdapFacade.class).toInstance(createNiceMock(LdapFacade.class));
      bind(AmbariLdapConfigurationProvider.class).toInstance(createNiceMock(AmbariLdapConfigurationProvider.class));
    }

    private void installDependencies() {
      install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
      install(new FactoryModuleBuilder().build(RoleGraphFactory.class));
      install(new FactoryModuleBuilder().implement(
          Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
      install(new FactoryModuleBuilder().implement(
          Host.class, HostImpl.class).build(HostFactory.class));
      install(new FactoryModuleBuilder().implement(
          Service.class, ServiceImpl.class).build(ServiceFactory.class));
      install(new FactoryModuleBuilder().implement(
          ServiceComponent.class, ServiceComponentImpl.class).build(
          ServiceComponentFactory.class));
      install(new FactoryModuleBuilder().implement(
          ServiceComponentHost.class, ServiceComponentHostImpl.class).build(
          ServiceComponentHostFactory.class));
      install(new FactoryModuleBuilder().implement(
          Config.class, ConfigImpl.class).build(ConfigFactory.class));
      install(new FactoryModuleBuilder().implement(
        ConfigGroup.class, ConfigGroupImpl.class).build(ConfigGroupFactory.class));
      install(new FactoryModuleBuilder().implement(RequestExecution.class,
        RequestExecutionImpl.class).build(RequestExecutionFactory.class));
      install(new FactoryModuleBuilder().build(StageFactory.class));
      install(new FactoryModuleBuilder().build(ExecutionCommandWrapperFactory.class));
      install(new FactoryModuleBuilder().build(ConfigureClusterTaskFactory.class));

      install(new FactoryModuleBuilder().build(RequestFactory.class));
      install(new FactoryModuleBuilder().implement(AmbariEvent.class, Names.named("userCreated"), UserCreatedEvent.class)
          .build(AmbariEventFactory.class));
      install(new FactoryModuleBuilder().implement(HookContext.class, PostUserCreationHookContext.class)
          .build(HookContextFactory.class));


      bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
      bind(SecurityHelper.class).toInstance(SecurityHelperImpl.getInstance());
      bind(AmbariEventPublisher.class).toInstance(EasyMock.createMock(AmbariEventPublisher.class));
      bind(StackManagerFactory.class).toInstance(
          EasyMock.createMock(StackManagerFactory.class));
    }
  }
}
