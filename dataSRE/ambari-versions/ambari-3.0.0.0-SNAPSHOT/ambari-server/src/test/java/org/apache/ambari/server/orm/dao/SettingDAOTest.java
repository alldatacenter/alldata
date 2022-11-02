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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.SettingEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class SettingDAOTest {
  private  Injector injector;
  private SettingDAO dao;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    dao = injector.getInstance(SettingDAO.class);
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(OrmTestHelper.class).createCluster();
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testCRUD() {
    Map<String, SettingEntity> entities = new HashMap<>();
    //Create
    for (int i=0; i < 3; i++) {
      SettingEntity entity = new SettingEntity();
      entity.setName("motd" + i);
      entity.setContent("test content" + i);
      entity.setUpdatedBy("ambari");
      entity.setSettingType("ambari-server");
      entity.setUpdateTimestamp(System.currentTimeMillis());
      entities.put(entity.getName(), entity);
      dao.create(entity);
    }

    //Retrieve
    retrieveAndValidateSame(entities);
    assertEquals(entities.size(), dao.findAll().size());

    //Should return null if doesn't exist.
    assertNull(dao.findByName("does-not-exist"));


    //Update
    for(Map.Entry<String, SettingEntity> entry : entities.entrySet()) {
      entry.getValue().setContent(Objects.toString(Math.random()));
      dao.merge(entry.getValue());
    }

    retrieveAndValidateSame(entities);
    assertEquals(entities.size(), dao.findAll().size());

    //Delete
    for(Map.Entry<String, SettingEntity> entry : entities.entrySet()) {
      dao.removeByName(entry.getKey());
    }
    assertEquals(0, dao.findAll().size());
  }

  private void retrieveAndValidateSame(Map<String, SettingEntity> entities) {
    for(Map.Entry<String, SettingEntity> entry : entities.entrySet()) {
      String name = entry.getKey();
      assertEquals(entry.getValue(), dao.findByName(name));
    }
  }
}
