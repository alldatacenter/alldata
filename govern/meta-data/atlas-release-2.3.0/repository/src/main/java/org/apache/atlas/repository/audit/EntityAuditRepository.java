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
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.exception.AtlasBaseException;

import java.util.List;
import java.util.Set;

/**
 * Interface for repository for storing entity audit events
 */
public interface EntityAuditRepository {
    /**
     * Add events to the event repository
     * @param events events to be added
     * @throws AtlasException
     */
    void putEventsV1(EntityAuditEvent... events) throws AtlasException;

    /**
     * Add events to the event repository
     * @param events events to be added
     * @throws AtlasException
     */
    void putEventsV1(List<EntityAuditEvent> events) throws AtlasException;

    /**
     * List events for the given entity id in decreasing order of timestamp, from the given timestamp. Returns n results
     * @param entityId entity id
     * @param startKey key for the first event to be returned, used for pagination
     * @param n number of events to be returned
     * @return list of events
     * @throws AtlasException
     */
    List<EntityAuditEvent> listEventsV1(String entityId, String startKey, short n) throws AtlasException;

    /**
     * Add v2 events to the event repository
     * @param events events to be added
     * @throws AtlasBaseException
     */
    void putEventsV2(EntityAuditEventV2... events) throws AtlasBaseException;

    /**
     * Add v2 events to the event repository
     * @param events events to be added
     * @throws AtlasBaseException
     */
    void putEventsV2(List<EntityAuditEventV2> events) throws AtlasBaseException;

    /**
     * List events for the given entity id in decreasing order of timestamp, from the given timestamp. Returns n results
     * @param entityId entity id
     * @param auditAction operation to be used for search at HBase column
     * @param startKey key for the first event to be returned, used for pagination
     * @param maxResultCount  Max numbers of events to be returned
     * @return list of events
     * @throws AtlasBaseException
     */
    List<EntityAuditEventV2> listEventsV2(String entityId, EntityAuditEventV2.EntityAuditActionV2 auditAction, String startKey, short maxResultCount) throws AtlasBaseException;

    /**
     * List events for the given entity id in sorted order of given column. Returns n results
     * @param entityId entity id
     * @param auditAction operation to be used for search at HBase column
     * @param sortByColumn name of column on which sorting is required
     * @param sortOrderDesc flag to set sort order descending
     * @param offset event list is truncated by removing offset number of items, used for pagination
     * @param limit  Max numbers of events to be returned
     * @return list of events
     * @throws AtlasBaseException
     */
    List<EntityAuditEventV2> listEventsV2(String entityId, EntityAuditEventV2.EntityAuditActionV2 auditAction, String sortByColumn, boolean sortOrderDesc, int offset, short limit) throws AtlasBaseException;

    /***
     * List events for given time range where classifications have been added, deleted or updated.
     * @param fromTimestamp from timestamp
     * @param toTimestamp to timestamp
     * @return events that match the range
     * @throws AtlasBaseException
     */
    Set<String> getEntitiesWithTagChanges(long fromTimestamp, long toTimestamp) throws AtlasBaseException;

    /**
     * List events for the given entity id in decreasing order of timestamp, from the given timestamp. Returns n results
     * @param entityId entity id
     * @param startKey key for the first event to be returned, used for pagination
     * @param n        number of events to be returned
     * @return list of events
     * @throws AtlasBaseException
     */
    List<Object> listEvents(String entityId, String startKey, short n) throws AtlasBaseException;

    /**
     * Returns maximum allowed repository size per EntityAuditEvent
     * @throws AtlasException
     */
    long repositoryMaxSize();

    /**
     * list of attributes to be excluded when storing in audit repo.
     * @param entityType type of entity
     * @return list of attribute names to be excluded
     * @throws AtlasException
     */
    List<String> getAuditExcludeAttributes(String entityType);
}
