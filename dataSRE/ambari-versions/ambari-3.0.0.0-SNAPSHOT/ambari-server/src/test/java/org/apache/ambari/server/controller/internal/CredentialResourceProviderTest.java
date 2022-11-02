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
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.security.SecurePasswordHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.security.encryption.MasterKeyServiceImpl;
import org.apache.ambari.server.state.stack.OsFamily;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;


/**
 * CredentialResourceProviderTest unit tests.
 */
@SuppressWarnings("unchecked")
public class CredentialResourceProviderTest {

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  private Injector injector;

  @Before
  public void setUp() throws Exception {
    tmpFolder.create();
    final File masterKeyFile = tmpFolder.newFile(Configuration.MASTER_KEY_FILENAME_DEFAULT);
    Assert.assertTrue(new MasterKeyServiceImpl("dummyKey").initializeMasterKeyFile(masterKeyFile, "secret"));

    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();

        properties.setProperty(Configuration.MASTER_KEY_LOCATION.getKey(), tmpFolder.getRoot().getAbsolutePath());
        properties.setProperty(Configuration.MASTER_KEYSTORE_LOCATION.getKey(), tmpFolder.getRoot().getAbsolutePath());

        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(SecurePasswordHelper.class).toInstance(new SecurePasswordHelper());
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });
  }

  @After
  public void tearDown() throws Exception {
    tmpFolder.delete();
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testCreateResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    Set<Map<String, Object>> setProperties = getCredentialTestProperties("c1", "alias1", "username1", "password1", CredentialStoreType.TEMPORARY);

    // set expectations
    expect(request.getProperties()).andReturn(setProperties);

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider) provider).addObserver(observer);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Credential, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    Assert.assertEquals(request, lastEvent.getRequest());
    Assert.assertNull(lastEvent.getPredicate());

    verify(request, factory, managementController);
  }

  @Test
  public void testCreateResources_FailMissingAlias() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    Set<Map<String, Object>> setProperties = getCredentialTestProperties("c1", null, "username1", "password1", CredentialStoreType.TEMPORARY);

    // set expectations
    expect(request.getProperties()).andReturn(setProperties);

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);

    try {
      provider.createResources(request);
      Assert.fail("Expected exception due to missing alias");
    } catch (IllegalArgumentException e) {
      // expected
    }

    verify(request, factory, managementController);
  }

  @Test
  public void testCreateResources_FailMissingPrincipal() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    Set<Map<String, Object>> setProperties = getCredentialTestProperties("c1", "alias1", null, "password1", CredentialStoreType.TEMPORARY);

    // set expectations
    expect(request.getProperties()).andReturn(setProperties);

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);

    try {
      provider.createResources(request);
      Assert.fail("Expected exception due to missing alias");
    } catch (IllegalArgumentException e) {
      // expected
    }

    verify(request, factory, managementController);
  }

  @Test
  public void testCreateResources_NotInitialized() throws Exception {

    // Create injector where the Configuration object does not have the persisted CredentialStore
    // details set.
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();

        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(SecurePasswordHelper.class).toInstance(new SecurePasswordHelper());
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    // Create resources requests
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias1", "username1", "password1", CredentialStoreType.TEMPORARY)).once();
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias1", "username1", "password1", CredentialStoreType.PERSISTED)).once();

    // Get resources request
    expect(request.getPropertyIds()).andReturn(null).anyTimes();

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);


    // The temporary store should always be initialized.... this should succeed.
    provider.createResources(request);

    try {
      provider.createResources(request);
      Assert.fail("Expected IllegalArgumentException thrown");
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("Credentials cannot be stored in Ambari's persistent secure credential " +
              "store since secure persistent storage has not yet be configured.  Use ambari-server " +
              "setup-security to enable this feature.", e.getLocalizedMessage()
      );
    }

    verify(request, factory, managementController);
  }


  @Test
  public void testGetResourcesAsAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesAsClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesAsServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    // Create resources requests
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias1", "username1", "password1", CredentialStoreType.TEMPORARY)).once();
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias2", "username2", "password2", CredentialStoreType.PERSISTED)).once();
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias3", "username3", "password3", CredentialStoreType.TEMPORARY)).once();


    // Get resources request
    expect(request.getPropertyIds()).andReturn(null).once();

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);

    provider.createResources(request);
    provider.createResources(request);
    provider.createResources(request);

    Predicate predicate = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();

    Set<Resource> results = provider.getResources(request, predicate);
    Assert.assertEquals(3, results.size());

    for (Resource result : results) {
      Object alias = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID);
      Object type = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_TYPE_PROPERTY_ID);

      if ("alias1".equals(alias)) {
        Assert.assertEquals(CredentialStoreType.TEMPORARY.name().toLowerCase(), type);
      } else if ("alias2".equals(alias)) {
        Assert.assertEquals(CredentialStoreType.PERSISTED.name().toLowerCase(), type);
      } else if ("alias3".equals(alias)) {
        Assert.assertEquals(CredentialStoreType.TEMPORARY.name().toLowerCase(), type);
      } else {
        Assert.fail("Unexpected alias in list: " + alias);
      }
    }

    verify(request, factory, managementController);
  }

  @Test
  public void testGetResourcesWithPredicateAsAdministrator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesWithPredicateAsClusterAdministrator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesWithPredicateAsServiceAdministrator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResourcesWithPredicate(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    // Create resources requests
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias1", "username1", "password1", CredentialStoreType.TEMPORARY)).once();
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias2", "username2", "password2", CredentialStoreType.PERSISTED)).once();
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias3", "username3", "password3", CredentialStoreType.TEMPORARY)).once();

    // Get resources request
    expect(request.getPropertyIds()).andReturn(null).once();

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);

    provider.createResources(request);
    provider.createResources(request);
    provider.createResources(request);

    Predicate predicate1 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate predicate2 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID).equals("alias1")
        .toPredicate();
    Predicate predicate = new AndPredicate(predicate1, predicate2);


    Set<Resource> results = provider.getResources(request, predicate);
    Assert.assertEquals(1, results.size());

    for (Resource result : results) {
      Object alias = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID);
      Object type = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_TYPE_PROPERTY_ID);

      if ("alias1".equals(alias)) {
        Assert.assertEquals(CredentialStoreType.TEMPORARY.name().toLowerCase(), type);
      } else {
        Assert.fail("Unexpected alias in list: " + alias);
      }
    }

    verify(request, factory, managementController);
  }

  @Test
  public void testGetResourcesWithPredicateNoResultsAsAdministrator() throws Exception {
    testGetResourcesWithPredicateNoResults(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesWithPredicateNoResultsAsClusterAdministrator() throws Exception {
    testGetResourcesWithPredicateNoResults(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesWithPredicateNoResultsAsServiceAdministrator() throws Exception {
    testGetResourcesWithPredicateNoResults(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResourcesWithPredicateNoResults(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    // Create resources requests
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias1", "username1", "password1", CredentialStoreType.TEMPORARY)).once();
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias2", "username2", "password2", CredentialStoreType.PERSISTED)).once();
    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", "alias3", "username3", "password3", CredentialStoreType.TEMPORARY)).once();

    // Get resources request
    expect(request.getPropertyIds()).andReturn(null).once();

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);

    provider.createResources(request);
    provider.createResources(request);
    provider.createResources(request);

    Predicate predicate1 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate predicate2 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID).equals("alias4")
        .toPredicate();
    Predicate predicate = new AndPredicate(predicate1, predicate2);

    try {
      provider.getResources(request, predicate);
      Assert.fail("Expected NoSuchResourceException not thrown");
    } catch (NoSuchResourceException e) {
      // expected
    }

    verify(request, factory, managementController);
  }

  @Test
  public void testGetResourcesWithoutPredicateAsAdministrator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesWithoutPredicateAsClusterAdministrator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesWithoutPredicateAsServiceAdministrator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResourcesWithoutPredicate(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    // Get resources request
    expect(request.getPropertyIds()).andReturn(null).once();

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);


    Set<Resource> results = provider.getResources(request, null);
    Assert.assertTrue(results.isEmpty());

    verify(request, factory, managementController);
  }

  @Test
  public void testUpdateResourcesAsAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testUpdateResourcesAsClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesAsServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testUpdateResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    // Create resources requests
    Set<Map<String, Object>> properties = new HashSet<>();
    properties.addAll(getCredentialTestProperties("c1", "alias1", "username1", "password1", CredentialStoreType.TEMPORARY));
    properties.addAll(getCredentialTestProperties("c1", "alias2", "username2", "password2", CredentialStoreType.TEMPORARY));
    properties.addAll(getCredentialTestProperties("c1", "alias3", "username3", "password3", CredentialStoreType.TEMPORARY));
    expect(request.getProperties()).andReturn(properties).once();

    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", null, "username1", "password1", CredentialStoreType.PERSISTED)).once();

    // Get resources request
    expect(request.getPropertyIds()).andReturn(null).anyTimes();

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);

    provider.createResources(request);

    Predicate predicate1 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate predicate2 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID).equals("alias1")
        .toPredicate();
    Predicate predicate = new AndPredicate(predicate1, predicate2);

    Set<Resource> results = provider.getResources(request, predicate);
    Assert.assertEquals(1, results.size());

    for (Resource result : results) {
      Object alias = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID);
      Object type = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_TYPE_PROPERTY_ID);

      if ("alias1".equals(alias)) {
        Assert.assertEquals(CredentialStoreType.TEMPORARY.name().toLowerCase(), type);
      } else {
        Assert.fail("Unexpected alias in list: " + alias);
      }
    }

    provider.updateResources(request, predicate);

    results = provider.getResources(request, predicate);
    Assert.assertEquals(1, results.size());

    for (Resource result : results) {
      Object alias = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID);
      Object type = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_TYPE_PROPERTY_ID);

      if ("alias1".equals(alias)) {
        Assert.assertEquals(CredentialStoreType.PERSISTED.name().toLowerCase(), type);
      } else {
        Assert.fail("Unexpected alias in list: " + alias);
      }
    }

    verify(request, factory, managementController);
  }

  @Test
  public void testUpdateResourcesResourceNotFoundAsAdministrator() throws Exception {
    testUpdateResourcesResourceNotFound(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testUpdateResourcesResourceNotFoundAsClusterAdministrator() throws Exception {
    testUpdateResourcesResourceNotFound(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesResourceNotFoundAsServiceAdministrator() throws Exception {
    testUpdateResourcesResourceNotFound(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testUpdateResourcesResourceNotFound(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    // Create resources requests
    Set<Map<String, Object>> properties = new HashSet<>();
    properties.addAll(getCredentialTestProperties("c1", "alias1", "username1", "password1", CredentialStoreType.TEMPORARY));
    properties.addAll(getCredentialTestProperties("c1", "alias2", "username2", "password2", CredentialStoreType.TEMPORARY));
    properties.addAll(getCredentialTestProperties("c1", "alias3", "username3", "password3", CredentialStoreType.TEMPORARY));
    expect(request.getProperties()).andReturn(properties).once();

    expect(request.getProperties()).andReturn(getCredentialTestProperties("c1", null, "username1", "password1", CredentialStoreType.PERSISTED)).once();

    // Get resources request
    expect(request.getPropertyIds()).andReturn(null).anyTimes();

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);

    provider.createResources(request);

    Predicate predicate1 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate predicate2 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID).equals("alias4")
        .toPredicate();
    Predicate predicate = new AndPredicate(predicate1, predicate2);

    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected NoSuchResourceException thrown");
    } catch (NoSuchResourceException e) {
      // expected
    }

    verify(request, factory, managementController);
  }

  @Test
  public void testDeleteResourcesAsAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testDeleteResourcesAsClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    CredentialResourceProvider credentialResourceProvider = new CredentialResourceProvider(managementController);
    injector.injectMembers(credentialResourceProvider);

    // Create resources requests
    Set<Map<String, Object>> properties = new HashSet<>();
    properties.addAll(getCredentialTestProperties("c1", "alias1", "username1", "password1", CredentialStoreType.TEMPORARY));
    properties.addAll(getCredentialTestProperties("c1", "alias2", "username2", "password2", CredentialStoreType.TEMPORARY));
    properties.addAll(getCredentialTestProperties("c1", "alias3", "username3", "password3", CredentialStoreType.TEMPORARY));
    expect(request.getProperties()).andReturn(properties).once();

    // Get resources request
    expect(request.getPropertyIds()).andReturn(null).anyTimes();

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getCredentialResourceProvider(anyObject(AmbariManagementController.class))).andReturn(credentialResourceProvider);

    replay(request, factory, managementController);
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractControllerResourceProvider.init(factory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Credential,
        managementController);

    provider.createResources(request);

    Predicate predicate1 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate predicate2 = new PredicateBuilder()
        .property(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID).equals("alias1")
        .toPredicate();
    Predicate predicate = new AndPredicate(predicate1, predicate2);

    Set<Resource> results = provider.getResources(request, predicate);
    Assert.assertEquals(1, results.size());

    for (Resource result : results) {
      Object alias = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID);
      Object type = result.getPropertyValue(CredentialResourceProvider.CREDENTIAL_TYPE_PROPERTY_ID);

      if ("alias1".equals(alias)) {
        Assert.assertEquals(CredentialStoreType.TEMPORARY.name().toLowerCase(), type);
      } else {
        Assert.fail("Unexpected alias in list: " + alias);
      }
    }

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    try {
      provider.getResources(request, predicate);
      Assert.fail("Expected NoSuchResourceException thrown");
    } catch (NoSuchResourceException e) {
      // expected
    }

    verify(request, factory, managementController);
  }

  private Set<Map<String, Object>> getCredentialTestProperties(String clusterName, String alias, String principal, String password, CredentialStoreType credentialStoreType) {
    Map<String, Object> mapProperties = new HashMap<>();

    if (clusterName != null) {
      mapProperties.put(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID, clusterName);
    }

    if (alias != null) {
      mapProperties.put(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID, alias);
    }

    if (password != null) {
      mapProperties.put(CredentialResourceProvider.CREDENTIAL_KEY_PROPERTY_ID, password);
    }

    if (principal != null) {
      mapProperties.put(CredentialResourceProvider.CREDENTIAL_PRINCIPAL_PROPERTY_ID, principal);
    }

    if (credentialStoreType != null) {
      mapProperties.put(CredentialResourceProvider.CREDENTIAL_TYPE_PROPERTY_ID, credentialStoreType.name().toLowerCase());
    }

    return Collections.singleton(mapProperties);
  }
}

