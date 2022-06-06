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
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.KerberosKeytabEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class KerberosKeytabDAO {
  private final static Logger LOG = LoggerFactory.getLogger(KerberosKeytabDAO.class);

  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  @Transactional
  public void create(KerberosKeytabEntity kerberosKeytabEntity) {
    entityManagerProvider.get().persist(kerberosKeytabEntity);
  }

  public void create(String keytabPath) {
    create(new KerberosKeytabEntity(keytabPath));
  }

  @Transactional
  public KerberosKeytabEntity merge(KerberosKeytabEntity kerberosKeytabEntity) {
    return entityManagerProvider.get().merge(kerberosKeytabEntity);
  }

  @Transactional
  public void remove(KerberosKeytabEntity kerberosKeytabEntity) {
    if (kerberosKeytabEntity != null) {
      EntityManager entityManager = entityManagerProvider.get();
      entityManager.remove(entityManager.merge(kerberosKeytabEntity));
    }
  }

  public void remove(String keytabPath) {
    KerberosKeytabEntity kke = find(keytabPath);
    if (kke != null) {
      remove(kke);
    }
  }

  @Transactional
  public void refresh(KerberosKeytabEntity kerberosKeytabEntity) {
    entityManagerProvider.get().refresh(kerberosKeytabEntity);
  }

  @RequiresSession
  public KerberosKeytabEntity find(String keytabPath) {
    return entityManagerProvider.get().find(KerberosKeytabEntity.class, keytabPath);
  }

  @RequiresSession
  public List<KerberosKeytabEntity> findByPrincipalAndHost(String principalName, Long hostId) {
    if(hostId == null) {
      return findByPrincipalAndNullHost(principalName);
    }
    TypedQuery<KerberosKeytabEntity> query = entityManagerProvider.get().
      createNamedQuery("KerberosKeytabEntity.findByPrincipalAndHost", KerberosKeytabEntity.class);
    query.setParameter("hostId", hostId);
    query.setParameter("principalName", principalName);
    List<KerberosKeytabEntity> result = query.getResultList();
    if(result == null) {
      return Collections.emptyList();
    }
    return result;
  }

  @RequiresSession
  public List<KerberosKeytabEntity> findByPrincipalAndNullHost(String principalName) {
    TypedQuery<KerberosKeytabEntity> query = entityManagerProvider.get().
      createNamedQuery("KerberosKeytabEntity.findByPrincipalAndNullHost", KerberosKeytabEntity.class);
    query.setParameter("principalName", principalName);
    List<KerberosKeytabEntity> result = query.getResultList();
    if(result == null) {
      return Collections.emptyList();
    }
    return result;
  }

  @RequiresSession
  public List<KerberosKeytabEntity> findAll() {
    TypedQuery<KerberosKeytabEntity> query = entityManagerProvider.get().
      createNamedQuery("KerberosKeytabEntity.findAll", KerberosKeytabEntity.class);
    List<KerberosKeytabEntity> result = query.getResultList();
    if(result == null) {
      return Collections.emptyList();
    }
    return result;
  }

  @RequiresSession
  public boolean exists(String keytabPath) {
    return find(keytabPath) != null;
  }

  @RequiresSession
  public boolean exists(KerberosKeytabEntity kerberosKeytabEntity) {
    return find(kerberosKeytabEntity.getKeytabPath()) != null;
  }

  public void remove(List<KerberosKeytabEntity> entities) {
    if (entities != null) {
      for (KerberosKeytabEntity entity : entities) {
        remove(entity);
      }
    }
  }

  /**
   * Determines if there are any references to the {@link KerberosKeytabEntity} before attemping
   * to remove it.  If there are any references to it, the entity will be not be removed.
   *
   * @param kerberosKeytabEntity the entity
   * @return <code>true</code>, if the entity was remove; <code>false</code> otherwise
   */
  public boolean removeIfNotReferenced(KerberosKeytabEntity kerberosKeytabEntity) {
    if (kerberosKeytabEntity != null) {
      if (CollectionUtils.isNotEmpty(kerberosKeytabEntity.getKerberosKeytabPrincipalEntities())) {
        ArrayList<String> ids = new ArrayList<>();
        for (KerberosKeytabPrincipalEntity entity : kerberosKeytabEntity.getKerberosKeytabPrincipalEntities()) {
          Long id = entity.getKkpId();

          if (id != null) {
            ids.add(String.valueOf(id));
          }
        }

        LOG.debug(String.format("keytab entry for %s is still referenced by [%s]", kerberosKeytabEntity.getKeytabPath(), String.join(",", ids)));
      } else {
        LOG.debug(String.format("keytab entry for %s is no longer referenced. It will be removed.", kerberosKeytabEntity.getKeytabPath()));
        remove(kerberosKeytabEntity);
        return true;
      }
    }

    return false;
  }
}
