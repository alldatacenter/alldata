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
import java.util.LinkedList;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutUserWidgetEntity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * WidgetDAO unit tests.
 */
public class WidgetDAOTest {

  private static Injector injector;
  private WidgetDAO widgetDAO;
  private WidgetLayoutDAO widgetLayoutDAO;
  OrmTestHelper helper;
  Long clusterId;


  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    widgetDAO = injector.getInstance(WidgetDAO.class);
    widgetLayoutDAO = injector.getInstance(WidgetLayoutDAO.class);
    injector.getInstance(GuiceJpaInitializer.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();
  }

  private void createRecords() {
    for (int i=0; i<3; i++) {
      WidgetEntity widgetEntity = new WidgetEntity();
      widgetEntity.setAuthor("author");
      widgetEntity.setDefaultSectionName("section_name");
      widgetEntity.setClusterId(clusterId);
      widgetEntity.setMetrics("metrics");
      widgetEntity.setDescription("description");
      widgetEntity.setProperties("{\"warning_threshold\": 0.5,\"error_threshold\": 0.7 }");
      widgetEntity.setScope("CLUSTER");
      widgetEntity.setWidgetName("widget" + i);
      widgetEntity.setWidgetType("GAUGE");
      widgetEntity.setWidgetValues("${`jvmMemoryHeapUsed + jvmMemoryHeapMax`}");
      widgetEntity.setListWidgetLayoutUserWidgetEntity(new LinkedList<>());
      final WidgetLayoutEntity widgetLayoutEntity = new WidgetLayoutEntity();
      widgetLayoutEntity.setClusterId(clusterId);
      widgetLayoutEntity.setLayoutName("layout name" + i);
      widgetLayoutEntity.setSectionName("section" + i%2);
      widgetLayoutEntity.setDisplayName("display_name");
      widgetLayoutEntity.setUserName("user_name");
      widgetLayoutEntity.setScope("CLUSTER");
      final WidgetLayoutUserWidgetEntity widgetLayoutUserWidget = new WidgetLayoutUserWidgetEntity();
      widgetLayoutUserWidget.setWidget(widgetEntity);
      widgetLayoutUserWidget.setWidgetLayout(widgetLayoutEntity);
      widgetLayoutUserWidget.setWidgetOrder(0);

      widgetEntity.getListWidgetLayoutUserWidgetEntity().add(widgetLayoutUserWidget);
      List<WidgetLayoutUserWidgetEntity> widgetLayoutUserWidgetEntityList = new LinkedList<>();
      widgetLayoutUserWidgetEntityList.add(widgetLayoutUserWidget);

      widgetLayoutEntity.setListWidgetLayoutUserWidgetEntity(widgetLayoutUserWidgetEntityList);
      widgetLayoutDAO.create(widgetLayoutEntity);
    }
  }

  @Test
  public void testFindByCluster() {
    createRecords();
    Assert.assertEquals(0, widgetDAO.findByCluster(99999).size());
    Assert.assertEquals(3, widgetDAO.findByCluster(clusterId).size());
  }

  @Test
  public void testFindBySectionName() {
    createRecords();
    Assert.assertEquals(0, widgetDAO.findBySectionName("non existing").size());
    Assert.assertEquals(2, widgetDAO.findBySectionName("section0").size());
    Assert.assertEquals(1, widgetDAO.findBySectionName("section1").size());
  }

  @Test
  public void testFindAll() {
    createRecords();
    Assert.assertEquals(3, widgetDAO.findAll().size());
  }

  @Test
  public void testFindByName() {
    createRecords();
    Assert.assertEquals(1, widgetDAO.findByName(clusterId, "widget0", "author", "section_name").size());
  }

  @After
  public void after() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;
  }
}
