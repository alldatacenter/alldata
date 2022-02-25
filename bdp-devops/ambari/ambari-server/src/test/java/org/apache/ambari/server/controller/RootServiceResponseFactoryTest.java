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

package org.apache.ambari.server.controller;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.apache.ambari.server.controller.RootComponent.AMBARI_SERVER;
import static org.apache.ambari.server.controller.RootService.AMBARI;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class RootServiceResponseFactoryTest {

  private Injector injector;

  @Inject
  private RootServiceResponseFactory responseFactory;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private Configuration config;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void getReturnsAllServicesForNullServiceName() throws Exception {
    // Request a null service name
    RootServiceRequest request = new RootServiceRequest(null);
    Set<RootServiceResponse> rootServices = responseFactory.getRootServices(request);
    assertEquals(RootService.values().length, rootServices.size());
  }

  @Test
  public void getReturnsAllServicesForNullRequest() throws Exception {
    // null request
    Set<RootServiceResponse> rootServices = responseFactory.getRootServices(null);
    assertEquals(RootService.values().length, rootServices.size());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void getThrowsForNonExistentService() throws Exception {
    // Request nonexistent service
    RootServiceRequest request = new RootServiceRequest("XXX");
    responseFactory.getRootServices(request);
  }

  @Test
  public void getReturnsSingleServiceForValidServiceName() throws Exception {
    // Request existent service
    RootServiceRequest request = new RootServiceRequest(AMBARI.name());
    Set<RootServiceResponse> rootServices = responseFactory.getRootServices(request);
    assertEquals(Collections.singleton(new RootServiceResponse(AMBARI.name())), rootServices);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void getThrowsForNullServiceNameNullComponentName() throws Exception {
    // Request null service name, null component name
    RootServiceComponentRequest request = new RootServiceComponentRequest(null, null);

    responseFactory.getRootServiceComponents(request);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void getThrowsForNullServiceNameValidComponentName() throws Exception {
    // Request null service name, not-null component name
    RootServiceComponentRequest request = new RootServiceComponentRequest(null, AMBARI_SERVER.name());

    responseFactory.getRootServiceComponents(request);
  }

  @Test
  public void getReturnsAllComponentsForValidServiceNameNullComponentName() throws Exception {
    // Request existent service name, null component name
    RootServiceComponentRequest request = new RootServiceComponentRequest(AMBARI.name(), null);

    Set<RootServiceComponentResponse> rootServiceComponents = responseFactory.getRootServiceComponents(request);
    assertEquals(AMBARI.getComponents().length, rootServiceComponents.size());

    for (int i = 0; i < AMBARI.getComponents().length; i++) {
      RootComponent component = AMBARI.getComponents()[i];

      if (component.name().equals(AMBARI_SERVER.name())) {
        for (RootServiceComponentResponse response : rootServiceComponents) {
          if (response.getComponentName().equals(AMBARI_SERVER.name())) {
            verifyResponseForAmbariServer(response);
          }
        }
      } else {
        assertTrue(rootServiceComponents.contains(new RootServiceComponentResponse(
            AMBARI.name(), component.name(), RootServiceResponseFactory.NOT_APPLICABLE,
            Collections.emptyMap())));
      }
    }
  }

  @Test
  public void getReturnsSingleComponentForValidServiceAndComponentName() throws Exception {
    // Request existent service name, existent component name
    RootServiceComponentRequest request = new RootServiceComponentRequest(AMBARI.name(), AMBARI_SERVER.name());

    Set<RootServiceComponentResponse> rootServiceComponents = responseFactory.getRootServiceComponents(request);

    assertEquals(1, rootServiceComponents.size());
    for (RootServiceComponentResponse response : rootServiceComponents) {
      verifyResponseForAmbariServer(response);
    }
  }

  @Test(expected = ObjectNotFoundException.class)
  public void getThrowsForNonexistentComponent() throws Exception {
    // Request existent service name, and component, not belongs to requested service
    RootServiceComponentRequest request = new RootServiceComponentRequest(AMBARI.name(), "XXX");
    responseFactory.getRootServiceComponents(request);
  }

  private void verifyResponseForAmbariServer(RootServiceComponentResponse response) {
    assertEquals(ambariMetaInfo.getServerVersion(), response.getComponentVersion());
    // all properties from config + "jdk_location" + "java.version"
    int expectedPropertyCount = config.getAmbariProperties().size() + 2;
    assertEquals(response.getProperties().toString(), expectedPropertyCount, response.getProperties().size());
    assertTrue(response.getProperties().containsKey("jdk_location"));
    assertTrue(response.getProperties().containsKey("java.version"));
  }
}
