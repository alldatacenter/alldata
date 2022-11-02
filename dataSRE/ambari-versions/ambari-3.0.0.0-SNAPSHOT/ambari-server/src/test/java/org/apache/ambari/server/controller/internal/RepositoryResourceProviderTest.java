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


import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RepositoryRequest;
import org.apache.ambari.server.controller.RepositoryResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.stack.RepoTag;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryResourceProviderTest {

  private static final String VAL_STACK_NAME = "HDP";
  private static final String VAL_STACK_VERSION = "0.2";
  private static final String VAL_OS = "centos6";
  private static final String VAL_REPO_ID = "HDP-0.2";
  private static final String VAL_REPO_NAME = "HDP1";
  private static final String VAL_BASE_URL = "http://foo.com";
  private static final String VAL_DISTRIBUTION = "mydist";
  private static final String VAL_COMPONENT_NAME = "mycomponentname";

  @Test
  public void testGetResources() throws Exception{
    AmbariManagementController managementController = EasyMock.createMock(AmbariManagementController.class);

    RepositoryResponse rr = new RepositoryResponse(VAL_BASE_URL, VAL_OS,
        VAL_REPO_ID, VAL_REPO_NAME, VAL_DISTRIBUTION, VAL_COMPONENT_NAME, null, null,
      Collections.<RepoTag>emptySet(), Collections.emptyList());
    rr.setStackName(VAL_STACK_NAME);
    rr.setStackVersion(VAL_STACK_VERSION);
    Set<RepositoryResponse> allResponse = new HashSet<>();
    allResponse.add(rr);

    // set expectations
    expect(managementController.getRepositories(EasyMock.anyObject())).andReturn(allResponse).times(2);

    // replay
    replay(managementController);

    ResourceProvider provider = new RepositoryResourceProvider(managementController);

    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_CLUSTER_STACK_VERSION_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID).equals(VAL_STACK_NAME)
          .and().property(RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID).equals(VAL_STACK_VERSION)
          .toPredicate();

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(allResponse.size(), resources.size());

    for (Resource resource : resources) {
      Object o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID);
      Assert.assertEquals(VAL_STACK_NAME, o);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID);
      Assert.assertEquals(VAL_STACK_VERSION, o);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID);
      Assert.assertEquals(o, VAL_REPO_NAME);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID);
      Assert.assertEquals(o, VAL_BASE_URL);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID);
      Assert.assertEquals(o, VAL_OS);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID);
      Assert.assertEquals(o, VAL_REPO_ID);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_CLUSTER_STACK_VERSION_PROPERTY_ID);
      Assert.assertNull(o);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID);
      Assert.assertEquals(o, VAL_DISTRIBUTION);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID);
      Assert.assertEquals(o, VAL_COMPONENT_NAME);
    }

    // !!! check that the stack version id is returned
    rr.setClusterVersionId(525L);
    resources = provider.getResources(request, predicate);
    Assert.assertEquals(allResponse.size(), resources.size());

    for (Resource resource : resources) {
      Object o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID);
      Assert.assertEquals(VAL_STACK_NAME, o);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID);
      Assert.assertEquals(VAL_STACK_VERSION, o);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID);
      Assert.assertEquals(o, VAL_REPO_NAME);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID);
      Assert.assertEquals(o, VAL_BASE_URL);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID);
      Assert.assertEquals(o, VAL_OS);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID);
      Assert.assertEquals(o, VAL_REPO_ID);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_CLUSTER_STACK_VERSION_PROPERTY_ID);
      Assert.assertEquals(525L, o);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID);
      Assert.assertEquals(o, VAL_DISTRIBUTION);

      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID);
      Assert.assertEquals(o, VAL_COMPONENT_NAME);
    }

    // verify
    verify(managementController);
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Repository;

    AmbariManagementController managementController = EasyMock.createMock(AmbariManagementController.class);

    RepositoryResponse rr = new RepositoryResponse(VAL_BASE_URL, VAL_OS,
      VAL_REPO_ID, VAL_REPO_NAME, null, null, null, null ,
      Collections.<RepoTag>emptySet(), Collections.emptyList());
    Set<RepositoryResponse> allResponse = new HashSet<>();
    allResponse.add(rr);

    // set expectations
    expect(managementController.getRepositories(EasyMock.anyObject())).andReturn(allResponse).times(1);
    managementController.verifyRepositories(EasyMock.<Set<RepositoryRequest>>anyObject());

    // replay
    replay(managementController);

    ResourceProvider provider = new RepositoryResourceProvider(managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID, "http://garbage.eu.co");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate =
        new PredicateBuilder().property(RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID).equals(VAL_STACK_NAME)
          .and().property(RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID).equals(VAL_STACK_VERSION)
          .toPredicate();

    provider.updateResources(request, predicate);

    // verify
    verify(managementController);
  }

}
