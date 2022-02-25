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

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provider;

/**
 * BlueprintDAO unit tests.
 */
public class BlueprintDAOTest {

  Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  EntityManager entityManager = createStrictMock(EntityManager.class);

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).atLeastOnce();
    replay(entityManagerProvider);
  }

  @Test
  public void testFindByName() {
    BlueprintEntity entity = new BlueprintEntity();

    // set expectations
    expect(entityManager.find(eq(BlueprintEntity.class), eq("test-cluster-name"))).andReturn(entity);
    replay(entityManager);

    BlueprintDAO dao = new BlueprintDAO();
    dao.entityManagerProvider = entityManagerProvider;
    BlueprintEntity result = dao.findByName("test-cluster-name");

    assertSame(result, entity);
    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testFindAll() {
    BlueprintEntity entity = new BlueprintEntity();
    TypedQuery<BlueprintEntity> query = createStrictMock(TypedQuery.class);

    // set expectations
    expect(entityManager.createNamedQuery(eq("allBlueprints"), eq(BlueprintEntity.class))).andReturn(query);
    expect(query.getResultList()).andReturn(Collections.singletonList(entity));
    replay(entityManager, query);

    BlueprintDAO dao = new BlueprintDAO();
    dao.entityManagerProvider = entityManagerProvider;
    List<BlueprintEntity> results = dao.findAll();

    assertEquals(1, results.size());
    assertSame(entity, results.get(0));

    verify(entityManagerProvider, entityManager, query);
  }

  @Test
  public void testRefresh() {
    BlueprintEntity entity = new BlueprintEntity();

    // set expectations
    entityManager.refresh(eq(entity));
    replay(entityManager);

    BlueprintDAO dao = new BlueprintDAO();
    dao.entityManagerProvider = entityManagerProvider;
    dao.refresh(entity);

    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testCreate() {
    BlueprintEntity entity = new BlueprintEntity();

    // set expectations
    entityManager.persist(eq(entity));
    replay(entityManager);

    BlueprintDAO dao = new BlueprintDAO();
    dao.entityManagerProvider = entityManagerProvider;
    dao.create(entity);

    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testMerge() {
    BlueprintEntity entity = new BlueprintEntity();
    BlueprintEntity entity2 = new BlueprintEntity();

    // set expectations
    expect(entityManager.merge(eq(entity))).andReturn(entity2);
    replay(entityManager);

    BlueprintDAO dao = new BlueprintDAO();
    dao.entityManagerProvider = entityManagerProvider;
    assertSame(entity2, dao.merge(entity));

    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testRemove() {
    BlueprintEntity entity = new BlueprintEntity();
    BlueprintEntity entity2 = new BlueprintEntity();

    // set expectations
    expect(entityManager.merge(eq(entity))).andReturn(entity2);
    entityManager.remove(eq(entity2));
    replay(entityManager);

    BlueprintDAO dao = new BlueprintDAO();
    dao.entityManagerProvider = entityManagerProvider;
    dao.remove(entity);

    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testRemoveByName() {
    BlueprintEntity entity = new BlueprintEntity();
    BlueprintDAO dao = new BlueprintDAO();
    dao.entityManagerProvider = entityManagerProvider;

    expect(entityManager.find(eq(BlueprintEntity.class), eq("test"))).andReturn(entity);
    entityManager.remove(entity);
    expectLastCall();

    replay(entityManager);

    dao.removeByName("test");

    verify(entityManager);
  }
}
