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

package org.apache.ambari.server.api.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

/**
 * ResourceInstanceFactoryImpl unit tests.
 */
public class ResourceInstanceFactoryImplTest {

  @Test
  public void testGetStackArtifactDefinition() {
    ResourceDefinition resourceDefinition = ResourceInstanceFactoryImpl.getResourceDefinition(
        Resource.Type.StackArtifact, null);

    assertEquals("artifact", resourceDefinition.getSingularName());
    assertEquals("artifacts", resourceDefinition.getPluralName());
    assertEquals(Resource.Type.StackArtifact, resourceDefinition.getType());
  }

  @Test
  public void testGetArtifactDefinition() {
    ResourceDefinition resourceDefinition = ResourceInstanceFactoryImpl.getResourceDefinition(
        Resource.Type.Artifact, null);

    assertEquals("artifact", resourceDefinition.getSingularName());
    assertEquals("artifacts", resourceDefinition.getPluralName());
    assertEquals(Resource.Type.Artifact, resourceDefinition.getType());
  }

  @Test
  public void testGetHostDefinition() {
    ResourceInstanceFactoryImpl resourceInstanceFactory = new ResourceInstanceFactoryImpl();
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Host, "TeSTHost1");
    ResourceInstance resourceInstance = resourceInstanceFactory.createResource(
            Resource.Type.Host, mapIds);

    assertEquals(mapIds.get(Resource.Type.Host), "testhost1");
  }

  @Test
  public void testGetHostKerberosIdentityDefinition() {
    ResourceDefinition resourceDefinition = ResourceInstanceFactoryImpl.getResourceDefinition(
        Resource.Type.HostKerberosIdentity, null);

    assertNotNull(resourceDefinition);
    assertEquals("kerberos_identity", resourceDefinition.getSingularName());
    assertEquals("kerberos_identities", resourceDefinition.getPluralName());
    assertEquals(Resource.Type.HostKerberosIdentity, resourceDefinition.getType());
  }

  @Test
  public void testGetRoleAuthorizationDefinition() {
    ResourceDefinition resourceDefinition = ResourceInstanceFactoryImpl.getResourceDefinition(
        Resource.Type.RoleAuthorization, null);

    assertNotNull(resourceDefinition);
    assertEquals("authorization", resourceDefinition.getSingularName());
    assertEquals("authorizations", resourceDefinition.getPluralName());
    assertEquals(Resource.Type.RoleAuthorization, resourceDefinition.getType());
  }

  @Test
  public void testGetUserAuthorizationDefinition() {
    ResourceDefinition resourceDefinition = ResourceInstanceFactoryImpl.getResourceDefinition(
        Resource.Type.UserAuthorization, null);

    assertNotNull(resourceDefinition);
    assertEquals("authorization", resourceDefinition.getSingularName());
    assertEquals("authorizations", resourceDefinition.getPluralName());
    assertEquals(Resource.Type.UserAuthorization, resourceDefinition.getType());
  }

  @Test
  public void testGetClusterKerberosDescriptorDefinition() {
    ResourceDefinition resourceDefinition = ResourceInstanceFactoryImpl.getResourceDefinition(
        Resource.Type.ClusterKerberosDescriptor, null);

    assertNotNull(resourceDefinition);
    assertEquals("kerberos_descriptor", resourceDefinition.getSingularName());
    assertEquals("kerberos_descriptors", resourceDefinition.getPluralName());
    assertEquals(Resource.Type.ClusterKerberosDescriptor, resourceDefinition.getType());
  }
}
