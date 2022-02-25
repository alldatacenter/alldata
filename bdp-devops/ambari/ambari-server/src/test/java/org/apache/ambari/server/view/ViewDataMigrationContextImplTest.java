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

package org.apache.ambari.server.view;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.configuration.EntityConfig;
import org.apache.ambari.server.view.configuration.PersistenceConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.migration.EntityConverter;
import org.easymock.Capture;
import org.junit.Test;

import junit.framework.Assert;

/**
 * ViewDataMigrationContextImpl tests.
 */
public class ViewDataMigrationContextImplTest {

  public static final String VERSION_1 = "1.0.0";
  public static final String VERSION_2 = "2.0.0";
  public static final String INSTANCE = "INSTANCE_1";
  public static final String VIEW_NAME = "MY_VIEW";

  @Test
  public void getDataVersion() throws Exception {
    ViewEntity entity1 = getViewEntityMock(VERSION_1);
    ViewEntity entity2 = getViewEntityMock(VERSION_2);

    ViewConfig config1 = createNiceMock(ViewConfig.class);
    expect(config1.getDataVersion()).andReturn(41);
    ViewConfig config2 = createNiceMock(ViewConfig.class);
    expect(config2.getDataVersion()).andReturn(42);
    replay(config1, config2);

    expect(entity1.getConfiguration()).andReturn(config1);
    expect(entity2.getConfiguration()).andReturn(config2);
    replay(entity1, entity2);

    ViewInstanceEntity instanceEntity1 = getViewInstanceEntityMock(entity1);
    ViewInstanceEntity instanceEntity2 = getViewInstanceEntityMock(entity2);

    replay(instanceEntity1, instanceEntity2);

    ViewDataMigrationContextImpl context = new TestViewDataMigrationContextImpl(instanceEntity1, instanceEntity2);

    Assert.assertEquals(41, context.getOriginDataVersion());
    Assert.assertEquals(42, context.getCurrentDataVersion());
  }

  @Test
  public void getDataStore() throws Exception {
    ViewEntity entity1 = getViewEntityMock(VERSION_1);
    ViewEntity entity2 = getViewEntityMock(VERSION_2);
    replay(entity1, entity2);

    ViewInstanceEntity instanceEntity1 = getViewInstanceEntityMock(entity1);
    ViewInstanceEntity instanceEntity2 = getViewInstanceEntityMock(entity2);
    replay(instanceEntity1, instanceEntity2);

    ViewDataMigrationContextImpl context = new TestViewDataMigrationContextImpl(instanceEntity1, instanceEntity2);

    Assert.assertNotSame(context.getCurrentDataStore(), context.getOriginDataStore());
  }

  @Test
  public void putCurrentInstanceData() throws Exception {
    ViewEntity entity1 = getViewEntityMock(VERSION_1);
    ViewEntity entity2 = getViewEntityMock(VERSION_2);
    replay(entity1, entity2);

    ViewInstanceEntity instanceEntity1 = getViewInstanceEntityMock(entity1);
    List<ViewInstanceDataEntity> data1 = new ArrayList<>();
    expect(instanceEntity1.getData()).andReturn(data1).anyTimes();
    ViewInstanceEntity instanceEntity2 = getViewInstanceEntityMock(entity2);
    List<ViewInstanceDataEntity> data2 = new ArrayList<>();
    expect(instanceEntity2.getData()).andReturn(data2).anyTimes();
    replay(instanceEntity1, instanceEntity2);

    ViewDataMigrationContextImpl context = new TestViewDataMigrationContextImpl(instanceEntity1, instanceEntity2);
    context.putOriginInstanceData("user1", "key1", "val1");
    context.putCurrentInstanceData("user2", "key2", "val2");

    Assert.assertEquals(1, data1.size());
    Assert.assertEquals("user1", data1.get(0).getUser());
    Assert.assertEquals("key1", data1.get(0).getName());
    Assert.assertEquals("val1", data1.get(0).getValue());

    Assert.assertEquals(1, data2.size());
    Assert.assertEquals("user2", data2.get(0).getUser());
    Assert.assertEquals("key2", data2.get(0).getName());
    Assert.assertEquals("val2", data2.get(0).getValue());
  }

