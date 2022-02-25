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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.cache.HostConfigMapping;
import org.apache.ambari.server.orm.cache.HostConfigMappingImpl;
import org.apache.ambari.server.orm.entities.HostConfigMappingEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Used for host configuration mapping operations.
 */
@Singleton
public class HostConfigMappingDAO {
  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @Inject
  private DaoUtils daoUtils;

  @Inject
  private HostDAO hostDAO;

  private ConcurrentHashMap<Long, Set<HostConfigMapping>> hostConfigMappingByHost;
  
  private volatile boolean cacheLoaded;
  
  private void populateCache() {
    
    if (!cacheLoaded) {
      if (hostConfigMappingByHost == null) {
        hostConfigMappingByHost = new ConcurrentHashMap<>();

        TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createNamedQuery(
            "HostConfigMappingEntity.findAll", HostConfigMappingEntity.class);

        List<HostConfigMappingEntity> hostConfigMappingEntities = daoUtils.selectList(query);

        for (HostConfigMappingEntity hostConfigMappingEntity : hostConfigMappingEntities) {
          Long hostId = hostConfigMappingEntity.getHostId();

          if (hostId == null) {
            continue;
          }

          Set<HostConfigMapping> setByHost;
          if (hostConfigMappingByHost.containsKey(hostId)) {
            setByHost = hostConfigMappingByHost.get(hostId);
          } else {
            setByHost = new HashSet<>();
            hostConfigMappingByHost.put(hostId, setByHost);
          }

          HostConfigMapping hostConfigMapping = buildHostConfigMapping(hostConfigMappingEntity);
          setByHost.add(hostConfigMapping);
        }
      }
      
      cacheLoaded = true;
    }
  }
  

  @Transactional
  public void create(HostConfigMapping hostConfigMapping) {
    populateCache();
    
    //create in db
    entityManagerProvider.get().persist(buildHostConfigMappingEntity(hostConfigMapping));
    
    //create in cache
    Long hostId = hostConfigMapping.getHostId();

    if (hostId != null) {
      Set<HostConfigMapping> set;
      if (hostConfigMappingByHost.containsKey(hostId)) {
        set = hostConfigMappingByHost.get(hostId);
      } else {
        set = new HashSet<>();
        hostConfigMappingByHost.put(hostId, set);
      }

      set.add(hostConfigMapping);
    }
  }

  @Transactional
  public HostConfigMapping merge(HostConfigMapping hostConfigMapping) {
    populateCache();

    Long hostId = hostConfigMapping.getHostId();
    if (hostId != null) {
      Set<HostConfigMapping> set;
      if (hostConfigMappingByHost.containsKey(hostId)) {
        set = hostConfigMappingByHost.get(hostId);
      } else {
        set = new HashSet<>();
        hostConfigMappingByHost.put(hostId, set);
      }

      //Update object in set
      set.remove(hostConfigMapping);
      set.add(hostConfigMapping);

      entityManagerProvider.get().merge(buildHostConfigMappingEntity(hostConfigMapping));
    }

    return hostConfigMapping;
  }

  @RequiresSession
  public Set<HostConfigMapping> findByType(final long clusterId, Long hostId, final String type) {
    populateCache();
    
    if (!hostConfigMappingByHost.containsKey(hostId))
      return Collections.emptySet();
      
    Set<HostConfigMapping> set = new HashSet<>(hostConfigMappingByHost.get(hostId));
     
    CollectionUtils.filter(set, new Predicate() {
        
      @Override
      public boolean evaluate(Object arg0) {
        return ((HostConfigMapping) arg0).getClusterId().equals(clusterId) 
            && ((HostConfigMapping) arg0).getType().equals(type);
      }
    });

    return set;
  }

  @RequiresSession
  public HostConfigMapping findSelectedByType(final long clusterId,
      Long hostId, final String type) {
    populateCache();
    
    if (!hostConfigMappingByHost.containsKey(hostId))
      return null;
    
    Set<HostConfigMapping> set = new HashSet<>(hostConfigMappingByHost.get(hostId));
    
    HostConfigMapping result = (HostConfigMapping) CollectionUtils.find(set, new Predicate() {
      
      @Override
      public boolean evaluate(Object arg0) {
        return ((HostConfigMapping) arg0).getClusterId().equals(clusterId) 
            && ((HostConfigMapping) arg0).getType().equals(type)
            && ((HostConfigMapping) arg0).getSelected() > 0;
      }
    });
    
    return result;
  }

  @RequiresSession
  public Set<HostConfigMapping> findSelected(final long clusterId, Long hostId) {
    populateCache();
    
    if (!hostConfigMappingByHost.containsKey(hostId))
      return Collections.emptySet();
    
    Set<HostConfigMapping> set = new HashSet<>(hostConfigMappingByHost.get(hostId));
    
    CollectionUtils.filter(set, new Predicate() {
      
      @Override
      public boolean evaluate(Object arg0) {
        return ((HostConfigMapping) arg0).getClusterId().equals(clusterId) 
            && ((HostConfigMapping) arg0).getSelected() > 0;
      }
    });
    
    return set;
  }

