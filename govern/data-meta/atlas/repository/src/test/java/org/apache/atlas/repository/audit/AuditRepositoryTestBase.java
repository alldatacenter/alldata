/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.audit;

import org.apache.atlas.EntityAuditEvent;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
public class AuditRepositoryTestBase {
    protected EntityAuditRepository eventRepository;

    private String rand() {
         return TestUtilsV2.randomString(10);
    }

    @BeforeTest
    public void setUp() throws Exception{
        eventRepository = new InMemoryEntityAuditRepository();
    }

    @Test
    public void testAddEventsV1() throws Exception {
        EntityAuditEvent event = new EntityAuditEvent(rand(), System.currentTimeMillis(), "u1",
                EntityAuditEvent.EntityAuditAction.ENTITY_CREATE, "d1", new Referenceable(rand()));

        eventRepository.putEventsV1(event);

        List<EntityAuditEvent> events = eventRepository.listEventsV1(event.getEntityId(), null, (short) 10);

        assertEquals(events.size(), 1);
        assertEventV1Equals(events.get(0), event);
    }

    @Test
    public void testListPaginationV1() throws Exception {
        String                 id1            = "id1" + rand();
        String                 id2            = "id2" + rand();
        String                 id3            = "id3" + rand();
        long                   ts             = System.currentTimeMillis();
        Referenceable          entity         = new Referenceable(rand());
        List<EntityAuditEvent> expectedEvents = new ArrayList<>(3);

        for (int i = 0; i < 3; i++) {
            //Add events for both ids
            EntityAuditEvent event = new EntityAuditEvent(id2, ts - i, "user" + i, EntityAuditEvent.EntityAuditAction.ENTITY_UPDATE, "details" + i, entity);

            eventRepository.putEventsV1(event);
            expectedEvents.add(event);
            eventRepository.putEventsV1(new EntityAuditEvent(id1, ts - i, "user" + i, EntityAuditEvent.EntityAuditAction.TAG_DELETE, "details" + i, entity));
            eventRepository.putEventsV1(new EntityAuditEvent(id3, ts - i, "user" + i, EntityAuditEvent.EntityAuditAction.TAG_ADD, "details" + i, entity));
        }

        //Use ts for which there is no event - ts + 2
        List<EntityAuditEvent> events = eventRepository.listEventsV1(id2, null, (short) 3);
        assertEquals(events.size(), 3);
        assertEventV1Equals(events.get(0), expectedEvents.get(0));
        assertEventV1Equals(events.get(1), expectedEvents.get(1));
        assertEventV1Equals(events.get(2), expectedEvents.get(2));

        //Use last event's timestamp for next list(). Should give only 1 event and shouldn't include events from other id
        events = eventRepository.listEventsV1(id2, events.get(2).getEventKey(), (short) 3);
        assertEquals(events.size(), 1);
        assertEventV1Equals(events.get(0), expectedEvents.get(2));
    }

    @Test
    public void testInvalidEntityIdV1() throws Exception {
        List<EntityAuditEvent> events = eventRepository.listEventsV1(rand(), null, (short) 3);

        assertEquals(events.size(), 0);
    }

    protected void assertEventV1Equals(EntityAuditEvent actual, EntityAuditEvent expected) {
        if (expected != null) {
            assertNotNull(actual);
        }

        assertEquals(actual.getEntityId(), expected.getEntityId());
        assertEquals(actual.getAction(), expected.getAction());
        assertEquals(actual.getTimestamp(), expected.getTimestamp());
        assertEquals(actual.getDetails(), expected.getDetails());
    }


    @Test
    public void testAddEventsV2() throws Exception {
        EntityAuditEventV2 event = new EntityAuditEventV2(rand(), System.currentTimeMillis(), "u1",
            EntityAuditEventV2.EntityAuditActionV2.ENTITY_CREATE, "d1", new AtlasEntity(rand()));

        eventRepository.putEventsV2(event);

        List<EntityAuditEventV2> events = eventRepository.listEventsV2(event.getEntityId(), null, null, (short) 10);

        assertEquals(events.size(), 1);
        assertEventV2Equals(events.get(0), event);
    }

