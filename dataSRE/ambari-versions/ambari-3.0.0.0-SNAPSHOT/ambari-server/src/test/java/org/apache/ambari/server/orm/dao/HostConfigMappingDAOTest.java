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

import java.sql.SQLException;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.cache.HostConfigMapping;
import org.apache.ambari.server.orm.cache.HostConfigMappingImpl;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

/**
 * Tests host config mapping DAO and Entities
 */
public class HostConfigMappingDAOTest {

  private Injector injector;

  @Inject
  private HostConfigMappingDAO hostConfigMappingDAO;

  @Inject
  private HostDAO hostDAO;
  
  @Before
   public void setup() throws AmbariException{
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    
    hostConfigMappingDAO = injector.getInstance(HostConfigMappingDAO.class);
    hostDAO = injector.getInstance(HostDAO.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }
  
  private HostConfigMapping createEntity(long clusterId, String hostName, String type, String version) throws Exception {
    HostConfigMapping hostConfigMappingEntity = new HostConfigMappingImpl();
    hostConfigMappingEntity.setClusterId(Long.valueOf(clusterId));
    hostConfigMappingEntity.setCreateTimestamp(Long.valueOf(System.currentTimeMillis()));

    HostEntity hostEntity = hostDAO.findByName(hostName);
    if (hostEntity == null) {
      hostEntity = new HostEntity();
      hostEntity.setHostName(hostName);
      hostDAO.create(hostEntity);
    }

    hostConfigMappingEntity.setHostId(hostEntity.getHostId());
    hostConfigMappingEntity.setSelected(1);
    hostConfigMappingEntity.setType(type);
    hostConfigMappingEntity.setVersion(version);
    hostConfigMappingEntity.setUser("_test");

    hostConfigMappingDAO.create(hostConfigMappingEntity);
    
    return hostConfigMappingEntity;
  }
  
  @Test
  public void testCreate() throws Exception {
    createEntity(1L, "h1", "global", "v1");
  }
  

  @Test
  public void testFindByType() throws Exception {
    HostConfigMapping source = createEntity(1L, "h1", "global", "v1");
    HostEntity hostEntity = hostDAO.findByName("h1");
    
    Set<HostConfigMapping> target = hostConfigMappingDAO.findByType(1L, hostEntity.getHostId(), "global");

    Assert.assertEquals("Expected one result", 1, target.size());
    
    for (HostConfigMapping item : target) 
      Assert.assertEquals("Expected version 'v1'", source.getVersion(), item.getVersion());
  }
  
  @Test
  public void testMerge() throws Exception {
    HostConfigMapping source = createEntity(1L, "h1", "global", "v1");
    HostEntity hostEntity = hostDAO.findByName("h1");

    Set<HostConfigMapping> target = hostConfigMappingDAO.findByType(1L, hostEntity.getHostId(), "global");
    Assert.assertEquals("Expected one result", 1, target.size());
    
    HostConfigMapping toChange = null;
    
    for (HostConfigMapping item: target) {
      Assert.assertEquals("Expected version 'v1'", source.getVersion(), item.getVersion());
      Assert.assertEquals("Expected selected flag 1", 1, (int)item.getSelected());
      toChange = item;
      toChange.setSelected(0);
    }
    
    hostConfigMappingDAO.merge(toChange);
    
    target = hostConfigMappingDAO.findByType(1L, hostEntity.getHostId(), "global");
    Assert.assertEquals("Expected one result", 1, target.size());
    
    for (HostConfigMapping item: target) {
      Assert.assertEquals("Expected version 'v1'", source.getVersion(), item.getVersion());
      Assert.assertEquals("Expected selected flag 0", 0, (int)item.getSelected());
    }
  }
  
  @Test
  public void testFindSelected() throws Exception {
    createEntity(1L, "h1", "global", "version1");
    HostConfigMapping coreSiteConfigV1 = createEntity(1L, "h1", "core-site", "version1");
    HostEntity hostEntity = hostDAO.findByName("h1");
    
    Set<HostConfigMapping> targets = hostConfigMappingDAO.findSelected(1L, hostEntity.getHostId());
    Assert.assertEquals("Expected two entities", 2, targets.size());

    coreSiteConfigV1.setSelected(0);
    hostConfigMappingDAO.merge(coreSiteConfigV1);
    
    createEntity(1L, "h1", "core-site", "version2");

    targets = hostConfigMappingDAO.findSelected(1L, hostEntity.getHostId());
    Assert.assertEquals("Expected two entities", 2, targets.size());
  }
  
  @Test
  public void testFindSelectedByType() throws Exception {
    HostConfigMapping entity1 = createEntity(1L, "h1", "global", "version1");
    HostEntity hostEntity = hostDAO.findByName("h1");
    
    HostConfigMapping target = hostConfigMappingDAO.findSelectedByType(1L, hostEntity.getHostId(), "core-site");
    Assert.assertNull("Expected null entity for type 'core-site'", target);
    
    target = hostConfigMappingDAO.findSelectedByType(1L, hostEntity.getHostId(), "global");
    Assert.assertNotNull("Expected non-null entity for type 'global'", target);
    Assert.assertEquals("Expected version to be '" + entity1.getVersion() + "'", entity1.getVersion(), target.getVersion());
    
    target.setSelected(0);
    hostConfigMappingDAO.merge(target);
    
    HostConfigMapping entity2 = createEntity(1L, "h1", "global", "version2");
    
    target = hostConfigMappingDAO.findSelectedByType(1L, hostEntity.getHostId(), "global");
    Assert.assertNotNull("Expected non-null entity for type 'global'", target);
    
    Assert.assertEquals("Expected version to be '" + entity2.getVersion() + "'", entity2.getVersion(), target.getVersion());
    
    Assert.assertEquals("Expected instance equality", entity2, target);
  }
  
  @Test
  public void testEmptyTable() throws Exception {
    createEntity(1L, "h1", "global", "version1");

    HostEntity hostEntity = hostDAO.findByName("h1");
    hostConfigMappingDAO.removeByClusterAndHostName(1L, "h1");
    HostConfigMapping target = hostConfigMappingDAO.findSelectedByType(1L, hostEntity.getHostId(), "core-site");
    
    Assert.assertEquals(null, target);
  }
}
