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

import org.apache.ambari.server.state.AlertState;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests methods on {@link AlertHistoryEntity}.
 */
public class AlertHistoryEntityTest {

  /**
   * Tests {@link AlertHistoryEntity#hashCode()} and {@link AlertHistoryEntity#equals(Object)}
   */
  @Test
  public void testHashCodeAndEquals(){
    AlertDefinitionEntity definition1 = new AlertDefinitionEntity();
    definition1.setDefinitionId(1L);
    AlertDefinitionEntity definition2 = new AlertDefinitionEntity();
    definition2.setDefinitionId(2L);

    AlertHistoryEntity history1 = new AlertHistoryEntity();
    AlertHistoryEntity history2 = new AlertHistoryEntity();

    Assert.assertEquals(history1.hashCode(), history2.hashCode());
    Assert.assertTrue(Objects.equals(history1, history2));

    history1.setAlertDefinition(definition1);
    history2.setAlertDefinition(definition2);
    Assert.assertNotSame(history1.hashCode(), history2.hashCode());
    Assert.assertFalse(Objects.equals(history1, history2));

    history2.setAlertDefinition(definition1);
    Assert.assertEquals(history1.hashCode(), history2.hashCode());
    Assert.assertTrue(Objects.equals(history1, history2));

    history1.setAlertLabel("label");
    history1.setAlertState(AlertState.OK);
    history1.setAlertText("text");
    history1.setAlertTimestamp(1L);
    history1.setClusterId(1L);
    history1.setComponentName("component");
    history1.setServiceName("service");
    history1.setHostName("host");
    history2.setAlertLabel("label");
    history2.setAlertState(AlertState.OK);
    history2.setAlertText("text");
    history2.setAlertTimestamp(1L);
    history2.setClusterId(1L);
    history2.setComponentName("component");
    history2.setServiceName("service");
    history2.setHostName("host");
    Assert.assertEquals(history1.hashCode(), history2.hashCode());
    Assert.assertTrue(Objects.equals(history1, history2));

    history2.setAlertState(AlertState.CRITICAL);
    Assert.assertNotSame(history1.hashCode(), history2.hashCode());
    Assert.assertFalse(Objects.equals(history1, history2));

    history2.setAlertState(AlertState.OK);
    history1.setAlertId(1L);
    history2.setAlertId(1L);
    Assert.assertEquals(history1.hashCode(), history2.hashCode());
    Assert.assertTrue(Objects.equals(history1, history2));

    history2.setAlertId(2L);
    Assert.assertNotSame(history1.hashCode(), history2.hashCode());
    Assert.assertFalse(Objects.equals(history1, history2));
  }
}
