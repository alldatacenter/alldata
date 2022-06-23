/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas;

import org.apache.atlas.model.instance.GuidMapping;
import org.apache.atlas.model.legacy.EntityResult;
import org.apache.atlas.type.AtlasType;

import java.util.Collections;
import java.util.List;

/**
 * Result from creating or updating entities.
 */
@Deprecated
public class CreateUpdateEntitiesResult {

    /**
     * Guid mapping for the entities that were created/updated
     */
    private GuidMapping guidMapping;

    /**
     * Entity result
     */
    private EntityResult entityResult;

    /**
     * Gets the guid mapping
     */
    public GuidMapping getGuidMapping() {
        return guidMapping;
    }

    /**
     * Sets the guid mapping
     */
    public void setGuidMapping(GuidMapping guidMapping) {
        this.guidMapping = guidMapping;
    }

    /**
     * Gets the entity result
     */
    public EntityResult getEntityResult() {
        return entityResult;
    }

    /**
     * Sets the entity result
     */
    public void setEntityResult(EntityResult entityResult) {
        this.entityResult = entityResult;
    }

    /**
     * Deserializes the given json into an instance of
     * CreateUpdateEntitiesResult.
     *
     * @param json
     *            the (unmodified) json that comes back from Atlas.
     * @return
     * @throws AtlasServiceException
     */
    public static CreateUpdateEntitiesResult fromJson(String json) throws AtlasServiceException {

        GuidMapping guidMapping = AtlasType.fromJson(json, GuidMapping.class);
        EntityResult entityResult = EntityResult.fromString(json);
        CreateUpdateEntitiesResult result = new CreateUpdateEntitiesResult();
        result.setEntityResult(entityResult);
        result.setGuidMapping(guidMapping);
        return result;
    }

    /**
     * Convenience method to get the guids of the created entities from
     * the EntityResult.
     */
    public List<String> getCreatedEntities() {
        if(entityResult == null) {
            return Collections.emptyList();
        }
        return getEntityResult().getCreatedEntities();
    }

    /**
     * Convenience method to get the guids of the updated entities from
     * the EntityResult.
     */
    public List<String> getUpdatedEntities() {
        if(entityResult == null) {
            return Collections.emptyList();
        }
        return getEntityResult().getUpdateEntities();
    }


    /**
     * Convenience method to get the guids of the deleted entities
     * from the EntityResult.
     */
    public List<String> getDeletedEntities() {
        if (entityResult == null) {
            return Collections.emptyList();
        }
        return getEntityResult().getDeletedEntities();
    }

}
