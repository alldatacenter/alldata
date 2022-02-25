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
 * WidgetLayoutDAO unit tests.
 */
public class WidgetLayoutDAOTest {

  private static Injector injector;
  private WidgetLayoutDAO widgetLayoutDAO;
  private WidgetDAO widgetDAO;
  OrmTestHelper helper;
  Long clusterId;


  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    widgetLayoutDAO = injector.getInstance(WidgetLayoutDAO.class);
    widgetDAO = injector.getInstance(WidgetDAO.class);
    injector.getInstance(GuiceJpaInitializer.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();
  }

  private void createRecords() {

    WidgetLayoutEntity widgetLayoutEntity = new WidgetLayoutEntity();
    widgetLayoutEntity.setClusterId(clusterId);
    widgetLayoutEntity.setLayoutName("layout name0");
    widgetLayoutEntity.setSectionName("section0");
    widgetLayoutEntity.setUserName("username");
    widgetLayoutEntity.setScope("CLUSTER");
    widgetLayoutEntity.setDisplayName("displ_name");

    WidgetLayoutEntity widgetLayoutEntity2 = new WidgetLayoutEntity();
    widgetLayoutEntity2.setClusterId(clusterId);
    widgetLayoutEntity2.setLayoutName("layout name1");
    widgetLayoutEntity2.setSectionName("section1");
    widgetLayoutEntity2.setUserName("username");
    widgetLayoutEntity2.setScope("CLUSTER");
    widgetLayoutEntity2.setDisplayName("displ_name2");

    List<WidgetLayoutUserWidgetEntity> widgetLayoutUserWidgetEntityList = new LinkedList<>();

    for (int i=0; i<3; i++) {
      WidgetEntity widgetEntity = new WidgetEntity();
      widgetEntity.setDefaultSectionName("display name" + i);
      widgetEntity.setAuthor("author");
      widgetEntity.setClusterId(clusterId);
      widgetEntity.setMetrics("metrics");
      widgetEntity.setDescription("description");
      widgetEntity.setProperties("{\"warning_threshold\": 0.5,\"error_threshold\": 0.7 }");
      widgetEntity.setScope("CLUSTER");
      widgetEntity.setWidgetName("widget" + i);
      widgetEntity.setWidgetType("GAUGE");
      widgetEntity.setWidgetValues("${`jvmMemoryHeapUsed + jvmMemoryHeapMax`}");

      WidgetLayoutUserWidgetEntity widgetLayoutUserWidget = new WidgetLayoutUserWidgetEntity();
      widgetLayoutUserWidget.setWidget(widgetEntity);
      widgetLayoutUserWidget.setWidgetLayout(widgetLayoutEntity);
      widgetLayoutUserWidget.setWidgetOrder(0);
      widgetLayoutUserWidgetEntityList.add(widgetLayoutUserWidget);
    }

    widgetLayoutEntity.setListWidgetLayoutUserWidgetEntity(widgetLayoutUserWidgetEntityList);
    widgetLayoutDAO.create(widgetLayoutEntity);
    widgetLayoutDAO.create(widgetLayoutEntity2);

  }

  @Test
  public void testFindByCluster() {
    createRecords();
    Assert.assertEquals(0, widgetLayoutDAO.findByCluster(99999).size());
    Assert.assertEquals(2, widgetLayoutDAO.findByCluster(clusterId).size());
  }

  @Test
  public void testFindBySectionName() {
    createRecords();
    Assert.assertEquals(0, widgetLayoutDAO.findBySectionName("non existing").size());
    List<WidgetLayoutEntity> widgetLayoutEntityList1 =  widgetLayoutDAO.findBySectionName("section0");
    List<WidgetLayoutEntity> widgetLayoutEntityList2 =  widgetLayoutDAO.findBySectionName("section1");

    Assert.assertEquals(1, widgetLayoutEntityList1.size());
    Assert.assertEquals(1, widgetLayoutEntityList2.size());
    Assert.assertEquals(3, widgetLayoutEntityList1.get(0).getListWidgetLayoutUserWidgetEntity().size());
  }

  @Test
  public void testFindAll() {
    createRecords();
    Assert.assertEquals(2, widgetLayoutDAO.findAll().size());
  }

  @After
  public void after() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;
  }
}
