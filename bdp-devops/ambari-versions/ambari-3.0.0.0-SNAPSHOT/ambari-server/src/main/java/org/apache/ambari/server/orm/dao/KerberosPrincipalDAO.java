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


import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;


/**
 * HostKerberosPrincipal Data Access Object.
 */
@Singleton
public class KerberosPrincipalDAO {

  private final static Logger LOG = LoggerFactory.getLogger(KerberosPrincipalDAO.class);
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * Make an instance managed and persistent.
   *
   * @param kerberosPrincipalEntity entity to persist
   */
  @Transactional
  public void create(KerberosPrincipalEntity kerberosPrincipalEntity) {
    entityManagerProvider.get().persist(kerberosPrincipalEntity);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param principalName the principal name to use when creating a new KerberosPrincipalEntity to
   *                      store
   * @param service       a boolean value declaring whether the principal represents a service (true) or not )false).
   */
  @Transactional
  public void create(String principalName, boolean service) {
    create(new KerberosPrincipalEntity(principalName, service, null));
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @param kerberosPrincipalEntity entity to merge
   * @return the merged entity
   */
  @Transactional
  public KerberosPrincipalEntity merge(KerberosPrincipalEntity kerberosPrincipalEntity) {
    return entityManagerProvider.get().merge(kerberosPrincipalEntity);
  }

  /**
   * Remove the entity instance.
   *
   * @param kerberosPrincipalEntity entity to remove
   */
  @Transactional
  public void remove(KerberosPrincipalEntity kerberosPrincipalEntity) {
    if (kerberosPrincipalEntity != null) {
      EntityManager entityManager = entityManagerProvider.get();
      entityManager.remove(entityManager.merge(kerberosPrincipalEntity));
    }
  }

  /**
   * Remove entity instance by primary key
   *
   * @param principalName Primary key: kerberos principal name
   */
  @Transactional
  public void remove(String principalName) {
    entityManagerProvider.get().remove(find(principalName));
  }

  /**
   * Refresh the state of the instance from the database,
   * overwriting changes made to the entity, if any.
   *
   * @param kerberosPrincipalEntity entity to refresh
   */
  @Transactional
  public void refresh(KerberosPrincipalEntity kerberosPrincipalEntity) {
    entityManagerProvider.get().refresh(kerberosPrincipalEntity);
  }

  /**
   * Find a KerberosPrincipalEntity with the given principal name.
   *
   * @param principalName name of kerberos principal to find
   * @return a matching KerberosPrincipalEntity or null
   */
  @RequiresSession
  public KerberosPrincipalEntity find(String principalName) {
    return entityManagerProvider.get().find(KerberosPrincipalEntity.class, principalName);
  }

  /**
   * Find all kerberos principals.
   *
   * @return a List of all KerberosPrincipalEntity objects or an empty List
   */
  @RequiresSession
  public List<KerberosPrincipalEntity> findAll() {
    TypedQuery<KerberosPrincipalEntity> query = entityManagerProvider.get()
        .createNamedQuery("KerberosPrincipalEntityFindAll", KerberosPrincipalEntity.class);
    return query.getResultList();
  }

  /**
   * Tests the given principal name to see if it already exists.
   *
   * @param principalName name of kerberos principal to find
   * @return true if the principal exists; false otherwise
   */
  @RequiresSession
  public boolean exists(String principalName) {
    return find(principalName) != null;
  }

  public void remove(List<KerberosPrincipalEntity> entities) {
    if (entities != null) {
      for (KerberosPrincipalEntity entity : entities) {
        remove(entity);
      }
    }
  }

  /**
   * Determines if there are any references to the {@link KerberosPrincipalEntity} before attempting
   * to remove it.  If there are any references to it, the entity will be not be removed.
   *
   * @param kerberosPrincipalEntity the entity
   * @return <code>true</code>, if the entity was remove; <code>false</code> otherwise
   */
  public boolean removeIfNotReferenced(KerberosPrincipalEntity kerberosPrincipalEntity) {
    if (kerberosPrincipalEntity != null) {
      if (CollectionUtils.isNotEmpty(kerberosPrincipalEntity.getKerberosKeytabPrincipalEntities())) {
        ArrayList<String> ids = new ArrayList<>();
        for (KerberosKeytabPrincipalEntity entity : kerberosPrincipalEntity.getKerberosKeytabPrincipalEntities()) {
          Long id = entity.getKkpId();

          if (id != null) {
            ids.add(String.valueOf(id));
          }
        }

        LOG.info(String.format("principal entry for %s is still referenced by [%s]", kerberosPrincipalEntity.getPrincipalName(), String.join(",", ids)));
      } else {
        LOG.info(String.format("principal entry for %s is no longer referenced. It will be removed.", kerberosPrincipalEntity.getPrincipalName()));
        remove(kerberosPrincipalEntity);
        return true;
      }
    }

    return false;
  }
}
