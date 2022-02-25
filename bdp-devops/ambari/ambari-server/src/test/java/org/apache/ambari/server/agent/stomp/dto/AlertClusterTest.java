/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.agent.stomp.dto;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.events.AlertDefinitionEventType;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.junit.Test;

public class AlertClusterTest {
  private static final int DEFAULT_INTERVAL = 1;
  private static final int CHANGED_INTERVAL = 999;
  private AlertCluster alertCluster;

  @Test
  public void testAddingNewAlertDefWithoutChangingExisting() throws Exception {
    // Given
    AlertDefinition existing1 = anAlertDefinition(1l);
    AlertDefinition existing2 = anAlertDefinition(2l);
    alertCluster = newAlertCluster(existing1, existing2);
    // When
    AlertDefinition newDef = anAlertDefinition(3l);
    AlertCluster result = update(alertCluster, newDef);
    // Then
    assertHasAlerts(result, existing1, existing2, newDef);
  }

  @Test
  public void testChangingContentOfExistingAlertDef() throws Exception {
    // Given
    AlertDefinition existing1 = anAlertDefinition(1l);
    AlertDefinition existing2 = anAlertDefinition(2l);
    alertCluster = newAlertCluster(existing1, existing2);
    // When
    AlertDefinition changed = anAlertDefinition(2, CHANGED_INTERVAL);
    AlertCluster result = update(alertCluster, changed);
    // Then
    assertHasAlerts(result, existing1, changed);
  }

  @Test
  public void testAddingNewAlertDefAndChangingExisting() throws Exception {
    // Given
    AlertDefinition existing1 = anAlertDefinition(1l);
    AlertDefinition existing2 = anAlertDefinition(2l);
    alertCluster = newAlertCluster(existing1, existing2);
    // When
    AlertDefinition newDef = anAlertDefinition(3l);
    AlertDefinition changed = anAlertDefinition(2, 999);
    AlertCluster result = update(alertCluster, newDef, changed);
    // Then
    assertHasAlerts(result, existing1, changed, newDef);
  }

  @Test
  public void testNoChange() throws Exception {
    // Given
    AlertDefinition existing = anAlertDefinition(1l);
    alertCluster = newAlertCluster(existing);
    // When
    AlertCluster result = update(alertCluster, existing);
    // Then
    assertThat(result, is(nullValue()));
  }

  private void assertHasAlerts(AlertCluster result, AlertDefinition... expectedItems) {
    assertNotNull(result);
    assertThat(result.getAlertDefinitions(), hasSize(expectedItems.length));
    for (AlertDefinition expected : expectedItems) {
      assertTrue(result.getAlertDefinitions() + " should have contained: " + expected,
        result.getAlertDefinitions().stream().anyMatch(each -> each.deeplyEquals(expected)));
    }
  }

  private AlertDefinition anAlertDefinition(long id) {
    return anAlertDefinition(id, DEFAULT_INTERVAL);
  }

  private AlertDefinition anAlertDefinition(long id, int interval) {
    AlertDefinition alertDefinition = new AlertDefinition();
    alertDefinition.setDefinitionId(id);
    alertDefinition.setName(Long.toString(id));
    alertDefinition.setInterval(interval);
    return alertDefinition;
  }

  private AlertCluster newAlertCluster(AlertDefinition... existingDefinitions) {
    return new AlertCluster(asMap(existingDefinitions), "host");
  }

  private Map<Long,AlertDefinition> asMap(AlertDefinition... alertDefinition) {
    Map<Long, AlertDefinition> alerts = new HashMap<>();
    for (AlertDefinition each : alertDefinition) {
      alerts.put(each.getDefinitionId(), each);
    }
    return alerts;
  }

  private AlertCluster update(AlertCluster alertCluster, AlertDefinition... alertDefinitions) {
    return alertCluster.handleUpdate(AlertDefinitionEventType.UPDATE, newAlertCluster(alertDefinitions));
  }
}