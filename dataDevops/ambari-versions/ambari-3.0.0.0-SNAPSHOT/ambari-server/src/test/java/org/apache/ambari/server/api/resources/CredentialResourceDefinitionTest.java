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

import java.util.Collection;

import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Assert;
import org.junit.Test;

/**
 * CredentialResourceDefinitionTest tests.
 */
public class CredentialResourceDefinitionTest {

  @Test
  public void testGetType() throws Exception {
    CredentialResourceDefinition definition = new CredentialResourceDefinition();
    Assert.assertEquals(Resource.Type.Credential, definition.getType());
  }

  @Test
  public void testGetPluralName() throws Exception {
    CredentialResourceDefinition definition = new CredentialResourceDefinition();
    Assert.assertEquals("credentials", definition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    CredentialResourceDefinition definition = new CredentialResourceDefinition();
    Assert.assertEquals("credential", definition.getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() {
    CredentialResourceDefinition definition = new CredentialResourceDefinition();
    Assert.assertTrue(definition.getSubResourceDefinitions().isEmpty());
  }

  @Test
  public void testGetCreateDirectives() {
    CredentialResourceDefinition definition = new CredentialResourceDefinition();
    Collection<String> directives = definition.getCreateDirectives();
    Assert.assertEquals(0, directives.size());
  }
}
