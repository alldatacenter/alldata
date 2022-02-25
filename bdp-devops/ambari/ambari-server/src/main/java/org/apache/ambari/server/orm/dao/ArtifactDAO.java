/*
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

package org.apache.ambari.server.orm.dao;


import java.util.List;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ArtifactEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;


/**
 * Cluster Artifact Data Access Object.
 */
@Singleton
public class ArtifactDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * Find an artifact with the given name and foreign keys.
   *
   * @param artifactName name of artifact to find
   * @param foreignKeys  foreign keys of artifact as json representation of
   *                     a map of key properties to values
   *
   * @return  a matching artifact or null
   */
  @RequiresSession
  public ArtifactEntity findByNameAndForeignKeys(String artifactName, TreeMap<String, String> foreignKeys) {
    //todo: need to update PK in DB
    TypedQuery<ArtifactEntity> query = entityManagerProvider.get()
        .createNamedQuery("artifactByNameAndForeignKeys", ArtifactEntity.class);
    query.setParameter("artifactName", artifactName);
    query.setParameter("foreignKeys", ArtifactEntity.serializeForeignKeys(foreignKeys));

    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  /**
   * Find all artifacts for the specified foreign keys.
   *
   * @param foreignKeys  foreign keys of artifact as json representation of
   *                     a map of key properties to values
   *
   * @return all artifacts for the specified foreign keys or an empty List
   */
  @RequiresSession
  public List<ArtifactEntity> findByForeignKeys(TreeMap<String, String> foreignKeys) {
    TypedQuery<ArtifactEntity> query = entityManagerProvider.get().
        createNamedQuery("artifactByForeignKeys", ArtifactEntity.class);
    query.setParameter("foreignKeys", ArtifactEntity.serializeForeignKeys(foreignKeys));

    return query.getResultList();
  }

  /**
   * Find all artifacts for the specified artifact name.
   *
   * @param artifactName name of artifact to find
   * @return all artifacts with the specified artifact name or an empty List
   */
  @RequiresSession
  public List<ArtifactEntity> findByName(String artifactName) {
    TypedQuery<ArtifactEntity> query = entityManagerProvider.get().
        createNamedQuery("artifactByName", ArtifactEntity.class);
    query.setParameter("artifactName", artifactName);

    return query.getResultList();
  }

  /**
   * Refresh the state of the instance from the database,
   * overwriting changes made to the entity, if any.
   *
   * @param entity  entity to refresh
   */
  @Transactional
  public void refresh(ArtifactEntity entity) {
    entityManagerProvider.get().refresh(entity);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param entity  entity to persist
   */
  @Transactional
  public void create(ArtifactEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @param artifactEntity  entity to merge
   * @return the merged entity
   */
  @Transactional
  public ArtifactEntity merge(ArtifactEntity artifactEntity) {
    return entityManagerProvider.get().merge(artifactEntity);
  }

  /**
   * Remove the entity instance.
   *
   * @param artifactEntity  entity to remove
   */
  @Transactional
  public void remove(ArtifactEntity artifactEntity) {
    entityManagerProvider.get().remove(merge(artifactEntity));
  }
}
