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

import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.SSO_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_ENABLED_SERVICES;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_MANAGE_SERVICES;
import static org.apache.ambari.server.events.AmbariEvent.AmbariEventType.AMBARI_CONFIGURATION_CHANGED;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import junit.framework.Assert;

public class AmbariServerSSOConfigurationHandlerTest extends EasyMockSupport {

  @Test
  public void testUpdateCategoryAmbariNotManagingServices() throws AmbariException {
    Map<String, String> ssoConfigurationProperties = new HashMap<>();
    ssoConfigurationProperties.put(SSO_MANAGE_SERVICES.key(), "false");

    AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
    entity.setCategoryName(SSO_CONFIGURATION.getCategoryName());
    entity.setPropertyName(SSO_MANAGE_SERVICES.key());
    entity.setPropertyValue("false");
    List<AmbariConfigurationEntity> entities = Collections.singletonList(entity);

    AmbariConfigurationDAO ambariConfigurationDAO = createMock(AmbariConfigurationDAO.class);
    expect(ambariConfigurationDAO.reconcileCategory(SSO_CONFIGURATION.getCategoryName(), Collections.singletonMap(SSO_MANAGE_SERVICES.key(), "false"), true)).andReturn(false).once();
    expect(ambariConfigurationDAO.findByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(entities).once();

    AmbariEventPublisher publisher = createMock(AmbariEventPublisher.class);
    Clusters clusters = createMock(Clusters.class);
    ConfigHelper configHelper = createMock(ConfigHelper.class);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    StackAdvisorHelper stackAdvisorHelper = createMock(StackAdvisorHelper.class);

    replayAll();

    AmbariServerSSOConfigurationHandler handler = new AmbariServerSSOConfigurationHandler(clusters, configHelper, managementController, stackAdvisorHelper, ambariConfigurationDAO, publisher);
    handler.updateComponentCategory(SSO_CONFIGURATION.getCategoryName(), ssoConfigurationProperties, true);

    verifyAll();
  }

  @Test
  public void testUpdateCategoryAmbariManagingServices() throws AmbariException, StackAdvisorException {
    Map<String, String> ssoConfigurationProperties = new HashMap<>();
    ssoConfigurationProperties.put(SSO_MANAGE_SERVICES.key(), "true");

    AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
    entity.setCategoryName(SSO_CONFIGURATION.getCategoryName());
    entity.setPropertyName(SSO_MANAGE_SERVICES.key());
    entity.setPropertyValue("true");
    List<AmbariConfigurationEntity> entities = Collections.singletonList(entity);

    StackId stackId = new StackId("HDP-3.0");

    Map<String, Set<String>> serviceComponentHostMap = Collections.singletonMap("ATLAS_COMPONENT", Collections.singleton("host1"));

    Host host = createMock(Host.class);
    expect(host.getHostName()).andReturn("host1").once();

    Service service = createMock(Service.class);
    expect(service.getName()).andReturn("SERVICE1").once();

    Map<String, Map<String, String>> tags = Collections.emptyMap();
    Map<String, Map<String, String>> existing_configurations = Collections.singletonMap("SERVICE1", Collections.singletonMap("service1-property1", "service1-value1"));

    ValueAttributesInfo nonSSOProperty1Attributes = new ValueAttributesInfo();
    nonSSOProperty1Attributes.setDelete("true");

    RecommendationResponse.BlueprintConfigurations blueprintConfigurations = new RecommendationResponse.BlueprintConfigurations();
    blueprintConfigurations.setProperties(Collections.singletonMap("service1-sso-property1", "service1-sso-value1"));
    blueprintConfigurations.setPropertyAttributes(Collections.singletonMap("service1-nonsso-property1", nonSSOProperty1Attributes));

    RecommendationResponse.Blueprint blueprint = new RecommendationResponse.Blueprint();
    blueprint.setConfigurations(Collections.singletonMap("service-site", blueprintConfigurations));

    RecommendationResponse.Recommendation recommendations = new RecommendationResponse.Recommendation();
    recommendations.setBlueprint(blueprint);

    RecommendationResponse recommendationResponse = new RecommendationResponse();
    recommendationResponse.setRecommendations(recommendations);

    Capture<AmbariEvent> capturedEvent = newCapture();
    Capture<StackAdvisorRequest> capturedRequest = newCapture();
    Capture<Map<String, String>> capturedUpdates = newCapture();
    Capture<Collection<String>> capturedRemovals = newCapture();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    AmbariConfigurationDAO ambariConfigurationDAO = createMock(AmbariConfigurationDAO.class);
    expect(ambariConfigurationDAO.reconcileCategory(SSO_CONFIGURATION.getCategoryName(), Collections.singletonMap(SSO_MANAGE_SERVICES.key(), "true"), true)).andReturn(true).once();
    expect(ambariConfigurationDAO.findByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(entities).once();

    AmbariEventPublisher publisher = createMock(AmbariEventPublisher.class);
    publisher.publish(capture(capturedEvent));
    expectLastCall().once();

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getClusterName()).andReturn("c1").once();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getHosts()).andReturn(Collections.singleton(host)).once();
    expect(cluster.getServices()).andReturn(Collections.singletonMap("SERVICE1", service)).once();
    expect(cluster.getServiceComponentHostMap(null, null)).andReturn(serviceComponentHostMap).once();

    ConfigHelper configHelper = createMock(ConfigHelper.class);
    expect(configHelper.getEffectiveDesiredTags(cluster, null)).andReturn(tags).once();
    expect(configHelper.getEffectiveConfigProperties(cluster, tags)).andReturn(existing_configurations).once();
    configHelper.updateConfigType(eq(cluster), eq(stackId), eq(managementController), eq("service-site"), capture(capturedUpdates), capture(capturedRemovals),
        eq("internal"), eq("Ambari-managed single sign-on configurations"));
    expectLastCall().once();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getClusters()).andReturn(Collections.singletonMap("c1", cluster)).once();

