/**
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
package org.apache.atlas.repository.store.graph;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.EntityStream;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EntityGraphDiscoveryContext {
    private static final Logger LOG = LoggerFactory.getLogger(EntityGraphDiscoveryContext.class);

    private final AtlasTypeRegistry               typeRegistry;
    private final EntityStream                    entityStream;
    private final List<String>                    referencedGuids          = new ArrayList<>();
    private final Set<AtlasObjectId>              referencedByUniqAttribs  = new HashSet<>();
    private final Map<String, AtlasVertex>        resolvedGuids            = new HashMap<>();
    private final Map<AtlasObjectId, AtlasVertex> resolvedIdsByUniqAttribs = new HashMap<>();
    private final Set<String>                     localGuids               = new HashSet<>();

    public EntityGraphDiscoveryContext(AtlasTypeRegistry typeRegistry, EntityStream entityStream) {
        this.typeRegistry = typeRegistry;
        this.entityStream = entityStream;
    }

    public EntityStream getEntityStream() {
        return entityStream;
    }

    public List<String> getReferencedGuids() { return referencedGuids; }

    public Set<AtlasObjectId> getReferencedByUniqAttribs() { return referencedByUniqAttribs; }

    public Map<String, AtlasVertex> getResolvedGuids() {
        return resolvedGuids;
    }

    public Map<AtlasObjectId, AtlasVertex> getResolvedIdsByUniqAttribs() {
        return resolvedIdsByUniqAttribs;
    }

    public Set<String> getLocalGuids() { return localGuids; }


    public void addReferencedGuid(String guid) {
        if (! referencedGuids.contains(guid)) {
            referencedGuids.add(guid);
        }
    }

    public void addReferencedByUniqAttribs(AtlasObjectId objId) { referencedByUniqAttribs.add(objId); }


    public void addResolvedGuid(String guid, AtlasVertex vertex) { resolvedGuids.put(guid, vertex); }

    public void addResolvedIdByUniqAttribs(AtlasObjectId objId, AtlasVertex vertex) { resolvedIdsByUniqAttribs.put(objId, vertex); }

    public void addLocalGuidReference(String guid) { localGuids.add(guid); }

    public boolean isResolvedGuid(String guid) { return resolvedGuids.containsKey(guid); }

    public boolean isResolvedIdByUniqAttrib(AtlasObjectId objId) { return resolvedIdsByUniqAttribs.containsKey(objId); }


    public AtlasVertex getResolvedEntityVertex(String guid) throws AtlasBaseException {
        AtlasVertex ret = resolvedGuids.get(guid);

        return ret;
    }

    public AtlasVertex getResolvedEntityVertex(AtlasObjectId objId) {
        if (resolvedIdsByUniqAttribs.containsKey(objId)) {
            return getAtlasVertexFromResolvedIdsByAttribs(objId);
        } else if (objId instanceof AtlasRelatedObjectId) {
            objId = new AtlasObjectId(objId.getGuid(), objId.getTypeName(), objId.getUniqueAttributes());
        }

        return getAtlasVertexFromResolvedIdsByAttribs(objId);
    }

    private AtlasVertex getAtlasVertexFromResolvedIdsByAttribs(AtlasObjectId objId) {
        AtlasVertex vertex = resolvedIdsByUniqAttribs.get(objId);
        // check also for sub-types; ref={typeName=Asset; guid=abcd} should match {typeName=hive_table; guid=abcd}
        if (vertex == null) {
            final AtlasEntityType entityType  = typeRegistry.getEntityTypeByName(objId.getTypeName());
            final Set<String>     allSubTypes = entityType.getAllSubTypes();

            for (String subType : allSubTypes) {
                AtlasObjectId subTypeObjId = new AtlasObjectId(objId.getGuid(), subType, objId.getUniqueAttributes());

                vertex = resolvedIdsByUniqAttribs.get(subTypeObjId);

                if (vertex != null) {
                    resolvedIdsByUniqAttribs.put(objId, vertex);
                    break;
                }
            }
        }

        return vertex;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("EntityGraphDiscoveryCtx{");
        sb.append("referencedGuids=").append(referencedGuids);
        sb.append(", referencedByUniqAttribs=").append(referencedByUniqAttribs);
        sb.append(", resolvedGuids='").append(resolvedGuids);
        sb.append(", resolvedIdsByUniqAttribs='").append(resolvedIdsByUniqAttribs);
        sb.append(", localGuids='").append(localGuids);
        sb.append('}');

        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public void cleanUp() {
        referencedGuids.clear();
        referencedByUniqAttribs.clear();
        resolvedGuids.clear();
        resolvedIdsByUniqAttribs.clear();
        localGuids.clear();
    }
}
