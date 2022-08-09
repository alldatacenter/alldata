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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.internal.SortRequestImpl;
import org.apache.ambari.server.controller.internal.StageResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.SortRequestProperty;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * StageDAO tests.
 */
public class StageDAOTest {

  private Injector injector;
  private StageDAO stageDao;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    stageDao = injector.getInstance(StageDAO.class);

    OrmTestHelper helper     = injector.getInstance(OrmTestHelper.class);
    Long          clusterId  = helper.createCluster();
    RequestDAO    requestDao = injector.getInstance(RequestDAO.class);

    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setClusterId(clusterId);
    requestEntity.setStartTime(1000L);
    requestEntity.setEndTime(1200L);
    requestEntity.setRequestId(99L);
    requestDao.create(requestEntity);

    for (int i = 0; i < 5; i++) {
      StageEntity definition = new StageEntity();
      definition.setClusterId(clusterId);
      definition.setRequestId(99L);
      definition.setStageId((long) (100 + i));
      definition.setLogInfo("log info for " + i);
      definition.setRequestContext("request context for " + i);
      definition.setRequest(requestEntity);
      stageDao.create(definition);
    }

    List<StageEntity> definitions = stageDao.findAll();
    assertNotNull(definitions);
    assertEquals(5, definitions.size());
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;
  }

  /**
   * Tests that the Ambari {@link org.apache.ambari.server.controller.spi.Predicate}
   * can be converted and submitted to JPA correctly to return a restricted result set.
   */
  @Test
  public void testStagePredicate() throws Exception {

    Predicate predicate = new PredicateBuilder().property(
        StageResourceProvider.STAGE_CLUSTER_NAME).equals("c1").toPredicate();

    List<StageEntity> entities = stageDao.findAll(PropertyHelper.getReadRequest(), predicate);
    assertEquals(5, entities.size());

    predicate = new PredicateBuilder().
        property(StageResourceProvider.STAGE_CONTEXT).equals("request context for 3").or().
        property(StageResourceProvider.STAGE_CONTEXT).equals("request context for 4").
        toPredicate();

    entities = stageDao.findAll(PropertyHelper.getReadRequest(), predicate);
    assertEquals(2, entities.size());
  }

  /**
   * Tests that JPA does the sorting work for us.
   */
  @Test
  public void testStageSorting() throws Exception {
    List<SortRequestProperty> sortProperties = new ArrayList<>();
    SortRequest sortRequest = new SortRequestImpl(sortProperties);

    Predicate predicate = new PredicateBuilder().property(
        StageResourceProvider.STAGE_CLUSTER_NAME).equals("c1").toPredicate();


    sortProperties.add(new SortRequestProperty(
        StageResourceProvider.STAGE_LOG_INFO, SortRequest.Order.ASC));

    Request request = PropertyHelper.getReadRequest(new HashSet<>(Arrays.asList()),
        null, null, null, sortRequest);

    // get back all 5
    List<StageEntity> entities = stageDao.findAll(request, predicate);
    assertEquals(5, entities.size());

    // assert sorting ASC
    String lastInfo = null;
    for (StageEntity entity : entities) {
      if (lastInfo == null) {
        lastInfo = entity.getLogInfo();
        continue;
      }

      String currentInfo = entity.getLogInfo();
      assertTrue(lastInfo.compareTo(currentInfo) <= 0);
      lastInfo = currentInfo;
    }

    // clear and do DESC
    sortProperties.clear();
    sortProperties.add(new SortRequestProperty(
        StageResourceProvider.STAGE_LOG_INFO, SortRequest.Order.DESC));

    // get back all 5
    entities = stageDao.findAll(request, predicate);
    assertEquals(5, entities.size());

    // assert sorting DESC
    lastInfo = null;
    for (StageEntity entity : entities) {
      if (null == lastInfo) {
        lastInfo = entity.getLogInfo();
        continue;
      }

      String currentInfo = entity.getLogInfo();
      assertTrue(lastInfo.compareTo(currentInfo) >= 0);
      lastInfo = currentInfo;
    }
  }
}
