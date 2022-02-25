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

package org.apache.ambari.server.view.persistence;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.configuration.EntityConfig;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.apache.ambari.view.PersistenceException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.jpa.JpaHelper;
import org.eclipse.persistence.jpa.dynamic.JPADynamicHelper;
import org.eclipse.persistence.sequencing.Sequence;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.sessions.server.ServerSession;
import org.eclipse.persistence.tools.schemaframework.SchemaManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * DataStoreImpl tests.
 */
@RunWith(PowerMockRunner.class)               // Allow mocking static methods
@PrepareForTest(JpaHelper.class)
public class DataStoreImplTest {
  private final static String xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "    </instance>\n" +
      "    <persistence>\n" +
      "      <entity>\n" +
      "        <class>org.apache.ambari.server.view.persistence.DataStoreImplTest$TestEntity</class>\n" +
      "        <id-property>id</id-property>\n" +
      "      </entity>\n" +
      "      <entity>\n" +
      "        <class>org.apache.ambari.server.view.persistence.DataStoreImplTest$TestSubEntity</class>\n" +
      "        <id-property>id</id-property>\n" +
      "      </entity>\n" +
      "    </persistence>" +
      "</view>";

  @Test
  public void testStore_create() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    JpaEntityManager jpaEntityManager = createMock(JpaEntityManager.class);
    ServerSession session = createMock(ServerSession.class);
    DatabaseLogin databaseLogin = createMock(DatabaseLogin.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);

    // set expectations
    PowerMock.mockStatic(JpaHelper.class);
    expect(JpaHelper.getEntityManager(entityManager)).andReturn(jpaEntityManager).anyTimes();
    PowerMock.replay(JpaHelper.class);
    expect(jpaEntityManager.getServerSession()).andReturn(session).anyTimes();
    expect(session.getLogin()).andReturn(databaseLogin).anyTimes();
    Capture<Sequence> sequenceCapture = EasyMock.newCapture();
    databaseLogin.addSequence(capture(sequenceCapture));
    EasyMock.expectLastCall().anyTimes();

