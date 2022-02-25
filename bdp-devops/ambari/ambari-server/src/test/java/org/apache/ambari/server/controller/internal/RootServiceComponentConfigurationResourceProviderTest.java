/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_CATEGORY_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_COMPONENT_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTIES_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_SERVICE_NAME_PROPERTY_ID;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.services.RootServiceComponentConfigurationService;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.encryption.AmbariServerConfigurationEncryptor;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileUtils.class, AmbariServerConfigurationHandler.class})
public class RootServiceComponentConfigurationResourceProviderTest extends EasyMockSupport {

  private static final String LDAP_CONFIG_CATEGORY = AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName();
  private static final String SSO_CONFIG_CATEGORY = AmbariServerConfigurationCategory.SSO_CONFIGURATION.getCategoryName();

  private Predicate predicate;
  private ResourceProvider resourceProvider;
  private RootServiceComponentConfigurationHandlerFactory factory;
  private Request request;
  private AmbariConfigurationDAO dao;
  private AmbariEventPublisher publisher;
  private AmbariServerLDAPConfigurationHandler ambariServerLDAPConfigurationHandler;
  private AmbariServerSSOConfigurationHandler ambariServerSSOConfigurationHandler;

  @Before
  public void init() {
    Injector injector = createInjector();
    resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);
    predicate = createPredicate(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), LDAP_CONFIG_CATEGORY);
    request = createMock(Request.class);
    dao = injector.getInstance(AmbariConfigurationDAO.class);
    factory = injector.getInstance(RootServiceComponentConfigurationHandlerFactory.class);
    publisher = injector.getInstance(AmbariEventPublisher.class);
    ambariServerLDAPConfigurationHandler = injector.getInstance(AmbariServerLDAPConfigurationHandler.class);
    ambariServerSSOConfigurationHandler = injector.getInstance(AmbariServerSSOConfigurationHandler.class);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources_Administrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ServiceOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceOperator(), null);
  }

  @Test
  public void testCreateResourcesWithDirective_Administrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ServiceOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceOperator(), "test-directive");
  }

  private void testCreateResources(Authentication authentication, String opDirective) throws Exception {
    Set<Map<String, Object>> propertySets = new HashSet<>();

    Map<String, String> properties = new HashMap<>();
    properties.put(AmbariServerConfigurationKey.LDAP_ENABLED.key(), "value1");
    properties.put(AmbariServerConfigurationKey.USER_BASE.key(), "value2");
    propertySets.add(toRequestProperties(LDAP_CONFIG_CATEGORY, properties));

    Map<String, String> properties2 = new HashMap<>();
    if (opDirective == null) {
      properties2.put(AmbariServerConfigurationKey.SSO_ENABLED_SERVICES.key(), "true");
      propertySets.add(toRequestProperties(SSO_CONFIG_CATEGORY, properties2));
    }

    Map<String, String> requestInfoProperties;
    if (opDirective == null) {
      requestInfoProperties = Collections.emptyMap();
    } else {
      requestInfoProperties = Collections.singletonMap(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION, opDirective);
    }

    expect(request.getProperties()).andReturn(propertySets).once();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();
    Capture<Map<String, String>> capturedProperties2 = newCapture();

    if (opDirective == null) {
      expect(dao.reconcileCategory(eq(LDAP_CONFIG_CATEGORY), capture(capturedProperties1), eq(true)))
          .andReturn(true)
          .once();
      expect(dao.reconcileCategory(eq(SSO_CONFIG_CATEGORY), capture(capturedProperties2), eq(true)))
          .andReturn(true)
          .once();
      expect(dao.findByCategory(eq(SSO_CONFIG_CATEGORY)))
          .andReturn(Collections.emptyList())
          .once();


      publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
      expectLastCall().times(2);
    }

    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), LDAP_CONFIG_CATEGORY))
        .andReturn(ambariServerLDAPConfigurationHandler)
        .once();
    if (opDirective == null) {
      expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), SSO_CONFIG_CATEGORY))
          .andReturn(ambariServerSSOConfigurationHandler)
          .once();
    }

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      resourceProvider.createResources(request);
      if (opDirective != null) {
        Assert.fail("Expected SystemException to be thrown");
      }
    } catch (AuthorizationException e) {
      throw e;
    } catch (SystemException e) {
      if (opDirective == null) {
        Assert.fail("Unexpected exception: " + e.getMessage());
      } else {
        Assert.assertEquals("The requested operation is not supported for this category: " + LDAP_CONFIG_CATEGORY, e.getMessage());
      }
    }

    verifyAll();

    if (opDirective == null) {
      validateCapturedProperties(properties, capturedProperties1);
      validateCapturedProperties(properties2, capturedProperties2);
    } else {
      Assert.assertFalse(capturedProperties1.hasCaptured());
      Assert.assertFalse(capturedProperties2.hasCaptured());
    }
  }

  @Test
  public void testDeleteResources_Administrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ClusterOperator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ServiceOperator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceOperator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {
    expect(dao.removeByCategory(LDAP_CONFIG_CATEGORY)).andReturn(1).once();

    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().once();

    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), LDAP_CONFIG_CATEGORY))
        .andReturn(ambariServerLDAPConfigurationHandler)
        .once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    resourceProvider.deleteResources(request, predicate);

    verifyAll();
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ClusterOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ServiceOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceOperator());
  }

  private void testGetResources(Authentication authentication) throws Exception {
    expect(request.getPropertyIds()).andReturn(null).anyTimes();

    Map<String, String> properties = new HashMap<>();
    properties.put(AmbariServerConfigurationKey.ANONYMOUS_BIND.key(), "value1");
    properties.put(AmbariServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE.key(), "value2");

    expect(dao.findByCategory(LDAP_CONFIG_CATEGORY)).andReturn(createEntities(LDAP_CONFIG_CATEGORY, properties)).once();

    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), LDAP_CONFIG_CATEGORY))
        .andReturn(ambariServerLDAPConfigurationHandler)
        .once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Set<Resource> response = resourceProvider.getResources(request, predicate);

    verifyAll();

    Assert.assertNotNull(response);
    Assert.assertEquals(1, response.size());

    Resource resource = response.iterator().next();
    Assert.assertEquals(Resource.Type.RootServiceComponentConfiguration, resource.getType());

    Map<String, Map<String, Object>> propertiesMap = resource.getPropertiesMap();
    Assert.assertEquals(3, propertiesMap.size());

    Assert.assertEquals(LDAP_CONFIG_CATEGORY, propertiesMap.get(RootServiceComponentConfigurationResourceProvider.RESOURCE_KEY).get("category"));

    Map<String, Object> retrievedProperties = propertiesMap.get(RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTIES_PROPERTY_ID);
    Assert.assertEquals(2, retrievedProperties.size());

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      Assert.assertEquals(entry.getValue(), retrievedProperties.get(entry.getKey()));
    }

    Map<String, Object> retrievedPropertyTypes = propertiesMap.get(RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTY_TYPES_PROPERTY_ID);
    Assert.assertEquals(2, retrievedPropertyTypes.size());
  }

  @Test
  public void testUpdateResources_Administrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterOperator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ServiceOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceOperator(), null);
  }

  @Test
  public void testUpdateResourcesWithDirective_Administrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ClusterOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterOperator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ServiceOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceOperator(), "test-directive");
  }

  private void testUpdateResources(Authentication authentication, String opDirective) throws Exception {
    Set<Map<String, Object>> propertySets = new HashSet<>();
    Map<String, String> properties = new HashMap<>();
    properties.put(AmbariServerConfigurationKey.GROUP_BASE.key(), "value1");
    properties.put(AmbariServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE.key(), "value2");
    propertySets.add(toRequestProperties(LDAP_CONFIG_CATEGORY, properties));

    Map<String, String> requestInfoProperties;
    if (opDirective == null) {
      requestInfoProperties = Collections.emptyMap();
    } else {
      requestInfoProperties = Collections.singletonMap(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION, opDirective);
    }

    expect(request.getProperties()).andReturn(propertySets).once();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();

    if (opDirective == null) {
      expect(dao.reconcileCategory(eq(LDAP_CONFIG_CATEGORY), capture(capturedProperties1), eq(false)))
          .andReturn(true)
          .once();
      publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
      expectLastCall().times(1);
    }

    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), LDAP_CONFIG_CATEGORY))
        .andReturn(ambariServerLDAPConfigurationHandler)
        .once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      resourceProvider.updateResources(request, predicate);

      if (opDirective != null) {
        Assert.fail("Expected SystemException to be thrown");
      }
    } catch (AuthorizationException e) {
      throw e;
    } catch (SystemException e) {
      if (opDirective == null) {
        Assert.fail("Unexpected exception: " + e.getMessage());
      } else {
        Assert.assertEquals("The requested operation is not supported for this category: " + LDAP_CONFIG_CATEGORY, e.getMessage());
      }
    }

    verifyAll();

    if (opDirective == null) {
      validateCapturedProperties(properties, capturedProperties1);
    } else {
      Assert.assertFalse(capturedProperties1.hasCaptured());
    }
  }

  private Predicate createPredicate(String serviceName, String componentName, String categoryName) {
    Predicate predicateService = new PredicateBuilder()
        .property(CONFIGURATION_SERVICE_NAME_PROPERTY_ID)
        .equals(serviceName)
        .toPredicate();
    Predicate predicateComponent = new PredicateBuilder()
        .property(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID)
        .equals(componentName)
        .toPredicate();
    Predicate predicateCategory = new PredicateBuilder()
        .property(CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(categoryName)
        .toPredicate();
    return new AndPredicate(predicateService, predicateComponent, predicateCategory);
  }

  private List<AmbariConfigurationEntity> createEntities(String categoryName, Map<String, String> properties) {
    List<AmbariConfigurationEntity> entities = new ArrayList<>();

    for (Map.Entry<String, String> property : properties.entrySet()) {
      AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
      entity.setCategoryName(categoryName);
      entity.setPropertyName(property.getKey());
      entity.setPropertyValue(property.getValue());
      entities.add(entity);
    }

    return entities;
  }

  private Map<String, Object> toRequestProperties(String categoryName1, Map<String, String> properties) {
    Map<String, Object> requestProperties = new HashMap<>();
    requestProperties.put(CONFIGURATION_SERVICE_NAME_PROPERTY_ID, "AMBARI");
    requestProperties.put(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID, "AMBARI_SERVER");
    requestProperties.put(CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName1);
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      requestProperties.put(CONFIGURATION_PROPERTIES_PROPERTY_ID + "/" + entry.getKey(), entry.getValue());
    }
    return requestProperties;
  }

  private void validateCapturedProperties(Map<String, String> expectedProperties, Capture<Map<String, String>> capturedProperties) {
    Assert.assertTrue(capturedProperties.hasCaptured());

    Map<String, String> properties = capturedProperties.getValue();
    Assert.assertNotNull(properties);

    // Convert the Map to a TreeMap to help with comparisons
    expectedProperties = new TreeMap<>(expectedProperties);
    properties = new TreeMap<>(properties);
    Assert.assertEquals(expectedProperties, properties);
  }

  private Injector createInjector() {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        AmbariEventPublisher publisher = createMock(AmbariEventPublisher.class);
        AmbariConfigurationDAO ambariConfigurationDAO = createMock(AmbariConfigurationDAO.class);
        Clusters clusters = createNiceMock(Clusters.class);
        ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
        AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
        StackAdvisorHelper stackAdvisorHelper = createNiceMock(StackAdvisorHelper.class);
        LdapFacade ldapFacade = createNiceMock(LdapFacade.class);
        Encryptor<AmbariServerConfiguration> encryptor = createNiceMock(AmbariServerConfigurationEncryptor.class);

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(AmbariConfigurationDAO.class).toInstance(ambariConfigurationDAO);
        bind(AmbariEventPublisher.class).toInstance(publisher);

        bind(AmbariServerConfigurationHandler.class).toInstance(new AmbariServerConfigurationHandler(ambariConfigurationDAO, publisher));
        bind(AmbariServerSSOConfigurationHandler.class).toInstance(new AmbariServerSSOConfigurationHandler(clusters, configHelper, managementController, stackAdvisorHelper, ambariConfigurationDAO, publisher));
        bind(AmbariServerLDAPConfigurationHandler.class).toInstance(new AmbariServerLDAPConfigurationHandler(clusters, configHelper, managementController,
            stackAdvisorHelper, ambariConfigurationDAO, publisher, ldapFacade, encryptor));
        bind(RootServiceComponentConfigurationHandlerFactory.class).toInstance(createMock(RootServiceComponentConfigurationHandlerFactory.class));
      }
    });
  }
}