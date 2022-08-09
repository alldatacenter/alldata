/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.listener;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.AtlasRelationship;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entity change notification listener V2.
 */
public interface EntityChangeListenerV2 {
    /**
     * This is upon adding new entities to the repository.
     *
     * @param entities the created entities
     * @param isImport
     */
    void onEntitiesAdded(List<AtlasEntity> entities, boolean isImport) throws AtlasBaseException;

    /**
     * This is upon updating an entity.
     *
     * @param entities the updated entities
     * @param isImport
     */
    void onEntitiesUpdated(List<AtlasEntity> entities, boolean isImport) throws AtlasBaseException;

    /**
     * This is upon deleting entities from the repository.
     *
     * @param entities the deleted entities
     * @param isImport
     */
    void onEntitiesDeleted(List<AtlasEntity> entities, boolean isImport) throws AtlasBaseException;


    /**
     * This is upon purging entities from the repository.
     *
     * @param entities the purged entities
     */
    void onEntitiesPurged(List<AtlasEntity> entities) throws AtlasBaseException;

    /**
     * This is upon adding new classifications to an entity.
     *
     * @param entity          the entity
     * @param classifications classifications that needs to be added to an entity
     * @throws AtlasBaseException if the listener notification fails
     */
    void onClassificationsAdded(AtlasEntity entity, List<AtlasClassification> classifications) throws AtlasBaseException;

    /**
     * This is upon adding new classifications to entities.
     *
     * @param entities              list of entities
     * @param classifications classifications that are to be added to entities
     * @throws AtlasBaseException if the listener notification fails
     */
    void onClassificationsAdded(List<AtlasEntity> entities, List<AtlasClassification> classifications) throws AtlasBaseException;

    /**
     * This is upon updating classifications to an entity.
     *
     * @param entity          the entity
     * @param classifications classifications that needs to be updated for an entity
     * @throws AtlasBaseException if the listener notification fails
     */
    void onClassificationsUpdated(AtlasEntity entity, List<AtlasClassification> classifications) throws AtlasBaseException;

    /**
     * This is upon deleting classifications from an entity.
     *
     * @param entity              the entity
     * @param classifications classifications that needs to be updated for an entity
     * @throws AtlasBaseException if the listener notification fails
     */
    void onClassificationsDeleted(AtlasEntity entity, List<AtlasClassification> classifications) throws AtlasBaseException;

    /**
     * This is upon deleting classifications from entities.
     *
     * @param entities              list of entities
     * @param classifications classifications that needs to be deleted from entities
     * @throws AtlasBaseException if the listener notification fails
     */
    void onClassificationsDeleted(List<AtlasEntity> entities, List<AtlasClassification> classifications) throws AtlasBaseException;

    /**
     * This is upon adding a new term to an entity.
     *
     * @param term     the term
     * @param entities list of entities to which the term is assigned
     */
    void onTermAdded(AtlasGlossaryTerm term, List<AtlasRelatedObjectId> entities) throws AtlasBaseException;

    /**
     * This is upon removing a term from an entity.
     *
     * @param term     the term
     * @param entities list of entities to which the term is assigned
     */
    void onTermDeleted(AtlasGlossaryTerm term, List<AtlasRelatedObjectId> entities) throws AtlasBaseException;

    /**
     * This is upon adding new relationships to the repository.
     *
     * @param relationships the created relationships
     * @param isImport
     */
    void onRelationshipsAdded(List<AtlasRelationship> relationships, boolean isImport) throws AtlasBaseException;

    /**
     * This is upon updating an relationships.
     *
     * @param relationships the updated relationships
     * @param isImport
     */
    void onRelationshipsUpdated(List<AtlasRelationship> relationships, boolean isImport) throws AtlasBaseException;

    /**
     * This is upon deleting relationships from the repository.
     *
     * @param relationships the deleted relationships
     * @param isImport
     */
    void onRelationshipsDeleted(List<AtlasRelationship> relationships, boolean isImport) throws AtlasBaseException;

    /**
     * This is upon purging relationships from the repository.
     *
     * @param relationships the purged relationships
     */
    void onRelationshipsPurged(List<AtlasRelationship> relationships) throws AtlasBaseException;

    /**
     * This is upon add new labels to an entity.
     *
     * @param entity the entity
     * @param labels labels that needs to be added to an entity
     * @throws AtlasBaseException if the listener notification fails
     */
    void onLabelsAdded(AtlasEntity entity, Set<String> labels) throws AtlasBaseException;

    /**
     * This is upon deleting labels from an entity.
     *
     * @param entity the entity
     * @param labels labels that needs to be deleted for an entity
     * @throws AtlasBaseException if the listener notification fails
     */
    void onLabelsDeleted(AtlasEntity entity, Set<String> labels) throws AtlasBaseException;

    /**
     *
     * @param entity the entity
     * @param updatedBusinessAttributes business metadata attribute
     * @throws AtlasBaseException if the listener notification fails
     */
    void onBusinessAttributesUpdated(AtlasEntity entity, Map<String, Map<String, Object>> updatedBusinessAttributes) throws AtlasBaseException;
}