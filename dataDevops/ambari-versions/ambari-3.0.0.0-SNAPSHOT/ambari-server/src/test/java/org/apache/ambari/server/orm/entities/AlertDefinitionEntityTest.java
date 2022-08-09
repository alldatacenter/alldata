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
package org.apache.ambari.server.orm.entities;

import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests methods on {@link AlertDefinitionEntity}.
 */
public class AlertDefinitionEntityTest {

  /**
   * Tests {@link AlertDefinitionEntity#hashCode()} and {@link AlertDefinitionEntity#equals(Object)}
   */
  @Test
  public void testHashCodeAndEquals(){
    AlertDefinitionEntity definition1 = new AlertDefinitionEntity();
    AlertDefinitionEntity definition2 = new AlertDefinitionEntity();

    Assert.assertEquals(definition1.hashCode(), definition2.hashCode());
    Assert.assertTrue(Objects.equals(definition1, definition2));

    definition1.setClusterId(1L);
    definition2.setClusterId(1L);
    Assert.assertEquals(definition1.hashCode(), definition2.hashCode());
    Assert.assertTrue(Objects.equals(definition1, definition2));

    definition1.setDefinitionName("definition-name");
    definition2.setDefinitionName("definition-name");
    Assert.assertEquals(definition1.hashCode(), definition2.hashCode());
    Assert.assertTrue(Objects.equals(definition1, definition2));

    definition2.setDefinitionName("definition-name-foo");
    Assert.assertNotSame(definition1.hashCode(), definition2.hashCode());
    Assert.assertFalse(Objects.equals(definition1, definition2));

    definition2.setDefinitionName("definition-name");
    definition2.setClusterId(2L);
    Assert.assertNotSame(definition1.hashCode(), definition2.hashCode());
    Assert.assertFalse(Objects.equals(definition1, definition2));

    definition2.setClusterId(1L);
    definition1.setDefinitionId(1L);
    Assert.assertNotSame(definition1.hashCode(), definition2.hashCode());
    Assert.assertFalse(Objects.equals(definition1, definition2));

    definition2.setDefinitionId(1L);
    Assert.assertEquals(definition1.hashCode(), definition2.hashCode());
    Assert.assertTrue(Objects.equals(definition1, definition2));
  }
}