  @Test
  public void copyAllObjects() throws Exception {
    ViewEntity entity1 = getViewEntityMock(VERSION_1);
    ViewEntity entity2 = getViewEntityMock(VERSION_2);
    replay(entity1, entity2);

    ViewInstanceEntity instanceEntity1 = getViewInstanceEntityMock(entity1);
    ViewInstanceEntity instanceEntity2 = getViewInstanceEntityMock(entity2);
    replay(instanceEntity1, instanceEntity2);

    TestViewDataMigrationContextImpl context = new TestViewDataMigrationContextImpl(instanceEntity1, instanceEntity2);

    DataStore dataStore1 = createStrictMock(DataStore.class);
    expect(dataStore1.findAll(eq(SampleEntity.class), (String) isNull())).andReturn(
        Arrays.asList(new SampleEntity("data1"), new SampleEntity("data2")));
    replay(dataStore1);

    DataStore dataStore2 = createStrictMock(DataStore.class);
    Capture<SampleEntity> copiedEntity1 = Capture.newInstance();
    Capture<SampleEntity> copiedEntity2 = Capture.newInstance();

    dataStore2.store(capture(copiedEntity1)); expectLastCall();
    dataStore2.store(capture(copiedEntity2)); expectLastCall();
    replay(dataStore2);
    context.setMockOriginDataStore(dataStore1);
    context.setMockCurrentDataStore(dataStore2);

    context.copyAllObjects(SampleEntity.class, SampleEntity.class);

    verify(dataStore1);
    verify(dataStore2);

    Assert.assertEquals("data1", copiedEntity1.getValue().getField());
    Assert.assertEquals("data2", copiedEntity2.getValue().getField());
  }

  @Test
  public void copyAllObjectsWithCustomConverter() throws Exception {
    ViewEntity entity1 = getViewEntityMock(VERSION_1);
    ViewEntity entity2 = getViewEntityMock(VERSION_2);
    replay(entity1, entity2);

    ViewInstanceEntity instanceEntity1 = getViewInstanceEntityMock(entity1);
    ViewInstanceEntity instanceEntity2 = getViewInstanceEntityMock(entity2);
    replay(instanceEntity1, instanceEntity2);

    TestViewDataMigrationContextImpl context = new TestViewDataMigrationContextImpl(instanceEntity1, instanceEntity2);

    DataStore dataStore1 = createStrictMock(DataStore.class);
    SampleEntity sampleEntity1 = new SampleEntity("data1");
    SampleEntity sampleEntity2 = new SampleEntity("data2");
    expect(dataStore1.findAll(eq(SampleEntity.class), (String) isNull())).andReturn(
        Arrays.asList(sampleEntity1, sampleEntity2));
    replay(dataStore1);

    DataStore dataStore2 = createStrictMock(DataStore.class);
    Capture<SampleEntity> copiedEntity1 = Capture.newInstance();
    Capture<SampleEntity> copiedEntity2 = Capture.newInstance();

    Capture<SampleEntity> convertedEntity1 = Capture.newInstance();
    Capture<SampleEntity> convertedEntity2 = Capture.newInstance();

    dataStore2.store(capture(copiedEntity1)); expectLastCall();
    dataStore2.store(capture(copiedEntity2)); expectLastCall();
    replay(dataStore2);
    context.setMockOriginDataStore(dataStore1);
    context.setMockCurrentDataStore(dataStore2);

    EntityConverter converter = createStrictMock(EntityConverter.class);
    converter.convert(eq(sampleEntity1), capture(convertedEntity1)); expectLastCall();
    converter.convert(eq(sampleEntity2), capture(convertedEntity2)); expectLastCall();
    replay(converter);

    context.copyAllObjects(SampleEntity.class, SampleEntity.class, converter);

    verify(dataStore1);
    verify(dataStore2);
    verify(converter);
    Assert.assertSame(copiedEntity1.getValue(), convertedEntity1.getValue());
    Assert.assertSame(copiedEntity2.getValue(), convertedEntity2.getValue());
  }

