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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.security.SecurePasswordHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptorTest;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptorTest;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

/**
 * ClusterKerberosDescriptorResourceProviderTest unit tests.
 */
@SuppressWarnings("unchecked")
public class ClusterKerberosDescriptorResourceProviderTest extends EasyMockSupport {

  private static final Gson GSON = new Gson();

  private static final Map<String, Object> STACK_MAP;

  private static final Map<String, Object> USER_MAP;

  private static final Map<String, Object> COMPOSITE_MAP;

  static {
    TreeMap<String, Object> stackProperties = new TreeMap<>();
    stackProperties.put("realm", "EXAMPLE.COM");
    stackProperties.put("some.property", "Hello World");

    Collection<String> authToLocalRules = new ArrayList<>();
    authToLocalRules.add("global.name.rules");

    TreeMap<String, Object> stackServices = new TreeMap<>();
    stackServices.put((String) KerberosServiceDescriptorTest.MAP_VALUE.get("name"), KerberosServiceDescriptorTest.MAP_VALUE);

    TreeMap<String, Object> stackClusterConfProperties = new TreeMap<>();
    stackClusterConfProperties.put("property1", "red");

    TreeMap<String, Object> stackClusterConf = new TreeMap<>();
    stackClusterConf.put("cluster-conf", stackClusterConfProperties);

    TreeMap<String, Object> stackConfigurations = new TreeMap<>();
    stackConfigurations.put("cluster-conf", stackClusterConf);

    TreeMap<String, Object> stackSharedIdentityKeytabOwner = new TreeMap<>();
    stackSharedIdentityKeytabOwner.put("name", "root");
    stackSharedIdentityKeytabOwner.put("access", "rw");

    TreeMap<String, Object> sharedIdentityKeytabGroup = new TreeMap<>();
    sharedIdentityKeytabGroup.put("name", "hadoop");
    sharedIdentityKeytabGroup.put("access", "r");

    TreeMap<String, Object> stackSharedIdentityKeytab = new TreeMap<>();
    stackSharedIdentityKeytab.put("file", "/etc/security/keytabs/subject.service.keytab");
    stackSharedIdentityKeytab.put("owner", stackSharedIdentityKeytabOwner);
    stackSharedIdentityKeytab.put("group", sharedIdentityKeytabGroup);
    stackSharedIdentityKeytab.put("configuration", "service-site/service2.component.keytab.file");

    TreeMap<String, Object> stackSharedIdentity = new TreeMap<>();
    stackSharedIdentity.put("name", "shared");
    stackSharedIdentity.put("principal", new TreeMap<>(KerberosPrincipalDescriptorTest.MAP_VALUE));
    stackSharedIdentity.put("keytab", stackSharedIdentityKeytab);

    TreeMap<String, Object> stackIdentities = new TreeMap<>();
    stackIdentities.put("shared", stackSharedIdentity);

    STACK_MAP = new TreeMap<>();
    STACK_MAP.put("properties", stackProperties);
    STACK_MAP.put(AbstractKerberosDescriptor.Type.AUTH_TO_LOCAL_PROPERTY.getDescriptorPluralName(), authToLocalRules);
    STACK_MAP.put(AbstractKerberosDescriptor.Type.SERVICE.getDescriptorPluralName(), stackServices.values());
    STACK_MAP.put(AbstractKerberosDescriptor.Type.CONFIGURATION.getDescriptorPluralName(), stackConfigurations.values());
    STACK_MAP.put(AbstractKerberosDescriptor.Type.IDENTITY.getDescriptorPluralName(), stackIdentities.values());

    TreeMap<String, Object> userProperties = new TreeMap<>();
    userProperties.put("realm", "HWX.COM");
    userProperties.put("some.property", "Hello World");

    TreeMap<String, Object> userClusterConfProperties = new TreeMap<>();
    userClusterConfProperties.put("property1", "blue");
    userClusterConfProperties.put("property2", "orange");

    TreeMap<String, Object> userClusterConf = new TreeMap<>();
    userClusterConf.put("cluster-conf", userClusterConfProperties);

    TreeMap<String, Object> userConfigurations = new TreeMap<>();
    userConfigurations.put("cluster-conf", userClusterConf);

    TreeMap<String, Object> userSharedIdentityKeytabOwner = new TreeMap<>();
    userSharedIdentityKeytabOwner.put("name", "root");
    userSharedIdentityKeytabOwner.put("access", "rw");

    TreeMap<String, Object> userSharedIdentityKeytabGroup = new TreeMap<>();
    userSharedIdentityKeytabGroup.put("name", "hadoop");
    userSharedIdentityKeytabGroup.put("access", "r");

    TreeMap<String, Object> userSharedIdentityKeytab = new TreeMap<>();
    userSharedIdentityKeytab.put("file", "/etc/security/keytabs/subject.service.keytab");
    userSharedIdentityKeytab.put("owner", userSharedIdentityKeytabOwner);
    userSharedIdentityKeytab.put("group", userSharedIdentityKeytabGroup);
    userSharedIdentityKeytab.put("configuration", "service-site/service2.component.keytab.file");

    TreeMap<String, Object> userSharedIdentity = new TreeMap<>();
    userSharedIdentity.put("name", "shared");
    userSharedIdentity.put("principal", new TreeMap<>(KerberosPrincipalDescriptorTest.MAP_VALUE));
    userSharedIdentity.put("keytab", userSharedIdentityKeytab);

    TreeMap<String, Object> userIdentities = new TreeMap<>();
    userIdentities.put("shared", userSharedIdentity);

    USER_MAP = new TreeMap<>();
    USER_MAP.put("properties", userProperties);
    USER_MAP.put(AbstractKerberosDescriptor.Type.CONFIGURATION.getDescriptorPluralName(), userConfigurations.values());
    USER_MAP.put(AbstractKerberosDescriptor.Type.IDENTITY.getDescriptorPluralName(), userIdentities.values());

    COMPOSITE_MAP = new TreeMap<>();
    COMPOSITE_MAP.putAll(STACK_MAP);
    COMPOSITE_MAP.putAll(USER_MAP);
  }

