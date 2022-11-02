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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabServiceMappingEntity;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.apache.commons.collections.CollectionUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class KerberosKeytabPrincipalDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  HostDAO hostDAO;

  @Transactional
  public void create(KerberosKeytabPrincipalEntity kerberosKeytabPrincipalEntity) {
    entityManagerProvider.get().persist(kerberosKeytabPrincipalEntity);
  }

  @Transactional
  public void create(
    KerberosKeytabEntity kerberosKeytabEntity,
    HostEntity hostEntity,
    KerberosPrincipalEntity principalEntity) {
    entityManagerProvider.get().persist(
      new KerberosKeytabPrincipalEntity(kerberosKeytabEntity, hostEntity, principalEntity)
    );
  }

  /**
   * Find or create {@link KerberosKeytabPrincipalEntity} with specified dependecies.
   *
   * @param kerberosKeytabEntity {@link KerberosKeytabEntity} which owns this principal
   * @param hostEntity           {@link HostEntity} which owns this principal
   * @param kerberosPrincipalEntity      {@link KerberosPrincipalEntity} which related to this principal
   * @return evaluated entity
   */
  public KerberosKeytabPrincipalEntity findOrCreate(KerberosKeytabEntity kerberosKeytabEntity, HostEntity hostEntity, KerberosPrincipalEntity kerberosPrincipalEntity) {
    Long hostId = hostEntity == null ? null : hostEntity.getHostId();
    KerberosKeytabPrincipalEntity kkp = findByNaturalKey(hostId, kerberosKeytabEntity.getKeytabPath(), kerberosPrincipalEntity.getPrincipalName());
    if (kkp == null) {
      kkp = new KerberosKeytabPrincipalEntity(
          kerberosKeytabEntity,
          hostEntity,
          kerberosPrincipalEntity
      );
      create(kkp);
      kerberosKeytabEntity.addKerberosKeytabPrincipal(kkp);
      kerberosPrincipalEntity.addKerberosKeytabPrincipal(kkp);
    }
    return kkp;
  }

  @Transactional
  public KerberosKeytabPrincipalEntity merge(KerberosKeytabPrincipalEntity kerberosKeytabPrincipalEntity) {
    return entityManagerProvider.get().merge(kerberosKeytabPrincipalEntity);
  }

  @Transactional
  public void remove(KerberosKeytabPrincipalEntity kerberosKeytabPrincipalEntity) {
    entityManagerProvider.get().remove(merge(kerberosKeytabPrincipalEntity));
  }

  public void remove(Collection<KerberosKeytabPrincipalEntity> kerberosKeytabPrincipalEntities) {
    for (KerberosKeytabPrincipalEntity entity : kerberosKeytabPrincipalEntities) {
      remove(entity);
    }
  }

  @RequiresSession
  public List<KerberosKeytabPrincipalEntity> findByPrincipal(String principal) {
    TypedQuery<KerberosKeytabPrincipalEntity> query = entityManagerProvider.get().
      createNamedQuery("KerberosKeytabPrincipalEntity.findByPrincipal", KerberosKeytabPrincipalEntity.class);
    query.setParameter("principalName", principal);
    List<KerberosKeytabPrincipalEntity> result = query.getResultList();
    if (result == null) {
      return Collections.emptyList();
    }
    return result;
  }

  @RequiresSession
  public List<KerberosKeytabPrincipalEntity> findByHost(Long hostId) {
    TypedQuery<KerberosKeytabPrincipalEntity> query = entityManagerProvider.get().
      createNamedQuery("KerberosKeytabPrincipalEntity.findByHost", KerberosKeytabPrincipalEntity.class);
    query.setParameter("hostId", hostId);
    List<KerberosKeytabPrincipalEntity> result = query.getResultList();
    if (result == null) {
      return Collections.emptyList();
    }
    return result;
  }

  @RequiresSession
  public List<KerberosKeytabPrincipalEntity> findByHostAndKeytab(Long hostId, String keytabPath) {
    TypedQuery<KerberosKeytabPrincipalEntity> query = entityManagerProvider.get().
      createNamedQuery("KerberosKeytabPrincipalEntity.findByHostAndKeytab", KerberosKeytabPrincipalEntity.class);
    query.setParameter("hostId", hostId);
    query.setParameter("keytabPath", keytabPath);
    List<KerberosKeytabPrincipalEntity> result = query.getResultList();
    if (result == null) {
      return Collections.emptyList();
    }
    return result;
  }

  @RequiresSession
  public KerberosKeytabPrincipalEntity findByHostKeytabAndPrincipal(Long hostId, String keytabPath, String principalName) {
    TypedQuery<KerberosKeytabPrincipalEntity> query = entityManagerProvider.get().
      createNamedQuery("KerberosKeytabPrincipalEntity.findByHostKeytabAndPrincipal", KerberosKeytabPrincipalEntity.class);
    query.setParameter("hostId", hostId);
    query.setParameter("keytabPath", keytabPath);
    query.setParameter("principalName", principalName);
    List<KerberosKeytabPrincipalEntity> result = query.getResultList();
    if (result == null || result.size() == 0) {
      return null;
    } else {
      return result.get(0);
    }
  }

  @RequiresSession
  public KerberosKeytabPrincipalEntity findByKeytabAndPrincipalNullHost(String keytabPath, String principal) {
    TypedQuery<KerberosKeytabPrincipalEntity> query = entityManagerProvider.get().
      createNamedQuery("KerberosKeytabPrincipalEntity.findByKeytabAndPrincipalNullHost", KerberosKeytabPrincipalEntity.class);
    query.setParameter("keytabPath", keytabPath);
    query.setParameter("principalName", principal);
    List<KerberosKeytabPrincipalEntity> result = query.getResultList();
    if (result == null || result.size() == 0) {
      return null;
    } else {
      return result.get(0);
    }
  }

  /**
   * Ideally for this record PK must be (hostId, keytabPath, principalName), but in some cases hostId can be null.
   * So surrogate auto-generated PK used, and unique constraint for (hostId, keytabPath, principalName) applied.
   * This method checks if hostId is null and calls specific method.
   *
   * @param hostId host id
   * @param keytabPath keytab path
   * @param principalName principal name
   * @return keytab found
   */
  public KerberosKeytabPrincipalEntity findByNaturalKey(Long hostId, String keytabPath, String principalName) {
    if (hostId == null) {
      return findByKeytabAndPrincipalNullHost(keytabPath, principalName);
    } else {
      return findByHostKeytabAndPrincipal(hostId, keytabPath, principalName);
    }
  }

  @RequiresSession
  public List<KerberosKeytabPrincipalEntity> findByFilter(KerberosKeytabPrincipalFilter filter) {
    CriteriaBuilder cb = entityManagerProvider.get().getCriteriaBuilder();
    CriteriaQuery<KerberosKeytabPrincipalEntity> cq = cb.createQuery(KerberosKeytabPrincipalEntity.class);
    Root<KerberosKeytabPrincipalEntity> root = cq.from(KerberosKeytabPrincipalEntity.class);
    ArrayList<Predicate> predicates = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(filter.getServiceNames())) {
      Join<KerberosKeytabPrincipalEntity, KerberosKeytabServiceMappingEntity> mappingJoin = root.join("serviceMapping");
      predicates.add(mappingJoin.get("serviceName").in(filter.getServiceNames()));
      if (CollectionUtils.isNotEmpty(filter.getComponentNames())) {
        predicates.add(mappingJoin.get("componentName").in(filter.getComponentNames()));
      }
    }

    if (CollectionUtils.isNotEmpty(filter.getHostNames())) {
      List<Long> hostIds = new ArrayList<>();
      boolean hasNull = false;

      for (String hostname : filter.getHostNames()) {
        HostEntity host = hostDAO.findByName(hostname);

        if (host == null) {
          // host may be null after a delete host operation, if so, add an OR NULL clause
          hasNull = true;
        } else {
          hostIds.add(host.getHostId());
        }
      }

      Predicate hostIDPredicate = (hostIds.isEmpty()) ? null : root.get("hostId").in(hostIds);
      Predicate hostNullIDPredicate = (hasNull) ? root.get("hostId").isNull() : null;

      if (hostIDPredicate != null) {
        if (hostNullIDPredicate != null) {
          predicates.add(cb.or(hostIDPredicate, hostNullIDPredicate));
        } else {
          predicates.add(hostIDPredicate);
        }
      } else if (hostNullIDPredicate != null) {
        predicates.add(hostNullIDPredicate);
      }
    }

    if (CollectionUtils.isNotEmpty(filter.getPrincipals())) {
      predicates.add(root.get("principalName").in(filter.getPrincipals()));
    }
    cq.where(cb.and(predicates.toArray(new Predicate[0])));

    TypedQuery<KerberosKeytabPrincipalEntity> query = entityManagerProvider.get().createQuery(cq);
    List<KerberosKeytabPrincipalEntity> result = query.getResultList();
    if (result == null) {
      return Collections.emptyList();
    }
    return result;
  }


  public List<KerberosKeytabPrincipalEntity> findByFilters(Collection<KerberosKeytabPrincipalFilter> filters) {
    ArrayList<KerberosKeytabPrincipalEntity> result = new ArrayList<>();
    for (KerberosKeytabPrincipalFilter filter : filters) {
      result.addAll(findByFilter(filter));
    }
    return result;
  }

  @RequiresSession
  public boolean exists(Long hostId, String keytabPath, String principalName) {
    return findByNaturalKey(hostId, keytabPath, principalName) != null;
  }

  @RequiresSession
  public List<KerberosKeytabPrincipalEntity> findAll() {
    TypedQuery<KerberosKeytabPrincipalEntity> query = entityManagerProvider.get().
      createNamedQuery("KerberosKeytabPrincipalEntity.findAll", KerberosKeytabPrincipalEntity.class);
    List<KerberosKeytabPrincipalEntity> result = query.getResultList();
    if (result == null) {
      return Collections.emptyList();
    }
    return result;
  }

  @Transactional
  public void remove(List<KerberosKeytabPrincipalEntity> entities) {
    if (entities != null) {
      for (KerberosKeytabPrincipalEntity entity : entities) {
        entityManagerProvider.get().remove(merge(entity));
      }
    }
  }

  public void removeByHost(Long hostId) {
    remove(findByHost(hostId));
  }

  public static class KerberosKeytabPrincipalFilter {
    private Collection<String> hostNames;
    private Collection<String> serviceNames;
    private Collection<String> componentNames;
    private Collection<String> principals;

    private KerberosKeytabPrincipalFilter() {
      this(null, null, null, null);
    }

    private KerberosKeytabPrincipalFilter(Collection<String> hostNames, Collection<String> serviceNames, Collection<String> componentNames, Collection<String> principals) {
      this.hostNames = hostNames;
      this.serviceNames = serviceNames;
      this.componentNames = componentNames;
      this.principals = principals;
    }

    public Collection<String> getHostNames() {
      return hostNames;
    }

    public void setHostNames(Collection<String> hostNames) {
      this.hostNames = hostNames;
    }

    public Collection<String> getServiceNames() {
      return serviceNames;
    }

    public void setServiceNames(Collection<String> serviceNames) {
      this.serviceNames = serviceNames;
    }

    public Collection<String> getComponentNames() {
      return componentNames;
    }

    public void setComponentNames(Collection<String> componentNames) {
      this.componentNames = componentNames;
    }

    public Collection<String> getPrincipals() {
      return principals;
    }

    public void setPrincipals(Collection<String> principals) {
      this.principals = principals;
    }

    public static KerberosKeytabPrincipalFilter createEmptyFilter() {
      return new KerberosKeytabPrincipalFilter();
    }

    public static KerberosKeytabPrincipalFilter createFilter(String serviceName, Collection<String> componentNames, Collection<String> hostNames, Collection<String> principalNames) {
      return new KerberosKeytabPrincipalFilter(hostNames,
          (serviceName == null) ? null : Collections.singleton(serviceName),
          componentNames,
          principalNames);
    }
  }
}
