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

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.BlueprintResourceProvider.BlueprintConfigPopulationStrategy;
import org.apache.ambari.server.controller.internal.BlueprintResourceProvider.BlueprintConfigPopulationStrategyV1;
import org.apache.ambari.server.controller.internal.BlueprintResourceProvider.BlueprintConfigPopulationStrategyV2;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintConfiguration;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.BlueprintSettingEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.topology.Setting;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;

/**
 * BlueprintResourceProvider unit tests.
 */
@SuppressWarnings("unchecked")
public class BlueprintResourceProviderTest {

  private static String BLUEPRINT_NAME = "test-blueprint";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final static BlueprintDAO blueprintDao = createStrictMock(BlueprintDAO.class);
  private final static TopologyRequestDAO topologyRequestDAO = createMock(TopologyRequestDAO.class);
  private final static StackDAO stackDAO = createNiceMock(StackDAO.class);
  private final static BlueprintEntity entity = createStrictMock(BlueprintEntity.class);
  private final static Blueprint blueprint = createMock(Blueprint.class);
  private final static AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
  private final static BlueprintFactory blueprintFactory = createMock(BlueprintFactory.class);
  private final static SecurityConfigurationFactory securityFactory = createMock(SecurityConfigurationFactory.class);
  private final static BlueprintResourceProvider provider = createProvider();
  private final static Gson gson = new Gson();

  @BeforeClass
  public static void initClass() {
    BlueprintResourceProvider.init(blueprintFactory, blueprintDao, topologyRequestDAO, securityFactory, gson, metaInfo);

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("test-stack-name");
    stackEntity.setStackVersion("test-stack-version");

    expect(
        stackDAO.find(anyObject(String.class),
          anyObject(String.class))).andReturn(stackEntity).anyTimes();
    replay(stackDAO);

  }

  private Map<String, Set<HashMap<String, String>>> getSettingProperties() {
    return new HashMap<>();
  }

  @Before
  public void resetGlobalMocks() {
    reset(blueprintDao, topologyRequestDAO, metaInfo, blueprintFactory, securityFactory, blueprint, entity);
  }