  @Test
  public void copyAllInstanceData() throws Exception {
    ViewEntity entity1 = getViewEntityMock(VERSION_1);
    ViewEntity entity2 = getViewEntityMock(VERSION_2);
    replay(entity1, entity2);

    ViewInstanceDataEntity dataEntity = new ViewInstanceDataEntity();
    dataEntity.setName("name1");
    dataEntity.setValue("value1");
    dataEntity.setUser("user1");
    Collection<ViewInstanceDataEntity> data1 = Arrays.asList(dataEntity);
    List<ViewInstanceDataEntity> data2 = new ArrayList<>();

    ViewInstanceEntity instanceEntity1 = getViewInstanceEntityMock(entity1);
    expect(instanceEntity1.getData()).andReturn(data1).anyTimes();
    ViewInstanceEntity instanceEntity2 = getViewInstanceEntityMock(entity2);
    expect(instanceEntity2.getData()).andReturn(data2).anyTimes();
    replay(instanceEntity1, instanceEntity2);

    ViewDataMigrationContextImpl context = new TestViewDataMigrationContextImpl(instanceEntity1, instanceEntity2);
    context.copyAllInstanceData();

    Assert.assertEquals(1, data2.size());
    Assert.assertEquals("user1", data2.get(0).getUser());
    Assert.assertEquals("name1", data2.get(0).getName());
    Assert.assertEquals("value1", data2.get(0).getValue());
  }

  @Test
  public void getEntityClasses() throws Exception {
    ViewEntity entity1 = getViewEntityMock(VERSION_1);
    ViewEntity entity2 = getViewEntityMock(VERSION_2);

    EntityConfig entityConfig = createNiceMock(EntityConfig.class);
    expect(entityConfig.getClassName()).andReturn(SampleEntity.class.getName()).anyTimes();
    replay(entityConfig);

    PersistenceConfig persistenceConfig1 = createStrictMock(PersistenceConfig.class);
    expect(persistenceConfig1.getEntities()).andReturn(Arrays.asList(entityConfig));
    PersistenceConfig persistenceConfig2 = createStrictMock(PersistenceConfig.class);
    expect(persistenceConfig2.getEntities()).andReturn(Arrays.asList(entityConfig));
    replay(persistenceConfig1, persistenceConfig2);

    ViewConfig config1 = createNiceMock(ViewConfig.class);
    expect(config1.getPersistence()).andReturn(persistenceConfig1);
    ViewConfig config2 = createNiceMock(ViewConfig.class);
    expect(config2.getPersistence()).andReturn(persistenceConfig2);
    replay(config1, config2);

    expect(entity1.getConfiguration()).andReturn(config1);
    expect(entity2.getConfiguration()).andReturn(config2);
    replay(entity1, entity2);

    ViewInstanceEntity instanceEntity1 = getViewInstanceEntityMock(entity1);
    ViewInstanceEntity instanceEntity2 = getViewInstanceEntityMock(entity2);

    replay(instanceEntity1, instanceEntity2);

    ViewDataMigrationContextImpl context = new TestViewDataMigrationContextImpl(instanceEntity1, instanceEntity2);

    Map<String, Class> current = context.getCurrentEntityClasses();
    Assert.assertEquals(1, current.size());
    Assert.assertEquals(SampleEntity.class.getName(), current.entrySet().iterator().next().getKey());
    Assert.assertEquals(SampleEntity.class, current.entrySet().iterator().next().getValue());

    Map<String, Class> origin = context.getOriginEntityClasses();
    Assert.assertEquals(1, origin.size());
    Assert.assertEquals(SampleEntity.class.getName(), origin.entrySet().iterator().next().getKey());
    Assert.assertEquals(SampleEntity.class, origin.entrySet().iterator().next().getValue());
  }

