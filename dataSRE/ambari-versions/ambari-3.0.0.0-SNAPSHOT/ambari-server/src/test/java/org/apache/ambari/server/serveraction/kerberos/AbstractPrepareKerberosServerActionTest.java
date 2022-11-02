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

package org.apache.ambari.server.serveraction.kerberos;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.KerberosHelperImpl;
import org.apache.ambari.server.controller.UpdateConfigurationPolicy;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class AbstractPrepareKerberosServerActionTest extends EasyMockSupport {
  private static final String KERBEROS_DESCRIPTOR_JSON = "" +
      "{" +
      "  \"identities\": [" +
      "    {" +
      "      \"keytab\": {" +
      "        \"file\": \"${keytab_dir}/spnego.service.keytab\"," +
      "        \"group\": {" +
      "          \"access\": \"r\"," +
      "          \"name\": \"${cluster-env/user_group}\"" +
      "        }," +
      "        \"owner\": {" +
      "          \"access\": \"r\"," +
      "          \"name\": \"root\"" +
      "        }" +
      "      }," +
      "      \"name\": \"spnego\"," +
      "      \"principal\": {" +
      "        \"configuration\": null," +
      "        \"local_username\": null," +
      "        \"type\": \"service\"," +
      "        \"value\": \"HTTP/_HOST@${realm}\"" +
      "      }" +
      "    }" +
      "  ]," +
      "  \"services\": [" +
      "    {" +
      "      \"components\": [" +
      "        {" +
      "          \"identities\": [" +
      "            {" +
      "              \"name\": \"service_master_spnego_identity\"," +
      "              \"reference\": \"/spnego\"" +
      "            }" +
      "          ]," +
      "          \"name\": \"SERVICE_MASTER\"" +
      "        }" +
      "      ]," +
      "      \"configurations\": [" +
      "        {" +
      "          \"service-site\": {" +
      "            \"property1\": \"property1_updated_value\"," +
      "            \"property2\": \"property2_updated_value\"" +
      "          }" +
      "        }" +
      "      ]," +
      "      \"identities\": [" +
      "        {" +
      "          \"name\": \"service_identity\"," +
      "          \"keytab\": {" +
      "            \"configuration\": \"service-site/keytab_file_path\"," +
      "            \"file\": \"${keytab_dir}/service.service.keytab\"," +
      "            \"group\": {" +
      "              \"access\": \"r\"," +
      "              \"name\": \"${cluster-env/user_group}\"" +
      "            }," +
      "            \"owner\": {" +
      "              \"access\": \"r\"," +
      "              \"name\": \"${service-env/service_user}\"" +
      "            }" +
      "          }," +
      "          \"principal\": {" +
      "            \"configuration\": \"service-site/principal_name\"," +
      "            \"local_username\": \"${service-env/service_user}\"," +
      "            \"type\": \"service\"," +
      "            \"value\": \"${service-env/service_user}/_HOST@${realm}\"" +
      "          }" +
      "        }" +
      "      ]," +
      "      \"name\": \"SERVICE\"" +
      "    }" +
      "  ]," +
      "  \"properties\": {" +
      "    \"additional_realms\": \"\"," +
      "    \"keytab_dir\": \"/etc/security/keytabs\"," +
      "    \"principal_suffix\": \"-${cluster_name|toLower()}\"," +
      "    \"realm\": \"${kerberos-env/realm}\"" +
      "  }" +
      "}";

  private class TestKerberosServerAction extends AbstractPrepareKerberosServerAction {

    @Override
    protected String getClusterName() {
      return "c1";
    }

    @Override
    public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) {
      return null;
    }
  }

  private Injector injector;
  private final AbstractPrepareKerberosServerAction testKerberosServerAction = new TestKerberosServerAction();

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariMetaInfo.class).toInstance(createNiceMock(AmbariMetaInfo.class));
        bind(KerberosHelper.class).to(KerberosHelperImpl.class);
        bind(KerberosIdentityDataFileWriterFactory.class).toInstance(createNiceMock(KerberosIdentityDataFileWriterFactory.class));
        bind(KerberosConfigDataFileWriterFactory.class).toInstance(createNiceMock(KerberosConfigDataFileWriterFactory.class));
        bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
        bind(ConfigHelper.class).toInstance(createNiceMock(ConfigHelper.class));
        bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
        bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementController.class));
        bind(ActionDBAccessor.class).toInstance(createNiceMock(ActionDBAccessor.class));
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(ConfigFactory.class).toInstance(createNiceMock(ConfigFactory.class));
        bind(ConfigGroupFactory.class).toInstance(createNiceMock(ConfigGroupFactory.class));
        bind(CredentialStoreService.class).toInstance(createNiceMock(CredentialStoreService.class));
        bind(RequestExecutionFactory.class).toInstance(createNiceMock(RequestExecutionFactory.class));
        bind(RequestFactory.class).toInstance(createNiceMock(RequestFactory.class));
        bind(RoleCommandOrderProvider.class).toInstance(createNiceMock(RoleCommandOrderProvider.class));
        bind(RoleGraphFactory.class).toInstance(createNiceMock(RoleGraphFactory.class));
        bind(AbstractRootServiceResponseFactory.class).toInstance(createNiceMock(AbstractRootServiceResponseFactory.class));
        bind(ServiceComponentFactory.class).toInstance(createNiceMock(ServiceComponentFactory.class));
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
        bind(HostRoleCommandFactory.class).toInstance(createNiceMock(HostRoleCommandFactory.class));
        bind(HookContextFactory.class).toInstance(createNiceMock(HookContextFactory.class));
        bind(HookService.class).toInstance(createNiceMock(HookService.class));
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
        bind(PersistedState.class).toInstance(createNiceMock(PersistedState.class));
        bind(ConfigureClusterTaskFactory.class).toInstance(createNiceMock(ConfigureClusterTaskFactory.class));
        Provider<EntityManager> entityManagerProvider = createNiceMock(Provider.class);
        bind(EntityManager.class).toProvider(entityManagerProvider);
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
        bind(AmbariLdapConfigurationProvider.class).toInstance(createMock(AmbariLdapConfigurationProvider.class));
        bind(new TypeLiteral<Encryptor<AgentConfigsUpdateEvent>>() {}).annotatedWith(Names.named("AgentConfigEncryptor")).toInstance(Encryptor.NONE);
        bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
      }
    });

    injector.injectMembers(testKerberosServerAction);
  }

  /**
   * Test checks that {@code KerberosHelper.applyStackAdvisorUpdates} would be called with
   * full list of the services and not only list of services with KerberosDescriptior.
   * In this test HDFS service will have KerberosDescriptor, while Zookeeper not.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testProcessServiceComponentHosts() throws Exception {
    final Cluster cluster = createNiceMock(Cluster.class);
    final KerberosIdentityDataFileWriter kerberosIdentityDataFileWriter = createNiceMock(KerberosIdentityDataFileWriter.class);
    final KerberosDescriptor kerberosDescriptor = createNiceMock(KerberosDescriptor.class);
    final ServiceComponentHost serviceComponentHostHDFS = createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost serviceComponentHostZK = createNiceMock(ServiceComponentHost.class);
    final KerberosServiceDescriptor serviceDescriptor = createNiceMock(KerberosServiceDescriptor.class);
    final KerberosComponentDescriptor componentDescriptor = createNiceMock(KerberosComponentDescriptor.class);

    final String hdfsService = "HDFS";
    final String zookeeperService = "ZOOKEEPER";
    final String hostName = "host1";
    final String hdfsComponent = "DATANODE";
    final String zkComponent = "ZK";

    Collection<String> identityFilter = new ArrayList<>();
    Map<String, Map<String, String>> kerberosConfigurations = new HashMap<>();
    Map<String, Set<String>> propertiesToIgnore = new HashMap<>();
    Map<String, Map<String, String>> configurations = new HashMap<>();

    List<ServiceComponentHost> serviceComponentHosts = new ArrayList<ServiceComponentHost>() {{
      add(serviceComponentHostHDFS);
      add(serviceComponentHostZK);
    }};
    Map<String, Service> clusterServices = new HashMap<String, Service>() {{
      put(hdfsService, null);
      put(zookeeperService, null);
    }};

    KerberosIdentityDataFileWriterFactory kerberosIdentityDataFileWriterFactory = injector.getInstance(KerberosIdentityDataFileWriterFactory.class);
    expect(kerberosIdentityDataFileWriterFactory.createKerberosIdentityDataFileWriter(anyObject(File.class))).andReturn(kerberosIdentityDataFileWriter);

    // it's important to pass a copy of clusterServices
    expect(cluster.getServices()).andReturn(new HashMap<>(clusterServices)).atLeastOnce();

    expect(serviceComponentHostHDFS.getHostName()).andReturn(hostName).atLeastOnce();
    expect(serviceComponentHostHDFS.getServiceName()).andReturn(hdfsService).atLeastOnce();
    expect(serviceComponentHostHDFS.getServiceComponentName()).andReturn(hdfsComponent).atLeastOnce();
    expect(serviceComponentHostHDFS.getHost()).andReturn(createNiceMock(Host.class)).atLeastOnce();

    expect(serviceComponentHostZK.getHostName()).andReturn(hostName).atLeastOnce();
    expect(serviceComponentHostZK.getServiceName()).andReturn(zookeeperService).atLeastOnce();
    expect(serviceComponentHostZK.getServiceComponentName()).andReturn(zkComponent).atLeastOnce();
    expect(serviceComponentHostZK.getHost()).andReturn(createNiceMock(Host.class)).atLeastOnce();

    expect(kerberosDescriptor.getService(hdfsService)).andReturn(serviceDescriptor).once();

    expect(serviceDescriptor.getComponent(hdfsComponent)).andReturn(componentDescriptor).once();
    expect(componentDescriptor.getConfigurations(anyBoolean())).andReturn(null);

    replayAll();

    injector.getInstance(AmbariMetaInfo.class).init();

    testKerberosServerAction.processServiceComponentHosts(cluster,
        kerberosDescriptor,
        serviceComponentHosts,
        identityFilter,
        "",
        configurations, kerberosConfigurations,
        false, propertiesToIgnore);

    verifyAll();

    // Ensure the host and hostname values were set in the configuration context
    Assert.assertEquals("host1", configurations.get("").get("host"));
    Assert.assertEquals("host1", configurations.get("").get("hostname"));
  }

  @Test
  public void testProcessConfigurationChanges() throws Exception {
    // Existing property map....
    Map<String, String> serviceSiteProperties = new HashMap<>();
    serviceSiteProperties.put("property1", "property1_value");
    serviceSiteProperties.put("principal_name", "principal_name_value");
    serviceSiteProperties.put("keytab_file_path", "keytab_file_path_value");

    Map<String, Map<String, String>> effectiveProperties = new HashMap<>();
    effectiveProperties.put("service-site", serviceSiteProperties);

    // Updated property map....
    Map<String, String> updatedServiceSiteProperties = new HashMap<>();
    updatedServiceSiteProperties.put("property1", "property1_updated_value");
    updatedServiceSiteProperties.put("property2", "property2_updated_value");
    updatedServiceSiteProperties.put("principal_name", "principal_name_updated_value");
    updatedServiceSiteProperties.put("keytab_file_path", "keytab_file_path_updated_value");

    Map<String, Map<String, String>> kerberosConfigurations = new HashMap<>();
    kerberosConfigurations.put("service-site", updatedServiceSiteProperties);

    KerberosDescriptor kerberosDescriptor = new KerberosDescriptorFactory().createInstance(KERBEROS_DESCRIPTOR_JSON);

    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    expect(configHelper.getEffectiveConfigProperties(eq("c1"), eq(null)))
        .andReturn(effectiveProperties).anyTimes();

    KerberosConfigDataFileWriterFactory factory = injector.getInstance(KerberosConfigDataFileWriterFactory.class);

    ConfigWriterData dataCaptureAll = setupConfigWriter(factory);
    ConfigWriterData dataCaptureIdentitiesOnly = setupConfigWriter(factory);
    ConfigWriterData dataCaptureNewAndIdentities = setupConfigWriter(factory);
    ConfigWriterData dataCaptureNone = setupConfigWriter(factory);

    replayAll();

    injector.getInstance(AmbariMetaInfo.class).init();

    Map<String, String> expectedProperties;

    // Update all configurations
    testKerberosServerAction.processConfigurationChanges("test_directory",
        kerberosConfigurations, Collections.emptyMap(), kerberosDescriptor, UpdateConfigurationPolicy.ALL);

    expectedProperties = new HashMap<>();
    expectedProperties.put("property1", "property1_updated_value");
    expectedProperties.put("property2", "property2_updated_value");
    expectedProperties.put("principal_name", "principal_name_updated_value");
    expectedProperties.put("keytab_file_path", "keytab_file_path_updated_value");

    verifyDataCapture(dataCaptureAll, Collections.singletonMap("service-site", expectedProperties));

    // Update only identity configurations
    testKerberosServerAction.processConfigurationChanges("test_directory",
        kerberosConfigurations, Collections.emptyMap(), kerberosDescriptor, UpdateConfigurationPolicy.IDENTITIES_ONLY);

    expectedProperties = new HashMap<>();
    expectedProperties.put("principal_name", "principal_name_updated_value");
    expectedProperties.put("keytab_file_path", "keytab_file_path_updated_value");

    verifyDataCapture(dataCaptureIdentitiesOnly, Collections.singletonMap("service-site", expectedProperties));

    // Update new and identity configurations
    testKerberosServerAction.processConfigurationChanges("test_directory",
        kerberosConfigurations, Collections.emptyMap(), kerberosDescriptor, UpdateConfigurationPolicy.NEW_AND_IDENTITIES);

    expectedProperties = new HashMap<>();
    expectedProperties.put("property2", "property2_updated_value");
    expectedProperties.put("principal_name", "principal_name_updated_value");
    expectedProperties.put("keytab_file_path", "keytab_file_path_updated_value");

    verifyDataCapture(dataCaptureNewAndIdentities, Collections.singletonMap("service-site", expectedProperties));

    // Update no configurations
    testKerberosServerAction.processConfigurationChanges("test_directory",
        kerberosConfigurations, Collections.emptyMap(), kerberosDescriptor, UpdateConfigurationPolicy.NONE);

    verifyDataCapture(dataCaptureNone, Collections.emptyMap());

    verifyAll();

  }

  private void verifyDataCapture(ConfigWriterData configWriterData, Map<String, Map<String, String>> expectedConfigurations) {

    int expectedCaptures = 0;
    Collection<Map<String, String>> expectedValuesCollection = expectedConfigurations.values();
    for (Map<String, String> expectedValues : expectedValuesCollection) {
      expectedCaptures += expectedValues.size();
    }

    Capture<String> captureConfigType = configWriterData.getCaptureConfigType();
    if (expectedCaptures > 0) {
      Assert.assertTrue(captureConfigType.hasCaptured());
      List<String> valuesConfigType = captureConfigType.getValues();
      Assert.assertEquals(expectedCaptures, valuesConfigType.size());
    } else {
      Assert.assertFalse(captureConfigType.hasCaptured());
    }

    Capture<String> capturePropertyName = configWriterData.getCapturePropertyName();
    if (expectedCaptures > 0) {
      Assert.assertTrue(capturePropertyName.hasCaptured());
      List<String> valuesPropertyName = capturePropertyName.getValues();
      Assert.assertEquals(expectedCaptures, valuesPropertyName.size());
    } else {
      Assert.assertFalse(capturePropertyName.hasCaptured());
    }

    Capture<String> capturePropertyValue = configWriterData.getCapturePropertyValue();
    if (expectedCaptures > 0) {
      Assert.assertTrue(capturePropertyValue.hasCaptured());
      List<String> valuesPropertyValue = capturePropertyValue.getValues();
      Assert.assertEquals(expectedCaptures, valuesPropertyValue.size());
    } else {
      Assert.assertFalse(capturePropertyValue.hasCaptured());
    }

    if (expectedCaptures > 0) {
      int i = 0;
      List<String> valuesConfigType = captureConfigType.getValues();
      List<String> valuesPropertyName = capturePropertyName.getValues();
      List<String> valuesPropertyValue = capturePropertyValue.getValues();

      for (Map.Entry<String, Map<String, String>> entry : expectedConfigurations.entrySet()) {
        String configType = entry.getKey();
        Map<String, String> properties = entry.getValue();

        for(Map.Entry<String, String> property:properties.entrySet()) {
          Assert.assertEquals(configType, valuesConfigType.get(i));
          Assert.assertEquals(property.getKey(), valuesPropertyName.get(i));
          Assert.assertEquals(property.getValue(), valuesPropertyValue.get(i));
          i++;
        }
      }
    }

  }

  private ConfigWriterData setupConfigWriter(KerberosConfigDataFileWriterFactory factory) throws IOException {
    Capture<String> captureConfigType = newCapture(CaptureType.ALL);
    Capture<String> capturePropertyName = newCapture(CaptureType.ALL);
    Capture<String> capturePropertyValue = newCapture(CaptureType.ALL);

    KerberosConfigDataFileWriter mockWriter = createMock(KerberosConfigDataFileWriter.class);
    mockWriter.addRecord(capture(captureConfigType), capture(capturePropertyName), capture(capturePropertyValue), eq(KerberosConfigDataFileWriter.OPERATION_TYPE_SET));
    expectLastCall().anyTimes();
    mockWriter.close();
    expectLastCall().anyTimes();

    expect(factory.createKerberosConfigDataFileWriter(anyObject(File.class))).andReturn(mockWriter).once();

    return new ConfigWriterData(captureConfigType, capturePropertyName, capturePropertyValue);
  }

  private class ConfigWriterData {
    private final Capture<String> captureConfigType;
    private final Capture<String> capturePropertyName;
    private final Capture<String> capturePropertyValue;

    private ConfigWriterData(Capture<String> captureConfigType, Capture<String> capturePropertyName, Capture<String> capturePropertyValue) {
      this.captureConfigType = captureConfigType;
      this.capturePropertyName = capturePropertyName;
      this.capturePropertyValue = capturePropertyValue;
    }

    Capture<String> getCaptureConfigType() {
      return captureConfigType;
    }

    Capture<String> getCapturePropertyName() {
      return capturePropertyName;
    }

    Capture<String> getCapturePropertyValue() {
      return capturePropertyValue;
    }
  }
}