    Capture<DynamicEntity> entityCapture = EasyMock.newCapture();
    entityManager.persist(capture(entityCapture));
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        ((DynamicEntity) EasyMock.getCurrentArguments()[0])
            .set("DS_id", 99); // for TestSubEntity
        return null;
      }
    });

    Capture<DynamicEntity> entityCapture2 = EasyMock.newCapture();
    entityManager.persist(capture(entityCapture2));
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        ((DynamicEntity) EasyMock.getCurrentArguments()[0])
            .set("DS_id", 100); // for TestEntity
        return null;
      }
    });

    Capture<DynamicType> typeCapture = EasyMock.newCapture();
    Capture<DynamicType> typeCapture2 = EasyMock.newCapture();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager).anyTimes();
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    entityManager.close();

    transaction.begin();
    transaction.commit();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, jpaEntityManager, session, databaseLogin);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    dataStore.store(new TestEntity("foo", new TestSubEntity("bar")));

    Assert.assertEquals("bar", entityCapture.getValue().get("DS_name"));
    Assert.assertEquals(new Integer(99), entityCapture.getValue().get("DS_id"));

    Assert.assertEquals(new Integer(100), entityCapture2.getValue().get("DS_id"));
    Assert.assertEquals("foo", entityCapture2.getValue().get("DS_name"));

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, jpaEntityManager, session, databaseLogin);
  }

  @Test
  public void testStore_create_longStringValue() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    JpaEntityManager jpaEntityManager = createMock(JpaEntityManager.class);
    ServerSession session = createMock(ServerSession.class);
    DatabaseLogin databaseLogin = createMock(DatabaseLogin.class);
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);

    // set expectations
    PowerMock.mockStatic(JpaHelper.class);
    expect(JpaHelper.getEntityManager(entityManager)).andReturn(jpaEntityManager).anyTimes();
    PowerMock.replay(JpaHelper.class);
    expect(jpaEntityManager.getServerSession()).andReturn(session).anyTimes();
    expect(session.getLogin()).andReturn(databaseLogin).anyTimes();
    Capture<Sequence> sequenceCapture = EasyMock.newCapture();
    databaseLogin.addSequence(capture(sequenceCapture));
    EasyMock.expectLastCall().anyTimes();

    Capture<DynamicType> typeCapture = EasyMock.newCapture();
    Capture<DynamicType> typeCapture2 = EasyMock.newCapture();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager).anyTimes();
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    entityManager.close();

    transaction.begin();
    expect(transaction.isActive()).andReturn(true);
    transaction.rollback();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, jpaEntityManager, session, databaseLogin);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 5000; ++i) {
      sb.append("A");
    }
    String longString = sb.toString();

    try {
      dataStore.store(new TestEntity(longString, new TestSubEntity("bar")));
      Assert.fail("Expected PersistenceException.");
    } catch (PersistenceException e) {
      // expected
    }
    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, jpaEntityManager, session, databaseLogin);
  }

  @Test
  public void testStore_create_largeEntity() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    JpaEntityManager jpaEntityManager = createMock(JpaEntityManager.class);
    ServerSession session = createMock(ServerSession.class);
    DatabaseLogin databaseLogin = createMock(DatabaseLogin.class);

    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);

    // set expectations
    PowerMock.mockStatic(JpaHelper.class);
    expect(JpaHelper.getEntityManager(entityManager)).andReturn(jpaEntityManager).anyTimes();
    PowerMock.replay(JpaHelper.class);
    expect(jpaEntityManager.getServerSession()).andReturn(session).anyTimes();
    expect(session.getLogin()).andReturn(databaseLogin).anyTimes();
    Capture<Sequence> sequenceCapture = EasyMock.newCapture();
    databaseLogin.addSequence(capture(sequenceCapture));
    EasyMock.expectLastCall().anyTimes();

    Capture<DynamicType> typeCapture = EasyMock.newCapture();
    Capture<DynamicType> typeCapture2 = EasyMock.newCapture();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager).anyTimes();
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    entityManager.close();

    transaction.begin();
    expect(transaction.isActive()).andReturn(true);
    transaction.rollback();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, jpaEntityManager, session, databaseLogin);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    try {
      dataStore.store(new TestLargeEntity());
      Assert.fail("Expected PersistenceException.");
    } catch (PersistenceException e) {
      // expected
    }
    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, jpaEntityManager, session, databaseLogin);
  }

  @Test
  public void testStore_update() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    JpaEntityManager jpaEntityManager = createMock(JpaEntityManager.class);
    ServerSession session = createMock(ServerSession.class);
    DatabaseLogin databaseLogin = createMock(DatabaseLogin.class);
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);
    DynamicEntity dynamicEntity = createMock(DynamicEntity.class);
    DynamicEntity dynamicSubEntity = createMock(DynamicEntity.class);

    // set expectations
    PowerMock.mockStatic(JpaHelper.class);
    expect(JpaHelper.getEntityManager(entityManager)).andReturn(jpaEntityManager).anyTimes();
    PowerMock.replay(JpaHelper.class);
    expect(jpaEntityManager.getServerSession()).andReturn(session).anyTimes();
    expect(session.getLogin()).andReturn(databaseLogin).anyTimes();
    Capture<Sequence> sequenceCapture = EasyMock.newCapture();
    databaseLogin.addSequence(capture(sequenceCapture));
    EasyMock.expectLastCall().anyTimes();

    Capture<DynamicType> typeCapture = EasyMock.newCapture();
    Capture<DynamicType> typeCapture2 = EasyMock.newCapture();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager).anyTimes();
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    Capture<Class> entityClassCapture = EasyMock.newCapture();
    expect(entityManager.find(capture(entityClassCapture), eq(100))).andReturn(dynamicEntity);

    Capture<Class> entityClassCapture2 = EasyMock.newCapture();
    expect(entityManager.find(capture(entityClassCapture2), eq(99))).andReturn(dynamicSubEntity);

    entityManager.close();

    expect(dynamicEntity.set("DS_id", 100)).andReturn(dynamicEntity);
    expect(dynamicEntity.set("DS_name", "foo")).andReturn(dynamicEntity);

    expect(dynamicSubEntity.set("DS_id", 99)).andReturn(dynamicSubEntity);
    expect(dynamicSubEntity.set("DS_name", "bar")).andReturn(dynamicSubEntity);

    Capture<DynamicEntity> subEntityCapture = EasyMock.newCapture();
    expect(dynamicEntity.set(eq("DS_subEntity"), capture(subEntityCapture))).andReturn(dynamicSubEntity);

    expect(dynamicEntity.get("DS_id")).andReturn(100);
    expect(dynamicEntity.get("DS_name")).andReturn("foo");
    expect(dynamicEntity.get("DS_subEntity")).andReturn(dynamicSubEntity);
    expect(dynamicEntity.get("DS_class")).andReturn(dynamicEntity.getClass());

    expect(dynamicSubEntity.get("DS_id")).andReturn(99);
    expect(dynamicSubEntity.get("DS_name")).andReturn("bar");

    transaction.begin();
    transaction.commit();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity, jpaEntityManager, session, databaseLogin, dynamicSubEntity);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    dataStore.store(new TestEntity(100, "foo", new TestSubEntity(99, "bar")));

    if ((entityClassCapture.getValue() != typeCapture.getValue().getJavaClass()) &&
        (entityClassCapture.getValue() != typeCapture2.getValue().getJavaClass())) {
      Assert.fail();
    }
    if ((entityClassCapture2.getValue() != typeCapture.getValue().getJavaClass()) &&
        (entityClassCapture2.getValue() != typeCapture2.getValue().getJavaClass())) {
      Assert.fail();
    }

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity, jpaEntityManager, session, databaseLogin, dynamicSubEntity);
  }

  @Test
  public void testStore_update_longStringValue() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    JpaEntityManager jpaEntityManager = createMock(JpaEntityManager.class);
    ServerSession session = createMock(ServerSession.class);
    DatabaseLogin databaseLogin = createMock(DatabaseLogin.class);
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);
    DynamicEntity dynamicEntity = createMock(DynamicEntity.class);

    // set expectations
    PowerMock.mockStatic(JpaHelper.class);
    expect(JpaHelper.getEntityManager(entityManager)).andReturn(jpaEntityManager).anyTimes();
    PowerMock.replay(JpaHelper.class);
    expect(jpaEntityManager.getServerSession()).andReturn(session).anyTimes();
    expect(session.getLogin()).andReturn(databaseLogin).anyTimes();
    Capture<Sequence> sequenceCapture = EasyMock.newCapture();
    databaseLogin.addSequence(capture(sequenceCapture));
    EasyMock.expectLastCall().anyTimes();

    Capture<DynamicType> typeCapture = EasyMock.newCapture();
    Capture<DynamicType> typeCapture2 = EasyMock.newCapture();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager).anyTimes();
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    Capture<Class> entityClassCapture2 = EasyMock.newCapture();
    expect(entityManager.find(capture(entityClassCapture2), eq(99))).andReturn(dynamicEntity);

    entityManager.close();

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 5000; ++i) {
      sb.append("A");
    }
    String longString = sb.toString();

    expect(dynamicEntity.set("DS_id", 99)).andReturn(dynamicEntity).anyTimes();

    transaction.begin();
    expect(transaction.isActive()).andReturn(true).anyTimes();
    transaction.rollback();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity, jpaEntityManager, session, databaseLogin);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    try {
      dataStore.store(new TestEntity(99, longString, new TestSubEntity("bar")));
      Assert.fail();
    } catch (PersistenceException e) {
      // expected
    }

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity, jpaEntityManager, session, databaseLogin);
  }

  @Test
  public void testRemove() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    JpaEntityManager jpaEntityManager = createMock(JpaEntityManager.class);
    ServerSession session = createMock(ServerSession.class);
    DatabaseLogin databaseLogin = createMock(DatabaseLogin.class);
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);
    DynamicEntity dynamicEntity = createMock(DynamicEntity.class);

    // set expectations
    PowerMock.mockStatic(JpaHelper.class);
    expect(JpaHelper.getEntityManager(entityManager)).andReturn(jpaEntityManager).anyTimes();
    PowerMock.replay(JpaHelper.class);
    expect(jpaEntityManager.getServerSession()).andReturn(session).anyTimes();
    expect(session.getLogin()).andReturn(databaseLogin).anyTimes();
    Capture<Sequence> sequenceCapture = EasyMock.newCapture();
    databaseLogin.addSequence(capture(sequenceCapture));
    EasyMock.expectLastCall().anyTimes();

    Capture<DynamicType> typeCapture = EasyMock.newCapture();
    Capture<DynamicType> typeCapture2 = EasyMock.newCapture();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager).anyTimes();
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();
    Capture<Class> entityClassCapture = EasyMock.newCapture();
    expect(entityManager.getReference(capture(entityClassCapture), eq(99))).andReturn(dynamicEntity);
    entityManager.remove(dynamicEntity);
    entityManager.close();

    transaction.begin();
    transaction.commit();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity, jpaEntityManager, session, databaseLogin);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    dataStore.remove(new TestEntity(99, "foo", new TestSubEntity("bar")));

    if ((entityClassCapture.getValue() != typeCapture.getValue().getJavaClass()) &&
        (entityClassCapture.getValue() != typeCapture2.getValue().getJavaClass())) {
      Assert.fail();
    }

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity, jpaEntityManager, session, databaseLogin);
  }

  @Test
  public void testFind() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    JpaEntityManager jpaEntityManager = createMock(JpaEntityManager.class);
    ServerSession session = createMock(ServerSession.class);
    DatabaseLogin databaseLogin = createMock(DatabaseLogin.class);
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    DynamicEntity dynamicEntity = createMock(DynamicEntity.class);

    // set expectations
    PowerMock.mockStatic(JpaHelper.class);
    expect(JpaHelper.getEntityManager(entityManager)).andReturn(jpaEntityManager).anyTimes();
    PowerMock.replay(JpaHelper.class);
    expect(jpaEntityManager.getServerSession()).andReturn(session).anyTimes();
    expect(session.getLogin()).andReturn(databaseLogin).anyTimes();
    Capture<Sequence> sequenceCapture = EasyMock.newCapture();
    databaseLogin.addSequence(capture(sequenceCapture));
    EasyMock.expectLastCall().anyTimes();

    Capture<DynamicType> typeCapture = EasyMock.newCapture();
    Capture<DynamicType> typeCapture2 = EasyMock.newCapture();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager).anyTimes();
    Capture<Class> entityClassCapture = EasyMock.newCapture();
    expect(entityManager.find(capture(entityClassCapture), eq(99))).andReturn(dynamicEntity);
    entityManager.close();

    expect(dynamicEntity.get("DS_id")).andReturn(99);
    expect(dynamicEntity.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity = new TestSubEntity("bar");
    expect(dynamicEntity.get("DS_subEntity")).andReturn(subEntity);

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, dynamicEntity, schemaManager, jpaEntityManager, session, databaseLogin);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    TestEntity entity = dataStore.find(TestEntity.class, 99);

    // Ensure the requested class type is one of the available types....
    if ((entityClassCapture.getValue() != typeCapture.getValue().getJavaClass()) &&
        (entityClassCapture.getValue() != typeCapture2.getValue().getJavaClass())) {
      Assert.fail();
    }
    Assert.assertEquals(99, (int) entity.getId());
    Assert.assertEquals("foo", entity.getName());

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, dynamicEntity, schemaManager, jpaEntityManager, session, databaseLogin);
  }

  @Test
  public void testFindAll() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    JpaEntityManager jpaEntityManager = createMock(JpaEntityManager.class);
    ServerSession session = createMock(ServerSession.class);
    DatabaseLogin databaseLogin = createMock(DatabaseLogin.class);
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    DynamicEntity dynamicEntity = createMock(DynamicEntity.class);
    Query query = createMock(Query.class);

    // set expectations
    PowerMock.mockStatic(JpaHelper.class);
    expect(JpaHelper.getEntityManager(entityManager)).andReturn(jpaEntityManager).anyTimes();
    PowerMock.replay(JpaHelper.class);
    expect(jpaEntityManager.getServerSession()).andReturn(session).anyTimes();
    expect(session.getLogin()).andReturn(databaseLogin).anyTimes();
    Capture<Sequence> sequenceCapture = EasyMock.newCapture();
    databaseLogin.addSequence(capture(sequenceCapture));
    EasyMock.expectLastCall().anyTimes();
    Capture<DynamicType> typeCapture = EasyMock.newCapture();
    Capture<DynamicType> typeCapture2 = EasyMock.newCapture();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager).anyTimes();
    expect(entityManager.createQuery(
        "SELECT e FROM DS_DataStoreImplTest$TestEntity_1 e WHERE e.DS_id=99")).andReturn(query);
    entityManager.close();

    expect(query.getResultList()).andReturn(Collections.singletonList(dynamicEntity));

    expect(dynamicEntity.get("DS_id")).andReturn(99);
    expect(dynamicEntity.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity = new TestSubEntity("bar");
    expect(dynamicEntity.get("DS_subEntity")).andReturn(subEntity);

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, dynamicEntity, query, schemaManager, jpaEntityManager, session, databaseLogin);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    Collection<TestEntity> entities = dataStore.findAll(TestEntity.class, "id=99");

    Assert.assertEquals(1, entities.size());

    TestEntity entity = entities.iterator().next();

    Assert.assertEquals(99, (int) entity.getId());
    Assert.assertEquals("foo", entity.getName());

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, dynamicEntity, query, schemaManager, jpaEntityManager, session, databaseLogin);
  }

  @Test
  public void testFindAll_multiple() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    JpaEntityManager jpaEntityManager = createMock(JpaEntityManager.class);
    ServerSession session = createMock(ServerSession.class);
    DatabaseLogin databaseLogin = createMock(DatabaseLogin.class);
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    DynamicEntity dynamicEntity1 = createMock(DynamicEntity.class);
    DynamicEntity dynamicEntity2 = createMock(DynamicEntity.class);
    DynamicEntity dynamicEntity3 = createMock(DynamicEntity.class);
    Query query = createMock(Query.class);

    // set expectations
    PowerMock.mockStatic(JpaHelper.class);
    expect(JpaHelper.getEntityManager(entityManager)).andReturn(jpaEntityManager).anyTimes();
    PowerMock.replay(JpaHelper.class);
    expect(jpaEntityManager.getServerSession()).andReturn(session).anyTimes();
    expect(session.getLogin()).andReturn(databaseLogin).anyTimes();
    Capture<Sequence> sequenceCapture = EasyMock.newCapture();
    databaseLogin.addSequence(capture(sequenceCapture));
    EasyMock.expectLastCall().anyTimes();
    Capture<DynamicType> typeCapture = EasyMock.newCapture();
    Capture<DynamicType> typeCapture2 = EasyMock.newCapture();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager).anyTimes();
    expect(entityManager.createQuery(
        "SELECT e FROM DS_DataStoreImplTest$TestEntity_1 e WHERE e.DS_name='foo'")).andReturn(query);
    entityManager.close();

    List<DynamicEntity> entityList = new LinkedList<>();
    entityList.add(dynamicEntity1);
    entityList.add(dynamicEntity2);
    entityList.add(dynamicEntity3);

    expect(query.getResultList()).andReturn(entityList);

    expect(dynamicEntity1.get("DS_id")).andReturn(99);
    expect(dynamicEntity1.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity1 = new TestSubEntity("bar");
    expect(dynamicEntity1.get("DS_subEntity")).andReturn(subEntity1);

    expect(dynamicEntity2.get("DS_id")).andReturn(100);
    expect(dynamicEntity2.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity2 = new TestSubEntity("bar");
    expect(dynamicEntity2.get("DS_subEntity")).andReturn(subEntity2);

    expect(dynamicEntity3.get("DS_id")).andReturn(101);
    expect(dynamicEntity3.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity3 = new TestSubEntity("bar");
    expect(dynamicEntity3.get("DS_subEntity")).andReturn(subEntity3);

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper,
        dynamicEntity1, dynamicEntity2, dynamicEntity3, query, schemaManager, jpaEntityManager, session, databaseLogin);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    Collection<TestEntity> entities = dataStore.findAll(TestEntity.class, "name='foo'");

    Assert.assertEquals(3, entities.size());

    for (TestEntity entity : entities) {
      Assert.assertEquals("foo", entity.getName());
    }

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper,
        dynamicEntity1, dynamicEntity2, dynamicEntity3, query, schemaManager, jpaEntityManager, session, databaseLogin);
  }

  private DataStoreImpl getDataStore(EntityManagerFactory entityManagerFactory,
                                     JPADynamicHelper jpaDynamicHelper,
                                     DynamicClassLoader classLoader,
                                     SchemaManager schemaManager)
      throws Exception {
    ViewConfig viewConfig = ViewConfigTest.getConfig(xml);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity(viewConfig);

    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity(viewDefinition, instanceConfig);

    setPersistenceEntities(viewInstanceEntity);

    Injector injector = Guice.createInjector(
        new TestModule(viewInstanceEntity, entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager));
    return injector.getInstance(DataStoreImpl.class);
  }


  // TODO : move to ViewEntityEntity test.
  private static void setPersistenceEntities(ViewInstanceEntity viewInstanceDefinition) {
    ViewEntity viewDefinition = viewInstanceDefinition.getViewEntity();
    Collection<ViewEntityEntity> entities = new HashSet<>();

    ViewConfig viewConfig = viewDefinition.getConfiguration();
    for (EntityConfig entityConfiguration : viewConfig.getPersistence().getEntities()) {
      ViewEntityEntity viewEntityEntity = new ViewEntityEntity();

      viewEntityEntity.setId(1L);
      viewEntityEntity.setViewName(viewDefinition.getName());
      viewEntityEntity.setViewInstanceName(viewInstanceDefinition.getName());
      viewEntityEntity.setClassName(entityConfiguration.getClassName());
      viewEntityEntity.setIdProperty(entityConfiguration.getIdProperty());
      viewEntityEntity.setViewInstance(viewInstanceDefinition);

      entities.add(viewEntityEntity);
    }
    viewInstanceDefinition.setEntities(entities);
  }


  public static class TestEntity {

    public TestEntity() {
    }

    TestEntity(int id, String name, TestSubEntity subEntity) {
      this.id = id;
      this.name = name;
      this.subEntity = subEntity;
    }

    TestEntity(String name, TestSubEntity subEntity) {
      this.name = name;
      this.subEntity = subEntity;
    }

    Integer id = null;
    String name;
    TestSubEntity subEntity;

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public TestSubEntity getSubEntity() {
      return subEntity;
    }

    public void setSubEntity(TestSubEntity subEntity) {
      this.subEntity = subEntity;
    }
  }

  public static class TestSubEntity {

    private Integer id = null;

    public TestSubEntity() {
    }

    TestSubEntity(String name) {
      this.name = name;
    }

    TestSubEntity(Integer id, String name) {
      this.id = id;
      this.name = name;
    }

    String name;

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class TestLargeEntity {

    public TestLargeEntity() {
    }

    public TestLargeEntity(int id) {
      this.id = id;
    }

    Integer id = null;

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }
  }

  private static class TestModule implements Module, SchemaManagerFactory {
    private final ViewInstanceEntity viewInstanceEntity;
    private final EntityManagerFactory entityManagerFactory;
    private final JPADynamicHelper jpaDynamicHelper;
    private final DynamicClassLoader classLoader;
    private final SchemaManager schemaManager;

    private TestModule(ViewInstanceEntity viewInstanceEntity, EntityManagerFactory entityManagerFactory,
                       JPADynamicHelper jpaDynamicHelper, DynamicClassLoader classLoader,
                       SchemaManager schemaManager) {
      this.viewInstanceEntity = viewInstanceEntity;
      this.entityManagerFactory = entityManagerFactory;
      this.jpaDynamicHelper = jpaDynamicHelper;
      this.classLoader = classLoader;
      this.schemaManager = schemaManager;
    }

    @Override
    public void configure(Binder binder) {
      binder.bind(ViewInstanceEntity.class).toInstance(viewInstanceEntity);
      binder.bind(EntityManagerFactory.class).toInstance(entityManagerFactory);
      binder.bind(JPADynamicHelper.class).toInstance(jpaDynamicHelper);
      binder.bind(DynamicClassLoader.class).toInstance(classLoader);
      binder.bind(SchemaManagerFactory.class).toInstance(this);
    }

    @Override
    public SchemaManager getSchemaManager(DatabaseSession session) {
      return schemaManager;
    }
  }
}
