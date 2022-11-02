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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockSupport;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Provider;

import junit.framework.Assert;

public class AmbariConfigurationDAOTest extends EasyMockSupport {

  private static final String CATEGORY_NAME = "test-category";
  private static Method methodMerge;
  private static Method methodRemove;
  private static Method methodCreate;
  private static Method methodFindByCategory;

  private static Field fieldEntityManagerProvider;

  @BeforeClass
  public static void beforeKDCKerberosOperationHandlerTest() throws Exception {
    methodMerge = AmbariConfigurationDAO.class.getMethod("merge", AmbariConfigurationEntity.class);
    methodRemove = CrudDAO.class.getMethod("remove", Object.class);
    methodCreate = AmbariConfigurationDAO.class.getMethod("create", AmbariConfigurationEntity.class);
    methodFindByCategory = AmbariConfigurationDAO.class.getMethod("findByCategory", String.class);

    fieldEntityManagerProvider = CrudDAO.class.getDeclaredField("entityManagerProvider");
  }

  @Test
  public void testReconcileCategoryNewCategory() throws Exception {
    Capture<AmbariConfigurationEntity> capturedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(null).once();

    dao.create(capture(capturedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> properties;
    properties = new HashMap<>();
    properties.put("property1", "value1");
    properties.put("property2", "value2");
    dao.reconcileCategory(CATEGORY_NAME, properties, true);

    verifyAll();

    validateCapturedEntities(CATEGORY_NAME, properties, capturedEntities);
  }

  @Test
  public void testReconcileCategoryReplaceCategory() throws Exception {

    Map<String, String> existingProperties;
    existingProperties = new HashMap<>();
    existingProperties.put("property1", "value1");
    existingProperties.put("property2", "value2");

    Capture<AmbariConfigurationEntity> capturedCreatedEntities = newCapture(CaptureType.ALL);
    Capture<AmbariConfigurationEntity> capturedRemovedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(toEntities(CATEGORY_NAME, existingProperties)).once();

    dao.remove(capture(capturedRemovedEntities));
    expectLastCall().anyTimes();

    dao.create(capture(capturedCreatedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> newProperties;
    newProperties = new HashMap<>();
    newProperties.put("property1_new", "value1");
    newProperties.put("property2_new", "value2");
    dao.reconcileCategory(CATEGORY_NAME, newProperties, true);

    verifyAll();

    validateCapturedEntities(CATEGORY_NAME, newProperties, capturedCreatedEntities);
    validateCapturedEntities(CATEGORY_NAME, existingProperties, capturedRemovedEntities);
  }

  @Test
  public void testReconcileCategoryUpdateCategoryKeepNotSpecified() throws Exception {

    Map<String, String> existingProperties;
    existingProperties = new HashMap<>();
    existingProperties.put("property1", "value1");
    existingProperties.put("property2", "value2");

    Capture<AmbariConfigurationEntity> capturedCreatedEntities = newCapture(CaptureType.ALL);
    Capture<AmbariConfigurationEntity> capturedMergedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(toEntities(CATEGORY_NAME, existingProperties)).once();

    expect(dao.merge(capture(capturedMergedEntities))).andReturn(createNiceMock(AmbariConfigurationEntity.class)).anyTimes();

    dao.create(capture(capturedCreatedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> newProperties;
    newProperties = new HashMap<>();
    newProperties.put("property1", "new_value1");
    newProperties.put("property2_new", "value2");
    newProperties.put("property3", "value3");
    dao.reconcileCategory(CATEGORY_NAME, newProperties, false);

    verifyAll();

    Map<String, String> expectedProperties;

    expectedProperties = new HashMap<>();
    expectedProperties.put("property2_new", "value2");
    expectedProperties.put("property3", "value3");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedCreatedEntities);

    expectedProperties = new HashMap<>();
    expectedProperties.put("property1", "new_value1");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedMergedEntities);
  }

  @Test
  public void testReconcileCategoryUpdateCategoryRemoveNotSpecified() throws Exception {

    Map<String, String> existingProperties;
    existingProperties = new HashMap<>();
    existingProperties.put("property1", "value1");
    existingProperties.put("property2", "value2");

    Capture<AmbariConfigurationEntity> capturedCreatedEntities = newCapture(CaptureType.ALL);
    Capture<AmbariConfigurationEntity> capturedRemovedEntities = newCapture(CaptureType.ALL);
    Capture<AmbariConfigurationEntity> capturedMergedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(toEntities(CATEGORY_NAME, existingProperties)).once();

    expect(dao.merge(capture(capturedMergedEntities))).andReturn(createNiceMock(AmbariConfigurationEntity.class)).anyTimes();

    dao.remove(capture(capturedRemovedEntities));
    expectLastCall().anyTimes();

    dao.create(capture(capturedCreatedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> newProperties;
    newProperties = new HashMap<>();
    newProperties.put("property1", "new_value1");
    newProperties.put("property2_new", "value2");
    newProperties.put("property3", "value3");
    dao.reconcileCategory(CATEGORY_NAME, newProperties, true);

    verifyAll();

    Map<String, String> expectedProperties;

    expectedProperties = new HashMap<>();
    expectedProperties.put("property2_new", "value2");
    expectedProperties.put("property3", "value3");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedCreatedEntities);

    expectedProperties = new HashMap<>();
    expectedProperties.put("property2", "value2");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedRemovedEntities);

    expectedProperties = new HashMap<>();
    expectedProperties.put("property1", "new_value1");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedMergedEntities);
  }

  @Test
  public void testReconcileCategoryAppendCategory() throws Exception {

    Map<String, String> existingProperties;
    existingProperties = new HashMap<>();
    existingProperties.put("property1", "value1");
    existingProperties.put("property2", "value2");

    Capture<AmbariConfigurationEntity> capturedCreatedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(toEntities(CATEGORY_NAME, existingProperties)).once();

    dao.create(capture(capturedCreatedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> newProperties;
    newProperties = new HashMap<>();
    newProperties.put("property3", "value3");
    newProperties.put("property4", "value3");
    dao.reconcileCategory(CATEGORY_NAME, newProperties, false);

    verifyAll();

    validateCapturedEntities(CATEGORY_NAME, newProperties, capturedCreatedEntities);
  }

  private AmbariConfigurationDAO createDao() throws IllegalAccessException {
    AmbariConfigurationDAO dao = createMockBuilder(AmbariConfigurationDAO.class)
        .addMockedMethods(methodMerge, methodRemove, methodCreate, methodFindByCategory)
        .createMock();

    EntityManager entityManager = createMock(EntityManager.class);
    entityManager.flush();
    expectLastCall().anyTimes();

    Provider<EntityManager> entityManagerProvider = createMock(Provider.class);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();

    fieldEntityManagerProvider.set(dao, entityManagerProvider);

    return dao;
  }

  private List<AmbariConfigurationEntity> toEntities(String categoryName, Map<String, String> properties) {
    List<AmbariConfigurationEntity> entities = new ArrayList<>();

    for (Map.Entry<String, String> property : properties.entrySet()) {
      AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
      entity.setCategoryName(categoryName);
      entity.setPropertyName(property.getKey());
      entity.setPropertyValue(property.getValue());
      entities.add(entity);
    }

    return entities;
  }

  private void validateCapturedEntities(String expectedCategoryName, Map<String, String> expectedProperties, Capture<AmbariConfigurationEntity> capturedEntities) {
    Assert.assertTrue(capturedEntities.hasCaptured());

    List<AmbariConfigurationEntity> entities = capturedEntities.getValues();
    Assert.assertNotNull(entities);

    Map<String, String> capturedProperties = new TreeMap<>();
    for (AmbariConfigurationEntity entity : entities) {
      Assert.assertEquals(expectedCategoryName, entity.getCategoryName());
      capturedProperties.put(entity.getPropertyName(), entity.getPropertyValue());
    }

    // Convert the Map to a TreeMap to help with comparisons
    expectedProperties = new TreeMap<>(expectedProperties);
    Assert.assertEquals(expectedProperties, capturedProperties);
  }

}