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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ActionRequest;
import org.apache.ambari.server.controller.ActionResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ActionResourceProviderTest {

  private Injector injector;

  public static ActionResourceProvider getActionDefinitionResourceProvider(
      AmbariManagementController managementController) {
    Resource.Type type = Resource.Type.Action;

    return (ActionResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);
  }

  public static Set<ActionResponse> getActions(AmbariManagementController controller,
                                               Set<ActionRequest> requests)
      throws AmbariException {
    ActionResourceProvider provider = getActionDefinitionResourceProvider(controller);
    return provider.getActionDefinitions(requests);
  }

  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Action;
    AmbariMetaInfo am = createNiceMock(AmbariMetaInfo.class);
    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    expect(managementController.getAmbariMetaInfo()).andReturn(am).anyTimes();

    List<ActionDefinition> allDefinition = new ArrayList<>();
    allDefinition.add(new ActionDefinition(
        "a1", ActionType.SYSTEM, "fileName", "HDFS", "DATANODE", "Does file exist", TargetHostType.ANY,
        Short.valueOf("100"), null));
    allDefinition.add(new ActionDefinition(
        "a2", ActionType.SYSTEM, "fileName", "HDFS", "DATANODE", "Does file exist", TargetHostType.ANY,
        Short.valueOf("100"), null));
    allDefinition.add(new ActionDefinition(
        "a3", ActionType.SYSTEM, "fileName", "HDFS", "DATANODE", "Does file exist", TargetHostType.ANY,
        Short.valueOf("100"), null));

    Set<ActionResponse> allResponse = new HashSet<>();
    for (ActionDefinition definition : allDefinition) {
      allResponse.add(definition.convertToResponse());
    }

    ActionDefinition namedDefinition = new ActionDefinition(
        "a1", ActionType.SYSTEM, "fileName", "HDFS", "DATANODE", "Does file exist", TargetHostType.ANY,
        Short.valueOf("100"), null);

    Set<ActionResponse> nameResponse = new HashSet<>();
    nameResponse.add(namedDefinition.convertToResponse());

    expect(am.getAllActionDefinition()).andReturn(allDefinition).once();
    expect(am.getActionDefinition("a1")).andReturn(namedDefinition).once();

    replay(managementController, am);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(ActionResourceProvider.ACTION_NAME_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.ACTION_TYPE_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.DEFAULT_TIMEOUT_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.DESCRIPTION_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.INPUTS_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.TARGET_COMPONENT_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.TARGET_HOST_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.TARGET_SERVICE_PROPERTY_ID);


    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(allResponse.size(), resources.size());
    for (Resource resource : resources) {
      String actionName = (String) resource.getPropertyValue(ActionResourceProvider.ACTION_NAME_PROPERTY_ID);
      String actionType = (String) resource.getPropertyValue(ActionResourceProvider.ACTION_TYPE_PROPERTY_ID);
      String defaultTimeout = (String) resource.getPropertyValue(ActionResourceProvider.DEFAULT_TIMEOUT_PROPERTY_ID);
      String description = (String) resource.getPropertyValue(ActionResourceProvider.DESCRIPTION_PROPERTY_ID);
      String inputs = (String) resource.getPropertyValue(ActionResourceProvider.INPUTS_PROPERTY_ID);
      String comp = (String) resource.getPropertyValue(ActionResourceProvider.TARGET_COMPONENT_PROPERTY_ID);
      String svc = (String) resource.getPropertyValue(ActionResourceProvider.TARGET_SERVICE_PROPERTY_ID);
      String host = (String) resource.getPropertyValue(ActionResourceProvider.TARGET_HOST_PROPERTY_ID);
      Assert.assertTrue(allResponse.contains(new ActionResponse(actionName, actionType,
          inputs, svc, comp, description, host, defaultTimeout)));
    }

    // get actions named a1
    Predicate predicate =
        new PredicateBuilder().property(ActionResourceProvider.ACTION_NAME_PROPERTY_ID).
            equals("a1").toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals("a1", resources.iterator().next().
        getPropertyValue(ActionResourceProvider.ACTION_NAME_PROPERTY_ID));


    // verify
    verify(managementController);
  }
}
