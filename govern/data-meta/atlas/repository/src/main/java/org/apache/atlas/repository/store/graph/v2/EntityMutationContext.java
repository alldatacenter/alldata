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
package org.apache.atlas.repository.store.graph.v2;

import org.apache.atlas.model.instance.AtlasEntity;

import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.EntityGraphDiscoveryContext;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class EntityMutationContext {
    private final EntityGraphDiscoveryContext  context;
    private final List<AtlasEntity>            entitiesCreated  = new ArrayList<>();
    private final List<AtlasEntity>            entitiesUpdated  = new ArrayList<>();
    private final Map<String, AtlasEntityType> entityVsType     = new HashMap<>();
    private final Map<String, AtlasVertex>     entityVsVertex   = new HashMap<>();
    private final Map<String, String>          guidAssignments  = new HashMap<>();
    private       List<AtlasVertex>            entitiesToDelete = null;

    public EntityMutationContext(final EntityGraphDiscoveryContext context) {
        this.context = context;
    }

    public EntityMutationContext() {
        this.context = null;
    }

    public void addCreated(String internalGuid, AtlasEntity entity, AtlasEntityType type, AtlasVertex atlasVertex) {
        entitiesCreated.add(entity);
        entityVsType.put(entity.getGuid(), type);
        entityVsVertex.put(entity.getGuid(), atlasVertex);

        if (!StringUtils.equals(internalGuid, entity.getGuid())) {
            guidAssignments.put(internalGuid, entity.getGuid());
            entityVsVertex.put(internalGuid, atlasVertex);
        }
    }

    public void addUpdated(String internalGuid, AtlasEntity entity, AtlasEntityType type, AtlasVertex atlasVertex) {
        if (!entityVsVertex.containsKey(internalGuid)) { // if the entity was already created/updated
            entitiesUpdated.add(entity);
            entityVsType.put(entity.getGuid(), type);
            entityVsVertex.put(entity.getGuid(), atlasVertex);

            if (!StringUtils.equals(internalGuid, entity.getGuid())) {
                guidAssignments.put(internalGuid, entity.getGuid());
                entityVsVertex.put(internalGuid, atlasVertex);
            }
        }
    }

    public void addEntityToDelete(AtlasVertex vertex) {
        if (entitiesToDelete == null) {
            entitiesToDelete = new ArrayList<>();
        }

        entitiesToDelete.add(vertex);
    }

    public void cacheEntity(String guid, AtlasVertex vertex, AtlasEntityType entityType) {
        entityVsType.put(guid, entityType);
        entityVsVertex.put(guid, vertex);
    }

    public EntityGraphDiscoveryContext getDiscoveryContext() {
        return this.context;
    }

    public Collection<AtlasEntity> getCreatedEntities() {
        return entitiesCreated;
    }

    public Collection<AtlasEntity> getUpdatedEntities() {
        return entitiesUpdated;
    }

    public Map<String, String> getGuidAssignments() {
        return guidAssignments;
    }

    public List<AtlasVertex> getEntitiesToDelete() {
        return entitiesToDelete;
    }

    public AtlasEntityType getType(String guid) {
        return entityVsType.get(guid);
    }

    public AtlasVertex getVertex(String guid) { return entityVsVertex.get(guid); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final EntityMutationContext that = (EntityMutationContext) o;

        return Objects.equals(context, that.context) &&
               Objects.equals(entitiesCreated, that.entitiesCreated) &&
               Objects.equals(entitiesUpdated, that.entitiesUpdated) &&
               Objects.equals(entityVsType, that.entityVsType) &&
               Objects.equals(entityVsVertex, that.entityVsVertex);
    }

    @Override
    public int hashCode() {
        int result = (context != null ? context.hashCode() : 0);
        result = 31 * result + entitiesCreated.hashCode();
        result = 31 * result + entitiesUpdated.hashCode();
        result = 31 * result + entityVsType.hashCode();
        result = 31 * result + entityVsVertex.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EntityMutationContext{" +
            "context=" + context +
            ", entitiesCreated=" + entitiesCreated +
            ", entitiesUpdated=" + entitiesUpdated +
            ", entityVsType=" + entityVsType +
            ", entityVsVertex=" + entityVsVertex +
            '}';
    }

    public AtlasEntity getCreatedEntity(String parentGuid) {
        return getFromCollection(parentGuid, getCreatedEntities());
    }

    public AtlasEntity getUpdatedEntity(String parentGuid) {
        return getFromCollection(parentGuid, getUpdatedEntities());
    }

    public boolean isDeletedEntity(AtlasVertex vertex) {
        return entitiesToDelete != null && entitiesToDelete.contains(vertex);
    }

    private AtlasEntity getFromCollection(String parentGuid, Collection<AtlasEntity> coll) {
        for (AtlasEntity e : coll) {
            if(e.getGuid().equalsIgnoreCase(parentGuid)) {
                return e;
            }
        }

        return null;
    }

    public AtlasEntity getCreatedOrUpdatedEntity(String parentGuid) {
        AtlasEntity e = getCreatedEntity(parentGuid);
        if(e == null) {
            return getUpdatedEntity(parentGuid);
        }

        return e;
    }
}
