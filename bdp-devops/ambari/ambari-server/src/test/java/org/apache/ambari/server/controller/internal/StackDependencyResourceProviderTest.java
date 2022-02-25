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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.state.DependencyInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * StackDependencyResourceProvider unit tests.
 */
public class StackDependencyResourceProviderTest {

  private static final AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
  @BeforeClass
  public static void initClass() {
    StackDependencyResourceProvider.init(metaInfo);
  }

  @Before
  public void resetGlobalMocks() {
    reset(metaInfo);
  }

  @Test
  public void testGetResources() throws SystemException, UnsupportedPropertyException,
      NoSuchParentResourceException, NoSuchResourceException, AmbariException {

    Request request = createNiceMock(Request.class);

    DependencyInfo dependencyInfo = new DependencyInfo();
    dependencyInfo.setName("service_name/comp_name");
    dependencyInfo.setScope("cluster");

    Predicate namePredicate = new EqualsPredicate<>(
      StackDependencyResourceProvider.COMPONENT_NAME_ID, "comp_name");
    Predicate depServicePredicate = new EqualsPredicate<>(
      StackDependencyResourceProvider.DEPENDENT_SERVICE_NAME_ID, "dep_service_name");
    Predicate depCompPredicate = new EqualsPredicate<>(
      StackDependencyResourceProvider.DEPENDENT_COMPONENT_NAME_ID, "dep_comp_name");
    Predicate stackNamePredicate = new EqualsPredicate<>
      (StackDependencyResourceProvider.STACK_NAME_ID, "stack_name");
    Predicate stackVersionPredicate = new EqualsPredicate<>(
      StackDependencyResourceProvider.STACK_VERSION_ID, "stack_version");

    Predicate andPredicate = new AndPredicate(namePredicate, depServicePredicate,
        depCompPredicate, stackNamePredicate, stackVersionPredicate);

    //mock expectations
    expect(metaInfo.getComponentDependency("stack_name", "stack_version", "dep_service_name",
        "dep_comp_name", "comp_name")).andReturn(dependencyInfo);

    replay(metaInfo, request);

    ResourceProvider provider = createProvider();
    Set<Resource> resources = provider.getResources(request, andPredicate);

    verify(metaInfo);

    assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();
    assertEquals("cluster", resource.getPropertyValue(StackDependencyResourceProvider.SCOPE_ID));
    assertEquals("comp_name", resource.getPropertyValue(StackDependencyResourceProvider.COMPONENT_NAME_ID));
    assertEquals("service_name", resource.getPropertyValue(StackDependencyResourceProvider.SERVICE_NAME_ID));
    assertEquals("dep_service_name", resource.getPropertyValue(StackDependencyResourceProvider.DEPENDENT_SERVICE_NAME_ID));
    assertEquals("dep_comp_name", resource.getPropertyValue(StackDependencyResourceProvider.DEPENDENT_COMPONENT_NAME_ID));
    assertEquals("stack_name", resource.getPropertyValue(StackDependencyResourceProvider.STACK_NAME_ID));
    assertEquals("stack_version", resource.getPropertyValue(StackDependencyResourceProvider.STACK_VERSION_ID));
  }

  @Test
  public void testGetResources_Query() throws SystemException, UnsupportedPropertyException,
      NoSuchParentResourceException, NoSuchResourceException, AmbariException {

    Request request = createNiceMock(Request.class);

    DependencyInfo dependencyInfo = new DependencyInfo();
    dependencyInfo.setName("service_name/comp_name");
    dependencyInfo.setScope("cluster");

    Predicate namePredicate = new EqualsPredicate<>(
      StackDependencyResourceProvider.COMPONENT_NAME_ID, "comp_name");
    Predicate name2Predicate = new EqualsPredicate<>(
      StackDependencyResourceProvider.COMPONENT_NAME_ID, "comp_name2");
    Predicate depServicePredicate = new EqualsPredicate<>(
      StackDependencyResourceProvider.DEPENDENT_SERVICE_NAME_ID, "dep_service_name");
    Predicate depCompPredicate = new EqualsPredicate<>(
      StackDependencyResourceProvider.DEPENDENT_COMPONENT_NAME_ID, "dep_comp_name");
    Predicate stackNamePredicate = new EqualsPredicate<>
      (StackDependencyResourceProvider.STACK_NAME_ID, "stack_name");
    Predicate stackVersionPredicate = new EqualsPredicate<>(
      StackDependencyResourceProvider.STACK_VERSION_ID, "stack_version");

    Predicate andPredicate1 = new AndPredicate(namePredicate, depServicePredicate,
        depCompPredicate, stackNamePredicate, stackVersionPredicate);
    Predicate andPredicate2 = new AndPredicate(name2Predicate, depServicePredicate,
        depCompPredicate, stackNamePredicate, stackVersionPredicate);
    Predicate orPredicate = new OrPredicate(andPredicate1, andPredicate2);

    //mock expectations
    expect(metaInfo.getComponentDependency("stack_name", "stack_version", "dep_service_name",
        "dep_comp_name", "comp_name")).andReturn(dependencyInfo);
    expect(metaInfo.getComponentDependency("stack_name", "stack_version", "dep_service_name",
        "dep_comp_name", "comp_name2")).andThrow(new StackAccessException("test"));

    replay(metaInfo, request);

    ResourceProvider provider = createProvider();
    Set<Resource> resources = provider.getResources(request, orPredicate);

    verify(metaInfo);

    assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();
    assertEquals("cluster", resource.getPropertyValue(StackDependencyResourceProvider.SCOPE_ID));
    assertEquals("comp_name", resource.getPropertyValue(StackDependencyResourceProvider.COMPONENT_NAME_ID));
    assertEquals("service_name", resource.getPropertyValue(StackDependencyResourceProvider.SERVICE_NAME_ID));
    assertEquals("dep_service_name", resource.getPropertyValue(StackDependencyResourceProvider.DEPENDENT_SERVICE_NAME_ID));
    assertEquals("dep_comp_name", resource.getPropertyValue(StackDependencyResourceProvider.DEPENDENT_COMPONENT_NAME_ID));
    assertEquals("stack_name", resource.getPropertyValue(StackDependencyResourceProvider.STACK_NAME_ID));
    assertEquals("stack_version", resource.getPropertyValue(StackDependencyResourceProvider.STACK_VERSION_ID));
  }

  private StackDependencyResourceProvider createProvider() {
    return new StackDependencyResourceProvider();
  }
}
