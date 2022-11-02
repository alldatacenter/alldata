/*
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
package org.apache.ambari.server.orm.dao;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.KerberosDescriptorEntity;

import com.google.inject.persist.Transactional;

@Singleton
public class KerberosDescriptorDAO {

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @RequiresSession
  public KerberosDescriptorEntity findByName(String kerberosDescriptorName) {
    return entityManagerProvider.get().find(KerberosDescriptorEntity.class, kerberosDescriptorName);
  }

  /**
   * Find all kerberos descriptors.
   *
   * @return all kerberos descriptors or an empty List
   */
  @RequiresSession
  public List<KerberosDescriptorEntity> findAll() {
    TypedQuery<KerberosDescriptorEntity> query = entityManagerProvider.get().
            createNamedQuery("allKerberosDescriptors", KerberosDescriptorEntity.class);
    return query.getResultList();
  }

  @Transactional
  public void create(KerberosDescriptorEntity kerberosDescriptorEntity) {
    entityManagerProvider.get().persist(kerberosDescriptorEntity);
  }


  /**
   * Refresh the state of the instance from the database,
   * overwriting changes made to the entity, if any.
   *
   * @param kerberosDescriptorEntity entity to refresh
   */
  @Transactional
  public void refresh(KerberosDescriptorEntity kerberosDescriptorEntity) {
    entityManagerProvider.get().refresh(kerberosDescriptorEntity);
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @param kerberosDescriptorEntity entity to merge
   * @return the merged entity
   */
  @Transactional
  public KerberosDescriptorEntity merge(KerberosDescriptorEntity kerberosDescriptorEntity) {
    return entityManagerProvider.get().merge(kerberosDescriptorEntity);
  }

  /**
   * Remove the entity instance.
   *
   * @param kerberosDescriptorEntity entity to remove
   */
  @Transactional
  public void remove(KerberosDescriptorEntity kerberosDescriptorEntity) {
    entityManagerProvider.get().remove(merge(kerberosDescriptorEntity));
  }

  /**
   * Remove entity instance by primary key
   *
   * @param name Primary key: kerberos descriptor name
   */
  @Transactional
  public void removeByName(String name) {
    entityManagerProvider.get().remove(findByName(name));
  }


}