  @Test
  public void testCreateResources() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);
    Setting setting = createStrictMock(Setting.class);

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();
    Map<String, String> requestInfoProperties = getTestRequestInfoProperties();
    Map<String, Set<HashMap<String, String>>> settingProperties = getSettingProperties();

    // set expectations
    expect(blueprintFactory.createBlueprint(setProperties.iterator().next(), null)).andReturn(blueprint).once();
    expect(securityFactory.createSecurityConfigurationFromRequest(null, true)).andReturn(null).anyTimes();
    blueprint.validateRequiredProperties();
    blueprint.validateTopology();
    expect(blueprint.getSetting()).andReturn(setting).anyTimes();
    expect(setting.getProperties()).andReturn(settingProperties).anyTimes();
    expect(blueprint.toEntity()).andReturn(entity);
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);
    expect(blueprintDao.findByName(BLUEPRINT_NAME)).andReturn(null);
    blueprintDao.create(entity);

    replay(blueprintDao, entity, metaInfo, blueprintFactory, securityFactory, blueprint, setting, request, managementController);
    // end expectations

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Blueprint,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    assertEquals(request, lastEvent.getRequest());
    assertNull(lastEvent.getPredicate());

    verify(blueprintDao, entity, blueprintFactory, securityFactory, metaInfo, request, managementController);
  }

  @Test()
  public void testCreateResources_ReqestBodyIsEmpty() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();
    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, null);

    // set expectations
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);

    replay(request, managementController);
    // end expectations

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      Resource.Type.Blueprint,
      managementController);

    try {
      provider.createResources(request);
      fail("Exception expected");
    } catch (IllegalArgumentException e) {
      //expected exception
      assertEquals(BlueprintResourceProvider.REQUEST_BODY_EMPTY_ERROR_MESSAGE, e.getMessage());
    }
    verify(request, managementController);
  }

  @Test
  public void testCreateResources_NoValidation() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);
    Setting setting = createStrictMock(Setting.class);

    Map<String, Set<HashMap<String, String>>> settingProperties = getSettingProperties();
    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();
    Map<String, String> requestInfoProperties = getTestRequestInfoProperties();
    requestInfoProperties.put("validate_topology", "false");

    // set expectations
    expect(blueprintFactory.createBlueprint(setProperties.iterator().next(), null)).andReturn(blueprint).once();
    blueprint.validateRequiredProperties();
    expect(blueprint.getSetting()).andReturn(setting).anyTimes();
    expect(setting.getProperties()).andReturn(settingProperties).anyTimes();
    expect(blueprint.toEntity()).andReturn(entity);
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);
    expect(blueprintDao.findByName(BLUEPRINT_NAME)).andReturn(null);
    blueprintDao.create(entity);

    replay(blueprintDao, entity, metaInfo, blueprintFactory, blueprint, setting, request, managementController);
    // end expectations

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Blueprint,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    assertEquals(request, lastEvent.getRequest());
    assertNull(lastEvent.getPredicate());

    verify(blueprintDao, entity, blueprintFactory, metaInfo, request, managementController);
  }

  @Test
  public void testCreateResources_TopologyValidationFails() throws Exception {

    Request request = createMock(Request.class);
    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();
    Map<String, String> requestInfoProperties = getTestRequestInfoProperties();

    // set expectations
    expect(blueprintFactory.createBlueprint(setProperties.iterator().next(), null)).andReturn(blueprint).once();
    blueprint.validateRequiredProperties();
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
    blueprint.validateTopology();
    expectLastCall().andThrow(new InvalidTopologyException("test"));

    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);
    expect(blueprintDao.findByName(BLUEPRINT_NAME)).andReturn(null);

    replay(blueprintDao, entity, metaInfo, blueprintFactory, blueprint, request);
    // end expectations

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Blueprint,
        createMock(AmbariManagementController.class));

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    try {
      provider.createResources(request);
      fail("Expected exception due to topology validation error");
    } catch (IllegalArgumentException e) {
      // expected
    }

    verify(blueprintDao, entity, blueprintFactory, metaInfo, request);
  }


  @Test
  public void testCreateResources_withConfiguration() throws Exception {

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();
    setConfigurationProperties(setProperties);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Map<String, String> requestInfoProperties = getTestRequestInfoProperties();
    Request request = createMock(Request.class);
    Setting setting = createStrictMock(Setting.class);
    Map<String, Set<HashMap<String, String>>> settingProperties = getSettingProperties();

    // set expectations
    expect(blueprintFactory.createBlueprint(setProperties.iterator().next(), null)).andReturn(blueprint).once();
    blueprint.validateRequiredProperties();
    blueprint.validateTopology();
    expect(blueprint.getSetting()).andReturn(setting).anyTimes();
    expect(setting.getProperties()).andReturn(settingProperties).anyTimes();
    expect(blueprint.toEntity()).andReturn(entity);
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);
    expect(blueprintDao.findByName(BLUEPRINT_NAME)).andReturn(null);
    blueprintDao.create(entity);

    replay(blueprintDao, entity, metaInfo, blueprintFactory, blueprint, setting, request, managementController);
    // end expectations

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Blueprint,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    assertEquals(request, lastEvent.getRequest());
    assertNull(lastEvent.getPredicate());

    verify(blueprintDao, entity, blueprintFactory, metaInfo, request, managementController);
  }

  @Test
  public void testCreateResource_BlueprintFactoryThrowsException() throws Exception
  {
    Request request = createMock(Request.class);

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();
    setProperties.iterator().next().remove(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID);

    Map<String, String> requestInfoProperties = getTestRequestInfoProperties();

    // set expectations
    expect(blueprintFactory.createBlueprint(setProperties.iterator().next(), null)).andThrow(
      new IllegalArgumentException("Blueprint name must be provided"));
    expect(securityFactory.createSecurityConfigurationFromRequest(null,true)).andReturn(null).anyTimes();
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);

    replay(blueprintDao, entity, metaInfo, blueprintFactory, securityFactory, blueprint, request);
    // end expectations

    try {
      provider.createResources(request);
      fail("Exception expected");
    } catch (IllegalArgumentException e) {
      // expected
    }
    verify(blueprintDao, entity, blueprintFactory, metaInfo, request);
  }

  @Test
  public void testCreateResources_withSecurityConfiguration() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);
    Setting setting = createStrictMock(Setting.class);

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();
    Map<String, String> requestInfoProperties = getTestRequestInfoProperties();
    Map<String, Set<HashMap<String, String>>> settingProperties = getSettingProperties();
    SecurityConfiguration securityConfiguration = SecurityConfiguration.withReference("testRef");

    // set expectations
    expect(securityFactory.createSecurityConfigurationFromRequest(EasyMock.anyObject(), anyBoolean())).andReturn
      (securityConfiguration).once();
    expect(blueprintFactory.createBlueprint(setProperties.iterator().next(), securityConfiguration)).andReturn(blueprint).once();
    blueprint.validateRequiredProperties();
    blueprint.validateTopology();
    expect(blueprint.getSetting()).andReturn(setting).anyTimes();
    expect(setting.getProperties()).andReturn(settingProperties).anyTimes();
    expect(blueprint.toEntity()).andReturn(entity);
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);
    expect(blueprintDao.findByName(BLUEPRINT_NAME)).andReturn(null);
    blueprintDao.create(entity);

    replay(blueprintDao, entity, metaInfo, blueprintFactory, securityFactory, blueprint, setting, request, managementController);
    // end expectations

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      Resource.Type.Blueprint,
      managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    assertEquals(request, lastEvent.getRequest());
    assertNull(lastEvent.getPredicate());

    verify(blueprintDao, entity, blueprintFactory, metaInfo, request, managementController);
  }

  @Test
  public void testGetResourcesNoPredicate() throws SystemException, UnsupportedPropertyException,
                                                   NoSuchParentResourceException, NoSuchResourceException {
    Request request = createNiceMock(Request.class);

    BlueprintEntity entity = createEntity(getBlueprintTestProperties().iterator().next());

    List<BlueprintEntity> results = new ArrayList<>();
    results.add(entity);

    // set expectations
    expect(blueprintDao.findAll()).andReturn(results);
    replay(blueprintDao, request);

    Set<Resource> setResults = provider.getResources(request, null);
    assertEquals(1, setResults.size());

    verify(blueprintDao);
    validateResource(setResults.iterator().next(), false);
  }

  @Test
  public void testGetResourcesNoPredicate_withConfiguration() throws SystemException, UnsupportedPropertyException,
      NoSuchParentResourceException, NoSuchResourceException, AmbariException {

    StackInfo info = createMock(StackInfo.class);
    expect(info.getConfigPropertiesTypes("core-site")).andReturn(new HashMap<>()).anyTimes();
    expect(metaInfo.getStack("test-stack-name", "test-stack-version")).andReturn(info).anyTimes();
    replay(info, metaInfo);
    Request request = createNiceMock(Request.class);

    Set<Map<String, Object>> testProperties = getBlueprintTestProperties();
    setConfigurationProperties(testProperties);
    BlueprintEntity entity = createEntity(testProperties.iterator().next());

    List<BlueprintEntity> results = new ArrayList<>();
    results.add(entity);

    // set expectations
    expect(blueprintDao.findAll()).andReturn(results);
    replay(blueprintDao, request);

    Set<Resource> setResults = provider.getResources(request, null);
    assertEquals(1, setResults.size());

    verify(blueprintDao);
    validateResource(setResults.iterator().next(), true);
  }


  @Test
  public void testDeleteResources() throws SystemException, UnsupportedPropertyException,
                                           NoSuchParentResourceException, NoSuchResourceException {

    BlueprintEntity blueprintEntity = createEntity(getBlueprintTestProperties().iterator().next());

    // set expectations
    expect(blueprintDao.findByName(BLUEPRINT_NAME)).andReturn(blueprintEntity);
    blueprintDao.removeByName(blueprintEntity.getBlueprintName());
    expectLastCall();
    expect(topologyRequestDAO.findAllProvisionRequests()).andReturn(ImmutableList.of());
    replay(blueprintDao, topologyRequestDAO);

    Predicate predicate = new EqualsPredicate<>(
      BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID, BLUEPRINT_NAME);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    provider.addObserver(observer);

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    assertNotNull(lastEvent.getPredicate());

    verify(blueprintDao);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeleteResources_clusterAlreadyProvisioned() throws SystemException, UnsupportedPropertyException,
    NoSuchParentResourceException, NoSuchResourceException {

    BlueprintEntity blueprintEntity = createEntity(getBlueprintTestProperties().iterator().next());

    // set expectations
    expect(blueprintDao.findByName(BLUEPRINT_NAME)).andReturn(blueprintEntity);
    blueprintDao.removeByName(blueprintEntity.getBlueprintName());
    expectLastCall();
    TopologyRequestEntity topologyRequestEntity = new TopologyRequestEntity();
    topologyRequestEntity.setBlueprintName(BLUEPRINT_NAME);
    expect(topologyRequestDAO.findAllProvisionRequests()).andReturn(ImmutableList.of(topologyRequestEntity));
    replay(blueprintDao, topologyRequestDAO);


    Predicate predicate = new EqualsPredicate<>(
      BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID, BLUEPRINT_NAME);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    provider.addObserver(observer);

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);
  }


  @Test
  public void testCreateResources_withEmptyConfiguration() throws Exception {

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();
    setConfigurationProperties(setProperties);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Map<String, String> requestInfoProperties = new HashMap<>();
    Map<String, Set<HashMap<String, String>>> settingProperties = getSettingProperties();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{\"configurations\":[]}");
    Request request = createMock(Request.class);
    Setting setting = createStrictMock(Setting.class);

    // set expectations
    expect(blueprintFactory.createBlueprint(setProperties.iterator().next(), null)).andReturn(blueprint).once();
    blueprint.validateRequiredProperties();
    blueprint.validateTopology();
    expect(blueprint.getSetting()).andReturn(setting).anyTimes();
    expect(setting.getProperties()).andReturn(settingProperties).anyTimes();
    expect(blueprint.toEntity()).andReturn(entity);
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);
    expect(blueprintDao.findByName(BLUEPRINT_NAME)).andReturn(null);
    blueprintDao.create(entity);

    replay(blueprintDao, entity, metaInfo, blueprintFactory, blueprint, setting, request, managementController);
    // end expectations

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Blueprint,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    assertEquals(request, lastEvent.getRequest());
    assertNull(lastEvent.getPredicate());

    verify(blueprintDao, entity, blueprintFactory, metaInfo, request, managementController);
  }

  @Test
  public void testCreateResources_withSingleConfigurationType() throws Exception {

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();
    setConfigurationProperties(setProperties);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Map<String, String> requestInfoProperties = new HashMap<>();
    Map<String, Set<HashMap<String, String>>> settingProperties = getSettingProperties();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{\"configurations\":[{\"configuration-type\":{\"properties\":{\"property\":\"value\"}}}]}");
    Request request = createMock(Request.class);
    Setting setting = createStrictMock(Setting.class);

    // set expectations
    expect(blueprintFactory.createBlueprint(setProperties.iterator().next(), null)).andReturn(blueprint).once();
    blueprint.validateRequiredProperties();
    blueprint.validateTopology();
    expect(blueprint.getSetting()).andReturn(setting).anyTimes();
    expect(setting.getProperties()).andReturn(settingProperties).anyTimes();
    expect(blueprint.toEntity()).andReturn(entity);
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);
    expect(blueprintDao.findByName(BLUEPRINT_NAME)).andReturn(null);
    blueprintDao.create(entity);

    replay(blueprintDao, entity, metaInfo, blueprintFactory, blueprint, setting, request, managementController);
    // end expectations

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Blueprint,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    assertEquals(request, lastEvent.getRequest());
    assertNull(lastEvent.getPredicate());

    verify(blueprintDao, entity, blueprintFactory, metaInfo, request, managementController);
  }

  @Test
  public void testCreateResources_wrongConfigurationsStructure_withWrongConfigMapSize() throws ResourceAlreadyExistsException, SystemException,
      UnsupportedPropertyException, NoSuchParentResourceException
  {
    Request request = createMock(Request.class);

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();

    Map<String, String> requestInfoProperties = new HashMap<>();
    String configurationData = "{\"configurations\":[{\"config-type1\":{\"properties\" :{\"property\":\"property-value\"}},"
        + "\"config-type2\" : {\"properties_attributes\" : {\"property\" : \"property-value\"}, \"properties\" : {\"property\" : \"property-value\"}}}"
        + "]}";
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, configurationData);

    // set expectations
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);

    replay(blueprintDao, metaInfo, request);
    // end expectations

    try {
      provider.createResources(request);
      fail("Exception expected");
    } catch (IllegalArgumentException e) {
      //expected exception
      assertEquals(BlueprintResourceProvider.CONFIGURATION_MAP_SIZE_CHECK_ERROR_MESSAGE, e.getMessage());
    }
    verify(blueprintDao, metaInfo, request);
  }

  @Test
  public void testCreateResources_wrongConfigurationStructure_withoutConfigMaps() throws ResourceAlreadyExistsException, SystemException,
    UnsupportedPropertyException, NoSuchParentResourceException {

    Request request = createMock(Request.class);

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();

    Map<String, String> requestInfoProperties = new HashMap<>();
    String configurationData = "{\"configurations\":[\"config-type1\", \"config-type2\"]}";
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, configurationData);

    // set expectations
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);

    replay(blueprintDao, metaInfo, request);
    // end expectations

    try {
      provider.createResources(request);
      fail("Exception expected");
    } catch (IllegalArgumentException e) {
      //expected exception
      assertEquals(BlueprintResourceProvider.CONFIGURATION_MAP_CHECK_ERROR_MESSAGE, e.getMessage());
    }
    verify(blueprintDao, metaInfo, request);
  }

  @Test
  public void testCreateResources_wrongConfigurationStructure_withoutConfigsList() throws ResourceAlreadyExistsException, SystemException,
    UnsupportedPropertyException, NoSuchParentResourceException {

    Request request = createMock(Request.class);

    Set<Map<String, Object>> setProperties = getBlueprintTestProperties();

    Map<String, String> requestInfoProperties = new HashMap<>();
    String configurationData = "{\"configurations\":{\"config-type1\": \"properties\", \"config-type2\": \"properties\"}}";
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, configurationData);

    // set expectations
    expect(request.getProperties()).andReturn(setProperties);
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties);

    replay(blueprintDao, metaInfo, request);
    // end expectations

    try {
      provider.createResources(request);
      fail("Exception expected");
    } catch (IllegalArgumentException e) {
      //expected exception
      assertEquals(BlueprintResourceProvider.CONFIGURATION_LIST_CHECK_ERROR_MESSAGE, e.getMessage());
    }
    verify(blueprintDao, metaInfo, request);
  }

  public static Set<Map<String, Object>> getBlueprintTestProperties() {
    Map<String, String> mapHostGroupComponentProperties = new HashMap<>();
    mapHostGroupComponentProperties.put(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID, "component1");

    Map<String, String> mapHostGroupComponentProperties2 = new HashMap<>();
    mapHostGroupComponentProperties2.put(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID, "component2");

    Set<Map<String, String>> setComponentProperties = new HashSet<>();
    setComponentProperties.add(mapHostGroupComponentProperties);
    setComponentProperties.add(mapHostGroupComponentProperties2);

    Set<Map<String, String>> setComponentProperties2 = new HashSet<>();
    setComponentProperties2.add(mapHostGroupComponentProperties);

    Map<String, Object> mapHostGroupProperties = new HashMap<>();
    mapHostGroupProperties.put(BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID, "group1");
    mapHostGroupProperties.put(BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID, "1");
    mapHostGroupProperties.put(BlueprintResourceProvider.COMPONENT_PROPERTY_ID, setComponentProperties);

    Map<String, Object> mapHostGroupProperties2 = new HashMap<>();
    mapHostGroupProperties2.put(BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID, "group2");
    mapHostGroupProperties2.put(BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID, "2");
    mapHostGroupProperties2.put(BlueprintResourceProvider.COMPONENT_PROPERTY_ID, setComponentProperties2);

    Set<Map<String, Object>> setHostGroupProperties = new HashSet<>();
    setHostGroupProperties.add(mapHostGroupProperties);
    setHostGroupProperties.add(mapHostGroupProperties2);

    Map<String, Object> mapProperties = new HashMap<>();
    mapProperties.put(BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID, BLUEPRINT_NAME);
    mapProperties.put(BlueprintResourceProvider.STACK_NAME_PROPERTY_ID, "test-stack-name");
    mapProperties.put(BlueprintResourceProvider.STACK_VERSION_PROPERTY_ID, "test-stack-version");
    mapProperties.put(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID, setHostGroupProperties);

    return Collections.singleton(mapProperties);
  }

  public static Map<String, Object> getBlueprintRawBodyProperties() {
    return new HashMap<>();
  }

  public static void setConfigurationProperties(Set<Map<String, Object>> properties ) {
    Map<String, String> clusterProperties = new HashMap<>();
    clusterProperties.put("core-site/properties/fs.trash.interval", "480");
    clusterProperties.put("core-site/properties/ipc.client.idlethreshold", "8500");
    clusterProperties.put("core-site/properties_attributes/final/ipc.client.idlethreshold", "true");

    // single entry in set which was created in getTestProperties
    Map<String, Object> mapProperties = properties.iterator().next();
    Set<Map<String, String>> configurations = new HashSet<>();
    configurations.add(clusterProperties);
    mapProperties.put("configurations", configurations);

    Map<String, Object> hostGroupProperties = new HashMap<>();
    hostGroupProperties.put("core-site/my.custom.hg.property", "anything");

    Collection<Map<String, Object>> hostGroups = (Collection<Map<String, Object>>) mapProperties.get
        (BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID);

    for (Map<String, Object> hostGroupProps : hostGroups) {
      if (hostGroupProps.get(BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID).equals("group2")) {
        hostGroupProps.put("configurations", Collections.singleton(hostGroupProperties));
        break;
      }
    }
  }

  private void validateResource(Resource resource, boolean containsConfig) {
    assertEquals(BLUEPRINT_NAME, resource.getPropertyValue(BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID));
    assertEquals("test-stack-name", resource.getPropertyValue(BlueprintResourceProvider.STACK_NAME_PROPERTY_ID));
    assertEquals("test-stack-version", resource.getPropertyValue(BlueprintResourceProvider.STACK_VERSION_PROPERTY_ID));

    Collection<Map<String, Object>> hostGroupProperties = (Collection<Map<String, Object>>)
        resource.getPropertyValue(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID);

    assertEquals(2, hostGroupProperties.size());
    for (Map<String, Object> hostGroupProps : hostGroupProperties) {
      String name = (String) hostGroupProps.get(BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID);
      assertTrue(name.equals("group1") || name.equals("group2"));
      List<Map<String, String>> listComponents = (List<Map<String, String>>)
          hostGroupProps.get(BlueprintResourceProvider.COMPONENT_PROPERTY_ID);
      if (name.equals("group1")) {
        assertEquals("1", hostGroupProps.get(BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID));
        assertEquals(2, listComponents.size());
        Map<String, String> mapComponent = listComponents.get(0);
        String componentName = mapComponent.get(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID);
        assertTrue(componentName.equals("component1") || componentName.equals("component2"));
        mapComponent = listComponents.get(1);
        String componentName2 = mapComponent.get(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID);
        assertFalse(componentName2.equals(componentName));
        assertTrue(componentName2.equals("component1") || componentName2.equals("component2"));
      } else if (name.equals("group2")) {
        assertEquals("2", hostGroupProps.get(BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID));
        assertEquals(1, listComponents.size());
        Map<String, String> mapComponent = listComponents.get(0);
        String componentName = mapComponent.get(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID);
        assertEquals("component1", componentName);
      } else {
        fail("Unexpected host group name");
      }
    }

    if (containsConfig) {
      Collection<Map<String, Object>> blueprintConfigurations = (Collection<Map<String, Object>>)
          resource.getPropertyValue(BlueprintResourceProvider.CONFIGURATION_PROPERTY_ID);
      assertEquals(1, blueprintConfigurations.size());

      Map<String, Object> typeConfigs = blueprintConfigurations.iterator().next();
      assertEquals(1, typeConfigs.size());
      Map<String, Map<String, Object>> coreSiteConfig = (Map<String, Map<String, Object>>) typeConfigs.get("core-site");
      assertEquals(2, coreSiteConfig.size());
      assertTrue(coreSiteConfig.containsKey(BlueprintResourceProvider.PROPERTIES_PROPERTY_ID));
      Map<String, Object> properties = coreSiteConfig.get(BlueprintResourceProvider.PROPERTIES_PROPERTY_ID);
      assertNotNull(properties);
      assertEquals("480", properties.get("fs.trash.interval"));
      assertEquals("8500", properties.get("ipc.client.idlethreshold"));

      assertTrue(coreSiteConfig.containsKey(BlueprintResourceProvider.PROPERTIES_ATTRIBUTES_PROPERTY_ID));
      Map<String, Object> attributes = coreSiteConfig.get(BlueprintResourceProvider.PROPERTIES_ATTRIBUTES_PROPERTY_ID);
      assertNotNull(attributes);
      assertEquals(1, attributes.size());
      assertTrue(attributes.containsKey("final"));
      Map<String, String> finalAttrs = (Map<String, String>) attributes.get("final");
      assertEquals(1, finalAttrs.size());
      assertEquals("true", finalAttrs.get("ipc.client.idlethreshold"));
    }

  }

  private static BlueprintResourceProvider createProvider() {
    return new BlueprintResourceProvider(null);
  }

  private BlueprintEntity createEntity(Map<String, Object> properties) {
    BlueprintEntity entity = new BlueprintEntity();
    entity.setBlueprintName((String) properties.get(BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID));

    String stackName = (String) properties.get(BlueprintResourceProvider.STACK_NAME_PROPERTY_ID);
    String stackVersion = (String) properties.get(BlueprintResourceProvider.STACK_VERSION_PROPERTY_ID);
    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName(stackName);
    stackEntity.setStackVersion(stackVersion);

    entity.setStack(stackEntity);

    Set<Map<String, Object>> hostGroupProperties = (Set<Map<String, Object>>) properties.get(
        BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID);

    Collection<HostGroupEntity> hostGroups = new ArrayList<>();
    for (Map<String, Object> groupProperties : hostGroupProperties) {
      HostGroupEntity hostGroup = new HostGroupEntity();
      hostGroups.add(hostGroup);
      hostGroup.setName((String) groupProperties.get(BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID));
      hostGroup.setCardinality((String) groupProperties.get(BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID));
      hostGroup.setConfigurations(new ArrayList<>());

      Set<Map<String, String>> setComponentProperties = (Set<Map<String, String>>) groupProperties.get(
          BlueprintResourceProvider.COMPONENT_PROPERTY_ID);

      Collection<HostGroupComponentEntity> components = new ArrayList<>();
      for (Map<String, String> compProperties : setComponentProperties) {
        HostGroupComponentEntity component = new HostGroupComponentEntity();
        components.add(component);
        component.setName(compProperties.get(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID));
      }
      hostGroup.setComponents(components);

    }
    entity.setHostGroups(hostGroups);


    Collection<Map<String, String>> configProperties = (Collection<Map<String, String>>) properties.get(
        BlueprintResourceProvider.CONFIGURATION_PROPERTY_ID);
    createProvider().createBlueprintConfigEntities(configProperties, entity);
    return entity;
  }

  private Map<String, String> getTestRequestInfoProperties() {
    Map<String, String> setPropertiesInfo = new HashMap<>();
    String configurationData = "{\"configurations\":[{\"config-type1\":{\"properties\" :{\"property\":\"property-value\"}}},"
        + "{\"config-type2\" : {\"properties_attributes\" : {\"property\" : \"property-value\"}, \"properties\" : {\"property\" : \"property-value\"}}}"
        + "]}";
    setPropertiesInfo.put(Request.REQUEST_INFO_BODY_PROPERTY, configurationData);
    return setPropertiesInfo;
  }

  @Test
  public void testPopulateConfigurationEntity_oldSchema() throws Exception {
    Map<String, String> configuration = new HashMap<>();
    configuration.put("global/property1", "val1");
    configuration.put("global/property2", "val2");
    BlueprintConfiguration config = new BlueprintConfigEntity();

    provider.populateConfigurationEntity(configuration, config);

    assertNotNull(config.getConfigData());
    assertNotNull(config.getConfigAttributes());
    Map<?, ?> configData = StageUtils.getGson().fromJson(config.getConfigData(), Map.class);
    Map<?, Map<?, ?>> configAttrs = StageUtils.getGson().fromJson(config.getConfigAttributes(), Map.class);
    assertNotNull(configData);
    assertNotNull(configAttrs);
    assertEquals(2, configData.size());
    assertTrue(configData.containsKey("property1"));
    assertTrue(configData.containsKey("property2"));
    assertEquals("val1", configData.get("property1"));
    assertEquals("val2", configData.get("property2"));
    assertEquals(0, configAttrs.size());
  }

  @Test
  public void testPopulateConfigurationEntity_newSchema() throws Exception {
    Map<String, String> configuration = new HashMap<>();
    configuration.put("global/properties/property1", "val1");
    configuration.put("global/properties/property2", "val2");
    configuration.put("global/properties_attributes/final/property1", "true");
    configuration.put("global/properties_attributes/final/property2", "false");
    configuration.put("global/properties_attributes/deletable/property1", "true");
    BlueprintConfiguration config = new BlueprintConfigEntity();

    provider.populateConfigurationEntity(configuration, config);

    assertNotNull(config.getConfigData());
    assertNotNull(config.getConfigAttributes());
    Map<?, ?> configData = StageUtils.getGson().fromJson(config.getConfigData(), Map.class);
    Map<?, Map<?, ?>> configAttrs = StageUtils.getGson().fromJson(config.getConfigAttributes(), Map.class);
    assertNotNull(configData);
    assertNotNull(configAttrs);
    assertEquals(2, configData.size());
    assertTrue(configData.containsKey("property1"));
    assertTrue(configData.containsKey("property2"));
    assertEquals("val1", configData.get("property1"));
    assertEquals("val2", configData.get("property2"));
    assertEquals(2, configAttrs.size());
    assertTrue(configAttrs.containsKey("final"));
    assertTrue(configAttrs.containsKey("deletable"));
    Map<?, ?> finalAttrs = configAttrs.get("final");
    assertNotNull(finalAttrs);
    assertEquals(2, finalAttrs.size());
    assertTrue(finalAttrs.containsKey("property1"));
    assertTrue(finalAttrs.containsKey("property2"));
    assertEquals("true", finalAttrs.get("property1"));
    assertEquals("false", finalAttrs.get("property2"));

    Map<?, ?> deletableAttrs = configAttrs.get("deletable");
    assertNotNull(deletableAttrs);
    assertEquals(1, deletableAttrs.size());
    assertTrue(deletableAttrs.containsKey("property1"));
    assertEquals("true", deletableAttrs.get("property1"));
  }

  @Test
  public void testPopulateConfigurationEntity_configIsNull() throws Exception {
    Map<String, String> configuration = null;
    BlueprintConfiguration config = new BlueprintConfigEntity();

    provider.populateConfigurationEntity(configuration, config);

    assertNotNull(config.getConfigAttributes());
    assertNotNull(config.getConfigData());
  }

  @Test
  public void testPopulateConfigurationEntity_configIsEmpty() throws Exception {
    Map<String, String> configuration = new HashMap<>();
    BlueprintConfiguration config = new BlueprintConfigEntity();

    provider.populateConfigurationEntity(configuration, config);

    assertNotNull(config.getConfigAttributes());
    assertNotNull(config.getConfigData());
  }

  @Test
  public void testDecidePopulationStrategy_configIsEmpty() throws Exception {
    Map<String, String> configMap = new HashMap<>();

    BlueprintConfigPopulationStrategy provisioner =
        provider.decidePopulationStrategy(configMap);

    assertNotNull(provisioner);
    assertTrue(provisioner instanceof BlueprintConfigPopulationStrategyV2);
  }

  @Test
  public void testDecidePopulationStrategy_configIsNull() throws Exception {
    Map<String, String> configMap = null;

    BlueprintConfigPopulationStrategy provisioner =
        provider.decidePopulationStrategy(configMap);

    assertNotNull(provisioner);
    assertTrue(provisioner instanceof BlueprintConfigPopulationStrategyV2);
  }

  @Test
  public void testDecidePopulationStrategy_withOldSchema() throws Exception {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("global/hive_database", "db");

    BlueprintConfigPopulationStrategy provisioner =
        provider.decidePopulationStrategy(configMap);

    assertNotNull(provisioner);
    assertTrue(provisioner instanceof BlueprintConfigPopulationStrategyV1);
  }

  @Test
  public void testDecidePopulationStrategy_withNewSchema_attributes() throws Exception {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("global/properties_attributes/final/foo_contact", "true");

    BlueprintConfigPopulationStrategy provisioner =
        provider.decidePopulationStrategy(configMap);

    assertNotNull(provisioner);
    assertTrue(provisioner instanceof BlueprintConfigPopulationStrategyV2);
  }

  @Test
  public void testDecidePopulationStrategy_withNewSchema_properties() throws Exception {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("global/properties/foo_contact", "foo@ffl.dsfds");

    BlueprintConfigPopulationStrategy provisioner =
        provider.decidePopulationStrategy(configMap);

    assertNotNull(provisioner);
    assertTrue(provisioner instanceof BlueprintConfigPopulationStrategyV2);
  }

  @Test
  public void testDecidePopulationStrategy_unsupportedSchema() throws Exception {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("global/properties/lot/foo_contact", "foo@ffl.dsfds");
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(BlueprintResourceProvider.SCHEMA_IS_NOT_SUPPORTED_MESSAGE);

    provider.decidePopulationStrategy(configMap);
  }

  @Test
  public void testPopulateConfigurationList() throws Exception {
    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("test-stack-name");
    stackEntity.setStackVersion("test-stack-version");
    BlueprintEntity entity = createMock(BlueprintEntity.class);
    expect(entity.getStack()).andReturn(stackEntity).anyTimes();

    HashMap<PropertyInfo.PropertyType, Set<String>> pwdProperties = new HashMap<PropertyInfo.PropertyType, Set<String>>() {{
      put(PropertyInfo.PropertyType.PASSWORD, new HashSet<String>(){{
        add("test.password");
      }});
    }};

    StackInfo info = createMock(StackInfo.class);
    expect(info.getConfigPropertiesTypes("type1")).andReturn(new HashMap<>()).anyTimes();
    expect(info.getConfigPropertiesTypes("type2")).andReturn(new HashMap<>()).anyTimes();
    expect(info.getConfigPropertiesTypes("type3")).andReturn(pwdProperties).anyTimes();
    expect(metaInfo.getStack("test-stack-name", "test-stack-version")).andReturn(info).anyTimes();

    replay(info, metaInfo, entity);


    // attributes is null
    BlueprintConfigEntity config1 = new BlueprintConfigEntity();
    config1.setType("type1");
    config1.setConfigData("{\"key1\":\"value1\"}");
    config1.setBlueprintEntity(entity);
    // attributes is empty
    BlueprintConfigEntity config2 = new BlueprintConfigEntity();
    config2.setType("type2");
    config2.setConfigData("{\"key2\":\"value2\"}");
    config2.setConfigAttributes("{}");
    config2.setBlueprintEntity(entity);
    // attributes is provided
    BlueprintConfigEntity config3 = new BlueprintConfigEntity();
    config3.setType("type3");
    config3.setConfigData("{\"key3\":\"value3\",\"key4\":\"value4\",\"test.password\":\"pwdValue\"}");
    config3.setConfigAttributes("{\"final\":{\"key3\":\"attrValue1\",\"key4\":\"attrValue2\"}}");
    config3.setBlueprintEntity(entity);

    List<Map<String, Map<String, Object>>> configs =
        provider.populateConfigurationList(Arrays.asList(config1, config2, config3));

    assertNotNull(configs);
    assertEquals(3, configs.size());
    Map<String, Map<String, Object>> configuration1 = configs.get(0);
    assertNotNull(configuration1);
    assertEquals(1, configuration1.size());
    assertTrue(configuration1.containsKey("type1"));
    Map<String, Object> typeConfig1 = configuration1.get("type1");
    assertNotNull(typeConfig1);
    assertEquals(1, typeConfig1.size());
    assertTrue(typeConfig1.containsKey(BlueprintResourceProvider.PROPERTIES_PROPERTY_ID));
    Map<String, String> confProperties1
        = (Map<String, String>) typeConfig1.get(BlueprintResourceProvider.PROPERTIES_PROPERTY_ID);
    assertNotNull(confProperties1);
    assertEquals(1, confProperties1.size());
    assertEquals("value1", confProperties1.get("key1"));

    Map<String, Map<String, Object>> configuration2 = configs.get(1);
    assertNotNull(configuration2);
    assertEquals(1, configuration2.size());
    assertTrue(configuration2.containsKey("type2"));
    Map<String, Object> typeConfig2 = configuration2.get("type2");
    assertNotNull(typeConfig2);
    assertEquals(1, typeConfig2.size());
    assertTrue(typeConfig2.containsKey(BlueprintResourceProvider.PROPERTIES_PROPERTY_ID));
    Map<String, String> confProperties2
        = (Map<String, String>) typeConfig2.get(BlueprintResourceProvider.PROPERTIES_PROPERTY_ID);
    assertNotNull(confProperties2);
    assertEquals(1, confProperties2.size());
    assertEquals("value2", confProperties2.get("key2"));

    Map<String, Map<String, Object>> configuration3 = configs.get(2);
    assertNotNull(configuration3);
    assertEquals(1, configuration3.size());
    assertTrue(configuration3.containsKey("type3"));
    Map<String, Object> typeConfig3 = configuration3.get("type3");
    assertNotNull(typeConfig3);
    assertEquals(2, typeConfig3.size());
    assertTrue(typeConfig3.containsKey(BlueprintResourceProvider.PROPERTIES_PROPERTY_ID));
    Map<String, String> confProperties3
        = (Map<String, String>) typeConfig3.get(BlueprintResourceProvider.PROPERTIES_PROPERTY_ID);
    assertNotNull(confProperties3);
    assertEquals(3, confProperties3.size());
    assertEquals("value3", confProperties3.get("key3"));
    assertEquals("value4", confProperties3.get("key4"));
    assertEquals("SECRET:type3:-1:test.password", confProperties3.get("test.password"));
    assertTrue(typeConfig3.containsKey(BlueprintResourceProvider.PROPERTIES_ATTRIBUTES_PROPERTY_ID));
    Map<String, Map<String, String>> confAttributes3
        = (Map<String, Map<String, String>>) typeConfig3.get(BlueprintResourceProvider.PROPERTIES_ATTRIBUTES_PROPERTY_ID);
    assertNotNull(confAttributes3);
    assertEquals(1, confAttributes3.size());
    assertTrue(confAttributes3.containsKey("final"));
    Map<String, String> finalAttrs = confAttributes3.get("final");
    assertEquals(2, finalAttrs.size());
    assertEquals("attrValue1", finalAttrs.get("key3"));
    assertEquals("attrValue2", finalAttrs.get("key4"));
  }

  @Test
  public void testPopulateSettingList() throws Exception {
    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("test-stack-name");
    stackEntity.setStackVersion("test-stack-version");
    BlueprintEntity entity = createMock(BlueprintEntity.class);
    expect(entity.getStack()).andReturn(stackEntity).anyTimes();

    HashMap<PropertyInfo.PropertyType, Set<String>> pwdProperties = new HashMap<PropertyInfo.PropertyType, Set<String>>() {{
      put(PropertyInfo.PropertyType.PASSWORD, new HashSet<String>(){{
        add("test.password");
      }});
    }};

    StackInfo info = createMock(StackInfo.class);
    expect(info.getConfigPropertiesTypes("type1")).andReturn(new HashMap<>()).anyTimes();
    expect(info.getConfigPropertiesTypes("type2")).andReturn(new HashMap<>()).anyTimes();
    expect(info.getConfigPropertiesTypes("type3")).andReturn(pwdProperties).anyTimes();
    expect(metaInfo.getStack("test-stack-name", "test-stack-version")).andReturn(info).anyTimes();

    replay(info, metaInfo, entity);

    // Blueprint setting entities
    // Global recovery setting
    BlueprintSettingEntity settingEntity1 = new BlueprintSettingEntity();
    settingEntity1.setSettingName("recovery_settings");
    settingEntity1.setSettingData("[{\"recovery_enabled\":\"true\"}]");
    settingEntity1.setBlueprintEntity(entity);

    // Service exceptions setting
    BlueprintSettingEntity settingEntity2 = new BlueprintSettingEntity();
    settingEntity2.setSettingName("service_settings");
    settingEntity2.setSettingData("[{\"name\":\"HDFS\", \"recovery_enabled\":\"false\"}, " +
            "{\"name\":\"ZOOKEEPER\", \"recovery_enabled\":\"false\"}]");
    settingEntity2.setBlueprintEntity(entity);

    // Service component exceptions setting
    BlueprintSettingEntity settingEntity3 = new BlueprintSettingEntity();
    settingEntity3.setSettingName("component_settings");
    settingEntity3.setSettingData("[{\"name\":\"METRICS_MONITOR\", \"recovery_enabled\":\"false\"}," +
            "{\"name\":\"KAFKA_CLIENT\", \"recovery_enabled\":\"false\"}]");
    settingEntity3.setBlueprintEntity(entity);

    List<BlueprintSettingEntity> settingEntities = new ArrayList();
    settingEntities.add(settingEntity1);
    settingEntities.add(settingEntity2);
    settingEntities.add(settingEntity3);

    List<Map<String, Object>> settings = provider.populateSettingList(settingEntities);

    assertNotNull(settings);
    assertEquals(settingEntities.size(), settings.size());

    // Verify global recovery setting
    Map<String, Object> setting1 = settings.get(0);
    assertNotNull(setting1);
    assertEquals(1, setting1.size());
    assertTrue(setting1.containsKey("recovery_settings"));
    List<Map<String, String>> setting1value = (List<Map<String, String>>) setting1.get("recovery_settings");
    assertNotNull(setting1value);
    assertEquals(1, setting1value.size());
    assertTrue(setting1value.get(0).containsKey("recovery_enabled"));
    assertEquals(setting1value.get(0).get("recovery_enabled"), "true");

    // Verify service exceptions
    Map<String, Object> setting2 = settings.get(1);
    assertNotNull(setting2);
    assertEquals(1, setting2.size());
    assertTrue(setting2.containsKey("service_settings"));
    List<Map<String, String>> setting2value = (List<Map<String, String>>) setting2.get("service_settings");
    assertNotNull(setting2value);
    assertEquals(2, setting2value.size());
    // first service exception is HDFS
    assertTrue(setting2value.get(0).containsKey("name"));
    assertEquals(setting2value.get(0).get("name"), "HDFS");
    assertTrue(setting2value.get(0).containsKey("recovery_enabled"));
    assertEquals(setting2value.get(0).get("recovery_enabled"), "false");
    // second service exception is ZOOKEEPER
    assertTrue(setting2value.get(1).containsKey("name"));
    assertEquals(setting2value.get(1).get("name"), "ZOOKEEPER");
    assertTrue(setting2value.get(1).containsKey("recovery_enabled"));
    assertEquals(setting2value.get(1).get("recovery_enabled"), "false");

    // Verify service component exceptions
    Map<String, Object> setting3 = settings.get(2);
    assertNotNull(setting3);
    assertEquals(1, setting3.size());
    assertTrue(setting3.containsKey("component_settings"));
    List<Map<String, String>> setting3value = (List<Map<String, String>>) setting3.get("component_settings");
    assertNotNull(setting3value);
    assertEquals(2, setting3value.size());
    // first service component exception is METRICS_MONITOR
    assertTrue(setting3value.get(0).containsKey("name"));
    assertEquals(setting3value.get(0).get("name"), "METRICS_MONITOR");
    assertTrue(setting3value.get(0).containsKey("recovery_enabled"));
    assertEquals(setting3value.get(0).get("recovery_enabled"), "false");
    // second service component exception is KAFKA_CLIENT
    assertTrue(setting3value.get(1).containsKey("name"));
    assertEquals(setting3value.get(1).get("name"), "KAFKA_CLIENT");
    assertTrue(setting3value.get(1).containsKey("recovery_enabled"));
    assertEquals(setting3value.get(1).get("recovery_enabled"), "false");
  }
}