  private Injector injector;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();

        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(SecurePasswordHelper.class).toInstance(new SecurePasswordHelper());
        bind(Configuration.class).toInstance(new Configuration(properties));
        bind(KerberosDescriptorFactory.class).toInstance(new KerberosDescriptorFactory());
      }
    });
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test(expected = SystemException.class)
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = SystemException.class)
  public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = SystemException.class)
  public void testCreateResourcesAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testCreateResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    ClusterKerberosDescriptorResourceProvider resourceProvider = new ClusterKerberosDescriptorResourceProvider(managementController);
    injector.injectMembers(resourceProvider);

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider) provider).addObserver(observer);

    provider.createResources(request);

    verifyAll();
  }

  @Test
  public void testGetResourcesAsAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesAsClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesAsClusterOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testGetResourcesAsServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetResourcesAsServiceOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceOperator());
  }

  @Test
  public void testGetResourcesAsClusterUser() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterUser());
  }

  private void testGetResources(Authentication authentication) throws Exception {

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).atLeastOnce();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).atLeastOnce();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        managementController);

    Predicate predicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();

    Set<Resource> results = provider.getResources(request, predicate);
    Assert.assertEquals(3, results.size());

    verifyAll();
  }

  @Test
  public void testGetResourcesWithPredicateAsAdministrator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesWithPredicateAsClusterAdministrator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesWithPredicateAsClusterOperator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testGetResourcesWithPredicateAsServiceAdministrator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetResourcesWithPredicateAsServiceOperator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createServiceOperator());
  }

  @Test
  public void testGetResourcesWithPredicateAsClusterUser() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createClusterUser());
  }

  private void testGetResourcesWithPredicate(Authentication authentication) throws Exception {

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).atLeastOnce();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).atLeastOnce();

    KerberosDescriptorFactory kerberosDescriptorFactory = injector.getInstance(KerberosDescriptorFactory.class);
    KerberosDescriptor stackKerberosDescriptor = kerberosDescriptorFactory.createInstance(STACK_MAP);
    KerberosDescriptor userKerberosDescriptor = kerberosDescriptorFactory.createInstance(USER_MAP);
    KerberosDescriptor compositeKerberosDescriptor = kerberosDescriptorFactory.createInstance(STACK_MAP);
    compositeKerberosDescriptor.update(userKerberosDescriptor);

    KerberosHelper kerberosHelper = createMock(KerberosHelper.class);
    expect(kerberosHelper.getKerberosDescriptor(eq(KerberosHelper.KerberosDescriptorType.STACK), eq(cluster), eq(false), anyObject(Collection.class), eq(false)))
        .andReturn(stackKerberosDescriptor).atLeastOnce();
    expect(kerberosHelper.getKerberosDescriptor(eq(KerberosHelper.KerberosDescriptorType.USER), eq(cluster), eq(false), anyObject(Collection.class), eq(false)))
        .andReturn(userKerberosDescriptor).atLeastOnce();
    expect(kerberosHelper.getKerberosDescriptor(eq(KerberosHelper.KerberosDescriptorType.COMPOSITE), eq(cluster), eq(false), anyObject(Collection.class), eq(false)))
        .andReturn(compositeKerberosDescriptor).atLeastOnce();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();
    expect(managementController.getKerberosHelper()).andReturn(kerberosHelper).atLeastOnce();

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).atLeastOnce();
    expect(request.getRequestInfoProperties()).andReturn(Collections.emptyMap()).atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        managementController);

    Predicate clusterPredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate typePredicate;
    Set<Resource> results;

    // --------------
    // Get the STACK Kerberos Descriptor
    typePredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("STACK")
        .toPredicate();

    results = provider.getResources(request, new AndPredicate(clusterPredicate, typePredicate));
    Assert.assertEquals(1, results.size());

    testResults("STACK", STACK_MAP, results);

    // --------------
    // Get the USER Kerberos Descriptor
    typePredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("USER")
        .toPredicate();

    results = provider.getResources(request, new AndPredicate(clusterPredicate, typePredicate));
    Assert.assertEquals(1, results.size());

    testResults("USER", USER_MAP, results);

    // --------------
    // Get the COMPOSITE Kerberos Descriptor
    typePredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("COMPOSITE")
        .toPredicate();

    results = provider.getResources(request, new AndPredicate(clusterPredicate, typePredicate));
    Assert.assertEquals(1, results.size());

    testResults("COMPOSITE", COMPOSITE_MAP, results);

    verifyAll();
  }

  @Test
  public void testGetResourcesWithPredicateAndDirectivesAsAdministrator() throws Exception {
    testGetResourcesWithPredicateAndDirectives(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesWithPredicateAndDirectivesAsClusterAdministrator() throws Exception {
    testGetResourcesWithPredicateAndDirectives(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesWithPredicateAndDirectivesAsClusterOperator() throws Exception {
    testGetResourcesWithPredicateAndDirectives(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testGetResourcesWithPredicateAndDirectivesAsServiceAdministrator() throws Exception {
    testGetResourcesWithPredicateAndDirectives(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetResourcesWithPredicateAndDirectivesAsServiceOperator() throws Exception {
    testGetResourcesWithPredicateAndDirectives(TestAuthenticationFactory.createServiceOperator());
  }

  @Test
  public void testGetResourcesWithPredicateAndDirectivesAsClusterUser() throws Exception {
    testGetResourcesWithPredicateAndDirectives(TestAuthenticationFactory.createClusterUser());
  }

  private void testGetResourcesWithPredicateAndDirectives(Authentication authentication) throws Exception {

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).atLeastOnce();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).atLeastOnce();

    KerberosDescriptorFactory kerberosDescriptorFactory = injector.getInstance(KerberosDescriptorFactory.class);
    KerberosDescriptor stackKerberosDescriptor = kerberosDescriptorFactory.createInstance(STACK_MAP);
    KerberosDescriptor userKerberosDescriptor = kerberosDescriptorFactory.createInstance(USER_MAP);
    KerberosDescriptor compositeKerberosDescriptor = kerberosDescriptorFactory.createInstance(STACK_MAP);
    compositeKerberosDescriptor.update(userKerberosDescriptor);

    Capture<? extends Collection<String>> captureAdditionalServices = newCapture(CaptureType.ALL);

    KerberosHelper kerberosHelper = createMock(KerberosHelper.class);
    expect(kerberosHelper.getKerberosDescriptor(eq(KerberosHelper.KerberosDescriptorType.STACK), eq(cluster), eq(true), capture(captureAdditionalServices), eq(false)))
        .andReturn(stackKerberosDescriptor).atLeastOnce();
    expect(kerberosHelper.getKerberosDescriptor(eq(KerberosHelper.KerberosDescriptorType.USER), eq(cluster), eq(true), capture(captureAdditionalServices), eq(false)))
        .andReturn(userKerberosDescriptor).atLeastOnce();
    expect(kerberosHelper.getKerberosDescriptor(eq(KerberosHelper.KerberosDescriptorType.COMPOSITE), eq(cluster), eq(true), capture(captureAdditionalServices), eq(false)))
        .andReturn(compositeKerberosDescriptor).atLeastOnce();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();
    expect(managementController.getKerberosHelper()).andReturn(kerberosHelper).atLeastOnce();

    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(ClusterKerberosDescriptorResourceProvider.DIRECTIVE_EVALUATE_WHEN_CLAUSE, "true");
    requestInfoProperties.put(ClusterKerberosDescriptorResourceProvider.DIRECTIVE_ADDITIONAL_SERVICES, "HIVE, TEZ,PIG");

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).atLeastOnce();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        managementController);

    Predicate clusterPredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate typePredicate;
    Set<Resource> results;

    // --------------
    // Get the STACK Kerberos Descriptor
    typePredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("STACK")
        .toPredicate();

    results = provider.getResources(request, new AndPredicate(clusterPredicate, typePredicate));
    Assert.assertEquals(1, results.size());

    for (Resource result : results) {
      Assert.assertEquals("c1", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("STACK", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID));

      // Reconstruct the deconstructed Kerberos Descriptor
      Map partial1 = result.getPropertiesMap().get(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID);
      Map partial2 = result.getPropertiesMap().get("KerberosDescriptor/kerberos_descriptor/properties");
      partial1.put("properties", partial2);

      Assert.assertEquals(GSON.toJson(STACK_MAP), GSON.toJson(partial1));
    }

    // --------------
    // Get the USER Kerberos Descriptor
    typePredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("USER")
        .toPredicate();

    results = provider.getResources(request, new AndPredicate(clusterPredicate, typePredicate));
    Assert.assertEquals(1, results.size());

    for (Resource result : results) {
      Assert.assertEquals("c1", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("USER", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID));

      // Reconstruct the deconstructed Kerberos Descriptor
      Map partial1 = result.getPropertiesMap().get(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID);
      Map partial2 = result.getPropertiesMap().get("KerberosDescriptor/kerberos_descriptor/properties");
      partial1.put("properties", partial2);

      Assert.assertEquals(GSON.toJson(USER_MAP), GSON.toJson(partial1));
    }

    // --------------
    // Get the COMPOSITE Kerberos Descriptor
    typePredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("COMPOSITE")
        .toPredicate();

    results = provider.getResources(request, new AndPredicate(clusterPredicate, typePredicate));
    Assert.assertEquals(1, results.size());

    testResults("COMPOSITE", COMPOSITE_MAP, results);

    verifyAll();

    List<? extends Collection<String>> capturedValues = captureAdditionalServices.getValues();
    Assert.assertEquals(3, capturedValues.size());

    for (Collection<String> capturedValue : capturedValues) {
      Assert.assertEquals(3, capturedValue.size());
      Assert.assertTrue(capturedValue.contains("HIVE"));
      Assert.assertTrue(capturedValue.contains("PIG"));
      Assert.assertTrue(capturedValue.contains("TEZ"));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetResourcesWithInvalidKerberosDescriptorTypeAsAdministrator() throws Exception {
    testGetResourcesWithInvalidKerberosDescriptorType(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetResourcesWithInvalidKerberosDescriptorTypeAsClusterAdministrator() throws Exception {
    testGetResourcesWithInvalidKerberosDescriptorType(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetResourcesWithInvalidKerberosDescriptorTypeAsServiceAdministrator() throws Exception {
    testGetResourcesWithInvalidKerberosDescriptorType(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResourcesWithInvalidKerberosDescriptorType(Authentication authentication) throws Exception {

    StackId stackVersion = createMock(StackId.class);
    expect(stackVersion.getStackName()).andReturn("stackName").atLeastOnce();
    expect(stackVersion.getStackVersion()).andReturn("stackVersion").atLeastOnce();

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).atLeastOnce();
    expect(cluster.getCurrentStackVersion()).andReturn(stackVersion).atLeastOnce();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).atLeastOnce();

    KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.toMap()).andReturn(STACK_MAP).atLeastOnce();

    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
    expect(metaInfo.getKerberosDescriptor("stackName", "stackVersion", false)).andReturn(kerberosDescriptor).atLeastOnce();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();
    expect(managementController.getAmbariMetaInfo()).andReturn(metaInfo).atLeastOnce();

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        managementController);

    Predicate predicate1 = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate predicate2 = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("BOGUS")
        .toPredicate();
    Predicate predicate = new AndPredicate(predicate1, predicate2);

    try {
      provider.getResources(request, predicate);
      Assert.fail("Expected NoSuchResourceException not thrown");
    } catch (NoSuchResourceException e) {
      // expected
    }

    verifyAll();
  }

  @Test
  public void testGetResourcesWithoutPredicateAsAdministrator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesWithoutPredicateAsClusterAdministrator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesWithoutPredicateAsClusterOperator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testGetResourcesWithoutPredicateAsServiceAdministrator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetResourcesWithoutPredicateAsServiceOperator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createServiceOperator());
  }

  @Test
  public void testGetResourcesWithoutPredicateAsClusterUser() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createClusterUser());
  }

  private void testGetResourcesWithoutPredicate(Authentication authentication) throws Exception {

    Clusters clusters = createMock(Clusters.class);

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        managementController);


    Set<Resource> results = provider.getResources(request, null);
    Assert.assertTrue(results.isEmpty());

    verifyAll();
  }

  @Test(expected = SystemException.class)
  public void testUpdateResourcesAsAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = SystemException.class)
  public void testUpdateResourcesAsClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = SystemException.class)
  public void testUpdateResourcesAsServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testUpdateResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    ClusterKerberosDescriptorResourceProvider resourceProvider = new ClusterKerberosDescriptorResourceProvider(managementController);
    injector.injectMembers(resourceProvider);

    replayAll();
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        managementController);

    provider.createResources(request);

    verifyAll();
  }

  @Test(expected = SystemException.class)
  public void testDeleteResourcesAsAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = SystemException.class)
  public void testDeleteResourcesAsClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = SystemException.class)
  public void testDeleteResourcesAsServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    ClusterKerberosDescriptorResourceProvider resourceProvider = new ClusterKerberosDescriptorResourceProvider(managementController);
    injector.injectMembers(resourceProvider);

    replayAll();
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        managementController);

    Predicate predicate1 = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate predicate2 = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("alias1")
        .toPredicate();
    Predicate predicate = new AndPredicate(predicate1, predicate2);

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    verifyAll();
  }

  private void testResults(String type, Map<String, Object> expectedData, Set<Resource> results) {
    for (Resource result : results) {
      Assert.assertEquals("c1", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals(type, result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID));

      // Reconstruct the deconstructed Kerberos Descriptor
      Map partial1 = result.getPropertiesMap().get(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID);
      Map partial2 = result.getPropertiesMap().get("KerberosDescriptor/kerberos_descriptor/properties");
      partial1.put("properties", partial2);

      Assert.assertEquals(GSON.toJson(expectedData), GSON.toJson(partial1));
    }
  }
}

