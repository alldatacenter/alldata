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

import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.MetadataUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.commons.collections.MapUtils;
import org.junit.Test;

public class AgentDataHolderTest {

  @Test
  public void testGetHashWithTimestamp() {
    AmbariEventPublisher ambariEventPublisher = createNiceMock(AmbariEventPublisher.class);
    AgentConfigsHolder agentConfigsHolder = new AgentConfigsHolder(ambariEventPublisher, Encryptor.NONE);

    AgentConfigsUpdateEvent event1 = new AgentConfigsUpdateEvent(null, null);
    event1.setHash("01");
    event1.setTimestamp(1L);
    String eventHash1 = agentConfigsHolder.getHash(event1);

    // difference in hash only
    AgentConfigsUpdateEvent event2 = new AgentConfigsUpdateEvent(null, null);
    event2.setHash("02");
    event2.setTimestamp(1L);
    String eventHash2 = agentConfigsHolder.getHash(event2);

    // difference in timestamp only
    AgentConfigsUpdateEvent event3 = new AgentConfigsUpdateEvent(null, null);
    event3.setHash("01");
    event3.setTimestamp(2L);
    String eventHash3 = agentConfigsHolder.getHash(event3);

    // difference in both hash and timestamp
    AgentConfigsUpdateEvent event4 = new AgentConfigsUpdateEvent(null, null);
    event4.setHash("02");
    event4.setTimestamp(2L);
    String eventHash4 = agentConfigsHolder.getHash(event4);

    // hash and timestamp are the same, changes in body
    AgentConfigsUpdateEvent event5 = new AgentConfigsUpdateEvent(null, MapUtils.EMPTY_SORTED_MAP);
    event5.setHash("01");
    event5.setTimestamp(1L);
    String eventHash5 = agentConfigsHolder.getHash(event5);

    assertEquals(eventHash1, eventHash2);
    assertEquals(eventHash1, eventHash3);
    assertEquals(eventHash1, eventHash4);
    assertFalse(eventHash1.equals(eventHash5));
  }

  @Test
  public void testGetHash() {
    AmbariEventPublisher ambariEventPublisher = createNiceMock(AmbariEventPublisher.class);
    MetadataHolder metadataHolder = new MetadataHolder(ambariEventPublisher);

    MetadataUpdateEvent event1 = new MetadataUpdateEvent(null,
        null,
        null,
        UpdateEventType.CREATE);
    event1.setHash("01");
    String eventHash1 = metadataHolder.getHash(event1);

    // difference in hash only
    MetadataUpdateEvent event2 = new MetadataUpdateEvent(null,
        null,
        null,
        UpdateEventType.CREATE);
    event2.setHash("02");
    String eventHash2 = metadataHolder.getHash(event2);

    // the same hash, but the body was changed
    MetadataUpdateEvent event3 = new MetadataUpdateEvent(null,
        null,
        null,
        UpdateEventType.UPDATE);
    event3.setHash("01");
    String eventHash3 = metadataHolder.getHash(event3);

    assertEquals(eventHash1, eventHash2);
    assertFalse(eventHash1.equals(eventHash3));
  }
}
