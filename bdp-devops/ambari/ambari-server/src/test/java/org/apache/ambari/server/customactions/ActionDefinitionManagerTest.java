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

package org.apache.ambari.server.customactions;

import java.io.File;
import java.util.EnumSet;

import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.junit.Test;

import junit.framework.Assert;

public class ActionDefinitionManagerTest {

  private static final String CUSTOM_ACTION_DEFINITION_ROOT = "./src/test/resources/custom_action_definitions/";
  private static final String CUSTOM_ACTION_DEFINITION_INVALID_ROOT = "./src/test/resources/custom_action_definitions_invalid/";

  @Test
  public void testReadCustomActionDefinitions() throws Exception {
    ActionDefinitionManager manager = new ActionDefinitionManager();
    manager.readCustomActionDefinitions(new File(CUSTOM_ACTION_DEFINITION_ROOT));

    Assert.assertEquals(3, manager.getAllActionDefinition().size());
    ActionDefinition ad = manager.getActionDefinition("customAction1");
    Assert.assertNotNull(ad);
    Assert.assertEquals("customAction1", ad.getActionName());
    Assert.assertEquals("A random test", ad.getDescription());
    Assert.assertEquals("threshold", ad.getInputs());
    Assert.assertEquals("TASKTRACKER", ad.getTargetComponent());
    Assert.assertEquals("MAPREDUCE", ad.getTargetService());
    Assert.assertEquals(60, (int)ad.getDefaultTimeout());
    Assert.assertEquals(TargetHostType.ALL, ad.getTargetType());
    Assert.assertEquals(ActionType.USER, ad.getActionType());
    Assert.assertEquals(EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_COMPONENTS, RoleAuthorization.HOST_ADD_DELETE_HOSTS), ad.getPermissions());

    ad = manager.getActionDefinition("customAction2");
    Assert.assertNotNull(ad);
    Assert.assertEquals("customAction2", ad.getActionName());
    Assert.assertEquals("A random test", ad.getDescription());
    Assert.assertEquals(null, ad.getInputs());
    Assert.assertEquals("TASKTRACKER", ad.getTargetComponent());
    Assert.assertEquals("MAPREDUCE", ad.getTargetService());
    Assert.assertEquals(60, (int)ad.getDefaultTimeout());
    Assert.assertEquals(null, ad.getTargetType());
    Assert.assertEquals(ActionType.USER, ad.getActionType());
    Assert.assertEquals(EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_COMPONENTS, RoleAuthorization.HOST_ADD_DELETE_HOSTS), ad.getPermissions());

    ad = manager.getActionDefinition("customAction3");
    Assert.assertNotNull(ad);
    Assert.assertEquals("customAction3", ad.getActionName());
    Assert.assertEquals("A random test", ad.getDescription());
    Assert.assertEquals(null, ad.getInputs());
    Assert.assertEquals("TASKTRACKER", ad.getTargetComponent());
    Assert.assertEquals("MAPREDUCE", ad.getTargetService());
    Assert.assertEquals(60, (int)ad.getDefaultTimeout());
    Assert.assertEquals(null, ad.getTargetType());
    Assert.assertEquals(ActionType.USER, ad.getActionType());
    Assert.assertNull(ad.getPermissions());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReadInvalidCustomActionDefinitions() throws Exception {
    ActionDefinitionManager manager = new ActionDefinitionManager();
    manager.readCustomActionDefinitions(new File(CUSTOM_ACTION_DEFINITION_INVALID_ROOT));
  }
}

