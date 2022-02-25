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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

import javax.persistence.EntityManager;

import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provider;

/**
 * PrincipalTypeDAO tests
 */
public class PrincipalTypeDAOTest {
  Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  EntityManager entityManager = createStrictMock(EntityManager.class);

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).atLeastOnce();
    replay(entityManagerProvider);
  }

  @Test
  public void testFindById() throws Exception {
    PrincipalTypeEntity entity = new PrincipalTypeEntity();

    // set expectations
    expect(entityManager.find(PrincipalTypeEntity.class, 99)).andReturn(entity);
    replay(entityManager);

    PrincipalTypeDAO dao = new PrincipalTypeDAO();
    dao.entityManagerProvider = entityManagerProvider;

    Assert.assertEquals(entity, dao.findById(99));
  }
}
