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

import org.apache.atlas.AtlasException;
import org.apache.atlas.EntityAuditEvent;
import org.apache.atlas.annotation.ConditionalOnAtlasProperty;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Entity audit repository where audit events are stored in-memory. Used only for integration tests
 */
@Singleton
@Component
@ConditionalOnAtlasProperty(property = "atlas.EntityAuditRepository.impl")
public class InMemoryEntityAuditRepository implements EntityAuditRepository {
    private TreeMap<String, EntityAuditEvent>   auditEvents   = new TreeMap<>();
    private TreeMap<String, EntityAuditEventV2> auditEventsV2 = new TreeMap<>();

    @Override
    public void putEventsV1(EntityAuditEvent... events) throws AtlasException {
        putEventsV1(Arrays.asList(events));
    }

    @Override
    public synchronized void putEventsV1(List<EntityAuditEvent> events) throws AtlasException {
        for (EntityAuditEvent event : events) {
            String rowKey = event.getEntityId() + (Long.MAX_VALUE - event.getTimestamp());
            event.setEventKey(rowKey);
            auditEvents.put(rowKey, event);
        }
    }

    //synchronized to avoid concurrent modification exception that occurs if events are added
    //while we are iterating through the map
    @Override
    public synchronized List<EntityAuditEvent> listEventsV1(String entityId, String startKey, short maxResults) {
        List<EntityAuditEvent> events = new ArrayList<>();
        String myStartKey = startKey;
        if (myStartKey == null) {
            myStartKey = entityId;
        }
        SortedMap<String, EntityAuditEvent> subMap = auditEvents.tailMap(myStartKey);
        for (EntityAuditEvent event : subMap.values()) {
            if (events.size() < maxResults && event.getEntityId().equals(entityId)) {
                events.add(event);
            }
        }
        return events;
    }

    @Override
    public long repositoryMaxSize() {
        return -1;
    }

    @Override
    public List<String> getAuditExcludeAttributes(String entityType) {
        return null;
    }

    @Override
    public void putEventsV2(EntityAuditEventV2... events) {
        putEventsV2(Arrays.asList(events));
    }

    @Override
    public void putEventsV2(List<EntityAuditEventV2> events) {
        for (EntityAuditEventV2 event : events) {
            String rowKey = event.getEntityId() + (Long.MAX_VALUE - event.getTimestamp());
            event.setEventKey(rowKey);
            auditEventsV2.put(rowKey, event);
        }
    }

    @Override
    public List<EntityAuditEventV2> listEventsV2(String entityId, EntityAuditEventV2.EntityAuditActionV2 auditAction, String sortByColumn, boolean sortOrderDesc, int offset, short limit) throws AtlasBaseException {
        List<EntityAuditEventV2> events     = new ArrayList<>();
        SortedMap<String, EntityAuditEventV2> subMap = auditEventsV2.tailMap(entityId);
        for (EntityAuditEventV2 event : subMap.values()) {
            if (event.getEntityId().equals(entityId)) {
                events.add(event);
            }
        }
        EntityAuditEventV2.sortEvents(events, sortByColumn, sortOrderDesc);
        events = events.subList(
                Math.min(events.size(), offset),
                Math.min(events.size(), offset + limit));
        return events;
    }

    @Override
    public List<EntityAuditEventV2> listEventsV2(String entityId, EntityAuditEventV2.EntityAuditActionV2 auditAction, String startKey, short maxResults) {
        List<EntityAuditEventV2> events     = new ArrayList<>();
        String                   myStartKey = startKey;

        if (myStartKey == null) {
            myStartKey = entityId;
        }

        SortedMap<String, EntityAuditEventV2> subMap = auditEventsV2.tailMap(myStartKey);

        for (EntityAuditEventV2 event : subMap.values()) {
            if (events.size() < maxResults && event.getEntityId().equals(entityId)) {
                events.add(event);
            }
        }

        return events;
    }

    @Override
    public Set<String> getEntitiesWithTagChanges(long fromTimestamp, long toTimestamp) throws AtlasBaseException {
        Set<String> events = new HashSet<>();

        for (EntityAuditEventV2 event : auditEventsV2.values()) {
            long timestamp = event.getTimestamp();
            if (timestamp > fromTimestamp && timestamp <= toTimestamp) {
                events.add(event.getEntityId());
            }
        }

        return events;
    }

    @Override
    public List<Object> listEvents(String entityId, String startKey, short maxResults) {
        List events = listEventsV2(entityId, null, startKey, maxResults);

        if (CollectionUtils.isEmpty(events)) {
            events = listEventsV1(entityId, startKey, maxResults);
        }

        return events;
    }
}