    @Test
    public void testListPaginationV2() throws Exception {
        String                 id1            = "id1" + rand();
        String                 id2            = "id2" + rand();
        String                 id3            = "id3" + rand();
        long                   ts             = System.currentTimeMillis();
        AtlasEntity          entity         = new AtlasEntity(rand());
        List<EntityAuditEventV2> expectedEvents = new ArrayList<>(3);

        for (int i = 0; i < 3; i++) {
            //Add events for both ids
            EntityAuditEventV2 event = new EntityAuditEventV2(id2, ts - i, "user" + i, EntityAuditEventV2.EntityAuditActionV2.ENTITY_UPDATE, "details" + i, entity);

            eventRepository.putEventsV2(event);
            expectedEvents.add(event);
            eventRepository.putEventsV2(new EntityAuditEventV2(id1, ts - i, "user" + i, EntityAuditEventV2.EntityAuditActionV2.ENTITY_DELETE, "details" + i, entity));
            eventRepository.putEventsV2(new EntityAuditEventV2(id3, ts - i, "user" + i, EntityAuditEventV2.EntityAuditActionV2.ENTITY_CREATE, "details" + i, entity));
        }

        //Use ts for which there is no event - ts + 2
        List<EntityAuditEventV2> events = eventRepository.listEventsV2(id2, null, null, (short) 3);
        assertEquals(events.size(), 3);
        assertEventV2Equals(events.get(0), expectedEvents.get(0));
        assertEventV2Equals(events.get(1), expectedEvents.get(1));
        assertEventV2Equals(events.get(2), expectedEvents.get(2));

        //Use last event's timestamp for next list(). Should give only 1 event and shouldn't include events from other id
        events = eventRepository.listEventsV2(id2, null, events.get(2).getEventKey(), (short) 3);
        assertEquals(events.size(), 1);
        assertEventV2Equals(events.get(0), expectedEvents.get(2));
    }

    @Test
    public void testInvalidEntityIdV2() throws Exception {
        List<EntityAuditEvent> events = eventRepository.listEventsV1(rand(), null, (short) 3);

        assertEquals(events.size(), 0);
    }
    
    @Test
    public void testSortListV2() throws Exception {
        String                 id1            = "id1" + rand();
        long                   ts             = System.currentTimeMillis();
        AtlasEntity          entity         = new AtlasEntity(rand());
        List<EntityAuditEventV2> expectedEvents = new ArrayList<>(3);

        expectedEvents.add(new EntityAuditEventV2(id1, ts, "user-a", EntityAuditEventV2.EntityAuditActionV2.ENTITY_UPDATE, "details-1", entity));
        expectedEvents.add(new EntityAuditEventV2(id1, ts+1, "user-C", EntityAuditEventV2.EntityAuditActionV2.ENTITY_DELETE, "details-2", entity));
        expectedEvents.add(new EntityAuditEventV2(id1, ts+2, "User-b", EntityAuditEventV2.EntityAuditActionV2.ENTITY_CREATE, "details-3", entity));
        for(EntityAuditEventV2 event : expectedEvents) {
            eventRepository.putEventsV2(event);
        }

        List<EntityAuditEventV2> events = eventRepository.listEventsV2(id1, null, "timestamp", false, 0, (short) 2);
        assertEquals(events.size(), 2);
        assertEventV2Equals(events.get(0), expectedEvents.get(0));
        assertEventV2Equals(events.get(1), expectedEvents.get(1));

        events = eventRepository.listEventsV2(id1, null, "user", false, 0, (short) 3);
        assertEquals(events.size(), 3);
        assertEventV2Equals(events.get(0), expectedEvents.get(0));
        assertEventV2Equals(events.get(1), expectedEvents.get(2));
        assertEventV2Equals(events.get(2), expectedEvents.get(1));

        events = eventRepository.listEventsV2(id1, null, "action", false, 0, (short) 3);
        assertEquals(events.size(), 3);
        assertEventV2Equals(events.get(0), expectedEvents.get(2));
        assertEventV2Equals(events.get(1), expectedEvents.get(1));
        assertEventV2Equals(events.get(2), expectedEvents.get(0));

    }

    protected void assertEventV2Equals(EntityAuditEventV2 actual, EntityAuditEventV2 expected) {
        if (expected != null) {
            assertNotNull(actual);
        }

        assertEquals(actual.getEntityId(), expected.getEntityId());
        assertEquals(actual.getAction(), expected.getAction());
        assertEquals(actual.getTimestamp(), expected.getTimestamp());
        assertEquals(actual.getDetails(), expected.getDetails());
    }

}
