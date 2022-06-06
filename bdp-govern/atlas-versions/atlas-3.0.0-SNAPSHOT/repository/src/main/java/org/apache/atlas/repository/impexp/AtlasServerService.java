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

package org.apache.atlas.repository.impexp;

import org.apache.atlas.annotation.AtlasService;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasServer;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.ogm.DataAccess;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.EntityGraphMapper;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@AtlasService
public class AtlasServerService {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasServerService.class);

    private final DataAccess dataAccess;
    private final AtlasEntityStore entityStore;
    private final AtlasTypeRegistry typeRegistry;
    private final EntityGraphRetriever entityGraphRetriever;

    @Inject
    public AtlasServerService(DataAccess dataAccess, AtlasEntityStore entityStore,
                              AtlasTypeRegistry typeRegistry,
                              EntityGraphRetriever entityGraphRetriever) {

        this.dataAccess = dataAccess;
        this.entityStore = entityStore;
        this.typeRegistry = typeRegistry;
        this.entityGraphRetriever = entityGraphRetriever;
    }

    public AtlasServer get(AtlasServer server) throws AtlasBaseException {
        try {
            return dataAccess.load(server);
        } catch (AtlasBaseException e) {
            throw e;
        }
    }

    public AtlasServer getCreateAtlasServer(String clusterName, String serverFullName) throws AtlasBaseException {
        AtlasServer defaultServer = new AtlasServer(clusterName, serverFullName);
        AtlasServer server = getAtlasServer(defaultServer);
        if (server == null) {
            return save(defaultServer);
        }

        return server;
    }

    private AtlasServer getAtlasServer(AtlasServer server) {
        try {
            return get(server);
        } catch (AtlasBaseException ex) {
            return null;
        }
    }

    @GraphTransaction
    public AtlasServer save(AtlasServer server) {
        try {
            return dataAccess.save(server);
        }
        catch (AtlasBaseException ex) {
            return server;
        }
    }

    @GraphTransaction
    public void updateEntitiesWithServer(AtlasServer server, List<String> entityGuids, String attributeName) throws AtlasBaseException {
        if (server != null && StringUtils.isEmpty(server.getGuid())) {
            return;
        }

        AtlasObjectId objectId = getObjectId(server);
        for (String guid : entityGuids) {
            AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = entityStore.getById(guid, false, false);
            updateAttribute(entityWithExtInfo, attributeName, objectId);
        }
    }

    private AtlasObjectId getObjectId(AtlasServer server) {
        return new AtlasObjectId(server.getGuid(), AtlasServer.class.getSimpleName());
    }


    /**
     * Attribute passed by name is updated with the value passed.
     * @param entityWithExtInfo Entity to be updated
     * @param propertyName attribute name
     * @param objectId Value to be set for attribute
     */
    private void updateAttribute(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo,
                                 String propertyName,
                                 AtlasObjectId objectId) {
        String value = EntityGraphMapper.getSoftRefFormattedValue(objectId);
        updateAttribute(entityWithExtInfo.getEntity(), propertyName, value);
        for (AtlasEntity e : entityWithExtInfo.getReferredEntities().values()) {
            updateAttribute(e, propertyName, value);
        }
    }

    private void updateAttribute(AtlasEntity entity, String attributeName, Object value) {
        if(entity.hasAttribute(attributeName) == false) return;

        try {
            AtlasVertex vertex = entityGraphRetriever.getEntityVertex(entity.getGuid());
            if(vertex == null) {
                return;
            }

            String qualifiedFieldName = getVertexPropertyName(entity, attributeName);
            List list = vertex.getListProperty(qualifiedFieldName);
            if (CollectionUtils.isEmpty(list)) {
                list = new ArrayList();
            }

            if (!list.contains(value)) {
                list.add(value);
                vertex.setListProperty(qualifiedFieldName, list);
            }
        }
        catch (AtlasBaseException ex) {
            LOG.error("error retrieving vertex from guid: {}", entity.getGuid(), ex);
        }
    }

    private String getVertexPropertyName(AtlasEntity entity, String attributeName) throws AtlasBaseException {
        AtlasEntityType type = (AtlasEntityType) typeRegistry.getType(entity.getTypeName());
        AtlasStructType.AtlasAttribute attribute = type.getAttribute(attributeName);
        return attribute.getVertexPropertyName();
    }
}
