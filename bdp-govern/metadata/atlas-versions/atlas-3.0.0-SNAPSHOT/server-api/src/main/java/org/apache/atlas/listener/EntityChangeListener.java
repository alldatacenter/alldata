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

package org.apache.atlas.listener;

import org.apache.atlas.AtlasException;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;

import java.util.Collection;

/**
 * Entity (a Typed instance) change notification listener.
 */
public interface EntityChangeListener {
    /**
     * This is upon adding new entities to the repository.
     *
     * @param entities  the created entities
     *
     * @param isImport
     * @throws AtlasException if the listener notification fails
     */
    void onEntitiesAdded(Collection<Referenceable> entities, boolean isImport) throws AtlasException;

    /**
     * This is upon updating an entity.
     *
     * @param entities        the updated entities
     *
     * @param isImport
     * @throws AtlasException if the listener notification fails
     */
    void onEntitiesUpdated(Collection<Referenceable> entities, boolean isImport) throws AtlasException;

    /**
     * This is upon adding a new trait to a typed instance.
     *
     * @param entity        the entity
     * @param traits    trait that needs to be added to entity
     *
     * @throws AtlasException if the listener notification fails
     */
    void onTraitsAdded(Referenceable entity, Collection<? extends Struct> traits) throws AtlasException;

    /**
     * This is upon deleting a trait from a typed instance.
     *
     * @param entity        the entity
     * @param traits    trait that needs to be added to entity
     *
     * @throws AtlasException if the listener notification fails
     */
    void onTraitsDeleted(Referenceable entity, Collection<? extends Struct> traits) throws AtlasException;

    /**
     * This is upon updating a trait from a typed instance.
     *
     * @param entity    the entity
     * @param traits    trait that needs to be added to entity
     *
     * @throws AtlasException if the listener notification fails
     */
    void onTraitsUpdated(Referenceable entity, Collection<? extends Struct> traits) throws AtlasException;
    
    /**
     * This is upon deleting entities from the repository.
     *
     * @param entities the deleted entities
     * @param isImport
     * @throws AtlasException
     */
    void onEntitiesDeleted(Collection<Referenceable> entities, boolean isImport) throws AtlasException;

    /**
     * This is upon adding a new term to a list of typed instance.
     *
     * @param entities entity list
     * @param term  term that needs to be added to entity
     * @throws AtlasException if the listener notification fails
     */
    void onTermAdded(Collection<Referenceable> entities, AtlasGlossaryTerm term) throws AtlasException;

    /**
     * This is upon adding a new trait to a typed instance.
     *
     * @param entities entity list
     * @param term  term that needs to be added to entity
     * @throws AtlasException if the listener notification fails
     */
    void onTermDeleted(Collection<Referenceable> entities, AtlasGlossaryTerm term) throws AtlasException;
}
