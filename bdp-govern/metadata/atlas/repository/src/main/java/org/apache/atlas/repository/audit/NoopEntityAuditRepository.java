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
import org.apache.atlas.annotation.ConditionalOnAtlasProperty;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Implementation that completely disables the audit repository.
 */
@Singleton
@Component
@ConditionalOnAtlasProperty(property = "atlas.EntityAuditRepository.impl")
public class NoopEntityAuditRepository implements EntityAuditRepository {

    @Override
    public void putEventsV1(EntityAuditEvent... events) {
        //do nothing
    }

    @Override
    public synchronized void putEventsV1(List<EntityAuditEvent> events) {
        //do nothing
    }

    @Override
    public List<EntityAuditEvent> listEventsV1(String entityId, String startKey, short maxResults) {
        return Collections.emptyList();
    }

    @Override
    public void putEventsV2(EntityAuditEventV2... events) {
        //do nothing
    }

    @Override
    public void putEventsV2(List<EntityAuditEventV2> events) {
        //do nothing
    }

    @Override
    public List<EntityAuditEventV2> listEventsV2(String entityId, EntityAuditEventV2.EntityAuditActionV2 auditAction, String sortByColumn, boolean sortOrderDesc, int offset, short limit) throws AtlasBaseException {
        return Collections.emptyList();
    }

    @Override
    public List<EntityAuditEventV2> listEventsV2(String entityId, EntityAuditEventV2.EntityAuditActionV2 auditAction, String startKey, short maxResultCount) {
        return Collections.emptyList();
    }

    @Override
    public Set<String> getEntitiesWithTagChanges(long fromTimestamp, long toTimestamp) throws AtlasBaseException {
        return Collections.emptySet();
    }

    @Override
    public List<Object> listEvents(String entityId, String startKey, short n) {
        return Collections.emptyList();
    }

    @Override
    public long repositoryMaxSize() {
        return -1;
    }

    @Override
    public List<String> getAuditExcludeAttributes(String entityType) {
        return null;
    }
}