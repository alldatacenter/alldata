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
package org.apache.ambari.server.agent.stomp;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.AlertCluster;
import org.apache.ambari.server.events.AlertDefinitionEventType;
import org.apache.ambari.server.events.AlertDefinitionsAgentUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.junit.Test;

public class AlertDefinitionsHolderTest {
  private final Long HOST_ID = 1L;

  @Test
  public void testHandleUpdateEmptyCurrent() throws AmbariException {
    AlertDefinitionsAgentUpdateEvent current = new AlertDefinitionsAgentUpdateEvent(AlertDefinitionEventType.CREATE,
        Collections.emptyMap(), "host1", HOST_ID);
    Map<Long, AlertCluster> clusters = new HashMap<>();
    AlertCluster cluster = AlertCluster.emptyAlertCluster();
    clusters.put(1L, cluster);
    AlertDefinitionsAgentUpdateEvent update = new AlertDefinitionsAgentUpdateEvent(AlertDefinitionEventType.UPDATE,
        clusters, "host1", HOST_ID);

    AlertDefinitionsHolder alertDefinitionsHolder = new AlertDefinitionsHolder(createNiceMock(AmbariEventPublisher.class));
    AlertDefinitionsAgentUpdateEvent result = alertDefinitionsHolder.handleUpdate(current, update);

    assertFalse(result == update);
    assertFalse(result == current);
    assertEquals(AlertDefinitionEventType.CREATE, result.getEventType());
    assertEquals(result.getClusters(), update.getClusters());
  }

  @Test
  public void testHandleUpdateEmptyUpdate() throws AmbariException {
    Map<Long, AlertCluster> clusters = new HashMap<>();
    AlertCluster cluster = AlertCluster.emptyAlertCluster();
    clusters.put(1L, cluster);
    AlertDefinitionsAgentUpdateEvent current = new AlertDefinitionsAgentUpdateEvent(AlertDefinitionEventType.CREATE,
        clusters, "host1", HOST_ID);
    AlertDefinitionsAgentUpdateEvent update = new AlertDefinitionsAgentUpdateEvent(AlertDefinitionEventType.UPDATE,
        Collections.emptyMap(), "host1", HOST_ID);

    AlertDefinitionsHolder alertDefinitionsHolder = new AlertDefinitionsHolder(createNiceMock(AmbariEventPublisher.class));
    AlertDefinitionsAgentUpdateEvent result = alertDefinitionsHolder.handleUpdate(current, update);

    assertFalse(result == update);
    assertFalse(result == current);
    assertEquals(result, null);
  }

  @Test
  public void testHandleUpdateNoChanges() throws AmbariException {
    Map<Long, AlertCluster> currentClusters = new HashMap<>();
    AlertCluster currentCluster = new AlertCluster(Collections.emptyMap(), "host1");
    currentClusters.put(1L, currentCluster);
    AlertDefinitionsAgentUpdateEvent current = new AlertDefinitionsAgentUpdateEvent(AlertDefinitionEventType.CREATE,
        currentClusters, "host1", HOST_ID);

    Map<Long, AlertCluster> updateClusters = new HashMap<>();
    AlertCluster updateCluster = new AlertCluster(Collections.emptyMap(), "host1");
    updateClusters.put(1L, updateCluster);
    AlertDefinitionsAgentUpdateEvent update = new AlertDefinitionsAgentUpdateEvent(AlertDefinitionEventType.UPDATE,
        updateClusters, "host1", HOST_ID);

    AlertDefinitionsHolder alertDefinitionsHolder = new AlertDefinitionsHolder(createNiceMock(AmbariEventPublisher.class));
    AlertDefinitionsAgentUpdateEvent result = alertDefinitionsHolder.handleUpdate(current, update);

    assertFalse(result == update);
    assertFalse(result == current);
    assertEquals(result, null);
  }

  @Test
  public void testHandleUpdateOnChanges() throws AmbariException {
    Map<Long, AlertCluster> currentClusters = new HashMap<>();
    AlertCluster currentCluster = new AlertCluster(Collections.emptyMap(), "host1");
    currentClusters.put(1L, currentCluster);
    AlertDefinitionsAgentUpdateEvent current = new AlertDefinitionsAgentUpdateEvent(AlertDefinitionEventType.CREATE,
        currentClusters, "host1", HOST_ID);

    Map<Long, AlertCluster> updateClusters = new HashMap<>();
    AlertCluster updateCluster = new AlertCluster(Collections.emptyMap(), "host1");
    updateClusters.put(2L, updateCluster);
    AlertDefinitionsAgentUpdateEvent update = new AlertDefinitionsAgentUpdateEvent(AlertDefinitionEventType.UPDATE,
        updateClusters, "host1", HOST_ID);

    AlertDefinitionsHolder alertDefinitionsHolder = new AlertDefinitionsHolder(createNiceMock(AmbariEventPublisher.class));
    AlertDefinitionsAgentUpdateEvent result = alertDefinitionsHolder.handleUpdate(current, update);

    assertFalse(result == update);
    assertFalse(result == current);
    assertEquals(2, result.getClusters().size());
    assertTrue(result.getClusters().containsKey(1L));
    assertTrue(result.getClusters().containsKey(2L));
  }
}