  @RequiresSession
  public Set<HostConfigMapping> findSelectedByHosts(Collection<Long> hostIds) {
    populateCache();

    if (hostIds == null || hostIds.isEmpty()) {
      return Collections.emptySet();
    }
    
    HashSet<HostConfigMapping> result = new HashSet<>();

    for (final Long hostId : hostIds) {
      if (!hostConfigMappingByHost.containsKey(hostId))
        continue;
      
      Set<HostConfigMapping> set = new HashSet<>(hostConfigMappingByHost.get(hostId));
      
      CollectionUtils.filter(set, new Predicate() {
        
        @Override
        public boolean evaluate(Object arg0) {
          return ((HostConfigMapping) arg0).getHostId().equals(hostId) &&
              ((HostConfigMapping) arg0).getSelected() > 0;
        }
      });
      
      result.addAll(set);
    } 
    
    return result;
  }


  @RequiresSession
  public Map<String, List<HostConfigMapping>> findSelectedHostsByTypes(final long clusterId,
                                                                             Collection<String> types) {
    populateCache();
    
    Map<String, List<HostConfigMapping>> mappingsByType = new HashMap<>();
    
    for (String type : types) {
      if (!mappingsByType.containsKey(type)) {
        mappingsByType.put(type, new ArrayList<>());
      }
    }

    if (!types.isEmpty()) {
      List<HostConfigMapping> mappings = new ArrayList<>();

      for (Set<HostConfigMapping> entries : hostConfigMappingByHost.values()) {
        
        for (HostConfigMapping entry : entries) {
          
          if (types.contains(entry.getType()) && entry.getClusterId().equals(clusterId))
            mappings.add(new HostConfigMappingImpl(entry));
        }
      }

      for (HostConfigMapping mapping : mappings) {
        mappingsByType.get(mapping.getType()).add(mapping);
      }
    }

    return mappingsByType;
  }

  @RequiresSession
  public List<HostConfigMappingEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), HostConfigMappingEntity.class);
  }

  @Transactional
  public void removeByHostId(Long hostId) {
    populateCache();

    HostEntity hostEntity = hostDAO.findById(hostId);
    if (hostEntity != null) {
      if (hostConfigMappingByHost.containsKey(hostEntity.getHostId())) {
        // Delete from db
        TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createNamedQuery(
            "HostConfigMappingEntity.findByHostId", HostConfigMappingEntity.class);
        query.setParameter("hostId", hostId);

        List<HostConfigMappingEntity> hostConfigMappingEntities = daoUtils.selectList(query);

        for (HostConfigMappingEntity entity : hostConfigMappingEntities) {
          entityManagerProvider.get().remove(entity);
        }
        // Update the cache
        hostConfigMappingByHost.remove(hostEntity.getHostId());
      }
    }
  }

  @Transactional
  public void removeByClusterAndHostName(final long clusterId, String hostName) {
    populateCache();

    HostEntity hostEntity = hostDAO.findByName(hostName);
    if (hostEntity != null) {
      if (hostConfigMappingByHost.containsKey(hostEntity.getHostId())) {
        // Delete from db
        TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
            "SELECT entity FROM HostConfigMappingEntity entity " +
                "WHERE entity.clusterId = ?1 AND entity.hostId=?2",
            HostConfigMappingEntity.class);

        List<HostConfigMappingEntity> list = daoUtils.selectList(query, clusterId, hostEntity.getHostId());

        for (HostConfigMappingEntity entity : list) {
          entityManagerProvider.get().remove(entity);
        }

        // Remove from cache items with given clusterId
        Set<HostConfigMapping> set = hostConfigMappingByHost.get(hostEntity.getHostId());
        CollectionUtils.filter(set, new Predicate() {
          @Override
          public boolean evaluate(Object arg0) {
            return !((HostConfigMapping) arg0).getClusterId().equals(clusterId);
          }
        });
      }
    }
  }

  public HostConfigMappingEntity buildHostConfigMappingEntity(HostConfigMapping hostConfigMapping) {
    HostEntity hostEntity = hostDAO.findById(hostConfigMapping.getHostId());
    HostConfigMappingEntity hostConfigMappingEntity = new HostConfigMappingEntity();

    hostConfigMappingEntity.setClusterId(hostConfigMapping.getClusterId());
    hostConfigMappingEntity.setCreateTimestamp(hostConfigMapping.getCreateTimestamp());
    hostConfigMappingEntity.setHostId(hostEntity.getHostId());
    hostConfigMappingEntity.setSelected(hostConfigMapping.getSelected());
    hostConfigMappingEntity.setServiceName(hostConfigMapping.getServiceName());
    hostConfigMappingEntity.setType(hostConfigMapping.getType());
    hostConfigMappingEntity.setUser(hostConfigMapping.getUser());
    hostConfigMappingEntity.setVersion(hostConfigMapping.getVersion());
    
    return hostConfigMappingEntity;
  }
  
  public HostConfigMapping buildHostConfigMapping(
      HostConfigMappingEntity hostConfigMappingEntity) {
    HostConfigMapping hostConfigMapping = new HostConfigMappingImpl();
    
    hostConfigMapping.setClusterId(hostConfigMappingEntity.getClusterId());
    hostConfigMapping.setCreateTimestamp(hostConfigMappingEntity.getCreateTimestamp());
    hostConfigMapping.setHostId(hostConfigMappingEntity.getHostId());
    hostConfigMapping.setServiceName(hostConfigMappingEntity.getServiceName());
    hostConfigMapping.setType(hostConfigMappingEntity.getType());
    hostConfigMapping.setUser(hostConfigMappingEntity.getUser());
    hostConfigMapping.setSelected(hostConfigMappingEntity.isSelected());
    hostConfigMapping.setVersion(hostConfigMappingEntity.getVersion());
    
    return hostConfigMapping;
  }
}