    StackAdvisorHelper stackAdvisorHelper = createMock(StackAdvisorHelper.class);
    expect(stackAdvisorHelper.recommend(capture(capturedRequest))).andReturn(recommendationResponse).once();

    replayAll();

    AmbariServerSSOConfigurationHandler handler = new AmbariServerSSOConfigurationHandler(clusters, configHelper, managementController, stackAdvisorHelper, ambariConfigurationDAO, publisher);
    handler.updateComponentCategory(SSO_CONFIGURATION.getCategoryName(), ssoConfigurationProperties, true);

    verifyAll();

    Assert.assertTrue(capturedEvent.hasCaptured());
    Assert.assertEquals(AMBARI_CONFIGURATION_CHANGED, capturedEvent.getValue().getType());
    Assert.assertEquals(SSO_CONFIGURATION.getCategoryName(), ((AmbariConfigurationChangedEvent) capturedEvent.getValue()).getCategoryName());

    Assert.assertTrue(capturedUpdates.hasCaptured());
    Assert.assertTrue(capturedUpdates.getValue().containsKey("service1-sso-property1"));
    Assert.assertEquals("service1-sso-value1", capturedUpdates.getValue().get("service1-sso-property1"));

    Assert.assertTrue(capturedRemovals.hasCaptured());
    Assert.assertTrue(capturedRemovals.getValue().contains("service1-nonsso-property1"));

    Assert.assertTrue(capturedRequest.hasCaptured());
  }

  @Test
  public void testCheckingIfSSOIsEnabledPerEachService() {
    Clusters clusters = createMock(Clusters.class);
    ConfigHelper configHelper = createMock(ConfigHelper.class);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    StackAdvisorHelper stackAdvisorHelper = createMock(StackAdvisorHelper.class);
    AmbariEventPublisher publisher = createMock(AmbariEventPublisher.class);

    List<AmbariConfigurationEntity> entities = new ArrayList<>();
    AmbariConfigurationEntity entity;

    entity = new AmbariConfigurationEntity();
    entity.setCategoryName(SSO_CONFIGURATION.getCategoryName());
    entity.setPropertyName(SSO_MANAGE_SERVICES.key());
    entity.setPropertyValue("true");
    entities.add(entity);

    entity = new AmbariConfigurationEntity();
    entity.setCategoryName(SSO_CONFIGURATION.getCategoryName());
    entity.setPropertyName(SSO_ENABLED_SERVICES.key());
    entity.setPropertyValue("SERVICE1,SERVICE2");
    entities.add(entity);

    AmbariConfigurationDAO ambariConfigurationDAO = createMock(AmbariConfigurationDAO.class);
    expect(ambariConfigurationDAO.findByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(entities).anyTimes();

    replayAll();

    AmbariServerSSOConfigurationHandler handler = new AmbariServerSSOConfigurationHandler(clusters, configHelper, managementController, stackAdvisorHelper, ambariConfigurationDAO, publisher);

    Assert.assertTrue(handler.getSSOEnabledServices().contains("SERVICE1"));
    Assert.assertTrue(handler.getSSOEnabledServices().contains("SERVICE2"));
    Assert.assertFalse(handler.getSSOEnabledServices().contains("SERVICE3"));

    verifyAll();
  }
}