  @Test
  public void getInstanceDataByUser() throws Exception {
    ViewEntity entity1 = getViewEntityMock(VERSION_1);
    ViewEntity entity2 = getViewEntityMock(VERSION_2);
    replay(entity1, entity2);

    ViewInstanceDataEntity dataEntityUser1 = new ViewInstanceDataEntity();
    dataEntityUser1.setName("key1");
    dataEntityUser1.setUser("user1");
    ViewInstanceDataEntity dataEntityUser2 = new ViewInstanceDataEntity();
    dataEntityUser2.setName("key1");
    dataEntityUser2.setUser("user2");
    ViewInstanceDataEntity dataEntity2User2 = new ViewInstanceDataEntity();
    dataEntity2User2.setName("key2");
    dataEntity2User2.setUser("user2");
    Collection<ViewInstanceDataEntity> data2 = Arrays.asList(dataEntityUser2, dataEntity2User2);
    Collection<ViewInstanceDataEntity> data1 = Arrays.asList(dataEntityUser1, dataEntityUser2, dataEntity2User2);

    ViewInstanceEntity instanceEntity1 = getViewInstanceEntityMock(entity1);
    expect(instanceEntity1.getData()).andReturn(data1);
    ViewInstanceEntity instanceEntity2 = getViewInstanceEntityMock(entity2);
    expect(instanceEntity2.getData()).andReturn(data2);
    replay(instanceEntity1, instanceEntity2);

    ViewDataMigrationContextImpl context = new TestViewDataMigrationContextImpl(instanceEntity1, instanceEntity2);
    Map<String, Map<String,String>> instanceData2 = context.getCurrentInstanceDataByUser();
    Assert.assertEquals(1, instanceData2.size());
    Assert.assertEquals(2, instanceData2.get("user2").size());

    Map<String, Map<String,String>> instanceData1 = context.getOriginInstanceDataByUser();
    Assert.assertEquals(2, instanceData1.size());
    Assert.assertEquals(1, instanceData1.get("user1").size());
    Assert.assertEquals(2, instanceData1.get("user2").size());
  }

  private ViewInstanceEntity getViewInstanceEntityMock(ViewEntity viewEntity) {
    ViewInstanceEntity instanceEntity = createNiceMock(ViewInstanceEntity.class);
    expect(instanceEntity.getViewEntity()).andReturn(viewEntity).anyTimes();
    expect(instanceEntity.getViewName()).andReturn(VIEW_NAME).anyTimes();
    expect(instanceEntity.getInstanceName()).andReturn(INSTANCE).anyTimes();
    return instanceEntity;
  }

  private ViewEntity getViewEntityMock(String version) {
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    expect(viewEntity.getViewName()).andReturn(VIEW_NAME).anyTimes();
    expect(viewEntity.getVersion()).andReturn(version).anyTimes();
    expect(viewEntity.getClassLoader()).andReturn(getClass().getClassLoader()).anyTimes();
    return viewEntity;
  }

  //Avoid accessing DB
  private static class TestViewDataMigrationContextImpl extends ViewDataMigrationContextImpl {
    private DataStore mockOriginDataStore;
    private DataStore mockCurrentDataStore;

    public TestViewDataMigrationContextImpl(ViewInstanceEntity originInstanceDefinition,
                                            ViewInstanceEntity currentInstanceDefinition) {
      super(originInstanceDefinition, currentInstanceDefinition);
    }

    @Override
    protected DataStore getDataStore(ViewInstanceEntity instanceDefinition) {
      if (instanceDefinition.getViewEntity().getVersion().equals(VERSION_1)) {
        if (mockOriginDataStore == null) {
          return createDataStoreMock();
        }
        return mockOriginDataStore;
      }

      if (instanceDefinition.getViewEntity().getVersion().equals(VERSION_2)) {
        if (mockCurrentDataStore == null) {
          return createDataStoreMock();
        }
        return mockCurrentDataStore;
      }
      return null;
    }

    private DataStore createDataStoreMock() {
      DataStore dataStoreMock = createNiceMock(DataStore.class);
      replay(dataStoreMock);
      return dataStoreMock;
    }

    public DataStore getMockOriginDataStore() {
      return mockOriginDataStore;
    }

    public void setMockOriginDataStore(DataStore mockOriginDataStore) {
      this.mockOriginDataStore = mockOriginDataStore;
    }

    public DataStore getMockCurrentDataStore() {
      return mockCurrentDataStore;
    }

    public void setMockCurrentDataStore(DataStore mockCurrentDataStore) {
      this.mockCurrentDataStore = mockCurrentDataStore;
    }
  }

  private static class SampleEntity {
    private String field;

    public SampleEntity() {
    }

    public SampleEntity(String field) {
      this.field = field;
    }

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }
  }
}
